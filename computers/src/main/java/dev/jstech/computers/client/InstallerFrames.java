/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client;

import dev.jstech.computers.JsComputers;
import dev.jstech.computers.os.DesktopEnvironmentDef;
import dev.jstech.computers.os.OsDef;
import dev.jstech.computers.os.OsRegistry;
import dev.jstech.computers.os.PanelStyle;
import dev.jstech.computers.os.install.InstallerFlow;
import dev.jstech.computers.os.install.InstallerPage;
import dev.jstech.computers.os.install.InstallerStyle;
import dev.jstech.core.palette.Palette;
import dev.jstech.core.palette.PaletteHolder;
import dev.jstech.core.palette.Palettes;
import dev.jstech.core.text.GameText;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

/**
 * The shapes the installers are drawn in: the whole screen of text, the grey window with its title in a tab, the
 * wizard with a picture down its side, the coloured ground with the steps on the left, and the pale card.
 *
 * <p>Each one paints its own frame and hands back the rectangle the page's own content goes in, along with the
 * ink to write it in and whatever buttons it drew. The pages themselves know nothing about any of this, which is
 * what lets one installer change shape halfway through without the questions changing with it.
 *
 * <p>Every colour is a palette: {@code jsc:installer/<frame>} for the frames with a shape of their own, and
 * {@code jsc:installer/text/<style>} for the ground and ink of each installer drawn as plain text.
 */
@PaletteHolder
final class InstallerFrames {

    /** How tall a title bar is in the frames that have one. */
    private static final int TITLE_BAR = 14;

    /** How tall a button is, and how far one sits from the next. */
    private static final int BUTTON = 14;

    /** The maker's lockup in a page header, at the size that bar has room for. */
    private static final int HEADER_MARK_W = 72;
    private static final int HEADER_MARK_H = 18;

    /** The picture down the side of the oldest wizard, and the size it is made at. */
    private static final ResourceLocation WIZARD_PICTURE =
            ResourceLocation.fromNamespaceAndPath("jsc", "textures/gui/installer/frames_95_wizard.png");
    private static final int WIZARD_W = 46;
    private static final int WIZARD_H = 168;

    private static final Palette<Boxed> BOXED = Palettes.declare(JsComputers.MODID, "installer/boxed",
            new Boxed(0xFF000000, 0xFFFFFFFF));
    private static final Palette<Wizard> WIZARD = Palettes.declare(JsComputers.MODID, "installer/wizard",
            new Wizard(0xFF000080, 0xFF1084D0, 0xFFFFFFFF, 0xFFC0C0C0, 0xFF008080,
                    0xFFFFFFFF, 0xFF808080, 0xFF000000, 0xFF000000, 0xFF000000,
                    0xFF000000, 0xFF000000, 0xFF404040, 0xFF0000A8, 0xFF000080, 0xFFFFFFFF));
    private static final Palette<SidePanel> SIDE_PANEL = Palettes.declare(JsComputers.MODID, "installer/side_panel",
            new SidePanel(0xFF5A7EDC, 0xFF00309C, 0xFF9DB9EB, 0xFFF3A660, 0xFF2A56C6, 0xFF1B3FA8,
                    0xFF5FE07A, 0xFFF3A660, 0xFF7D96D8, 0xFFFFFFFF, 0xFFB7C8F2, 0xFFDCE6FA, 0xFFFFFFFF,
                    0xFF16307F, 0xFF7BC34A,
                    0xFFFFFFFF, 0xFFFFFFFF, 0xFFDCE6FA, 0xFFF3A660, 0xFF00309C, 0xFFFFFFFF,
                    0xFFECE9D8, 0xFF0831D9, 0xFF3F8CF3, 0xFF0846C0, 0xFFFFFFFF,
                    0xFF000000, 0xFF000000, 0xFF505050, 0xFF0846C0, 0xFF0846C0, 0xFFFFFFFF));
    private static final Palette<Card> CARD = Palettes.declare(JsComputers.MODID, "installer/card",
            new Card(0xFF1E3E74, 0xFF0B1530, 0xFFFAFAFE, 0xFFC0C4D2, 0xFF6B7488, 0xFFE3E5EE, 0xFF202434,
                    0xFF202434, 0xFF202434, 0xFF6B7488, 0xFF3A6AE0, 0xFFE7EEFC, 0xFF202434,
                    0xFF3A6AE0, 0xFF2B4FAA, 0xFFCDD1DD, 0xFFFFFFFF,
                    0xFFFFFFFF, 0xFFE7EAF2, 0xFFCDD1DD, 0xFF9AA2B6, 0xFF202434));

