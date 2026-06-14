from __future__ import annotations

import argparse
import hashlib
import json
import math
import re
from collections import Counter, defaultdict
from dataclasses import dataclass
from http import HTTPStatus
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from typing import Any


TOKEN_RE = re.compile(r"[A-Za-z0-9_#+.-]+|[\u4e00-\u9fff]")
SECTION_RE = re.compile(r"^(education|experience|project|projects|skills|certificates|summary|profile|work|实习|项目|技能|教育|证书|自我)", re.I)


class ResumeValidationError(ValueError):
    pass


@dataclass(frozen=True)
class Chunk:
    id: str
    text: str
    section: str
    position: int
    metadata: dict[str, Any]


def analyze_resume(payload: dict[str, Any]) -> dict[str, Any]:
    vector_store_id = _required_str(payload, "vector_store_id")
    user_id = _required_int(payload, "user_id")
    resume_text = _required_str(payload, "resume_text")
    if not resume_text.strip():
        raise ResumeValidationError("resume_text must not be empty")

    file_type = str(payload.get("file_type") or "txt").lower()
    original_file_name = str(payload.get("original_file_name") or "")
    resume_file_path = str(payload.get("resume_file_path") or "")
    metadata = payload.get("metadata") if isinstance(payload.get("metadata"), dict) else {}

    base_metadata = {
        "user_id": user_id,
        "document_type": "resume",
        "vector_store_id": vector_store_id,
        "visibility": metadata.get("visibility", "user"),
        "file_type": file_type,
        "source": metadata.get("source", "resume_upload"),
    }
    chunks = recursive_chunk(resume_text, base_metadata)
    summaries = build_summary_index(chunks, resume_text)
    parsed = parse_resume_fields(resume_text, chunks)
    queries = expand_queries(parsed)
    filtered_chunks = metadata_filter(chunks, user_id=user_id, vector_store_id=vector_store_id)
    ranked = retrieve_and_rank(filtered_chunks, queries)
    selected = ranked[:5]

    scores = score_resume(parsed, selected, chunks)
    highlights = build_highlights(parsed, selected)
    suggestions = build_suggestions(parsed, scores)
    capability = build_capability_profile(parsed, scores, selected)

    return {
        "status": "completed",
        "parsed_data": parsed,
        "scores": scores,
        "highlights": highlights,
        "suggestions": suggestions,
        "capability_profile": capability,
        "rag_diagnostics": {
            "chunk_count": len(chunks),
            "summary_index_count": len(summaries),
            "metadata_filters": {
                "user_id": user_id,
                "document_type": "resume",
                "vector_store_id": vector_store_id,
                "visibility": base_metadata["visibility"],
            },
            "queries": queries,
            "retrieval": {
                "bm25": True,
                "embedding_fallback": "hash",
                "fusion": "rrf",
                "reranker": "deterministic",
            },
            "selected_evidence_ids": [item["chunk"].id for item in selected],
            "evidence": [
                {
                    "id": item["chunk"].id,
                    "section": item["chunk"].section,
                    "score": round(item["score"], 4),
                    "snippet": sanitize_snippet(item["chunk"].text),
                }
                for item in selected[:3]
            ],
            "sensitive_text_included": False,
            "source_file_type": file_type,
            "original_file_name_present": bool(original_file_name),
            "resume_file_path_present": bool(resume_file_path),
        },
    }


def recursive_chunk(text: str, metadata: dict[str, Any], max_chars: int = 700, overlap: int = 80) -> list[Chunk]:
    sections: list[tuple[str, str]] = []
    current_section = "summary"
    buffer: list[str] = []
    for raw_line in text.splitlines():
        line = raw_line.strip()
        if not line:
            if buffer:
                buffer.append("")
            continue
        if SECTION_RE.search(line) and buffer:
            sections.append((current_section, "\n".join(buffer).strip()))
            current_section = normalize_section(line)
            buffer = [line]
        else:
            if SECTION_RE.search(line):
                current_section = normalize_section(line)
            buffer.append(line)
    if buffer:
        sections.append((current_section, "\n".join(buffer).strip()))
    if not sections:
        sections = [("summary", text.strip())]

    chunks: list[Chunk] = []
    for section, section_text in sections:
        paragraphs = [p.strip() for p in re.split(r"\n\s*\n", section_text) if p.strip()]
        if not paragraphs:
            paragraphs = [section_text]
        for paragraph in paragraphs:
            for part in split_with_budget(paragraph, max_chars=max_chars, overlap=overlap):
                chunk_id = f"chunk-{len(chunks)}"
                chunk_metadata = dict(metadata)
                chunk_metadata.update({"section": section, "chunk_position": len(chunks)})
                chunks.append(Chunk(chunk_id, part, section, len(chunks), chunk_metadata))
    return chunks


