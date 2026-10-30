/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.rocket;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.jstech.core.dimension.DimensionRulesData;
import dev.jstech.core.moving.MovingStructure;
import dev.jstech.core.moving.MovingStructureEvent;
import dev.jstech.core.moving.MovingStructures;
import dev.jstech.core.moving.physics.StructurePhysics;
import dev.jstech.core.uuid.StructureUuid;
import dev.jstech.space.api.RocketParts;
import dev.jstech.space.orbit.OrbitScale;
import dev.jstech.space.platform.LandingPlatforms;
import dev.jstech.space.wreck.StageImpacts;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

/**
 * The spent stages of the world's rockets on their way, each where its fate sends it ({@link StageFates}). One that
 * comes back flies to its platform and lands there ({@link StageLanding}). One that falls near where it came loose
 * falls by its physics and strikes the ground where it meets it; one sent farther, to its fall zone or anywhere round
 * a player, falls out of sight and strikes its column there ({@link StageImpacts}). Kept with the world, so a stage
 * on its way when the game is saved goes on when it loads.
 */
public final class StageFlights extends SavedData {

    private final Map<UUID, Flight> flights = new HashMap<>();

    private static final String FILE = "jsspace_stage_flights";
    private static final String FLIGHTS = "Flights";
    private static final Codec<Map<UUID, Flight>> MAP = Codec.unboundedMap(UUIDUtil.STRING_CODEC, Flight.CODEC);
    /* How far from where it came loose, across, a stage still falls in sight, in blocks. */
    private static final double NEAR = 64;
    /*
     * After how many ticks a stage sent farther has fallen out of sight, and one falling in sight may have come to
     * rest; after how many a stage whose body never came is forgotten; how slow one at rest goes, in metres a second.
     */
    private static final long OUT_OF_SIGHT = 100;
    private static final long FORGET = 600;
    private static final double RESTING = 0.5;
    /* The fastest a stage strikes the ground, in metres a second, the air braking it; a tick, in seconds. */
    private static final double FASTEST = 300;
    private static final double TICKS = 20;

    private StageFlights() {
    }

    /** Hands the game's bus what this answers, once, from the mod's constructor. */
    public static void listen() {
        NeoForge.EVENT_BUS.addListener(MovingStructureEvent.Parted.class, StageFlights::parted);
        NeoForge.EVENT_BUS.addListener(MovingStructureEvent.Collision.class, StageFlights::struck);
        NeoForge.EVENT_BUS.addListener(LevelTickEvent.Post.class, event -> {
            if (event.getLevel() instanceof ServerLevel level) {
                tick(level);
            }
        });
    }

    /**
     * The stage {@code stage} of the rocket {@code rocket} is let go as the body {@code piece}, its dry mass
     * {@code dry} kilograms, its lowest block {@code bottom} blocks over the rocket's foot, standing over
     * {@code under} in the world: where it goes is decided now, and it goes there once its body is made.
     */
    public static void parting(final ServerLevel level, final MovingStructure rocket, final StructureUuid piece,
                               final int stage, final double dry, final int bottom, final Vec3 under) {
        final Optional<Rockets.Rocket> record = Rockets.get(level.getServer(), rocket.id());
        if (record.isEmpty()) {
            return;
        }
        final StageFates.Outcome outcome = StageFates.decide(level, RocketParts.all(level), record.get(), stage, piece,
                under, level.getRandom().nextLong());
        final Flight flight = switch (outcome) {
            case StageFates.Outcome.Lands(final UUID platform, final ResourceKey<Level> dimension, final Vec3 spot)
                    when dimension.equals(level.dimension()) -> new Flight(level.dimension(), dry, bottom,
                    level.getGameTime(), Optional.of(platform), dimension, spot, false);
            case StageFates.Outcome.Lands(final UUID platform, final ResourceKey<Level> dimension, final Vec3 spot) -> {
                // A platform of another world is out of its reach: it falls where it came loose.
                LandingPlatforms.release(level.getServer(), platform);
                yield new Flight(level.dimension(), dry, bottom, level.getGameTime(), Optional.empty(),
                        level.dimension(), new Vec3(under.x, level.getMinBuildHeight(), under.z), true);
            }
            case StageFates.Outcome.Falls(final ResourceKey<Level> dimension, final BlockPos column,
                                          final boolean inZone) -> {
                final double across = Math.hypot(column.getX() + 0.5 - under.x, column.getZ() + 0.5 - under.z);
                yield new Flight(level.dimension(), dry, bottom, level.getGameTime(), Optional.empty(), dimension,
                        Vec3.atBottomCenterOf(column), dimension.equals(level.dimension()) && across <= NEAR);
            }
        };
        final StageFlights all = of(level.getServer());
        all.flights.put(piece.value(), flight);
        all.setDirty();
    }

