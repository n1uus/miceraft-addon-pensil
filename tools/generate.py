#!/usr/bin/env python3
"""Генерирует текстуры, иконку и повторяющиеся JSON-файлы аддона Big Pencils.

Запуск: python3 tools/generate.py   (нужен Pillow)
"""
import json
import os
from PIL import Image

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
BP = os.path.join(ROOT, "BigPencils_BP")
RP = os.path.join(ROOT, "BigPencils_RP")

# (id, русское имя, английское имя, цвет корпуса)
COLORS = [
    ("white", "белый", "White", (240, 240, 240)),
    ("orange", "оранжевый", "Orange", (249, 128, 29)),
    ("magenta", "пурпурный", "Magenta", (199, 78, 189)),
    ("light_blue", "голубой", "Light Blue", (58, 179, 218)),
    ("yellow", "жёлтый", "Yellow", (254, 216, 61)),
    ("lime", "лаймовый", "Lime", (128, 199, 31)),
    ("pink", "розовый", "Pink", (243, 139, 170)),
    ("gray", "серый", "Gray", (71, 79, 82)),
    ("light_gray", "светло-серый", "Light Gray", (157, 157, 151)),
    ("cyan", "бирюзовый", "Cyan", (22, 156, 156)),
    ("purple", "фиолетовый", "Purple", (137, 50, 184)),
    ("blue", "синий", "Blue", (60, 68, 170)),
    ("brown", "коричневый", "Brown", (131, 84, 50)),
    ("green", "зелёный", "Green", (94, 124, 22)),
    ("red", "красный", "Red", (176, 46, 38)),
    ("black", "чёрный", "Black", (29, 29, 33)),
]

TEX = 128
# имя -> (uv_x, uv_y, w, h, d)
CUBES = {
    "eraser": (0, 0, 12, 6, 12),
    "ferrule": (0, 20, 12, 8, 12),
    "body": (0, 40, 12, 28, 12),
    "wood1": (56, 0, 10, 4, 10),
    "wood2": (56, 16, 8, 4, 8),
    "wood3": (56, 30, 6, 4, 6),
    "wood4": (56, 42, 4, 3, 4),
    "lead": (56, 51, 2, 3, 2),
}
# имя -> (origin_x, origin_y, origin_z)
ORIGINS = {
    "eraser": (-6, 0, -6),
    "ferrule": (-6, 6, -6),
    "body": (-6, 14, -6),
    "wood1": (-5, 42, -5),
    "wood2": (-4, 46, -4),
    "wood3": (-3, 50, -3),
    "wood4": (-2, 54, -2),
    "lead": (-1, 57, -1),
}
HEIGHT_PX = 60  # 3.75 блока


def shade(c, k):
    return tuple(max(0, min(255, int(v * k))) for v in c)


def faces(cube):
    u, v, w, h, d = cube
    return {
        "top": (u + d, v, w, d),
        "bottom": (u + d + w, v, w, d),
        "east": (u, v + d, d, h),
        "north": (u + d, v + d, w, h),  # перед
        "west": (u + d + w, v + d, d, h),
        "south": (u + 2 * d + w, v + d, w, h),
    }


FACE_K = {"top": 1.12, "bottom": 0.65, "east": 0.9, "north": 1.0, "west": 0.8, "south": 0.85}


def fill(img, rect, color):
    x, y, w, h = rect
    for i in range(x, x + w):
        for j in range(y, y + h):
            img.putpixel((i, j), color + (255,))


def paint_cube(img, name, base, edge_dark=None, stripes=None):
    for face, rect in faces(CUBES[name]).items():
        col = shade(base, FACE_K[face])
        fill(img, rect, col)
        if edge_dark and face in ("north", "south", "east", "west"):
            x, y, w, h = rect
            fill(img, (x, y, 1, h), shade(base, FACE_K[face] * edge_dark))
            fill(img, (x + w - 1, y, 1, h), shade(base, FACE_K[face] * edge_dark))
        if stripes and face in ("north", "south", "east", "west"):
            x, y, w, h = rect
            for sy in range(1, h, 2):
                fill(img, (x, y + sy, w, 1), shade(base, FACE_K[face] * stripes))


