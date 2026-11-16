/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.ship;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.jstech.core.input.KeyAction;
import dev.jstech.core.input.KeyActions;
import dev.jstech.core.moving.IStructureBody;
import dev.jstech.core.moving.IStructureMotion;
import dev.jstech.core.moving.LevelMovingStructures;
import dev.jstech.core.moving.MovingStructure;
import dev.jstech.core.moving.StructureFrame;
import dev.jstech.core.moving.StructureMounts;
import dev.jstech.core.moving.StructurePose;
import dev.jstech.space.JsSpace;
import dev.jstech.space.network.ShipPulsePayload;
import dev.jstech.space.rocket.SpaceAhead;
import dev.jstech.space.universe.SolarBody;
import dev.jstech.space.universe.SystemSpaces;
import java.util.Locale;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

/**
 * A ship's pulse jump, in the spirit of No Man's Sky: held on its key at the helm, B until the player changes it, in
 * its system's space away from the worlds, its drive spools a second and a half and it jumps, level along its nose,
 * thousands of metres a second ({@link PulseCourse}), barely turning, its reactor burning its deuterium six times as
 * fast. It drops out as its pilot holds back, by itself as its way comes over the low orbit of a world, or to the edge
 * of the Sun, near another ship, or with its deuterium spent.
 *
 * <p>No deck can be walked going that fast, nor the land loaded under it, so the ship stands still where it began and
 * only its jump goes on: how far it has gone ({@link Jump}), told every tick to whoever is aboard, so their game draws
 * the worlds and the Sun moving round it ({@link ShipPulsePayload}). Dropping out, it is set where the jump took it,
 * once the land there has loaded, its flight going on as it was; told in the same tick, their game draws no jump.
 */
public final class ShipPulse {

    /** The key that jumps the ship its player pilots. */
    public static final KeyAction KEY = KeyActions.declare(KeyAction.builder(
            ResourceLocation.fromNamespaceAndPath(JsSpace.MODID, "ship_pulse"), ShipFlightTexts.PULSE_KEY)
            .key(GLFW.GLFW_KEY_B).atTheHelm());
    /** How many ticks its drive spools. */
    public static final int SPOOL_TICKS = (int) Math.round(PulseCourse.SPOOL_SECONDS / ShipFlightModel.TICK);
    /* How long it is shown why a ship dropped out of its jump, in ticks. */
    private static final int SHOWN = 60;
    /* How far round a ship's box, in blocks, a player is aboard it to see its jump. */
    private static final double ABOARD = 4;
    /* How far it goes on its way a tick, in blocks. */
    private static final double A_TICK = PulseCourse.SPEED * ShipFlightModel.TICK;
    /* When each ship's pilot last held its key, how far each ship's drive has spooled, and why each dropped out. */
    private static final HeldAtTheHelm HELD = new HeldAtTheHelm();
    private static final Map<MovingStructure, Integer> SPOOLED = new WeakHashMap<>();
    private static final Map<MovingStructure, Dropped> DROPPED = new WeakHashMap<>();

    private ShipPulse() {
    }

    /** Declares the key, once, from the mod's constructor: loading the class declares it. */
    public static void declare() {
        // Loading the class declares it.
    }

    /** {@code player}'s game says whether they hold the pulse jump's key: heard only from a pilot at a ship's helm. */
    public static void told(final ServerPlayer player, final boolean held) {
        HELD.told(player, held);
    }

    /** Whether {@code ship}'s pilot holds its pulse jump's key at the game's tick {@code now}. */
    public static boolean held(final MovingStructure ship, final long now) {
        return HELD.held(ship, now);
    }

    /** Has {@code ship}'s pulse jump's key held at {@code now}, as a pilot holding it would: for a test. */
    public static void hold(final MovingStructure ship, final long now) {
        HELD.hold(ship, now);
    }

    /**
     * Whether {@code ship} is docked, to another ship or with one docked to it: its drive does not spool till the two
     * part, and its pilot holding its key at {@code now} is shown why, as a ship dropping out is.
     */
    public static boolean docked(final MovingStructure ship, final long now) {
        final boolean docked = ship.mount() != null || StructureMounts.children(ship).stream()
                .anyMatch(child -> child.mount() != null && child.mount().anchor().isPresent());
        if (docked && held(ship, now)) {
            DROPPED.put(ship, new Dropped(Drop.DOCKED, now));
        }
        return docked;
    }

    /**
     * Whether {@code ship}, in flight at {@code now} in {@code level}, may jump there: in a system's space, over no
     * world's low orbit nor the Sun, and no other ship near.
     */
    public static boolean mayJump(final ServerLevel level, final MovingStructure ship, final StructurePose now) {
        return SystemSpaces.systemOf(level.dimension()).isPresent() && PulseCourse.mayBegin(now.x(), now.z())
                && !shipNear(level, ship, now.x(), now.z());
    }