def split_with_budget(text: str, max_chars: int, overlap: int) -> list[str]:
    if len(text) <= max_chars:
        return [text]
    sentences = re.split(r"(?<=[。！？.!?])\s+|\n+", text)
    parts: list[str] = []
    current = ""
    for sentence in sentences:
        if not sentence:
            continue
        if len(current) + len(sentence) + 1 <= max_chars:
            current = f"{current} {sentence}".strip()
        else:
            if current:
                parts.append(current)
            if len(sentence) <= max_chars:
                current = sentence
            else:
                current = ""
                start = 0
                while start < len(sentence):
                    end = min(len(sentence), start + max_chars)
                    parts.append(sentence[start:end])
                    if end == len(sentence):
                        break
                    start = max(0, end - overlap)
    if current:
        parts.append(current)
    return parts


def build_summary_index(chunks: list[Chunk], resume_text: str) -> list[dict[str, Any]]:
    summary_records = [
        {
            "id": "summary-document",
            "level": "document",
            "section": "all",
            "summary": sanitize_snippet(resume_text, limit=180),
        }
    ]
    by_section: dict[str, list[str]] = defaultdict(list)
    for chunk in chunks:
        by_section[chunk.section].append(chunk.text)
    for section, texts in by_section.items():
        summary_records.append(
            {
                "id": f"summary-{section}",
                "level": "section",
                "section": section,
                "summary": sanitize_snippet(" ".join(texts), limit=140),
            }
        )
    return summary_records


def metadata_filter(chunks: list[Chunk], *, user_id: int, vector_store_id: str) -> list[Chunk]:
    return [
        chunk
        for chunk in chunks
        if chunk.metadata.get("user_id") == user_id
        and chunk.metadata.get("document_type") == "resume"
        and chunk.metadata.get("vector_store_id") == vector_store_id
        and chunk.metadata.get("visibility") == "user"
    ]


def expand_queries(parsed: dict[str, Any]) -> list[str]:
    skills = parsed.get("skills") or []
    target = parsed.get("target_role") or "resume target role"
    top_skills = " ".join(skills[:6]) if skills else "technical skills"
    return [
        f"{target} skill evidence",
        f"{top_skills} project experience",
        "resume education certificates internship",
        "career intent skill gap measurable outcome",
    ]


def retrieve_and_rank(chunks: list[Chunk], queries: list[str]) -> list[dict[str, Any]]:
    bm25_lists = [rank_bm25(chunks, query) for query in queries]
    embedding_lists = [rank_hash_embedding(chunks, query) for query in queries]
    fused_scores: dict[str, float] = defaultdict(float)
    chunk_lookup = {chunk.id: chunk for chunk in chunks}
    for ranked in bm25_lists + embedding_lists:
        for rank, item in enumerate(ranked, start=1):
            fused_scores[item["chunk"].id] += 1.0 / (60 + rank)
    fused = [
        {"chunk": chunk_lookup[chunk_id], "score": score}
        for chunk_id, score in fused_scores.items()
        if chunk_id in chunk_lookup
    ]
    fused.sort(key=lambda item: (item["score"], section_priority(item["chunk"].section)), reverse=True)
    return fused


def rank_bm25(chunks: list[Chunk], query: str) -> list[dict[str, Any]]:
    query_terms = tokenize(query)
    docs = [tokenize(chunk.text) for chunk in chunks]
    doc_freq: Counter[str] = Counter()
    for terms in docs:
        doc_freq.update(set(terms))
    avgdl = sum(len(terms) for terms in docs) / max(len(docs), 1)
    ranked = []
    for chunk, terms in zip(chunks, docs):
        tf = Counter(terms)
        score = 0.0
        for term in query_terms:
            if not tf[term]:
                continue
            idf = math.log(1 + (len(docs) - doc_freq[term] + 0.5) / (doc_freq[term] + 0.5))
            numerator = tf[term] * 2.2
            denominator = tf[term] + 1.2 * (0.25 + 0.75 * len(terms) / max(avgdl, 1))
            score += idf * numerator / denominator
        ranked.append({"chunk": chunk, "score": score})
    ranked.sort(key=lambda item: item["score"], reverse=True)
    return ranked


