/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.rocket;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.jstech.core.heat.HeatClimate;
import dev.jstech.core.moving.MovingStructure;
import dev.jstech.core.moving.MovingStructures;
import dev.jstech.core.team.Access;
import dev.jstech.core.team.Ownership;
import dev.jstech.core.uuid.StructureUuid;
import dev.jstech.space.api.RocketPart;
import dev.jstech.space.api.RocketParts;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.UnaryOperator;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.Nullable;

/**
 * The rockets of a world, each a moving structure of J's Core's whose parts are a rocket's: its stack of parts from the
 * bottom up, whose it is, what its tanks hold and where each of its stages is set to go. A rocket belongs to whoever
 * built it and is used by their team, by J's Core's kit of ownership, as a starship is; it keeps its record as it goes
 * from one dimension to another, its structure's id the same. What its tanks hold boils off as the time goes by,
 * reckoned whenever it is asked for rather than every tick. Kept with the world.
 */
public final class Rockets extends SavedData {

    private final Map<UUID, Rocket> rockets = new HashMap<>();

    private static final String FILE = "jsspace_rockets";
    private static final String ROCKETS = "Rockets";
    private static final Codec<Map<UUID, Rocket>> MAP = Codec.unboundedMap(UUIDUtil.STRING_CODEC, Rocket.CODEC);
    /* A tick of the game, in seconds. */
    private static final double TICK_SECONDS = 0.05;

    private Rockets() {
    }

    /**
     * Makes the moving structure {@code structure} a rocket of {@code stack}, built by {@code builder}, its team's to
     * use, its tanks empty and no stage set to go anywhere; a rocket it was already is made again.
     */
    public static Rocket register(final MinecraftServer server, final StructureUuid structure, final RocketStack stack,
                                  final UUID builder) {
        final Rocket rocket = new Rocket(stack, Ownership.of(builder).withAccess(Access.TEAM), Map.of(), Map.of(),
                server.overworld().getGameTime());
        put(server, structure, rocket);
        return rocket;
    }

    /** The rocket the moving structure {@code structure} is, if it is one, as it was last reckoned. */
    public static Optional<Rocket> get(final MinecraftServer server, final StructureUuid structure) {
        return Optional.ofNullable(of(server).rockets.get(structure.value()));
    }

    /** Lets the moving structure {@code structure} be a rocket no more, as it is taken apart; whether it was one. */
    public static boolean forget(final MinecraftServer server, final StructureUuid structure) {
        final Rockets all = of(server);
        final boolean was = all.rockets.remove(structure.value()) != null;
        if (was) {
            all.setDirty();
        }
        return was;
    }

    /** Sets whose {@code structure}'s rocket is and who may use it; whether it is a rocket. */
    public static boolean own(final MinecraftServer server, final StructureUuid structure,
                              final Ownership ownership) {
        return change(server, structure, rocket -> new Rocket(rocket.stack(), ownership, rocket.tanks(),
                rocket.fates(), rocket.settled()));
    }

    /**
     * Fills the tank at {@code section} of {@code structure}'s rocket, counted from the bottom, with {@code load},
     * after reckoning what its tanks lost till now; whether it is a rocket and that place one of its tanks.
     */
    public static boolean load(final MinecraftServer server, final StructureUuid structure, final int section,
                               final TankLoad load) {
        settle(server, structure);
        return change(server, structure, rocket -> {
            if (section < 0 || section >= rocket.stack().sections().size()) {
                return rocket;
            }
            final Map<Integer, TankLoad> tanks = new LinkedHashMap<>(rocket.tanks());
            tanks.put(section, load);
            return new Rocket(rocket.stack(), rocket.ownership(), tanks, rocket.fates(), rocket.settled());
        }) && get(server, structure).map(rocket -> load.equals(rocket.tanks().get(section))).orElse(false);
    }

    /** Sets where the stage {@code stage} of {@code structure}'s rocket goes, or that it goes nowhere set. */
    public static boolean fate(final MinecraftServer server, final StructureUuid structure, final int stage,
                               @Nullable final StageFate fate) {
        return change(server, structure, rocket -> {
            final Map<Integer, StageFate> fates = new LinkedHashMap<>(rocket.fates());
            if (fate == null) {
                fates.remove(stage);
            } else {
                fates.put(stage, fate);
            }
            return new Rocket(rocket.stack(), rocket.ownership(), rocket.tanks(), fates, rocket.settled());
        });
    }

