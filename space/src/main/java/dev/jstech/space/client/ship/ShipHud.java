/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.client.ship;

import dev.jstech.core.client.gui.component.Draw;
import dev.jstech.core.client.moving.ClientMovingStructures;
import dev.jstech.core.client.moving.ClientStructure;
import dev.jstech.core.moving.StructureFrame;
import dev.jstech.core.moving.StructureSeatEntity;
import dev.jstech.core.moving.YardLayout;
import dev.jstech.core.multiblock.Orientation;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextKey;
import dev.jstech.space.JsSpace;
import dev.jstech.space.api.ShipPart;
import dev.jstech.space.api.ShipParts;
import dev.jstech.space.ship.AirReserveEntity;
import dev.jstech.space.ship.ShipAboard;
import dev.jstech.space.ship.ShipAltitude;
import dev.jstech.space.ship.ShipHudTexts;
import dev.jstech.space.ship.ShipReadings;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.common.NeoForge;
import org.jetbrains.annotations.Nullable;

/**
 * A ship's HUD, as the approved flight draws it: green symbols projected on its canopy for the pilot at its helm, flat
 * and in pixels. The heading tape across the top; the pitch ladder with the flight path marker in the middle; the speed
 * and the boost's bar on the left, amber while it boosts; the altitude and the climb on the right; the reactor's power
 * and its deuterium above; the nozzles' angle and the gear below, amber with the gear down; and the autopilot's three
 * modes along the bottom. Laid out on the approved screen's 960 by 540 points, scaled to the player's; seen from behind
 * the ship it spreads over the screen.
 */
public final class ShipHud {

    /** The HUD's layer over the game's own. */
    public static final ResourceLocation LAYER = ResourceLocation.fromNamespaceAndPath(JsSpace.MODID, "ship_hud");
    /* The approved screen the HUD is laid out on, in points; its box on the canopy from the seat, and from behind. */
    private static final float WIDTH = 960;
    private static final float HEIGHT = 540;
    private static final Box SEAT = new Box(280, 30, 680, 330);
    private static final Box BEHIND = new Box(80, 20, 880, 520);
    /* The small labels' scale of the game's font, and the figures'. */
    private static final float SMALL = 0.75F;
    private static final float FIGURE = 1F;
    /* The heading tape: degrees each side of the nose, points a degree. */
    private static final int TAPE_DEGREES = 30;
    private static final int TAPE_STEP = 5;
    /* The pitch ladder: points a degree, the half widths of the horizon's line and the others', the gap they leave. */
    private static final float LADDER_STEP = 4.2F;
    private static final int HORIZON_HALF = 70;
    private static final int RUNG_HALF = 34;
    private static final int RUNG_GAP = 14;
    /* The speed's and the altitude's boxes, and the scales beside them. */
    private static final int BOX_WIDTH = 48;
    private static final int BOX_HALF = 8;
    private static final int SCALE_STEP = 12;
    private static final int BOOST_CELLS = 10;
    /* The autopilot's modes, and how far apart they stand. */
    private static final List<TextKey> MODES = List.of(ShipHudTexts.LAND, ShipHudTexts.ORBIT, ShipHudTexts.DOCK);
    private static final int MODE_STEP = 50;
    private static final double WATTS_A_MEGAWATT = 1.0e6;
    private static final int PERCENT = 100;

    private ShipHud() {
    }

    /**
     * Hands the mod's bus the HUD's layer, once, from the mod's client entry point; at a ship's helm its flight path
     * marker stands for the game's crosshair, which is not drawn.
     */
    public static void listen(final IEventBus modBus) {
        modBus.addListener(RegisterGuiLayersEvent.class,
                event -> event.registerAbove(VanillaGuiLayers.CROSSHAIR, LAYER, ShipHud::render));
        NeoForge.EVENT_BUS.addListener(RenderGuiLayerEvent.Pre.class, event -> {
            if (event.getName().equals(VanillaGuiLayers.CROSSHAIR) && atTheHelm(Minecraft.getInstance()) != null) {
                event.setCanceled(true);
            }
        });
    }

