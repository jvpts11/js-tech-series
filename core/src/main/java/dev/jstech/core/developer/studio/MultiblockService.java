/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.developer.studio;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import dev.jstech.core.developer.DevSources;
import dev.jstech.core.log.CoreLogs;
import dev.jstech.core.multiblock.MultiblockPatterns;
import dev.jstech.core.multiblock.MultiblockRecorder;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.storage.LevelResource;
import org.slf4j.Logger;

/**
 * Recording a multiblock: the blocks between two corners read as a pattern, the block named its terminal, each kind
 * of block a letter, ports their kind. Asked to write it, the pattern's file goes to the mod's own source in this run,
 * {@code data/<mod>/multiblock/<name>.json}, or, with no source here, into a data pack of the world's own, in force
 * once the world's data is reloaded.
 */
@TextHolder
final class MultiblockService implements IStudioService {

    static final String NAME = "multiblock";
    private static final Logger LOGGER = CoreLogs.DEVELOPER.logger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final String JSON = ".json";
    /* The folder of {@code data/<mod>/} patterns are read from. */
    private static final String FOLDER = "multiblock";
    /* The data pack of a world's own the patterns go to when this run has no source of their mod. */
    private static final String PACK = "jstech_studio";
    private static final String PACK_FILE = "pack.mcmeta";
    /* The data packs of this version of the game. */
    private static final int PACK_FORMAT = 48;
    private static final TextKey NOT_A_NAME = TextKey.of("gui.jscore.studio.structures.not_a_name",
            "%s is not a name a pattern can have");
    private static final TextKey TERMINAL_OUTSIDE = TextKey.of("gui.jscore.studio.structures.terminal_outside",
            "The terminal must be a block between the corners");
    private static final TextKey TOO_MANY_KINDS = TextKey.of("gui.jscore.studio.structures.too_many_kinds",
            "The structure holds more kinds of block than a pattern has letters");
    private static final TextKey TOO_BIG = TextKey.of("gui.jscore.studio.structures.too_big",
            "A pattern is recorded of at most %s blocks; choose nearer corners");
    private static final TextKey TOO_BIG_TO_SHOW = TextKey.of("gui.jscore.studio.structures.too_big_to_show",
            "The pattern is too big to show here; save it to read it in its file");
    /* The most blocks a pattern is recorded of, and the most of its words sent to be shown, under a word's limit. */
    private static final long MOST_BLOCKS = 32_768L;
    private static final int MOST_SENT_BYTES = 60_000;
    private static final TextKey PACK_DESCRIPTION = TextKey.of("gui.jscore.studio.structures.pack",
            "The multiblocks written by J's Core's studio");