    /**
     * Reckons what the tanks of {@code structure}'s rocket boiled off since they were last reckoned, at the place its
     * structure stands now, and gives the rocket as it is; as it was where its structure is in no loaded world.
     */
    public static Optional<Rocket> settle(final MinecraftServer server, final StructureUuid structure) {
        final Optional<Rocket> found = get(server, structure);
        if (found.isEmpty()) {
            return found;
        }
        final long now = server.overworld().getGameTime();
        final Rocket rocket = found.get();
        final double seconds = (now - rocket.settled()) * TICK_SECONDS;
        if (seconds <= 0) {
            return found;
        }
        for (final ServerLevel level : server.getAllLevels()) {
            final MovingStructure standing = MovingStructures.byId(level, structure);
            if (standing != null) {
                final Rocket reckoned = rocket.boiled(RocketParts.all(level), HeatClimate.air(level,
                        BlockPos.containing(standing.pose().x(), standing.pose().y(), standing.pose().z())).kelvin(),
                        seconds, now);
                put(server, structure, reckoned);
                return Optional.of(reckoned);
            }
        }
        return found;
    }

    @Override
    public CompoundTag save(final CompoundTag tag, final HolderLookup.Provider registries) {
        MAP.encodeStart(NbtOps.INSTANCE, this.rockets).result().ifPresent(saved -> tag.put(ROCKETS, saved));
        return tag;
    }

    private static Rockets load(final CompoundTag tag, final HolderLookup.Provider registries) {
        final Rockets loaded = new Rockets();
        final Tag saved = tag.get(ROCKETS);
        if (saved != null) {
            MAP.parse(NbtOps.INSTANCE, saved).result().ifPresent(loaded.rockets::putAll);
        }
        return loaded;
    }

    /* The world's rockets, kept with its overworld, the one dimension every server has. */
    private static Rockets of(final MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(new SavedData.Factory<>(Rockets::new,
                Rockets::load), FILE);
    }

    private static void put(final MinecraftServer server, final StructureUuid structure, final Rocket rocket) {
        final Rockets all = of(server);
        all.rockets.put(structure.value(), rocket);
        all.setDirty();
    }

    /* Changes {@code structure}'s rocket as {@code how} says; whether it is one. */
    private static boolean change(final MinecraftServer server, final StructureUuid structure,
                                  final UnaryOperator<Rocket> how) {
        final Rocket rocket = of(server).rockets.get(structure.value());
        if (rocket == null) {
            return false;
        }
        put(server, structure, how.apply(rocket));
        return true;
    }

    /**
     * A rocket: its parts, whose it is, what its tanks hold, where its stages go and when its tanks were last reckoned.
     *
     * @param stack     its parts, from the bottom up
     * @param ownership whose it is, and who may use it
     * @param tanks     what each of its tanks holds, by the tank's place in its stack from the bottom
     * @param fates     where each of its stages goes, by the stage's number from the bottom, from one
     * @param settled   the game's time its tanks were last reckoned at, in ticks
     */
    public record Rocket(RocketStack stack, Ownership ownership, Map<Integer, TankLoad> tanks,
                         Map<Integer, StageFate> fates, long settled) {

        private static final Codec<Integer> INDEX = Codec.STRING.comapFlatMap(text -> {
            try {
                return DataResult.success(Integer.parseInt(text));
            } catch (final NumberFormatException wrong) {
                return DataResult.error(() -> "not a place in a rocket: " + text);
            }
        }, String::valueOf);
        public static final Codec<Rocket> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                RocketStack.CODEC.fieldOf("stack").forGetter(Rocket::stack),
                Ownership.CODEC.fieldOf("ownership").forGetter(Rocket::ownership),
                Codec.unboundedMap(INDEX, TankLoad.CODEC).optionalFieldOf("tanks", Map.of()).forGetter(Rocket::tanks),
                Codec.unboundedMap(INDEX, StageFate.CODEC).optionalFieldOf("fates", Map.of())
                        .forGetter(Rocket::fates),
                Codec.LONG.optionalFieldOf("settled", 0L).forGetter(Rocket::settled)
        ).apply(instance, Rocket::new));

        public Rocket {
            tanks = Map.copyOf(tanks);
            fates = Map.copyOf(fates);
        }

        /** Where its stage {@code stage} is set to go, if it is set. */
        public Optional<StageFate> fate(final int stage) {
            return Optional.ofNullable(this.fates.get(stage));
        }

        /* The same rocket, its tanks having stood {@code seconds} at a place {@code kelvin} warm, as of {@code now}. */
        Rocket boiled(final Map<ResourceLocation, RocketPart> parts, final double kelvin, final double seconds,
                      final long now) {
            final Map<Integer, TankLoad> left = new LinkedHashMap<>();
            for (final Map.Entry<Integer, TankLoad> tank : this.tanks.entrySet()) {
                final int section = tank.getKey();
                final RocketStack.Part placed = section < this.stack.sections().size()
                        ? this.stack.sections().get(section).part() : null;
                final RocketPart part = placed == null ? null : parts.get(placed.id());
                if (part == null) {
                    left.put(section, tank.getValue());
                    continue;
                }
                final int height = part.height() * (part.stretches() ? placed.layers() : 1);
                left.put(section, RocketBoilOff.boil(tank.getValue(), RocketBoilOff.surface(part.diameter(), height),
                        kelvin, seconds));
            }
            return new Rocket(this.stack, this.ownership, left, this.fates, now);
        }
    }
}
