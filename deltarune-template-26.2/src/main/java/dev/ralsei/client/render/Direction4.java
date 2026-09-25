package dev.ralsei.client.render;

public enum Direction4 {
    FRONT, BACK, LEFT, RIGHT;

    public static Direction4 fromAngle(float angle) {
        if (angle >= -45f && angle < 45f) return FRONT;
        if (angle >= 45f && angle < 135f) return LEFT;   //flipped for some reason don't change
        if (angle >= -135f && angle < -45f) return RIGHT;
        return BACK;
    }

    public String folderName() {
        return name().toLowerCase(java.util.Locale.ROOT);
    }
}