    @Override
    public void answer(final ServerPlayer player, final CompoundTag args, final Consumer<CompoundTag> reply) {
        final ServerLevel level = player.serverLevel();
        final ResourceLocation name = ResourceLocation.tryParse(args.getString(StudioKeys.NAME).trim());
        if (name == null) {
            reply.accept(refused(GameText.resolve(NOT_A_NAME.with(args.getString(StudioKeys.NAME)))));
            return;
        }
        final BlockPos from = BlockPos.of(args.getLong(StudioKeys.FROM));
        final BlockPos to = BlockPos.of(args.getLong(StudioKeys.TO));
        final BlockPos terminal = BlockPos.of(args.getLong(StudioKeys.TERMINAL));
        // Each way the recording can be refused says its own reason: the box, the terminal, the kinds of block.
        final BoundingBox box = BoundingBox.fromCorners(from, to);
        if ((long) box.getXSpan() * box.getYSpan() * box.getZSpan() > MOST_BLOCKS) {
            reply.accept(refused(GameText.resolve(TOO_BIG.with(MOST_BLOCKS))));
            return;
        }
        StudioServices.requireLoaded(level, from, to);
        if (!box.isInside(terminal) || level.getBlockState(terminal).isAir()) {
            reply.accept(refused(GameText.resolve(TERMINAL_OUTSIDE)));
            return;
        }
        final MultiblockRecorder.Recording recording;
        try {
            recording = MultiblockRecorder.record(level, from, to, terminal, name.toString(),
                    args.getBoolean(StudioKeys.MIRRORS), args.getBoolean(StudioKeys.LIES_DOWN));
        } catch (final IllegalArgumentException wrong) {
            reply.accept(refused(GameText.resolve(TOO_MANY_KINDS)));
            return;
        }
        final JsonElement json = MultiblockPatterns.CODEC.encodeStart(JsonOps.INSTANCE, recording.pattern())
                .getOrThrow();
        final String words = json.toString();
        // A word sent whole has a most it can be; past it the pattern is only written, never shown.
        final boolean shown = words.getBytes(StandardCharsets.UTF_8).length <= MOST_SENT_BYTES;
        if (!shown && !args.getBoolean(StudioKeys.SAVE)) {
            reply.accept(refused(GameText.resolve(TOO_BIG_TO_SHOW)));
            return;
        }
        final CompoundTag out = new CompoundTag();
        out.putIntArray(StudioKeys.SIZE, new int[] {recording.sizeX(), recording.sizeY(), recording.sizeZ()});
        out.putInt(StudioKeys.BLOCKS, recording.blocks());
        if (shown) {
            out.putString(StudioKeys.PATTERN, words);
        }
        final ListTag letters = new ListTag();
        for (final MultiblockRecorder.Letter letter : recording.letters()) {
            final CompoundTag each = new CompoundTag();
            each.putString(StudioKeys.LETTER, String.valueOf(letter.letter()));
            each.putString(StudioKeys.BLOCK, letter.block());
            letter.port().ifPresent(port -> each.putString(StudioKeys.PORT, port.serializedName()));
            each.putInt(StudioKeys.COUNT, letter.count());
            letters.add(each);
        }
        out.put(StudioKeys.LETTERS, letters);
        if (args.getBoolean(StudioKeys.SAVE)) {
            // Written to the world's own pack, it is in force only once the world's data is reloaded.
            final boolean inPack = DevSources.resourcesOf(name.getNamespace()).isEmpty();
            write(level, name, GSON.toJson(json)).ifPresent(path -> {
                out.putString(StudioKeys.PATH, path.toString());
                out.putBoolean(StudioKeys.RELOAD, inPack);
            });
        }
        reply.accept(out);
    }

    /* The file written, in the mod's source when this run has it, else in the world's own data pack. */
    private static Optional<Path> write(final ServerLevel level, final ResourceLocation name, final String json) {
        final Optional<Path> source = DevSources.resourcesOf(name.getNamespace());
        try {
            final Path root;
            if (source.isPresent()) {
                root = source.get();
            } else {
                root = level.getServer().getWorldPath(LevelResource.DATAPACK_DIR).resolve(PACK);
                final Path meta = root.resolve(PACK_FILE);
                if (!Files.exists(meta)) {
                    Files.createDirectories(root);
                    Files.writeString(meta, GSON.toJson(packMeta()));
                }
            }
            final Path file = StudioServices.fileIn(root.resolve("data"), name.getNamespace() + "/" + FOLDER + "/"
                    + name.getPath() + JSON);
            Files.createDirectories(file.getParent());
            Files.writeString(file, json);
            return Optional.of(file);
        } catch (final IOException | RuntimeException unwritten) {
            LOGGER.warn("The multiblock pattern {} could not be written", name, unwritten);
            return Optional.empty();
        }
    }

    /* What the world's own data pack says of itself. */
    private static JsonObject packMeta() {
        final JsonObject pack = new JsonObject();
        pack.addProperty("pack_format", PACK_FORMAT);
        pack.addProperty("description", GameText.resolve(PACK_DESCRIPTION));
        final JsonObject meta = new JsonObject();
        meta.add("pack", pack);
        return meta;
    }

    private static CompoundTag refused(final String why) {
        final CompoundTag answer = new CompoundTag();
        answer.putString(StudioServices.REFUSED, why);
        return answer;
    }
}
