/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.resource;

import dev.jstech.core.log.CoreLogs;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Stream;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLPaths;
import org.slf4j.Logger;

/**
 * The files of a pack made as the game loads, kept on disk once made: the next load reads them back rather than
 * making them again, for as long as what they are made from stays the same. What they are made from is said by the
 * pack's fingerprint, the mods installed and their versions and whatever else the pack is drawn from; when it changes,
 * the kept files are thrown away and made again as they are first read. They are kept under {@code jstech/cache/} in
 * the game's folder, which can be emptied at any time.
 */
public final class CachedRuntimeFiles implements IRuntimeFiles {

    private final Path folder;
    private final IRuntimeFiles made;
    private final Supplier<String> fingerprint;

    private static final Logger LOGGER = CoreLogs.CONTENT.logger();
    /** The folder in the game's folder the kept files go in. */
    public static final String FOLDER = "jstech/cache";
    private static final String FINGERPRINT = "fingerprint.txt";

    private CachedRuntimeFiles(final Path folder, final IRuntimeFiles made, final Supplier<String> fingerprint) {
        this.folder = folder;
        this.made = made;
        this.fingerprint = fingerprint;
    }

    /** The files {@code made} gives for the pack {@code id}, kept on disk while {@code fingerprint} stays the same. */
    public static IRuntimeFiles of(final ResourceLocation id, final IRuntimeFiles made,
                                   final Supplier<String> fingerprint) {
        return of(FMLPaths.GAMEDIR.get().resolve(FOLDER).resolve(id.getNamespace()).resolve(id.getPath()), made,
                fingerprint);
    }

    /** The same, kept in {@code folder}. */
    public static IRuntimeFiles of(final Path folder, final IRuntimeFiles made, final Supplier<String> fingerprint) {
        return new CachedRuntimeFiles(folder, made, fingerprint);
    }

    /** A fingerprint of the mods installed, each with its version, for a pack's fingerprint to start from. */
    public static String modsFingerprint() {
        final StringBuilder out = new StringBuilder();
        ModList.get().getSortedMods().forEach(mod -> out.append(mod.getModId()).append('@')
                .append(mod.getModInfo().getVersion()).append('\n'));
        return out.toString();
    }

    @Override
    public Map<String, Supplier<byte[]>> files() {
        final Map<String, Supplier<byte[]>> fresh = this.made.files();
        final String print = digest(this.fingerprint.get() + "\n" + String.join("\n", fresh.keySet()));
        if (!print.equals(keptFingerprint())) {
            startAgain(print);
        }
        final Map<String, Supplier<byte[]>> out = new LinkedHashMap<>();
        fresh.forEach((path, make) -> out.put(path, () -> read(path, make)));
        return out;
    }

    /* The file at {@code path}: as it was kept, or made now and kept. */
    private byte[] read(final String path, final Supplier<byte[]> make) {
        final Path file = this.folder.resolve(path).normalize();
        if (!file.startsWith(this.folder)) {
            return make.get();
        }
        try {
            if (Files.isRegularFile(file)) {
                return Files.readAllBytes(file);
            }
        } catch (final IOException unreadable) {
            LOGGER.debug("the kept {} could not be read and is made again", file, unreadable);
        }
        final byte[] bytes = make.get();
        try {
            writeWhole(file, bytes);
        } catch (final IOException unwritable) {
            LOGGER.debug("{} could not be kept, and is made again next time", file, unwritable);
        }
        return bytes;
    }

    private String keptFingerprint() {
        try {
            final Path file = this.folder.resolve(FINGERPRINT);
            return Files.isRegularFile(file) ? Files.readString(file, StandardCharsets.UTF_8).trim() : "";
        } catch (final IOException unreadable) {
            return "";
        }
    }

    /* What was kept no longer holds: the folder is emptied and the new fingerprint written down. */
    private void startAgain(final String print) {
        try {
            if (Files.isDirectory(this.folder)) {
                final List<Path> kept;
                try (Stream<Path> walk = Files.walk(this.folder)) {
                    kept = walk.sorted(Comparator.reverseOrder()).toList();
                }
                for (final Path path : kept) {
                    if (!path.equals(this.folder)) {
                        Files.deleteIfExists(path);
                    }
                }
            }
            writeWhole(this.folder.resolve(FINGERPRINT), print.getBytes(StandardCharsets.UTF_8));
        } catch (final IOException unwritable) {
            LOGGER.warn("the files kept in {} could not be made anew; they are made again every load", this.folder,
                    unwritable);
        }
    }

    /* {@code bytes} into a file beside {@code path} first, then over it in one move, so no half file is ever read. */
    private static void writeWhole(final Path path, final byte[] bytes) throws IOException {
        Files.createDirectories(path.getParent());
        final Path partial = path.resolveSibling(path.getFileName() + ".partial");
        Files.write(partial, bytes);
        try {
            Files.move(partial, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (final AtomicMoveNotSupportedException notOnThisDisk) {
            Files.move(partial, path, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static String digest(final String text) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(text.getBytes(StandardCharsets.UTF_8)));
        } catch (final NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("every Java has SHA-256", impossible);
        }
    }
}
