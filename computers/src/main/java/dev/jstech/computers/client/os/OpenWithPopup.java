/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.os;

import dev.jstech.computers.gui.layout.OpenWithLayout;
import dev.jstech.computers.os.fs.FsPaths;
import dev.jstech.core.client.gui.component.Button;
import dev.jstech.core.client.gui.component.Label;
import dev.jstech.core.client.gui.component.ListView;
import dev.jstech.core.client.gui.component.Popup;
import dev.jstech.core.client.gui.component.Texts;
import dev.jstech.core.client.gui.component.UiComponent;
import dev.jstech.core.client.gui.component.UiContext;
import java.util.List;
import net.minecraft.Util;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import org.lwjgl.glfw.GLFW;

/**
 * The Open with chooser: which program opens a file, asked over the whole desktop.
 *
 * <p>A double-click on a file of a kind nothing on the computer opens brings it up, and so does Choose another
 * program on any file. It lists the programs on the computer that can open the file, with their icons in the
 * desktop's own style, the first one picked. Only this time opens the file in the picked program and remembers
 * nothing; Always does the same and makes that program the one for every file with the same extension on this
 * computer. Escape closes it without opening anything; a click outside it does nothing, as with the error dialog.
 */
public final class OpenWithPopup extends Popup {

    /** How far apart two clicks on the same program may be to count as a double-click, in milliseconds. */
    private static final long DOUBLE_CLICK_MS = 400L;
    /** The longest extension an Always can be written down for. */
    private static final int MAX_EXTENSION = 32;

    /** What the player chose: a program, and whether it opens every file with that extension from now on. */
    @FunctionalInterface
    public interface IChoice {
        void chose(String programId, boolean always);
    }

    private final List<String> programs;
    private final String iconSet;
    private final IChoice onChoice;
    private final boolean canRemember;
    private final Label question;
    private final Label note;
    private final Well well;
    private final ListView<String> list;
    private final Button once;
    private final Button always;
    private int lastClicked = -1;
    private long lastClickAt;

    /**
     * @param path      the file to open
     * @param extension its extension, empty when it has none (Always then has nothing to keep the choice under)
     * @param programs  the programs that can open it, best first; not empty
     * @param opener    the name of the program that opens such a file now, empty when nothing does
     * @param iconSet   the desktop's icon style, for the programs' icons
     */
    public OpenWithPopup(final String path, final String extension, final List<String> programs, final String opener,
                         final String iconSet, final Font font, final IChoice onChoice) {
        super("Open with", OpenWithLayout.WIDTH, OpenWithLayout.HEIGHT);
        this.programs = List.copyOf(programs);
        this.iconSet = iconSet;
        this.onChoice = onChoice;
        this.canRemember = !extension.isEmpty() && extension.length() <= MAX_EXTENSION;
        final int textW = OpenWithLayout.listW();
        final String name = FsPaths.fileName(path);
        this.question = add(new Label(fit("How do you want to open ", name, "?", font, textW)));
        final int noteUnits = Texts.smallFits(OpenWithLayout.noteW());
        this.note = add(new Label(Texts.clip(font, noteFor(extension, opener, font, noteUnits), noteUnits),
                Label.Tone.DIM).setScale(Texts.SMALL));
        this.well = add(new Well());
        this.list = add(new ListView<String>(() -> this.programs, OpenWithLayout.ROW_H, this::drawRow)
                .setOnClick(this::clicked));
        this.list.setSelected(0);
        this.once = add(new Button("Only this time", () -> choose(false)).setPrimary(true));
        this.always = add(new Button("Always", () -> choose(true)));
        this.always.setEnabled(this.canRemember);
        setDim(0x80000000);
        setCloseOnOutsideClick(false);
        setLayouter(this::layoutContent);
        open();
    }

    /** The programs offered, best first. */
    public List<String> programs() {
        return programs;
    }

    /** The question at the top, as the player reads it. */
    public String question() {
        return question.text();
    }

    /** The line under the question, as the player reads it. */
    public String note() {
        return note.text();
    }

    /** Picks the program at {@code index}, as a click on it does. */
    public void pick(final int index) {
        if (index >= 0 && index < programs.size()) {
            list.setSelected(index);
        }
    }

    /** Presses Only this time. */
    public void onlyThisTime() {
        choose(false);
    }

    /** Presses Always. */
    public void always() {
        choose(true);
    }

