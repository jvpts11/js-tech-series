/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.os;

import dev.jstech.computers.operation.payload.RequestFileContentPayload;
import dev.jstech.computers.os.edit.project.ProjectFile;
import dev.jstech.computers.os.edit.project.SolutionFile;
import dev.jstech.computers.sigma.LanguageLevel;
import dev.jstech.core.JsCore;
import dev.jstech.core.language.IProgrammingLanguage;
import dev.jstech.core.text.GameText;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * The solution the Virtual Studio has open: its file, the project files under it, and the folder they live in.
 *
 * <p>The machine answers one file at a time, so loading reads the solution file first and then each of its
 * projects in turn, and only when the last one has landed is the window told that the solution is open. This
 * class also keeps the studio's recent solutions and writes the solution and project files back.
 */
final class VirtualStudioLoader {

    private final BlockPos host;
    private final CodeWorkspace workspace;
    private final CodeFileReplies.IReader reader;
    /** Run when a solution or a plain folder is ready to be shown, which is when the window leaves its Start Window. */
    private final Runnable onOpened;
    private final Map<String, ProjectFile> projects = new LinkedHashMap<>();
    /** Project files still to be read, in order, since one file is waited for at a time. */
    private final Deque<String> loading = new ArrayDeque<>();
    /** The files to put on tabs once the solution being read is in. */
    private final List<String> openWhenLoaded = new ArrayList<>();
    private SolutionFile solution;
    private String solutionDir = "";
    /** The tab to end on once the files wait no longer. */
    private String currentWhenLoaded = "";

    private static final int RECENT_MAX = 5;

    /** The solutions opened on each machine lately, for the Start Window while the game runs. */
    private static final Map<BlockPos, Deque<String>> RECENT = new LinkedHashMap<>();

    VirtualStudioLoader(final BlockPos host, final CodeWorkspace workspace, final CodeFileReplies.IReader reader,
                        final Runnable onOpened) {
        this.host = host;
        this.workspace = workspace;
        this.reader = reader;
        this.onOpened = onOpened;
    }

    static String shortName(final String path) {
        final int slash = path.lastIndexOf('/');
        return slash >= 0 && slash < path.length() - 1 ? path.substring(slash + 1) : path;
    }

    static String join(final String dir, final String name) {
        return dir.isEmpty() ? name : dir + "/" + name;
    }

    /**
     * What a source of that project ends in, dot included: whatever the language it is written in says, which is
     * how a Σ project lists, adds and builds {@code .sg} files where a Σ# one does {@code .sgs}.
     */
    static String sourceSuffix(final ProjectFile project) {
        final IProgrammingLanguage language = project == null ? null
                : JsCore.languages().get(ResourceLocation.tryParse(project.language()));
        if (language == null || language.sourceExtensions().isEmpty()) {
            return "." + LanguageLevel.SIGMA_SHARP.sourceExtension();
        }
        return "." + language.sourceExtensions().iterator().next();
    }

    /** The open solution, or null on a plain folder or the Start Window. */
    SolutionFile solution() {
        return this.solution;
    }

    void setSolution(final SolutionFile value) {
        this.solution = value;
    }

    /** The folder the solution, or the plain folder, lives in. */
    String dir() {
        return this.solutionDir;
    }

    /** The projects of the open solution by name, in the order the solution lists them. */
    Map<String, ProjectFile> projects() {
        return this.projects;
    }

    /** The solutions opened lately on this machine, newest first. */
    List<String> recent() {
        return new ArrayList<>(RECENT.getOrDefault(this.host, new ArrayDeque<>()));
    }

    /** Opens the solution kept in {@code dir}: its file, then each project's, then the folders. */
    void openSolutionFolder(final String dir) {
        this.solutionDir = dir;
        this.solution = null;
        this.projects.clear();
        this.loading.clear();
        final String file = join(dir, SolutionFile.fileName(shortName(dir)));
        CodeFileReplies.expectContent(this.reader, file);
        PacketDistributor.sendToServer(new RequestFileContentPayload(this.host, file));
    }

    /**
     * Opens the solution in {@code dir} and, once it is in, puts {@code files} on tabs and ends on {@code current}.
     * The files wait for the solution because the machine answers one file at a time and the solution goes first.
     */
    void openSolution(final String dir, final List<String> files, final String current) {
        this.openWhenLoaded.clear();
        this.openWhenLoaded.addAll(files);
        this.currentWhenLoaded = current;
        openSolutionFolder(dir);
    }

    /** Opens a plain folder, with no solution around it: the files are the tree. */
    void openFolder(final String dir) {
        this.loading.clear();
        CodeFileReplies.forget(this.reader);
        this.solution = null;
        this.solutionDir = dir;
        this.projects.clear();
        this.workspace.setFolder(dir);
        this.onOpened.run();
        remember(dir);
    }

