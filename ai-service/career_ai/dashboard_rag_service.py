from __future__ import annotations

import hashlib
import math
import re
from collections import Counter, defaultdict
from dataclasses import dataclass
from typing import Any


RRF_K = 60
SENSITIVE_PROFILE_PATTERNS = [
    re.compile(r"[\w.+-]+@[\w-]+(?:\.[\w-]+)+", re.IGNORECASE),
    re.compile(r"(?<!\d)(?:\+?86[- ]?)?1[3-9]\d{9}(?!\d)"),
    re.compile(r"(?:token|secret|api[_-]?key)\s*[:=]\s*\S+", re.IGNORECASE),
    re.compile(r"(?=.*[A-Za-z])(?=.*\d)[A-Za-z0-9_-]{24,}"),
]


@dataclass
class RagRecord:
    record_id: str
    text: str
    summary: str
    metadata: dict[str, Any]
    embedding: list[float]

    @property
    def chunk_id(self) -> str:
        return self.record_id


def recursive_chunk(text: str, max_chars: int = 520, overlap: int = 60) -> list[str]:
    cleaned = re.sub(r"[ \t]+", " ", str(text or "").replace("\r\n", "\n")).strip()
    if not cleaned:
        return []
    if len(cleaned) <= max_chars:
        return [cleaned]

    separators = ["\n\n", "\n", "。", "；", ";", ". ", "，", ",", " "]

    def split_segment(segment: str, sep_index: int) -> list[str]:
        if len(segment) <= max_chars:
            return [segment.strip()]
        if sep_index >= len(separators):
            chunks: list[str] = []
            step = max(1, max_chars - overlap)
            for start in range(0, len(segment), step):
                chunk = segment[start : start + max_chars].strip()
                if chunk:
                    chunks.append(chunk)
            return chunks

        separator = separators[sep_index]
        if separator not in segment:
            return split_segment(segment, sep_index + 1)

        parts = [part.strip() for part in segment.split(separator) if part.strip()]
        chunks: list[str] = []
        current = ""
        joiner = separator if separator.strip() else " "
        for part in parts:
            candidate = f"{current}{joiner}{part}".strip() if current else part
            if len(candidate) <= max_chars:
                current = candidate
                continue
            if current:
                chunks.extend(split_segment(current, sep_index + 1))
            current = part
        if current:
            chunks.extend(split_segment(current, sep_index + 1))
        return chunks

    return split_segment(cleaned, 0)


def build_summary_index(payload: dict[str, Any]) -> list[RagRecord]:
    filters = _as_dict(payload.get("filters"))
    language = _text(filters.get("language")) or "zh-CN"
    user_id = payload.get("user_id")
    records: list[RagRecord] = []

    resume_profile = _as_dict(payload.get("resume_profile"))
    resume_doc_id = payload.get("resume_analysis_id")
    resume_vector_id = _text(payload.get("resume_vector_store_id")) or "resume"
    target_role = _safe_profile_text(resume_profile.get("target_role"))
    profile_skills = _skills_from_profile(resume_profile)
    experience_years = _safe_experience_years(resume_profile.get("experience_years"))
    _append_document_records(
        records,
        document_key=resume_vector_id,
        document_type="resume",
        document_id=resume_doc_id,
        job_id=None,
        text_by_section={
            "resume_profile": "\n".join(
                [
                    f"目标岗位: {target_role}",
                    f"技能: {'、'.join(profile_skills)}",
                    f"经验年限: {_text(experience_years)}",
                ]
            ),
            "resume_content": _text(payload.get("resume_content")),
        },
        user_id=user_id,
        language=language,
        visibility_scope="user_private",
    )

    for job in _as_list(payload.get("job_candidates")):
        if not isinstance(job, dict):
            continue
        job_id = job.get("job_id")
        _append_document_records(
            records,
            document_key=f"job_{job_id}",
            document_type="job",
            document_id=job_id,
            job_id=job_id,
            text_by_section={
                "job_requirement": _job_requirement_text(job),
                "job_description": _job_description_text(job),
            },
            user_id=user_id,
            language=language,
            visibility_scope="public_job",
            extra_metadata={
                "job_name": job.get("job_name"),
                "job_level": job.get("job_level"),
            },
        )
    return records


