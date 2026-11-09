/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.portable;

import dev.jstech.computers.JsComputers;
import dev.jstech.computers.portable.MobileApp;
import dev.jstech.computers.portable.PortableModel;
import dev.jstech.core.palette.Palette;
import dev.jstech.core.palette.PaletteHolder;
import dev.jstech.core.palette.Palettes;
import net.minecraft.resources.ResourceLocation;

/**
 * The pictures the portables' systems are drawn with, as the K0.11 pages drew them, each kept at twice the GUI's
 * pixels: the apps' icons in each family's look, the wallpapers, each device's face held up and its start picture,
 * Droidix's robot; and the small glyphs drawn mark by mark.
 */
@PaletteHolder
public final class MobileArt {

    /** The apps' own colours, which their icons are drawn in. */
    private static final Palette<Apps> APPS = Palettes.declare(JsComputers.MODID, "mobile/apps",
            new Apps(0xFF1FA595, 0xFFE8A020, 0xFFE0A020, 0xFF3A8EE6, 0xFF5C6370, 0xFFE0507A, 0xFF42A846,
                    0xFFA8703A));
    /** Droidix's robot, on its start: twelve marks by nine. */
    public static final ResourceLocation DROID = gui("mobile/droid");
    public static final int DROID_WIDTH = 12;
    public static final int DROID_HEIGHT = 9;
    /** The Wi-Fi's arcs on a status bar, eleven pixels by six. */
    public static final String[] WIFI = {"..XXXXXXX..", ".X.......X.", "X..XXXXX..X", "..X.....X..", "....XXX....",
        ".....X....."};
    /** The padlock of Frames Pocket's locked Today. */
    public static final String[] PADLOCK = {"..XXXX..", ".X....X.", ".X....X.", "XXXXXXXX", "XXX..XXX", "XXX..XXX",
        "XXXXXXXX"};
    /** The arrow on qOS's slider. */
    public static final String[] SLIDER_ARROW = {"X...", "XX..", "XXX.", "XX..", "X..."};
    /** The disclosure arrow at the end of a qOS settings row. */
    public static final String[] CHEVRON = {"X..", ".X.", "..X", ".X.", "X.."};
    /** The dots of Droidix's app drawer. */
    public static final String[] DRAWER = {"X.X.X", ".....", "X.X.X", ".....", "X.X.X"};
    /** Droidix 2009's padlock on its unlock tab. */
    public static final String[] UNLOCK_TAB = {"..XXX..", ".X...X.", ".X...X.", "XXXXXXX", "XXXXXXX"};
    /** The torch on qOS 2017's lock. */
    public static final String[] TORCH = {".XX.", ".XX.", "XXXX", ".XX.", ".XX."};
    /** The camera on qOS 2017's lock: the camera icon's body. */
    public static final String[] CAMERA = {"....XXXX....", ".XXXXXXXXXX.", "XXXX....XXXX", "XXX..XX..XXX",
        "XXX.X..X.XXX", "XXX..XX..XXX", "XXXX....XXXX", ".XXXXXXXXXX."};
    /** The network app's glyph, twelve by twelve, drawn bare on Droidix's shade. */
    public static final String[] NETWORK_GLYPH = {".....XX.....", ".....XX.....", "......X.....", "......X.....",
        "...XXXXXX...", "...X....X...", "..XX....XX..", "..XX....XX..", "............", "............",
        "............", "............"};
    /** The alerts app's glyph. */
    public static final String[] ALERTS_GLYPH = {".....XX.....", "....XXXX....", "...XXXXXX...", "...XXXXXX...",
        "...XXXXXX...", "..XXXXXXXX..", "..XXXXXXXX..", ".XXXXXXXXXX.", "............", ".....XX.....",
        "............", "............"};
    /** The settings' glyph. */
    public static final String[] SETTINGS_GLYPH = {".....XX.....", "..X.XXXX.X..", "...XXXXXX...", "..XXX..XXX..",
        "XXXX....XXXX", "XXXX....XXXX", "..XXX..XXX..", "...XXXXXX...", "..X.XXXX.X..", ".....XX.....",
        "............", "............"};

    private MobileArt() {
    }

    /** The icon of {@code app} in {@code style}. */
    public static ResourceLocation icon(final IconStyle style, final MobileApp app) {
        return icon(style, glyph(app));
    }

    /** The icon called {@code name} (an app's glyph, settings, store, clock) in {@code style}. */
    public static ResourceLocation icon(final IconStyle style, final String name) {
        return gui("mobile/icon/" + style.folder() + "/" + name);
    }

    /** The name of the glyph {@code app}'s icon is drawn from. */
    public static String glyph(final MobileApp app) {
        return switch (app) {
            case NETWORK -> "network";
            case ALERTS -> "alerts";
            case CRYPTO_WALLET -> "wallet";
            case MESSENGER -> "messenger";
            case CAMERA -> "camera";
            case GALLERY -> "gallery";
            case MAPS -> "maps";
            case PROSPECTING -> "prospect";
        };
    }

    /** The colour {@code app} is known by: its icon's, where a system draws it as a disc or a bar. */
    public static int colour(final MobileApp app) {
        final Apps apps = APPS.get();
        return switch (app) {
            case NETWORK -> apps.network();
            case ALERTS -> apps.alerts();
            case CRYPTO_WALLET -> apps.wallet();
            case MESSENGER -> apps.messenger();
            case CAMERA -> apps.camera();
            case GALLERY -> apps.gallery();
            case MAPS -> apps.maps();
            case PROSPECTING -> apps.prospecting();
        };
    }

    /** The wallpaper of a qOS or Droidix device, at its screen's size. */
    public static ResourceLocation wallpaper(final PortableModel model) {
        return gui("mobile/wallpaper/" + model.serializedName());
    }

    /** The device's face held up, its screen cut out. */
    public static ResourceLocation face(final PortableModel model) {
        return gui("portable/" + model.serializedName());
    }

    /** What the device shows as it starts: its maker's mark, or its system's. */
    public static ResourceLocation start(final PortableModel model) {
        return gui("portable/start/" + model.serializedName());
    }

    private static ResourceLocation gui(final String path) {
        return ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, "textures/gui/" + path + ".png");
    }

    /** The apps' colours, as the palette above names them. */
    private record Apps(int network, int alerts, int wallet, int messenger, int camera, int gallery, int maps,
                        int prospecting) {
    }

    /** The look of an app's icon: Frames Pocket's, each qOS's, each Droidix's. */
    public enum IconStyle {
        FRAMES_POCKET("fp", 16),
        QOS_2007("q_t", 22),
        QOS_2013("q_s", 22),
        QOS_2017("q_a", 22),
        DROIDIX_2009("d_t", 20),
        DROIDIX_2014("d_s", 20),
        DROIDIX_2019("d_a", 22);

        private final String folder;
        private final int size;

        IconStyle(final String folder, final int size) {
            this.folder = folder;
            this.size = size;
        }

        public String folder() {
            return folder;
        }

        /** How wide and tall its icons are, in GUI pixels; their textures have twice that. */
        public int size() {
            return size;
        }
    }
}
