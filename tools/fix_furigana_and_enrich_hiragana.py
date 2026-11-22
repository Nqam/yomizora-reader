import hashlib
import json
import re
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from conversational_dict import CONVERSATIONAL_EXPRESSIONS
from kana_and_quiz_enricher import HIRAGANA_DICT, KATAKANA_DICT, make_dict_id

ROOT = Path(__file__).resolve().parent.parent
PACKS = [
    ROOT / "app" / "src" / "main" / "assets" / "content" / "01_n5_pack.json",
    ROOT / "app" / "src" / "main" / "assets" / "content" / "02_n4_pack.json",
    ROOT / "content_updates" / "yomizora_pack_n3_n2_n1_v1.json",
    ROOT / "content_updates" / "yomizora_pack_n3_n2_n1_v2.json",
]
MANIFEST_PATH = ROOT / "content_updates" / "manifest.json"

EXTRA_HIRAGANA_DICT = {
    "たとえば": ("например", "наречие"),
    "ただし": ("однако, правда (оговорка)", "союз"),
    "つまり": ("то есть, короче говоря", "союз"),
    "おそらく": ("вероятно, скорее всего", "наречие"),
    "まさに": ("именно, как раз", "наречие"),
    "すでに": ("уже", "наречие"),
    "とりわけ": ("особенно, в частности", "наречие"),
    "むしろ": ("скорее, вернее", "наречие"),
    "かえって": ("наоборот, напротив", "наречие"),
    "まるで": ("словно, будто", "наречие"),
    "ほとんど": ("почти (всё/не)", "наречие"),
    "すっかり": ("совсем, полностью", "наречие"),
    "ますます": ("всё больше и больше", "наречие"),
    "いよいよ": ("наконец-то, вот-вот", "наречие"),
    "わざと": ("нарочно, специально", "наречие"),
    "じっと": ("пристально, неподвижно", "наречие"),
    "そっと": ("тихонько, осторожно", "наречие"),
    "ぐっと": ("резко, заметно; сдержав (слёзы/дыхание)", "наречие"),
    "もっと": ("больше, ещё", "наречие"),
    "きっと": ("наверняка, обязательно", "наречие"),
    "とうとう": ("в конце концов, наконец", "наречие"),
    "それぞれ": ("каждый, по отдельности", "наречие / сущ."),
    "さまざま": ("разнообразный, разный", "な-прилагательное"),
    "だいぶ": ("довольно, значительно", "наречие"),
    "かなり": ("довольно-таки, весьма", "наречие"),
    "すごく": ("очень, невероятно", "наречие"),
    "ときどき": ("иногда, время от времени", "наречие"),
    "いっしょに": ("вместе", "наречие"),
    "だいたい": ("примерно, в основном", "наречие"),
    "たしかに": ("действительно, бесспорно", "наречие"),
    "ごちそうさま": ("спасибо за угощение!", "выражение"),
    "いただきます": ("приятного аппетита / с благодарностью принимаю пищу", "выражение"),
    "おかえり": ("с возвращением!", "приветствие"),
    "ただいま": ("я вернулся!", "приветствие"),
    "がんばって": ("удачи! старайся!", "выражение"),
    "もったいない": ("жаль тратить впустую, расточительно", "выражение"),
    "しかたがない": ("ничего не поделаешь", "выражение"),
    "ぴったり": ("идеально подходит, вплотную", "наречие"),
    "はっきり": ("чётко, ясно", "наречие"),
    "のんびり": ("неспешно, расслабленно", "наречие"),
    "にこにこ": ("с улыбкой, приветливо", "наречие"),
    "ふわふわ": ("мягкий, воздушный", "наречие"),
    "たのしい": ("весёлый, радостный", "い-прилагательное"),
    "うれしい": ("радостный, счастливый", "い-прилагательное"),
    "やさしい": ("добрый; лёгкий", "い-прилагательное"),
    "むずかしい": ("трудный, сложный", "い-прилагательное"),
    "あたらしい": ("новый", "い-прилагательное"),
    "おおきい": ("большой", "い-прилагательное"),
    "ちいさい": ("маленький", "い-прилагательное"),
    "げんき": ("бодрый, здоровый", "な-прилагательное"),
    "だいじょうぶ": ("всё в порядке, нормально", "な-прилагательное"),
    "ひとまず": ("пока что, на данный момент", "наречие"),
    "そもそも": ("вообще-то, изначально", "наречие"),
    "いわば": ("так сказать, образно говоря", "наречие"),
    "あるいは": ("или, либо", "союз"),
    "したがって": ("следовательно, поэтому", "союз"),
    "すなわち": ("то есть, иначе говоря", "союз"),
    "しかしながら": ("тем не менее, однако", "союз"),
    "なぜなら": ("потому что", "союз"),
    "むしろ": ("скорее", "наречие"),
    "せっかく": ("раз уж так вышло / с таким трудом", "разговорное выражение"),
    "なんとなく": ("почему-то, сам не знаю почему", "разговорное выражение"),
    "しょうがない": ("ничего не поделаешь", "разговорное выражение"),
    "なるほど": ("вот оно что! понятно!", "разговорное выражение"),
    "やっぱり": ("как и ожидалось, всё-таки", "разговорное выражение"),
    "まさか": ("неужели?! не может быть!", "разговорное выражение"),
    "とにかく": ("в любом случае, главное...", "разговорное выражение"),
    "ちなみに": ("кстати говоря, между прочим", "разговорное выражение"),
    "あいかわらず": ("как всегда, по-прежнему", "разговорное выражение"),
    "おつかれさま": ("спасибо за работу!", "разговорное выражение"),
    "おかげさまで": ("благодаря вам, всё хорошо", "разговорное выражение"),
    "そろそろ": ("пожалуй, пора уже", "разговорное выражение"),
    "まあまあ": ("ну так себе, более-менее", "разговорное выражение"),
    "そうですね": ("да, пожалуй...", "разговорное выражение"),
    "ぜんぜん": ("совсем (не)", "разговорное выражение"),
    "たぶん": ("наверное, скорее всего", "разговорное выражение"),
    "ほんとうに": ("правда, действительно", "разговорное выражение"),
    "よかった": ("как хорошо! слава богу!", "разговорное выражение"),
    "わざわざ": ("специально, нарочно", "разговорное выражение"),
    "めったに": ("редко, почти никогда (с отриц.)", "разговорное выражение"),
    "どんどん": ("стремительно, всё больше", "разговорное выражение"),
    "さすが": ("как и ожидалось! вот это уровень!", "разговорное выражение"),
    "ほっと": ("с облегчением", "разговорное выражение"),
    "びっくり": ("сильно удивиться", "разговорное выражение"),
    "がっかり": ("расстроиться, разочароваться", "разговорное выражение"),
}

