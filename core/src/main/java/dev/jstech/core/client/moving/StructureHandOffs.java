/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.moving;

import dev.jstech.core.client.light.ColouredLights;
import dev.jstech.core.moving.StructureFrame;
import dev.jstech.core.moving.StructureHandOffPayload;
import dev.jstech.core.region.Box;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * Hands the drawing of a structure's blocks over between the structure and the world in one frame, as they change
 * hands: the game builds a section of the world again a few frames after its blocks change, and the structure is
 * drawn as soon as its own sections are built, so either way, for a few frames, neither would be drawn.
 * <ul>
 *   <li>Arriving, the blocks leave the world for the structure's plot. The sections of the world they leave, and those
 *       beside them their light reaches, are held as they were drawn until the structure is drawn; then they are
 *       built again, and the structure is drawn from the frame the sections that held its blocks are drawn without
 *       them.</li>
 *   <li>Landing, the structure is set down. It stands where it lands, told nothing more, and is drawn there, its
 *       block entities and its glow with it, after its entity has gone, until the sections of the world its blocks
 *       land in are drawn with them.</li>
 * </ul>
 * A section counts as drawn anew when a build of it begun once its blocks changed hands is put on the graphics card:
 * the game lets go of any build begun before, so the first drawn after is that one. A section out of sight waits for
 * nothing, built when it comes into sight with the blocks as they are by then. Either hand-off waits a few seconds at
 * most, then hands over as it is. All of it on the render thread, but for a build of an empty section, drawn on a
 * builder's.
 */
final class StructureHandOffs {

    /* The most a hand-off waits, in ticks; and the most an arriving structure built waits for the light to rest. */
    private static final int LONGEST = 60;
    private static final int LIGHT_WAIT = 5;
    /* The land the blocks of arriving structures leave, by the structure's plot. */
    private static final Int2ObjectMap<Leaving> LEAVING = new Int2ObjectOpenHashMap<>();
    /* The structures being set down. */
    private static final List<Landing> LANDING = new ArrayList<>();
    /* The sections waited for whose build under way began once their blocks changed hands, and those drawn so. */
    private static final Set<Long> BEGUN = ConcurrentHashMap.newKeySet();
    private static final Set<Long> DRAWN = ConcurrentHashMap.newKeySet();

    private StructureHandOffs() {
    }

    /** The server told that a structure's blocks are about to change hands with the world's, at the game's tick. */
    static void told(final StructureHandOffPayload payload, final Level level, final long now) {
        if (!payload.landing()) {
            LEAVING.put(payload.plot(), new Leaving(payload.plot(), payload.world(), now + LONGEST,
                    holding(level, payload.world())));
            return;
        }
        final ClientStructure structure = ClientMovingStructures.byPlot(payload.plot());
        if (structure == null) {
            return;
        }
        structure.land(payload.pose());
        LANDING.add(new Landing(structure, payload.world(), now + LONGEST, landingIn(structure)));
    }

    /**
     * Whether the game's building again of the section of the world at {@code (x, y, z)} waits: one of the land an
     * arriving structure's blocks leave, until the structure is drawn.
     */
    static boolean holdsBack(final int x, final int y, final int z) {
        for (final Leaving leaving : LEAVING.values()) {
            if (!leaving.released && leaving.near(x, y, z)) {
                leaving.held.add(SectionPos.asLong(x, y, z));
                return true;
            }
        }
        return false;
    }

    /** The game begins to build the section {@code key} of the world, from its blocks as they are now. */
    static void buildBegun(final long key) {
        for (final Leaving leaving : LEAVING.values()) {
            if (leaving.released && leaving.holding.contains(key)) {
                BEGUN.add(key);
                return;
            }
        }
        for (final Landing landing : LANDING) {
            if (landing.arrived(key)) {
                BEGUN.add(key);
                return;
            }
        }
    }

    /** A build of the section {@code key} of the world is drawn; on any thread. */
    static void built(final long key) {
        if (BEGUN.contains(key)) {
            DRAWN.add(key);
        }
    }

