/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.bus;

import dev.jstech.computers.bus.BusScript;
import dev.jstech.computers.bus.BusSettings;
import dev.jstech.computers.gui.layout.BusLayout;
import dev.jstech.computers.menu.AbstractBusMenu;
import dev.jstech.core.client.gui.theme.JsTechTheme;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.TextKey;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The Software tab of a bus's window: the same settings the long way, as software reaches them: the bus's address, the
 * IQL statements and the Σ calls that set it as it is set now, and which programs set what.
 */
final class BusSoftwareView {

    private final AbstractBusMenu menu;
    private final Font font;
    private final List<Section> sections = new ArrayList<>();
    private int contentHeight;
    private int scroll;

    /** The room a line of code has in its box. */
    private static final int CODE_ROOM = BusLayout.ROW_W - 2 * BusLayout.BOX_PAD - 2;
    /** How far a line of code broken in two goes in on its second part. */
    private static final String CONTINUED = "  ";

    BusSoftwareView(final AbstractBusMenu menu, final Font font) {
        this.menu = menu;
        this.font = font;
    }

    /** How tall the window is on this tab, for the bus as it is set now. */
    int windowHeight() {
        layOut();
        return BusLayout.softwareHeight(contentHeight);
    }

    void render(final GuiGraphics g, final int left, final int top) {
        layOut();
        final int viewTop = top + BusLayout.VIEW_Y;
        final int viewH = BusLayout.softwareView(contentHeight);
        scroll = Math.max(0, Math.min(scroll, contentHeight - viewH));
        g.enableScissor(left + BusLayout.LABEL_X, viewTop, left + BusLayout.RIGHT, viewTop + viewH);
        int y = viewTop - scroll;
        for (final Section section : sections) {
            y = drawSection(g, section, left, y);
        }
        g.disableScissor();
        BusDraw.scrollbar(g, left + BusLayout.SCROLL_X, viewTop, viewH, scroll, contentHeight);
    }

    boolean scrolled(final double mx, final double my, final double delta, final int left, final int top) {
        final int viewH = BusLayout.softwareView(contentHeight);
        if (!BusDraw.inside(mx, my, left + BusLayout.LABEL_X, top + BusLayout.VIEW_Y, BusLayout.ROW_W + 6, viewH)
                || contentHeight <= viewH) {
            return false;
        }
        scroll = Math.max(0, Math.min(contentHeight - viewH, scroll - (int) Math.signum(delta) * BusLayout.ROW));
        return true;
    }

    /** One part of the tab: a word over a box of lines, or a note on its own. */
    private record Section(TextKey word, List<String> lines, boolean code) {
    }

    private void layOut() {
        final BusSettings s = menu.settings();
        sections.clear();
        if (s.name().isEmpty()) {
            sections.add(new Section(BusTexts.ADDRESS, wrap(GameText.resolve(BusTexts.NO_NAME)), false));
        } else if (menu.window() == BusLayout.Window.ROUTER) {
            sections.add(new Section(BusTexts.ADDRESS, wrap(BusScript.routerAddress(s.name())), true));
            sections.add(new Section(BusTexts.IQL, wrapAll(BusScript.routerIql(s)), true));
            sections.add(new Section(BusTexts.SIGMA, wrapAll(BusScript.routerSigma(s)), true));
        } else if (menu.window() == BusLayout.Window.RECEIVING) {
            // Nothing a program sets reaches a Receiving Bus: it is set here, and credits what its interface fed.
            sections.add(new Section(null, wrap(GameText.resolve(BusTexts.RECEIVING_SOFTWARE)), false));
        } else if (BusScript.iql(s).isEmpty()) {
            // A bus with nothing to set (the Vintage External Storage Bus) is still found by its address.
            sections.add(new Section(BusTexts.ADDRESS, wrap(BusScript.address(s.name())), true));
            sections.add(new Section(null, wrap(GameText.resolve(BusTexts.NOTHING_TO_SET)), false));
        } else {
            sections.add(new Section(BusTexts.ADDRESS, wrap(BusScript.address(s.name())), true));
            sections.add(new Section(BusTexts.IQL, wrapAll(BusScript.iql(s)), true));
            sections.add(new Section(BusTexts.SIGMA, wrapAll(BusScript.sigma(s)), true));
        }
        sections.add(new Section(BusTexts.SET_BY_PROGRAMS, programs(s), false));
        sections.add(new Section(null, wrap(GameText.resolve(BusTexts.SOFTWARE_NOTE)), false));
        int height = 0;
        for (final Section section : sections) {
            height += sectionHeight(section);
        }
        contentHeight = height;
    }

