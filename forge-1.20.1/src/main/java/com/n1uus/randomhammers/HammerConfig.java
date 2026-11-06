package com.n1uus.randomhammers;

import net.minecraftforge.common.ForgeConfigSpec;

/** Настройки: config/randomhammers-common.toml */
public final class HammerConfig {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.DoubleValue SIZE_X, SIZE_Y;
    public static final ForgeConfigSpec.DoubleValue ROTATION_X, ROTATION_Y, SPIN_X, SPIN_Y;
    public static final ForgeConfigSpec.DoubleValue SPAWN_CHANCE;
    public static final ForgeConfigSpec.IntValue HEALTH;
    public static final ForgeConfigSpec.DoubleValue SPEED;

    static {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();
        b.comment("Размер молотка. 1.0 = 3x3 блока").push("size");
        SIZE_X = b.comment("Масштаб по оси X (ширина и глубина)").defineInRange("size_x", 1.0, 0.1, 10.0);
        SIZE_Y = b.comment("Масштаб по оси Y (высота)").defineInRange("size_y", 1.0, 0.1, 10.0);
        b.pop();
        b.comment("Вращение (только визуально, в градусах)").push("rotation");
        ROTATION_X = b.comment("Наклон по оси X, градусы (0 = модель уже перевёрнутая Т из блоков)").defineInRange("rotation_x", 0.0, -360.0, 360.0);
        ROTATION_Y = b.comment("Поворот по оси Y, градусы").defineInRange("rotation_y", 0.0, -360.0, 360.0);
        SPIN_X = b.comment("Постоянное вращение по оси X, градусов в секунду (0 = нет)").defineInRange("spin_x", 0.0, -3600.0, 3600.0);
        SPIN_Y = b.comment("Постоянное вращение вокруг себя по высоте (ось Y), градусов в секунду (0 = нет)").defineInRange("spin_y", 0.0, -3600.0, 3600.0);
        b.pop();
        b.push("mob");
        SPAWN_CHANCE = b.comment("Шанс, что попытка спавна удастся (0..1). Меньше = реже").defineInRange("spawn_chance", 1.0, 0.0, 1.0);
        HEALTH = b.comment("Здоровье (молотков можно убить)").defineInRange("health", 20, 1, 1000);
        SPEED = b.comment("Скорость движения (при езде на нём)").defineInRange("speed", 0.25, 0.05, 1.0);
        b.pop();
        SPEC = b.build();
    }

    private HammerConfig() {}
}