    /** The console of the ship whose helm the player sits at, or none. */
    public static @Nullable AirReserveEntity atTheHelm(final Minecraft minecraft) {
        if (minecraft.player == null || !(minecraft.player.getVehicle() instanceof StructureSeatEntity seat)
                || !seat.helm()) {
            return null;
        }
        final AirReserveEntity console = ShipConsoles.of(YardLayout.plotOfBlock(seat.seat().getX(),
                seat.seat().getZ()));
        return console != null && console.ship().isPresent() ? console : null;
    }

    private static void render(final GuiGraphics g, final DeltaTracker delta) {
        final Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.options.hideGui || minecraft.level == null) {
            return;
        }
        final AirReserveEntity console = atTheHelm(minecraft);
        if (console == null) {
            return;
        }
        final ShipAboard ship = console.ship().orElseThrow();
        final ClientStructure structure = ClientMovingStructures.byPlot(YardLayout.plotOfBlock(
                console.getBlockPos().getX(), console.getBlockPos().getZ()));
        if (structure == null || !structure.ready()) {
            return;
        }
        final float scale = Math.min(g.guiWidth() / WIDTH, g.guiHeight() / HEIGHT);
        g.pose().pushPose();
        g.pose().translate((g.guiWidth() - WIDTH * scale) / 2, (g.guiHeight() - HEIGHT * scale) / 2, 0);
        g.pose().scale(scale, scale, 1);
        final Box box = minecraft.options.getCameraType().isFirstPerson() ? SEAT : BEHIND;
        new Painter(g, minecraft.font, box, ShipHudArt.PALETTE.get()).paint(Flown.of(minecraft, console, ship,
                structure.frame()));
        g.pose().popPose();
    }

    /* The box on the approved screen the HUD is projected in, in points. */
    private record Box(int x0, int y0, int x1, int y1) {
    }

    /*
     * What the HUD shows of a ship's flight: where its nose points and how far it banks, in degrees, how far the way
     * it goes is off its nose, its readings, its reactor's power made and drawn, in watts, and its deuterium left.
     */
    private record Flown(double heading, double pitch, double bank, double offPath, ShipReadings readings,
                         double made, double drawn, double deuterium) {

        static Flown of(final Minecraft minecraft, final AirReserveEntity console, final ShipAboard ship,
                        final StructureFrame frame) {
            final Orientation turn = ship.orientation();
            final double[] nose = world(frame, turn.apply(0, 0, -1));
            final double[] right = world(frame, turn.apply(1, 0, 0));
            final ShipReadings readings = console.flight();
            final double heading = Math.floorMod(Math.round(Math.toDegrees(Math.atan2(nose[0], -nose[2])) * 10),
                    3600) / 10.0;
            final double pitch = Math.toDegrees(Math.asin(Math.clamp(nose[1], -1, 1)));
            final double bank = -Math.toDegrees(Math.asin(Math.clamp(right[1], -1, 1)));
            final double offPath = Math.toDegrees(Math.atan2(readings.climb(), Math.max(readings.speed(), 1.0)));
            final Map<ResourceLocation, ShipPart> parts = ShipParts.all(minecraft.level);
            final List<ResourceLocation> all = new ArrayList<>(ship.design().spine());
            all.addAll(ship.design().hung());
            double made = 0;
            double drawn = 0;
            for (final ResourceLocation id : all) {
                final ShipPart part = parts.get(id);
                if (part != null) {
                    final double power = part.figures().power();
                    made += Math.max(0, power);
                    drawn += Math.max(0, -power);
                }
            }
            final double deuterium = console.deuterium();
            if (deuterium <= 0) {
                made = 0;
            } else if (readings.charge() < PERCENT) {
                // What the parts leave over fills the boost.
                drawn = Math.max(drawn, made);
            }
            return new Flown(heading, pitch, bank, offPath, readings, made, drawn, deuterium);
        }

        private static double[] world(final StructureFrame frame, final int[] way) {
            final double x = frame.turnX(way[0], way[1], way[2]);
            final double y = frame.turnY(way[0], way[1], way[2]);
            final double z = frame.turnZ(way[0], way[1], way[2]);
            final double length = Math.sqrt(x * x + y * y + z * z);
            return length <= 0 ? new double[] {0, 0, -1} : new double[] {x / length, y / length, z / length};
        }
    }

    /* Paints the HUD in its box, in the approved screen's points, as the approved flight's own drawing lays it. */
    private record Painter(GuiGraphics g, Font font, Box box, ShipHudArt.Colours colours) {

        void paint(final Flown flown) {
            final int cx = (box.x0() + box.x1()) / 2;
            final int cy = (box.y0() + box.y1()) / 2 + 10;
            tape(flown.heading(), cx);
            ladder(flown.pitch(), flown.bank(), cx, cy);
            marker(cx, cy + (int) Math.round(-flown.offPath() * LADDER_STEP));
            speed(flown.readings(), cy);
            altitude(flown, cy);
            corners(flown);
            modes(cx);
        }

        /* The heading tape across the top, a tick each five degrees, a figure each ten, and the nose's mark. */
        private void tape(final double heading, final int cx) {
            final int whole = (int) Math.floor(heading);
            final double part = heading - whole;
            for (int k = -TAPE_DEGREES; k <= TAPE_DEGREES; k++) {
                final int deg = Math.floorMod(whole + k, 360);
                final int x = cx + (int) Math.round((k - part) * TAPE_STEP);
                if (deg % 5 == 0) {
                    vline(x, box.y0() + 14, deg % 10 == 0 ? box.y0() + 21 : box.y0() + 18, colours.symbol());
                }
                if (deg % 10 == 0) {
                    small(compass(deg), x, box.y0() + 4, colours.symbol(), Align.CENTRE);
                }
            }
            fill(cx - 1, box.y0() + 23, cx + 2, box.y0() + 26, colours.symbol());
        }

        /* The pitch ladder, each ten degrees, solid above the horizon and broken below, turned as the ship banks. */
        private void ladder(final double pitch, final double bank, final int cx, final int cy) {
            final double ca = Math.cos(Math.toRadians(bank));
            final double sa = Math.sin(Math.toRadians(bank));
            for (int p = -90; p <= 90; p += 10) {
                final double off = (p - pitch) * LADDER_STEP;
                final int half = p == 0 ? HORIZON_HALF : RUNG_HALF;
                for (final int side : new int[] {-1, 1}) {
                    for (int s = RUNG_GAP; s < half; s++) {
                        if (p < 0 && (s / 4) % 2 == 1) {
                            continue;
                        }
                        final double rx = cx + side * s * ca + off * sa;
                        final double ry = cy + side * s * sa - off * ca;
                        if (rx > box.x0() + 30 && rx < box.x1() - 30 && ry > box.y0() + 34 && ry < box.y1() - 30) {
                            dot((int) rx, (int) ry, colours.symbol());
                        }
                    }
                    if (p != 0) {
                        final double rx = cx + side * half * ca + off * sa;
                        final double ry = cy + side * half * sa - off * ca;
                        if (rx > box.x0() + 30 && rx < box.x1() - 40 && ry > box.y0() + 34 && ry < box.y1() - 30) {
                            small(Integer.toString(Math.abs(p)), (int) rx + (side > 0 ? 4 : -14), (int) ry - 3,
                                    colours.symbol(), Align.LEFT);
                        }
                    }
                }
            }
        }

        /* The flight path marker: where the ship is going, a ring with its wings and fin. */
        private void marker(final int cx, final int y) {
            final int fy = Math.max(box.y0() + 34, Math.min(box.y1() - 30, y));
            for (int a = 0; a < 360; a += 20) {
                dot((int) (cx + 4 * Math.cos(Math.toRadians(a))), (int) (fy + 4 * Math.sin(Math.toRadians(a))),
                        colours.symbol());
            }
            hline(cx - 12, cx - 4, fy, colours.symbol());
            hline(cx + 5, cx + 13, fy, colours.symbol());
            vline(cx, fy - 9, fy - 4, colours.symbol());
        }

        /* The speed on the left, boxed, with its scale, and the boost's bar under it. */
        private void speed(final ShipReadings readings, final int cy) {
            final int sx = box.x0() + 6;
            frame(sx, cy - BOX_HALF, sx + BOX_WIDTH, cy + BOX_HALF, colours.symbol());
            figure(Long.toString(Math.round(readings.speed())), sx + 44, cy - 6);
            small(GameText.resolve(ShipHudTexts.SPEED), sx, cy - 20, colours.dim(), Align.LEFT);
            for (int k = -5; k <= 5; k++) {
                if (k != 0) {
                    hline(sx + 50, Math.floorMod(k, 2) == 1 ? sx + 55 : sx + 58, cy + k * SCALE_STEP, colours.dim());
                }
            }
            final int boostColour = readings.boosting() ? colours.hot() : colours.symbol();
            small(GameText.resolve(ShipHudTexts.BOOST), sx, cy + 14, readings.boosting() ? colours.hot()
                    : colours.dim(), Align.LEFT);
            for (int k = 0; k < BOOST_CELLS; k++) {
                final boolean on = (k + 0.5) / BOOST_CELLS <= readings.charge() / (double) PERCENT;
                fill(sx + k * 5, cy + 24, sx + k * 5 + 4, cy + 28, on ? boostColour : colours.unfilled());
            }
        }

        /* The altitude on the right, boxed, with its scale, and the climb under it. */
        private void altitude(final Flown flown, final int cy) {
            final int ax = box.x1() - 54;
            frame(ax, cy - BOX_HALF, ax + BOX_WIDTH, cy + BOX_HALF, colours.symbol());
            final int altitude = flown.readings().altitude();
            figure(altitude == ShipAltitude.NONE ? "---" : Integer.toString(altitude), ax + 44, cy - 6);
            small(GameText.resolve(ShipHudTexts.ALTITUDE), ax, cy - 20, colours.dim(), Align.LEFT);
            final long climb = Math.round(flown.readings().speed() * Math.sin(Math.toRadians(flown.pitch()))
                    + flown.readings().climb());
            small(GameText.resolve(ShipHudTexts.CLIMB.with(Text.literal((climb >= 0 ? "+" : "") + climb))), ax,
                    cy + 14, colours.symbol(), Align.LEFT);
            for (int k = -5; k <= 5; k++) {
                if (k != 0) {
                    hline(Math.floorMod(k, 2) == 1 ? ax - 6 : ax - 9, ax - 1, cy + k * SCALE_STEP, colours.dim());
                }
            }
        }

        /* The reactor's power and its deuterium above; the nozzles' angle and the gear below. */
        private void corners(final Flown flown) {
            small(GameText.resolve(ShipHudTexts.POWER.with(Text.literal(megawatts(flown.drawn())),
                    Text.literal(megawatts(flown.made())))), box.x0() + 6, box.y0() + 32, colours.dim(), Align.LEFT);
            small(GameText.resolve(ShipHudTexts.FUEL.with(Text.literal(Long.toString(Math.round(flown.deuterium()
                    * PERCENT))))), box.x0() + 6, box.y0() + 42, colours.dim(), Align.LEFT);
            final int nozzles = flown.readings().nozzles();
            final int nx = box.x0() + 14;
            final int ny = box.y1() - 46;
            for (int a = 0; a <= 90; a += 6) {
                dot((int) (nx + 10 * Math.cos(Math.toRadians(a))), (int) (ny + 10 * Math.sin(Math.toRadians(a))),
                        colours.dim());
            }
            for (int r = 0; r <= 10; r++) {
                dot((int) (nx + r * Math.cos(Math.toRadians(nozzles))), (int) (ny + r * Math.sin(Math.toRadians(
                        nozzles))), colours.symbol());
            }
            small(GameText.resolve(ShipHudTexts.NOZZLES.with(Text.literal(Integer.toString(nozzles)))), nx + 16,
                    ny - 2, colours.symbol(), Align.LEFT);
            final boolean down = flown.readings().gearDown();
            small(GameText.resolve(down ? ShipHudTexts.GEAR_DOWN : ShipHudTexts.GEAR_UP), nx + 16, ny + 8,
                    down ? colours.hot() : colours.symbol(), Align.LEFT);
        }

        /* The autopilot's three modes along the bottom, none engaged: each mode's own flight lights its word. */
        private void modes(final int cx) {
            final int mx = cx - 70;
            small(GameText.resolve(ShipHudTexts.AUTOPILOT), mx - 18, box.y1() - 15, colours.dim(), Align.LEFT);
            for (int k = 0; k < MODES.size(); k++) {
                final String mode = GameText.resolve(MODES.get(k));
                final int w = Math.round(font.width(mode) * SMALL) + 8;
                final int bx = mx + k * MODE_STEP;
                frame(bx, box.y1() - 18, bx + w, box.y1() - 6, colours.dim());
                small(mode, bx + 4, box.y1() - 16, colours.dim(), Align.LEFT);
            }
        }

        private String compass(final int degrees) {
            return switch (degrees) {
                case 0 -> GameText.resolve(ShipHudTexts.NORTH);
                case 90 -> GameText.resolve(ShipHudTexts.EAST);
                case 180 -> GameText.resolve(ShipHudTexts.SOUTH);
                case 270 -> GameText.resolve(ShipHudTexts.WEST);
                default -> String.format("%02d", degrees / 10);
            };
        }

        private static String megawatts(final double watts) {
            return Long.toString(Math.round(watts / WATTS_A_MEGAWATT));
        }

        private void small(final String text, final int x, final int y, final int colour, final Align align) {
            final int width = Math.round(font.width(text) * SMALL);
            final int left = switch (align) {
                case LEFT -> x;
                case CENTRE -> x - width / 2;
                case RIGHT -> x - width;
            };
            Draw.textScaled(g, font, text, left, y, colour, SMALL);
        }

        /* A figure in a box, its right edge at {@code right}. */
        private void figure(final String text, final int right, final int y) {
            Draw.textScaled(g, font, text, right - Math.round(font.width(text) * FIGURE), y, colours.symbol(),
                    FIGURE);
        }

        private void dot(final int x, final int y, final int colour) {
            g.fill(x, y, x + 1, y + 1, colour);
        }

        private void hline(final int x0, final int x1, final int y, final int colour) {
            g.fill(Math.min(x0, x1), y, Math.max(x0, x1) + 1, y + 1, colour);
        }

        private void vline(final int x, final int y0, final int y1, final int colour) {
            g.fill(x, Math.min(y0, y1), x + 1, Math.max(y0, y1) + 1, colour);
        }

        private void fill(final int x0, final int y0, final int x1, final int y1, final int colour) {
            g.fill(x0, y0, x1, y1, colour);
        }

        private void frame(final int x0, final int y0, final int x1, final int y1, final int colour) {
            hline(x0, x1, y0, colour);
            hline(x0, x1, y1, colour);
            vline(x0, y0, y1, colour);
            vline(x1, y0, y1, colour);
        }
    }

    /* Where a label stands against its point. */
    private enum Align {
        LEFT,
        CENTRE,
        RIGHT
    }
}
