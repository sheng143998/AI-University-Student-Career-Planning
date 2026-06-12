from __future__ import annotations

import hashlib
import math
import re
from collections import Counter, defaultdict
from dataclasses import dataclass
from typing import Any


TOKEN_RE = re.compile(r"[\w\u4e00-\u9fff]+", re.UNICODE)
EMAIL_RE = re.compile(r"[\w.+-]+@[\w.-]+\.[A-Za-z]{2,}")
PHONE_RE = re.compile(r"(?<!\d)(?:\+?86[- ]?)?1[3-9]\d{9}(?!\d)")
ID_CARD_RE = re.compile(r"(?<!\d)\d{17}[\dXx](?!\d)")


@dataclass(frozen=True)
class Chunk:
    id: str
    text: str
    metadata: dict[str, Any]


class ReportSupportService:
    def generate_support(self, payload: dict[str, Any]) -> dict[str, Any]:
        report_id = payload.get("reportId")
        user_id = payload.get("userId")
        if not report_id or not user_id:
            raise ValueError("reportId and userId are required")

        target_job = str(payload.get("targetJobName") or payload.get("careerData", {}).get("targetJob") or "target job")
        filters = payload.get("metadataFilters") or {}
        chunks = self._build_chunks(payload)
        filtered_chunks = self._apply_filters(chunks, filters)
        queries = self._expand_queries(target_job, payload)
        ranked = self._hybrid_retrieve(queries, filtered_chunks)

        diagnostics = {
            "status": "OK",
            "retrievalMode": "deterministic_fallback",
            "embeddingMode": "hash_embedding_fallback",
            "expandedQueryCount": len(queries),
            "candidateCount": len(filtered_chunks),
            "selectedEvidenceCount": 0,
            "scoreNormalization": "min_max",
            "fusion": "rrf",
            "reranker": "deterministic_keyword_overlap",
            "emptyRetrieval": False,
        }

        if not ranked:
            diagnostics.update(
                {
                    "status": "EMPTY_RETRIEVAL",
                    "candidateCount": 0,
                    "emptyRetrieval": True,
                }
            )
            return {
                "status": "EMPTY_RETRIEVAL",
                "aiSuggestions": "",
                "evidenceRefs": [],
                "ragDiagnostics": diagnostics,
            }

        evidence = [self._to_evidence_ref(chunk, score) for chunk, score in ranked[:3]]
        diagnostics["selectedEvidenceCount"] = len(evidence)
        return {
            "status": "OK",
            "aiSuggestions": self._build_suggestions(target_job, payload, evidence),
            "evidenceRefs": evidence,
            "ragDiagnostics": diagnostics,
        }

    def _build_chunks(self, payload: dict[str, Any]) -> list[Chunk]:
        user_id = payload.get("userId")
        report_id = payload.get("reportId")
        sources = [
            ("capability_profile", self._source_id(payload.get("capabilityProfile")), payload.get("capabilityProfile") or {}),
            ("career_data", self._source_id(payload.get("careerData")), payload.get("careerData") or {}),
            ("resume_analysis", self._source_id(payload.get("resumeAnalysis")), payload.get("resumeAnalysis") or {}),
            ("match_details", report_id, payload.get("matchDetails") or {}),
            ("action_plan", report_id, payload.get("actionPlan") or {}),
            ("development_path", report_id, payload.get("developmentPath") or {}),
        ]
        chunks: list[Chunk] = []
        for source_type, source_id, value in sources:
            if not value:
                continue
            text = self._flatten(value)
            for idx, part in enumerate(self._recursive_chunk(text)):
                chunks.append(
                    Chunk(
                        id=f"{source_type}:{report_id}:chunk:{idx}",
                        text=part,
                        metadata={
                            "userId": user_id,
                            "documentType": source_type,
                            "documentId": source_id,
                            "reportId": report_id,
                            "sourceType": source_type,
                            "section": source_type,
                            "visibility": "private",
                            "chunkIndex": idx,
                        },
                    )
                )
            summary = self._summarize(text)
            if summary:
                chunks.append(
                    Chunk(
                        id=f"{source_type}:{report_id}:summary:0",
                        text=summary,
                        metadata={
                            "userId": user_id,
                            "documentType": source_type,
                            "documentId": source_id,
                            "reportId": report_id,
                            "sourceType": source_type,
                            "section": "summary",
                            "visibility": "private",
                            "chunkIndex": -1,
                        },
                    )
                )
        return chunks

    def _flatten(self, value: Any) -> str:
        if value is None:
            return ""
        if isinstance(value, dict):
            return " ".join(f"{key}: {self._flatten(item)}" for key, item in value.items())
        if isinstance(value, list):
            return " ".join(self._flatten(item) for item in value)
        return str(value)

    def _recursive_chunk(self, text: str, max_chars: int = 260) -> list[str]:
        text = " ".join(text.split())
        if not text:
            return []
        if len(text) <= max_chars:
            return [text]

        chunks: list[str] = []
        for paragraph in re.split(r"[\n。；;]+", text):
            paragraph = paragraph.strip()
            if not paragraph:
                continue
            if len(paragraph) <= max_chars:
                chunks.append(paragraph)
                continue
            sentences = re.split(r"[，,、\s]+", paragraph)
            current = ""
            for sentence in sentences:
                if not sentence:
                    continue
                candidate = f"{current} {sentence}".strip()
                if len(candidate) > max_chars and current:
                    chunks.append(current)
                    current = sentence
                else:
                    current = candidate
            if current:
                chunks.append(current)
        return chunks

    def _summarize(self, text: str, max_chars: int = 180) -> str:
        return " ".join(text.split())[:max_chars]

    def _apply_filters(self, chunks: list[Chunk], filters: dict[str, Any]) -> list[Chunk]:
        user_id = filters.get("userId")
        document_types = set(filters.get("documentTypes") or [])
        visibility = filters.get("visibility")
        result = []
        for chunk in chunks:
            if user_id is not None and str(chunk.metadata.get("userId")) != str(user_id):
                continue
            if document_types and chunk.metadata.get("documentType") not in document_types:
                continue
            if visibility and chunk.metadata.get("visibility") != visibility:
                continue
            result.append(chunk)
        return result

    def _expand_queries(self, target_job: str, payload: dict[str, Any]) -> list[str]:
        capability = payload.get("capabilityProfile") or {}
        career = payload.get("careerData") or {}
        resume = payload.get("resumeAnalysis") or {}
        return [
            target_job,
            f"{target_job} skills gap",
            f"{target_job} resume evidence {self._flatten(resume.get('highlights'))}",
            f"{target_job} requirements {self._flatten(career.get('jobProfile'))} {self._flatten(capability.get('professionalSkills'))}",
        ]

    def _hybrid_retrieve(self, queries: list[str], chunks: list[Chunk]) -> list[tuple[Chunk, float]]:
        if not chunks:
            return []
        rank_inputs: list[list[tuple[Chunk, float]]] = []
        for query in queries:
            rank_inputs.append(self._normalize(self._rank_bm25(query, chunks)))
            rank_inputs.append(self._normalize(self._rank_hash_embedding(query, chunks)))
        return self._rerank(queries, self._rrf(rank_inputs))

    def _rank_bm25(self, query: str, chunks: list[Chunk]) -> list[tuple[Chunk, float]]:
        query_terms = self._tokens(query)
        if not query_terms:
            return []
        doc_terms = [self._tokens(chunk.text) for chunk in chunks]
        df: dict[str, int] = defaultdict(int)
        for terms in doc_terms:
            for term in set(terms):
                df[term] += 1
        avgdl = sum(len(terms) for terms in doc_terms) / max(len(doc_terms), 1)
        scored = []
        for chunk, terms in zip(chunks, doc_terms):
            counts = Counter(terms)
            score = 0.0
            for term in query_terms:
                if counts[term] == 0:
                    continue
                idf = math.log(1 + (len(chunks) - df[term] + 0.5) / (df[term] + 0.5))
                denom = counts[term] + 1.5 * (1 - 0.75 + 0.75 * len(terms) / max(avgdl, 1))
                score += idf * counts[term] * 2.5 / denom
            if score > 0:
                scored.append((chunk, score))
        return sorted(scored, key=lambda item: item[1], reverse=True)

    def _rank_hash_embedding(self, query: str, chunks: list[Chunk]) -> list[tuple[Chunk, float]]:
        query_vector = self._hash_vector(query)
        scored = []
        for chunk in chunks:
            score = self._cosine(query_vector, self._hash_vector(chunk.text))
            if score > 0:
                scored.append((chunk, score))
        return sorted(scored, key=lambda item: item[1], reverse=True)

    def _hash_vector(self, text: str, dims: int = 32) -> list[float]:
        vector = [0.0] * dims
        for token in self._tokens(text):
            digest = hashlib.sha256(token.encode("utf-8")).digest()
            idx = digest[0] % dims
            sign = 1 if digest[1] % 2 == 0 else -1
            vector[idx] += sign
        return vector

    def _cosine(self, left: list[float], right: list[float]) -> float:
        dot = sum(a * b for a, b in zip(left, right))
        left_norm = math.sqrt(sum(a * a for a in left))
        right_norm = math.sqrt(sum(b * b for b in right))
        if left_norm == 0 or right_norm == 0:
            return 0.0
        return dot / (left_norm * right_norm)

    def _normalize(self, ranked: list[tuple[Chunk, float]]) -> list[tuple[Chunk, float]]:
        if not ranked:
            return []
        values = [score for _, score in ranked]
        low, high = min(values), max(values)
        if high == low:
            return [(chunk, 1.0) for chunk, _ in ranked]
        return [(chunk, (score - low) / (high - low)) for chunk, score in ranked]

    def _rrf(self, ranked_lists: list[list[tuple[Chunk, float]]], k: int = 60) -> list[tuple[Chunk, float]]:
        scores: dict[str, float] = defaultdict(float)
        chunks_by_id: dict[str, Chunk] = {}
        for ranked in ranked_lists:
            for rank, (chunk, _) in enumerate(ranked, start=1):
                scores[chunk.id] += 1.0 / (k + rank)
                chunks_by_id[chunk.id] = chunk
        return sorted(((chunks_by_id[cid], score) for cid, score in scores.items()), key=lambda item: item[1], reverse=True)

    def _rerank(self, queries: list[str], fused: list[tuple[Chunk, float]]) -> list[tuple[Chunk, float]]:
        query_terms = set(self._tokens(" ".join(queries)))
        reranked = []
        for chunk, score in fused:
            overlap = len(query_terms.intersection(self._tokens(chunk.text)))
            reranked.append((chunk, score + overlap * 0.01))
        return sorted(reranked, key=lambda item: item[1], reverse=True)

    def _to_evidence_ref(self, chunk: Chunk, score: float) -> dict[str, Any]:
        return {
            "id": chunk.id,
            "sourceType": chunk.metadata.get("sourceType"),
            "title": f"{chunk.metadata.get('documentType')} {chunk.metadata.get('section')}",
            "score": round(float(score), 4),
            "snippet": self._redact_sensitive(chunk.text)[:160],
            "metadata": {
                "documentType": chunk.metadata.get("documentType"),
                "documentId": chunk.metadata.get("documentId"),
                "reportId": chunk.metadata.get("reportId"),
                "section": chunk.metadata.get("section"),
                "chunkIndex": chunk.metadata.get("chunkIndex"),
            },
        }

    def _source_id(self, value: Any) -> Any:
        if isinstance(value, dict):
            return value.get("id") or value.get("documentId") or value.get("jobId")
        return None

    def _redact_sensitive(self, text: str) -> str:
        redacted = EMAIL_RE.sub("[REDACTED_EMAIL]", text)
        redacted = PHONE_RE.sub("[REDACTED_PHONE]", redacted)
        redacted = ID_CARD_RE.sub("[REDACTED_ID]", redacted)
        return redacted

    def _build_suggestions(self, target_job: str, payload: dict[str, Any], evidence: list[dict[str, Any]]) -> str:
        match = payload.get("matchDetails") or {}
        score = match.get("overall")
        focus = "project evidence and skill gaps"
        if evidence:
            focus = ", ".join({str(item.get("sourceType")) for item in evidence[:2]})
        if score is None:
            return f"For {target_job}, quantify outcomes and update the action plan around {focus}."
        return f"For {target_job}, the current match score is about {score}. Prioritize {focus}, then turn the strongest evidence into measurable resume bullets."

    def _tokens(self, text: str) -> list[str]:
        return [token.lower() for token in TOKEN_RE.findall(text or "")]