def rank_hash_embedding(chunks: list[Chunk], query: str) -> list[dict[str, Any]]:
    qvec = hash_vector(query)
    ranked = []
    for chunk in chunks:
        score = cosine(qvec, hash_vector(chunk.text))
        ranked.append({"chunk": chunk, "score": score})
    ranked.sort(key=lambda item: item["score"], reverse=True)
    return ranked


def parse_resume_fields(text: str, chunks: list[Chunk]) -> dict[str, Any]:
    skills = extract_skills(text)
    target_role = extract_target_role(text, skills)
    education = extract_education(text)
    experience = extract_experience(text)
    name = extract_name(text)
    years = estimate_experience_years(text, experience)
    return {
        "name": name,
        "target_role": target_role,
        "location": extract_location(text),
        "current_role": target_role,
        "skills": skills,
        "experience_years": years,
        "match_score": min(95, 45 + len(skills) * 4 + min(years, 5) * 5 + len(experience) * 4),
        "education": education,
        "experience": experience,
    }


def extract_skills(text: str) -> list[str]:
    known = [
        "Python",
        "Java",
        "Spring Boot",
        "Vue",
        "React",
        "TypeScript",
        "JavaScript",
        "PostgreSQL",
        "Redis",
        "Docker",
        "RAG",
        "LangChain",
        "MyBatis",
        "FastAPI",
        "Machine Learning",
        "NLP",
        "SQL",
        "Git",
    ]
    lowered = text.lower()
    skills = [skill for skill in known if skill.lower() in lowered]
    if not skills:
        for token, count in Counter(tokenize(text)).most_common(8):
            if len(token) >= 3 and count >= 2:
                skills.append(token)
    return list(dict.fromkeys(skills))[:12]


def extract_target_role(text: str, skills: list[str]) -> str:
    patterns = [
        r"(?:target|objective|role|position)[:：]\s*([^\n,;；]{2,40})",
        r"(?:求职意向|目标岗位|应聘岗位)[:：]\s*([^\n,;；]{2,40})",
    ]
    for pattern in patterns:
        match = re.search(pattern, text, re.I)
        if match:
            return match.group(1).strip()
    if "rag" in text.lower() or "agent" in text.lower():
        return "AI Agent Intern"
    if "java" in [s.lower() for s in skills]:
        return "Java Developer"
    if "python" in [s.lower() for s in skills]:
        return "Python Developer"
    return "Software Engineer"


def extract_education(text: str) -> list[dict[str, str]]:
    lines = [line.strip() for line in text.splitlines() if line.strip()]
    result = []
    for line in lines:
        if any(word in line.lower() for word in ["university", "college", "bachelor", "master", "大学", "学院", "本科", "硕士"]):
            result.append({"school": line[:80], "major": "", "degree": "", "period": extract_period(line)})
        if len(result) >= 3:
            break
    return result


def extract_experience(text: str) -> list[dict[str, str]]:
    lines = [line.strip() for line in text.splitlines() if line.strip()]
    result = []
    for line in lines:
        lower = line.lower()
        if any(word in lower for word in ["project", "intern", "company", "项目", "实习", "公司"]):
            result.append(
                {
                    "company": "",
                    "position": "",
                    "period": extract_period(line),
                    "description": sanitize_snippet(line, limit=120),
                }
            )
        if len(result) >= 5:
            break
    return result


def extract_name(text: str) -> str:
    for line in text.splitlines()[:8]:
        line = line.strip()
        if not line:
            continue
        match = re.search(r"(?:name|姓名)[:：]\s*([A-Za-z\u4e00-\u9fff .]{2,30})", line, re.I)
        if match:
            return match.group(1).strip()
    return ""


