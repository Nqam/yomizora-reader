#!/usr/bin/env python3
"""
Resegments all Yomizora content packs using Janome morphological analysis:
1. Makes almost every content word clickable (kanji+okurigana AND hiragana/katakana),
   while leaving particles (助詞), standalone copulas, and punctuation non-clickable.
2. Shares the same dict_id across conjugated verb/adjective stems and their okurigana/auxiliaries
   (e.g. 見[み] + ました -> both have dict_id='d_miru') so FuriganaParagraph highlights
   and taps the whole word while keeping furigana strictly over kanji.
3. Preserves exact plainText() byte-for-byte on every material so user progress is never reset.
"""

import hashlib
import json
import os
import sys
from pathlib import Path
from janome.tokenizer import Tokenizer

ROOT = Path(__file__).resolve().parent.parent
MISSING_RU_PATHS = [
    Path(
        r"C:\Users\am200\.gemini\antigravity\brain\b1dc824a-314c-4c02-ab70-90ecec894781\scratch\missing_words_ru.json"
    ),
    Path(
        r"C:\Users\am200\.gemini\antigravity\brain\b1dc824a-314c-4c02-ab70-90ecec894781\scratch\missing_kana_words_ru.json"
    ),
    Path(
        r"C:\Users\am200\.gemini\antigravity\brain\b1dc824a-314c-4c02-ab70-90ecec894781\scratch\missing_va_ru_1.json"
    ),
    Path(
        r"C:\Users\am200\.gemini\antigravity\brain\b1dc824a-314c-4c02-ab70-90ecec894781\scratch\missing_va_ru_2.json"
    ),
]

PACK_FILES = [
    ROOT / "app/src/main/assets/content/01_n5_pack.json",
    ROOT / "app/src/main/assets/content/02_n4_pack.json",
    ROOT / "content_updates/yomizora_addon_n5_n4_v1.json",
    ROOT / "content_updates/yomizora_addon_n5_n4_v2.json",
    ROOT / "content_updates/yomizora_pack_n3_n2_n1_v1.json",
    ROOT / "content_updates/yomizora_pack_n3_n2_n1_v2.json",
]

DICT_SOURCES = [
    ROOT / "app/src/main/assets/content/01_n5_pack.json",
    ROOT / "content_updates/yomizora_addon_n5_n4_v2.json",
    ROOT / "content_updates/yomizora_pack_n3_n2_n1_v1.json",
    ROOT / "content_updates/yomizora_pack_n3_n2_n1_v2.json",
]

AUX_CONJ_VERBS = {
    "いる", "いく", "くる", "みる", "おく", "しまう", "ある",
    "くださる", "あげる", "もらう", "くれる", "おる",
}

PUNCT_CHARS = set("。、「」！？・（）()…―〜ー・\n\r\t 　")


def kata_to_hira(s: str) -> str:
    return "".join(chr(ord(c) - 0x60) if "ァ" <= c <= "ヶ" else c for c in s)


def make_dict_id(word: str, reading: str) -> str:
    h = hashlib.sha1(f"{word}|{reading}".encode("utf-8")).hexdigest()[:10]
    return f"dh_{h}"


def load_dictionaries():
    by_word_read = {}
    by_word = {}
    by_read = {}

    def add_entry(e):
        w = e["word"]
        r = e.get("reading") or ""
        eid = e["id"]
        by_word_read.setdefault((w, r), eid)
        by_word.setdefault(w, eid)
        if r:
            by_read.setdefault(r, eid)

    for p in DICT_SOURCES:
        with open(p, encoding="utf-8") as f:
            data = json.load(f)
        for e in data.get("dictionary", []):
            add_entry(e)

    new_entries = []
    for ru_path in MISSING_RU_PATHS:
        if not ru_path.exists():
            continue
        with open(ru_path, encoding="utf-8") as f:
            missing_list = json.load(f)
        for item in missing_list:
            w = item["word"]
            r = item.get("reading") or ""
            if w in by_word:
                continue
            eid = make_dict_id(w, r)
            entry = {
                "id": eid,
                "word": w,
                "reading": r,
                "translation_ru": item["translation_ru"],
                "part_of_speech": item.get("part_of_speech", "слово"),
                "note": item.get("note"),
            }
            add_entry(entry)
            new_entries.append(entry)

    return by_word_read, by_word, by_read, new_entries


