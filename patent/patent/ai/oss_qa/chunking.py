from __future__ import annotations

from dataclasses import dataclass


@dataclass(frozen=True)
class Chunk:
    source: str
    url: str | None
    chunk_id: int
    text: str


def chunk_text(
    source: str,
    url: str | None,
    text: str,
    *,
    chunk_size: int = 1800,
    overlap: int = 200,
) -> list[Chunk]:
    t = (text or "").strip()
    if not t:
        return []
    if chunk_size <= overlap:
        raise ValueError("chunk_size must be > overlap")

    chunks: list[Chunk] = []
    start = 0
    cid = 0
    while start < len(t):
        end = min(len(t), start + chunk_size)
        piece = t[start:end].strip()
        if piece:
            chunks.append(Chunk(source=source, url=url, chunk_id=cid, text=piece))
            cid += 1
        if end >= len(t):
            break
        start = max(0, end - overlap)
    return chunks