    /** Forgets the open solution, and any file still being waited for. */
    void close() {
        this.loading.clear();
        CodeFileReplies.forget(this.reader);
        this.solution = null;
        this.projects.clear();
        this.solutionDir = "";
    }

    /** Takes in a file the machine sent back; a solution file or a project file moves the loading along. */
    void onContent(final String path, final String content, final boolean exists) {
        if (path.endsWith("." + SolutionFile.EXTENSION)) {
            if (!exists) {
                this.workspace.say(GameText.resolve(VirtualStudioTexts.NO_SOLUTION_IN.with(
                        shortName(this.solutionDir))));
                openFolder(this.solutionDir);
                return;
            }
            this.solution = SolutionFile.read(content);
            for (final String project : this.solution.projects()) {
                this.loading.add(join(this.solutionDir, project));
            }
            readNextProject();
            return;
        }
        if (ProjectFile.isProjectFile(path)) {
            if (exists) {
                final ProjectFile project = ProjectFile.read(content);
                this.projects.put(project.name(), project);
            }
            readNextProject();
        }
    }

    String projectDir(final String name) {
        return join(this.solutionDir, name);
    }

    void saveSolution() {
        if (this.solution == null) {
            return;
        }
        saveFile(join(this.solutionDir, SolutionFile.fileName(this.solution.name())), this.solution.write());
    }

    void saveProject(final ProjectFile project) {
        this.projects.put(project.name(), project);
        saveFile(join(projectDir(project.name()), project.fileName()), project.write());
    }

    /** Sends one file of the solution to be saved, or tells the player it is too long to. */
    void saveFile(final String path, final String content) {
        if (FileSaves.send(this.host, path, content)) {
            FilesApps.diskChanged();
        } else {
            this.workspace.say(GameText.resolve(FileSaves.tooLong(content)));
        }
    }

    /** The project that lives in that folder, or null when the folder is nobody's. */
    ProjectFile projectIn(final String dir) {
        for (final ProjectFile project : this.projects.values()) {
            if (projectDir(project.name()).equals(dir)) {
                return project;
            }
        }
        return null;
    }

    /** The project whose folder holds {@code path}, or null when it is in none of the solution's. */
    String projectOf(final String path) {
        for (final String name : this.projects.keySet()) {
            if (path.startsWith(projectDir(name) + "/")) {
                return name;
            }
        }
        return null;
    }

    /** The version of its language the project holding {@code path} names, or 0 when it names none or is none. */
    int projectVersionOf(final String path) {
        final String own = projectOf(path);
        return own == null ? 0 : this.projects.get(own).languageVersion();
    }

    /** The project the solution starts: the one it names, else the first that builds a listing, else none. */
    String startupName() {
        if (this.solution == null) {
            return "";
        }
        if (!this.solution.startup().isEmpty()) {
            return this.solution.startup();
        }
        for (final ProjectFile project : this.projects.values()) {
            if (project.buildsAListing()) {
                return project.name();
            }
        }
        return "";
    }

    /** The order to read a project's parts in: its libraries first, itself last. */
    void collectOrder(final String name, final Deque<String> order, final Set<String> seen) {
        if (!seen.add(name) || !this.projects.containsKey(name)) {
            return;
        }
        for (final String reference : this.projects.get(name).references()) {
            collectOrder(reference, order, seen);
        }
        order.add(name);
    }

    private void readNextProject() {
        final String next = this.loading.poll();
        if (next != null) {
            CodeFileReplies.expectContent(this.reader, next);
            PacketDistributor.sendToServer(new RequestFileContentPayload(this.host, next));
            return;
        }
        if (this.solution == null) {
            // The solution was closed or replaced while a late reply was still on its way.
            return;
        }
        // Everything is read: the tree can be built, and the folders are asked for their outputs.
        this.workspace.setFolder(this.solutionDir);
        for (final String name : this.projects.keySet()) {
            this.workspace.toggleFolder(join(join(this.solutionDir, name), "build"));
        }
        this.onOpened.run();
        remember(this.solutionDir);
        this.workspace.say(GameText.resolve(EditorTexts.OPENED.with(this.solution.name())));
        if (!this.openWhenLoaded.isEmpty()) {
            final List<String> paths = new ArrayList<>(this.openWhenLoaded);
            this.openWhenLoaded.clear();
            this.workspace.openAll(paths, this.currentWhenLoaded);
            this.currentWhenLoaded = "";
        }
    }

    private void remember(final String dir) {
        final Deque<String> recent = RECENT.computeIfAbsent(this.host, h -> new ArrayDeque<>());
        recent.remove(dir);
        recent.addFirst(dir);
        while (recent.size() > RECENT_MAX) {
            recent.removeLast();
        }
    }
}
