package dev.ralsei.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.ralsei.Deltarune;
import dev.ralsei.entity.RalseiEntity;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

import java.util.EnumMap;
import java.util.Map;

public class RalseiEntityRenderer extends EntityRenderer<RalseiEntity, RalseiEntityRenderState> {

    private static final float SPRITE_SCALE = 1f / 21f;
    private static final float WALK_ANIMATION_THRESHOLD = 0.3125f;
    private static final float SPRINT_ANIMATION_THRESHOLD = 0.6875f;

    private static final Map<RalseiVisualState, Map<Direction4, RenderType>> RENDER_TYPES = buildRenderTypes();

    private static Map<RalseiVisualState, Map<Direction4, RenderType>> buildRenderTypes() {
        Map<RalseiVisualState, Map<Direction4, RenderType>> byState = new EnumMap<>(RalseiVisualState.class);
        for (RalseiVisualState visualState : RalseiVisualState.values()) {
            Map<Direction4, RenderType> byDirection = new EnumMap<>(Direction4.class);
            if (visualState.directional) {
                for (Direction4 direction : Direction4.values()) {
                    byDirection.put(direction, renderTypeFor(direction.folderName(), visualState.fileName()));
                }
            } else {
                RenderType front = renderTypeFor("front", visualState.fileName());
                for (Direction4 direction : Direction4.values()) {
                    byDirection.put(direction, front);
                }
            }
            byState.put(visualState, byDirection);
        }
        return byState;
    }

    private static RenderType renderTypeFor(String directionFolder, String fileName) {
        Identifier texture = Deltarune.id("textures/entity/npc/ralsei/" + directionFolder + "/" + fileName + ".png");
        return RenderTypes.entityCutout(texture);
    }