    /**
     * Its drive spools a tick on, as {@code spooling} says its pilot holds its key where it may jump, its reactor
     * burning for it; let go, it spools down at once. How many ticks it has spooled.
     */
    public static int spool(final MovingStructure ship, final boolean spooling) {
        if (!spooling) {
            SPOOLED.remove(ship);
            return 0;
        }
        final int spooled = SPOOLED.getOrDefault(ship, 0) + 1;
        if (spooled >= SPOOL_TICKS) {
            SPOOLED.remove(ship);
        } else {
            SPOOLED.put(ship, spooled);
        }
        return spooled;
    }

    /** How its HUD shows {@code ship}'s pulse jump, its drive having spooled {@code spooled} ticks, at {@code now}. */
    public static ShipReadings.Pulse shown(final MovingStructure ship, final int spooled, final long now) {
        final Dropped dropped = DROPPED.get(ship);
        final String why = dropped != null && now - dropped.at() <= SHOWN ? dropped.why() : "";
        if (spooled <= 0) {
            return why.isEmpty() ? ShipReadings.Pulse.NONE : new ShipReadings.Pulse(0, 0, false, "", 0, 0, why);
        }
        final double share = spooled / (double) SPOOL_TICKS;
        final int left = (int) Math.ceil((SPOOL_TICKS - spooled) * ShipFlightModel.TICK * 10);
        return new ShipReadings.Pulse((int) Math.round(share * 100), left, false, "", 0, 0, why);
    }

    /**
     * A tick of {@code flight}'s jump, {@code ship} standing at {@code now} and turned as {@code frame}, its pilot
     * holding {@code held}, its reactor kept by {@code keeper}: it goes on, or drops out and, once the land where the
     * jump took it has loaded, is set there.
     */
    public static IStructureMotion.Step jumping(final ServerLevel level, final MovingStructure ship,
                                                final StructurePose now, final StructureFrame frame,
                                                final ShipFlight flight, final ShipFlightModel.Held held,
                                                final @Nullable AirReserveEntity keeper) {
        final Jump jump = flight.pulse().orElseThrow();
        final long tick = level.getGameTime();
        if (!jump.dropping().isEmpty()) {
            return droppingOut(level, ship, now, flight, jump, keeper, tick);
        }
        final boolean burnt = keeper != null && keeper.burn(ShipFlightModel.TICK / ShipFlight.REACTOR_LOAD_SECONDS
                * (PulseCourse.BURNS - 1));
        final double turn = (held.left() - held.right()) * PulseCourse.TURNS * ShipFlightModel.TICK;
        final StructurePose turned = turn == 0 ? now : now.turnedBy(0, 1, 0, Math.toRadians(turn));
        final double[] nose = flight.noseIn(new StructureFrame(turned, frame.pivotX(), frame.pivotY(),
                frame.pivotZ()));
        final double across = Math.hypot(nose[0], nose[2]);
        final double fromX = now.x() + jump.dx();
        final double fromZ = now.z() + jump.dz();
        double toX = across > 0 ? fromX + nose[0] / across * A_TICK : fromX;
        double toZ = across > 0 ? fromZ + nose[2] / across * A_TICK : fromZ;
        String why = "";
        final PulseCourse.Stop stop = PulseCourse.stopOn(fromX, fromZ, toX, toZ);
        if (stop != null) {
            toX = stop.x();
            toZ = stop.z();
            why = stop.body() == null ? Drop.SUN : stop.body().name().toLowerCase(Locale.ROOT);
        } else if (shipNear(level, ship, toX, toZ)) {
            why = Drop.SHIP;
        } else if (!burnt) {
            why = Drop.FUEL;
        } else if (held.back() > 0) {
            why = Drop.PILOT;
        }
        final Jump next = new Jump(toX - now.x(), toZ - now.z(), why);
        tell(level, ship, tick, next, true);
        if (keeper != null) {
            keeper.flew(ShipReadings.jumping(flight.state(), jumpShown(toX, toZ)));
        }
        return new IStructureMotion.Step(told(turned), flight.jumping(next));
    }

    /*
     * Where the ship stands, {@code pose}, as a pose of its own: a structure whose rule leaves it its very pose is held
     * still and told to nobody, and whoever is aboard would be told where it stands only as it is set where its jump
     * took it, their game drawing it going there across all the ticks between, the sky drawn jumping with it.
     */
    private static StructurePose told(final StructurePose pose) {
        return pose.movedTo(pose.x(), pose.y(), pose.z());
    }

