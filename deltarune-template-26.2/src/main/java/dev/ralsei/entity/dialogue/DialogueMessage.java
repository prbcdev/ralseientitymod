package dev.ralsei.entity.dialogue;

public record DialogueMessage(
        String text, TypingSpeed typingSpeed, String portrait, TextAnimation animation,
        TextSize size, TextAlignment alignment, String talkOverride
) {
    public DialogueMessage(String text) {
        this(text, TypingSpeed.NORMAL, "ralsei_mood_default_smile", TextAnimation.STILL, TextSize.NORMAL, TextAlignment.LEFT, null);
    }
    public DialogueMessage(String text, TypingSpeed typingSpeed) {
        this(text, typingSpeed, "ralsei_mood_default_smile", TextAnimation.STILL, TextSize.NORMAL, TextAlignment.LEFT, null);
    }
    public DialogueMessage(String text, TypingSpeed typingSpeed, String portrait) {
        this(text, typingSpeed, portrait, TextAnimation.STILL, TextSize.NORMAL, TextAlignment.LEFT, null);
    }
    public DialogueMessage(String text, TypingSpeed typingSpeed, String portrait, TextAnimation animation) {
        this(text, typingSpeed, portrait, animation, TextSize.NORMAL, TextAlignment.LEFT, null);
    }
    public DialogueMessage(String text, TypingSpeed typingSpeed, String portrait, TextAnimation animation, TextSize size, TextAlignment alignment) {
        this(text, typingSpeed, portrait, animation, size, alignment, null);
    }
}