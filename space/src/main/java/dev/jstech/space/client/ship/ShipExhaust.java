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
import dev.jstech.core.moving.StructureFrame;
import dev.jstech.core.moving.YardLayout;
import dev.jstech.core.multiblock.Orientation;
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
import dev.jstech.space.shipyard.ShipyardLayout;
import dev.jstech.space.universe.SolarBody;
import dev.jstech.space.universe.SolarSystemWorlds;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
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
 * What holds a hovering ship up, on a player's game: each nozzle of its air engines blows down a jet of flame, as the
 * approved landing on feet draws them, and, where the air leaves its nozzles short, each of its thrusters fires down a
 * jet of cold gas; near the land the dust under the ship is blown out round it, the world's own, flying out flat on the
 * Moon with no air to hold it. Shown for the ships whose consoles are drawn, while their consoles say their nozzles or
 * their thrusters hold them up.
 */
public final class ShipExhaust {

    /* The jet of flame each nozzle blows down, as the approved landing on feet draws it, and each thruster's gas. */
    private static final ResourceLocation JET = ResourceLocation.fromNamespaceAndPath(JsSpace.MODID, "lift_jet");
    private static final ResourceLocation GAS = ResourceLocation.fromNamespaceAndPath(JsSpace.MODID, "thruster_jet");
    /* How long an unseen console still blows, how often the dust rises, in ticks; how near the land, in blocks. */
    private static final int SEEN_FOR = 5;
    private static final int DUST_EVERY = 10;
    private static final double DUST_HEIGHT = 8;
    /* The letter of a nozzle in an air engine's kit, and of a thruster in a thrusters' kit. */
    private static final char NOZZLE = 'V';
    private static final char THRUSTER = 'R';
    private static final Vec3 UP = new Vec3(0, 1, 0);
    /* How far up the Moon's dust flies as it goes out, against how far out, as a touchdown's; a jet at the middle. */
    private static final double RAY_RISE = 0.06;
    private static final double NO_WAY = 1.0e-6;
    /* The consoles drawn and what they blow; the nozzles and the thrusters of each design, in the ship's places. */
    private static final Map<AirReserveEntity, Blown> SEEN = new WeakHashMap<>();
    private static final Map<ShipDesign, List<ShipLayout.Cell>> NOZZLES = new HashMap<>();
    private static final Map<ShipDesign, List<ShipLayout.Cell>> THRUSTERS = new HashMap<>();
    /* The world and the resources the nozzles and thrusters were found with. */
    private static WeakReference<ClientLevel> readIn = new WeakReference<>(null);
    private static long readAt = -1;

    private ShipExhaust() {
    }

    /** Hands the game's bus the jets, once, from the mod's player-side entry point. */
    public static void listen() {
        NeoForge.EVENT_BUS.addListener(ClientTickEvent.Post.class, event -> tick());
    }

