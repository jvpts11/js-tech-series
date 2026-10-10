/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.os.fs;

import com.mojang.serialization.Codec;
import dev.jstech.computers.JsComputers;
import dev.jstech.computers.crafting.CraftingPattern;
import dev.jstech.computers.crafting.MultiStagePattern;
import dev.jstech.computers.crafting.ProcessingPattern;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.TagParser;
import net.minecraft.resources.RegistryOps;

import java.util.Optional;

/**
 * Serializes and parses a {@link CraftingPattern} to and from the UTF-8 string content stored in a
 * {@link FileType#CRAFT} file on a computer's system disk.
 *
 * <p>The wire format is SNBT (Stringified NBT), produced by encoding the pattern with
 * {@link CraftingPattern#CODEC} over {@link NbtOps} backed by the server's
 * {@link HolderLookup.Provider}. SNBT is self-contained ASCII text, survives round-trips through
 * the filesystem's UTF-8 string storage, and matches the same encoding path already used in
 * the Crafting Computer's {@code saveExtra}/{@code loadExtra} methods.
 */
public final class CraftFile {

    /* Lists and compounds inside one another: an item's components nest a dozen deep at most. */
    private static final int MOST_NESTING = 128;

    private CraftFile() {
    }

    /**
     * Serializes {@code pattern} to an SNBT string suitable for storage in a {@code .craft} file.
     *
     * <p>Returns an empty {@link Optional} when encoding fails.
     *
     * @param pattern    the pattern to serialize
     * @param registries the server's holder-lookup provider (needed for item-component codecs)
     * @return the SNBT string, or empty on failure
     */
    public static Optional<String> serialize(final CraftingPattern pattern,
                                             final HolderLookup.Provider registries) {
        final RegistryOps<Tag> ops = RegistryOps.create(NbtOps.INSTANCE, registries);
        final var result = CraftingPattern.CODEC.encodeStart(ops, pattern);
        if (result.isError()) {
            JsComputers.LOGGER.warn("Failed to serialize CraftingPattern to .craft: {}",
                    result.error().map(e -> e.message()).orElse("unknown"));
            return Optional.empty();
        }
        final Tag tag = result.getOrThrow();
        return Optional.of(tag.toString());
    }

    /**
     * Parses a {@link CraftingPattern} from the content string of a {@code .craft} file.
     *
     * <p>Returns an empty {@link Optional} when the content is not valid SNBT or does not decode
     * to a {@link CraftingPattern}.
     *
     * @param content    the UTF-8 string content of the file (SNBT)
     * @param registries the server's holder-lookup provider (needed for item-component codecs)
     * @return the parsed pattern, or empty on failure
     */
    public static Optional<CraftingPattern> parse(final String content,
                                                   final HolderLookup.Provider registries) {
        final Optional<Tag> tag = tagOf(content);
        if (tag.isEmpty()) {
            return Optional.empty();
        }
        final RegistryOps<Tag> ops = RegistryOps.create(NbtOps.INSTANCE, registries);
        return decode(CraftingPattern.CODEC, ops, tag.get());
    }

    /*
     * Typed .craft for machine recipes. A wrapper tags the kind so parse can route; the legacy bench
     * .craft above stays untagged for back-compat, and typeOf() reports "craft" for it.
     */

    public static Optional<String> serializeProcessing(final ProcessingPattern pattern,
                                                       final HolderLookup.Provider registries) {
        return wrap("proc", ProcessingPattern.CODEC, pattern, registries);
    }

    public static Optional<String> serializeMultiStage(final MultiStagePattern pattern,
                                                       final HolderLookup.Provider registries) {
        return wrap("multi", MultiStagePattern.CODEC, pattern, registries);
    }

    public static Optional<ProcessingPattern> parseProcessing(final String content,
                                                              final HolderLookup.Provider registries) {
        return unwrap(content, "proc", ProcessingPattern.CODEC, registries);
    }

