package dev.ralsei.entity;

import dev.ralsei.network.DialogueForceClosePayload;
import dev.ralsei.network.DialogueOpenDyingPayload;
import dev.ralsei.network.StopLullabyPayload;
import dev.ralsei.sound.ModSounds;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.BodyRotationControl;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;

public class RalseiEntity extends PathfinderMob {

    private static final EntityDataAccessor<Boolean> DATA_FALLING =
            SynchedEntityData.defineId(RalseiEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_LANDING =
            SynchedEntityData.defineId(RalseiEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_JUMPING =
            SynchedEntityData.defineId(RalseiEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_TALKING =
            SynchedEntityData.defineId(RalseiEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_FLYING =
            SynchedEntityData.defineId(RalseiEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_SLEEPING =
            SynchedEntityData.defineId(RalseiEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_SINGING =
            SynchedEntityData.defineId(RalseiEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_SCARED =
            SynchedEntityData.defineId(RalseiEntity.class, EntityDataSerializers.BOOLEAN);

    private static final float MIN_FALL_DISTANCE = 1.0f;
    private static final int LANDING_ANIMATION_TICKS = 10;
    private boolean wasFalling = false;
    private int landingTicks = -1;
    private int clientLandingStartTick = -1;

    private static final int MIN_BOREDOM_TICKS = 20 * 10;
    private static final int MAX_BOREDOM_TICKS = 20 * 15;
    private static final int MIN_SLEEP_TICKS = 20 * 300;
    private static final int MAX_SLEEP_TICKS = 20 * 450;
    private static final int SCARED_DURATION_TICKS = 9;
    private static final int SING_DURATION_TICKS = 20 * 48;

    private static final float DYING_HEALTH_FRACTION = 0.5f;
    private static final double ESCAPE_TARGET_HORIZONTAL_RANGE = 300.0D;
    private static final double ESCAPE_TARGET_MIN_HEIGHT_OFFSET = 100.0D;
    private static final double ESCAPE_TARGET_HEIGHT_RANGE = 100.0D;
    private static final int ESCAPE_TIMEOUT_TICKS = 20 * 40;

    private enum EscapeState { NONE, DIALOGUE, FLEEING }
    private EscapeState escapeState = EscapeState.NONE;
    private Vec3 escapeTarget = null;
    private int escapeTicks = 0;

    public boolean isEscaping() {
        return escapeState != EscapeState.NONE;
    }

    private enum BoredomTask { NONE, SLEEP, SING }
    private BoredomTask currentBoredomTask = BoredomTask.NONE;
    private int boredomTaskTicks = -1;
    private int boredomTicks = randomRange(MIN_BOREDOM_TICKS, MAX_BOREDOM_TICKS);
    private int scaredTicks = -1;

    private static int randomRange(int min, int max) {
        return min + (int) (Math.random() * (max - min));
    }

    private static final float FACING_SNAP_INCREMENT = 90f;
    private static final float MOVEMENT_SNAP_INCREMENT = 45f;
    private static final float FACING_HYSTERESIS_DEGREES = 30f;
    private static final float MOVEMENT_HYSTERESIS_DEGREES = 20f;
    private float facingSnap = 0f;
    private float lastMovementHeading = 0f;

    private static final double MIN_FACING_DISTANCE_SQ = 1.0E-6D; // value taken from vanilla LookControl.java please don't change

    public static final double MAX_TALK_DISTANCE_SQ = 4.5 * 4.5;
    private final PlayerLock talkLock = new PlayerLock();
    private final PlayerLock followLock = new PlayerLock();
    private enum PendingFollowAction { NONE, START, STOP }
    private PendingFollowAction pendingFollowAction = PendingFollowAction.NONE;

    public enum FollowPrompt { START, STOP, DENIED }

    private boolean aerialFollowing = false;
    private boolean rescuing = false;

    public RalseiEntity(EntityType<? extends RalseiEntity> entityType, Level level) {
        super(entityType, level);
        this.moveControl = new GridMoveControl(this);
    }
    public static AttributeSupplier.Builder createRalseiAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 72.0)
                .add(Attributes.MOVEMENT_SPEED, 0.26)
                .add(Attributes.FOLLOW_RANGE, 16.0)
                .add(Attributes.JUMP_STRENGTH, 0.4725);
    }
    public String clientTalkOverride = null;
    private String lastClientTalkOverride = null;
    private int clientTalkOverrideTicks = 0;

    public void tickClientTalkOverride() {
        if (!java.util.Objects.equals(clientTalkOverride, lastClientTalkOverride)) {
            clientTalkOverrideTicks = 0;
            lastClientTalkOverride = clientTalkOverride;
        } else {
            clientTalkOverrideTicks++;
        }
    }
    public int getClientTalkOverrideTicks() {
        return clientTalkOverrideTicks;
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new EscapeGoal());
        this.goalSelector.addGoal(1, new FloatGoal(this));
        this.goalSelector.addGoal(2, new RescueGoal());
        this.goalSelector.addGoal(3, new AerialFollowGoal());
        this.goalSelector.addGoal(4, new FollowPlayerGoal());
        this.goalSelector.addGoal(5, new GatedWanderGoal(this, 0.8D));
    }
    @Override
    protected BodyRotationControl createBodyControl() {
        return new NoOpBodyRotationControl(this);
    }

    private static class NoOpBodyRotationControl extends BodyRotationControl {
        NoOpBodyRotationControl(Mob mob) {
            super(mob);
        }

        @Override
        public void clientTick() {
        }
    }
    private class GatedWanderGoal extends WaterAvoidingRandomStrollGoal {
        GatedWanderGoal(PathfinderMob mob, double speedModifier) {
            super(mob, speedModifier);
        }

        @Override
        public boolean canUse() {
            return !isTalking() && !followLock.isOwned() && !isInBoredomTask() && !isEscaping() && super.canUse();
        }

        @Override
        public boolean canContinueToUse() {
            return !isTalking() && !followLock.isOwned() && !isInBoredomTask() && !isEscaping() && super.canContinueToUse();
        }
    }
    private class GridMoveControl extends MoveControl {
        private static final double STOP_THRESHOLD_SQ = 0.01D;
        private static final double JUMP_STEP_THRESHOLD = 0.5D;
        private static final double JUMP_HORIZONTAL_PUSH = 0.35D;

        GridMoveControl(Mob mob) {
            super(mob);
        }

        @Override
        public void tick() {
            if (isTalking() || rescuing || aerialFollowing || isEscaping() || this.operation != Operation.MOVE_TO) {
                return;
            }

            double dx = this.wantedX - RalseiEntity.this.getX();
            double dz = this.wantedZ - RalseiEntity.this.getZ();

            if (dx * dx + dz * dz < STOP_THRESHOLD_SQ) {
                this.operation = Operation.WAIT;
                RalseiEntity.this.setSpeed(0f);
                return;
            }

            tryJumpIfBlocked(dx, dz);

            float rawHeading = yawTo(dx, dz);
            lastMovementHeading = applyHysteresisSnap(rawHeading, MOVEMENT_SNAP_INCREMENT, MOVEMENT_HYSTERESIS_DEGREES, lastMovementHeading);
            RalseiEntity.this.setYRot(lastMovementHeading);

            float speed = (float) (this.speedModifier * RalseiEntity.this.getAttributeValue(Attributes.MOVEMENT_SPEED));
            RalseiEntity.this.setSpeed(speed);
        }
        private void tryJumpIfBlocked(double dx, double dz) {
            if (!RalseiEntity.this.onGround()) {
                return;
            }

            Direction facing = Math.abs(dx) > Math.abs(dz)
                    ? (dx > 0 ? Direction.EAST : Direction.WEST)
                    : (dz > 0 ? Direction.SOUTH : Direction.NORTH);

            Level level = RalseiEntity.this.level();

            BlockPos feet = new BlockPos(
                    Mth.floor(RalseiEntity.this.getX()),
                    Mth.floor(RalseiEntity.this.getY() + 0.5),
                    Mth.floor(RalseiEntity.this.getZ())
            );
            BlockPos ahead = feet.relative(facing);
            var aheadShape = level.getBlockState(ahead).getCollisionShape(level, ahead);
            double aheadTop = aheadShape.isEmpty() ? ahead.getY() : ahead.getY() + aheadShape.max(Direction.Axis.Y);
            double stepHeight = aheadTop - RalseiEntity.this.getY();

            if (stepHeight <= JUMP_STEP_THRESHOLD) {
                return;
            }

            boolean clearAbove = level.getBlockState(ahead.above()).getCollisionShape(level, ahead.above()).isEmpty();
            if (!clearAbove) {
                return;
            }

            RalseiEntity.this.setDeltaMovement(new Vec3(
                    facing.getStepX() * JUMP_HORIZONTAL_PUSH,
                    RalseiEntity.this.getJumpPower(),
                    facing.getStepZ() * JUMP_HORIZONTAL_PUSH
            ));
        }
    }
    private class RescueGoal extends Goal {
        private static final double RESCUE_DISTANCE_SQ = 24.0 * 24.0;
        private static final double CLEARANCE_ABOVE_TARGET = 3.0D;
        private static final double POSITION_LERP = 0.2D;
        private static final double LAUNCH_VELOCITY = 0.5D;
        private static final int LAUNCH_TICKS = 8;
        private static final float ARRIVE_DISTANCE = 3.0f;

        private int launchTicksElapsed;
        private boolean wasInvulnerable;

        RescueGoal() {
            this.setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            if (!followLock.isOwned() || isTalking()) {
                return false;
            }
            ServerPlayer target = RalseiEntity.this.followTarget();
            if (target == null) {
                return false;
            }
            return RalseiEntity.this.distanceToSqr(target) > RESCUE_DISTANCE_SQ
                    || RalseiEntity.this.getNavigation().isStuck();
        }
        @Override
        public boolean canContinueToUse() {
            if (!followLock.isOwned() || isTalking()) {
                return false;
            }
            ServerPlayer target = RalseiEntity.this.followTarget();
            if (target == null) {
                return false;
            }
            double dx = target.getX() - RalseiEntity.this.getX();
            double dz = target.getZ() - RalseiEntity.this.getZ();
            return dx * dx + dz * dz > ARRIVE_DISTANCE * ARRIVE_DISTANCE;
        }
        @Override
        public void start() {
            launchTicksElapsed = 0;
            rescuing = true;
            wasInvulnerable = RalseiEntity.this.isInvulnerable();
            RalseiEntity.this.setInvulnerable(true);
            RalseiEntity.this.setNoGravity(true);
            RalseiEntity.this.entityData.set(DATA_FLYING, true);
            RalseiEntity.this.getNavigation().stop();

            ServerPlayer target = RalseiEntity.this.followTarget();
            if (target != null) {
                RalseiEntity.this.turnToFace(target);
            }
        }
        @Override
        public void stop() {
            rescuing = false;
            RalseiEntity.this.setInvulnerable(wasInvulnerable);
            RalseiEntity.this.setNoGravity(false);
            RalseiEntity.this.entityData.set(DATA_FLYING, false);
            RalseiEntity.this.setDeltaMovement(Vec3.ZERO);
        }
        @Override
        public void tick() {
            ServerPlayer target = RalseiEntity.this.followTarget();
            if (target == null) {
                return;
            }

            launchTicksElapsed++;
            if (RalseiEntity.this.tickLaunchPhase(launchTicksElapsed, LAUNCH_TICKS, LAUNCH_VELOCITY)) {
                RalseiEntity.this.turnToFace(target);
                return;
            }

            double clearance = Math.max(CLEARANCE_ABOVE_TARGET, target.getY() - RalseiEntity.this.getY() + CLEARANCE_ABOVE_TARGET);
            RalseiEntity.this.flyTowardLerp(target.position().add(0, clearance, 0), POSITION_LERP);
            RalseiEntity.this.turnToFace(target);
        }
    }
    private class AerialFollowGoal extends Goal {
        private static final double FOLLOW_DISTANCE = 3.0D;
        private static final double POSITION_LERP = 0.2D;
        private static final double LAUNCH_VELOCITY = 0.5D;
        private static final int LAUNCH_TICKS = 8;

        AerialFollowGoal() {
            this.setFlags(EnumSet.of(Flag.MOVE));
        }

        private boolean targetIsFlying() {
            ServerPlayer target = RalseiEntity.this.followTarget();
            return target != null && target.isFallFlying();
        }
        @Override
        public boolean canUse() {
            return followLock.isOwned() && !isTalking() && !rescuing && targetIsFlying();
        }
        @Override
        public boolean canContinueToUse() {
            if (!followLock.isOwned() || isTalking() || rescuing) {
                return false;
            }
            if (targetIsFlying()) {
                return true;
            }
            return !closeToGround();
        }

        private boolean closeToGround() {
            ServerPlayer target = RalseiEntity.this.followTarget();
            return target == null || Math.abs(RalseiEntity.this.getY() - target.getY()) < 1.5;
        }

        private int launchTicksElapsed;

        @Override
        public void start() {
            launchTicksElapsed = 0;
            aerialFollowing = true;

            ServerPlayer target = RalseiEntity.this.followTarget();
            if (target != null) {
                RalseiEntity.this.turnToFace(target);
            }
        }
        @Override
        public void stop() {
            aerialFollowing = false;
            RalseiEntity.this.setNoGravity(false);
            RalseiEntity.this.entityData.set(DATA_FLYING, false);
        }
        @Override
        public void tick() {
            ServerPlayer target = RalseiEntity.this.followTarget();
            if (target == null) {
                return;
            }

            launchTicksElapsed++;
            if (RalseiEntity.this.tickLaunchPhase(launchTicksElapsed, LAUNCH_TICKS, LAUNCH_VELOCITY)) {
                RalseiEntity.this.turnToFace(target);
                return;
            }

            if (!RalseiEntity.this.isNoGravity()) {
                RalseiEntity.this.setNoGravity(true);
                RalseiEntity.this.entityData.set(DATA_FLYING, true);
            }

            Vec3 look = target.getLookAngle();
            Vec3 behind = new Vec3(-look.x, 0, -look.z).normalize().scale(FOLLOW_DISTANCE);
            RalseiEntity.this.flyTowardLerp(target.position().add(behind), POSITION_LERP);
            RalseiEntity.this.turnToFace(target);
        }
    }
    private class FollowPlayerGoal extends Goal {
        private static final double SPEED_MODIFIER = 1.075D;
        private static final double SPRINT_SPEED_MODIFIER = 1.6D;
        private static final double SPRINT_GAP_SQ = 6.0 * 6.0;
        private static final float START_DISTANCE = 4.0f;
        private static final float STOP_DISTANCE = 1.3f;
        private static final int RECALC_TICKS_NORMAL = 10;
        private static final int RECALC_TICKS_SPRINT = 4;
        private int timeToRecalcPath;

        FollowPlayerGoal() {
            this.setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            return followLock.isOwned() && !isTalking() && !aerialFollowing && !rescuing;
        }

        @Override
        public boolean canContinueToUse() {
            return canUse();
        }

        @Override
        public void start() {
            timeToRecalcPath = 0;
        }

        @Override
        public void stop() {
            RalseiEntity.this.getNavigation().stop();
        }

        @Override
        public void tick() {
            ServerPlayer target = RalseiEntity.this.followTarget();
            if (target == null) {
                return;
            }

            double distSq = RalseiEntity.this.distanceToSqr(target);
            boolean shouldMove = distSq > START_DISTANCE * START_DISTANCE;

            if (!shouldMove) {
                RalseiEntity.this.turnToFace(target);
            }

            if (--timeToRecalcPath > 0) {
                return;
            }

            if (shouldMove) {
                boolean shouldSprint = distSq > SPRINT_GAP_SQ || target.isSprinting();
                RalseiEntity.this.getNavigation().moveTo(target, shouldSprint ? SPRINT_SPEED_MODIFIER : SPEED_MODIFIER);
                timeToRecalcPath = shouldSprint ? RECALC_TICKS_SPRINT : RECALC_TICKS_NORMAL;
            } else if (distSq < STOP_DISTANCE * STOP_DISTANCE) {
                RalseiEntity.this.getNavigation().stop();
                timeToRecalcPath = RECALC_TICKS_NORMAL;
            } else {
                timeToRecalcPath = RECALC_TICKS_NORMAL;
            }
        }
    }
    private static float snapToNearest(float degrees, float increment) {
        return Math.round(Mth.wrapDegrees(degrees) / increment) * increment;
    }
    private static float yawTo(double dx, double dz) {
        return (float) (Mth.atan2(dz, dx) * (180D / Math.PI)) - 90F;
    }

    private static float applyHysteresisSnap(float rawDegrees, float increment, float hysteresisDegrees, float previousSnap) {
        float candidate = snapToNearest(rawDegrees, increment);
        boolean crossedThreshold = Mth.abs(Mth.wrapDegrees(candidate - previousSnap)) > hysteresisDegrees;
        return crossedThreshold ? candidate : previousSnap;
    }
    private void setFacingSnap(float snapped) {
        this.facingSnap = snapped;
        this.yBodyRot = snapped;
        this.yHeadRot = snapped;
        this.yBodyRotO = snapped;
        this.yHeadRotO = snapped;
    }
    public float getVisualFacingYaw() {
        return this.facingSnap;
    }

    private void turnToFace(double targetX, double targetZ) {
        double dx = targetX - this.getX();
        double dz = targetZ - this.getZ();
        if (dx * dx + dz * dz < MIN_FACING_DISTANCE_SQ) {
            return; // no meaningful horizontal direction, keep current facing
        }
        float rawYaw = yawTo(dx, dz);
        this.setYRot(rawYaw);
        this.yRotO = rawYaw;
    }
    public void turnToFace(Entity target) {
        turnToFace(target.getX(), target.getZ());
    }

    private boolean tickLaunchPhase(int elapsedTicks, int launchTicks, double launchVelocity) {
        if (elapsedTicks > launchTicks) {
            return false;
        }
        this.setDeltaMovement(0, launchVelocity, 0);
        return true;
    }
    private void flyTowardLerp(Vec3 targetPos, double lerpFactor) {
        Vec3 lerped = this.position().lerp(targetPos, lerpFactor);
        this.setPos(lerped.x, lerped.y, lerped.z);
        this.setDeltaMovement(Vec3.ZERO);
    }
    private @Nullable ServerPlayer followTarget() {
        return followLock.getOwner((ServerLevel) this.level());
    }
    private boolean isPlayerPresent(ServerPlayer player) {
        return !player.isRemoved() && player.isAlive() && player.level() == this.level();
    }
    private void enterTalkStance(ServerPlayer target) {
        turnToFace(target);
        this.getNavigation().stop();
        this.setSpeed(0f);
        this.entityData.set(DATA_TALKING, true);
    }
    private void enterTalkingState(ServerPlayer player) {
        enterTalkStance(player);
        resetBoredomTimer();
    }
    private void resetBoredomTimer() {
        boredomTicks = randomRange(MIN_BOREDOM_TICKS, MAX_BOREDOM_TICKS);
    }

    private void startBoredomTask(BoredomTask task) {
        currentBoredomTask = task;
        this.getNavigation().stop();
        if (task == BoredomTask.SLEEP) {
            boredomTaskTicks = randomRange(MIN_SLEEP_TICKS, MAX_SLEEP_TICKS);
            this.entityData.set(DATA_SLEEPING, true);
        } else {
            boredomTaskTicks = SING_DURATION_TICKS;
            this.entityData.set(DATA_SINGING, true);
            this.playSound(ModSounds.RALSEI_LULLABY, 1.0f, 1.0f);
        }
    }
    private void endBoredomTask() {
        this.entityData.set(DATA_SLEEPING, false);
        this.entityData.set(DATA_SINGING, false);
        currentBoredomTask = BoredomTask.NONE;
        resetBoredomTimer();
    }
    private String applyBoredomInterruptionEffects() {
        boolean wasSleeping = isSleeping();
        boolean wasSinging = isSinging();
        endBoredomTask();

        if (wasSleeping) {
            scaredTicks = SCARED_DURATION_TICKS;
            this.entityData.set(DATA_SCARED, true);
            return "sleep";
        }
        if (wasSinging) {
            for (ServerPlayer nearby : PlayerLookup.tracking(this)) {
                ServerPlayNetworking.send(nearby, new StopLullabyPayload(this.getId()));
            }
            return "sing";
        }
        return "";
    }
    public boolean interruptBoredomTask(ServerPlayer player) {
        applyBoredomInterruptionEffects();
        return beginTalking(player);
    }
    public @Nullable String beginTalkingWithInterruption(ServerPlayer player) {
        String interrupted = applyBoredomInterruptionEffects();
        return beginTalking(player) ? interrupted : null;
    }
    public boolean isSleeping() {
        return this.entityData.get(DATA_SLEEPING);
    }

    public boolean isSinging() {
        return this.entityData.get(DATA_SINGING);
    }

    public boolean isInBoredomTask() {
        return isSleeping() || isSinging();
    }

    public boolean isScared() {
        return this.entityData.get(DATA_SCARED);
    }

    public boolean beginTalking(ServerPlayer player) {
        if (!talkLock.tryAcquire(player)) {
            return false;
        }
        enterTalkingState(player);
        return true;
    }
    public void endTalking(ServerPlayer player) {
        if (escapeState == EscapeState.DIALOGUE) {
            this.entityData.set(DATA_TALKING, false);
            beginFlee();
            return;
        }

        talkLock.release(player.getUUID());
        if (talkLock.isOwned()) {
            return;
        }
        this.entityData.set(DATA_TALKING, false);

        if (pendingFollowAction == PendingFollowAction.START) {
            followLock.tryAcquire(player);
        } else if (pendingFollowAction == PendingFollowAction.STOP) {
            followLock.release(player.getUUID());
        }
        pendingFollowAction = PendingFollowAction.NONE;
    }
    public FollowPrompt requestFollowToggle(ServerPlayer player) {
        if (talkLock.isOwned() || isInBoredomTask()) {
            return FollowPrompt.DENIED;
        }

        boolean currentlyFollowingThisPlayer = followLock.isOwned()
                && followLock.getOwner((ServerLevel) this.level()) == player;
        if (followLock.isOwned() && !currentlyFollowingThisPlayer) {
            return FollowPrompt.DENIED;
        }
        if (!talkLock.tryAcquire(player)) {
            return FollowPrompt.DENIED;
        }

        enterTalkingState(player);
        pendingFollowAction = currentlyFollowingThisPlayer ? PendingFollowAction.STOP : PendingFollowAction.START;
        return currentlyFollowingThisPlayer ? FollowPrompt.STOP : FollowPrompt.START;
    }
    public boolean isTalking() {
        return this.entityData.get(DATA_TALKING);
    }

    public boolean isFlying() {
        return this.entityData.get(DATA_FLYING);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_FALLING, false);
        builder.define(DATA_LANDING, false);
        builder.define(DATA_JUMPING, false);
        builder.define(DATA_TALKING, false);
        builder.define(DATA_FLYING, false);
        builder.define(DATA_SLEEPING, false);
        builder.define(DATA_SINGING, false);
        builder.define(DATA_SCARED, false);
    }
    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        if (this.isInvulnerableTo(level, source)) {
            return false;
        }
        if (escapeState != EscapeState.NONE) {
            return false;
        }
        if (this.getHealth() - amount <= this.getMaxHealth() * DYING_HEALTH_FRACTION
                && source.getEntity() instanceof ServerPlayer attacker) {
            this.setHealth(Math.max(1.0f, this.getHealth() - amount));
            beginEscapeDialogue(attacker);
            return true;
        }
        return super.hurtServer(level, source, amount);
    }
    private void beginEscapeDialogue(ServerPlayer attacker) {
        escapeState = EscapeState.DIALOGUE;
        applyBoredomInterruptionEffects(); // stops the lullaby
        talkLock.forceRelease();
        followLock.forceRelease();
        enterTalkStance(attacker);
        talkLock.tryAcquire(attacker);
        ServerPlayNetworking.send(attacker, new DialogueOpenDyingPayload(this.getId()));
    }
    private void beginFlee() {
        escapeState = EscapeState.FLEEING;
        escapeTicks = 0;
        talkLock.forceRelease();

        double angle = Math.random() * Math.PI * 2;
        double horizontalDistance = ESCAPE_TARGET_HORIZONTAL_RANGE * (0.5 + Math.random() * 0.5);
        double targetY = this.getY() + ESCAPE_TARGET_MIN_HEIGHT_OFFSET + Math.random() * ESCAPE_TARGET_HEIGHT_RANGE;
        escapeTarget = new Vec3(
                this.getX() + Math.cos(angle) * horizontalDistance,
                targetY,
                this.getZ() + Math.sin(angle) * horizontalDistance
        );
        this.setInvulnerable(true);
        this.setNoGravity(true);
        this.entityData.set(DATA_FLYING, true);
    }
    private class EscapeGoal extends Goal {
        private static final double FLEE_SPEED = 2.5D;
        private static final double ESCAPE_LAUNCH_VELOCITY = 0.33D;
        private static final int ESCAPE_LAUNCH_TICKS = 4;

        EscapeGoal() {
            this.setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            return escapeState == EscapeState.FLEEING;
        }

        @Override
        public boolean canContinueToUse() {
            return escapeState == EscapeState.FLEEING;
        }

        @Override
        public void tick() {
            if (escapeTarget == null) {
                return;
            }
            escapeTicks++;

            if (RalseiEntity.this.tickLaunchPhase(escapeTicks, ESCAPE_LAUNCH_TICKS, ESCAPE_LAUNCH_VELOCITY)) {
                return;
            }

            Vec3 currentPos = RalseiEntity.this.position();
            Vec3 toTarget = escapeTarget.subtract(currentPos);
            double distanceToTarget = toTarget.length();

            if (distanceToTarget <= FLEE_SPEED) {
                RalseiEntity.this.discard();
                return;
            }

            Vec3 nextPos = currentPos.add(toTarget.scale(FLEE_SPEED / distanceToTarget));
            RalseiEntity.this.turnToFace(escapeTarget.x, escapeTarget.z);
            RalseiEntity.this.setPos(nextPos.x, nextPos.y, nextPos.z);
            RalseiEntity.this.setDeltaMovement(Vec3.ZERO);

            if (escapeTicks >= ESCAPE_TIMEOUT_TICKS) {
                RalseiEntity.this.discard(); // handles exceptions; should never be called
            }
        }
    }
    private void updateAirborneState() {
        boolean airborne = !this.onGround();
        double verticalMotion = this.getDeltaMovement().y;
        boolean falling = airborne && verticalMotion < 0 && this.fallDistance > MIN_FALL_DISTANCE;
        boolean jumping = airborne && verticalMotion > 0;

        if (wasFalling && this.onGround() && landingTicks < 0) {
            landingTicks = 0;
        }
        wasFalling = falling;

        if (landingTicks >= 0) {
            landingTicks++;
            if (landingTicks >= LANDING_ANIMATION_TICKS) {
                landingTicks = -1;
            }
        }
        this.entityData.set(DATA_FALLING, falling);
        this.entityData.set(DATA_LANDING, landingTicks >= 0);
        this.entityData.set(DATA_JUMPING, jumping);
    }
    @Override
    public void tick() {
        super.tick();

        float newFacing = applyHysteresisSnap(this.getYRot(), FACING_SNAP_INCREMENT, FACING_HYSTERESIS_DEGREES, this.facingSnap);
        if (newFacing != this.facingSnap) {
            setFacingSnap(newFacing);
        }

        if (this.level().isClientSide()) {
            return;
        }

        ServerLevel serverLevel = (ServerLevel) this.level();

        if (followLock.isOwned()) {
            followLock.validate(serverLevel, (level, player) -> isPlayerPresent(player));
        }
        if (talkLock.isOwned()) {
            ServerPlayer invalidated = talkLock.validate(serverLevel, (level, player) ->
                    isPlayerPresent(player) && player.distanceToSqr(this) <= MAX_TALK_DISTANCE_SQ
            );
            if (invalidated != null) {
                ServerPlayNetworking.send(invalidated, new DialogueForceClosePayload());
            }
            if (!talkLock.isOwned()) {
                this.entityData.set(DATA_TALKING, false);
            }
        }
        if (currentBoredomTask != BoredomTask.NONE) {
            boredomTaskTicks--;
            if (isShowingHurtSprite()) {
                applyBoredomInterruptionEffects();
            } else if (boredomTaskTicks < 0) {
                endBoredomTask();
            }
        } else if (!isTalking() && !followLock.isOwned() && this.onGround() && --boredomTicks <= 0) {
            startBoredomTask(Math.random() < 0.5 ? BoredomTask.SLEEP : BoredomTask.SING);
        }
        if (scaredTicks >= 0) {
            scaredTicks--;
            this.entityData.set(DATA_SCARED, scaredTicks >= 0);
        }

        updateAirborneState();
    }
    public boolean isFalling() {
        return this.entityData.get(DATA_FALLING);
    }
    public boolean isLanding() {
        return this.entityData.get(DATA_LANDING);
    }
    public boolean isJumping() {
        return this.entityData.get(DATA_JUMPING);
    }
    public boolean isShowingHurtSprite() {
        return this.hurtTime > 0;
    }
    public boolean isShowingDeathSprite() {
        return this.isDeadOrDying();
    }
    public void updateClientLandingTracking() {
        if (this.isLanding()) {
            if (clientLandingStartTick < 0) {
                clientLandingStartTick = this.tickCount;
            }
        } else {
            clientLandingStartTick = -1;
        }
    }
    public int getClientLandingElapsedTicks() {
        return clientLandingStartTick < 0 ? 0 : this.tickCount - clientLandingStartTick;
    }
    @Override
    public boolean isInvulnerableTo(ServerLevel level, DamageSource source) {
        if (source.is(DamageTypeTags.IS_EXPLOSION)) {
            return true;
        }
        if (source.getEntity() instanceof Player) {
            return super.isInvulnerableTo(level, source);
        }
        return true;
    }
    @Override
    public boolean fireImmune() {
        return true;
    }
}