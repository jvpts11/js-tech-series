/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.client.ship;

import dev.jstech.core.client.gui.component.Draw;
import dev.jstech.core.client.input.KeyActionsClient;
import dev.jstech.core.client.moving.ClientMovingStructures;
import dev.jstech.core.client.moving.ClientStructure;
import dev.jstech.core.gui.TextSizes;
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
import dev.jstech.space.ship.ShipAutopilot;
import dev.jstech.space.ship.PulseCourse;
import dev.jstech.space.ship.ShipHudTexts;
import dev.jstech.space.ship.ShipPulse;
import dev.jstech.space.ship.ShipReadings;
import dev.jstech.space.universe.SolarBody;
import dev.jstech.space.universe.SystemLayout;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.common.NeoForge;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Vector4f;

/**
 * A ship's HUD, as the approved flight draws it: green symbols projected on its canopy for the pilot at its helm, flat
 * and in pixels. The heading tape across the top; the pitch ladder with the flight path marker in the middle; the speed
 * and the boost's bar on the left, amber while it boosts, TURN SLOW under it; the altitude and the climb on the right;
 * the reactor's power and its deuterium above; the nozzles' angle and the gear below, amber with the gear down; the
 * way home at the top right, to where the pilot last took off from in the world flown in or over, its mark on the
 * heading tape and over the place ({@link ShipHudHome}); and the autopilot's three modes along the bottom, the one
 * engaged lit, its own symbol standing on its target in the world.
 * Laid out on the approved screen's 960 by 540 points, scaled to the player's; seen from behind the ship it spreads
 * over the screen.
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
    /*
     * The pulse jump's spool ring round the middle, its radius and how far apart its dots stand, in degrees of a whole
     * turn; how far the corners of the bracket on the nearest world stand from it, and how long their arms are.
     */
    private static final int SPOOL_RING = 30;
    private static final int RING_STEP = 4;
    private static final int FULL_TURN = 360;
    private static final int BRACKET = 9;
    private static final int BRACKET_ARM = 5;
    /* How far down the box the pitch ladder's rungs begin: under the heading tape, or under the pulse's labels. */
    private static final int UNDER_THE_TAPE = 34;
    private static final int UNDER_THE_LABELS = 58;
    /* The autopilot's modes, and how far apart they stand. */
    private static final Map<ShipAutopilot.Mode, TextKey> MODES = new EnumMap<>(Map.of(
            ShipAutopilot.Mode.LAND, ShipHudTexts.LAND, ShipAutopilot.Mode.ORBIT, ShipHudTexts.ORBIT,
            ShipAutopilot.Mode.DOCK, ShipHudTexts.DOCK));
    private static final int MODE_STEP = 50;
    /*
     * The way home: how far down the box its first line stands and how far apart its lines are; how far down the box
     * its mark stands on the heading tape, how far under the mark its label stands, and how tall the mark's roof is.
     */
    private static final int HOME_LINES = 50;
    private static final int HOME_LINE = 10;
    private static final int ON_THE_TAPE = 30;
    private static final int UNDER_THE_HOUSE = 9;
    private static final int HOUSE_ROOF = 5;
    private static final double WATTS_A_MEGAWATT = 1.0e6;
    private static final int PERCENT = 100;
    private static final int THOUSAND = 1000;
    /* How the last frame drew the world, for the autopilot's symbol; none before one was drawn. */
    private static @Nullable Seen seen;

    private ShipHud() {
    }

    /**
     * Hands the mod's bus the HUD's layer, once, from the mod's client entry point; at a ship's helm its flight path
     * marker stands for the game's crosshair, which is not drawn.
     */
    public static void listen(final IEventBus modBus) {
        ShipHudHome.listen();
        modBus.addListener(RegisterGuiLayersEvent.class,
                event -> event.registerAbove(VanillaGuiLayers.CROSSHAIR, LAYER, ShipHud::render));
        NeoForge.EVENT_BUS.addListener(RenderGuiLayerEvent.Pre.class, event -> {
            if (event.getName().equals(VanillaGuiLayers.CROSSHAIR) && atTheHelm(Minecraft.getInstance()) != null) {
                event.setCanceled(true);
            }
        });
        // The autopilot's symbol stands on a place of the world: the frame's own view finds it on the screen.
        NeoForge.EVENT_BUS.addListener(RenderLevelStageEvent.class, event -> {
            if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_WEATHER) {
                seen = new Seen(new Matrix4f(event.getModelViewMatrix()), new Matrix4f(event.getProjectionMatrix()),
                        event.getCamera().getPosition());
            }
        });
    }

    /**
     * Where the world's {@code point} falls on the screen, in the screen's scaled pixels, as the last frame drew the
     * world: none before a frame was drawn, or behind the camera.
     */
    public static @Nullable Vec2 onScreen(final Vec3 point) {
        final Seen last = seen;
        if (last == null) {
            return null;
        }
        final Vector4f at = new Vector4f((float) (point.x - last.eye().x), (float) (point.y - last.eye().y),
                (float) (point.z - last.eye().z), 1.0F);
        last.view().transform(at);
        last.projection().transform(at);
        if (at.w <= 0) {
            return null;
        }
        final Minecraft minecraft = Minecraft.getInstance();
        return new Vec2((at.x / at.w + 1) / 2 * minecraft.getWindow().getGuiScaledWidth(),
                (1 - at.y / at.w) / 2 * minecraft.getWindow().getGuiScaledHeight());
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
        final float left = (g.guiWidth() - WIDTH * scale) / 2;
        final float top = (g.guiHeight() - HEIGHT * scale) / 2;
        g.pose().pushPose();
        g.pose().translate(left, top, 0);
        g.pose().scale(scale, scale, 1);
        final Box box = minecraft.options.getCameraType().isFirstPerson() ? SEAT : BEHIND;
        final Flown flown = Flown.of(minecraft, console, ship, structure.frame());
        final ShipReadings.Pilot pilot = flown.readings().pilot();
        final Vec2 target = pilot.mode().isEmpty() ? null : onScreen(pilot.target());
        final Vec2 world = jumpedTo(flown.readings().pulse(), delta.getGameTimeDeltaPartialTick(false));
        final ShipHudHome.Home home = minecraft.player == null ? ShipHudHome.NONE
                : ShipHudHome.of(minecraft.level, minecraft.player.position());
        final Vec2 homeAt = home.mark() == null ? null : onScreen(home.mark());
        // Laid out on a screen larger than the window, its text would fall under a screen pixel to a font pixel and
        // lose whole rows and columns of its letters: it is drawn at one at least.
        final double pixels = scale * minecraft.getWindow().getGuiScale();
        new Painter(g, minecraft.font, box, ShipHudArt.PALETTE.get(), TextSizes.legible(SMALL, pixels),
                TextSizes.legible(FIGURE, pixels)).paint(flown, inBox(target, left, top, scale),
                inBox(world, left, top, scale), home, inBox(homeAt, left, top, scale));
        g.pose().popPose();
    }

    /* {@code point} of the screen, in the approved screen's points laid {@code scale} to one from its corner. */
    private static @Nullable Vec2 inBox(final @Nullable Vec2 point, final float left, final float top,
                                        final float scale) {
        return point == null ? null : new Vec2((point.x - left) / scale, (point.y - top) / scale);
    }

    /*
     * Where on the screen the world nearest a ship's pulse jump is seen, as the space is drawn from as far on as the
     * jump has gone: none out of a jump, or behind the player.
     */
    private static @Nullable Vec2 jumpedTo(final ShipReadings.Pulse pulse, final float partialTick) {
        final SolarBody body = pulse.on() ? ShipPulse.Drop.bodyOf(pulse.near()) : null;
        if (body == null) {
            return null;
        }
        final double[] middle = SystemLayout.centre(body);
        final double[] jumped = ShipPulseClient.offset(partialTick);
        return onScreen(new Vec3(middle[0] - jumped[0], middle[1], middle[2] - jumped[1]));
    }

    /* The box on the approved screen the HUD is projected in, in points. */
    private record Box(int x0, int y0, int x1, int y1) {

        boolean holds(final Vec2 point) {
            return point.x >= x0 && point.x <= x1 && point.y >= y0 && point.y <= y1;
        }
    }

    /* How the last frame drew the world: its view, its projection, and where its eye stood. */
    private record Seen(Matrix4f view, Matrix4f projection, Vec3 eye) {
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

    /*
     * Paints the HUD in its box, in the approved screen's points, as the approved flight's own drawing lays it, its
     * small labels and its figures at {@code smallScale} and {@code figureScale} of the game's font.
     */
    private record Painter(GuiGraphics g, Font font, Box box, ShipHudArt.Colours colours, float smallScale,
                           float figureScale) {

        void paint(final Flown flown, final @Nullable Vec2 target, final @Nullable Vec2 world,
                   final ShipHudHome.Home home, final @Nullable Vec2 homeAt) {
            final int cx = (box.x0() + box.x1()) / 2;
            final int cy = (box.y0() + box.y1()) / 2 + 10;
            tape(flown.heading(), cx);
            // Under the heading tape the pulse jump's labels stand, the ladder's rungs kept clear of them.
            final ShipReadings.Pulse jump = flown.readings().pulse();
            final boolean labelled = jump.spool() > 0 || jump.on() || !jump.dropped().isEmpty();
            ladder(flown.pitch(), flown.bank(), cx, cy, box.y0() + (labelled ? UNDER_THE_LABELS : UNDER_THE_TAPE));
            marker(cx, cy + (int) Math.round(-flown.offPath() * LADDER_STEP));
            speed(flown.readings(), cy);
            altitude(flown, cy);
            corners(flown);
            home(home, flown.heading(), cx, labelled, homeAt);
            final ShipReadings.Pilot pilot = flown.readings().pilot();
            modes(cx, pilot.mode());
            if (target != null && box.holds(target)) {
                symbol(pilot, (int) target.x, (int) target.y);
            }
            pulse(flown.readings().pulse(), cx, cy, world);
        }

        /*
         * The way home, as the approved HUD draws it: its lines at the top right, as the reactor's stand at the top
         * left, the last lit; the home mark on the heading tape at the place's bearing while the tape spans it and no
         * pulse jump's labels stand there ({@code labelled}), and over the place where it is seen, {@code at}, with
         * how far it is under it.
         */
        private void home(final ShipHudHome.Home home, final double heading, final int cx, final boolean labelled,
                          final @Nullable Vec2 at) {
            final List<String> lines = home.lines();
            for (int k = 0; k < lines.size(); k++) {
                small(lines.get(k), box.x1() - 6, box.y0() + HOME_LINES + HOME_LINE * k, k == lines.size() - 1
                        ? colours.symbol() : colours.dim(), Align.RIGHT);
            }
            final double off = ((home.bearing() - heading) % FULL_TURN + FULL_TURN + FULL_TURN / 2.0) % FULL_TURN
                    - FULL_TURN / 2.0;
            if (!labelled && Math.abs(off) <= TAPE_DEGREES) {
                house(cx + (int) Math.round(off * TAPE_STEP), box.y0() + ON_THE_TAPE);
            }
            if (at != null && box.holds(at)) {
                house((int) at.x, (int) at.y);
                small(home.label(), (int) at.x, (int) at.y + UNDER_THE_HOUSE, colours.symbol(), Align.CENTRE);
            }
        }

        /* The home mark: a little house, its roof a chevron over a square, standing on {@code y}'s line. */
        private void house(final int x, final int y) {
            final int c = colours.symbol();
            for (int k = 0; k < HOUSE_ROOF; k++) {
                dot(x - k, y - HOUSE_ROOF - 1 + k, c);
                dot(x + k, y - HOUSE_ROOF - 1 + k, c);
            }
            frame(x - 3, y - 1, x + 4, y + HOUSE_ROOF, c);
        }

        /*
         * The pulse jump, as the approved HUD draws it: spooling, a ring round the middle filling from the top, what
         * spools and how long under the heading tape, and the key to hold; jumping, PULSE lit there, the way out, and
         * the nearest world bracketed with how far off and how long away; dropped out, why, framed, a moment.
         */
        private void pulse(final ShipReadings.Pulse pulse, final int cx, final int cy, final @Nullable Vec2 world) {
            final int y0 = box.y0();
            if (pulse.spool() > 0) {
                for (int a = 0; a < FULL_TURN; a += RING_STEP) {
                    final boolean on = a / (double) FULL_TURN <= pulse.spool() / (double) PERCENT;
                    final int x = (int) (cx + SPOOL_RING * Math.sin(Math.toRadians(a)));
                    final int y = (int) (cy - SPOOL_RING * Math.cos(Math.toRadians(a)));
                    fill(x - 1, y - 1, x + 2, y + 2, on ? colours.hot() : colours.unfilled());
                }
                label(GameText.resolve(ShipHudTexts.PULSE_SPOOL.with(Text.literal(String.format(Locale.ROOT, "%.1f",
                        pulse.left() / 10.0)))), cx, y0 + 32, colours.hot());
                small(GameText.resolve(ShipHudTexts.PULSE_HOLD.with(Text.literal(keyName(KeyActionsClient.mapping(
                        ShipPulse.KEY))))), cx, y0 + 48, colours.dim(), Align.CENTRE);
            } else if (pulse.on()) {
                final String word = GameText.resolve(ShipHudTexts.PULSE);
                final int half = (Math.round(font.width(word) * figureScale) + 14) / 2;
                fill(cx - half, y0 + 30, cx + half, y0 + 44, colours.hot());
                label(word, cx, y0 + 32, colours.lit());
                small(GameText.resolve(ShipHudTexts.PULSE_EXIT.with(Text.literal(keyName(Minecraft.getInstance()
                        .options.keyDown)))), cx, y0 + 48, colours.dim(), Align.CENTRE);
                final SolarBody body = ShipPulse.Drop.bodyOf(pulse.near());
                if (world != null && body != null && box.holds(world)) {
                    bracket((int) world.x, (int) world.y, GameText.resolve(ShipHudTexts.PULSE_WORLD.with(
                            Text.literal(GameText.resolve(ShipHudTexts.worldName(body))),
                            Text.literal(thousands(pulse.range())),
                            Text.literal(clock(pulse.seconds())))));
                }
            } else if (!pulse.dropped().isEmpty()) {
                final String off = GameText.resolve(ShipHudTexts.PULSE_OFF);
                final String why = why(pulse.dropped());
                final int lines = why.isEmpty() ? 1 : 2;
                final int half = (Math.round(Math.max(font.width(off), font.width(why)) * figureScale) + 20) / 2;
                frame(cx - half, y0 + 30, cx + half, y0 + 30 + 14 * lines + 6, colours.hot());
                label(off, cx, y0 + 34, colours.hot());
                if (!why.isEmpty()) {
                    label(why, cx, y0 + 48, colours.symbol());
                }
            }
        }

        /* The nearest world in a pulse jump bracketed where it is seen, with {@code text} beside it. */
        private void bracket(final int tx, final int ty, final String text) {
            final int c = colours.symbol();
            for (final int dx : new int[] {-1, 1}) {
                for (final int dy : new int[] {-1, 1}) {
                    hline(tx + dx * BRACKET, tx + dx * (BRACKET - BRACKET_ARM), ty + dy * BRACKET, c);
                    vline(tx + dx * BRACKET, ty + dy * BRACKET, ty + dy * (BRACKET - BRACKET_ARM), c);
                }
            }
            small(text, tx + BRACKET + 4, ty - 4, c, Align.LEFT);
        }

        /*
         * Why a ship dropped out of its pulse jump, or would not begin one, as its banner's second line says it; none
         * for its pilot.
         */
        private static String why(final String dropped) {
            final SolarBody body = ShipPulse.Drop.bodyOf(dropped);
            if (body != null) {
                return GameText.resolve(ShipHudTexts.PULSE_NEAR.with(Text.literal(GameText.resolve(
                        ShipHudTexts.worldName(body)))));
            }
            return switch (dropped) {
                case ShipPulse.Drop.SUN -> GameText.resolve(ShipHudTexts.PULSE_SUN);
                case ShipPulse.Drop.SHIP -> GameText.resolve(ShipHudTexts.PULSE_SHIP);
                case ShipPulse.Drop.DOCKED -> GameText.resolve(ShipHudTexts.PULSE_DOCKED);
                case ShipPulse.Drop.FUEL -> GameText.resolve(ShipHudTexts.PULSE_SPENT);
                default -> "";
            };
        }

        /* The key a key binding is on, as the HUD writes it. */
        private static String keyName(final KeyMapping mapping) {
            return mapping.getTranslatedKeyMessage().getString().toUpperCase(Locale.ROOT);
        }

        /* A distance in blocks, in thousands from a thousand, as the bracket reads it. */
        private static String thousands(final int blocks) {
            return blocks < THOUSAND ? Integer.toString(blocks) : Math.round(blocks / (double) THOUSAND) + "K";
        }

        /*
         * The engaged mode's own symbol where its target is: LAND's diamond on the landing spot with how far off it
         * is; ORBIT's caret on the climb ahead with how high it tops out and how long till then; DOCK's cross on the
         * other's collar, with how far off it is, how fast it closes and, in line, ALIGNED.
         */
        private void symbol(final ShipReadings.Pilot pilot, final int tx, final int ty) {
            final int c = colours.symbol();
            switch (pilot.mode()) {
                case "land" -> {
                    for (int k = 0; k < 9; k++) {
                        dot(tx - k, ty - 9 + k, c);
                        dot(tx + k, ty - 9 + k, c);
                        dot(tx - k, ty + 9 - k, c);
                        dot(tx + k, ty + 9 - k, c);
                    }
                    small(GameText.resolve(ShipHudTexts.PAD.with(Text.literal(Long.toString(Math.round(
                            pilot.range()))))), tx + 14, ty - 4, c, Align.LEFT);
                }
                case "dock" -> {
                    for (final int r : new int[] {10, 18}) {
                        for (int a = 0; a < 360; a += 8) {
                            dot((int) (tx + r * Math.cos(Math.toRadians(a))), (int) (ty + r * Math.sin(
                                    Math.toRadians(a))), c);
                        }
                    }
                    hline(tx - 26, tx - 12, ty, c);
                    hline(tx + 13, tx + 27, ty, c);
                    vline(tx, ty - 26, ty - 12, c);
                    vline(tx, ty + 13, ty + 27, c);
                    small(GameText.resolve(ShipHudTexts.RANGE.with(Text.literal(tenths(pilot.range())))), tx + 30,
                            ty - 12, c, Align.LEFT);
                    small(GameText.resolve(ShipHudTexts.CLOSE.with(Text.literal(tenths(pilot.closing())))), tx + 30,
                            ty - 2, c, Align.LEFT);
                    if (pilot.aligned()) {
                        small(GameText.resolve(ShipHudTexts.ALIGNED), tx + 30, ty + 8, colours.hot(), Align.LEFT);
                    }
                }
                case "orbit" -> {
                    hline(tx - 10, tx + 11, ty, c);
                    for (int k = 0; k < 7; k++) {
                        dot(tx - k, ty - k - 1, c);
                        dot(tx + k, ty - k - 1, c);
                    }
                    small(GameText.resolve(ShipHudTexts.APOGEE.with(Text.literal(metres(pilot.apogee())),
                            Text.literal(clock(pilot.seconds())))), tx + 16, ty - 4, c, Align.LEFT);
                }
                default -> {
                }
            }
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
        private void ladder(final double pitch, final double bank, final int cx, final int cy, final int top) {
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
                        if (rx > box.x0() + 30 && rx < box.x1() - 30 && ry > top && ry < box.y1() - 30) {
                            dot((int) rx, (int) ry, colours.symbol());
                        }
                    }
                    if (p != 0) {
                        final double rx = cx + side * half * ca + off * sa;
                        final double ry = cy + side * half * sa - off * ca;
                        if (rx > box.x0() + 30 && rx < box.x1() - 40 && ry > top && ry < box.y1() - 30) {
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

        /*
         * The speed on the left, boxed, with its scale, and the boost's bar under it: how far into the boost's speed
         * the ship is, and while it boosts, its one cost, its nose turning slowly.
         */
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
                final boolean on = (k + 0.5) / BOOST_CELLS <= readings.boost() / (double) PERCENT;
                fill(sx + k * 5, cy + 24, sx + k * 5 + 4, cy + 28, on ? boostColour : colours.unfilled());
            }
            if (readings.boosting()) {
                small(GameText.resolve(ShipHudTexts.TURN_SLOW), sx, cy + 32, colours.hot(), Align.LEFT);
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

        /*
         * The reactor's power and its deuterium above, burning faster in a pulse jump; the nozzles' angle and the
         * gear below.
         */
        private void corners(final Flown flown) {
            small(GameText.resolve(ShipHudTexts.POWER.with(Text.literal(megawatts(flown.drawn())),
                    Text.literal(megawatts(flown.made())))), box.x0() + 6, box.y0() + 32, colours.dim(), Align.LEFT);
            final Text left = Text.literal(Long.toString(Math.round(flown.deuterium() * PERCENT)));
            small(GameText.resolve(flown.readings().pulse().on() ? ShipHudTexts.PULSE_FUEL.with(left,
                    Text.literal(Long.toString(Math.round(PulseCourse.BURNS)))) : ShipHudTexts.FUEL.with(left)),
                    box.x0() + 6, box.y0() + 42, colours.dim(), Align.LEFT);
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

        /* The autopilot's three modes along the bottom, the one engaged, {@code engaged} by its name, lit. */
        private void modes(final int cx, final String engaged) {
            final int mx = cx - 70;
            small(GameText.resolve(ShipHudTexts.AUTOPILOT), mx - 18, box.y1() - 15, colours.dim(), Align.LEFT);
            int k = 0;
            for (final Map.Entry<ShipAutopilot.Mode, TextKey> each : MODES.entrySet()) {
                final String mode = GameText.resolve(each.getValue());
                final int w = Math.round(font.width(mode) * smallScale) + 8;
                final int bx = mx + k++ * MODE_STEP;
                if (each.getKey().serializedName().equals(engaged)) {
                    fill(bx, box.y1() - 18, bx + w, box.y1() - 6, colours.symbol());
                    small(mode, bx + 4, box.y1() - 16, colours.lit(), Align.LEFT);
                } else {
                    frame(bx, box.y1() - 18, bx + w, box.y1() - 6, colours.dim());
                    small(mode, bx + 4, box.y1() - 16, colours.dim(), Align.LEFT);
                }
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

        /* A figure to a tenth, as the docking cross reads it. */
        private static String tenths(final double value) {
            return String.format(Locale.ROOT, "%.1f", value);
        }

        /* A height as the climb's caret reads it: in metres, or in thousands of them from a thousand. */
        private static String metres(final int metres) {
            return metres < THOUSAND ? Integer.toString(metres)
                    : String.format(Locale.ROOT, "%.1fK", metres / (double) THOUSAND);
        }

        /* A time in minutes and seconds. */
        private static String clock(final int seconds) {
            return String.format(Locale.ROOT, "%d:%02d", seconds / 60, seconds % 60);
        }

        private void small(final String text, final int x, final int y, final int colour, final Align align) {
            final int width = Math.round(font.width(text) * smallScale);
            final int left = switch (align) {
                case LEFT -> x;
                case CENTRE -> x - width / 2;
                case RIGHT -> x - width;
            };
            Draw.textScaled(g, font, text, left, y, colour, smallScale);
        }

        /* A label in the figures' size, its middle at {@code x}. */
        private void label(final String text, final int x, final int y, final int colour) {
            Draw.textScaled(g, font, text, x - Math.round(font.width(text) * figureScale) / 2, y, colour,
                    figureScale);
        }

        /* A figure in a box, its right edge at {@code right}. */
        private void figure(final String text, final int right, final int y) {
            Draw.textScaled(g, font, text, right - Math.round(font.width(text) * figureScale), y, colours.symbol(),
                    figureScale);
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
