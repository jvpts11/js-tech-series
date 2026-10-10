/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.os;

import static dev.jstech.computers.client.os.VirtualStudioLoader.join;

import dev.jstech.computers.audio.SystemSound;
import dev.jstech.computers.operation.payload.DeleteFilePayload;
import dev.jstech.computers.operation.payload.FolderContentPayload;
import dev.jstech.computers.operation.payload.MachineSoundPayload;
import dev.jstech.computers.operation.payload.RequestFolderContentPayload;
import dev.jstech.computers.os.edit.project.ProjectFile;
import dev.jstech.core.JsCore;
import dev.jstech.core.language.IProgrammingLanguage;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.Text;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * The Virtual Studio's build: the queue of projects to compile, the folders read for their sources, the Output
 * pane's lines and the complaints of the last build.
 *
 * <p>The machine answers one folder at a time, so a project is built by reading its folder and every library's,
 * one after the other, and compiling once the last has landed. Start builds the startup project and then runs the
 * listing at the terminal.
 */
final class VirtualStudioBuild {

    /** What a line of the Output pane reports, which is the colour it is drawn in. */
    enum Tone { PLAIN, SUCCEEDED, FAILED }

    /** One line of the Output pane: its words, and what it reports. */
    record OutputLine(Text text, Tone tone) {
    }

    private final BlockPos host;
    private final CodeWorkspace workspace;
    private final VirtualStudioLoader loader;
    private final CodeFileReplies.IReader reader;
    private final Runnable showOutput;
    private final Runnable showErrors;
    private final Consumer<List<String>> terminal;
    private final Deque<String> buildQueue = new ArrayDeque<>();
    private final Deque<String> foldersToRead = new ArrayDeque<>();
    private final Map<String, String> sourceTexts = new LinkedHashMap<>();
    private final Map<String, List<IProgrammingLanguage.Complaint>> buildErrors = new LinkedHashMap<>();
    private final List<OutputLine> output = new ArrayList<>();
    private String building = "";
    private boolean runAfterBuild;

    /**
     * Makes the build of one studio window.
     *
     * @param reader     the window that asks for the folders, so the answers find their way back to it
     * @param showOutput brings the Output pane to the front
     * @param showErrors brings the Error List to the front
     * @param terminal   types lines at the window's terminal, one after another as the machine answers each
     */
    VirtualStudioBuild(final BlockPos host, final CodeWorkspace workspace, final VirtualStudioLoader loader,
                       final CodeFileReplies.IReader reader, final Runnable showOutput, final Runnable showErrors,
                       final Consumer<List<String>> terminal) {
        this.host = host;
        this.workspace = workspace;
        this.loader = loader;
        this.reader = reader;
        this.showOutput = showOutput;
        this.showErrors = showErrors;
        this.terminal = terminal;
    }

    /** The lines of the Output pane. */
    List<OutputLine> output() {
        return this.output;
    }

    /** The complaints of the last build by file path, empty when it built clean or nothing was built. */
    Map<String, List<IProgrammingLanguage.Complaint>> errors() {
        return this.buildErrors;
    }

    /** Empties the Output pane and the complaints, as closing a solution does. */
    void clearResults() {
        this.output.clear();
        this.buildErrors.clear();
    }

    /** A line of the Output pane, as it is worded. */
    void print(final Text text) {
        print(text, Tone.PLAIN);
    }

    /** A line of the Output pane, in the colour of what it reports. */
    void print(final Text text, final Tone tone) {
        this.output.add(new OutputLine(text, tone));
    }

    /** Builds every project that makes a listing, in the order the solution lists them. */
    void buildSolution() {
        if (this.loader.solution() == null) {
            buildOpenFile();
            return;
        }
        clearResults();
        print(VirtualStudioTexts.BUILD_STARTED.with(this.loader.solution().name()));
        this.buildQueue.clear();
        for (final ProjectFile project : this.loader.projects().values()) {
            if (project.buildsAListing()) {
                this.buildQueue.add(project.name());
            }
        }
        this.showOutput.run();
        buildNext();
    }

    void buildProject(final String name) {
        clearResults();
        print(VirtualStudioTexts.BUILD_STARTED.with(name));
        this.buildQueue.clear();
        this.buildQueue.add(name);
        this.showOutput.run();
        buildNext();
    }

    /** Takes in the sources of a folder that was asked for, and goes on to the next folder or to compiling. */
    void onFolder(final FolderContentPayload folder) {
        for (final FolderContentPayload.WireFile file : folder.files()) {
            this.sourceTexts.put(file.path(), file.text());
        }
        readNextFolder();
    }

