/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.orbit;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.jstech.core.jstechseries.persistor.SpacePersistor;
import dev.jstech.core.moving.MovingStructure;
import dev.jstech.core.moving.MovingStructureEvent;
import dev.jstech.core.region.Box;
import dev.jstech.core.team.Ownership;
import dev.jstech.space.config.SpaceServerConfig;
import dev.jstech.space.rocket.Rockets;
import dev.jstech.space.universe.SolarBody;
import dev.jstech.space.universe.SolarSystemWorlds;
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
import net.neoforged.neoforge.common.NeoForge;

/**
 * Everything in the orbits of a world's worlds, by id: the rockets parked there, the satellites and probes, and the
 * debris of those destroyed. A rocket that passes into a world's orbit is told here as it arrives and forgotten as it
 * goes back down. A satellite's or a probe's owner may let it go: it works no more, and any team may take it, as
 * satellites are given or traded between teams. A destroyed one becomes debris in its orbit, unless the server turns
 * debris off. A Space Persistor aboard keeps a thing's place loaded. Where each rocket rests is kept too, so one coming
 * into its orbit finds room beside it even where its part of space is not loaded; a rocket taken away or set down is
 * forgotten. Kept with the world.
 */
public final class OrbitObjects extends SavedData {

    private final Map<UUID, OrbitObject> objects = new HashMap<>();
    /* Where each rocket in an orbit rests in its system's space. */
    private final Map<UUID, Rest> resting = new HashMap<>();

    private static final String FILE = "jsspace_orbit_objects";
    private static final String OBJECTS = "Objects";
    private static final String RESTING = "Resting";
    private static final Codec<Map<UUID, OrbitObject>> MAP = Codec.unboundedMap(UUIDUtil.STRING_CODEC,
            OrbitObject.CODEC);
    private static final Codec<Map<UUID, Rest>> RESTS = Codec.unboundedMap(UUIDUtil.STRING_CODEC, Rest.CODEC);

    private OrbitObjects() {
    }

    /** Hands the game's bus what forgets a rocket gone from the game for good, once, from the mod's constructor. */
    public static void listen() {
        NeoForge.EVENT_BUS.addListener(MovingStructureEvent.Removed.class, event -> {
            if (event.structure().level() instanceof ServerLevel level) {
                forget(level.getServer(), event.structure().id().value());
            }
        });
    }

    /** Keeps {@code object} as the thing in orbit {@code id}, in place of what it was. */
    public static void put(final MinecraftServer server, final UUID id, final OrbitObject object) {
        final OrbitObjects all = of(server);
        all.objects.put(id, object);
        all.setDirty();
    }

    /** The thing in orbit {@code id}, if there is one. */
    public static Optional<OrbitObject> get(final MinecraftServer server, final UUID id) {
        return Optional.ofNullable(of(server).objects.get(id));
    }

    /** Forgets the thing in orbit {@code id}, gone down or away; whether there was one. */
    public static boolean forget(final MinecraftServer server, final UUID id) {
        final OrbitObjects all = of(server);
        final boolean was = all.objects.remove(id) != null;
        final boolean rested = all.resting.remove(id) != null;
        if (was || rested) {
            all.setDirty();
        }
        return was;
    }

    /**
     * The thing in orbit {@code id} rests at {@code rest} of its orbit, a place of its system's space, till it is
     * forgotten; nothing for one not told in an orbit.
     */
    public static void restsAt(final MinecraftServer server, final UUID id, final Rest rest) {
        final OrbitObjects all = of(server);
        if (all.objects.containsKey(id)) {
            all.resting.put(id, rest);
            all.setDirty();
        }
    }

    /** Where the rockets in the orbits of {@code world} rest, by id. */
    public static Map<UUID, Rest> restingAround(final MinecraftServer server, final ResourceLocation world) {
        final OrbitObjects all = of(server);
        final Map<UUID, Rest> out = new LinkedHashMap<>();
        all.resting.forEach((id, rest) -> {
            final OrbitObject object = all.objects.get(id);
            if (object != null && object.world().equals(world)) {
                out.put(id, rest);
            }
        });
        return out;
    }

    /** Everything in the orbits of the world {@code world}, by id. */
    public static Map<UUID, OrbitObject> around(final MinecraftServer server, final ResourceLocation world) {
        final Map<UUID, OrbitObject> out = new LinkedHashMap<>();
        of(server).objects.forEach((id, object) -> {
            if (object.world().equals(world)) {
                out.put(id, object);
            }
        });
        return Map.copyOf(out);
    }

