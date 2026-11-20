import hashlib
import json
import re
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from content_builder import KANJI_RE, normalize_text, parse_annotated_text, text_similarity
from conversational_dict import CONVERSATIONAL_EXPRESSIONS
from kana_and_quiz_enricher import HIRAGANA_DICT, KATAKANA_DICT, QUESTION_KANJI_READINGS, make_dict_id, split_plain_segment
from n1_texts import N1_TEXTS
from n2_texts import N2_TEXTS
from n3_texts import N3_TEXTS
from n321_dict_1 import DICT_ENTRIES as D1
from n321_dict_2 import DICT_ENTRIES as D2
from n321_dict_3 import DICT_ENTRIES as D3
from n321_long_dict_1 import DICT_ENTRIES as L1
from n321_long_dict_2 import DICT_ENTRIES as L2
from n321_long_dict_3 import DICT_ENTRIES as L3
from n321_long_dict_4 import DICT_ENTRIES as L4
from n321_long_dict_extra import DICT_ENTRIES as LX
from upgrade_dictionary_and_sort import complexity_score

ROOT = Path(__file__).resolve().parent.parent
N5_PATH = ROOT / "app" / "src" / "main" / "assets" / "content" / "01_n5_pack.json"
N4_PATH = ROOT / "app" / "src" / "main" / "assets" / "content" / "02_n4_pack.json"
OUT_PACK_PATH = ROOT / "content_updates" / "yomizora_pack_n3_n2_n1_v1.json"
MANIFEST_PATH = ROOT / "content_updates" / "manifest.json"


