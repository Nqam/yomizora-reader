import hashlib
import json
import re
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from content_builder import KANJI_RE, normalize_text, parse_annotated_text, text_similarity
from conversational_dict import CONVERSATIONAL_EXPRESSIONS
from kana_and_quiz_enricher import HIRAGANA_DICT, KATAKANA_DICT, QUESTION_KANJI_READINGS, make_dict_id, split_plain_segment
from n1_extra10 import N1_EXTRA10
from n2_extra10 import N2_EXTRA10
from n3_extra10 import N3_EXTRA10
from n321_extra10_dict_1 import DICT_ENTRIES as E1
from n321_extra10_dict_2 import DICT_ENTRIES as E2
from upgrade_dictionary_and_sort import complexity_score

ROOT = Path(__file__).resolve().parent.parent
N5_PATH = ROOT / "app" / "src" / "main" / "assets" / "content" / "01_n5_pack.json"
N4_PATH = ROOT / "app" / "src" / "main" / "assets" / "content" / "02_n4_pack.json"
V1_PACK_PATH = ROOT / "content_updates" / "yomizora_pack_n3_n2_n1_v1.json"
OUT_PACK_PATH = ROOT / "content_updates" / "yomizora_pack_n3_n2_n1_v2.json"
MANIFEST_PATH = ROOT / "content_updates" / "manifest.json"


