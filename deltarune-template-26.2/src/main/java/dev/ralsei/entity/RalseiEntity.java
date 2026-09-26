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

    private static final EntityDataAccessor<Boolean> dFalling  = SynchedEntityData.defineId(RalseiEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> dLanding  = SynchedEntityData.defineId(RalseiEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> dJumping  = SynchedEntityData.defineId(RalseiEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> dTalking  = SynchedEntityData.defineId(RalseiEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> dFlying   = SynchedEntityData.defineId(RalseiEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> dSleeping = SynchedEntityData.defineId(RalseiEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> dSinging  = SynchedEntityData.defineId(RalseiEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> dScared   = SynchedEntityData.defineId(RalseiEntity.class, EntityDataSerializers.BOOLEAN);

    private static final float minFallDist  = 1.0f;                 // fall-dist before "falling" sprite kicks in
    private static final int landAnimTicks  = 10;                   // land sprite hold time
    private boolean wasFalling = false;
    private int landTicks = -1;
    private int landStartTick = -1;                                 // client-side copy, for anim timing

    private static final int minBoredom = 20 * 10;                  // idle ticks - min/max before sleep or sing
    private static final int maxBoredom = 20 * 15;
    private static final int minSleep   = 20 * 300;
    private static final int maxSleep   = 20 * 450;
    private static final int scaredDur  = 9;                        // ticks - scared sprite after woken
    private static final int singDur    = 20 * 48;                  // lullaby length

    private static final float dyingHpFrac   = 0.5f;                // hp% that triggers flee dialogue
    private static final double escapeRangeXZ = 300.0D;             // flee target - horizontal spread
    private static final double escapeMinY    = 100.0D;             // flee target - min height above start
    private static final double escapeRangeY  = 100.0D;             // flee target - extra height spread
    private static final int escapeTimeout    = 20 * 40;            // failsafe despawn

    private enum EscapeState { NONE, DIALOGUE, FLEEING }
    private EscapeState escapeState = EscapeState.NONE;
    private Vec3 escapeTarget = null;
    private int escapeTicks = 0;

    public boolean isEscaping() {
        return escapeState != EscapeState.NONE;
    }
    private enum BoredomTask { NONE, SLEEP, SING }
    private BoredomTask boredomTask = BoredomTask.NONE;
    private int taskTicks = -1;
    private int boredomTicks = rand(minBoredom, maxBoredom);
    private int scaredTicks = -1;

    private static int rand(int min, int max) {
        return min + (int) (Math.random() * (max - min));
    }
    private static final float faceSnapStep = 90f;                  // facing snap - 4-way
    private static final float moveSnapStep = 45f;                  // movement heading snap - 8-way
    private static final float faceHyst = 30f;                      // hysteresis deg - face
    private static final float moveHyst = 20f;                      // hysteresis deg - move
    private float faceSnap = 0f;
    private float lastMoveHead = 0f;

    private static final double minFaceDistSq = 1.0E-6D; // from vanilla lookcontrol - don't change

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
    private String lastOverride = null;
    private int overrideTicks = 0;
    public void tickClientTalkOverride() {
        if (!java.util.Objects.equals(clientTalkOverride, lastOverride)) {
            overrideTicks = 0;
            lastOverride = clientTalkOverride;
        } else {
            overrideTicks++;
        }
    }
    public int getClientTalkOverrideTicks() {
        return overrideTicks;
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
        private static final double stopDistSq = 0.01D;  // sq-dist - stop threshold
        private static final double jumpStepMin = 0.5D;  // min step height to trigger a hop
        private static final double jumpPush = 0.35D;    // horizontal push on hop
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

            if (dx * dx + dz * dz < stopDistSq) {
                this.operation = Operation.WAIT;
                RalseiEntity.this.setSpeed(0f);
                return;
            }
            tryJumpIfBlocked(dx, dz);
            float rawHeading = yawTo(dx, dz);
            lastMoveHead = hystSnap(rawHeading, moveSnapStep, moveHyst, lastMoveHead);
            RalseiEntity.this.setYRot(lastMoveHead);
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
            if (stepHeight <= jumpStepMin) {
                return;
            }
            boolean clearAbove = level.getBlockState(ahead.above()).getCollisionShape(level, ahead.above()).isEmpty();
            if (!clearAbove) {
                return;
            }
            RalseiEntity.this.setDeltaMovement(new Vec3(
                    facing.getStepX() * jumpPush,
                    RalseiEntity.this.getJumpPower(),
                    facing.getStepZ() * jumpPush
            ));
        }
    }
    private class RescueGoal extends Goal {
        private static final double rescueDistSq = 24.0 * 24.0;     // trigger dist - too far/stuck
        private static final double clearance = 3.0D;               // hover height above target
        private static final double lerp = 0.2D;
        private static final double launchVel = 0.5D;
        private static final int launchTicks = 8;
        private static final float arriveDist = 3.0f;
        private int launchElapsed;
        private boolean wasInvuln;
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
            return RalseiEntity.this.distanceToSqr(target) > rescueDistSq
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
            return dx * dx + dz * dz > arriveDist * arriveDist;
        }
        @Override
        public void start() {
            launchElapsed = 0;
            rescuing = true;
            wasInvuln = RalseiEntity.this.isInvulnerable();
            RalseiEntity.this.setInvulnerable(true);
            RalseiEntity.this.setNoGravity(true);
            RalseiEntity.this.entityData.set(dFlying, true);
            RalseiEntity.this.getNavigation().stop();

            ServerPlayer target = RalseiEntity.this.followTarget();
            if (target != null) {
                RalseiEntity.this.turnToFace(target);
            }
        }
        @Override
        public void stop() {
            rescuing = false;
            RalseiEntity.this.setInvulnerable(wasInvuln);
            RalseiEntity.this.setNoGravity(false);
            RalseiEntity.this.entityData.set(dFlying, false);
            RalseiEntity.this.setDeltaMovement(Vec3.ZERO);
        }
        @Override
        public void tick() {
            ServerPlayer target = RalseiEntity.this.followTarget();
            if (target == null) {
                return;
            }
            launchElapsed++;
            if (RalseiEntity.this.launchTick(launchElapsed, launchTicks, launchVel)) {
                RalseiEntity.this.turnToFace(target);
                return;
            }
            double clear = Math.max(clearance, target.getY() - RalseiEntity.this.getY() + clearance);
            RalseiEntity.this.lerpTo(target.position().add(0, clear, 0), lerp);
            RalseiEntity.this.turnToFace(target);
        }
    }
    private class AerialFollowGoal extends Goal {
        private static final double followDist = 3.0D;              // hover dist - behind target
        private static final double lerp = 0.2D;
        private static final double launchVel = 0.5D;
        private static final int launchTicks = 8;

        AerialFollowGoal() {
            this.setFlags(EnumSet.of(Flag.MOVE));
        }
        private boolean targetFlying() {
            ServerPlayer target = RalseiEntity.this.followTarget();
            return target != null && target.isFallFlying();
        }
        @Override
        public boolean canUse() {
            return followLock.isOwned() && !isTalking() && !rescuing && targetFlying();
        }
        @Override
        public boolean canContinueToUse() {
            if (!followLock.isOwned() || isTalking() || rescuing) {
                return false;
            }
            if (targetFlying()) {
                return true;
            }
            return !closeToGround();
        }
        private boolean closeToGround() {
            ServerPlayer target = RalseiEntity.this.followTarget();
            return target == null || Math.abs(RalseiEntity.this.getY() - target.getY()) < 1.5;
        }
        private int launchElapsed;
        @Override
        public void start() {
            launchElapsed = 0;
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
            RalseiEntity.this.entityData.set(dFlying, false);
        }
        @Override
        public void tick() {
            ServerPlayer target = RalseiEntity.this.followTarget();
            if (target == null) {
                return;
            }
            launchElapsed++;
            if (RalseiEntity.this.launchTick(launchElapsed, launchTicks, launchVel)) {
                RalseiEntity.this.turnToFace(target);
                return;
            }
            if (!RalseiEntity.this.isNoGravity()) {
                RalseiEntity.this.setNoGravity(true);
                RalseiEntity.this.entityData.set(dFlying, true);
            }
            Vec3 look = target.getLookAngle();
            Vec3 behind = new Vec3(-look.x, 0, -look.z).normalize().scale(followDist);
            RalseiEntity.this.lerpTo(target.position().add(behind), lerp);
            RalseiEntity.this.turnToFace(target);
        }
    }
    private class FollowPlayerGoal extends Goal {
        private static final double speedMod = 1.075D;                      // walk speed mult
        private static final double sprintMod = 1.6D;                       // sprint speed mult
        private static final double sprintGapSq = 6.0 * 6.0;                // gap that forces sprint
        private static final float startDist = 4.0f;                        // gap before moving
        private static final float stopDist = 1.3f;                         // gap before stopping
        private static final int recalcNormal = 10;                         // path recalc cadence
        private static final int recalcSprint = 4;
        private int recalcIn;
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
            recalcIn = 0;
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
            boolean shouldMove = distSq > startDist * startDist;
            if (!shouldMove) {
                RalseiEntity.this.turnToFace(target);
            }
            if (--recalcIn > 0) {
                return;
            }
            if (shouldMove) {
                boolean sprint = distSq > sprintGapSq || target.isSprinting();
                RalseiEntity.this.getNavigation().moveTo(target, sprint ? sprintMod : speedMod);
                recalcIn = sprint ? recalcSprint : recalcNormal;
            } else if (distSq < stopDist * stopDist) {
                RalseiEntity.this.getNavigation().stop();
                recalcIn = recalcNormal;
            } else {
                recalcIn = recalcNormal;
            }
        }
    }
    private static float snap(float degrees, float increment) {
        return Math.round(Mth.wrapDegrees(degrees) / increment) * increment;
    }
    private static float yawTo(double dx, double dz) {
        return (float) (Mth.atan2(dz, dx) * (180D / Math.PI)) - 90F;
    }
    private static float hystSnap(float rawDegrees, float increment, float hystDegrees, float prevSnap) {
        float candidate = snap(rawDegrees, increment);
        boolean crossed = Mth.abs(Mth.wrapDegrees(candidate - prevSnap)) > hystDegrees;
        return crossed ? candidate : prevSnap;
    }
    private void setFaceSnap(float snapped) {
        this.faceSnap = snapped;
        this.yBodyRot = snapped;
        this.yHeadRot = snapped;
        this.yBodyRotO = snapped;
        this.yHeadRotO = snapped;
    }
    public float getVisualFacingYaw() {
        return this.faceSnap;
    }
    private void turnToFace(double targetX, double targetZ) {
        double dx = targetX - this.getX();
        double dz = targetZ - this.getZ();
        if (dx * dx + dz * dz < minFaceDistSq) {
            return; // no meaningful direction - keep current facing
        }
        float rawYaw = yawTo(dx, dz);
        this.setYRot(rawYaw);
        this.yRotO = rawYaw;
    }
    public void turnToFace(Entity target) {
        turnToFace(target.getX(), target.getZ());
    }
    private boolean launchTick(int elapsed, int launchTicks, double launchVel) {
        if (elapsed > launchTicks) {
            return false;
        }
        this.setDeltaMovement(0, launchVel, 0);
        return true;
    }
    private void lerpTo(Vec3 targetPos, double lerpFactor) {
        Vec3 lerped = this.position().lerp(targetPos, lerpFactor);
        this.setPos(lerped.x, lerped.y, lerped.z);
        this.setDeltaMovement(Vec3.ZERO);
    }
    private @Nullable ServerPlayer followTarget() {
        return followLock.getOwner((ServerLevel) this.level());
    }
    private boolean present(ServerPlayer player) {
        return !player.isRemoved() && player.isAlive() && player.level() == this.level();
    }
    private void enterStance(ServerPlayer target) {
        turnToFace(target);
        this.getNavigation().stop();
        this.setSpeed(0f);
        this.entityData.set(dTalking, true);
    }
    private void enterTalk(ServerPlayer player) {
        enterStance(player);
        resetBoredom();
    }
    private void resetBoredom() {
        boredomTicks = rand(minBoredom, maxBoredom);
    }
    private void startBoredom(BoredomTask task) {
        boredomTask = task;
        this.getNavigation().stop();
        if (task == BoredomTask.SLEEP) {
            taskTicks = rand(minSleep, maxSleep);
            this.entityData.set(dSleeping, true);
        } else {
            taskTicks = singDur;
            this.entityData.set(dSinging, true);
            this.playSound(ModSounds.RALSEI_LULLABY, 1.0f, 1.0f);
        }
    }
    private void endBoredom() {
        this.entityData.set(dSleeping, false);
        this.entityData.set(dSinging, false);
        boredomTask = BoredomTask.NONE;
        resetBoredom();
    }
    private String breakBoredom() {
        boolean wasSleeping = isSleeping();
        boolean wasSinging = isSinging();
        endBoredom();

        if (wasSleeping) {
            scaredTicks = scaredDur;
            this.entityData.set(dScared, true);
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
        breakBoredom();
        return beginTalking(player);
    }
    public @Nullable String beginTalkingWithInterruption(ServerPlayer player) {
        String interrupted = breakBoredom();
        return beginTalking(player) ? interrupted : null;
    }
    public boolean isSleeping() {
        return this.entityData.get(dSleeping);
    }
    public boolean isSinging() {
        return this.entityData.get(dSinging);
    }
    public boolean isInBoredomTask() {
        return isSleeping() || isSinging();
    }
    public boolean isScared() {
        return this.entityData.get(dScared);
    }
    public boolean beginTalking(ServerPlayer player) {
        if (!talkLock.tryAcquire(player)) {
            return false;
        }
        enterTalk(player);
        return true;
    }
    public void endTalking(ServerPlayer player) {
        if (escapeState == EscapeState.DIALOGUE) {
            this.entityData.set(dTalking, false);
            beginFlee();
            return;
        }
        talkLock.release(player.getUUID());
        if (talkLock.isOwned()) {
            return;
        }
        this.entityData.set(dTalking, false);

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
        boolean alreadyFollowing = followLock.isOwned()
                && followLock.getOwner((ServerLevel) this.level()) == player;
        if (followLock.isOwned() && !alreadyFollowing) {
            return FollowPrompt.DENIED;
        }
        if (!talkLock.tryAcquire(player)) {
            return FollowPrompt.DENIED;
        }
        enterTalk(player);
        pendingFollowAction = alreadyFollowing ? PendingFollowAction.STOP : PendingFollowAction.START;
        return alreadyFollowing ? FollowPrompt.STOP : FollowPrompt.START;
    }
    public boolean isTalking() {
        return this.entityData.get(dTalking);
    }
    public boolean isFlying() {
        return this.entityData.get(dFlying);
    }
    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(dFalling, false);
        builder.define(dLanding, false);
        builder.define(dJumping, false);
        builder.define(dTalking, false);
        builder.define(dFlying, false);
        builder.define(dSleeping, false);
        builder.define(dSinging, false);
        builder.define(dScared, false);
    }
    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        if (this.isInvulnerableTo(level, source)) {
            return false;
        }
        if (escapeState != EscapeState.NONE) {
            return false;
        }
        if (this.getHealth() - amount <= this.getMaxHealth() * dyingHpFrac
                && source.getEntity() instanceof ServerPlayer attacker) {
            this.setHealth(Math.max(1.0f, this.getHealth() - amount));
            beginEscape(attacker);
            return true;
        }
        return super.hurtServer(level, source, amount);
    }
    private void beginEscape(ServerPlayer attacker) {
        escapeState = EscapeState.DIALOGUE;
        breakBoredom(); // stops the lullaby
        talkLock.forceRelease();
        followLock.forceRelease();
        enterStance(attacker);
        talkLock.tryAcquire(attacker);
        ServerPlayNetworking.send(attacker, new DialogueOpenDyingPayload(this.getId()));
    }
    private void beginFlee() {
        escapeState = EscapeState.FLEEING;
        escapeTicks = 0;
        talkLock.forceRelease();
        double angle = Math.random() * Math.PI * 2;
        double distXZ = escapeRangeXZ * (0.5 + Math.random() * 0.5);
        double targetY = this.getY() + escapeMinY + Math.random() * escapeRangeY;
        escapeTarget = new Vec3(
                this.getX() + Math.cos(angle) * distXZ,
                targetY,
                this.getZ() + Math.sin(angle) * distXZ
        );
        this.setInvulnerable(true);
        this.setNoGravity(true);
        this.entityData.set(dFlying, true);
    }
    private class EscapeGoal extends Goal {
        private static final double fleeSpeed = 2.5D;
        private static final double launchVel = 0.33D;
        private static final int launchTicks = 4;
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
            if (RalseiEntity.this.launchTick(escapeTicks, launchTicks, launchVel)) {
                return;
            }
            Vec3 pos = RalseiEntity.this.position();
            Vec3 toTarget = escapeTarget.subtract(pos);
            double dist = toTarget.length();

            if (dist <= fleeSpeed) {
                RalseiEntity.this.discard();
                return;
            }
            Vec3 next = pos.add(toTarget.scale(fleeSpeed / dist));
            RalseiEntity.this.turnToFace(escapeTarget.x, escapeTarget.z);
            RalseiEntity.this.setPos(next.x, next.y, next.z);
            RalseiEntity.this.setDeltaMovement(Vec3.ZERO);
            if (escapeTicks >= escapeTimeout) {
                RalseiEntity.this.discard(); // failsafe - shouldn't be reached
            }
        }
    }
    private void updateAirborne() {
        boolean airborne = !this.onGround();
        double vMotion = this.getDeltaMovement().y;
        boolean falling = airborne && vMotion < 0 && this.fallDistance > minFallDist;
        boolean jumping = airborne && vMotion > 0;

        if (wasFalling && this.onGround() && landTicks < 0) {
            landTicks = 0;
        }
        wasFalling = falling;
        if (landTicks >= 0) {
            landTicks++;
            if (landTicks >= landAnimTicks) {
                landTicks = -1;
            }
        }
        this.entityData.set(dFalling, falling);
        this.entityData.set(dLanding, landTicks >= 0);
        this.entityData.set(dJumping, jumping);
    }
    @Override
    public void tick() {
        super.tick();
        float newFace = hystSnap(this.getYRot(), faceSnapStep, faceHyst, this.faceSnap);
        if (newFace != this.faceSnap) {
            setFaceSnap(newFace);
        }

        if (this.level().isClientSide()) {
            return;
        }
        ServerLevel lvl = (ServerLevel) this.level();
        if (followLock.isOwned()) {
            followLock.validate(lvl, (level, player) -> present(player));
        }
        if (talkLock.isOwned()) {
            ServerPlayer invalidated = talkLock.validate(lvl, (level, player) ->
                    present(player) && player.distanceToSqr(this) <= MAX_TALK_DISTANCE_SQ
            );
            if (invalidated != null) {
                ServerPlayNetworking.send(invalidated, new DialogueForceClosePayload());
            }
            if (!talkLock.isOwned()) {
                this.entityData.set(dTalking, false);
            }
        }
        if (boredomTask != BoredomTask.NONE) {
            taskTicks--;
            if (isShowingHurtSprite()) {
                breakBoredom();
            } else if (taskTicks < 0) {
                endBoredom();
            }
        } else if (!isTalking() && !followLock.isOwned() && this.onGround() && --boredomTicks <= 0) {
            startBoredom(Math.random() < 0.5 ? BoredomTask.SLEEP : BoredomTask.SING);
        }
        if (scaredTicks >= 0) {
            scaredTicks--;
            this.entityData.set(dScared, scaredTicks >= 0);
        }

        updateAirborne();
    }
    public boolean isFalling() {
        return this.entityData.get(dFalling);
    }
    public boolean isLanding() {
        return this.entityData.get(dLanding);
    }
    public boolean isJumping() {
        return this.entityData.get(dJumping);
    }
    public boolean isShowingHurtSprite() {
        return this.hurtTime > 0;
    }
    public boolean isShowingDeathSprite() {
        return this.isDeadOrDying();
    }
    public void updateClientLandingTracking() {
        if (this.isLanding()) {
            if (landStartTick < 0) {
                landStartTick = this.tickCount;
            }
        } else {
            landStartTick = -1;
        }
    }
    public int getClientLandingElapsedTicks() {
        return landStartTick < 0 ? 0 : this.tickCount - landStartTick;
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