    /** Where the stage of body {@code piece} is on its way to, if it is a stage on its way. */
    public static Optional<Flight> get(final MinecraftServer server, final StructureUuid piece) {
        return Optional.ofNullable(of(server).flights.get(piece.value()));
    }

    @Override
    public CompoundTag save(final CompoundTag tag, final HolderLookup.Provider registries) {
        MAP.encodeStart(NbtOps.INSTANCE, this.flights).result().ifPresent(saved -> tag.put(FLIGHTS, saved));
        return tag;
    }

    /* A stage's body made: one coming back starts for its platform; one falling falls by its physics. */
    private static void parted(final MovingStructureEvent.Parted event) {
        if (!(event.structure().level() instanceof ServerLevel level)) {
            return;
        }
        for (final MovingStructure piece : event.pieces()) {
            final Flight flight = of(level.getServer()).flights.get(piece.id().value());
            if (flight != null && flight.lands()) {
                piece.move(StageLanding.to(flight.at().x, flight.at().y - flight.bottom(), flight.at().z));
            }
        }
    }

    /* A stage falling in sight strikes the world it meets. */
    private static void struck(final MovingStructureEvent.Collision event) {
        if (event.other() != null || !(event.structure().level() instanceof ServerLevel level)) {
            return;
        }
        final Flight flight = of(level.getServer()).flights.get(event.structure().id().value());
        if (flight != null && flight.near()) {
            strike(level, event.structure(), flight, event.at(), event.speed());
        }
    }

    /* Each stage on its way in {@code level}: landed, fallen out of sight, or falling too long. */
    private static void tick(final ServerLevel level) {
        final StageFlights all = of(level.getServer());
        if (all.flights.isEmpty()) {
            return;
        }
        for (final Map.Entry<UUID, Flight> each : List.copyOf(all.flights.entrySet())) {
            final Flight flight = each.getValue();
            if (!flight.in().equals(level.dimension())) {
                continue;
            }
            final MovingStructure piece = MovingStructures.byId(level, new StructureUuid(each.getKey()));
            final long age = level.getGameTime() - flight.parted();
            if (piece == null) {
                if (age > FORGET) {
                    all.forget(each.getKey());
                }
            } else if (flight.lands()) {
                if (piece.motion() == null) {
                    all.forget(each.getKey());
                }
            } else if (flight.near()) {
                // One that came to rest with no blow worth telling lies there as its wreckage.
                final double speed = speedOf(piece);
                if (age > OUT_OF_SIGHT && speed < RESTING) {
                    strike(level, piece, flight, new Vec3(piece.pose().x(), piece.pose().y(), piece.pose().z()),
                            speed);
                }
            } else if (age >= OUT_OF_SIGHT) {
                fallElsewhere(level, piece, flight);
            }
        }
    }

    /* The stage {@code piece} strikes at {@code at}, {@code speed} metres a second fast: its blast and wreckage. */
    private static void strike(final ServerLevel level, final MovingStructure piece, final Flight flight,
                               final Vec3 at, final double speed) {
        final List<ItemStack> made = StageImpacts.blocksOf(piece);
        of(level.getServer()).forget(piece.id().value());
        MovingStructures.takeAway(piece);
        StageImpacts.strike(level, at, flight.dry(), 0, Math.min(FASTEST, speed), made, level.getRandom().nextLong());
    }