    private static final Palette<Ink> MC_DOS_INK = Palettes.declare(JsComputers.MODID, "installer/text/mc_dos",
            new Ink(0xFF000000, 0xFF41D862, 0xFFA4FFBC, 0xFF1F8A3F, 0xFF41D862, 0xFF000000,
                    0xFF41D862, 0xFF000000, 0xFFA4FFBC, 0xFF000000, 0xFF41D862));
    /*
     * The appliance's own phosphor, taken from the Vintage skin its screens wear, so the install and the machine it
     * installs look like one thing rather than two. Amber where it wants the eye, as that generation's boxes did.
     */
    private static final Palette<Ink> MC_NET_INK = Palettes.declare(JsComputers.MODID, "installer/text/mc_net",
            new Ink(0xFF000000, 0xFF33FF66, 0xFF99FFBB, 0xFF2E8B2E, 0xFF33FF66, 0xFF000000,
                    0xFF33FF66, 0xFF000000, 0xFFFFB000, 0xFF000000, 0xFF33FF66));
    private static final Palette<Ink> FRAMES_INK = Palettes.declare(JsComputers.MODID, "installer/text/frames",
            new Ink(0xFF0000A8, 0xFFC0C0C0, 0xFFFFFFFF, 0xFF7B7BB8, 0xFFC0C0C0, 0xFF000000,
                    0xFFC0C0C0, 0xFF0000A8, 0xFFFFFF55, 0xFFC0C0C0, 0xFF000000));
    private static final Palette<Ink> FRAMES_11_INK = Palettes.declare(JsComputers.MODID,
            "installer/text/frames_11",
            new Ink(0xFF0B1530, 0xFFE8EAF2, 0xFFFFFFFF, 0xFF9AA2B2, 0xFF3A6AE0, 0xFFFFFFFF,
                    0xFF3A6AE0, 0xFFFFFFFF, 0xFF7FA6FF, 0xFF0B1530, 0xFFE8EAF2));
    private static final Palette<Ink> UBUNTU_INK = Palettes.declare(JsComputers.MODID, "installer/text/ubuntu",
            new Ink(0xFF111111, 0xFFE6E6E6, 0xFFFFFFFF, 0xFF8A8A8A, 0xFFE95420, 0xFFFFFFFF,
                    0xFFE95420, 0xFFFFFFFF, 0xFFE95420, 0xFF111111, 0xFFE6E6E6));
    private static final Palette<Ink> DEBIAN_INK = Palettes.declare(JsComputers.MODID, "installer/text/debian",
            new Ink(0xFF0000A8, 0xFF000000, 0xFF000000, 0xFF5A5A5A, 0xFF0000A8, 0xFFFFFFFF,
                    0xFFA80000, 0xFFFFFFFF, 0xFFA80000, 0xFFC0C0C0, 0xFF000000));
    private static final Palette<Ink> FEDORA_INK = Palettes.declare(JsComputers.MODID, "installer/text/fedora",
            new Ink(0xFF000000, 0xFFE6E6E6, 0xFFFFFFFF, 0xFF8A8A8A, 0xFF1A1A1A, 0xFFE6E6E6,
                    0xFF294172, 0xFFFFFFFF, 0xFF5FE07A, 0xFF000000, 0xFFE6E6E6));
    private static final Palette<Ink> PLAIN_INK = Palettes.declare(JsComputers.MODID, "installer/text/plain",
            new Ink(0xFF10151B, 0xFFCDD6E2, 0xFF39D6C4, 0xFF7D8A9C, 0xFF19212B, 0xFFCDD6E2,
                    0xFF19212B, 0xFF39D6C4, 0xFF39D6C4, 0xFF10151B, 0xFFCDD6E2));

