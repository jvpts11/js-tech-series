/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.os;

import dev.jstech.computers.gui.CdePalette;
import dev.jstech.computers.gui.CdeStyle;
import dev.jstech.computers.operation.payload.SetSettingPayload;
import dev.jstech.computers.os.IOsHost;
import dev.jstech.computers.os.PanelStyle;
import dev.jstech.core.tier.HardwareEra;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * How the machine's owner chose its desktop should look, and the skin that dresses it: the accent, the brightness,
 * the clock's format, the wallpaper, where the taskbar's buttons stand, the dark theme, and CDE's palette and
 * backdrops. The choices are the machine's, kept on the server; this holds the copy the desktop is drawn from.
 *
 * <p>Also the two readings of the world a desktop shows beside them: the time on its clock, and the era of the
 * machine it runs on, which picks the skin's period look.
 */
final class DesktopPrefs {

    private final DesktopState desktop;
    private final ResourceLocation desktopId;
    /** The accent the owner put over the skin's own, or 0 for the skin's. */
    private int accent;
    /** How bright the screen is, 0 to 100. */
    private int brightness = 100;
    private boolean clock12h;
    /** Frames 11's taskbar: its buttons centred (the default) or at the left beside Start. */
    private boolean taskbarCentered = true;
    /** Frames 11's dark theme, which darkens the window chrome and the Start menu. */
    private boolean darkMode;
    /** The wallpaper the owner chose, or empty for the system's own. */
    private String wallpaper = "";
    /** CDE's palette and the backdrop of each workspace, worn while one is chosen. */
    private CdeStyle cdeStyle = CdeStyle.DEFAULT;
    private OsSkin skin;

    DesktopPrefs(final DesktopState desktop, final ResourceLocation desktopId) {
        this.desktop = desktop;
        this.desktopId = desktopId;
        // A provisional skin: rebuildSkin() refines it with the host's era once the level is reachable.
        this.skin = OsSkin.forDesktop(desktopId);
    }

    /** Takes a whole set of choices at once, as the machine's listing or the Settings app sends them. */
    void apply(final int accent, final int brightness, final boolean clock12h, final String wallpaper,
               final boolean taskbarCentered, final boolean darkMode) {
        this.accent = accent;
        this.brightness = brightness;
        this.clock12h = clock12h;
        this.wallpaper = wallpaper == null ? "" : wallpaper;
        this.taskbarCentered = taskbarCentered;
        this.darkMode = darkMode;
        rebuildSkin();
    }

    /**
     * Dresses the desktop in its skin again, from the choices and the host's era: a Linux desktop on Legacy hardware
     * wears its own period, instead of a modern flat theme on a machine from another decade. CDE is drawn from the
     * palette the machine keeps, which is a choice and not a fact of its era; the dark theme applies to the flat
     * Frames 11 alone.
     */
    void rebuildSkin() {
        OsSkin base = desktop.panelStyle() == PanelStyle.CDE
                ? OsSkin.motif(cdeStyle.scheme()) : OsSkin.forDesktop(desktopId, era());
        if (darkMode) {
            base = base.darkVariant();
        }
        this.skin = base.withAccent(accent);
    }

    /** The skin the desktop is drawn in. */
    OsSkin skin() {
        return skin;
    }

    int brightness() {
        return brightness;
    }

    boolean taskbarCentered() {
        return taskbarCentered;
    }

    boolean darkMode() {
        return darkMode;
    }

    /** The wallpaper the owner chose, or empty for the system's own. */
    String wallpaper() {
        return wallpaper;
    }

    /** Hangs that wallpaper at once, before the machine's next listing says so. */
    void setWallpaper(final String choice) {
        this.wallpaper = choice == null ? "" : choice;
    }

    /** CDE's look as this desktop is wearing it, which is what the Style Manager starts from. */
    CdeStyle cdeStyle() {
        return cdeStyle;
    }

    /** The palette a CDE desktop is drawn from. */
    CdePalette cdePalette() {
        return cdeStyle.colours();
    }

    /** Takes the look the machine keeps, as its listing brings it; the skin follows at the next rebuild. */
    void takeCdeStyle(final CdeStyle style) {
        this.cdeStyle = style == null ? CdeStyle.DEFAULT : style;
    }

    /**
     * Puts a look on the desktop at once, frames and panel and backdrop, without telling the machine: the Style
     * Manager shows a palette this way while it is being chosen, and puts the kept one back on Cancel.
     */
    void wearCdeStyle(final CdeStyle style) {
        takeCdeStyle(style);
        rebuildSkin();
    }

    /** Puts a look on the desktop and has the machine keep it, so it is there for whoever looks next. */
    void keepCdeStyle(final CdeStyle style) {
        wearCdeStyle(style);
        PacketDistributor.sendToServer(new SetSettingPayload(desktop.hostPos(), "cdestyle", cdeStyle.encoded()));
    }

    /**
     * The host computer's hardware era, read from its block entity so the skin and the monitor frame match the
     * chassis. Never null: a host that cannot name an era yet (a rack whose unit the client has not received) gets
     * the Standard one, since the frame's geometry is asked for every frame by the recipe viewer as well.
     */
    HardwareEra era() {
        final Minecraft mc = Minecraft.getInstance();
        if (mc.level != null && mc.level.getBlockEntity(desktop.hostPos()) instanceof IOsHost be) {
            final HardwareEra era = be.displayEra();
            if (era != null) {
                return era;
            }
        }
        return HardwareEra.STANDARD;
    }

    /** The world's time of day as the taskbar clock shows it, in the format the owner chose (dayTime 0 is 06:00). */
    String clockText() {
        final Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return "";
        }
        final int totalMin = minuteOfDay();
        final int hour24 = totalMin / 60;
        final int minute = totalMin % 60;
        if (clock12h) {
            final int h12 = hour24 % 12 == 0 ? 12 : hour24 % 12;
            return String.format(Locale.ROOT, "%d:%02d %s", h12, minute, hour24 < 12 ? "AM" : "PM");
        }
        return String.format(Locale.ROOT, "%02d:%02d", hour24, minute);
    }

    /** The minute of the world's day counted from midnight, for a clock that has hands instead of figures. */
    int minuteOfDay() {
        final Minecraft mc = Minecraft.getInstance();
        return mc.level == null ? 0 : (int) (((mc.level.getDayTime() % 24000L + 6000L) % 24000L) * 3L / 50L);
    }

    /** Which day of the world it is, counted from one, for a calendar page. */
    int dayOfWorld() {
        final Minecraft mc = Minecraft.getInstance();
        return mc.level == null ? 1 : (int) (mc.level.getDayTime() / 24000L % 9999L) + 1;
    }
}
