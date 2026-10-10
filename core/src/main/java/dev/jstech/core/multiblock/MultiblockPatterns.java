/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.multiblock;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.jstech.core.JsCore;
import dev.jstech.core.data.DataRegistry;
import dev.jstech.core.id.StableCodecs;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Nullable;

/**
 * The multiblock patterns of every mod, by id, as data: each file under {@code data/<namespace>/multiblock/} is one
 * pattern, read whenever the server reloads its data, so a datapack can change a structure or add one. A mod declares
 * its own in code with {@link #declare}, and the generator writes each declared pattern to its file, so the pattern
 * a mod ships is the one a pack overrides.
 *
 * <p>A pattern's file is its layers from the bottom up, each a list of rows from north to south written west to east,
 * a key giving each letter the blocks or tags that fit it, and the letters whose slots are ports:
 *
 * <pre>{@code
 * {"layers": [["AIA", "A#A", "AOA"]],
 *  "key": {"A": {"blocks": ["minecraft:iron_block"]}, "I": {"tags": ["minecraft:logs"]}, ...},
 *  "ports": {"I": "item_input", "O": "item_output"}}
 * }</pre>
 *
 * <p>A structure saved with a structure block reads as a pattern too, through {@link #fromStructure}: the block that
 * is the controller is named, air and structure voids are left free, and every other block must be there as saved.
 */
public final class MultiblockPatterns {

    private static final Codec<BlockMatch> MATCH = RecordCodecBuilder.<RawMatch>create(instance -> instance.group(
                    Codec.STRING.listOf().optionalFieldOf("blocks", List.of()).forGetter(RawMatch::blocks),
                    Codec.STRING.listOf().optionalFieldOf("tags", List.of()).forGetter(RawMatch::tags))
            .apply(instance, RawMatch::new))
            .flatXmap(raw -> raw.blocks().isEmpty() && raw.tags().isEmpty()
                            ? DataResult.error(() -> "a slot names at least one block or tag")
                            : DataResult.success(new BlockMatch(raw.blocks(), raw.tags())).flatMap(match -> {
                                final String bad = unreadableId(match);
                                return bad == null ? DataResult.success(match)
                                        : DataResult.<BlockMatch>error(() -> "a slot names \"" + bad
                                                + "\", which is not an id");
                            }),
                    match -> DataResult.success(new RawMatch(match.blocks(), match.tags())));

    private static final Codec<Character> LETTER = Codec.STRING.flatXmap(
            text -> text.length() == 1 ? DataResult.success(text.charAt(0))
                    : DataResult.error(() -> "a key is one letter, not \"" + text + "\""),
            letter -> DataResult.success(String.valueOf(letter)));

    /** A pattern as its file holds it. */
    public static final Codec<MultiblockPattern> CODEC = RecordCodecBuilder.<RawPattern>create(instance ->
                    instance.group(
                            Codec.STRING.optionalFieldOf("name", "multiblock").forGetter(RawPattern::name),
                            Codec.STRING.listOf().listOf().fieldOf("layers").forGetter(RawPattern::layers),
                            Codec.unboundedMap(LETTER, MATCH).fieldOf("key").forGetter(RawPattern::key),
                            Codec.unboundedMap(LETTER, StableCodecs.byName(PortKind.class))
                                    .optionalFieldOf("ports", Map.of()).forGetter(RawPattern::ports))
                            .apply(instance, RawPattern::new))
            .flatXmap(MultiblockPatterns::build, MultiblockPatterns::raw);

    private static final DataRegistry<MultiblockPattern> FILES = DataRegistry.builder(
            ResourceLocation.fromNamespaceAndPath(JsCore.MODID, "multiblock"), "multiblock", CODEC).register();

    /** The patterns mods declared in code, which the files of the same id replace. */
    private static final Map<ResourceLocation, MultiblockPattern> DECLARED =
            Collections.synchronizedMap(new LinkedHashMap<>());

    private MultiblockPatterns() {
    }

    /** Loads the registry of pattern files, from the Core's constructor. */
    public static void register() {
        // The registry is made when this class loads; loading it while the game starts is all this does.
    }

    /**
     * Declares a mod's pattern, which a file of the same id replaces.
     *
     * @throws IllegalStateException    when a pattern of that id is declared already
     * @throws IllegalArgumentException when a slot of the pattern names a block or a tag by an id the game cannot read,
     *                                  which would otherwise leave the structure unable to form with no message
     */
    public static MultiblockPattern declare(final ResourceLocation id, final MultiblockPattern pattern) {
        for (final IBlockMatcher matcher : pattern.mapping().values()) {
            final String bad = matcher instanceof BlockMatch match ? unreadableId(match) : null;
            if (bad != null) {
                throw new IllegalArgumentException("the multiblock pattern " + id + " names \"" + bad
                        + "\", which is not an id");
            }
        }
        if (DECLARED.putIfAbsent(id, pattern) != null) {
            throw new IllegalStateException("the multiblock pattern " + id + " is declared twice");
        }
        return pattern;
    }

