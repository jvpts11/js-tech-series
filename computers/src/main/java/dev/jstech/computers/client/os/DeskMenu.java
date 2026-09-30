/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.os;

import dev.jstech.computers.JsComputers;
import dev.jstech.computers.operation.payload.DesktopShellRunPayload;
import dev.jstech.computers.operation.payload.DiskFilesPayload;
import dev.jstech.computers.os.ProgramSpec;
import dev.jstech.computers.os.WindowKeys;
import dev.jstech.computers.os.fs.Archive;
import dev.jstech.computers.os.fs.FileOpeners;
import dev.jstech.computers.os.fs.FileType;
import dev.jstech.computers.program.Programs;
import dev.jstech.core.client.gui.component.ContextMenu;
import dev.jstech.core.client.gui.component.UiContext;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.TextKey;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

/**
 * The menu the right button opens on the wallpaper, for whatever the cursor is on: a program, a file or folder, or
 * the wallpaper itself.
 *
 * <p>Each gets the entries that mean something for it, which is why a program does not offer to be renamed and the
 * wallpaper does not offer to be opened. The entries are the ones a desktop has: a file opens, opens with a chosen
 * program, is renamed, deleted or looked at; the wallpaper makes new things, refreshes, and leads to the settings that
 * dress it.
 */
final class DeskMenu {

    private final DesktopScreen desktop;
    /** The menu itself, the same component every program's menus are. */
    private final ContextMenu menu;

