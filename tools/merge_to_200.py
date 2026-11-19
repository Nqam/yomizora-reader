import hashlib
import json
import re
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from content_builder import KANJI_RE, normalize_text, parse_annotated_text, text_similarity
from conversational_dict import CONVERSATIONAL_EXPRESSIONS
from ext_dict_entries import NEW_KANJI_DICT, NEW_KATAKANA_DICT
from ext_dict_extra import EXTRA_KANJI_DICT, EXTRA_KATAKANA_DICT
from ext_n4_texts import EXT_N4_TEXTS
from ext_n5_texts import EXT_N5_TEXTS
from kana_and_quiz_enricher import (
    HIRAGANA_DICT,
    KATAKANA_DICT,
    QUESTION_KANJI_READINGS,
    make_dict_id,
    split_plain_segment,
)
from upgrade_dictionary_and_sort import complexity_score

ROOT = Path(__file__).resolve().parent.parent
N5_PATH = ROOT / "app" / "src" / "main" / "assets" / "content" / "01_n5_pack.json"
N4_PATH = ROOT / "app" / "src" / "main" / "assets" / "content" / "02_n4_pack.json"
ADDON_V1_PATH = ROOT / "content_updates" / "yomizora_addon_n5_n4_v1.json"
ADDON_V2_PATH = ROOT / "content_updates" / "yomizora_addon_n5_n4_v2.json"
MANIFEST_PATH = ROOT / "content_updates" / "manifest.json"


def build_master_dictionary(n5_dict, n4_dict, v1_dict):
    dictionary = []
    seen_ids = set()
    for src in (n5_dict, n4_dict, v1_dict):
        for e in src:
            if e["id"] not in seen_ids:
                seen_ids.add(e["id"])
                dictionary.append(e)

    # Index existing entries by (word, reading) and word
    word_reading_to_id = {}
    word_to_entry = {}
    for e in dictionary:
        word_reading_to_id[(e["word"], e["reading"])] = e["id"]
        if e["word"] not in word_to_entry:
            word_to_entry[e["word"]] = e

    # Add NEW_KANJI_DICT + EXTRA_KANJI_DICT entries
    stem_to_id = {}
    for (seg_w, seg_r), (dw, dr, pos, trans) in {**NEW_KANJI_DICT, **EXTRA_KANJI_DICT}.items():
        if (dw, dr) in word_reading_to_id:
            did = word_reading_to_id[(dw, dr)]
        elif dw in word_to_entry:
            did = word_to_entry[dw]["id"]
        else:
            did = make_dict_id("dw", f"{dw}_{dr}")
            while did in seen_ids:
                did = did + "x"
            seen_ids.add(did)
            entry = {
                "id": did,
                "word": dw,
                "reading": dr,
                "translation_ru": trans,
                "part_of_speech": pos,
                "note": None,
            }
            dictionary.append(entry)
            word_reading_to_id[(dw, dr)] = did
            if dw not in word_to_entry:
                word_to_entry[dw] = entry
        stem_to_id[(seg_w, seg_r)] = did

    # Add NEW_KATAKANA_DICT + EXTRA_KATAKANA_DICT entries
    for kw, (dw, dr, pos, trans) in {**NEW_KATAKANA_DICT, **EXTRA_KATAKANA_DICT}.items():
        if dw in word_to_entry:
            continue
        did = make_dict_id("dk", dw)
        while did in seen_ids:
            did = did + "x"
        seen_ids.add(did)
        entry = {
            "id": did,
            "word": dw,
            "reading": dr,
            "translation_ru": trans,
            "part_of_speech": pos,
            "note": None,
        }
        dictionary.append(entry)
        word_reading_to_id[(dw, dr)] = did
        word_to_entry[dw] = entry

    # Ensure conversational expressions have natural speech meanings and part_of_speech = "разговорное выражение"
    for key, (dw, dr, trans) in CONVERSATIONAL_EXPRESSIONS.items():
        if dw in word_to_entry:
            e = word_to_entry[dw]
            e["part_of_speech"] = "разговорное выражение"
            e["translation_ru"] = trans
            if key not in word_to_entry:
                word_to_entry[key] = e
        else:
            did = make_dict_id("dc", dw)
            while did in seen_ids:
                did = did + "x"
            seen_ids.add(did)
            entry = {
                "id": did,
                "word": dw,
                "reading": dr,
                "translation_ru": trans,
                "part_of_speech": "разговорное выражение",
                "note": None,
            }
            dictionary.append(entry)
            word_reading_to_id[(dw, dr)] = did
            word_to_entry[dw] = entry
            word_to_entry[key] = entry

    return dictionary, word_reading_to_id, word_to_entry, stem_to_id


