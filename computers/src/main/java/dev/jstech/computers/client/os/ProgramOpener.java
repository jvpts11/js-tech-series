/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.os;

import dev.jstech.computers.JsComputers;
import dev.jstech.computers.operation.payload.SetSettingPayload;
import dev.jstech.computers.operation.payload.UiWindowPayload;
import dev.jstech.computers.os.CdeAppGroup;
import dev.jstech.computers.os.OsRegistry;
import dev.jstech.computers.os.PanelStyle;
import dev.jstech.computers.os.ProgramSpec;
import dev.jstech.computers.os.WindowKeys;
import dev.jstech.computers.os.fs.FileOpeners;
import dev.jstech.computers.os.fs.FsPaths;
import dev.jstech.computers.program.Programs;
import dev.jstech.core.JsCore;
import dev.jstech.core.text.GameText;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

/**
 * How the desktop opens things: a program from its launcher, its key or its name, a file in the program that opens
 * its kind or in one the player picked, a compiled program at the terminal, and the windows the machine's own
 * programs have open. Which program a kind of file belongs to is one answer, kept here, so a double-click, a pick
 * from Open with and a run from the file manager all reach the same one. A window only opens when the machine's
 * memory can hold it.
 */
final class ProgramOpener {

    private final DesktopState desktop;
    private final BlockPos host;
    private final BlockPos monitorPos;
    private final ResourceLocation desktopId;
    /**
     * The program chosen with Always for each extension on this machine, as the desktop listing brings it. A choice
     * made here goes in at once, before the server has sent the listing again.
     */
    private final Map<String, String> defaultApps = new HashMap<>();

