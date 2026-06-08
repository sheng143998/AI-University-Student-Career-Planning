from __future__ import annotations

import argparse
import json
from http import HTTPStatus
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from typing import Any

from ai_service.dashboard_rag_service import match_target_job
from ai_service.goals_rag_service import generate_goal_advice
from ai_service.rag_feedback_service import accept_rag_feedback, validate_rag_preferences
from ai_service.reports_rag_service import generate_report_support
from ai_service.roadmap_rag_service import generate_roadmap_recommendations


def _as_int(value: Any, default: int = 0) -> int:
    try:
        return int(value)
    except (TypeError, ValueError):
        return default


def _as_number(value: Any) -> float | None:
    try:
        return float(value)
    except (TypeError, ValueError):
        return None


def _job(payload: dict[str, Any]) -> dict[str, Any]:
    value = payload.get("job")
    return value if isinstance(value, dict) else {}


def _required_skills(job: dict[str, Any]) -> list[str]:
    skills = job.get("requiredSkills")
    if isinstance(skills, list):
        return [str(skill) for skill in skills if str(skill).strip()]
    if isinstance(skills, str):
        try:
            parsed = json.loads(skills)
            if isinstance(parsed, list):
                return [str(skill) for skill in parsed if str(skill).strip()]
        except json.JSONDecodeError:
            return [item.strip() for item in skills.split(",") if item.strip()]
    return []


def _demand_level(source_job_count: int) -> str:
    if source_job_count >= 80:
        return "VERY_HIGH"
    if source_job_count >= 50:
        return "HIGH"
    if source_job_count >= 20:
        return "MEDIUM"
    return "LOW"


def generate_market_insight(payload: dict[str, Any]) -> dict[str, Any]:
    job = _job(payload)
    skills = _required_skills(job)
    job_name = str(job.get("jobName") or "目标岗位")
    city = str(payload.get("city") or "全国")
    source_job_count = _as_int(job.get("sourceJobCount"), 100)
    min_salary = _as_number(job.get("minSalary"))
    max_salary = _as_number(job.get("maxSalary"))
    salary_unit = str(job.get("salaryUnit") or "month").lower()
    demand_level = _demand_level(source_job_count)

    if min_salary is not None and max_salary is not None:
        salary_value = f"{int(min_salary)}-{int(max_salary)}/{salary_unit}"
    else:
        salary_value = "样本不足"

    top_skills = "、".join(skills[:3]) if skills else "核心岗位技能"
    return {
        "title": f"{job_name} 市场洞察",
        "summary": (
            f"{city}{job_name}岗位当前需求等级为{demand_level}，招聘样本约{source_job_count}条。"
            f"具备{top_skills}等能力的候选人更容易形成竞争优势。"
        ),
        "marketSignals": [
            {"label": "需求强度", "value": demand_level, "trend": "UP" if source_job_count >= 50 else "STABLE"},
            {"label": "样本岗位", "value": str(source_job_count), "trend": "UP"},
            {"label": "薪资区间", "value": salary_value, "trend": "UP" if min_salary and max_salary else "STABLE"},
            {"label": "技能更新", "value": "FAST" if len(skills) >= 3 else "MEDIUM", "trend": "UP"},
        ],
        "industryTrends": [
            f"{job_name}对可验证项目经验和岗位技能匹配度的要求提升",
            "企业更关注候选人将技术能力转化为业务结果的证据",
            "复合技能和持续学习能力成为岗位竞争的重要区分点",
            "城市和行业场景差异会影响薪资、技能组合和成长路径",
        ],
        "suggestedActions": [
            {"title": "补强核心技能证据", "desc": f"围绕{top_skills}沉淀项目、实习或作品集证据", "priority": "HIGH"},
            {"title": "跟踪岗位关键词", "desc": "定期复盘JD中的高频技能、工具和业务场景", "priority": "MEDIUM"},
            {"title": "完善简历表达", "desc": "将技能描述改写为可量化的职责、行动和结果", "priority": "MEDIUM"},
            {"title": "关注区域差异", "desc": "结合目标城市样本调整投递策略和薪资预期", "priority": "LOW"},
        ],
    }


