/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core Test Kit.
 */
package dev.jstech.testkit.check;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Stream;

/**
 * Data generation kept up to date: the files the data generation wrote before, as a mod keeps them, against the ones
 * a run writes now. A file new, gone or changed means the declarations moved on without their files, and a player's
 * game would read the old ones. The data generation's own cache, {@code .cache}, is left out.
 *
 * <p>Pure, so a build runs it as a program after its data run, {@code GeneratedData <before> <after>}, failing when
 * anything differs, and a unit test calls {@link #differences} itself.
 */
public final class GeneratedData {

    private static final String CACHE = ".cache";

    private GeneratedData() {
    }

    /** Every file new, gone or changed from {@code before} to {@code after}, by its path from either, sorted. */
    public static List<String> differences(final Path before, final Path after) {
        final Map<String, Path> was = filesUnder(before);
        final Map<String, Path> is = filesUnder(after);
        final TreeSet<String> paths = new TreeSet<>(was.keySet());
        paths.addAll(is.keySet());
        final List<String> out = new ArrayList<>();
        for (final String path : paths) {
            final Path old = was.get(path);
            final Path now = is.get(path);
            if (old == null) {
                out.add("new: " + path);
            } else if (now == null) {
                out.add("gone: " + path);
            } else if (!sameBytes(old, now)) {
                out.add("changed: " + path);
            }
        }
        return out;
    }

    /**
     * Compares the files of the folder named first, as they were, with the folder named second, as a run left them,
     * and ends with a code that fails the build when anything differs, after saying what.
     */
    public static void main(final String[] args) {
        if (args.length != 2) {
            System.err.println("GeneratedData <the generated files before> <the generated files after>");
            System.exit(2);
        }
        final List<String> differ = differences(Path.of(args[0]), Path.of(args[1]));
        if (differ.isEmpty()) {
            System.out.println("The generated data is up to date.");
            return;
        }
        System.err.println("The generated data was out of date, " + differ.size() + " files; commit what the data"
                + " run wrote:");
        differ.forEach(line -> System.err.println("  " + line));
        System.exit(1);
    }

    /* Every file under the folder but the cache, by its path from it with forward slashes. */
    private static Map<String, Path> filesUnder(final Path folder) {
        final Map<String, Path> out = new TreeMap<>();
        if (!Files.isDirectory(folder)) {
            return out;
        }
        try (Stream<Path> walk = Files.walk(folder)) {
            walk.filter(Files::isRegularFile).forEach(file -> {
                final String path = folder.relativize(file).toString().replace('\\', '/');
                if (!path.equals(CACHE) && !path.startsWith(CACHE + "/")) {
                    out.put(path, file);
                }
            });
        } catch (final IOException e) {
            throw new UncheckedIOException("the files under " + folder + " could not be listed", e);
        }
        return out;
    }

    private static boolean sameBytes(final Path a, final Path b) {
        try {
            return Files.size(a) == Files.size(b) && Arrays.equals(Files.readAllBytes(a), Files.readAllBytes(b));
        } catch (final IOException e) {
            throw new UncheckedIOException("the files " + a + " and " + b + " could not be compared", e);
        }
    }
}
