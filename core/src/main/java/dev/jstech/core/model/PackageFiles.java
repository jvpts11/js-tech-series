/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.model;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * The files of a model that is a zip of files, as 3MF and USDZ are: each by its path in the package, found whatever
 * the case of its letters, as a package's parts are named. A package that would unpack into more than the Core reads
 * for one model is refused, so a file made to fill the memory fills nothing. Pure.
 */
final class PackageFiles {

    private final Map<String, byte[]> files;

    /* The most a package unpacks into, and the most files it holds. */
    private static final long MOST_BYTES = 256L << 20;
    private static final int MOST_FILES = 10_000;
    private static final int BUFFER = 8192;

    private PackageFiles(final Map<String, byte[]> files) {
        this.files = files;
    }

    /**
     * The files of the zip {@code bytes}.
     *
     * @throws ModelFormatException when it is not a zip, or unpacks into too much
     */
    static PackageFiles of(final String name, final byte[] bytes) {
        final Map<String, byte[]> files = new LinkedHashMap<>();
        long total = 0;
        try (ZipInputStream in = new ZipInputStream(new ByteArrayInputStream(bytes))) {
            final byte[] buffer = new byte[BUFFER];
            for (ZipEntry entry = in.getNextEntry(); entry != null; entry = in.getNextEntry()) {
                if (entry.isDirectory()) {
                    continue;
                }
                if (files.size() >= MOST_FILES) {
                    throw new ModelFormatException("the package " + name + " holds more files than a model may");
                }
                final ByteArrayOutputStream out = new ByteArrayOutputStream();
                for (int read = in.read(buffer); read > 0; read = in.read(buffer)) {
                    total += read;
                    if (total > MOST_BYTES) {
                        throw new ModelFormatException("the package " + name + " unpacks into more than a model may");
                    }
                    out.write(buffer, 0, read);
                }
                files.put(key(entry.getName()), out.toByteArray());
            }
        } catch (final IOException broken) {
            throw new ModelFormatException("the package " + name + " is not a zip that reads: "
                    + broken.getMessage(), broken);
        }
        if (files.isEmpty()) {
            throw new ModelFormatException("the package " + name + " holds no files");
        }
        return new PackageFiles(files);
    }

    /** The file at {@code path}, from the package's root, with a slash before it or not. */
    Optional<byte[]> file(final String path) {
        return Optional.ofNullable(this.files.get(key(path)));
    }

    /** Every file's path, in the package's order, in small letters. */
    Iterable<String> paths() {
        return this.files.keySet();
    }

    /** The path {@code relative} names from the file at {@code from}, as a package names one part from another. */
    static String resolve(final String from, final String relative) {
        if (relative.startsWith("/")) {
            return relative.substring(1);
        }
        final int slash = from.lastIndexOf('/');
        return (slash < 0 ? "" : from.substring(0, slash + 1)) + relative;
    }

    private static String key(final String path) {
        final String plain = path.replace('\\', '/');
        return (plain.startsWith("/") ? plain.substring(1) : plain).toLowerCase(Locale.ROOT);
    }
}
