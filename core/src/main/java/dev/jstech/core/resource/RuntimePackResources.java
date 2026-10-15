/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.resource;

import dev.jstech.core.JsCore;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.AbstractPackResources;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.metadata.MetadataSectionSerializer;
import net.minecraft.server.packs.resources.IoSupplier;
import org.jetbrains.annotations.Nullable;

/*
 * One opening of a pack made as the game loads: its files as the declaration gave them at that moment, each made the
 * first time it is read and kept until the next reload opens the pack anew.
 */
final class RuntimePackResources implements PackResources {

    private final PackLocationInfo location;
    private final PackType type;
    private final byte[] metadata;
    private final Map<String, Supplier<byte[]>> files;
    private final Map<String, byte[]> made = new ConcurrentHashMap<>();

    private static final String METADATA = "pack.mcmeta";

    RuntimePackResources(final PackLocationInfo location, final PackType type, final byte[] metadata,
                         final Map<String, Supplier<byte[]>> files) {
        this.location = location;
        this.type = type;
        this.metadata = metadata;
        this.files = Map.copyOf(files);
    }

    @Override
    public @Nullable IoSupplier<InputStream> getRootResource(final String... elements) {
        if (elements.length == 1 && METADATA.equals(elements[0])) {
            return () -> new ByteArrayInputStream(this.metadata);
        }
        return null;
    }

    @Override
    public @Nullable IoSupplier<InputStream> getResource(final PackType packType, final ResourceLocation location) {
        if (packType != this.type) {
            return null;
        }
        final String path = packType.getDirectory() + "/" + location.getNamespace() + "/" + location.getPath();
        return this.files.containsKey(path) ? open(path) : null;
    }

    @Override
    public void listResources(final PackType packType, final String namespace, final String path,
                              final ResourceOutput resourceOutput) {
        if (packType != this.type) {
            return;
        }
        final String root = packType.getDirectory() + "/" + namespace + "/";
        final String under = root + path + "/";
        for (final String file : this.files.keySet()) {
            if (file.startsWith(under)) {
                final ResourceLocation id = ResourceLocation.tryBuild(namespace, file.substring(root.length()));
                if (id != null) {
                    resourceOutput.accept(id, open(file));
                }
            }
        }
    }

    @Override
    public Set<String> getNamespaces(final PackType packType) {
        final Set<String> out = new TreeSet<>();
        if (packType != this.type) {
            return out;
        }
        final String root = packType.getDirectory() + "/";
        for (final String file : this.files.keySet()) {
            final int end = file.indexOf('/', root.length());
            if (file.startsWith(root) && end > root.length()) {
                out.add(file.substring(root.length(), end));
            }
        }
        return out;
    }

    @Override
    public <T> @Nullable T getMetadataSection(final MetadataSectionSerializer<T> deserializer) {
        return AbstractPackResources.getMetadataFromStream(deserializer, new ByteArrayInputStream(this.metadata));
    }

    @Override
    public PackLocationInfo location() {
        return this.location;
    }

    /* What the pack holds is the game's own things drawn anew, nothing a player picks or drops. */
    @Override
    public boolean isHidden() {
        return true;
    }

    @Override
    public void close() {
        // Nothing is held open: every file is bytes in memory.
    }

    private IoSupplier<InputStream> open(final String path) {
        return () -> new ByteArrayInputStream(this.made.computeIfAbsent(path, this::make));
    }

    private byte[] make(final String path) {
        try {
            return this.files.get(path).get();
        } catch (final RuntimeException failed) {
            // One file that cannot be made leaves only that file out; the rest of the pack still loads.
            JsCore.LOGGER.warn("The generated file {} of {} could not be made", path, this.location.id(), failed);
            return new byte[0];
        }
    }
}
