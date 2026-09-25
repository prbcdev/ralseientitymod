package dev.ralsei.client.gui;

import dev.ralsei.Deltarune;
import dev.ralsei.entity.RalseiEntity;
import dev.ralsei.entity.dialogue.*;
import dev.ralsei.network.DialogueClosedPayload;
import dev.ralsei.sound.ModSounds;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.List;

public class RalseiDialogueScreen extends Screen {

    private static final Identifier BOX_TEXTURE = Deltarune.id("textures/gui/dialogue/box.png");
    private static final int BOX_TEXTURE_WIDTH = 289;  // was 296
    private static final int BOX_TEXTURE_HEIGHT = 76;   // was 420

    private static final int PORTRAIT_WIDTH = 48;
    private static final int PORTRAIT_HEIGHT = 51;

    private static final int PADDING = 14;

    private static final int CONTENT_HEIGHT = BOX_TEXTURE_HEIGHT - PADDING * 2; // 56

    private static final float PORTRAIT_SCALE = CONTENT_HEIGHT / (float) PORTRAIT_HEIGHT;

    private static final int TARGET_TEXT_ROWS = 3;
    private static final int LINE_STEP = 10;
    private static final float TEXT_SCALE = (CONTENT_HEIGHT / (float) TARGET_TEXT_ROWS) / LINE_STEP;

    private static final int CHAR_SPACING_ADJUST = -1;
    private static final int SPACE_WIDTH_BONUS = 2;
    private static final int PERIOD_WIDTH_BONUS = 1;

    private static final int UI_SCALE = 1;
    private static final int BOTTOM_MARGIN = 64;

    private static final int FADE_DURATION_TICKS = 3;
    private static final int SLIDE_DISTANCE = 21;

    private static final int ASTERISK_GAP = 4;

    private static final float SCARED_ACTIVE_CHANCE = 0.18f;
    private static final float SCARED_OFFSET_SCALE = 0.35f; // fraction of a local text unit

    private static long mix64(long x) {
        x = (x ^ (x >>> 30)) * 0xbf58476d1ce4e5b9L;
        x = (x ^ (x >>> 27)) * 0x94d049bb133111ebL;
        return x ^ (x >>> 31);
    }

    private Component asteriskGlyph;
    private int asteriskIndentLocal;

    private static final FontDescription DELTARUNE_FONT = new FontDescription.Resource(Deltarune.id("deltarune"));
    private static final Style GLYPH_STYLE = Style.EMPTY.withFont(DELTARUNE_FONT);

    private enum AnimState { OPENING, OPEN, CLOSING }

    private final DialogueMessage[] queue;
    private int currentIndex = 0;
    private DialogueMessage message;
    private Identifier portraitTexture;

    private List<String> wrappedLines;
    private List<Component[]> lineGlyphs;
    private List<int[]> linePrefixWidths;
    private int totalLength;

    private int ticks;
    private int typingTicks;
    private int revealedChars;

    private AnimState animState = AnimState.OPENING;
    private int animTicks = 0;

    private final int ralseiEntityId;

    public RalseiDialogueScreen(int ralseiEntityId, DialogueMessage[] messages) {
        super(Component.literal("Ralsei"));
        this.ralseiEntityId = ralseiEntityId;
        this.queue = messages;
    }

    @Override
    public void onClose() {
        super.onClose();
        if (Minecraft.getInstance().level != null
                && Minecraft.getInstance().level.getEntity(ralseiEntityId) instanceof RalseiEntity ralsei) {
            ralsei.clientTalkOverride = null;
        }
        ClientPlayNetworking.send(new DialogueClosedPayload(ralseiEntityId));
    }

    @Override
    protected void init() {
        super.init();
        loadCurrentMessage();
        animState = AnimState.OPENING;
        animTicks = 0;
    }

