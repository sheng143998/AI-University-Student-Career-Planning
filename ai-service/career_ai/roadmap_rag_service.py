from __future__ import annotations

import math
import re
from collections import Counter, defaultdict
from typing import Any


RRF_K = 60
SENSITIVE_PATTERNS = [
    re.compile(r"1[3-9]\d{9}"),
    re.compile(r"[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}"),
    re.compile(r"(?i)(api[_-]?key|secret|token)\s*[:=]\s*[A-Za-z0-9_\-]{8,}"),
    re.compile(r"(?i)\bsk[-_][A-Za-z0-9_\-]{8,}\b"),
]


def _as_list(value: Any) -> list[Any]:
    return value if isinstance(value, list) else []


def _as_dict(value: Any) -> dict[str, Any]:
    return value if isinstance(value, dict) else {}


def _text(value: Any) -> str:
    return str(value).strip() if value is not None else ""


def _tokens(text: str) -> list[str]:
    normalized = text.lower()
    words = re.findall(r"[a-z0-9+#.]+|[\u4e00-\u9fff]", normalized)
    return [word for word in words if word.strip()]


def _safe_float(value: Any, default: float = 0.0) -> float:
    try:
        return float(value)
    except (TypeError, ValueError):
        return default


def _safe_int(value: Any, default: int = 0) -> int:
    try:
        return int(value)
    except (TypeError, ValueError):
        return default


def _redact_sensitive(text: str) -> str:
    redacted = text
    for pattern in SENSITIVE_PATTERNS:
        redacted = pattern.sub("[REDACTED]", redacted)
    return redacted


def recursive_chunk_text(text: str, max_chars: int = 420, overlap: int = 60) -> list[str]:
    text = re.sub(r"\s+", " ", _redact_sensitive(text)).strip()
    if not text:
        return []
    if len(text) <= max_chars:
        return [text]

    separators = ["\n\n", "\n", "。", "；", ";", "，", ",", " "]

    def split_recursive(segment: str, sep_index: int) -> list[str]:
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
            return split_recursive(segment, sep_index + 1)

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
                chunks.extend(split_recursive(current, sep_index + 1))
            current = part
        if current:
            chunks.extend(split_recursive(current, sep_index + 1))
        return chunks

    return split_recursive(text, 0)


def _summary(text: str, max_chars: int = 180) -> str:
    compact = re.sub(r"\s+", " ", _redact_sensitive(text)).strip()
    return compact[:max_chars]


def _base_category(job: dict[str, Any]) -> str:
    explicit = _text(job.get("baseCategoryCode"))
    if explicit:
        return explicit
    code = _text(job.get("categoryCode"))
    return re.sub(r"_(INTERNSHIP|JUNIOR|MID|SENIOR)$", "", code)


def _required_skills(job: dict[str, Any]) -> list[str]:
    return [_text(skill) for skill in _as_list(job.get("requiredSkills")) if _text(skill)]


def _salary_range(job: dict[str, Any]) -> str:
    salary = _text(job.get("salaryRange"))
    return salary if salary else "negotiable"


def _job_content(job: dict[str, Any]) -> str:
    skills = ", ".join(_required_skills(job))
    return "\n".join(
        [
            f"Job: {_text(job.get('name'))}",
            f"Category: {_base_category(job)}",
            f"Level: {_text(job.get('levelName')) or _text(job.get('level'))}",
            f"Skills: {skills}",
            f"Salary: {_salary_range(job)}",
            f"Description: {_text(job.get('description'))}",
            f"Profile: {_text(job.get('profile'))}",
        ]
    )


def _resume_summary_content(payload: dict[str, Any]) -> str:
    resume_data = _as_dict(payload.get("resumeData"))
    allowed = {
        "target_role": _text(resume_data.get("target_role")),
        "current_role": _text(resume_data.get("current_role")),
        "experience_years": _text(resume_data.get("experience_years")),
        "match_score": _text(resume_data.get("match_score")),
        "skills": ", ".join(
            _redact_sensitive(_text(skill)) for skill in _as_list(resume_data.get("skills")) if _text(skill)
        ),
    }
    user_skills = ", ".join(
        _redact_sensitive(_text(skill)) for skill in _as_list(payload.get("userSkills")) if _text(skill)
    )
    return "\n".join(
        [
            f"Current job: {_text(payload.get('currentJob'))}",
            f"Resume current role: {allowed['current_role']}",
            f"Target role: {allowed['target_role']}",
            f"Experience years: {allowed['experience_years']}",
            f"Match score: {allowed['match_score']}",
            f"Skills: {allowed['skills'] or user_skills}",
        ]
    )


