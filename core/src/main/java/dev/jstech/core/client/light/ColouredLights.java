/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.light;

import com.mojang.logging.LogUtils;
import dev.jstech.core.client.moving.ClientMovingStructures;
import dev.jstech.core.light.Beams;
import dev.jstech.core.light.BeamLights;
import dev.jstech.core.light.LampBlockEntity;
import dev.jstech.core.light.LightBlock;
import dev.jstech.core.light.LightColours;
import dev.jstech.core.light.LightFlood;
import dev.jstech.core.light.LightKind;
import dev.jstech.core.light.LightMath;
import dev.jstech.core.light.LightingSettings;
import dev.jstech.core.light.SkyTints;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

/**
 * The sources of coloured light around the player, by the section of the world each is in: the blocks that give light
 * of a colour ({@link LightColours}), the lamps of the lighting kit and the places their spotlights light, and the
 * dynamic light the player's game lays. A batch of sections is looked at each tick, round and round, within the reach
 * of light, away from the main thread: there each source's light is spread through the blocks as the game spreads its
 * own ({@link LightFlood}), round walls and through stained glass, which tints it; a white source's too where a block
 * that tints stands near; and daylight come down through tinting blocks takes their colour ({@link SkyTints}). The main
 * thread only takes the batch back, and where a section's light changed, the sections it reaches are built again, so
 * the world's colours follow. A whole bright source, coloured or not, is kept too, for its glow. A section where a
 * block that gives light or tints it changed, or a lamp turned on or off, is looked at on the main thread in the tick
 * it changed, out of its turn, so the light's colour and glow show with the block, as the game's own light does.
 */
public final class ColouredLights {

    private static final Logger LOGGER = LogUtils.getLogger();
    /* A section's sources, swapped in whole; the sections waiting their turn to be looked at. */
    private static final Map<Long, List<Source>> BY_SECTION = new ConcurrentHashMap<>();
    /* The light of each source as it spreads; none yet for one the worker has not reached. */
    private static final Map<Source, LightFlood> FLOODS = new ConcurrentHashMap<>();
    /* Daylight's colour below tinting blocks, by place; the places each section's gave; the sections holding any. */
    private static final Map<Long, Integer> SKY = new ConcurrentHashMap<>();
    private static final Map<Long, long[]> SKY_BY_SECTION = new ConcurrentHashMap<>();
    private static final Set<Long> SKY_SECTIONS = ConcurrentHashMap.newKeySet();
    /*
     * The sections to look at in this tick, out of their turn, as a block there that gives light or tints it changed;
     * and the stamp each was last looked at so, for a batch the worker began before not to be taken for it.
     */
    private static final Set<Long> NOW = new LinkedHashSet<>();
    private static final Map<Long, Long> LOOKED_NOW = new HashMap<>();
    /* The most sections looked at out of their turn a tick; the rest wait for the next. */
    private static final int NOW_A_TICK = 16;
    /* The one thread the sections are looked at on, below the game's own, so it never holds a frame up. */
    private static final ExecutorService WORKER = Executors.newSingleThreadExecutor(work -> {
        final Thread thread = new Thread(work, "jscore-coloured-light");
        thread.setDaemon(true);
        thread.setPriority(Thread.MIN_PRIORITY);
        return thread;
    });
    private static final int SECTIONS_A_TICK = 48;
    private static final int SECTION = 16;
    /* What a source must give to glow without a colour: a lamp's or a lantern's, not a torch's. */
    private static final int GLOWS_FROM = 13;
    private static final ThreadLocal<Near> LAST = new ThreadLocal<>();
    /* Changed whenever the sources do, so a building thread's last answer is known stale. */
    private static final AtomicInteger VERSION = new AtomicInteger();
    /* The sky's light is told in quarters, so the world is built again only four times as day turns to night. */
    private static final float SKY_STEPS = 4.0F;
    private static final double NANOS_A_MILLI = 1_000_000.0;

    private static List<Long> queue = new ArrayList<>();
    private static int next;
    private static volatile List<Source> dynamic = List.of();
    private static volatile float sky = 1.0F;
    @Nullable
    private static Future<Batch> pending;
    /* The stamp the pending batch was begun at; stamps count up as sections are looked at and batches begun. */
    private static long pendingStamp;
    private static long stamp;
    /* The lit lamps' own light last tick, by place, to tell when one turns on, off or to another light. */
    private static Map<Long, Source> lampsBefore = Map.of();
    private static volatile double lastTickMillis;
    private static boolean warned;