    /** {@code console} was drawn: its ship's jets are blown while it is. */
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
            } else if ((console.flight().lifting() || console.flight().thrusting()) && console.ship().isPresent()) {
                blow(level, console, console.ship().get(), blown, now);
            } else {
                blown.stop();
            }
        }
    }

    /*
     * Holds the jets of {@code console}'s ship, one a nozzle while its nozzles hold it up and one a thruster while its
     * thrusters do, and raises the dust under it near the land.
     */
    private static void blow(final ClientLevel level, final AirReserveEntity console, final ShipAboard ship,
                             final Blown blown, final long now) {
        final BlockPos at = console.getBlockPos();
        final ClientStructure structure = ClientMovingStructures.byPlot(YardLayout.plotOfBlock(at.getX(), at.getZ()));
        if (structure == null || !structure.ready()) {
            blown.stop();
            return;
        }
        final boolean dust = now - blown.dust >= DUST_EVERY;
        final SolarBody world = worldOf(level);
        final boolean nozzlesDust = jets(level, structure, at, ship, console.flight().lifting(), JET,
                cells(level, ship.design(), ShipPartKind.AIR_ENGINE, NOZZLE, NOZZLES), blown.jets, dust, world);
        final boolean thrustersDust = jets(level, structure, at, ship, console.flight().thrusting(), GAS,
                cells(level, ship.design(), ShipPartKind.THRUSTERS, THRUSTER, THRUSTERS), blown.gas, dust, world);
        if (nozzlesDust || thrustersDust) {
            blown.dust = now;
        }
    }

    /*
     * Holds the jet {@code effect} under each of {@code cells} while {@code on}, its holders {@code held}, else lets
     * them go; near the land, {@code dust} due, raises the dust of {@code world} under them: whether it did.
     */
    private static boolean jets(final ClientLevel level, final ClientStructure structure, final BlockPos at,
                                final ShipAboard ship, final boolean on, final ResourceLocation effect,
                                final List<ShipLayout.Cell> cells, final List<Object> held, final boolean dust,
                                final @Nullable SolarBody world) {
        if (!on) {
            held.forEach(ClientEffects::release);
            return false;
        }
        while (held.size() < cells.size()) {
            held.add(new Object());
        }
        final List<Vec3> places = new ArrayList<>();
        Vec3 middle = Vec3.ZERO;
        for (int i = 0; i < cells.size(); i++) {
            final ShipLayout.Cell cell = cells.get(i);
            // Each jet follows its block as the ship moves, asked where it is every tick.
            if (!ClientEffects.holds(held.get(i))) {
                ClientEffects.hold(held.get(i), effect, () -> under(structure, at, ship, cell),
                        () -> down(structure));
            }
            final Vec3 jet = under(structure, at, ship, cell);
            places.add(jet);
            middle = middle.add(jet.scale(1.0 / cells.size()));
        }
        boolean raised = false;
        for (final Vec3 jet : places) {
            final int ground = level.getHeight(Heightmap.Types.MOTION_BLOCKING, (int) Math.floor(jet.x),
                    (int) Math.floor(jet.z));
            if (dust && jet.y - ground < DUST_HEIGHT) {
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

    /* Where in the world the bottom of the block at {@code cell} of {@code ship} is, its console at {@code at}. */
    private static Vec3 under(final ClientStructure structure, final BlockPos at, final ShipAboard ship,
                              final ShipLayout.Cell cell) {
        final Orientation turn = ship.orientation();
        final int[] off = turn.apply(cell.x(), cell.y(), cell.z());
        final double x = at.getX() + ship.nose().getX() + off[0] + 0.5;
        final double y = at.getY() + ship.nose().getY() + off[1];
        final double z = at.getZ() + ship.nose().getZ() + off[2] + 0.5;
        final StructureFrame frame = structure.frame();
        return new Vec3(frame.worldX(x, y, z), frame.worldY(x, y, z), frame.worldZ(x, y, z));
    }

    /* The way down from the ship, as its frame is turned in the world. */
    private static Vec3 down(final ClientStructure structure) {
        final StructureFrame frame = structure.frame();
        return new Vec3(frame.turnX(0, -1, 0), frame.turnY(0, -1, 0), frame.turnZ(0, -1, 0));
    }

    /*
     * The blocks lettered {@code letter} of the parts of {@code kind} of a ship of {@code design}, in its places, read
     * once into {@code found}.
     */
    private static List<ShipLayout.Cell> cells(final ClientLevel level, final ShipDesign design,
                                               final ShipPartKind kind, final char letter,
                                               final Map<ShipDesign, List<ShipLayout.Cell>> found) {
        // The parts a design is read with are the world's and the resources', so another of either reads it again.
        if (readIn.get() != level || readAt != KitModels.reloadedAt()) {
            NOZZLES.clear();
            THRUSTERS.clear();
            readIn = new WeakReference<>(level);
            readAt = KitModels.reloadedAt();
        }
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

    /* What a drawn console blows: the tick it was last drawn at, the tick its dust last rose, its jets' holders. */
    private static final class Blown {

        private final List<Object> jets = new ArrayList<>();
        private final List<Object> gas = new ArrayList<>();
        private long seen;
        private long dust;

        /* Lets its jets go: they blow no more, and their flames and gas die away. */
        private void stop() {
            this.jets.forEach(ClientEffects::release);
            this.gas.forEach(ClientEffects::release);
        }
    }
}
