import hashlib
import json
import re
from pathlib import Path

KANJI_RE = re.compile(r"[\u4E00-\u9FAF\u3400-\u4DBF]")
TOKEN_RE = re.compile(r"([^\s\[\]]+)\[([^\[\]]+)\]")
PUNCT_SPLIT_RE = re.compile(r"([。、「」！？…\n])")
NORM_STRIP_RE = re.compile(r"[\s\W。、「」！？…・（）『』【】]+", re.UNICODE)


def parse_annotated_text(raw: str, dict_lookup: dict[str, str]) -> list[dict]:
    """
    Parses annotated Japanese text like:
      '私[わたし]は学生[がくせい]です。\n明日[あした]、学校[がっこう]へ行[い]きます。'
    into paragraphs -> sentences -> segments.
    """
    paragraphs = []
    for para_str in [p.strip() for p in raw.strip().split("\n\n") if p.strip()]:
        sentences = []
        # Split into sentences by 。！？ while keeping delimiter
        raw_sents = re.split(r"(?<=[。！？])", para_str.replace("\n", ""))
        for sent_str in [s.strip() for s in raw_sents if s.strip()]:
            segments = parse_segments(sent_str, dict_lookup)
            if segments:
                sentences.append({"segments": segments})
        if sentences:
            paragraphs.append({"sentences": sentences})
    return paragraphs


def parse_segments(sentence: str, dict_lookup: dict[str, str]) -> list[dict]:
    segments = []
    pos = 0
    # Match kanji+reading tokens: e.g. 勉強[べんきょう] or 行[い]
    # Note: we match the shortest sequence before '[' that contains kanji or word chars
    pattern = re.compile(r"([\u4E00-\u9FAF\u3400-\u4DBF々ヶ〇a-zA-Z0-9]+)\[([^\[\]]+)\]")
    for m in pattern.finditer(sentence):
        if m.start() > pos:
            plain = sentence[pos:m.start()]
            if KANJI_RE.search(plain):
                raise ValueError(f"Unannotated kanji found in plain segment: '{plain}' in '{sentence}'")
            segments.extend(split_plain_with_dict(plain, dict_lookup))
        word = m.group(1)
        reading = m.group(2)
        dict_id = dict_lookup.get(word)
        seg = {"text": word, "reading": reading}
        if dict_id:
            seg["dict_id"] = dict_id
        segments.append(seg)
        pos = m.end()

    if pos < len(sentence):
        tail = sentence[pos:]
        if KANJI_RE.search(tail):
            raise ValueError(f"Unannotated kanji found in tail segment: '{tail}' in '{sentence}'")
        segments.extend(split_plain_with_dict(tail, dict_lookup))

    return segments


def split_plain_with_dict(plain: str, dict_lookup: dict[str, str]) -> list[dict]:
    if not plain:
        return []
    return [{"text": plain, "reading": None}]


def normalize_text(raw_plain: str) -> str:
    return re.sub(r"[\s。、「」！？…・（）『』【】\.,!?]+", "", raw_plain).strip()


def text_similarity(a: str, b: str) -> float:
    if not a and not b:
        return 1.0
    if len(a) < 3 or len(b) < 3:
        return 1.0 if a == b else 0.0
    grams_a = {a[i : i + 3] for i in range(len(a) - 2)}
    grams_b = {b[i : i + 3] for i in range(len(b) - 2)}
    inter = len(grams_a & grams_b)
    union = len(grams_a | grams_b)
    return inter / union if union else 0.0


def build_material(
    mat_id: str,
    order_index: int,
    level: str,
    category: str,
    title: str,
    description: str,
    body_annotated: str,
    questions: list[dict],
    dict_lookup: dict[str, str],
) -> dict:
    paragraphs = parse_annotated_text(body_annotated, dict_lookup)
    return {
        "id": mat_id,
        "order_index": order_index,
        "level": level,
        "category": category,
        "title": title,
        "description": description,
        "content_type": "original",
        "source": "Yomizora Editorial Team",
        "author": "Yomizora",
        "license": "CC-BY-4.0 / Original",
        "created_at": "2026-11-18",
        "version": 1,
        "paragraphs": paragraphs,
        "questions": questions,
    }


def validate_pack(materials: list[dict]):
    seen_ids = set()
    seen_hashes = {}
    seen_titles = set()
    normalized_texts = []

    for m in materials:
        mid = m["id"]
        assert mid not in seen_ids, f"Duplicate id: {mid}"
        seen_ids.add(mid)

        title_key = (m["level"], m["title"])
        assert title_key not in seen_titles, f"Duplicate title: {title_key}"
        seen_titles.add(title_key)

        plain = "".join(
            seg["text"]
            for p in m["paragraphs"]
            for s in p["sentences"]
            for seg in s["segments"]
        )
        norm = normalize_text(plain)
        h = hashlib.sha256(norm.encode("utf-8")).hexdigest()
        assert h not in seen_hashes, f"Duplicate text: {mid} == {seen_hashes[h]}"
        seen_hashes[h] = mid

        for other_id, other_norm in normalized_texts:
            sim = text_similarity(norm, other_norm)
            assert sim < 0.85, f"Near-duplicate text: {mid} ~ {other_id} (sim={sim:.2f})"
        normalized_texts.append((mid, norm))

        qs = m["questions"]
        assert 1 <= len(qs) <= 3, f"Invalid question count in {mid}: {len(qs)}"
        for q in qs:
            assert 3 <= len(q["options"]) <= 4, f"Invalid options count in {mid}/{q['id']}"
            assert 0 <= q["correct_answer"] < len(q["options"]), f"Invalid correct_answer in {mid}/{q['id']}"
            assert q["explanation"].strip(), f"Empty explanation in {mid}/{q['id']}"
