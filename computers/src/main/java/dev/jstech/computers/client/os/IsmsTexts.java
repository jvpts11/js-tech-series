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

/** What the IQL Server Management Studio's window says: its menus, toolbar, explorer, panes and dialogs. */
@TextHolder
final class IsmsTexts {

    // The window.
    static final TextKey STUDIO = TextKey.of("jsc.isms_app.studio", "IQL Server Management Studio");
    static final TextKey ANALYZER = TextKey.of("jsc.isms_app.analyzer", "IQL Query Analyzer");
    static final TextKey TITLE = TextKey.of("jsc.isms_app.title", "%s - %s - %s");
    static final TextKey QUERY_NAME = TextKey.of("jsc.isms_app.query_name", "Query %s");
    static final TextKey PRODUCT = TextKey.of("jsc.isms_app.product", "%s %s");
    static final TextKey SHORT_PRODUCT = TextKey.of("jsc.isms_app.short_product", "Midsoft %s");
    static final TextKey SERVER_PICK = TextKey.of("jsc.isms_app.server_pick", "%s - %s (%s)");

    // The menu bar.
    static final TextKey FILE = TextKey.of("jsc.isms_app.file", "File");
    static final TextKey EDIT = TextKey.of("jsc.isms_app.edit", "Edit");
    static final TextKey VIEW = TextKey.of("jsc.isms_app.view", "View");
    static final TextKey QUERY = TextKey.of("jsc.isms_app.query", "Query");
    static final TextKey TOOLS = TextKey.of("jsc.isms_app.tools", "Tools");
    static final TextKey WINDOW = TextKey.of("jsc.isms_app.window", "Window");
    static final TextKey HELP = TextKey.of("jsc.isms_app.help", "Help");

    // File.
    static final TextKey NEW_QUERY = TextKey.of("jsc.isms_app.new_query", "New Query");
    static final TextKey NEW = TextKey.of("jsc.isms_app.new", "New");
    static final TextKey OPEN_FILE = TextKey.of("jsc.isms_app.open_file", "Open File...");
    static final TextKey OPEN = TextKey.of("jsc.isms_app.open", "Open");
    static final TextKey RECENT_FILES = TextKey.of("jsc.isms_app.recent_files", "Recent Files");
    static final TextKey CLOSE = TextKey.of("jsc.isms_app.close", "Close");
    static final TextKey SAVE = TextKey.of("jsc.isms_app.save", "Save");
    static final TextKey SAVE_AS = TextKey.of("jsc.isms_app.save_as", "Save As...");
    static final TextKey SAVE_ALL = TextKey.of("jsc.isms_app.save_all", "Save All");
    static final TextKey EXIT = TextKey.of("jsc.isms_app.exit", "Exit");
    static final TextKey SCRIPTS = TextKey.of("jsc.isms_app.scripts", "IQL scripts");
    static final TextKey TRACES = TextKey.of("jsc.isms_app.traces", "Trace files");
    static final TextKey RESULT_FILES = TextKey.of("jsc.isms_app.result_files", "Comma-separated values");

    // Edit.
    static final TextKey UNDO = TextKey.of("jsc.isms_app.undo", "Undo");
    static final TextKey REDO = TextKey.of("jsc.isms_app.redo", "Redo");
    static final TextKey CUT = TextKey.of("jsc.isms_app.cut", "Cut");
    static final TextKey COPY = TextKey.of("jsc.isms_app.copy", "Copy");
    static final TextKey PASTE = TextKey.of("jsc.isms_app.paste", "Paste");
    static final TextKey SELECT_ALL = TextKey.of("jsc.isms_app.select_all", "Select All");
    static final TextKey FIND = TextKey.of("jsc.isms_app.find", "Find...");
    static final TextKey REPLACE = TextKey.of("jsc.isms_app.replace", "Replace...");
    static final TextKey GO_TO_LINE = TextKey.of("jsc.isms_app.go_to_line", "Go To Line...");
    static final TextKey COMMENT = TextKey.of("jsc.isms_app.comment", "Comment / Uncomment");
    static final TextKey UPPERCASE = TextKey.of("jsc.isms_app.uppercase", "Make Uppercase");
    static final TextKey LOWERCASE = TextKey.of("jsc.isms_app.lowercase", "Make Lowercase");
    static final TextKey INTELLISENSE = TextKey.of("jsc.isms_app.intellisense", "IntelliSense");
    static final TextKey LIST_MEMBERS = TextKey.of("jsc.isms_app.list_members", "List Members");
    static final TextKey COMPLETE_WORD = TextKey.of("jsc.isms_app.complete_word", "Complete Word");