def resolve_kanji_dict_id(w, r, dictionary, word_reading_to_id, word_to_entry, stem_to_id):
    if (w, r) in stem_to_id:
        return stem_to_id[(w, r)]
    if (w, r) in word_reading_to_id:
        return word_reading_to_id[(w, r)]
    if w in word_to_entry:
        return word_to_entry[w]["id"]
    for e in dictionary:
        ew, er = e["word"], e["reading"]
        if len(ew) > len(w) and ew.startswith(w) and er.startswith(r):
            suffix = ew[len(w):]
            if not KANJI_RE.search(suffix):
                return e["id"]
    return None


EXTRA_QUESTION_READINGS = {
    "一月二日": "いちがつふつか",
    "一週間後": "いっしゅうかんご",
    "一駅分歩": "ひとえきぶんある",
    "丈夫": "じょうぶ",
    "三時間": "さんじかん",
    "中止": "ちゅうし",
    "九時五分": "くじごふん",
    "今何時": "いまなんじ",
    "今強": "いまつよ",
    "全部出": "ぜんぶだ",
    "全部暗記": "ぜんぶあんき",
    "八時五十分": "はちじごじゅっぷん",
    "六": "ろく",
    "写真二枚": "しゃしんにまい",
    "冬休": "ふゆやす",
    "十二月三十一日": "じゅうにがつさんじゅういちにち",
    "午後三時二十分": "ごごさんじにじゅっぷん",
    "午後二時二十分": "ごごにじにじゅっぷん",
    "取引先": "とりひきさき",
    "塔": "とう",
    "市長": "しちょう",
    "旅行会社": "りょこうがいしゃ",
    "月延長": "げつえんちょう",
    "月後": "げつご",
    "毎日二時間": "まいにちにじかん",
    "毎日買": "まいにちか",
    "流暢": "りゅうちょう",
    "片手": "かたて",
    "画数": "かくすう",
    "盗": "ぬす",
    "社長": "しゃちょう",
    "素早": "すばや",
    "船": "ふね",
    "退屈": "たいくつ",
    "鍵": "かぎ",
    "昼過": "ひるす",
    "朝早": "あさはや",
    "順番": "じゅんばん",
    "一年前": "いちねんまえ",
    "一度使": "いちどつか",
    "一番前": "いちばんまえ",
    "一番後": "いちばんうし",
    "一番高": "いちばんたか",
    "一週間前": "いっしゅうかんまえ",
    "七時半": "しちじはん",
    "三週間続": "さんしゅうかんつづ",
    "二倍": "にばい",
    "二時十分": "にじじゅっぷん",
    "人々": "ひとびと",
    "今少": "いますこ",
    "何日": "なんにち",
    "何時何分": "なんじなんぷん",
    "何点": "なんてん",
    "何色": "なにいろ",
    "保存方法": "ほぞんほうほう",
    "修理": "しゅうり",
    "先週入": "せんしゅうはい",
    "全部消": "ぜんぶけ",
    "全部送": "ぜんぶおく",
    "八十点": "はちじゅってん",
    "凍": "こお",
    "凶": "きょう",
    "列": "れつ",
    "割": "わ",
    "割引": "わりびき",
    "十時半": "じゅうじはん",
    "半分近": "はんぶんちか",
    "半額以下": "はんがくいか",
    "厳": "きび",
    "反応": "はんのう",
    "味噌作": "みそづく",
    "命令": "めいれい",
    "大雪": "おおゆき",
    "急行電車": "きゅうこうでんしゃ",
    "月前": "げつまえ",
    "月続": "げつつづ",
    "服": "ふく",
    "材料": "ざいりょう",
    "毎日三十分": "まいにちさんじゅっぷん",
    "毎日十回書": "まいにちじゅっかいか",
    "毎日十時間寝": "まいにちじゅうじかんね",
    "毎日練習": "まいにちれんしゅう",
    "毎朝何時": "まいあさなんじ",
    "点数": "てんすう",
    "無口": "むくち",
    "端": "はし",
    "街": "まち",
    "賞金": "しょうきん",
    "運転": "うんてん",
    "運転手": "うんてんしゅ",
    "金属": "きんぞく",
    "鏡": "かがみ",
    "閉館時間": "へいかんじかん",
}


