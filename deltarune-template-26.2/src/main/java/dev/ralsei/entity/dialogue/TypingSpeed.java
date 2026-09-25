package dev.ralsei.entity.dialogue;

public enum TypingSpeed {
    SLOW(4),
    NORMAL(2),
    FAST(1);

    public final int ticksPerChar;

    TypingSpeed(int ticksPerChar) {
        this.ticksPerChar = ticksPerChar;
    }
}