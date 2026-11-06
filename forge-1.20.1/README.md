# Random Hammers — Forge 1.20.1 (Java Edition)

Те же молотки-«Т», что и в Bedrock-аддоне: голова 3 блока + ручка, у каждого случайный цвет головы и ручки,
их можно убить (выпадают железо и палки), на них можно сесть и ездить (WASD, Shift — слезть).

## Сборка
- **GitHub Actions**: вкладка Actions → «Build Forge mod» → Run workflow → скачать артефакт `randomhammers-1.20.1` (там .jar).
- **Локально**: JDK 17 и Gradle 8.1.1: `cd forge-1.20.1 && gradle build` → `build/libs/randomhammers-1.20.1-1.0.0.jar`.

Положите .jar в папку `mods` (Forge 1.20.1, 47.x).

## Настройки — `config/randomhammers-common.toml` (появляется после первого запуска)
- `size_x` / `size_y` — размер по осям X (ширина) и Y (высота), 1.0 = 3×3 блока
- `rotation_x` / `rotation_y` — наклон/поворот в градусах (по умолчанию 180 по X = вверх ногами)
- `spin_x` / `spin_y` — вращение, градусов в секунду (по умолчанию `spin_y` = 90 — крутится вокруг себя)
- `spawn_chance` — шанс спавна (0..1), `health`, `speed`

Спавн: молотки автоматически появляются в Верхнем мире (группы 1–3); вес — `data/randomhammers/forge/biome_modifier/add_hammers.json`.
Яйцо призыва — во вкладке «Яйца призыва», или `/summon randomhammers:hammer`.
