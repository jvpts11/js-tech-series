/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest.core;

import dev.jstech.computers.ComputingModule;
import dev.jstech.core.client.developer.LiveReloadClient;
import dev.jstech.core.client.model.kit.KitAnimations;
import dev.jstech.core.client.model.kit.KitInstance;
import dev.jstech.core.client.model.kit.KitModel;
import dev.jstech.core.client.model.kit.KitModels;
import dev.jstech.core.client.model.kit.ModelPreviewScreen;
import dev.jstech.core.developer.LiveReload;
import dev.jstech.core.developer.ModFolders;
import dev.jstech.core.model.ModelAnimation;
import dev.jstech.core.model.ModelShaping;
import dev.jstech.core.model.network.ModelAnimations;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.TestModels;
import dev.jstech.tests.clienttest.ClientTest;
import dev.jstech.tests.clienttest.ClientTestContext;
import dev.jstech.tests.clienttest.TestModelRenderers;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;

/**
 * The Core's models as a player sees them: a press from a Blockbench project striking as the server tells it, its lamp
 * lit while powered, an ingot on its plate, the words its strike hands to code heard; J's Computers' Dock Station
 * drawn by the Core's models beside GeckoLib's own, to set the two side by side; a crate from an OBJ file in the hand,
 * a walker from an FBX file turning its head, a helmet from a Bedrock geometry on an armour stand; the preview of a
 * model, its animation played from its button; the models of 3MF, STL, STEP and USD files on showcases, the pump's
 * parts spinning, and its preview with its parts, its triangles and its model far away; and a model file changed,
 * read again by itself.
 */
public final class ModelKitClientTests {

    private static final BlockPos STAND = new BlockPos(0, 3, -3);
    private static final BlockPos PRESS = new BlockPos(-2, 2, 1);
    private static final BlockPos KIT_DOCK = new BlockPos(0, 2, 1);
    private static final BlockPos GECKO_DOCK = new BlockPos(1, 2, 1);
    /* The first of the four showcases, the others to its west. */
    private static final BlockPos SHOWCASE = new BlockPos(1, 2, 1);
    private static final int WAIT = 200;

    private ModelKitClientTests() {
    }