    private void layoutContent(final Popup p) {
        final int left = p.x() + OpenWithLayout.PAD;
        final int top = p.y();
        final int width = OpenWithLayout.listW();
        final int inset = OpenWithLayout.WELL_INSET;
        question.setBounds(left, top + OpenWithLayout.QUESTION_Y, width, OpenWithLayout.LINE_H);
        note.setBounds(left, top + OpenWithLayout.NOTE_Y, OpenWithLayout.noteW(), OpenWithLayout.LINE_H);
        well.setBounds(left, top + OpenWithLayout.LIST_Y, width, OpenWithLayout.LIST_H);
        list.setBounds(left + inset, top + OpenWithLayout.LIST_Y + inset, width - inset * 2,
                OpenWithLayout.LIST_H - inset * 2);
        once.setBounds(p.x() + OpenWithLayout.onceX(), top + OpenWithLayout.BUTTON_Y, OpenWithLayout.ONCE_W,
                OpenWithLayout.BUTTON_H);
        always.setBounds(p.x() + OpenWithLayout.alwaysX(), top + OpenWithLayout.BUTTON_Y, OpenWithLayout.ALWAYS_W,
                OpenWithLayout.BUTTON_H);
    }

    private void drawRow(final GuiGraphics g, final UiContext ctx, final String programId, final int index,
                         final int x, final int y, final int w, final int h, final boolean hovered,
                         final boolean selected) {
        int color = ctx.skin().text();
        if (ctx.skin() instanceof OsSkin skin) {
            skin.listRow(g, x, y, w, h, hovered, selected);
            color = skin.listRowText(selected);
        }
        ProgramIcons.draw(g, x + OpenWithLayout.ICON_X, y + (h - ProgramIcons.SIZE) / 2, ProgramIcons.SIZE,
                ProgramIcons.SIZE, ResourceLocation.fromNamespaceAndPath("jsc", programId), iconSet);
        g.drawString(ctx.font(), DesktopScreen.openerName(programId), x + OpenWithLayout.ROW_TEXT_X,
                y + (h - OpenWithLayout.LINE_H) / 2, color, false);
    }

    /** A click picks a program; a second click on the same one soon after is the same as Only this time. */
    private void clicked(final int index, final int button, final double mx, final double my) {
        if (button != 0 || index < 0) {
            return;
        }
        final long now = Util.getMillis();
        list.setSelected(index);
        if (index == lastClicked && now - lastClickAt <= DOUBLE_CLICK_MS) {
            choose(false);
            return;
        }
        lastClicked = index;
        lastClickAt = now;
    }

    @Override
    public boolean keyPressed(final int key, final int scanCode, final int modifiers) {
        if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) {
            choose(false);
            return true;
        }
        if (key == GLFW.GLFW_KEY_DOWN || key == GLFW.GLFW_KEY_UP) {
            final int picked = Math.max(0, Math.min(programs.size() - 1,
                    list.selected() + (key == GLFW.GLFW_KEY_DOWN ? 1 : -1)));
            list.setSelected(picked);
            if (picked < list.scroll()) {
                list.setScroll(picked);
            } else if (picked >= list.scroll() + OpenWithLayout.ROWS) {
                list.setScroll(picked - OpenWithLayout.ROWS + 1);
            }
            return true;
        }
        return super.keyPressed(key, scanCode, modifiers);
    }

    private void choose(final boolean forGood) {
        final int index = list.selected();
        if (index < 0 || index >= programs.size() || (forGood && !canRemember)) {
            return;
        }
        close();
        onChoice.chose(programs.get(index), forGood);
    }

    /**
     * The line under the question: the program that opens such a file now, or that nothing does yet. A long
     * extension is cut short with dots, so the sentence still ends the way it should.
     */
    private static String noteFor(final String extension, final String opener, final Font font, final int width) {
        if (opener.isEmpty()) {
            return extension.isEmpty() ? "Nothing on this computer opens this file yet."
                    : fit("Nothing on this computer opens .", extension, " files yet.", font, width);
        }
        return extension.isEmpty() ? "This file opens in " + opener + "."
                : fit(".", extension, " files open in " + opener + ".", font, width);
    }

    /** {@code before + name + after}, the name cut short with dots when the whole would be wider than {@code width}. */
    private static String fit(final String before, final String name, final String after, final Font font,
                              final int width) {
        String shown = name;
        while (!shown.isEmpty() && font.width(before + shown + after) > width) {
            shown = shown.length() <= 3 ? "" : shown.substring(0, shown.length() - 4) + "...";
        }
        return before + shown + after;
    }

    /** The sunken well the programs are listed in, drawn the desktop's way. */
    private static final class Well extends UiComponent {

        @Override
        public void render(final GuiGraphics g, final UiContext ctx) {
            if (ctx.skin() instanceof OsSkin skin) {
                skin.field(g, x(), y(), width(), height(), false);
            }
        }
    }
}