def generate_soft_skills(payload: dict[str, Any]) -> list[dict[str, Any]]:
    capability_scores = payload.get("capabilityScores")
    if not isinstance(capability_scores, dict):
        capability_scores = {}
    job = _job(payload)
    job_name = str(job.get("jobName") or "该岗位")

    definitions = [
        ("innovation", "创新能力", "能结合岗位场景提出改进方案，并将新工具或新方法转化为可执行实践。"),
        ("learning", "学习能力", "能快速理解岗位所需知识，并持续补齐技能差距。"),
        ("resilience", "抗压能力", "能在需求变化、交付压力和复杂问题下保持稳定推进。"),
        ("communication", "沟通能力", "能清晰表达技术或业务判断，并推动跨角色协作。"),
        ("internship", "实习能力", "能把课堂、项目或实习经验转化为岗位相关成果。"),
    ]

    result: list[dict[str, Any]] = []
    for key, name, description in definitions:
        score = max(0, min(100, _as_int(capability_scores.get(key), 60)))
        result.append(
            {
                "name": name,
                "score": score,
                "description": f"{job_name}需要{name}：{description}",
                "evidence": [
                    f"围绕{job_name}的真实任务说明该能力如何发挥作用",
                    "用项目、实习或作品集中的具体结果支撑能力判断",
                ],
            }
        )
    return result


class MarketAiHandler(BaseHTTPRequestHandler):
    def do_POST(self) -> None:
        try:
            payload = self._read_json()
            if self.path == "/api/v1/market/insight":
                self._write_json(HTTPStatus.OK, generate_market_insight(payload))
                return
            if self.path == "/api/v1/market/soft-skills":
                self._write_json(HTTPStatus.OK, generate_soft_skills(payload))
                return
            if self.path == "/api/roadmap/recommendations/personalized":
                self._write_json(HTTPStatus.OK, generate_roadmap_recommendations(payload))
                return
            if self.path == "/api/v1/goals/advice":
                self._write_json(HTTPStatus.OK, generate_goal_advice(payload))
                return
            if self.path == "/api/v1/reports/generate-support":
                self._write_json(HTTPStatus.OK, generate_report_support(payload))
                return
            if self.path == "/internal/dashboard/target-job/match":
                self._write_json(HTTPStatus.OK, match_target_job(payload))
                return
            if self.path == "/internal/rag/feedback":
                self._write_json(HTTPStatus.OK, accept_rag_feedback(payload))
                return
            if self.path == "/internal/rag/preferences/validate":
                self._write_json(HTTPStatus.OK, validate_rag_preferences(payload))
                return
            self._write_json(HTTPStatus.NOT_FOUND, {"message": "not found"})
        except ValueError as exc:
            self._write_json(HTTPStatus.BAD_REQUEST, {"message": str(exc)})
        except Exception as exc:  # pragma: no cover - last-resort HTTP boundary guard
            self._write_json(HTTPStatus.INTERNAL_SERVER_ERROR, {"message": str(exc)})

    def log_message(self, format: str, *args: Any) -> None:
        return

    def _read_json(self) -> dict[str, Any]:
        content_length = _as_int(self.headers.get("Content-Length"), 0)
        raw_body = self.rfile.read(content_length).decode("utf-8") if content_length else "{}"
        try:
            payload = json.loads(raw_body)
        except json.JSONDecodeError as exc:
            raise ValueError("invalid json body") from exc
        if not isinstance(payload, dict):
            raise ValueError("json body must be an object")
        return payload

    def _write_json(self, status: HTTPStatus, payload: Any) -> None:
        body = json.dumps(payload, ensure_ascii=False).encode("utf-8")
        self.send_response(status.value)
        self.send_header("Content-Type", "application/json; charset=utf-8")
        self.send_header("Content-Length", str(len(body)))
        self.end_headers()
        self.wfile.write(body)


def run(host: str, port: int) -> None:
    server = ThreadingHTTPServer((host, port), MarketAiHandler)
    print(f"Python AI service listening on http://{host}:{port}", flush=True)
    server.serve_forever()


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--host", default="127.0.0.1")
    parser.add_argument("--port", default=8090, type=int)
    args = parser.parse_args()
    run(args.host, args.port)