    private InstallerFrames() {
    }

    /** Which of a frame's buttons the player has the mouse held down on, so it can be drawn pressed. */
    enum Held { NONE, NEXT, BACK, CANCEL, ERASE }

    /**
     * Paints the frame for that page and answers where its content goes.
     *
     * @param sx   the left of the monitor's picture
     * @param sy   the top of the monitor's picture
     * @param sw   how wide the picture is
     * @param sh   how tall it is
     * @param held the button the player is holding down, which is drawn pressed in
     */
    static Frame paint(final GuiGraphics g, final Font font, final InstallerFlow flow, final int ticksDone,
                       final int sx, final int sy, final int sw, final int sh, final Held held) {
        return switch (flow.chrome()) {
            case FULL_TEXT -> fullText(g, font, flow, sx, sy, sw, sh);
            case BOXED_TEXT -> boxedText(g, font, flow, sx, sy, sw, sh);
            case WIZARD -> wizard(g, font, flow, sx, sy, sw, sh, held);
            case SIDE_PANEL -> sidePanel(g, font, flow, ticksDone, sx, sy, sw, sh, held);
            case CARD -> card(g, font, flow, sx, sy, sw, sh, held);
        };
    }

    /** A label cut to the room it has, so a long step name never runs out of its panel. */
    static String clip(final Font font, final String text, final int room) {
        if (font.width(text) <= room) {
            return text;
        }
        String cut = text;
        while (cut.length() > 1 && font.width(cut + "...") > room) {
            cut = cut.substring(0, cut.length() - 1);
        }
        return cut + "...";
    }

    /**
     * The whole screen in text: a heading at the top, the keys along the foot.
     *
     * <p>One of them wears its heading in a coloured band across the top instead, with its help one word away
     * at the other end and its two buttons written out in the middle of the foot, which is what that installer
     * looked like and not a variation on the others.
     */
    private static Frame fullText(final GuiGraphics g, final Font font, final InstallerFlow flow,
                                  final int sx, final int sy, final int sw, final int sh) {
        final Ink ink = inkOf(flow.style());
        g.fill(sx, sy, sx + sw, sy + sh, ink.back());
        if (flow.style() == InstallerStyle.UBUNTU) {
            return banded(g, font, flow, ink, sx, sy, sw, sh);
        }
        TextWall.draw(g, font, GameText.resolve(flow.style().title(flow.systemName())), sx + 8, sy + 8,
                ink.bright());
        TextWall.draw(g, font, GameText.resolve(flow.style().heading(flow.page(), flow.systemName())), sx + 8,
                sy + 20, ink.text());
        final String hint = GameText.resolve(flow.style().hint(flow.page()));
        if (!hint.isEmpty()) {
            g.fill(sx, sy + sh - TITLE_BAR, sx + sw, sy + sh, ink.bar());
            TextWall.draw(g, font, hint, sx + 6, sy + sh - TITLE_BAR + 4, ink.barText());
        }
        return new Frame(sx + 16, sy + 36, sw - 32, sh - 54, ink.paint(), null, null, null, null);
    }

    /**
     * The server installer: its heading in a coloured band at the top, its help at the other end of that band,
     * and the two things a page can do written out in the middle of the foot.
     */
    private static Frame banded(final GuiGraphics g, final Font font, final InstallerFlow flow, final Ink ink,
                                final int sx, final int sy, final int sw, final int sh) {
        final int band = 19;
        g.fill(sx, sy, sx + sw, sy + band, ink.bar());
        TextWall.draw(g, font, GameText.resolve(flow.style().heading(flow.page(), flow.systemName())), sx + 10,
                sy + 6, ink.barText());
        TextWall.right(g, font, GameText.resolve(InstallerScreenTexts.FRAME_HELP), sx + sw - 10, sy + 6, ink.barText());
        /*
         * The buttons of that installer are written out rather than drawn: it ran in a terminal, and the
         * brackets around a word were the whole of what a button looked like there.
         */
        final boolean finishing = flow.page() == InstallerPage.COPY || flow.page() == InstallerPage.DONE;
        final String first = GameText.resolve(finishing ? InstallerScreenTexts.FRAME_REBOOT_NOW
                : InstallerScreenTexts.FRAME_DONE);
        TextWall.centered(g, font, first, sx + sw / 2, sy + sh - 22, ink.accent());
        if (!finishing) {
            TextWall.centered(g, font, GameText.resolve(InstallerScreenTexts.FRAME_BACK), sx + sw / 2, sy + sh - 12,
                    ink.text());
        }
        return new Frame(sx + 12, sy + band + 10, sw - 24, sh - band - 40, ink.paint(), null, null, null, null);
    }

