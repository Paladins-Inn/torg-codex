#!/usr/bin/env python3
# Copyright (c) 2026. Roland T. Lichti
# AGPL-3.0-or-later
"""
Migriert YAML-Quelldateien in CSV-Dateien für Liquibase loadData.
- YAML-Feld "id" (oder "name") wird zur CSV-Spalte "name"
- Eine neue UUID wird für die CSV-Spalte "id" generiert
- Referenzen zwischen Entitäten werden über einen uuid_cache aufgelöst
"""
import os, re, csv, uuid, yaml
from pathlib import Path
BASE_DIR = Path(__file__).parent / "torg-codex-data/src/main/resources/db"
LOAD_DIR = BASE_DIR / "load"
LOAD_DIR.mkdir(exist_ok=True)
uuid_cache: dict = {}
def get_or_create_uuid(name: str) -> str:
    key = str(name).strip().lower()
    if key not in uuid_cache:
        uuid_cache[key] = str(uuid.uuid4())
    return uuid_cache[key]
def get_entity_name(entry: dict) -> str:
    raw = entry.get("id") or entry.get("name") or ""
    return str(raw).strip()
def clean(value) -> str:
    if value is None:
        return ""
    s = str(value).strip()
    if " #" in s:
        s = s.split(" #")[0].strip()
    return s
def to_list(value) -> list:
    if value is None:
        return []
    if isinstance(value, list):
        return value
    return [value]
csv_writers: dict = {}
def get_writer(table_name: str, columns: list):
    if table_name not in csv_writers:
        filepath = LOAD_DIR / f"{table_name}.csv"
        f = open(filepath, "w", newline="", encoding="utf-8")
        writer = csv.DictWriter(f, fieldnames=columns, delimiter=";",
                                extrasaction="ignore", quoting=csv.QUOTE_MINIMAL)
        writer.writeheader()
        csv_writers[table_name] = (f, writer, columns)
    return csv_writers[table_name][1]
def write_row(table_name: str, columns: list, row: dict):
    w = get_writer(table_name, columns)
    clean_row = {col: row.get(col, "") for col in columns}
    w.writerow(clean_row)
def process_article(entry, product_hint=""):
    name = get_entity_name(entry)
    eid = get_or_create_uuid(name)
    write_row("torg_article", ["id","name","clearance_level","text"], {
        "id": eid, "name": entry.get("name", name),
        "clearance_level": clean(entry.get("clearanceLevel")),
        "text": clean(entry.get("text")),
    })
def process_cosm(entry, product_hint=""):
    name = get_entity_name(entry)
    eid = get_or_create_uuid(name)
    world_laws = entry.get("worldLaws") or entry.get("world_laws")
    if isinstance(world_laws, list):
        world_laws = "; ".join(str(w) for w in world_laws)
    write_row("torg_cosm", ["id","name","clearance_level","text","world_laws"], {
        "id": eid, "name": entry.get("name", name),
        "clearance_level": clean(entry.get("clearanceLevel")),
        "text": clean(entry.get("text")), "world_laws": clean(world_laws),
    })
    for prod in to_list(entry.get("product")):
        write_row("torg_cosm_products", ["cosm_id","product"], {"cosm_id": eid, "product": clean(prod)})
    axioms = entry.get("axioms") or {}
    if isinstance(axioms, dict):
        for ax_name, ax_val in axioms.items():
            write_row("torg_cosm_axioms", ["cosm_id","axiom","value"],
                      {"cosm_id": eid, "axiom": ax_name, "value": clean(ax_val)})
