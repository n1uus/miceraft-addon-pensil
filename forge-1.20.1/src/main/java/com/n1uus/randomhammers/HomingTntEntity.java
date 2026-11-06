package com.n1uus.randomhammers;

import java.util.UUID;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** Тнт, который висит в воздухе и летит на игрока, а у цели взрывается. */
public class HomingTntEntity extends PrimedTnt {
    private UUID target;

    public HomingTntEntity(EntityType<? extends PrimedTnt> type, Level level) {
        super(type, level);
        setNoGravity(true);
        setFuse(400); // на случай, если не долетит
    }

    public void setTarget(UUID id) { target = id; }

    @Override
    public void tick() {
        if (!level().isClientSide && target != null) {
            Player p = level().getPlayerByUUID(target);
            if (p != null && p.isAlive()) {
                Vec3 to = p.position().add(0, p.getBbHeight() * 0.5, 0).subtract(position());
                if (to.length() < 2.5) {
                    setFuse(Math.min(getFuse(), 1));
                } else {
                    setDeltaMovement(to.normalize().scale(0.7));
                }
            }
        }
        super.tick();
    }

    @Override
    protected void explode() {
        level().explode(this, getX(), getY(0.0625), getZ(), 4.0F,
                HammerConfig.TNT_BREAK_BLOCKS.get() ? Level.ExplosionInteraction.TNT : Level.ExplosionInteraction.NONE);
    }
}
