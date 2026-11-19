import json
import re
from pathlib import Path

from stem_to_dict_map import STEM_TO_FULL

ROOT = Path(__file__).resolve().parent.parent
N5_PATH = ROOT / "app" / "src" / "main" / "assets" / "content" / "01_n5_pack.json"
N4_PATH = ROOT / "app" / "src" / "main" / "assets" / "content" / "02_n4_pack.json"
ADDON_PATH = ROOT / "examples" / "yomizora_addon_n5_n4_v1.json"

GRAMMAR_PATTERNS = [
    r"てみ(る|ます|ました|て|た)",
    r"[てで]しま(う|います|いました|った)",
    r"[てで]お(く|きます|いた|いて)",
    r"ようにし(て|ます|た)",
    r"ようになり(ます|ました|った)",
    r"つもり(です|だ)",
    r"かもしれ(ない|ません)",
    r"そう(です|だ|な|に)",
    r"(やすい|やすく|にくい|にくく)",
    r"[てで]くれ(る|ます|ました|た)",
    r"ので",
    r"のに",
    r"なければなら(ない|ません)",
    r"[ただ]り",
    r"[てで]はいけ(ない|ません)",
    r"たい(です|と|か)",
    r"てから",
    r"ながら",
    r"ため",
    r"おかげで",
    r"せいで",
    r"ことに(なる|なりました|する|しました)",
]
GRAMMAR_REGEXES = [re.compile(p) for p in GRAMMAR_PATTERNS]
KANJI_RE = re.compile(r"[\u4E00-\u9FAF\u3400-\u4DBF]")


def complexity_score(mat: dict) -> tuple[float, int, str]:
    sentences = [s for p in mat["paragraphs"] for s in p["sentences"]]
    plain_sents = ["".join(seg["text"] for seg in s["segments"]) for s in sentences]
    full_text = "".join(plain_sents)

    total_chars = len(full_text)
    avg_sent_len = total_chars / max(len(plain_sents), 1)

    unique_kanji = set(KANJI_RE.findall(full_text))
    unique_dict_ids = {
        seg["dict_id"]
        for s in sentences
        for seg in s["segments"]
        if seg.get("dict_id")
    }
    grammar_hits = sum(1 for r in GRAMMAR_REGEXES if r.search(full_text))
    q_count = len(mat.get("questions", []))

    score = (
        total_chars * 0.25
        + avg_sent_len * 1.4
        + len(unique_kanji) * 1.8
        + len(unique_dict_ids) * 1.1
        + grammar_hits * 8.0
        + q_count * 2.0
    )
    return (round(score, 2), total_chars, mat["id"])


def main():
    n5_data = json.loads(N5_PATH.read_text(encoding="utf-8"))
    updated_dict_count = 0
    for entry in n5_data["dictionary"]:
        if entry["id"] in STEM_TO_FULL:
            full_word, full_reading, clean_pos = STEM_TO_FULL[entry["id"]]
            entry["word"] = full_word
            entry["reading"] = full_reading
            entry["part_of_speech"] = clean_pos
            entry["note"] = None
            updated_dict_count += 1

    # Sort N5 by ascending complexity
    n5_mats = sorted(n5_data["materials"], key=complexity_score)
    for idx, m in enumerate(n5_mats, start=1):
        m["order_index"] = idx
        m["version"] = 2
    n5_data["materials"] = n5_mats
    n5_data["pack_version"] = 4
    N5_PATH.write_text(json.dumps(n5_data, ensure_ascii=False, indent=2), encoding="utf-8")

    # Sort N4 by ascending complexity
    n4_data = json.loads(N4_PATH.read_text(encoding="utf-8"))
    n4_mats = sorted(n4_data["materials"], key=complexity_score)
    for idx, m in enumerate(n4_mats, start=1):
        m["order_index"] = idx
        m["version"] = 2
    n4_data["materials"] = n4_mats
    n4_data["pack_version"] = 4
    N4_PATH.write_text(json.dumps(n4_data, ensure_ascii=False, indent=2), encoding="utf-8")

    # Sort Addon N5 & N4 by ascending complexity
    addon_data = json.loads(ADDON_PATH.read_text(encoding="utf-8"))
    addon_n5 = sorted([m for m in addon_data["materials"] if m["level"] == "N5"], key=complexity_score)
    addon_n4 = sorted([m for m in addon_data["materials"] if m["level"] == "N4"], key=complexity_score)
    for idx, m in enumerate(addon_n5, start=56):
        m["order_index"] = idx
        m["version"] = 2
    for idx, m in enumerate(addon_n4, start=56):
        m["order_index"] = idx
        m["version"] = 2
    addon_data["materials"] = addon_n5 + addon_n4
    addon_data["pack_version"] = 4
    ADDON_PATH.write_text(json.dumps(addon_data, ensure_ascii=False, indent=2), encoding="utf-8")

    print(f"Upgraded {updated_dict_count} stem dictionary entries to full dictionary forms.")
    print("N5 easiest -> hardest:", n5_mats[0]["title"], complexity_score(n5_mats[0])[0], "->", n5_mats[-1]["title"], complexity_score(n5_mats[-1])[0])
    print("N4 easiest -> hardest:", n4_mats[0]["title"], complexity_score(n4_mats[0])[0], "->", n4_mats[-1]["title"], complexity_score(n4_mats[-1])[0])


if __name__ == "__main__":
    main()
