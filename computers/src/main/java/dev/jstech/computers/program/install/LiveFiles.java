/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.program.install;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * The files an installation by hand touches, and where the session is standing among them.
 *
 * <p>Not a filesystem: the few files a person really reads or writes while installing by hand, so that what
 * the steps wrote is what reading them shows, and what somebody wrote into the build options is what the
 * compile afterwards reads. The guide the medium carries is put here when the session starts and is never
 * saved, since it belongs to the medium and not to the installation.
 *
 * <p>Inside the new system a path is that system's own. {@code /etc/fstab} typed in there is the file the
 * step outside wrote under the mount point, because that is the same file seen from inside. It is the one
 * thing about a chroot that has to be true for any of the rest to make sense.
 */
final class LiveFiles {

    private final Map<String, String> files = new LinkedHashMap<>();
    private final Set<String> dirs = new LinkedHashSet<>();

    /** Where the disk being installed onto hangs while the session is outside it. */
    private final String root;
    private String cwd = HOME;
    private boolean inside;

    /** Where a live session starts, and what its prompt writes as a tilde. */
    static final String HOME = "/root";

    /** The guide the medium carries, which is the medium's and so is never written down with the rest. */
    private static final String GUIDE = HOME + "/install.txt";

    /**
     * @param root  where the new system's disk is mounted, which is this distribution's own choice of place
     * @param guide the walkthrough the medium carries in the root user's home
     */
    LiveFiles(final String root, final String guide) {
        this.root = root;
        this.dirs.add("/");
        this.dirs.add(HOME);
        this.dirs.add("/mnt");
        this.dirs.add(root);
        this.files.put(GUIDE, guide);
    }

    String root() {
        return this.root;
    }

    /** Where the session is standing, as somebody standing there would write it. */
    String cwd() {
        return this.cwd;
    }

    /** Where the session is standing as the prompt writes it, the root user's home being a tilde. */
    String cwdForPrompt() {
        return HOME.equals(this.cwd) ? "~" : this.cwd;
    }

    boolean inside() {
        return this.inside;
    }

    /** Steps into the new system, which drops the session at its root. */
    void enter() {
        this.inside = true;
        this.cwd = "/";
    }

    /** Steps back out onto the medium, where the session was standing before it went in. */
    void leave() {
        this.inside = false;
        this.cwd = HOME;
    }

    /** The whole path something typed names, from wherever the session is standing. */
    String resolve(final String typed) {
        final String path = typed.isEmpty() ? this.cwd : typed;
        final String whole = path.startsWith("/") ? path : (this.cwd.equals("/") ? "" : this.cwd) + "/" + path;
        final String tidy = tidy(whole);
        return this.inside ? tidy(this.root + tidy) : tidy;
    }

    /** How a whole path reads back to somebody standing where the session is. */
    String asTyped(final String whole) {
        if (!this.inside) {
            return whole;
        }
        return whole.equals(this.root) ? "/"
                : whole.startsWith(this.root + "/") ? whole.substring(this.root.length()) : whole;
    }

    /** A path inside the new system by its own name there, wherever the session is standing. */
    String inNewSystem(final String path) {
        return this.root + path;
    }

    boolean isFile(final String whole) {
        return this.files.containsKey(whole);
    }

    boolean isDir(final String whole) {
        return this.dirs.contains(whole);
    }

    /** What a file holds, or null when there is no such file. */
    String read(final String whole) {
        return this.files.get(whole);
    }

    /** Writes a file, making the directories above it. */
    void write(final String whole, final String content) {
        this.files.put(whole, content);
        String at = whole;
        while (at.lastIndexOf('/') > 0) {
            at = at.substring(0, at.lastIndexOf('/'));
            this.dirs.add(at);
        }
        this.dirs.add("/");
    }

    void makeDir(final String whole) {
        this.dirs.add(whole);
        String at = whole;
        while (at.lastIndexOf('/') > 0) {
            at = at.substring(0, at.lastIndexOf('/'));
            this.dirs.add(at);
        }
    }

    LiveTurn ls(final String arg) {
        final String whole = this.resolve(arg);
        if (this.files.containsKey(whole)) {
            return LiveTurn.said(this.asTyped(whole));
        }
        if (!this.dirs.contains(whole)) {
            return LiveTurn.refused("ls: cannot access '" + (arg.isEmpty() ? this.asTyped(this.cwd) : arg)
                    + "': No such file or directory");
        }
        final SortedSet<String> here = new TreeSet<>();
        final String prefix = whole.equals("/") ? "/" : whole + "/";
        for (final String dir : this.dirs) {
            if (dir.startsWith(prefix) && !dir.equals(whole)) {
                here.add(firstStep(dir.substring(prefix.length())));
            }
        }
        for (final String file : this.files.keySet()) {
            if (file.startsWith(prefix)) {
                here.add(firstStep(file.substring(prefix.length())));
            }
        }
        return here.isEmpty() ? LiveTurn.silent() : LiveTurn.said(String.join("  ", here));
    }