    /** Its owner lets the satellite or probe {@code id} go: it works no more; whether it could be let go. */
    public static boolean letGo(final MinecraftServer server, final UUID id) {
        return change(server, id, object -> object.kind().mayBeLetGo() && object.working() ? object.orphaned(true)
                : object);
    }

    /** {@code by} takes the satellite or probe {@code id} its owner let go, and it works again; whether it could. */
    public static boolean take(final MinecraftServer server, final UUID id, final Ownership by) {
        return change(server, id, object -> object.kind().mayBeLetGo() && object.orphaned()
                ? object.owned(by).orphaned(false) : object);
    }

    /**
     * The thing in orbit {@code id} is destroyed: it becomes {@code pieces} pieces of debris in its orbit, or, with
     * debris turned off, is gone; whether there was one.
     */
    public static boolean destroy(final MinecraftServer server, final UUID id, final int pieces) {
        if (!SpaceServerConfig.debris()) {
            return forget(server, id);
        }
        return change(server, id, object -> object.broken(pieces));
    }

    /** A Space Persistor aboard the thing in orbit {@code id} keeps its place loaded, or no longer; whether it is. */
    public static boolean keep(final MinecraftServer server, final UUID id, final boolean kept) {
        return change(server, id, object -> object.kept(kept));
    }

    /**
     * The moving structure {@code structure}, its blocks in {@code level}, passes into the orbit of {@code body}: told
     * as a rocket, if it is one, whose it is, kept loaded if a Space Persistor stands aboard.
     */
    public static void arrived(final ServerLevel level, final MovingStructure structure, final SolarBody body) {
        Rockets.get(level.getServer(), structure.id()).ifPresent(rocket -> put(level.getServer(),
                structure.id().value(), OrbitObject.rocket(SolarSystemWorlds.worldOf(body), structure.id(),
                        rocket.ownership(), persistorAboard(level, structure))));
    }

    @Override
    public CompoundTag save(final CompoundTag tag, final HolderLookup.Provider registries) {
        MAP.encodeStart(NbtOps.INSTANCE, this.objects).result().ifPresent(saved -> tag.put(OBJECTS, saved));
        RESTS.encodeStart(NbtOps.INSTANCE, this.resting).result().ifPresent(saved -> tag.put(RESTING, saved));
        return tag;
    }

    /* Whether a Space Persistor stands among {@code structure}'s blocks. */
    private static boolean persistorAboard(final ServerLevel level, final MovingStructure structure) {
        final Box box = structure.bounds();
        for (final BlockPos pos : BlockPos.betweenClosed(box.minX(), box.minY(), box.minZ(), box.maxX(), box.maxY(),
                box.maxZ())) {
            if (level.getBlockState(pos).is(SpacePersistor.BLOCK.get())) {
                return true;
            }
        }
        return false;
    }

    /* Changes the thing in orbit {@code id} as {@code how} says; whether it changed. */
    private static boolean change(final MinecraftServer server, final UUID id,
                                  final UnaryOperator<OrbitObject> how) {
        final OrbitObjects all = of(server);
        final OrbitObject was = all.objects.get(id);
        if (was == null) {
            return false;
        }
        final OrbitObject now = how.apply(was);
        if (now.equals(was)) {
            return false;
        }
        all.objects.put(id, now);
        all.setDirty();
        return true;
    }

    private static OrbitObjects load(final CompoundTag tag, final HolderLookup.Provider registries) {
        final OrbitObjects loaded = new OrbitObjects();
        final Tag saved = tag.get(OBJECTS);
        if (saved != null) {
            MAP.parse(NbtOps.INSTANCE, saved).result().ifPresent(loaded.objects::putAll);
        }
        final Tag rests = tag.get(RESTING);
        if (rests != null) {
            RESTS.parse(NbtOps.INSTANCE, rests).result().ifPresent(loaded.resting::putAll);
        }
        return loaded;
    }

    /* The world's things in orbit, kept with its overworld. */
    private static OrbitObjects of(final MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(new SavedData.Factory<>(OrbitObjects::new,
                OrbitObjects::load), FILE);
    }

    /**
     * Where a rocket rests in its orbit, in its system's space.
     *
     * @param x     its place across
     * @param z     its place along
     * @param width how wide it is, the wider of its two sides, in blocks
     */
    public record Rest(double x, double z, double width) {

        public static final Codec<Rest> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.DOUBLE.fieldOf("x").forGetter(Rest::x),
                Codec.DOUBLE.fieldOf("z").forGetter(Rest::z),
                Codec.DOUBLE.fieldOf("width").forGetter(Rest::width)
        ).apply(instance, Rest::new));
    }
}
