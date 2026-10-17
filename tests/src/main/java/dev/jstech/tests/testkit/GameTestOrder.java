/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.testkit;

import com.mojang.logging.LogUtils;
import dev.jstech.core.room.SealedRooms;
import dev.jstech.tests.JsTests;
import java.lang.reflect.Field;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.gametest.framework.GameTestBatch;
import net.minecraft.gametest.framework.GameTestServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import org.slf4j.Logger;

/**
 * How the battery's batches run: each from a world with no sealed room of the batches before it, and the benchmarks
 * after every other batch.
 *
 * <p>Every batch is built on the same places, so a room a test left behind, with whatever air it held, would close
 * round the next batch's test standing there and keep that air: a vacuum round a computer that needs air to cool.
 * The game test server also groups its tests into batches in the order of a hash map, so adding a batch anywhere
 * can move a benchmark to the front, where its first measurement pays for loading every class it touches and reads as
 * a regression; behind the rest, the game is warm, as it was when the baselines were taken.
 *
 * <p>The server makes its batches once its world is loaded and before it starts, so they are set in between, as it
 * starts.
 */
@EventBusSubscriber(modid = JsTests.MODID)
public final class GameTestOrder {

    private static final Logger LOGGER = LogUtils.getLogger();
    /* What every benchmark's batch is named with. */
    private static final String BENCH = "_bench_";
    /* Farther out than any place of the world. */
    private static final double WORLD = 3.0E7;

    private GameTestOrder() {
    }

    @SubscribeEvent
    public static void onStarting(final ServerStartingEvent event) {
        if (!(event.getServer() instanceof GameTestServer server)) {
            return;
        }
        try {
            final Field field = GameTestServer.class.getDeclaredField("testBatches");
            field.setAccessible(true);
            @SuppressWarnings("unchecked")
            final List<GameTestBatch> batches = (List<GameTestBatch>) field.get(server);
            batches.replaceAll(batch -> new GameTestBatch(batch.name(), batch.gameTestInfos(),
                    forgetRooms().andThen(batch.beforeBatchFunction()), batch.afterBatchFunction()));
            // A stable sort: the other batches keep their order, and so do the benchmarks among themselves.
            batches.sort(Comparator.comparing(batch -> batch.name().contains(BENCH)));
        } catch (final ReflectiveOperationException | RuntimeException e) {
            LOGGER.error("[JSC-GT] could not set the batches up; they run as the server made them", e);
        }
    }

    /* Forgets every sealed room of the level the batch is built in. */
    private static Consumer<ServerLevel> forgetRooms() {
        return level -> SealedRooms.forget(level, new AABB(-WORLD, level.getMinBuildHeight(), -WORLD, WORLD,
                level.getMaxBuildHeight(), WORLD));
    }
}
