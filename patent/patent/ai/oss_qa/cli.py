from __future__ import annotations

import argparse
import json
import os
import sys
from pathlib import Path
from typing import Any

try:
    from dotenv import load_dotenv
except Exception:
    load_dotenv = None

from oss_qa.config import load_config
from oss_qa.index import answer_with_rag, build_index_from_export_csv, search_index
from oss_qa.qwen_api import QwenClient


def _load_dotenv_fallback(dotenv_path: str | Path = ".env") -> None:
    p = Path(dotenv_path)
    if not p.exists() or not p.is_file():
        return
    for raw in p.read_text(encoding="utf-8", errors="ignore").splitlines():
        line = raw.strip()
        if not line or line.startswith("#"):
            continue
        if "=" not in line:
            continue
        k, v = line.split("=", 1)
        k = k.strip()
        v = v.strip().strip('"').strip("'")
        if not k:
            continue
        if k not in os.environ:
            os.environ[k] = v


def _trim_history(messages: list[dict[str, str]], keep_turns: int) -> list[dict[str, str]]:
    if keep_turns <= 0:
        return []
    pairs: list[tuple[dict[str, str], dict[str, str]]] = []
    i = 0
    while i + 1 < len(messages):
        if messages[i].get("role") == "user" and messages[i + 1].get("role") == "assistant":
            pairs.append((messages[i], messages[i + 1]))
            i += 2
        else:
            i += 1
    kept = pairs[-keep_turns:]
    out: list[dict[str, str]] = []
    for u, a in kept:
        out.append(u)
        out.append(a)
    return out


def _chat_turn(
    cfg: Any,
    index_dir: str,
    question: str,
    *,
    history: list[dict[str, str]],
    top_k: int,
    keep_turns: int,
    model_chat: str,
    model_embed: str,
) -> tuple[str, list[dict[str, str]]]:
    hits = search_index(cfg, index_dir, question, top_k=top_k, model_embed=model_embed)
    blocks: list[str] = []
    for i, (_, c) in enumerate(hits, start=1):
        meta = f"来源={c.source}"
        if c.url:
            meta += f" | url={c.url}"
        blocks.append(f"[资料{i}] {meta}\n{c.text}")
    context = "\n\n".join(blocks)

    messages: list[dict[str, str]] = [
        {
            "role": "system",
            "content": "你是一个严谨的检索增强问答助手。只基于用户提供的资料回答，不要编造；资料不足就明确说明。",
        }
    ]
    messages.extend(_trim_history(history, keep_turns))
    messages.append(
        {
            "role": "user",
            "content": f"问题：{question}\n\n资料：\n{context}\n\n要求：给出答案，并在关键结论处引用对应[资料编号]。",
        }
    )
    client = QwenClient(cfg.dashscope_api_key)
    ans = client.chat(messages, model=model_chat, temperature=0.2)
    history = history + [{"role": "user", "content": question}, {"role": "assistant", "content": ans}]
    return ans, history


def _build_arg_parser() -> argparse.ArgumentParser:
    p = argparse.ArgumentParser(prog="oss-qa")
    sub = p.add_subparsers(dest="cmd", required=True)

    d = sub.add_parser("doctor")
    d.add_argument("--dotenv", default=".env")

    b = sub.add_parser("build-index")
    b.add_argument("--export-csv", default="export_urls.csv")
    b.add_argument("--out-dir", default="index")
    b.add_argument("--cache-dir", default="cache")
    b.add_argument("--max-files", type=int, default=None)
    b.add_argument("--max-bytes", type=int, default=20 * 1024 * 1024)
    b.add_argument("--batch-size", type=int, default=8)
    b.add_argument("--max-chunks", type=int, default=2000)
    b.add_argument("--sleep", type=float, default=0.2)

    s = sub.add_parser("search")
    s.add_argument("query")
    s.add_argument("--index-dir", default="index")
    s.add_argument("--top-k", type=int, default=6)

    a = sub.add_parser("ask")
    a.add_argument("question")
    a.add_argument("--index-dir", default="index")
    a.add_argument("--top-k", type=int, default=6)

    c = sub.add_parser("chat")
    c.add_argument("--index-dir", default="index")
    c.add_argument("--top-k", type=int, default=6)
    c.add_argument("--keep-turns", type=int, default=6)
    c.add_argument("--model-chat", default="qwen-plus")
    c.add_argument("--model-embed", default="text-embedding-v2")
    return p