    private int drawSection(final GuiGraphics g, final Section section, final int left, final int top) {
        final int x = left + BusLayout.LABEL_X;
        int y = top;
        if (section.word() == null) {
            for (final String line : section.lines()) {
                BusDraw.small(g, font, line, x, y, JsTechTheme.dim());
                y += BusLayout.LINE;
            }
            return top + sectionHeight(section);
        }
        BusDraw.small(g, font, GameText.resolve(section.word()), x, y, JsTechTheme.dim());
        y += BusLayout.SECTION_LABEL_H;
        final int boxH = BusLayout.codeBox(section.lines().size());
        if (section.code()) {
            BusDraw.code(g, x, y, BusLayout.ROW_W, boxH);
        } else {
            BusDraw.bar(g, x, y, BusLayout.ROW_W, boxH, false);
        }
        int lineY = y + BusLayout.BOX_PAD;
        for (final String line : section.lines()) {
            BusDraw.small(g, font, line, x + BusLayout.BOX_PAD + 1, lineY,
                    section.code() ? JsTechTheme.accent2() : JsTechTheme.text());
            lineY += BusLayout.LINE;
        }
        return top + sectionHeight(section);
    }

    private static int sectionHeight(final Section section) {
        if (section.word() == null) {
            return section.lines().size() * BusLayout.LINE;
        }
        return BusLayout.SECTION_LABEL_H + BusLayout.codeBox(section.lines().size()) + BusLayout.SECTION_GAP;
    }

    /* Which programs set what, a line each: "Night shift: the hours, the priority". */
    private List<String> programs(final BusSettings s) {
        final Map<String, List<String>> byProgram = new LinkedHashMap<>();
        s.setBy().forEach((setting, program) -> byProgram.computeIfAbsent(program, p -> new ArrayList<>())
                .add(GameText.resolve(settingWord(setting))));
        if (byProgram.isEmpty()) {
            return wrap(GameText.resolve(BusTexts.NOTHING_SET));
        }
        final List<String> lines = new ArrayList<>();
        byProgram.forEach((program, settings) -> lines.addAll(wrap(GameText.resolve(
                BusTexts.PROGRAM_SET.with(program, String.join(", ", settings))))));
        return lines;
    }

    private static TextKey settingWord(final String setting) {
        return switch (setting) {
            case BusSettings.POWER -> BusTexts.SETTING_POWER;
            case BusSettings.MODE -> BusTexts.SETTING_MODE;
            case BusSettings.KEEP -> BusTexts.SETTING_KEEP;
            case BusSettings.MAX -> BusTexts.SETTING_MAX;
            case BusSettings.PRIORITY -> BusTexts.SETTING_PRIORITY;
            case BusSettings.CONDITIONS -> BusTexts.SETTING_CONDITIONS;
            case BusSettings.MATCH -> BusTexts.SETTING_MATCH;
            case BusSettings.ACCESS -> BusTexts.SETTING_ACCESS;
            default -> BusTexts.SETTING_FILTER;
        };
    }

    private List<String> wrap(final String text) {
        return BusDraw.lines(font, text, CODE_ROOM);
    }

    /* Each line of code broken where it runs past its box, the rest going in a little on the next. */
    private List<String> wrapAll(final List<String> code) {
        final List<String> lines = new ArrayList<>();
        for (final String line : code) {
            final List<String> parts = BusDraw.lines(font, line, CODE_ROOM);
            for (int i = 0; i < parts.size(); i++) {
                lines.add(i == 0 ? parts.get(i) : CONTINUED + parts.get(i).stripLeading());
            }
        }
        return lines;
    }
}
