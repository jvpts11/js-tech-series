/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest.core;

import com.mojang.blaze3d.platform.NativeImage;
import dev.jstech.core.client.model.kit.EntityQueries;
import dev.jstech.core.client.model.kit.IKitLayer;
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
import java.util.function.IntPredicate;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.animal.MushroomCow;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/**
 * What the Core's models give entities ready-made, as a player sees it: an entity's own queries read by its model and
 * by the armour it wears, a creature of the game drawn with a model a resource pack names, a texture of four frames
 * that glows, a layer laid over one model or every model redrawing their faces as it chooses, and a tool held in first
 * person by the player's own arm.
 */
public final class ModelKitEntitiesClientTests {

    private static final BlockPos STAND = new BlockPos(0, 3, -3);
    private static final BlockPos AHEAD = new BlockPos(0, 2, 1);
    private static final int WAIT = 200;
    private static final ResourceLocation FLASHER = ResourceLocation.fromNamespaceAndPath(JsTests.MODID,
            "textures/kit/test_kit_flasher.png");
    private static final ResourceLocation MAGENTA = ResourceLocation.withDefaultNamespace(
            "textures/block/magenta_wool.png");
    private static final ResourceLocation YELLOW = ResourceLocation.withDefaultNamespace(
            "textures/block/yellow_wool.png");
    /* How much of the view's middle the walker's terracotta fills at least, and a mooshroom's red at most. */
    private static final double TERRACOTTA = 0.03;
    private static final double RED = 0.005;
    /* How much of the view's middle a layer's colour fills at least, and at most while it is not laid. */
    private static final double LAID = 0.03;
    private static final double UNLAID = 0.005;
    /* How much of the hand's corner of the view goes with the arm at least, and changes at most as nothing does. */
    private static final double ARM = 0.02;
    private static final double STILL = 0.002;
    /* How far apart two pixels are, red, green and blue together, to be told apart. */
    private static final int APART = 30;
    private static final float BODY = 10.0F;
    private static final float TURN = 40.0F;
    private static final int HURT = 6;
    private static final float SWING = 0.5F;
    private static final double EPSILON = 1.0e-4;

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

    @ClientTest(timeoutTicks = 200)
    public static void queries_readHowALivingEntitySwingsTurnsItsHeadAndIsHurt(final ClientTestContext ctx) {
        ctx.then(0, () -> {
            final MushroomCow cow = EntityType.MOOSHROOM.create(ctx.mc().level);
            cow.yBodyRotO = BODY;
            cow.yBodyRot = BODY;
            cow.yHeadRotO = BODY + TURN;
            cow.yHeadRot = BODY + TURN;
            cow.hurtTime = HURT;
            cow.walkAnimation.update(SWING, 1.0F);
            final ExpressionContext queries = new ExpressionContext();
            // A whole tick on, the entity stands as its last tick left it.
            EntityQueries.hand(queries, cow, 1.0F);
            final double amount = queries.readQuery("limb_swing_amount");
            final double head = queries.readQuery("head_y_rotation");
            final double hurt = queries.readQuery("hurt_time");
            ctx.assertTrue(Math.abs(amount - SWING) < EPSILON && Math.abs(head - TURN) < EPSILON
                    && Math.abs(hurt - (HURT - 1)) < EPSILON && queries.readQuery("max_health") == cow.getMaxHealth(),
                    "a living entity's model reads how hard it swings its legs (" + amount + "), how far its head "
                    + "turns from its body (" + head + ") and how long its hurt has left (" + hurt + ")");
        });
    }

    @ClientTest(timeoutTicks = 600)
    public static void helmet_readsItsWearersQueries(final ClientTestContext ctx) {
        ctx.afterTest(() -> ctx.server().execute(() -> clear(ctx, ctx.serverLevel())));
        ctx.then(0, () -> helmet().value("max_health", 0).value("is_alive", 0))
                .thenTeleport(0, STAND, Direction.SOUTH)
                .thenServer(0, level -> {
                    final ArmorStand stand = EntityType.ARMOR_STAND.create(level);
                    stand.moveTo(Vec3.atBottomCenterOf(ctx.abs(AHEAD)));
                    stand.setItemSlot(EquipmentSlot.HEAD, new ItemStack(TestModels.HELMET_ITEM.get()));
                    level.addFreshEntity(stand);
                })
                .then(4, () -> ctx.lookAt(Vec3.atCenterOf(ctx.abs(AHEAD))))
                .thenWaitUntil(() -> helmet().readQuery("is_alive") == 1.0, WAIT, "the helmet to be drawn on its "
                        + "wearer")
                .thenAssert(2, () -> helmet().readQuery("max_health") == stand(ctx).map(ArmorStand::getMaxHealth)
                        .orElse(-1.0F) && helmet().readQuery("distance_from_camera") > 1.0,
                        "the helmet's model reads its wearer's health and how far it is, as the wearer's own would");
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
                .then(5, () -> {
                    final double terracotta = share(ctx, ModelKitEntitiesClientTests::terracotta);
                    final double red = share(ctx, ModelKitEntitiesClientTests::mooshroomRed);
                    ctx.assertTrue(terracotta > TERRACOTTA && red < RED, "the mooshroom is seen as the walker's "
                            + "terracotta (" + terracotta + " of the view's middle), not a mooshroom's red (" + red
                            + ")");
                })
                .thenScreenshot(0, "kit-mooshroom");
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
        final IKitLayer counting = (instance, faces, buffers, light, overlay) -> laid[0]++;
        ctx.afterTest(() -> {
            KitLayers.remove(counting);
            ctx.server().execute(() -> clear(ctx, ctx.serverLevel()));
        });
        ctx.then(0, () -> KitLayers.add(TestModels.WALKER.id(), counting))
                .thenTeleport(0, STAND, Direction.SOUTH)
                .thenServer(0, level -> spawnWalker(ctx, level))
                .then(4, () -> ctx.lookAt(Vec3.atCenterOf(ctx.abs(AHEAD))))
                .thenWaitUntil(() -> laid[0] > 3, WAIT, "the layer to be laid as the walker is drawn");
    }