def make_texture(body_color):
    img = Image.new("RGBA", (TEX, TEX), (0, 0, 0, 0))
    paint_cube(img, "eraser", (244, 143, 177), edge_dark=0.88)
    paint_cube(img, "ferrule", (190, 195, 200), stripes=0.82)
    paint_cube(img, "body", body_color, edge_dark=0.82)
    for n in ("wood1", "wood2", "wood3", "wood4"):
        paint_cube(img, n, (234, 196, 140), edge_dark=0.9)
    paint_cube(img, "lead", (45, 45, 50))

    # Лицо на передней грани корпуса
    x0, y0, w, h = faces(CUBES["body"])["north"]
    light = sum(body_color) > 600
    eye_bg = (255, 255, 255)
    pupil = (20, 20, 20)
    for ex in (x0 + 2, x0 + 7):
        fill(img, (ex, y0 + 5, 3, 4), eye_bg)
        fill(img, (ex + 1, y0 + 6, 2, 2), pupil)
    mouth = (20, 20, 20) if light or sum(body_color) > 250 else (255, 255, 255)
    fill(img, (x0 + 3, y0 + 13, 6, 1), mouth)
    fill(img, (x0 + 2, y0 + 12, 1, 1), mouth)
    fill(img, (x0 + 9, y0 + 12, 1, 1), mouth)
    # Полоска-«брендинг» внизу корпуса
    fill(img, (x0, y0 + h - 5, w, 1), shade(body_color, 0.7))
    fill(img, (x0, y0 + h - 3, w, 1), shade(body_color, 0.7))
    return img


def make_icon():
    size = 128
    img = Image.new("RGBA", (size, size), (255, 244, 214, 255))
    body = (254, 216, 61)
    cx = 64
    def rect(x, y, w, h, c):
        for i in range(x, x + w):
            for j in range(y, y + h):
                img.putpixel((i, j), c + (255,))
    rect(cx - 12, 98, 24, 12, (244, 143, 177))
    rect(cx - 12, 84, 24, 14, (190, 195, 200))
    rect(cx - 12, 36, 24, 48, body)
    rect(cx - 12, 36, 3, 48, shade(body, 0.85))
    rect(cx - 10, 24, 20, 12, (234, 196, 140))
    rect(cx - 6, 14, 12, 10, (234, 196, 140))
    rect(cx - 2, 6, 4, 8, (45, 45, 50))
    rect(cx - 7, 52, 5, 8, (255, 255, 255)); rect(cx + 2, 52, 5, 8, (255, 255, 255))
    rect(cx - 5, 55, 3, 4, (20, 20, 20)); rect(cx + 4, 55, 3, 4, (20, 20, 20))
    rect(cx - 6, 68, 12, 2, (20, 20, 20))
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


