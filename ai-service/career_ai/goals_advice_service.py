from __future__ import annotations

import hashlib
import math
import re
from collections import Counter, defaultdict
from dataclasses import dataclass
from typing import Any


PII_RE = re.compile(
    r"[\w.+-]+@[\w.-]+\.[A-Za-z]{2,}|(?:\+?86[-\s]?)?1[3-9]\d{9}|"
    r"(?:api[_-]?key|token|secret|password)\s*[:=]\s*[\w.-]{6,}",
    re.IGNORECASE,
)
TOKEN_RE = re.compile(r"(?:sk|ak|token|secret|key)[-_.][A-Za-z0-9._-]{8,}", re.IGNORECASE)


@dataclass(frozen=True)
class RagRecord:
    record_id: str
    text: str
    summary: str
    metadata: dict[str, Any]
    embedding: list[float]


def _as_dict(value: Any) -> dict[str, Any]:
    return value if isinstance(value, dict) else {}


def _as_list(value: Any) -> list[Any]:
    return value if isinstance(value, list) else []


def _safe_text(value: Any, max_chars: int = 180) -> str:
    if not isinstance(value, (str, int, float, bool)):
        return ""
    text = str(value).strip()
    text = PII_RE.sub("[REDACTED]", text)
    text = TOKEN_RE.sub("[REDACTED]", text)
    text = re.sub(r"\s+", " ", text)
    return text[:max_chars]


def _as_int(value: Any, default: int = 0) -> int:
    try:
        return int(value)
    except (TypeError, ValueError):
        return default


def _normalize_score(value: float, max_value: float) -> float:
    if max_value <= 0:
        return 0.0
    return round(max(0.0, min(1.0, value / max_value)), 6)


def _tokens(text: str) -> list[str]:
    return re.findall(r"[\w\u4e00-\u9fff]+", text.lower())


def _summary(text: str, max_chars: int = 110) -> str:
    clean = _safe_text(text, max_chars * 2)
    if len(clean) <= max_chars:
        return clean
    return clean[:max_chars].rstrip() + "..."


def _hash_embedding(text: str, dims: int = 12) -> list[float]:
    digest = hashlib.sha256(text.encode("utf-8")).digest()
    vector = []
    for index in range(dims):
        raw = digest[index] / 255.0
        vector.append(round(raw * 2 - 1, 6))
    return vector


def _cosine(left: list[float], right: list[float]) -> float:
    dot = sum(a * b for a, b in zip(left, right))
    left_norm = math.sqrt(sum(a * a for a in left))
    right_norm = math.sqrt(sum(b * b for b in right))
    if left_norm == 0 or right_norm == 0:
        return 0.0
    return dot / (left_norm * right_norm)


def recursive_chunks(text: str, max_chars: int = 140) -> list[str]:
    clean = _safe_text(text, 1200)
    if not clean:
        return []
    separators = ["\n\n", "\n", "。", "；", ";", "，", ","]
    chunks = [clean]
    for separator in separators:
        next_chunks: list[str] = []
        for chunk in chunks:
            if len(chunk) <= max_chars:
                next_chunks.append(chunk)
                continue
            parts = [part.strip() for part in chunk.split(separator) if part.strip()]
            if len(parts) <= 1:
                next_chunks.append(chunk)
            else:
                next_chunks.extend(parts)
        chunks = next_chunks
    final_chunks: list[str] = []
    for chunk in chunks:
        if len(chunk) <= max_chars:
            final_chunks.append(chunk)
        else:
            final_chunks.extend(chunk[i : i + max_chars] for i in range(0, len(chunk), max_chars))
    return [chunk for chunk in final_chunks if chunk.strip()]


def _metadata_filters(payload: dict[str, Any]) -> dict[str, Any]:
    options = _as_dict(payload.get("retrievalOptions"))
    filters = _as_dict(options.get("metadataFilters"))
    goal = _as_dict(payload.get("goal"))
    user_id = _safe_text(payload.get("userId"), 32)
    goal_id = _safe_text(goal.get("id"), 32)
    result = {
        "userId": user_id,
        "goalId": goal_id,
        "documentTypes": [
            _safe_text(item, 40)
            for item in _as_list(filters.get("documentTypes") or ["goal", "milestone", "successCriteria"])
            if _safe_text(item, 40)
        ],
        "visibilityScope": _safe_text(filters.get("visibilityScope"), 40) or "USER_PRIVATE",
    }
    return result