def _jd_summary_items(payload: dict[str, Any]) -> list[Any]:
    for key in ("jdSummaries", "jds", "jobDescriptions"):
        items = _as_list(payload.get(key))
        if items:
            return items
    return []


def _jd_content(item: Any) -> tuple[str, dict[str, Any]]:
    if isinstance(item, str):
        return item, {}
    jd = _as_dict(item)
    skills = ", ".join(_text(skill) for skill in _as_list(jd.get("requiredSkills")) if _text(skill))
    content = "\n".join(
        [
            f"JD title: {_text(jd.get('title')) or _text(jd.get('jobName'))}",
            f"JD skills: {skills}",
            f"JD requirements: {_text(jd.get('requirements'))}",
            f"JD description: {_text(jd.get('description')) or _text(jd.get('content'))}",
        ]
    )
    return content, jd


def build_summary_index(payload: dict[str, Any]) -> list[dict[str, Any]]:
    user_id = payload.get("userId")
    records: list[dict[str, Any]] = []

    for job in _as_list(payload.get("jobs")):
        if not isinstance(job, dict):
            continue
        content = _job_content(job)
        chunks = recursive_chunk_text(content)
        for index, chunk in enumerate(chunks):
            job_id = job.get("id")
            section = "summary" if index == 0 else f"chunk_{index}"
            records.append(
                {
                    "chunkId": f"job:{job_id}:{section}",
                    "text": _summary(chunk),
                    "rawText": chunk,
                    "metadata": {
                        "userId": user_id,
                        "documentType": "job",
                        "jobId": job_id,
                        "categoryCode": _text(job.get("categoryCode")),
                        "baseCategoryCode": _base_category(job),
                        "level": _text(job.get("level")),
                        "section": section,
                        "source": "summary_index",
                        "visibilityScope": "public_job",
                    },
                }
            )

    resume_content = _resume_summary_content(payload)
    for index, chunk in enumerate(recursive_chunk_text(resume_content)):
        records.append(
            {
                "chunkId": f"resume:{user_id}:summary:{index}",
                "text": _summary(chunk),
                "rawText": chunk,
                "metadata": {
                    "userId": user_id,
                    "documentType": "resume_summary",
                    "jobId": None,
                    "categoryCode": "",
                    "baseCategoryCode": "",
                    "level": "",
                    "section": "summary",
                    "source": "summary_index",
                    "visibilityScope": "user_private",
                },
            }
        )

    for jd_index, item in enumerate(_jd_summary_items(payload)):
        content, jd = _jd_content(item)
        for chunk_index, chunk in enumerate(recursive_chunk_text(content)):
            job_id = jd.get("jobId") if jd else None
            section = "summary" if chunk_index == 0 else f"chunk_{chunk_index}"
            records.append(
                {
                    "chunkId": f"jd:{job_id or jd_index}:{section}",
                    "text": _summary(chunk),
                    "rawText": chunk,
                    "metadata": {
                        "userId": user_id,
                        "documentType": "jd_summary",
                        "jobId": job_id,
                        "categoryCode": _text(jd.get("categoryCode")) if jd else "",
                        "baseCategoryCode": _text(jd.get("baseCategoryCode")) if jd else "",
                        "level": _text(jd.get("level")) if jd else "",
                        "section": section,
                        "source": "summary_index",
                        "visibilityScope": "jd_context",
                    },
                }
            )

    return records


def generate_multi_queries(payload: dict[str, Any]) -> list[str]:
    resume_data = _as_dict(payload.get("resumeData"))
    current_job = _redact_sensitive(_text(payload.get("currentJob")))
    target_role = _redact_sensitive(_text(resume_data.get("target_role")))
    skills = [_redact_sensitive(_text(skill)) for skill in _as_list(payload.get("userSkills")) if _text(skill)]
    if not skills:
        skills = [_redact_sensitive(_text(skill)) for skill in _as_list(resume_data.get("skills")) if _text(skill)]
    skill_query = " ".join(skills[:6])

    queries = [
        f"{current_job} transition learning path",
        f"{current_job} skill gap recommended role",
        f"{target_role or current_job} JD requirements evidence",
        f"{skill_query} transferable skills career roadmap",
        f"{current_job} lateral transition path",
    ]
    return [query.strip() for query in queries if query.strip()]


