package com.n1uus.randomhammers;

import java.awt.Color;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.phys.Vec3;

public class HammerEntity extends PathfinderMob {
    private static final EntityDataAccessor<Integer> HEAD_COLOR =
            SynchedEntityData.defineId(HammerEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> HANDLE_COLOR =
            SynchedEntityData.defineId(HammerEntity.class, EntityDataSerializers.INT);

    private static final EntityDataAccessor<Integer> TIP_COLOR =
            SynchedEntityData.defineId(HammerEntity.class, EntityDataSerializers.INT);

    private boolean configured;

    public HammerEntity(EntityType<? extends HammerEntity> type, Level level) {
        super(type, level);
        setMaxUpStep(1.1F);
        if (!level.isClientSide) {
            RandomSource r = level.getRandom();
            // случайный цвет головы и ручки
            entityData.set(HEAD_COLOR, Color.HSBtoRGB(r.nextFloat(), 0.55F + r.nextFloat() * 0.45F, 0.65F + r.nextFloat() * 0.35F));
            entityData.set(TIP_COLOR, Color.HSBtoRGB(r.nextFloat(), 0.6F + r.nextFloat() * 0.4F, 0.8F + r.nextFloat() * 0.2F));
            entityData.set(HANDLE_COLOR, Color.HSBtoRGB(r.nextFloat(), 0.3F + r.nextFloat() * 0.6F, 0.4F + r.nextFloat() * 0.5F));
        }
        refreshDimensions();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0)
                .add(Attributes.MOVEMENT_SPEED, 0.25);
    }

    public static boolean checkSpawn(EntityType<HammerEntity> type, net.minecraft.world.level.ServerLevelAccessor level,
                                     MobSpawnType reason, BlockPos pos, RandomSource random) {
        if (reason == MobSpawnType.NATURAL || reason == MobSpawnType.CHUNK_GENERATION) {
            if (random.nextDouble() > HammerConfig.SPAWN_CHANCE.get()) return false;
        }
        return level.getBlockState(pos.below()).isValidSpawn(level, pos.below(), type);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(HEAD_COLOR, 0xFFE53935);
        entityData.define(HANDLE_COLOR, 0xFF8D6E63);
        entityData.define(TIP_COLOR, 0xFFFFC107);
    }

    public int getHeadColor() { return entityData.get(HEAD_COLOR); }
    public int getHandleColor() { return entityData.get(HANDLE_COLOR); }
    public int getTipColor() { return entityData.get(TIP_COLOR); }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("HeadColor", getHeadColor());
        tag.putInt("HandleColor", getHandleColor());
        tag.putInt("TipColor", getTipColor());
        tag.putBoolean("Configured", configured);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        configured = tag.getBoolean("Configured");
        if (tag.contains("HeadColor")) entityData.set(HEAD_COLOR, tag.getInt("HeadColor"));
        if (tag.contains("TipColor")) entityData.set(TIP_COLOR, tag.getInt("TipColor"));
        if (tag.contains("HandleColor")) entityData.set(HANDLE_COLOR, tag.getInt("HandleColor"));
    }

    @Override
    public void onAddedToWorld() {
        super.onAddedToWorld();
        if (!level().isClientSide) {
            // скорость и здоровье из конфига; новому молотку — полное здоровье
            getAttribute(Attributes.MAX_HEALTH).setBaseValue(HammerConfig.HEALTH.get());
            getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(HammerConfig.SPEED.get());
            if (!configured) { setHealth(getMaxHealth()); configured = true; }
            else if (getHealth() > getMaxHealth()) setHealth(getMaxHealth());
        }
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new PanicGoal(this, 1.4) {
            @Override public boolean canUse() { return !isVehicle() && super.canUse(); }
        });
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.5) {
            @Override public boolean canUse() { return !isVehicle() && super.canUse(); }
        });
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
    }

    /** Размер хитбокса по осям X и Y из конфига. */
    @Override
    public EntityDimensions getDimensions(Pose pose) {
        return EntityDimensions.scalable(3.0F * HammerConfig.SIZE_X.get().floatValue(),
                                         3.0F * HammerConfig.SIZE_Y.get().floatValue());
    }

    @Override
    protected float getStandingEyeHeight(Pose pose, EntityDimensions dims) {
        return dims.height * 0.8F;
    }

    // ----- езда -----

    private int airTicks, groundTicks;
    private boolean flying;

    private static void say(Entity passenger, String text) {
        if (passenger instanceof Player p && !p.level().isClientSide) p.displayClientMessage(Component.literal(text), false);
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (!isVehicle() && !player.isSecondaryUseActive()) {
            if (!level().isClientSide) player.startRiding(this);
            return InteractionResult.sidedSuccess(level().isClientSide);
        }
        return super.mobInteract(player, hand);
    }

    @Override
    protected void addPassenger(Entity passenger) {
        super.addPassenger(passenger);
        say(passenger, "Вы сели на pensil");
        flying = false; airTicks = 0; groundTicks = 0;
    }

    @Override
    protected void removePassenger(Entity passenger) {
        super.removePassenger(passenger);
        say(passenger, "Вы слезли с pensil");
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide && getControllingPassenger() instanceof Player rider) {
            if (onGround()) { groundTicks++; airTicks = 0; } else { airTicks++; groundTicks = 0; }
            if (!flying && airTicks >= 8) { flying = true; say(rider, "Вы взлетели на pensil"); }
            else if (flying && groundTicks >= 3) { flying = false; say(rider, "Вы приземлились на pensil"); }
        }
    }

    /** Пока на молотке сидит игрок — гравитации нет, он летит туда, куда смотрит игрок. */
    @Override
    public boolean isNoGravity() {
        return getControllingPassenger() != null || super.isNoGravity();
    }

    @Override
    public boolean causeFallDamage(float distance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    public double getPassengersRidingOffset() {
        return getBbHeight() + 0.35;
    }

    @Override
    public LivingEntity getControllingPassenger() {
        return getFirstPassenger() instanceof Player p ? p : null;
    }

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        return getPassengers().isEmpty();
    }

    @Override
    public void travel(Vec3 travelVector) {
        if (isAlive() && isVehicle() && getControllingPassenger() instanceof Player rider) {
            setYRot(rider.getYRot());
            yRotO = getYRot();
            setXRot(rider.getXRot() * 0.5F);
            setRot(getYRot(), getXRot());
            yBodyRot = getYRot();
            yHeadRot = yBodyRot;
            if (isControlledByLocalInstance()) {
                // летим туда, куда смотрит игрок (W — вперёд/вверх/вниз по взгляду, A/D — вбок)
                double speed = getAttributeValue(Attributes.MOVEMENT_SPEED) * 1.3;
                float yaw = rider.getYRot() * Mth.DEG_TO_RAD;
                Vec3 look = rider.getLookAngle();
                Vec3 left = new Vec3(Mth.cos(yaw), 0.0, Mth.sin(yaw));
                Vec3 target = look.scale(rider.zza).add(left.scale(rider.xxa * 0.7)).scale(speed);
                Vec3 motion = getDeltaMovement().scale(0.8).add(target.scale(0.2));
                setDeltaMovement(motion);
                move(MoverType.SELF, getDeltaMovement());
                if (horizontalCollision) setDeltaMovement(getDeltaMovement().multiply(0.5, 1.0, 0.5));
            } else {
                setDeltaMovement(Vec3.ZERO);
            }
            calculateEntityAnimation(false);
            return;
        }
        super.travel(travelVector);
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return !isVehicle() && super.removeWhenFarAway(distance);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        return super.hurt(source, amount);
    }
}