    // View.
    static final TextKey OBJECT_EXPLORER = TextKey.of("jsc.isms_app.object_explorer", "Object Explorer");
    static final TextKey OBJECT_BROWSER = TextKey.of("jsc.isms_app.object_browser", "Object Browser");
    static final TextKey EXPLORER_DETAILS = TextKey.of("jsc.isms_app.explorer_details", "Object Explorer Details");
    static final TextKey PROPERTIES_WINDOW = TextKey.of("jsc.isms_app.properties_window", "Properties Window");
    static final TextKey TEMPLATE_EXPLORER = TextKey.of("jsc.isms_app.template_explorer", "Template Explorer");
    static final TextKey RESULTS_PANE = TextKey.of("jsc.isms_app.results_pane", "Results Pane");
    static final TextKey REFRESH_EXPLORER = TextKey.of("jsc.isms_app.refresh_explorer", "Refresh Object Explorer");

    // Query.
    static final TextKey EXECUTE = TextKey.of("jsc.isms_app.execute", "Execute");
    static final TextKey EXECUTE_SELECTION = TextKey.of("jsc.isms_app.execute_selection", "Execute Selection");
    static final TextKey CANCEL_QUERY = TextKey.of("jsc.isms_app.cancel_query", "Cancel Executing Query");
    static final TextKey PARSE = TextKey.of("jsc.isms_app.parse", "Parse");
    static final TextKey ESTIMATED_PLAN = TextKey.of("jsc.isms_app.estimated_plan", "Display Estimated Plan");
    static final TextKey TO_GRID = TextKey.of("jsc.isms_app.to_grid", "Results to Grid");
    static final TextKey TO_TEXT = TextKey.of("jsc.isms_app.to_text", "Results to Text");
    static final TextKey TO_FILE = TextKey.of("jsc.isms_app.to_file", "Results to File");
    static final TextKey TEMPLATE_VALUES = TextKey.of("jsc.isms_app.template_values",
            "Specify Values for Template Parameters...");
    static final TextKey QUERY_OPTIONS = TextKey.of("jsc.isms_app.query_options", "Query Options...");

    // Tools.
    static final TextKey PROFILER = TextKey.of("jsc.isms_app.profiler", "IQL Server Profiler");
    static final TextKey ACTIVITY_MONITOR = TextKey.of("jsc.isms_app.activity_monitor", "Activity Monitor");
    static final TextKey INDEX_MAINTENANCE = TextKey.of("jsc.isms_app.index_maintenance", "Index Maintenance...");
    static final TextKey OPTIONS = TextKey.of("jsc.isms_app.options", "Options...");

    // Window and Help.
    static final TextKey NEXT_TAB = TextKey.of("jsc.isms_app.next_tab", "Next Tab");
    static final TextKey PREVIOUS_TAB = TextKey.of("jsc.isms_app.previous_tab", "Previous Tab");
    static final TextKey CLOSE_ALL = TextKey.of("jsc.isms_app.close_all", "Close All Documents");
    static final TextKey RESET_LAYOUT = TextKey.of("jsc.isms_app.reset_layout", "Reset Window Layout");
    static final TextKey WINDOW_ENTRY = TextKey.of("jsc.isms_app.window_entry", "%s %s");
    static final TextKey IQL_REFERENCE = TextKey.of("jsc.isms_app.iql_reference", "IQL Reference");
    static final TextKey SHORTCUTS = TextKey.of("jsc.isms_app.shortcuts", "Keyboard Shortcuts");
    static final TextKey ABOUT = TextKey.of("jsc.isms_app.about", "About %s");
    static final TextKey ABOUT_TEXT = TextKey.of("jsc.isms_app.about_text",
            "%s, version %s.\nConnected to %s on %s, from the computer %s.");