def _bm25_scores(query: str, records: list[dict[str, Any]]) -> list[tuple[str, float]]:
    query_tokens = _tokens(query)
    if not query_tokens or not records:
        return []

    doc_tokens = [_tokens(_text(record.get("rawText")) + " " + _text(record.get("text"))) for record in records]
    avg_len = sum(len(tokens) for tokens in doc_tokens) / max(len(doc_tokens), 1)
    doc_freq: Counter[str] = Counter()
    for tokens in doc_tokens:
        doc_freq.update(set(tokens))

    scores: list[tuple[str, float]] = []
    for record, tokens in zip(records, doc_tokens):
        counts = Counter(tokens)
        doc_len = len(tokens) or 1
        score = 0.0
        for token in query_tokens:
            df = doc_freq.get(token, 0)
            if df == 0:
                continue
            idf = math.log(1 + (len(records) - df + 0.5) / (df + 0.5))
            tf = counts[token]
            denominator = tf + 1.5 * (1 - 0.75 + 0.75 * doc_len / max(avg_len, 1))
            score += idf * (tf * 2.5 / denominator)
        if score > 0:
            scores.append((_text(record.get("chunkId")), score))
    return sorted(scores, key=lambda item: item[1], reverse=True)


def _char_vector(text: str) -> Counter[str]:
    normalized = re.sub(r"\s+", "", text.lower())
    grams = [normalized[i : i + 2] for i in range(max(len(normalized) - 1, 0))]
    grams.extend(_tokens(text))
    return Counter(grams)


def _cosine(left: Counter[str], right: Counter[str]) -> float:
    if not left or not right:
        return 0.0
    common = set(left) & set(right)
    numerator = sum(left[key] * right[key] for key in common)
    left_norm = math.sqrt(sum(value * value for value in left.values()))
    right_norm = math.sqrt(sum(value * value for value in right.values()))
    if left_norm == 0 or right_norm == 0:
        return 0.0
    return numerator / (left_norm * right_norm)


def _embedding_scores(query: str, records: list[dict[str, Any]]) -> list[tuple[str, float]]:
    query_vector = _char_vector(query)
    scores: list[tuple[str, float]] = []
    for record in records:
        record_vector = _char_vector(_text(record.get("rawText")) + " " + _text(record.get("text")))
        score = _cosine(query_vector, record_vector)
        if score > 0:
            scores.append((_text(record.get("chunkId")), score))
    return sorted(scores, key=lambda item: item[1], reverse=True)


def reciprocal_rank_fusion(rank_lists: list[list[tuple[str, float]]], k: int = RRF_K) -> dict[str, float]:
    fused: defaultdict[str, float] = defaultdict(float)
    for rank_list in rank_lists:
        for rank, (chunk_id, _) in enumerate(rank_list, start=1):
            fused[chunk_id] += 1.0 / (k + rank)
    return dict(fused)


def _infer_current_base(current_job: str, jobs: list[dict[str, Any]]) -> str:
    current_vector = _char_vector(current_job)
    best_base = ""
    best_score = 0.0
    for job in jobs:
        score = _cosine(current_vector, _char_vector(_text(job.get("name"))))
        if score > best_score:
            best_score = score
            best_base = _base_category(job)
    return best_base if best_score >= 0.2 else ""


def _skill_overlap(user_skills: list[str], target_skills: list[str]) -> tuple[list[str], list[str], float]:
    possessed: list[str] = []
    required: list[str] = []
    lowered_user = [skill.lower() for skill in user_skills]
    for skill in target_skills:
        lower_skill = skill.lower()
        matched = any(lower_skill in user_skill or user_skill in lower_skill for user_skill in lowered_user)
        if matched:
            possessed.append(skill)
        else:
            required.append(skill)
    overlap = len(possessed) / max(len(target_skills), 1)
    return possessed, required, overlap


def _default_filters(payload: dict[str, Any]) -> dict[str, Any]:
    retrieval = _as_dict(payload.get("retrieval"))
    filters = dict(_as_dict(retrieval.get("filters")))
    filters.setdefault("excludeSameCategory", True)
    filters.setdefault("documentTypes", ["job", "resume_summary", "jd_summary"])
    return filters


def _diagnostic_filters(filters: dict[str, Any]) -> dict[str, Any]:
    document_types = [
        _redact_sensitive(_text(document_type))
        for document_type in _as_list(filters.get("documentTypes"))
        if _text(document_type)
    ]
    return {
        "excludeSameCategory": bool(filters.get("excludeSameCategory")),
        "documentTypes": document_types,
    }