    /**
     * Each frame, once the game has put the sections it built on the graphics card and before the structures are
     * drawn: hands over what is ready.
     */
    static void frame(final LevelRenderer renderer, final long now) {
        if (!busy()) {
            return;
        }
        final Set<SectionRenderDispatcher.RenderSection> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        seen.addAll(renderer.visibleSections);
        final Iterator<Leaving> leavings = LEAVING.values().iterator();
        while (leavings.hasNext()) {
            final Leaving leaving = leavings.next();
            if (!leaving.released) {
                final ClientStructure structure = ClientMovingStructures.byPlot(leaving.plot);
                if (structure != null && built(structure) && leaving.builtAt < 0) {
                    leaving.builtAt = now;
                }
                // Built, it waits a few ticks at most for the light: a world whose light never rests holds nothing up.
                if (leaving.builtAt >= 0 && (lightRests(structure) || now - leaving.builtAt > LIGHT_WAIT)
                        || now > leaving.until) {
                    leaving.release(renderer);
                }
            } else if (drawnAnew(renderer, seen, leaving.holding) || now > leaving.until) {
                leavings.remove();
                lookAgain(leaving.world, ClientMovingStructures.byPlot(leaving.plot));
            }
        }
        final Iterator<Landing> landings = LANDING.iterator();
        while (landings.hasNext()) {
            final Landing landing = landings.next();
            if (!landing.done && (drawnAnew(renderer, seen, landing.places.keySet()) || now > landing.until)) {
                landing.done = true;
                lookAgain(landing.world, null);
            }
            if (landing.done && landing.gone) {
                landing.structure.meshes().close();
                landings.remove();
            }
        }
        if (!busy()) {
            BEGUN.clear();
            DRAWN.clear();
        }
    }

    /** Whether {@code structure} waits to be drawn, as the land its blocks leave is still drawn as it was. */
    static boolean hidden(final ClientStructure structure) {
        return LEAVING.containsKey(structure.plot());
    }

    /**
     * {@code structure}'s entity has left the player's world: whether it is kept, drawn where it landed, until the land
     * is drawn with its blocks.
     */
    static boolean keeps(final ClientStructure structure) {
        for (final Landing landing : LANDING) {
            if (landing.structure == structure) {
                landing.gone = true;
                return !landing.done;
            }
        }
        return false;
    }

    /** The structures set down whose entity has gone, still drawn where they landed. */
    static List<ClientStructure> landed() {
        final List<ClientStructure> out = new ArrayList<>();
        for (final Landing landing : LANDING) {
            if (landing.gone && !landing.done) {
                out.add(landing.structure);
            }
        }
        return out;
    }

    /** Whether any hand-off is under way. */
    static boolean busy() {
        return !LEAVING.isEmpty() || !LANDING.isEmpty();
    }

    /** Lets go of every hand-off, as the player's world goes. */
    static void clear() {
        for (final Landing landing : LANDING) {
            if (landing.gone) {
                landing.structure.meshes().close();
            }
        }
        LANDING.clear();
        LEAVING.clear();
        BEGUN.clear();
        DRAWN.clear();
    }

    /* Whether {@code structure}'s blocks are on the player's game, every section of them built. */
    private static boolean built(final ClientStructure structure) {
        return structure.ready() && structure.meshes().built() > 0 && !structure.meshes().waiting();
    }

    /*
     * Whether the light of the player's game rests, so {@code structure}'s sections, built again whenever it changes,
     * are built as they will be drawn.
     */
    private static boolean lightRests(@Nullable final ClientStructure structure) {
        return structure != null && built(structure) && structure.level() instanceof ClientLevel level
                && level.isLightUpdateQueueEmpty() && !level.getLightEngine().hasLightWork();
    }

    /* The sections of the world holding a block in {@code box} on the player's game now. */
    private static LongSet holding(final Level level, final Box box) {
        final LongSet out = new LongOpenHashSet();
        for (final BlockPos at : BlockPos.betweenClosed(box.minX(), box.minY(), box.minZ(), box.maxX(), box.maxY(),
                box.maxZ())) {
            if (!level.getBlockState(at).isAir()) {
                out.add(SectionPos.asLong(at));
            }
        }
        return out;
    }

    /*
     * The places of the world {@code structure}'s blocks land on, by the section of the world each is in, as it stands
     * landed, its plot not yet cleared.
     */
    private static Long2ObjectMap<List<Place>> landingIn(final ClientStructure structure) {
        final Long2ObjectMap<List<Place>> out = new Long2ObjectOpenHashMap<>();
        final StructureFrame frame = structure.frame();
        final Box box = structure.bounds();
        for (final BlockPos at : BlockPos.betweenClosed(box.minX(), box.minY(), box.minZ(), box.maxX(), box.maxY(),
                box.maxZ())) {
            final BlockState state = structure.level().getBlockState(at);
            if (!state.isAir()) {
                final double x = at.getX() + 0.5;
                final double y = at.getY() + 0.5;
                final double z = at.getZ() + 0.5;
                final BlockPos onto = BlockPos.containing(frame.worldX(x, y, z), frame.worldY(x, y, z),
                        frame.worldZ(x, y, z));
                out.computeIfAbsent(SectionPos.asLong(onto), key -> new ArrayList<>())
                        .add(new Place(onto, state.getBlock()));
            }
        }
        return out;
    }