    /** A grey window over a coloured ground, its title in a tab on the top edge. */
    private static Frame boxedText(final GuiGraphics g, final Font font, final InstallerFlow flow,
                                   final int sx, final int sy, final int sw, final int sh) {
        final Ink ink = inkOf(flow.style());
        final Boxed c = BOXED.get();
        g.fill(sx, sy, sx + sw, sy + sh, ink.back());
        final int wx = sx + 10;
        final int wy = sy + 14;
        final int ww = sw - 20;
        final int wh = sh - 34;
        g.fill(wx + 3, wy + 3, wx + ww + 3, wy + wh + 3, c.shadow());
        g.fill(wx, wy, wx + ww, wy + wh, ink.panel());
        final String tab = " " + GameText.resolve(flow.style().heading(flow.page(), flow.systemName())) + " ";
        final int tabW = TextWall.width(font, tab);
        final int tabX = wx + (ww - tabW) / 2;
        g.fill(tabX, wy - 5, tabX + tabW, wy + 5, ink.panel());
        TextWall.draw(g, font, tab, tabX, wy - 4, ink.panelText());
        final String hint = GameText.resolve(flow.style().hint(flow.page()));
        if (!hint.isEmpty()) {
            TextWall.draw(g, font, hint, sx + 8, sy + sh - 11, c.hint());
        }
        final Paint paint = ink.paint();
        return new Frame(wx + 8, wy + 12, ww - 16, wh - 20,
                new Paint(ink.panelText(), ink.panelText(), paint.dim(), ink.accent(), ink.select(),
                        ink.selectText()),
                null, null, null, null);
    }

    /** A grey dialog with a picture down one side and the buttons along the bottom. */
    private static Frame wizard(final GuiGraphics g, final Font font, final InstallerFlow flow,
                                final int sx, final int sy, final int sw, final int sh, final Held held) {
        final Wizard c = WIZARD.get();
        g.fillGradient(sx, sy, sx + sw, sy + sh, c.groundFrom(), c.groundTo());
        final String title = GameText.resolve(flow.style().title(flow.systemName()));
        g.drawString(font, title, sx + 14, sy + 8, c.titleInk(), true);

        final int dx = sx + 22;
        final int dy = sy + 26;
        final int dw = sw - 44;
        final int dh = sh - 38;
        bevel(g, dx, dy, dw, dh, c.face(), true);
        g.fillGradient(dx + 2, dy + 2, dx + dw - 2, dy + 2 + TITLE_BAR, c.groundFrom(), c.groundTo());
        g.drawString(font, GameText.resolve(InstallerScreenTexts.FRAME_WIZARD.with(title)), dx + 6, dy + 6,
                c.titleInk(), false);

        final int railW = WIZARD_W;
        final int railTop = dy + TITLE_BAR + 8;
        final int railBottom = dy + dh - 30;
        /*
         * The picture that wizard had down its side: the computer, the box the software came in and its disk. It
         * is drawn 1:1 and cut to the rail, over the rail's own teal, so a taller or shorter glass never stretches it.
         */
        final int railH = railBottom - railTop;
        bevel(g, dx + 6, railTop, railW, railH, c.rail(), false);
        final int shown = Math.min(WIZARD_H, railH);
        g.blit(WIZARD_PICTURE, dx + 6, railTop, 0.0F, 0.0F, railW, shown, WIZARD_W, WIZARD_H);
        edges(g, dx + 6, railTop, railW, railH, false);

        // The groove above the buttons, which is two lines and not one: that is what makes it look pressed in.
        g.fill(dx + 6, dy + dh - 24, dx + dw - 6, dy + dh - 23, c.shadow());
        g.fill(dx + 6, dy + dh - 23, dx + dw - 6, dy + dh - 22, c.light());

        final int by = dy + dh - 18;
        final int[] cancel = button(g, font, dx + dw - 6 - 52, by, 52,
                GameText.resolve(InstallerScreenTexts.FRAME_CANCEL), false, held == Held.CANCEL);
        final boolean canGo = flow.canContinue() || flow.page() == InstallerPage.DONE;
        final int[] next = button(g, font, dx + dw - 6 - 106, by, 52,
                GameText.resolve(flow.page() == InstallerPage.DONE ? InstallerScreenTexts.FRAME_RESTART
                        : InstallerScreenTexts.FRAME_NEXT_ARROW), canGo, held == Held.NEXT);
        final int[] back = button(g, font, dx + dw - 6 - 160, by, 52,
                GameText.resolve(InstallerScreenTexts.FRAME_BACK_ARROW), false, held == Held.BACK);

        final int cx = dx + 6 + railW + 8;
        final Paint paint = new Paint(c.text(), c.bright(), c.dim(), c.accent(), c.select(), c.selectText());
        return new Frame(cx, railTop, dx + dw - 8 - cx, railBottom - railTop, paint, next, back, cancel, null);
    }

