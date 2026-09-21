import io
import unittest
import zipfile

from oss_qa.chunking import chunk_text
from oss_qa.extract import extract_text
from oss_qa.fetchers import FetchedObject
from oss_qa.sources import ObjectRef


class TestBasic(unittest.TestCase):
    def test_display_name_decodes(self) -> None:
        ref = ObjectRef(object_key="patent%2Fa.csv", url=None)
        self.assertEqual(ref.display_name, "patent/a.csv")

    def test_csv_extract(self) -> None:
        content = "a,b\n1,2\n".encode("utf-8")
        f = FetchedObject(ref=ObjectRef(object_key="x.csv", url=None), content_bytes=content, content_type=None)
        doc = extract_text(f)
        self.assertIn("a=1", doc.text)
        self.assertIn("b=2", doc.text)

    def test_docx_extract_minimal(self) -> None:
        w = "http://schemas.openxmlformats.org/wordprocessingml/2006/main"
        xml = (
            f'<?xml version="1.0" encoding="UTF-8" standalone="yes"?>'
            f'<w:document xmlns:w="{w}"><w:body>'
            f"<w:p><w:r><w:t>你好</w:t></w:r></w:p>"
            f"<w:p><w:r><w:t>世界</w:t></w:r></w:p>"
            f"</w:body></w:document>"
        ).encode("utf-8")

        buf = io.BytesIO()
        with zipfile.ZipFile(buf, "w", compression=zipfile.ZIP_DEFLATED) as z:
            z.writestr("word/document.xml", xml)
        f = FetchedObject(ref=ObjectRef(object_key="a.docx", url=None), content_bytes=buf.getvalue(), content_type=None)
        doc = extract_text(f)
        self.assertIn("你好", doc.text)
        self.assertIn("世界", doc.text)

    def test_chunking(self) -> None:
        text = "a" * 5000
        chunks = chunk_text("s", None, text, chunk_size=1000, overlap=100)
        self.assertGreater(len(chunks), 1)


if __name__ == "__main__":
    unittest.main()

