/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.os;

import dev.jstech.computers.JsComputers;
import dev.jstech.computers.audio.SoundOutput;
import dev.jstech.computers.gui.layout.VolumePopupLayout;
import dev.jstech.computers.gui.layout.VolumePopupLayout.Geometry;
import dev.jstech.computers.gui.layout.VolumePopupLayout.Look;
import dev.jstech.computers.gui.layout.VolumePopupLayout.Rect;
import dev.jstech.computers.operation.payload.RequestSettingsPayload;
import dev.jstech.computers.operation.payload.SetSettingPayload;
import dev.jstech.computers.operation.payload.SettingsSnapshotPayload;
import dev.jstech.computers.os.PanelStyle;
import dev.jstech.core.client.gui.component.ContextMenu;
import dev.jstech.core.client.gui.component.Draw;
import dev.jstech.core.client.gui.component.UiContext;
import dev.jstech.core.palette.Palette;
import dev.jstech.core.palette.PaletteHolder;
import dev.jstech.core.palette.Palettes;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.TextKey;
import net.minecraft.Util;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * The volume control a system opens from the speaker on its panel, in the form its desktop gives it: the small popup
 * with an upright slider of the older Frames, the quick settings of the newest, the Plasma and Cinnamon applets, the
 * GNOME system menu, and the period popup with its Mixer button. Every one of them turns the same three settings of
 * the machine, its volume, whether it is muted and where its sound goes; the right button on the speaker opens a menu
 * of one entry that goes to the Sound settings instead.
 *
 * <p>A change is shown at once and sent to the machine, whose answer is not let back over it for a moment, so a
 * slider being dragged does not jump back to a value already left behind.
 *
 * <p>Its own colours, those of the GNOME menu that are not its skin's, are the palette {@code jsc:panel/volume}.
 */
@PaletteHolder
final class VolumePopup {

    private final DesktopScreen desktop;
    private final ContextMenu menu = new ContextMenu(MENU_W, MENU_ITEM_H);
    private boolean open;
    /** Whether the outputs are unfolded, on a control that keeps them behind an arrow. */
    private boolean expanded;
    private boolean dragging;
    private SettingsSnapshotPayload.Sound sound = SettingsSnapshotPayload.Sound.NONE;
    private int volume = 100;
    private boolean muted;
    private SoundOutput output = SoundOutput.BOTH;
    /** Until when the machine's answers are not let over what the player just set. */
    private long localUntil;
    private long lastSent;
    private int sentVolume = -1;
    /** Where the control was last drawn and how, so a click reads the same places. */
    private int originX;
    private int originY;
    @Nullable
    private Geometry geometry;

    private static final int MENU_W = 96;
    private static final int MENU_ITEM_H = 12;
    private static final int WHEEL_STEP = 5;
    private static final long SEND_EVERY_MILLIS = 100L;
    private static final long SETTLE_MILLIS = 700L;
    private static final int TICKS = 9;
    /** The outputs in the order every control lists them: the monitor, the speakers, then both. */
    private static final List<SoundOutput> LISTED = List.of(SoundOutput.MONITOR, SoundOutput.SPEAKERS,
            SoundOutput.BOTH);
    private static final Palette<Colours> PALETTE = Palettes.declare(JsComputers.MODID, "panel/volume",
            new Colours(0xFF26262B, 0xFF3D3D45, 0xFFEDEDF0, 0xFF9A9AA4, 0xFF303036, 0xFF3A3A42, 0xFF55555E,
                    0xFFFFFFFF));

    VolumePopup(final DesktopScreen desktop) {
        this.desktop = desktop;
    }

    /** Takes the machine's word on its sound, unless the player has just set it otherwise. */
    void accept(final SettingsSnapshotPayload.Sound state) {
        sound = state;
        if (dragging || Util.getMillis() < localUntil) {
            return;
        }
        volume = state.volume();
        muted = state.muted();
        final SoundOutput chosen = SoundOutput.byId(state.output());
        output = chosen == null ? SoundOutput.BOTH : chosen;
    }

    /** Asks the machine for its sound as it is, which it answers like any other settings request. */
    void requestState() {
        PacketDistributor.sendToServer(new RequestSettingsPayload(desktop.host()));
    }

    boolean muted() {
        return muted;
    }

    int volume() {
        return volume;
    }

    SoundOutput output() {
        return output;
    }

