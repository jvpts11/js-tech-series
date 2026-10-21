/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.mcp;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.platform.NativeImage;
import dev.jstech.core.client.developer.Pictures;
import dev.jstech.core.client.gui.data.BuiltNode;
import dev.jstech.core.client.gui.data.IDataScreen;
import dev.jstech.core.developer.ThingTests;
import dev.jstech.core.developer.mcp.McpCall;
import dev.jstech.core.developer.mcp.McpPermission;
import dev.jstech.core.developer.mcp.McpResult;
import dev.jstech.core.developer.mcp.McpSchema;
import dev.jstech.core.developer.mcp.McpTool;
import dev.jstech.core.developer.mcp.McpWords;
import dev.jstech.core.developer.mcp.WorldTools;
import dev.jstech.core.developer.studio.RecordedTest;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.phys.Vec3;
import net.neoforged.fml.ModList;
import net.neoforged.neoforgespi.language.IModFileInfo;

/**
 * The test world an agent sets up and checks: items given, blocks set, the world saved and opened again from its
 * files, the GameTests run by name, and a client test kept as a file played step by step in the world open, its
 * pictures compared with their references.
 */
public final class PlayWorld {

    private static final int MOST_GIVEN = 6_400;
    private static final int RELOAD_TICKS = 1_200;
    private static final int TEST_TICKS = 6_000;
    private static final int SETTLE = 3;
    private static final int SCREEN_WAIT = 60;
    private static final String REFERENCES = "clienttest/references/";

    private PlayWorld() {
    }

    /** The tools. */
    public static List<McpTool> all() {
        return List.of(
                new McpTool("world_give", "Gives the player an item, in a test world, as /give does.",
                        McpSchema.object().words("item", "The item's id.", true).whole("count", "How many.", 1,
                                MOST_GIVEN, false).build(), McpPermission.ACT, PlayWorld::give),
                new McpTool("world_set_block", "Sets a block, in a test world, as /setblock does.", McpSchema.object()
                        .place(Play.PLACE, "Where.", true).words("block", "The block's id, with its state if any, "
                                + "such as minecraft:furnace[facing=north].", true).build(), McpPermission.ACT,
                        PlayWorld::setBlock),
                new McpTool("world_save_and_reload", "Saves the player's own world, leaves it and opens it again from "
                        + "its files, and answers once the player is back in: what is checked afterwards went "
                        + "through the save.", McpSchema.object().build(), McpPermission.ACT, PlayWorld::reload),
                new McpTool("gametests_run", "Runs near the player the GameTests whose names hold some words, in the "
                        + "player's own world, and answers with whether each passed once the last ends.",
                        McpSchema.object().words("words", "What the tests' names hold, any case.", true).build(),
                        McpPermission.ACT, PlayWorld::gameTests),
                new McpTool("client_test_run", "Plays a client test kept as a file (as the studio's Record a test "
                        + "keeps them) in the world open, step by step, and answers with each picture compared with "
                        + "its reference.", McpSchema.object().words("mod", "The test's mod.", true).words("name",
                        "The test's name.", true).build(), McpPermission.ACT, PlayWorld::clientTest));
    }

    private static CompletableFuture<McpResult> give(final McpCall call) {
        final Optional<String> item = call.text("item");
        if (item.isEmpty() || ResourceLocation.tryParse(item.get()) == null) {
            return WorldTools.done(McpCall.wrong("item", McpWords.AN_ID.text()));
        }
        final long count = Math.max(1, Math.min(MOST_GIVEN, call.whole("count").orElse(1L)));
        return McpGame.command(call, "give @s " + item.get() + " " + count);
    }

    private static CompletableFuture<McpResult> setBlock(final McpCall call) {
        final Optional<BlockPos> pos = Play.place(call, Play.PLACE);
        final Optional<String> block = call.text("block");
        if (pos.isEmpty()) {
            return WorldTools.done(McpCall.wrong(Play.PLACE, McpWords.A_PLACE.text()));
        }
        if (block.isEmpty() || block.get().contains(" ")) {
            return WorldTools.done(McpCall.wrong("block", McpWords.AN_ID.text()));
        }
        return McpGame.command(call, "setblock " + pos.get().getX() + " " + pos.get().getY() + " "
                + pos.get().getZ() + " " + block.get());
    }

