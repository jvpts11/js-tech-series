/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * Every asset of the series accounted for in {@code ASSET_REGISTRY.md}: where it came from and who made it.
 *
 * <p>An asset nobody can say the origin of is one nobody can say the mods may ship, and an AI-generated one nobody
 * marked is one nobody will remember to replace. So the registry is read here as the build runs, and the build fails
 * on an asset no entry covers, on one two entries both claim, on an entry that does not say its origin and author,
 * and on an entry left behind after its assets were removed.
 */
class AssetRegistryTest {

    /** The registry, at the root of the repository. */
    private static final String REGISTRY = "ASSET_REGISTRY.md";

    /** The mods whose assets ship; the test mod is for development only. */
    private static final List<String> MODULES = List.of("core", "computers", "industrial");

    /** Where an origin can come from; anything else is not an answer. */
    private static final Set<String> ORIGINS = Set.of("AI-generated", "Hand-made", "Datagen", "Third-party");

    /** A picture a mod's resources carry beside its code, its logo and icon. */
    private static final Pattern ROOT_IMAGE = Pattern.compile("[^/]+\\.(png|jpg|jpeg|gif)");

    @Test
    void everyAsset_isCoveredByExactlyOneEntry() {
        final List<Entry> entries = entries();
        final List<String> wrong = new ArrayList<>();
        for (final String asset : assets()) {
            final List<String> claiming = entries.stream().filter(entry -> entry.covers(asset)).map(Entry::path)
                    .toList();
            if (claiming.isEmpty()) {
                wrong.add(asset + ": no entry says where it came from");
            } else if (claiming.size() > 1) {
                wrong.add(asset + ": claimed by " + String.join(" and ", claiming));
            }
        }
        if (!wrong.isEmpty()) {
            fail("every asset needs one entry in " + REGISTRY + ":\n" + String.join("\n", wrong));
        }
    }

    @Test
    void everyEntry_saysWhereItCameFromAndWhoMadeIt() {
        final List<String> wrong = new ArrayList<>();
        for (final Entry entry : entries()) {
            if (!ORIGINS.contains(entry.origin())) {
                wrong.add(entry.path() + ": the origin \"" + entry.origin() + "\" is not one of " + ORIGINS);
            }
            if (entry.madeBy().isBlank() || "-".equals(entry.madeBy())) {
                wrong.add(entry.path() + ": does not say who made it");
            }
            if (entry.what().isBlank() || entry.use().isBlank()) {
                wrong.add(entry.path() + ": does not say what it is and how it is used");
            }
        }
        if (!wrong.isEmpty()) {
            fail(String.join("\n", wrong));
        }
    }

    @Test
    void everyEntry_coversSomethingThatIsThere() {
        final List<String> files = everyFile();
        final List<String> stale = new ArrayList<>();
        for (final Entry entry : entries()) {
            if (files.stream().noneMatch(entry::covers)) {
                stale.add(entry.path());
            }
        }
        if (!stale.isEmpty()) {
            fail("entries left behind by assets that are gone:\n" + String.join("\n", stale));
        }
    }

    /** The checks above would pass on nothing at all, so both sides are made sure to be there. */
    @Test
    void theRegistryAndTheAssetsAreBothRead() {
        assertFalse(entries().isEmpty(), REGISTRY + " was read as having no entries");
        assertFalse(assets().isEmpty(), "no asset was found to check");
    }

    @Test
    void covers_readsStarsAndBracesTheWayTheRegistrySays() {
        final Entry one = new Entry("a/*.png", "x", "x", "Datagen", "x");
        assertTrue(one.covers("a/b.png"));
        assertFalse(one.covers("a/b/c.png"), "one star stays in its folder");
        final Entry deep = new Entry("a/**/*.png", "x", "x", "Datagen", "x");
        assertTrue(deep.covers("a/b/c/d.png"));
        final Entry either = new Entry("a/{b,c}_x.png", "x", "x", "Datagen", "x");
        assertTrue(either.covers("a/c_x.png"));
        assertFalse(either.covers("a/d_x.png"));
        assertFalse(one.covers("a/b.pngx"), "a dot is a dot, not any character");
    }