    /** A coloured ground with the steps listed down one side and the question in a dialog over it. */
    private static Frame sidePanel(final GuiGraphics g, final Font font, final InstallerFlow flow,
                                   final int ticksDone, final int sx, final int sy, final int sw, final int sh,
                                   final Held held) {
        final SidePanel c = SIDE_PANEL.get();
        g.fill(sx, sy, sx + sw, sy + sh, c.ground());
        g.fill(sx, sy, sx + sw, sy + 22, c.band());
        g.fill(sx, sy + 22, sx + sw, sy + 23, c.headerRule());
        /*
         * The maker's lockup, not its name typed beside a mark. This header is the one place on the page that
         * says whose system is being installed, and it says it the way that system said it everywhere else.
         */
        SplashLogos.draw(g, SplashLogos.FRAMES_XP, sx + 4 + HEADER_MARK_W / 2, sy + 2,
                HEADER_MARK_W, HEADER_MARK_H);
        g.fill(sx, sy + sh - 14, sx + sw, sy + sh, c.band());
        g.fill(sx, sy + sh - 15, sx + sw, sy + sh - 14, c.footerRule());

        final int panelW = 116;
        g.fillGradient(sx, sy + 23, sx + panelW, sy + sh - 15, c.stepsFrom(), c.stepsTo());
        int ty = sy + 30;
        final int running = flow.stepAt(ticksDone);
        for (int i = 0; i < flow.steps().size(); i++) {
            final boolean done = i < running;
            final boolean now = i == running;
            g.fill(sx + 8, ty + 2, sx + 13, ty + 7, done ? c.stepDone() : now ? c.stepNow() : c.stepTodo());
            g.drawString(font, clip(font, GameText.resolve(flow.steps().get(i).label()), panelW - 26), sx + 17, ty,
                    done || now ? c.stepInk() : c.stepTodoInk(), false);
            ty += 11;
        }
        ty += 6;
        g.drawString(font, GameText.resolve(InstallerScreenTexts.FRAME_COMPLETE_IN), sx + 8, ty, c.note(), false);
        g.drawString(font, GameText.resolve(InstallerScreenTexts.FRAME_APPROXIMATELY), sx + 8, ty + 9, c.note(),
                false);
        final int left = Math.max(0, (flow.ticksTotal() - ticksDone) / 20);
        g.drawString(font, GameText.resolve(InstallerScreenTexts.FRAME_SECONDS.with(left)), sx + 8, ty + 20,
                c.seconds(), false);
        g.fill(sx + 8, ty + 34, sx + panelW - 8, ty + 40, c.barBack());
        g.fill(sx + 8, ty + 34, sx + 8 + (panelW - 16) * flow.permille(ticksDone) / 1000, ty + 40, c.barFill());

        final int cx = sx + panelW + 8;
        final int cw = sw - panelW - 16;
        if (!flow.stage().asks()) {
            // Nothing to ask: the maker's name over the middle of the ground, the way it waits in life.
            /*
             * The whole lockup, not a mark with the name typed beside it: this is the one place in the install
             * where the maker's name is the picture, and the mock has it as the thing itself.
             */
            SplashLogos.draw(g, SplashLogos.FRAMES_XP, cx + cw / 2, sy + 58);
            final Paint plain = new Paint(c.groundText(), c.groundBright(), c.groundDim(), c.groundAccent(),
                    c.groundSelect(), c.groundSelectText());
            return new Frame(cx, sy + 104, cw, sh - 122, plain, null, null, null, null);
        }
        final int dy = sy + 40;
        final int dh = sh - 80;
        g.fill(cx, dy, cx + cw, dy + dh, c.dialog());
        outline(g, cx, dy, cw, dh, c.dialogEdge());
        g.fillGradient(cx + 1, dy + 1, cx + cw - 1, dy + 1 + TITLE_BAR, c.dialogTitleFrom(), c.dialogTitleTo());
        g.drawString(font, GameText.resolve(flow.style().title(flow.systemName())), cx + 5, dy + 5,
                c.dialogTitleInk(), false);
        final int by = dy + dh - 18;
        final boolean canGo = flow.canContinue();
        final int[] next = button(g, font, cx + cw - 6 - 52, by, 52,
                GameText.resolve(InstallerScreenTexts.FRAME_NEXT_ARROW), canGo, held == Held.NEXT);
        final int[] back = button(g, font, cx + cw - 6 - 106, by, 52,
                GameText.resolve(InstallerScreenTexts.FRAME_BACK_ARROW), false, held == Held.BACK);
        final Paint paint = new Paint(c.dialogText(), c.dialogBright(), c.dialogDim(), c.dialogAccent(),
                c.dialogSelect(), c.dialogSelectText());
        return new Frame(cx + 8, dy + TITLE_BAR + 8, cw - 16, dh - TITLE_BAR - 32, paint, next, back, null, null);
    }

