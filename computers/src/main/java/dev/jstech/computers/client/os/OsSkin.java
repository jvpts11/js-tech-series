/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.os;

import dev.jstech.computers.JsComputers;
import dev.jstech.computers.gui.CdePalette;
import dev.jstech.computers.gui.CdeScheme;
import dev.jstech.computers.os.DesktopEnvironmentDef;
import dev.jstech.computers.os.OsRegistry;
import dev.jstech.core.client.gui.skin.ISkin;
import dev.jstech.core.palette.PaletteHolder;
import dev.jstech.core.palette.Palettes;
import dev.jstech.core.tier.HardwareEra;
import java.util.EnumMap;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

/**
 * The one drawing framework every desktop program paints through, so a program looks like the OS it runs on
 * rather than carrying its own hardcoded chrome. A skin is resolved from the installed desktop. Each skin is a
 * <em>distinct design</em>, not a recolour of one layout: its {@link Form} is drawn by a chrome of its own, and the
 * skin adds the colours that are its alone. It is the core's {@link ISkin}, so the core's components paint through
 * it as well.
 *
 * <ul>
 *   <li>{@link Form#BEVEL} (Frames 95): raised/sunken 3D bevels, a navy title, grey chrome, square.</li>
 *   <li>{@link Form#LUNA} (Frames XP): blue gradients, white title text, green active tab; only the TOP
 *       window corners are rounded (XP kept square bottom corners).</li>
 *   <li>{@link Form#FLAT} (Frames 11 and the modern Linux desktops): light chrome with DARK title text, flat
 *       fills, a thin accent line, all four window corners gently rounded.</li>
 * </ul>
 *
 * <p>Every colour is a declared palette, which a resource pack can recolour: each skin's own,
 * {@code jsc:skin/<skin>}, and each form's chrome, {@code jsc:chrome/<form>}. They are asked for each time something
 * is painted, so a change of pack reaches a program that is already open.
 */
@PaletteHolder
public final class OsSkin implements ISkin {

    /**
     * The shape language a skin draws its primitives in. A period Unix desktop is not the Frames 95
     * bevel in other colours (it has its own geometry) so KDE and GNOME on Legacy hardware each get
     * a form of their own rather than a recolour.
     */
    public enum Form {
        BEVEL, LUNA, FLAT,
        /** KDE of the early 2000s: vertical title gradient, single-pixel borders, soft greys. */
        KDE2,
        /** GNOME of the late 1990s: thick frame, centred title, warm greys, chunky bevelled studs. */
        GNOME1,
        /** Motif, which is CDE: one grey, light and shade for relief, and a palette everything is read from. */
        MOTIF
    }

    /** A window-control glyph, so the control's shape can vary independently of its meaning. */
    public enum Control {
        MINIMIZE, MAXIMIZE, RESTORE, CLOSE
    }

    private final IFormChrome chrome;
    private final Supplier<SkinColours> colours;
    /** The accent chosen for this computer in Settings, over the skin's own, or {@code 0} for none. */
    private final int accentOverride;
    private final int topRadius;
    private final int bottomRadius;
    private final boolean titleShadow;
    /** The desktop environment id path this skin belongs to (the icon set and wallpaper key). */
    private final String desktopPath;

    // Frames 95: classic grey bevel, navy title, square corners.
    static final OsSkin FRAMES_95 = new OsSkin(BevelChrome.INSTANCE, 0, 0, false, "frames_95",
            Palettes.declare(JsComputers.MODID, "skin/frames_95", new SkinColours(
                    0xFFFFFFFF, 0xFFC0C0C0, 0xFF000000, 0xFF000080, 0xFF000000, 0xFF505050, 0xFFFFFFFF,
                    0xFF000080, 0xFFFFFFFF, 0xFFD4D0C8)));

    // Frames XP: Luna blue gradients, white title, cream client; ONLY the top corners are rounded.
    static final OsSkin FRAMES_XP = new OsSkin(LunaChrome.INSTANCE, 2, 0, true, "frames_xp",
            Palettes.declare(JsComputers.MODID, "skin/frames_xp", new SkinColours(
                    0xFFFFFFFF, 0xFFECECF6, 0xFF0831D9, 0xFF2C66BD, 0xFF10203A, 0xFF5A6B85, 0xFFFFFFFF,
                    0xFF2C66BD, 0xFFFFFFFF, 0xFFD8E4FB)));

