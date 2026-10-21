/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.developer.mcp;

import dev.jstech.core.text.Text;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * The only files an agent reads or writes: inside the folders given, a mod's own resources or a pack's, named as
 * {@code <folder>/<path>}, never one step outside them, not through a link either, and only of the kinds that are data,
 * art or sound, never code: a script written there would be code run from outside.
 */
public final class McpFiles {

    /** The kinds of file read and written, by their ending, and how large one may be. */
    public static final Set<String> KINDS = Set.of("json", "mcmeta", "png", "nbt", "txt", "md", "ogg", "gltf", "glb",
            "bin");
    public static final long MOST = 4L * 1024 * 1024;
    /** The kinds that are text, read and written as such; the others go as Base64. */
    public static final Set<String> TEXT = Set.of("json", "mcmeta", "txt", "md", "gltf");

    private McpFiles() {
    }

    /**
     * The file {@code path} names among {@code folders}, or why it is not one an agent may reach: a folder that is not
     * given, a step outside it, a link out of it, or a kind of file that is not read or written.
     */
    public static Found find(final List<Folder> folders, final String path, final boolean file) {
        final String given = path.replace('\\', '/').strip();
        final int slash = given.indexOf('/');
        final String name = slash < 0 ? given : given.substring(0, slash);
        final String rest = slash < 0 ? "" : given.substring(slash + 1);
        final Optional<Folder> folder = folders.stream().filter(one -> one.name().equals(name)).findFirst();
        if (folder.isEmpty()) {
            return Found.not(McpWords.FOLDER_UNKNOWN.with(name, folders.stream().map(Folder::name)
                    .collect(Collectors.joining(", "))));
        }
        if (rest.startsWith("/") || rest.contains(":") || List.of(rest.split("/")).contains("..")) {
            return Found.not(McpWords.FILE_OUTSIDE.with(path));
        }
        final Path root = folder.get().root().toAbsolutePath().normalize();
        final Path target = root.resolve(rest).normalize();
        if (!target.startsWith(root)) {
            return Found.not(McpWords.FILE_OUTSIDE.with(path));
        }
        if (!inside(root, target)) {
            return Found.not(McpWords.FILE_OUTSIDE.with(path));
        }
        if (file) {
            final String kind = kindOf(target);
            if (!KINDS.contains(kind)) {
                return Found.not(McpWords.FILE_KIND.with(kind.isEmpty() ? "" : "." + kind));
            }
        }
        return new Found(Optional.of(target), Optional.empty());
    }

    /** The ending of {@code file}'s name, in small letters, or none. */
    public static String kindOf(final Path file) {
        final Path name = file.getFileName();
        if (name == null) {
            return "";
        }
        final String text = name.toString();
        final int dot = text.lastIndexOf('.');
        return dot < 0 ? "" : text.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    /*
     * Whether {@code target}, as the disk has it after every link is followed, is still inside {@code root}: the
     * nearest part of it that is there is looked at, so a file not written yet is judged by its folder.
     */
    private static boolean inside(final Path root, final Path target) {
        try {
            final Path realRoot = root.toRealPath();
            Path there = target;
            while (there != null && !Files.exists(there, LinkOption.NOFOLLOW_LINKS)) {
                there = there.getParent();
            }
            return there != null && there.toRealPath().startsWith(realRoot);
        } catch (final IOException unreadable) {
            return false;
        }
    }

    /**
     * A folder an agent may reach.
     *
     * @param name what it is called in a path, such as {@code mod:mymod}
     * @param root where it is
     */
    public record Folder(String name, Path root) {
    }

    /**
     * A file found, or why it was not.
     *
     * @param path the file
     * @param why  why there is none
     */
    public record Found(Optional<Path> path, Optional<Text> why) {

        static Found not(final Text why) {
            return new Found(Optional.empty(), Optional.of(why));
        }
    }
}