def main():
    # --- текстуры
    for cid, _, _, col in COLORS:
        p = os.path.join(RP, "textures", "entity", "big_pencil", f"{cid}.png")
        os.makedirs(os.path.dirname(p), exist_ok=True)
        make_texture(col).save(p)
    make_icon().save(os.path.join(RP, "pack_icon.png"))
    make_icon().save(os.path.join(BP, "pack_icon.png"))

    # --- модель
    cubes = []
    for name, (u, v, w, h, d) in CUBES.items():
        ox, oy, oz = ORIGINS[name]
        cubes.append({"origin": [ox, oy, oz], "size": [w, h, d], "uv": [u, v]})
    write_json(os.path.join(RP, "models", "entity", "big_pencil.geo.json"), {
        "format_version": "1.12.0",
        "minecraft:geometry": [{
            "description": {
                "identifier": "geometry.big_pencil",
                "texture_width": TEX, "texture_height": TEX,
                "visible_bounds_width": 3, "visible_bounds_height": 5,
                "visible_bounds_offset": [0, 2, 0],
            },
            "bones": [{"name": "root", "pivot": [0, 0, 0], "cubes": cubes}],
        }],
    })

    # --- клиентская сущность
    write_json(os.path.join(RP, "entity", "big_pencil.entity.json"), {
        "format_version": "1.10.0",
        "minecraft:client_entity": {
            "description": {
                "identifier": "pencil:big_pencil",
                "materials": {"default": "entity_alphatest"},
                "textures": {c[0]: f"textures/entity/big_pencil/{c[0]}" for c in COLORS},
                "geometry": {"default": "geometry.big_pencil"},
                "animations": {"wobble": "animation.big_pencil.wobble"},
                "scripts": {"animate": ["wobble"]},
                "render_controllers": ["controller.render.big_pencil"],
                "spawn_egg": {"base_color": "#FED83D", "overlay_color": "#F48FB1"},
            }
        },
    })
    write_json(os.path.join(RP, "render_controllers", "big_pencil.render_controllers.json"), {
        "format_version": "1.10.0",
        "render_controllers": {
            "controller.render.big_pencil": {
                "arrays": {"textures": {"Array.skins": [f"Texture.{c[0]}" for c in COLORS]}},
                "geometry": "Geometry.default",
                "materials": [{"*": "Material.default"}],
                "textures": ["Array.skins[query.variant]"],
            }
        },
    })
    write_json(os.path.join(RP, "animations", "big_pencil.animation.json"), {
        "format_version": "1.8.0",
        "animations": {
            "animation.big_pencil.wobble": {
                "loop": True,
                "bones": {"root": {"rotation": [
                    "math.sin(query.life_time * 200) * 6 * query.modified_move_speed",
                    0,
                    "math.sin(query.life_time * 100) * 8 * query.modified_move_speed",
                ]}},
            }
        },
    })

    # --- серверная сущность
    groups = {f"pencil:color_{i}": {"minecraft:variant": {"value": i}} for i in range(len(COLORS))}
    write_json(os.path.join(BP, "entities", "big_pencil.json"), {
        "format_version": "1.20.0",
        "minecraft:entity": {
            "description": {
                "identifier": "pencil:big_pencil",
                "is_spawnable": True,
                "is_summonable": True,
            },
            "component_groups": groups,
            "components": {
                "minecraft:type_family": {"family": ["pencil", "inanimate"]},
                "minecraft:health": {"value": 20, "max": 20},
                "minecraft:collision_box": {"width": 0.9, "height": 3.75},
                "minecraft:physics": {},
                "minecraft:pushable": {"is_pushable": True, "is_pushable_by_piston": True},
                "minecraft:movement": {"value": 0.12},
                "minecraft:movement.basic": {},
                "minecraft:navigation.walk": {"can_path_over_water": False, "avoid_water": True},
                "minecraft:jump.static": {},
                "minecraft:can_climb": {},
                "minecraft:behavior.float": {"priority": 0},
                "minecraft:behavior.random_stroll": {"priority": 5, "speed_multiplier": 0.8},
                "minecraft:behavior.look_at_player": {"priority": 6, "look_distance": 8},
                "minecraft:behavior.random_look_around": {"priority": 7},
                "minecraft:nameable": {},
                "minecraft:loot": {"table": "loot_tables/entities/big_pencil.json"},
            },
            "events": {
                "minecraft:entity_spawned": {
                    "randomize": [
                        {"weight": 1, "add": {"component_groups": [f"pencil:color_{i}"]}}
                        for i in range(len(COLORS))
                    ]
                }
            },
        },
    })
    write_json(os.path.join(BP, "loot_tables", "entities", "big_pencil.json"), {
        "pools": [
            {"rolls": 1, "entries": [{
                "type": "item", "name": "minecraft:stick",
                "functions": [{"function": "set_count", "count": {"min": 2, "max": 5}}]}]},
            {"rolls": 1, "entries": [{
                "type": "item", "name": "minecraft:charcoal",
                "functions": [{"function": "set_count", "count": {"min": 0, "max": 2}}]}]},
        ]
    })

    # --- язык
    en = ["entity.pencil:big_pencil.name=Big Pencil",
          "item.spawn_egg.entity.pencil:big_pencil.name=Big Pencil Spawn Egg"]
    ru = ["entity.pencil:big_pencil.name=Большой карандаш",
          "item.spawn_egg.entity.pencil:big_pencil.name=Яйцо призыва большого карандаша"]
    write_text(os.path.join(RP, "texts", "en_US.lang"), "\n".join(en) + "\n")
    write_text(os.path.join(RP, "texts", "ru_RU.lang"), "\n".join(ru) + "\n")
    write_json(os.path.join(RP, "texts", "languages.json"), ["en_US", "ru_RU"])


if __name__ == "__main__":
    main()