def _filtered_records(payload: dict[str, Any], records: list[dict[str, Any]], jobs: list[dict[str, Any]]) -> list[dict[str, Any]]:
    filters = _default_filters(payload)
    current_base = _infer_current_base(_text(payload.get("currentJob")), jobs)
    document_types = set(_as_list(filters.get("documentTypes")))
    filtered: list[dict[str, Any]] = []
    for record in records:
        metadata = _as_dict(record.get("metadata"))
        if filters.get("excludeSameCategory") and metadata.get("baseCategoryCode") == current_base:
            continue
        if document_types and metadata.get("documentType") not in document_types:
            continue
        filtered.append(record)
    return filtered


def generate_roadmap_recommendations(payload: dict[str, Any]) -> dict[str, Any]:
    jobs = [job for job in _as_list(payload.get("jobs")) if isinstance(job, dict)]
    records = build_summary_index(payload)
    filtered_records = _filtered_records(payload, records, jobs)
    queries = generate_multi_queries(payload)
    retrieval = _as_dict(payload.get("retrieval"))
    top_k = max(1, _safe_int(retrieval.get("topK"), 10))

    rank_lists: list[list[tuple[str, float]]] = []
    for query in queries:
        rank_lists.append(_bm25_scores(query, filtered_records)[:top_k])
        rank_lists.append(_embedding_scores(query, filtered_records)[:top_k])
    fused = reciprocal_rank_fusion(rank_lists)

    records_by_id = {_text(record.get("chunkId")): record for record in filtered_records}
    score_by_base: defaultdict[str, float] = defaultdict(float)
    evidence_by_base: defaultdict[str, list[dict[str, Any]]] = defaultdict(list)
    for chunk_id, score in sorted(fused.items(), key=lambda item: item[1], reverse=True):
        record = records_by_id.get(chunk_id)
        if not record:
            continue
        metadata = _as_dict(record.get("metadata"))
        base_code = _text(metadata.get("baseCategoryCode"))
        if not base_code:
            continue
        score_by_base[base_code] += score
        if len(evidence_by_base[base_code]) < 3:
            evidence_by_base[base_code].append(
                {
                    "documentType": metadata.get("documentType"),
                    "jobId": metadata.get("jobId"),
                    "chunkId": chunk_id,
                    "score": round(score, 4),
                    "source": metadata.get("source"),
                }
            )

    jobs_by_base: defaultdict[str, list[dict[str, Any]]] = defaultdict(list)
    for job in jobs:
        jobs_by_base[_base_category(job)].append(job)

    filters = _default_filters(payload)
    current_base = _infer_current_base(_text(payload.get("currentJob")), jobs)
    user_skills = [_text(skill) for skill in _as_list(payload.get("userSkills")) if _text(skill)]
    recommendations: list[dict[str, Any]] = []
    for base_code, category_jobs in jobs_by_base.items():
        if filters.get("excludeSameCategory") and base_code == current_base:
            continue
        representative = category_jobs[0]
        target_skills = _required_skills(representative)
        possessed, required, overlap = _skill_overlap(user_skills, target_skills)
        retrieval_score = score_by_base.get(base_code, 0.0)
        if retrieval_score == 0 and overlap == 0:
            continue
        combined = min(0.98, 0.45 + overlap * 0.35 + min(retrieval_score * 4, 0.2))
        difficulty = max(1, min(5, int(round(5 - combined * 3))))
        recommendations.append(
            {
                "targetJobId": representative.get("id"),
                "targetCategoryCode": base_code,
                "targetJobName": _text(representative.get("name")),
                "matchScore": round(combined, 2),
                "transitionDifficulty": difficulty,
                "estimatedMonths": max(6, min(24, 6 + difficulty * 3)),
                "requiredSkills": required[:6],
                "possessedSkills": possessed[:6],
                "aiRecommendationReason": (
                    f"Based on Multi-Query, BM25 plus embedding-style retrieval, and RAG-Fusion, "
                    f"{_text(representative.get('name'))} matches {len(possessed)} existing skills "
                    f"and has {len(required)} key skill gaps."
                ),
                "evidence": evidence_by_base.get(base_code, []),
            }
        )

    recommendations.sort(key=lambda item: item["matchScore"], reverse=True)
    diagnostics = {
        "queries": queries,
        "filters": _diagnostic_filters(filters),
        "fusion": "rrf",
        "reranker": "deterministic-fallback",
        "candidateCount": len(filtered_records),
    }
    return {"lateralPaths": recommendations[:10], "diagnostics": diagnostics}
