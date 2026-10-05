/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.os;

import dev.jstech.computers.JsComputers;
import dev.jstech.computers.gui.help.HelpForm;
import dev.jstech.core.palette.Palette;
import dev.jstech.core.palette.PaletteHolder;
import dev.jstech.core.palette.Palettes;

/**
 * The colours of the help windows, one palette per form, {@code jsc:app/help/<form>}: the grey of Frames 95 and its
 * cream topic window, XP's blue band and lilac pane, 7's and Get Help's white, KDE's Breeze, GNOME's Adwaita and CDE's
 * blue-grey, each with the purple books of the help of those years.
 */
@PaletteHolder
final class HelpViewerPalette {

    private static final Palette<Colours> FRAMES_95 = Palettes.declare(JsComputers.MODID, "app/help/frames_95",
            new Colours(0xFFC0C0C0, 0xFFFFFFFF, 0xFFFFFFFF, 0xFF000000, 0xFF000000, 0xFF000000, 0xFF2050D0,
                    0xFFC00000, 0xFF707070, 0xFF800000, 0xFF808080, 0xFFE8E8E8, 0xFFC0C0C0, 0xFF000000, 0xFF7A2F86,
                    0xFFC58AD0, 0xFF000080, 0xFFFFFFFF));
    private static final Palette<Colours> FRAMES_95_TOPIC = Palettes.declare(JsComputers.MODID,
            "app/help/frames_95_topic", new Colours(0xFFC0C0C0, 0xFFFFFFFF, 0xFFFFFFE1, 0xFF000000, 0xFF000000,
                    0xFF000000, 0xFF2050D0, 0xFFC00000, 0xFF707070, 0xFF800000, 0xFF808080, 0xFFF4F0C8, 0xFFC0C0C0,
                    0xFF000000, 0xFF7A2F86, 0xFFC58AD0, 0xFF000080, 0xFFFFFFFF));
    private static final Palette<Colours> FRAMES_XP = Palettes.declare(JsComputers.MODID, "app/help/frames_xp",
            new Colours(0xFFFFFFFF, 0xFFD6DFF7, 0xFFFFFFFF, 0xFF000000, 0xFF21409A, 0xFF000000, 0xFF2A5EC9,
                    0xFFE05A10, 0xFF6D6D6D, 0xFFA01010, 0xFFA8A8A8, 0xFFF2F2F2, 0xFF2B5DD1, 0xFFFFFFFF, 0xFF7A2F86,
                    0xFFC58AD0, 0xFF0A246A, 0xFFFFFFFF));
    private static final Palette<Colours> FRAMES_7 = Palettes.declare(JsComputers.MODID, "app/help/frames_7",
            new Colours(0xFFF0F0F0, 0xFFFFFFFF, 0xFFFFFFFF, 0xFF1E1E1E, 0xFF1F3E79, 0xFF1F5BA8, 0xFF2266CC,
                    0xFF0F3E9E, 0xFF6D6D6D, 0xFFA01010, 0xFFB4B4B4, 0xFFF4F4F4, 0xFFE4EAF2, 0xFF1E1E1E, 0xFF7A2F86,
                    0xFFC58AD0, 0xFF1E3A6E, 0xFFFFFFFF));
    private static final Palette<Colours> GET_HELP = Palettes.declare(JsComputers.MODID, "app/help/get_help",
            new Colours(0xFFF3F3F3, 0xFFF3F3F3, 0xFFFFFFFF, 0xFF1A1A1A, 0xFF1A1A1A, 0xFF1A1A1A, 0xFF2B6CD6,
                    0xFF0F4FA8, 0xFF6D6D6D, 0xFFB01818, 0xFFC8C8C8, 0xFFF6F6F6, 0xFFF3F3F3, 0xFF1A1A1A, 0xFF7A2F86,
                    0xFFC58AD0, 0xFFE0E0E0, 0xFF1A1A1A));
    private static final Palette<Colours> KDE = Palettes.declare(JsComputers.MODID, "app/help/kde",
            new Colours(0xFFEFF0F1, 0xFFEFF0F1, 0xFFFFFFFF, 0xFF232627, 0xFF232627, 0xFF232627, 0xFF2980B9,
                    0xFF1D5F8A, 0xFF7F8C8D, 0xFFDA4453, 0xFFBDC3C7, 0xFFF7F7F7, 0xFFEFF0F1, 0xFF232627, 0xFF7A2F86,
                    0xFFC58AD0, 0xFF3DAEE9, 0xFFFFFFFF));
    private static final Palette<Colours> YELP = Palettes.declare(JsComputers.MODID, "app/help/yelp",
            new Colours(0xFFFAFAFA, 0xFFFAFAFA, 0xFFFAFAFA, 0xFF2E3436, 0xFF2E3436, 0xFF2E3436, 0xFF1C71D8,
                    0xFF0D4FA0, 0xFF77767B, 0xFFC01C28, 0xFFC0BFBC, 0xFFF2F2F2, 0xFFEBEBEB, 0xFF2E3436, 0xFF7A2F86,
                    0xFFC58AD0, 0xFF3584E4, 0xFFFFFFFF));
    private static final Palette<Colours> CDE = Palettes.declare(JsComputers.MODID, "app/help/cde",
            new Colours(0xFFAEB2C3, 0xFFFFFFFF, 0xFFFFFFFF, 0xFF000000, 0xFF000000, 0xFF000000, 0xFF2A4FBF,
                    0xFFB02020, 0xFF5A5E6E, 0xFF800000, 0xFF6E7282, 0xFFE6E8EE, 0xFFAEB2C3, 0xFF000000, 0xFF7A2F86,
                    0xFFC58AD0, 0xFF5C6B9E, 0xFFFFFFFF));

    private HelpViewerPalette() {
    }

    /**
     * A form's colours.
     *
     * @param ground    the window around the parts
     * @param pane      behind the tree
     * @param page      behind the page
     * @param ink       running text
     * @param title     a page's title
     * @param heading   a heading inside a page
     * @param link      a link
     * @param linkHover a link under the pointer
     * @param faint     quiet text: a caption, a hint, what is not there
     * @param warning   a warning
     * @param rule      the edges of a table, a picture or a well
     * @param shade     behind a table's labels and a picture
     * @param band      the strip along the top
     * @param bandInk   what is written on it
     * @param book      a shut book of the tree
     * @param bookOpen  an open one
     * @param select    behind the row picked in the tree
     * @param selectInk the words of that row
     */
    record Colours(int ground, int pane, int page, int ink, int title, int heading, int link, int linkHover,
                   int faint, int warning, int rule, int shade, int band, int bandInk, int book, int bookOpen,
                   int select, int selectInk) {
    }

    /** The colours of a form, as the packs last gave them. */
    static Colours of(final HelpForm form) {
        return switch (form) {
            case FRAMES_95 -> FRAMES_95.get();
            case FRAMES_95_TOPIC -> FRAMES_95_TOPIC.get();
            case FRAMES_XP -> FRAMES_XP.get();
            case FRAMES_7 -> FRAMES_7.get();
            case GET_HELP -> GET_HELP.get();
            case KDE -> KDE.get();
            case YELP -> YELP.get();
            case CDE -> CDE.get();
        };
    }
}
