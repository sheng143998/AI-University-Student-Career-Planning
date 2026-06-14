from dataclasses import asdict, dataclass, field
from typing import Any


@dataclass
class ChatHistoryItem:
    role: str
    content: str
    createdAt: str | None = None

    @classmethod
    def from_dict(cls, data: dict[str, Any]) -> "ChatHistoryItem":
        return cls(
            role=str(data.get("role", "")),
            content=str(data.get("content", "")),
            createdAt=data.get("createdAt"),
        )


@dataclass
class MetadataFilter:
    userId: int
    documentTypes: list[str] = field(default_factory=list)
    resumeId: int | None = None
    jobId: int | None = None
    visibilityScope: str | None = None

    @classmethod
    def from_dict(cls, data: dict[str, Any] | None, fallback_user_id: int) -> "MetadataFilter":
        data = data or {}
        return cls(
            userId=int(data.get("userId") or fallback_user_id),
            documentTypes=list(data.get("documentTypes") or []),
            resumeId=data.get("resumeId"),
            jobId=data.get("jobId"),
            visibilityScope=data.get("visibilityScope"),
        )

    def to_filter_dict(self) -> dict[str, Any]:
        return {key: value for key, value in asdict(self).items() if value is not None}


@dataclass
class RetrievalOptions:
    multiQuery: bool = True
    hybridSearch: bool = True
    ragFusion: bool = True
    metadataFilter: MetadataFilter | None = None

    @classmethod
    def from_dict(cls, data: dict[str, Any] | None, fallback_user_id: int) -> "RetrievalOptions":
        data = data or {}
        return cls(
            multiQuery=bool(data.get("multiQuery", True)),
            hybridSearch=bool(data.get("hybridSearch", True)),
            ragFusion=bool(data.get("ragFusion", True)),
            metadataFilter=MetadataFilter.from_dict(data.get("metadataFilter"), fallback_user_id),
        )


@dataclass
class ChatCompleteRequest:
    userId: int
    conversationId: int
    content: str
    resumeId: int | None = None
    parsedData: dict[str, Any] = field(default_factory=dict)
    history: list[ChatHistoryItem] = field(default_factory=list)
    retrievalOptions: RetrievalOptions = field(default_factory=RetrievalOptions)

    @classmethod
    def from_dict(cls, data: dict[str, Any]) -> "ChatCompleteRequest":
        user_id = int(data["userId"])
        return cls(
            userId=user_id,
            conversationId=int(data["conversationId"]),
            content=str(data["content"]),
            resumeId=data.get("resumeId"),
            parsedData=dict(data.get("parsedData") or {}),
            history=[ChatHistoryItem.from_dict(item) for item in data.get("history", [])],
            retrievalOptions=RetrievalOptions.from_dict(data.get("retrievalOptions"), user_id),
        )


@dataclass
class EvidenceItem:
    sourceType: str
    sourceId: str
    chunkId: str
    summary: str
    score: float


@dataclass
class ChatDiagnostics:
    expandedQueries: list[str] = field(default_factory=list)
    retrieval: str = "bm25+embedding"
    fusion: str = "rag-fusion"
    reranker: str = "deterministic"


@dataclass
class ChatCompleteResponse:
    content: str
    suggestionQuestions: list[str] = field(default_factory=list)
    tip: str | None = None
    title: str | None = None
    evidence: list[EvidenceItem] = field(default_factory=list)
    diagnostics: ChatDiagnostics = field(default_factory=ChatDiagnostics)

    def to_dict(self) -> dict[str, Any]:
        return asdict(self)


@dataclass
class DailySuggestionsRequest:
    userId: int
    resumeId: int | None = None
    parsedData: dict[str, Any] = field(default_factory=dict)

    @classmethod
    def from_dict(cls, data: dict[str, Any]) -> "DailySuggestionsRequest":
        return cls(
            userId=int(data["userId"]),
            resumeId=data.get("resumeId"),
            parsedData=dict(data.get("parsedData") or {}),
        )


@dataclass
class SuggestionItem:
    title: str
    text: str


@dataclass
class QuickQuestion:
    title: str
    text: str


@dataclass
class DailySuggestionsResponse:
    suggestions: list[SuggestionItem] = field(default_factory=list)
    quickQuestions: list[QuickQuestion] = field(default_factory=list)

    def to_dict(self) -> dict[str, Any]:
        return asdict(self)