    /** The asset files the mods ship, from the root of the repository, with forward slashes. */
    private static List<String> assets() {
        final Path root = root();
        final List<String> out = new ArrayList<>();
        for (final String module : MODULES) {
            final Path resources = root.resolve(module).resolve("src").resolve("main").resolve("resources");
            if (Files.isDirectory(resources)) {
                try (Stream<Path> top = Files.list(resources)) {
                    top.filter(Files::isRegularFile)
                            .map(path -> relative(root, path))
                            .filter(path -> ROOT_IMAGE.matcher(path.substring(path.lastIndexOf('/') + 1)).matches())
                            .forEach(out::add);
                } catch (final IOException e) {
                    throw new UncheckedIOException(e);
                }
            }
            out.addAll(filesUnder(root, resources.resolve("assets")));
            out.addAll(filesUnder(root, root.resolve(module).resolve("src").resolve("generated").resolve("resources")
                    .resolve("assets")));
        }
        out.addAll(filesUnder(root, root.resolve("docs").resolve("brand")));
        out.removeIf(path -> path.contains("/lang/"));
        return out;
    }

    /** Every file an entry could name, assets or code, so an entry pointing at a class is checked too. */
    private static List<String> everyFile() {
        final Path root = root();
        final List<String> out = new ArrayList<>();
        for (final String module : MODULES) {
            out.addAll(filesUnder(root, root.resolve(module).resolve("src")));
        }
        out.addAll(filesUnder(root, root.resolve("docs")));
        return out;
    }

    private static List<String> filesUnder(final Path root, final Path dir) {
        if (!Files.isDirectory(dir)) {
            return List.of();
        }
        try (Stream<Path> walk = Files.walk(dir)) {
            return walk.filter(Files::isRegularFile).map(path -> relative(root, path)).sorted().toList();
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static String relative(final Path root, final Path path) {
        return root.relativize(path).toString().replace('\\', '/');
    }

    private static Path root() {
        return Path.of("").toAbsolutePath().getParent();
    }

    /** The rows of the registry's tables whose first cell is a path, in the order they are written. */
    private static List<Entry> entries() {
        final String text;
        try {
            text = Files.readString(root().resolve(REGISTRY), StandardCharsets.UTF_8);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
        final List<Entry> out = new ArrayList<>();
        for (final String line : text.split("\r?\n")) {
            if (!line.startsWith("| `")) {
                continue;
            }
            final String[] cells = line.split("\\|", -1);
            if (cells.length < 7) {
                fail("an entry of " + REGISTRY + " does not have its five columns: " + line);
            }
            out.add(new Entry(cells[1].strip().replace("`", ""), cells[2].strip(), cells[3].strip(),
                    cells[4].strip(), cells[5].strip()));
        }
        return out;
    }

    /**
     * One row of the registry.
     *
     * @param path   the assets it covers, as a pattern: {@code *} within a name, {@code **} across folders, and
     *               {@code {a,b}} for either name
     * @param what   what they are
     * @param use    where and how they are used
     * @param origin where they came from
     * @param madeBy who made them, or how
     */
    private record Entry(String path, String what, String use, String origin, String madeBy) {

        boolean covers(final String file) {
            return pattern(this.path).matcher(file).matches();
        }

        private static Pattern pattern(final String glob) {
            final StringBuilder regex = new StringBuilder();
            for (int i = 0; i < glob.length(); i++) {
                final char c = glob.charAt(i);
                if (c == '*' && i + 1 < glob.length() && glob.charAt(i + 1) == '*') {
                    regex.append(".*");
                    i++;
                } else if (c == '*') {
                    regex.append("[^/]*");
                } else if (c == '?') {
                    regex.append("[^/]");
                } else if (c == '{') {
                    regex.append("(?:");
                } else if (c == '}') {
                    regex.append(')');
                } else if (c == ',') {
                    regex.append('|');
                } else {
                    regex.append(Pattern.quote(String.valueOf(c)));
                }
            }
            return Pattern.compile(regex.toString());
        }
    }
}