    /** A pale card, the question at the top and the buttons at the bottom right. */
    private static Frame card(final GuiGraphics g, final Font font, final InstallerFlow flow,
                              final int sx, final int sy, final int sw, final int sh, final Held held) {
        final Card c = CARD.get();
        g.fillGradient(sx, sy, sx + sw, sy + sh, c.groundFrom(), c.groundTo());
        final int cx = sx + 14;
        final int cy = sy + 10;
        final int cw = sw - 28;
        final int ch = sh - 20;
        g.fill(cx, cy, cx + cw, cy + ch, c.card());
        outline(g, cx, cy, cw, ch, c.cardEdge());

        FramesEmblem.draw(g, cx + 8, cy + 5, editionOf(flow));
        g.drawString(font, GameText.resolve(flow.style().title(flow.systemName())), cx + 20, cy + 6, c.title(),
                false);
        g.fill(cx + 1, cy + 19, cx + cw - 1, cy + 20, c.rule());
        g.drawString(font, GameText.resolve(flow.style().heading(flow.page(), flow.systemName())), cx + 10, cy + 27,
                c.heading(), false);

        final int by = cy + ch - 20;
        /*
         * Nothing to press while it is copying. A page with a Back and a Next on it is a page offering a
         * choice, and there is none here: the work runs to the end and the machine restarts itself.
         */
        final boolean working = flow.page() == InstallerPage.COPY;
        final boolean canGo = flow.canContinue() || flow.page() == InstallerPage.DONE;
        final int[] next = working ? null : primary(g, font, cx + cw - 10 - 56, by, 56,
                GameText.resolve(flow.page() == InstallerPage.DONE ? InstallerScreenTexts.FRAME_RESTART
                        : InstallerScreenTexts.FRAME_NEXT), canGo, held == Held.NEXT);
        final int[] back = working ? null : pale(g, font, cx + cw - 10 - 118, by, 56,
                GameText.resolve(InstallerScreenTexts.FRAME_BACK_PLAIN), held == Held.BACK);
        final int[] erase = flow.page() == InstallerPage.DISK
                ? pale(g, font, cx + 10, by, 66, GameText.resolve(InstallerScreenTexts.FRAME_ERASE_DISK),
                        held == Held.ERASE)
                : null;
        final Paint paint = new Paint(c.text(), c.bright(), c.dim(), c.accent(), c.select(), c.selectText());
        return new Frame(cx + 10, cy + 42, cw - 20, ch - 68, paint, next, back, null, erase);
    }

