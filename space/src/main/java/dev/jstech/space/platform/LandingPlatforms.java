/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.platform;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.jstech.core.moving.MovingStructures;
import dev.jstech.core.moving.YardLayout;
import dev.jstech.core.team.Ownership;
import dev.jstech.core.uuid.StructureUuid;
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
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * The landing platforms of a world, each where its terminal stands, how wide it was built, whose it is and the stage it
 * holds, so a flight computer can set a stage to land on one far away, as SpaceX's boosters land on their zones and
 * drone ships. A platform built on a moving structure afloat, a raft or a barge, is a floating one: it is found
 * wherever its structure has taken it. A platform holds one stage at a time. Kept with the world, a platform noted as
 * it forms and let go as it is undone.
 */
public final class LandingPlatforms extends SavedData {

    private final Map<UUID, Entry> platforms = new HashMap<>();

    /** How many times a stage's diameter a platform's side must be for the stage to land on it, its legs spread. */
    public static final int SIDE_PER_DIAMETER = 3;
    private static final String FILE = "jsspace_landing_platforms";
    private static final String PLATFORMS = "Platforms";
    private static final Codec<Map<UUID, Entry>> MAP = Codec.unboundedMap(UUIDUtil.STRING_CODEC, Entry.CODEC);

    private LandingPlatforms() {
    }

    /**
     * Notes the platform {@code id}, formed with its terminal at {@code terminal} of {@code level} and {@code side}
     * blocks wide, {@code ownership}'s; the stage it holds is kept.
     */
    public static void note(final ServerLevel level, final UUID id, final BlockPos terminal, final int side,
                            final Ownership ownership) {
        final LandingPlatforms all = of(level.getServer());
        final Entry was = all.platforms.get(id);
        final Entry now = new Entry(level.dimension(), terminal.immutable(), side, ownership,
                was == null ? Optional.empty() : was.holding());
        if (!now.equals(was)) {
            all.platforms.put(id, now);
            all.setDirty();
        }
    }

    /** Lets the platform {@code id} go, undone or broken. */
    public static void drop(final MinecraftServer server, final UUID id) {
        final LandingPlatforms all = of(server);
        if (all.platforms.remove(id) != null) {
            all.setDirty();
        }
    }

    /** The platform {@code id}, if it stands formed. */
    public static Optional<Entry> get(final MinecraftServer server, final UUID id) {
        return Optional.ofNullable(of(server).platforms.get(id));
    }

    /** The platforms {@code player} may use, by id: theirs and their team's. */
    public static Map<UUID, Entry> usableBy(final ServerPlayer player) {
        final Map<UUID, Entry> usable = new HashMap<>();
        for (final Map.Entry<UUID, Entry> platform : of(player.getServer()).platforms.entrySet()) {
            if (platform.getValue().ownership().mayUse(player)) {
                usable.put(platform.getKey(), platform.getValue());
            }
        }
        return Map.copyOf(usable);
    }

    /**
     * Keeps the platform {@code id} for the stage {@code stage}, which is coming down on it; whether it was free, or
     * kept for that stage already.
     */
    public static boolean reserve(final MinecraftServer server, final UUID id, final StructureUuid stage) {
        final LandingPlatforms all = of(server);
        final Entry platform = all.platforms.get(id);
        if (platform == null) {
            return false;
        }
        if (platform.holding().isPresent()) {
            return platform.holding().get().equals(stage.value());
        }
        all.platforms.put(id, platform.holding(stage.value()));
        all.setDirty();
        return true;
    }

    /** Frees the platform {@code id}, its stage gone from it. */
    public static void release(final MinecraftServer server, final UUID id) {
        final LandingPlatforms all = of(server);
        final Entry platform = all.platforms.get(id);
        if (platform != null && platform.holding().isPresent()) {
            all.platforms.put(id, platform.holding(null));
            all.setDirty();
        }
    }

    /**
     * The middle of the top of {@code platform} in its world, where a stage lands: where its terminal stands, or, for
     * a platform afloat on a moving structure, wherever the structure has it now; none where its world is not loaded.
     */
    public static Optional<Vec3> landingSpot(final MinecraftServer server, final Entry platform) {
        final ServerLevel level = server.getLevel(platform.dimension());
        if (level == null) {
            return Optional.empty();
        }
        final Vec3 top = Vec3.atBottomCenterOf(platform.terminal().above());
        return Optional.of(YardLayout.isYardBlock(platform.terminal().getX()) ? MovingStructures.inWorld(level, top)
                : top);
    }

    /** The widest stage, in blocks, a platform {@code side} blocks wide takes. */
    public static int takes(final int side) {
        return side / SIDE_PER_DIAMETER;
    }

    /** Every platform of the world, by id. */
    public static List<UUID> all(final MinecraftServer server) {
        return List.copyOf(of(server).platforms.keySet());
    }

    @Override
    public CompoundTag save(final CompoundTag tag, final HolderLookup.Provider registries) {
        MAP.encodeStart(NbtOps.INSTANCE, this.platforms).result().ifPresent(saved -> tag.put(PLATFORMS, saved));
        return tag;
    }

    private static LandingPlatforms load(final CompoundTag tag, final HolderLookup.Provider registries) {
        final LandingPlatforms loaded = new LandingPlatforms();
        final Tag saved = tag.get(PLATFORMS);
        if (saved != null) {
            MAP.parse(NbtOps.INSTANCE, saved).result().ifPresent(loaded.platforms::putAll);
        }
        return loaded;
    }

    private static LandingPlatforms of(final MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(new SavedData.Factory<>(LandingPlatforms::new,
                LandingPlatforms::load), FILE);
    }

    /**
     * A landing platform as the world keeps it.
     *
     * @param dimension the world its terminal is in
     * @param terminal  where its terminal stands, in the world or, afloat, in its structure's yard
     * @param side      how wide it was built, in blocks
     * @param ownership whose it is, and who may set a stage to land on it
     * @param holding   the stage it holds, by its structure's id
     */
    public record Entry(ResourceKey<Level> dimension, BlockPos terminal, int side, Ownership ownership,
                        Optional<UUID> holding) {

        public static final Codec<Entry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                ResourceKey.codec(Registries.DIMENSION).fieldOf("dimension").forGetter(Entry::dimension),
                BlockPos.CODEC.fieldOf("terminal").forGetter(Entry::terminal),
                Codec.INT.fieldOf("side").forGetter(Entry::side),
                Ownership.CODEC.fieldOf("ownership").forGetter(Entry::ownership),
                UUIDUtil.STRING_CODEC.optionalFieldOf("holding").forGetter(Entry::holding)
        ).apply(instance, Entry::new));

        /** Whether it holds no stage. */
        public boolean free() {
            return this.holding.isEmpty();
        }

        /** Whether a stage {@code diameter} blocks wide may land on it, its legs spread. */
        public boolean takes(final int diameter) {
            return diameter > 0 && LandingPlatforms.takes(this.side) >= diameter;
        }

        Entry holding(@Nullable final UUID stage) {
            return new Entry(this.dimension, this.terminal, this.side, this.ownership, Optional.ofNullable(stage));
        }
    }
}
