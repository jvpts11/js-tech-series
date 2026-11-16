/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.client.ship;

import dev.jstech.core.client.effect.ClientEffects;
import dev.jstech.core.client.model.kit.KitModels;
import dev.jstech.core.client.moving.ClientMovingStructures;
import dev.jstech.core.client.moving.ClientStructure;
import dev.jstech.core.dimension.DimensionRulesData;
import dev.jstech.core.moving.StructureFrame;
import dev.jstech.core.multiblock.shape.CellSpace;
import dev.jstech.core.moving.YardLayout;
import dev.jstech.space.JsSpace;
import dev.jstech.space.api.ShipPart;
import dev.jstech.space.api.ShipPartKind;
import dev.jstech.space.api.ShipParts;
import dev.jstech.space.api.ShipKits;
import dev.jstech.space.api.SpaceWorld;
import dev.jstech.space.api.SpaceWorlds;
import dev.jstech.space.descent.DescentFlight;
import dev.jstech.space.ship.AirReserveEntity;
import dev.jstech.space.ship.ShipAboard;
import dev.jstech.space.ship.ShipDesign;
import dev.jstech.space.ship.ShipLayout;
import dev.jstech.space.ship.ShipPartModels;
import dev.jstech.space.ship.ShipReadings;
import dev.jstech.space.shipyard.ShipyardLayout;
import dev.jstech.space.universe.SolarBody;
import dev.jstech.space.universe.SolarSystemWorlds;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.DoubleSupplier;
import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.jetbrains.annotations.Nullable;

/**
 * A ship's engines on a player's game, while they run, as the approved mock draws them. The mouths of its main
 * engines' bells glow, faintly where the air is thick and leaves them cold, brightly where they push, in thin air and
 * in space, and there a jet grows out of each with the throttle, longer while it boosts, leaving a heat trail in thin
 * air and clean in space. Its air engines blow out of their nozzles where they breathe: down holding the ship up, a jet
 * of flame as the approved landing on feet draws it, and back as they push, as long as the throttle. Where the air
 * leaves its nozzles short each of its thrusters fires down a jet of cold gas, and near the land the
 * dust under the jets blowing down is blown out round the ship, the world's own, flying out flat on the Moon with no
 * air to hold it. Shown for the ships whose consoles are drawn.
 */
public final class ShipExhaust {

    /* The jet each nozzle blows down holding the ship up, as the approved landing on feet draws it; the exhaust it
       blows back pushing; and each thruster's cold gas. */
    private static final ResourceLocation JET = ResourceLocation.fromNamespaceAndPath(JsSpace.MODID, "lift_jet");
    private static final ResourceLocation EXHAUST = ResourceLocation.fromNamespaceAndPath(JsSpace.MODID,
            "nozzle_exhaust");
    private static final ResourceLocation GAS = ResourceLocation.fromNamespaceAndPath(JsSpace.MODID, "thruster_jet");
    /* A bell's mouth glowing as it pushes and faintly as the air leaves it cold; its jet in thin air and in space. */
    private static final ResourceLocation GLOW = ResourceLocation.fromNamespaceAndPath(JsSpace.MODID, "bell_glow");
    private static final ResourceLocation IDLE = ResourceLocation.fromNamespaceAndPath(JsSpace.MODID, "bell_idle");
    private static final ResourceLocation BELL_AIR = ResourceLocation.fromNamespaceAndPath(JsSpace.MODID,
            "bell_jet_air");
    private static final ResourceLocation BELL_VACUUM = ResourceLocation.fromNamespaceAndPath(JsSpace.MODID,
            "bell_jet_vacuum");
    /* How long an unseen console still blows, how often the dust rises, in ticks; how near the land, in blocks. */
    private static final int SEEN_FOR = 5;
    private static final int DUST_EVERY = 10;
    private static final double DUST_HEIGHT = 8;
    /* The letter of a nozzle in an air engine's kit, and of a thruster in a thrusters' kit. */
    private static final char NOZZLE = 'V';
    private static final char THRUSTER = 'R';
    private static final Vec3 UP = new Vec3(0, 1, 0);
    private static final Vec3 DOWN = new Vec3(0, -1, 0);
    private static final double HALF = 0.5;
    /* How far up the Moon's dust flies as it goes out, against how far out, as a touchdown's; a jet at the middle. */
    private static final double RAY_RISE = 0.06;
    private static final double NO_WAY = 1.0e-6;
    /*
     * As the approved mock draws them: a jet's length with the throttle shut, a share of its file's, and how many
     * times longer boosting; a bells' jet shorter than the least share is not lit, the air leaving them cold. A bell
     * whose mouth is so wide, in blocks, the twin bells', is drawn at its effects' own size.
     */
    private static final double IDLING = 0.35;
    private static final double BOOSTED = 1.7;
    private static final double LEAST = 0.1;
    private static final double PERCENT = 100;
    private static final double BELL_RADIUS = 0.74;
    private static final DoubleSupplier WHOLE = () -> 1.0;
    /*
     * The consoles drawn and what they blow; the nozzles' and the thrusters' blocks of each design, and where its
     * engines' models blow out, in the ship's own frame.
     */
    private static final Map<AirReserveEntity, Blown> SEEN = new WeakHashMap<>();
    private static final Map<ShipDesign, List<ShipLayout.Cell>> NOZZLES = new HashMap<>();
    private static final Map<ShipDesign, List<ShipLayout.Cell>> THRUSTERS = new HashMap<>();
    private static final Map<ShipDesign, List<Mouth>> EXHAUSTS = new HashMap<>();
    /* The world and the resources the nozzles, thrusters and exhausts were found with. */
    private static WeakReference<ClientLevel> readIn = new WeakReference<>(null);
    private static long readAt = -1;

