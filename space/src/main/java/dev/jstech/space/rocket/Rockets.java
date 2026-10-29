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
import dev.jstech.core.team.Access;
import dev.jstech.core.team.Ownership;
import dev.jstech.core.uuid.StructureUuid;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * The rockets of a world, each a moving structure of J's Core's whose parts are a rocket's: its stack of parts from the
 * bottom up, and whose it is. A rocket belongs to whoever built it and is used by their team, by J's Core's kit of
 * ownership, as a starship is; it keeps its record as it goes from one dimension to another, its structure's id the
 * same. Kept with the world.
 */
public final class Rockets extends SavedData {

    private final Map<UUID, Rocket> rockets = new HashMap<>();

    private static final String FILE = "jsspace_rockets";
    private static final String ROCKETS = "Rockets";
    private static final Codec<Map<UUID, Rocket>> MAP = Codec.unboundedMap(UUIDUtil.STRING_CODEC, Rocket.CODEC);

    private Rockets() {
    }

    /**
     * Makes the moving structure {@code structure} a rocket of {@code stack}, built by {@code builder}, its team's to
     * use; a rocket it was already is made again.
     */
    public static Rocket register(final MinecraftServer server, final StructureUuid structure, final RocketStack stack,
                                  final UUID builder) {
        final Rocket rocket = new Rocket(stack, Ownership.of(builder).withAccess(Access.TEAM));
        final Rockets all = of(server);
        all.rockets.put(structure.value(), rocket);
        all.setDirty();
        return rocket;
    }

    /** The rocket the moving structure {@code structure} is, if it is one. */
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
        final Rockets all = of(server);
        final Rocket rocket = all.rockets.get(structure.value());
        if (rocket == null) {
            return false;
        }
        all.rockets.put(structure.value(), new Rocket(rocket.stack(), ownership));
        all.setDirty();
        return true;
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

    /**
     * A rocket: its parts and whose it is.
     *
     * @param stack     its parts, from the bottom up
     * @param ownership whose it is, and who may use it
     */
    public record Rocket(RocketStack stack, Ownership ownership) {

        public static final Codec<Rocket> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                RocketStack.CODEC.fieldOf("stack").forGetter(Rocket::stack),
                Ownership.CODEC.fieldOf("ownership").forGetter(Rocket::ownership)
        ).apply(instance, Rocket::new));
    }
}