    // The toolbar and the status bar.
    static final TextKey PLAN = TextKey.of("jsc.isms_app.plan", "Plan");
    static final TextKey GRID = TextKey.of("jsc.isms_app.grid", "Grid");
    static final TextKey TEXT = TextKey.of("jsc.isms_app.text", "Text");
    static final TextKey TO_A_FILE = TextKey.of("jsc.isms_app.to_a_file", "File");
    static final TextKey CANCEL = TextKey.of("jsc.isms_app.cancel", "Cancel");
    static final TextKey READY = TextKey.of("jsc.isms_app.ready", "Ready");
    static final TextKey EXECUTING = TextKey.of("jsc.isms_app.executing", "Executing query...");
    static final TextKey EXECUTED = TextKey.of("jsc.isms_app.executed", "Query executed successfully");
    static final TextKey WITH_ERRORS = TextKey.of("jsc.isms_app.with_errors", "Query completed with errors");
    static final TextKey CANCELLED = TextKey.of("jsc.isms_app.cancelled", "Query was cancelled by user.");
    static final TextKey NOT_CONNECTED = TextKey.of("jsc.isms_app.not_connected", "Not connected");
    static final TextKey PARSED = TextKey.of("jsc.isms_app.parsed", "Commands completed successfully.");
    static final TextKey PARSE_FAILED = TextKey.of("jsc.isms_app.parse_failed", "Parse found an error");
    static final TextKey HOST = TextKey.of("jsc.isms_app.host", "host: %s");
    static final TextKey ROWS = TextKey.of("jsc.isms_app.rows", "%s rows");

    // The Object Explorer.
    static final TextKey ROOT = TextKey.of("jsc.isms_app.root", "%s (%s)");
    static final TextKey COUNTED = TextKey.of("jsc.isms_app.counted", "%s (%s)");
    static final TextKey TABLES = TextKey.of("jsc.isms_app.tables", "Tables");
    static final TextKey VIEWS = TextKey.of("jsc.isms_app.views", "Views");
    static final TextKey PROGRAMMABILITY = TextKey.of("jsc.isms_app.programmability", "Programmability");
    static final TextKey PROCEDURES = TextKey.of("jsc.isms_app.procedures", "Stored Procedures");
    static final TextKey STORAGE = TextKey.of("jsc.isms_app.storage", "Storage");
    static final TextKey MANAGEMENT = TextKey.of("jsc.isms_app.management", "Management");
    static final TextKey MAINTENANCE = TextKey.of("jsc.isms_app.maintenance", "Maintenance");
    static final TextKey INDEX = TextKey.of("jsc.isms_app.index", "Index");
    static final TextKey LOCKS = TextKey.of("jsc.isms_app.locks", "Locks");
    static final TextKey OPERATIONS_LOG = TextKey.of("jsc.isms_app.operations_log", "Operations Log");
    static final TextKey AGENT = TextKey.of("jsc.isms_app.agent", "Automation Agent");
    static final TextKey JOBS = TextKey.of("jsc.isms_app.jobs", "Jobs");
    static final TextKey HEALTHY = TextKey.of("jsc.isms_app.healthy", "healthy");
    static final TextKey STALE = TextKey.of("jsc.isms_app.stale", "stale");
    static final TextKey FRAGMENTED = TextKey.of("jsc.isms_app.fragmented", "fragmented");
    static final TextKey PAUSED = TextKey.of("jsc.isms_app.paused", "paused");
    static final TextKey LOCK_ENTRY = TextKey.of("jsc.isms_app.lock_entry", "%s x%s");