    @ClientTest(timeoutTicks = 600)
    public static void layers_redrawTheModelsFacesIntoWhatTheyChoose(final ClientTestContext ctx) {
        final IKitLayer everyModel = (instance, faces, buffers, light, overlay) -> faces.into(
                texture -> RenderType.entityCutoutNoCull(MAGENTA), light, overlay);
        final IKitLayer walkerAlone = (instance, faces, buffers, light, overlay) -> faces.into(
                texture -> RenderType.entityCutoutNoCull(YELLOW), light, overlay);
        ctx.afterTest(() -> {
            KitLayers.remove(everyModel);
            KitLayers.remove(walkerAlone);
            ctx.server().execute(() -> clear(ctx, ctx.serverLevel()));
        });
        ctx.thenTeleport(0, STAND, Direction.SOUTH)
                .thenServer(0, level -> spawnWalker(ctx, level))
                .then(4, () -> ctx.lookAt(Vec3.atCenterOf(ctx.abs(AHEAD))))
                .thenWaitUntil(() -> walker(ctx).isPresent(), WAIT, "the walker to be drawn")
                .then(2, () -> assertLaid(ctx, 0, 0, "unlaid, the walker is its own terracotta"))
                .then(0, () -> KitLayers.everywhere(everyModel))
                .then(2, () -> assertLaid(ctx, LAID, 0, "a layer over every model redraws the walker in magenta"))
                .thenScreenshot(0, "kit-layer-every-model")
                .then(0, () -> {
                    KitLayers.remove(everyModel);
                    KitLayers.add(TestModels.WALKER.id(), walkerAlone);
                })
                .then(2, () -> assertLaid(ctx, 0, LAID, "taken off, and one over the walker alone laid, it is "
                        + "redrawn in yellow"))
                .thenScreenshot(0, "kit-layer-walker");
    }

