/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.os;

import dev.jstech.computers.os.MachineMemory;
import dev.jstech.computers.os.ProgramSpec;
import dev.jstech.computers.os.QuincePartitions;
import dev.jstech.computers.os.WindowKeys;
import dev.jstech.core.client.gui.component.Draw;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import java.util.Locale;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import org.lwjgl.glfw.GLFW;

/**
 * A program's Get Info on Quince's classic systems (computers/docs/design/quince.md): what the program is, and the
 * memory block it opens in, which is set here. The Quince System has one size; Quince OS 9 a minimum and a preferred
 * size beside the size the maker suggests. A size is typed in K, the unit those systems counted memory in, and kept in
 * whole megabytes.
 */
@TextHolder
final class QuinceInfoApp implements IDesktopApp {

    /** What a Get Info window is known by, ahead of its program's id path. */
    static final String KEY = "quince_info:";

    private final DesktopState desktop;
    private final ProgramSpec spec;
    /** Whether this is Quince OS 9's, with its minimum and preferred sizes, or the Quince System's one size. */
    private final boolean nine;
    /** What is typed in the minimum and the preferred fields, in K. */
    private final StringBuilder minimum = new StringBuilder();
    private final StringBuilder preferred = new StringBuilder();
    /** The field being typed in: 0 the minimum, 1 the preferred (the only one on the Quince System). */
    private int focus = 1;
    /* Where the window's body was last drawn, so a click lands on the field it was drawn under. */
    private int bodyX;
    private int bodyY;

    private static final TextKey TITLE = TextKey.of("jsc.quince.info.title", "%s Info");
    private static final TextKey KIND = TextKey.of("jsc.quince.info.kind", "Kind:");
    private static final TextKey APPLICATION = TextKey.of("jsc.quince.info.application", "application program");
    private static final TextKey WHERE = TextKey.of("jsc.quince.info.where", "Where:");
    private static final TextKey MEMORY = TextKey.of("jsc.quince.info.memory", "Memory size (K):");
    private static final TextKey ONE_SIZE = TextKey.of("jsc.quince.info.one_size",
            "The program gets this much memory, no more, while it runs.");
    private static final TextKey REQUIREMENTS = TextKey.of("jsc.quince.info.requirements", "Memory Requirements");
    private static final TextKey SUGGESTED = TextKey.of("jsc.quince.info.suggested", "Suggested Size:");
    private static final TextKey MINIMUM = TextKey.of("jsc.quince.info.minimum", "Minimum Size:");
    private static final TextKey PREFERRED = TextKey.of("jsc.quince.info.preferred", "Preferred Size:");
    private static final TextKey OPENS = TextKey.of("jsc.quince.info.opens",
            "The program opens in a block of the preferred size if that much is free.");
    private static final int ROW = 16;
    private static final int FIELD_X = 118;
    private static final int FIELD_W = 58;
    private static final int K = 1024;

    QuinceInfoApp(final DesktopState desktop, final ProgramSpec spec) {
        this.desktop = desktop;
        this.spec = spec;
        this.nine = desktop.quinceShell().look() == QuinceLook.PLATINUM;
        final QuincePartitions.Partition block = partition();
        this.minimum.append(block.minimumMb() * K);
        this.preferred.append(block.preferredMb() * K);
    }

    @Override
    public String title() {
        return GameText.resolve(TITLE.with(name()));
    }

    @Override
    public int defaultWidth() {
        return 230;
    }

    @Override
    public int defaultHeight() {
        return this.nine ? 180 : 150;
    }

    @Override
    public ResourceLocation iconId() {
        return this.spec.id();
    }

    @Override
    public void renderContent(final GuiGraphics g, final Font font, final int x, final int y, final int width,
                              final int height, final int mouseX, final int mouseY, final float partialTick) {
        this.bodyX = x;
        this.bodyY = y;
        final QuinceLook look = this.desktop.quinceShell().look();
        final QuinceLook.Colours c = look.colours();
        ProgramIcons.draw(g, x + 8, y + 4, 16, 16, this.spec.id(), this.desktop.icons());
        Draw.text(g, font, name(), x + 30, y + 8, c.ink());
        g.fill(x + 4, y + 24, x + width - 4, y + 25, c.shade());
        int ty = y + 30;
        Draw.text(g, font, GameText.resolve(KIND) + " " + GameText.resolve(APPLICATION), x + 8, ty, c.ink());
        ty += 11;
        Draw.text(g, font, GameText.resolve(WHERE) + " " + GameText.resolve(DesktopLaunchers.QUINCE_DISK), x + 8, ty,
                c.ink());
        ty += 14;
        g.fill(x + 4, ty - 3, x + width - 4, ty - 2, c.shade());
        if (!this.nine) {
            Draw.text(g, font, GameText.resolve(MEMORY), x + 8, ty + 3, c.ink());
            field(g, font, look, x + FIELD_X - 16, ty, this.preferred, this.focus == 1);
            wrap(g, font, GameText.resolve(ONE_SIZE), x + 8, ty + 20, width - 16, c.ink());
            return;
        }
        Draw.text(g, font, GameText.resolve(REQUIREMENTS), x + 8, ty, c.ink());
        ty += 14;
        row(g, font, x, ty, GameText.resolve(SUGGESTED), thousands(suggestedMb() * K), c.ink());
        ty += ROW;
        Draw.text(g, font, GameText.resolve(MINIMUM), x + FIELD_X - 6 - font.width(GameText.resolve(MINIMUM)),
                ty + 2, c.ink());
        field(g, font, look, x + FIELD_X, ty, this.minimum, this.focus == 0);
        Draw.text(g, font, "K", x + FIELD_X + FIELD_W + 4, ty + 2, c.ink());
        ty += ROW;
        Draw.text(g, font, GameText.resolve(PREFERRED),
                x + FIELD_X - 6 - font.width(GameText.resolve(PREFERRED)), ty + 2, c.ink());
        field(g, font, look, x + FIELD_X, ty, this.preferred, this.focus == 1);
        Draw.text(g, font, "K", x + FIELD_X + FIELD_W + 4, ty + 2, c.ink());
        wrap(g, font, GameText.resolve(OPENS), x + 8, ty + ROW + 4, width - 16, c.ink());
    }

