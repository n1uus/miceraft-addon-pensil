package com.n1uus.randomhammers;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Месть: сначала молнии бьют в убийцу, потом вокруг него в воздухе появляется тнт и летит на него. */
@Mod.EventBusSubscriber(modid = RandomHammers.MODID)
public final class HammerStorm {
    private static final class Storm {
        final UUID player; int ticks;
        Storm(UUID p) { player = p; }
    }

    private static final List<Storm> STORMS = new ArrayList<>();

    public static void start(ServerPlayer killer) {
        if (!HammerConfig.STORM_ENABLED.get()) return;
        for (Storm s : STORMS) if (s.player.equals(killer.getUUID())) return; // уже идёт
        STORMS.add(new Storm(killer.getUUID()));
    }

    @SubscribeEvent
    public static void onTick(TickEvent.ServerTickEvent e) {
        if (e.phase != TickEvent.Phase.END || STORMS.isEmpty()) return;
        STORMS.removeIf(s -> {
            ServerPlayer p = e.getServer().getPlayerList().getPlayer(s.player);
            if (p == null || !p.isAlive()) return true;
            ServerLevel level = p.serverLevel();
            int bolts = HammerConfig.LIGHTNING_COUNT.get();
            int tntAt = bolts * 15 + 10;
            if (s.ticks % 15 == 0 && s.ticks / 15 < bolts) {
                LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);
                if (bolt != null) {
                    bolt.moveTo(Vec3.atBottomCenterOf(p.blockPosition()));
                    level.addFreshEntity(bolt);
                }
            }
            if (s.ticks == tntAt) {
                int count = HammerConfig.TNT_COUNT.get();
                double half = HammerConfig.TNT_AREA.get() / 2.0;
                for (int i = 0; i < count; i++) {
                    HomingTntEntity tnt = new HomingTntEntity(RandomHammers.HOMING_TNT.get(), level);
                    tnt.setPos(p.getX() + (level.random.nextDouble() * 2 - 1) * half,
                               p.getY() + 5 + level.random.nextDouble() * 5,
                               p.getZ() + (level.random.nextDouble() * 2 - 1) * half);
                    tnt.setTarget(p.getUUID());
                    level.addFreshEntity(tnt);
                }
            }
            return ++s.ticks > tntAt + 5;
        });
    }

    private HammerStorm() {}
}
