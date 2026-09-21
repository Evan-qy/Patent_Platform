from __future__ import annotations

from typing import Optional

import oss2

from oss_qa.config import AppConfig


def build_oss_bucket(cfg: AppConfig) -> Optional[oss2.Bucket]:
    if not (cfg.oss_endpoint and cfg.oss_bucket and cfg.oss_access_key_id and cfg.oss_access_key_secret):
        return None
    endpoint = cfg.oss_endpoint.strip()
    if not (endpoint.startswith("http://") or endpoint.startswith("https://")):
        endpoint = "https://" + endpoint
    auth = oss2.Auth(cfg.oss_access_key_id, cfg.oss_access_key_secret)
    if cfg.oss_sts_token:
        auth = oss2.StsAuth(cfg.oss_access_key_id, cfg.oss_access_key_secret, cfg.oss_sts_token)
    return oss2.Bucket(auth, endpoint, cfg.oss_bucket)