def _append_document_records(
    records: list[RagRecord],
    *,
    document_key: str,
    document_type: str,
    document_id: Any,
    job_id: Any,
    text_by_section: dict[str, str],
    user_id: Any,
    language: str,
    visibility_scope: str,
    extra_metadata: dict[str, Any] | None = None,
) -> None:
    cleaned_sections = {
        section: text
        for section, text in text_by_section.items()
        if _text(text)
    }
    if not cleaned_sections:
        return

    base_metadata = {
        "user_id": user_id,
        "document_type": document_type,
        "document_id": document_id,
        "job_id": job_id,
        "language": language,
        "visibility_scope": visibility_scope,
    }
    if extra_metadata:
        base_metadata.update(extra_metadata)

    document_text = "\n".join(cleaned_sections.values())
    document_record_id = f"{document_key}:document"
    _append_record(
        records,
        document_record_id,
        document_text,
        metadata = {
            **base_metadata,
            "summary_level": "document",
            "record_id": document_record_id,
            "parent_id": None,
            "section": "document",
        },
    )

    chunk_index = 0
    for section, section_text in cleaned_sections.items():
        section_record_id = f"{document_key}:section:{section}"
        _append_record(
            records,
            section_record_id,
            section_text,
            metadata={
                **base_metadata,
                "summary_level": "section",
                "record_id": section_record_id,
                "parent_id": document_record_id,
                "section": section,
            },
        )
        for chunk in recursive_chunk(section_text):
            chunk_record_id = f"{document_key}:chunk:{chunk_index}"
            _append_record(
                records,
                chunk_record_id,
                chunk,
                metadata={
                    **base_metadata,
                    "summary_level": "chunk",
                    "record_id": chunk_record_id,
                    "parent_id": section_record_id,
                    "section": section,
                },
            )
            chunk_index += 1


def _append_record(records: list[RagRecord], record_id: str, text: str, metadata: dict[str, Any]) -> None:
    summary = _summary(text)
    records.append(RagRecord(record_id, text, summary, metadata, _hash_embedding(f"{summary}\n{text}")))


def generate_multi_queries(payload: dict[str, Any]) -> list[str]:
    profile = _as_dict(payload.get("resume_profile"))
    target_role = _safe_profile_text(profile.get("target_role"))
    skills = _skills_from_profile(profile)
    skill_query = " ".join(skills[:8])
    evidence_focus = " ".join(item for item in [target_role, skill_query, "项目经历 实习 作品集"] if item)
    queries = [
        f"{target_role} 目标岗位 匹配",
        f"{skill_query} 技能差距 岗位要求",
        f"{evidence_focus} JD 证据",
        f"{target_role or skill_query} 职业画像 能力画像",
    ]
    return [query.strip() for query in queries if query and query.strip()]


def reciprocal_rank_fusion(rank_lists: list[list[tuple[str, float]]], k: int = RRF_K) -> dict[str, float]:
    fused: defaultdict[str, float] = defaultdict(float)
    for rank_list in rank_lists:
        for rank, (chunk_id, _) in enumerate(rank_list, start=1):
            fused[chunk_id] += 1.0 / (k + rank)
    return dict(fused)