    public static Optional<MultiStagePattern> parseMultiStage(final String content,
                                                              final HolderLookup.Provider registries) {
        return unwrap(content, "multi", MultiStagePattern.CODEC, registries);
    }

    /** The recipe kind a {@code .craft} holds: "proc", "multi", or "craft" for the legacy untagged bench pattern. */
    public static String typeOf(final String content) {
        // Anything not readable as a tagged compound is taken for the legacy bench pattern.
        if (tagOf(content).orElse(null) instanceof CompoundTag c && c.contains("type")) {
            return c.getString("type");
        }
        return "craft";
    }

    private static <T> Optional<String> wrap(final String type, final Codec<T> codec, final T value,
                                             final HolderLookup.Provider registries) {
        final RegistryOps<Tag> ops = RegistryOps.create(NbtOps.INSTANCE, registries);
        final var result = codec.encodeStart(ops, value);
        if (result.isError()) {
            JsComputers.LOGGER.warn("Failed to serialize {} pattern to .craft: {}", type,
                    result.error().map(e -> e.message()).orElse("unknown"));
            return Optional.empty();
        }
        final CompoundTag wrapped = new CompoundTag();
        wrapped.putString("type", type);
        wrapped.put("data", result.getOrThrow());
        return Optional.of(wrapped.toString());
    }

    private static <T> Optional<T> unwrap(final String content, final String type, final Codec<T> codec,
                                          final HolderLookup.Provider registries) {
        if (!(tagOf(content).orElse(null) instanceof CompoundTag c) || !type.equals(c.getString("type"))
                || c.get("data") == null) {
            return Optional.empty();
        }
        final RegistryOps<Tag> ops = RegistryOps.create(NbtOps.INSTANCE, registries);
        return decode(codec, ops, c.get("data"));
    }

    /*
     * The text of a file read as SNBT, or nothing when it is not SNBT. The game's reader goes one call deeper for every
     * list or compound it opens and has no limit of its own, so a file that is a long run of brackets, which any player
     * can write and rename, would overflow the server's stack; one nested deeper than any pattern is refused first.
     */
    private static Optional<Tag> tagOf(final String content) {
        if (content == null || nestsTooDeep(content)) {
            return Optional.empty();
        }
        try {
            return Optional.of(TagParser.parseTag(content));
        } catch (final Exception e) {
            JsComputers.LOGGER.debug("A .craft file is not SNBT: {}", e.getMessage());
            return Optional.empty();
        }
    }

    /*
     * A value decoded from a tag, or nothing when the tag does not hold one. A record that checks itself as it is made
     * throws rather than answering with an error, and a file a player wrote must never get that far.
     */
    private static <T> Optional<T> decode(final Codec<T> codec, final RegistryOps<Tag> ops, final Tag tag) {
        try {
            final var result = codec.parse(ops, tag);
            if (result.isError()) {
                JsComputers.LOGGER.debug("A .craft file does not hold a pattern: {}",
                        result.error().map(e -> e.message()).orElse("unknown"));
            }
            return result.result();
        } catch (final RuntimeException malformed) {
            JsComputers.LOGGER.debug("A .craft file does not hold a pattern: {}", malformed.getMessage());
            return Optional.empty();
        }
    }

    /* Whether the text opens more lists and compounds inside one another than any pattern does, quoted text aside. */
    private static boolean nestsTooDeep(final String content) {
        int depth = 0;
        char quote = 0;
        boolean escaped = false;
        for (int i = 0; i < content.length(); i++) {
            final char c = content.charAt(i);
            if (quote != 0) {
                if (escaped) {
                    escaped = false;
                } else if (c == '\\') {
                    escaped = true;
                } else if (c == quote) {
                    quote = 0;
                }
            } else if (c == '"' || c == '\'') {
                quote = c;
            } else if (c == '[' || c == '{') {
                if (++depth > MOST_NESTING) {
                    return true;
                }
            } else if (c == ']' || c == '}') {
                depth = Math.max(0, depth - 1);
            }
        }
        return false;
    }
}
