/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.worldgen.structure;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.jstech.core.JsCore;
import dev.jstech.core.state.CoreState;
import dev.jstech.core.state.CoreStates;
import dev.jstech.core.state.DimensionState;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

/**
 * The settlements that grow over time: each one is noted the first time its chunk is loaded, and from then on, as the
 * world's days pass, the stages its growth allows are built from its plan ({@link SettlementPlan}), into the chunks
 * that are loaded; a chunk loaded later catches up with the stages it missed. A settlement grows to the size it was
 * laid out at as it generated, and no further.
 */
public final class Settlements {

    private static final DimensionState<Book> BOOK = CoreState.builder(
            ResourceLocation.fromNamespaceAndPath(JsCore.MODID, "settlements"), Book.CODEC, Book.EMPTY).dimension();
    /* How often the settlements of a dimension are looked at, in ticks: growth is counted in days. */
    private static final int EVERY = 200;
    private static final long DAY = 24_000L;

    private Settlements() {
    }

    /** Registers the settlements' book and hands the game's bus what grows them, once, from the Core's constructor. */
    public static void listen() {
        CoreStates.register(BOOK);
        NeoForge.EVENT_BUS.addListener(ChunkEvent.Load.class, Settlements::onChunkLoad);
        NeoForge.EVENT_BUS.addListener(LevelTickEvent.Post.class, event -> {
            if (event.getLevel() instanceof ServerLevel level && level.getGameTime() % EVERY == 0) {
                grow(level);
            }
        });
    }

    /** The settlements {@code level} has noted, by their keys. */
    public static Map<String, Settlement> of(final ServerLevel level) {
        return BOOK.get(level).settlements();
    }

    /**
     * Makes the settlement {@code key} of {@code level} older by {@code days} of the world's days, as if first seen
     * that much earlier, and builds what that allows, as a story moving time on would.
     *
     * @return whether the settlement exists
     */
    public static boolean advance(final ServerLevel level, final String key, final int days) {
        final Settlement settlement = of(level).get(key);
        if (settlement == null) {
            return false;
        }
        save(level, key, settlement.seenAt(settlement.seen() - Math.max(0, days) * DAY));
        grow(level);
        return true;
    }

    /** Builds what the days that passed allow in every settlement of {@code level}, into its loaded chunks. */
    public static void grow(final ServerLevel level) {
        for (final Map.Entry<String, Settlement> entry : of(level).entrySet()) {
            final Settlement settlement = entry.getValue();
            final Optional<Growth> growth = growthOf(level, settlement.structure());
            if (growth.isEmpty()) {
                continue;
            }
            final int standing = growth.get().standing((level.getGameTime() - settlement.seen()) / DAY);
            if (standing > settlement.standing()) {
                save(level, entry.getKey(), settlement.withStanding(standing));
                build(level, entry.getKey());
            }
        }
    }

    /**
     * Notes each growing settlement whose start lies in {@code chunk}, and catches the chunk up with the stages of the
     * settlements it is part of: what loading a chunk does, for a mod that loads chunks its own way.
     */
    public static void look(final ServerLevel level, final LevelChunk chunk) {
        final ChunkPos at = chunk.getPos();
        for (final Map.Entry<Structure, StructureStart> start : chunk.getAllStarts().entrySet()) {
            if (start.getKey() instanceof PlacedStructure placed && placed.growth().isPresent()
                    && start.getValue().isValid()) {
                note(level, start.getKey(), start.getValue(), placed.growth().get());
            }
        }
        for (final Map.Entry<String, Settlement> entry : of(level).entrySet()) {
            if (entry.getValue().box().intersects(at.getMinBlockX(), at.getMinBlockZ(), at.getMaxBlockX(),
                    at.getMaxBlockZ())) {
                build(level, entry.getKey());
            }
        }
    }

    /* Looks at a chunk as it loads, on a later turn of the server's loop, never inside the loading itself. */
    private static void onChunkLoad(final ChunkEvent.Load event) {
        if (event.getLevel() instanceof ServerLevel level && event.getChunk() instanceof LevelChunk chunk) {
            final MinecraftServer server = level.getServer();
            server.tell(new TickTask(server.getTickCount(), () -> {
                // A dimension made while the game runs may be gone by then.
                if (server.getLevel(level.dimension()) == level) {
                    look(level, chunk);
                }
            }));
        }
    }

    private static void note(final ServerLevel level, final Structure structure, final StructureStart start,
                             final Growth growth) {
        final ResourceLocation id = level.registryAccess().registryOrThrow(Registries.STRUCTURE).getKey(structure);
        if (id == null) {
            return;
        }
        final String key = id + "@" + start.getChunkPos().x + "," + start.getChunkPos().z;
        if (of(level).containsKey(key)) {
            return;
        }
        BoundingBox box = start.getBoundingBox();
        for (final StructurePiece piece : start.getPieces()) {
            if (piece instanceof SettlementPlan plan) {
                for (final SettlementPlan.Planned planned : plan.planned()) {
                    box = BoundingBox.encapsulatingBoxes(List.of(box, planned.piece().getBoundingBox())).orElse(box);
                }
            }
        }
        save(level, key, new Settlement(id, start.getChunkPos().x, start.getChunkPos().z, level.getGameTime(),
                growth.built(), box, Map.of()));
    }

