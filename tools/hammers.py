#!/usr/bin/env python3
"""Генерирует и собирает аддон Random Hammers (случайные цветные молотки-«Т»).

Настройки читаются из hammers_config.json в корне репозитория:
  size_x  — масштаб по оси X (ширина/глубина), 1.0 = исходный размер (голова 3 блока)
  size_y  — масштаб по оси Y (высота), 1.0 = исходный размер (3 блока)
  spawn_weight / herd_min / herd_max — частота спавна и размер групп
  health  — здоровье (молотки можно убить)
  color_variants / color_seed — сколько случайных раскрасок и «зерно» генератора

Запуск: python3 tools/hammers.py   (нужен Pillow) -> dist/RandomHammers.mcaddon
"""
import colorsys
import json
import os
import random
import zipfile
from PIL import Image

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
BP = os.path.join(ROOT, "RandomHammers_BP")
RP = os.path.join(ROOT, "RandomHammers_RP")
OUT = os.path.join(ROOT, "dist", "RandomHammers.mcaddon")
ID = "hammer:random_hammer"

DEFAULTS = {"size_x": 1.0, "size_y": 1.0, "spawn_weight": 100, "herd_min": 1,
            "herd_max": 3, "health": 20, "color_variants": 32, "color_seed": 1}

TEX = 128
# Голова: 3 блока в ширину (48 px), 1 блок высотой/глубиной. Ручка («палка») по центру: 8x32x8.
HEAD = {"origin": (-24, 32, -8), "size": (48, 16, 16), "uv": (0, 0)}
HANDLE = {"origin": (-4, 0, -4), "size": (8, 32, 8), "uv": (0, 40)}
BASE_W, BASE_H = 3.0, 3.0  # блоков при size = 1.0

FACE_K = {"top": 1.15, "bottom": 0.6, "east": 0.85, "north": 1.0, "west": 0.75, "south": 0.8}


def load_config():
    cfg = dict(DEFAULTS)
    with open(os.path.join(ROOT, "hammers_config.json"), encoding="utf-8") as f:
        cfg.update({k: v for k, v in json.load(f).items() if k in DEFAULTS})
    cfg["size_x"] = max(0.1, float(cfg["size_x"]))
    cfg["size_y"] = max(0.1, float(cfg["size_y"]))
    cfg["color_variants"] = max(1, int(cfg["color_variants"]))
    cfg["herd_min"] = max(1, int(cfg["herd_min"]))
    cfg["herd_max"] = max(cfg["herd_min"], int(cfg["herd_max"]))
    return cfg


def shade(c, k):
    return tuple(max(0, min(255, int(v * k))) for v in c)


def faces(box):
    u, v = box["uv"]
    w, h, d = box["size"]
    return {
        "top": (u + d, v, w, d),
        "bottom": (u + d + w, v, w, d),
        "east": (u, v + d, d, h),
        "north": (u + d, v + d, w, h),  # перёд
        "west": (u + d + w, v + d, d, h),
        "south": (u + 2 * d + w, v + d, w, h),
    }


def fill(img, rect, color):
    x, y, w, h = rect
    for i in range(x, x + w):
        for j in range(y, y + h):
            img.putpixel((i, j), color + (255,))


def hsv(h, s, v):
    return tuple(int(c * 255) for c in colorsys.hsv_to_rgb(h, s, v))


def make_texture(head_col, handle_col):
    img = Image.new("RGBA", (TEX, TEX), (0, 0, 0, 0))
    for face, rect in faces(HEAD).items():
        k = FACE_K[face]
        fill(img, rect, shade(head_col, k))
        x, y, w, h = rect
        if face in ("north", "south", "east", "west"):  # тёмная окантовка
            fill(img, (x, y, w, 1), shade(head_col, k * 0.75))
            fill(img, (x, y + h - 1, w, 1), shade(head_col, k * 0.75))
        if face in ("north", "south"):  # металлические накладки по краям головы
            fill(img, (x, y, 3, h), shade((170, 175, 180), k))
            fill(img, (x + w - 3, y, 3, h), shade((170, 175, 180), k))
    for face, rect in faces(HANDLE).items():
        k = FACE_K[face]
        fill(img, rect, shade(handle_col, k))
        x, y, w, h = rect
        if face in ("north", "south", "east", "west"):
            for sy in range(2, h, 6):  # полоски-обмотка
                fill(img, (x, y + sy, w, 1), shade(handle_col, k * 0.7))
    # Лицо на передней грани головы
    x0, y0, w, h = faces(HEAD)["north"]
    for ex in (x0 + 12, x0 + 30):
        fill(img, (ex, y0 + 4, 6, 7), (255, 255, 255))
        fill(img, (ex + 2, y0 + 6, 3, 4), (20, 20, 20))
    fill(img, (x0 + 18, y0 + 13, 12, 1), (20, 20, 20))
    return img