def extract_location(text: str) -> str:
    match = re.search(r"(?:location|city|期望城市|城市)[:：]\s*([A-Za-z\u4e00-\u9fff ]{2,30})", text, re.I)
    return match.group(1).strip() if match else ""


def extract_period(text: str) -> str:
    match = re.search(r"(20\d{2}|19\d{2})\s*[-/至~]\s*(20\d{2}|present|now|至今)?", text, re.I)
    return match.group(0) if match else ""


def estimate_experience_years(text: str, experience: list[dict[str, str]]) -> int:
    explicit = re.search(r"(\d+)\+?\s*(?:years|年)", text, re.I)
    if explicit:
        return min(20, int(explicit.group(1)))
    return min(8, max(0, len(experience)))


def score_resume(parsed: dict[str, Any], selected: list[dict[str, Any]], chunks: list[Chunk]) -> dict[str, int]:
    skill_count = len(parsed.get("skills") or [])
    exp_count = len(parsed.get("experience") or [])
    edu_count = len(parsed.get("education") or [])
    evidence_bonus = min(15, len(selected) * 3)
    return {
        "keyword_match": clamp_score(45 + skill_count * 5 + evidence_bonus),
        "layout": clamp_score(55 + min(20, len(chunks) * 2) + (10 if edu_count else 0)),
        "skill_depth": clamp_score(45 + skill_count * 6 + exp_count * 4),
        "experience": clamp_score(45 + exp_count * 8 + min(20, parsed.get("experience_years") or 0) * 3),
    }


def build_highlights(parsed: dict[str, Any], selected: list[dict[str, Any]]) -> list[str]:
    highlights = []
    skills = parsed.get("skills") or []
    if skills:
        highlights.append(f"Detected core skills: {', '.join(skills[:5])}.")
    if parsed.get("experience"):
        highlights.append("Resume contains project or internship evidence.")
    for item in selected[:2]:
        snippet = sanitize_snippet(item["chunk"].text, limit=90)
        if snippet:
            highlights.append(f"Evidence {item['chunk'].id}: {snippet}")
    return highlights[:5] or ["Resume has extractable text for deterministic analysis."]


def build_suggestions(parsed: dict[str, Any], scores: dict[str, int]) -> list[dict[str, str]]:
    suggestions = []
    if scores["skill_depth"] < 75:
        suggestions.append({"type": "SKILL", "content": "Add concrete project outcomes for each core skill."})
    if scores["experience"] < 70:
        suggestions.append({"type": "CONTENT", "content": "Describe internship or project responsibilities with measurable results."})
    if not parsed.get("target_role"):
        suggestions.append({"type": "CONTENT", "content": "Add a clear target role or career objective."})
    if scores["layout"] < 75:
        suggestions.append({"type": "LAYOUT", "content": "Group education, skills, projects, and experience into clear sections."})
    return suggestions[:4]


