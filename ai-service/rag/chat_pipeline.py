from rag.chunking import RecursiveChunker
from rag.retrieval import HybridRetriever, expand_queries
from rag.summary_index import build_summary_index
from schemas.chat import (
    ChatCompleteRequest,
    ChatCompleteResponse,
    ChatDiagnostics,
    DailySuggestionsRequest,
    DailySuggestionsResponse,
    EvidenceItem,
    QuickQuestion,
    SuggestionItem,
)


def compact_value(value) -> str:
    if value is None:
        return ""
    if isinstance(value, dict):
        parts = []
        for key, item in value.items():
            text = compact_value(item)
            if text:
                parts.append(f"{key}: {text}")
        return "；".join(parts)
    if isinstance(value, list):
        return "；".join(compact_value(item) for item in value if compact_value(item))
    return str(value)


class ChatRagPipeline:
    def __init__(self) -> None:
        self.chunker = RecursiveChunker()
        self.retriever = HybridRetriever()

    def complete(self, request: ChatCompleteRequest) -> ChatCompleteResponse:
        seed_documents = self._seed_documents(request)
        chunks = []
        for document in seed_documents:
            chunks.extend(self.chunker.split(document["text"], document["metadata"]))
        summary_chunks = build_summary_index(chunks)
        queries = expand_queries(request.content)
        metadata_filter = None
        if request.retrievalOptions.metadataFilter:
            metadata_filter = request.retrievalOptions.metadataFilter.to_filter_dict()

        retrieved = self.retriever.retrieve(queries, chunks + summary_chunks, metadata_filter=metadata_filter)
        evidence = [
            EvidenceItem(
                sourceType=str(item.chunk.metadata.get("documentType", "unknown")),
                sourceId=str(item.chunk.metadata.get("documentId", "")),
                chunkId=item.chunk.chunk_id,
                summary=item.chunk.text[:180],
                score=round(item.score, 6),
            )
            for item in retrieved[:5]
        ]

        content = self._compose_answer(request, evidence)
        return ChatCompleteResponse(
            content=content,
            suggestionQuestions=self._suggestion_questions(request.content),
            tip="可以上传目标岗位 JD，我会按招聘要求逐条对齐你的经历。",
            title=self._title(request.content),
            evidence=evidence,
            diagnostics=ChatDiagnostics(expandedQueries=queries),
        )

    def daily_suggestions(self, request: DailySuggestionsRequest) -> DailySuggestionsResponse:
        parsed = request.parsedData or {}
        skills = parsed.get("skills") or parsed.get("professional_skills") or []
        target_role = parsed.get("targetRole") or parsed.get("target_role") or "目标岗位"
        skill_text = "、".join(map(str, skills[:5])) if isinstance(skills, list) else str(skills)
        if not skill_text:
            skill_text = "核心技能"

        return DailySuggestionsResponse(
            suggestions=[
                SuggestionItem(title="技能对齐", text=f"围绕{target_role}，补充能证明{skill_text}的项目证据。"),
                SuggestionItem(title="JD 对照", text="选择一条目标岗位 JD，逐项标记已满足和待补强要求。"),
                SuggestionItem(title="表达优化", text="用 STAR 法则重写一条最能体现岗位匹配度的经历。"),
            ],
            quickQuestions=[
                QuickQuestion(title="技能差距", text=f"我和{target_role}还有哪些技能差距？"),
                QuickQuestion(title="简历优化", text="如何把我的项目经历改得更贴合目标岗位？"),
                QuickQuestion(title="行动计划", text="请给我一周内可执行的求职准备计划。"),
            ],
        )

    def _seed_documents(self, request: ChatCompleteRequest) -> list[dict]:
        base_metadata = {
            "userId": request.userId,
            "resumeId": request.resumeId,
            "visibilityScope": "private",
        }
        history_text = "\n".join(f"{item.role}: {item.content}" for item in request.history[-6:])
        documents = [
            {
                "text": (
                    f"用户当前问题：{request.content}\n"
                    f"最近对话上下文：{history_text}\n"
                    "职业规划回答需要结合用户简历、目标岗位 JD、技能差距、项目经历证据和行动建议。"
                ),
                "metadata": {
                    **base_metadata,
                    "documentType": "chat_context",
                    "documentId": f"conversation-{request.conversationId}",
                },
            }
        ]
        parsed_resume = compact_value(request.parsedData)
        if parsed_resume:
            documents.append(
                {
                    "text": f"用户简历解析数据：{parsed_resume}",
                    "metadata": {
                        **base_metadata,
                        "documentType": "resume",
                        "documentId": str(request.resumeId or f"user-{request.userId}-profile"),
                    },
                }
            )
        return documents

    def _compose_answer(self, request: ChatCompleteRequest, evidence: list[EvidenceItem]) -> str:
        if evidence:
            evidence_text = "；".join(item.summary for item in evidence[:2])
            return (
                f"我会按职业规划和岗位匹配的方式回答。针对“{request.content}”，"
                f"当前检索到的主要证据包括：{evidence_text}。"
                "建议先明确目标岗位 JD，再把简历中的项目经历按技能要求逐条对齐。"
            )
        return f"针对“{request.content}”，建议先补充目标岗位、当前简历和期望城市，我再给出更具体的职业规划建议。"

    def _suggestion_questions(self, content: str) -> list[str]:
        return [
            "我的简历和目标岗位 JD 还有哪些差距？",
            "如何把项目经历改写得更符合岗位要求？",
            f"围绕“{content[:18]}”，下一步应该做什么？",
        ]

    def _title(self, content: str) -> str:
        compact = "".join(content.split())
        return compact[:18] or "职业规划对话"