    /** Sets all three at once and tells the machine, the way a dialog with an OK button does. */
    void apply(final int newVolume, final boolean newMuted, final SoundOutput newOutput) {
        settle();
        if (newVolume != volume) {
            volume = Math.max(0, Math.min(100, newVolume));
            send();
        }
        if (newMuted != muted) {
            muted = newMuted;
            set("mute", muted ? "on" : "off");
        }
        if (newOutput != output) {
            output = newOutput;
            set("output", output.id());
        }
    }

    /** What the panel's tip says while the cursor rests on the speaker. */
    String tip() {
        return GameText.resolve(muted ? VolumeTexts.MUTED.text() : VolumeTexts.VOLUME_AT.with(volume));
    }

    /** Whether the control or its menu is up, and takes the next click. */
    boolean isOpen() {
        return open || menu.isOpen();
    }

    /** Whether the control itself is up, rather than its menu. */
    boolean controlOpen() {
        return open;
    }

    boolean menuOpen() {
        return menu.isOpen();
    }

    /** The speaker was clicked: the control comes up, or goes if it was up. */
    void toggle() {
        menu.close();
        open = !open;
        if (open) {
            requestState();
        }
    }

    /**
     * The right button on the speaker: a menu of one entry, which opens the Sound settings, above a bottom panel or
     * under a top bar.
     */
    void openMenu(final int x, final int panelY, final boolean topBar) {
        open = false;
        final int y = topBar ? DesktopScreen.TASKBAR_H + 2 : panelY - MENU_ITEM_H - 4;
        menu.open(List.of(new ContextMenu.Item(GameText.resolve(VolumeTexts.SOUND_SETTINGS), true,
                desktop::openSoundSettings)), x, y, 0, 0, desktop.screenW(), desktop.screenH());
    }

    /** Whether a desktop-local point is on the open control. */
    boolean over(final double mx, final double my) {
        final Geometry geo = geometry;
        return open && geo != null && mx >= originX && my >= originY && mx < originX + geo.width()
                && my < originY + geo.height();
    }

    /**
     * The desktop-local centre of a part of the open control, for the client tests: {@code track} at the volume
     * {@code index}, {@code mute}, {@code chevron}, {@code output} number {@code index}, {@code footer}, or the
     * menu's {@code entry}; null when the control has no such part or is not up.
     */
    @Nullable
    int[] pointOf(final String part, final int index) {
        if ("entry".equals(part)) {
            return menu.isOpen() ? menu.itemCenter(0) : null;
        }
        final Geometry geo = geometry;
        if (!open || geo == null) {
            return null;
        }
        final Rect rect = switch (part) {
            case "mute" -> geo.mute();
            case "chevron" -> geo.chevron();
            case "footer" -> geo.footer();
            case "output" -> index < geo.outputs().size() ? geo.outputs().get(index) : Rect.NONE;
            default -> Rect.NONE;
        };
        if ("track".equals(part)) {
            final int at = VolumePopupLayout.thumbAt(geo, index);
            final Rect t = geo.track();
            return geo.upright() ? new int[] {originX + t.x() + t.w() / 2, originY + at}
                    : new int[] {originX + at, originY + t.y() + t.h() / 2};
        }
        return rect.present() ? new int[] {originX + rect.x() + rect.w() / 2, originY + rect.y() + rect.h() / 2}
                : null;
    }

    void close() {
        open = false;
        dragging = false;
        menu.close();
    }

    /** A turn of the wheel over the speaker: up or down by a step for each notch. */
    void nudge(final int notches) {
        setVolume(volume + notches * WHEEL_STEP);
        send();
    }

    /** The control this desktop opens, or null on one with no speaker on its panel. */
    @Nullable
    Look look() {
        final OsSkin.Form form = desktop.panelSkin().form();
        if (form == OsSkin.Form.KDE2 || form == OsSkin.Form.GNOME1) {
            return Look.PERIOD;
        }
        final PanelStyle style = desktop.panelStyle();
        return switch (style) {
            case FRAMES_95, FRAMES_XP -> Look.CLASSIC;
            case FRAMES_11 -> Look.QUICK;
            case KDE -> Look.PLASMA;
            case GNOME -> Look.SYSTEM_MENU;
            case CINNAMON -> Look.APPLET;
            case CDE -> null;
        };
    }

