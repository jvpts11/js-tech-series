/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest.core;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import dev.jstech.core.camera.CameraPath;
import dev.jstech.core.camera.CameraPathCodecs;
import dev.jstech.core.camera.CameraView;
import dev.jstech.core.client.camera.CameraPathsClient;
import dev.jstech.core.client.camera.CameraViewsClient;
import dev.jstech.core.client.studio.BenchState;
import dev.jstech.core.client.studio.CameraPathRecorder;
import dev.jstech.core.client.studio.CameraPathTexts;
import dev.jstech.core.client.studio.StudioClient;
import dev.jstech.core.client.studio.StudioScreen;
import dev.jstech.core.client.studio.StudioSheet;
import dev.jstech.core.client.studio.StudioTool;
import dev.jstech.core.client.studio.ThingKind;
import dev.jstech.core.client.studio.ToolTexts;
import dev.jstech.core.client.studio.UnificationTexts;
import dev.jstech.core.client.input.KeyActionsClient;
import dev.jstech.core.developer.DevSources;
import dev.jstech.core.input.CoreKeys;
import dev.jstech.core.text.GameText;
import dev.jstech.tests.TestItems;
import dev.jstech.tests.TestKitMachine;
import dev.jstech.tests.TestStructures;
import dev.jstech.tests.clienttest.ClientTest;
import dev.jstech.tests.clienttest.ClientTestContext;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/**
 * The studio: every mod's things by kind, a machine chosen on the bench in its states, what it is made of, each part
 * with its file and its state, and the checks across its parts; a camera path kept where the player stands, played as
 * a scene before it is saved, and written to its mod's file; and a structure caught made a multiblock.
 */
public final class StudioClientTests {

    private static final int WAIT = 60;
    private static final int SETTLE = 3;
    /* The studio reads every thing as it opens, and the ticks it held up then run with no frame between. */
    private static final int SHOT = 20;
    /* A machine of the test mod's, drawn from a states file and named in English. */
    private static final ResourceLocation MACHINE = TestKitMachine.BLOCK.getId();
    /* Where the player stands, and the machine in front of it. */
    private static final BlockPos STAND = new BlockPos(0, 2, 0);
    private static final BlockPos AT = new BlockPos(0, 2, -3);
    private static final String TIN_TAG = "c:ingots/tin";
    /* What the checks of a screen's slots against its menu say, in English. */
    private static final String SLOTS_BEYOND = "Its screen shows slots its menu does not have";
    private static final String SLOTS_NOWHERE = "Its screen leaves out slots of its menu";
    private static final String SLOTS_TWICE = "Its screen shows slots twice";
    /* The camera path the studio writes, and where the player flies for its second keyframe. */
    private static final ResourceLocation STUDIO_PATH = ResourceLocation.fromNamespaceAndPath("jstests",
            "studio_path");
    private static final BlockPos FLOWN_TO = new BlockPos(0, 5, 8);
    private static final BlockPos FLOWN_ON = new BlockPos(6, 4, 10);
    /*
     * The furnace made a multiblock: its corners, its front amid the face the player sees, and where its far corner
     * is seen from.
     */
    private static final BlockPos FURNACE_FROM = new BlockPos(-1, 2, -3);
    private static final BlockPos FURNACE_TO = new BlockPos(1, 3, -5);
    private static final BlockPos FURNACE_FRONT = new BlockPos(0, 2, -3);
    private static final BlockPos ABOVE = new BlockPos(1, 7, -5);

    private StudioClientTests() {
    }