    private ColouredLights() {
    }

    /** How long the main thread spent on coloured light in its last tick, in milliseconds; the worker spares it. */
    public static double lastTickMillis() {
        return lastTickMillis;
    }

    /** How many sources' light is spread now, coloured or tinted on its way. */
    public static int spread() {
        return FLOODS.size();
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
        lookNow(level, sections);
    }

    /**
     * Tells coloured light that the block at {@code pos} changed on the player's game, from {@code before} to
     * {@code after}. Where either gives light (not the light standing in the air), its section is looked at in this
     * tick; where either tints light, the sections beside it too, whose sources' light may pass through it.
     */
    public static void blockChanged(final BlockPos pos, final BlockState before, final BlockState after) {
        if (before == after) {
            return;
        }
        final long section = SectionPos.asLong(pos);
        if (LightColours.tints(before) || LightColours.tints(after)) {
            NOW.addAll(beside(section));
        } else if (gives(before) || gives(after)) {
            NOW.add(section);
        }
    }

    static void listen() {
        NeoForge.EVENT_BUS.addListener(ClientTickEvent.Post.class, event -> tick());
        NeoForge.EVENT_BUS.addListener(ClientPlayerNetworkEvent.LoggingOut.class, event -> clear());
    }

    /**
     * The coloured sources whose light may reach a place in the section {@code section}: those in it and in the
     * sections beside it, and the white ones whose light a block near tints. Asked for each block as the world's mesh
     * is built, so each building thread keeps the last section's answer until the sources change.
     */
    static List<Source> near(final long section) {
        return nearest(section).sources();
    }

    /** Whether daylight is tinted at a place in the section {@code section} or the sections beside it. */
    static boolean shaded(final long section) {
        return nearest(section).shaded();
    }

    /** The light of {@code source} as it spreads, if it has been worked out. */
    @Nullable
    static LightFlood flood(final Source source) {
        return FLOODS.get(source);
    }