def process_item(entry, product_hint=""):
    name = get_entity_name(entry)
    eid = get_or_create_uuid(name)
    axioms = entry.get("axioms") or {}
    features = entry.get("features")
    if isinstance(features, dict):
        features = "; ".join(f"{k}: {v}" for k, v in features.items())
    elif isinstance(features, list):
        features = "; ".join(str(f) for f in features)
    write_row("torg_item", ["id","name","clearance_level","type","cosm","axiom_tech","axiom_magic",
                             "price","bonus","ammo","range","features","additional_features","text"], {
        "id": eid, "name": entry.get("name", name),
        "clearance_level": clean(entry.get("clearanceLevel")),
        "type": clean(entry.get("type")), "cosm": clean(entry.get("cosm")),
        "axiom_tech": clean(axioms.get("TECH") if isinstance(axioms, dict) else None),
        "axiom_magic": clean(axioms.get("MAGIC") if isinstance(axioms, dict) else None),
        "price": clean(entry.get("price")), "bonus": clean(entry.get("bonus")),
        "ammo": clean(entry.get("ammo")), "range": clean(entry.get("range")),
        "features": clean(features),
        "additional_features": clean(entry.get("additionalFeatures") or entry.get("additional_features")),
        "text": clean(entry.get("text")),
    })
    for prod in to_list(entry.get("product") or product_hint):
        if prod:
            write_row("torg_item_products", ["item_id","product"], {"item_id": eid, "product": clean(prod)})
def _dn_parts(entry):
    dn = entry.get("dn") or {}
    if isinstance(dn, dict):
        return clean(dn.get("level")), clean(dn.get("text"))
    return "", clean(dn)
def process_miracle(entry, product_hint=""):
    name = get_entity_name(entry)
    eid = get_or_create_uuid(name)
    dn_level, dn_text = _dn_parts(entry)
    write_row("torg_miracle", ["id","name","clearance_level","axiom","casting_time",
                                "dn_level","dn_text","range","duration","text"], {
        "id": eid, "name": entry.get("name", name),
        "clearance_level": clean(entry.get("clearanceLevel")),
        "axiom": clean(entry.get("axiom")), "casting_time": clean(entry.get("castingTime")),
        "dn_level": dn_level, "dn_text": dn_text,
        "range": clean(entry.get("range")), "duration": clean(entry.get("duration")),
        "text": clean(entry.get("text")),
    })
    for prod in to_list(entry.get("product") or product_hint):
        if prod:
            write_row("torg_miracle_products", ["miracle_id","product"], {"miracle_id": eid, "product": clean(prod)})
    for sk, val in (entry.get("requiredSkill") or {}).items():
        write_row("torg_miracle_required_skills", ["miracle_id","skill","required_value"],
                  {"miracle_id": eid, "skill": sk, "required_value": clean(val)})
def process_miraclelist(entry, product_hint=""):
    name = get_entity_name(entry)
    eid = get_or_create_uuid(name)
    write_row("torg_miracle_list", ["id","name","clearance_level","cosm","unlocking_perk","text","notes","disable_if"], {
        "id": eid, "name": entry.get("name", name),
        "clearance_level": clean(entry.get("clearanceLevel")),
        "cosm": clean(entry.get("cosm")), "unlocking_perk": clean(entry.get("unlockingPerk")),
        "text": clean(entry.get("text")), "notes": clean(entry.get("notes")),
        "disable_if": clean(entry.get("disableIf")),
    })
    for prod in to_list(entry.get("product") or product_hint):
        if prod:
            write_row("torg_miracle_list_products", ["list_id","product"], {"list_id": eid, "product": clean(prod)})
    for idx, miracle_name in enumerate(to_list(entry.get("miracles"))):
        mid = get_or_create_uuid(str(miracle_name))
        write_row("torg_miracle_list_entries", ["list_id","miracle_id","entry_order"],
                  {"list_id": eid, "miracle_id": mid, "entry_order": idx})
def process_perk(entry, product_hint=""):
    name = get_entity_name(entry)
    eid = get_or_create_uuid(name)
    contradiction = entry.get("contradiction") or False
    write_row("torg_perk", ["id","name","clearance_level","contradiction","cosm","perk_group","prerequisites","text"], {
        "id": eid, "name": entry.get("name", name),
        "clearance_level": clean(entry.get("clearanceLevel")),
        "contradiction": "true" if contradiction else "false",
        "cosm": clean(entry.get("cosm")), "perk_group": clean(entry.get("group")),
        "prerequisites": clean(entry.get("prerequisites")), "text": clean(entry.get("text")),
    })
    for prod in to_list(entry.get("product") or product_hint):
        if prod:
            write_row("torg_perk_products", ["perk_id","product"], {"perk_id": eid, "product": clean(prod)})
