from rag.chunking import Chunk


def summarize(text: str, max_chars: int = 140) -> str:
    clean = " ".join(text.split())
    if len(clean) <= max_chars:
        return clean
    return clean[:max_chars].rstrip() + "..."


def build_summary_index(chunks: list[Chunk]) -> list[Chunk]:
    summaries: list[Chunk] = []
    for chunk in chunks:
        metadata = {**chunk.metadata, "indexType": "summary", "rawChunkId": chunk.chunk_id}
        summaries.append(
            Chunk(
                chunk_id=f"summary-{chunk.chunk_id}",
                text=summarize(chunk.text),
                metadata=metadata,
            )
        )
    return summaries
