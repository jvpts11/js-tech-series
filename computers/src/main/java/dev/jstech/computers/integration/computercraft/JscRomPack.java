/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.integration.computercraft;

import com.mojang.logging.LogUtils;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.Set;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackSelectionConfig;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.metadata.MetadataSectionSerializer;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackCompatibility;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.server.packs.resources.IoSupplier;
import net.minecraft.world.flag.FeatureFlagSet;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

/**
 * The agent, handed to ComputerCraft the way ComputerCraft takes things: as a data pack.
 *
 * <p>Their computers build what they call the rom out of every data pack in the world, and anything in
 * its autorun folder runs when a computer starts. This pack holds exactly one file, which is the agent,
 * and it is made when the pack is opened rather than kept anywhere: the jar has the agent's source, and
 * the Lua exists only for as long as the world is loaded.
 *
 * <p>It is an ordinary pack, always on, so a world that does not want it can turn it off like any other.
 */
public final class JscRomPack implements PackResources {

    /** The pack's name, which is what a player sees in the data pack list. */
    public static final String ID = "jsc_agent";

    private static final String CC = "computercraft";
    private static final Logger LOG = LogUtils.getLogger();
    private static boolean said;

    private final PackLocationInfo location;

    private JscRomPack(final PackLocationInfo location) {
        this.location = location;
    }

    /** The pack itself, ready to be handed to the game. */
    public static Pack pack() {
        final PackLocationInfo where = new PackLocationInfo(ID,
                Component.literal("J's Computers agent"), PackSource.BUILT_IN, Optional.empty());
        final Pack.Metadata about = new Pack.Metadata(
                Component.literal("What a ComputerCraft computer runs to answer this network"),
                PackCompatibility.COMPATIBLE, FeatureFlagSet.of(), java.util.List.of(), false);
        return new Pack(where, new Pack.ResourcesSupplier() {
            @Override
            public PackResources openPrimary(final PackLocationInfo at) {
                return new JscRomPack(at);
            }

            @Override
            public PackResources openFull(final PackLocationInfo at, final Pack.Metadata ignored) {
                return new JscRomPack(at);
            }
        }, about, new PackSelectionConfig(true, Pack.Position.TOP, false));
    }

    @Override
    @Nullable
    public IoSupplier<InputStream> getRootResource(final String... elements) {
        return null;
    }

    /** Everything this pack serves, in the places their computers look for them. */
    private static final java.util.List<String> FILES =
            java.util.List.of(JscRom.AGENT_PATH, JscRom.COMMAND_PATH);

    @Override
    @Nullable
    public IoSupplier<InputStream> getResource(final PackType type, final ResourceLocation at) {
        if (type != PackType.SERVER_DATA || !CC.equals(at.getNamespace())
                || !FILES.contains(at.getPath())) {
            return null;
        }
        final String lua = JscRom.at(at.getPath());
        if (lua.isEmpty()) {
            /*
             * The agent is made out of this mod's own source by this mod's own translator, so there is
             * no world in which a player can fix this: it is said once, loudly, and nothing is served.
             */
            if (!said) {
                said = true;
                LOG.error("No ComputerCraft agent: {}", JscRom.failure());
            }
            return null;
        }
        return () -> new ByteArrayInputStream(lua.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public void listResources(final PackType type, final String namespace, final String path,
                              final ResourceOutput output) {
        if (type != PackType.SERVER_DATA || !CC.equals(namespace)) {
            return;
        }
        for (final String file : FILES) {
            if (!file.startsWith(path)) {
                continue;
            }
            final ResourceLocation at = ResourceLocation.fromNamespaceAndPath(CC, file);
            final IoSupplier<InputStream> held = this.getResource(type, at);
            if (held != null) {
                output.accept(at, held);
            }
        }
    }

    @Override
    public Set<String> getNamespaces(final PackType type) {
        return type == PackType.SERVER_DATA ? Set.of(CC) : Set.of();
    }

    @Override
    @Nullable
    public <T> T getMetadataSection(final MetadataSectionSerializer<T> serializer) {
        return null;
    }

    @Override
    public PackLocationInfo location() {
        return this.location;
    }

    @Override
    public void close() {
    }
}
