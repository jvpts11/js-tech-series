/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.light;

import dev.jstech.core.client.moving.ClientMovingStructures;
import dev.jstech.core.light.Beams;
import dev.jstech.core.light.BeamLights;
import dev.jstech.core.light.LampBlockEntity;
import dev.jstech.core.light.LightBlock;
import dev.jstech.core.light.LightColours;
import dev.jstech.core.light.LightKind;
import dev.jstech.core.light.LightMath;
import dev.jstech.core.light.LightingSettings;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;

/**
 * The sources of coloured light around the player, by the section of the world each is in: the blocks that give light
 * of a colour ({@link LightColours}), the lamps of the lighting kit and the places their spotlights light, and the
 * dynamic light the player's game lays. Looked at a batch of sections a tick, round and round, within the reach of
 * light; where a section's sources change, the sections they reach are built again, so the world's colours follow.
 * A whole bright source, coloured or not, is kept too, for its glow.
 */
public final class ColouredLights {

    /* A section's sources, swapped in whole; the sections waiting their turn to be looked at. */
    private static final Map<Long, List<Source>> BY_SECTION = new ConcurrentHashMap<>();
    private static final int SECTIONS_A_TICK = 48;
    private static final int SECTION = 16;
    /* What a source must give to glow without a colour: a lamp's or a lantern's, not a torch's. */
    private static final int GLOWS_FROM = 13;
    private static final ThreadLocal<Near> LAST = new ThreadLocal<>();
    /* Changed whenever the sources do, so a building thread's last answer is known stale. */
    private static final AtomicInteger VERSION = new AtomicInteger();

    /* The sky's light is told in quarters, so the world is built again only four times as day turns to night. */
    private static final float SKY_STEPS = 4.0F;

    private static List<Long> queue = new ArrayList<>();
    private static int next;
    private static volatile List<Source> dynamic = List.of();
    private static volatile float sky = 1.0F;

    private ColouredLights() {
    }

    static void listen() {
        NeoForge.EVENT_BUS.addListener(ClientTickEvent.Post.class, event -> tick());
        NeoForge.EVENT_BUS.addListener(ClientPlayerNetworkEvent.LoggingOut.class, event -> clear());
    }

    /**
     * The coloured sources whose light may reach a place in the section {@code section}: those in it and in the
     * sections beside it. Asked for each block as the world's mesh is built, so each building thread keeps the last
     * section's answer until the sources change.
     */
    static List<Source> near(final long section) {
        final Near last = LAST.get();
        final int now = VERSION.get();
        if (last != null && last.section() == section && last.version() == now) {
            return last.sources();
        }
        final List<Source> sources = gather(section);
        LAST.set(new Near(section, now, sources));
        return sources;
    }

    /** The blocks kept that glow, coloured or bright, for their halos. */
    static List<Source> glowing() {
        final List<Source> out = new ArrayList<>();
        for (final List<Source> sources : BY_SECTION.values()) {
            for (final Source source : sources) {
                if (source.glows()) {
                    out.add(source);
                }
            }
        }
        return out;
    }

    /** Sets the dynamic light the player's game lays, as sources, and has the sections they reach built again. */
    static void dynamic(final List<Source> sources) {
        if (dynamic.equals(sources)) {
            return;
        }
        final Set<Long> touched = new HashSet<>();
        for (final Source source : dynamic) {
            touched.add(SectionPos.asLong(source.pos()));
        }
        for (final Source source : sources) {
            touched.add(SectionPos.asLong(source.pos()));
        }
        dynamic = List.copyOf(sources);
        VERSION.incrementAndGet();
        touched.forEach(ColouredLights::rebuildAround);
    }

    /**
     * How much of its daylight the open sky gives now, in quarters: all of it by day, a fifth by night. Open sky
     * drowns coloured light by day, and leaves it by night.
     */
    static float sky() {
        return sky;
    }

