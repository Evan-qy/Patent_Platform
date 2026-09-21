from __future__ import annotations

import json
import time
from dataclasses import asdict, dataclass
from pathlib import Path
from typing import Iterable

from oss_qa.chunking import Chunk, chunk_text
from oss_qa.config import AppConfig
from oss_qa.pipeline import load_docs_from_export_csv
from oss_qa.qwen_api import QwenClient, batch_iter


@dataclass(frozen=True)
class IndexedChunk:
    source: str
    url: str | None
    chunk_id: int
    text: str
    embedding: list[float]


def _cosine_list(a: list[float], b: list[float]) -> float:
    if len(a) != len(b):
        raise ValueError("embedding length mismatch")
    dot = 0.0
    na = 0.0
    nb = 0.0
    for x, y in zip(a, b, strict=True):
        dot += x * y
        na += x * x
        nb += y * y
    denom = (na**0.5) * (nb**0.5) + 1e-12
    return float(dot / denom)


def build_index_from_export_csv(
    cfg: AppConfig,
    export_csv_path: str | Path,
    *,
    out_dir: str | Path = "index",
    cache_dir: str | Path = "cache",
    model_embed: str = "text-embedding-v2",
    batch_size: int = 16,
    max_files: int | None = None,
    max_bytes: int = 20 * 1024 * 1024,
    max_chunks: int = 2000,
    sleep_s: float = 0.2,
) -> Path:
    if not cfg.dashscope_api_key:
        raise ValueError("missing DASHSCOPE_API_KEY")
    out_dir = Path(out_dir)
    out_dir.mkdir(parents=True, exist_ok=True)

    client = QwenClient(cfg.dashscope_api_key)
    docs = load_docs_from_export_csv(
        cfg, export_csv_path, cache_dir=cache_dir, max_files=max_files, max_bytes=max_bytes
    )

    chunks: list[Chunk] = []
    for d in docs:
        chunks.extend(chunk_text(d.source, d.url, d.text))
    if max_chunks and len(chunks) > max_chunks:
        chunks = chunks[:max_chunks]

    texts = [c.text[:6000] for c in chunks]
    embeddings: list[list[float]] = []
    for b in batch_iter(texts, batch_size=batch_size):
        embeddings.extend(client.embeddings(b, model=model_embed))
        if sleep_s and sleep_s > 0:
            time.sleep(sleep_s)

    indexed: list[IndexedChunk] = []
    for c, e in zip(chunks, embeddings, strict=True):
        indexed.append(IndexedChunk(source=c.source, url=c.url, chunk_id=c.chunk_id, text=c.text, embedding=e))

    (out_dir / "chunks.json").write_text(
        json.dumps([asdict(x) for x in indexed], ensure_ascii=False),
        encoding="utf-8",
    )
    (out_dir / "manifest.json").write_text(
        json.dumps(
            {
                "export_csv_path": str(export_csv_path),
                "chunk_count": len(indexed),
                "embed_model": model_embed,
            },
            ensure_ascii=False,
            indent=2,
        ),
        encoding="utf-8",
    )
    return out_dir


def load_index(index_dir: str | Path) -> list[IndexedChunk]:
    index_dir = Path(index_dir)
    data = json.loads((index_dir / "chunks.json").read_text(encoding="utf-8"))
    return [IndexedChunk(**x) for x in data]


def search_index(
    cfg: AppConfig,
    index_dir: str | Path,
    query: str,
    *,
    top_k: int = 6,
    model_embed: str = "text-embedding-v2",
) -> list[tuple[float, IndexedChunk]]:
    if not cfg.dashscope_api_key:
        raise ValueError("missing DASHSCOPE_API_KEY")
    client = QwenClient(cfg.dashscope_api_key)
    q_emb = client.embeddings([query], model=model_embed)[0]

    chunks = load_index(index_dir)
    scored: list[tuple[float, IndexedChunk]] = []
    for c in chunks:
        scored.append((_cosine_list(q_emb, c.embedding), c))
    scored.sort(key=lambda x: x[0], reverse=True)
    return scored[: max(1, top_k)]


def answer_with_rag(
    cfg: AppConfig,
    index_dir: str | Path,
    question: str,
    *,
    top_k: int = 6,
    model_chat: str = "qwen-plus",
    model_embed: str = "text-embedding-v2",
) -> str:
    if not cfg.dashscope_api_key:
        raise ValueError("missing DASHSCOPE_API_KEY")

    hits = search_index(cfg, index_dir, question, top_k=top_k, model_embed=model_embed)
    blocks: list[str] = []
    for i, (_, c) in enumerate(hits, start=1):
        meta = f"来源={c.source}"
        if c.url:
            meta += f" | url={c.url}"
        blocks.append(f"[资料{i}] {meta}\n{c.text}")

    context = "\n\n".join(blocks)
    messages = [
        {
            "role": "system",
            "content": "你是一个严谨的检索增强问答助手。只基于用户提供的资料回答，不要编造；资料不足就明确说明。",
        },
        {"role": "user", "content": f"问题：{question}\n\n资料：\n{context}\n\n要求：给出答案，并在关键结论处引用对应[资料编号]。"},
    ]
    client = QwenClient(cfg.dashscope_api_key)
    return client.chat(messages, model=model_chat, temperature=0.2)

