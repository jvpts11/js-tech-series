/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.os;

import dev.jstech.computers.operation.payload.ArchiveFilesPayload;
import dev.jstech.computers.operation.payload.DiskFilesPayload;
import dev.jstech.computers.operation.payload.ExtractArchivePayload;
import dev.jstech.computers.operation.payload.MkdirPayload;
import dev.jstech.computers.operation.payload.RenameFilePayload;
import dev.jstech.computers.operation.payload.SaveFilePayload;
import dev.jstech.computers.os.fs.Archive;
import dev.jstech.computers.os.fs.FileType;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.Text;
import java.util.List;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

/**
 * Making, renaming and deleting the things that live on the desktop.
 *
 * <p>The desktop folder is a folder like any other, so all four of these are ordinary file operations sent
 * to the machine. What makes them worth keeping together is the renaming, which is not one step: a name is
 * typed in place under the icon, over several frames, while the rest of the desktop carries on around it.
 *
 * <p>Making something and naming it is the same act, so a new file or folder arrives already being renamed.
 * The name cannot be typed into until the machine has sent the folder back with the file in it, so the wanted
 * name is remembered and the rename begins when that listing arrives.
 *
 * <p>The whole name is edited, extension included. The extension is what decides which program opens a file,
 * and keeping it out of reach left a text file that should have been a program with no way to become one.
 */
final class DeskFiles implements CodeFileReplies.IReader {

    /** The desktop file being renamed in place, as an index into the desktop's own listing, or -1. */
    private int renaming = -1;

    /** What has been typed so far. */
    private final StringBuilder typed = new StringBuilder();

    /** A name to begin renaming as soon as the listing carrying it arrives, for a file just created. */
    @Nullable
    private String pending;

    /** How long a name may run, which is the cap the rename typing is held to. */
    private static final int MAX_NAME = 64;

    private final DesktopScreen desktop;

    DeskFiles(final DesktopScreen desktop) {
        this.desktop = desktop;
    }

    /** The desktop file being renamed, or -1 while none is. */
    int renaming() {
        return renaming;
    }

    /** What has been typed into the name so far. */
    String typedName() {
        return typed.toString();
    }

    /** Whether a name is being typed right now, which is what makes the desktop take keys for it. */
    boolean isRenaming() {
        return renaming >= 0;
    }

    /** Begins renaming the desktop file at {@code idx}, with its whole current name to edit. */
    void startRename(final int idx) {
        final var files = desktop.deskFiles();
        if (idx < 0 || idx >= files.size()) {
            return;
        }
        final DiskFilesPayload.WireFile f = files.get(idx);
        if (f.readOnly()) {
            desktop.showLocked();
            return;
        }
        renaming = idx;
        desktop.pickIcon(desktop.deskIcons().size() + idx);
        typed.setLength(0);
        typed.append(DesktopIcons.baseName(f.path()));
    }

    /** Ends a rename, sending it only when the name is neither empty nor the one the file already had. */
    void commitRename() {
        final var files = desktop.deskFiles();
        if (renaming >= 0 && renaming < files.size()) {
            final DiskFilesPayload.WireFile f = files.get(renaming);
            final String oldPath = f.path();
            final String newName = typed.toString().trim();
            final String newPath = desktop.deskDir() + "/" + newName;
            if (!newName.isEmpty() && !newPath.equals(oldPath)) {
                PacketDistributor.sendToServer(new RenameFilePayload(desktop.hostPos(), oldPath, newPath));
                FilesApps.diskChanged();
            }
        }
        renaming = -1;
    }

    /** Gives up on a rename, leaving the file with the name it had. */
    void cancelRename() {
        renaming = -1;
    }

    /**
     * One character typed into the name. Answers false once the name is as long as a name may be, so the
     * key goes on to whatever would have had it, rather than being swallowed by a field that cannot take it.
     */
    boolean type(final char c) {
        if (typed.length() >= MAX_NAME) {
            return false;
        }
        typed.append(c);
        return true;
    }

    /** Takes back the last character typed. */
    void backspace() {
        if (typed.length() > 0) {
            typed.deleteCharAt(typed.length() - 1);
        }
    }

