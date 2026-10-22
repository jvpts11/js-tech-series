/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest.core;

import dev.jstech.core.client.model.kit.KitAnimations;
import dev.jstech.core.client.model.kit.KitEntityModels;
import dev.jstech.core.client.model.kit.KitFirstPerson;
import dev.jstech.core.client.model.kit.KitInstance;
import dev.jstech.core.client.model.kit.KitLayers;
import dev.jstech.core.client.model.kit.KitModels;
import dev.jstech.core.client.model.kit.KitTextures;
import dev.jstech.core.model.ExpressionContext;
import dev.jstech.core.model.ModelBone;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.TestModels;
import dev.jstech.tests.clienttest.ClientTest;
import dev.jstech.tests.clienttest.ClientTestContext;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.MushroomCow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/**
 * What the Core's models give entities ready-made, as a player sees it: an entity's own queries read by its model, a
 * creature of the game drawn with a model a resource pack names, a texture of four frames that glows, a layer laid
 * over a model, and a tool held in first person by the player's own arm.
 */
public final class ModelKitEntitiesClientTests {

    private static final BlockPos STAND = new BlockPos(0, 3, -3);
    private static final BlockPos AHEAD = new BlockPos(0, 2, 1);
    private static final int WAIT = 200;
    private static final ResourceLocation FLASHER = ResourceLocation.fromNamespaceAndPath(JsTests.MODID,
            "textures/kit/test_kit_flasher.png");

    private ModelKitEntitiesClientTests() {
    }

    @ClientTest(timeoutTicks = 600)
    public static void walker_readsItsEntitysQueriesReadyMade(final ClientTestContext ctx) {
        ctx.afterTest(() -> ctx.server().execute(() -> clear(ctx, ctx.serverLevel())));
        ctx.thenTeleport(0, STAND, Direction.SOUTH)
                .thenServer(0, level -> spawnWalker(ctx, level))
                .then(4, () -> ctx.lookAt(Vec3.atCenterOf(ctx.abs(AHEAD))))
                .thenWaitUntil(() -> walker(ctx).isPresent(), WAIT, "the walker to be drawn")
                .thenAssert(2, () -> {
                    final ExpressionContext queries = walker(ctx).orElseThrow().animator().context();
                    return queries.readQuery("distance_from_camera") > 1.0
                            && queries.readQuery("time_of_day") >= 0.0 && queries.readQuery("time_of_day") < 1.0
                            && queries.readQuery("is_alive") == 1.0;
                }, "the walker's model reads how far it is, the time of day and that it is alive");
    }

    @ClientTest(timeoutTicks = 600)
    public static void mooshroom_isDrawnWithTheModelAPackNames(final ClientTestContext ctx) {
        ctx.afterTest(() -> ctx.server().execute(() -> clear(ctx, ctx.serverLevel())));
        ctx.thenAssert(0, () -> KitEntityModels.of(EntityType.MOOSHROOM)
                        .filter(entry -> entry.model().equals(TestModels.WALKER.id())).isPresent(),
                "the tests mod's pack draws the mooshroom with the walker")
                .thenTeleport(0, STAND, Direction.SOUTH)
                .thenServer(0, level -> {
                    final MushroomCow cow = EntityType.MOOSHROOM.create(level);
                    cow.setNoAi(true);
                    cow.moveTo(Vec3.atBottomCenterOf(ctx.abs(AHEAD)));
                    level.addFreshEntity(cow);
                })
                .then(4, () -> ctx.lookAt(Vec3.atCenterOf(ctx.abs(AHEAD))))
                .thenWaitUntil(() -> mooshroom(ctx).map(instance -> instance.animator()
                        .controller("kit_entity_model").playing() != null).orElse(false), WAIT,
                        "the mooshroom to be drawn as the walker, walking")
                .thenScreenshot(5, "kit-mooshroom");
    }

    @ClientTest(timeoutTicks = 200)
    public static void flasher_isReadAsFourFramesThatGlow(final ClientTestContext ctx) {
        ctx.thenAssert(0, () -> KitTextures.of(FLASHER).frames().count() == 4,
                        "its .mcmeta makes the strip four frames")
                .thenAssert(0, () -> FLASHER.withPath("textures/kit/test_kit_flasher_e.png")
                        .equals(KitTextures.of(FLASHER).glowing()), "and the _e beside it glows over it")
                .thenAssert(0, () -> KitTextures.of(ResourceLocation.withDefaultNamespace(
                        "textures/block/stone.png")).frames().still(), "while stone neither moves nor glows");
    }

    @ClientTest(timeoutTicks = 600)
    public static void layer_isLaidOverTheModelEachTimeItIsDrawn(final ClientTestContext ctx) {
        final int[] laid = new int[1];
        ctx.afterTest(() -> ctx.server().execute(() -> clear(ctx, ctx.serverLevel())));
        ctx.then(0, () -> KitLayers.add(TestModels.WALKER.id(), (instance, faces, buffers, light, overlay) -> {
                    laid[0]++;
                }))
                .thenTeleport(0, STAND, Direction.SOUTH)
                .thenServer(0, level -> spawnWalker(ctx, level))
                .then(4, () -> ctx.lookAt(Vec3.atCenterOf(ctx.abs(AHEAD))))
                .thenWaitUntil(() -> laid[0] > 3, WAIT, "the layer to be laid as the walker is drawn");
    }

    @ClientTest(timeoutTicks = 600)
    public static void arms_holdTheToolInFirstPerson(final ClientTestContext ctx) {
        ctx.afterTest(() -> ctx.server().execute(() -> ctx.serverPlayer().setItemInHand(InteractionHand.MAIN_HAND,
                ItemStack.EMPTY)));
        ctx.thenAssert(0, () -> KitModels.get(TestModels.ARMS.id()).map(model -> model.data().bones().stream()
                        .map(ModelBone::name).anyMatch(KitFirstPerson.RIGHT_ARM::equals)).orElse(false),
                "the tool's model has a bone for the player's right arm")
                .thenTeleport(0, STAND, Direction.SOUTH)
                .thenServer(0, level -> ctx.serverPlayer().setItemInHand(InteractionHand.MAIN_HAND,
                        new ItemStack(TestModels.ARMS_ITEM.get())))
                .thenScreenshot(20, "kit-arms-first-person");
    }

    private static void spawnWalker(final ClientTestContext ctx, final ServerLevel level) {
        final TestModels.Walker walker = TestModels.WALKER_ENTITY.get().create(level);
        walker.moveTo(Vec3.atBottomCenterOf(ctx.abs(AHEAD)));
        level.addFreshEntity(walker);
    }

    private static Optional<KitInstance> walker(final ClientTestContext ctx) {
        return ctx.mc().level.getEntitiesOfClass(TestModels.Walker.class, ctx.player().getBoundingBox().inflate(8))
                .stream().map(KitAnimations::drawn).flatMap(Optional::stream).findFirst();
    }

    private static Optional<KitInstance> mooshroom(final ClientTestContext ctx) {
        return ctx.mc().level.getEntitiesOfClass(MushroomCow.class, ctx.player().getBoundingBox().inflate(8))
                .stream().map(KitAnimations::drawn).flatMap(Optional::stream).findFirst();
    }

    private static void clear(final ClientTestContext ctx, final ServerLevel level) {
        level.getEntitiesOfClass(TestModels.Walker.class, ctx.serverPlayer().getBoundingBox().inflate(16))
                .forEach(walker -> walker.discard());
        level.getEntitiesOfClass(MushroomCow.class, ctx.serverPlayer().getBoundingBox().inflate(16))
                .forEach(cow -> cow.discard());
    }
}