    // Frames 11: flat light chrome with DARK title text and a thin accent; all corners rounded.
    static final OsSkin FRAMES_11 = new OsSkin(FlatChrome.LIGHT, 2, 2, false, "frames_11",
            Palettes.declare(JsComputers.MODID, "skin/frames_11", new SkinColours(
                    0xFF202434, 0xFFFAFAFE, 0xFFC0C4D2, 0xFF3A6AE0, 0xFF202434, 0xFF6B7488, 0xFFFFFFFF,
                    0xFFE7EEFC, 0xFF1D4ED8, 0xFFF0F1F7)));

    /*
     * Frames 11 (dark): the same flat shape language on a dark slate palette, chosen in Settings. A brighter
     * accent keeps contrast on the dark ground.
     */
    private static final OsSkin FRAMES_11_DARK = new OsSkin(FlatChrome.DARK, 2, 2, false, "frames_11",
            Palettes.declare(JsComputers.MODID, "skin/frames_11_dark", new SkinColours(
                    0xFFE7E9EF, 0xFF1E212A, 0xFF3A4150, 0xFF5B84F0, 0xFFE7E9EF, 0xFF9AA2B2, 0xFF14171F,
                    0xFF2A3A63, 0xFFDCE7FF, 0xFF262B36)));

    /*
     * The Linux desktop environments: flat chrome like Frames 11, each in its own palette and accent.
     * KDE Plasma (Breeze): light grey window, sky-blue accent.
     */
    static final OsSkin KDE_PLASMA = new OsSkin(FlatChrome.LIGHT, 2, 2, false, "kde_plasma",
            Palettes.declare(JsComputers.MODID, "skin/kde_plasma", new SkinColours(
                    0xFF232629, 0xFFEFF0F1, 0xFFB9BFC8, 0xFF3DAEE9, 0xFF232629, 0xFF6E7680, 0xFFFCFCFC,
                    0xFFD6ECF7, 0xFF1F6F9A, 0xFFE6EBEF)));

    // GNOME (Adwaita): warm light window, GNOME blue accent, rounded.
    static final OsSkin GNOME = new OsSkin(FlatChrome.LIGHT, 3, 3, false, "gnome",
            Palettes.declare(JsComputers.MODID, "skin/gnome", new SkinColours(
                    0xFF2E3436, 0xFFF6F5F4, 0xFFC0BFBC, 0xFF3584E4, 0xFF2E3436, 0xFF77767B, 0xFFFFFFFF,
                    0xFFDCE8FA, 0xFF1C5FB4, 0xFFEBEBEA)));

    // Cinnamon (Mint-Y): light grey window, Mint green accent.
    static final OsSkin CINNAMON = new OsSkin(FlatChrome.LIGHT, 2, 2, false, "cinnamon",
            Palettes.declare(JsComputers.MODID, "skin/cinnamon", new SkinColours(
                    0xFF2B2B2B, 0xFFF7F7F7, 0xFFB0B0B0, 0xFF69B03B, 0xFF2B2B2B, 0xFF6E6E6E, 0xFFFFFFFF,
                    0xFFDFF0D4, 0xFF3C6E1E, 0xFFEBEBEB)));

    /*
     * The same desktops as they looked on Legacy-era hardware. These are not the modern skins in older
     * colours: each carries its own geometry, because that is what actually told the two apart at the
     * time. KDE ran cold blue-grey with vertical gradients and hairline borders; GNOME ran warm putty
     * with a thick frame and a centred title, since a separate window manager drew its decoration.
     */

    static final OsSkin KDE_PLASMA_LEGACY = new OsSkin(Kde2Chrome.INSTANCE, 0, 0, true, "kde_plasma",
            Palettes.declare(JsComputers.MODID, "skin/kde_plasma_legacy", new SkinColours(
                    0xFFFFFFFF, 0xFFD6D2CD, 0xFF6F6A64, 0xFF1D4C80, 0xFF1A1A1A, 0xFF5F5A54, 0xFFFFFFFF,
                    0xFF33679F, 0xFFFFFFFF, 0xFFC7C2BB)));

