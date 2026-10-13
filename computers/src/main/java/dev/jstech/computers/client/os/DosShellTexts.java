/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.os;

import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;

/** The words of the MC-DOS Shell: its title, its menus and what each item does, its areas, dialogs and help. */
@TextHolder
final class DosShellTexts {

    static final TextKey TITLE = TextKey.of("jsc.dosshell.title", "MC-DOS Shell");

    // The menu bar.
    static final TextKey FILE = TextKey.of("jsc.dosshell.menu.file", "File");
    static final TextKey OPTIONS = TextKey.of("jsc.dosshell.menu.options", "Options");
    static final TextKey VIEW = TextKey.of("jsc.dosshell.menu.view", "View");
    static final TextKey TREE = TextKey.of("jsc.dosshell.menu.tree", "Tree");
    static final TextKey HELP = TextKey.of("jsc.dosshell.menu.help", "Help");

    // File.
    static final TextKey OPEN = TextKey.of("jsc.dosshell.file.open", "Open");
    static final TextKey OPEN_HELP = TextKey.of("jsc.dosshell.file.open.help",
            "Starts the selected program, or opens the selected folder.");
    static final TextKey RUN = TextKey.of("jsc.dosshell.file.run", "Run...");
    static final TextKey RUN_HELP = TextKey.of("jsc.dosshell.file.run.help",
            "Runs a command line as a task of its own.");
    static final TextKey PRINT = TextKey.of("jsc.dosshell.file.print", "Print");
    static final TextKey PRINT_HELP = TextKey.of("jsc.dosshell.file.print.help",
            "Prints the selected file on the linked printer (PRINT).");
    static final TextKey SEARCH = TextKey.of("jsc.dosshell.file.search", "Search...");
    static final TextKey SEARCH_HELP = TextKey.of("jsc.dosshell.file.search.help",
            "Finds files by name all over the disk.");
    static final TextKey VIEW_CONTENTS = TextKey.of("jsc.dosshell.file.view", "View File Contents");
    static final TextKey VIEW_CONTENTS_HELP = TextKey.of("jsc.dosshell.file.view.help",
            "Shows the contents of the selected file (TYPE).");
    static final TextKey MOVE = TextKey.of("jsc.dosshell.file.move", "Move...");
    static final TextKey MOVE_HELP = TextKey.of("jsc.dosshell.file.move.help",
            "Moves the selected file to another folder (MOVE).");
    static final TextKey COPY = TextKey.of("jsc.dosshell.file.copy", "Copy...");
    static final TextKey COPY_HELP = TextKey.of("jsc.dosshell.file.copy.help",
            "Copies the selected file (COPY).");
    static final TextKey DELETE = TextKey.of("jsc.dosshell.file.delete", "Delete...");
    static final TextKey DELETE_HELP = TextKey.of("jsc.dosshell.file.delete.help",
            "Deletes the selected file (DEL), or the selected empty folder (RMDIR).");
    static final TextKey RENAME = TextKey.of("jsc.dosshell.file.rename", "Rename...");
    static final TextKey RENAME_HELP = TextKey.of("jsc.dosshell.file.rename.help",
            "Gives the selected file another name (REN).");
    static final TextKey MAKE_FOLDER = TextKey.of("jsc.dosshell.file.mkdir", "Create Directory...");
    static final TextKey MAKE_FOLDER_HELP = TextKey.of("jsc.dosshell.file.mkdir.help",
            "Makes a folder in the selected one (MKDIR).");
    static final TextKey EXIT = TextKey.of("jsc.dosshell.file.exit", "Exit");
    static final TextKey EXIT_HELP = TextKey.of("jsc.dosshell.file.exit.help",
            "Leaves the MC-DOS Shell for the command prompt.");