    private ShipExhaust() {
    }

    /** Hands the game's bus the jets, once, from the mod's player-side entry point. */
    public static void listen() {
        NeoForge.EVENT_BUS.addListener(ClientTickEvent.Post.class, event -> tick());
    }

    /** {@code console} was drawn: its ship's engines blow while it is. */
    public static void seen(final AirReserveEntity console) {
        final ClientLevel level = Minecraft.getInstance().level;
        if (level != null) {
            SEEN.computeIfAbsent(console, key -> new Blown()).seen = level.getGameTime();
        }
    }

    private static void tick() {
        final ClientLevel level = Minecraft.getInstance().level;
        if (level == null || SEEN.isEmpty()) {
            return;
        }
        final long now = level.getGameTime();
        final Iterator<Map.Entry<AirReserveEntity, Blown>> each = SEEN.entrySet().iterator();
        while (each.hasNext()) {
            final Map.Entry<AirReserveEntity, Blown> entry = each.next();
            final AirReserveEntity console = entry.getKey();
            final Blown blown = entry.getValue();
            if (console.isRemoved() || now - blown.seen > SEEN_FOR) {
                blown.stop();
                each.remove();
            } else if (console.flight().running() && console.ship().isPresent()) {
                blow(level, console, console.ship().get(), blown, now);
            } else {
                blown.stop();
            }
        }
    }

    /* Holds what the engines of {@code console}'s ship blow, and raises the dust under its jets blowing down. */
    private static void blow(final ClientLevel level, final AirReserveEntity console, final ShipAboard ship,
                             final Blown blown, final long now) {
        final BlockPos at = console.getBlockPos();
        final ClientStructure structure = ClientMovingStructures.byPlot(YardLayout.plotOfBlock(at.getX(), at.getZ()));
        if (structure == null || !structure.ready()) {
            blown.stop();
            return;
        }
        final Drawn drawn = new Drawn(structure, at, ship);
        final boolean due = now - blown.dust >= DUST_EVERY;
        final List<Vec3> down = new ArrayList<>();
        nozzles(level, drawn, console, blown.jets, down);
        thrusters(level, drawn, console.flight(), blown.gas, down);
        bells(level, drawn, console, blown);
        if (due && dust(level, down, worldOf(level))) {
            blown.dust = now;
        }
    }