    LiveTurn cat(final String arg, final boolean paged) {
        if (arg.isEmpty()) {
            return LiveTurn.refused(paged ? "Usage: less <file>" : "Usage: cat <file>");
        }
        final String whole = this.resolve(arg);
        if (this.dirs.contains(whole)) {
            return LiveTurn.refused("cat: " + arg + ": Is a directory");
        }
        final String content = this.files.get(whole);
        if (content == null) {
            return LiveTurn.refused((paged ? arg + ": " : "cat: " + arg + ": ") + "No such file or directory");
        }
        final List<String> out = new ArrayList<>(List.of(content.split("\n", -1)));
        if (paged) {
            // The pager has nowhere to page to on a screen this size, so it says what a pager says at the end.
            out.add("(END)");
        }
        return LiveTurn.said(out.toArray(String[]::new));
    }

    LiveTurn cd(final String arg) {
        final String whole = this.resolve(arg.isEmpty() ? HOME : arg);
        if (this.files.containsKey(whole)) {
            return LiveTurn.refused("cd: " + arg + ": Not a directory");
        }
        if (!this.dirs.contains(whole)) {
            return LiveTurn.refused("cd: " + arg + ": No such file or directory");
        }
        this.cwd = this.asTyped(whole);
        return LiveTurn.silent();
    }

    /**
     * Says something, or puts it in a file.
     *
     * <p>How a line of configuration gets written without an editor, which is what anybody in a hurry does:
     * {@code echo 'MAKEOPTS="-j4"' >> /etc/portage/make.conf}.
     */
    LiveTurn echo(final String line) {
        final String said = line.length() > 4 ? line.substring(4).trim() : "";
        final int append = said.lastIndexOf(">>");
        final int over = append >= 0 ? -1 : said.lastIndexOf('>');
        if (append < 0 && over < 0) {
            return LiveTurn.said(unquote(said));
        }
        final int at = append >= 0 ? append : over;
        final String text = unquote(said.substring(0, at).trim());
        final String target = said.substring(at + (append >= 0 ? 2 : 1)).trim();
        if (target.isEmpty()) {
            return LiveTurn.refused("bash: syntax error near unexpected token `newline'");
        }
        final String whole = this.resolve(target);
        if (this.dirs.contains(whole)) {
            return LiveTurn.refused("bash: " + target + ": Is a directory");
        }
        final String had = append >= 0 ? this.files.get(whole) : null;
        this.write(whole, had == null || had.isEmpty() ? text : had + "\n" + text);
        return LiveTurn.silent();
    }

    /** Writes down everything the installation made, a named line each, into what the state is saved as. */
    void save(final LiveSaved out) {
        out.put("cwd", this.cwd);
        out.put("inside", this.inside);
        for (final String dir : this.dirs) {
            out.put("dir:" + dir, "1");
        }
        for (final Map.Entry<String, String> file : this.files.entrySet()) {
            if (!file.getKey().equals(GUIDE)) {
                out.put("file:" + file.getKey(), file.getValue());
            }
        }
    }

    void load(final LiveSaved saved) {
        this.cwd = saved.text("cwd", HOME);
        this.inside = saved.flag("inside");
        for (final Map.Entry<String, String> line : saved.all().entrySet()) {
            if (line.getKey().startsWith("dir:")) {
                this.dirs.add(line.getKey().substring(4));
            } else if (line.getKey().startsWith("file:")) {
                this.files.put(line.getKey().substring(5), line.getValue());
            }
        }
    }

    /** A path with its dots resolved and its trailing slash gone, so two ways of writing one agree. */
    private static String tidy(final String path) {
        final Deque<String> parts = new ArrayDeque<>();
        for (final String part : path.split("/")) {
            if (part.isEmpty() || part.equals(".")) {
                continue;
            }
            if (part.equals("..")) {
                parts.pollLast();
                continue;
            }
            parts.addLast(part);
        }
        return parts.isEmpty() ? "/" : "/" + String.join("/", parts);
    }

    /** The first step of a path under a directory, which is what that directory lists. */
    private static String firstStep(final String rest) {
        final int slash = rest.indexOf('/');
        return slash < 0 ? rest : rest.substring(0, slash);
    }

    private static String unquote(final String text) {
        if (text.length() >= 2) {
            final char first = text.charAt(0);
            if ((first == '\'' || first == '"') && text.charAt(text.length() - 1) == first) {
                return text.substring(1, text.length() - 1);
            }
        }
        return text;
    }
}