    /*
     * Has the glow and coloured light of the land {@code world}, and of {@code structure}'s blocks, looked at again now
     * rather than at their turn, so they change hands in the frame the blocks do.
     */
    private static void lookAgain(final Box world, @Nullable final ClientStructure structure) {
        final List<Long> sections = new ArrayList<>();
        addSections(world, sections);
        if (structure != null) {
            addSections(structure.bounds(), sections);
        }
        ColouredLights.lookAgain(sections);
    }

    private static void addSections(final Box box, final List<Long> into) {
        for (int x = box.minX() >> 4; x <= box.maxX() >> 4; x++) {
            for (int y = box.minY() >> 4; y <= box.maxY() >> 4; y++) {
                for (int z = box.minZ() >> 4; z <= box.maxZ() >> 4; z++) {
                    into.add(SectionPos.asLong(x, y, z));
                }
            }
        }
    }

    /* Whether each section of {@code keys} the game draws this frame is drawn from a build begun once it changed. */
    private static boolean drawnAnew(final LevelRenderer renderer,
                                     final Set<SectionRenderDispatcher.RenderSection> seen, final Iterable<Long> keys) {
        if (renderer.viewArea == null) {
            return true;
        }
        for (final long key : keys) {
            final SectionRenderDispatcher.RenderSection section = renderer.viewArea.getRenderSectionAt(
                    SectionPos.of(key).origin());
            if (section != null && seen.contains(section) && !DRAWN.contains(key)) {
                return false;
            }
        }
        return true;
    }

    /* Whether the section at {@code (x, y, z)} is of the box {@code box}, or a section beside it. */
    private static boolean nearBox(final Box box, final int x, final int y, final int z) {
        return x >= (box.minX() >> 4) - 1 && x <= (box.maxX() >> 4) + 1 && y >= (box.minY() >> 4) - 1
                && y <= (box.maxY() >> 4) + 1 && z >= (box.minZ() >> 4) - 1 && z <= (box.maxZ() >> 4) + 1;
    }

    /* The land an arriving structure's blocks leave: held as it was drawn, then built again. */
    private static final class Leaving {

        private final int plot;
        private final Box world;
        private final long until;
        /* The sections that held its blocks, waited for; and every section held. */
        private final LongSet holding;
        private final LongSet held = new LongOpenHashSet();
        private boolean released;
        /* The tick the structure was first built whole, or below zero. */
        private long builtAt = -1;

        private Leaving(final int plot, final Box world, final long until, final LongSet holding) {
            this.plot = plot;
            this.world = world;
            this.until = until;
            this.holding = holding;
        }

        /* Whether the section at {@code (x, y, z)} is of the land left, or beside it, where its light reaches. */
        boolean near(final int x, final int y, final int z) {
            return nearBox(world, x, y, z);
        }

        /* The structure is drawn: the sections held are built again, and it waits until those of its blocks are. */
        void release(final LevelRenderer renderer) {
            released = true;
            for (final long key : held) {
                renderer.setSectionDirty(SectionPos.x(key), SectionPos.y(key), SectionPos.z(key));
            }
            // A section that held its blocks but was not marked meanwhile is built again too, to be waited for.
            for (final long key : holding) {
                if (!held.contains(key)) {
                    renderer.setSectionDirty(SectionPos.x(key), SectionPos.y(key), SectionPos.z(key));
                }
            }
            held.clear();
        }
    }

    /* A structure set down: drawn where it lands until the land is drawn with its blocks. */
    private static final class Landing {

        private final ClientStructure structure;
        private final Box world;
        private final long until;
        /* The places its blocks land on, by the section of the world each is in. */
        private final Long2ObjectMap<List<Place>> places;
        private boolean gone;
        private boolean done;

        private Landing(final ClientStructure structure, final Box world, final long until,
                        final Long2ObjectMap<List<Place>> places) {
            this.structure = structure;
            this.world = world;
            this.until = until;
            this.places = places;
        }

        /* Whether the section {@code key} is one its blocks land in, and they have come to it on the player's game. */
        boolean arrived(final long key) {
            final List<Place> onto = places.get(key);
            if (onto == null) {
                return false;
            }
            for (final Place place : onto) {
                if (structure.level().getBlockState(place.at()).is(place.block())) {
                    return true;
                }
            }
            return false;
        }
    }

    /* A place of the world a block of a structure set down lands on, and the block. */
    private record Place(BlockPos at, Block block) {
    }
}