def match_target_job(payload: dict[str, Any]) -> dict[str, Any]:
    validate_payload(payload)
    filters = _as_dict(payload.get("filters"))
    top_k = max(1, min(20, _safe_int(payload.get("top_k"), 5)))
    user_id = payload.get("user_id")

    records = [
        record
        for record in build_summary_index(payload)
        if _metadata_allowed(record, filters, user_id)
    ]
    queries = generate_multi_queries(payload)
    rank_lists: list[list[tuple[str, float]]] = []
    for query in queries:
        rank_lists.append(_bm25_scores(query, records)[:top_k])
        rank_lists.append(_embedding_scores(query, records)[:top_k])
    fused = reciprocal_rank_fusion(rank_lists)
    ranked_jobs, evidence_by_job = _deterministic_job_scores(payload, records, fused)

    filters_applied = {
        "user_id": user_id,
        "document_type": filters.get("document_type", ["resume", "job", "jd"]),
        "visibility_scope": filters.get("visibility_scope", "user_or_public"),
        "language": filters.get("language", "zh-CN"),
    }
    diagnostics = {
        "query_variants": queries,
        "filters_applied": filters_applied,
        "fusion_method": "rrf",
        "reranker": "deterministic-fallback",
        "candidate_count": len(records),
    }
    if not ranked_jobs:
        diagnostics["selected_evidence_ids"] = []
        return {
            "code": 0,
            "msg": "NO_MATCH",
            "data": {"retrieval": diagnostics, "evidence_refs": []},
        }

    best = ranked_jobs[0]
    job = best["job"]
    evidence_refs = evidence_by_job.get(job.get("job_id"), [])
    if not evidence_refs:
        evidence_refs = _fallback_chunk_evidence(records, job.get("job_id"))
    if not evidence_refs:
        diagnostics["selected_evidence_ids"] = []
        return {
            "code": 0,
            "msg": "NO_MATCH",
            "data": {"retrieval": diagnostics, "evidence_refs": []},
        }
    diagnostics["selected_evidence_ids"] = [item["source_id"] for item in evidence_refs]
    return {
        "code": 1,
        "msg": "success",
        "data": {
            "matched_job": {
                "job_id": job.get("job_id"),
                "job_name": job.get("job_name"),
                "job_level": job.get("job_level"),
                "score": best["score"],
            },
            "retrieval": diagnostics,
            "evidence_refs": evidence_refs,
        },
    }


def validate_payload(payload: dict[str, Any]) -> None:
    required = ["request_id", "user_id", "resume_analysis_id", "resume_vector_store_id"]
    missing = [field for field in required if payload.get(field) in (None, "")]
    if missing:
        raise ValueError("missing required fields: " + ", ".join(missing))
    if not isinstance(payload.get("resume_profile"), dict):
        raise ValueError("resume_profile must be an object")
    if not _text(payload.get("resume_content")):
        raise ValueError("resume_content is required")
    if not isinstance(payload.get("job_candidates"), list) or not payload.get("job_candidates"):
        raise ValueError("job_candidates must be a non-empty array")


def _metadata_allowed(record: RagRecord, filters: dict[str, Any], user_id: Any) -> bool:
    metadata = record.metadata
    document_types = filters.get("document_type")
    if isinstance(document_types, list) and metadata.get("document_type") not in set(document_types):
        return False
    language = _text(filters.get("language"))
    if language and metadata.get("language") != language:
        return False
    scope = _text(filters.get("visibility_scope"))
    if scope == "user_or_public":
        return metadata.get("visibility_scope") == "public_job" or metadata.get("user_id") == user_id
    if scope and metadata.get("visibility_scope") != scope:
        return False
    return True


def _bm25_scores(query: str, records: list[RagRecord]) -> list[tuple[str, float]]:
    query_tokens = tokenize(query)
    if not query_tokens or not records:
        return []
    docs = [tokenize(f"{record.summary}\n{record.text}") for record in records]
    avg_len = sum(len(tokens) for tokens in docs) / max(len(docs), 1)
    doc_freq: Counter[str] = Counter()
    for tokens in docs:
        doc_freq.update(set(tokens))

    scored: list[tuple[str, float]] = []
    for record, tokens in zip(records, docs):
        counts = Counter(tokens)
        doc_len = len(tokens) or 1
        score = 0.0
        for token in query_tokens:
            df = doc_freq.get(token, 0)
            if df == 0:
                continue
            idf = math.log(1 + (len(records) - df + 0.5) / (df + 0.5))
            tf = counts[token]
            score += idf * (tf * 2.5 / (tf + 1.5 * (1 - 0.75 + 0.75 * doc_len / max(avg_len, 1))))
        if score > 0:
            scored.append((record.chunk_id, score))
    return sorted(scored, key=lambda item: item[1], reverse=True)


