package dev.ralsei.client.render;

import java.util.Locale;

public enum RalseiVisualState {
    IDLE(21, 41, true, 1, false, 1),
    HURT(30, 41, false, 1, false, 1),
    DEAD(29,41, false, 1, false, 1),
    FALL(26, 38, false, 3, true, 3),
    LAND(24, 41, false, 2, false, 6),
    WALK(21, 41, true, 4, true, 4),
    JUMP(33, 47, false, 4, true, 4),
    SPRINT(33, 40, true, 6, true, 3),
    FLY(31, 47, true, 6, true, 3),
    SLEEP(21, 40, false, 2, true, 9),
    SCARED(34, 36, false, 1, false, 1),
    SING(27, 42, false, 3, true, 3),
    HAPPY(32, 40, false, 1, false, 1),
    SHOCKED(31, 41, false, 1, false, 1),
    SHY(21, 41, true, 1, false, 1),
    BLUSH(21, 41, true, 1, false, 1),
    GIGGLE(21, 41, false, 8, false, 12),
    EXCITED(21, 40, false, 3, false, 3),
    WAVE(26, 40, false, 6, false, 12),
    DYING(24, 40, false, 1, false, 1);

    public final int frameWidth;
    public final int frameHeight;
    public final boolean directional;
    public final int frameCount;
    public final boolean loop;
    public final int frameTimeTicks;

    RalseiVisualState(int frameWidth, int frameHeight, boolean directional, int frameCount, boolean loop, int frameTimeTicks) {
        this.frameWidth = frameWidth;
        this.frameHeight = frameHeight;
        this.directional = directional;
        this.frameCount = frameCount;
        this.loop = loop;
        this.frameTimeTicks = frameTimeTicks;
    }

    public String fileName() {
        return name().toLowerCase(Locale.ROOT);
    }
}