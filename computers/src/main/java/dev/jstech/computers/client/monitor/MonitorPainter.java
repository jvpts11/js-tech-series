/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.monitor;

import dev.jstech.computers.JsComputers;
import dev.jstech.computers.client.os.OffscreenDesktop;
import dev.jstech.computers.client.term.TermPalette;
import dev.jstech.computers.gui.MonitorGlass;
import dev.jstech.computers.monitor.IMonitorPicture;
import dev.jstech.computers.operation.payload.DesktopWindowsPayload;
import dev.jstech.computers.operation.payload.WireLine;
import dev.jstech.computers.os.OpenWindow;
import dev.jstech.computers.os.VramLedger;
import dev.jstech.computers.program.cli.CliStyle;
import dev.jstech.core.client.gui.component.Draw;
import dev.jstech.core.client.live.LiveGraphics;
import dev.jstech.core.palette.Palette;
import dev.jstech.core.palette.PaletteHolder;
import dev.jstech.core.palette.Palettes;
import dev.jstech.core.text.GameText;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;

/**
 * Draws what a monitor's face shows from the server's description of it, into the picture the world shows: the glass
 * of the machine's own screen, at the glass's own size.
 *
 * <p>A desktop is the machine's own desktop drawn with no screen open, with the windows the machine has open; a
 * screen of text or a console is drawn in the inks of the terminals. Whatever the tube does to the colours is done
 * after, to the whole picture.
 */
@PaletteHolder
@EventBusSubscriber(modid = JsComputers.MODID, value = Dist.CLIENT)
public final class MonitorPainter {

    /** The inks of a text screen on a monitor's face. */
    private static final Palette<Inks> INKS = Palettes.declare(JsComputers.MODID, "monitor/picture", new Inks(
            0xFF000000, 0xFFFFFFFF, 0xFFC0C0C0, 0xFF808080, 0xFF55FF55, 0xFFFF5555, 0xFFC0C0C0, 0xFF000000));
    private static final int ROW = 10;
    private static final int MARGIN = 6;
    /* What a monitor's glass keeps clear round itself, which an area for the desktop leaves for it. */
    private static final int GLASS_SIDES = MonitorGlass.BESIDE;
    private static final int GLASS_ENDS = MonitorGlass.ABOVE_AND_BELOW;
    /* The desktops drawn for faces, by the monitor showing them: a few at once, the least recently seen let go. */
    private static final int DESKTOPS_KEPT = 8;
    private static final Map<BlockPos, Shown> DESKTOPS = new LinkedHashMap<>(16, 0.75F, true) {
        @Override
        protected boolean removeEldestEntry(final Map.Entry<BlockPos, Shown> eldest) {
            return size() > DESKTOPS_KEPT;
        }
    };

    private MonitorPainter() {
    }

    /** Draws {@code picture}, the face of the monitor at {@code monitor}, into an area of that size. */
    public static void paint(final GuiGraphics g, final int width, final int height, final IMonitorPicture picture,
                             final BlockPos monitor, final float partialTick) {
        final Inks inks = INKS.get();
        g.fill(0, 0, width, height, inks.ground());
        switch (picture) {
            case IMonitorPicture.Dark dark -> {
                // The glass with nothing on it is the ground already laid.
            }
            case IMonitorPicture.Lines lines -> lines(g, width, lines, inks);
            case IMonitorPicture.Console console -> console(g, height, console, inks);
            case IMonitorPicture.Desktop desktop -> desktop(g, desktop, monitor, partialTick);
        }
    }

    /**
     * The monitor's own menu, on dark glass, saying it has no signal for want of video memory: what it needs, what is
     * free, and that it lights when the card has room.
     */
    public static void outOfVideoMemory(final GuiGraphics g, final int width, final int height, final long needKb,
                                        final long freeKb) {
        final Inks inks = INKS.get();
        final Font font = Minecraft.getInstance().font;
        g.fill(0, 0, width, height, inks.ground());
        final String title = GameText.resolve(MonitorPaintTexts.NO_VRAM_TITLE);
        final String need = GameText.resolve(MonitorPaintTexts.NO_VRAM_NEED.with(VramLedger.label(needKb),
                VramLedger.label(freeKb)));
        final String wait = GameText.resolve(MonitorPaintTexts.NO_VRAM_WAIT);
        final int boxW = Math.min(width - 2 * MARGIN, Math.max(font.width(need), font.width(wait)) + 4 * MARGIN);
        final int boxH = ROW * 4;
        final int bx = (width - boxW) / 2;
        final int by = (height - boxH) / 2;
        g.fill(bx, by, bx + boxW, by + boxH, inks.pickedText());
        Draw.outline(g, bx, by, boxW, boxH, inks.dim());
        Draw.textCentered(g, font, title, width / 2, by + MARGIN, inks.title());
        Draw.textCentered(g, font, need, width / 2, by + MARGIN + ROW + 2, inks.plain());
        Draw.textCentered(g, font, wait, width / 2, by + MARGIN + 2 * ROW + 2, inks.dim());
    }

    @SubscribeEvent
    public static void onLeave(final ClientPlayerNetworkEvent.LoggingOut event) {
        DESKTOPS.clear();
    }

