from pathlib import Path
import sys

from fastapi.testclient import TestClient

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))

from app.main import app
from rag.chunking import RecursiveChunker
from rag.retrieval import HybridRetriever, expand_queries
from rag.summary_index import build_summary_index
from rag.chat_pipeline import ChatRagPipeline
from schemas.chat import ChatCompleteRequest, ChatHistoryItem, MetadataFilter, RetrievalOptions


def test_recursive_chunker_splits_long_text_with_metadata():
    chunker = RecursiveChunker(chunk_size=20, overlap=4)
    chunks = chunker.split("第一段。第二段很长很长很长很长。第三段也很长很长。", {"documentId": "resume-1", "userId": 1})

    assert len(chunks) > 1
    assert chunks[0].metadata["userId"] == 1
    assert chunks[0].chunk_id.startswith("resume-1-")


def test_summary_index_binds_raw_chunk_id():
    chunker = RecursiveChunker(chunk_size=20)
    chunks = chunker.split("Vue TypeScript 项目经历和前端工程化经验", {"documentId": "resume-1", "userId": 1})
    summaries = build_summary_index(chunks)

    assert summaries[0].metadata["indexType"] == "summary"
    assert summaries[0].metadata["rawChunkId"] == chunks[0].chunk_id


def test_hybrid_retrieval_respects_metadata_filter():
    chunker = RecursiveChunker()
    chunks = []
    chunks.extend(chunker.split("前端 Vue TypeScript 简历证据", {"documentId": "resume-1", "userId": 1, "documentType": "resume"}))
    chunks.extend(chunker.split("后端 Java 岗位信息", {"documentId": "resume-2", "userId": 2, "documentType": "resume"}))

    results = HybridRetriever().retrieve(
        expand_queries("前端 TypeScript"),
        chunks,
        metadata_filter={"userId": 1, "documentTypes": ["resume"]},
    )

    assert results
    assert all(item.chunk.metadata["userId"] == 1 for item in results)


def test_chat_pipeline_returns_diagnostics_and_evidence():
    request = ChatCompleteRequest(
        userId=1,
        conversationId=10,
        content="前端开发未来趋势",
        resumeId=123,
        parsedData={"skills": ["Vue", "TypeScript"], "targetRole": "前端开发工程师"},
        retrievalOptions=RetrievalOptions(
            metadataFilter=MetadataFilter(userId=1, documentTypes=["resume"], resumeId=123, visibilityScope="private")
        ),
    )

    response = ChatRagPipeline().complete(request)

    assert response.content
    assert response.diagnostics.expandedQueries
    assert response.diagnostics.fusion == "rag-fusion"
    assert response.evidence
    assert any(item.sourceType == "resume" for item in response.evidence)


def test_chat_pipeline_keeps_chat_context_when_filtered():
    request = ChatCompleteRequest(
        userId=1,
        conversationId=10,
        content="继续说 TypeScript 面试准备",
        resumeId=123,
        history=[ChatHistoryItem(role="user", content="我想准备 TypeScript 面试")],
        retrievalOptions=RetrievalOptions(
            metadataFilter=MetadataFilter(
                userId=1,
                documentTypes=["resume", "job", "chat_context"],
                resumeId=123,
                visibilityScope="private",
            )
        ),
    )

    response = ChatRagPipeline().complete(request)

    assert any(item.sourceType == "chat_context" for item in response.evidence)


def test_chat_http_contract_returns_rag_fields():
    request_body = (
        b'{"userId":1,"conversationId":10,"content":"frontend job advice","resumeId":123,'
        b'"parsedData":{"skills":["Vue","TypeScript"],"targetRole":"frontend engineer"},'
        b'"retrievalOptions":{"metadataFilter":{"userId":1,"documentTypes":["resume"],'
        b'"resumeId":123,"visibilityScope":"private"}}}'
    )
    response = TestClient(app).post("/api/v1/chat/complete", content=request_body)

    assert response.status_code == 200
    response_body = response.text
    assert '"content"' in response_body
    assert '"evidence"' in response_body
    assert '"diagnostics"' in response_body
    assert '"expandedQueries"' in response_body


if __name__ == "__main__":
    for name, value in list(globals().items()):
        if name.startswith("test_") and callable(value):
            value()
    print("chat pipeline tests passed")