def _embedding_scores(query: str, records: list[RagRecord]) -> list[tuple[str, float]]:
    query_vector = _hash_embedding(query)
    scored = [(record.chunk_id, _cosine(query_vector, record.embedding)) for record in records]
    return sorted((item for item in scored if item[1] > 0), key=lambda item: item[1], reverse=True)


def _deterministic_job_scores(
    payload: dict[str, Any],
    records: list[RagRecord],
    fused: dict[str, float],
) -> tuple[list[dict[str, Any]], dict[Any, list[dict[str, Any]]]]:
    records_by_id = {record.chunk_id: record for record in records}
    score_by_job: defaultdict[Any, float] = defaultdict(float)
    evidence_by_job: defaultdict[Any, list[dict[str, Any]]] = defaultdict(list)
    for chunk_id, score in sorted(fused.items(), key=lambda item: item[1], reverse=True):
        record = records_by_id.get(chunk_id)
        if record is None:
            continue
        job_id = record.metadata.get("job_id")
        if job_id is None:
            continue
        score_by_job[job_id] += score
        if record.metadata.get("summary_level") == "chunk" and len(evidence_by_job[job_id]) < 3:
            evidence_by_job[job_id].append(
                {
                    "source_type": "job_chunk",
                    "source_id": chunk_id,
                    "section": record.metadata.get("section"),
                    "score": round(score, 4),
                }
            )

    resume_profile = _as_dict(payload.get("resume_profile"))
    resume_skills = _skills_from_profile(resume_profile)
    resume_text = _text(payload.get("resume_content")).lower()
    target_role = _safe_profile_text(resume_profile.get("target_role")).lower()
    ranked: list[dict[str, Any]] = []
    for job in _as_list(payload.get("job_candidates")):
        if not isinstance(job, dict):
            continue
        job_id = job.get("job_id")
        retrieval_score = score_by_job.get(job_id, 0.0)
        required_skills = _required_skills(job)
        skill_score = max(
            _skill_overlap(resume_skills, required_skills),
            _required_skill_mentions(resume_text, required_skills),
        )
        name_score = 1.0 if _text(job.get("job_name")).lower() in resume_text else 0.0
        target_score = 1.0 if target_role and target_role in _job_text(job).lower() else 0.0
        combined = min(0.99, retrieval_score * 8 + skill_score * 0.35 + name_score * 0.08)
        if retrieval_score <= 0:
            continue
        if skill_score <= 0 and name_score <= 0 and target_score <= 0:
            continue
        ranked.append(
            {
                "job": job,
                "score": round(max(0.35, combined), 4),
                "retrieval_score": round(retrieval_score, 4),
                "skill_overlap": round(skill_score, 4),
            }
        )
    ranked.sort(key=lambda item: (item["score"], item["retrieval_score"], item["skill_overlap"]), reverse=True)
    return ranked, evidence_by_job


def _fallback_chunk_evidence(records: list[RagRecord], job_id: Any) -> list[dict[str, Any]]:
    evidence: list[dict[str, Any]] = []
    for record in records:
        if record.metadata.get("job_id") != job_id or record.metadata.get("summary_level") != "chunk":
            continue
        evidence.append(
            {
                "source_type": "job_chunk",
                "source_id": record.record_id,
                "section": record.metadata.get("section"),
                "score": 0.0,
            }
        )
        if len(evidence) >= 3:
            break
    return evidence


