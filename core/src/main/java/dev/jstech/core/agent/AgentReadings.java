/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.agent;

import com.mojang.logging.LogUtils;
import dev.jstech.core.moving.IStructureBody;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import org.slf4j.Logger;

/**
 * What mods tell an agent of their things, beyond what the game shows of every block and entity: a machine working,
 * a computer and its job, a ship's flight. A mod declares its readings once, in its constructor, or its client's
 * setup for what only the player's game knows; the Core's MCP server adds what they tell where the agent meets the
 * thing, under the mod's id: the block or the entity the player looks at, the vehicle ridden, the moving structure
 * stood on. A reading that fails is left out, and said in the log.
 */
public final class AgentReadings {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final List<Declared<IBlockReading>> BLOCKS = new CopyOnWriteArrayList<>();
    private static final List<Declared<IEntityReading>> ENTITIES = new CopyOnWriteArrayList<>();
    private static final List<Declared<IStructureReading>> STRUCTURES = new CopyOnWriteArrayList<>();

    private AgentReadings() {
    }

    /** Declares what the mod {@code modid} tells an agent of blocks. */
    public static void block(final String modid, final IBlockReading reading) {
        BLOCKS.add(new Declared<>(modid, reading));
    }

    /** Declares what the mod {@code modid} tells an agent of entities. */
    public static void entity(final String modid, final IEntityReading reading) {
        ENTITIES.add(new Declared<>(modid, reading));
    }

    /** Declares what the mod {@code modid} tells an agent of moving structures. */
    public static void structure(final String modid, final IStructureReading reading) {
        STRUCTURES.add(new Declared<>(modid, reading));
    }

    /** What every mod tells of the block at {@code pos} of {@code level}, each mod's a part named by its id. */
    public static AgentReport ofBlock(final Level level, final BlockPos pos) {
        return of(BLOCKS, (reading, report) -> reading.read(level, pos, report));
    }

    /** What every mod tells of {@code entity}, each mod's a part named by its id. */
    public static AgentReport ofEntity(final Entity entity) {
        return of(ENTITIES, (reading, report) -> reading.read(entity, report));
    }

    /** What every mod tells of {@code structure}, each mod's a part named by its id. */
    public static AgentReport ofStructure(final IStructureBody structure) {
        return of(STRUCTURES, (reading, report) -> reading.read(structure, report));
    }

    /* Each declared reading asked, by {@code ask}, into its mod's part; a part left empty is left out. */
    private static <T> AgentReport of(final List<Declared<T>> declared, final BiConsumer<T, AgentReport> ask) {
        final AgentReport out = new AgentReport();
        for (final Declared<T> each : declared) {
            final AgentReport mine = new AgentReport();
            try {
                ask.accept(each.reading(), mine);
            } catch (final RuntimeException failed) {
                LOGGER.warn("A reading of {} for an agent failed", each.modid(), failed);
                continue;
            }
            if (!mine.isEmpty()) {
                out.part(each.modid()).merge(mine);
            }
        }
        return out;
    }

    /* A reading, and the mod that declared it. */
    private record Declared<T>(String modid, T reading) {

        Declared {
            Objects.requireNonNull(modid, "modid");
            Objects.requireNonNull(reading, "reading");
        }
    }
}