    @ClientTest(timeoutTicks = 600)
    public static void arms_holdTheToolInFirstPerson(final ClientTestContext ctx) {
        final NativeImage[] shown = new NativeImage[2];
        ctx.afterTest(() -> {
            hideArm(false);
            for (final NativeImage picture : shown) {
                if (picture != null) {
                    picture.close();
                }
            }
            ctx.server().execute(() -> ctx.serverPlayer().setItemInHand(InteractionHand.MAIN_HAND,
                    ItemStack.EMPTY));
        });
        ctx.thenAssert(0, () -> KitModels.get(TestModels.ARMS.id()).map(model -> model.data().bones().stream()
                        .map(ModelBone::name).anyMatch(KitFirstPerson.RIGHT_ARM::equals)).orElse(false),
                "the tool's model has a bone for the player's right arm")
                .thenTeleport(0, STAND, Direction.SOUTH)
                .thenServer(0, level -> ctx.serverPlayer().setItemInHand(InteractionHand.MAIN_HAND,
                        new ItemStack(TestModels.ARMS_ITEM.get())))
                .thenScreenshot(20, "kit-arms-first-person")
                .then(20, () -> shown[0] = hand(ctx))
                .then(2, () -> shown[1] = hand(ctx))
                .then(0, () -> hideArm(true))
                .then(2, () -> {
                    try (NativeImage bare = hand(ctx)) {
                        final double still = apart(shown[0], shown[1]);
                        final double gone = apart(shown[1], bare);
                        ctx.assertTrue(still < STILL && gone > ARM, "the player's arm is drawn holding the tool: "
                                + gone + " of the hand's corner goes with it, " + still + " changes as nothing does");
                    }
                })
                .then(0, () -> hideArm(false));
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

    private static Optional<ArmorStand> stand(final ClientTestContext ctx) {
        return ctx.mc().level.getEntitiesOfClass(ArmorStand.class, ctx.player().getBoundingBox().inflate(8))
                .stream().findFirst();
    }

    /* What the helmet's model reads: one instance for every helmet of its kind worn, as one is drawn at a time. */
    private static ExpressionContext helmet() {
        return KitAnimations.of(new ItemStack(TestModels.HELMET_ITEM.get()), ItemDisplayContext.NONE,
                KitModels.get(TestModels.HELMET.id()).orElseThrow()).animator().context();
    }

    /* Hides or shows the arm the tool's model draws in first person; the tool rests, so its pose keeps it. */
    private static void hideArm(final boolean hide) {
        KitModels.get(TestModels.ARMS.id()).ifPresent(model -> {
            final KitInstance instance = KitAnimations.of(new ItemStack(TestModels.ARMS_ITEM.get()),
                    ItemDisplayContext.FIRST_PERSON_RIGHT_HAND, model);
            instance.pose().setHidden(instance.pose().index(KitFirstPerson.RIGHT_ARM), hide);
        });
    }

    private static void assertLaid(final ClientTestContext ctx, final double magenta, final double yellow,
                                   final String what) {
        final double magentaSeen = share(ctx, ModelKitEntitiesClientTests::magenta);
        final double yellowSeen = share(ctx, ModelKitEntitiesClientTests::yellow);
        ctx.assertTrue(magenta > 0 ? magentaSeen > magenta : magentaSeen < UNLAID, what + ": magenta fills "
                + magentaSeen + " of the view's middle");
        ctx.assertTrue(yellow > 0 ? yellowSeen > yellow : yellowSeen < UNLAID, what + ": yellow fills " + yellowSeen
                + " of the view's middle");
    }

    /* How much of the middle of the frame, a third of it each way, is of a colour. */
    private static double share(final ClientTestContext ctx, final IntPredicate colour) {
        final int width = ctx.mc().getWindow().getGuiScaledWidth();
        final int height = ctx.mc().getWindow().getGuiScaledHeight();
        try (NativeImage view = ctx.capture(width / 3, height / 3, width / 3, height / 3)) {
            int count = 0;
            for (int y = 0; y < view.getHeight(); y++) {
                for (int x = 0; x < view.getWidth(); x++) {
                    if (colour.test(view.getPixelRGBA(x, y))) {
                        count++;
                    }
                }
            }
            return (double) count / (view.getWidth() * view.getHeight());
        }
    }

    /* The bottom right corner of the frame, where the main hand is drawn below the tool's flashing head. */
    private static NativeImage hand(final ClientTestContext ctx) {
        final int width = ctx.mc().getWindow().getGuiScaledWidth();
        final int height = ctx.mc().getWindow().getGuiScaledHeight();
        return ctx.capture(width * 3 / 4, height * 3 / 4, width / 4, height / 4);
    }

    /* How much of two pictures of one size is told apart. */
    private static double apart(final NativeImage one, final NativeImage other) {
        int count = 0;
        for (int y = 0; y < one.getHeight(); y++) {
            for (int x = 0; x < one.getWidth(); x++) {
                final int a = one.getPixelRGBA(x, y);
                final int b = other.getPixelRGBA(x, y);
                final int distance = Math.abs(red(a) - red(b)) + Math.abs(green(a) - green(b))
                        + Math.abs(blue(a) - blue(b));
                if (distance > APART) {
                    count++;
                }
            }
        }
        return (double) count / (one.getWidth() * one.getHeight());
    }

    /* Terracotta's orange brown, however lit: redder than green, greener than blue, and not a pure red. */
    private static boolean terracotta(final int pixel) {
        return red(pixel) > green(pixel) + 20 && green(pixel) > blue(pixel) + 8 && green(pixel) * 100 > red(pixel) * 45;
    }

    /* A mooshroom's red: far redder than green. */
    private static boolean mooshroomRed(final int pixel) {
        return red(pixel) > green(pixel) + 60 && green(pixel) * 100 < red(pixel) * 35;
    }

    private static boolean magenta(final int pixel) {
        return red(pixel) > green(pixel) + 40 && blue(pixel) > green(pixel) + 40;
    }

    private static boolean yellow(final int pixel) {
        return red(pixel) > blue(pixel) + 60 && green(pixel) > blue(pixel) + 60 && green(pixel) * 100 > red(pixel) * 65;
    }

    /* A pixel of the frame, as the game's pictures keep it: red lowest, then green, then blue. */
    private static int red(final int pixel) {
        return pixel & 0xFF;
    }

    private static int green(final int pixel) {
        return pixel >> 8 & 0xFF;
    }

    private static int blue(final int pixel) {
        return pixel >> 16 & 0xFF;
    }

    private static void clear(final ClientTestContext ctx, final ServerLevel level) {
        level.getEntitiesOfClass(TestModels.Walker.class, ctx.serverPlayer().getBoundingBox().inflate(16))
                .forEach(walker -> walker.discard());
        level.getEntitiesOfClass(MushroomCow.class, ctx.serverPlayer().getBoundingBox().inflate(16))
                .forEach(cow -> cow.discard());
        level.getEntitiesOfClass(ArmorStand.class, ctx.serverPlayer().getBoundingBox().inflate(16))
                .forEach(stand -> stand.discard());
    }
}
