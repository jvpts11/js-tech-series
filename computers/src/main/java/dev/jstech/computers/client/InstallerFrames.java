/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client;

import dev.jstech.computers.JsComputers;
import dev.jstech.computers.client.term.TermFace;
import dev.jstech.computers.client.term.TermText;
import dev.jstech.computers.gui.layout.InstallerLayout;
import dev.jstech.computers.gui.term.TermBuffer;
import dev.jstech.computers.os.Branding;
import dev.jstech.computers.os.DesktopEnvironmentDef;
import dev.jstech.computers.os.OsDef;
import dev.jstech.computers.os.OsRegistry;
import dev.jstech.computers.os.PanelStyle;
import dev.jstech.computers.os.install.InstallerChrome;
import dev.jstech.computers.os.install.InstallerFlow;
import dev.jstech.computers.os.install.InstallerPage;
import dev.jstech.computers.os.install.InstallerStyle;
import dev.jstech.core.client.gui.component.Draw;
import dev.jstech.core.palette.Palette;
import dev.jstech.core.palette.PaletteHolder;
import dev.jstech.core.palette.Palettes;
import dev.jstech.core.text.GameText;
import java.util.ArrayList;
import java.util.List;
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

    /** The margins a console keeps either side of its columns, which a text-mode page keeps too. */
    private static final int WALL_MARGINS = 20;

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

    /** The grounds the later Frames setups stand on, made at the size of the glass. */
    private static final ResourceLocation SEVEN_GROUND =
            ResourceLocation.fromNamespaceAndPath("jsc", "textures/gui/splash/ground/frames_7.png");
    private static final ResourceLocation TEN_GROUND =
            ResourceLocation.fromNamespaceAndPath("jsc", "textures/gui/splash/ground/frames_10.png");
    private static final int GROUND_W = InstallerLayout.WIDTH;
    private static final int GROUND_H = InstallerLayout.HEIGHT;
    /** The mark beside the edition's name on a page that greets, and the big button of a welcome. */
    private static final int BRAND_MARK = 18;
    private static final int BIG_BUTTON = 18;
    /** How tall the strip naming the two phases runs along the foot of Frames 7's screen. */
    private static final int PHASE_STRIP = 14;
    private static final int GLASS_RADIUS = 3;
    /** How much larger the first set-up's question is written than the rest of the page. */
    private static final float HEADING_SCALE = 1.5F;

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
    private static final Palette<Bsd> BSD = Palettes.declare(JsComputers.MODID, "installer/bsd",
            new Bsd(0xFF0000A8, 0xFFFFFFFF, 0xFFA8A8A8, 0xFF000000, 0xFF000000, 0xFFFFFFFF, 0xFF3C3C3C,
                    0xFF0000A8, 0xFFFFFFFF, 0xFF000000, 0xFF3C3C3C, 0xFFC00000, 0xFF0000A8, 0xFFFFFFFF,
                    0xFFFFFFFF, 0xFF0000A8, 0xFFFFE14D));
    /*
     * The one colour System V's console adds to its shared text ink: the green a finished part is named in. It is
     * a palette of its own beside the frames' rather than under installer/text/, which holds one ink per style.
     */
    private static final Palette<SystemVOk> SYSTEM_V_OK = Palettes.declare(JsComputers.MODID,
            "installer/system_v_ok", new SystemVOk(0xFF5FE07A));
    private static final Palette<Card> CARD = Palettes.declare(JsComputers.MODID, "installer/card",
            new Card(0xFF1E3E74, 0xFF0B1530, 0xFFFAFAFE, 0xFFC0C4D2, 0xFF6B7488, 0xFFE3E5EE, 0xFF202434,
                    0xFF202434, 0xFF202434, 0xFF6B7488, 0xFF3A6AE0, 0xFFE7EEFC, 0xFF202434,
                    0xFF3A6AE0, 0xFF2B4FAA, 0xFFCDD1DD, 0xFFFFFFFF,
                    0xFFFFFFFF, 0xFFE7EAF2, 0xFFCDD1DD, 0xFF9AA2B6, 0xFF202434));

    private static final Palette<Glass> GLASS = Palettes.declare(JsComputers.MODID, "installer/glass",
            new Glass(0x40000000, 0xFFFFFFFF, 0xFFBBCCDD, 0x40FFFFFF, 0xFF6FD0FF,
                    0xCC283C5A, 0x99BAD2EE, 0x8C96B4DC, 0x80FFFFFF, 0x66FFFFFF, 0xFF000000,
                    0xFF9FB3CF, 0xFFFFFFFF, 0xFF1E3287, 0xFF1E3287,
                    0xFF000000, 0xFF6B6B6B, 0xFF3399FF, 0xFFCCE8FF, 0xFF000000,
                    0xFFF2F2F2, 0xFFDDDDDD, 0xFFDAEEF9, 0xFFC2E4F6, 0xFFEAF6FD, 0xFFA7D9F5, 0xFF707070,
                    0xFF3A6EA5));
    private static final Palette<Metro> METRO = Palettes.declare(JsComputers.MODID, "installer/metro",
            new Metro(0xFF1883D7, 0xFFFFFFFF, 0xFF000000, 0xFF0078D7, 0xFF003399,
                    0xFF000000, 0xFF6D6D6D, 0xFF0078D7, 0xFFE5F1FB, 0xFF000000,
                    0xFFE1E1E1, 0xFFCCE4F7, 0xFFADADAD));
    private static final Palette<FirstSetup> FIRST_SETUP = Palettes.declare(JsComputers.MODID,
            "installer/first_setup",
            new FirstSetup(0xFF0063B1, 0xFFFFFFFF, 0xFFCCDDEE, 0xFFFFFFFF, 0x40FFFFFF, 0xFFFFFFFF,
                    0xFFFFFFFF, 0xFFDDE8F2, 0xFF7FA9CF, 0xFF0063B1));
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
    /*
     * UNIX System V's own console: white on black, the one banner in reverse video the light grey the real
     * terminal shows one in, and the accent a pale cyan for the few words this console picks out with it.
     */
    private static final Palette<Ink> SYSTEM_V_INK = Palettes.declare(JsComputers.MODID, "installer/text/system_v",
            new Ink(0xFF000000, 0xFFBDBDBD, 0xFFFFFFFF, 0xFF8A8A8A, 0xFFBDBDBD, 0xFF000000,
                    0xFFBDBDBD, 0xFF000000, 0xFF6FD3E0, 0xFFBDBDBD, 0xFF000000));

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
     * @param asks whether the next button opens a question rather than moving on (bsdinstall's Auto row that
     *             erases a disk), which makes it live even while the page itself cannot move on yet
     */
    static Frame paint(final GuiGraphics g, final Font font, final InstallerFlow flow, final int ticksDone,
                       final int sx, final int sy, final int sw, final int sh, final Held held, final boolean asks) {
        return switch (flow.chrome()) {
            case FULL_TEXT -> fullText(g, font, flow, sx, sy, sw, sh);
            case BOXED_TEXT -> boxedText(g, font, flow, sx, sy, sw, sh);
            case WIZARD -> wizard(g, font, flow, sx, sy, sw, sh, held);
            case SIDE_PANEL -> sidePanel(g, font, flow, ticksDone, sx, sy, sw, sh, held);
            case CARD -> card(g, font, flow, sx, sy, sw, sh, held);
            case DIALOG_BOX -> dialogBox(g, font, flow, sx, sy, sw, sh, held, asks);
            case GLASS -> glass(g, font, flow, ticksDone, sx, sy, sw, sh, held);
            case METRO -> metro(g, font, flow, sx, sy, sw, sh, held);
            case FIRST_SETUP -> firstSetup(g, font, flow, sx, sy, sw, sh, held);
        };
    }

    /** Whether a frame of that shape draws the system's brand and its big Install now on its welcome page. */
    static boolean brandsWelcome(final InstallerChrome chrome) {
        return chrome == InstallerChrome.GLASS || chrome == InstallerChrome.METRO;
    }

    /**
     * The size of the terminal font a text-mode page is written in, and its scale: those of a console on the same
     * glass, since such an installer ran in the machine's terminal and fitted its eighty columns.
     */
    static TermFace.Fitted wall() {
        return TermFace.forGlass(InstallerLayout.WIDTH - WALL_MARGINS, TermBuffer.MONITOR_COLUMNS);
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
        if (flow.style() == InstallerStyle.SYSTEM_V) {
            return sysVConsole(g, font, flow, ink, sx, sy, sw, sh);
        }
        final TermFace.Fitted wall = wall();
        final TermFace face = wall.face();
        final float scale = wall.scale();
        TermText.draw(face, g, font, GameText.resolve(flow.style().title(flow.systemName())), sx + 8, sy + 8, scale,
                ink.bright());
        TermText.draw(face, g, font, GameText.resolve(flow.style().heading(flow.page(), flow.systemName())), sx + 8,
                sy + 20, scale, ink.text());
        final String hint = GameText.resolve(flow.style().hint(flow.page()));
        if (!hint.isEmpty()) {
            g.fill(sx, sy + sh - TITLE_BAR, sx + sw, sy + sh, ink.bar());
            TermText.draw(face, g, font, hint, sx + 6, sy + sh - TITLE_BAR + 3, scale, ink.barText());
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
        final TermFace.Fitted wall = wall();
        final TermFace face = wall.face();
        final float scale = wall.scale();
        TermText.draw(face, g, font, GameText.resolve(flow.style().heading(flow.page(), flow.systemName())), sx + 10,
                sy + 6, scale, ink.barText());
        TermText.right(face, g, font, GameText.resolve(InstallerScreenTexts.FRAME_HELP), sx + sw - 10, sy + 6, scale,
                ink.barText());
        /*
         * The buttons of that installer are written out rather than drawn: it ran in a terminal, and the
         * brackets around a word were the whole of what a button looked like there.
         */
        final boolean finishing = flow.page() == InstallerPage.COPY || flow.page() == InstallerPage.DONE;
        final String first = GameText.resolve(finishing ? InstallerScreenTexts.FRAME_REBOOT_NOW
                : InstallerScreenTexts.FRAME_DONE);
        TermText.centred(face, g, font, first, sx + sw / 2, sy + sh - 22, scale, ink.accent());
        if (!finishing) {
            TermText.centred(face, g, font, GameText.resolve(InstallerScreenTexts.FRAME_BACK), sx + sw / 2,
                    sy + sh - 12, scale, ink.text());
        }
        return new Frame(sx + 12, sy + band + 10, sw - 24, sh - band - 40, ink.paint(), null, null, null, null);
    }

    /**
     * UNIX System V's own console: one banner in reverse video the whole way down, the page's own content under
     * it, and a dim line of keys along the foot; the last page swaps that line for a reverse-video button instead,
     * the one bsdinstall's neighbour prints its "Reboot" as.
     */
    private static Frame sysVConsole(final GuiGraphics g, final Font font, final InstallerFlow flow, final Ink ink,
                                     final int sx, final int sy, final int sw, final int sh) {
        final String banner = " " + GameText.resolve(flow.style().title(flow.systemName())) + " ";
        final int barTop = sy + InstallerLayout.SYSV_BANNER_TOP;
        final int barH = InstallerLayout.SYSV_BANNER_H;
        g.fill(sx, barTop, sx + sw, barTop + barH, ink.panel());
        final TermFace.Fitted wall = wall();
        final TermFace face = wall.face();
        final float scale = wall.scale();
        TermText.centred(face, g, font, banner, sx + sw / 2, barTop + 1, scale, ink.panelText());
        final boolean done = flow.page() == InstallerPage.DONE;
        final int footTop = sy + sh - InstallerLayout.SYSV_FOOT_H;
        if (done) {
            final String reboot = " " + GameText.resolve(InstallerScreenTexts.FRAME_REBOOT) + " ";
            final int rw = TermText.width(face, reboot, scale);
            g.fill(sx + (sw - rw) / 2, footTop, sx + (sw + rw) / 2, sy + sh - 3, ink.panel());
            TermText.draw(face, g, font, reboot, sx + (sw - rw) / 2, footTop + 1, scale, ink.panelText());
        } else {
            TermText.draw(face, g, font, GameText.resolve(flow.style().hint(flow.page())),
                    sx + InstallerLayout.SYSV_CONTENT_INSET, sy + sh - 9, scale, ink.dim());
        }
        final int contentTop = barTop + barH + InstallerLayout.SYSV_CONTENT_GAP;
        final int inset = InstallerLayout.SYSV_CONTENT_INSET;
        return new Frame(sx + inset, contentTop, sw - 2 * inset, footTop - contentTop, ink.paint(),
                done ? new int[]{sx, footTop, sw, barH} : null, null, null, null);
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
        final TermFace.Fitted wall = wall();
        final TermFace face = wall.face();
        final float scale = wall.scale();
        final String tab = " " + GameText.resolve(flow.style().heading(flow.page(), flow.systemName())) + " ";
        final int tabW = TermText.width(face, tab, scale);
        final int tabX = wx + (ww - tabW) / 2;
        g.fill(tabX, wy - 5, tabX + tabW, wy + 5, ink.panel());
        TermText.draw(face, g, font, tab, tabX, wy - 4, scale, ink.panelText());
        final String hint = GameText.resolve(flow.style().hint(flow.page()));
        if (!hint.isEmpty()) {
            TermText.draw(face, g, font, hint, sx + 8, sy + sh - 11, scale, c.hint());
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
        Draw.text(g, font, title, sx + 14, sy + 8, c.titleInk());

        final int dx = sx + 22;
        final int dy = sy + 26;
        final int dw = sw - 44;
        final int dh = sh - 38;
        bevel(g, dx, dy, dw, dh, c.face(), true);
        g.fillGradient(dx + 2, dy + 2, dx + dw - 2, dy + 2 + TITLE_BAR, c.groundFrom(), c.groundTo());
        Draw.text(g, font, GameText.resolve(InstallerScreenTexts.FRAME_WIZARD.with(title)), dx + 6, dy + 6,
                c.titleInk());

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
            Draw.text(g, font, clip(font, GameText.resolve(flow.steps().get(i).label()), panelW - 26), sx + 17, ty,
                    done || now ? c.stepInk() : c.stepTodoInk());
            ty += 11;
        }
        ty += 6;
        Draw.text(g, font, GameText.resolve(InstallerScreenTexts.FRAME_COMPLETE_IN), sx + 8, ty, c.note());
        Draw.text(g, font, GameText.resolve(InstallerScreenTexts.FRAME_APPROXIMATELY), sx + 8, ty + 9, c.note());
        final int left = Math.max(0, (flow.ticksTotal() - ticksDone) / 20);
        Draw.text(g, font, GameText.resolve(InstallerScreenTexts.FRAME_SECONDS.with(left)), sx + 8, ty + 20,
                c.seconds());
        g.fill(sx + 8, ty + 34, sx + panelW - 8, ty + 40, c.barBack());
        g.fill(sx + 8, ty + 34, sx + 8 + (panelW - 16) * flow.permille(ticksDone) / 1000, ty + 40, c.barFill());

        final int cx = sx + panelW + 8;
        final int cw = sw - panelW - 16;
        if (!flow.stage().asks()) {
            // Nothing to ask: the maker's name over the middle of the ground, the way it waits in life.
            /*
             * The whole lockup, not a mark with the name typed beside it: this is the one place in the install
             * where the maker's name is the picture, on its own rather than beside a smaller mark.
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
        Draw.text(g, font, GameText.resolve(flow.style().title(flow.systemName())), cx + 5, dy + 5,
                c.dialogTitleInk());
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
        Draw.text(g, font, GameText.resolve(flow.style().title(flow.systemName())), cx + 20, cy + 6, c.title());
        g.fill(cx + 1, cy + 19, cx + cw - 1, cy + 20, c.rule());
        Draw.text(g, font, GameText.resolve(flow.style().heading(flow.page(), flow.systemName())), cx + 10, cy + 27,
                c.heading());

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

    /**
     * Frames 7's: a window of tinted glass over the night-blue ground, the page on white inside it, the edition's
     * mark and name on the pages that greet, and a strip along the foot of the screen naming the install's two
     * phases, the one it is in lit and a bar beside them while it copies. Its welcome is one big Install now.
     */
    private static Frame glass(final GuiGraphics g, final Font font, final InstallerFlow flow, final int ticksDone,
                               final int sx, final int sy, final int sw, final int sh, final Held held) {
        final Glass c = GLASS.get();
        g.blit(SEVEN_GROUND, sx, sy, sw, sh, 0.0F, 0.0F, GROUND_W, GROUND_H, GROUND_W, GROUND_H);
        final int stripY = sy + sh - PHASE_STRIP;
        g.fill(sx, stripY, sx + sw, sy + sh, c.strip());
        final boolean second = flow.page() != InstallerPage.WELCOME && flow.page() != InstallerPage.DISK;
        final String first = GameText.resolve(InstallerScreenTexts.FRAME_PHASE_COLLECTING);
        final String then = GameText.resolve(InstallerScreenTexts.FRAME_PHASE_INSTALLING.with("Frames"));
        Draw.text(g, font, first, sx + 8, stripY + 3, second ? c.phaseDone() : c.phaseOn());
        final int thenX = sx + 8 + font.width(first) + 10;
        Draw.text(g, font, then, thenX, stripY + 3, second ? c.phaseOn() : c.phaseDone());
        if (flow.page() == InstallerPage.COPY) {
            final int barX = thenX + font.width(then) + 10;
            final int barW = sx + sw - 8 - barX;
            if (barW > 8) {
                g.fill(barX, stripY + 5, barX + barW, stripY + 9, c.barTrough());
                g.fill(barX, stripY + 5, barX + barW * flow.permille(ticksDone) / 1000, stripY + 9, c.barFill());
            }
        }

        final int wx = sx + 34;
        final int wy = sy + 10;
        final int ww = sw - 68;
        final int wh = sh - PHASE_STRIP - 18;
        roundTop(g, wx - 1, wy - 1, ww + 2, wh + 2, c.rim(), GLASS_RADIUS);
        roundTop(g, wx, wy, ww, wh, c.glassTop(), GLASS_RADIUS);
        g.fillGradient(wx, wy + GLASS_RADIUS, wx + ww, wy + wh, c.glassTop(), c.glassBottom());
        g.fill(wx + GLASS_RADIUS, wy, wx + ww - GLASS_RADIUS, wy + 1, c.gloss());
        final String title = flow.page() == InstallerPage.NAME
                ? GameText.resolve(InstallerScreenTexts.FRAME_SET_UP.with("Frames"))
                : GameText.resolve(flow.style().title(flow.systemName()));
        final int titleW = font.width(title);
        for (int i = 3; i >= 1; i--) {
            g.fill(wx + 6 - i - 1, wy + 4 - i, wx + 6 + titleW + i + 1, wy + 11 + i, c.glow());
        }
        Draw.text(g, font, title, wx + 6, wy + 4, c.title());

        final int ix = wx + 4;
        final int iy = wy + TITLE_BAR + 1;
        final int iw = ww - 8;
        final int ih = wh - TITLE_BAR - 5;
        g.fill(ix - 1, iy - 1, ix + iw + 1, iy + ih + 1, c.innerRim());
        g.fill(ix, iy, ix + iw, iy + ih, c.paper());
        int top = iy + 8;
        final boolean greets = flow.page() == InstallerPage.WELCOME || flow.page() == InstallerPage.NAME;
        if (greets) {
            SplashLogos.mark(g, PanelStyle.FRAMES_7, ix + 12, top, BRAND_MARK);
            Draw.text(g, font, flow.systemName(), ix + 14 + BRAND_MARK, top + 6, c.brand());
            top += BRAND_MARK + 8;
        }
        if (flow.page() != InstallerPage.WELCOME) {
            for (final String line : wrap(font, GameText.resolve(flow.style().heading(flow.page(),
                    flow.systemName())), iw - 24)) {
                Draw.text(g, font, line, ix + 12, top, c.heading());
                top += InstallerLayout.ROW;
            }
            top += 4;
        }
        final Paint paint = new Paint(c.text(), c.heading(), c.dim(), c.accent(), c.select(), c.selectText());
        if (flow.page() == InstallerPage.WELCOME) {
            final int bw = 86;
            final int[] next = sevenButton(g, font, c, ix + (iw - bw) / 2, top + 4, bw, BIG_BUTTON,
                    GameText.resolve(InstallerScreenTexts.FRAME_INSTALL_NOW), flow.canContinue(),
                    held == Held.NEXT, true);
            final String copyright = GameText.resolve(InstallerScreenTexts.FRAME_COPYRIGHT.with(
                    Branding.osYear(flow.systemName(), null), Branding.houseOf(flow.systemName()).legalName()));
            Draw.text(g, font, copyright, ix + (iw - font.width(copyright)) / 2, iy + ih - 12, c.dim());
            final int contentTop = top + BIG_BUTTON + 14;
            return new Frame(ix + 12, contentTop, iw - 24, iy + ih - 16 - contentTop, paint, next, null, null, null);
        }
        final int by = iy + ih - 8 - BUTTON;
        final boolean working = flow.page() == InstallerPage.COPY;
        final int[] next = working ? null : sevenButton(g, font, c, ix + iw - 8 - 56, by, 56, BUTTON,
                GameText.resolve(flow.page() == InstallerPage.DONE ? InstallerScreenTexts.FRAME_RESTART
                        : InstallerScreenTexts.FRAME_NEXT),
                flow.canContinue() || flow.page() == InstallerPage.DONE, held == Held.NEXT, false);
        return new Frame(ix + 12, top, iw - 24, by - 4 - top, paint, next, null, null, null);
    }

    /**
     * Frames 10's setup window: white and square with a thin blue border over the dark ground, the small mark in its
     * title, flat grey buttons, the edition's name in blue on the welcome with a big Install now.
     */
    private static Frame metro(final GuiGraphics g, final Font font, final InstallerFlow flow, final int sx,
                               final int sy, final int sw, final int sh, final Held held) {
        final Metro c = METRO.get();
        g.blit(TEN_GROUND, sx, sy, sw, sh, 0.0F, 0.0F, GROUND_W, GROUND_H, GROUND_W, GROUND_H);
        final int wx = sx + 40;
        final int wy = sy + 18;
        final int ww = sw - 80;
        final int wh = sh - 36;
        g.fill(wx - 1, wy - 1, wx + ww + 1, wy + wh + 1, c.border());
        g.fill(wx, wy, wx + ww, wy + wh, c.window());
        FramesEmblem.draw(g, wx + 4, wy + 3, PanelStyle.FRAMES_10);
        Draw.text(g, font, GameText.resolve(flow.style().title(flow.systemName())), wx + 17, wy + 4, c.title());
        int top = wy + TITLE_BAR + 8;
        final Paint paint = new Paint(c.text(), c.heading(), c.dim(), c.accent(), c.select(), c.selectText());
        if (flow.page() == InstallerPage.WELCOME) {
            SplashLogos.mark(g, PanelStyle.FRAMES_10, wx + 16, top, BRAND_MARK);
            Draw.text(g, font, flow.systemName(), wx + 18 + BRAND_MARK, top + 6, c.brand());
            top += BRAND_MARK + 12;
            final int bw = 86;
            final int[] next = tenButton(g, font, c, wx + (ww - bw) / 2, top, bw, BIG_BUTTON,
                    GameText.resolve(InstallerScreenTexts.FRAME_INSTALL_NOW), flow.canContinue(),
                    held == Held.NEXT);
            final String copyright = GameText.resolve(InstallerScreenTexts.FRAME_COPYRIGHT.with(
                    Branding.osYear(flow.systemName(), null), Branding.houseOf(flow.systemName()).legalName()));
            Draw.text(g, font, copyright, wx + 16, wy + wh - 12, c.dim());
            final int contentTop = top + BIG_BUTTON + 12;
            return new Frame(wx + 16, contentTop, ww - 32, wy + wh - 16 - contentTop, paint, next, null, null, null);
        }
        for (final String line : wrap(font, GameText.resolve(flow.style().heading(flow.page(), flow.systemName())),
                ww - 32)) {
            Draw.text(g, font, line, wx + 16, top, c.heading());
            top += InstallerLayout.ROW;
        }
        top += 4;
        if (flow.page() == InstallerPage.COPY) {
            Draw.text(g, font, GameText.resolve(InstallerScreenTexts.FRAME_STATUS), wx + 16, top, c.dim());
            top += InstallerLayout.ROW;
            return new Frame(wx + 16, top, ww - 32, wy + wh - 8 - top, paint, null, null, null, null);
        }
        final int by = wy + wh - 8 - BUTTON;
        final int[] next = tenButton(g, font, c, wx + ww - 10 - 56, by, 56, BUTTON,
                GameText.resolve(InstallerScreenTexts.FRAME_NEXT), flow.canContinue(), held == Held.NEXT);
        return new Frame(wx + 16, top, ww - 32, by - 4 - top, paint, next, null, null, null);
    }

    /**
     * Frames 10's first set-up: the whole glass in one blue, the question large across the top of a column in its
     * middle, and a white button at the column's foot.
     */
    private static Frame firstSetup(final GuiGraphics g, final Font font, final InstallerFlow flow, final int sx,
                                    final int sy, final int sw, final int sh, final Held held) {
        final FirstSetup c = FIRST_SETUP.get();
        g.fill(sx, sy, sx + sw, sy + sh, c.ground());
        final int colX = sx + 64;
        final int colW = sw - 128;
        int top = sy + 36;
        final String heading = GameText.resolve(flow.style().heading(flow.page(), flow.systemName()));
        g.pose().pushPose();
        g.pose().translate(colX, top, 0);
        g.pose().scale(HEADING_SCALE, HEADING_SCALE, 1.0F);
        Draw.text(g, font, clip(font, heading, (int) (colW / HEADING_SCALE)), 0, 0, c.text());
        g.pose().popPose();
        top += 26;
        final int by = sy + sh - 34;
        final int bw = 56;
        final boolean done = flow.page() == InstallerPage.DONE;
        final String label = GameText.resolve(done ? InstallerScreenTexts.FRAME_RESTART
                : InstallerScreenTexts.FRAME_NEXT);
        final boolean on = flow.canContinue() || done;
        final int bx = colX + colW - bw;
        g.fill(bx, by, bx + bw, by + BUTTON, on ? held == Held.NEXT ? c.buttonHeld() : c.button() : c.buttonOff());
        Draw.text(g, font, label, bx + (bw - font.width(label)) / 2, by + 3, c.buttonInk());
        final Paint paint = new Paint(c.text(), c.text(), c.dim(), c.accent(), c.select(), c.selectText());
        return new Frame(colX, top, colW, by - 6 - top, paint, new int[]{bx, by, bw, BUTTON}, null, null, null);
    }

    /** A rectangle with its top corners rounded by {@code r}, the way the glass window's top was. */
    private static void roundTop(final GuiGraphics g, final int x, final int y, final int w, final int h,
                                 final int colour, final int r) {
        for (int i = 0; i < r; i++) {
            g.fill(x + r - i, y + i, x + w - r + i, y + i + 1, colour);
        }
        g.fill(x, y + r, x + w, y + h, colour);
    }

    /**
     * Frames 7's push button: grey, lighter above its middle, a rim that turns blue when it is the way on; the big one
     * on the welcome page is blue all over, as its Install now was.
     */
    private static int[] sevenButton(final GuiGraphics g, final Font font, final Glass c, final int x, final int y,
                                     final int w, final int h, final String label, final boolean on,
                                     final boolean held, final boolean big) {
        final int upper = big ? c.bigTop() : held ? c.heldTop() : c.faceTop();
        final int lower = big ? c.bigBottom() : held ? c.heldBottom() : c.faceBottom();
        g.fill(x, y, x + w, y + h / 2, upper);
        g.fill(x, y + h / 2, x + w, y + h, lower);
        outline(g, x, y, w, h, on ? c.focusRim() : c.faceRim());
        Draw.text(g, font, label, x + (w - font.width(label)) / 2, y + (h - 7) / 2, on ? c.text() : c.dim());
        return new int[]{x, y, w, h};
    }

    /** Frames 10's push button: flat grey with a grey rim, the rim blue when it is the way on. */
    private static int[] tenButton(final GuiGraphics g, final Font font, final Metro c, final int x, final int y,
                                   final int w, final int h, final String label, final boolean on,
                                   final boolean held) {
        g.fill(x, y, x + w, y + h, held ? c.buttonHeld() : c.button());
        outline(g, x, y, w, h, on ? c.accent() : c.buttonRim());
        Draw.text(g, font, label, x + (w - font.width(label)) / 2, y + (h - 7) / 2, on ? c.text() : c.dim());
        return new int[]{x, y, w, h};
    }

    /** A sentence broken into the lines that fit {@code room}, at word boundaries. */
    private static List<String> wrap(final Font font, final String text, final int room) {
        final List<String> out = new ArrayList<>();
        final StringBuilder line = new StringBuilder();
        for (final String word : text.split(" ")) {
            final String tried = line.isEmpty() ? word : line + " " + word;
            if (font.width(tried) > room && !line.isEmpty()) {
                out.add(line.toString());
                line.setLength(0);
                line.append(word);
            } else {
                line.setLength(0);
                line.append(tried);
            }
        }
        if (!line.isEmpty()) {
            out.add(line.toString());
        }
        return out;
    }

    /**
     * bsdinstall's own look: a navy ground with the installer's name in its top corner, and a grey dialog box
     * centred on it, its own page named in the top of its border the way the real dialogs are, with an OK-style
     * button (named to fit the page) and, until the copy begins, a Cancel beside it. The copy itself draws no
     * buttons at all: it runs to the end on its own.
     */
    private static Frame dialogBox(final GuiGraphics g, final Font font, final InstallerFlow flow,
                                   final int sx, final int sy, final int sw, final int sh, final Held held,
                                   final boolean asks) {
        final Bsd c = BSD.get();
        g.fill(sx, sy, sx + sw, sy + sh, c.ground());
        Draw.text(g, font, GameText.resolve(flow.style().title(flow.systemName())), sx + 4, sy + 3, c.groundText());
        g.fill(sx + 4, sy + 12, sx + sw - 4, sy + 13, c.groundText());

        final int dx = sx + InstallerLayout.BSD_DIALOG_INSET_X;
        final int dy = sy + InstallerLayout.BSD_DIALOG_INSET_TOP;
        final int dw = sw - 2 * InstallerLayout.BSD_DIALOG_INSET_X;
        final int dh = sh - InstallerLayout.BSD_DIALOG_INSET_BOTTOM;
        final int shadow = InstallerLayout.BSD_DIALOG_SHADOW;
        g.fill(dx + shadow, dy + shadow, dx + dw + shadow, dy + dh + shadow, c.shadow());
        g.fill(dx, dy, dx + dw, dy + dh, c.face());
        edgesBsd(g, dx, dy, dw, dh, c, true);
        final String title = " " + GameText.resolve(flow.style().heading(flow.page(), flow.systemName())) + " ";
        final int titleW = font.width(title);
        final int titleX = dx + (dw - titleW) / 2;
        g.fill(titleX, dy - 4, titleX + titleW, dy + 4, c.face());
        Draw.text(g, font, title, titleX, dy - 3, c.select());

        final boolean copying = flow.page() == InstallerPage.COPY;
        final boolean welcome = flow.page() == InstallerPage.WELCOME;
        final boolean done = flow.page() == InstallerPage.DONE;
        int[] next = null;
        int[] cancel = null;
        if (!copying) {
            /*
             * The plain OK label is padded a couple of spaces either side, the way the real dialogs draw it; a
             * button with a word of its own (Install, Reboot) carries none, and its hotkey is that word's own
             * first letter rather than the padded one.
             */
            final boolean plainOk = !welcome && !done;
            final String okBody = plainOk ? "  " + GameText.resolve(InstallerScreenTexts.BSD_OK_BUTTON) + "  "
                    : GameText.resolve(welcome ? InstallerScreenTexts.BSD_INSTALL_BUTTON
                            : InstallerScreenTexts.FRAME_REBOOT);
            final int okHotkey = plainOk ? 2 : 0;
            final String cancelBody = GameText.resolve(InstallerScreenTexts.FRAME_CANCEL);
            final int by = dy + dh - InstallerLayout.BSD_BUTTON_ROW_DY;
            final boolean canGo = flow.canContinue() || done || asks;
            /*
             * Only the pages before anything is written offer a way out, and only the ones with a question of
             * their own: the hostname page has nowhere else for the player to have come from but the page
             * before it, so it carries none.
             */
            final boolean showCancel = flow.quittable() && (welcome || flow.page() == InstallerPage.COMPONENTS
                    || flow.page() == InstallerPage.DISK || flow.page() == InstallerPage.MIRROR);
            final int nextW = bsdButtonWidth(font, okBody);
            final int cancelW = showCancel ? bsdButtonWidth(font, cancelBody) : 0;
            int cursorX = InstallerLayout.bsdButtonRowX(dx, dw, nextW, cancelW);
            // The button Enter presses is always this one, so it is the one drawn in the dialog's own navy.
            next = bsdButton(g, font, cursorX, by, nextW, okBody, okHotkey, canGo, held == Held.NEXT, true, c);
            if (showCancel) {
                cursorX += nextW + InstallerLayout.BSD_BUTTON_GAP;
                cancel = bsdButton(g, font, cursorX, by, cancelW, cancelBody, 0, true, held == Held.CANCEL, false, c);
            }
        }
        final Paint paint = new Paint(c.text(), c.faceText(), c.dim(), c.hotkey(), c.select(), c.selectText());
        // bsdinstall never draws a Back button of its own: every page's dialog offers only Cancel and its OK.
        return new Frame(dx + InstallerLayout.BSD_DIALOG_CONTENT_X, dy + InstallerLayout.BSD_DIALOG_CONTENT_TOP,
                dw - 2 * InstallerLayout.BSD_DIALOG_CONTENT_X,
                dh - InstallerLayout.BSD_DIALOG_CONTENT_HEIGHT_MARGIN, paint, next, null, cancel, null);
    }

    /**
     * A button in bsdinstall's own dialog style: raised, pressed a pixel in while held, its label between the
     * angle brackets the real dialogs draw one in. The button Enter presses (the default) is drawn in the
     * dialog's own navy with white letters and a yellow hotkey; every other one keeps the plain grey face,
     * black letters and the hotkey's usual red.
     *
     * @param x           the button's own left edge, not a point measured back from the row's right end: the
     *                    row is centred under the dialog rather than hung from its corner
     * @param w           how wide the button stands, from {@link #bsdButtonWidth}, so the caller can lay out
     *                    the whole row before any of it is drawn
     * @param body        the label exactly as it is written between the angle brackets, padding and all
     * @param hotkeyIndex which letter of {@code body} is the one picked out, since a padded label's hotkey
     *                    does not sit at the start of the drawn string the way an unpadded one's does
     */
    private static int[] bsdButton(final GuiGraphics g, final Font font, final int x, final int y, final int w,
                                   final String body, final int hotkeyIndex, final boolean enabled,
                                   final boolean held, final boolean isDefault, final Bsd c) {
        final String rendered = "<" + body + ">";
        g.fill(x, y, x + w, y + BUTTON, isDefault && enabled ? c.select() : c.face());
        edgesBsd(g, x, y, w, BUTTON, c, !held);
        final int nudge = held ? 1 : 0;
        final int ink = !enabled ? c.dim() : isDefault ? c.selectText() : c.faceText();
        final int hotkeyInk = !enabled ? ink : isDefault ? c.hotkeyOn() : c.hotkey();
        int cursor = x + (w - font.width(rendered)) / 2 + nudge;
        final int textY = y + 3 + nudge;
        cursor = drawLetter(g, font, "<", cursor, textY, ink);
        for (int i = 0; i < body.length(); i++) {
            final String letter = body.substring(i, i + 1);
            cursor = drawLetter(g, font, letter, cursor, textY, i == hotkeyIndex && enabled ? hotkeyInk : ink);
        }
        drawLetter(g, font, ">", cursor, textY, ink);
        return new int[]{x, y, w, BUTTON};
    }

    /** How wide a bsdinstall button comes out for that label, the angle brackets and the pad on both sides. */
    private static int bsdButtonWidth(final Font font, final String body) {
        return font.width("<" + body + ">") + 8;
    }

    /** One letter of a button's label, answering where the next one starts. */
    private static int drawLetter(final GuiGraphics g, final Font font, final String letter, final int x,
                                  final int y, final int ink) {
        Draw.text(g, font, letter, x, y, ink);
        return x + font.width(letter);
    }

    /** The navy a field being typed into sits on in bsdinstall's own dialogs, for the hostname page's own field. */
    static int bsdField() {
        return BSD.get().field();
    }

    /** The white a field being typed into is written in there. */
    static int bsdFieldText() {
        return BSD.get().fieldText();
    }

    /** The white a bsdinstall progress gauge is troughed in, empty or full. */
    static int bsdGaugeTrough() {
        return BSD.get().gaugeTrough();
    }

    /** The navy the same gauge fills with as it runs. */
    static int bsdGaugeFill() {
        return BSD.get().gaugeFill();
    }

    /** The green System V's own console names a finished part in. */
    static int systemVDone() {
        return SYSTEM_V_OK.get().done();
    }

    /**
     * The two edges of a bevel in bsdinstall's own grey, since {@link #edges} always reads the wizard's palette:
     * raised for the dialog's own border, which never presses in, and either way for a button, which does.
     */
    private static void edgesBsd(final GuiGraphics g, final int x, final int y, final int w, final int h,
                                 final Bsd c, final boolean raised) {
        final int light = raised ? c.light() : c.dark();
        final int dark = raised ? c.dark() : c.light();
        g.fill(x, y, x + w, y + 1, light);
        g.fill(x, y, x + 1, y + h, light);
        g.fill(x, y + h - 1, x + w, y + h, dark);
        g.fill(x + w - 1, y, x + w, y + h, dark);
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
            /* The later editions draw no page in text, so they borrow the newest one's ink for the screens that ask. */
            case FRAMES_7, FRAMES_10, FRAMES_11 -> FRAMES_11_INK.get();
            case UBUNTU -> UBUNTU_INK.get();
            case DEBIAN -> DEBIAN_INK.get();
            case FEDORA -> FEDORA_INK.get();
            case PLAIN -> PLAIN_INK.get();
            case SYSTEM_V -> SYSTEM_V_INK.get();
            /* bsdinstall paints its own navy ground in dialogBox() and never asks the plain-text ink for it. */
            case BSD_INSTALL -> PLAIN_INK.get();
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
        Draw.text(g, font, label, x + (w - font.width(label)) / 2 + nudge, y + 3 + nudge, c.buttonInk());
        return new int[]{x, y, w, BUTTON};
    }

    /* A modern button has no bevel to turn over, so it answers a press by darkening under the finger. */
    private static int[] primary(final GuiGraphics g, final Font font, final int x, final int y, final int w,
                                 final String label, final boolean on, final boolean held) {
        final Card c = CARD.get();
        g.fill(x, y, x + w, y + BUTTON, on ? (held ? c.primaryHeld() : c.primary()) : c.primaryOff());
        Draw.text(g, font, label, x + (w - font.width(label)) / 2, y + 3, c.primaryInk());
        return new int[]{x, y, w, BUTTON};
    }

    private static int[] pale(final GuiGraphics g, final Font font, final int x, final int y, final int w,
                              final String label, final boolean held) {
        final Card c = CARD.get();
        g.fill(x, y, x + w, y + BUTTON, held ? c.paleHeld() : c.pale());
        outline(g, x, y, w, BUTTON, held ? c.paleHeldEdge() : c.paleEdge());
        Draw.text(g, font, label, x + (w - font.width(label)) / 2, y + 3, c.paleInk());
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

    /**
     * bsdinstall's own colours: the navy ground and its lettering, the grey dialog face and its text, the shadow
     * under it and the two edges of its bevel, a selected row (navy, the way the real dialogs mark one), a field
     * being typed into (navy too, white letters), plain body text and its dimmer note, the red a hotkey letter is
     * picked out in, the white trough and navy fill of the extraction's own gauge, and the yellow a hotkey turns
     * into on the button Enter presses.
     */
    private record Bsd(int ground, int groundText, int face, int faceText, int shadow, int light, int dark,
                       int select, int selectText, int text, int dim, int hotkey, int field, int fieldText,
                       int gaugeTrough, int gaugeFill, int hotkeyOn) {
    }

    /** System V's own console adds one colour to the shared text ink: the green a finished part is named in. */
    private record SystemVOk(int done) {
    }

    /**
     * Frames 7's glass setup: the strip of phases along the foot with the ink of a phase done and the one running,
     * its bar; the window's rim, glass, gloss and the glow behind its title; the white page inside, the edition's
     * name and the headings in blue, the ink of the page; and the buttons, plain and the big blue one.
     */
    private record Glass(int strip, int phaseOn, int phaseDone, int barTrough, int barFill,
                         int rim, int glassTop, int glassBottom, int gloss, int glow, int title,
                         int innerRim, int paper, int brand, int heading,
                         int text, int dim, int accent, int select, int selectText,
                         int faceTop, int faceBottom, int heldTop, int heldBottom, int bigTop, int bigBottom,
                         int faceRim, int focusRim) {
    }

    /**
     * Frames 10's setup window: its blue border and white ground, the title's ink, the edition's name, the headings,
     * the ink of the page, and the flat buttons.
     */
    private record Metro(int border, int window, int title, int brand, int heading,
                         int text, int dim, int accent, int select, int selectText,
                         int button, int buttonHeld, int buttonRim) {
    }

    /**
     * Frames 10's first set-up: the blue, the ink of the page and its quieter lines, the accent and a picked row, and
     * the white button with its states and its blue word.
     */
    private record FirstSetup(int ground, int text, int dim, int accent, int select, int selectText,
                              int button, int buttonHeld, int buttonOff, int buttonInk) {
    }
}