def make_icon(head, handle):
    img = Image.new("RGBA", (128, 128), (255, 244, 214, 255))
    fill(img, (8, 20, 112, 36), head)
    fill(img, (8, 20, 112, 5), shade(head, 1.15))
    fill(img, (8, 51, 112, 5), shade(head, 0.7))
    fill(img, (52, 56, 24, 64), handle)
    fill(img, (52, 56, 4, 64), shade(handle, 0.8))
    for y in range(62, 120, 10):
        fill(img, (52, y, 24, 2), shade(handle, 0.65))
    fill(img, (30, 30, 14, 14), (255, 255, 255)); fill(img, (35, 33, 7, 9), (20, 20, 20))
    fill(img, (84, 30, 14, 14), (255, 255, 255)); fill(img, (89, 33, 7, 9), (20, 20, 20))
    return img


def write_json(path, data):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8") as f:
        json.dump(data, f, indent=2, ensure_ascii=False)
        f.write("\n")


def write_text(path, text):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8") as f:
        f.write(text)


def manifest(name, desc, pack_uuid, module_uuid, module_type, dep_uuid):
    return {
        "format_version": 2,
        "header": {"name": name, "description": desc, "uuid": pack_uuid,
                   "version": [1, 0, 0], "min_engine_version": [1, 20, 0]},
        "modules": [{"type": module_type, "uuid": module_uuid, "version": [1, 0, 0]}],
        "dependencies": [{"uuid": dep_uuid, "version": [1, 0, 0]}],
    }


