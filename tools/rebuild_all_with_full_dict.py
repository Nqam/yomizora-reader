import hashlib
import json
import re
from pathlib import Path

from dictionary_data import DICTIONARY_ENTRIES
from extra_dictionary_1 import EXTRA_DICT_1
from extra_dictionary_2 import EXTRA_DICT_2
from extra_dictionary_3 import EXTRA_DICT_3

ROOT = Path(__file__).resolve().parent.parent
PACK_FILES = [
    ROOT / "app" / "src" / "main" / "assets" / "content" / "01_n5_pack.json",
    ROOT / "app" / "src" / "main" / "assets" / "content" / "02_n4_pack.json",
    ROOT / "examples" / "yomizora_addon_n5_n4_v1.json",
]

KANJI_NUMS = {
    "〇": 0, "一": 1, "二": 2, "三": 3, "四": 4,
    "五": 5, "六": 6, "七": 7, "八": 8, "九": 9,
}
MULT_NUMS = {"十": 10, "百": 100, "千": 1000, "万": 10000}

COUNTER_RU = {
    "時": "час(ов)",
    "時半": "половина (час с половиной)",
    "分": "минут(а)",
    "月": "месяц",
    "日": "день / число",
    "日間": "дней (длительность)",
    "日目": "-й день",
    "年": "год / лет",
    "年間": "лет (длительность)",
    "歳": "лет (возраст)",
    "円": "иен",
    "万円": "десятков тысяч иен",
    "円分": "на сумму в ... иен",
    "階": "этаж",
    "回": "раз(а)",
    "枚": "штук (плоские предметы)",
    "本": "штук (длинные предметы)",
    "個": "штук (мелкие предметы)",
    "匹": "животных (счёт)",
    "羽": "птиц (счёт)",
    "人": "человек",
    "名": "человек (вежл.)",
    "名様": "человек / персон (вежл.)",
    "度": "градусов / раз",
    "キロ": "километров / кг",
    "行": "строк(а)",
    "両": "вагон(ов)",
    "か月間": "месяцев (длительность)",
    "つ": "штук",
    "さじ": "ложек",
}

MONTH_RU = {
    1: "январь", 2: "февраль", 3: "март", 4: "апрель",
    5: "май", 6: "июнь", 7: "июль", 8: "август",
    9: "сентябрь", 10: "октябрь", 11: "ноябрь", 12: "декабрь",
}


def parse_kanji_number(s: str) -> int | None:
    if not s:
        return None
    if s.isdigit():
        return int(s)
    total = 0
    current = 0
    for ch in s:
        if ch in KANJI_NUMS:
            current = KANJI_NUMS[ch]
        elif ch in MULT_NUMS:
            m = MULT_NUMS[ch]
            if m == 10000:
                total = (total + current) * 10000
                current = 0
            else:
                total += (current if current != 0 else 1) * m
                current = 0
        else:
            return None
    return total + current


def translate_counter(word: str) -> tuple[str, str] | None:
    m = re.match(
        r"^([一二三四五六七八九十百千万0-9]+)(時|時半|分|月|日|日間|日目|年|年間|歳|円|万円|円分|階|回|枚|本|個|匹|羽|人|名|名様|度|キロ|行|両|か月間|つ|さじ)$",
        word,
    )
    if not m:
        return None
    num = parse_kanji_number(m.group(1))
    unit = m.group(2)
    if num is None:
        return None
    if unit == "月" and num in MONTH_RU:
        return (MONTH_RU[num], "существительное")
    if unit == "時半":
        return (f"{num}:30 (половина {num + 1}-го)", "числительное / время")
    if unit == "万円":
        return (f"{num * 10000} иен ({num} ман)", "числительное")
    return (f"{num} ({COUNTER_RU[unit]})", "числительное со счётным словом")


def make_dict_id(word: str, reading: str) -> str:
    h = hashlib.md5(f"{word}|{reading}".encode("utf-8")).hexdigest()[:10]
    return f"dw_{h}"


def main():
    # Build unified map: (word, reading) -> dict entry
    entries_by_pair: dict[tuple[str, str], dict] = {}
    for e in DICTIONARY_ENTRIES:
        entries_by_pair[(e["word"], e["reading"])] = dict(e)

    for source in (EXTRA_DICT_1, EXTRA_DICT_2, EXTRA_DICT_3):
        for (word, reading), (trans_ru, pos) in source.items():
            if (word, reading) not in entries_by_pair:
                entries_by_pair[(word, reading)] = {
                    "id": make_dict_id(word, reading),
                    "word": word,
                    "reading": reading,
                    "translation_ru": trans_ru,
                    "part_of_speech": pos,
                    "note": None,
                }

    # Check all packs and auto-fill any numeric counters, verify 0 missing
    missing = set()
    for path in PACK_FILES:
        data = json.loads(path.read_text(encoding="utf-8"))
        for mat in data["materials"]:
            for p in mat["paragraphs"]:
                for s in p["sentences"]:
                    for seg in s["segments"]:
                        reading = seg.get("reading")
                        if not reading:
                            continue
                        pair = (seg["text"], reading)
                        if pair not in entries_by_pair:
                            counter_tr = translate_counter(seg["text"])
                            if counter_tr:
                                entries_by_pair[pair] = {
                                    "id": make_dict_id(seg["text"], reading),
                                    "word": seg["text"],
                                    "reading": reading,
                                    "translation_ru": counter_tr[0],
                                    "part_of_speech": counter_tr[1],
                                    "note": None,
                                }
                            else:
                                missing.add(pair)

    if missing:
        print("STILL MISSING:", sorted(missing))
        raise SystemExit(1)

    # Update dict_id on every segment in every pack
    for path in PACK_FILES:
        data = json.loads(path.read_text(encoding="utf-8"))
        for mat in data["materials"]:
            for p in mat["paragraphs"]:
                for s in p["sentences"]:
                    for seg in s["segments"]:
                        reading = seg.get("reading")
                        if reading:
                            seg["dict_id"] = entries_by_pair[(seg["text"], reading)]["id"]

        if path.name == "01_n5_pack.json":
            data["pack_version"] = 2
            data["dictionary"] = list(entries_by_pair.values())
        elif path.name == "02_n4_pack.json":
            data["pack_version"] = 2
            data["dictionary"] = []
        else:
            data["pack_version"] = 2

        path.write_text(json.dumps(data, ensure_ascii=False, indent=2), encoding="utf-8")

    print(f"SUCCESS: 100% dictionary coverage! Total dictionary entries: {len(entries_by_pair)}")


if __name__ == "__main__":
    main()
