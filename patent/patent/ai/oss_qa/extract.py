from __future__ import annotations

import csv
import io
import zipfile
from dataclasses import dataclass
from typing import Iterable
from urllib.parse import unquote

from xml.etree import ElementTree as ET

from oss_qa.fetchers import FetchedObject


@dataclass(frozen=True)
class ExtractedDoc:
    source: str
    url: str | None
    text: str


def _suffix_from_ref(f: FetchedObject) -> str:
    key = f.ref.object_key or ""
    if "." in key:
        return "." + key.rsplit(".", 1)[-1].lower()
    if f.ref.url and "." in f.ref.url:
        return "." + f.ref.url.rsplit(".", 1)[-1].lower()
    return ""


def _decode_text(b: bytes) -> str:
    try:
        return b.decode("utf-8")
    except UnicodeDecodeError:
        return b.decode("utf-8", errors="ignore")


def _extract_csv_text(b: bytes, *, max_rows: int = 5000, max_cols: int = 60) -> str:
    text = _decode_text(b)
    f = io.StringIO(text)
    reader = csv.reader(f)
    rows: list[list[str]] = []
    for i, row in enumerate(reader):
        if i >= max_rows:
            break
        rows.append([cell.strip() for cell in row[:max_cols]])
    if not rows:
        return ""
    header = rows[0]
    lines: list[str] = []
    for row in rows[1:]:
        parts = []
        for j, cell in enumerate(row):
            if j >= len(header):
                break
            h = header[j].strip() or f"col{j+1}"
            if cell:
                parts.append(f"{h}={cell}")
        if parts:
            lines.append(" | ".join(parts))
    return "\n".join(lines)


def _extract_docx_text(b: bytes) -> str:
    try:
        with zipfile.ZipFile(io.BytesIO(b)) as z:
            xml_bytes = z.read("word/document.xml")
    except Exception:
        return ""

    try:
        root = ET.fromstring(xml_bytes)
    except Exception:
        return ""

    w = "http://schemas.openxmlformats.org/wordprocessingml/2006/main"
    ns = {"w": w}
    lines: list[str] = []
    for p in root.findall(".//w:p", ns):
        texts = [t.text for t in p.findall(".//w:t", ns) if t.text]
        line = "".join(texts).strip()
        if line:
            lines.append(line)
    return "\n".join(lines)


def extract_text(f: FetchedObject) -> ExtractedDoc:
    suffix = _suffix_from_ref(f)
    source = unquote(f.ref.object_key) if f.ref.object_key else (f.ref.url or "unknown")

    if suffix == ".csv":
        text = _extract_csv_text(f.content_bytes)
    elif suffix == ".docx":
        text = _extract_docx_text(f.content_bytes)
    else:
        text = _decode_text(f.content_bytes)

    return ExtractedDoc(source=source, url=f.ref.url, text=text)


def extract_many(fetched: Iterable[FetchedObject]) -> list[ExtractedDoc]:
    docs: list[ExtractedDoc] = []
    for f in fetched:
        d = extract_text(f)
        if d.text.strip():
            docs.append(d)
    return docs

