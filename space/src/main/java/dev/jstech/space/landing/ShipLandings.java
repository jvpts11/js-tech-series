/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.landing;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.jstech.core.moving.IStructureBody;
import dev.jstech.core.moving.MovingStructures;
import dev.jstech.core.moving.YardLayout;
import dev.jstech.core.team.Ownership;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Where a starship may set down, so its autopilot can fly there from far away: the ship landing pads of every world,
 * each where its terminal stands and whose it is, and the runway beacons, each where it stands and the way its runway
 * runs. A pad is noted as it forms and let go as it is undone; a beacon as it is placed and broken, and let go too
 * when it is found gone. A pad built on a moving structure afloat is found wherever its structure has taken it. Kept
 * with the world.
 */
public final class ShipLandings extends SavedData {

    private final Map<UUID, Pad> pads = new HashMap<>();
    private final Map<Place, Direction> beacons = new HashMap<>();

    private static final String FILE = "jsspace_ship_landings";
    private static final String PADS = "Pads";
    private static final String BEACONS = "Beacons";
    private static final Codec<Map<UUID, Pad>> PAD_MAP = Codec.unboundedMap(UUIDUtil.STRING_CODEC, Pad.CODEC);
    private static final Codec<List<Beacon>> BEACON_LIST = Beacon.CODEC.listOf();
    /* How far round a pad's middle, and how high over it, another ship standing there keeps it taken, in blocks. */
    private static final double TAKEN_ACROSS = 4;
    private static final double TAKEN_UP = 6;
    private static final int CHUNK = 4;

    private ShipLandings() {
    }

    /**
     * Notes the pad {@code id}, formed with its terminal at {@code terminal} of {@code level}, {@code ownership}'s, its
     * ships setting down on top of the terminal itself.
     */
    public static void notePad(final ServerLevel level, final UUID id, final BlockPos terminal,
                               final Ownership ownership) {
        notePad(level, id, terminal, terminal, ownership);
    }

    /**
     * Notes the pad {@code id}, formed with its terminal at {@code terminal} of {@code level}, {@code ownership}'s, its
     * ships setting down on top of the block {@code middle}, its deck's middle.
     */
    public static void notePad(final ServerLevel level, final UUID id, final BlockPos terminal, final BlockPos middle,
                               final Ownership ownership) {
        final ShipLandings all = of(level.getServer());
        final Pad now = new Pad(level.dimension(), terminal.immutable(), middle.immutable(), ownership);
        if (!now.equals(all.pads.get(id))) {
            all.pads.put(id, now);
            all.setDirty();
        }
    }

    /** Lets the pad {@code id} go, undone or broken. */
    public static void dropPad(final MinecraftServer server, final UUID id) {
        final ShipLandings all = of(server);
        if (all.pads.remove(id) != null) {
            all.setDirty();
        }
    }

    /** The pad {@code id}, if it stands formed. */
    public static Optional<Pad> pad(final MinecraftServer server, final UUID id) {
        return Optional.ofNullable(of(server).pads.get(id));
    }

    /** The pads of {@code level} that {@code player} may use: theirs and their team's. */
    public static List<Pad> padsUsableBy(final ServerPlayer player, final ServerLevel level) {
        final List<Pad> usable = new ArrayList<>();
        for (final Pad pad : of(level.getServer()).pads.values()) {
            if (pad.dimension().equals(level.dimension()) && pad.ownership().mayUse(player)) {
                usable.add(pad);
            }
        }
        return List.copyOf(usable);
    }

    /**
     * The middle of the top of {@code pad}, where a ship sets down: over its deck's middle, or, for a pad afloat on a
     * moving structure, wherever the structure has it now.
     */
    public static Vec3 padSpot(final ServerLevel level, final Pad pad) {
        return over(level, pad.middle());
    }

    /**
     * The middle of the top of the pad whose deck's middle is the block {@code middle}: over it, or, afloat on a moving
     * structure, wherever the structure has it now.
     */
    public static Vec3 over(final ServerLevel level, final BlockPos middle) {
        final Vec3 top = Vec3.atBottomCenterOf(middle.above());
        return YardLayout.isYardBlock(middle.getX()) ? MovingStructures.inWorld(level, top) : top;
    }

    /** Whether a moving structure other than {@code ours} stands on the pad whose middle is {@code spot}. */
    public static boolean taken(final ServerLevel level, final Vec3 spot, @Nullable final IStructureBody ours) {
        final AABB over = new AABB(spot.x - TAKEN_ACROSS, spot.y, spot.z - TAKEN_ACROSS, spot.x + TAKEN_ACROSS,
                spot.y + TAKEN_UP, spot.z + TAKEN_ACROSS);
        for (final IStructureBody body : MovingStructures.near(level, over)) {
            if (body != ours) {
                return true;
            }
        }
        return false;
    }

    /** Notes the runway beacon at {@code at} of {@code level}, its runway running {@code way}. */
    public static void noteBeacon(final ServerLevel level, final BlockPos at, final Direction way) {
        final ShipLandings all = of(level.getServer());
        final Place place = new Place(level.dimension(), at.immutable());
        if (all.beacons.put(place, way) != way) {
            all.setDirty();
        }
    }