    @ClientTest(timeoutTicks = 800)
    public static void press_playsWhatTheServerTellsIt(final ClientTestContext ctx) {
        final int[] words = new int[1];
        ctx.thenTeleport(0, STAND, Direction.SOUTH)
                .thenServer(0, level -> {
                    clear(ctx, level);
                    level.setBlockAndUpdate(ctx.abs(PRESS), TestModels.PRESS_BLOCK.get().defaultBlockState()
                            .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH)
                            .setValue(BlockStateProperties.POWERED, true));
                    level.setBlockAndUpdate(ctx.abs(KIT_DOCK), TestModels.DOCK_BLOCK.get().defaultBlockState());
                    level.setBlockAndUpdate(ctx.abs(GECKO_DOCK), ComputingModule.DOCK_STATION.get()
                            .defaultBlockState());
                })
                .thenWaitUntil(() -> drawnPress(ctx).isPresent(), WAIT, "the press to be drawn")
                .then(0, () -> words[0] = TestModelRenderers.PRESS_WORDS.get())
                .thenServer(0, level -> ModelAnimations.play(level.getBlockEntity(ctx.abs(PRESS)), "main",
                        "strike"))
                .thenWaitUntil(() -> playing(ctx, "animation.test_kit_press.strike"), WAIT,
                        "the press to play the strike the server told it to")
                .thenWaitUntil(() -> TestModelRenderers.PRESS_WORDS.get() > words[0], WAIT,
                        "the strike to hand its word to code halfway through")
                .then(2, () -> ctx.lookAt(Vec3.atCenterOf(ctx.abs(new BlockPos(-1, 2, 1)))))
                .thenScreenshot(8, "kit-press-and-the-two-docks")
                .thenServer(0, level -> level.setBlockAndUpdate(ctx.abs(PRESS), level.getBlockState(ctx.abs(PRESS))
                        .setValue(BlockStateProperties.POWERED, false)))
                .thenScreenshot(6, "kit-press-unpowered-lamp-hidden")
                .thenServer(0, level -> clear(ctx, level));
    }

    @ClientTest(timeoutTicks = 600)
    public static void itemEntityAndArmour_drawWithTheKit(final ClientTestContext ctx) {
        ctx.thenTeleport(0, STAND, Direction.SOUTH)
                .thenServer(0, level -> {
                    clear(ctx, level);
                    ctx.serverPlayer().setItemInHand(InteractionHand.MAIN_HAND,
                            new ItemStack(TestModels.CRATE_ITEM.get()));
                    final ArmorStand stand = EntityType.ARMOR_STAND.create(level);
                    stand.moveTo(Vec3.atBottomCenterOf(ctx.abs(new BlockPos(1, 2, 1))));
                    stand.setItemSlot(EquipmentSlot.HEAD, new ItemStack(TestModels.HELMET_ITEM.get()));
                    level.addFreshEntity(stand);
                    final TestModels.Walker walker = TestModels.WALKER_ENTITY.get().create(level);
                    walker.moveTo(Vec3.atBottomCenterOf(ctx.abs(new BlockPos(-1, 2, 1))));
                    level.addFreshEntity(walker);
                })
                .then(4, () -> ctx.lookAt(Vec3.atCenterOf(ctx.abs(new BlockPos(0, 2, 1)))))
                .thenWaitUntil(() -> ctx.mc().level.getEntitiesOfClass(TestModels.Walker.class,
                        ctx.player().getBoundingBox().inflate(8)).stream().anyMatch(walker -> KitAnimations
                        .drawn(walker).isPresent()), WAIT, "the walker to be drawn")
                .thenScreenshot(10, "kit-crate-walker-and-helmet")
                .thenServer(0, level -> {
                    ctx.serverPlayer().setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
                    clear(ctx, level);
                });
    }

    @ClientTest(timeoutTicks = 600)
    public static void preview_playsTheModelsAnimationsFromTheirButtons(final ClientTestContext ctx) {
        ctx.thenServer(0, level -> ctx.server().getCommands().performPrefixedCommand(
                        ctx.serverPlayer().createCommandSourceStack(), "jstech model preview "
                                + TestModels.PRESS.id()))
                .thenAwaitScreen(ModelPreviewScreen.class, WAIT)
                .thenScreenshot(4, "kit-preview-at-rest")
                .then(0, () -> {
                    final ModelPreviewScreen screen = ctx.screen(ModelPreviewScreen.class);
                    final Optional<Button> strike = screen.children().stream()
                            .filter(Button.class::isInstance).map(Button.class::cast)
                            .filter(button -> button.getMessage().getString().contains("strike")).findFirst();
                    strike.orElseThrow(() -> new IllegalStateException("the preview has no button for the strike"))
                            .onPress();
                })
                .thenScreenshot(12, "kit-preview-striking")
                .then(0, () -> ctx.mc().setScreen(null))
                .thenAwaitNoScreen(WAIT);
    }

    @ClientTest(timeoutTicks = 900)
    public static void cadModels_drawNearFarAndInThePreview(final ClientTestContext ctx) {
        ctx.thenTeleport(0, STAND, Direction.SOUTH)
                .thenServer(0, level -> {
                    clear(ctx, level);
                    for (int i = 0; i < TestModels.SHOWN.size(); i++) {
                        level.setBlockAndUpdate(ctx.abs(SHOWCASE.west(i)), TestModels.SHOWCASE_BLOCK.get()
                                .defaultBlockState().setValue(TestModels.ShowcaseBlock.MODEL, i));
                    }
                })
                .thenWaitUntil(() -> showcases(ctx).stream().allMatch(Optional::isPresent), WAIT,
                        "the four models of CAD and scene files to be drawn")
                .then(2, () -> ctx.lookAt(Vec3.atCenterOf(ctx.abs(SHOWCASE.west(1))).add(-0.5, -0.2, 0)))
                .thenAssert(0, () -> showcases(ctx).getFirst().map(pump -> pump.animator().controller("main")
                        .playing() != null).orElse(false), "the pump plays its spin")
                .thenScreenshot(12, "kit-cad-models-near")
                .thenAssert(0, () -> {
                    final KitModel pump = KitModels.get(TestModels.PUMP.id()).orElseThrow();
                    return pump.far() != null && pump.at(8) == pump && pump.at(40) == pump.far()
                            && ModelShaping.triangles(pump.far().data()) <= 300
                            && ModelShaping.triangles(pump.data()) <= 800;
                }, "the pump is drawn with 800 triangles at most near, and with its model of 300 past 32 blocks")
                .thenServer(0, level -> ctx.server().getCommands().performPrefixedCommand(
                        ctx.serverPlayer().createCommandSourceStack(), "jstech model preview "
                                + TestModels.PUMP.id()))
                .thenAwaitScreen(ModelPreviewScreen.class, WAIT)
                .then(0, () -> flip(ctx, "Wire"))
                .thenScreenshot(6, "kit-cad-preview-pump")
                .then(0, () -> flip(ctx, "Far detail"))
                .thenScreenshot(4, "kit-cad-preview-pump-far")
                .then(0, () -> ctx.mc().setScreen(null))
                .thenAwaitNoScreen(WAIT)
                .thenServer(0, level -> clear(ctx, level));
    }

    /* The probe is named for this run, so the other clients of a battery pay it no mind. */
    @ClientTest(timeoutTicks = 1200)
    public static void changedModelFiles_areReadAgainByThemselves(final ClientTestContext ctx) {
        final String file = LiveReload.ownFile("live_probe", "model.json");
        final Path probe = resourceRoot().resolve("assets").resolve(JsTests.MODID).resolve("jsmodels").resolve(file);
        final ResourceLocation id = ResourceLocation.fromNamespaceAndPath(JsTests.MODID,
                file.substring(0, file.length() - ".model.json".length()));
        final int[] before = new int[2];
        ctx.afterTest(() -> delete(probe));
        ctx.then(0, () -> {
                    before[0] = LiveReloadClient.modelReloads();
                    before[1] = LiveReloadClient.reloads();
                    write(probe, "{\"model\": \"" + TestModels.CRATE.definition().model() + "\"}");
                })
                .thenWaitUntil(() -> LiveReloadClient.modelReloads() > before[0], 400,
                        "the models to be read again for the new definition")
                .thenAssert(0, () -> KitModels.get(id).isPresent(), "the new model is read")
                .thenAssert(0, () -> LiveReloadClient.reloads() == before[1],
                        "and nothing else of the resources was reloaded for it")
                .then(0, () -> delete(probe))
                .thenWaitUntil(() -> LiveReloadClient.modelReloads() > before[0] + 1, 400,
                        "the models to be read again once the definition is gone")
                .thenAssert(0, () -> KitModels.get(id).isEmpty(), "and the model is gone with it");
    }

    private static Optional<KitInstance> drawnPress(final ClientTestContext ctx) {
        if (ctx.mc().level == null || ctx.mc().level.getBlockEntity(ctx.abs(PRESS)) == null) {
            return Optional.empty();
        }
        return KitAnimations.drawn(ctx.mc().level.getBlockEntity(ctx.abs(PRESS)));
    }

    /* Flips the preview's switch called {@code name}. */
    private static void flip(final ClientTestContext ctx, final String name) {
        ctx.screen(ModelPreviewScreen.class).children().stream().filter(Checkbox.class::isInstance)
                .map(Checkbox.class::cast).filter(box -> box.getMessage().getString().equals(name)).findFirst()
                .orElseThrow(() -> new IllegalStateException("the preview has no switch " + name)).onPress();
    }

    /* What draws each of the showcases, in the order of the models they show. */
    private static List<Optional<KitInstance>> showcases(final ClientTestContext ctx) {
        final List<Optional<KitInstance>> out = new ArrayList<>();
        for (int i = 0; i < TestModels.SHOWN.size(); i++) {
            final BlockEntity showcase = ctx.mc().level == null ? null : ctx.mc().level.getBlockEntity(ctx.abs(
                    SHOWCASE.west(i)));
            out.add(showcase == null ? Optional.empty() : KitAnimations.drawn(showcase));
        }
        return out;
    }

    private static boolean playing(final ClientTestContext ctx, final String animation) {
        return drawnPress(ctx).map(instance -> {
            final ModelAnimation playing = instance.animator().controller("main").playing();
            return playing != null && playing.name().equals(animation);
        }).orElse(false);
    }

    /* Everything the tests place, back to air, and the entities they put down gone. */
    private static void clear(final ClientTestContext ctx, final ServerLevel level) {
        for (int x = -3; x <= 2; x++) {
            for (int y = 2; y <= 4; y++) {
                for (int z = 0; z <= 2; z++) {
                    level.setBlockAndUpdate(ctx.abs(new BlockPos(x, y, z)), Blocks.AIR.defaultBlockState());
                }
            }
        }
        level.getEntitiesOfClass(ArmorStand.class, ctx.serverPlayer().getBoundingBox().inflate(16))
                .forEach(stand -> stand.discard());
        level.getEntitiesOfClass(TestModels.Walker.class, ctx.serverPlayer().getBoundingBox().inflate(16))
                .forEach(walker -> walker.discard());
    }

    /** The tests mod's folder of resources in this run: the one of its folders holding its assets. */
    private static Path resourceRoot() {
        final List<Path> folders = ModFolders.ofThisRun().getOrDefault(JsTests.MODID, List.of());
        for (final Path folder : folders) {
            if (Files.isDirectory(folder.resolve("assets"))) {
                return folder;
            }
        }
        throw new IllegalStateException("this run loads the tests mod from no folder of assets: " + folders);
    }

    private static void write(final Path file, final String text) {
        try {
            Files.createDirectories(file.getParent());
            Files.writeString(file, text);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static void delete(final Path file) {
        try {
            Files.deleteIfExists(file);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