def annotate_q_text(text, dictionary, word_reading_to_id, word_to_entry, stem_to_id, kana_words_sorted, kana_id_map, unannotated_q):
    kanji_re = re.compile(r"[\u4E00-\u9FAF\u3400-\u4DBF々]+")
    stem_reading_map = {seg_w: seg_r for (seg_w, seg_r) in stem_to_id}
    segments = []
    pos = 0
    for m in kanji_re.finditer(text):
        if m.start() > pos:
            plain = text[pos:m.start()]
            segments.extend(split_plain_segment(plain, kana_words_sorted, kana_id_map))
        kword = m.group(0)
        if kword in word_to_entry:
            e = word_to_entry[kword]
            segments.append({"text": kword, "reading": e["reading"], "dict_id": e["id"]})
        elif kword in stem_reading_map:
            r = stem_reading_map[kword]
            did = stem_to_id[(kword, r)]
            segments.append({"text": kword, "reading": r, "dict_id": did})
        elif kword in EXTRA_QUESTION_READINGS:
            segments.append({"text": kword, "reading": EXTRA_QUESTION_READINGS[kword]})
        elif kword in QUESTION_KANJI_READINGS:
            segments.append({"text": kword, "reading": QUESTION_KANJI_READINGS[kword]})
        else:
            # Try stem-prefix match against dictionary (e.g. 買 -> 買う)
            matched = None
            for e in dictionary:
                ew, er = e["word"], e["reading"]
                if len(ew) > len(kword) and ew.startswith(kword):
                    suffix = ew[len(kword):]
                    if not KANJI_RE.search(suffix) and er.endswith(suffix):
                        matched = (er[:-len(suffix)], e["id"])
                        break
            if matched:
                segments.append({"text": kword, "reading": matched[0], "dict_id": matched[1]})
            else:
                unannotated_q.add(kword)
                segments.append({"text": kword, "reading": None})
        pos = m.end()
    if pos < len(text):
        segments.extend(split_plain_segment(text[pos:], kana_words_sorted, kana_id_map))
    return segments


def build_new_materials(raw_list, level, dictionary, word_reading_to_id, word_to_entry, stem_to_id, kana_words_sorted, kana_id_map, unannotated_q):
    built = []
    for item in raw_list:
        paragraphs = parse_annotated_text(item["body"], {})
        for p in paragraphs:
            for s in p["sentences"]:
                new_segs = []
                for seg in s["segments"]:
                    if seg.get("reading"):
                        did = resolve_kanji_dict_id(
                            seg["text"], seg["reading"],
                            dictionary, word_reading_to_id, word_to_entry, stem_to_id
                        )
                        if did:
                            seg["dict_id"] = did
                        else:
                            raise ValueError(f"Missing dict_id for {seg['text']}[{seg['reading']}] in {item['id']}")
                        new_segs.append(seg)
                    else:
                        new_segs.extend(split_plain_segment(seg["text"], kana_words_sorted, kana_id_map))
                s["segments"] = new_segs

        plain_full = "".join(
            seg["text"] for p in paragraphs for s in p["sentences"] for seg in s["segments"]
        )
        content_hash = hashlib.sha256(normalize_text(plain_full).encode("utf-8")).hexdigest()

        questions = []
        for q in item["questions"]:
            q_copy = dict(q)
            q_copy["question_segments"] = annotate_q_text(
                q["question"], dictionary, word_reading_to_id, word_to_entry, stem_to_id, kana_words_sorted, kana_id_map, unannotated_q
            )
            q_copy["option_segments"] = [
                annotate_q_text(
                    opt, dictionary, word_reading_to_id, word_to_entry, stem_to_id, kana_words_sorted, kana_id_map, unannotated_q
                )
                for opt in q["options"]
            ]
            questions.append(q_copy)

        built.append({
            "id": item["id"],
            "level": level,
            "category": item["category"],
            "title": item["title"],
            "description": item["description"],
            "order_index": 0,
            "version": 3,
            "content_hash": content_hash,
            "paragraphs": paragraphs,
            "questions": questions,
        })
    return built