    @ClientTest(timeoutTicks = 400)
    public static void studio_showsWhatAMachineIsMadeOfOnItsBench(final ClientTestContext ctx) {
        ctx.afterTest(() -> ctx.mc().setScreen(null));
        ctx.then(0, () -> ctx.mc().setScreen(StudioClient.openOn(StudioTool.THINGS)))
                .thenAwaitScreen(StudioScreen.class, WAIT)
                .then(SETTLE, () -> {
                    final StudioScreen studio = ctx.screen(StudioScreen.class);
                    ctx.assertTrue(studio.choose(MACHINE), "the test mod's machine is among the things");
                    ctx.assertTrue(studio.chosen().filter(thing -> thing.kind() == ThingKind.MACHINES).isPresent(),
                            "a block whose block entity is a machine's is under the machines");
                    final StudioSheet sheet = studio.sheet().orElseThrow();
                    final List<String> parts = sheet.parts().stream().map(StudioSheet.Part::name).toList();
                    ctx.assertTrue(parts.containsAll(List.of("Model", "Names", "Recipe", "Tags", "Drops", "Tests")),
                            "it is made of its model, names, recipe, tags, drops and tests: " + parts);
                    final StudioSheet.Part model = part(sheet, "Model");
                    ctx.assertTrue(model.detail().equals("blockstates/" + MACHINE.getPath() + ".json")
                                    && model.mark() == StudioSheet.Mark.FINE,
                            "its model is its states file, there: " + model);
                    final StudioSheet.Part names = part(sheet, "Names");
                    ctx.assertTrue(names.detail().contains("en_us") && names.mark() == StudioSheet.Mark.FINE,
                            "its name is in every language of its mod: " + names);
                    ctx.assertTrue(part(sheet, "Drops").detail().equals("itself"),
                            "it drops itself: " + part(sheet, "Drops"));
                    ctx.assertTrue(sheet.checks().stream().noneMatch(check -> check.mark()
                            == StudioSheet.Mark.ERROR), "nothing across its parts is wrong: " + sheet.checks());
                    ctx.assertTrue(studio.problems() >= sheet.checks().size(),
                            "the checks count every thing's problems");
                })
                .thenScreenshot(SHOT, "studio-working")
                .then(0, () -> ctx.screen(StudioScreen.class).showState(BenchState.JAMMED))
                .thenScreenshot(SHOT, "studio-jammed");
    }

    @ClientTest(timeoutTicks = 300)
    public static void studio_checksAScreensSlotsAgainstItsMenu(final ClientTestContext ctx) {
        ctx.afterTest(() -> ctx.mc().setScreen(null));
        ctx.then(0, () -> ctx.mc().setScreen(StudioClient.openOn(StudioTool.THINGS)))
                .thenAwaitScreen(StudioScreen.class, WAIT)
                .then(SETTLE, () -> {
                    final StudioScreen studio = ctx.screen(StudioScreen.class);
                    ctx.assertTrue(studio.choose(TestItems.SLOT_CHECK.getId()),
                            "the thing whose screen shows its slots wrongly is among the things");
                    final List<String> wrong = titles(studio.sheet().orElseThrow());
                    ctx.assertTrue(wrong.containsAll(List.of(SLOTS_BEYOND, SLOTS_NOWHERE, SLOTS_TWICE)),
                            "a slot the menu has not, one of the menu's left out and one twice are found: " + wrong);
                    ctx.assertTrue(studio.choose(ResourceLocation.fromNamespaceAndPath("jsexample", "logbook")),
                            "the example's logbook is among the things");
                    final List<String> fine = titles(studio.sheet().orElseThrow());
                    ctx.assertTrue(fine.stream().noneMatch(List.of(SLOTS_BEYOND, SLOTS_NOWHERE, SLOTS_TWICE)::contains),
                            "and a screen that shows its menu as it is has nothing found: " + fine);
                });
    }

    @ClientTest(timeoutTicks = 300)
    public static void studio_readsTheBlockLookedAtOnItsPage(final ClientTestContext ctx) {
        ctx.afterTest(() -> ctx.mc().setScreen(null));
        ctx.thenTeleport(0, STAND, Direction.NORTH)
                .thenServer(SETTLE, level -> level.setBlockAndUpdate(ctx.abs(AT), TestKitMachine.BLOCK.get()
                        .defaultBlockState()))
                .then(SETTLE, () -> ctx.lookAt(Vec3.atCenterOf(ctx.abs(AT))))
                // The game picks the block looked at each tick, and the studio takes it as it opens.
                .then(SETTLE, () -> ctx.mc().setScreen(StudioClient.openOn(StudioTool.BLOCK)))
                .thenAwaitScreen(StudioScreen.class, WAIT)
                .thenWaitUntil(() -> !ctx.screen(StudioScreen.class).lines().isEmpty(), WAIT,
                        "the server answers with the fields of the machine looked at")
                .then(0, () -> ctx.assertTrue(ctx.screen(StudioScreen.class).tool() == StudioTool.BLOCK,
                        "the studio opens on the tool asked"))
                .thenScreenshot(SHOT, "studio-block");
    }