    /*
     * Blows out of the air engines' nozzles where they breathe: down from their blocks holding the ship up, as the
     * approved landing on feet draws it, and back out of their exits swung back for flight as they push, as long as
     * the throttle; the jets blowing down are added to {@code down}. Each jet follows the ship as it moves, asked
     * where it is every tick.
     */
    private static void nozzles(final ClientLevel level, final Drawn drawn, final AirReserveEntity console,
                                final Jets jets, final List<Vec3> down) {
        final ShipReadings flight = console.flight();
        if (flight.lifting()) {
            final List<ShipLayout.Cell> cells = cells(level, drawn.ship().design(), ShipPartKind.AIR_ENGINE, NOZZLE,
                    NOZZLES);
            for (int i = 0; i < cells.size(); i++) {
                final ShipLayout.Cell cell = cells.get(i);
                jets.hold(i, JET, () -> drawn.under(cell), drawn::down, 1.0F, WHOLE);
                down.add(drawn.under(cell));
            }
            jets.keep(cells.size());
        } else if (flight.breathing()) {
            final List<Mouth> nozzles = exhausts(level, drawn.ship().design(), ShipEngineExhausts.Exhaust.Kind.NOZZLE);
            for (int i = 0; i < nozzles.size(); i++) {
                final Mouth nozzle = nozzles.get(i);
                jets.hold(i, EXHAUST, () -> drawn.place(nozzle.at()), () -> drawn.turned(nozzle.way()), 1.0F,
                        () -> length(console.flight()));
            }
            jets.keep(nozzles.size());
        } else {
            jets.stop();
        }
    }

    /* Fires each thruster's cold gas down while they hold the ship up; the jets are added to {@code down}. */
    private static void thrusters(final ClientLevel level, final Drawn drawn, final ShipReadings flight,
                                  final Jets gas, final List<Vec3> down) {
        if (!flight.thrusting()) {
            gas.stop();
            return;
        }
        final List<ShipLayout.Cell> cells = cells(level, drawn.ship().design(), ShipPartKind.THRUSTERS, THRUSTER,
                THRUSTERS);
        for (int i = 0; i < cells.size(); i++) {
            final ShipLayout.Cell cell = cells.get(i);
            gas.hold(i, GAS, () -> drawn.under(cell), drawn::down, 1.0F, WHOLE);
            down.add(drawn.under(cell));
        }
    }

    /*
     * Glows at the mouth of each bell, brightly where the bells push and faintly where the air leaves them cold, and
     * where they push lights a jet out of it as long as the throttle and their share of the push: in thin air with
     * its heat trail, in space clean. Each is as big as its bell's mouth is wide.
     */
    private static void bells(final ClientLevel level, final Drawn drawn, final AirReserveEntity console,
                              final Blown blown) {
        final List<Mouth> mouths = exhausts(level, drawn.ship().design(), ShipEngineExhausts.Exhaust.Kind.BELL);
        final boolean pushing = pushed(console.flight()) >= LEAST;
        if (!pushing) {
            blown.bells.stop();
        }
        if (mouths.isEmpty()) {
            return;
        }
        final double y = drawn.place(mouths.get(0).at()).y;
        final ResourceLocation jet = DimensionRulesData.of(level).at(y).pressure() > 0 ? BELL_AIR : BELL_VACUUM;
        for (int i = 0; i < mouths.size(); i++) {
            final Mouth mouth = mouths.get(i);
            final float scale = (float) (mouth.radius() / BELL_RADIUS);
            final Supplier<Vec3> at = () -> drawn.place(mouth.at());
            final Supplier<Vec3> way = () -> drawn.turned(mouth.way());
            blown.glows.hold(i, pushing ? GLOW : IDLE, at, way, scale, WHOLE);
            if (pushing) {
                blown.bells.hold(i, jet, at, way, scale, () -> pushed(console.flight()));
            }
        }
    }

    /* How long a jet is as {@code flight} has it, a share of its file's: from idling to whole with the throttle. */
    private static double length(final ShipReadings flight) {
        return (IDLING + (1 - IDLING) * flight.throttle() / PERCENT) * (flight.boosting() ? BOOSTED : 1.0);
    }

    /* How long the bells' jet is as {@code flight} has it: as the throttle has it, as much as the bells push. */
    private static double pushed(final ShipReadings flight) {
        return length(flight) * flight.bells() / PERCENT;
    }

    /*
     * Raises the dust of the world {@code level} is under each of {@code jets} blowing down near the land: whether it
     * did.
     */
    private static boolean dust(final ClientLevel level, final List<Vec3> jets, final @Nullable SolarBody world) {
        if (jets.isEmpty()) {
            return false;
        }
        Vec3 middle = Vec3.ZERO;
        for (final Vec3 jet : jets) {
            middle = middle.add(jet.scale(1.0 / jets.size()));
        }
        boolean raised = false;
        for (final Vec3 jet : jets) {
            final int ground = level.getHeight(Heightmap.Types.MOTION_BLOCKING, (int) Math.floor(jet.x),
                    (int) Math.floor(jet.z));
            if (jet.y - ground < DUST_HEIGHT) {
                raise(world, new Vec3(jet.x, ground, jet.z), middle);
                raised = true;
            }
        }
        return raised;
    }