    /**
     * Deletes the desktop file at {@code idx}, which puts it in the trash. A read-only projection of stored items
     * cannot be deleted.
     */
    void delete(final int idx) {
        final var files = desktop.deskFiles();
        if (idx < 0 || idx >= files.size()) {
            return;
        }
        final DiskFilesPayload.WireFile f = files.get(idx);
        if (f.readOnly()) {
            desktop.showLocked();
            return;
        }
        DeskTrash.delete(desktop.hostPos(), List.of(f.path()));
    }

    /**
     * Packs the desktop file at {@code idx} into an archive of its own name, beside it.
     *
     * <p>Here as well as in the explorer because the desktop is a folder like any other and a player who
     * keeps their work on it should not have to open a window to tidy it away.
     */
    void compress(final int idx) {
        final DiskFilesPayload.WireFile file = fileAt(idx);
        if (file == null) {
            return;
        }
        CodeFileReplies.expectSaved(this);
        PacketDistributor.sendToServer(new ArchiveFilesPayload(desktop.hostPos(),
                archiveNameFor(file.path()), List.of(file.path()), false));
    }

    /** Takes everything out of the desktop archive at {@code idx}, onto the desktop beside it. */
    void extractHere(final int idx) {
        final DiskFilesPayload.WireFile file = fileAt(idx);
        if (file == null) {
            return;
        }
        CodeFileReplies.expectSaved(this);
        PacketDistributor.sendToServer(new ExtractArchivePayload(desktop.hostPos(), file.path(), "",
                desktop.deskDir()));
    }

    @Override
    public void onSaved(final boolean ok, final Text message) {
        desktop.showBalloon(ok ? "67ark" : GameText.resolve(FilesTexts.COULD_NOT), GameText.resolve(message));
        FilesApps.diskChanged();
    }

    @Nullable
    private DiskFilesPayload.WireFile fileAt(final int idx) {
        final var files = desktop.deskFiles();
        return idx < 0 || idx >= files.size() ? null : files.get(idx);
    }

    /** The archive a thing is packed into: its own name with the archive's extension, beside it. */
    private static String archiveNameFor(final String path) {
        final String leaf = Archive.leaf(path);
        final int dot = leaf.lastIndexOf('.');
        final String stem = dot > 0 ? leaf.substring(0, dot) : leaf;
        final int slash = path.lastIndexOf('/');
        return (slash > 0 ? path.substring(0, slash + 1) : "") + stem + "." + Archive.EXTENSION;
    }

    /**
     * Makes an empty file of that kind, and puts the cursor in its name.
     *
     * <p>The kind is chosen before the file exists, because the extension is what decides which program
     * opens it and a file created as text and renamed afterwards is a rename the player should not have
     * had to do.
     */
    void newFile(final FileType type) {
        final String name = uniqueName("New File", "." + type.extension());
        pending = name;
        PacketDistributor.sendToServer(new SaveFilePayload(desktop.hostPos(), desktop.deskDir() + "/" + name, ""));
        FilesApps.diskChanged();
    }

    void newFolder() {
        final String name = uniqueName("New Folder", "");
        pending = name;
        PacketDistributor.sendToServer(new MkdirPayload(desktop.hostPos(), desktop.deskDir() + "/" + name));
        FilesApps.diskChanged();
    }

    /**
     * Begins renaming whatever was just created, once the listing carrying it has arrived. Called with the
     * fresh listing, and does nothing unless something is waiting to be named.
     */
    void takePendingRename() {
        if (pending == null) {
            return;
        }
        final var files = desktop.deskFiles();
        for (int i = 0; i < files.size(); i++) {
            if (DesktopIcons.baseName(files.get(i).path()).equals(pending)) {
                startRename(i);
                break;
            }
        }
        pending = null;
    }

    /** A name nothing on the desktop is using: the wanted one, or it with a number after it. */
    private String uniqueName(final String base, final String ext) {
        if (!nameExists(base + ext)) {
            return base + ext;
        }
        int n = 2;
        while (nameExists(base + " (" + n + ")" + ext)) {
            n++;
        }
        return base + " (" + n + ")" + ext;
    }

    private boolean nameExists(final String name) {
        for (final DiskFilesPayload.WireFile f : desktop.deskFiles()) {
            if (DesktopIcons.baseName(f.path()).equals(name)) {
                return true;
            }
        }
        return false;
    }
}