    // Options.
    static final TextKey CONFIRMATION = TextKey.of("jsc.dosshell.options.confirmation", "Confirmation...");
    static final TextKey CONFIRMATION_HELP = TextKey.of("jsc.dosshell.options.confirmation.help",
            "Sets whether the shell asks before it deletes.");
    static final TextKey DISPLAY_OPTIONS = TextKey.of("jsc.dosshell.options.display_options",
            "File Display Options...");
    static final TextKey DISPLAY_OPTIONS_HELP = TextKey.of("jsc.dosshell.options.display_options.help",
            "Sets which files are listed and how they are sorted.");
    static final TextKey ACROSS = TextKey.of("jsc.dosshell.options.across", "Select Across Directories");
    static final TextKey INFORMATION = TextKey.of("jsc.dosshell.options.information", "Show Information...");
    static final TextKey INFORMATION_HELP = TextKey.of("jsc.dosshell.options.information.help",
            "Shows the selected file, its folder and its disk.");
    static final TextKey SWAPPER = TextKey.of("jsc.dosshell.options.swapper", "Enable Task Swapper");
    static final TextKey SWAPPER_HELP = TextKey.of("jsc.dosshell.options.swapper.help",
            "Keeps programs started from the shell as tasks to switch between.");
    static final TextKey DISPLAY = TextKey.of("jsc.dosshell.options.display", "Display...");
    static final TextKey COLORS = TextKey.of("jsc.dosshell.options.colors", "Colors...");
    static final TextKey NOT_HERE = TextKey.of("jsc.dosshell.not_here", "Not available on this machine.");

    // View.
    static final TextKey SINGLE = TextKey.of("jsc.dosshell.view.single", "Single File List");
    static final TextKey SINGLE_HELP = TextKey.of("jsc.dosshell.view.single.help",
            "Shows the tree and the file list down to the bottom of the screen.");
    static final TextKey DUAL = TextKey.of("jsc.dosshell.view.dual", "Dual File Lists");
    static final TextKey ALL_FILES = TextKey.of("jsc.dosshell.view.all", "All Files");
    static final TextKey BOTH = TextKey.of("jsc.dosshell.view.both", "Program/File Lists");
    static final TextKey BOTH_HELP = TextKey.of("jsc.dosshell.view.both.help",
            "Shows the files over the programs, the way the shell opens.");
    static final TextKey PROGRAM_LIST = TextKey.of("jsc.dosshell.view.programs", "Program List");
    static final TextKey PROGRAM_LIST_HELP = TextKey.of("jsc.dosshell.view.programs.help",
            "Shows only the programs and the tasks.");
    static final TextKey REPAINT = TextKey.of("jsc.dosshell.view.repaint", "Repaint Screen");
    static final TextKey REPAINT_HELP = TextKey.of("jsc.dosshell.view.repaint.help", "Draws the screen again.");
    static final TextKey REFRESH = TextKey.of("jsc.dosshell.view.refresh", "Refresh");
    static final TextKey REFRESH_HELP = TextKey.of("jsc.dosshell.view.refresh.help",
            "Reads the disk again for what changed on it.");

    // Tree.
    static final TextKey EXPAND_ONE = TextKey.of("jsc.dosshell.tree.expand_one", "Expand One Level");
    static final TextKey EXPAND_ONE_HELP = TextKey.of("jsc.dosshell.tree.expand_one.help",
            "Shows the folders in the selected folder.");
    static final TextKey EXPAND_BRANCH = TextKey.of("jsc.dosshell.tree.expand_branch", "Expand Branch");
    static final TextKey EXPAND_BRANCH_HELP = TextKey.of("jsc.dosshell.tree.expand_branch.help",
            "Shows every folder under the selected folder.");
    static final TextKey EXPAND_ALL = TextKey.of("jsc.dosshell.tree.expand_all", "Expand All");
    static final TextKey EXPAND_ALL_HELP = TextKey.of("jsc.dosshell.tree.expand_all.help",
            "Shows every folder on the disk.");
    static final TextKey COLLAPSE = TextKey.of("jsc.dosshell.tree.collapse", "Collapse Branch");
    static final TextKey COLLAPSE_HELP = TextKey.of("jsc.dosshell.tree.collapse.help",
            "Puts away the folders under the selected folder.");

    // Help.
    static final TextKey INDEX = TextKey.of("jsc.dosshell.help.index", "Index");
    static final TextKey KEYBOARD = TextKey.of("jsc.dosshell.help.keyboard", "Keyboard");
    static final TextKey KEYBOARD_HELP = TextKey.of("jsc.dosshell.help.keyboard.help",
            "The keys the shell answers to.");
    static final TextKey BASICS = TextKey.of("jsc.dosshell.help.basics", "Shell Basics");
    static final TextKey COMMANDS = TextKey.of("jsc.dosshell.help.commands", "Commands");
    static final TextKey COMMANDS_HELP = TextKey.of("jsc.dosshell.help.commands.help",
            "Which MC-DOS command each action of the menus is.");
    static final TextKey PROCEDURES = TextKey.of("jsc.dosshell.help.procedures", "Procedures");
    static final TextKey USING_HELP = TextKey.of("jsc.dosshell.help.using", "Using Help");
    static final TextKey ABOUT = TextKey.of("jsc.dosshell.help.about", "About Shell");
    static final TextKey ABOUT_HELP = TextKey.of("jsc.dosshell.help.about.help", "Which shell this is.");