    private void loadCurrentMessage() {
        message = queue[currentIndex];
        boolean showAsterisk = message.alignment() == TextAlignment.LEFT;
        asteriskGlyph = showAsterisk ? Component.literal("*").setStyle(GLYPH_STYLE) : null;
        asteriskIndentLocal = showAsterisk ? this.font.width(asteriskGlyph) + ASTERISK_GAP : 0;
        portraitTexture = Deltarune.id("textures/gui/portrait/ralsei/" + message.portrait() + ".png");

        if (Minecraft.getInstance().level != null
                && Minecraft.getInstance().level.getEntity(ralseiEntityId) instanceof RalseiEntity ralsei) {
            ralsei.clientTalkOverride = message.talkOverride();
        }

        int scaledPortraitWidth = Math.round(PORTRAIT_WIDTH * PORTRAIT_SCALE);
        int textX = PADDING + scaledPortraitWidth + PADDING;
        float effectiveTextScale = TEXT_SCALE * message.size().multiplier;
        int wrapWidth = (int) ((BOX_TEXTURE_WIDTH - textX - PADDING) / effectiveTextScale) - asteriskIndentLocal;

        wrappedLines = wrapText(message.text(), wrapWidth);

        lineGlyphs = new ArrayList<>(wrappedLines.size());
        linePrefixWidths = new ArrayList<>(wrappedLines.size());
        int total = 0;
        for (String line : wrappedLines) {
            Component[] glyphs = new Component[line.length()];
            int[] prefixWidths = new int[line.length() + 1];
            for (int i = 0; i < line.length(); i++) {
                Component glyph = Component.literal(String.valueOf(line.charAt(i))).setStyle(GLYPH_STYLE);
                glyphs[i] = glyph;

                int advance = this.font.width(glyph);
                if (line.charAt(i) == ' ') {
                    advance += SPACE_WIDTH_BONUS;
                } else if (line.charAt(i) == '.') {
                    advance += PERIOD_WIDTH_BONUS;
                } else {
                    advance += CHAR_SPACING_ADJUST;
                }
                prefixWidths[i + 1] = prefixWidths[i] + advance;
            }
            lineGlyphs.add(glyphs);
            linePrefixWidths.add(prefixWidths);
            total += line.length();
        }
        totalLength = total;

        ticks = 0;
        typingTicks = 0;
        revealedChars = 0;
    }

    private List<String> wrapText(String text, int maxWidth) {
        List<String> lines = new ArrayList<>();
        StringBuilder currentLine = new StringBuilder();

        for (String word : text.split(" ")) {
            String candidate = currentLine.isEmpty() ? word : currentLine + " " + word;
            if (this.font.width(candidate) > maxWidth && !currentLine.isEmpty()) {
                lines.add(currentLine.toString());
                currentLine = new StringBuilder(word);
            } else {
                currentLine = new StringBuilder(candidate);
            }
        }
        if (!currentLine.isEmpty()) {
            lines.add(currentLine.toString());
        }
        return lines;
    }

    @Override
    public void tick() {
        super.tick();
        ticks++;

        if (animState == AnimState.OPENING) {
            animTicks++;
            if (animTicks >= FADE_DURATION_TICKS) {
                animState = AnimState.OPEN;
            }
        } else if (animState == AnimState.CLOSING) {
            animTicks++;
            if (animTicks >= FADE_DURATION_TICKS) {
                this.onClose();
                return;
            }
        }

        if (animState == AnimState.OPEN) {
            typingTicks++;
            int targetRevealed = Math.min(typingTicks / message.typingSpeed().ticksPerChar, totalLength);
            if (targetRevealed > revealedChars) {
                revealedChars = targetRevealed;
                if (charAtGlobalIndex(revealedChars - 1) != ' ') {
                    float pitch = 0.858f + (float) Math.random() * 0.366f;
                    Minecraft.getInstance().getSoundManager().play(
                            SimpleSoundInstance.forUI(ModSounds.RALSEI_TALK_BLIP, pitch)
                    );
                }
            }
        }
    }

    private char charAtGlobalIndex(int index) {
        int remaining = index;
        for (String line : wrappedLines) {
            if (remaining < line.length()) return line.charAt(remaining);
            remaining -= line.length();
        }
        return ' ';
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() != 0) {
            return super.mouseClicked(event, doubleClick);
        }

        if (animState != AnimState.OPEN) {
            return true;
        }