def main():
    cfg = load_config()
    n = cfg["color_variants"]
    rnd = random.Random(cfg["color_seed"])

    # --- случайные раскраски: свой цвет головы и свой цвет ручки у каждого варианта
    palettes = []
    for i in range(n):
        head = hsv(rnd.random(), rnd.uniform(0.55, 1.0), rnd.uniform(0.65, 1.0))
        handle = hsv(rnd.random(), rnd.uniform(0.3, 0.9), rnd.uniform(0.4, 0.9))
        palettes.append((head, handle))
        p = os.path.join(RP, "textures", "entity", "random_hammer", f"v{i}.png")
        os.makedirs(os.path.dirname(p), exist_ok=True)
        make_texture(head, handle).save(p)
    os.makedirs(BP, exist_ok=True)
    os.makedirs(RP, exist_ok=True)
    icon = make_icon(*palettes[0])
    icon.save(os.path.join(RP, "pack_icon.png"))
    icon.save(os.path.join(BP, "pack_icon.png"))

    # --- модель
    cubes = [{"origin": list(b["origin"]), "size": list(b["size"]), "uv": list(b["uv"])}
             for b in (HANDLE, HEAD)]
    write_json(os.path.join(RP, "models", "entity", "random_hammer.geo.json"), {
        "format_version": "1.12.0",
        "minecraft:geometry": [{
            "description": {
                "identifier": "geometry.random_hammer",
                "texture_width": TEX, "texture_height": TEX,
                "visible_bounds_width": 8, "visible_bounds_height": 8,
                "visible_bounds_offset": [0, 2, 0],
            },
            "bones": [{"name": "root", "pivot": [0, 0, 0], "cubes": cubes}],
        }],
    })

    # --- анимация: масштаб по осям X/Y из конфига + покачивание при ходьбе
    sx, sy = cfg["size_x"], cfg["size_y"]
    write_json(os.path.join(RP, "animations", "random_hammer.animation.json"), {
        "format_version": "1.8.0",
        "animations": {
            "animation.random_hammer.main": {
                "loop": True,
                "bones": {"root": {
                    "scale": [sx, sy, sx],
                    "rotation": [
                        "math.sin(query.life_time * 200) * 5 * query.modified_move_speed",
                        0,
                        "math.sin(query.life_time * 100) * 7 * query.modified_move_speed",
                    ],
                }},
            }
        },
    })

    # --- клиентская сущность и рендер
    write_json(os.path.join(RP, "entity", "random_hammer.entity.json"), {
        "format_version": "1.10.0",
        "minecraft:client_entity": {
            "description": {
                "identifier": ID,
                "materials": {"default": "entity_alphatest"},
                "textures": {f"v{i}": f"textures/entity/random_hammer/v{i}" for i in range(n)},
                "geometry": {"default": "geometry.random_hammer"},
                "animations": {"main": "animation.random_hammer.main"},
                "scripts": {"animate": ["main"]},
                "render_controllers": ["controller.render.random_hammer"],
                "spawn_egg": {"base_color": "#E53935", "overlay_color": "#FDD835"},
            }
        },
    })
    write_json(os.path.join(RP, "render_controllers", "random_hammer.render_controllers.json"), {
        "format_version": "1.10.0",
        "render_controllers": {
            "controller.render.random_hammer": {
                "arrays": {"textures": {"Array.skins": [f"Texture.v{i}" for i in range(n)]}},
                "geometry": "Geometry.default",
                "materials": [{"*": "Material.default"}],
                "textures": ["Array.skins[query.variant]"],
            }
        },
    })

    # --- серверная сущность (можно убить: здоровье + лут + убегает при ударе)
    groups = {f"hammer:color_{i}": {"minecraft:variant": {"value": i}} for i in range(n)}
    write_json(os.path.join(BP, "entities", "random_hammer.json"), {
        "format_version": "1.20.0",
        "minecraft:entity": {
            "description": {"identifier": ID, "is_spawnable": True, "is_summonable": True},
            "component_groups": groups,
            "components": {
                "minecraft:type_family": {"family": ["hammer", "mob"]},
                "minecraft:health": {"value": cfg["health"], "max": cfg["health"]},
                "minecraft:collision_box": {"width": round(BASE_W * sx, 3),
                                            "height": round(BASE_H * sy, 3)},
                "minecraft:physics": {},
                "minecraft:pushable": {"is_pushable": True, "is_pushable_by_piston": True},
                "minecraft:movement": {"value": 0.15},
                "minecraft:movement.basic": {},
                "minecraft:navigation.walk": {"can_path_over_water": False, "avoid_water": True},
                "minecraft:jump.static": {},
                "minecraft:behavior.float": {"priority": 0},
                "minecraft:behavior.panic": {"priority": 1, "speed_multiplier": 1.4},
                "minecraft:behavior.random_stroll": {"priority": 5, "speed_multiplier": 0.9},
                "minecraft:behavior.look_at_player": {"priority": 6, "look_distance": 8},
                "minecraft:behavior.random_look_around": {"priority": 7},
                "minecraft:nameable": {},
                "minecraft:loot": {"table": "loot_tables/entities/random_hammer.json"},
            },
            "events": {
                "minecraft:entity_spawned": {
                    "randomize": [{"weight": 1, "add": {"component_groups": [f"hammer:color_{i}"]}}
                                  for i in range(n)]
                }
            },
        },
    })
    write_json(os.path.join(BP, "loot_tables", "entities", "random_hammer.json"), {
        "pools": [
            {"rolls": 1, "entries": [{"type": "item", "name": "minecraft:iron_ingot",
                "functions": [{"function": "set_count", "count": {"min": 1, "max": 3}}]}]},
            {"rolls": 1, "entries": [{"type": "item", "name": "minecraft:stick",
                "functions": [{"function": "set_count", "count": {"min": 1, "max": 4}}]}]},
        ]
    })
    write_json(os.path.join(BP, "spawn_rules", "random_hammer.json"), {
        "format_version": "1.8.0",
        "minecraft:spawn_rules": {
            "description": {"identifier": ID, "population_control": "ambient"},
            "conditions": [{
                "minecraft:spawns_on_surface": {},
                "minecraft:brightness_filter": {"min": 0, "max": 15, "adjust_for_weather": False},
                "minecraft:weight": {"default": int(cfg["spawn_weight"])},
                "minecraft:herd": {"min_size": cfg["herd_min"], "max_size": cfg["herd_max"]},
                "minecraft:biome_filter": {"any_of": [
                    {"test": "has_biome_tag", "operator": "==", "value": "overworld"}]},
            }],
        },
    })

    # --- манифесты и язык
    bp_uuid, bp_mod = "c99271e7-22aa-480e-83cf-0c3e3a370a58", "d741cea8-66f6-4cc5-a0df-dcb473d6aeaf"
    rp_uuid, rp_mod = "31682c7e-f7eb-4846-9144-21ea86b6f297", "2900a6c1-6fed-4480-9257-1edbc6526cd6"
    write_json(os.path.join(BP, "manifest.json"), manifest(
        "Random Hammers BP / Случайные молотки BP",
        "Random-coloured T-shaped hammers spawn in the world / Цветные молотки-«Т» случайно появляются в мире",
        bp_uuid, bp_mod, "data", rp_uuid))
    write_json(os.path.join(RP, "manifest.json"), manifest(
        "Random Hammers RP / Случайные молотки RP",
        "Textures and models / Текстуры и модели", rp_uuid, rp_mod, "resources", bp_uuid))
    write_text(os.path.join(RP, "texts", "en_US.lang"),
               f"entity.{ID}.name=Random Hammer\nitem.spawn_egg.entity.{ID}.name=Random Hammer Spawn Egg\n")
    write_text(os.path.join(RP, "texts", "ru_RU.lang"),
               f"entity.{ID}.name=Случайный молоток\nitem.spawn_egg.entity.{ID}.name=Яйцо призыва случайного молотка\n")
    write_json(os.path.join(RP, "texts", "languages.json"), ["en_US", "ru_RU"])

    # --- сборка .mcaddon
    os.makedirs(os.path.dirname(OUT), exist_ok=True)
    with zipfile.ZipFile(OUT, "w", zipfile.ZIP_DEFLATED) as z:
        for pack in ("RandomHammers_BP", "RandomHammers_RP"):
            for d, _, files in os.walk(os.path.join(ROOT, pack)):
                for f in files:
                    p = os.path.join(d, f)
                    z.write(p, os.path.relpath(p, ROOT))
    print(f"Собрано: {OUT}  (size_x={sx}, size_y={sy}, раскрасок: {n})")


if __name__ == "__main__":
    main()