    // The areas and the program list.
    static final TextKey DIRECTORY_TREE = TextKey.of("jsc.dosshell.area.tree", "Directory Tree");
    static final TextKey MAIN = TextKey.of("jsc.dosshell.area.main", "Main");
    static final TextKey TASKS = TextKey.of("jsc.dosshell.area.tasks", "Active Task List");
    static final TextKey COMMAND_PROMPT = TextKey.of("jsc.dosshell.program.prompt", "Command Prompt");
    static final TextKey SIGMA = TextKey.of("jsc.dosshell.program.sigma", "Sigma");
    static final TextKey PROGRAMS = TextKey.of("jsc.dosshell.program.programs", "Programs");
    static final TextKey DISK_UTILITIES = TextKey.of("jsc.dosshell.program.disk_utilities", "Disk Utilities");
    static final TextKey GROUP = TextKey.of("jsc.dosshell.program.group", "[%s]");
    static final TextKey FORMAT = TextKey.of("jsc.dosshell.program.format", "Format");
    static final TextKey NUMBERED = TextKey.of("jsc.dosshell.program.numbered", "%s (%s)");
    static final TextKey NO_FILES = TextKey.of("jsc.dosshell.no_files", "No files match file specifier.");
    static final TextKey NOT_READY = TextKey.of("jsc.dosshell.not_ready", "Drive not ready.");

    // The key line and the task switcher.
    static final TextKey KEYS = TextKey.of("jsc.dosshell.keys",
            "F10=Actions  Shift+F9=Command Prompt  Alt+Tab=Next Task");
    static final TextKey KEYS_NO_TASKS = TextKey.of("jsc.dosshell.keys.no_tasks",
            "F10=Actions  Shift+F9=Command Prompt");
    static final TextKey SWITCHING = TextKey.of("jsc.dosshell.switching", "Alt+Tab:  %s  ►  Next");
    static final TextKey RETURN = TextKey.of("jsc.dosshell.return", "Press any key to return to MC-DOS Shell.");
    static final TextKey STILL_RUNNING = TextKey.of("jsc.dosshell.still_running",
            "You cannot quit MC-DOS Shell with programs in the Active Task List; quit those programs first.");