    /** Daylight's colour at {@code pos}, below tinting blocks; white where it has none. */
    static int skyTint(final BlockPos pos) {
        final Integer colour = SKY.get(pos.asLong());
        return colour == null ? LightMath.WHITE : colour;
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

    /**
     * Sets the dynamic light the player's game lays, as sources, and has the sections they reach built again: at once
     * for a coloured one; for a white one once its light is spread, where a block near tints it.
     */
    static void dynamic(final List<Source> sources) {
        if (dynamic.equals(sources)) {
            return;
        }
        final Set<Long> touched = new HashSet<>();
        final Set<Source> now = new HashSet<>(sources);
        for (final Source source : dynamic) {
            if (now.contains(source)) {
                continue;
            }
            final LightFlood gone = FLOODS.remove(source);
            if (source.colour() != LightMath.WHITE) {
                touched.add(SectionPos.asLong(source.pos()));
            } else if (gone != null && gone.tinted()) {
                touched.addAll(reached(source, gone));
            }
        }
        for (final Source source : sources) {
            if (source.colour() != LightMath.WHITE) {
                touched.add(SectionPos.asLong(source.pos()));
            }
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

    /** Whether light of {@code level} and {@code colour} is kept for its glow: of a colour, or bright enough. */
    static boolean glows(final int level, final int colour) {
        return colour != LightMath.WHITE || level >= GLOWS_FROM;
    }

    /* The building thread's answer for {@code section}: its last, unless the sources changed since. */
    private static Near nearest(final long section) {
        final Near last = LAST.get();
        final int now = VERSION.get();
        if (last != null && last.section() == section && last.version() == now) {
            return last;
        }
        final Near near = new Near(section, now, gather(section), shadedAround(section));
        LAST.set(near);
        return near;
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
                        for (final Source source : sources) {
                            if (tints(source)) {
                                if (out == null) {
                                    out = new ArrayList<>();
                                }
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
                    && Math.abs(SectionPos.blockToSectionCoord(source.z()) - z) <= 1 && tints(source)) {
                if (out == null) {
                    out = new ArrayList<>();
                }
                out.add(source);
            }
        }
        return out == null ? List.of() : List.copyOf(out);
    }

    /* Whether a source gives any place a colour: one of a colour, or a white one whose light a block tinted. */
    private static boolean tints(final Source source) {
        if (source.colour() != LightMath.WHITE) {
            return true;
        }
        final LightFlood flood = FLOODS.get(source);
        return flood != null && flood.tinted();
    }

    private static boolean shadedAround(final long section) {
        if (SKY_SECTIONS.isEmpty()) {
            return false;
        }
        final int x = SectionPos.x(section);
        final int y = SectionPos.y(section);
        final int z = SectionPos.z(section);
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (SKY_SECTIONS.contains(SectionPos.asLong(x + dx, y + dy, z + dz))) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static void tick() {
        final long started = System.nanoTime();
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
                if (sources.stream().anyMatch(ColouredLights::tints)) {
                    rebuildAround(section);
                }
            });
            SKY_SECTIONS.forEach(ColouredLights::rebuild);
        }
        lampsChanged(level);
        if (!NOW.isEmpty()) {
            final List<Long> now = new ArrayList<>(Math.min(NOW.size(), NOW_A_TICK));
            for (final Iterator<Long> it = NOW.iterator(); it.hasNext() && now.size() < NOW_A_TICK; ) {
                now.add(it.next());
                it.remove();
            }
            lookNow(level, now);
        }
        if (pending != null) {
            if (!pending.isDone()) {
                lastTickMillis = (System.nanoTime() - started) / NANOS_A_MILLI;
                return;
            }
            take(level, pending, pendingStamp);
            pending = null;
        }
        if (next >= queue.size()) {
            queue = around(player.blockPosition());
            // A moving structure's blocks are out in the yard: its sections are looked at while it is near.
            queue.addAll(ClientMovingStructures.yardSectionsNear(player.position(), LightingSettings.reach()));
            next = 0;
            forgetBut(new HashSet<>(queue));
        }
        final List<Long> batch = new ArrayList<>(queue.subList(next, Math.min(queue.size(), next + SECTIONS_A_TICK)));
        next += batch.size();
        final Map<Long, List<Source>> lamps = lamps(level);
        final List<Source> moving = new ArrayList<>();
        for (final Source source : dynamic) {
            if (!FLOODS.containsKey(source)) {
                moving.add(source);
            }
        }
        pending = WORKER.submit(() -> look(level, batch, lamps, moving));
        pendingStamp = ++stamp;
        lastTickMillis = (System.nanoTime() - started) / NANOS_A_MILLI;
    }

    /* Looks at {@code sections} on the main thread now; the worker's batch begun before is not taken for them. */
    private static void lookNow(final ClientLevel level, final List<Long> sections) {
        apply(look(level, sections, lamps(level), List.of()));
        final long now = ++stamp;
        sections.forEach(section -> LOOKED_NOW.put(section, now));
    }

    /* The lamps turned on, off or to another light since the last tick: their sections are looked at now. */
    private static void lampsChanged(final ClientLevel level) {
        final Map<Long, Source> now = new HashMap<>();
        for (final LampBlockEntity lamp : LampBlockEntity.seen()) {
            if (lamp.getLevel() == level && lamp.lit()) {
                final LightKind kind = lamp.kind();
                final BlockPos at = lamp.getBlockPos();
                now.put(at.asLong(), new Source(at.getX(), at.getY(), at.getZ(), kind.intensity(), kind.colour(),
                        false));
            }
        }
        if (now.equals(lampsBefore)) {
            return;
        }
        final Set<Long> places = new HashSet<>(now.keySet());
        places.addAll(lampsBefore.keySet());
        for (final long place : places) {
            if (!Objects.equals(now.get(place), lampsBefore.get(place))) {
                NOW.add(SectionPos.asLong(BlockPos.of(place)));
            }
        }
        lampsBefore = now;
    }

    /* Takes a batch back from the worker, if it was for this world, but for the sections looked at since it began. */
    private static void take(final ClientLevel level, final Future<Batch> done, final long begun) {
        try {
            final Batch batch = done.get();
            if (batch.level() == level) {
                final List<Looked> sections = new ArrayList<>(batch.sections().size());
                for (final Looked looked : batch.sections()) {
                    if (LOOKED_NOW.getOrDefault(looked.section(), Long.MIN_VALUE) < begun) {
                        sections.add(looked);
                    }
                }
                apply(new Batch(level, sections, batch.moving()));
            }
            // A later batch is begun after every look so far: none of them can be newer than it.
            LOOKED_NOW.values().removeIf(at -> at < begun);
        } catch (final ExecutionException failed) {
            if (!warned) {
                warned = true;
                LOGGER.warn("Coloured light could not be worked out for a batch of sections", failed.getCause());
            }
        } catch (final InterruptedException interrupted) {
            Thread.currentThread().interrupt();
        }
    }

    /*
     * Looks at the sources of {@code sections} as they are now, and spreads their light: on the worker, or on the
     * main thread for a few sections that must follow at once. Reads the world's blocks and light, changes nothing.
     */
    private static Batch look(final ClientLevel level, final List<Long> sections, final Map<Long, List<Source>> lamps,
                              final List<Source> moving) {
        final LevelPassage passage = new LevelPassage(level);
        final List<Looked> looked = new ArrayList<>(sections.size());
        for (final long section : sections) {
            final List<Source> sources = new ArrayList<>(blocks(level, section));
            sources.addAll(lamps.getOrDefault(section, List.of()));
            final Map<Source, LightFlood> floods = new HashMap<>();
            for (final Source source : sources) {
                spread(level, passage, source, floods);
            }
            looked.add(new Looked(section, List.copyOf(sources), floods, shades(level, passage, section)));
        }
        final Map<Source, LightFlood> movingFloods = new HashMap<>();
        for (final Source source : moving) {
            spread(level, passage, source, movingFloods);
        }
        return new Batch(level, looked, movingFloods);
    }

    /* Spreads {@code source}'s light, if it is of a colour or a block near may tint it; kept where it gives colour. */
    private static void spread(final ClientLevel level, final LevelPassage passage, final Source source,
                               final Map<Source, LightFlood> into) {
        final boolean coloured = source.colour() != LightMath.WHITE;
        if (!coloured && !tintingNear(level, source)) {
            return;
        }
        final LightFlood flood = LightFlood.spread(source.x(), source.y(), source.z(), source.level(),
                source.colour(), passage);
        if (coloured || flood.tinted()) {
            into.put(source, flood);
        }
    }

    /* Whether a block that tints light may stand within a white source's reach: in its section or one beside it. */
    private static boolean tintingNear(final ClientLevel level, final Source source) {
        final int x = SectionPos.blockToSectionCoord(source.x());
        final int y = SectionPos.blockToSectionCoord(source.y());
        final int z = SectionPos.blockToSectionCoord(source.z());
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                final LevelChunk chunk = level.getChunkSource().getChunk(x + dx, z + dz, ChunkStatus.FULL, false);
                if (chunk == null) {
                    continue;
                }
                for (int dy = -1; dy <= 1; dy++) {
                    final int index = chunk.getSectionIndexFromSectionY(y + dy);
                    if (index >= 0 && index < chunk.getSectionsCount()
                            && chunk.getSection(index).maybeHas(LightColours::tints)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /* The places below the section's tinting blocks open to the sky, and daylight's colour there. */
    private static Map<Long, Integer> shades(final ClientLevel level, final LevelPassage passage,
                                             final long section) {
        final LevelChunkSection blocks = section(level, section);
        if (blocks == null || blocks.hasOnlyAir() || !blocks.maybeHas(LightColours::tints)) {
            return Map.of();
        }
        final Map<Long, Integer> out = new HashMap<>();
        final int bx = SectionPos.sectionToBlockCoord(SectionPos.x(section));
        final int by = SectionPos.sectionToBlockCoord(SectionPos.y(section));
        final int bz = SectionPos.sectionToBlockCoord(SectionPos.z(section));
        final BlockPos.MutableBlockPos above = new BlockPos.MutableBlockPos();
        for (int y = 0; y < SECTION; y++) {
            for (int z = 0; z < SECTION; z++) {
                for (int x = 0; x < SECTION; x++) {
                    if (!LightColours.tints(blocks.getBlockState(x, y, z))
                            || level.getBrightness(LightLayer.SKY, above.set(bx + x, by + y + 1, bz + z))
                            < LightMath.BRIGHTEST) {
                        continue;
                    }
                    SkyTints.below(bx + x, by + y, bz + z, passage, (px, py, pz, colour) ->
                            out.put(BlockPos.asLong(px, py, pz), colour));
                }
            }
        }
        return out;
    }

    /* Takes what was looked at: the sources, their light, daylight's colours; what changed is built again. */
    private static void apply(final Batch batch) {
        final Set<Long> around = new HashSet<>();
        final Set<Long> exactly = new HashSet<>();
        for (final Looked looked : batch.sections()) {
            final List<Source> before = BY_SECTION.put(looked.section(), looked.sources());
            if (before != null && !before.equals(looked.sources()) || before == null && !looked.sources().isEmpty()) {
                around.add(looked.section());
            }
            if (before != null) {
                for (final Source gone : before) {
                    if (!looked.floods().containsKey(gone)) {
                        final LightFlood flood = FLOODS.remove(gone);
                        if (flood != null) {
                            exactly.addAll(reached(gone, flood));
                        }
                    }
                }
            }
            looked.floods().forEach((source, flood) -> {
                if (!flood.equals(FLOODS.put(source, flood))) {
                    exactly.addAll(reached(source, flood));
                }
            });
            exactly.addAll(shade(looked.section(), looked.shades()));
        }
        batch.moving().forEach((source, flood) -> {
            if (dynamic.contains(source) && !flood.equals(FLOODS.put(source, flood)) && flood.tinted()) {
                exactly.addAll(reached(source, flood));
            }
        });
        if (!around.isEmpty() || !exactly.isEmpty()) {
            VERSION.incrementAndGet();
            around.forEach(ColouredLights::rebuildAround);
            exactly.forEach(ColouredLights::rebuild);
        }
    }

    /* Swaps in daylight's colours below a section's tinting blocks; the sections whose places changed. */
    private static Set<Long> shade(final long section, final Map<Long, Integer> shades) {
        final long[] before = SKY_BY_SECTION.getOrDefault(section, new long[0]);
        final long[] now = shades.keySet().stream().mapToLong(Long::longValue).sorted().toArray();
        boolean same = Arrays.equals(before, now);
        for (int i = 0; same && i < now.length; i++) {
            same = Objects.equals(SKY.get(now[i]), shades.get(now[i]));
        }
        if (same) {
            return Set.of();
        }
        final Set<Long> touched = new HashSet<>();
        for (final long cell : before) {
            SKY.remove(cell);
            touched.add(SectionPos.asLong(BlockPos.of(cell)));
        }
        SKY.putAll(shades);
        for (final long cell : now) {
            touched.add(SectionPos.asLong(BlockPos.of(cell)));
        }
        if (now.length == 0) {
            SKY_BY_SECTION.remove(section);
        } else {
            SKY_BY_SECTION.put(section, now);
        }
        SKY_SECTIONS.clear();
        for (final long cell : SKY.keySet()) {
            SKY_SECTIONS.add(SectionPos.asLong(BlockPos.of(cell)));
        }
        final Set<Long> out = new HashSet<>();
        for (final long sectionTouched : touched) {
            out.addAll(beside(sectionTouched));
        }
        return out;
    }

    /* Lets go of the sections out of reach, their sources' light and daylight's colours under them. */
    private static void forgetBut(final Set<Long> kept) {
        boolean changed = false;
        final List<Long> gone = new ArrayList<>();
        for (final Map.Entry<Long, List<Source>> entry : BY_SECTION.entrySet()) {
            if (!kept.contains(entry.getKey())) {
                gone.add(entry.getKey());
            }
        }
        for (final long section : gone) {
            for (final Source source : BY_SECTION.remove(section)) {
                FLOODS.remove(source);
            }
            changed = true;
        }
        for (final long section : new ArrayList<>(SKY_BY_SECTION.keySet())) {
            if (!kept.contains(section)) {
                shade(section, Map.of());
                changed = true;
            }
        }
        if (changed) {
            VERSION.incrementAndGet();
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

    @Nullable
    private static LevelChunkSection section(final ClientLevel level, final long section) {
        final LevelChunk chunk = level.getChunkSource().getChunk(SectionPos.x(section), SectionPos.z(section),
                ChunkStatus.FULL, false);
        if (chunk == null) {
            return null;
        }
        final int index = chunk.getSectionIndexFromSectionY(SectionPos.y(section));
        return index < 0 || index >= chunk.getSectionsCount() ? null : chunk.getSection(index);
    }

    /* A section's blocks that give light, coloured or bright enough to glow. */
    private static List<Source> blocks(final ClientLevel level, final long section) {
        final LevelChunkSection blocks = section(level, section);
        if (blocks == null || blocks.hasOnlyAir() || !blocks.maybeHas(ColouredLights::worthKeeping)) {
            return List.of();
        }
        final int sx = SectionPos.x(section);
        final int sy = SectionPos.y(section);
        final int sz = SectionPos.z(section);
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

    /* A block that gives light; not the light standing in the air, which the lamps' light and dynamic light lay. */
    private static boolean gives(final BlockState state) {
        return state.getLightEmission() > 0 && !(state.getBlock() instanceof LightBlock);
    }

    /* A block that gives light of a colour, or bright enough to glow; not the light standing in the air. */
    private static boolean worthKeeping(final BlockState state) {
        final int level = state.getLightEmission();
        return level > 0 && !(state.getBlock() instanceof LightBlock) && glows(level, LightColours.of(state));
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

    /* The sections whose blocks look into a place a source's light reaches. */
    private static Set<Long> reached(final Source source, final LightFlood flood) {
        final int reach = flood.reach() + 1;
        final Set<Long> out = new HashSet<>();
        for (int x = SectionPos.blockToSectionCoord(source.x() - reach);
             x <= SectionPos.blockToSectionCoord(source.x() + reach); x++) {
            for (int y = SectionPos.blockToSectionCoord(source.y() - reach);
                 y <= SectionPos.blockToSectionCoord(source.y() + reach); y++) {
                for (int z = SectionPos.blockToSectionCoord(source.z() - reach);
                     z <= SectionPos.blockToSectionCoord(source.z() + reach); z++) {
                    out.add(SectionPos.asLong(x, y, z));
                }
            }
        }
        return out;
    }

    /* A section and the 26 beside it. */
    private static Set<Long> beside(final long section) {
        final Set<Long> out = new HashSet<>();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    out.add(SectionPos.asLong(SectionPos.x(section) + dx, SectionPos.y(section) + dy,
                            SectionPos.z(section) + dz));
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

    private static void rebuild(final long section) {
        Minecraft.getInstance().levelRenderer.setSectionDirty(SectionPos.x(section), SectionPos.y(section),
                SectionPos.z(section));
    }

    private static void clear() {
        if (!BY_SECTION.isEmpty() || !dynamic.isEmpty() || !FLOODS.isEmpty() || !SKY.isEmpty()) {
            VERSION.incrementAndGet();
        }
        BY_SECTION.clear();
        FLOODS.clear();
        SKY.clear();
        SKY_BY_SECTION.clear();
        SKY_SECTIONS.clear();
        queue = new ArrayList<>();
        next = 0;
        dynamic = List.of();
        pending = null;
        NOW.clear();
        LOOKED_NOW.clear();
        lampsBefore = Map.of();
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

    /* A building thread's last answer: the section asked, the sources' version then, what they were, and the sky. */
    private record Near(long section, int version, List<Source> sources, boolean shaded) {

        Near {
            sources = List.copyOf(sources);
        }
    }

    /* What the worker looked at: the world it was for, each section's, and the spread light of what moves. */
    private record Batch(ClientLevel level, List<Looked> sections, Map<Source, LightFlood> moving) {
    }

    /* A section looked at: its sources, their light spread, and daylight's colour below its tinting blocks. */
    private record Looked(long section, List<Source> sources, Map<Source, LightFlood> floods,
                          Map<Long, Integer> shades) {
    }

    /* The world's blocks as light goes through them, read away from the main thread, one chunk kept at a time. */
    private static final class LevelPassage implements LightFlood.IPassage {

        private final ClientLevel level;
        private final BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();
        @Nullable
        private LevelChunk chunk;
        private int chunkX = Integer.MIN_VALUE;
        private int chunkZ = Integer.MIN_VALUE;

        LevelPassage(final ClientLevel level) {
            this.level = level;
        }

        @Override
        public int dims(final int x, final int y, final int z) {
            final BlockState state = state(x, y, z);
            return state.isAir() ? 0 : state.getLightBlock(this.level, this.at.set(x, y, z));
        }

        @Override
        public int tint(final int x, final int y, final int z) {
            return LightColours.tint(state(x, y, z));
        }

        private BlockState state(final int x, final int y, final int z) {
            if (this.level.isOutsideBuildHeight(y)) {
                return Blocks.AIR.defaultBlockState();
            }
            final int cx = SectionPos.blockToSectionCoord(x);
            final int cz = SectionPos.blockToSectionCoord(z);
            if (cx != this.chunkX || cz != this.chunkZ) {
                this.chunk = this.level.getChunkSource().getChunk(cx, cz, ChunkStatus.FULL, false);
                this.chunkX = cx;
                this.chunkZ = cz;
            }
            return this.chunk == null ? Blocks.AIR.defaultBlockState() : this.chunk.getBlockState(this.at.set(x, y,
                    z));
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
