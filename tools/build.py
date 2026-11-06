#!/usr/bin/env python3
"""Собирает dist/BigPencils.mcaddon (двойной клик на телефоне — импорт в Minecraft)."""
import os
import zipfile

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
OUT = os.path.join(ROOT, "dist", "BigPencils.mcaddon")
os.makedirs(os.path.dirname(OUT), exist_ok=True)
with zipfile.ZipFile(OUT, "w", zipfile.ZIP_DEFLATED) as z:
    for pack in ("BigPencils_BP", "BigPencils_RP"):
        for d, _, files in os.walk(os.path.join(ROOT, pack)):
            for f in files:
                p = os.path.join(d, f)
                z.write(p, os.path.relpath(p, ROOT))
print("Собрано:", OUT)
