package com.n1uus.randomhammers;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Случайные страшные и забавные события вокруг игроков. */
@Mod.EventBusSubscriber(modid = RandomHammers.MODID)
public final class HammerEvents {
    @SubscribeEvent
    public static void onTick(TickEvent.ServerTickEvent e) {
        if (e.phase != TickEvent.Phase.END || !HammerConfig.EVENTS_ENABLED.get()) return;
        if (e.getServer().getTickCount() % 600 != 0) return;
        for (ServerPlayer p : e.getServer().getPlayerList().getPlayers()) {
            if (p.isSpectator() || !p.isAlive() || HammerRaid.isRaiding(p.getUUID())) continue;
            ServerLevel level = p.serverLevel();
            if (level.random.nextDouble() > HammerConfig.EVENT_CHANCE.get()) continue;
            switch (level.random.nextInt(4)) {
                case 0 -> skyRain(level, p);
                case 1 -> whispers(level, p);
                case 2 -> gift(level, p);
                default -> jumpscare(level, p);
            }
        }
    }

    private static HammerEntity spawn(ServerLevel level, double x, double y, double z, ServerPlayer target) {
        HammerEntity h = RandomHammers.HAMMER.get().create(level);
        if (h == null) return null;
        h.moveTo(x, y, z, level.random.nextFloat() * 360F, 0F);
        h.setTarget(target);
        level.addFreshEntity(h);
        return h;
    }

    /** С неба падают pensil. */
    private static void skyRain(ServerLevel level, ServerPlayer p) {
        p.displayClientMessage(Component.literal("§4С неба падают pensil!"), false);
        for (int i = 0; i < 6; i++) {
            spawn(level, p.getX() + (level.random.nextDouble() * 2 - 1) * 12, p.getY() + 25 + level.random.nextInt(10),
                    p.getZ() + (level.random.nextDouble() * 2 - 1) * 12, p);
        }
    }

    /** Шёпот во тьме: темнеет, стучит сердце, вдалеке бьёт молния (без урона). */
    private static void whispers(ServerLevel level, ServerPlayer p) {
        p.displayClientMessage(Component.literal("§8§oТы слышишь, как pensil шепчет твоё имя..."), false);
        p.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 240, 0, false, false, false));
        p.playNotifySound(SoundEvents.WARDEN_HEARTBEAT, SoundSource.HOSTILE, 2.0F, 0.8F);
        LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);
        if (bolt != null) {
            double ang = level.random.nextDouble() * Math.PI * 2;
            bolt.moveTo(Vec3.atBottomCenterOf(p.blockPosition().offset((int) (Math.cos(ang) * 40), 0, (int) (Math.sin(ang) * 40))));
            bolt.setVisualOnly(true);
            level.addFreshEntity(bolt);
        }
    }

    /** pensil оставляет подарок. */
    private static void gift(ServerLevel level, ServerPlayer p) {
        p.displayClientMessage(Component.literal("§dpensil оставил тебе подарок..."), false);
        Vec3 pos = p.position().add(p.getLookAngle().multiply(3, 0, 3)).add(0, 1, 0);
        ItemStack stack = switch (level.random.nextInt(4)) {
            case 0 -> new ItemStack(Items.DIAMOND, 1 + level.random.nextInt(2));
            case 1 -> new ItemStack(Items.EMERALD, 2 + level.random.nextInt(4));
            case 2 -> new ItemStack(Items.GOLD_INGOT, 3 + level.random.nextInt(5));
            default -> new ItemStack(Items.IRON_INGOT, 4 + level.random.nextInt(6));
        };
        level.addFreshEntity(new ItemEntity(level, pos.x, pos.y, pos.z, stack));
    }

    /** Оглянись: pensil за спиной. */
    private static void jumpscare(ServerLevel level, ServerPlayer p) {
        p.displayClientMessage(Component.literal("§4Оглянись..."), false);
        p.playNotifySound(SoundEvents.WARDEN_NEARBY_CLOSEST, SoundSource.HOSTILE, 2.0F, 1.0F);
        Vec3 back = p.position().subtract(p.getLookAngle().multiply(6, 0, 6));
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, (int) back.x, (int) back.z);
        spawn(level, back.x, y, back.z, p);
    }

    private HammerEvents() {}
}