    /*
     * The stage {@code piece}, out of sight, comes down on its column far off: it is taken away here and strikes the
     * ground there, as fast as falling from its height has made it.
     */
    private static void fallElsewhere(final ServerLevel level, final MovingStructure piece, final Flight flight) {
        final ServerLevel there = level.getServer().getLevel(flight.to());
        final List<ItemStack> made = StageImpacts.blocksOf(piece);
        final double height = piece.pose().y();
        of(level.getServer()).forget(piece.id().value());
        MovingStructures.takeAway(piece);
        if (there == null) {
            return;
        }
        final int x = (int) Math.floor(flight.at().x);
        final int z = (int) Math.floor(flight.at().z);
        there.getChunk(x >> 4, z >> 4);
        final int ground = there.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
        final double pull = DimensionRulesData.of(there).gravity() * OrbitScale.STANDARD_GRAVITY;
        final double speed = Math.sqrt(2 * pull * Math.max(0, height - ground));
        StageImpacts.strike(there, new Vec3(x + 0.5, ground, z + 0.5), flight.dry(), 0, Math.min(FASTEST, speed),
                made, there.getRandom().nextLong());
    }

    /* How fast {@code piece} goes, in metres a second. */
    private static double speedOf(final MovingStructure piece) {
        final StructurePhysics physics = piece.physics();
        if (physics != null) {
            return physics.body().velocity().length();
        }
        final double[] moved = piece.velocity();
        return Math.sqrt(moved[0] * moved[0] + moved[1] * moved[1] + moved[2] * moved[2]) * TICKS;
    }

    private void forget(final UUID piece) {
        if (this.flights.remove(piece) != null) {
            setDirty();
        }
    }

    private static StageFlights load(final CompoundTag tag, final HolderLookup.Provider registries) {
        final StageFlights loaded = new StageFlights();
        final Tag saved = tag.get(FLIGHTS);
        if (saved != null) {
            MAP.parse(NbtOps.INSTANCE, saved).result().ifPresent(loaded.flights::putAll);
        }
        return loaded;
    }

    /* The world's stages on their way, kept with its overworld. */
    private static StageFlights of(final MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(new SavedData.Factory<>(StageFlights::new,
                StageFlights::load), FILE);
    }

    /**
     * A spent stage on its way.
     *
     * @param in       the world it came loose in
     * @param dry      its mass empty, in kilograms
     * @param bottom   how far over its rocket's foot its lowest block was, in blocks
     * @param parted   the game's time it came loose at, in ticks
     * @param platform the platform it comes back to; none for one that falls
     * @param to       the world it lands or falls in
     * @param at       the middle of the platform's top it lands on, or the bottom of the column it falls on
     * @param near     whether it falls in sight of where it came loose, by its physics
     */
    public record Flight(ResourceKey<Level> in, double dry, int bottom, long parted, Optional<UUID> platform,
                         ResourceKey<Level> to, Vec3 at, boolean near) {

        public static final Codec<Flight> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                ResourceKey.codec(Registries.DIMENSION).fieldOf("in").forGetter(Flight::in),
                Codec.DOUBLE.fieldOf("dry").forGetter(Flight::dry),
                Codec.INT.fieldOf("bottom").forGetter(Flight::bottom),
                Codec.LONG.fieldOf("parted").forGetter(Flight::parted),
                UUIDUtil.STRING_CODEC.optionalFieldOf("platform").forGetter(Flight::platform),
                ResourceKey.codec(Registries.DIMENSION).fieldOf("to").forGetter(Flight::to),
                Vec3.CODEC.fieldOf("at").forGetter(Flight::at),
                Codec.BOOL.fieldOf("near").forGetter(Flight::near)
        ).apply(instance, Flight::new));

        /** Whether it comes back to land. */
        public boolean lands() {
            return this.platform.isPresent();
        }
    }
}