def build_summary_index(payload: dict[str, Any]) -> list[RagRecord]:
    goal = _as_dict(payload.get("goal"))
    filters = _metadata_filters(payload)
    allowed_document_types = set(filters.get("documentTypes") or ["goal", "milestone", "successCriteria"])
    records: list[RagRecord] = []

    def add_record(record_id: str, text: str, document_type: str, position: int) -> None:
        if document_type not in allowed_document_types:
            return
        for chunk_index, chunk in enumerate(recursive_chunks(text)):
            metadata = {
                **filters,
                "documentType": document_type,
                "chunkPosition": position,
                "chunkIndex": chunk_index,
                "source": "goals",
            }
            summary = _summary(chunk)
            rid = f"{record_id}:chunk:{chunk_index}"
            records.append(RagRecord(rid, chunk, summary, metadata, _hash_embedding(summary + "\n" + chunk)))

    goal_text = " ".join(
        item
        for item in [
            _safe_text(goal.get("title"), 160),
            _safe_text(goal.get("desc"), 240),
            _safe_text(goal.get("status"), 40),
            _safe_text(goal.get("eta"), 80),
        ]
        if item
    )
    add_record(f"goal_{filters['goalId']}", goal_text, "goal", 0)

    for index, milestone in enumerate(item for item in _as_list(payload.get("milestones")) if isinstance(item, dict)):
        text = " ".join(
            item
            for item in [
                _safe_text(milestone.get("title"), 160),
                _safe_text(milestone.get("desc"), 220),
                _safe_text(milestone.get("status"), 40),
                _safe_text(milestone.get("progress"), 12),
            ]
            if item
        )
        add_record(f"goal_{filters['goalId']}:milestone_{_safe_text(milestone.get('id'), 32) or index}", text, "milestone", index)

    criteria = _as_dict(payload.get("successCriteria"))
    criteria_text = " ".join(
        [
            _safe_text(criteria.get("salary"), 80),
            " ".join(_safe_text(item, 80) for item in _as_list(criteria.get("companies"))),
            " ".join(_safe_text(item, 80) for item in _as_list(criteria.get("cities"))),
        ]
    )
    add_record(f"goal_{filters['goalId']}:success_criteria", criteria_text, "successCriteria", 0)
    return records


def expand_queries(payload: dict[str, Any]) -> list[str]:
    goal = _as_dict(payload.get("goal"))
    title = _safe_text(goal.get("title"), 80) or "职业目标"
    desc = _safe_text(goal.get("desc"), 100)
    return [
        f"{title} 目标拆解 下一步",
        f"{title} 技能差距 里程碑",
        f"{title} 简历证据 岗位要求",
        f"{desc or title} 成功准则 投递准备",
    ]


def _bm25_scores(query: str, records: list[RagRecord]) -> dict[str, float]:
    query_tokens = _tokens(query)
    doc_tokens = [_tokens(record.summary + " " + record.text) for record in records]
    doc_freq: Counter[str] = Counter()
    for tokens in doc_tokens:
        doc_freq.update(set(tokens))
    total_docs = max(1, len(records))
    avg_len = sum(len(tokens) for tokens in doc_tokens) / total_docs if doc_tokens else 1
    scores: dict[str, float] = {}
    for record, tokens in zip(records, doc_tokens):
        counts = Counter(tokens)
        score = 0.0
        for token in query_tokens:
            if not counts[token]:
                continue
            idf = math.log(1 + (total_docs - doc_freq[token] + 0.5) / (doc_freq[token] + 0.5))
            denom = counts[token] + 1.5 * (1 - 0.75 + 0.75 * len(tokens) / max(1, avg_len))
            score += idf * counts[token] * 2.5 / denom
        scores[record.record_id] = score
    max_score = max(scores.values(), default=0.0)
    return {key: _normalize_score(value, max_score) for key, value in scores.items()}


