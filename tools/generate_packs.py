import json
from pathlib import Path

from content_builder import build_material, validate_pack
from dictionary_data import DICTIONARY_ENTRIES, DICT_LOOKUP
from n4_texts_part1 import N4_TEXTS_PART1
from n4_texts_part2 import N4_TEXTS_PART2
from n5_texts_part1 import N5_TEXTS_PART1
from n5_texts_part2 import N5_TEXTS_PART2

ROOT = Path(__file__).resolve().parent.parent
ASSETS_CONTENT = ROOT / "app" / "src" / "main" / "assets" / "content"
ASSETS_CONTENT.mkdir(parents=True, exist_ok=True)


def convert_list(items: list[dict], level: str) -> list[dict]:
    result = []
    for idx, item in enumerate(items, start=1):
        result.append(
            build_material(
                mat_id=item["id"],
                order_index=idx,
                level=level,
                category=item["category"],
                title=item["title"],
                description=item["description"],
                body_annotated=item["body"],
                questions=item["questions"],
                dict_lookup=DICT_LOOKUP,
            )
        )
    return result


def main():
    n5_raw = N5_TEXTS_PART1 + N5_TEXTS_PART2
    n4_raw = N4_TEXTS_PART1 + N4_TEXTS_PART2

    n5_materials = convert_list(n5_raw, "N5")
    n4_materials = convert_list(n4_raw, "N4")

    validate_pack(n5_materials + n4_materials)

    n5_pack = {
        "pack_id": "starter_n5_v1",
        "pack_version": 1,
        "materials": n5_materials,
        "dictionary": DICTIONARY_ENTRIES,
    }
    n4_pack = {
        "pack_id": "starter_n4_v1",
        "pack_version": 1,
        "materials": n4_materials,
        "dictionary": [],
    }

    (ASSETS_CONTENT / "01_n5_pack.json").write_text(
        json.dumps(n5_pack, ensure_ascii=False, indent=2), encoding="utf-8"
    )
    (ASSETS_CONTENT / "02_n4_pack.json").write_text(
        json.dumps(n4_pack, ensure_ascii=False, indent=2), encoding="utf-8"
    )

    print(f"Generated {len(n5_materials)} N5 materials and {len(n4_materials)} N4 materials (Total: {len(n5_materials) + len(n4_materials)}).")
    print(f"Dictionary entries: {len(DICTIONARY_ENTRIES)}")


if __name__ == "__main__":
    main()