    /** The height of a row on the desktop's menus: this one, the panel's and a program's alike. */
    static final int ITEM_H = 11;
    private static final int W = 88;
    /** The archiver, by the id the desktop knows it under; nothing of its is offered without it installed. */
    private static final String ARCHIVER = "ark";
    private static final String THIS_PC_KEY =
            WindowKeys.of(ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, "this_pc"));

    DeskMenu(final DesktopScreen desktop) {
        this.desktop = desktop;
        this.menu = new ContextMenu(W, ITEM_H);
    }

    /** One entry of a desktop menu, labelled in the player's language. */
    static ContextMenu.Item item(final TextKey label, final boolean enabled, final Runnable action) {
        return new ContextMenu.Item(GameText.resolve(label), enabled, action);
    }

    boolean isOpen() {
        return menu.isOpen();
    }

    /** Opens the menu at a desktop-local point for the icon in {@code slot}, or for the wallpaper when none. */
    void openFor(final int slot, final int x, final int y) {
        final List<ContextMenu.Item> entries = new ArrayList<>();
        final List<Launcher> icons = desktop.deskIcons();
        final List<DiskFilesPayload.WireFile> files = desktop.deskFiles();
        if (desktop.isTrashIcon(slot)) {
            entries.addAll(desktop.trash().menu());
        } else if (slot >= 0 && slot < icons.size()) {
            addProgramItems(entries, icons.get(slot));
        } else if (slot >= icons.size() && slot - icons.size() < files.size()) {
            addFileItems(entries, files.get(slot - icons.size()), slot - icons.size(), slot);
        } else {
            entries.add(ContextMenu.Item.submenu(GameText.resolve(DesktopTexts.NEW), newItems()));
            entries.add(ContextMenu.Item.separator());
            entries.add(item(DesktopTexts.REFRESH, true, desktop::requestDesktop));
            entries.add(ContextMenu.Item.separator());
            entries.add(item(DesktopTexts.DISPLAY_SETTINGS, true,
                    () -> desktop.openSettingsPage(SettingsApp.PAGE_DISPLAY)));
            entries.add(item(DesktopTexts.PERSONALIZE, true,
                    () -> desktop.openSettingsPage(SettingsApp.PAGE_PERSONALIZE)));
            entries.add(ContextMenu.Item.separator());
            entries.add(item(DesktopTexts.PROPERTIES, true, () -> desktop.runLauncherKeyed(THIS_PC_KEY)));
        }
        menu.open(entries, x, y, 0, 0, desktop.view().width(), desktop.view().height());
    }

    void render(final GuiGraphics g, final UiContext ctx) {
        menu.render(g, ctx);
    }

    /** A click while the menu is up, which it takes whatever it lands on, closing on it. */
    void mouseClicked(final double mouseX, final double mouseY, final int button) {
        menu.mouseClicked(mouseX, mouseY, button);
    }

    /** A key while the menu is up: the arrows walk it and Escape leaves it. Returns whether it took the key. */
    boolean keyPressed(final int key, final int scanCode, final int modifiers) {
        return menu.keyPressed(key, scanCode, modifiers);
    }

    /** The desktop-local centre of item {@code index}, where a test clicks it. */
    int[] itemCenter(final int index) {
        return menu.itemCenter(index);
    }

    /** The labels of the items, in order, so a test finds one by name. */
    List<String> labels() {
        final List<String> out = new ArrayList<>();
        for (final ContextMenu.Item entry : menu.items()) {
            out.add(entry.label());
        }
        return out;
    }

    /** The desktop-local centre of item {@code index} of the menu open beside this one, or null when none is. */
    @Nullable
    int[] submenuItemCenter(final int index) {
        final ContextMenu sub = menu.openSubmenu();
        return sub == null ? null : sub.itemCenter(index);
    }

    /**
     * A program's entries: open it, pin it, and take it off the machine. Only what the machine could actually take
     * off is offered: the programs that ship with a system are part of it, so offering to remove one would be
     * offering something that then fails.
     */
    private void addProgramItems(final List<ContextMenu.Item> entries, final Launcher launcher) {
        entries.add(item(DesktopTexts.OPEN, true, () -> desktop.runLauncher(launcher)));
        final TaskbarModel taskbar = desktop.taskbar();
        if (taskbar.pinsOnPanel() && launcher.factory() != null) {
            final boolean pinned = taskbar.isPinned(launcher.programId().getPath());
            entries.add(item(pinned ? DesktopTexts.UNPIN : DesktopTexts.PIN, true,
                    () -> taskbar.togglePin(launcher.key())));
        }
        final ProgramSpec spec = Programs.get(launcher.programId());
        if (spec != null && spec.installable()) {
            entries.add(ContextMenu.Item.separator());
            entries.add(item(DesktopTexts.UNINSTALL, true, () -> uninstall(spec)));
        }
    }

    /** A file's or folder's entries, {@code index} being its place among the desktop's files. */
    private void addFileItems(final List<ContextMenu.Item> entries, final DiskFilesPayload.WireFile file,
                              final int index, final int slot) {
        final DeskFiles actions = desktop.fileActions();
        entries.add(item(DesktopTexts.OPEN, true, () -> desktop.openSlot(slot)));
        if (!file.directory()) {
            entries.add(ContextMenu.Item.submenu(GameText.resolve(DesktopTexts.OPEN_WITH), openWithItems(file.path())));
        }
        addArchiveItems(entries, file, index);
        entries.add(ContextMenu.Item.separator());
        /*
         * A projection of what a drive holds is not a file anybody wrote, so it cannot be renamed or deleted by hand;
         * the filesystem refuses both, and a menu that offered them would be lying.
         */
        entries.add(item(DesktopTexts.RENAME, !file.readOnly(), () -> actions.startRename(index)));
        entries.add(item(DesktopTexts.DELETE, !file.readOnly(), () -> actions.delete(index)));
        entries.add(ContextMenu.Item.separator());
        entries.add(item(DesktopTexts.PROPERTIES, true, () -> DesktopScreen.requestFileProperties(file.path())));
    }

    /**
     * What the archiver offers on a desktop icon, when the machine has it installed.
     *
     * <p>The same two entries the explorer offers, because the desktop is a folder like any other and a menu that
     * changed depending on which window a file was looked at through would be the odd one.
     */
    private void addArchiveItems(final List<ContextMenu.Item> entries, final DiskFilesPayload.WireFile file,
                                 final int index) {
        if (!desktop.installedPrograms().contains(ARCHIVER) || file.projectsItem()) {
            return;
        }
        final DeskFiles actions = desktop.fileActions();
        entries.add(ContextMenu.Item.separator());
        if (Archive.EXTENSION.equalsIgnoreCase(file.ext())) {
            entries.add(item(DesktopTexts.EXTRACT_HERE, true, () -> actions.extractHere(index)));
            return;
        }
        entries.add(new ContextMenu.Item(
                GameText.resolve(DesktopTexts.COMPRESS_TO.with(Archive.leaf(archiveNameOf(file.path())))), true,
                () -> actions.compress(index)));
    }

    /**
     * Offers the programs on this machine that can open the file, so the player picks one.
     *
     * <p>A menu rather than a dialog: it is the same question as the one that was just asked with the right button,
     * and the answer is one of a handful of names.
     */
    private List<ContextMenu.Item> openWithItems(final String path) {
        final List<ContextMenu.Item> entries = new ArrayList<>();
        final List<String> installed = desktop.installedPrograms();
        for (final String programId : FileOpeners.available(path, installed)) {
            final ProgramSpec spec = Programs.get(ResourceLocation.fromNamespaceAndPath("jsc", programId));
            final String label = spec == null ? programId : spec.displayName();
            entries.add(new ContextMenu.Item(label, true, () -> desktop.openIn(programId, path)));
        }
        if (!FileOpeners.choices(path, installed).isEmpty()) {
            if (!entries.isEmpty()) {
                entries.add(ContextMenu.Item.separator());
            }
            entries.add(item(DesktopTexts.CHOOSE_ANOTHER, true, () -> desktop.chooseOpener(path)));
        }
        if (entries.isEmpty()) {
            entries.add(item(DesktopTexts.NO_PROGRAM_OPENS_THIS, false, () -> { }));
        }
        return entries;
    }

    /** What New offers on the desktop: a folder first, then a file of every kind the machine can create. */
    private List<ContextMenu.Item> newItems() {
        final DeskFiles actions = desktop.fileActions();
        final List<ContextMenu.Item> entries = new ArrayList<>();
        entries.add(item(DesktopTexts.FOLDER, true, actions::newFolder));
        entries.add(ContextMenu.Item.separator());
        for (final FileType type : FileOpeners.creatable()) {
            entries.add(new ContextMenu.Item(FilesApp.typeLabel(type) + " (." + type.extension() + ")", true,
                    () -> actions.newFile(type)));
        }
        return entries;
    }

    /** Takes a program off this computer, the way {@code uninstall} at the prompt does. */
    private void uninstall(final ProgramSpec spec) {
        PacketDistributor.sendToServer(
                new DesktopShellRunPayload(desktop.hostPos(), "uninstall " + spec.commandName()));
    }

    /** The name the archive of a thing would take, for the menu entry that offers to make it. */
    private static String archiveNameOf(final String path) {
        final String leaf = Archive.leaf(path);
        final int dot = leaf.lastIndexOf('.');
        return (dot > 0 ? leaf.substring(0, dot) : leaf) + "." + Archive.EXTENSION;
    }
}