    /** Start: builds the startup project and, when it built, runs it at the terminal. */
    void startProgram() {
        if (this.loader.solution() == null) {
            buildOpenFile();
            final CodeWorkspace.Doc doc = this.workspace.current();
            if (doc != null && this.buildErrors.isEmpty()) {
                runListing(doc.path().replaceAll("\\.[^.]+$", "") + ".asm");
            }
            return;
        }
        final String startup = this.loader.startupName();
        if (startup.isEmpty()) {
            print(VirtualStudioTexts.NO_STARTUP.text());
            return;
        }
        this.runAfterBuild = true;
        buildProject(startup);
    }

    /** Clean: the listings every project built are deleted, and the tree stops showing them. */
    void cleanSolution() {
        this.output.clear();
        for (final ProjectFile project : this.loader.projects().values()) {
            if (project.buildsAListing()) {
                PacketDistributor.sendToServer(new DeleteFilePayload(this.host,
                        join(this.loader.projectDir(project.name()), project.entry())));
                print(VirtualStudioTexts.DELETED_OF.with(project.entry(), project.name()));
            }
        }
        FilesApps.diskChanged();
        this.workspace.refresh();
    }

    /** Package: the prompt's sgpack does it, in the project's folder, at the terminal. */
    void packageStartup() {
        final ProjectFile project = this.loader.projects().get(this.loader.startupName());
        if (project == null) {
            print(VirtualStudioTexts.NO_PACKAGE.text());
            return;
        }
        this.terminal.accept(List.of(
                "cd \\" + this.loader.projectDir(project.name()).replace('/', '\\'),
                "sgpack init " + project.name(),
                "sgpack build"));
    }

    /** With no solution, Build is the open file on its own, as the light editor does it. */
    private void buildOpenFile() {
        final CodeWorkspace.Doc doc = this.workspace.current();
        clearResults();
        if (doc == null) {
            print(VirtualStudioTexts.NOTHING_TO_BUILD.text());
            return;
        }
        final IProgrammingLanguage language = CodeWorkspace.languageOf(doc.path());
        if (language == null) {
            print(VirtualStudioTexts.NO_LANGUAGE_CLAIMS.with(doc.name()));
            return;
        }
        final IProgrammingLanguage.CompileResult result = language.compile(
                List.of(new IProgrammingLanguage.SourceText(doc.name(), doc.area().text())),
                InstalledCompilers.options(language, "", this.loader.projectVersionOf(doc.path())));
        finishBuild(doc.name(), doc.path().replaceAll("\\.[^.]+$", "") + ".asm", result,
                Map.of(doc.path(), doc.name()));
    }

    private void buildNext() {
        final String name = this.buildQueue.poll();
        if (name == null) {
            if (this.runAfterBuild) {
                this.runAfterBuild = false;
                runStartup();
            }
            return;
        }
        this.building = name;
        this.sourceTexts.clear();
        this.foldersToRead.clear();
        final Map<String, ProjectFile> projects = this.loader.projects();
        final ProjectFile project = projects.get(name);
        if (project == null) {
            buildNext();
            return;
        }
        // The project's own folder and every library it references, which may reference more.
        final Deque<String> pending = new ArrayDeque<>(List.of(name));
        final Set<String> seen = new LinkedHashSet<>();
        while (!pending.isEmpty()) {
            final String each = pending.poll();
            if (!seen.add(each) || !projects.containsKey(each)) {
                continue;
            }
            this.foldersToRead.add(this.loader.projectDir(each));
            pending.addAll(projects.get(each).references());
        }
        readNextFolder();
    }

    private void readNextFolder() {
        final String dir = this.foldersToRead.poll();
        if (dir != null) {
            CodeFileReplies.expectFolder(this.reader, dir);
            // Each folder is read for the sources of its own project, since a Σ# program may stand on a Σ library.
            PacketDistributor.sendToServer(new RequestFolderContentPayload(this.host, dir,
                    VirtualStudioLoader.sourceSuffix(this.loader.projectIn(dir))));
            return;
        }
        compileBuilding();
    }