    /** The edition being installed, by the chrome of the desktop it comes with; the newest for any other. */
    private static PanelStyle editionOf(final InstallerFlow flow) {
        final ResourceLocation id = ResourceLocation.tryParse(flow.systemId());
        final OsDef system = id == null ? null : OsRegistry.getOs(id);
        final DesktopEnvironmentDef desktop = system == null ? null
                : system.bundledDesktop().map(OsRegistry::getDesktop).orElse(null);
        return desktop == null ? PanelStyle.FRAMES_11 : desktop.panelStyle();
    }

    /** The ground and ink of an installer drawn as plain text; the two oldest desktop systems share theirs. */
    private static Ink inkOf(final InstallerStyle style) {
        return switch (style) {
            case MC_DOS -> MC_DOS_INK.get();
            case MC_NET -> MC_NET_INK.get();
            case FRAMES_95, FRAMES_XP -> FRAMES_INK.get();
            case FRAMES_11 -> FRAMES_11_INK.get();
            case UBUNTU -> UBUNTU_INK.get();
            case DEBIAN -> DEBIAN_INK.get();
            case FEDORA -> FEDORA_INK.get();
            case PLAIN -> PLAIN_INK.get();
        };
    }

    /** A raised or sunken bevel in the manner of the grey machines: light one way, dark the other. */
    private static void bevel(final GuiGraphics g, final int x, final int y, final int w, final int h,
                              final int fill, final boolean raised) {
        g.fill(x, y, x + w, y + h, fill);
        edges(g, x, y, w, h, raised);
    }

    /** The two edges of a bevel alone, laid over whatever is already inside it. */
    private static void edges(final GuiGraphics g, final int x, final int y, final int w, final int h,
                              final boolean raised) {
        final Wizard c = WIZARD.get();
        final int light = raised ? c.light() : c.shadow();
        final int dark = raised ? c.dark() : c.light();
        g.fill(x, y, x + w, y + 1, light);
        g.fill(x, y, x + 1, y + h, light);
        g.fill(x, y + h - 1, x + w, y + h, dark);
        g.fill(x + w - 1, y, x + w, y + h, dark);
    }

    private static void outline(final GuiGraphics g, final int x, final int y, final int w, final int h,
                                final int colour) {
        g.fill(x, y, x + w, y + 1, colour);
        g.fill(x, y + h - 1, x + w, y + h, colour);
        g.fill(x, y, x + 1, y + h, colour);
        g.fill(x + w - 1, y, x + w, y + h, colour);
    }

    /**
     * A bevelled grey button; the one that carries the page forward wears the outline the default one wore.
     *
     * <p>A button of those machines went IN when it was pressed: the light edge and the dark edge swapped
     * places and the label moved a pixel down and right with them, so the whole face looked pushed into the
     * panel. Washing it lighter is what a modern button does, and on a grey machine it read as a highlight
     * rather than a press.
     */
    private static int[] button(final GuiGraphics g, final Font font, final int x, final int y, final int w,
                                final String label, final boolean strong, final boolean held) {
        final Wizard c = WIZARD.get();
        bevel(g, x, y, w, BUTTON, c.face(), !held);
        if (strong && !held) {
            outline(g, x - 1, y - 1, w + 2, BUTTON + 2, c.defaultRing());
        }
        final int nudge = held ? 1 : 0;
        g.drawString(font, label, x + (w - font.width(label)) / 2 + nudge, y + 3 + nudge, c.buttonInk(), false);
        return new int[]{x, y, w, BUTTON};
    }

