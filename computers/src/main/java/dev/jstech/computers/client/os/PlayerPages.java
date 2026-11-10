/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.os;

import dev.jstech.computers.JsComputers;
import dev.jstech.computers.gui.layout.FirmwareLayout;
import dev.jstech.computers.gui.layout.PlayerPagesLayout;
import dev.jstech.computers.os.crash.PlayerPanic;
import dev.jstech.computers.os.fs.PixImage;
import dev.jstech.computers.os.player.PlayerBoot;
import dev.jstech.computers.os.player.SystemScreen;
import dev.jstech.core.client.gui.component.Draw;
import dev.jstech.core.palette.Palette;
import dev.jstech.core.palette.PaletteHolder;
import dev.jstech.core.palette.Palettes;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * The pages of an operating system a player wrote (computers/docs/design/player-systems.md), in its own two colours:
 * its coming up, with its logo in the middle, the bar of its progress under it, its last line under the bar and its
 * name in a corner; and its panic, with what it went down with, the error, the system and the reboot.
 */
@PaletteHolder
@TextHolder
public final class PlayerPages {

    /* The page's own colours when the system named none: a dark ground, light words, quieter ones, the bar's track. */
    private static final Palette<Colours> PALETTE = Palettes.declare(JsComputers.MODID, "os/player_pages",
            new Colours(0xFF15111F, 0xFFF0E6D2, 0xFFE8E4F0, 0xFFA79FB8, 0xFF3A2D52));
    /* A colour a system names comes without its opacity, and is drawn fully opaque. */
    private static final int OPAQUE = 0xFF << 24;

    private static final TextKey STOPPED = TextKey.of("jsc.player_pages.stopped", "%s stopped and has to start again.");
    /* The system, its author, the process that went down and the memory it held. */
    private static final TextKey FACTS = TextKey.of("jsc.player_pages.facts", "%s by %s   pid %s   %s MB");
    private static final TextKey RESTARTING = TextKey.of("jsc.player_pages.restarting", "Restarting in %s seconds.");
    private static final TextKey RESTART_IT = TextKey.of("jsc.player_pages.restart_it", "Restart the computer.");

    /* The picture each machine's system is coming up with, as the server last said it. */
    private static final Map<BlockPos, PlayerBoot> BOOTING = new HashMap<>();

    private PlayerPages() {
    }

    /** Takes what the server said the system coming up on a machine looks like now. */
    public static void accept(final PlayerBoot boot) {
        if (boot.nobody()) {
            BOOTING.remove(boot.host());
        } else {
            BOOTING.put(boot.host(), boot);
        }
    }

    /** The picture of the system coming up on that machine, or null when it is not a player's system. */
    @Nullable
    public static PlayerBoot booting(final BlockPos host) {
        return BOOTING.get(host);
    }

    /** Forgets every machine's picture, for a world being left. */
    public static void clear() {
        BOOTING.clear();
    }

    /** The coming-up page, on the glass at {@code (x, y)}. */
    public static void boot(final GuiGraphics g, final Font font, final int x, final int y, final PlayerBoot boot) {
        final Colours c = PALETTE.get();
        final int ground = colour(boot.background(), c.ground());
        final int text = colour(boot.text(), c.text());
        final int w = FirmwareLayout.WIDTH;
        g.fill(x, y, x + w, y + FirmwareLayout.HEIGHT, ground);
        logo(g, boot.host(), boot.logo(), x + (w - PlayerPagesLayout.LOGO) / 2, y + PlayerPagesLayout.LOGO_Y,
                PlayerPagesLayout.LOGO);
        final int barX = x + (w - PlayerPagesLayout.BAR_W) / 2;
        final int barY = y + PlayerPagesLayout.BAR_Y;
        g.fill(barX, barY, barX + PlayerPagesLayout.BAR_W, barY + PlayerPagesLayout.BAR_H, c.track());
        g.fill(barX, barY, barX + PlayerPagesLayout.BAR_W * Math.clamp(boot.progress(), 0, 100) / 100,
                barY + PlayerPagesLayout.BAR_H, text);
        centred(g, font, boot.line(), x + w / 2, y + PlayerPagesLayout.LINE_Y, c.light());
        small(g, font, boot.system(), x + PlayerPagesLayout.NAME_X, y + PlayerPagesLayout.NAME_Y, c.dim());
    }