    // Dialogs.
    static final TextKey OK = TextKey.of("jsc.dosshell.ok", "OK");
    static final TextKey CANCEL = TextKey.of("jsc.dosshell.cancel", "Cancel");
    static final TextKey YES = TextKey.of("jsc.dosshell.yes", "Yes");
    static final TextKey NO = TextKey.of("jsc.dosshell.no", "No");
    static final TextKey COPY_TITLE = TextKey.of("jsc.dosshell.copy.title", "Copy File");
    static final TextKey MOVE_TITLE = TextKey.of("jsc.dosshell.move.title", "Move File");
    static final TextKey FROM = TextKey.of("jsc.dosshell.from", "From:");
    static final TextKey TO = TextKey.of("jsc.dosshell.to", "To:");
    static final TextKey RENAME_TITLE = TextKey.of("jsc.dosshell.rename.title", "Rename File");
    static final TextKey NEW_NAME = TextKey.of("jsc.dosshell.rename.new", "New name:");
    static final TextKey MAKE_FOLDER_TITLE = TextKey.of("jsc.dosshell.mkdir.title", "Create Directory");
    static final TextKey PARENT = TextKey.of("jsc.dosshell.mkdir.parent", "Parent name:");
    static final TextKey FOLDER_NAME = TextKey.of("jsc.dosshell.mkdir.name", "New directory name:");
    static final TextKey RUN_TITLE = TextKey.of("jsc.dosshell.run.title", "Run");
    static final TextKey COMMAND_LINE = TextKey.of("jsc.dosshell.run.line", "Command Line:");
    static final TextKey SEARCH_TITLE = TextKey.of("jsc.dosshell.search.title", "Search File");
    static final TextKey SEARCH_FOR = TextKey.of("jsc.dosshell.search.for", "Search for:");
    static final TextKey SEARCH_RESULTS = TextKey.of("jsc.dosshell.search.results", "Search Results for: %s");
    static final TextKey FORMAT_TITLE = TextKey.of("jsc.dosshell.format.title", "Format");
    static final TextKey PARAMETERS = TextKey.of("jsc.dosshell.format.parameters", "Parameters:");
    static final TextKey DELETE_TITLE = TextKey.of("jsc.dosshell.delete.title", "Delete File Confirmation");
    static final TextKey DELETE_ASK = TextKey.of("jsc.dosshell.delete.ask", "Delete %s?");
    static final TextKey CONFIRM_TITLE = TextKey.of("jsc.dosshell.confirm.title", "Confirmation");
    static final TextKey CONFIRM_DELETE = TextKey.of("jsc.dosshell.confirm.delete", "Confirm on Delete");
    static final TextKey DISPLAY_TITLE = TextKey.of("jsc.dosshell.display.title", "File Display Options");
    static final TextKey NAME = TextKey.of("jsc.dosshell.display.name", "Name:");
    static final TextKey SORT_BY = TextKey.of("jsc.dosshell.display.sort", "Sort by:");
    static final TextKey BY_NAME = TextKey.of("jsc.dosshell.display.by_name", "Name");
    static final TextKey BY_EXTENSION = TextKey.of("jsc.dosshell.display.by_extension", "Extension");
    static final TextKey BY_DATE = TextKey.of("jsc.dosshell.display.by_date", "Date");
    static final TextKey BY_SIZE = TextKey.of("jsc.dosshell.display.by_size", "Size");
    static final TextKey INFO_TITLE = TextKey.of("jsc.dosshell.info.title", "Show Information");
    static final TextKey INFO_FILE = TextKey.of("jsc.dosshell.info.file", "File");
    static final TextKey INFO_NAME = TextKey.of("jsc.dosshell.info.name", "  Name : %s");
    static final TextKey INFO_ATTR = TextKey.of("jsc.dosshell.info.attr", "  Attr : %s");
    static final TextKey INFO_FOLDER = TextKey.of("jsc.dosshell.info.folder", "Directory");
    static final TextKey INFO_SIZE = TextKey.of("jsc.dosshell.info.size", "  Size : %s mB");
    static final TextKey INFO_FILES = TextKey.of("jsc.dosshell.info.files", "  Files: %s");
    static final TextKey INFO_DISK = TextKey.of("jsc.dosshell.info.disk", "Disk");
    static final TextKey INFO_AVAILABLE = TextKey.of("jsc.dosshell.info.available", "  Avail: %s mB");
    static final TextKey VIEWER_TITLE = TextKey.of("jsc.dosshell.viewer.title", "File View");
    static final TextKey VIEWER_KEYS = TextKey.of("jsc.dosshell.viewer.keys",
            "To view the file's contents use PgUp or PgDn or the arrows.  Esc=Cancel");
    static final TextKey MESSAGE_TITLE = TextKey.of("jsc.dosshell.message.title", "MC-DOS Shell");

    // Help pages.
    static final TextKey KEYS_PAGE = TextKey.of("jsc.dosshell.page.keys", "Tab  moves to the next area\n"
            + "Arrows  move within an area\n"
            + "Enter  opens what is selected\n"
            + "F10 or Alt+letter  opens a menu\n"
            + "F5  reads the disk again\n"
            + "F9  views the selected file\n"
            + "F7  moves it, F8 copies it, Del deletes it\n"
            + "Shift+F9  opens a command prompt as a task\n"
            + "Alt+Tab  switches between the shell and its tasks\n"
            + "F3 or Alt+F4  leaves the shell");
    static final TextKey COMMANDS_PAGE = TextKey.of("jsc.dosshell.page.commands", "Every action is a command\n"
            + "of MC-DOS, run as you would type it:\n"
            + "View File Contents  TYPE\n"
            + "Copy  COPY     Move  MOVE\n"
            + "Delete  DEL or RMDIR\n"
            + "Rename  REN    Create Directory  MKDIR\n"
            + "Print  PRINT   Format  FORMAT");
    static final TextKey ABOUT_PAGE = TextKey.of("jsc.dosshell.page.about", "MC-DOS Shell\n"
            + "Version 5.00\n"
            + "Copyright (C) Midsoft Corp. 1987-1991");

    private DosShellTexts() {
    }
}