    private static void lines(final GuiGraphics g, final int width, final IMonitorPicture.Lines picture,
                              final Inks inks) {
        final Font font = Minecraft.getInstance().font;
        Draw.text(g, font, GameText.resolve(picture.title()), MARGIN, MARGIN, inks.title(), inks.ground());
        int y = MARGIN + ROW * 2;
        final int right = width * 11 / 20;
        for (final IMonitorPicture.Line line : picture.lines()) {
            int colour = switch (line.tone()) {
                case IMonitorPicture.Line.DIM -> inks.dim();
                case IMonitorPicture.Line.GOOD -> inks.good();
                case IMonitorPicture.Line.BAD -> inks.bad();
                default -> inks.plain();
            };
            int ground = inks.ground();
            if (line.tone() == IMonitorPicture.Line.PICKED) {
                g.fill(MARGIN - 2, y - 1, width - MARGIN + 2, y + ROW - 1, inks.pickedBar());
                colour = inks.pickedText();
                ground = inks.pickedBar();
            }
            Draw.text(g, font, GameText.resolve(line.left()), MARGIN, y, colour, ground);
            if (!line.right().isEmpty()) {
                Draw.text(g, font, GameText.resolve(line.right()), right, y, colour, ground);
            }
            y += ROW;
        }
    }

    private static void console(final GuiGraphics g, final int height, final IMonitorPicture.Console picture,
                                final Inks inks) {
        final Font font = Minecraft.getInstance().font;
        final int rows = Math.max(1, (height - MARGIN * 2) / ROW - 1);
        final List<WireLine> lines = picture.lines();
        int y = MARGIN;
        for (int i = Math.max(0, lines.size() - rows); i < lines.size(); i++) {
            int x = MARGIN;
            for (final WireLine.Span span : lines.get(i).spans()) {
                final String text = GameText.resolve(span.text());
                Draw.text(g, font, text, x, y, TermPalette.colorOf(CliStyle.byId(span.style())), inks.ground());
                x += font.width(text);
            }
            y += ROW;
        }
        final int prompt = TermPalette.colorOf(CliStyle.PROMPT);
        Draw.text(g, font, picture.prompt(), MARGIN, y, prompt, inks.ground());
        // The cursor blinks where the next letter goes, as a terminal's does.
        if (Util.getMillis() / 500 % 2 == 0) {
            final int at = MARGIN + font.width(picture.prompt());
            g.fill(at, y, at + 5, y + 8, prompt);
        }
    }

    private static void desktop(final GuiGraphics g, final IMonitorPicture.Desktop picture, final BlockPos monitor,
                                final float partialTick) {
        Shown shown = DESKTOPS.get(monitor);
        if (shown == null || !shown.matches(picture)) {
            shown = new Shown(picture, new OffscreenDesktop(picture.host(), monitor, picture.osId(),
                    picture.desktopId(), picture.ramTotalMb(), picture.ramReservedMb()));
            DESKTOPS.put(monitor.immutable(), shown);
        }
        if (!picture.windows().equals(shown.windows) || picture.workspace() != shown.workspace) {
            final List<OpenWindow> windows = new ArrayList<>(picture.windows().size());
            for (final DesktopWindowsPayload.WireWindow window : picture.windows()) {
                windows.add(window.toOpenWindow());
            }
            shown.desktop.showWindows(windows, picture.workspace());
            shown.windows = picture.windows();
            shown.workspace = picture.workspace();
        }
        /*
         * The desktop lays itself out round a monitor's glass on the area it is given, as it does on the game's window;
         * the area is made just big enough for the whole glass, and moved so the glass falls on the picture.
         */
        final int areaWidth = MonitorGlass.WIDTH + GLASS_SIDES;
        final int areaHeight = MonitorGlass.HEIGHT + GLASS_ENDS;
        if (g instanceof LiveGraphics live) {
            live.shift(-GLASS_SIDES / 2, -GLASS_ENDS / 2);
        }
        shown.desktop.paint(g, areaWidth, areaHeight, Integer.MIN_VALUE / 2, Integer.MIN_VALUE / 2, partialTick);
    }

    /** A desktop drawn for a face, the identity it was made for, and the layout it shows. */
    private static final class Shown {

        private final IMonitorPicture.Desktop made;
        private final OffscreenDesktop desktop;
        private List<DesktopWindowsPayload.WireWindow> windows = List.of();
        private int workspace = -1;

        private Shown(final IMonitorPicture.Desktop made, final OffscreenDesktop desktop) {
            this.made = made;
            this.desktop = desktop;
        }

        /* The same machine, system, desktop and memory: the same desktop, whatever windows it has open. */
        private boolean matches(final IMonitorPicture.Desktop other) {
            return made.host().equals(other.host()) && made.osId().equals(other.osId())
                    && made.desktopId().equals(other.desktopId()) && made.ramTotalMb() == other.ramTotalMb()
                    && made.ramReservedMb() == other.ramReservedMb();
        }
    }

    private record Inks(int ground, int title, int plain, int dim, int good, int bad, int pickedBar,
                        int pickedText) {
    }
}
