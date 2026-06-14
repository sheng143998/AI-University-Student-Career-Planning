from __future__ import annotations

import json
from typing import Any


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


def _job(payload: dict[str, Any]) -> dict[str, Any]:
    value = payload.get("job")
    return value if isinstance(value, dict) else {}


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
