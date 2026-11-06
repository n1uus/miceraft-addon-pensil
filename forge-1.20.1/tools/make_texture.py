#!/usr/bin/env python3
"""Рисует серую «маску» текстуры молотка (раскрашивается в коде). python3 make_texture.py"""
import os
from PIL import Image

OUT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..",
                   "src/main/resources/assets/randomhammers/textures/entity/hammer.png")
K = {"top": 1.0, "bottom": 0.55, "east": 0.8, "north": 0.93, "west": 0.7, "south": 0.75}


def faces(u, v, w, h, d):
    return {"top": (u + d, v, w, d), "bottom": (u + d + w, v, w, d), "east": (u, v + d, d, h),
            "north": (u + d, v + d, w, h), "west": (u + d + w, v + d, d, h),
            "south": (u + 2 * d + w, v + d, w, h)}


def fill(img, r, g):
    x, y, w, h = r
    for i in range(x, x + w):
        for j in range(y, y + h):
            img.putpixel((i, j), (g, g, g, 255))


img = Image.new("RGBA", (128, 128), (0, 0, 0, 0))
for face, r in faces(0, 0, 48, 16, 16).items():  # голова
    g = int(255 * K[face]); fill(img, r, g)
    x, y, w, h = r
    if face in ("north", "south", "east", "west"):
        fill(img, (x, y, w, 1), int(g * .75)); fill(img, (x, y + h - 1, w, 1), int(g * .75))
for (u, v) in ((0, 40), (32, 40)):  # ручка: основная часть и конец (красится отдельно)
  for face, r in faces(u, v, 8, 16, 8).items():
    g = int(255 * K[face]); fill(img, r, g)
    x, y, w, h = r
    if face in ("north", "south", "east", "west") and u == 0:
        for sy in range(2, h, 6):
            fill(img, (x, sy + y, w, 1), int(g * .7))
x0, y0, w, h = faces(0, 0, 48, 16, 16)["north"]  # лицо
for ex in (x0 + 12, x0 + 30):
    fill(img, (ex, y0 + 4, 6, 7), 255); fill(img, (ex + 2, y0 + 6, 3, 4), 10)
fill(img, (x0 + 18, y0 + 13, 12, 1), 10)
img.save(OUT)