    /**
     * Draws the control over the panel it came from: above a bottom panel, under a top bar, against the right edge
     * where the speaker is.
     */
    void render(final GuiGraphics g, final UiContext ctx, final int sw, final int tbY, final boolean topBar) {
        if (menu.isOpen()) {
            menu.render(g, ctx);
        }
        final Look look = look();
        if (!open || look == null) {
            return;
        }
        final Font font = ctx.font();
        final Geometry geo = VolumePopupLayout.of(look, expanded, labels(look), font::width);
        geometry = geo;
        final int margin = look == Look.QUICK || look == Look.SYSTEM_MENU ? 4 : 2;
        originX = Math.max(2, sw - margin - geo.width());
        originY = topBar ? DesktopScreen.TASKBAR_H + 3 : tbY - (look == Look.QUICK ? 4 : 1) - geo.height();
        final int mx = ctx.mouseX() - originX;
        final int my = ctx.mouseY() - originY;
        g.pose().pushPose();
        g.pose().translate(originX, originY, 0);
        final OsSkin skin = desktop.panelSkin();
        switch (look) {
            case CLASSIC, PERIOD -> drawUpright(g, font, skin, geo, mx, my);
            case SYSTEM_MENU -> drawSystemMenu(g, font, geo, mx, my);
            default -> drawPanelApplet(g, font, skin, geo, mx, my);
        }
        g.pose().popPose();
    }

    /**
     * A click while the control or its menu is up. Whatever it lands on, it goes no further: on the control it
     * turns what it lands on, anywhere else it puts the control away, the way every one of these did.
     */
    void mouseClicked(final double mx, final double my, final int button) {
        if (menu.isOpen()) {
            menu.mouseClicked(mx, my, button);
            return;
        }
        final Geometry geo = geometry;
        final double x = mx - originX;
        final double y = my - originY;
        if (geo == null || x < 0 || y < 0 || x >= geo.width() || y >= geo.height()) {
            close();
            return;
        }
        if (button != 0) {
            return;
        }
        if (onTrack(geo, x, y)) {
            dragging = true;
            setVolume(VolumePopupLayout.volumeAt(geo, x, y));
            send();
            return;
        }
        if (geo.mute().contains(x, y)) {
            muted = !muted;
            settle();
            set("mute", muted ? "on" : "off");
            return;
        }
        if (geo.chevron().contains(x, y)) {
            expanded = !expanded;
            return;
        }
        for (int i = 0; i < geo.outputs().size(); i++) {
            if (geo.outputs().get(i).contains(x, y)) {
                output = LISTED.get(i);
                settle();
                set("output", output.id());
                return;
            }
        }
        if (geo.footer().contains(x, y)) {
            close();
            desktop.openSoundSettings();
        }
    }

    /** A drag that began on the slider moves it; true while one is under way. */
    boolean mouseDragged(final double mx, final double my) {
        final Geometry geo = geometry;
        if (!dragging || geo == null) {
            return false;
        }
        setVolume(VolumePopupLayout.volumeAt(geo, mx - originX, my - originY));
        if (Util.getMillis() - lastSent >= SEND_EVERY_MILLIS) {
            send();
        }
        return true;
    }

    /** The drag ends: the value it ended on is sent, whatever the pace of the ones before it. */
    void mouseReleased() {
        if (dragging) {
            dragging = false;
            send();
        }
    }

    /* Everything the control shows, in the player's language, for the look that shows it. */
    private VolumePopupLayout.Labels labels(final Look look) {
        final List<String> outputs = List.of(words(VolumeTexts.OUTPUT_MONITOR), words(VolumeTexts.OUTPUT_SPEAKERS),
                words(VolumeTexts.OUTPUT_BOTH));
        final String names = speakerNames();
        return switch (look) {
            case CLASSIC -> new VolumePopupLayout.Labels(words(VolumeTexts.VOLUME), words(VolumeTexts.MUTE), "",
                    outputs, names, "");
            case PERIOD -> new VolumePopupLayout.Labels(words(VolumeTexts.VOLUME), words(VolumeTexts.MUTE), "",
                    outputs, names, words(VolumeTexts.MIXER));
            case QUICK -> new VolumePopupLayout.Labels("", "", words(VolumeTexts.SOUND_OUTPUT), outputs, names,
                    words(VolumeTexts.MORE_SOUND_SETTINGS));
            case PLASMA -> new VolumePopupLayout.Labels(words(VolumeTexts.AUDIO_VOLUME), "",
                    words(VolumeTexts.PLAY_THROUGH), outputs, names, words(VolumeTexts.CONFIGURE_DEVICES));
            case SYSTEM_MENU -> new VolumePopupLayout.Labels("", "", words(VolumeTexts.SOUND_OUTPUT_TITLE), outputs,
                    names, words(VolumeTexts.SOUND_SETTINGS_TITLE));
            case APPLET -> new VolumePopupLayout.Labels(GameText.resolve(VolumeTexts.VOLUME_AT.with(100)),
                    words(VolumeTexts.MUTE_OUTPUT), words(VolumeTexts.OUTPUT_DEVICE), outputs, names,
                    words(VolumeTexts.SOUND_SETTINGS_TITLE));
        };
    }

