/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.guide;

import com.mojang.logging.LogUtils;
import dev.jstech.core.guide.GuideBlock;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import org.slf4j.Logger;

/**
 * The structures a manual's scenes are read from, on the player's game: a structure saved with the game's structure
 * block, shipped as {@code assets/<namespace>/jstech/structures/<path>.nbt}, read once and kept until the resources
 * are read again. Its palette's first state is the one each block takes, as the game places a structure.
 */
public final class GuideStructures {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String FOLDER = "jstech/structures/";
    private static final String NBT = ".nbt";
    private static final Map<String, List<GuideBlock.SceneBlock>> READ = new ConcurrentHashMap<>();

    private GuideStructures() {
    }

    /** The blocks of the structure of {@code id} as a scene holds them; none where its file is missing or broken. */
    public static List<GuideBlock.SceneBlock> blocksOf(final String id) {
        return READ.computeIfAbsent(id, GuideStructures::read);
    }

    /** Forgets every structure read, for the resources being read again. */
    static void forget() {
        READ.clear();
    }

    private static List<GuideBlock.SceneBlock> read(final String id) {
        final ResourceLocation structure = ResourceLocation.tryParse(id);
        if (structure == null) {
            return List.of();
        }
        final ResourceLocation file = ResourceLocation.fromNamespaceAndPath(structure.getNamespace(),
                FOLDER + structure.getPath() + NBT);
        final Optional<Resource> resource = Minecraft.getInstance().getResourceManager().getResource(file);
        if (resource.isEmpty()) {
            LOGGER.warn("The manual's scene of the structure {} finds no file at {}", id, file);
            return List.of();
        }
        try (InputStream in = resource.get().open()) {
            return blocksOf(NbtIo.readCompressed(in, NbtAccounter.unlimitedHeap()));
        } catch (final IOException | RuntimeException broken) {
            LOGGER.warn("The manual's scene of the structure {} cannot read {}: {}", id, file, broken.getMessage());
            return List.of();
        }
    }

    /* A structure's file as the game saves it: its size, its palette of states, and its blocks by palette index. */
    private static List<GuideBlock.SceneBlock> blocksOf(final CompoundTag saved) {
        final ListTag size = saved.getList("size", Tag.TAG_INT);
        if (size.size() != 3) {
            return List.of();
        }
        final ListTag palette = saved.contains("palette", Tag.TAG_LIST) ? saved.getList("palette", Tag.TAG_COMPOUND)
                : firstOf(saved.getList("palettes", Tag.TAG_LIST));
        final List<String> states = new ArrayList<>();
        for (int i = 0; i < palette.size(); i++) {
            states.add(stateOf(palette.getCompound(i)));
        }
        final ListTag blocks = saved.getList("blocks", Tag.TAG_COMPOUND);
        final List<GuideBlock.Structure.Saved> read = new ArrayList<>();
        for (int i = 0; i < blocks.size(); i++) {
            final CompoundTag block = blocks.getCompound(i);
            final ListTag pos = block.getList("pos", Tag.TAG_INT);
            final int state = block.getInt("state");
            if (pos.size() != 3 || state < 0 || state >= states.size()) {
                continue;
            }
            final String data = block.contains("nbt", Tag.TAG_COMPOUND) ? block.getCompound("nbt").toString() : "";
            read.add(new GuideBlock.Structure.Saved(pos.getInt(0), pos.getInt(1), pos.getInt(2), states.get(state),
                    data));
        }
        return GuideBlock.Structure.blocksOf(size.getInt(0), size.getInt(1), size.getInt(2), read);
    }

    /* A structure that keeps several palettes, as a shipwreck does, is read in its first. */
    private static ListTag firstOf(final ListTag palettes) {
        return palettes.isEmpty() ? new ListTag() : palettes.getList(0);
    }

    /* A palette entry as the game writes a block's state: its name, then its properties in brackets. */
    private static String stateOf(final CompoundTag entry) {
        final String name = entry.getString("Name");
        if (!entry.contains("Properties", Tag.TAG_COMPOUND)) {
            return name;
        }
        final CompoundTag properties = entry.getCompound("Properties");
        final StringJoiner written = new StringJoiner(",", name + "[", "]");
        properties.getAllKeys().stream().sorted().forEach(key -> written.add(key + "=" + properties.getString(key)));
        return written.toString();
    }
}