    @ClientTest(timeoutTicks = 300)
    public static void studio_saysWhoMayDoWhatAtTheBlockLookedAt(final ClientTestContext ctx) {
        ctx.afterTest(() -> ctx.mc().setScreen(null));
        ctx.thenTeleport(0, STAND, Direction.NORTH)
                .thenServer(SETTLE, level -> level.setBlockAndUpdate(ctx.abs(AT), TestKitMachine.BLOCK.get()
                        .defaultBlockState()))
                .then(SETTLE, () -> ctx.lookAt(Vec3.atCenterOf(ctx.abs(AT))))
                .then(SETTLE, () -> ctx.mc().setScreen(StudioClient.openOn(StudioTool.ACCESS)))
                .thenAwaitScreen(StudioScreen.class, WAIT)
                .thenWaitUntil(() -> ctx.screen(StudioScreen.class).lines().size() == 9, WAIT,
                        "the server answers the eight actions for each one weighed")
                .then(0, () -> ctx.assertTrue(ctx.screen(StudioScreen.class).lines().get(1).startsWith("break yes"),
                        "the player may break where nothing protects the place"))
                .thenScreenshot(SHOT, "studio-access");
    }

    @ClientTest(timeoutTicks = 400)
    public static void studio_showsWhatUnificationChangesOnTheRecipesPage(final ClientTestContext ctx) {
        ctx.afterTest(() -> ctx.mc().setScreen(null));
        ctx.then(0, () -> ctx.mc().setScreen(StudioClient.openOn(StudioTool.RECIPES)))
                .thenAwaitScreen(StudioScreen.class, WAIT)
                .then(SETTLE, () -> ctx.assertTrue(studio(ctx).press(GameText.resolve(UnificationTexts.OPEN)),
                        "the recipe bench has the button to unification"))
                .thenWaitUntil(() -> studio(ctx).lines().stream().anyMatch(line -> line.startsWith(TIN_TAG + " ")),
                        WAIT, "the server answers each tag unified, the tin ingots among them")
                .then(0, () -> ctx.assertTrue(studio(ctx).lines().stream().filter(line -> line.startsWith(
                        TIN_TAG + " ")).allMatch(line -> !line.endsWith(" 0")),
                        "the tin ingots' tag changed the test mod's recipes: " + studio(ctx).lines()))
                .thenScreenshot(SHOT, "studio-unification")
                .then(0, () -> ctx.assertTrue(studio(ctx).press(GameText.resolve(UnificationTexts.BACK)),
                        "and back to the recipes"))
                .then(SETTLE, () -> ctx.assertTrue(studio(ctx).lines().stream().noneMatch(line -> line.startsWith(
                        TIN_TAG)), "the page is the recipe bench again"));
    }

    @ClientTest(timeoutTicks = 600)
    public static void studio_opensEveryToolOnItsOwnPage(final ClientTestContext ctx) {
        ctx.afterTest(() -> ctx.mc().setScreen(null));
        ctx.then(0, () -> ctx.mc().setScreen(StudioClient.openOn(StudioTool.THINGS)))
                .thenAwaitScreen(StudioScreen.class, WAIT);
        for (final StudioTool tool : StudioTool.inOrder()) {
            ctx.then(SETTLE, () -> ctx.screen(StudioScreen.class).open(tool))
                    .then(SETTLE, () -> ctx.assertTrue(ctx.screen(StudioScreen.class).tool() == tool,
                            "the tab of " + tool + " opens its page"))
                    .thenScreenshot(SETTLE, "studio-tool-" + tool.name().toLowerCase(Locale.ROOT));
        }
    }

