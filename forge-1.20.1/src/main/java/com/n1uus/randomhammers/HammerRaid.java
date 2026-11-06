package com.n1uus.randomhammers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Рейд из pensil на деревню: волны, полоса сверху экрана «Волна N/M». */
@Mod.EventBusSubscriber(modid = RandomHammers.MODID)
public final class HammerRaid {
    public static final String TAG = "pensil_raid";

    private static final class Raid {
        final UUID player;
        final ServerBossEvent bar = new ServerBossEvent(Component.literal("Рейд pensil"),
                BossEvent.BossBarColor.PINK, BossEvent.BossBarOverlay.NOTCHED_10);
        final List<UUID> mobs = new ArrayList<>();
        int wave = 0, total = 1, delay = 100;
        Raid(UUID p) { player = p; }
    }

    private static final Map<UUID, Raid> RAIDS = new HashMap<>();
    private static final Map<UUID, Long> NEXT_ALLOWED = new HashMap<>();

    public static boolean isRaiding(UUID player) { return RAIDS.containsKey(player); }

    @SubscribeEvent
    public static void onTick(TickEvent.ServerTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        MinecraftServer server = e.getServer();
        long time = server.overworld().getGameTime();

        if (HammerConfig.RAID_ENABLED.get() && time % 100 == 0) {
            for (ServerPlayer p : server.getPlayerList().getPlayers()) tryStart(p, time);
        }

        RAIDS.entrySet().removeIf(en -> tickRaid(server, en.getValue(), time));
    }

    private static void tryStart(ServerPlayer p, long time) {
        if (RAIDS.containsKey(p.getUUID()) || p.isSpectator() || !p.isAlive()) return;
        if (p.level().dimension() != Level.OVERWORLD) return;
        if (time < NEXT_ALLOWED.getOrDefault(p.getUUID(), 0L)) return;
        int villagers = p.serverLevel().getEntitiesOfClass(Villager.class, p.getBoundingBox().inflate(40.0)).size();
        if (villagers < 3) return;
        Raid r = new Raid(p.getUUID());
        r.bar.addPlayer(p);
        RAIDS.put(p.getUUID(), r);
        p.displayClientMessage(Component.literal("§4§lНа деревню надвигается рейд pensil!"), false);
        p.playNotifySound(SoundEvents.WITHER_SPAWN, SoundSource.HOSTILE, 1.0F, 0.8F);
        r.bar.setName(Component.literal("Рейд pensil — приготовьтесь..."));
    }

    /** @return true, если рейд закончился */
    private static boolean tickRaid(MinecraftServer server, Raid r, long time) {
        ServerPlayer p = server.getPlayerList().getPlayer(r.player);
        int waves = HammerConfig.RAID_WAVES.get();
        if (p == null || !p.isAlive() || p.level().dimension() != Level.OVERWORLD) {
            r.bar.removeAllPlayers();
            if (p != null) p.displayClientMessage(Component.literal("§cРейд pensil провален..."), false);
            NEXT_ALLOWED.put(r.player, time + HammerConfig.RAID_COOLDOWN_MIN.get() * 1200L);
            return true;
        }
        ServerLevel level = p.serverLevel();
        if (r.delay > 0) {
            if (--r.delay == 0) spawnWave(level, p, r, waves);
            return false;
        }
        r.mobs.removeIf(id -> { Entity en = level.getEntity(id); return en == null || !en.isAlive(); });
        r.bar.setProgress(Math.max(0.0F, Math.min(1.0F, r.mobs.size() / (float) r.total)));
        if (!r.mobs.isEmpty()) return false;

        if (r.wave >= waves) {
            r.bar.removeAllPlayers();
            p.displayClientMessage(Component.literal("§a§lРейд pensil отражён! Вы герой деревни."), false);
            p.addEffect(new MobEffectInstance(MobEffects.HERO_OF_THE_VILLAGE, 24000, 0));
            p.drop(new ItemStack(Items.EMERALD, 8), false);
            p.drop(new ItemStack(Items.DIAMOND, 2), false);
            NEXT_ALLOWED.put(r.player, time + HammerConfig.RAID_COOLDOWN_MIN.get() * 1200L);
            return true;
        }
        p.displayClientMessage(Component.literal("§eВолна " + r.wave + "/" + waves + " пройдена. Следующая скоро..."), false);
        r.bar.setName(Component.literal("Рейд pensil — Волна " + r.wave + "/" + waves + " пройдена"));
        r.delay = 120;
        return false;
    }

    private static void spawnWave(ServerLevel level, ServerPlayer p, Raid r, int waves) {
        r.wave++;
        r.total = 2 + r.wave * 2;
        for (int i = 0; i < r.total; i++) {
            double ang = level.random.nextDouble() * Math.PI * 2;
            double dist = 22 + level.random.nextDouble() * 10;
            int x = (int) (p.getX() + Math.cos(ang) * dist);
            int z = (int) (p.getZ() + Math.sin(ang) * dist);
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            HammerEntity h = RandomHammers.HAMMER.get().create(level);
            if (h == null) continue;
            h.moveTo(x + 0.5, y, z + 0.5, level.random.nextFloat() * 360F, 0F);
            h.addTag(TAG);
            h.setPersistenceRequired();
            h.setTarget(p);
            level.addFreshEntity(h);
            r.mobs.add(h.getUUID());
        }
        r.bar.setName(Component.literal("Рейд pensil — Волна " + r.wave + "/" + waves));
        r.bar.setProgress(1.0F);
        p.displayClientMessage(Component.literal("§c§lВолна " + r.wave + "/" + waves), false);
        p.playNotifySound(SoundEvents.WITHER_SPAWN, SoundSource.HOSTILE, 0.8F, 1.2F);
    }

    private HammerRaid() {}
}