def process_perkgroup(entry, product_hint=""):
    name = get_entity_name(entry)
    eid = get_or_create_uuid(name)
    write_row("torg_perk_group", ["id","name","clearance_level","text","infos"], {
        "id": eid, "name": entry.get("name", name),
        "clearance_level": clean(entry.get("clearanceLevel")),
        "text": clean(entry.get("text")), "infos": clean(entry.get("infos")),
    })
    for prod in to_list(entry.get("product") or product_hint):
        if prod:
            write_row("torg_perk_group_products", ["group_id","product"], {"group_id": eid, "product": clean(prod)})
def process_power(entry, product_hint=""):
    name = get_entity_name(entry)
    eid = get_or_create_uuid(name)
    dn_level, dn_text = _dn_parts(entry)
    enhancements = entry.get("enhancements")
    if isinstance(enhancements, list):
        enhancements = "; ".join(str(e) for e in enhancements)
    limitations = entry.get("limitations")
    if isinstance(limitations, list):
        limitations = "; ".join(str(l) for l in limitations)
    write_row("torg_power", ["id","name","clearance_level","axiom","casting_time","dn_level","dn_text",
                              "range","duration","text","enhancements","limitations"], {
        "id": eid, "name": entry.get("name", name),
        "clearance_level": clean(entry.get("clearanceLevel")),
        "axiom": clean(entry.get("axiom")), "casting_time": clean(entry.get("castingTime")),
        "dn_level": dn_level, "dn_text": dn_text,
        "range": clean(entry.get("range")), "duration": clean(entry.get("duration")),
        "text": clean(entry.get("text")), "enhancements": clean(enhancements),
        "limitations": clean(limitations),
    })
    for prod in to_list(entry.get("product") or product_hint):
        if prod:
            write_row("torg_power_products", ["power_id","product"], {"power_id": eid, "product": clean(prod)})
    for sk, val in (entry.get("requiredSkill") or {}).items():
        write_row("torg_power_required_skills", ["power_id","skill","required_value"],
                  {"power_id": eid, "skill": sk, "required_value": clean(val)})
def process_powerlist(entry, product_hint=""):
    name = get_entity_name(entry)
    eid = get_or_create_uuid(name)
    write_row("torg_power_list", ["id","name","clearance_level","cosm","unlocking_perk","text","notes","disable_if"], {
        "id": eid, "name": entry.get("name", name),
        "clearance_level": clean(entry.get("clearanceLevel")),
        "cosm": clean(entry.get("cosm")), "unlocking_perk": clean(entry.get("unlockingPerk")),
        "text": clean(entry.get("text")), "notes": clean(entry.get("notes")),
        "disable_if": clean(entry.get("disableIf")),
    })
    for prod in to_list(entry.get("product") or product_hint):
        if prod:
            write_row("torg_power_list_products", ["list_id","product"], {"list_id": eid, "product": clean(prod)})
    for idx, power_name in enumerate(to_list(entry.get("powers"))):
        pid = get_or_create_uuid(str(power_name))
        write_row("torg_power_list_entries", ["list_id","power_id","entry_order"],
                  {"list_id": eid, "power_id": pid, "entry_order": idx})
def process_race(entry, product_hint=""):
    name = get_entity_name(entry)
    eid = get_or_create_uuid(name)
    abilities = entry.get("abilities")
    if isinstance(abilities, list):
        abilities = "; ".join(str(a) for a in abilities)
    elif isinstance(abilities, dict):
        abilities = "; ".join(f"{k}: {v}" for k, v in abilities.items())
    major = entry.get("major") or False
    write_row("torg_race", ["id","name","clearance_level","major","abilities","text","perk_text"], {
        "id": eid, "name": entry.get("name", name),
        "clearance_level": clean(entry.get("clearanceLevel")),
        "major": "true" if major else "false",
        "abilities": clean(abilities), "text": clean(entry.get("text")),
        "perk_text": clean(entry.get("perkText")),
    })
    for prod in to_list(entry.get("product") or product_hint):
        if prod:
            write_row("torg_race_products", ["race_id","product"], {"race_id": eid, "product": clean(prod)})
    for attr, val in (entry.get("attributeLimits") or {}).items():
        write_row("torg_race_attribute_limits", ["race_id","attribute","max_value"],
                  {"race_id": eid, "attribute": attr, "max_value": clean(val)})
