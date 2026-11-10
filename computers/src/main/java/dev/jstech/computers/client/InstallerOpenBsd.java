/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client;

import dev.jstech.computers.JsComputers;
import dev.jstech.computers.gui.layout.CrashLayout;
import dev.jstech.computers.gui.layout.InstallerLayout;
import dev.jstech.computers.operation.payload.InstallerActionPayload;
import dev.jstech.computers.os.install.InstallerFlow;
import dev.jstech.computers.os.install.InstallerPage;
import dev.jstech.computers.os.install.OpenBsdInstall;
import dev.jstech.core.palette.Palette;
import dev.jstech.core.palette.PaletteHolder;
import dev.jstech.core.palette.Palettes;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.Text;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.IntSupplier;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import org.lwjgl.glfw.GLFW;

/**
 * OpenBSD's installer on the glass: its terminal, what it printed so far and the question waiting, with what is being
 * typed after it (computers/docs/design/operating-systems.md). Enter gives the answer, and the machine is told what it
 * means; an answer it does not take is echoed and the question asked again, as the real one does.
 */
@PaletteHolder
final class InstallerOpenBsd implements IOwnInstallerPage {

    private final Consumer<InstallerActionPayload> send;
    private final BlockPos host;
    private final BlockPos monitor;
    /* The machine's memory as its firmware last described it, which the layout's swap is the size of. */
    private final IntSupplier memoryMb;
    /* What was typed on this page and refused, and what the installer said back, a line each. */
    private final List<OpenBsdInstall.Line> echoes = new ArrayList<>();
    private InstallerFlow flow;
    private OpenBsdInstall.Answers answers;
    private String typing = "";

    /** The terminal's ground, its text and the bright it prints answers and its one loud line in. */
    private static final Palette<Colours> PALETTE = Palettes.declare(JsComputers.MODID, "installer/openbsd",
            new Colours(0xFF000000, 0xFFC8C8C8, 0xFFFFFFFF));
    /*
     * Where each column of the installer's tables goes, from the left of the terminal; a negative stop right-aligns
     * its cell there: the layout's five, the sets' three and a set's bar's five.
     */
    private static final int[] LAYOUT = {0, -65, -115, 125, 160};
    private static final int[] SETS = {14, 75, 145};
    private static final int[] BARS = {0, -115, 120, -260, 267};
    /** The longest answer the terminal takes, longer than a machine's name may be. */
    private static final int MOST_TYPED = InstallerFlow.MOST_NAME_LETTERS;
    private static final int CARET_TICKS = 10;

    InstallerOpenBsd(final InstallerFlow flow, final BlockPos host, final BlockPos monitor,
                     final Consumer<InstallerActionPayload> send, final IntSupplier memoryMb) {
        this.send = send;
        this.host = host;
        this.monitor = monitor;
        this.memoryMb = memoryMb;
        this.accept(flow, false);
    }

    @Override
    public void accept(final InstallerFlow given, final boolean samePage) {
        this.flow = given;
        if (!samePage) {
            this.answers = OpenBsdInstall.Answers.fresh(given);
            this.echoes.clear();
            // The machine's name is offered where it is asked, as the field the player types it in.
            this.typing = given.page() == InstallerPage.NAME ? given.computerName() : "";
        }
    }

    @Override
    public void paint(final GuiGraphics g, final Font font, final int x, final int y, final int ticksDone,
                      final int blink, final int mouseX, final int mouseY) {
        final Colours c = PALETTE.get();
        g.fill(x, y, x + InstallerLayout.WIDTH, y + InstallerLayout.HEIGHT, c.ground());
        final List<OpenBsdInstall.Line> lines = OpenBsdInstall.lines(this.flow, this.answers, this.echoes, ticksDone,
                this.memoryMb.getAsInt());
        final int rows = (InstallerLayout.HEIGHT - 2 * CrashLayout.CONSOLE_MARGIN) / TextWall.ROW;
        final int first = Math.max(0, lines.size() - rows);
        int top = y + CrashLayout.CONSOLE_MARGIN;
        final int left = x + CrashLayout.CONSOLE_MARGIN;
        for (int i = first; i < lines.size(); i++) {
            final OpenBsdInstall.Line line = lines.get(i);
            if (line.table() != OpenBsdInstall.Table.NONE) {
                columns(g, font, line, left, top, c.text());
            } else {
                final String said = GameText.resolve(line.parts().getFirst());
                TextWall.draw(g, font, said, left, top, line.loud() ? c.bright() : c.text());
                final String typed = line.open() ? this.typing + ((blink / CARET_TICKS) % 2 == 0 ? "_" : "")
                        : line.typed();
                if (!typed.isEmpty()) {
                    final int after = said.isEmpty() ? 0 : TextWall.width(font, said + " ");
                    TextWall.draw(g, font, typed, left + after, top, c.bright());
                }
            }
            top += TextWall.ROW;
        }
    }