    // What each node's menu offers.
    static final TextKey REFRESH = TextKey.of("jsc.isms_app.refresh", "Refresh");
    static final TextKey START = TextKey.of("jsc.isms_app.start", "Start");
    static final TextKey STOP = TextKey.of("jsc.isms_app.stop", "Stop");
    static final TextKey RESTART = TextKey.of("jsc.isms_app.restart", "Restart");
    static final TextKey PROPERTIES = TextKey.of("jsc.isms_app.properties", "Properties");
    static final TextKey TOP_ROWS = TextKey.of("jsc.isms_app.top_rows", "Query Top 100 Rows");
    static final TextKey COUNT_ROWS = TextKey.of("jsc.isms_app.count_rows", "Count Rows");
    static final TextKey SCRIPT_TABLE = TextKey.of("jsc.isms_app.script_table", "Script Table as");
    static final TextKey SCRIPT_VIEW = TextKey.of("jsc.isms_app.script_view", "Script View as");
    static final TextKey SCRIPT_PROCEDURE = TextKey.of("jsc.isms_app.script_procedure", "Script Procedure as");
    static final TextKey QUERY_TO_WINDOW = TextKey.of("jsc.isms_app.query_to_window",
            "QUERY to New Query Editor Window");
    static final TextKey QUERY_TO_CLIPBOARD = TextKey.of("jsc.isms_app.query_to_clipboard", "QUERY to Clipboard");
    static final TextKey CREATE_TO_WINDOW = TextKey.of("jsc.isms_app.create_to_window",
            "CREATE to New Query Editor Window");
    static final TextKey DROP_TO_WINDOW = TextKey.of("jsc.isms_app.drop_to_window",
            "DROP to New Query Editor Window");
    static final TextKey QUERY_VIEW = TextKey.of("jsc.isms_app.query_view", "Query View");
    static final TextKey EXECUTE_PROCEDURE = TextKey.of("jsc.isms_app.execute_procedure", "Execute Stored Procedure");
    static final TextKey MODIFY = TextKey.of("jsc.isms_app.modify", "Modify");
    static final TextKey QUERY_SERVER = TextKey.of("jsc.isms_app.query_server", "Query What It Holds");
    static final TextKey ANALYZE = TextKey.of("jsc.isms_app.analyze", "Analyze");
    static final TextKey REBUILD = TextKey.of("jsc.isms_app.rebuild", "Rebuild");
    static final TextKey VACUUM = TextKey.of("jsc.isms_app.vacuum", "Vacuum");
    static final TextKey UNLOCK = TextKey.of("jsc.isms_app.unlock", "Unlock");
    static final TextKey QUERY_LOG = TextKey.of("jsc.isms_app.query_log", "Query the Log");
    static final TextKey START_JOB = TextKey.of("jsc.isms_app.start_job", "Start Job");
    static final TextKey PAUSE_JOB = TextKey.of("jsc.isms_app.pause_job", "Pause Job");
    static final TextKey DELETE = TextKey.of("jsc.isms_app.delete", "Delete");
    static final TextKey NEW_JOB = TextKey.of("jsc.isms_app.new_job", "New Job...");
    static final TextKey CANCEL_OPERATION = TextKey.of("jsc.isms_app.cancel_operation", "Cancel Operation");

    // The results pane.
    static final TextKey RESULTS = TextKey.of("jsc.isms_app.results", "Results");
    static final TextKey MESSAGES = TextKey.of("jsc.isms_app.messages", "Messages");
    static final TextKey NO_RESULTS = TextKey.of("jsc.isms_app.no_results", "(no results)");
    static final TextKey NO_PLAN = TextKey.of("jsc.isms_app.no_plan",
            "Display Estimated Plan on a CRAFT shows its plan here.");
    static final TextKey RESULT_SET = TextKey.of("jsc.isms_app.result_set", "Result %s");

