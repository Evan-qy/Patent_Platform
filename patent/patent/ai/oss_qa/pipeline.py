from __future__ import annotations

from pathlib import Path

from oss_qa.extract import ExtractedDoc, extract_text
from oss_qa.fetchers import fetch_object
from oss_qa.oss_client import build_oss_bucket
from oss_qa.sources import read_export_urls_csv
from oss_qa.config import AppConfig


def load_docs_from_export_csv(
    cfg: AppConfig,
    csv_path: str | Path,
    *,
    cache_dir: str | Path = "cache",
    max_files: int | None = None,
    max_bytes: int = 20 * 1024 * 1024,
) -> list[ExtractedDoc]:
    refs = read_export_urls_csv(csv_path)
    if max_files is not None:
        refs = refs[:max_files]

    bucket = build_oss_bucket(cfg)
    docs: list[ExtractedDoc] = []
    for ref in refs:
        fetched = fetch_object(ref, cache_dir=cache_dir, oss_bucket=bucket, max_bytes=max_bytes)
        doc = extract_text(fetched)
        if doc.text.strip():
            docs.append(doc)
    return docs