def check_duplicates(materials):
    seen_ids = set()
    seen_titles = set()
    seen_hashes = set()
    norms = []
    for m in materials:
        mid = m["id"]
        if mid in seen_ids:
            raise ValueError(f"Duplicate ID: {mid}")
        seen_ids.add(mid)

        tkey = (m["level"], m["title"].strip())
        if tkey in seen_titles:
            raise ValueError(f"Duplicate title: {tkey}")
        seen_titles.add(tkey)

        plain = "".join(seg["text"] for p in m["paragraphs"] for s in p["sentences"] for seg in s["segments"])
        norm = normalize_text(plain)
        h = m.get("content_hash") or hashlib.sha256(norm.encode("utf-8")).hexdigest()
        if h in seen_hashes:
            raise ValueError(f"Duplicate hash: {mid}")
        seen_hashes.add(h)

        for prev_id, prev_norm in norms:
            sim = text_similarity(norm, prev_norm)
            if sim >= 0.85:
                raise ValueError(f"Near-duplicate texts: {mid} vs {prev_id} (sim={sim:.2f})")
        norms.append((mid, norm))


def main():
    n5_data = json.loads(N5_PATH.read_text(encoding="utf-8"))
    n4_data = json.loads(N4_PATH.read_text(encoding="utf-8"))
    v1_data = json.loads(ADDON_V1_PATH.read_text(encoding="utf-8"))

    dictionary, word_reading_to_id, word_to_entry, stem_to_id = build_master_dictionary(
        n5_data.get("dictionary", []),
        n4_data.get("dictionary", []),
        v1_data.get("dictionary", []),
    )

    kana_id_map = {}
    for w in {**KATAKANA_DICT, **HIRAGANA_DICT, **NEW_KATAKANA_DICT, **EXTRA_KATAKANA_DICT}:
        if w in word_to_entry:
            kana_id_map[w] = word_to_entry[w]["id"]
    for key, (dw, dr, _) in CONVERSATIONAL_EXPRESSIONS.items():
        if not KANJI_RE.search(key) and key in word_to_entry:
            kana_id_map[key] = word_to_entry[key]["id"]
    kana_words_sorted = sorted(kana_id_map.keys(), key=len, reverse=True)

    unannotated_q = set()
    ext_n5_built = build_new_materials(
        EXT_N5_TEXTS, "N5", dictionary, word_reading_to_id, word_to_entry, stem_to_id, kana_words_sorted, kana_id_map, unannotated_q
    )
    ext_n4_built = build_new_materials(
        EXT_N4_TEXTS, "N4", dictionary, word_reading_to_id, word_to_entry, stem_to_id, kana_words_sorted, kana_id_map, unannotated_q
    )

    if unannotated_q:
        print("UNANNOTATED QUESTION KANJI:", sorted(unannotated_q))

    v1_n5 = [m for m in v1_data["materials"] if m["level"] == "N5"]
    v1_n4 = [m for m in v1_data["materials"] if m["level"] == "N4"]

    # Deduplicate by id, preferring newer ext_built over previously merged copies
    def merge_unique(*lists):
        by_id = {}
        for lst in lists:
            for m in lst:
                by_id[m["id"]] = m
        return list(by_id.values())

    all_n5 = merge_unique(n5_data["materials"], v1_n5, ext_n5_built)
    all_n4 = merge_unique(n4_data["materials"], v1_n4, ext_n4_built)

    print(f"Merged counts: N5={len(all_n5)}, N4={len(all_n4)}, Total={len(all_n5) + len(all_n4)}")
    check_duplicates(all_n5 + all_n4)

    all_n5 = sorted(all_n5, key=complexity_score)
    for idx, m in enumerate(all_n5, start=1):
        m["order_index"] = idx
        m["version"] = 4

    all_n4 = sorted(all_n4, key=complexity_score)
    for idx, m in enumerate(all_n4, start=1):
        m["order_index"] = idx
        m["version"] = 4

    n5_data["pack_version"] = 6
    n5_data["materials"] = all_n5
    n5_data["dictionary"] = dictionary
    N5_PATH.write_text(json.dumps(n5_data, ensure_ascii=False, indent=2), encoding="utf-8")

    n4_data["pack_version"] = 6
    n4_data["materials"] = all_n4
    n4_data["dictionary"] = []
    N4_PATH.write_text(json.dumps(n4_data, ensure_ascii=False, indent=2), encoding="utf-8")

    # Also build yomizora_addon_n5_n4_v2.json for existing users updating via GitHub Content Updates
    v2_materials = sorted(ext_n5_built + ext_n4_built, key=lambda m: (m["level"] != "N5", complexity_score(m)))
    n5_i, n4_i = 71, 71
    for m in v2_materials:
        m["version"] = 4
        if m["level"] == "N5":
            m["order_index"] = n5_i
            n5_i += 1
        else:
            m["order_index"] = n4_i
            n4_i += 1

    v2_dict_ids = {
        seg["dict_id"]
        for m in v2_materials
        for p in m["paragraphs"]
        for s in p["sentences"]
        for seg in s["segments"]
        if seg.get("dict_id")
    } | {
        seg["dict_id"]
        for m in v2_materials
        for q in m.get("questions", [])
        for seg_list in [q.get("question_segments", [])] + q.get("option_segments", [])
        for seg in seg_list
        if seg.get("dict_id")
    }
    v2_dict = [e for e in dictionary if e["id"] in v2_dict_ids]

    addon_v2 = {
        "pack_id": "yomizora_addon_n5_n4_v2",
        "pack_version": 2,
        "materials": v2_materials,
        "dictionary": v2_dict,
    }
    ADDON_V2_PATH.write_text(json.dumps(addon_v2, ensure_ascii=False, indent=2), encoding="utf-8")

    manifest = {
        "manifest_version": 3,
        "packs": [
            {
                "pack_id": "yomizora_addon_n5_n4_v1",
                "pack_version": 4,
                "title": "Дополнительный набор N5–N4 (30 текстов)",
                "description": "15 текстов JLPT N5 и 15 текстов JLPT N4 во всех категориях",
                "url": "https://raw.githubusercontent.com/Nqam/yomizora-reader/main/content_updates/yomizora_addon_n5_n4_v1.json",
            },
            {
                "pack_id": "yomizora_addon_n5_n4_v2",
                "pack_version": 2,
                "title": "Большой набор N5–N4 (60 расширенных текстов)",
                "description": "30 длинных текстов JLPT N5 и 30 длинных текстов JLPT N4 с разговорными выражениями",
                "url": "https://raw.githubusercontent.com/Nqam/yomizora-reader/main/content_updates/yomizora_addon_n5_n4_v2.json",
            },
        ],
    }
    MANIFEST_PATH.write_text(json.dumps(manifest, ensure_ascii=False, indent=2), encoding="utf-8")
    print("Successfully built 100 N5 + 100 N4 = 200 materials! Dictionary size:", len(dictionary))


if __name__ == "__main__":
    main()