def build_capability_profile(parsed: dict[str, Any], scores: dict[str, int], selected: list[dict[str, Any]]) -> dict[str, Any]:
    professional = scores["skill_depth"]
    internship = scores["experience"]
    learning = clamp_score(55 + len(parsed.get("skills") or []) * 4)
    communication = clamp_score(55 + len(selected) * 3)
    capability_scores = {
        "professional_skill": professional,
        "certificate": 60 if "certificate" in " ".join(parsed.get("skills") or []).lower() else 50,
        "innovation": clamp_score(50 + len(parsed.get("experience") or []) * 4),
        "learning": learning,
        "resilience": clamp_score(55 + min(15, parsed.get("experience_years") or 0) * 2),
        "communication": communication,
        "internship": internship,
    }
    skills = [
        {"name": skill, "proficiency": 3 if idx < 5 else 2, "years": max(1, parsed.get("experience_years") or 1), "evidence": "Detected from resume evidence"}
        for idx, skill in enumerate((parsed.get("skills") or [])[:8])
    ]
    overall = round(sum(capability_scores.values()) / len(capability_scores))
    return {
        "overall_score": overall,
        "completeness_score": clamp_score(50 + bool(parsed.get("education")) * 10 + bool(parsed.get("experience")) * 15 + len(parsed.get("skills") or []) * 3),
        "competitiveness_score": clamp_score((professional + internship + learning) // 3),
        "capability_scores": capability_scores,
        "professional_skills": skills,
        "certificates": [],
        "soft_skills": {
            "learning": {"score": learning, "evidence": ["Skills and project text were detected."], "description": "Learning ability inferred from technical breadth."},
            "communication": {"score": communication, "evidence": ["Resume sections provide structured evidence."], "description": "Communication inferred from resume structure."},
        },
        "ai_evaluation": "Deterministic fallback evaluation generated from extracted resume evidence and retrieval diagnostics.",
    }


def normalize_section(line: str) -> str:
    lowered = line.lower()
    if "skill" in lowered or "技能" in line:
        return "skills"
    if "project" in lowered or "项目" in line:
        return "projects"
    if "education" in lowered or "教育" in line:
        return "education"
    if "experience" in lowered or "intern" in lowered or "实习" in line:
        return "experience"
    if "certificate" in lowered or "证书" in line:
        return "certificates"
    return "summary"


def section_priority(section: str) -> int:
    priorities = {"skills": 5, "projects": 4, "experience": 4, "education": 3, "certificates": 2, "summary": 1}
    return priorities.get(section, 0)


def tokenize(text: str) -> list[str]:
    return [token.lower() for token in TOKEN_RE.findall(text or "") if token.strip()]


def hash_vector(text: str, dims: int = 32) -> list[float]:
    vector = [0.0] * dims
    for token in tokenize(text):
        digest = hashlib.sha256(token.encode("utf-8")).digest()
        idx = digest[0] % dims
        sign = 1.0 if digest[1] % 2 == 0 else -1.0
        vector[idx] += sign
    return vector


def cosine(left: list[float], right: list[float]) -> float:
    dot = sum(a * b for a, b in zip(left, right))
    ln = math.sqrt(sum(a * a for a in left))
    rn = math.sqrt(sum(b * b for b in right))
    if ln == 0 or rn == 0:
        return 0.0
    return dot / (ln * rn)


def sanitize_snippet(text: str, limit: int = 120) -> str:
    compact = re.sub(r"\s+", " ", text or "").strip()
    compact = re.sub(r"[\w.+-]+@[\w.-]+", "[email]", compact)
    compact = re.sub(r"(?:\+?\d[\d -]{7,}\d)", "[phone]", compact)
    return compact[:limit]


def clamp_score(value: int | float) -> int:
    return max(0, min(100, int(round(value))))


def _required_str(payload: dict[str, Any], key: str) -> str:
    value = payload.get(key)
    if not isinstance(value, str) or not value.strip():
        raise ResumeValidationError(f"{key} is required")
    return value.strip()


def _required_int(payload: dict[str, Any], key: str) -> int:
    value = payload.get(key)
    if isinstance(value, bool):
        raise ResumeValidationError(f"{key} must be an integer")
    try:
        parsed = int(value)
    except (TypeError, ValueError) as exc:
        raise ResumeValidationError(f"{key} must be an integer") from exc
    if parsed <= 0:
        raise ResumeValidationError(f"{key} must be positive")
    return parsed


class ResumeAiHandler(BaseHTTPRequestHandler):
    def do_POST(self) -> None:
        if self.path != "/api/v1/resume/analyze":
            self._write_json(HTTPStatus.NOT_FOUND, {"message": "not found"})
            return
        try:
            payload = self._read_json()
            result = analyze_resume(payload)
            self._write_json(HTTPStatus.OK, result)
        except ResumeValidationError as exc:
            self._write_json(HTTPStatus.BAD_REQUEST, {"message": str(exc)})
        except ValueError as exc:
            self._write_json(HTTPStatus.BAD_REQUEST, {"message": str(exc)})
        except Exception as exc:  # pragma: no cover - HTTP boundary guard
            self._write_json(HTTPStatus.INTERNAL_SERVER_ERROR, {"message": type(exc).__name__})

    def log_message(self, format: str, *args: Any) -> None:
        return

    def _read_json(self) -> dict[str, Any]:
        content_length = int(self.headers.get("Content-Length") or 0)
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
    server = ThreadingHTTPServer((host, port), ResumeAiHandler)
    print(f"Resume AI service listening on http://{host}:{port}", flush=True)
    server.serve_forever()


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--host", default="127.0.0.1")
    parser.add_argument("--port", default=8091, type=int)
    args = parser.parse_args()
    run(args.host, args.port)