    @Override
    public boolean key(final int key) {
        if (key == GLFW.GLFW_KEY_BACKSPACE) {
            if (!this.typing.isEmpty()) {
                this.typing = this.typing.substring(0, this.typing.length() - 1);
            }
            return true;
        }
        if (key != GLFW.GLFW_KEY_ENTER && key != GLFW.GLFW_KEY_KP_ENTER) {
            // Every other key is the terminal's, so none of them reaches the game's own bindings.
            return true;
        }
        final OpenBsdInstall.Step step = OpenBsdInstall.enter(this.flow, this.answers, this.typing);
        if (step.refused()) {
            this.echoes.add(OpenBsdInstall.refused(this.flow, this.answers, this.typing));
            if (!step.said().isEmpty()) {
                this.echoes.add(OpenBsdInstall.said(step.said()));
            }
        }
        this.answers = step.answers();
        this.typing = "";
        for (final OpenBsdInstall.Action action : step.actions()) {
            this.send.accept(payloadOf(action));
        }
        return true;
    }

    @Override
    public boolean typed(final char letter) {
        if (letter >= ' ' && letter != 127 && this.typing.length() < MOST_TYPED) {
            this.typing += letter;
        }
        return true;
    }

    @Override
    public boolean click(final double mouseX, final double mouseY) {
        return false;
    }

    /* The installer's own payload for one of the actions Enter decided on. */
    private InstallerActionPayload payloadOf(final OpenBsdInstall.Action action) {
        if (action.kind() == OpenBsdInstall.Action.NAME) {
            return InstallerActionPayload.named(this.host, this.monitor, action.text());
        }
        final int code = switch (action.kind()) {
            case OpenBsdInstall.Action.SSHD -> InstallerActionPayload.ACTION_SSHD;
            case OpenBsdInstall.Action.OPTIONS -> InstallerActionPayload.ACTION_OPTIONS;
            case OpenBsdInstall.Action.SELECT_DISK -> InstallerActionPayload.ACTION_SELECT_DISK;
            case OpenBsdInstall.Action.ERASE -> InstallerActionPayload.ACTION_ERASE;
            case OpenBsdInstall.Action.CANCEL_ERASE -> InstallerActionPayload.ACTION_CANCEL_ERASE;
            case OpenBsdInstall.Action.MIRROR -> InstallerActionPayload.ACTION_MIRROR;
            case OpenBsdInstall.Action.REBOOT -> InstallerActionPayload.ACTION_REBOOT;
            case OpenBsdInstall.Action.HALT -> InstallerActionPayload.ACTION_HALT;
            default -> InstallerActionPayload.ACTION_NEXT;
        };
        return InstallerActionPayload.of(this.host, this.monitor, code, action.value());
    }

    /* A row of one of the installer's tables, each cell at its column's stop. */
    private static void columns(final GuiGraphics g, final Font font, final OpenBsdInstall.Line line, final int left,
                                final int top, final int colour) {
        final int[] stops = switch (line.table()) {
            case LAYOUT -> LAYOUT;
            case SETS -> SETS;
            default -> BARS;
        };
        final List<Text> parts = line.parts();
        for (int i = 0; i < parts.size() && i < stops.length; i++) {
            final String cell = GameText.resolve(parts.get(i));
            if (cell.isEmpty()) {
                continue;
            }
            final int stop = stops[i];
            TextWall.draw(g, font, cell, left + (stop < 0 ? -stop - TextWall.width(font, cell) : stop), top, colour);
        }
    }

    /** The terminal's colours, as the palette above names them. */
    private record Colours(int ground, int text, int bright) {
    }
}