    /* The speakers' names as the Speakers row lists them, or that there are none. */
    private String speakerNames() {
        if (sound.speakers().isEmpty()) {
            return words(VolumeTexts.NO_SPEAKERS);
        }
        final List<String> names = new ArrayList<>();
        for (final SettingsSnapshotPayload.SpeakerRow speaker : sound.speakers()) {
            names.add(speaker.name().isEmpty() ? words(VolumeTexts.UNNAMED_SPEAKER) : speaker.name());
        }
        return String.join(", ", names);
    }

    /* The upright popup of the older Frames and the period desktops. */
    private void drawUpright(final GuiGraphics g, final Font font, final OsSkin skin, final Geometry geo,
                             final int mx, final int my) {
        final boolean raised = skin.form() != OsSkin.Form.LUNA;
        final int ground = raised ? skin.panelBg() : skin.windowBg();
        if (raised) {
            skin.button(g, font, 0, 0, geo.width(), geo.height(), "", false, false, false);
        } else {
            g.fill(0, 0, geo.width(), geo.height(), skin.windowBg());
            Draw.outline(g, 0, 0, geo.width(), geo.height(), skin.edge());
        }
        final String title = words(VolumeTexts.VOLUME);
        Draw.text(g, font, title, (geo.width() - font.width(title)) / 2, geo.title().y(), skin.text(), ground);
        final Rect t = geo.track();
        skin.field(g, t.x(), t.y(), t.w(), t.h(), false);
        for (int i = 0; i < TICKS; i++) {
            final int ty = t.y() + 3 + i * (t.h() - 6) / (TICKS - 1);
            g.fill(t.x() - 9, ty, t.x() - 6, ty + 1, skin.text());
        }
        final int at = VolumePopupLayout.thumbAt(geo, volume);
        skin.button(g, font, t.x() - 6, at - 4, 17, 9, "", onTrack(geo, mx, my), dragging, false);
        final Rect m = geo.mute();
        skin.field(g, m.x() + 2, m.y() + 2, 10, 10, false);
        if (muted) {
            check(g, m.x() + 4, m.y() + 3, skin.text());
        }
        Draw.text(g, font, words(VolumeTexts.MUTE), m.x() + 14, m.y() + 3, skin.text(), ground);
        final Rect f = geo.footer();
        if (f.present()) {
            skin.button(g, font, f.x(), f.y(), f.w(), f.h(), words(VolumeTexts.MIXER), f.contains(mx, my), false,
                    false);
        }
    }

