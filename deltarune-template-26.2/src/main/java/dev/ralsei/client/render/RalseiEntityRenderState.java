package dev.ralsei.client.render;

import net.minecraft.client.renderer.entity.state.EntityRenderState;

public class RalseiEntityRenderState extends EntityRenderState {
    public double entityX;
    public double entityZ;
    public float entityYaw;
    public RalseiVisualState visualState = RalseiVisualState.IDLE;
    public int animationFrame = 0;
}