def _embedding_scores(query: str, records: list[RagRecord]) -> dict[str, float]:
    query_embedding = _hash_embedding(query)
    raw_scores = {record.record_id: (_cosine(query_embedding, record.embedding) + 1) / 2 for record in records}
    max_score = max(raw_scores.values(), default=0.0)
    return {key: _normalize_score(value, max_score) for key, value in raw_scores.items()}


def _rank(scores: dict[str, float]) -> list[str]:
    return [item[0] for item in sorted(scores.items(), key=lambda item: item[1], reverse=True)]


def rag_fusion(queries: list[str], records: list[RagRecord]) -> dict[str, float]:
    fused: defaultdict[str, float] = defaultdict(float)
    for query in queries:
        for ranking in (_rank(_bm25_scores(query, records)), _rank(_embedding_scores(query, records))):
            for rank, record_id in enumerate(ranking, start=1):
                fused[record_id] += 1 / (60 + rank)
    max_score = max(fused.values(), default=0.0)
    return {key: _normalize_score(value, max_score) for key, value in fused.items()}


def rerank_records(records: list[RagRecord], fused: dict[str, float], limit: int = 3) -> list[tuple[RagRecord, float]]:
    lookup = {record.record_id: record for record in records}
    ranked = []
    for record_id, score in sorted(fused.items(), key=lambda item: item[1], reverse=True):
        record = lookup.get(record_id)
        if record:
            ranked.append((record, score))
        if len(ranked) >= limit:
            break
    return ranked


def _priority(progress: int, evidence_count: int) -> str:
    if progress < 35 or evidence_count == 0:
        return "HIGH"
    if progress < 75:
        return "MEDIUM"
    return "LOW"


def _contains_sensitive(value: Any) -> bool:
    text = json_dumps(value)
    return bool(PII_RE.search(text) or TOKEN_RE.search(text))


def json_dumps(value: Any) -> str:
    import json

    return json.dumps(value, ensure_ascii=False, sort_keys=True)


def generate_goal_advice(payload: dict[str, Any]) -> dict[str, Any]:
    goal = _as_dict(payload.get("goal"))
    if not goal:
        raise ValueError("goal is required")

    filters = _metadata_filters(payload)
    queries = expand_queries(payload)
    records = build_summary_index(payload)
    fused = rag_fusion(queries, records) if records else {}
    selected = rerank_records(records, fused, 3)
    progress = max(0, min(100, _as_int(goal.get("progress"), 0)))
    title = _safe_text(goal.get("title"), 80) or "职业目标"
    status = _safe_text(goal.get("status"), 40) or "TODO"
    eta = _safe_text(goal.get("eta"), 60) or "未设定"
    priority = _priority(progress, len(selected))

    if selected:
        next_action = f"优先围绕「{selected[0][0].summary}」补齐可验证成果，并把证据同步到简历或目标里程碑。"
    else:
        next_action = "先补充 2-3 个可检查里程碑，并为每个里程碑绑定项目、投递或学习产出证据。"

    content = (
        f"当前目标「{title}」状态为{status}，进度约{progress}%，预计达成为{eta}。"
        f"建议按{priority}优先级推进：{next_action}"
        "本次建议基于 Goals-RAG 的摘要索引、用户元数据过滤、多查询、BM25 与轻量 embedding 混合召回、RRF 融合和确定性重排生成。"
    )

    evidence = [
        {
            "sourceType": record.metadata["documentType"],
            "sourceId": record.record_id,
            "reason": record.summary,
            "score": score,
        }
        for record, score in selected
    ]
    diagnostics = {
        "expandedQueries": queries,
        "metadataFilters": filters,
        "retrieval": "multi_query+bm25+embedding",
        "fusion": "rag_fusion_rrf",
        "reranker": "deterministic_fallback",
        "chunking": "recursive",
        "summaryIndexCount": len(records),
        "candidateCount": len(records),
        "selectedEvidenceCount": len(evidence),
        "emptyRetrievalFallback": not bool(selected),
        "scoreNormalization": "minmax_0_1",
    }
    result = {
        "content": content,
        "evidenceReferences": evidence,
        "retrievalDiagnostics": diagnostics,
    }
    if _contains_sensitive(result):
        raise ValueError("sensitive data detected in diagnostics")
    return result
