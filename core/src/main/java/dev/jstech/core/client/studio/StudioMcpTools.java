/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.studio;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import dev.jstech.core.client.developer.ScreenFit;
import dev.jstech.core.client.gui.data.ScreenFiles;
import dev.jstech.core.developer.RunThingTestsPayload;
import dev.jstech.core.developer.mcp.McpCall;
import dev.jstech.core.developer.mcp.McpPermission;
import dev.jstech.core.developer.mcp.McpResult;
import dev.jstech.core.developer.mcp.McpSchema;
import dev.jstech.core.developer.mcp.McpTool;
import dev.jstech.core.developer.mcp.McpWords;
import dev.jstech.core.developer.studio.TranslationGaps;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * The studio's own tools on the player's game, as MCP tools: every thing of every mod with its problems, what a thing
 * is made of and the checks across its parts, what each language of a mod lacks and the texts too long for their
 * place, reloading everything, and running a thing's GameTests. They read what the studio reads, on the game's own
 * thread.
 */
public final class StudioMcpTools {

    private static final String ID = "id";
    private static final String MOD = "mod";
    private static final int MOST_THINGS = 2_000;

    private StudioMcpTools() {
    }

    /** The tools. */
    public static List<McpTool> all() {
        return List.of(
                new McpTool("things_list", "Every thing of every mod on J's Core and of every content pack, as the "
                        + "studio lists them: its id, mod, kind, name and how many problems its parts have.",
                        McpSchema.object().words(MOD, "Only this mod's things.", false)
                                .oneOf("kind", "Only things of this kind.", List.of("machines", "blocks", "items",
                                        "fluids", "entities", "parts", "research"), false)
                                .flag("problems_only", "Only things with problems.").build(), McpPermission.READ,
                        call -> onGame(() -> things(call))),
                new McpTool("thing_sheet", "What a thing is made of, each part the file it always was with its "
                        + "state (its model, screen, effects, sounds, names, recipes, manual page, tags, drops, "
                        + "settings and GameTests), and the checks across its parts, each with what mends it.",
                        McpSchema.object().words(ID, "The thing's id, such as mymod:kiln.", true).build(),
                        McpPermission.READ, call -> onGame(() -> sheet(call))),
                new McpTool("translations_read", "A mod's keys: how many, what each of its languages lacks of "
                        + "English, and the texts too long for their place in each screen declared in a file.",
                        McpSchema.object().words(MOD, "The mod's id.", true).build(), McpPermission.READ,
                        call -> onGame(() -> translations(call))),
                new McpTool("reload_all", "Reloads the resources, and the data when the world is the player's own, "
                        + "as the studio's Reload all does; answers once the resources are back.", McpSchema.object()
                        .build(), McpPermission.ACT, call -> reload()),
                new McpTool("thing_tests_run", "Runs near the player the GameTests whose names name a thing; the "
                        + "chat says how many passed.", McpSchema.object().words(ID, "The thing's id.", true).build(),
                        McpPermission.ACT, call -> onGame(() -> tests(call))));
    }

    private static CompletableFuture<McpResult> onGame(final Supplier<McpResult> read) {
        return Minecraft.getInstance().submit(read);
    }

    private static McpResult things(final McpCall call) {
        final StudioContext context = new StudioContext(Minecraft.getInstance().getResourceManager());
        final Optional<String> mod = call.text(MOD);
        final Optional<String> kind = call.text("kind");
        final boolean problems = call.flag("problems_only", false);
        final JsonArray out = new JsonArray();
        for (final StudioThing thing : StudioThings.all()) {
            if (out.size() >= MOST_THINGS || mod.isPresent() && !mod.get().equals(thing.modid())
                    || kind.isPresent() && !kind.get().equals(kindOf(thing.kind()))) {
                continue;
            }
            final StudioSheet read = StudioReaders.read(thing, context);
            if (problems && read.checks().isEmpty()) {
                continue;
            }
            final JsonObject one = new JsonObject();
            one.addProperty(ID, thing.id().toString());
            one.addProperty(MOD, thing.modid());
            one.addProperty("kind", kindOf(thing.kind()));
            one.addProperty("name", thing.name().getString());
            one.addProperty("problems", read.checks().size());
            out.add(one);
        }
        final JsonObject result = new JsonObject();
        result.add("things", out);
        return McpResult.json(result);
    }