    /* Builds every stage the settlement {@code key} allows into each of its loaded chunks that has not had it. */
    private static void build(final ServerLevel level, final String key) {
        final Settlement settlement = of(level).get(key);
        if (settlement == null) {
            return;
        }
        final Registry<Structure> structures = level.registryAccess().registryOrThrow(Registries.STRUCTURE);
        final Structure structure = structures.get(settlement.structure());
        if (structure == null) {
            return;
        }
        final ChunkAccess startChunk = level.getChunk(settlement.chunkX(), settlement.chunkZ(),
                ChunkStatus.STRUCTURE_STARTS, true);
        final StructureStart start = startChunk == null ? null : startChunk.getStartForStructure(structure);
        if (start == null || !start.isValid()) {
            return;
        }
        final Optional<SettlementPlan> plan = start.getPieces().stream().filter(SettlementPlan.class::isInstance)
                .map(SettlementPlan.class::cast).findFirst();
        if (plan.isEmpty()) {
            return;
        }
        final BoundingBox whole = start.getBoundingBox();
        final BlockPos pivot = new BlockPos(whole.getCenter().getX(), whole.minY(), whole.getCenter().getZ());
        final Map<String, Integer> built = new HashMap<>(settlement.built());
        for (final SettlementPlan.Planned planned : plan.get().planned()) {
            if (planned.stage() > settlement.standing()) {
                continue;
            }
            final BoundingBox box = planned.piece().getBoundingBox();
            for (int cx = box.minX() >> 4; cx <= box.maxX() >> 4; cx++) {
                for (int cz = box.minZ() >> 4; cz <= box.maxZ() >> 4; cz++) {
                    final String chunkKey = Long.toString(ChunkPos.asLong(cx, cz));
                    if (!level.hasChunk(cx, cz) || built.getOrDefault(chunkKey, 0) >= planned.stage()) {
                        continue;
                    }
                    final ChunkPos chunk = new ChunkPos(cx, cz);
                    final BoundingBox chunkBox = new BoundingBox(chunk.getMinBlockX(), level.getMinBuildHeight(),
                            chunk.getMinBlockZ(), chunk.getMaxBlockX(), level.getMaxBuildHeight() - 1,
                            chunk.getMaxBlockZ());
                    planned.piece().postProcess(level, level.structureManager(),
                            level.getChunkSource().getGenerator(), RandomSource.create(level.getSeed() ^ chunk
                                    .toLong()), chunkBox, chunk, pivot);
                    if (structure instanceof PlacedStructure placed) {
                        placed.foundation().ifPresent(block -> PlacedStructure.found(level, box, chunkBox, block));
                    }
                }
            }
        }
        // Each loaded chunk of the settlement now stands at the settlement's stage.
        for (int cx = settlement.box().minX() >> 4; cx <= settlement.box().maxX() >> 4; cx++) {
            for (int cz = settlement.box().minZ() >> 4; cz <= settlement.box().maxZ() >> 4; cz++) {
                if (level.hasChunk(cx, cz)) {
                    built.put(Long.toString(ChunkPos.asLong(cx, cz)), settlement.standing());
                }
            }
        }
        save(level, key, settlement.withBuilt(built));
    }

    private static Optional<Growth> growthOf(final ServerLevel level, final ResourceLocation id) {
        final Structure structure = level.registryAccess().registryOrThrow(Registries.STRUCTURE).get(id);
        return structure instanceof PlacedStructure placed ? placed.growth() : Optional.empty();
    }

    private static void save(final ServerLevel level, final String key, final Settlement settlement) {
        BOOK.update(level, book -> {
            final Map<String, Settlement> next = new HashMap<>(book.settlements());
            next.put(key, settlement);
            return new Book(next);
        });
    }

    /**
     * A settlement, as its dimension keeps it.
     *
     * @param structure its structure's id
     * @param chunkX    the chunk it started in
     * @param chunkZ    and along
     * @param seen      the game time it was first seen at
     * @param standing  how many stages of it stand
     * @param box       the box its whole layout lies in
     * @param built     how many stages each of its chunks has had built, by the chunk's packed place
     */
    public record Settlement(ResourceLocation structure, int chunkX, int chunkZ, long seen, int standing,
                             BoundingBox box, Map<String, Integer> built) {

        public static final Codec<Settlement> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                ResourceLocation.CODEC.fieldOf("structure").forGetter(Settlement::structure),
                Codec.INT.fieldOf("chunk_x").forGetter(Settlement::chunkX),
                Codec.INT.fieldOf("chunk_z").forGetter(Settlement::chunkZ),
                Codec.LONG.fieldOf("seen").forGetter(Settlement::seen),
                Codec.INT.fieldOf("standing").forGetter(Settlement::standing),
                BoundingBox.CODEC.fieldOf("box").forGetter(Settlement::box),
                Codec.unboundedMap(Codec.STRING, Codec.INT).fieldOf("built").forGetter(Settlement::built))
                .apply(instance, Settlement::new));

        public Settlement {
            built = Map.copyOf(built);
        }

        Settlement withStanding(final int stages) {
            return new Settlement(this.structure, this.chunkX, this.chunkZ, this.seen, stages, this.box, this.built);
        }

        Settlement seenAt(final long time) {
            return new Settlement(this.structure, this.chunkX, this.chunkZ, time, this.standing, this.box,
                    this.built);
        }

        Settlement withBuilt(final Map<String, Integer> chunks) {
            return new Settlement(this.structure, this.chunkX, this.chunkZ, this.seen, this.standing, this.box,
                    chunks);
        }
    }

    /**
     * The settlements of a dimension, by their keys.
     *
     * @param settlements each settlement
     */
    public record Book(Map<String, Settlement> settlements) {

        public static final Codec<Book> CODEC = Codec.unboundedMap(Codec.STRING, Settlement.CODEC)
                .xmap(Book::new, Book::settlements);
        static final Book EMPTY = new Book(Map.of());

        public Book {
            settlements = Map.copyOf(settlements);
        }
    }
}