def tokenize_with_offsets(tok: Tokenizer, text: str):
    tokens = []
    idx = 0
    for t in tok.tokenize(text):
        n = len(t.surface)
        pos = t.part_of_speech.split(",")
        base = t.base_form if t.base_form != "*" else t.surface
        read_hira = kata_to_hira(t.reading) if t.reading != "*" else ""
        tokens.append({
            "start": idx,
            "end": idx + n,
            "surface": t.surface,
            "pos": pos,
            "base": base,
            "read": read_hira,
        })
        idx += n
    assert idx == len(text), f"Offset mismatch: {idx} != {len(text)}"
    return tokens


def coalesce_original_reading_spans(tokens, segments):
    """Ensure tokens never split an original segment that has reading != None."""
    span_bounds = []
    pos = 0
    for s in segments:
        n = len(s["text"])
        if s.get("reading"):
            span_bounds.append((pos, pos + n, s["text"]))
        pos += n

    if not span_bounds:
        return tokens

    result = []
    i = 0
    sp_idx = 0
    while i < len(tokens):
        t = tokens[i]
        while sp_idx < len(span_bounds) and span_bounds[sp_idx][1] <= t["start"]:
            sp_idx += 1
        if sp_idx < len(span_bounds):
            sp_start, sp_end, sp_text = span_bounds[sp_idx]
            if t["start"] >= sp_start and t["start"] < sp_end:
                # Merge all tokens until end >= sp_end
                merged_toks = [t]
                while merged_toks[-1]["end"] < sp_end and i + 1 < len(tokens):
                    i += 1
                    merged_toks.append(tokens[i])
                last = merged_toks[-1]
                result.append({
                    "start": merged_toks[0]["start"],
                    "end": last["end"],
                    "surface": "".join(x["surface"] for x in merged_toks),
                    "pos": last["pos"],
                    "base": sp_text if len(merged_toks) > 1 else last["base"],
                    "read": "".join(x["read"] for x in merged_toks),
                })
                i += 1
                continue
        result.append(t)
        i += 1
    return result


def is_content_token(t):
    pos0, pos1 = t["pos"][0], t["pos"][1]
    if pos0 in ("助詞", "助動詞", "記号", "フィラー", "接頭詞"):
        return False
    if pos1 in ("接尾", "数"):
        return False
    if pos1 == "非自立" and t["base"] in ("の", "ん", "よう", "はず", "わけ", "つもり"):
        return False
    if all(c in PUNCT_CHARS for c in t["surface"]):
        return False
    return True


def group_tokens(tokens):
    """Group janome tokens into whole-word units."""
    groups = []
    i = 0
    n = len(tokens)
    while i < n:
        t = tokens[i]
        pos0, pos1 = t["pos"][0], t["pos"][1]

        # Honorific prefix お/ご + following noun -> merge
        if pos0 == "接頭詞" and i + 1 < n and tokens[i + 1]["pos"][0] == "名詞":
            nxt = tokens[i + 1]
            groups.append({
                "start": t["start"],
                "end": nxt["end"],
                "base": t["surface"] + nxt["base"],
                "surface": t["surface"] + nxt["surface"],
                "is_word": True,
                "pos0": "名詞",
            })
            i += 2
            continue

        if not is_content_token(t):
            groups.append({
                "start": t["start"],
                "end": t["end"],
                "base": t["base"],
                "surface": t["surface"],
                "is_word": False,
                "pos0": pos0,
            })
            i += 1
            continue

        # Start a content word group
        w_start = t["start"]
        w_end = t["end"]
        w_base = t["base"]
        w_pos0 = pos0
        w_pos1 = pos1
        j = i + 1

        # Absorb suffixes / conjugations / auxiliary chains
        while j < n:
            nxt = tokens[j]
            npos0, npos1 = nxt["pos"][0], nxt["pos"][1]

            # Noun suffixes (たち, ら, さ, み)
            if npos0 == "名詞" and npos1 == "接尾" and nxt["surface"] in ("たち", "ら", "さ", "み"):
                w_end = nxt["end"]
                j += 1
                continue

            # Na-adjective stem + な/だ/に? Merge な/だ (not に, which is particle)
            if w_pos0 == "名詞" and w_pos1 == "形容動詞語幹":
                if npos0 == "助動詞" and nxt["base"] in ("だ", "です") and nxt["surface"] in ("な", "だ", "で"):
                    w_end = nxt["end"]
                    j += 1
                    continue
                break

            # Suru-noun + する (and its auxiliaries)
            if w_pos0 == "名詞" and w_pos1 == "サ変接続":
                if npos0 == "動詞" and npos1 == "自立" and nxt["base"] == "する":
                    w_end = nxt["end"]
                    w_pos0 = "動詞"
                    w_pos1 = "自立"
                    j += 1
                    continue
                break

            # Verbs and i-adjectives absorb inflectional tails
            if w_pos0 in ("動詞", "形容詞"):
                if npos0 == "動詞" and npos1 == "接尾":
                    w_end = nxt["end"]
                    j += 1
                    continue
                if npos0 == "助動詞":
                    # Stop before standalone copula だ/です after past/adj
                    if nxt["base"] in ("だ", "です"):
                        break
                    w_end = nxt["end"]
                    j += 1
                    continue
                # て/で + auxiliary verb (ている, ていた, ています, てしまう, etc.)
                if npos0 == "助詞" and nxt["pos"][1] == "接続助詞" and nxt["surface"] in ("て", "で"):
                    # Guard against janome mis-parsing て+いただけ as て+い+た+だけ
                    is_itadake = (
                        j + 3 < n
                        and tokens[j + 1]["surface"] == "い"
                        and tokens[j + 2]["surface"] == "た"
                        and tokens[j + 3]["surface"].startswith("だけ")
                    )
                    if not is_itadake and j + 1 < n and tokens[j + 1]["pos"][0] == "動詞" and tokens[j + 1]["pos"][1] == "非自立" and tokens[j + 1]["base"] in AUX_CONJ_VERBS:
                        w_end = tokens[j + 1]["end"]
                        j += 2
                        continue
                    w_end = nxt["end"]
                    j += 1
                    continue
                break

            break

        groups.append({
            "start": w_start,
            "end": w_end,
            "base": w_base,
            "surface": "".join(tokens[k]["surface"] for k in range(i, j)),
            "is_word": True,
            "pos0": w_pos0,
        })
        i = j

    return groups