    /** The patterns declared in code by the mod of that namespace, for its generator to write. */
    public static Map<ResourceLocation, MultiblockPattern> declaredBy(final String namespace) {
        final Map<ResourceLocation, MultiblockPattern> out = new LinkedHashMap<>();
        synchronized (DECLARED) {
            DECLARED.forEach((id, pattern) -> {
                if (id.getNamespace().equals(namespace)) {
                    out.put(id, pattern);
                }
            });
        }
        return out;
    }

    /** The pattern of that id: the server's file when there is one, the declared one otherwise. */
    public static Optional<MultiblockPattern> get(final ResourceLocation id) {
        final Optional<MultiblockPattern> file = FILES.get(id);
        return file.isPresent() ? file : Optional.ofNullable(DECLARED.get(id));
    }

    /**
     * Matches {@code pattern} against the world with its controller at {@code controller}, in any of the four
     * rotations, block tags included.
     */
    public static IMatchResult match(final MultiblockPattern pattern, final BlockGetter level,
                                     final BlockPos controller) {
        return PatternMatcher.match(pattern, provider(level), PatternMatcher.encodePosition(controller.getX(),
                controller.getY(), controller.getZ()));
    }

    /** The world as the matcher reads it: each place's block id and the block's tags. */
    public static IBlockProvider provider(final BlockGetter level) {
        return new IBlockProvider() {
            @Override
            public String blockAt(final long encodedPos) {
                return BuiltInRegistries.BLOCK.getKey(blockIn(level, encodedPos)).toString();
            }

            @Override
            public boolean hasTag(final long encodedPos, final String tag) {
                final ResourceLocation tagId = ResourceLocation.tryParse(tag);
                return tagId != null && blockIn(level, encodedPos).builtInRegistryHolder()
                        .is(TagKey.create(Registries.BLOCK, tagId));
            }
        };
    }

    /** The place an encoded position stands for. */
    public static BlockPos decode(final long encodedPos) {
        final int[] xyz = PatternMatcher.decodePosition(encodedPos);
        return new BlockPos(xyz[0], xyz[1], xyz[2]);
    }

    /**
     * Reads a structure block's file as a pattern: the block named {@code controllerBlock} is the controller, the
     * blocks of {@code portBlocks} are ports of their kind, air and structure voids are free, and every other block
     * must be there as it was saved.
     *
     * @throws IllegalArgumentException when the structure holds no controller, or more than one, or is not a
     *                                  structure file
     */
    public static MultiblockPattern fromStructure(final String name, final CompoundTag structure,
                                                  final String controllerBlock,
                                                  final Map<String, PortKind> portBlocks) {
        final ListTag size = structure.getList("size", Tag.TAG_INT);
        final ListTag palette = structure.getList("palette", Tag.TAG_COMPOUND);
        final ListTag blocks = structure.getList("blocks", Tag.TAG_COMPOUND);
        if (size.size() != 3 || palette.isEmpty()) {
            throw new IllegalArgumentException(name + " is not a structure file");
        }
        final int sx = size.getInt(0);
        final int sy = size.getInt(1);
        final int sz = size.getInt(2);
        final char[][][] grid = new char[sy][sz][sx];
        for (final char[][] layer : grid) {
            for (final char[] row : layer) {
                Arrays.fill(row, MultiblockPattern.IGNORE_CHAR);
            }
        }
        final List<String> paletteIds = new ArrayList<>();
        for (int i = 0; i < palette.size(); i++) {
            paletteIds.add(palette.getCompound(i).getString("Name"));
        }
        final Map<String, Character> letters = new LinkedHashMap<>();
        char next = 'a';
        for (int i = 0; i < blocks.size(); i++) {
            final CompoundTag block = blocks.getCompound(i);
            final ListTag pos = block.getList("pos", Tag.TAG_INT);
            final int state = block.getInt("state");
            if (state < 0 || state >= paletteIds.size()) {
                throw new IllegalArgumentException(name + " has a block of palette entry " + state + ", and the palette"
                        + " holds " + paletteIds.size());
            }
            if (pos.size() != 3 || pos.getInt(0) < 0 || pos.getInt(0) >= sx || pos.getInt(1) < 0
                    || pos.getInt(1) >= sy || pos.getInt(2) < 0 || pos.getInt(2) >= sz) {
                throw new IllegalArgumentException(name + " has a block outside its own size of " + sx + " by " + sy
                        + " by " + sz);
            }
            final String id = paletteIds.get(state);
            if (id.equals("minecraft:air") || id.equals("minecraft:structure_void")) {
                continue;
            }
            final char letter;
            if (id.equals(controllerBlock)) {
                letter = MultiblockPattern.CONTROLLER_CHAR;
            } else {
                Character known = letters.get(id);
                if (known == null) {
                    next = nextLetter(next);
                    known = next++;
                    letters.put(id, known);
                }
                letter = known;
            }
            grid[pos.getInt(1)][pos.getInt(2)][pos.getInt(0)] = letter;
        }
        final MultiblockPattern.Builder builder = MultiblockPattern.builder(name);
        for (final char[][] layer : grid) {
            final String[] rows = new String[sz];
            for (int z = 0; z < sz; z++) {
                rows[z] = new String(layer[z]);
            }
            builder.layer(rows);
        }
        letters.forEach((id, letter) -> {
            builder.where(letter, BlockMatch.blocks(id));
            final PortKind port = portBlocks.get(id);
            if (port != null) {
                builder.port(letter, port);
            }
        });
        try {
            return builder.build();
        } catch (final IllegalStateException wrong) {
            throw new IllegalArgumentException(name + ": " + wrong.getMessage(), wrong);
        }
    }