    /*
     * Dropping out: the land where its jump took it is held loading, and once it goes the ship is set there, its jump
     * over and its flight going on as it was, whoever is aboard told in the same tick.
     */
    private static IStructureMotion.Step droppingOut(final ServerLevel level, final MovingStructure ship,
                                                     final StructurePose now, final ShipFlight flight,
                                                     final Jump jump, final @Nullable AirReserveEntity keeper,
                                                     final long tick) {
        final double x = now.x() + jump.dx();
        final double z = now.z() + jump.dz();
        SpaceAhead.holdAround(level, x, now.y(), z);
        if (!SpaceAhead.ready(level, x, now.y(), z)) {
            tell(level, ship, tick, jump, true);
            if (keeper != null) {
                keeper.flew(ShipReadings.jumping(flight.state(), jumpShown(x, z)));
            }
            return new IStructureMotion.Step(told(now), flight);
        }
        final StructurePose there = now.movedTo(x, now.y(), z);
        // Told before it is set there, while whoever is aboard still stands round where it stood.
        tell(level, ship, tick, jump, false);
        ship.teleport(there);
        DROPPED.put(ship, new Dropped(jump.dropping(), tick));
        return new IStructureMotion.Step(there, flight.jumping(null));
    }

    /* How the HUD shows a jump at ({@code x}, {@code z}): the nearest world, how far off and how long at its speed. */
    private static ShipReadings.Pulse jumpShown(final double x, final double z) {
        final PulseCourse.Nearest nearest = PulseCourse.nearest(x, z);
        return new ShipReadings.Pulse(0, 0, true, nearest.body().name().toLowerCase(Locale.ROOT),
                (int) Math.round(nearest.range()), (int) Math.round(nearest.range() / PulseCourse.SPEED), "");
    }

    /*
     * Tells whoever is aboard {@code ship} how far its jump has gone at {@code tick}, and whether it goes on, or is
     * over, the ship set that far on.
     */
    private static void tell(final ServerLevel level, final MovingStructure ship, final long tick, final Jump jump,
                             final boolean jumping) {
        final ShipPulsePayload payload = new ShipPulsePayload(ship.entity().getId(), tick, jump.dx(), jump.dz(),
                jumping);
        final AABB aboard = ship.worldBox().inflate(ABOARD);
        for (final ServerPlayer player : level.players()) {
            if (aboard.contains(player.position())) {
                PacketDistributor.sendToPlayer(player, payload);
            }
        }
    }

    /* Whether another ship than {@code ship} stands near ({@code x}, {@code z}) of {@code level}. */
    private static boolean shipNear(final ServerLevel level, final MovingStructure ship, final double x,
                                    final double z) {
        for (final IStructureBody body : LevelMovingStructures.of(level).all()) {
            if (body != ship && body instanceof MovingStructure other && other.motion() instanceof ShipFlight
                    && Math.hypot(other.pose().x() - x, other.pose().z() - z) < PulseCourse.NEAR_SHIP) {
                return true;
            }
        }
        return false;
    }

    /**
     * How far a ship's jump has carried it beyond where it stands, level, in blocks; and, dropping out, why.
     *
     * @param dx       how far east
     * @param dz       how far south
     * @param dropping why it drops out ({@link Drop}, or a world's name, as its body is named); empty while it jumps
     */
    public record Jump(double dx, double dz, String dropping) {

        public static final Codec<Jump> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.DOUBLE.fieldOf("dx").forGetter(Jump::dx),
                Codec.DOUBLE.fieldOf("dz").forGetter(Jump::dz),
                Codec.STRING.optionalFieldOf("dropping", "").forGetter(Jump::dropping)
        ).apply(instance, Jump::new));

        /** A jump just begun, where the ship stands. */
        public static Jump begun() {
            return new Jump(0, 0, "");
        }
    }

    /**
     * Why a ship dropped out of its jump, or would not begin one, besides coming over a world's low orbit, named by
     * the world's body.
     */
    public static final class Drop {

        /** Its pilot held back. */
        public static final String PILOT = "pilot";
        /** It came to the edge of the Sun. */
        public static final String SUN = "sun";
        /** It came near another ship. */
        public static final String SHIP = "ship";
        /** Its deuterium was spent. */
        public static final String FUEL = "fuel";
        /** Its pilot held its key docked to another ship, or with one docked to it: it will not jump so. */
        public static final String DOCKED = "docked";

        private Drop() {
        }

        /** The body a ship came over the low orbit of, dropping out, for why {@code why}; null for another why. */
        public static @Nullable SolarBody bodyOf(final String why) {
            for (final SolarBody body : SolarBody.values()) {
                if (body.name().toLowerCase(Locale.ROOT).equals(why)) {
                    return body;
                }
            }
            return null;
        }
    }

    /* Why a ship dropped out of its jump, and at which tick. */
    private record Dropped(String why, long at) {
    }
}