def main():
    n5_data = json.loads(N5_PATH.read_text(encoding="utf-8"))
    n4_data = json.loads(N4_PATH.read_text(encoding="utf-8"))
    existing_dict = n5_data.get("dictionary", [])

    seen_ids = {e["id"] for e in existing_dict}
    word_reading_to_id = {(e["word"], e["reading"]): e["id"] for e in existing_dict}
    word_to_entry = {}
    for e in existing_dict:
        if e["word"] not in word_to_entry:
            word_to_entry[e["word"]] = e

    pack_dict_entries = {}  # id -> entry
    stem_to_id = {}

    # Load all N3/N2/N1 dictionary entries
    all_new = {}
    for src in (D1, D2, D3, L1, L2, L3, L4, LX):
        all_new.update(src)
    # Fix typo entry if needed
    if ("成果", "せいかつ") in all_new:
        all_new[("成果", "せいか")] = ("成果", "せいか", "существительное", "результат, плоды труда")

    for (seg_w, seg_r), (dw, dr, pos, trans) in all_new.items():
        if (dw, dr) in word_reading_to_id:
            did = word_reading_to_id[(dw, dr)]
            entry = next(e for e in existing_dict if e["id"] == did)
        elif dw in word_to_entry:
            entry = word_to_entry[dw]
            did = entry["id"]
        else:
            did = make_dict_id("da", f"{dw}_{dr}")
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

    # Conversational expressions
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
        "一切": "いっさい", "一切聞": "いっさいき", "一年中同": "いちねんじゅうおな",
        "一度失": "いちどうしな", "一時間前": "いちじかんまえ", "一歩踏": "いっぽふ",
        "一汁一菜": "いちじゅういっさい", "一番鮮明": "いちばんせんめい", "三杯飲": "さんばいの",
        "上昇": "じょうしょう", "世代間": "せだいかん", "中止": "ちゅうし",
        "丸暗記": "まるあんき", "主体的": "しゅたいてき", "二倍": "にばい",
        "二十四時間営業": "にじゅうよじかんえいぎょう", "伝統工芸": "でんとうこうげい",
        "伝統技法": "でんとうぎほう", "位置": "いち", "住人": "じゅうにん", "余": "あま",
        "作品全体": "さくひんぜんたい", "作業時間": "さぎょうじかん", "使用": "しよう",
        "信": "しん", "停": "てい", "健康面": "けんこうめん", "傘代": "かさだい",
        "克服": "こくふく", "全面的": "ぜんめんてき", "六時間未満": "ろくじかんみまん",
        "具": "ぐ", "具体例": "ぐたいれい", "具体的": "ぐたいてき",
        "再発防止策": "さいはつぼうしさく", "削除": "さくじょ", "効果": "こうか",
        "包装": "ほうそう", "包装紙": "ほうそうし", "卒業": "そつぎょう",
        "即座": "そくざ", "反対意見": "はんたいいけん", "口中調味": "こうちゅうちょうみ",
        "合図": "あいず", "命令": "めいれい", "営業成績": "えいぎょうせいせき",
        "回数": "かいすう", "地方移住": "ちほういじゅう", "基礎的": "きそてき",
        "増加": "ぞうか", "外出": "がいしゅつ", "夜間": "やかん",
        "定時退社": "ていじたいしゃ", "定着率": "ていちゃくりつ", "定義": "ていぎ",
        "専門知": "せんもんち", "工場": "こうじょう", "広告費": "こうこくひ",
        "座席番号": "ざせきばんごう", "廃止": "はいし", "延": "の",
        "得意分野": "とくいぶんや", "忍耐力": "にんたいりょく", "応用研究": "おうようけんきゅう",
        "恐怖": "きょうふ", "成功体験": "せいこうたいけん", "成果物": "せいかぶつ",
        "批判的思考": "ひはんてきしこう", "技術革新": "ぎじゅつかくしん", "挙": "あ",
        "数値化": "すうちか", "数十年後": "すうじゅうねんご", "数年後": "すうねんご",
        "方向性": "ほうこうせい", "日本建築": "にほんけんちく", "旧街道": "きゅうかいどう",
        "明日同": "あしたおな", "昼寝": "ひるね", "時間的": "じかんてき",
        "普段意識": "ふだんいしき", "景勝地": "けいしょうち", "暖房費": "だんぼうひ",
        "最新基準": "さいしんきじゅん", "最終的": "さいしゅうてき", "月保存": "げつほぞん",
        "月遅": "げつおく", "未完成": "みかんせい", "本来何": "ほんらいなに",
        "構造部分": "こうぞうぶぶん", "模範解答": "もはんかいとう", "残業時間": "ざんぎょうじかん",
        "毎日出社": "まいにちしゅっしゃ", "毎日練習": "まいにちれんしゅう", "比例": "ひれい",
        "決断": "けつだん", "没頭": "ぼっとう", "消灯後": "しょうとうご",
        "温度": "おんど", "理想的": "りそうてき", "環境面": "かんきょうめん",
        "異動": "いどう", "異文化理解": "いぶんかりかい", "発売日": "はつばいび",
        "発酵食品": "はっこうしょくひん", "白味噌仕立": "しろみそじた", "盲点": "もうてん",
        "睡眠時間": "すいみんじかん", "短期的": "たんきてき", "研究費": "けんきゅうひ",
        "社会基盤": "しゃかいきばん", "禁止": "きんし", "移動手段": "いどうしゅだん",
        "移動時間": "いどうじかん", "競合他社": "きょうごうたしゃ", "管理人": "かんりにん",
        "精神的": "せいしんてき", "納得解": "なっとくかい", "給料": "きゅうりょう",
        "締": "し", "翻訳機": "ほんやくき", "自分一人": "じぶんひとり",
        "自分自身": "じぶんじしん", "要素": "ようそ", "観光旅行": "かんこうりょこう",
        "解説書": "かいせつしょ", "計算": "けいさん", "試行錯誤": "しこうさくご",
        "語学上達": "ごがくじょうたつ", "警戒": "けいかい", "資格試験": "しかくしけん",
        "趣味": "しゅみ", "辞": "や", "述": "の", "迷子": "まいご", "退屈": "たいくつ",
        "連絡方法": "れんらくほうほう", "部署間": "ぶしょかん", "配送会社": "はいそうがいしゃ",
        "長距離輸送": "ちょうきょりゆそう", "開始時間": "かいしじかん", "関": "かか",
        "隙間時間": "すきまじかん", "飲酒状態": "いんしゅじょうたい", "駐車場": "ちゅうしゃじょう",
        "高温多湿": "こうおんたしつ", "黙": "だま",
        "一切乗": "いっさいの", "一切取": "いっさいと", "一切持": "いっさいも",
        "一切立": "いっさいた", "一呼吸置": "ひとこきゅうお", "一度決": "いちどき",
        "一日間違": "いちにちまちが", "一番大": "いちばんおお", "一番面白": "いちばんおもしろ",
        "不可避": "ふかひ", "不明": "ふめい", "両者": "りょうしゃ",
        "事務所": "じむしょ", "二倍以上速": "にばいいじょうはや", "交渉": "こうしょう",
        "今感": "いまかん", "付属物": "ふぞくぶつ", "充実感": "じゅうじつかん",
        "入居者": "にゅうきょしゃ", "全員降": "ぜんいんお", "凝縮感": "ぎょうしゅくかん",
        "出席": "しゅっせき", "出席者": "しゅっせきしゃ", "出版": "しゅっぱん",
        "出版社": "しゅっぱんしゃ", "切迫感": "せっぱくかん", "制覇": "せいは",
        "割引券": "わりびきけん", "加入": "かにゅう", "劣": "おと",
        "劣化": "れっか", "十回見": "じゅっかいみ", "協業": "きょうぎょう",
        "反映": "はんえい", "各自": "かくじ", "同一": "どういつ",
        "同居": "どうきょ", "名乗": "なの", "喧嘩": "けんか",
        "四時間": "よじかん", "団体": "だんたい", "大賛成": "だいさんせい",
        "季節感": "きせつかん", "孤立感": "こりつかん", "専門知識": "せんもんちしき",
        "展示": "てんじ", "徴収": "ちょうしゅう", "心構": "こころがま",
        "意地悪": "いじわる", "慰": "なぐさ", "提出": "ていしゅつ",
        "損害": "そんがい", "改装": "かいそう", "政府": "せいふ",
        "敗": "やぶ", "昇進": "しょうしん", "有無": "うむ",
        "段落": "だんらく", "殺到": "さっとう", "毎日二十四時間見続": "まいにちにじゅうよじかんみつづ",
        "毎日出": "まいにちで", "毎日届": "まいにちとど", "毎日必": "まいにちかなら",
        "毎日早": "まいにちはや", "毎食必": "まいしょくかなら", "法律": "ほうりつ",
        "消去": "しょうきょ", "混乱": "こんらん", "湿気": "しっけ",
        "無人": "むじん", "無視": "むし", "片方": "かたほう",
        "画質": "がしつ", "省略": "しょうりゃく", "破棄": "はき",
        "納得感": "なっとくかん", "経費削減": "けいひさくげん", "総責任者": "そうせきにんしゃ",
        "耐震工事": "たいしんこうじ", "脅迫": "きょうはく", "血圧": "けつあつ",
        "補助金": "ほじょきん", "褪": "あ", "要旨": "ようし",
        "見続": "みつづ", "見込": "みこ", "規則": "きそく",
        "観光業": "かんこうぎょう", "言葉少": "ことばずく", "訂正": "ていせい",
        "輸出": "ゆしゅつ", "返却": "へんきゃく", "追及": "ついきゅう",
        "週一度振": "しゅういちどふ", "違反": "いはん", "遠近感": "えんきんかん",
        "避難所": "ひなんじょ", "長年続": "ながねんつづ", "開催場所": "かいさいばしょ",
        "開放": "かいほう", "食器洗": "しょっきあら", "飲酒時": "いんしゅじ",
        "高額": "こうがく",
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
                    # Greedy split of multi-kanji compounds using known dictionary/stem words (e.g. 人間関係 -> 人間 + 関係)
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
    for level, raw_list in [("N3", N3_TEXTS), ("N2", N2_TEXTS), ("N1", N1_TEXTS)]:
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
                "version": 2,
                "content_hash": chash,
                "paragraphs": paras,
                "questions": qs,
            })

        level_mats.sort(key=complexity_score)
        for idx, m in enumerate(level_mats, start=1):
            m["order_index"] = idx
        built_all.extend(level_mats)

    if unannotated_q:
        print("UNANNOTATED QUESTION KANJI:", len(unannotated_q), sorted(unannotated_q))

    # Duplicate check across N5 + N4 + N3 + N2 + N1 (all 290 texts)
    existing_mats = n5_data["materials"] + n4_data["materials"]
    seen_ids = {m["id"] for m in existing_mats}
    seen_titles = {(m["level"], m["title"].strip()) for m in existing_mats}
    seen_hashes = set()
    norms = []
    for m in existing_mats:
        plain = "".join(seg["text"] for p in m["paragraphs"] for s in p["sentences"] for seg in s["segments"])
        norm = normalize_text(plain)
        seen_hashes.add(m.get("content_hash") or hashlib.sha256(norm.encode("utf-8")).hexdigest())
        norms.append((m["id"], norm))

    for m in built_all:
        if m["id"] in seen_ids:
            raise ValueError(f"Duplicate ID: {m['id']}")
        seen_ids.add(m["id"])
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
        "pack_id": "yomizora_pack_n3_n2_n1_v1",
        "pack_version": 2,
        "materials": built_all,
        "dictionary": list(pack_dict_entries.values()),
    }
    OUT_PACK_PATH.write_text(json.dumps(pack_out, ensure_ascii=False, indent=2), encoding="utf-8")

    manifest = json.loads(MANIFEST_PATH.read_text(encoding="utf-8"))
    manifest["manifest_version"] = 5
    packs = [p for p in manifest["packs"] if p["pack_id"] != "yomizora_pack_n3_n2_n1_v1"]
    packs.append({
        "pack_id": "yomizora_pack_n3_n2_n1_v1",
        "pack_version": 2,
        "title": "Уровни JLPT N3–N2–N1 (90 лонгрид-текстов)",
        "description": "По 30 расширенных текстов для JLPT N3 (500–900 симв.), N2 (950–1500 симв.) и N1 (1550–2130 симв.) с 2–4 вопросами",
        "url": "https://raw.githubusercontent.com/Nqam/yomizora-reader/main/content_updates/yomizora_pack_n3_n2_n1_v1.json",
    })
    manifest["packs"] = packs
    MANIFEST_PATH.write_text(json.dumps(manifest, ensure_ascii=False, indent=2), encoding="utf-8")
    print(f"Built N3/N2/N1 pack: {len(built_all)} materials, {len(pack_dict_entries)} dictionary entries!")


if __name__ == "__main__":
    main()
