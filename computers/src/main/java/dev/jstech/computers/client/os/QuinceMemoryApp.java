/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.os;

import dev.jstech.computers.operation.payload.crash.ForceQuitPayload;
import dev.jstech.computers.operation.payload.crash.MemoryAskPayload;
import dev.jstech.core.client.gui.component.Draw;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * The question Quince OS X and quinceOS ask when the machine runs out of memory for its programs
 * (computers/docs/design/quince.md): every program is paused, and the player chooses which to force to quit; nothing
 * ends without the player, and the window goes once the machine has room again.
 */
@TextHolder
final class QuinceMemoryApp implements IDesktopApp {

    /** What the question's window is known by. */
    static final String KEY = "quince_memory";

    private final DesktopState desktop;
    /** The row chosen in the list. */
    private int chosen;
    /* Where the list and the button were last drawn, so a click lands on what it was drawn under. */
    private int listX;
    private int listY;
    private int listW;
    private int[] button = new int[4];

    private static final TextKey TITLE = TextKey.of("jsc.quince.memory.title", "Force Quit Applications");
    private static final TextKey RUN_OUT = TextKey.of("jsc.quince.memory.run_out",
            "Your system has run out of application memory.");
    private static final TextKey QUIT_SOME = TextKey.of("jsc.quince.memory.quit_some",
            "To avoid problems with your computer, quit any applications you are not using. They are paused until"
                    + " you choose.");
    private static final TextKey PAUSED = TextKey.of("jsc.quince.memory.paused", "(Paused)");
    private static final TextKey FORCE_QUIT = TextKey.of("jsc.quince.memory.force_quit", "Force Quit");
    private static final int ROW = 14;
    private static final int LIST_H = 72;

    QuinceMemoryApp(final DesktopState desktop) {
        this.desktop = desktop;
    }

    @Override
    public String title() {
        return GameText.resolve(TITLE);
    }

    @Override
    public int defaultWidth() {
        return 292;
    }

    @Override
    public int defaultHeight() {
        return 190;
    }

    @Override
    public void renderContent(final GuiGraphics g, final Font font, final int x, final int y, final int width,
                              final int height, final int mouseX, final int mouseY, final float partialTick) {
        final QuinceLook look = this.desktop.quinceShell().look();
        final QuinceLook.Colours c = look.colours();
        QuinceArt.sprite(g, QuinceArt.CAUTION_YELLOW, x + 6, y + 6, c.ink());
        int ty = y + 4;
        for (final String line : wrap(font, GameText.resolve(RUN_OUT), width - 40)) {
            Draw.text(g, font, Component.literal(line).withStyle(ChatFormatting.BOLD), x + 28, ty, c.ink());
            ty += 10;
        }
        ty += 4;
        for (final String line : wrap(font, GameText.resolve(QUIT_SOME), width - 36)) {
            Draw.text(g, font, line, x + 28, ty, c.ink());
            ty += 10;
        }
        final List<MemoryAskPayload.Asked> asked = this.desktop.memoryAsk();
        this.chosen = Math.max(0, Math.min(this.chosen, asked.size() - 1));
        this.listX = x + 6;
        this.listY = ty + 4;
        this.listW = width - 12;
        g.fill(this.listX, this.listY, this.listX + this.listW, this.listY + LIST_H, c.light());
        ChromeShapes.outline(g, this.listX, this.listY, this.listW, LIST_H, c.shade());
        for (int k = 0; k < asked.size() && (k + 1) * ROW <= LIST_H; k++) {
            final MemoryAskPayload.Asked one = asked.get(k);
            final int ry = this.listY + 3 + k * ROW;
            final boolean picked = k == this.chosen;
            if (picked) {
                g.fill(this.listX + 1, ry - 2, this.listX + this.listW - 1, ry + 11, c.select());
            }
            final int ink = picked ? c.selectInk() : c.ink();
            Draw.text(g, font, one.name(), this.listX + 6, ry, ink);
            Draw.text(g, font, GameText.resolve(PAUSED), this.listX + 10 + font.width(one.name()), ry,
                    picked ? c.selectInk() : QuinceArt.colours().red());
            final String size = one.mb() >= 1024 ? String.format(Locale.ROOT, "%.2f GB", one.mb() / 1024.0)
                    : one.mb() + " MB";
            Draw.text(g, font, size, this.listX + this.listW - 6 - font.width(size), ry, ink);
        }
        final String label = GameText.resolve(FORCE_QUIT);
        final int bw = font.width(label) + 18;
        this.button = new int[] {x + width - bw - 6, y + height - 16, bw, 12};
        QuinceArt.button(g, look, this.button[0], this.button[1], bw, 12, true, false);
        Draw.text(g, font, label, this.button[0] + 9, this.button[1] + 2, QuinceArt.buttonInk(look, true));
    }

    @Override
    public void mouseClicked(final DesktopWindow window, final double mouseX, final double mouseY, final int button) {
        if (mouseX >= this.listX && mouseX < this.listX + this.listW && mouseY >= this.listY
                && mouseY < this.listY + LIST_H) {
            this.chosen = (int) ((mouseY - this.listY - 1) / ROW);
            return;
        }
        if (mouseX >= this.button[0] && mouseX < this.button[0] + this.button[2] && mouseY >= this.button[1]
                && mouseY < this.button[1] + this.button[3]) {
            forceQuit();
        }
    }

    /** Ends the chosen program; the machine puts the question away once that leaves enough room. */
    private void forceQuit() {
        final List<MemoryAskPayload.Asked> asked = this.desktop.memoryAsk();
        if (this.chosen >= 0 && this.chosen < asked.size()) {
            PacketDistributor.sendToServer(new ForceQuitPayload(this.desktop.host(),
                    List.of(asked.get(this.chosen).token())));
        }
    }

    private static List<String> wrap(final Font font, final String text, final int width) {
        final List<String> lines = new ArrayList<>();
        String line = "";
        for (final String word : text.split(" ")) {
            final String probe = line.isEmpty() ? word : line + " " + word;
            if (font.width(probe) <= width || line.isEmpty()) {
                line = probe;
            } else {
                lines.add(line);
                line = word;
            }
        }
        if (!line.isEmpty()) {
            lines.add(line);
        }
        return lines;
    }
}
