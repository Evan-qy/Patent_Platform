# OSS 文件智能问答（通义千问 RAG）

基于 `export_urls.csv`（object,url 清单）从 OSS 拉取文件内容，做向量检索，并调用通义千问生成“带引用”的答案。

## 1. 安装

```bash
python -m venv .venv
.\.venv\Scripts\activate
python -m pip install -r requirements.txt
```

## 2. 配置（不要把 Key 写进代码）

在项目根目录创建 `.env`（参考 `.env.example`）：

- `DASHSCOPE_API_KEY`：通义千问 API Key
- 如果你的 URL 无法直接访问，才需要配置 OSS：
  - `OSS_ENDPOINT` / `OSS_BUCKET` / `OSS_ACCESS_KEY_ID` / `OSS_ACCESS_KEY_SECRET`（可选 `OSS_STS_TOKEN`）

## 3. 构建索引

默认读取根目录的 `export_urls.csv`，会把下载内容缓存到 `cache/`，索引写到 `index/`：

```bash
python -m oss_qa build-index --export-csv export_urls.csv
```

如果你看到 URL 访问 403/Forbidden，说明对象不支持匿名读，此时需要在 `.env` 里配置 OSS 凭证与桶信息，让程序通过 OSS SDK 拉取对象内容。

如果你看到 “object too large”，可以提高上限（单位：字节），例如 100MB：

```bash
python -m oss_qa build-index --export-csv export_urls.csv --max-bytes 104857600
```

如果你看到 DashScope 返回 429（Too Many Requests），可以降低并发/速率：

```bash
python -m oss_qa build-index --export-csv export_urls.csv --batch-size 4 --sleep 0.5 --max-chunks 1000
```

## 4. 检索与问答

检索（看命中的来源）：

```bash
python -m oss_qa search "氢能相关专利有哪些机构？" --top-k 6
```

问答（带引用编号）：

```bash
python -m oss_qa ask "项目计划书的目标是什么？" --top-k 6
```

## 5. 最小化自测
## 4.1 AI 聊天（连续对话）

先确保已经构建索引（见上面的 build-index），然后运行：

```bash
python -m oss_qa chat --index-dir index --top-k 6 --keep-turns 6
```

- 退出：输入 `/exit` 或 `/quit`
- `--keep-turns` 控制保留多少轮历史对话（越大上下文越长、请求越慢）
- 遇到 429（限流）：把 `--batch-size` 调小、把 `--sleep` 调大后重新 build-index


```bash
python -m unittest discover -s tests -p "test_*.py" -q
```


## 说明

- CSV 文件会按“表头=单元格”的形式转成可检索文本。
- DOCX 会抽取段落与表格文字。
- 默认分块为 1800 字符左右，适合多数中文文本的检索与问答场景。