    /** Compiles what was read for the project being built, and writes the listing when it built. */
    private void compileBuilding() {
        final Map<String, ProjectFile> projects = this.loader.projects();
        final ProjectFile project = projects.get(this.building);
        if (project == null) {
            buildNext();
            return;
        }
        final IProgrammingLanguage language = JsCore.languages().get(
                ResourceLocation.tryParse(project.language()));
        if (language == null) {
            print(VirtualStudioTexts.NO_LANGUAGE_CALLED.with(project.name(), project.language()));
            buildNext();
            return;
        }
        /*
         * What is open and changed counts over what the disk holds, so a build sees what the player
         * sees; a library's sources come in first, so its types are known when the project's are read.
         */
        final List<IProgrammingLanguage.SourceText> sources = new ArrayList<>();
        final Map<String, String> names = new LinkedHashMap<>();
        final Deque<String> order = new ArrayDeque<>();
        this.loader.collectOrder(project.name(), order, new LinkedHashSet<>());
        for (final String each : order) {
            final ProjectFile part = projects.get(each);
            for (final String source : part.sources()) {
                final String path = join(this.loader.projectDir(each), source);
                final String text = openText(path, this.sourceTexts.get(path));
                if (text != null) {
                    names.put(path, each + "/" + source);
                    sources.add(new IProgrammingLanguage.SourceText(each + "/" + source, text));
                }
            }
        }
        if (sources.isEmpty()) {
            print(VirtualStudioTexts.NO_SOURCES.with(project.name()));
            buildNext();
            return;
        }
        final IProgrammingLanguage.CompileResult result = language.compile(sources,
                InstalledCompilers.options(language, project.platform(), project.languageVersion()));
        finishBuild(project.name(), join(this.loader.projectDir(project.name()), project.entry()), result, names);
        buildNext();
    }

    /** The text of a source as the player sees it: the open buffer when there is one, else the disk's. */
    private String openText(final String path, final String fromDisk) {
        for (final CodeWorkspace.Doc doc : this.workspace.docs()) {
            if (doc.path().equals(path)) {
                return doc.area().text();
            }
        }
        return fromDisk;
    }

    private void finishBuild(final String what, final String outputPath,
                             final IProgrammingLanguage.CompileResult result,
                             final Map<String, String> names) {
        if (result.ok()) {
            if (!FileSaves.send(this.host, outputPath, result.binary())) {
                // A listing longer than a file holds is not written, and the build is not called a success.
                PacketDistributor.sendToServer(new MachineSoundPayload(this.host, SystemSound.ERROR));
                print(FileSaves.tooLong(result.binary()), Tone.FAILED);
                print(VirtualStudioTexts.BUILD_FAILED_NAMED.with(what, 1), Tone.FAILED);
                this.workspace.say(GameText.resolve(VirtualStudioTexts.BUILD_FAILED));
                this.buildQueue.clear();
                this.runAfterBuild = false;
                return;
            }
            final int lines = result.binary().split("\n", -1).length;
            print(VirtualStudioTexts.BUILT_LISTING.with(what, outputPath, lines));
            print(VirtualStudioTexts.BUILD_SUCCEEDED_NAMED.with(what), Tone.SUCCEEDED);
            FilesApps.diskChanged();
            this.workspace.say(GameText.resolve(VirtualStudioTexts.BUILD_SUCCEEDED));
            // The machine sounds the end of the build once, when the last project in line has built.
            if (this.buildQueue.isEmpty()) {
                PacketDistributor.sendToServer(new MachineSoundPayload(this.host, SystemSound.NOTIFY));
            }
            return;
        }
        PacketDistributor.sendToServer(new MachineSoundPayload(this.host, SystemSound.ERROR));
        for (final IProgrammingLanguage.Complaint complaint : result.complaints()) {
            print(VirtualStudioTexts.COMPLAINT.with(what, complaint.text()), Tone.FAILED);
            // The complaint names the source as the compiler saw it; the row needs the path on the disk.
            String path = complaint.file();
            for (final Map.Entry<String, String> entry : names.entrySet()) {
                if (entry.getValue().equals(complaint.file())) {
                    path = entry.getKey();
                }
            }
            this.buildErrors.computeIfAbsent(path, p -> new ArrayList<>()).add(complaint);
        }
        print(VirtualStudioTexts.BUILD_FAILED_NAMED.with(what, result.complaints().size()), Tone.FAILED);
        this.workspace.say(GameText.resolve(VirtualStudioTexts.BUILD_FAILED));
        this.showErrors.run();
        this.buildQueue.clear();
        this.runAfterBuild = false;
    }

    private void runStartup() {
        final ProjectFile project = this.loader.projects().get(this.loader.startupName());
        if (project != null && project.buildsAListing() && this.buildErrors.isEmpty()) {
            runListing(join(this.loader.projectDir(project.name()), project.entry()));
        }
    }

    /**
     * Runs a listing at the dock's terminal.
     *
     * <p>The terminal is a shell of its own and may have been moved anywhere; the listing is named
     * from the root so it is found wherever the shell stands.
     */
    private void runListing(final String path) {
        this.terminal.accept(List.of("cd \\", "sigma run \"" + path.replace('/', '\\') + "\""));
    }
}