    // Messages.
    static final TextKey ROW_COUNT = TextKey.of("jsc.isms_app.row_count", "(%s rows)");
    static final TextKey ONE_ROW = TextKey.of("jsc.isms_app.one_row", "(1 row)");
    static final TextKey LINE_ERROR = TextKey.of("jsc.isms_app.line_error", "Line %s: %s");
    static final TextKey STARTED = TextKey.of("jsc.isms_app.started",
            "Operation #%s started - Cancel Executing Query stops it");
    static final TextKey COMPLETION = TextKey.of("jsc.isms_app.completion", "Completion time: %s");
    static final TextKey TOO_LONG = TextKey.of("jsc.isms_app.too_long",
            "Line %s: the statement is %s characters long, more than the %s a statement may be");
    static final TextKey NOTHING = TextKey.of("jsc.isms_app.nothing", "There is nothing to run.");
    static final TextKey STOPPED_OPERATION = TextKey.of("jsc.isms_app.stopped_operation", "Stopping operation #%s");
    static final TextKey PLAN_CRAFT_ONLY = TextKey.of("jsc.isms_app.plan_craft_only",
            "Display Estimated Plan works on a CRAFT statement.");
    static final TextKey RESULTS_SAVED = TextKey.of("jsc.isms_app.results_saved", "The results were saved to %s.");
    static final TextKey NO_RESULTS_TO_SAVE = TextKey.of("jsc.isms_app.no_results_to_save",
            "There are no results to save.");
    static final TextKey HOLDS_ROWS = TextKey.of("jsc.isms_app.holds_rows", "%s holds %s rows.");
    static final TextKey COPIED = TextKey.of("jsc.isms_app.copied", "Copied to the clipboard.");
    static final TextKey NO_MATCH = TextKey.of("jsc.isms_app.no_match", "Nothing the studio knows starts with %s.");
    static final TextKey OPENED = TextKey.of("jsc.isms_app.opened", "Opened %s");
    static final TextKey COULD_NOT_OPEN = TextKey.of("jsc.isms_app.could_not_open", "%s could not be opened");
    static final TextKey TOO_LARGE = TextKey.of("jsc.isms_app.too_large",
            "%s is too large for the studio to open");
    static final TextKey NOT_CONNECTED_TO = TextKey.of("jsc.isms_app.not_connected_to", "Not connected: %s");

