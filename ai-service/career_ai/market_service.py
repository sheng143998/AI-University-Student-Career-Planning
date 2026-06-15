from __future__ import annotations

import hashlib
import json
import math
import re
from typing import Any

from rag.chunking import RecursiveChunker
from rag.retrieval import HybridRetriever, expand_queries


EMBEDDING_DIMENSIONS = 1024
LEVEL_NAMES = {
    "INTERNSHIP": "Internship",
    "JUNIOR": "Junior",
    "MID": "Mid",
    "SENIOR": "Senior",
}

SKILL_KEYWORDS = [
    "Python",
    "Java",
    "Spring Boot",
    "Vue",
    "React",
    "TypeScript",
    "JavaScript",
    "RAG",
    "LLM",
    "AI Agent",
    "PyTorch",
    "TensorFlow",
    "Machine Learning",
    "SQL",
    "Redis",
    "Docker",
    "Kubernetes",
    "Linux",
    "Testing",
    "Figma",
    "Product",
]


def generate_market_insight(payload: dict[str, Any]) -> dict[str, Any]:
    job = _job(payload)
    skills = _required_skills(job)
    job_name = _text(job.get("jobName") or job.get("job_name") or "Target role")
    city = _text(payload.get("city") or job.get("city") or "National")
    source_job_count = _int(job.get("sourceJobCount") or job.get("source_job_count"), 100)
    min_salary = _number(job.get("minSalary") or job.get("min_salary"))
    max_salary = _number(job.get("maxSalary") or job.get("max_salary"))
    salary_unit = _text(job.get("salaryUnit") or job.get("salary_unit") or "month").lower()
    demand_level = _demand_level(source_job_count)
    top_skills = skills[:3] or ["core role skills"]

    if min_salary is not None and max_salary is not None:
        salary_value = f"{int(min_salary)}-{int(max_salary)}/{salary_unit}"
    else:
        salary_value = "insufficient sample"

    return {
        "title": f"{job_name} market insight",
        "summary": (
            f"{city} demand for {job_name} is {demand_level} based on about "
            f"{source_job_count} sampled jobs. Candidates with evidence around "
            f"{', '.join(top_skills)} are likely to stand out."
        ),
        "marketSignals": [
            {"label": "Demand level", "value": demand_level, "trend": "UP" if source_job_count >= 50 else "STABLE"},
            {"label": "Sampled jobs", "value": str(source_job_count), "trend": "UP"},
            {"label": "Salary range", "value": salary_value, "trend": "UP" if salary_value != "insufficient sample" else "STABLE"},
            {"label": "Skill change", "value": "FAST" if len(skills) >= 3 else "MEDIUM", "trend": "UP"},
        ],
        "industryTrends": [
            f"{job_name} hiring is increasingly evidence-driven, with project outcomes weighed alongside keywords.",
            "Employers prefer candidates who can translate technical skills into measurable business results.",
            "Hybrid skill sets and continuous learning capacity are strong differentiators.",
            "City, industry, and company-stage differences should shape application strategy and salary expectations.",
        ],
        "suggestedActions": [
            {
                "title": "Strengthen core evidence",
                "desc": f"Build resume and portfolio proof around {', '.join(top_skills)}.",
                "priority": "HIGH",
            },
            {
                "title": "Track JD keywords",
                "desc": "Review high-frequency tools, skills, and business scenarios in target job descriptions.",
                "priority": "MEDIUM",
            },
            {
                "title": "Quantify resume impact",
                "desc": "Rewrite skill statements as actions, responsibilities, and measurable outcomes.",
                "priority": "MEDIUM",
            },
            {
                "title": "Adjust by city",
                "desc": "Tune application strategy and salary expectations with local job samples.",
                "priority": "LOW",
            },
        ],
    }


def generate_soft_skills(payload: dict[str, Any]) -> list[dict[str, Any]]:
    job = _job(payload)
    job_name = _text(job.get("jobName") or job.get("job_name") or "this role")
    scores = _capability_scores(payload, job)

    definitions = [
        (
            "innovation",
            "Innovation",
            "can turn new tools or ideas into practical improvements for role-specific work",
        ),
        (
            "learning",
            "Learning ability",
            "can quickly understand required knowledge and close skill gaps through deliberate practice",
        ),
        (
            "resilience",
            "Resilience",
            "can keep steady delivery under changing requirements, deadlines, and ambiguous tasks",
        ),
        (
            "communication",
            "Communication",
            "can explain technical or business tradeoffs and coordinate with cross-functional collaborators",
        ),
        (
            "internship",
            "Internship readiness",
            "can convert coursework, projects, and internship experience into workplace results",
        ),
    ]

    result: list[dict[str, Any]] = []
    for key, name, description in definitions:
        score = _bounded_score(scores.get(key), default=60)
        result.append(
            {
                "name": name,
                "score": score,
                "description": f"{job_name} requires {name.lower()}: {description}.",
                "evidence": [
                    f"Use real {job_name} tasks to show how this capability changes outcomes.",
                    "Support the claim with projects, internships, coursework, or portfolio artifacts.",
                ],
            }
        )
    return result


