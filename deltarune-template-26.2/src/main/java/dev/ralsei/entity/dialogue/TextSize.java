package dev.ralsei.entity.dialogue;

public enum TextSize {
    SMALL(0.5f),
    NORMAL(1.0f),
    BIG(1.5f);

    public final float multiplier;

    TextSize(float multiplier) {
        this.multiplier = multiplier;
    }
}