    @ClientTest(timeoutTicks = 800)
    public static void cameras_keepsAPathFlownPlaysItAndWritesItsFile(final ClientTestContext ctx) {
        final Path[] written = new Path[1];
        ctx.afterTest(() -> {
            ctx.mc().setScreen(null);
            if (written[0] != null) {
                try {
                    Files.deleteIfExists(written[0]);
                } catch (final IOException kept) {
                    // Left where the test wrote it, for whoever runs the tests to see.
                }
            }
        });
        ctx.thenTeleport(0, STAND, Direction.SOUTH)
                .then(SETTLE, () -> ctx.mc().setScreen(StudioClient.openOn(StudioTool.CAMERAS)))
                .thenAwaitScreen(StudioScreen.class, WAIT)
                .then(SETTLE, () -> {
                    ctx.assertTrue(studio(ctx).choose(GameText.resolve(CameraPathTexts.A_NEW_PATH)),
                            "the page has a row for a new path");
                    studio(ctx).type(STUDIO_PATH.toString());
                    ctx.assertTrue(studio(ctx).press(GameText.resolve(CameraPathTexts.ADD_HERE)),
                            "a keyframe added where the player stands");
                })
                .thenTeleport(0, FLOWN_TO, Direction.NORTH)
                .then(SETTLE, () -> ctx.assertTrue(studio(ctx).press(GameText.resolve(CameraPathTexts.ADD_HERE)),
                        "a second keyframe added where the player flew"))
                .then(0, () -> ctx.assertTrue(studio(ctx).lines().size() == 2
                        && studio(ctx).lines().get(1).startsWith("2 s"), "two keyframes, the second two seconds on"))
                .thenScreenshot(SETTLE, "studio-cameras")
                .then(0, () -> ctx.assertTrue(studio(ctx).press(GameText.resolve(CameraPathTexts.FLY_AND_RECORD)),
                        "recording begun"))
                .thenWaitUntil(() -> ctx.mc().screen == null && CameraPathRecorder.recording(), WAIT,
                        "the studio shut while the path records")
                .thenTeleport(0, FLOWN_ON, Direction.WEST)
                .then(SETTLE, () -> KeyActionsClient.press(CoreKeys.KEEP_KEYFRAME))
                .thenAssert(SETTLE, () -> CameraPathRecorder.keyframes().size() == 3,
                        "the key keeps where the player flew as the third keyframe")
                .thenScreenshot(0, "camera-path-recording")
                .then(0, () -> ctx.mc().setScreen(StudioClient.openOn(StudioTool.CAMERAS)))
                .thenAwaitScreen(StudioScreen.class, WAIT)
                .then(SETTLE, () -> ctx.assertTrue(studio(ctx).press(GameText.resolve(
                        CameraPathTexts.STOP_RECORDING)) && !CameraPathRecorder.recording(), "recording stopped"))
                .then(0, () -> ctx.assertTrue(studio(ctx).press(GameText.resolve(CameraPathTexts.PLAY)),
                        "the path played"))
                .thenWaitUntil(() -> ctx.mc().screen == null && CameraViewsClient.showing()
                        .filter(CameraView.Scene.class::isInstance).isPresent(), WAIT, "the path playing as a scene")
                .thenWaitUntil(() -> CameraViewsClient.showing().isEmpty() && ctx.mc().getCameraEntity()
                        == ctx.player(), WAIT * 3, "the scene over, the view back with the player")
                .then(SETTLE, () -> ctx.mc().setScreen(StudioClient.openOn(StudioTool.CAMERAS)))
                .thenAwaitScreen(StudioScreen.class, WAIT)
                .then(SETTLE, () -> {
                    ctx.assertTrue(studio(ctx).lines().size() == 3, "the keyframes kept while the studio was shut");
                    written[0] = DevSources.resourcesOf(STUDIO_PATH.getNamespace()).orElseThrow()
                            .resolve("assets/jstests/jstech/camera_paths/studio_path.json");
                    ctx.assertTrue(studio(ctx).press(GameText.resolve(CameraPathTexts.SAVE_TO_FILE)),
                            "the path saved");
                })
                .then(SETTLE, () -> {
                    final CameraPath read = CameraPathCodecs.PATH.parse(JsonOps.INSTANCE, JsonParser.parseString(
                            readFile(written[0]))).getOrThrow();
                    ctx.assertTrue(read.keyframes().equals(CameraPathRecorder.keyframes()),
                            "the file says the keyframes kept");
                    ctx.assertTrue(read.keyframes().get(1).z() > read.keyframes().get(0).z(),
                            "the second keyframe south of the first, where the player flew");
                    ctx.assertTrue(CameraPathsClient.get(STUDIO_PATH).isPresent(),
                            "the game plays the path as its file says from now");
                });
    }