def classify_job(payload: dict[str, Any]) -> dict[str, Any]:
    job = _job(payload)
    job_content = _text(payload.get("job_content"))
    merged_text = _merge_text(
        job_content,
        job.get("jobName"),
        job.get("job_name"),
        job.get("jobDescription"),
        job.get("job_description"),
        job.get("jobRequirements"),
        job.get("job_requirements"),
        job.get("jobDetail"),
        job.get("job_detail"),
        job.get("industry"),
    )
    if not merged_text:
        return {"code": 0, "msg": "VALIDATION_ERROR", "data": {"error": "job_content is required"}}

    job_name = _text(job.get("jobName") or job.get("job_name")) or _infer_job_name(merged_text)
    base_code, category_name = _infer_category(merged_text, job_name)
    level = _infer_level(merged_text)
    min_salary, max_salary, salary_unit = _parse_salary(_text(job.get("salaryRange") or job.get("salary_range")) or merged_text)
    required_skills = _infer_skills(merged_text)
    experience_years = _infer_experience_years(merged_text, level)
    category_code = base_code if base_code.endswith("_" + level) else f"{base_code}_{level}"

    return {
        "code": 1,
        "msg": "success",
        "data": {
            "category_code": category_code,
            "category_name": category_name,
            "level": level,
            "level_name": LEVEL_NAMES[level],
            "min_salary": min_salary,
            "max_salary": max_salary,
            "salary_unit": salary_unit,
            "required_experience_years": experience_years,
            "required_skills": required_skills,
            "job_description": _short_text(merged_text, 300),
            "confidence": 0.86 if required_skills else 0.62,
        },
    }


def index_jobs(payload: dict[str, Any]) -> dict[str, Any]:
    jobs = payload.get("jobs")
    if not isinstance(jobs, list):
        return {"code": 0, "msg": "VALIDATION_ERROR", "data": {"error": "jobs must be an array"}}

    records: list[dict[str, Any]] = []
    chunker = RecursiveChunker(chunk_size=600, overlap=80)
    for item in jobs:
        if not isinstance(item, dict):
            continue
        job_id = _int(item.get("job_id") or item.get("jobId"), 0)
        content = _text(item.get("content"))
        if job_id <= 0 or not content:
            continue
        content_hash = hashlib.sha256(content.encode("utf-8")).hexdigest()
        metadata = item.get("metadata") if isinstance(item.get("metadata"), dict) else {}
        safe_metadata = _sanitize_metadata(
            {
                **metadata,
                "job_id": job_id,
                "document_type": metadata.get("document_type") or "jd",
                "source": metadata.get("source") or "recruitment_data",
                "visibility_scope": metadata.get("visibility_scope") or "public",
                "content_hash": content_hash,
            }
        )
        chunks = chunker.split(content, {"documentId": f"job-{job_id}", "jobId": job_id, "documentType": "jd"})
        safe_metadata["chunk_count"] = len(chunks)
        records.append(
            {
                "id": f"job-{job_id}-{content_hash[:16]}",
                "job_id": job_id,
                "embedding": _embedding_string(content),
                "metadata": safe_metadata,
                "content_hash": content_hash,
            }
        )

    return {
        "code": 1,
        "msg": "success",
        "data": {
            "records": records,
            "diagnostics": {
                "chunking": "recursive",
                "embedding": "python-deterministic-hash",
                "record_count": len(records),
            },
        },
    }


