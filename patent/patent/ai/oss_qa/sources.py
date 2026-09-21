from __future__ import annotations

import csv
from dataclasses import dataclass
from pathlib import Path
from typing import Iterable
from urllib.parse import unquote


@dataclass(frozen=True)
class ObjectRef:
    object_key: str
    url: str | None

    @property
    def display_name(self) -> str:
        return unquote(self.object_key)


def read_export_urls_csv(csv_path: str | Path) -> list[ObjectRef]:
    csv_path = Path(csv_path)
    with csv_path.open("r", encoding="utf-8-sig", newline="") as f:
        reader = csv.DictReader(f)
        refs: list[ObjectRef] = []
        for row in reader:
            object_key = (row.get("object") or "").strip()
            url = (row.get("url") or "").strip() or None
            if not object_key and not url:
                continue
            refs.append(ObjectRef(object_key=object_key, url=url))
        return refs


def iter_refs_from_csvs(csv_paths: Iterable[str | Path]) -> list[ObjectRef]:
    merged: list[ObjectRef] = []
    for p in csv_paths:
        merged.extend(read_export_urls_csv(p))
    return merged

