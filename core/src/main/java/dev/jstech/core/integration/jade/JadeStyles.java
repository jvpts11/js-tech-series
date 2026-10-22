/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.integration.jade;

import dev.jstech.core.JsCore;
import dev.jstech.core.client.gui.style.Styles;
import dev.jstech.core.gui.style.Style;
import dev.jstech.core.gui.style.StyleRole;
import dev.jstech.core.gui.style.StyleSettings;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.jetbrains.annotations.Nullable;
import snownee.jade.api.config.IWailaConfig;
import snownee.jade.api.theme.IThemeHelper;
import snownee.jade.api.theme.TextSetting;
import snownee.jade.api.theme.Theme;
import snownee.jade.api.ui.BoxStyle;
import snownee.jade.api.ui.ColorPalette;

/**
 * Jade's tooltip in the style the player picked for everything: its ground and its edge, and its words in the style's
 * colours, the plain one, the title, what is fine, what warns and what fails, with or without a shadow as the style
 * draws its text, and square corners as every style of the series has. The Core brings a theme of its own for it,
 * hidden from Jade's list, painted anew each time the styles are read or the player picks another; a player who picks
 * none has Jade's own theme back. Only ever loaded when Jade is.
 */
public final class JadeStyles {

    /** The Core's theme of Jade, which wears the player's style. */
    public static final ResourceLocation THEME = ResourceLocation.fromNamespaceAndPath(JsCore.MODID, "player_style");

    private static volatile boolean repaint = true;
    /* The theme Jade wore before it wore the player's style, to give back. */
    private static @Nullable ResourceLocation before;

    private JadeStyles() {
    }

    /** Whether Jade's tooltip wears the player's style now. */
    public static boolean wearing() {
        return THEME.equals(IWailaConfig.get().getOverlay().getTheme().id);
    }

    /** The colour of the ground Jade's tooltip is drawn on now, as its theme says it, ARGB. */
    public static int ground() {
        return IWailaConfig.get().getOverlay().getTheme().tooltipStyle instanceof BoxStyle.GradientBorder box
                ? box.bgColor : 0;
    }

    /** Paints Jade's tooltip again at once, as a test does after it changes the player's style. */
    public static void repaintNow() {
        repaint = false;
        paint();
    }

    /** Hands the game what this listens to, once, as Jade registers the Core's plugin on the player's game. */
    static void listen() {
        Styles.onReload(() -> repaint = true);
        StyleSettings.FILE.whenChanged(StyleSettings.EVERYTHING, chosen -> repaint = true);
        NeoForge.EVENT_BUS.addListener(ClientTickEvent.Post.class, event -> {
            // Jade reads its themes again with the resources; the next tick paints the Core's anew.
            if (repaint) {
                repaintNow();
            }
        });
    }

    private static void paint() {
        final IWailaConfig.IConfigOverlay overlay = IWailaConfig.get().getOverlay();
        final ResourceLocation wearing = overlay.getTheme().id;
        final Optional<Style> chosen = Optional.ofNullable(ResourceLocation.tryParse(StyleSettings.everything()))
                .filter(id -> !StyleSettings.everything().isBlank()).flatMap(Styles::get);
        if (chosen.isEmpty()) {
            if (THEME.equals(wearing) && before != null) {
                overlay.applyTheme(before);
            }
            return;
        }
        if (!THEME.equals(wearing)) {
            before = wearing;
        }
        final Style style = chosen.get();
        final Theme theme = IThemeHelper.get().getTheme(THEME);
        final BoxStyle.GradientBorder box = BoxStyle.GradientBorder.DEFAULT_VIEW_GROUP.clone();
        final int edge = style.colour(StyleRole.TOOLTIP_EDGE);
        final int shade = style.colour(StyleRole.LINE);
        box.bgColor = style.colour(StyleRole.TOOLTIP_GROUND);
        box.borderColor = new int[] {edge, edge, shade, shade};
        box.borderWidth = 1;
        box.roundCorner = false;
        theme.tooltipStyle = box;
        theme.changeRoundCorner = false;
        final int text = style.colour(StyleRole.TEXT);
        theme.text = new TextSetting(new ColorPalette(text, style.colour(StyleRole.DIM), style.colour(StyleRole.TITLE),
                style.colour(StyleRole.GREEN), style.colour(StyleRole.AMBER), style.colour(StyleRole.RED),
                style.colour(StyleRole.RED)), style.textShadow(), Optional.empty(), text);
        overlay.applyTheme(THEME);
    }
}