    // Dialogs.
    static final TextKey CONFIRM = TextKey.of("jsc.isms_app.confirm", "Confirm");
    static final TextKey CONFIRM_TEXT = TextKey.of("jsc.isms_app.confirm_text",
            "This statement cannot be undone:\n%s\nRun it?");
    static final TextKey DONT_ASK = TextKey.of("jsc.isms_app.dont_ask", "Don't ask again");
    static final TextKey YES = TextKey.of("jsc.isms_app.yes", "Yes");
    static final TextKey NO = TextKey.of("jsc.isms_app.no", "No");
    static final TextKey OK = TextKey.of("jsc.isms_app.ok", "OK");
    static final TextKey UNSAVED = TextKey.of("jsc.isms_app.unsaved",
            "%s has changes that are not saved. Close it anyway?");
    static final TextKey DELETE_JOB = TextKey.of("jsc.isms_app.delete_job", "Delete the job %s?");
    static final TextKey NO_ENGINE = TextKey.of("jsc.isms_app.no_engine",
            "No compatible Midsoft IQL Server was found on this network.");
    static final TextKey NOTICE = TextKey.of("jsc.isms_app.notice", "%s\n%s");
    static final TextKey OTHER_ENGINE = TextKey.of("jsc.isms_app.other_engine",
            "%s runs %s. Use the tools of that engine, or ask whoever runs the Mainframe to switch it in the"
                    + " Network Manager.");
    static final TextKey NOT_INSTALLED = TextKey.of("jsc.isms_app.not_installed",
            "%s has no Midsoft IQL Server installed. It can be installed from the Network Manager's Network"
                    + " Services.");
    static final TextKey TEMPLATE_TITLE = TextKey.of("jsc.isms_app.template_title",
            "Specify Values for Template Parameters");
    static final TextKey PARAMETER = TextKey.of("jsc.isms_app.parameter", "%s (%s)");
    static final TextKey NO_PARAMETERS = TextKey.of("jsc.isms_app.no_parameters",
            "This query has no template parameters.");
    static final TextKey QUERY_OPTIONS_TITLE = TextKey.of("jsc.isms_app.query_options_title", "Query Options");
    static final TextKey STOP_ON_ERROR = TextKey.of("jsc.isms_app.stop_on_error", "Stop the script at the first error");
    static final TextKey RESULTS_TO = TextKey.of("jsc.isms_app.results_to", "Results to: %s");
    static final TextKey OPTIONS_TITLE = TextKey.of("jsc.isms_app.options_title", "Options");
    static final TextKey ASK_FIRST = TextKey.of("jsc.isms_app.ask_first",
            "Ask before a statement that cannot be undone");
    static final TextKey SHOW_LINE_NUMBERS = TextKey.of("jsc.isms_app.show_line_numbers", "Show line numbers");
    static final TextKey QUERY_AT_START = TextKey.of("jsc.isms_app.query_at_start",
            "Open a new query when the studio starts");
    static final TextKey INDEX_TITLE = TextKey.of("jsc.isms_app.index_title", "Index Maintenance");
    static final TextKey INDEX_TEXT = TextKey.of("jsc.isms_app.index_text",
            "The index is %s: %s kinds of item on %s servers.");

    // Object Explorer Details and the Properties Window.
    static final TextKey NAME = TextKey.of("jsc.isms_app.name", "Name");
    static final TextKey KIND = TextKey.of("jsc.isms_app.kind", "Kind");
    static final TextKey ROWS_COLUMN = TextKey.of("jsc.isms_app.rows_column", "Rows");
    static final TextKey COLUMNS = TextKey.of("jsc.isms_app.columns", "Columns");
    static final TextKey HOLDS = TextKey.of("jsc.isms_app.holds", "What it holds");
    static final TextKey LAST_CHANGE = TextKey.of("jsc.isms_app.last_change", "Last change");
    static final TextKey STATE = TextKey.of("jsc.isms_app.state", "State");
    static final TextKey TRIGGER = TextKey.of("jsc.isms_app.trigger", "Trigger");
    static final TextKey QUANTITY = TextKey.of("jsc.isms_app.quantity", "Quantity");
    static final TextKey VERSION = TextKey.of("jsc.isms_app.version", "Version");
    static final TextKey SEARCH = TextKey.of("jsc.isms_app.search", "Search");
    static final TextKey ITEMS_SHOWN = TextKey.of("jsc.isms_app.items_shown", "%s items");
    static final TextKey KIND_TABLE = TextKey.of("jsc.isms_app.kind_table", "Table");
    static final TextKey KIND_COLUMN = TextKey.of("jsc.isms_app.kind_column", "Column");
    static final TextKey KIND_VIEW = TextKey.of("jsc.isms_app.kind_view", "View");
    static final TextKey KIND_PROCEDURE = TextKey.of("jsc.isms_app.kind_procedure", "Stored Procedure");
    static final TextKey KIND_SERVER = TextKey.of("jsc.isms_app.kind_server", "Server");
    static final TextKey KIND_JOB = TextKey.of("jsc.isms_app.kind_job", "Job");
    static final TextKey KIND_LOCK = TextKey.of("jsc.isms_app.kind_lock", "Item held by hand");
    static final TextKey KIND_FOLDER = TextKey.of("jsc.isms_app.kind_folder", "Folder");
    static final TextKey KIND_NETWORK = TextKey.of("jsc.isms_app.kind_network", "Network");
    static final TextKey KIND_INDEX = TextKey.of("jsc.isms_app.kind_index", "Index");
    static final TextKey KIND_LOG = TextKey.of("jsc.isms_app.kind_log", "Log");
    static final TextKey RUNNING = TextKey.of("jsc.isms_app.running", "running");
    static final TextKey NO_SELECTION = TextKey.of("jsc.isms_app.no_selection", "Nothing is picked in the explorer.");