def resolve_dict_id(group, orig_char_dict, by_word_read, by_word, by_read):
    base = group["base"]
    surf = group["surface"]
    # 1. Prefer exact dictionary match for the full lemma / surface (fixes 変 -> 変わる, 知 -> 知る, etc.)
    if base in by_word:
        return by_word[base]
    if surf in by_word:
        return by_word[surf]

    # 2. Fall back to original segment dict_id if no exact full-word match was found
    for c_idx in range(group["start"], group["end"]):
        if orig_char_dict[c_idx]:
            return orig_char_dict[c_idx]
    return None


def resegment_sentence(tok: Tokenizer, segments: list, by_word_read, by_word, by_read):
    txt = "".join(s["text"] for s in segments)
    if not txt:
        return segments

    # Build per-char metadata from original segments
    orig_reading = []
    orig_char_dict = []
    orig_span_id = []
    for s_idx, s in enumerate(segments):
        t = s["text"]
        r = s.get("reading")
        d = s.get("dict_id")
        for _ in t:
            orig_reading.append(r)
            orig_char_dict.append(d)
            orig_span_id.append(s_idx)

    # Preserve any original segment with dict_id that has multiple tokens and isn't a verb/adj
    # Actually, let's check char-by-char: if an original segment had dict_id AND reading (e.g. kanji compound),
    # its char_dict is already recorded in orig_char_dict!
    tokens = tokenize_with_offsets(tok, txt)
    tokens = coalesce_original_reading_spans(tokens, segments)
    groups = group_tokens(tokens)

    # Emit new segments, splitting each group at original reading/span boundaries
    new_segs = []
    for g in groups:
        # Guard against merging multiple distinct original words (e.g. 木 + の + 葉)
        distinct_orig_dicts = {
            orig_char_dict[idx]
            for idx in range(g["start"], g["end"])
            if orig_char_dict[idx]
        }
        if len(distinct_orig_dicts) >= 2 and g["surface"] not in by_word:
            cur_start = g["start"]
            while cur_start < g["end"]:
                sp0 = orig_span_id[cur_start]
                r0 = orig_reading[cur_start]
                d0 = orig_char_dict[cur_start]
                cur_end = cur_start + 1
                while cur_end < g["end"] and orig_span_id[cur_end] == sp0:
                    cur_end += 1
                seg_obj = {
                    "text": txt[cur_start:cur_end],
                    "reading": r0,
                }
                if d0:
                    seg_obj["dict_id"] = d0
                new_segs.append(seg_obj)
                cur_start = cur_end
            continue

        did = resolve_dict_id(g, orig_char_dict, by_word_read, by_word, by_read) if g["is_word"] else None
        # Split [g['start'], g['end']) whenever orig_reading changes or original reading span changes
        cur_start = g["start"]
        while cur_start < g["end"]:
            r0 = orig_reading[cur_start]
            sp0 = orig_span_id[cur_start]
            cur_end = cur_start + 1
            while cur_end < g["end"]:
                r1 = orig_reading[cur_end]
                sp1 = orig_span_id[cur_end]
                if r0 is not None or r1 is not None:
                    # Keep original kanji reading span intact
                    if sp1 != sp0:
                        break
                cur_end += 1
            seg_obj = {
                "text": txt[cur_start:cur_end],
                "reading": r0,
            }
            if did:
                seg_obj["dict_id"] = did
            new_segs.append(seg_obj)
            cur_start = cur_end

    # Merge adjacent non-interactive plain segments (no dict_id, no reading)
    merged = []
    for seg in new_segs:
        if (
            merged
            and not seg.get("dict_id")
            and not seg.get("reading")
            and not merged[-1].get("dict_id")
            and not merged[-1].get("reading")
        ):
            merged[-1]["text"] += seg["text"]
        else:
            merged.append(seg)

    rebuilt = "".join(s["text"] for s in merged)
    assert rebuilt == txt, f"Plain text mismatch!\nOLD: {txt}\nNEW: {rebuilt}"
    return merged