    /*
     * Raises the dust of {@code world} at {@code foot}, under a jet whose fellows stand round {@code middle}: on the
     * Moon, with no air to hold it, it flies out flat away from the ship, as a touchdown's does; on Mars its own rises;
     * elsewhere the land's.
     */
    private static void raise(final @Nullable SolarBody world, final Vec3 foot, final Vec3 middle) {
        if (world != SolarBody.MOON) {
            ClientEffects.play(world == SolarBody.MARS ? DescentFlight.MARS_DUST : DescentFlight.DUST, foot, UP);
            return;
        }
        final double outX = foot.x - middle.x;
        final double outZ = foot.z - middle.z;
        final double across = Math.max(Math.hypot(outX, outZ), NO_WAY);
        ClientEffects.play(DescentFlight.MOON_DUST, foot, new Vec3(outX / across, RAY_RISE, outZ / across));
    }

    /* The world of the Solar System {@code level} is, by its dimension; none for another. */
    private static @Nullable SolarBody worldOf(final ClientLevel level) {
        for (final Map.Entry<ResourceLocation, SpaceWorld> world : SpaceWorlds.all(level).entrySet()) {
            if (world.getValue().dimension().equals(level.dimension())) {
                return SolarSystemWorlds.bodyOf(world.getKey()).orElse(null);
            }
        }
        return null;
    }

    /*
     * Where the engines of a ship of {@code design} blow out of the kind {@code kind}, in the ship's own frame, as
     * their parts' models say ({@link ShipEngineExhausts}), each model set where the ship is drawn with it, read once.
     */
    private static List<Mouth> exhausts(final ClientLevel level, final ShipDesign design,
                                        final ShipEngineExhausts.Exhaust.Kind kind) {
        read(level);
        final List<Mouth> all = EXHAUSTS.computeIfAbsent(design, key -> {
            final List<Mouth> out = new ArrayList<>();
            for (final ShipPartModels.Placed part : ShipPartModels.of(ShipParts.all(level), ShipKits.all(level),
                    design)) {
                // Flipped left for right about the ship's centre line, a place from x stands at -x.
                final double flip = part.flipped() ? -1 : 1;
                for (final ShipEngineExhausts.Exhaust exhaust : ShipEngineExhausts.of(ShipPartsRenderer.cabinOf(
                        part.model()))) {
                    out.add(new Mouth(exhaust.kind(), new Vec3(flip * exhaust.at().x(), exhaust.at().y(),
                            exhaust.at().z() + part.z()), new Vec3(flip * exhaust.way().x(), exhaust.way().y(),
                            exhaust.way().z()), exhaust.radius()));
                }
            }
            return List.copyOf(out);
        });
        return all.stream().filter(mouth -> mouth.kind() == kind).toList();
    }

    /*
     * The blocks lettered {@code letter} of the parts of {@code kind} of a ship of {@code design}, in its places, read
     * once into {@code found}.
     */
    private static List<ShipLayout.Cell> cells(final ClientLevel level, final ShipDesign design,
                                               final ShipPartKind kind, final char letter,
                                               final Map<ShipDesign, List<ShipLayout.Cell>> found) {
        read(level);
        return found.computeIfAbsent(design, key -> {
            final Map<ResourceLocation, ShipPart> parts = ShipParts.all(level);
            final List<ResourceLocation> all = new ArrayList<>(design.spine());
            all.addAll(design.hung());
            final List<ShipLayout.Cell> cells = new ArrayList<>();
            for (final ShipLayout.Block block : ShipyardLayout.blocks(parts, ShipKits.all(level), design)) {
                final ShipPart part = block.part() < all.size() ? parts.get(all.get(block.part())) : null;
                if (part != null && part.kind() == kind && block.letter() == letter) {
                    cells.add(block.cell());
                }
            }
            return List.copyOf(cells);
        });
    }

    /* Forgets what was read of the designs when the world or the resources they are read with are others. */
    private static void read(final ClientLevel level) {
        if (readIn.get() != level || readAt != KitModels.reloadedAt()) {
            NOZZLES.clear();
            THRUSTERS.clear();
            EXHAUSTS.clear();
            readIn = new WeakReference<>(level);
            readAt = KitModels.reloadedAt();
        }
    }