def search_jobs(payload: dict[str, Any]) -> dict[str, Any]:
    query_text = _text(payload.get("query_text") or payload.get("queryText"))
    raw_jobs = payload.get("jobs")
    if not query_text:
        return {"code": 0, "msg": "VALIDATION_ERROR", "data": {"error": "query_text is required"}}
    if not isinstance(raw_jobs, list):
        return {"code": 0, "msg": "VALIDATION_ERROR", "data": {"error": "jobs must be an array"}}

    limit = max(1, min(_int(payload.get("limit"), 5), 20))
    chunks = []
    chunker = RecursiveChunker(chunk_size=500, overlap=60)
    for job in raw_jobs:
        if not isinstance(job, dict):
            continue
        job_id = _int(job.get("job_id") or job.get("jobId"), 0)
        if job_id <= 0:
            continue
        text = _job_candidate_text(job)
        if not text:
            continue
        metadata = {
            "documentId": f"job-{job_id}",
            "jobId": job_id,
            "documentType": "job",
            "visibilityScope": "public",
        }
        chunks.extend(chunker.split(text, metadata))

    queries = expand_queries(query_text)
    retriever = HybridRetriever()
    scored_chunks = retriever.retrieve(
        queries,
        chunks,
        {"documentTypes": ["job"], "visibilityScope": "public"},
        top_k=max(limit * 3, limit),
    )

    best_by_job: dict[int, dict[str, Any]] = {}
    for item in scored_chunks:
        job_id = _int(item.chunk.metadata.get("jobId"), 0)
        if job_id <= 0:
            continue
        previous = best_by_job.get(job_id)
        if previous is None or item.score > previous["score"]:
            best_by_job[job_id] = {
                "job_id": job_id,
                "score": round(float(item.score), 6),
                "source": item.source,
            }

    scores = sorted(best_by_job.values(), key=lambda item: item["score"], reverse=True)[:limit]
    return {
        "code": 1,
        "msg": "success",
        "data": {
            "job_ids": [item["job_id"] for item in scores],
            "scores": scores,
            "retrieval": {
                "expanded_queries": queries,
                "fusion_method": "rrf",
                "reranker": "deterministic-fallback",
                "candidate_count": len(chunks),
            },
        },
    }


def _job(payload: dict[str, Any]) -> dict[str, Any]:
    value = payload.get("job")
    return value if isinstance(value, dict) else {}


def _merge_text(*values: Any) -> str:
    return "\n".join(_text(value) for value in values if _text(value))


def _infer_job_name(text: str) -> str:
    first_line = next((line.strip() for line in text.splitlines() if line.strip()), "")
    for prefix in ("job name:", "Job Name:", "role:", "Role:"):
        if first_line.startswith(prefix):
            return first_line[len(prefix):].strip() or "General Technical Role"
    if "\uff1a" in first_line:
        maybe_label, maybe_value = first_line.split("\uff1a", 1)
        if maybe_value.strip() and any(token in maybe_label.lower() for token in ("job", "role")):
            return maybe_value.strip()
    return first_line[:80] or "General Technical Role"

def _infer_category(text: str, job_name: str) -> tuple[str, str]:
    lower = f"{text} {job_name}".lower()
    rules = [
        (("rag", "llm", "agent", "ai", "machine learning", "algorithm", "pytorch", "tensorflow"), "AI_APP", "AI Application Engineer"),
        (("java", "spring", "backend", "back-end"), "JAVA_DEV", "Java Backend Engineer"),
        (("frontend", "front-end", "vue", "react"), "FRONTEND_DEV", "Frontend Engineer"),
        (("data", "sql", "etl"), "DATA_ENGINEER", "Data Engineer"),
        (("test", "qa"), "TEST_ENGINEER", "Test Engineer"),
        (("product", "product manager"), "PRODUCT_MANAGER", "Product Manager"),
        (("security",), "SECURITY_ENGINEER", "Security Engineer"),
        (("design", "ui", "ux", "figma"), "DESIGNER", "Designer"),
        (("devops", "sre", "kubernetes", "docker"), "DEVOPS_ENGINEER", "DevOps Engineer"),
    ]
    for keywords, code, name in rules:
        if any(keyword in lower for keyword in keywords):
            return code, name
    normalized = re.sub(r"[^A-Za-z0-9]+", "_", job_name.upper()).strip("_")[:40]
    return normalized or "GENERAL_TECH", job_name or "General Technical Role"

def _infer_level(text: str) -> str:
    lower = text.lower()
    if any(token in lower for token in ("intern", "internship")):
        return "INTERNSHIP"
    if any(token in lower for token in ("senior", "expert", "lead", "5+", "5 years")):
        return "SENIOR"
    if any(token in lower for token in ("mid", "3-5", "3+", "3 years")):
        return "MID"
    return "JUNIOR"