    // Template Explorer.
    static final TextKey TEMPLATE_QUERY = TextKey.of("jsc.isms_app.template_query", "Query a table");
    static final TextKey TEMPLATE_COUNT = TextKey.of("jsc.isms_app.template_count", "Count an item");
    static final TextKey TEMPLATE_CRAFT = TextKey.of("jsc.isms_app.template_craft", "Craft an item");
    static final TextKey TEMPLATE_VIEW = TextKey.of("jsc.isms_app.template_view", "Create a view");
    static final TextKey TEMPLATE_PROCEDURE = TextKey.of("jsc.isms_app.template_procedure", "Create a procedure");
    static final TextKey TEMPLATE_JOB = TextKey.of("jsc.isms_app.template_job", "Create a job");
    static final TextKey TEMPLATE_LOCK = TextKey.of("jsc.isms_app.template_lock", "Hold an item");
    static final TextKey TEMPLATE_UPDATE = TextKey.of("jsc.isms_app.template_update", "Update an item");
    static final TextKey TEMPLATE_HINT = TextKey.of("jsc.isms_app.template_hint",
            "Click a template to open it in a new query.");

    // The IQL Reference.
    static final TextKey REF_QUERY = TextKey.of("jsc.isms_app.ref_query", "reads the rows of a table");
    static final TextKey REF_COUNT = TextKey.of("jsc.isms_app.ref_count", "counts an item the network holds");
    static final TextKey REF_SELECT = TextKey.of("jsc.isms_app.ref_select", "brings an item to this computer");
    static final TextKey REF_INSERT = TextKey.of("jsc.isms_app.ref_insert", "stores an item from this computer");
    static final TextKey REF_DELETE = TextKey.of("jsc.isms_app.ref_delete", "sends an item out of the network");
    static final TextKey REF_MOVE = TextKey.of("jsc.isms_app.ref_move", "moves an item between servers");
    static final TextKey REF_DROP = TextKey.of("jsc.isms_app.ref_drop", "destroys an item, for good");
    static final TextKey REF_CRAFT = TextKey.of("jsc.isms_app.ref_craft", "makes an item from its recipes");
    static final TextKey REF_UPDATE = TextKey.of("jsc.isms_app.ref_update",
            "changes an item with a personal-use card");
    static final TextKey REF_LOCK = TextKey.of("jsc.isms_app.ref_lock", "holds an item, or lets it go");
    static final TextKey REF_MAINTENANCE = TextKey.of("jsc.isms_app.ref_maintenance", "looks after the index");
    static final TextKey REF_VIEW = TextKey.of("jsc.isms_app.ref_view", "saves a query under a name");
    static final TextKey REF_PROCEDURE = TextKey.of("jsc.isms_app.ref_procedure",
            "saves statements to run together");
    static final TextKey REF_JOB = TextKey.of("jsc.isms_app.ref_job", "runs statements on a timer or a condition");
    static final TextKey REF_EXEC = TextKey.of("jsc.isms_app.ref_exec", "runs a saved procedure");
    static final TextKey REF_CLAUSES = TextKey.of("jsc.isms_app.ref_clauses",
            "WHERE keeps the rows that match, ORDER BY sorts them, LIMIT stops after so many.");
    static final TextKey REF_COMMENTS = TextKey.of("jsc.isms_app.ref_comments",
            "A semicolon ends a statement; two dashes start a comment.");

    private IsmsTexts() {
    }
}