    @Override
    public void mouseClicked(final DesktopWindow window, final double mouseX, final double mouseY, final int button) {
        final int fieldsTop = this.bodyY + 30 + 11 + 14 + (this.nine ? 14 + ROW : 0);
        if (this.nine && mouseY >= fieldsTop && mouseY < fieldsTop + ROW) {
            apply();
            this.focus = 0;
        } else if (mouseY >= fieldsTop + (this.nine ? ROW : 0) && mouseY < fieldsTop + (this.nine ? 2 * ROW : ROW)) {
            apply();
            this.focus = 1;
        }
    }

    @Override
    public boolean charTyped(final char c) {
        final StringBuilder typed = this.focus == 0 ? this.minimum : this.preferred;
        if (Character.isDigit(c) && typed.length() < 7) {
            typed.append(c);
            return true;
        }
        return false;
    }

    @Override
    public boolean keyPressed(final int key, final int scanCode, final int modifiers) {
        final StringBuilder typed = this.focus == 0 ? this.minimum : this.preferred;
        if (key == GLFW.GLFW_KEY_BACKSPACE && typed.length() > 0) {
            typed.setLength(typed.length() - 1);
            return true;
        }
        if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER || key == GLFW.GLFW_KEY_TAB) {
            apply();
            if (key == GLFW.GLFW_KEY_TAB && this.nine) {
                this.focus = 1 - this.focus;
            }
            return true;
        }
        return false;
    }

    /** A Get Info closed keeps what was typed in it, as the real one did. */
    @Override
    public void onClosed() {
        apply();
    }

    /** Keeps the sizes typed, in whole megabytes, the minimum never above the preferred. */
    private void apply() {
        final int preferredMb = megabytes(this.preferred);
        final int minimumMb = this.nine ? megabytes(this.minimum) : preferredMb;
        final QuincePartitions.Partition block = new QuincePartitions.Partition(minimumMb, preferredMb);
        if (!block.equals(partition())) {
            this.desktop.setPartition(this.spec.id().getPath(), block);
        }
        this.minimum.setLength(0);
        this.minimum.append(block.minimumMb() * K);
        this.preferred.setLength(0);
        this.preferred.append(block.preferredMb() * K);
    }

    /** The program's block as the machine keeps it, or the one its maker suggests. */
    private QuincePartitions.Partition partition() {
        return MachineMemory.partitionOf(this.desktop.partitions(), WindowKeys.of(this.spec.id()), suggestedMb());
    }

    private int suggestedMb() {
        return this.spec.ramMbOn(this.desktop.prefs().era(), false);
    }

    private String name() {
        return this.desktop.nameOf(WindowKeys.of(this.spec.id()));
    }

    private static int megabytes(final CharSequence typedK) {
        if (typedK.length() == 0) {
            return QuincePartitions.LEAST_MB;
        }
        final long k = Long.parseLong(typedK.toString());
        return (int) Math.max(QuincePartitions.LEAST_MB, Math.min(QuincePartitions.MOST_MB, (k + K - 1) / K));
    }

    private static void field(final GuiGraphics g, final Font font, final QuinceLook look, final int x, final int y,
                              final CharSequence typed, final boolean focused) {
        final QuinceLook.Colours c = look.colours();
        g.fill(x, y - 1, x + FIELD_W, y + 11, c.light());
        ChromeShapes.outline(g, x, y - 1, FIELD_W, 12, focused ? c.ink() : c.shade());
        final String shown = thousands(typed.length() == 0 ? 0 : Long.parseLong(typed.toString()));
        Draw.text(g, font, shown, x + FIELD_W - 4 - font.width(shown), y + 2, c.ink());
        if (focused) {
            g.fill(x + FIELD_W - 3, y + 1, x + FIELD_W - 2, y + 9, c.ink());
        }
    }

    private static void row(final GuiGraphics g, final Font font, final int x, final int y, final String label,
                            final String value, final int ink) {
        Draw.text(g, font, label, x + FIELD_X - 6 - font.width(label), y + 2, ink);
        Draw.text(g, font, value, x + FIELD_X + FIELD_W - 4 - font.width(value), y + 2, ink);
        Draw.text(g, font, "K", x + FIELD_X + FIELD_W + 4, y + 2, ink);
    }

    private static void wrap(final GuiGraphics g, final Font font, final String text, final int x, final int y,
                             final int width, final int ink) {
        int ty = y;
        String line = "";
        for (final String word : text.split(" ")) {
            final String probe = line.isEmpty() ? word : line + " " + word;
            if (font.width(probe) <= width || line.isEmpty()) {
                line = probe;
            } else {
                Draw.text(g, font, line, x, ty, ink);
                ty += 11;
                line = word;
            }
        }
        Draw.text(g, font, line, x, ty, ink);
    }

    private static String thousands(final long value) {
        return String.format(Locale.ROOT, "%,d", value);
    }
}