    static final OsSkin GNOME_LEGACY = new OsSkin(Gnome1Chrome.INSTANCE, 0, 0, true, "gnome",
            Palettes.declare(JsComputers.MODID, "skin/gnome_legacy", new SkinColours(
                    0xFFFFFFFF, 0xFFD6D2C8, 0xFFB0AA9C, 0xFF6D5A78, 0xFF1A1A1A, 0xFF5C574E, 0xFFFFFFFF,
                    0xFF6D5A78, 0xFFFFFFFF, 0xFFC4BFB2)));

    /** CDE in each of its schemes, made once, so choosing one in the Style Manager builds nothing. */
    private static final Map<CdeScheme, OsSkin> MOTIF = motifSkins();

    /* CDE in the scheme it ships with; the Style Manager's choice is another of the same. */
    static final OsSkin CDE = motif(CdeScheme.DEFAULT);

    private OsSkin(final IFormChrome chrome, final int topRadius, final int bottomRadius, final boolean titleShadow,
                   final String desktopPath, final Supplier<SkinColours> colours) {
        this(chrome, topRadius, bottomRadius, titleShadow, desktopPath, colours, 0);
    }

    private OsSkin(final IFormChrome chrome, final int topRadius, final int bottomRadius, final boolean titleShadow,
                   final String desktopPath, final Supplier<SkinColours> colours, final int accentOverride) {
        this.chrome = chrome;
        this.topRadius = topRadius;
        this.bottomRadius = bottomRadius;
        this.titleShadow = titleShadow;
        this.desktopPath = desktopPath;
        this.colours = colours;
        this.accentOverride = accentOverride;
    }

    /** The skin for a desktop as it looks on hardware of {@code era}, as its look says. */
    public static OsSkin forDesktop(final ResourceLocation desktopId, final HardwareEra era) {
        return DesktopLook.of(desktopId).skinOn(era);
    }

    /** The skin of the desktop under that id, as its look names it. */
    public static OsSkin forDesktop(final ResourceLocation desktopId) {
        return DesktopLook.of(desktopId).skin();
    }

    /**
     * CDE drawn from that scheme. Everything such a skin answers, the frames, the text, the wells and what is
     * picked out in a list, is one of the scheme's colours, so choosing another scheme changes all of it.
     */
    public static OsSkin motif(final CdeScheme scheme) {
        return MOTIF.get(scheme);
    }

    /** A safe default skin (Frames 95) for a field that needs a non-null value before the first render. */
    public static OsSkin fallback() {
        return FRAMES_95;
    }

    /**
     * Returns this skin with its accent replaced by {@code argb} (a per-computer override chosen in
     * Settings), or this skin unchanged when {@code argb} is {@code 0} (no override) or already the
     * current accent. Every control the skin draws (primary buttons, tab and field accents, the flat
     * list-selection bar) then uses the chosen colour.
     *
     * @param argb the override accent as an opaque ARGB int, or {@code 0} to keep the skin default
     * @return a skin using the override accent, or {@code this}
     */
    public OsSkin withAccent(final int argb) {
        // A Motif skin has no accent of its own to override: its colours are its scheme's, all of them.
        if (argb == 0 || argb == accent() || form() == Form.MOTIF) {
            return this;
        }
        return new OsSkin(chrome, topRadius, bottomRadius, titleShadow, desktopPath, colours, argb);
    }

    /**
     * The dark-theme counterpart of this skin, or {@code this} when the skin has no dark variant. Only the flat
     * Frames 11 skin defines one; the earlier editions have no historical dark mode, so they are returned as-is.
     */
    public OsSkin darkVariant() {
        return form() == Form.FLAT ? FRAMES_11_DARK : this;
    }

    /** Whether this is the dark-theme variant. */
    public boolean isDark() {
        return chrome.dark();
    }

    /** The desktop environment id path this skin represents (frames_95/xp/11, kde_plasma, gnome, cinnamon). */
    public String osPath() {
        return desktopPath;
    }