    /** Lets the runway beacon at {@code at} of {@code level} go. */
    public static void dropBeacon(final ServerLevel level, final BlockPos at) {
        final ShipLandings all = of(level.getServer());
        if (all.beacons.remove(new Place(level.dimension(), at)) != null) {
            all.setDirty();
        }
    }

    /**
     * The runway beacons of {@code level}, on its land: one found gone where its land is loaded is let go, and one
     * aboard a moving structure is no runway.
     */
    public static List<Beacon> beacons(final ServerLevel level) {
        final ShipLandings all = of(level.getServer());
        final List<Beacon> found = new ArrayList<>();
        final List<Place> gone = new ArrayList<>();
        for (final Map.Entry<Place, Direction> beacon : all.beacons.entrySet()) {
            final BlockPos at = beacon.getKey().at();
            if (!beacon.getKey().dimension().equals(level.dimension()) || YardLayout.isYardBlock(at.getX())) {
                continue;
            }
            final LevelChunk chunk = level.getChunkSource().getChunkNow(at.getX() >> CHUNK, at.getZ() >> CHUNK);
            if (chunk != null && !chunk.getBlockState(at).is(ShipLandingBlocks.BEACON.get())) {
                gone.add(beacon.getKey());
                continue;
            }
            found.add(new Beacon(level.dimension(), at, beacon.getValue()));
        }
        if (!gone.isEmpty()) {
            gone.forEach(all.beacons::remove);
            all.setDirty();
        }
        return List.copyOf(found);
    }

    @Override
    public CompoundTag save(final CompoundTag tag, final HolderLookup.Provider registries) {
        PAD_MAP.encodeStart(NbtOps.INSTANCE, this.pads).result().ifPresent(saved -> tag.put(PADS, saved));
        final List<Beacon> listed = new ArrayList<>();
        this.beacons.forEach((place, way) -> listed.add(new Beacon(place.dimension(), place.at(), way)));
        BEACON_LIST.encodeStart(NbtOps.INSTANCE, listed).result().ifPresent(saved -> tag.put(BEACONS, saved));
        return tag;
    }

    private static ShipLandings load(final CompoundTag tag, final HolderLookup.Provider registries) {
        final ShipLandings loaded = new ShipLandings();
        final Tag pads = tag.get(PADS);
        if (pads != null) {
            PAD_MAP.parse(NbtOps.INSTANCE, pads).result().ifPresent(loaded.pads::putAll);
        }
        final Tag beacons = tag.get(BEACONS);
        if (beacons != null) {
            BEACON_LIST.parse(NbtOps.INSTANCE, beacons).result().ifPresent(listed -> listed.forEach(beacon ->
                    loaded.beacons.put(new Place(beacon.dimension(), beacon.at()), beacon.way())));
        }
        return loaded;
    }

    private static ShipLandings of(final MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(new SavedData.Factory<>(ShipLandings::new,
                ShipLandings::load), FILE);
    }

    /**
     * A ship landing pad as the world keeps it.
     *
     * @param dimension the world its terminal is in
     * @param terminal  where its terminal stands, in the world or, afloat, in its structure's yard
     * @param middle    its deck's middle block, a ship setting down on top of it, where its terminal is kept
     * @param ownership whose it is, and who may set a ship down on it
     */
    public record Pad(ResourceKey<Level> dimension, BlockPos terminal, BlockPos middle, Ownership ownership) {

        public static final Codec<Pad> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                ResourceKey.codec(Registries.DIMENSION).fieldOf("dimension").forGetter(Pad::dimension),
                BlockPos.CODEC.fieldOf("terminal").forGetter(Pad::terminal),
                BlockPos.CODEC.optionalFieldOf("middle").forGetter(pad -> Optional.of(pad.middle())),
                Ownership.CODEC.fieldOf("ownership").forGetter(Pad::ownership)
        ).apply(instance, (dimension, terminal, middle, ownership) -> new Pad(dimension, terminal,
                middle.orElse(terminal), ownership)));
    }

    /**
     * A runway beacon as the world keeps it.
     *
     * @param dimension the world it stands in
     * @param at        where it stands, at the start of its runway
     * @param way       the way its runway runs from it
     */
    public record Beacon(ResourceKey<Level> dimension, BlockPos at, Direction way) {

        public static final Codec<Beacon> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                ResourceKey.codec(Registries.DIMENSION).fieldOf("dimension").forGetter(Beacon::dimension),
                BlockPos.CODEC.fieldOf("at").forGetter(Beacon::at),
                Direction.CODEC.fieldOf("way").forGetter(Beacon::way)
        ).apply(instance, Beacon::new));

        /** The way its runway runs, in degrees east of north, as a compass reads it. */
        public double heading() {
            return Math.floorMod((int) this.way.toYRot() + 180, 360);
        }

        /** The foot of the beacon, the runway's level, where LAND aims. */
        public Vec3 foot() {
            return Vec3.atBottomCenterOf(this.at);
        }
    }

    /* Where a beacon stands: its world and its block. */
    private record Place(ResourceKey<Level> dimension, BlockPos at) {
    }
}