    /* Leaves the world and opens it again, as the test kit's own round trip through the save does. */
    private static CompletableFuture<McpResult> reload(final McpCall call) {
        final String[] world = new String[1];
        final int[] phase = new int[1];
        return PlayTasks.run(call, RELOAD_TICKS, (game, tick) -> {
            switch (phase[0]) {
                case 0 -> {
                    final IntegratedServer own = game.getSingleplayerServer();
                    if (own == null || game.level == null) {
                        return Optional.of(McpResult.failed(McpTexts.OWN_WORLD_ONLY.text()));
                    }
                    world[0] = own.getWorldPath(LevelResource.ROOT).toAbsolutePath().normalize().getFileName()
                            .toString();
                    game.level.disconnect();
                    game.disconnect(new TitleScreen());
                    phase[0] = 1;
                }
                case 1 -> {
                    if (game.level == null && game.getSingleplayerServer() == null) {
                        game.createWorldOpenFlows().openWorld(world[0], () -> game.setScreen(new TitleScreen()));
                        phase[0] = 2;
                    }
                }
                default -> {
                    final IntegratedServer own = game.getSingleplayerServer();
                    if (game.level != null && game.player != null && own != null && own.isReady()
                            && game.player.tickCount > 20) {
                        final JsonObject out = new JsonObject();
                        out.addProperty("reloaded", world[0]);
                        return Optional.of(McpResult.json(out));
                    }
                }
            }
            return Optional.empty();
        });
    }

    private static CompletableFuture<McpResult> gameTests(final McpCall call) {
        final String words = call.text("words").orElse("");
        final Minecraft game = Minecraft.getInstance();
        final CompletableFuture<McpResult> done = new CompletableFuture<>();
        game.execute(() -> {
            final IntegratedServer own = game.getSingleplayerServer();
            if (own == null || game.player == null) {
                done.complete(McpResult.failed(McpTexts.OWN_WORLD_ONLY.text()));
                return;
            }
            final UUID player = game.player.getUUID();
            own.execute(() -> {
                final ServerPlayer runner = own.getPlayerList().getPlayer(player);
                final int started = runner == null ? 0 : ThingTests.runNamed(runner, words, results -> {
                    final JsonArray tests = new JsonArray();
                    results.forEach((name, passed) -> {
                        final JsonObject one = new JsonObject();
                        one.addProperty("test", name);
                        one.addProperty("passed", passed);
                        tests.add(one);
                    });
                    final JsonObject out = new JsonObject();
                    out.addProperty("passed", results.values().stream().filter(Boolean::booleanValue).count());
                    out.addProperty("ran", results.size());
                    out.add("tests", tests);
                    done.complete(McpResult.json(out));
                });
                if (started == 0) {
                    done.complete(McpResult.failed(McpTexts.NO_TESTS.with(words)));
                }
            });
        });
        return done;
    }

    /* Plays a recorded test's steps as the player played them, each picture compared with its reference. */
    private static CompletableFuture<McpResult> clientTest(final McpCall call) {
        final Optional<String> mod = call.text("mod");
        final Optional<String> name = call.text("name");
        final Optional<RecordedTest> test = mod.flatMap(owner -> name.flatMap(asked -> read(owner, asked)));
        if (test.isEmpty()) {
            return WorldTools.done(McpResult.failed(McpTexts.NO_RECORDED_TEST.with(mod.orElse("") + ":"
                    + name.orElse(""))));
        }
        final List<RecordedTest.Step> steps = test.get().steps();
        final JsonArray pictures = new JsonArray();
        final int[] at = {0, 0};
        return PlayTasks.run(call, TEST_TICKS, (game, tick) -> {
            if (at[0] >= steps.size()) {
                final JsonObject out = new JsonObject();
                out.addProperty("steps", steps.size());
                out.add("pictures", pictures);
                out.addProperty("passed", pictures.asList().stream().allMatch(picture -> picture.getAsJsonObject()
                        .get("passed").getAsBoolean()));
                return Optional.of(McpResult.json(out));
            }
            if (++at[1] < SETTLE) {
                return Optional.empty();
            }
            final Optional<Boolean> stepped = step(game, steps.get(at[0]), at[1], test.get().mod(), pictures);
            if (stepped.isPresent()) {
                if (!stepped.get()) {
                    return Optional.of(McpResult.failed(McpWords.WENT_WRONG.with(steps.get(at[0]).toString())));
                }
                at[0]++;
                at[1] = 0;
            }
            return Optional.empty();
        });
    }

