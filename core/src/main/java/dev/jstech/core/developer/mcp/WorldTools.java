/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.developer.mcp;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import dev.jstech.core.developer.studio.NbtLines;
import dev.jstech.core.developer.studio.StudioKeys;
import dev.jstech.core.developer.studio.StudioServices;
import dev.jstech.core.text.Text;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.StringTag;

/**
 * The studio's tools that read and change the world, as MCP tools: a block's fields and faces, a block saved and read
 * back, what machines cost, the packets sent, where tags and recipes came from, a loot table rolled, a structure
 * counted or saved. Both games have them: the player's asks the server as the studio does, a dedicated server asks
 * itself for the operator who let the client in; each answer is the studio's, as JSON.
 */
public final class WorldTools {

    private static final String PLACE = "place";
    private static final String FROM = "from";
    private static final String TO = "to";
    private static final int MOST_SECONDS = 30;
    private static final int MOST_ROLLS = 10_000;

    private WorldTools() {
    }

    /** The tools, asking through {@code ask}, a place left out being the block {@code looked} says. */
    public static List<McpTool> of(final IAsk ask, final ILookedAt looked) {
        return List.of(
                new McpTool("block_read", "Reads a block: its id, every field its block entity saves (what players "
                        + "wrote marked as theirs), what each face offers (energy, slots and what they hold, tanks), "
                        + "and, for a block entity of J's Core, the energy, items and fluid that crossed its faces "
                        + "since it loaded.", McpSchema.object().place(PLACE, "The block; left out, the block the "
                        + "player looks at.", false).build(), McpPermission.READ,
                        call -> placed(call, looked, pos -> ask.ask(call, StudioServices.BLOCK, at(pos))
                                .thenApply(answer -> answered(answer, WorldTools::block)))),
                new McpTool("block_save_round_trip", "Saves a block entity and reads it back as a world closed and "
                        + "opened again does, field by field, and says which fields came back changed.",
                        McpSchema.object().place(PLACE, "The block; left out, the block the player looks at.",
                                false).build(), McpPermission.READ,
                        call -> placed(call, looked, pos -> ask.ask(call, StudioServices.SAVE, at(pos))
                                .thenApply(answer -> answered(answer, Function.identity())))),
                new McpTool("cost_measure", "Times every machine for some seconds and answers what each costs the "
                        + "server in microseconds a tick, the dearest first, with where it is and who owns it.",
                        McpSchema.object().whole("seconds", "How long to measure.", 1, MOST_SECONDS, true).build(),
                        McpPermission.READ, call -> {
                            final Optional<Long> seconds = call.whole("seconds").filter(s -> s >= 1
                                    && s <= MOST_SECONDS);
                            if (seconds.isEmpty()) {
                                return done(McpCall.wrong("seconds", McpWords.A_NUMBER.with(1, MOST_SECONDS)));
                            }
                            final CompoundTag args = new CompoundTag();
                            args.putInt(StudioKeys.SECONDS, seconds.get().intValue());
                            return ask.ask(call, StudioServices.COST, args).thenApply(answer -> answered(answer,
                                    Function.identity()));
                        }),
                new McpTool("payloads_read", "The packets the server sent over the last hundred ticks, tick by "
                        + "tick: by kind, how many, how many bytes and the biggest one.", McpSchema.object()
                        .flag("everyone", "Everyone's packets rather than only this player's.").build(),
                        McpPermission.READ, call -> {
                            final CompoundTag args = new CompoundTag();
                            args.putBoolean(StudioKeys.EVERYONE, call.flag("everyone", false));
                            return ask.ask(call, StudioServices.PAYLOADS, args).thenApply(answer -> answered(answer,
                                    Function.identity()));
                        }),
                new McpTool("tag_sources", "Where each entry of a tag came from: every pack that wrote the tag's "
                        + "file, in order, with what it put in and whether it replaced what came before.",
                        McpSchema.object().oneOf("registry", "What the tag holds.", List.of("item", "block", "fluid",
                                "entity_type"), true).words("tag", "The tag's id, such as minecraft:planks.", true)
                                .build(), McpPermission.READ, call -> {
                            final Optional<String> tag = call.text("tag");
                            if (tag.isEmpty()) {
                                return done(McpCall.wrong("tag", McpWords.AN_ID.text()));
                            }
                            final CompoundTag args = new CompoundTag();
                            args.putString(StudioKeys.REGISTRY, call.text("registry").orElse("item"));
                            args.putString(StudioKeys.TAG, tag.get());
                            return ask.ask(call, StudioServices.SOURCES, args).thenApply(answer -> answered(answer,
                                    Function.identity()));
                        }),
                new McpTool("recipe_sources", "Which pack each recipe came from, and the recipes the packs took "
                        + "away, replaced or changed.", McpSchema.object().words("recipes", "The recipes' ids, "
                        + "parted by spaces or commas.", true).build(), McpPermission.READ, call -> {
                            final List<String> ids = List.of(call.text("recipes").orElse("").split("[,\\s]+"))
                                    .stream().filter(id -> !id.isBlank()).toList();
                            if (ids.isEmpty()) {
                                return done(McpCall.wrong("recipes", McpWords.AN_ID.text()));
                            }
                            final CompoundTag args = new CompoundTag();
                            final ListTag asked = new ListTag();
                            ids.forEach(id -> asked.add(StringTag.valueOf(id)));
                            args.put(StudioKeys.RECIPES, asked);
                            return ask.ask(call, StudioServices.SOURCES, args).thenApply(answer -> answered(answer,
                                    Function.identity()));
                        }),
                new McpTool("loot_roll", "Rolls a loot table many times with a tool and shows how the drops spread: "
                        + "for each item, in how many rolls it came, how many a roll, the fewest and the most.",
                        McpSchema.object().words("table", "The table's id, such as minecraft:blocks/gravel.", true)
                                .whole("rolls", "How many times to roll.", 1, MOST_ROLLS, false)
                                .words("tool", "The item held, such as minecraft:iron_pickaxe; the hand if left "
                                        + "out.", false)
                                .whole("fortune", "The level of Fortune on the tool.", 0, 3, false)
                                .flag("silk_touch", "Whether the tool has Silk Touch.").build(), McpPermission.READ,
                        call -> {
                            final Optional<String> table = call.text("table");
                            if (table.isEmpty()) {
                                return done(McpCall.wrong("table", McpWords.AN_ID.text()));
                            }
                            final CompoundTag args = new CompoundTag();
                            args.putString(StudioKeys.TABLE, table.get());
                            args.putInt(StudioKeys.ROLLS, (int) Math.max(1, Math.min(MOST_ROLLS,
                                    call.whole("rolls").orElse(1_000L))));
                            call.text("tool").ifPresent(tool -> args.putString(StudioKeys.TOOL, tool));
                            args.putInt(StudioKeys.FORTUNE, (int) Math.max(0, Math.min(3,
                                    call.whole("fortune").orElse(0L))));
                            args.putBoolean(StudioKeys.SILK, call.flag("silk_touch", false));
                            return ask.ask(call, StudioServices.LOOT, args).thenApply(answer -> answered(answer,
                                    Function.identity()));
                        }),
                new McpTool("structure_count", "Counts the blocks between two corners, both kept, as a structure "
                        + "file would hold them: its size, the blocks other than air and the block entities.",
                        corners(false), McpPermission.READ, call -> structure(call, ask, false)),
                new McpTool("structure_save", "Saves the blocks between two corners as a structure file of a mod, "
                        + "data/<mod>/structure/<path>.nbt, in the mod's source when this run has it.",
                        corners(true), McpPermission.ACT, call -> structure(call, ask, true)));
    }