    /* A modern button has no bevel to turn over, so it answers a press by darkening under the finger. */
    private static int[] primary(final GuiGraphics g, final Font font, final int x, final int y, final int w,
                                 final String label, final boolean on, final boolean held) {
        final Card c = CARD.get();
        g.fill(x, y, x + w, y + BUTTON, on ? (held ? c.primaryHeld() : c.primary()) : c.primaryOff());
        g.drawString(font, label, x + (w - font.width(label)) / 2, y + 3, c.primaryInk(), false);
        return new int[]{x, y, w, BUTTON};
    }

    private static int[] pale(final GuiGraphics g, final Font font, final int x, final int y, final int w,
                              final String label, final boolean held) {
        final Card c = CARD.get();
        g.fill(x, y, x + w, y + BUTTON, held ? c.paleHeld() : c.pale());
        outline(g, x, y, w, BUTTON, held ? c.paleHeldEdge() : c.paleEdge());
        g.drawString(font, label, x + (w - font.width(label)) / 2, y + 3, c.paleInk(), false);
        return new int[]{x, y, w, BUTTON};
    }

    /**
     * Where a page's content goes and what it is written in, with whatever buttons the frame drew.
     *
     * <p>A button that a frame does not draw is null, and the page reaches the same thing with a key.
     */
    record Frame(int x, int y, int w, int h, Paint paint, int[] next, int[] back, int[] cancel, int[] erase) {
    }

    /** The ink a page is written in inside its frame. */
    record Paint(int text, int bright, int dim, int accent, int select, int selectText) {
    }

    /**
     * The ground and ink of an installer drawn as plain text.
     *
     * <p>The frames with a shape of their own carry their colours with that shape, since a wizard is grey on
     * every machine that ever ran one.
     */
    private record Ink(int back, int text, int bright, int dim, int bar, int barText, int select, int selectText,
                       int accent, int panel, int panelText) {

        Paint paint() {
            return new Paint(this.text, this.bright, this.dim, this.accent, this.select, this.selectText);
        }
    }

    /** The grey window over a coloured ground: the shadow under the window and the hint along the foot. */
    private record Boxed(int shadow, int hint) {
    }

    /**
     * The grey wizard: its ground and title in the navy gradient, its face and the teal behind its picture, the
     * three edges of a bevel, its buttons, and the ink of the page inside it.
     */
    private record Wizard(int groundFrom, int groundTo, int titleInk, int face, int rail,
                          int light, int shadow, int dark, int buttonInk, int defaultRing,
                          int text, int bright, int dim, int accent, int select, int selectText) {
    }

    /**
     * The coloured ground with the steps down its side: the bands at the top and foot, the steps in each of their
     * states, the count of what is left and its bar, the ink on the bare ground, and the dialog a question sits in.
     */
    private record SidePanel(int ground, int band, int headerRule, int footerRule, int stepsFrom, int stepsTo,
                             int stepDone, int stepNow, int stepTodo, int stepInk, int stepTodoInk, int note,
                             int seconds, int barBack, int barFill,
                             int groundText, int groundBright, int groundDim, int groundAccent, int groundSelect,
                             int groundSelectText,
                             int dialog, int dialogEdge, int dialogTitleFrom, int dialogTitleTo, int dialogTitleInk,
                             int dialogText, int dialogBright, int dialogDim, int dialogAccent, int dialogSelect,
                             int dialogSelectText) {
    }

    /**
     * The pale card over a dark gradient: the card and its heading, the ink of the page, the button that carries the
     * page forward, and the pale ones beside it.
     */
    private record Card(int groundFrom, int groundTo, int card, int cardEdge, int title, int rule, int heading,
                        int text, int bright, int dim, int accent, int select, int selectText,
                        int primary, int primaryHeld, int primaryOff, int primaryInk,
                        int pale, int paleHeld, int paleEdge, int paleHeldEdge, int paleInk) {
    }
}