def _parse_salary(text: str) -> tuple[int, int, str]:
    normalized = text.replace("\uff0c", ",").replace("\u2013", "-").replace("\u2014", "-")
    match = re.search(r"(\d+(?:\.\d+)?)\s*[-~]\s*(\d+(?:\.\d+)?)\s*([kK\u4e07]?)", normalized)
    if not match:
        return 0, 0, "MONTH"
    left = float(match.group(1))
    right = float(match.group(2))
    unit = match.group(3)
    multiplier = 1000 if unit.lower() == "k" else 10000 if unit == "\u4e07" else 1
    return int(left * multiplier), int(right * multiplier), "MONTH"

def _infer_experience_years(text: str, level: str) -> int:
    match = re.search(r"(\d+)\s*(?:years?|yrs?)", text, re.IGNORECASE)
    if match:
        return int(match.group(1))
    return {"INTERNSHIP": 0, "JUNIOR": 1, "MID": 3, "SENIOR": 5}[level]

def _infer_skills(text: str) -> list[str]:
    lower = text.lower()
    skills = [skill for skill in SKILL_KEYWORDS if skill.lower() in lower]
    seen = set()
    result = []
    for skill in skills:
        key = skill.lower()
        if key not in seen:
            seen.add(key)
            result.append(skill)
    return result[:12]


def _short_text(text: str, max_length: int) -> str:
    compact = re.sub(r"\s+", " ", text).strip()
    return compact[:max_length]


def _sanitize_metadata(metadata: dict[str, Any]) -> dict[str, Any]:
    allowed = {
        "job_id",
        "document_type",
        "source",
        "visibility_scope",
        "content_hash",
        "chunk_count",
        "job_name",
        "industry",
        "city",
        "company_size",
    }
    return {key: value for key, value in metadata.items() if key in allowed and _is_scalar(value)}


def _is_scalar(value: Any) -> bool:
    return value is None or isinstance(value, (str, int, float, bool))


def _embedding_string(text: str, dimensions: int = EMBEDDING_DIMENSIONS) -> str:
    values: list[float] = []
    seed = hashlib.sha256(text.encode("utf-8")).digest()
    counter = 0
    while len(values) < dimensions:
        block = hashlib.sha256(seed + counter.to_bytes(4, "big")).digest()
        values.extend((byte - 127.5) / 127.5 for byte in block)
        counter += 1
    values = values[:dimensions]
    norm = math.sqrt(sum(value * value for value in values)) or 1.0
    return "[" + ",".join(f"{value / norm:.6f}" for value in values) + "]"


def _job_candidate_text(job: dict[str, Any]) -> str:
    required_skills = job.get("required_skills") or job.get("requiredSkills")
    if isinstance(required_skills, list):
        skills_text = ", ".join(_text(item) for item in required_skills if _text(item))
    else:
        skills_text = _text(required_skills)
    return _merge_text(
        job.get("job_name") or job.get("jobName"),
        job.get("job_category_code") or job.get("jobCategoryCode"),
        job.get("job_level") or job.get("jobLevel"),
        skills_text,
        job.get("job_description") or job.get("jobDescription"),
        job.get("job_profile") or job.get("jobProfile"),
    )


def _required_skills(job: dict[str, Any]) -> list[str]:
    value = job.get("requiredSkills") or job.get("required_skills") or job.get("coreSkills")
    if isinstance(value, list):
        return [_text(item) for item in value if _text(item)]
    if isinstance(value, str):
        try:
            parsed = json.loads(value)
        except json.JSONDecodeError:
            parsed = None
        if isinstance(parsed, list):
            return [_text(item) for item in parsed if _text(item)]
        return [item.strip() for item in value.split(",") if item.strip()]
    return []


def _capability_scores(payload: dict[str, Any], job: dict[str, Any]) -> dict[str, Any]:
    for key in ("capabilityScores", "capability_scores"):
        value = payload.get(key)
        if isinstance(value, dict):
            return value
    value = job.get("capabilityRequirements") or job.get("capability_requirements")
    return value if isinstance(value, dict) else {}


def _demand_level(source_job_count: int) -> str:
    if source_job_count >= 80:
        return "VERY_HIGH"
    if source_job_count >= 50:
        return "HIGH"
    if source_job_count >= 20:
        return "MEDIUM"
    return "LOW"


def _bounded_score(value: Any, default: int) -> int:
    return max(0, min(100, _int(value, default)))


def _int(value: Any, default: int) -> int:
    try:
        return int(value)
    except (TypeError, ValueError):
        return default


def _number(value: Any) -> float | None:
    try:
        return float(value)
    except (TypeError, ValueError):
        return None


def _text(value: Any) -> str:
    return str(value).strip() if value is not None else ""