def _required_skill_mentions(text: str, required_skills: list[str]) -> float:
    if not required_skills:
        return 0.0
    matched = 0
    for skill in required_skills:
        candidate = skill.lower()
        if candidate and candidate in text:
            matched += 1
    return matched / len(required_skills)


def _skill_overlap(resume_skills: list[str], required_skills: list[str]) -> float:
    if not required_skills:
        return 0.0
    lowered = [skill.lower() for skill in resume_skills]
    matched = 0
    for skill in required_skills:
        candidate = skill.lower()
        if any(candidate in item or item in candidate for item in lowered):
            matched += 1
    return matched / len(required_skills)


def _job_text(job: dict[str, Any]) -> str:
    return "\n".join([_job_requirement_text(job), _job_description_text(job)])


def _job_requirement_text(job: dict[str, Any]) -> str:
    return "\n".join(
        [
            f"岗位名称: {_text(job.get('job_name'))}",
            f"岗位编码: {_text(job.get('job_category_code'))}",
            f"岗位级别: {_text(job.get('job_level_name')) or _text(job.get('job_level'))}",
            f"技能要求: {'、'.join(_required_skills(job))}",
        ]
    )


def _job_description_text(job: dict[str, Any]) -> str:
    profile = _as_dict(job.get("job_profile"))
    return "\n".join(
        [
            f"岗位描述: {_text(job.get('job_description'))}",
            f"岗位画像: {' '.join(_text(value) for value in profile.values())}",
        ]
    )


def _required_skills(job: dict[str, Any]) -> list[str]:
    skills = job.get("required_skills")
    if isinstance(skills, list):
        return [_text(skill) for skill in skills if _text(skill)]
    if isinstance(skills, str):
        return [item.strip() for item in re.split(r"[,，、]", skills) if item.strip()]
    return []


def _skills_from_profile(profile: dict[str, Any]) -> list[str]:
    skills = profile.get("skills")
    if isinstance(skills, list):
        return [
            skill
            for skill in (_safe_profile_text(item) for item in skills)
            if skill
        ]
    return []


def _safe_profile_text(value: Any) -> str:
    if not _is_scalar_profile_value(value):
        return ""
    text = _text(value)
    if not text:
        return ""
    if any(pattern.search(text) for pattern in SENSITIVE_PROFILE_PATTERNS):
        return ""
    return text


def _is_scalar_profile_value(value: Any) -> bool:
    return isinstance(value, (str, int, float, bool))


def _safe_experience_years(value: Any) -> int | float | None:
    if isinstance(value, bool):
        return None
    if isinstance(value, (int, float)):
        return value
    return None


def tokenize(text: str) -> list[str]:
    return re.findall(r"[a-z0-9+#.]+|[\u4e00-\u9fff]", str(text or "").lower())


def _summary(text: str, max_chars: int = 180) -> str:
    compact = re.sub(r"\s+", " ", text).strip()
    return compact[:max_chars]


def _hash_embedding(text: str, dimensions: int = 64) -> list[float]:
    vector = [0.0] * dimensions
    for token in tokenize(text):
        digest = hashlib.sha256(token.encode("utf-8")).digest()
        index = int.from_bytes(digest[:2], "big") % dimensions
        sign = 1.0 if digest[2] % 2 == 0 else -1.0
        vector[index] += sign
    norm = math.sqrt(sum(item * item for item in vector)) or 1.0
    return [item / norm for item in vector]


def _cosine(left: list[float], right: list[float]) -> float:
    return sum(a * b for a, b in zip(left, right))


def _safe_int(value: Any, default: int = 0) -> int:
    try:
        return int(value)
    except (TypeError, ValueError):
        return default


def _as_dict(value: Any) -> dict[str, Any]:
    return value if isinstance(value, dict) else {}


def _as_list(value: Any) -> list[Any]:
    return value if isinstance(value, list) else []


def _text(value: Any) -> str:
    return str(value).strip() if value is not None else ""
