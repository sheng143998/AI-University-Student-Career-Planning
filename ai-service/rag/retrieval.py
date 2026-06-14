from collections import Counter, defaultdict
from dataclasses import dataclass
import math
import re

from rag.chunking import Chunk


TOKEN_RE = re.compile(r"[A-Za-z0-9_]+|[\u4e00-\u9fff]+", re.UNICODE)


@dataclass(frozen=True)
class ScoredChunk:
    chunk: Chunk
    score: float
    source: str


def tokenize(text: str) -> list[str]:
    tokens: list[str] = []
    for token in TOKEN_RE.findall(text):
        normalized = token.lower()
        if re.fullmatch(r"[\u4e00-\u9fff]+", normalized):
            tokens.extend(normalized)
            tokens.extend(normalized[index:index + 2] for index in range(len(normalized) - 1))
        else:
            tokens.append(normalized)
    return tokens


def expand_queries(query: str) -> list[str]:
    query = query.strip()
    expansions = [
        query,
        f"{query} 职业规划 建议",
        f"{query} 技能差距 简历证据",
        f"{query} 岗位要求 JD 匹配",
    ]
    seen = set()
    result = []
    for item in expansions:
        if item and item not in seen:
            seen.add(item)
            result.append(item)
    return result


def metadata_match(chunk: Chunk, metadata_filter: dict | None) -> bool:
    if not metadata_filter:
        return True
    metadata = chunk.metadata
    if metadata.get("userId") != metadata_filter.get("userId"):
        return False
    document_types = metadata_filter.get("documentTypes") or []
    if document_types and metadata.get("documentType") not in document_types:
        return False
    for key in ("resumeId", "jobId", "visibilityScope"):
        expected = metadata_filter.get(key)
        if expected is not None and metadata.get(key) != expected:
            return False
    return True


class HybridRetriever:
    def retrieve(self, queries: list[str], chunks: list[Chunk], metadata_filter: dict | None, top_k: int = 8) -> list[ScoredChunk]:
        candidates = [chunk for chunk in chunks if metadata_match(chunk, metadata_filter)]
        if not candidates:
            return []

        ranked_lists: list[list[ScoredChunk]] = []
        for query in queries:
            ranked_lists.append(self._bm25(query, candidates, top_k))
            ranked_lists.append(self._embedding_like(query, candidates, top_k))
        return reciprocal_rank_fusion(ranked_lists, top_k=top_k)

    def _bm25(self, query: str, chunks: list[Chunk], top_k: int) -> list[ScoredChunk]:
        query_terms = tokenize(query)
        docs = [tokenize(chunk.text) for chunk in chunks]
        doc_count = len(docs)
        avg_len = sum(len(doc) for doc in docs) / max(doc_count, 1)
        doc_freq: Counter[str] = Counter()
        for doc in docs:
            doc_freq.update(set(doc))

        results = []
        for chunk, doc in zip(chunks, docs):
            frequencies = Counter(doc)
            score = 0.0
            for term in query_terms:
                if not frequencies[term]:
                    continue
                idf = math.log(1 + (doc_count - doc_freq[term] + 0.5) / (doc_freq[term] + 0.5))
                numerator = frequencies[term] * 2.2
                denominator = frequencies[term] + 1.2 * (0.25 + 0.75 * len(doc) / max(avg_len, 1))
                score += idf * numerator / denominator
            if score > 0:
                results.append(ScoredChunk(chunk=chunk, score=score, source="bm25"))
        return sorted(results, key=lambda item: item.score, reverse=True)[:top_k]

    def _embedding_like(self, query: str, chunks: list[Chunk], top_k: int) -> list[ScoredChunk]:
        query_vector = term_vector(query)
        results = []
        for chunk in chunks:
            score = cosine(query_vector, term_vector(chunk.text))
            if score > 0:
                results.append(ScoredChunk(chunk=chunk, score=score, source="embedding"))
        return sorted(results, key=lambda item: item.score, reverse=True)[:top_k]


def term_vector(text: str) -> Counter[str]:
    return Counter(tokenize(text))


def cosine(left: Counter[str], right: Counter[str]) -> float:
    if not left or not right:
        return 0.0
    common = set(left) & set(right)
    numerator = sum(left[token] * right[token] for token in common)
    left_norm = math.sqrt(sum(value * value for value in left.values()))
    right_norm = math.sqrt(sum(value * value for value in right.values()))
    if left_norm == 0 or right_norm == 0:
        return 0.0
    return numerator / (left_norm * right_norm)


def reciprocal_rank_fusion(ranked_lists: list[list[ScoredChunk]], top_k: int = 8, k: int = 60) -> list[ScoredChunk]:
    scores: dict[str, float] = defaultdict(float)
    best: dict[str, ScoredChunk] = {}
    for ranked in ranked_lists:
        for rank, item in enumerate(ranked, start=1):
            chunk_id = item.chunk.chunk_id
            scores[chunk_id] += 1.0 / (k + rank)
            if chunk_id not in best or item.score > best[chunk_id].score:
                best[chunk_id] = item

    fused = [
        ScoredChunk(chunk=best[chunk_id].chunk, score=score, source="rag-fusion")
        for chunk_id, score in scores.items()
    ]
    return sorted(fused, key=lambda item: item.score, reverse=True)[:top_k]