    /** A place given as {@code {x, y, z}}. */
    public static Optional<BlockPos> placeOf(final JsonObject place) {
        try {
            return Optional.of(new BlockPos(place.get("x").getAsInt(), place.get("y").getAsInt(),
                    place.get("z").getAsInt()));
        } catch (final RuntimeException wrong) {
            return Optional.empty();
        }
    }

    /** The studio's answer as a tool's result: refused when the server refused it, else shaped as JSON. */
    public static McpResult answered(final CompoundTag answer,
                                     final Function<JsonObject, ? extends JsonElement> shape) {
        if (answer.contains(StudioServices.REFUSED)) {
            return McpResult.failed(Text.literal(answer.getString(StudioServices.REFUSED)));
        }
        final JsonElement json = NbtOps.INSTANCE.convertTo(JsonOps.INSTANCE, answer);
        return McpResult.json(json.isJsonObject() ? shape.apply(json.getAsJsonObject()) : json);
    }

    /** A result there at once. */
    public static CompletableFuture<McpResult> done(final McpResult result) {
        return CompletableFuture.completedFuture(result);
    }

    private static CompletableFuture<McpResult> placed(final McpCall call, final ILookedAt looked,
                                                       final Function<BlockPos, CompletableFuture<McpResult>> then) {
        final Optional<JsonObject> given = call.object(PLACE);
        if (given.isPresent()) {
            final Optional<BlockPos> pos = placeOf(given.get());
            return pos.isPresent() ? then.apply(pos.get()) : done(McpCall.wrong(PLACE, McpWords.A_PLACE.text()));
        }
        return looked.lookedAt(call).thenCompose(pos -> pos.isPresent() ? then.apply(pos.get())
                : done(McpCall.wrong(PLACE, McpWords.A_PLACE.text())));
    }