    public RalseiEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.4f;
    }

    private static RalseiVisualState resolveTalkOverride(String key) {
        if (key == null) return null;
        return switch (key) {
            case "happy" -> RalseiVisualState.HAPPY;
            case "excited" -> RalseiVisualState.EXCITED;
            case "shocked" -> RalseiVisualState.SHOCKED;
            case "shy" -> RalseiVisualState.SHY;
            case "blush" -> RalseiVisualState.BLUSH;
            case "giggle" -> RalseiVisualState.GIGGLE;
            case "wave" -> RalseiVisualState.WAVE;
            case "dying" -> RalseiVisualState.DYING;
            default -> null;
        };
    }

    @Override
    public RalseiEntityRenderState createRenderState() {
        return new RalseiEntityRenderState();
    }

    @Override
    public void extractRenderState(RalseiEntity entity, RalseiEntityRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.entityX = entity.getX();
        state.entityZ = entity.getZ();
        state.entityYaw = entity.getVisualFacingYaw();

        RalseiVisualState talkOverride = entity.isTalking() ? resolveTalkOverride(entity.clientTalkOverride) : null;

        RalseiVisualState visualState;
        if (entity.isShowingHurtSprite()) {
            visualState = RalseiVisualState.HURT;
        } else if (entity.isShowingDeathSprite()) {
            visualState = RalseiVisualState.DEAD;
        } else if (entity.isScared()) {
            visualState = RalseiVisualState.SCARED;
        } else if (entity.isLanding()) {
            visualState = RalseiVisualState.LAND;
        } else if (entity.isJumping()) {
            visualState = RalseiVisualState.JUMP;
        } else if (entity.isFlying()) {
            visualState = RalseiVisualState.FLY;
        } else if (entity.isFalling()) {
            visualState = RalseiVisualState.FALL;
        } else if (entity.isSleeping()) {
            visualState = RalseiVisualState.SLEEP;
        } else if (entity.isSinging()) {
            visualState = RalseiVisualState.SING;
        } else if (talkOverride != null) {
            visualState = talkOverride;
        } else if (entity.walkAnimation.speed() > SPRINT_ANIMATION_THRESHOLD) {
            visualState = RalseiVisualState.SPRINT;
        } else if (entity.walkAnimation.speed() > WALK_ANIMATION_THRESHOLD) {
            visualState = RalseiVisualState.WALK;
        } else {
            visualState = RalseiVisualState.IDLE;
        }
        state.visualState = visualState;

        if (entity.isLanding()) {
            entity.updateClientLandingTracking();
        }
        if (visualState == talkOverride) {
            entity.tickClientTalkOverride();
        }

        if (visualState == RalseiVisualState.LAND) {
            state.animationFrame = Math.min(entity.getClientLandingElapsedTicks() / visualState.frameTimeTicks, visualState.frameCount - 1);
        } else if (!visualState.loop && visualState.frameCount > 1) {
            // one-shot multi-frame talk overlays (GIGGLE, WAVE) -- hold last frame once played through
            state.animationFrame = Math.min(entity.getClientTalkOverrideTicks() / visualState.frameTimeTicks, visualState.frameCount - 1);
        } else if (visualState.loop && visualState.frameCount > 1) {
            state.animationFrame = (entity.tickCount / visualState.frameTimeTicks) % visualState.frameCount;
        } else {
            state.animationFrame = 0;
        }
    }

    @Override
    public void submit(RalseiEntityRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera) {
        poseStack.pushPose();

        Direction4 facing = state.visualState.directional ? computeFacing(state, camera) : Direction4.FRONT;
        RenderType renderType = RENDER_TYPES.get(state.visualState).get(facing);

        float rotation;
        if (state.visualState.directional) {
            rotation = 180.0f - state.entityYaw - offsetFor(facing);
        } else {
            double dx = camera.pos.x - state.entityX;
            double dz = camera.pos.z - state.entityZ;
            float angleToCamera = (float) (Mth.atan2(dz, dx) * (180D / Math.PI)) - 90f;
            rotation = 180.0f - angleToCamera;
        }
        poseStack.mulPose(Axis.YP.rotationDegrees(rotation));

        float width = state.visualState.frameWidth * SPRITE_SCALE;
        float height = state.visualState.frameHeight * SPRITE_SCALE;
        float halfWidth = width / 2f;

        float v0 = state.animationFrame / (float) state.visualState.frameCount;
        float v1 = (state.animationFrame + 1) / (float) state.visualState.frameCount;

        submitNodeCollector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
            vertex(buffer, pose, -halfWidth, height, 0f, v0, state.lightCoords);
            vertex(buffer, pose,  halfWidth, height, 1f, v0, state.lightCoords);
            vertex(buffer, pose,  halfWidth, 0,      1f, v1, state.lightCoords);
            vertex(buffer, pose, -halfWidth, 0,      0f, v1, state.lightCoords);
        });

        poseStack.popPose();
        super.submit(state, poseStack, submitNodeCollector, camera);
    }

    private static Direction4 computeFacing(RalseiEntityRenderState state, CameraRenderState camera) {
        double dx = camera.pos.x - state.entityX;
        double dz = camera.pos.z - state.entityZ;
        float angleToCamera = (float) (Mth.atan2(dz, dx) * (180D / Math.PI)) - 90f;
        float relativeAngle = Mth.wrapDegrees(angleToCamera - state.entityYaw);
        return Direction4.fromAngle(relativeAngle);
    }

    private static float offsetFor(Direction4 facing) {
        return switch (facing) {
            case FRONT -> 0f;
            case RIGHT -> 270f;
            case BACK  -> 180f;
            case LEFT  -> 90f;
        };
    }

    private static void vertex(VertexConsumer buffer, PoseStack.Pose pose, float x, float y, float u, float v, int light) {
        buffer.addVertex(pose, x, y, 0.0F)
                .setColor(255, 255, 255, 255)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light)
                .setNormal(pose, 0.0F, 1.0F, 0.0F);
    }
}