def process_shard(entry, product_hint=""):
    name = get_entity_name(entry)
    eid = get_or_create_uuid(name)
    powers = entry.get("powers")
    if isinstance(powers, list):
        powers = "; ".join(str(p) for p in powers)
    restrictions = entry.get("restrictions")
    if isinstance(restrictions, list):
        restrictions = "; ".join(str(r) for r in restrictions)
    write_row("torg_shard", ["id","name","clearance_level","cosm","possibilities","tapping_difficulty",
                              "purpose","text","powers","restrictions"], {
        "id": eid, "name": entry.get("name", name),
        "clearance_level": clean(entry.get("clearanceLevel")),
        "cosm": clean(entry.get("cosm")), "possibilities": clean(entry.get("possibilities")),
        "tapping_difficulty": clean(entry.get("tappingDifficulty")),
        "purpose": clean(entry.get("purpose")), "text": clean(entry.get("text")),
        "powers": clean(powers), "restrictions": clean(restrictions),
    })
    for prod in to_list(entry.get("product") or product_hint):
        if prod:
            write_row("torg_shard_products", ["shard_id","product"], {"shard_id": eid, "product": clean(prod)})
def process_spell(entry, product_hint=""):
    name = get_entity_name(entry)
    eid = get_or_create_uuid(name)
    dn_level, dn_text = _dn_parts(entry)
    write_row("torg_spell", ["id","name","clearance_level","axiom","casting_time",
                              "dn_level","dn_text","range","duration","text"], {
        "id": eid, "name": entry.get("name", name),
        "clearance_level": clean(entry.get("clearanceLevel")),
        "axiom": clean(entry.get("axiom")), "casting_time": clean(entry.get("castingTime")),
        "dn_level": dn_level, "dn_text": dn_text,
        "range": clean(entry.get("range")), "duration": clean(entry.get("duration")),
        "text": clean(entry.get("text")),
    })
    for prod in to_list(entry.get("product") or product_hint):
        if prod:
            write_row("torg_spell_products", ["spell_id","product"], {"spell_id": eid, "product": clean(prod)})
    for sk, val in (entry.get("requiredSkill") or {}).items():
        write_row("torg_spell_required_skills", ["spell_id","skill","required_value"],
                  {"spell_id": eid, "skill": sk, "required_value": clean(val)})
def process_spelllist(entry, product_hint=""):
    name = get_entity_name(entry)
    eid = get_or_create_uuid(name)
    write_row("torg_spell_list", ["id","name","clearance_level","cosm","unlocking_perk","text","notes","disable_if"], {
        "id": eid, "name": entry.get("name", name),
        "clearance_level": clean(entry.get("clearanceLevel")),
        "cosm": clean(entry.get("cosm")), "unlocking_perk": clean(entry.get("unlockingPerk")),
        "text": clean(entry.get("text")), "notes": clean(entry.get("notes")),
        "disable_if": clean(entry.get("disableIf")),
    })
    for prod in to_list(entry.get("product") or product_hint):
        if prod:
            write_row("torg_spell_list_products", ["list_id","product"], {"list_id": eid, "product": clean(prod)})
    for idx, spell_name in enumerate(to_list(entry.get("spells"))):
        sid = get_or_create_uuid(str(spell_name))
        write_row("torg_spell_list_entries", ["list_id","spell_id","entry_order"],
                  {"list_id": eid, "spell_id": sid, "entry_order": idx})
def process_tag(entry, product_hint=""):
    name = get_entity_name(entry)
    eid = get_or_create_uuid(name)
    parent_name = entry.get("parent")
    parent_id = get_or_create_uuid(str(parent_name)) if parent_name else ""
    write_row("torg_tag", ["id","name","clearance_level","parent_id"], {
        "id": eid, "name": entry.get("name", name),
        "clearance_level": clean(entry.get("clearanceLevel")), "parent_id": parent_id,
    })