# Exact furigana fixes: (material_id_or_None, segment_text, wrong_reading) -> correct_reading
FURIGANA_FIXES = {
    ("具体的", "ぐたい"): "ぐたいてき",
    ("年齢", "ねんだい"): "ねんれい",
    ("発展", "はったつ"): "はってん",
    ("現在", "げんだい"): "げんざい",
    ("数字", "すうち"): "すうじ",
    ("芸術", "げんじゅつ"): "げいじゅつ",
    ("集中", "しゅううちゅう"): "しゅうちゅう",
    ("成果", "せいかい"): "せいか",
    ("調味料", "ちょうみょうりょう"): "ちょうみりょう",
    ("安易", "やすい"): "あんい",
    ("理由", "りゅう"): "りゆう",
    ("同種", "どうし"): "どうしゅ",
    ("店番", "みせば"): "みせばん",
    ("早口", "はやぐち"): "はやくち",
    ("気配", "きくば"): "きくばり",
    ("塩味", "しおみ"): "しおあじ",
    ("塩味", "えんみ"): "しおあじ",
    ("身体", "からだ"): "しんたい",
}


def main():
    # Load all packs
    pack_data = {p: json.loads(p.read_text(encoding="utf-8")) for p in PACKS}

    # Build unified dict lookup
    master_dict = {}
    word_to_entry = {}
    for p, data in pack_data.items():
        for e in data.get("dictionary", []):
            master_dict[e["id"]] = e
            if e["word"] not in word_to_entry:
                word_to_entry[e["word"]] = e

    # Add EXTRA_HIRAGANA_DICT + CONVERSATIONAL_EXPRESSIONS
    new_kana_entries = {}
    for w, (trans, pos) in {**HIRAGANA_DICT, **EXTRA_HIRAGANA_DICT}.items():
        if w in word_to_entry:
            e = word_to_entry[w]
            if pos == "разговорное выражение":
                e["part_of_speech"] = pos
                e["translation_ru"] = trans
            new_kana_entries[w] = e
        else:
            did = make_dict_id("dh", w)
            while did in master_dict:
                did += "x"
            e = {
                "id": did,
                "word": w,
                "reading": w,
                "translation_ru": trans,
                "part_of_speech": pos,
                "note": None,
            }
            master_dict[did] = e
            word_to_entry[w] = e
            new_kana_entries[w] = e

    for key, (dw, dr, trans) in CONVERSATIONAL_EXPRESSIONS.items():
        if dw in word_to_entry:
            e = word_to_entry[dw]
            e["part_of_speech"] = "разговорное выражение"
            e["translation_ru"] = trans
            if not re.search(r"[\u4e00-\u9fff]", key):
                new_kana_entries[key] = e

    kana_words_sorted = sorted(new_kana_entries.keys(), key=len, reverse=True)
    kana_pattern = re.compile("|".join(re.escape(w) for w in kana_words_sorted))

    fixed_furigana_count = 0
    enriched_kana_count = 0

    for p, data in pack_data.items():
        pack_added_dict = {}
        for e in data.get("dictionary", []):
            pack_added_dict[e["id"]] = e

        for m in data["materials"]:
            m["version"] = m.get("version", 1) + 1
            for para in m["paragraphs"]:
                for sent in para["sentences"]:
                    new_segs = []
                    for seg in sent["segments"]:
                        t = seg["text"]
                        r = seg.get("reading")
                        if r and (t, r) in FURIGANA_FIXES:
                            seg["reading"] = FURIGANA_FIXES[(t, r)]
                            fixed_furigana_count += 1
                        if r is None and not seg.get("dict_id") and t:
                            pos = 0
                            matched_any = False
                            for match in kana_pattern.finditer(t):
                                matched_any = True
                                if match.start() > pos:
                                    new_segs.append({"text": t[pos:match.start()], "reading": None})
                                kw = match.group(0)
                                ke = new_kana_entries[kw]
                                pack_added_dict[ke["id"]] = ke
                                new_segs.append({"text": kw, "reading": None, "dict_id": ke["id"]})
                                enriched_kana_count += 1
                                pos = match.end()
                            if matched_any:
                                if pos < len(t):
                                    new_segs.append({"text": t[pos:], "reading": None})
                            else:
                                new_segs.append(seg)
                        else:
                            new_segs.append(seg)
                    sent["segments"] = new_segs

        if p == PACKS[0]:
            data["dictionary"] = list(master_dict.values())
            data["pack_version"] = data.get("pack_version", 1) + 1
        elif p == PACKS[1]:
            data["pack_version"] = data.get("pack_version", 1) + 1
        else:
            data["dictionary"] = list(pack_added_dict.values())
            data["pack_version"] = data.get("pack_version", 1) + 1

        p.write_text(json.dumps(data, ensure_ascii=False, indent=2), encoding="utf-8")

    # Update manifest versions to match updated remote packs
    manifest = json.loads(MANIFEST_PATH.read_text(encoding="utf-8"))
    manifest["manifest_version"] = manifest.get("manifest_version", 1) + 1
    v1_ver = pack_data[PACKS[2]]["pack_version"]
    v2_ver = pack_data[PACKS[3]]["pack_version"]
    for pk in manifest["packs"]:
        if pk["pack_id"] == "yomizora_pack_n3_n2_n1_v1":
            pk["pack_version"] = v1_ver
        elif pk["pack_id"] == "yomizora_pack_n3_n2_n1_v2":
            pk["pack_version"] = v2_ver
    MANIFEST_PATH.write_text(json.dumps(manifest, ensure_ascii=False, indent=2), encoding="utf-8")

    print(f"Fixed furigana segments: {fixed_furigana_count}")
    print(f"Enriched hiragana segments with dict_id: {enriched_kana_count}")
    print(f"Total dictionary entries: {len(master_dict)}")


if __name__ == "__main__":
    main()