    /**
     * Reads the structure file {@code data/<namespace>/structure/<path>.nbt} of {@code id} as a pattern, as
     * {@link #fromStructure} does.
     *
     * @throws IOException when the file is missing or unreadable
     */
    public static MultiblockPattern loadStructure(final ResourceManager resources, final ResourceLocation id,
                                                  final String controllerBlock,
                                                  final Map<String, PortKind> portBlocks) throws IOException {
        final ResourceLocation file = id.withPath(path -> "structure/" + path + ".nbt");
        final Resource resource = resources.getResource(file)
                .orElseThrow(() -> new IOException("no structure file " + file));
        try (InputStream in = resource.open()) {
            final CompoundTag tag = NbtIo.readCompressed(in, NbtAccounter.unlimitedHeap());
            return fromStructure(id.toString(), tag, controllerBlock, portBlocks);
        } catch (final IllegalArgumentException unreadable) {
            throw new IOException(unreadable.getMessage(), unreadable);
        }
    }

    /* The first block or tag id of the match that cannot be read as an id, or null when all of them can. */
    @Nullable
    private static String unreadableId(final BlockMatch match) {
        for (final String id : match.blocks()) {
            if (ResourceLocation.tryParse(id) == null) {
                return id;
            }
        }
        for (final String id : match.tags()) {
            if (ResourceLocation.tryParse(id) == null) {
                return id;
            }
        }
        return null;
    }

    private static Block blockIn(final BlockGetter level, final long encodedPos) {
        return level.getBlockState(decode(encodedPos)).getBlock();
    }

    /* The next letter a structure's block can take, past the two the pattern keeps for itself. */
    private static char nextLetter(final char from) {
        char letter = from;
        while (letter == MultiblockPattern.CONTROLLER_CHAR || letter == MultiblockPattern.IGNORE_CHAR) {
            letter++;
        }
        return letter;
    }

    private static DataResult<MultiblockPattern> build(final RawPattern raw) {
        try {
            final MultiblockPattern.Builder builder = MultiblockPattern.builder(raw.name());
            for (final List<String> layer : raw.layers()) {
                builder.layer(layer.toArray(String[]::new));
            }
            raw.key().forEach(builder::where);
            raw.ports().forEach(builder::port);
            return DataResult.success(builder.build());
        } catch (final IllegalArgumentException | IllegalStateException wrong) {
            return DataResult.error(wrong::getMessage);
        }
    }

    private static DataResult<RawPattern> raw(final MultiblockPattern pattern) {
        final Map<Character, BlockMatch> key = new LinkedHashMap<>();
        for (final Map.Entry<Character, IBlockMatcher> slot : pattern.mapping().entrySet()) {
            if (!(slot.getValue() instanceof BlockMatch match)) {
                return DataResult.error(() -> "the pattern " + pattern.name() + " fits '" + slot.getKey()
                        + "' with code, which no file can hold; declare it with BlockMatch");
            }
            key.put(slot.getKey(), match);
        }
        final List<List<String>> layers = new ArrayList<>();
        for (int y = 0; y < pattern.sizeY(); y++) {
            layers.add(List.of(pattern.rows(y)));
        }
        return DataResult.success(new RawPattern(pattern.name(), layers, key, pattern.ports()));
    }

    private record RawMatch(List<String> blocks, List<String> tags) {
    }

    private record RawPattern(String name, List<List<String>> layers, Map<Character, BlockMatch> key,
                              Map<Character, PortKind> ports) {
    }
}
