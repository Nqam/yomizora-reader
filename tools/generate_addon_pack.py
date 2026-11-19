import hashlib
import json
from pathlib import Path

from addon_texts_n5_n4 import ADDON_N4_TEXTS, ADDON_N5_TEXTS
from content_builder import build_material, normalize_text, text_similarity, validate_pack
from dictionary_data import DICT_LOOKUP

ROOT = Path(__file__).resolve().parent.parent
ASSETS_CONTENT = ROOT / "app" / "src" / "main" / "assets" / "content"
OUT_PATH = ROOT / "examples" / "yomizora_addon_n5_n4_v1.json"


def load_existing_materials() -> list[dict]:
    existing = []
    for p in sorted(ASSETS_CONTENT.glob("*.json")):
        data = json.loads(p.read_text(encoding="utf-8"))
        existing.extend(data.get("materials", []))
    return existing


def main():
    existing = load_existing_materials()
    existing_ids = {m["id"] for m in existing}
    existing_titles = {(m["level"], m["title"]) for m in existing}
    existing_norms = []
    existing_hashes = set()

    for m in existing:
        plain = "".join(
            seg["text"]
            for p in m["paragraphs"]
            for s in p["sentences"]
            for seg in s["segments"]
        )
        norm = normalize_text(plain)
        existing_norms.append((m["id"], norm))
        existing_hashes.add(hashlib.sha256(norm.encode("utf-8")).hexdigest())

    new_materials = []
    for idx, item in enumerate(ADDON_N5_TEXTS, start=56):
        new_materials.append(
            build_material(
                mat_id=item["id"],
                order_index=idx,
                level="N5",
                category=item["category"],
                title=item["title"],
                description=item["description"],
                body_annotated=item["body"],
                questions=item["questions"],
                dict_lookup=DICT_LOOKUP,
            )
        )
    for idx, item in enumerate(ADDON_N4_TEXTS, start=56):
        new_materials.append(
            build_material(
                mat_id=item["id"],
                order_index=idx,
                level="N4",
                category=item["category"],
                title=item["title"],
                description=item["description"],
                body_annotated=item["body"],
                questions=item["questions"],
                dict_lookup=DICT_LOOKUP,
            )
        )

    # Validate internal uniqueness and structure
    validate_pack(new_materials)

    # Cross-validate against ALL existing starter materials
    for m in new_materials:
        assert m["id"] not in existing_ids, f"ID collision with starter pack: {m['id']}"
        assert (m["level"], m["title"]) not in existing_titles, f"Title collision: {m['title']}"
        plain = "".join(
            seg["text"]
            for p in m["paragraphs"]
            for s in p["sentences"]
            for seg in s["segments"]
        )
        norm = normalize_text(plain)
        h = hashlib.sha256(norm.encode("utf-8")).hexdigest()
        assert h not in existing_hashes, f"Exact text collision for {m['id']}"
        for eid, enorm in existing_norms:
            sim = text_similarity(norm, enorm)
            assert sim < 0.85, f"Near-duplicate with starter {eid}: {m['id']} (sim={sim:.2f})"

    addon_pack = {
        "pack_id": "yomizora_addon_n5_n4_v1",
        "pack_version": 1,
        "materials": new_materials,
        "dictionary": [],
    }
    OUT_PATH.write_text(json.dumps(addon_pack, ensure_ascii=False, indent=2), encoding="utf-8")
    print(f"Generated addon pack at {OUT_PATH} with {len(ADDON_N5_TEXTS)} N5 + {len(ADDON_N4_TEXTS)} N4 = {len(new_materials)} unique texts.")


if __name__ == "__main__":
    main()