    /*
     * Where an engine of a ship blows out, in the ship's own frame: what blows there, the middle of its mouth, the way
     * it blows and how wide its mouth is, in blocks.
     */
    private record Mouth(ShipEngineExhausts.Exhaust.Kind kind, Vec3 at, Vec3 way, double radius) {
    }

    /*
     * A ship as it is drawn: its moving structure, the block of its console in its yard, and how it is laid there;
     * the places and ways of its own frame (from its nose, a block from x to x + 1) in the world, as the parts'
     * renderer sets them and as its frame stands now.
     */
    private record Drawn(ClientStructure structure, BlockPos console, ShipAboard ship) {

        /* The bottom of the block at {@code cell}: where a jet blowing down leaves it. */
        private Vec3 under(final ShipLayout.Cell cell) {
            return place(new Vec3(cell.x() + HALF, cell.y(), cell.z() + HALF));
        }

        /* The way down from the ship, as its frame is turned in the world. */
        private Vec3 down() {
            return turned(DOWN);
        }

        /* Where in the world the place {@code at} of the ship's own frame is. */
        private Vec3 place(final Vec3 at) {
            final double[] laid = new double[3];
            CellSpace.toWorld(new double[] {at.x, at.y, at.z}, this.ship.orientation(), laid);
            final double x = this.console.getX() + this.ship.nose().getX() + laid[0];
            final double y = this.console.getY() + this.ship.nose().getY() + laid[1];
            final double z = this.console.getZ() + this.ship.nose().getZ() + laid[2];
            final StructureFrame frame = this.structure.frame();
            return new Vec3(frame.worldX(x, y, z), frame.worldY(x, y, z), frame.worldZ(x, y, z));
        }

        /* The way {@code way} of the ship's own frame in the world, as it is laid in its yard and its frame turned. */
        private Vec3 turned(final Vec3 way) {
            final double[] laid = new double[3];
            CellSpace.toWorld(new double[] {HALF + way.x, HALF + way.y, HALF + way.z}, this.ship.orientation(), laid);
            final double x = laid[0] - HALF;
            final double y = laid[1] - HALF;
            final double z = laid[2] - HALF;
            final StructureFrame frame = this.structure.frame();
            return new Vec3(frame.turnX(x, y, z), frame.turnY(x, y, z), frame.turnZ(x, y, z));
        }
    }

    /*
     * Effects held one at each of a few places, each its own effect: one held as another takes the place of the one
     * it held, else left to play.
     */
    private static final class Jets {

        private final List<Object> holders = new ArrayList<>();
        private final List<@Nullable ResourceLocation> effects = new ArrayList<>();

        /*
         * Holds {@code effect} as its {@code i}th, at {@code at} facing {@code way}, {@code scale} times its size and
         * {@code reach} times as long, all asked as it plays.
         */
        private void hold(final int i, final ResourceLocation effect, final Supplier<Vec3> at,
                          final Supplier<Vec3> way, final float scale, final DoubleSupplier reach) {
            while (this.holders.size() <= i) {
                this.holders.add(new Object());
                this.effects.add(null);
            }
            final Object holder = this.holders.get(i);
            if (!effect.equals(this.effects.get(i)) || !ClientEffects.holds(holder)) {
                ClientEffects.holdReaching(holder, effect, at, way, scale, reach);
                this.effects.set(i, effect);
            }
        }

        /* Lets go of all but its first {@code count}: they blow no more, and what they blew dies away. */
        private void keep(final int count) {
            for (int i = count; i < this.holders.size(); i++) {
                ClientEffects.release(this.holders.get(i));
                this.effects.set(i, null);
            }
        }

        /* Lets them go: they blow no more, and what they blew dies away. */
        private void stop() {
            this.holders.forEach(ClientEffects::release);
            Collections.fill(this.effects, null);
        }
    }

    /*
     * What a drawn console blows: the tick it was last drawn at, the tick its dust last rose, its nozzles' jets, its
     * thrusters' gas, its bells' glow and jets.
     */
    private static final class Blown {

        private final Jets jets = new Jets();
        private final Jets gas = new Jets();
        private final Jets glows = new Jets();
        private final Jets bells = new Jets();
        private long seen;
        private long dust;

        /* Lets all it blows go: they blow no more, and their flames, gas and glow die away. */
        private void stop() {
            this.jets.stop();
            this.gas.stop();
            this.glows.stop();
            this.bells.stop();
        }
    }
}
