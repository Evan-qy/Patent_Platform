from __future__ import annotations

import hashlib
from dataclasses import dataclass
from pathlib import Path
from typing import Optional
from urllib.parse import urlparse
from urllib.parse import unquote

import requests

from oss_qa.sources import ObjectRef


@dataclass(frozen=True)
class FetchedObject:
    ref: ObjectRef
    content_bytes: bytes
    content_type: str | None


def _safe_cache_name(ref: ObjectRef) -> str:
    h = hashlib.sha1()
    h.update((ref.object_key or "").encode("utf-8"))
    h.update(b"\n")
    h.update((ref.url or "").encode("utf-8"))
    return h.hexdigest()


def _guess_suffix(ref: ObjectRef) -> str:
    if ref.object_key:
        key = ref.object_key
        if "." in key:
            return "." + key.rsplit(".", 1)[-1].lower()
    if ref.url:
        path = urlparse(ref.url).path
        if "." in path:
            return "." + path.rsplit(".", 1)[-1].lower()
    return ""


def _allow_truncate_for_suffix(suffix: str) -> bool:
    return suffix in {".csv", ".txt", ".md", ".json", ".log"}



def fetch_object(
    ref: ObjectRef,
    *,
    cache_dir: str | Path = "cache",
    max_bytes: int = 20 * 1024 * 1024,
    timeout_s: int = 60,
    oss_bucket: Optional[object] = None,
) -> FetchedObject:
    cache_dir = Path(cache_dir)
    cache_dir.mkdir(parents=True, exist_ok=True)
    cache_key = _safe_cache_name(ref)
    suffix = _guess_suffix(ref)
    cache_path = cache_dir / f"{cache_key}{suffix}"
    meta_path = cache_dir / f"{cache_key}.meta"

    if cache_path.exists():
        content_type = None
        if meta_path.exists():
            content_type = meta_path.read_text(encoding="utf-8", errors="ignore").strip() or None
        return FetchedObject(ref=ref, content_bytes=cache_path.read_bytes(), content_type=content_type)

    allow_truncate = _allow_truncate_for_suffix(suffix)

    if ref.url:
        try:
            r = requests.get(ref.url, stream=True, timeout=timeout_s)
            r.raise_for_status()
            content_type = r.headers.get("content-type")
            total = 0
            with cache_path.open("wb") as f:
                for chunk in r.iter_content(chunk_size=1024 * 256):
                    if not chunk:
                        continue
                    total += len(chunk)
                    if total > max_bytes:
                        if allow_truncate:
                            remaining = max(0, max_bytes - (total - len(chunk)))
                            if remaining:
                                f.write(chunk[:remaining])
                            break
                        raise ValueError(f"object too large (> {max_bytes} bytes): {ref.display_name}")
                    f.write(chunk)
            meta_path.write_text(content_type or "", encoding="utf-8")
            return FetchedObject(ref=ref, content_bytes=cache_path.read_bytes(), content_type=content_type)
        except requests.HTTPError as e:
            status = getattr(getattr(e, "response", None), "status_code", None)
            if status == 403 and oss_bucket is None and ref.object_key:
                raise ValueError(
                    "URL 访问 403（对象不支持匿名读），请在 .env 中配置 OSS_ENDPOINT/OSS_BUCKET/OSS_ACCESS_KEY_ID/OSS_ACCESS_KEY_SECRET（可选 OSS_STS_TOKEN）后重试"
                ) from e
            if oss_bucket is None:
                raise
        except Exception:
            if oss_bucket is None:
                raise

    if oss_bucket is not None and ref.object_key:
        object_key = unquote(ref.object_key)
        result = oss_bucket.get_object(object_key)
        content = result.read(max_bytes + 1)
        if len(content) > max_bytes:
            if allow_truncate:
                content = content[:max_bytes]
            else:
                raise ValueError(f"object too large (> {max_bytes} bytes): {ref.display_name}")
        cache_path.write_bytes(content)
        meta_path.write_text(getattr(result, "content_type", "") or "", encoding="utf-8")
        return FetchedObject(ref=ref, content_bytes=content, content_type=getattr(result, "content_type", None))

    raise ValueError(f"no way to fetch object: {ref.display_name}")