    /* The flat applets: the newest Frames' quick settings, Plasma's and Cinnamon's. */
    private void drawPanelApplet(final GuiGraphics g, final Font font, final OsSkin skin, final Geometry geo,
                                 final int mx, final int my) {
        final int ground = skin.windowBg();
        g.fill(0, 0, geo.width(), geo.height(), ground);
        Draw.outline(g, 0, 0, geo.width(), geo.height(), skin.windowBorder());
        for (final int rule : geo.rules()) {
            g.fill(1, rule, geo.width() - 1, rule + 1, skin.windowBorder());
        }
        final Look look = geo.look();
        if (geo.title().present()) {
            final String title = look == Look.APPLET ? GameText.resolve(VolumeTexts.VOLUME_AT.with(volume))
                    : words(VolumeTexts.AUDIO_VOLUME);
            Draw.text(g, font, title, geo.title().x(), geo.title().y(), skin.text(), ground);
        }
        final Rect icon = geo.icon();
        PanelTray.speaker(g, icon.x(), icon.y(), skin.text(), muted);
        slider(g, geo, skin.windowBorder(), skin.accent(), skin.fieldBg(), look == Look.QUICK);
        if (geo.percent().present()) {
            final String percent = volume + "%";
            Draw.text(g, font, percent, geo.percent().right() - font.width(percent), geo.percent().y(), skin.text(),
                    ground);
        }
        final Rect m = geo.mute();
        if (look == Look.PLASMA) {
            skin.button(g, font, m.x(), m.y(), m.w(), m.h(), "", m.contains(mx, my), muted, false);
            PanelTray.speaker(g, m.x() + 2, m.y() + 2, skin.text(), true);
        } else if (look == Look.APPLET) {
            Draw.text(g, font, words(VolumeTexts.MUTE_OUTPUT), m.x() + 5, m.y() + 2, skin.text(), ground);
            final int sx = m.right() - 21;
            g.fill(sx, m.y() + 1, sx + 16, m.y() + 10, muted ? skin.accent() : skin.windowBorder());
            final int knob = muted ? sx + 8 : sx + 1;
            g.fill(knob, m.y() + 2, knob + 7, m.y() + 9, skin.fieldBg());
        }
        final Rect c = geo.chevron();
        if (c.present()) {
            g.fill(c.x(), c.y(), c.right(), c.bottom(), skin.listHover());
            Draw.text(g, font, expanded ? "v" : ">", c.x() + 7, c.y() + 3, skin.text(), skin.listHover());
        }
        if (geo.heading().present()) {
            final TextKey heading = switch (look) {
                case PLASMA -> VolumeTexts.PLAY_THROUGH;
                case APPLET -> VolumeTexts.OUTPUT_DEVICE;
                default -> VolumeTexts.SOUND_OUTPUT;
            };
            Draw.text(g, font, words(heading), geo.heading().x(), geo.heading().y(),
                    look == Look.QUICK ? skin.text() : skin.dim(), ground);
        }
        final int indent = look == Look.APPLET ? 9 : 6;
        for (int i = 0; i < geo.outputs().size(); i++) {
            final Rect row = geo.outputs().get(i);
            final boolean chosen = LISTED.get(i) == output;
            skin.listRow(g, row.x(), row.y(), row.w(), row.h(), row.contains(mx, my), chosen);
            final int rowGround = chosen ? skin.listSelect() : ground;
            Draw.text(g, font, outputWords(i), row.x() + indent, row.y() + 2, skin.listRowText(chosen), rowGround);
            if (i == 1) {
                Draw.text(g, font, speakerNames(), row.x() + indent, row.y() + 11, skin.dim(), rowGround);
            }
        }
        final Rect f = geo.footer();
        switch (look) {
            case PLASMA -> skin.button(g, font, f.x(), f.y(), f.w(), f.h(), words(VolumeTexts.CONFIGURE_DEVICES),
                    f.contains(mx, my), false, false);
            case QUICK -> Draw.text(g, font, words(VolumeTexts.MORE_SOUND_SETTINGS), f.x(), f.y() + 1, skin.accent(),
                    ground);
            default -> Draw.text(g, font, words(VolumeTexts.SOUND_SETTINGS_TITLE), f.x(), f.y(), skin.text(),
                    ground);
        }
    }