    /**
     * Whether the desktop this skin draws stands on a Unix family, which sees one tree from {@code /} rather
     * than lettered drives. The desktop says so through its panel style, so a program asks here instead of
     * keeping a list of desktop names that the next desktop is missing from.
     */
    public boolean unixLike() {
        final DesktopEnvironmentDef desktop =
                OsRegistry.getDesktop(ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, desktopPath));
        return desktop != null && desktop.panelStyle().unixLike();
    }

    /**
     * The program-icon set this skin draws with: its own desktop's, and the period set for the two Legacy
     * Unix desktops, which have artwork of their own age.
     */
    public String iconSet() {
        return form() == Form.KDE2 || form() == Form.GNOME1
                ? desktopPath + ProgramIcons.PERIOD_SUFFIX
                : desktopPath;
    }

    public Form form() {
        return chrome.form();
    }

    @Override
    public int accent() {
        return accentOverride != 0 ? accentOverride : colours.get().accent();
    }

    @Override
    public int text() {
        return colours.get().text();
    }

    @Override
    public int dim() {
        return colours.get().dim();
    }

    @Override
    public int windowBg() {
        return colours.get().windowBg();
    }

    public int windowBorder() {
        return colours.get().windowBorder();
    }

    public int titleText() {
        return colours.get().titleText();
    }

    public boolean textShadow() {
        return titleShadow;
    }

    /** The fill of a content panel (grey 95 / cream XP / white 11). */
    @Override
    public int panelBg() {
        return chrome.panelFill(this);
    }

    /** The fill of a text field. */
    @Override
    public int fieldBg() {
        return colours.get().fieldBg();
    }

    /** A 1px border/separator colour for panels and bands, per skin. */
    @Override
    public int edge() {
        return chrome.edge(this);
    }

    /** The hover-row background. */
    @Override
    public int listHover() {
        return colours.get().listHover();
    }

    /** The selected-row background, which is what the text of a selected row is written on. */
    public int listSelect() {
        return colours.get().listSelect();
    }

    // window chrome

    /**
     * The window body background and its outer border, with the skin's per-corner rounding (XP rounds only the
     * top; 11 rounds all four; 95 none). Rounded corner pixels are left unpainted, so they show what is behind.
     */
    @Override
    public void windowFrame(final GuiGraphics g, final int x, final int y, final int w, final int h) {
        chrome.windowFrame(g, this, x, y, w, h);
    }

    /** The title bar fill (solid navy / Luna gradient / flat light), rounding only the top corners. */
    public void titleBar(final GuiGraphics g, final int x, final int y, final int w, final int titleH) {
        titleBar(g, x, y, w, titleH, true);
    }

    /**
     * A window's title bar, lit when the window has focus and greyed when it does not. Every desktop
     * this mod imitates says which window is in front the same way (the active bar keeps its colour
     * and the rest fall back to a flat grey) and without that a stack of open programs gives the
     * player nothing to read.
     */
    public void titleBar(final GuiGraphics g, final int x, final int y, final int w, final int titleH,
                         final boolean active) {
        titleBar(g, x, y, w, titleH, active, 0, 0);
    }

    /**
     * The same, told how much of each end of the bar the window's buttons take. Only Motif asks: it lights the
     * strip the title is written on and leaves the buttons the frame's own grey, so it has to know where the
     * strip starts and stops. Every other form colours the whole bar and draws its buttons over it.
     */
    public void titleBar(final GuiGraphics g, final int x, final int y, final int w, final int titleH,
                         final boolean active, final int left, final int right) {
        chrome.titleBar(g, this, x, y, w, titleH, active, left, right);
    }

    /**
     * Whether this skin centres a window title. Only the GNOME and Motif forms do, which is exactly why they
     * read as different desktops rather than repainted ones.
     */
    public boolean titleCentered() {
        return chrome.titleCentered();
    }

    /**
     * Whether a window's one way out sits at the LEFT end of its bar, as Motif's menu button does, with
     * minimise and maximise alone at the right. Every other form keeps all three at the right.
     */
    public boolean menuAtLeft() {
        return chrome.menuAtLeft();
    }

    /** The thickness of the window frame in pixels; the GNOME and Motif forms draw a chunky border. */
    public int frameThickness() {
        return chrome.frameThickness();
    }

    /** One title-bar control, shaped per skin, with a real pressed state so a click reads. */
    public void windowControl(final GuiGraphics g, final Font font, final int x, final int y, final int bw,
                              final int bh, final Control control, final boolean hovered, final boolean pressed) {
        chrome.control(g, font, this, x, y, bw, bh, control, hovered, pressed);
    }

    // widgets (used by the programs' content)

    /** A group panel/box: sunken bevel (95), soft border (XP), or hairline (11). */
    @Override
    public void panel(final GuiGraphics g, final int x, final int y, final int w, final int h) {
        chrome.panel(g, this, x, y, w, h);
    }

    /** A push button, optionally the primary/default one, with a pressed state. */
    @Override
    public void button(final GuiGraphics g, final Font font, final int x, final int y, final int w, final int h,
                       final String label, final boolean hovered, final boolean pressed, final boolean primary) {
        chrome.button(g, this, x, y, w, h, hovered, pressed, primary);
        g.drawString(font, label, x + (w - font.width(label)) / 2, y + (h - 7) / 2 + (pressed ? 1 : 0),
                chrome.buttonText(this, primary), false);
    }

    /** A text input field. */
    @Override
    public void field(final GuiGraphics g, final int x, final int y, final int w, final int h,
                      final boolean focused) {
        g.fill(x, y, x + w, y + h, fieldBg());
        chrome.field(g, this, x, y, w, h, focused);
    }

    /** A tab in a tab strip. */
    @Override
    public void tab(final GuiGraphics g, final Font font, final int x, final int y, final int w, final int h,
                    final String label, final boolean active) {
        chrome.tab(g, this, x, y, w, h, active);
        g.drawString(font, label, x + (w - font.width(label)) / 2, y + (h - 7) / 2, chrome.tabText(this, active),
                false);
    }

    /** A list/grid row background for the hover and selection states. */
    @Override
    public void listRow(final GuiGraphics g, final int x, final int y, final int w, final int h,
                        final boolean hovered, final boolean selected) {
        if (selected) {
            g.fill(x, y, x + w, y + h, listSelect());
            if (chrome.selectionBar()) {
                g.fill(x, y, x + 3, y + h, accent());
            }
        } else if (hovered) {
            g.fill(x, y, x + w, y + h, listHover());
        }
    }

    /** The text colour for a list row, given its selection state. */
    @Override
    public int listRowText(final boolean selected) {
        return selected ? colours.get().listSelectText() : text();
    }

    /** A scrollbar thumb. */
    @Override
    public void scrollThumb(final GuiGraphics g, final int x, final int y, final int w, final int h) {
        chrome.scrollThumb(g, this, x, y, w, h);
    }

    /** A status/footer bar background. */
    @Override
    public void statusBar(final GuiGraphics g, final int x, final int y, final int w, final int h) {
        chrome.statusBar(g, this, x, y, w, h);
    }

    /**
     * A soft drop shadow behind a window, so it lifts off the wallpaper. Drawn just before the window frame at
     * the same depth: the offset fringe stays visible while the opaque body covers the rest.
     */
    public void windowShadow(final GuiGraphics g, final int x, final int y, final int w, final int h) {
        final int spread = chrome.shadowSpread();
        final int base = chrome.shadowStrength();
        /*
         * Farther layers are lighter; nearer layers stack on top, so the fringe just outside the window is
         * darkest and it fades out toward the edge.
         */
        for (int i = spread; i >= 1; i--) {
            final int a = Math.min(0xFF, base * (spread - i + 1));
            g.fill(x + i, y + i + 1, x + w + i, y + h + i + 1, a << 24);
        }
    }

    int topRadius() {
        return topRadius;
    }

    int bottomRadius() {
        return bottomRadius;
    }

    private static Map<CdeScheme, OsSkin> motifSkins() {
        final Map<CdeScheme, OsSkin> out = new EnumMap<>(CdeScheme.class);
        for (final CdeScheme scheme : CdeScheme.ALL) {
            out.put(scheme, new OsSkin(new MotifFormChrome(scheme), 0, 0, false, "cde",
                    Palettes.derive(() -> fromCde(scheme.colours()))));
        }
        return out;
    }

    /** The skin's colours out of a CDE scheme, each one of the scheme's own. */
    private static SkinColours fromCde(final CdePalette p) {
        return new SkinColours(p.activeInk(), p.window(), p.shade(), p.active(), p.ink(), p.shade(), p.inset(),
                p.active(), p.activeInk(), p.inset());
    }
}
