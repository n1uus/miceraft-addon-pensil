package com.n1uus.randomhammers;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.event.entity.SpawnPlacementRegisterEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

@Mod(RandomHammers.MODID)
public class RandomHammers {
    public static final String MODID = "randomhammers";

    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, MODID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MODID);

    public static final RegistryObject<EntityType<HammerEntity>> HAMMER = ENTITIES.register("hammer",
            () -> EntityType.Builder.of(HammerEntity::new, MobCategory.MONSTER)
                    .sized(3.0F, 3.0F).clientTrackingRange(10).build("hammer"));

    public static final RegistryObject<EntityType<HomingTntEntity>> HOMING_TNT = ENTITIES.register("homing_tnt",
            () -> EntityType.Builder.<HomingTntEntity>of(HomingTntEntity::new, MobCategory.MISC)
                    .fireImmune().sized(0.98F, 0.98F).clientTrackingRange(10).updateInterval(1).build("homing_tnt"));

    public static final RegistryObject<Item> HAMMER_SPAWN_EGG = ITEMS.register("hammer_spawn_egg",
            () -> new ForgeSpawnEggItem(HAMMER, 0xE53935, 0xFDD835, new Item.Properties()));

    public RandomHammers() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        ENTITIES.register(bus);
        ITEMS.register(bus);
        bus.addListener(this::onAttributes);
        bus.addListener(this::onSpawnPlacement);
        bus.addListener(this::onCreativeTab);
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, HammerConfig.SPEC, "randomhammers-v2.toml");
    }

    private void onAttributes(EntityAttributeCreationEvent e) {
        e.put(HAMMER.get(), HammerEntity.createAttributes().build());
    }

    private void onSpawnPlacement(SpawnPlacementRegisterEvent e) {
        e.register(HAMMER.get(), SpawnPlacements.Type.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                HammerEntity::checkSpawn, SpawnPlacementRegisterEvent.Operation.REPLACE);
    }

    private void onCreativeTab(BuildCreativeModeTabContentsEvent e) {
        if (e.getTabKey() == CreativeModeTabs.SPAWN_EGGS) e.accept(HAMMER_SPAWN_EGG);
    }
}