    @ClientTest(timeoutTicks = 600)
    public static void structures_makesACaughtStructureAMultiblock(final ClientTestContext ctx) {
        ctx.afterTest(() -> {
            if (ctx.mc().screen instanceof StudioScreen open) {
                open.press(GameText.resolve(ToolTexts.CATCH_ANOTHER));
            }
            ctx.mc().setScreen(null);
        });
        ctx.thenTeleport(0, STAND, Direction.NORTH)
                .thenServer(SETTLE, level -> {
                    for (final BlockPos at : BlockPos.betweenClosed(ctx.abs(FURNACE_FROM), ctx.abs(FURNACE_TO))) {
                        level.setBlockAndUpdate(at, Blocks.BRICKS.defaultBlockState());
                    }
                    level.setBlockAndUpdate(ctx.abs(FURNACE_FRONT), Blocks.BLAST_FURNACE.defaultBlockState());
                    level.setBlockAndUpdate(ctx.abs(FURNACE_FRONT.north()), Blocks.AIR.defaultBlockState());
                    level.setBlockAndUpdate(ctx.abs(FURNACE_FRONT.north().west()),
                            TestStructures.ITEM_INPUT.get().defaultBlockState());
                    level.setBlockAndUpdate(ctx.abs(FURNACE_FRONT.north().east()),
                            TestStructures.ITEM_OUTPUT.get().defaultBlockState());
                })
                .then(SETTLE, () -> ctx.lookAt(Vec3.atCenterOf(ctx.abs(FURNACE_FROM))))
                .then(SETTLE, () -> ctx.mc().setScreen(StudioClient.openOn(StudioTool.STRUCTURES)))
                .thenAwaitScreen(StudioScreen.class, WAIT)
                .then(SETTLE, () -> ctx.assertTrue(studio(ctx).press(GameText.resolve(ToolTexts.CORNER_1)),
                        "the first corner taken"))
                .then(0, () -> ctx.mc().setScreen(null))
                // The far corner is hidden behind the furnace from the front: it is taken from above.
                .thenTeleport(SETTLE, ABOVE, Direction.NORTH)
                .then(SETTLE, () -> ctx.lookAt(Vec3.atCenterOf(ctx.abs(FURNACE_TO))))
                .then(SETTLE, () -> ctx.mc().setScreen(StudioClient.openOn(StudioTool.STRUCTURES)))
                .thenAwaitScreen(StudioScreen.class, WAIT)
                .then(SETTLE, () -> ctx.assertTrue(studio(ctx).press(GameText.resolve(ToolTexts.CORNER_2)),
                        "the second corner taken"))
                .then(0, () -> ctx.mc().setScreen(null))
                .thenTeleport(SETTLE, STAND, Direction.NORTH)
                .then(SETTLE, () -> ctx.lookAt(Vec3.atCenterOf(ctx.abs(FURNACE_FRONT))))
                .then(SETTLE, () -> ctx.mc().setScreen(StudioClient.openOn(StudioTool.STRUCTURES)))
                .thenAwaitScreen(StudioScreen.class, WAIT)
                .then(SETTLE, () -> ctx.assertTrue(studio(ctx).press(GameText.resolve(ToolTexts.MAKE_MULTIBLOCK)),
                        "the furnace looked at is made its controller"))
                .thenWaitUntil(() -> studio(ctx).lines().stream().anyMatch(line -> line.contains(
                        "minecraft:bricks")), WAIT, "the server records the pattern")
                .then(0, () -> {
                    final List<String> letters = studio(ctx).lines();
                    ctx.assertTrue(letters.contains("B minecraft:bricks 14"), "fourteen bricks under B: " + letters);
                    ctx.assertTrue(letters.stream().anyMatch(line -> line.startsWith("I "))
                            && letters.stream().anyMatch(line -> line.startsWith("O ")),
                            "the ports under I and O: " + letters);
                })
                .thenScreenshot(SHOT, "studio-multiblock");
    }

    private static StudioScreen studio(final ClientTestContext ctx) {
        return ctx.screen(StudioScreen.class);
    }

    private static String readFile(final Path file) {
        try {
            return Files.readString(file, StandardCharsets.UTF_8);
        } catch (final IOException unread) {
            throw new IllegalStateException("the path's file was not written: " + file, unread);
        }
    }

    private static StudioSheet.Part part(final StudioSheet sheet, final String name) {
        return sheet.parts().stream().filter(part -> part.name().equals(name)).findFirst().orElseThrow();
    }

    /* The titles of the checks across its parts a sheet says. */
    private static List<String> titles(final StudioSheet sheet) {
        return sheet.checks().stream().map(StudioSheet.Check::title).toList();
    }
}