    /* One step, its {@code waited}th tick: whether it went, once it is done, none while it goes on. */
    private static Optional<Boolean> step(final Minecraft game, final RecordedTest.Step step, final int waited,
                                          final String mod, final JsonArray pictures) {
        final LocalPlayer player = game.player;
        if (player == null) {
            return Optional.of(false);
        }
        switch (step) {
            case RecordedTest.Use use -> {
                // The block is set two blocks ahead of the player, and used, as the test kit sets it in its area.
                final BlockPos ahead = BlockPos.containing(player.position().add(player.getLookAngle().multiply(1, 0,
                        1).normalize().scale(2)));
                if (waited == SETTLE) {
                    final IntegratedServer own = game.getSingleplayerServer();
                    final ResourceLocation id = ResourceLocation.tryParse(use.block());
                    if (own == null || id == null || !BuiltInRegistries.BLOCK.containsKey(id)) {
                        return Optional.of(false);
                    }
                    final Block block = BuiltInRegistries.BLOCK.get(id);
                    own.execute(() -> own.getLevel(player.level().dimension()).setBlockAndUpdate(ahead,
                            block.defaultBlockState()));
                    return Optional.empty();
                }
                if (waited == SETTLE + 4) {
                    Play.lookAt(player, Vec3.atCenterOf(ahead));
                    return Optional.empty();
                }
                if (waited == SETTLE + 5) {
                    KeyMapping.click(game.options.keyUse.getKey());
                    return Optional.empty();
                }
                return game.screen != null ? Optional.of(true) : waited > SCREEN_WAIT ? Optional.of(false)
                        : Optional.empty();
            }
            case RecordedTest.Click click -> {
                if (!(game.screen instanceof IDataScreen data)) {
                    return Optional.of(false);
                }
                final Optional<BuiltNode> node = data.find(click.component());
                if (node.isEmpty() || node.get().component() == null) {
                    return Optional.of(false);
                }
                final int[] center = node.get().component().center();
                game.screen.mouseClicked(center[0], center[1], 0);
                game.screen.mouseReleased(center[0], center[1], 0);
                return Optional.of(true);
            }
            case RecordedTest.ClickAt clickAt -> {
                final Screen screen = game.screen;
                if (screen == null) {
                    return Optional.of(false);
                }
                screen.mouseClicked(screen.width / 2.0 + clickAt.dx(), screen.height / 2.0 + clickAt.dy(), 0);
                screen.mouseReleased(screen.width / 2.0 + clickAt.dx(), screen.height / 2.0 + clickAt.dy(), 0);
                return Optional.of(true);
            }
            case RecordedTest.Type type -> {
                if (game.screen == null) {
                    return Optional.of(false);
                }
                for (final char c : type.text().toCharArray()) {
                    game.screen.charTyped(c, 0);
                }
                return Optional.of(true);
            }
            case RecordedTest.Key key -> {
                if (game.screen == null) {
                    return Optional.of(false);
                }
                game.screen.keyPressed(key.key(), 0, 0);
                return Optional.of(true);
            }
            case RecordedTest.Wait wait -> {
                return waited >= SETTLE + wait.ticks() ? Optional.of(true) : Optional.empty();
            }
            case RecordedTest.Picture picture -> {
                pictures.add(compare(game, mod, picture.name()));
                return Optional.of(true);
            }
            case RecordedTest.Close close -> {
                game.setScreen(null);
                return Optional.of(true);
            }
        }
    }

    /* The screen, or the menu's rectangle, compared with the reference picture of that name in the mod's files. */
    private static JsonObject compare(final Minecraft game, final String mod, final String name) {
        final JsonObject out = new JsonObject();
        out.addProperty("picture", name);
        final Screen screen = game.screen;
        final int[] area = screen instanceof AbstractContainerScreen<?> container ? new int[] {container.getGuiLeft(),
            container.getGuiTop(), container.getXSize(), container.getYSize()} : new int[] {0, 0,
                screen == null ? game.getWindow().getGuiScaledWidth() : screen.width,
                screen == null ? game.getWindow().getGuiScaledHeight() : screen.height};
        final IModFileInfo file = ModList.get().getModFileById(mod);
        final Path reference = file == null ? null : file.getFile().findResource(REFERENCES + name + ".png");
        try (NativeImage drawn = Pictures.capture(game, area[0], area[1], area[2], area[3])) {
            if (reference == null || !Files.isRegularFile(reference)) {
                out.addProperty("passed", false);
                out.addProperty("reference", "none");
                return out;
            }
            try (InputStream in = Files.newInputStream(reference); NativeImage expected = NativeImage.read(in)) {
                final boolean sized = drawn.getWidth() == expected.getWidth()
                        && drawn.getHeight() == expected.getHeight();
                final int off = sized ? Pictures.differing(drawn, expected, null) : drawn.getWidth()
                        * drawn.getHeight();
                out.addProperty("off", off);
                out.addProperty("most", Pictures.mostOff(drawn));
                out.addProperty("passed", sized && off <= Pictures.mostOff(drawn));
            }
        } catch (final IOException unread) {
            out.addProperty("passed", false);
            out.addProperty("reference", String.valueOf(unread.getMessage()));
        }
        return out;
    }

    private static Optional<RecordedTest> read(final String mod, final String name) {
        final ResourceLocation file = ResourceLocation.tryBuild(mod, RecordedTest.FOLDER + "/" + name + ".json");
        if (file == null) {
            return Optional.empty();
        }
        final Optional<Resource> found = Minecraft.getInstance().getResourceManager().getResource(file);
        if (found.isEmpty()) {
            return Optional.empty();
        }
        try (Reader reader = found.get().openAsReader()) {
            return Optional.of(RecordedTest.fromJson(JsonParser.parseReader(reader).getAsJsonObject()));
        } catch (final IOException | RuntimeException unread) {
            return Optional.empty();
        }
    }
}