        if (revealedChars < totalLength) {
            revealedChars = totalLength;
        } else if (currentIndex < queue.length - 1) {
            currentIndex++;
            loadCurrentMessage();
        } else {
            animState = AnimState.CLOSING;
            animTicks = 0;
        }
        return true;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
    }

    private record CharOffset(int x, int y) {
        static final CharOffset ZERO = new CharOffset(0, 0);
    }

    private static CharOffset animationOffset(TextAnimation animation, int charIndex, int ticks) {
        return switch (animation) {
            case STILL -> CharOffset.ZERO;
            case SHAKE -> {
                double phase = (ticks * 1.547) + (charIndex * 1.2635);
                yield new CharOffset(0, (Math.sin(phase) > 0) ? 1 : 0);
            }
            case WAVE -> {
                double phase = (ticks * 0.334) - (charIndex * 0.565);
                yield new CharOffset(0, (int) Math.round(Math.sin(phase) * 1.2556));
            }
            case SCARED -> {
                long base = (long) charIndex * 0x9E3779B97F4A7C15L + (long) ticks * 0x2545F4914F6CDD1DL;
                long activeBits = mix64(base);
                float activeRoll = (activeBits >>> 40) / (float) (1L << 24);
                if (activeRoll >= SCARED_ACTIVE_CHANCE) {
                    yield CharOffset.ZERO;
                }
                long jitterBits = mix64(base ^ 0xA5A5A5A5A5A5A5A5L);
                int x = Math.floorMod(jitterBits, 3) - 1;
                int y = Math.floorMod(jitterBits >>> 8, 3) - 1;
                yield new CharOffset(x, y);
            }
        };
    }

    private float visibility() {
        return switch (animState) {
            case OPENING -> Mth.clamp(animTicks / (float) FADE_DURATION_TICKS, 0f, 1f);
            case OPEN -> 1f;
            case CLOSING -> 1f - Mth.clamp(animTicks / (float) FADE_DURATION_TICKS, 0f, 1f);
        };
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);

        float visibility = visibility();
        int slideOffset = (int) ((1f - visibility) * SLIDE_DISTANCE);
        int alpha = (int) (255 * visibility);
        int tint = (alpha << 24) | 0xFFFFFF;

        int scaledBoxWidth = (int) (BOX_TEXTURE_WIDTH * UI_SCALE);
        int scaledBoxHeight = (int) (BOX_TEXTURE_HEIGHT * UI_SCALE);
        int boxX = (this.width - scaledBoxWidth) / 2;
        int boxY = this.height - scaledBoxHeight - BOTTOM_MARGIN + slideOffset;

        graphics.pose().pushMatrix();
        graphics.pose().translate(boxX, boxY);
        graphics.pose().scale(UI_SCALE, UI_SCALE);

        graphics.blit(RenderPipelines.GUI_TEXTURED, BOX_TEXTURE, 0, 0, 0f, 0f,
                BOX_TEXTURE_WIDTH, BOX_TEXTURE_HEIGHT, BOX_TEXTURE_WIDTH, BOX_TEXTURE_HEIGHT, tint);

        int scaledPortraitWidth = Math.round(PORTRAIT_WIDTH * PORTRAIT_SCALE);
        int portraitX = PADDING;
        int portraitY = PADDING;

        graphics.pose().pushMatrix();
        graphics.pose().translate(portraitX, portraitY);
        graphics.pose().scale(PORTRAIT_SCALE, PORTRAIT_SCALE);
        graphics.blit(RenderPipelines.GUI_TEXTURED, portraitTexture, 0, 0, 0f, 0f,
                PORTRAIT_WIDTH, PORTRAIT_HEIGHT, PORTRAIT_WIDTH, PORTRAIT_HEIGHT, tint);
        graphics.pose().popMatrix();

        int textX = portraitX + scaledPortraitWidth + PADDING;
        float effectiveTextScale = TEXT_SCALE * message.size().multiplier;

        int textY = message.alignment() == TextAlignment.CENTERED
                ? PADDING + (CONTENT_HEIGHT - (int) (wrappedLines.size() * LINE_STEP * effectiveTextScale)) / 2
                : PADDING;

        graphics.pose().pushMatrix();
        graphics.pose().translate(textX, textY);
        graphics.pose().scale(effectiveTextScale, effectiveTextScale);

        int remaining = revealedChars;
        int lineY = 0;
        int globalIndex = 0;
        int availableWidth = (int) ((BOX_TEXTURE_WIDTH - textX - PADDING) / effectiveTextScale);

        if (asteriskGlyph != null) {
            graphics.text(this.font, asteriskGlyph, 0, 0, tint, false);
        }

        for (int lineIndex = 0; lineIndex < wrappedLines.size(); lineIndex++) {
            String line = wrappedLines.get(lineIndex);
            Component[] glyphs = lineGlyphs.get(lineIndex);
            int[] prefixWidths = linePrefixWidths.get(lineIndex);

            int visibleInLine = Math.max(0, Math.min(remaining, line.length()));

            int cursorX = message.alignment() == TextAlignment.CENTERED
                    ? Math.max(0, (availableWidth - prefixWidths[visibleInLine]) / 2)
                    : asteriskIndentLocal;

            for (int i = 0; i < visibleInLine; i++) {
                CharOffset offset = animationOffset(message.animation(), globalIndex, ticks);
                if (offset.x() != 0 || offset.y() != 0) {
                    graphics.pose().pushMatrix();
                    graphics.pose().translate(offset.x() * SCARED_OFFSET_SCALE, offset.y() * SCARED_OFFSET_SCALE);
                    graphics.text(this.font, glyphs[i], cursorX, lineY, tint, false);
                    graphics.pose().popMatrix();
                } else {
                    graphics.text(this.font, glyphs[i], cursorX, lineY, tint, false);
                }
                cursorX += prefixWidths[i + 1] - prefixWidths[i];
                globalIndex++;
            }

            remaining -= line.length();
            globalIndex += line.length() - visibleInLine;
            lineY += LINE_STEP;
        }

        graphics.pose().popMatrix();
        graphics.pose().popMatrix();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}