def process_threat(entry, product_hint=""):
    name = get_entity_name(entry)
    eid = get_or_create_uuid(name)
    attributes = entry.get("attributes") or {}
    move = entry.get("move") or {}
    if not isinstance(move, dict):
        move = {"walk": move}
    unique = entry.get("unique") or False
    write_row("torg_threat", [
        "id","name","clearance_level","cosm","unique","sub_name","quote","text",
        "attr_charisma","attr_dexterity","attr_mind","attr_spirit","attr_strength",
        "move_walk","move_fly","move_swim","tough","shock","wounds","possibilities"
    ], {
        "id": eid, "name": entry.get("name", name),
        "clearance_level": clean(entry.get("clearanceLevel")),
        "cosm": clean(entry.get("cosm")),
        "unique": "true" if unique else "false",
        "sub_name": clean(entry.get("subName")), "quote": clean(entry.get("quote")),
        "text": clean(entry.get("text")),
        "attr_charisma": clean(attributes.get("CHARISMA")),
        "attr_dexterity": clean(attributes.get("DEXTERITY")),
        "attr_mind": clean(attributes.get("MIND")),
        "attr_spirit": clean(attributes.get("SPIRIT")),
        "attr_strength": clean(attributes.get("STRENGTH")),
        "move_walk": clean(move.get("walk") or move.get("WALK")),
        "move_fly": clean(move.get("fly") or move.get("FLY")),
        "move_swim": clean(move.get("swim") or move.get("SWIM")),
        "tough": clean(entry.get("tough")), "shock": clean(entry.get("shock")),
        "wounds": clean(entry.get("wounds")), "possibilities": clean(entry.get("possibilities")),
    })
    for prod in to_list(entry.get("product") or product_hint):
        if prod:
            write_row("torg_threat_products", ["threat_id","product"], {"threat_id": eid, "product": clean(prod)})
    skills = entry.get("skills") or {}
    if isinstance(skills, dict):
        for sk, val in skills.items():
            write_row("torg_threat_skills", ["threat_id","skill","value"],
                      {"threat_id": eid, "skill": sk, "value": clean(val)})
    special_abilities = entry.get("specialAbilities")
    if isinstance(special_abilities, dict):
        # Format: {ability_name: description}
        for ab_name, ab_desc in special_abilities.items():
            write_row("torg_threat_special_abilities", ["threat_id","ability_name","ability_description"],
                      {"threat_id": eid, "ability_name": clean(ab_name), "ability_description": clean(ab_desc)})
    elif isinstance(special_abilities, list):
        for special in special_abilities:
            if isinstance(special, dict):
                # Format: [{name: ..., description: ...}] or {ability_name: description}
                if "name" in special:
                    write_row("torg_threat_special_abilities", ["threat_id","ability_name","ability_description"],
                              {"threat_id": eid, "ability_name": clean(special.get("name")),
                               "ability_description": clean(special.get("description") or special.get("text"))})
                else:
                    for ab_name, ab_desc in special.items():
                        write_row("torg_threat_special_abilities", ["threat_id","ability_name","ability_description"],
                                  {"threat_id": eid, "ability_name": clean(ab_name), "ability_description": clean(ab_desc)})
            elif isinstance(special, str):
                write_row("torg_threat_special_abilities", ["threat_id","ability_name","ability_description"],
                          {"threat_id": eid, "ability_name": special, "ability_description": ""})
def process_vehicle(entry, product_hint=""):
    name = get_entity_name(entry)
    eid = get_or_create_uuid(name)
    speed = entry.get("speed") or {}
    if not isinstance(speed, dict):
        speed = {"value": speed}
    unique = entry.get("unique") or False
    axioms = entry.get("axioms") or {}
    write_row("torg_vehicle", [
        "id","name","clearance_level","type","cosm","axiom_tech","unique",
        "speed","speed_value","speed_mod","size","passengers",
        "maneuver_rating","wounds","tough","price","text"
    ], {
        "id": eid, "name": entry.get("name", name),
        "clearance_level": clean(entry.get("clearanceLevel")),
        "type": clean(entry.get("type")), "cosm": clean(entry.get("cosm")),
        "axiom_tech": clean(axioms.get("TECH") if isinstance(axioms, dict) else None),
        "unique": "true" if unique else "false",
        "speed": clean(speed.get("speed") or speed.get("value")),
        "speed_value": clean(speed.get("value")), "speed_mod": clean(speed.get("mod")),
        "size": clean(entry.get("size")), "passengers": clean(entry.get("passengers")),
        "maneuver_rating": clean(entry.get("maneuverRating")),
        "wounds": clean(entry.get("wounds")), "tough": clean(entry.get("tough")),
        "price": clean(entry.get("price")), "text": clean(entry.get("text")),
    })
    for prod in to_list(entry.get("product") or product_hint):
        if prod:
            write_row("torg_vehicle_products", ["vehicle_id","product"], {"vehicle_id": eid, "product": clean(prod)})
    for weapon in to_list(entry.get("weapons") or entry.get("weaponry")):
        if isinstance(weapon, dict):
            wname = clean(weapon.get("name") or weapon.get("id") or "")
            wid = get_or_create_uuid(wname) if wname else ""
            write_row("torg_vehicle_weaponry", ["vehicle_id","weapon_id","ammo","amount"],
                      {"vehicle_id": eid, "weapon_id": wid,
                       "ammo": clean(weapon.get("ammo")), "amount": clean(weapon.get("amount"))})