    /**
     * The panic page, drawn from the glass's corner: what the system went down with at the top beside its logo, the
     * error under it, the system and its facts, and the reboot it asked for at the foot.
     */
    public static void panic(final GuiGraphics g, final Font font, final BlockPos host, final PlayerPanic panic,
                             final int remainingTicks, final boolean endsDark) {
        final Colours c = PALETTE.get();
        final int text = colour(panic.text(), c.text());
        g.fill(0, 0, FirmwareLayout.WIDTH, FirmwareLayout.HEIGHT, colour(panic.background(), c.ground()));
        logo(g, host, panic.logo(), PlayerPagesLayout.MARGIN, PlayerPagesLayout.PANIC_LOGO_Y,
                PlayerPagesLayout.PANIC_LOGO);
        Draw.text(g, font, panic.message(), PlayerPagesLayout.MESSAGE_X, PlayerPagesLayout.MESSAGE_Y, text);
        small(g, font, GameText.resolve(STOPPED.with(panic.system())), PlayerPagesLayout.MESSAGE_X,
                PlayerPagesLayout.STOPPED_Y, c.light());
        int y = PlayerPagesLayout.TRACE_Y;
        for (int i = 0; i < panic.trace().size() && i < PlayerPagesLayout.MOST_TRACE_ROWS; i++) {
            final int indent = i == 0 ? 0 : PlayerPagesLayout.TRACE_INDENT;
            small(g, font, panic.trace().get(i), PlayerPagesLayout.MARGIN + indent, y, i == 0 ? c.light() : c.dim());
            y += PlayerPagesLayout.ROW;
        }
        y += panic.trace().isEmpty() ? 0 : PlayerPagesLayout.ROW;
        small(g, font, GameText.resolve(FACTS.with(panic.system(), panic.author(), panic.pid(), panic.memoryMb())),
                PlayerPagesLayout.MARGIN, y, c.light());
        if (!endsDark) {
            small(g, font, GameText.resolve(panic.standsUntilRestarted() ? RESTART_IT.text()
                    : RESTARTING.with(Math.max(0, (remainingTicks + 19) / 20))), PlayerPagesLayout.MARGIN,
                    PlayerPagesLayout.FOOT_Y, text);
        }
    }

    /* The system's logo in a square box, when it named one and the machine has sent it. */
    private static void logo(final GuiGraphics g, final BlockPos host, final String path, final int x, final int y,
                             final int size) {
        final Optional<PixImage> picture = SigmaImages.get(host, path);
        picture.ifPresent(image -> SigmaImages.paint(g, image, x, y, size, size));
    }

    /* A colour the system named, as the game draws it, or the page's own when it named none it could read. */
    private static int colour(final String named, final int otherwise) {
        final int rgb = SystemScreen.colour(named);
        return rgb < 0 ? otherwise : OPAQUE | rgb;
    }

    private static void small(final GuiGraphics g, final Font font, final String text, final int x, final int y,
                              final int colour) {
        Draw.textScaled(g, font, text, x, y, colour, PlayerPagesLayout.SMALL);
    }

    private static void centred(final GuiGraphics g, final Font font, final String text, final int cx, final int y,
                                final int colour) {
        final int width = Math.round(font.width(text) * PlayerPagesLayout.SMALL);
        small(g, font, text, cx - width / 2, y, colour);
    }

    /** The page's own colours: its ground, its words, the light ones, the quiet ones, and the bar's track. */
    private record Colours(int ground, int text, int light, int dim, int track) {
    }
}