    private static List<Source> gather(final long section) {
        final int x = SectionPos.x(section);
        final int y = SectionPos.y(section);
        final int z = SectionPos.z(section);
        List<Source> out = null;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    final List<Source> sources = BY_SECTION.get(SectionPos.asLong(x + dx, y + dy, z + dz));
                    if (sources != null && !sources.isEmpty()) {
                        if (out == null) {
                            out = new ArrayList<>();
                        }
                        for (final Source source : sources) {
                            if (source.colour() != LightMath.WHITE) {
                                out.add(source);
                            }
                        }
                    }
                }
            }
        }
        for (final Source source : dynamic) {
            if (Math.abs(SectionPos.blockToSectionCoord(source.x()) - x) <= 1
                    && Math.abs(SectionPos.blockToSectionCoord(source.y()) - y) <= 1
                    && Math.abs(SectionPos.blockToSectionCoord(source.z()) - z) <= 1
                    && source.colour() != LightMath.WHITE) {
                if (out == null) {
                    out = new ArrayList<>();
                }
                out.add(source);
            }
        }
        return out == null ? List.of() : List.copyOf(out);
    }

    private static void tick() {
        final Minecraft minecraft = Minecraft.getInstance();
        final ClientLevel level = minecraft.level;
        final LocalPlayer player = minecraft.player;
        if (level == null || player == null) {
            clear();
            return;
        }
        final float daylight = Math.round(level.getSkyDarken(1.0F) * SKY_STEPS) / SKY_STEPS;
        if (daylight != sky) {
            sky = daylight;
            VERSION.incrementAndGet();
            BY_SECTION.forEach((section, sources) -> {
                if (sources.stream().anyMatch(source -> source.colour() != LightMath.WHITE)) {
                    rebuildAround(section);
                }
            });
        }
        if (next >= queue.size()) {
            queue = around(player.blockPosition());
            // A moving structure's blocks are out in the yard: its sections are looked at while it is near.
            queue.addAll(ClientMovingStructures.yardSectionsNear(player.position(), LightingSettings.reach()));
            next = 0;
            if (BY_SECTION.keySet().retainAll(new HashSet<>(queue))) {
                VERSION.incrementAndGet();
            }
        }
        final Map<Long, List<Source>> lamps = lamps(level);
        for (int i = 0; i < SECTIONS_A_TICK && next < queue.size(); i++, next++) {
            look(level, queue.get(next), lamps);
        }
    }

    /**
     * Looks at {@code sections} again now rather than at their turn: the blocks there changed hands with a moving
     * structure's, and their glow and coloured light go with them in the frame they do.
     */
    public static void lookAgain(final List<Long> sections) {
        final ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }
        final Map<Long, List<Source>> lamps = lamps(level);
        for (final long section : sections) {
            look(level, section, lamps);
        }
    }

    /* Takes the sources of the section {@code section} as they are now; what their light reaches is built again. */
    private static void look(final ClientLevel level, final long section, final Map<Long, List<Source>> lamps) {
        final List<Source> now = new ArrayList<>(blocks(level, section));
        now.addAll(lamps.getOrDefault(section, List.of()));
        final List<Source> before = BY_SECTION.put(section, List.copyOf(now));
        if (before != null && !before.equals(now) || before == null && !now.isEmpty()) {
            VERSION.incrementAndGet();
            rebuildAround(section);
        }
    }

    /* The sections within the reach of light, nearest first. */
    private static List<Long> around(final BlockPos at) {
        final int reach = Math.max(1, LightingSettings.reach() / SECTION);
        final int x = SectionPos.blockToSectionCoord(at.getX());
        final int y = SectionPos.blockToSectionCoord(at.getY());
        final int z = SectionPos.blockToSectionCoord(at.getZ());
        final List<long[]> sections = new ArrayList<>();
        for (int dx = -reach; dx <= reach; dx++) {
            for (int dy = -reach; dy <= reach; dy++) {
                for (int dz = -reach; dz <= reach; dz++) {
                    sections.add(new long[] {SectionPos.asLong(x + dx, y + dy, z + dz), (long) dx * dx + dy * dy
                            + dz * dz});
                }
            }
        }
        sections.sort((a, b) -> Long.compare(a[1], b[1]));
        final List<Long> out = new ArrayList<>(sections.size());
        sections.forEach(section -> out.add(section[0]));
        return out;
    }

    /* A section's blocks that give light, coloured or bright enough to glow. */
    private static List<Source> blocks(final ClientLevel level, final long section) {
        final int sx = SectionPos.x(section);
        final int sy = SectionPos.y(section);
        final int sz = SectionPos.z(section);
        final LevelChunk chunk = level.getChunkSource().getChunk(sx, sz, ChunkStatus.FULL, false);
        if (chunk == null) {
            return List.of();
        }
        final int index = chunk.getSectionIndexFromSectionY(sy);
        if (index < 0 || index >= chunk.getSectionsCount()) {
            return List.of();
        }
        final LevelChunkSection blocks = chunk.getSection(index);
        if (blocks.hasOnlyAir() || !blocks.maybeHas(ColouredLights::worthKeeping)) {
            return List.of();
        }
        final List<Source> out = new ArrayList<>();
        for (int y = 0; y < SECTION; y++) {
            for (int z = 0; z < SECTION; z++) {
                for (int x = 0; x < SECTION; x++) {
                    final BlockState state = blocks.getBlockState(x, y, z);
                    if (worthKeeping(state)) {
                        out.add(new Source(SectionPos.sectionToBlockCoord(sx) + x,
                                SectionPos.sectionToBlockCoord(sy) + y, SectionPos.sectionToBlockCoord(sz) + z,
                                state.getLightEmission(), LightColours.of(state), true));
                    }
                }
            }
        }
        return out;
    }

    /* A block that gives light of a colour, or bright enough to glow; not the light standing in the air. */
    private static boolean worthKeeping(final BlockState state) {
        final int level = state.getLightEmission();
        return level > 0 && !(state.getBlock() instanceof LightBlock) && glows(level, LightColours.of(state));
    }

    /** Whether light of {@code level} and {@code colour} is kept for its glow: of a colour, or bright enough. */
    static boolean glows(final int level, final int colour) {
        return colour != LightMath.WHITE || level >= GLOWS_FROM;
    }

    /* The lamps the player's game has, and the places their spotlights light, by section. */
    private static Map<Long, List<Source>> lamps(final ClientLevel level) {
        final Map<Long, List<Source>> out = new HashMap<>();
        for (final LampBlockEntity lamp : LampBlockEntity.seen()) {
            if (lamp.getLevel() != level || !lamp.lit()) {
                continue;
            }
            // Its whole light, not its flicker: the flicker is in the game's light, which dims the tint with it.
            final LightKind kind = lamp.kind();
            final BlockPos at = lamp.getBlockPos();
            out.computeIfAbsent(SectionPos.asLong(at), key -> new ArrayList<>()).add(new Source(at.getX(),
                    at.getY(), at.getZ(), kind.intensity(), kind.colour(), false));
            if (kind.shape() != LightKind.Shape.POINT) {
                for (final Map.Entry<BlockPos, BeamLights.Lit> lit : LampBeams.cells(level, lamp, kind)
                        .entrySet()) {
                    final BlockPos cell = lit.getKey();
                    out.computeIfAbsent(SectionPos.asLong(cell), key -> new ArrayList<>()).add(new Source(cell.getX(),
                            cell.getY(), cell.getZ(), lit.getValue().level(), lit.getValue().colour(), false));
                }
            }
        }
        return out;
    }

    /* Has the sections the light of a source in {@code section} may reach built again. */
    private static void rebuildAround(final long section) {
        Minecraft.getInstance().levelRenderer.setSectionDirtyWithNeighbors(SectionPos.x(section),
                SectionPos.y(section), SectionPos.z(section));
    }

    private static void clear() {
        if (!BY_SECTION.isEmpty() || !dynamic.isEmpty()) {
            VERSION.incrementAndGet();
        }
        BY_SECTION.clear();
        queue = new ArrayList<>();
        next = 0;
        dynamic = List.of();
        LampBeams.clear();
    }

    /**
     * A source of light.
     *
     * @param x      where it is, across
     * @param y      up
     * @param z      and along
     * @param level  its level, from 1 to 15
     * @param colour its colour, {@code 0xRRGGBB}; white for none
     * @param glows  whether it is a block that glows, with a halo of its own; not a spotlight's lit place, nor a lamp,
     *               whose halo flickers with it, nor what dynamic light lays
     */
    record Source(int x, int y, int z, int level, int colour, boolean glows) {

        /** Where it is. */
        BlockPos pos() {
            return new BlockPos(this.x, this.y, this.z);
        }
    }

    /* A building thread's last answer: the section asked, the sources' version then, and what they were. */
    private record Near(long section, int version, List<Source> sources) {

        Near {
            sources = List.copyOf(sources);
        }
    }

    /* The places a lamp's spotlight lights, worked out as the server does, at most once a second each. */
    private static final class LampBeams {

        private static final Map<BlockPos, Cached> CACHE = new ConcurrentHashMap<>();
        private static final long KEEP_TICKS = 20;

        static Map<BlockPos, BeamLights.Lit> cells(final ClientLevel level, final LampBlockEntity lamp,
                                                   final LightKind kind) {
            final long now = level.getGameTime();
            final Cached cached = CACHE.get(lamp.getBlockPos());
            // A lamp aboard a structure going sweeps the world: what it lights is worked out again each tick.
            final long keep = Beams.goingAboard(level, lamp.getBlockPos()) ? 1 : KEEP_TICKS;
            if (cached != null && now - cached.at() < keep && now >= cached.at()) {
                return cached.cells();
            }
            final Map<BlockPos, BeamLights.Lit> cells = Beams.lamp(level, lamp.getBlockPos(), lamp.facing(), kind,
                    kind.intensity());
            CACHE.put(lamp.getBlockPos(), new Cached(now, cells));
            return cells;
        }

        static void clear() {
            CACHE.clear();
        }

        private record Cached(long at, Map<BlockPos, BeamLights.Lit> cells) {

            Cached {
                cells = Map.copyOf(cells);
            }
        }
    }
}