    /* GNOME's system menu, dark whatever the theme of its windows, as the shell's own menus are. */
    private void drawSystemMenu(final GuiGraphics g, final Font font, final Geometry geo, final int mx,
                                final int my) {
        final Colours c = PALETTE.get();
        final int accent = desktop.panelSkin().accent();
        g.fill(0, 0, geo.width(), geo.height(), c.menuFill());
        Draw.outline(g, 0, 0, geo.width(), geo.height(), c.menuEdge());
        final Rect icon = geo.icon();
        PanelTray.speaker(g, icon.x(), icon.y(), c.menuInk(), muted);
        slider(g, geo, c.menuTrough(), accent, c.menuThumb(), false);
        final Rect ch = geo.chevron();
        g.fill(ch.x(), ch.y(), ch.right(), ch.bottom(), c.menuButton());
        Draw.text(g, font, expanded ? "v" : ">", ch.x() + 7, ch.y() + 3, c.menuInk(), c.menuButton());
        final Rect well = geo.panel();
        if (well.present()) {
            g.fill(well.x(), well.y(), well.right(), well.bottom(), c.menuWell());
            Draw.text(g, font, words(VolumeTexts.SOUND_OUTPUT_TITLE), geo.heading().x(), geo.heading().y(),
                    c.menuInk(), c.menuWell());
        }
        for (int i = 0; i < geo.outputs().size(); i++) {
            final Rect row = geo.outputs().get(i);
            if (row.contains(mx, my)) {
                g.fill(row.x(), row.y(), row.right(), row.bottom(), c.menuButton());
            }
            if (LISTED.get(i) == output) {
                check(g, row.x() + 4, row.y() + 2, accent);
            }
            Draw.text(g, font, outputWords(i), row.x() + 15, row.y() + 2, c.menuInk(), c.menuWell());
            if (i == 1) {
                Draw.text(g, font, speakerNames(), row.x() + 15, row.y() + 11, c.menuDim(), c.menuWell());
            }
        }
        for (final int rule : geo.rules()) {
            g.fill(1, rule, geo.width() - 1, rule + 1, c.menuEdge());
        }
        final Rect f = geo.footer();
        Draw.text(g, font, words(VolumeTexts.SOUND_SETTINGS_TITLE), f.x(), f.y(), c.menuInk(), c.menuFill());
    }

    /* A slider lying across: its groove, the part up to the thumb in the accent, and the thumb. */
    private void slider(final GuiGraphics g, final Geometry geo, final int trough, final int accent, final int thumb,
                        final boolean dotted) {
        final Rect t = geo.track();
        final int at = VolumePopupLayout.thumbAt(geo, volume);
        g.fill(t.x(), t.y() + 3, t.right(), t.y() + 5, trough);
        g.fill(t.x(), t.y() + 3, at, t.y() + 5, accent);
        g.fill(at - 3, t.y(), at + 4, t.bottom(), accent);
        g.fill(at - 2, t.y() + 1, at + 3, t.bottom() - 1, thumb);
        if (dotted) {
            g.fill(at - 1, t.y() + 2, at + 2, t.bottom() - 2, accent);
        }
    }

    /* Whether a point is on the slider, taken a little wider than its groove, where the thumb stands out of it. */
    private static boolean onTrack(final Geometry geo, final double x, final double y) {
        final Rect t = geo.track();
        return geo.upright()
                ? x >= t.x() - 7 && x < t.right() + 7 && y >= t.y() - 4 && y < t.bottom() + 4
                : x >= t.x() - 3 && x < t.right() + 3 && y >= t.y() - 2 && y < t.bottom() + 2;
    }

    /* A tick, drawn a pixel at a time: the font has none. */
    private static void check(final GuiGraphics g, final int x, final int y, final int color) {
        final int[][] marks = {{0, 3}, {1, 4}, {2, 5}, {3, 4}, {4, 3}, {5, 2}, {6, 1}};
        for (final int[] p : marks) {
            g.fill(x + p[0], y + p[1], x + p[0] + 1, y + p[1] + 1, color);
        }
    }

    private void setVolume(final int value) {
        volume = Math.max(0, Math.min(100, value));
        settle();
        // Turning the volume is taking the sound back: a muted system is heard again, the way these all did it.
        if (muted) {
            muted = false;
            set("mute", "off");
        }
    }

    private void send() {
        if (volume != sentVolume) {
            sentVolume = volume;
            lastSent = Util.getMillis();
            set("volume", Integer.toString(volume));
        }
    }

    private void settle() {
        localUntil = Util.getMillis() + SETTLE_MILLIS;
    }

    private void set(final String key, final String value) {
        PacketDistributor.sendToServer(new SetSettingPayload(desktop.host(), key, value));
    }

    private static String outputWords(final int index) {
        return words(switch (index) {
            case 0 -> VolumeTexts.OUTPUT_MONITOR;
            case 1 -> VolumeTexts.OUTPUT_SPEAKERS;
            default -> VolumeTexts.OUTPUT_BOTH;
        });
    }

    private static String words(final TextKey key) {
        return GameText.resolve(key);
    }

    /**
     * The GNOME menu's colours: its fill and edge, its two inks, the well its outputs sit in, a button's face and the
     * slider's groove and thumb.
     */
    private record Colours(int menuFill, int menuEdge, int menuInk, int menuDim, int menuWell, int menuButton,
                           int menuTrough, int menuThumb) {
    }
}
