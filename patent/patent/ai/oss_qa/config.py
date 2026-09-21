from __future__ import annotations

import os
from dataclasses import dataclass


@dataclass(frozen=True)
class AppConfig:
    dashscope_api_key: str | None
    oss_endpoint: str | None
    oss_bucket: str | None
    oss_access_key_id: str | None
    oss_access_key_secret: str | None
    oss_sts_token: str | None


def _clean_env_value(v: str | None) -> str | None:
    if v is None:
        return None
    s = v.strip()
    return s or None


def load_config() -> AppConfig:
    dashscope_api_key = _clean_env_value(os.getenv("DASHSCOPE_API_KEY"))
    if dashscope_api_key in {"你的通义千问Key", "your_key", "YOUR_KEY"}:
        dashscope_api_key = None
    if dashscope_api_key is not None and (not dashscope_api_key.isascii()):
        dashscope_api_key = None

    return AppConfig(
        dashscope_api_key=dashscope_api_key,
        oss_endpoint=_clean_env_value(os.getenv("OSS_ENDPOINT")),
        oss_bucket=_clean_env_value(os.getenv("OSS_BUCKET")),
        oss_access_key_id=_clean_env_value(os.getenv("OSS_ACCESS_KEY_ID")),
        oss_access_key_secret=_clean_env_value(os.getenv("OSS_ACCESS_KEY_SECRET")),
        oss_sts_token=_clean_env_value(os.getenv("OSS_STS_TOKEN")),
    )