def main(argv: list[str] | None = None) -> int:
    args = _build_arg_parser().parse_args(argv)
    dotenv_path = getattr(args, "dotenv", ".env")
    if load_dotenv is not None:
        load_dotenv(dotenv_path=dotenv_path)
    else:
        _load_dotenv_fallback(dotenv_path)
    cfg = load_config()

    try:
        if args.cmd == "doctor":
            p = Path(dotenv_path).resolve()
            key = os.getenv("DASHSCOPE_API_KEY") or ""
            print(f"dotenv_path: {p} (exists={p.exists()})")
            print(f"DASHSCOPE_API_KEY set: {bool(key)}")
            print(f"DASHSCOPE_API_KEY len: {len(key)}")
            print(f"DASHSCOPE_API_KEY isascii: {key.isascii()}")
            print(f"OSS_BUCKET set: {bool(os.getenv('OSS_BUCKET'))}")
            print(f"OSS_ENDPOINT set: {bool(os.getenv('OSS_ENDPOINT'))}")
            print(f"OSS_ACCESS_KEY_ID set: {bool(os.getenv('OSS_ACCESS_KEY_ID'))}")
            print(f"OSS_ACCESS_KEY_SECRET set: {bool(os.getenv('OSS_ACCESS_KEY_SECRET'))}")
            print(f"config.dashscope_api_key: {bool(cfg.dashscope_api_key)}")
            print(f"config.oss_bucket: {bool(cfg.oss_bucket)}")
            return 0

        if args.cmd == "build-index":
            build_index_from_export_csv(
                cfg,
                args.export_csv,
                out_dir=args.out_dir,
                cache_dir=args.cache_dir,
                max_files=args.max_files,
                max_bytes=args.max_bytes,
                batch_size=args.batch_size,
                max_chunks=args.max_chunks,
                sleep_s=args.sleep,
            )
            print(f"index built at: {Path(args.out_dir).resolve()}")
            return 0

        if args.cmd == "search":
            hits = search_index(cfg, args.index_dir, args.query, top_k=args.top_k)
            payload = [
                {"score": float(score), "source": c.source, "url": c.url, "chunk_id": c.chunk_id}
                for score, c in hits
            ]
            print(json.dumps(payload, ensure_ascii=False, indent=2))
            return 0

        if args.cmd == "ask":
            ans = answer_with_rag(cfg, args.index_dir, args.question, top_k=args.top_k)
            print(ans)
            return 0

        if args.cmd == "chat":
            if not cfg.dashscope_api_key:
                raise ValueError("missing DASHSCOPE_API_KEY")
            history: list[dict[str, str]] = []
            print("进入聊天模式：输入问题回车；输入 /exit 或 /quit 退出。")
            while True:
                try:
                    q = input("你> ").strip()
                except (EOFError, KeyboardInterrupt):
                    print()
                    break
                if not q:
                    continue
                if q.lower() in {"/exit", "/quit"}:
                    break
                ans, history = _chat_turn(
                    cfg,
                    args.index_dir,
                    q,
                    history=history,
                    top_k=args.top_k,
                    keep_turns=args.keep_turns,
                    model_chat=args.model_chat,
                    model_embed=args.model_embed,
                )
                print(f"AI> {ans}\n")
            return 0

        raise ValueError(f"unknown cmd: {args.cmd}")
    except ValueError as e:
        if str(e) == "missing DASHSCOPE_API_KEY":
            print(
                "缺少 DASHSCOPE_API_KEY。\n"
                "解决办法：在项目根目录的 .env 中设置 DASHSCOPE_API_KEY=你的通义千问Key，并保存文件后重试。\n"
                "也可以直接在终端里临时设置：\n"
                "  $env:DASHSCOPE_API_KEY='你的Key'\n",
                file=sys.stderr,
            )
            return 2
        if str(e) == "invalid DASHSCOPE_API_KEY: contains non-ASCII characters":
            print(
                "DASHSCOPE_API_KEY 不合法：包含非 ASCII 字符（通常是把占位符“你的通义千问Key”当成真实 Key 了）。\n"
                "请在 .env 中把 DASHSCOPE_API_KEY=... 替换为你真实的 sk-... Key，然后重试。\n",
                file=sys.stderr,
            )
            return 2
        raise


if __name__ == "__main__":
    raise SystemExit(main())