def main():
    n5_data = json.loads(N5_PATH.read_text(encoding="utf-8"))
    n4_data = json.loads(N4_PATH.read_text(encoding="utf-8"))
    v1_data = json.loads(V1_PACK_PATH.read_text(encoding="utf-8"))

    existing_dict = []
    seen_ids = set()
    for e in n5_data.get("dictionary", []) + v1_data.get("dictionary", []):
        if e["id"] not in seen_ids:
            seen_ids.add(e["id"])
            existing_dict.append(e)

    word_reading_to_id = {(e["word"], e["reading"]): e["id"] for e in existing_dict}
    word_to_entry = {}
    for e in existing_dict:
        if e["word"] not in word_to_entry:
            word_to_entry[e["word"]] = e

    pack_dict_entries = {}
    stem_to_id = {}

    all_new = {}
    all_new.update(E1)
    all_new.update(E2)

    for (seg_w, seg_r), (dw, dr, pos, trans) in all_new.items():
        if (dw, dr) in word_reading_to_id:
            did = word_reading_to_id[(dw, dr)]
            entry = next(e for e in existing_dict if e["id"] == did)
        elif dw in word_to_entry:
            entry = word_to_entry[dw]
            did = entry["id"]
        else:
            did = make_dict_id("db", f"{dw}_{dr}")
            while did in seen_ids:
                did += "x"
            seen_ids.add(did)
            entry = {
                "id": did,
                "word": dw,
                "reading": dr,
                "translation_ru": trans,
                "part_of_speech": pos,
                "note": None,
            }
            existing_dict.append(entry)
            word_reading_to_id[(dw, dr)] = did
            if dw not in word_to_entry:
                word_to_entry[dw] = entry
        pack_dict_entries[did] = entry
        stem_to_id[(seg_w, seg_r)] = did

    for key, (dw, dr, trans) in CONVERSATIONAL_EXPRESSIONS.items():
        if dw in word_to_entry:
            e = word_to_entry[dw]
            e["part_of_speech"] = "разговорное выражение"
            e["translation_ru"] = trans
            if key not in word_to_entry:
                word_to_entry[key] = e
            pack_dict_entries[e["id"]] = e

    kana_id_map = {}
    for w in list(KATAKANA_DICT.keys()) + list(HIRAGANA_DICT.keys()) + [w for (w, _) in all_new.keys() if not KANJI_RE.search(w)]:
        if w in word_to_entry:
            kana_id_map[w] = word_to_entry[w]["id"]
            pack_dict_entries[word_to_entry[w]["id"]] = word_to_entry[w]
    for key, (dw, dr, _) in CONVERSATIONAL_EXPRESSIONS.items():
        if not KANJI_RE.search(key) and key in word_to_entry:
            kana_id_map[key] = word_to_entry[key]["id"]
            pack_dict_entries[word_to_entry[key]["id"]] = word_to_entry[key]
    kana_words_sorted = sorted(kana_id_map.keys(), key=len, reverse=True)

    def resolve_kanji_id(w, r):
        if (w, r) in stem_to_id:
            did = stem_to_id[(w, r)]
            pack_dict_entries[did] = next(e for e in existing_dict if e["id"] == did)
            return did
        if (w, r) in word_reading_to_id:
            did = word_reading_to_id[(w, r)]
            pack_dict_entries[did] = next(e for e in existing_dict if e["id"] == did)
            return did
        if w in word_to_entry:
            e = word_to_entry[w]
            pack_dict_entries[e["id"]] = e
            return e["id"]
        for e in existing_dict:
            ew, er = e["word"], e["reading"]
            if len(ew) > len(w) and ew.startswith(w) and er.startswith(r):
                suffix = ew[len(w):]
                if not KANJI_RE.search(suffix):
                    pack_dict_entries[e["id"]] = e
                    return e["id"]
        return None

    stem_reading_map = {seg_w: seg_r for (seg_w, seg_r) in stem_to_id}
    kanji_re = re.compile(r"[\u4E00-\u9FAF\u3400-\u4DBF々]+")
    unannotated_q = set()

    extra_q_readings = {
        "一切": "いっさい", "一切使": "いっさいつか", "一切頼": "いっさいたよ",
        "一度使": "いちどつか", "一度忘": "いちどわす", "二倍": "にばい",
        "五年間": "ごねんかん", "五感全体": "ごかんぜんたい", "会話中": "かいわちゅう",
        "保存法": "ほぞんほう", "修理費": "しゅうりひ", "全員降": "ぜんいんお",
        "出": "だ", "切": "き", "削除": "さくじょ", "動作": "どうさ",
        "十回": "じゅっかい", "十時間以上": "じゅうじかんいじょう", "反対派": "はんたいは",
        "古本屋": "ふるほんや", "同業者": "どうぎょうしゃ", "名乗": "なの",
        "周囲": "しゅうい", "夏場": "なつば", "外出": "がいしゅつ",
        "大賛成": "だいさんせい", "季節感": "きせつかん", "家賃": "やちん",
        "対話": "たいわ", "専門家": "せんもんか", "年齢": "ねんれい",
        "心構": "こころがま", "忍耐力": "にんたいりょく", "意識的": "いしきてき",
        "感情的": "かんじょうてき", "手作業": "てさぎょう", "批判": "ひはん",
        "掲載": "けいさい", "廃止": "はいし", "技術": "ぎじゅつ",
        "時短": "じたん", "時間制限": "じかんせいげん", "曲": "きょく",
        "最新技術": "さいしんぎじゅん", "本気": "ほんき", "材料費": "ざいりょうひ",
        "板": "いた", "検索結果": "けんさくけっか", "歴史的": "れきしてき",
        "毎日見": "まいにちみ", "毎晩": "まいばん", "比較": "ひかく",
        "気温": "きおん", "決断": "けつだん", "注意": "ちゅうい",
        "海外旅行": "かいがいりょこう", "消費": "しょうひ", "湿気": "しっけ",
        "温度": "おんど", "無意識": "むいしき", "無料": "むりょう",
        "物価": "ぶっか", "環境面": "かんきょうめん", "生産者": "せいさんしゃ",
        "目的": "もくてき", "相談": "そうだん", "短期": "たんき",
        "研究": "けんきゅう", "社会全体": "しゃかいぜんたい", "秋": "あき",
        "移動時間": "いどうじかん", "空気": "くうき", "節約": "せつやく",
        "簡単": "かんたん", "管理": "かんり", "紙": "かみ",
        "練習": "れんしゅう", "自動的": "じどうてき", "自分一人": "じぶんひとり",
        "自分自身": "じぶんじしん", "自然": "しぜん", "若者": "わかもの",
        "街": "まち", "表示": "ひょうじ", "規則": "きそく",
        "親": "おや", "視覚的": "しかくてき", "観光客": "かんこうきゃく",
        "計算": "けいさん", "記録": "きろく", "設備": "せつび",
        "評価": "ひょうか", "語学": "ごがく", "調査": "ちょうさ",
        "資金": "しきん", "距離": "きょり", "車": "くるま",
        "近道": "ちかみち", "返事": "へんじ", "連絡": "れんらく",
        "選択": "せんたく", "都市部": "としぶ", "重要": "じゅうよう",
        "音楽": "おんがく", "頭": "あたま", "風": "かぜ",
        "食文化": "しょくぶんか", "食事": "しょくじ", "駅": "えき",
        "一刻": "いっこく", "一年中同": "いちねんじゅうおな", "一年使": "いちねんつか",
        "一度同": "いちどおな", "不正確": "ふせいかく", "中立性": "ちゅうりつせい",
        "事態": "じたい", "五十人呼": "ごじゅうにんよ", "伝統芸能": "でんとうげいのう",
        "住環境": "じゅうかんきょう", "使用": "しよう", "値引": "ねび",
        "六十歳以上": "ろくじゅっさいいじょう", "切断": "せつだん", "十倍長": "じゅうばいなが",
        "十合": "じゅうごう", "和洋中": "わようちゅう", "大衆": "たいしゅう",
        "天守閣": "てんしゅかく", "安物": "やすもの", "市販": "しはん",
        "急減": "きゅうげん", "慎重論": "しんちょうろん", "挿入": "そうにゅう",
        "提出": "ていしゅつ", "支配": "しはい", "数年間休眠状態": "すうねんかんきゅうみんじょうたい",
        "早期退職": "そうきたいしょく", "早送": "はやおく", "明日使": "あしたつか",
        "映画上映": "えいがじょうえい", "是正": "ぜせい", "普段目立": "ふだんめだ",
        "本来測": "ほんらいはか", "枯": "か", "椅子": "いす",
        "毎日必": "まいにちかなら", "毎朝一合": "まいあさいちごう", "法律": "ほうりつ",
        "法律違反": "ほうりついはん", "混在": "こんざい", "満杯": "まんぱい",
        "画質": "がしつ", "納得感": "なっとくかん", "終盤": "しゅうばん",
        "給与": "きゅうよ", "統一感": "とういつかん", "縁起": "えんぎ",
        "繁盛": "はんじょう", "被害": "ひがい", "言及": "げんきゅう",
        "請求": "せいきゅう", "資金援助": "しきんえんじょ", "車掌": "しゃしょう",
        "退出": "たいしゅつ", "通行証": "つうこうしょう", "閉鎖": "へいさ",
        "開設": "かいせつ", "飲食業界": "いんしょくぎょうかい",
    }

    def annotate_q(text):
        segs = []
        pos = 0
        for m in kanji_re.finditer(text):
            if m.start() > pos:
                segs.extend(split_plain_segment(text[pos:m.start()], kana_words_sorted, kana_id_map))
            kw = m.group(0)
            if kw in word_to_entry:
                e = word_to_entry[kw]
                pack_dict_entries[e["id"]] = e
                segs.append({"text": kw, "reading": e["reading"], "dict_id": e["id"]})
            elif kw in stem_reading_map:
                r = stem_reading_map[kw]
                did = stem_to_id[(kw, r)]
                segs.append({"text": kw, "reading": r, "dict_id": did})
            elif kw in extra_q_readings:
                segs.append({"text": kw, "reading": extra_q_readings[kw]})
            elif kw in QUESTION_KANJI_READINGS:
                segs.append({"text": kw, "reading": QUESTION_KANJI_READINGS[kw]})
            else:
                matched = None
                for e in existing_dict:
                    ew, er = e["word"], e["reading"]
                    if len(ew) > len(kw) and ew.startswith(kw):
                        suffix = ew[len(kw):]
                        if not KANJI_RE.search(suffix) and er.endswith(suffix):
                            matched = (er[:-len(suffix)], e["id"], e)
                            break
                if matched:
                    pack_dict_entries[matched[2]["id"]] = matched[2]
                    segs.append({"text": kw, "reading": matched[0], "dict_id": matched[1]})
                else:
                    sub_segs = []
                    idx = 0
                    all_ok = True
                    while idx < len(kw):
                        found_sub = None
                        for end in range(len(kw), idx, -1):
                            sub = kw[idx:end]
                            if sub in word_to_entry:
                                e = word_to_entry[sub]
                                pack_dict_entries[e["id"]] = e
                                found_sub = {"text": sub, "reading": e["reading"], "dict_id": e["id"]}
                                break
                            elif sub in stem_reading_map:
                                r = stem_reading_map[sub]
                                did = stem_to_id[(sub, r)]
                                found_sub = {"text": sub, "reading": r, "dict_id": did}
                                break
                            elif sub in extra_q_readings:
                                found_sub = {"text": sub, "reading": extra_q_readings[sub]}
                                break
                            elif sub in QUESTION_KANJI_READINGS:
                                found_sub = {"text": sub, "reading": QUESTION_KANJI_READINGS[sub]}
                                break
                        if found_sub:
                            sub_segs.append(found_sub)
                            idx += len(found_sub["text"])
                        else:
                            all_ok = False
                            break
                    if all_ok and sub_segs:
                        segs.extend(sub_segs)
                    else:
                        unannotated_q.add(kw)
                        segs.append({"text": kw, "reading": None})
            pos = m.end()
        if pos < len(text):
            segs.extend(split_plain_segment(text[pos:], kana_words_sorted, kana_id_map))
        return segs

    built_all = []
    for level, raw_list in [("N3", N3_EXTRA10), ("N2", N2_EXTRA10), ("N1", N1_EXTRA10)]:
        level_mats = []
        for item in raw_list:
            paras = parse_annotated_text(item["body"], {})
            for p in paras:
                for s in p["sentences"]:
                    new_segs = []
                    for seg in s["segments"]:
                        if seg.get("reading"):
                            did = resolve_kanji_id(seg["text"], seg["reading"])
                            if not did:
                                raise ValueError(f"Missing dict_id for {seg['text']}[{seg['reading']}] in {item['id']}")
                            seg["dict_id"] = did
                            new_segs.append(seg)
                        else:
                            new_segs.extend(split_plain_segment(seg["text"], kana_words_sorted, kana_id_map))
                    s["segments"] = new_segs

            plain = "".join(seg["text"] for p in paras for s in p["sentences"] for seg in s["segments"])
            chash = hashlib.sha256(normalize_text(plain).encode("utf-8")).hexdigest()

            qs = []
            for q in item["questions"]:
                qc = dict(q)
                qc["question_segments"] = annotate_q(q["question"])
                qc["option_segments"] = [annotate_q(opt) for opt in q["options"]]
                qs.append(qc)

            level_mats.append({
                "id": item["id"],
                "level": level,
                "category": item["category"],
                "title": item["title"],
                "description": item["description"],
                "order_index": 0,
                "version": 1,
                "content_hash": chash,
                "paragraphs": paras,
                "questions": qs,
            })

        level_mats.sort(key=complexity_score)
        for idx, m in enumerate(level_mats, start=31):
            m["order_index"] = idx
        built_all.extend(level_mats)

    if unannotated_q:
        print("UNANNOTATED QUESTION KANJI:", len(unannotated_q), sorted(unannotated_q))

    # Duplicate check against all 290 existing texts
    existing_mats = n5_data["materials"] + n4_data["materials"] + v1_data["materials"]
    seen_m_ids = {m["id"] for m in existing_mats}
    seen_titles = {(m["level"], m["title"].strip()) for m in existing_mats}
    seen_hashes = set()
    norms = []
    for m in existing_mats:
        plain = "".join(seg["text"] for p in m["paragraphs"] for s in p["sentences"] for seg in s["segments"])
        norm = normalize_text(plain)
        seen_hashes.add(m.get("content_hash") or hashlib.sha256(norm.encode("utf-8")).hexdigest())
        norms.append((m["id"], norm))

    for m in built_all:
        if m["id"] in seen_m_ids:
            raise ValueError(f"Duplicate ID: {m['id']}")
        seen_m_ids.add(m["id"])
        tk = (m["level"], m["title"].strip())
        if tk in seen_titles:
            raise ValueError(f"Duplicate title: {tk}")
        seen_titles.add(tk)
        if m["content_hash"] in seen_hashes:
            raise ValueError(f"Duplicate hash: {m['id']}")
        seen_hashes.add(m["content_hash"])
        plain = "".join(seg["text"] for p in m["paragraphs"] for s in p["sentences"] for seg in s["segments"])
        norm = normalize_text(plain)
        for pid, pnorm in norms:
            sim = text_similarity(norm, pnorm)
            if sim >= 0.85:
                raise ValueError(f"Near-duplicate: {m['id']} vs {pid} ({sim:.2f})")
        norms.append((m["id"], norm))

    pack_out = {
        "pack_id": "yomizora_pack_n3_n2_n1_v2",
        "pack_version": 1,
        "materials": built_all,
        "dictionary": list(pack_dict_entries.values()),
    }
    OUT_PACK_PATH.write_text(json.dumps(pack_out, ensure_ascii=False, indent=2), encoding="utf-8")

    manifest = json.loads(MANIFEST_PATH.read_text(encoding="utf-8"))
    manifest["manifest_version"] = 6
    packs = [p for p in manifest["packs"] if p["pack_id"] != "yomizora_pack_n3_n2_n1_v2"]
    packs.append({
        "pack_id": "yomizora_pack_n3_n2_n1_v2",
        "pack_version": 1,
        "title": "Дополнение N3–N2–N1 (+30 текстов)",
        "description": "По 10 новых лонгрид-текстов для JLPT N3, N2 и N1 (итого по 40 текстов в каждом уровне)",
        "url": "https://raw.githubusercontent.com/Nqam/yomizora-reader/main/content_updates/yomizora_pack_n3_n2_n1_v2.json",
    })
    manifest["packs"] = packs
    MANIFEST_PATH.write_text(json.dumps(manifest, ensure_ascii=False, indent=2), encoding="utf-8")
    print(f"Built N3/N2/N1 v2 pack: {len(built_all)} materials, {len(pack_dict_entries)} dictionary entries!")


if __name__ == "__main__":
    main()
