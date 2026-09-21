from __future__ import annotations

from dataclasses import dataclass
import time
import sys
from typing import Any, Iterable

import requests


@dataclass(frozen=True)
class QwenClient:
    api_key: str
    base_url: str = "https://dashscope.aliyuncs.com/compatible-mode/v1"

    def _headers(self) -> dict[str, str]:
        if not self.api_key or not isinstance(self.api_key, str):
            raise ValueError("missing DASHSCOPE_API_KEY")
        if not self.api_key.isascii():
            raise ValueError("invalid DASHSCOPE_API_KEY: contains non-ASCII characters")
        return {"Authorization": f"Bearer {self.api_key}", "Content-Type": "application/json"}

    def _post_json_with_retry(self, url: str, payload: dict[str, Any], *, timeout: int = 120) -> dict[str, Any]:
        last_err: Exception | None = None
        for attempt in range(1, 7):
            try:
                r = requests.post(url, headers=self._headers(), json=payload, timeout=timeout)
                if r.status_code in {429, 500, 502, 503, 504}:
                    retry_after = r.headers.get("retry-after")
                    if retry_after:
                        try:
                            wait_s = float(retry_after)
                        except Exception:
                            wait_s = min(60.0, 2.0**attempt)
                    else:
                        wait_s = min(60.0, 2.0**attempt)
                    print(f"DashScope HTTP {r.status_code}, retrying in {wait_s:.1f}s (attempt {attempt}/6)", file=sys.stderr)
                    time.sleep(wait_s)
                    continue
                r.raise_for_status()
                return r.json()
            except requests.RequestException as e:
                last_err = e
                wait_s = min(60.0, 2.0**attempt)
                print(f"DashScope request error, retrying in {wait_s:.1f}s (attempt {attempt}/6)", file=sys.stderr)
                time.sleep(wait_s)
        if last_err is not None:
            raise last_err
        raise RuntimeError("request failed")

    def embeddings(self, texts: list[str], *, model: str = "text-embedding-v2") -> list[list[float]]:
        url = f"{self.base_url}/embeddings"
        payload = {"model": model, "input": texts}
        data = self._post_json_with_retry(url, payload, timeout=120)
        items = data.get("data") or []
        items = sorted(items, key=lambda x: x.get("index", 0))
        return [it["embedding"] for it in items]

    def chat(
        self,
        messages: list[dict[str, str]],
        *,
        model: str = "qwen-plus",
        temperature: float = 0.2,
    ) -> str:
        url = f"{self.base_url}/chat/completions"
        payload = {"model": model, "messages": messages, "temperature": temperature}
        data = self._post_json_with_retry(url, payload, timeout=120)
        choices = data.get("choices") or []
        if not choices:
            raise ValueError(f"unexpected chat response: {data}")
        msg = choices[0].get("message") or {}
        content = msg.get("content")
        if not content:
            raise ValueError(f"unexpected chat response: {data}")
        return content


def batch_iter(items: list[str], batch_size: int) -> Iterable[list[str]]:
    if batch_size <= 0:
        raise ValueError("batch_size must be > 0")
    for i in range(0, len(items), batch_size):
        yield items[i : i + batch_size]