    private static CompletableFuture<McpResult> structure(final McpCall call, final IAsk ask, final boolean save) {
        final Optional<BlockPos> from = call.object(FROM).flatMap(WorldTools::placeOf);
        final Optional<BlockPos> to = call.object(TO).flatMap(WorldTools::placeOf);
        if (from.isEmpty() || to.isEmpty()) {
            return done(McpCall.wrong(from.isEmpty() ? FROM : TO, McpWords.A_PLACE.text()));
        }
        final CompoundTag args = new CompoundTag();
        args.putLong(StudioKeys.FROM, from.get().asLong());
        args.putLong(StudioKeys.TO, to.get().asLong());
        args.putBoolean(StudioKeys.SAVE, save);
        if (save) {
            final Optional<String> name = call.text("name");
            if (name.isEmpty()) {
                return done(McpCall.wrong("name", McpWords.AN_ID.text()));
            }
            args.putString(StudioKeys.NAME, name.get());
        }
        return ask.ask(call, StudioServices.STRUCTURE, args).thenApply(answer -> answered(answer,
                Function.identity()));
    }

    private static JsonObject corners(final boolean named) {
        final McpSchema schema = McpSchema.object().place(FROM, "One corner.", true).place(TO, "The other corner.",
                true);
        if (named) {
            schema.words("name", "The structure's id, such as mymod:kiln.", true);
        }
        return schema.build();
    }

    private static CompoundTag at(final BlockPos pos) {
        final CompoundTag args = new CompoundTag();
        args.putLong(StudioKeys.POS, pos.asLong());
        return args;
    }

    /* A block's answer with its fields by path, each a player could have written marked. */
    private static JsonElement block(final JsonObject answer) {
        if (answer.has(StudioKeys.FIELDS) && answer.get(StudioKeys.FIELDS).isJsonArray()) {
            final JsonObject fields = new JsonObject();
            for (final JsonElement line : answer.getAsJsonArray(StudioKeys.FIELDS)) {
                final JsonObject field = line.getAsJsonObject();
                final String path = field.get(NbtLines.PATH).getAsString();
                final String value = field.get(NbtLines.VALUE).getAsString();
                if (PlayerText.writtenAt(path)) {
                    fields.add(path, PlayerText.mark(value));
                } else {
                    fields.addProperty(path, value);
                }
            }
            answer.add(StudioKeys.FIELDS, fields);
        }
        return answer;
    }

    /** What asks the studio's services, as the game the server runs in does. */
    @FunctionalInterface
    public interface IAsk {

        CompletableFuture<CompoundTag> ask(McpCall call, String service, CompoundTag args);
    }

    /** What finds the block the player looks at, when a call names no place; none where there is no player. */
    @FunctionalInterface
    public interface ILookedAt {

        CompletableFuture<Optional<BlockPos>> lookedAt(McpCall call);
    }
}