    private static McpResult sheet(final McpCall call) {
        final Optional<ResourceLocation> id = call.text(ID).map(ResourceLocation::tryParse);
        final Optional<StudioThing> thing = id.flatMap(asked -> StudioThings.all().stream().filter(one -> one.id()
                .equals(asked)).findFirst());
        if (thing.isEmpty()) {
            return McpCall.wrong(ID, McpWords.AN_ID.text());
        }
        final StudioSheet read = StudioReaders.read(thing.get(), new StudioContext(Minecraft.getInstance()
                .getResourceManager()));
        final JsonArray parts = new JsonArray();
        for (final StudioSheet.Part part : read.parts()) {
            final JsonObject one = new JsonObject();
            one.addProperty("part", part.name());
            one.addProperty("detail", part.detail());
            one.addProperty("mark", markOf(part.mark()));
            part.file().ifPresent(file -> one.addProperty("file", file.modid() + ":" + file.path()));
            parts.add(one);
        }
        final JsonArray checks = new JsonArray();
        for (final StudioSheet.Check check : read.checks()) {
            final JsonObject one = new JsonObject();
            one.addProperty("mark", markOf(check.mark()));
            one.addProperty("problem", check.title());
            one.addProperty("detail", check.detail());
            check.file().ifPresent(file -> one.addProperty("file", file.modid() + ":" + file.path()));
            checks.add(one);
        }
        final JsonObject out = new JsonObject();
        out.addProperty(ID, thing.get().id().toString());
        out.add("parts", parts);
        out.add("checks", checks);
        return McpResult.json(out);
    }

    private static McpResult translations(final McpCall call) {
        final Optional<String> mod = call.text(MOD);
        if (mod.isEmpty()) {
            return McpCall.wrong(MOD, McpWords.AN_ID.text());
        }
        final StudioContext context = new StudioContext(Minecraft.getInstance().getResourceManager());
        final Map<String, Map<String, String>> texts = context.texts(mod.get());
        final Map<String, Set<String>> languages = new TreeMap<>();
        texts.forEach((language, words) -> languages.put(language, new HashSet<>(words.keySet())));
        final JsonObject lacks = new JsonObject();
        TranslationGaps.of(languages).forEach((language, missing) -> {
            final JsonArray keys = new JsonArray();
            missing.forEach(keys::add);
            lacks.add(language, keys);
        });
        final JsonArray tooLong = new JsonArray();
        for (final ResourceLocation screen : ScreenFiles.all().keySet()) {
            if (!screen.getNamespace().equals(mod.get())) {
                continue;
            }
            try {
                for (final ScreenFit.Overflow overflow : ScreenFit.of(screen, Minecraft.getInstance().font)) {
                    final JsonObject one = new JsonObject();
                    one.addProperty("screen", screen.toString());
                    one.addProperty("component", overflow.component());
                    one.addProperty("language", overflow.language());
                    one.addProperty("pixels_over", overflow.over());
                    tooLong.add(one);
                }
            } catch (final RuntimeException unbuilt) {
                // A screen that cannot be built alone, for want of its menu, is measured when it opens.
            }
        }
        final JsonObject out = new JsonObject();
        out.addProperty(MOD, mod.get());
        out.addProperty("keys", TranslationGaps.keys(languages).size());
        out.add("lacks", lacks);
        out.add("too_long", tooLong);
        return McpResult.json(out);
    }

    private static CompletableFuture<McpResult> reload() {
        final Minecraft game = Minecraft.getInstance();
        return game.submit(() -> {
            if (game.player != null && game.hasSingleplayerServer() && game.player.hasPermissions(2)) {
                game.player.connection.sendCommand("reload");
            }
            return game.reloadResourcePacks();
        }).thenCompose(reloaded -> reloaded).thenApply(done -> {
            final JsonObject out = new JsonObject();
            out.addProperty("reloaded", true);
            return McpResult.json(out);
        });
    }

    private static McpResult tests(final McpCall call) {
        final Optional<ResourceLocation> id = call.text(ID).map(ResourceLocation::tryParse);
        if (id.isEmpty() || Minecraft.getInstance().getConnection() == null) {
            return McpCall.wrong(ID, McpWords.AN_ID.text());
        }
        PacketDistributor.sendToServer(new RunThingTestsPayload(id.get()));
        final JsonObject out = new JsonObject();
        out.addProperty("asked", id.get().toString());
        return McpResult.json(out);
    }

    /* A kind as a word that travels: the last part of its title's key. */
    private static String kindOf(final ThingKind kind) {
        final String key = kind.title().key();
        return key.substring(key.lastIndexOf('.') + 1);
    }

    private static String markOf(final StudioSheet.Mark mark) {
        return switch (mark) {
            case NONE -> "none";
            case FINE -> "fine";
            case WARNING -> "warning";
            case ERROR -> "error";
        };
    }
}
