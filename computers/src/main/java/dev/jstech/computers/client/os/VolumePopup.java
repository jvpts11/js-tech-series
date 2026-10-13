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

    private final DesktopState desktop;
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
    private static final Palette<Glass> GLASS = Palettes.declare(JsComputers.MODID, "panel/volume_glass",
            new Glass(0x9EBAD2EE, 0x8C96B4DC, 0xCC283C5A, 0xFFFFFFFF, 0xFF9FB3CF, 0xFFF2F8FD, 0xFFDCEBFA,
                    0xFF7DA2CE, 0xFF5D6B7C, 0xFF000000, 0xFF555555, 0xFF3B8AD9, 0xFFDCEBFB, 0xFF9EC2EA, 0xFF3D6AA3,
                    0xFFDCEBFB));
    private static final Palette<Flyout> FLYOUT = Palettes.declare(JsComputers.MODID, "panel/volume_flyout",
            new Flyout(0xF81F1F1F, 0xFF2B2B2B, 0xFF3A3A3A, 0x1FFFFFFF, 0xFFFFFFFF, 0xFF999999));

    VolumePopup(final DesktopState desktop) {
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
                () -> desktop.opener().openSettingsPage(SettingsApp.PAGE_SOUND))), x, y, 0, 0,
                desktop.view().width(), desktop.view().height());
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
        final OsSkin.Form form = desktop.prefs().skin().form();
        if (form == OsSkin.Form.KDE2 || form == OsSkin.Form.GNOME1) {
            return Look.PERIOD;
        }
        // KDE 4's KMix was already Plasma's applet; GNOME 2's volume control was the small upright slider of its age.
        if (form == OsSkin.Form.OXYGEN) {
            return Look.PLASMA;
        }
        if (form == OsSkin.Form.CLEARLOOKS) {
            return Look.CLASSIC;
        }
        final PanelStyle style = desktop.panelStyle();
        return switch (style) {
            case FRAMES_95, FRAMES_XP -> Look.CLASSIC;
            case FRAMES_7 -> Look.GLASS;
            case FRAMES_10 -> Look.FLYOUT;
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
        final boolean floats = look == Look.QUICK || look == Look.FLYOUT;
        final int margin = floats || look == Look.SYSTEM_MENU ? 4 : 2;
        originX = Math.max(2, sw - margin - geo.width());
        originY = topBar ? DesktopScreen.TASKBAR_H + 3 : tbY - (floats ? 4 : 1) - geo.height();
        final int mx = ctx.mouseX() - originX;
        final int my = ctx.mouseY() - originY;
        g.pose().pushPose();
        g.pose().translate(originX, originY, 0);
        final OsSkin skin = desktop.prefs().skin();
        switch (look) {
            case CLASSIC, PERIOD -> drawUpright(g, font, skin, geo, mx, my);
            case SYSTEM_MENU -> drawSystemMenu(g, font, geo, mx, my);
            case GLASS -> drawGlass(g, font, skin, geo, mx, my);
            case FLYOUT -> drawFlyout(g, font, skin, geo, mx, my);
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
            desktop.opener().openSettingsPage(SettingsApp.PAGE_SOUND);
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
            case GLASS -> new VolumePopupLayout.Labels("", "", "", outputs, names, words(VolumeTexts.MIXER));
            case FLYOUT -> new VolumePopupLayout.Labels("", "", "", outputs, names, "");
        };
    }

    /*
     * Frames 7's glass popup: the column of glass with the page of white inside it, the device button naming where
     * the sound goes, the slider standing with its figure under it, the mute button and the Mixer link; the outputs
     * in a white list to its left while the device button is pressed.
     */
    private void drawGlass(final GuiGraphics g, final Font font, final OsSkin skin, final Geometry geo,
                           final int mx, final int my) {
        final Glass c = GLASS.get();
        final Rect col = geo.panel();
        g.fillGradient(col.x(), col.y(), col.right(), col.bottom(), c.glassTop(), c.glassBottom());
        Draw.outline(g, col.x(), col.y(), col.w(), col.h(), c.glassRim());
        g.fill(col.x() + 4, col.y() + 3, col.right() - 4, col.bottom() - 4, c.paper());
        Draw.outline(g, col.x() + 4, col.y() + 3, col.w() - 8, col.h() - 7, c.paperRim());
        final Rect d = geo.chevron();
        if (expanded || d.contains(mx, my)) {
            g.fillGradient(d.x(), d.y(), d.right(), d.bottom(), c.hotTop(), c.hotBottom());
            Draw.outline(g, d.x(), d.y(), d.w(), d.h(), c.hotRim());
        }
        PanelTray.speaker(g, d.x() + 3, d.y() + 5, c.speaker(), false);
        final String where = outputWords(LISTED.indexOf(output));
        Draw.text(g, font, clip(font, where, d.w() - 22), d.x() + 14, d.y() + 6, c.ink(), c.paper());
        caret(g, d.right() - 6, d.y() + 8, c.ink(), true);
        final Rect t = geo.track();
        skin.field(g, t.x(), t.y(), t.w(), t.h(), false);
        final int at = VolumePopupLayout.thumbAt(geo, volume);
        g.fill(t.x() + 1, at, t.right() - 1, t.bottom() - 1, c.fill());
        g.fillGradient(t.x() - 6, at - 4, t.x() + 11, at + 5, c.thumbTop(), c.thumbBottom());
        Draw.outline(g, t.x() - 6, at - 4, 17, 9, c.thumbRim());
        final String figure = Integer.toString(volume);
        final Rect p = geo.percent();
        Draw.text(g, font, figure, p.x() + (p.w() - font.width(figure)) / 2, p.y(), c.dim(), c.paper());
        final Rect m = geo.mute();
        skin.button(g, font, m.x(), m.y(), m.w(), m.h(), "", m.contains(mx, my), muted, false);
        PanelTray.speaker(g, m.x() + 4, m.y() + 2, c.speaker(), muted);
        final Rect f = geo.footer();
        Draw.text(g, font, words(VolumeTexts.MIXER), f.x(), f.y(), skin.accent(), c.paper());
        if (!geo.outputs().isEmpty()) {
            final Rect first = geo.outputs().get(0);
            final Rect last = geo.outputs().get(geo.outputs().size() - 1);
            g.fill(first.x() - 2, first.y() - 2, first.right() + 2, last.bottom() + 2, c.paper());
            Draw.outline(g, first.x() - 2, first.y() - 2, first.w() + 4, last.bottom() - first.y() + 4,
                    c.paperRim());
            for (int i = 0; i < geo.outputs().size(); i++) {
                final Rect row = geo.outputs().get(i);
                final boolean chosen = LISTED.get(i) == output;
                if (row.contains(mx, my)) {
                    g.fill(row.x(), row.y(), row.right(), row.bottom(), c.rowHot());
                    Draw.outline(g, row.x(), row.y(), row.w(), row.h(), c.hotRim());
                }
                if (chosen) {
                    g.fill(row.x() + 4, row.y() + 4, row.x() + 7, row.y() + 7, c.ink());
                }
                Draw.text(g, font, outputWords(i), row.x() + 11, row.y() + 2, c.ink(), c.paper());
            }
        }
    }

    /*
     * Frames 10's dark flyout: the device row, lit while the outputs are open under it, the outputs with a bar of the
     * accent against the one in use, and the slider row: the speaker that mutes, the groove filled in the accent up
     * to its thumb, and the figure.
     */
    private void drawFlyout(final GuiGraphics g, final Font font, final OsSkin skin, final Geometry geo,
                            final int mx, final int my) {
        final Flyout c = FLYOUT.get();
        final int accent = skin.accent();
        g.fill(0, 0, geo.width(), geo.height(), c.fill());
        Draw.outline(g, 0, 0, geo.width(), geo.height(), c.edge());
        final Rect d = geo.chevron();
        if (expanded || d.contains(mx, my)) {
            g.fill(d.x(), d.y(), d.right(), d.bottom(), c.row());
        }
        final String where = outputWords(LISTED.indexOf(output));
        Draw.text(g, font, clip(font, where, d.w() - 16), d.x() + 4, d.y() + 3, c.ink(), c.fill());
        caret(g, d.right() - 8, d.y() + 6, c.ink(), !expanded);
        for (int i = 0; i < geo.outputs().size(); i++) {
            final Rect row = geo.outputs().get(i);
            final boolean chosen = LISTED.get(i) == output;
            if (chosen || row.contains(mx, my)) {
                g.fill(row.x(), row.y(), row.right(), row.bottom(), c.picked());
            }
            if (chosen) {
                g.fill(row.x(), row.y(), row.x() + 2, row.bottom(), accent);
            }
            Draw.text(g, font, outputWords(i), row.x() + 6, row.y() + 2, c.ink(), c.fill());
        }
        final Rect icon = geo.icon();
        PanelTray.speaker(g, icon.x(), icon.y(), c.ink(), muted);
        final Rect t = geo.track();
        final int at = VolumePopupLayout.thumbAt(geo, volume);
        g.fill(t.x(), t.y() + 3, t.right(), t.y() + 5, c.trough());
        g.fill(t.x(), t.y() + 3, at, t.y() + 5, accent);
        g.fill(at - 2, t.y() - 1, at + 2, t.bottom() + 1, accent);
        final String figure = Integer.toString(volume);
        final Rect p = geo.percent();
        Draw.text(g, font, figure, p.right() - font.width(figure), p.y(), c.ink(), c.fill());
    }

    /* A small caret, pointing down or up, the mark of something that opens. */
    private static void caret(final GuiGraphics g, final int cx, final int y, final int colour, final boolean down) {
        for (int i = 0; i < 3; i++) {
            final int row = down ? y + i : y + 2 - i;
            g.fill(cx - 2 + i, row, cx + 3 - i, row + 1, colour);
        }
    }

    /* Words cut to the room they have, so a long output name never runs out of its row. */
    private static String clip(final Font font, final String text, final int room) {
        if (font.width(text) <= room) {
            return text;
        }
        String cut = text;
        while (cut.length() > 1 && font.width(cut + "...") > room) {
            cut = cut.substring(0, cut.length() - 1);
        }
        return cut + "...";
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
        final int accent = desktop.prefs().skin().accent();
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

    /**
     * Frames 7's glass popup: the glass and its rim, the white page and its rim, the device button lit, the speaker
     * and the ink, the figure, the slider's fill below its thumb and the thumb, and a row under the cursor.
     */
    private record Glass(int glassTop, int glassBottom, int glassRim, int paper, int paperRim, int hotTop,
                         int hotBottom, int hotRim, int speaker, int ink, int dim, int fill, int thumbTop,
                         int thumbBottom, int thumbRim, int rowHot) {
    }

    /**
     * Frames 10's dark flyout: its fill and edge, the device row lit, the output in use and the one under the cursor,
     * its ink, and the slider's groove.
     */
    private record Flyout(int fill, int edge, int row, int picked, int ink, int trough) {
    }
}