def main():
    tok = Tokenizer()
    by_word_read, by_word, by_read, new_entries = load_dictionaries()
    print(f"Loaded dictionary: {len(by_word)} words, {len(new_entries)} new entries to add.")

    total_mats = 0
    total_segs_before = 0
    total_segs_after = 0
    interactive_before = 0
    interactive_after = 0

    for pack_path in PACK_FILES:
        with open(pack_path, encoding="utf-8") as f:
            data = json.load(f)

        # Add new entries to 01_n5_pack.json dictionary
        if pack_path.name == "01_n5_pack.json" and new_entries:
            existing_ids = {e["id"] for e in data.get("dictionary", [])}
            added = 0
            for e in new_entries:
                if e["id"] not in existing_ids:
                    data["dictionary"].append(e)
                    existing_ids.add(e["id"])
                    added += 1
            print(f"Added {added} new dictionary entries to {pack_path.name}")

        for mat in data.get("materials", []):
            total_mats += 1
            old_plain = "\n".join(
                "".join(seg["text"] for s in p["sentences"] for seg in s["segments"])
                for p in mat["paragraphs"]
            )
            for p in mat["paragraphs"]:
                for s in p["sentences"]:
                    total_segs_before += len(s["segments"])
                    interactive_before += sum(1 for seg in s["segments"] if seg.get("dict_id") or seg.get("reading"))
                    s["segments"] = resegment_sentence(tok, s["segments"], by_word_read, by_word, by_read)
                    total_segs_after += len(s["segments"])
                    interactive_after += sum(1 for seg in s["segments"] if seg.get("dict_id") or seg.get("reading"))
            new_plain = "\n".join(
                "".join(seg["text"] for s in p["sentences"] for seg in s["segments"])
                for p in mat["paragraphs"]
            )
            assert new_plain == old_plain, f"Material {mat['id']} plainText changed!"
            mat["version"] = mat.get("version", 1) + 3

        data["pack_version"] = data.get("pack_version", 1) + 3
        with open(pack_path, "w", encoding="utf-8") as f:
            json.dump(data, f, ensure_ascii=False, indent=2)
        print(f"Updated {pack_path.name} -> pack_version={data['pack_version']}")

    # Bump manifest.json
    manifest_path = ROOT / "content_updates/manifest.json"
    with open(manifest_path, encoding="utf-8") as f:
        manifest = json.load(f)
    manifest["manifest_version"] = manifest.get("manifest_version", 1) + 4
    pack_versions = {}
    for pack_path in PACK_FILES:
        with open(pack_path, encoding="utf-8") as f:
            d = json.load(f)
            pack_versions[d["pack_id"]] = d["pack_version"]
    for p in manifest.get("packs", []):
        if p["pack_id"] in pack_versions:
            p["pack_version"] = pack_versions[p["pack_id"]]
    with open(manifest_path, "w", encoding="utf-8") as f:
        json.dump(manifest, f, ensure_ascii=False, indent=2)
    print(f"Updated manifest.json -> manifest_version={manifest['manifest_version']}")
    print(f"Total materials verified: {total_mats}")
    print(f"Interactive segments: {interactive_before} -> {interactive_after}")


if __name__ == "__main__":
    main()