ENTITY_PROCESSORS = {
    "articles": process_article, "article": process_article,
    "cosms": process_cosm, "cosm": process_cosm,
    "items": process_item, "item": process_item,
    "miracles": process_miracle, "miracle": process_miracle,
    "miraclelists": process_miraclelist, "miraclelist": process_miraclelist,
    "perks": process_perk, "perk": process_perk,
    "perkgroups": process_perkgroup, "groups": process_perkgroup, "group": process_perkgroup,
    "powers": process_power, "power": process_power,
    "powerlists": process_powerlist, "powerlist": process_powerlist,
    "races": process_race, "race": process_race,
    "shards": process_shard, "shard": process_shard,
    "spells": process_spell, "spell": process_spell,
    "spelllists": process_spelllist, "spelllist": process_spelllist,
    "tags": process_tag, "tag": process_tag,
    "threats": process_threat, "threat": process_threat,
    "vehicles": process_vehicle, "vehicle": process_vehicle,
}
SKIP_KEYWORDS = {"images", "image", "vehicle-addons", "vehicle_addons"}
def get_product_hint(filepath: Path) -> str:
    stem = filepath.stem
    if "." in stem:
        product = stem.split(".")[0]
        return product.lower().replace("_", "-").replace(" ", "-")
    return ""
def process_file(filepath: Path):
    stem = filepath.stem
    parts = stem.split(".")
    entity_type = parts[-1].lower().strip()
    if entity_type in SKIP_KEYWORDS:
        return
    processor = ENTITY_PROCESSORS.get(entity_type)
    if processor is None:
        print(f"  UNBEKANNT ({entity_type}): {filepath.relative_to(BASE_DIR)}")
        return
    product_hint = get_product_hint(filepath)
    try:
        with open(filepath, "r", encoding="utf-8") as f:
            data = yaml.safe_load(f)
    except Exception as e:
        print(f"  FEHLER: {filepath}: {e}")
        return
    if data is None:
        return
    if isinstance(data, list):
        entries = data
    elif isinstance(data, dict):
        entries = []
        for v in data.values():
            if isinstance(v, list):
                entries = v
                break
        if not entries:
            entries = [data]
    else:
        return
    count = 0
    for entry in entries:
        if isinstance(entry, dict) and (entry.get("name") or entry.get("id")):
            processor(entry, product_hint)
            count += 1
    if count > 0:
        print(f"  {count:4d} ({entity_type:20s}): {filepath.relative_to(BASE_DIR)}")
def main():
    print(f"Scanne {BASE_DIR} ...")
    skip_names = {"torg-data-entity.yml", "torg-data-load.yml", "master-changelog.yml"}
    yaml_files = sorted(BASE_DIR.rglob("*.yml"))
    for fpath in yaml_files:
        if fpath.name in skip_names:
            continue
        if "load" in fpath.parts:
            continue
        process_file(fpath)
    for name, (f, w, cols) in csv_writers.items():
        f.close()
    print(f"\nFertig. {len(csv_writers)} CSV-Dateien in {LOAD_DIR}")
    for name in sorted(csv_writers.keys()):
        fpath = LOAD_DIR / f"{name}.csv"
        lines = sum(1 for _ in open(fpath)) - 1
        print(f"  {lines:5d} Zeilen: {name}.csv")
if __name__ == "__main__":
    main()