    /** The windows the desktop opens by itself, whatever this desktop calls the programs they belong to. */
    static final String FILES_KEY = WindowKeys.of(Programs.FILES);
    private static final String SETTINGS_KEY = WindowKeys.of(Programs.SETTINGS);
    private static final String EDITOR_KEY =
            WindowKeys.of(ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, FileOpeners.EDITOR));

    ProgramOpener(final DesktopState desktop, final BlockPos host, final BlockPos monitorPos,
                  final ResourceLocation desktopId) {
        this.desktop = desktop;
        this.host = host;
        this.monitorPos = monitorPos;
        this.desktopId = desktopId;
    }

    /** What a program is called, for a menu that offers it by id. */
    static String openerName(final String programId) {
        if (programId.equals(FileOpeners.EDITOR)) {
            return GameText.resolve(DesktopTexts.EDITOR);
        }
        final ProgramSpec spec = Programs.get(ResourceLocation.fromNamespaceAndPath("jsc", programId));
        return spec == null ? programId : GameText.resolve(spec.name());
    }

    /** Takes the programs chosen with Always, as the machine's listing brings them. */
    void takeDefaults(final Map<String, String> chosen) {
        defaultApps.clear();
        defaultApps.putAll(chosen);
    }

    /** Carries out one request to open something. */
    void open(final OpenRequest request) {
        switch (request) {
            case OpenRequest.FilesAt at -> {
                if (desktop.memory().allowOpen(FILES_KEY)) {
                    openFolder(at.dir());
                }
            }
            case OpenRequest.RunAtTerminal run -> runAtTerminal(run.path());
            case OpenRequest.TypeAtTerminal typed -> typeAtTerminal(typed.lines());
            case OpenRequest.OpenFile file -> {
                if (file.programId().isEmpty()) {
                    openFile(file.path());
                } else {
                    openIn(file.programId(), file.path());
                }
            }
            case OpenRequest.ChooseOpener choose -> chooseOpener(choose.path());
            case OpenRequest.Properties properties -> {
                // A desktop icon's Properties: the file manager on the desktop's folder shows the window.
                if (desktop.memory().allowOpen(FILES_KEY)) {
                    final FilesApp files = new FilesApp(host, desktopId.getPath(), desktop.deskDir(), monitorPos);
                    files.showPropertiesFor(FsPaths.fileName(properties.path()));
                    desktop.wm().open(FILES_KEY, files);
                }
            }
            case OpenRequest.Program program -> {
                // By its window key, or by the name a player typed for it at a shell.
                final String key = desktop.keyFor(program.key());
                final IDesktopApp app = factoryFor(key);
                if (app != null && desktop.memory().allowOpen(key)) {
                    desktop.wm().open(key, app);
                }
            }
        }
    }

    /** Opens, redraws or takes away a window one of the machine's Σ# programs has. */
    void acceptProgramWindow(final UiWindowPayload payload) {
        if (!payload.hostPos().equals(host)) {
            return;
        }
        final String key = SigmaWindowApp.keyFor(payload.program(), payload.window());
        final List<DesktopWindow> windows = desktop.wm().all();
        for (final DesktopWindow open : windows) {
            if (open.appKey().equals(key) && open.app() instanceof SigmaWindowApp app) {
                if (payload.open()) {
                    app.accept(payload);
                } else {
                    // The program closed it: the window goes without telling the program again.
                    windows.remove(open);
                }
                return;
            }
        }
        if (payload.open()) {
            desktop.wm().open(key, new SigmaWindowApp(host, payload));
        }
    }

    /** Starts a launcher: a program with a window opens it; a player's own program runs at the terminal. */
    void run(final Launcher launcher) {
        if (!launcher.runs().isEmpty()) {
            DesktopRequests.open(new OpenRequest.RunAtTerminal(launcher.runs()));
            return;
        }
        if (desktop.memory().allowOpen(launcher.key())) {
            desktop.wm().open(launcher.key(), launcher.factory().get());
        }
    }

    /** Runs the launcher known by {@code key}, when the desktop has one. */
    void runKeyed(final String key) {
        final Launcher launcher = desktop.catalogue().byKey(key);
        if (launcher != null) {
            run(launcher);
        }
    }

    /** Opens that program's window on this desktop, if this machine has it at all. */
    void startProgram(final String path) {
        final Launcher launcher =
                desktop.catalogue().find(l -> l.programId().getPath().equals(path) && l.factory() != null);
        if (launcher != null) {
            desktop.wm().open(launcher.key(), launcher.factory().get());
        }
    }

    /**
     * Makes a program's window from its key, for restoring a session and for opening by key. The welcome, CDE's
     * Application Manager and the trash have no launcher of their own; a program the desktop shows no launcher for
     * (the Task Manager, or one a file opens in) still opens, and still comes back with the session, by the id its
     * window goes by. Null when nothing answers to the key.
     */
    @Nullable
    IDesktopApp factoryFor(final String key) {
        if (WelcomeApp.KEY.equals(key)) {
            return new WelcomeApp(host);
        }
        if (desktop.panelStyle() == PanelStyle.CDE && ApplicationManagerApp.owns(key)) {
            return new ApplicationManagerApp(ApplicationManagerApp.groupOf(key));
        }
        if (WindowKeys.TRASH.equals(key)) {
            return desktop.trash().window();
        }
        final Launcher launcher = desktop.catalogue().find(l -> l.key().equals(key) && l.factory() != null);
        if (launcher != null) {
            return launcher.factory().get();
        }
        final ResourceLocation id = ResourceLocation.tryParse(key);
        final ProgramClient.IDesktopAppFactory factory = id == null ? null : ProgramClient.factory(id);
        return factory == null ? null : factory.create(host, monitorPos, desktopId);
    }

    /** Opens that window, or brings it forward when one of that key is already up. */
    void openOnce(final String key, final Supplier<IDesktopApp> make) {
        final DesktopWindow open = desktop.windowFor(key);
        if (open != null) {
            desktop.wm().focus(open);
            return;
        }
        final IDesktopApp app = make.get();
        app.applySkin(desktop.prefs().skin());
        desktop.wm().open(key, app);
    }

    /**
     * Brings the window of that key forward, or opens it the way a session coming back would, when the memory can
     * hold it.
     */
    void openOrFocus(final String key) {
        for (final DesktopWindow w : desktop.wm().all()) {
            if (!w.dialog() && w.appKey().equals(key)) {
                desktop.wm().focus(w);
                return;
            }
        }
        final IDesktopApp app = factoryFor(key);
        if (app != null && desktop.memory().allowOpen(key)) {
            app.applySkin(desktop.prefs().skin());
            desktop.wm().open(key, app);
        }
    }

    /**
     * Opens the Task Manager, or brings it forward when it is already up. It is the panel menu's destination and has
     * no launcher of its own, exactly as on the desktops this imitates.
     */
    void openTaskManager() {
        if (OsRegistry.getProgram(Programs.TASK_MANAGER) != null) {
            openOrFocus(WindowKeys.of(Programs.TASK_MANAGER));
        }
    }

    /**
     * Opens CDE's Application Manager on a group, or on the groups themselves for null, and brings the window
     * forward instead when it is already up: one window for each, however often it is asked for.
     */
    void openApplicationManager(@Nullable final CdeAppGroup group) {
        openOnce(ApplicationManagerApp.keyOf(group), () -> new ApplicationManagerApp(group));
    }

    /** Opens a file manager at that folder, as a window of the file manager this desktop has. */
    void openFolder(final String dir) {
        desktop.wm().open(FILES_KEY, new FilesApp(host, desktopId.getPath(), dir, monitorPos));
    }

    /** Opens the Settings window on one of its pages, the way a menu entry names a page rather than the program. */
    void openSettingsPage(final int page) {
        if (desktop.memory().allowOpen(SETTINGS_KEY)) {
            desktop.wm().open(SETTINGS_KEY, new SettingsApp(host, monitorPos).showPage(page));
        }
    }

    /**
     * Opens a file the way a double-click does: in the program chosen with Always for its extension on this
     * computer, else in the one that opens its kind. A file of a kind nothing here knows, and that no language runs,
     * asks the player which program to use.
     */
    void openFile(final String path) {
        final String program = FileOpeners.defaultFor(path, desktop.catalogue().installed(), defaultApps);
        if (program.isEmpty() && FileOpeners.isUnknownKind(path) && !runsAsProgram(path)) {
            chooseOpener(path);
            return;
        }
        openIn(program, path);
    }

    /**
     * Asks the player which program opens a file, with the Open with chooser over the whole desktop. Always makes the
     * choice this computer's program for the file's extension, written down on the machine.
     */
    void chooseOpener(final String path) {
        final List<String> installed = desktop.catalogue().installed();
        final List<String> programs = FileOpeners.choices(path, installed);
        if (programs.isEmpty()) {
            cannotOpen(path);
            return;
        }
        final String extension = FileOpeners.extensionOf(path);
        final String current = FileOpeners.defaultFor(path, installed, defaultApps);
        final String opener = current.isEmpty() ? "" : openerName(current);
        final String iconSet = desktop.prefs().skin().iconSet();
        desktop.notices().show(new OpenWithPopup(path, extension, programs, opener, iconSet, desktop.textFont(),
                (program, always) -> {
                    if (always) {
                        defaultApps.put(extension, program);
                        PacketDistributor.sendToServer(
                                new SetSettingPayload(host, "defaultapp:" + extension, program));
                    }
                    openIn(program, path);
                }));
    }

    /** Opens a file in a named program, or says why it cannot be opened at all. */
    void openIn(final String programId, final String path) {
        if (programId.isEmpty()) {
            /*
             * Nothing here claims the kind, but a language an addon brought may: its compiled programs have an
             * extension of their own, and opening one of those means running it.
             */
            if (runsAsProgram(path)) {
                runAtTerminal(path);
                return;
            }
            cannotOpen(path);
            return;
        }
        if (programId.equals(FileOpeners.EDITOR)) {
            final EditorApp editor = new EditorApp(host);
            desktop.wm().open(EDITOR_KEY, editor);
            editor.openFile(path);
            return;
        }
        if (programId.equals(FileOpeners.RUNTIME)) {
            // A compiled program is run, not read: it gets this desktop's terminal and prints into it.
            runAtTerminal(path);
            return;
        }
        final ProgramSpec spec = Programs.get(ResourceLocation.fromNamespaceAndPath("jsc", programId));
        final Launcher launcher = spec == null ? null : desktop.catalogue().byProgram(spec.id());
        if (launcher == null) {
            cannotOpen(path);
            return;
        }
        run(launcher);
        /*
         * The window exists once the launcher has run, so the file goes to it straight away. A program that opens no
         * files ignores this, which is what lets any program be picked without a special case.
         */
        final DesktopWindow opened = desktop.windowFor(launcher.key());
        if (opened != null) {
            opened.app().openFile(path);
        }
    }

    /** What this desktop calls its terminal, or an empty string when it has none installed. */
    String terminalLabel() {
        final Launcher terminal = desktop.catalogue().byProgram(Programs.COMMAND_PROMPT);
        return terminal == null ? "" : terminal.label();
    }

    /** Whether a language on this computer runs files like this one, so opening it means running it. */
    private static boolean runsAsProgram(final String path) {
        final String extension = FileOpeners.extensionOf(path);
        return !extension.isEmpty() && JsCore.languages().runnerOf(extension) != null;
    }

    /** The notice for a file that no program on this machine opens. */
    private void cannotOpen(final String path) {
        desktop.notices().showBalloon(GameText.resolve(DesktopTexts.CANNOT_OPEN),
                GameText.resolve(DesktopTexts.NO_PROGRAM_OPENS.with(FsPaths.fileName(path))));
    }

    /** Runs a program at this desktop's terminal, as if the command had been typed there. */
    private void runAtTerminal(final String path) {
        final ShellApp shell = terminal();
        if (shell != null) {
            shell.runProgram(path);
        }
    }

    /** Types lines at this desktop's terminal, one after the other. */
    private void typeAtTerminal(final List<String> lines) {
        final ShellApp shell = terminal();
        if (shell != null) {
            shell.typeLines(lines);
        }
    }

    /**
     * This desktop's terminal window, brought forward, or opened when there is none: a program that needs the prompt
     * gets the one that is up rather than a second one beside it.
     */
    @Nullable
    private ShellApp terminal() {
        final Launcher launcher = desktop.catalogue().byProgram(Programs.COMMAND_PROMPT);
        if (launcher == null) {
            return null;
        }
        final String key = launcher.key();
        final DesktopWindow open = desktop.windowFor(key);
        if (open != null && open.app() instanceof ShellApp shell) {
            open.setMinimized(false);
            desktop.wm().bringToFront(open);
            return shell;
        }
        final IDesktopApp made = factoryFor(key);
        if (made instanceof ShellApp shell && desktop.memory().allowOpen(key)) {
            desktop.wm().open(key, shell);
            return shell;
        }
        return null;
    }
}
