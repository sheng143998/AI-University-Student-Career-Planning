from dataclasses import dataclass


@dataclass(frozen=True)
class Chunk:
    chunk_id: str
    text: str
    metadata: dict


class RecursiveChunker:
    def __init__(self, chunk_size: int = 600, overlap: int = 80) -> None:
        self.chunk_size = chunk_size
        self.overlap = overlap
        self.separators = ["\n## ", "\n### ", "\n\n", "\n", "。", "，", " "]

    def split(self, text: str, metadata: dict | None = None) -> list[Chunk]:
        metadata = metadata or {}
        raw_chunks = self._split_recursive(text.strip(), self.separators)
        chunks: list[Chunk] = []
        for index, chunk_text in enumerate(raw_chunks):
            clean = chunk_text.strip()
            if not clean:
                continue
            chunk_metadata = {**metadata, "chunkPosition": index}
            chunks.append(Chunk(chunk_id=f"{metadata.get('documentId', 'doc')}-{index}", text=clean, metadata=chunk_metadata))
        return chunks

    def _split_recursive(self, text: str, separators: list[str]) -> list[str]:
        if len(text) <= self.chunk_size:
            return [text] if text else []
        if not separators:
            return self._split_by_length(text)

        separator = separators[0]
        pieces = text.split(separator)
        if len(pieces) == 1:
            return self._split_recursive(text, separators[1:])

        chunks: list[str] = []
        current = ""
        for piece in pieces:
            candidate = piece if not current else current + separator + piece
            if len(candidate) <= self.chunk_size:
                current = candidate
                continue
            if current:
                chunks.extend(self._split_recursive(current, separators[1:]))
            current = piece
        if current:
            chunks.extend(self._split_recursive(current, separators[1:]))
        return self._add_overlap(chunks)

    def _split_by_length(self, text: str) -> list[str]:
        chunks = []
        start = 0
        step = max(1, self.chunk_size - self.overlap)
        while start < len(text):
            chunks.append(text[start:start + self.chunk_size])
            start += step
        return chunks

    def _add_overlap(self, chunks: list[str]) -> list[str]:
        if self.overlap <= 0 or len(chunks) <= 1:
            return chunks
        result = [chunks[0]]
        for previous, current in zip(chunks, chunks[1:]):
            prefix = previous[-self.overlap:]
            result.append(prefix + current)
        return result
