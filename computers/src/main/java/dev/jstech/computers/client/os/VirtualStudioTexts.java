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

/**
 * What Virtual Studio says in words of its own: the Start Window, the Solution Explorer, the New Project wizard, the
 * project's properties, its menus and what a build reports. Solution, project and file names, paths, language and
 * architecture names are data. Kept apart from the window so the language generator can read it on a server too,
 * where windows do not exist.
 */
@TextHolder
final class VirtualStudioTexts {

    // The window.
    static final TextKey ERROR_LIST_TAB = TextKey.of("jsc.virtual_studio.error_list_tab", "ERROR LIST");
    static final TextKey OUTPUT_TAB = TextKey.of("jsc.virtual_studio.output_tab", "OUTPUT");
    static final TextKey RELEASE = TextKey.of("jsc.virtual_studio.release", "Release");
    static final TextKey SOLUTION_EXPLORER = TextKey.of("jsc.virtual_studio.solution_explorer", "SOLUTION EXPLORER");
    static final TextKey CODE_COLUMN = TextKey.of("jsc.virtual_studio.code_column", "Code");
    static final TextKey DESCRIPTION_COLUMN = TextKey.of("jsc.virtual_studio.description_column", "Description");
    static final TextKey FILE_COLUMN = TextKey.of("jsc.virtual_studio.file_column", "File");
    static final TextKey LINE_COLUMN = TextKey.of("jsc.virtual_studio.line_column", "Line");
    static final TextKey ERROR_TOOLTIP = TextKey.of("jsc.virtual_studio.error_tooltip", "%s: %s  (%s, line %s)");
    static final TextKey READY = TextKey.of("jsc.virtual_studio.ready", "Ready");
    static final TextKey ERRORS = TextKey.of("jsc.virtual_studio.errors", "%s error(s)");
    static final TextKey EMPTY_FOLDER = TextKey.of("jsc.virtual_studio.empty_folder", "Open a file from the folder");
    static final TextKey EMPTY_SOLUTION =
            TextKey.of("jsc.virtual_studio.empty_solution", "Open a source from the Solution Explorer");

    // The Start Window.
    static final TextKey OPEN_RECENT = TextKey.of("jsc.virtual_studio.open_recent", "Open recent");
    static final TextKey GET_STARTED = TextKey.of("jsc.virtual_studio.get_started", "Get started");
    static final TextKey OPEN_SOLUTION_CARD =
            TextKey.of("jsc.virtual_studio.open_solution_card", "Open a project or solution");
    static final TextKey OPEN_SOLUTION_HINT =
            TextKey.of("jsc.virtual_studio.open_solution_hint", "A .sln on this machine");
    static final TextKey OPEN_FOLDER_CARD = TextKey.of("jsc.virtual_studio.open_folder_card", "Open a local folder");
    static final TextKey OPEN_FOLDER_HINT = TextKey.of("jsc.virtual_studio.open_folder_hint", "Any folder of programs");
    static final TextKey OPEN_FILE_CARD = TextKey.of("jsc.virtual_studio.open_file_card", "Open a file");
    static final TextKey OPEN_FILE_HINT =
            TextKey.of("jsc.virtual_studio.open_file_hint", "One .sgs or .sg, no project");
    static final TextKey CREATE_PROJECT = TextKey.of("jsc.virtual_studio.create_project", "Create a new project");
    static final TextKey CREATE_PROJECT_HINT =
            TextKey.of("jsc.virtual_studio.create_project_hint", "Start from a template");

    // The Solution Explorer.
    static final TextKey SOLUTION_NODE = TextKey.of("jsc.virtual_studio.solution_node", "Solution '%s' (%s)");
    static final TextKey DEPENDENCIES = TextKey.of("jsc.virtual_studio.dependencies", "Dependencies");
    static final TextKey PROPERTIES = TextKey.of("jsc.virtual_studio.properties", "Properties");

    // The New Project wizard.
    static final TextKey CONFIGURE_TITLE =
            TextKey.of("jsc.virtual_studio.configure_title", "Configure your new project");
    static final TextKey RECENT_TEMPLATES = TextKey.of("jsc.virtual_studio.recent_templates", "Recent templates");
    static final TextKey SEARCH_TEMPLATES = TextKey.of("jsc.virtual_studio.search_templates", "Search for templates");
    static final TextKey ALL_LANGUAGES = TextKey.of("jsc.virtual_studio.all_languages", "All languages");
    static final TextKey ALL_PLATFORMS = TextKey.of("jsc.virtual_studio.all_platforms", "All platforms");
    static final TextKey ALL_TYPES = TextKey.of("jsc.virtual_studio.all_types", "All types");
    static final TextKey BACK = TextKey.of("jsc.virtual_studio.back", "Back");
    static final TextKey NEXT = TextKey.of("jsc.virtual_studio.next", "Next");
    static final TextKey PROJECT_NAME = TextKey.of("jsc.virtual_studio.project_name", "Project name");
    static final TextKey LOCATION = TextKey.of("jsc.virtual_studio.location", "Location");
    static final TextKey SOLUTION_NAME = TextKey.of("jsc.virtual_studio.solution_name", "Solution name");
    static final TextKey SAME_FOLDER =
            TextKey.of("jsc.virtual_studio.same_folder", "Same folder for solution and project");
    static final TextKey CREATE = TextKey.of("jsc.virtual_studio.create", "Create");
    static final TextKey WILL_BE_CREATED =
            TextKey.of("jsc.virtual_studio.will_be_created", "Project will be created in %s");
    static final TextKey ONE_WORD =
            TextKey.of("jsc.virtual_studio.one_word", "A project name is one word, without slashes");
    static final TextKey PROJECT_LOCATION = TextKey.of("jsc.virtual_studio.project_location", "Project Location");

    // The project's properties, and the options.
    static final TextKey PROPERTIES_TITLE = TextKey.of("jsc.virtual_studio.properties_title", "Project Properties");
    static final TextKey NAME_LINE = TextKey.of("jsc.virtual_studio.name_line", "Name: %s");
    static final TextKey KIND_LINE = TextKey.of("jsc.virtual_studio.kind_line", "Kind: %s");
    static final TextKey LANGUAGE_LINE = TextKey.of("jsc.virtual_studio.language_line", "Language: %s");
    static final TextKey ENTRY_LINE = TextKey.of("jsc.virtual_studio.entry_line", "Entry: %s");
    static final TextKey NO_ENTRY = TextKey.of("jsc.virtual_studio.no_entry", "none, a library");
    static final TextKey SOURCES_LINE =
            TextKey.of("jsc.virtual_studio.sources_line", "Sources: %s, references: %s");
    static final TextKey PLATFORM_TARGET = TextKey.of("jsc.virtual_studio.platform_target", "Platform target");
    static final TextKey NOTHING_ANSWERS =
            TextKey.of("jsc.virtual_studio.nothing_answers", "nothing here answers to %s");
    static final TextKey NO_MACHINE = TextKey.of("jsc.virtual_studio.no_machine", "no machine here runs it");
    static final TextKey RUNS_ON = TextKey.of("jsc.virtual_studio.runs_on", "runs on %s machines");
    static final TextKey AND = TextKey.of("jsc.virtual_studio.and", " and ");
    static final TextKey OPTIONS_TITLE = TextKey.of("jsc.virtual_studio.options_title", "Options");
    static final TextKey SUGGEST = TextKey.of("jsc.virtual_studio.suggest", "Suggest what can follow a name");

    // The questions.
    static final TextKey DELETE_QUESTION = TextKey.of("jsc.virtual_studio.delete_question", "Delete %s?");
    static final TextKey DELETE = TextKey.of("jsc.virtual_studio.delete", "Delete");
    static final TextKey ADD_NEW_ITEM = TextKey.of("jsc.virtual_studio.add_new_item", "Add New Item");

    // The menus.
    static final TextKey BUILD = TextKey.of("jsc.virtual_studio.build", "Build");
    static final TextKey DEBUG_MENU = TextKey.of("jsc.virtual_studio.debug_menu", "Debug");
    static final TextKey TOOLS_MENU = TextKey.of("jsc.virtual_studio.tools_menu", "Tools");
    static final TextKey NEW_PROJECT = TextKey.of("jsc.virtual_studio.new_project", "New Project...");
    static final TextKey OPEN_SOLUTION_ITEM =
            TextKey.of("jsc.virtual_studio.open_solution_item", "Open Project/Solution...");
    static final TextKey CLOSE_SOLUTION = TextKey.of("jsc.virtual_studio.close_solution", "Close Solution");
    static final TextKey ERROR_LIST = TextKey.of("jsc.virtual_studio.error_list", "Error List");
    static final TextKey OUTPUT = TextKey.of("jsc.virtual_studio.output", "Output");
    static final TextKey ASSEMBLY = TextKey.of("jsc.virtual_studio.assembly", "Assembly");
    static final TextKey START_WINDOW = TextKey.of("jsc.virtual_studio.start_window", "Start Window");
    static final TextKey QUICK_ACTIONS =
            TextKey.of("jsc.virtual_studio.quick_actions", "Quick Actions and Refactorings");
    static final TextKey ADD_NEW_ITEM_ITEM = TextKey.of("jsc.virtual_studio.add_new_item_item", "Add New Item...");
    static final TextKey ADD_EXISTING_ITEM =
            TextKey.of("jsc.virtual_studio.add_existing_item", "Add Existing Item...");
    static final TextKey ADD_REFERENCE = TextKey.of("jsc.virtual_studio.add_reference", "Add Project Reference...");
    static final TextKey ADD_NEW_PROJECT = TextKey.of("jsc.virtual_studio.add_new_project", "Add New Project...");
    static final TextKey SET_STARTUP = TextKey.of("jsc.virtual_studio.set_startup", "Set as Startup Project");
    static final TextKey BUILD_SOLUTION = TextKey.of("jsc.virtual_studio.build_solution", "Build Solution");
    static final TextKey REBUILD_SOLUTION = TextKey.of("jsc.virtual_studio.rebuild_solution", "Rebuild Solution");
    static final TextKey CLEAN_SOLUTION = TextKey.of("jsc.virtual_studio.clean_solution", "Clean Solution");
    static final TextKey BUILD_NAMED = TextKey.of("jsc.virtual_studio.build_named", "Build %s");
    static final TextKey BUILD_PROJECT = TextKey.of("jsc.virtual_studio.build_project", "Build Project");
    static final TextKey PACKAGE = TextKey.of("jsc.virtual_studio.package", "Package");
    static final TextKey OPTIONS_ITEM = TextKey.of("jsc.virtual_studio.options_item", "Options...");
    static final TextKey ABOUT_ITEM = TextKey.of("jsc.virtual_studio.about_item", "About Virtual Studio");
    static final TextKey ABOUT =
            TextKey.of("jsc.virtual_studio.about", "Virtual Studio, by Midsoft. Σ# 1.0 and Σ 1.0.");
    static final TextKey OPEN_SOLUTION_TITLE =
            TextKey.of("jsc.virtual_studio.open_solution_title", "Open Project/Solution");
    static final TextKey SOLUTIONS = TextKey.of("jsc.virtual_studio.solutions", "Solutions");

    // The menu on a row of the tree.
    static final TextKey REFRESH = TextKey.of("jsc.virtual_studio.refresh", "Refresh");
    static final TextKey OPEN = TextKey.of("jsc.virtual_studio.open", "Open");
    static final TextKey EXCLUDE = TextKey.of("jsc.virtual_studio.exclude", "Exclude From Project");

    // What the Output pane and the status line report.
    static final TextKey NO_SOLUTION_IN =
            TextKey.of("jsc.virtual_studio.no_solution_in", "No solution in %s; opened as a folder");
    static final TextKey BUILD_STARTED = TextKey.of("jsc.virtual_studio.build_started", "Build started: %s");
    static final TextKey NOTHING_TO_BUILD = TextKey.of("jsc.virtual_studio.nothing_to_build", "Nothing to build");
    static final TextKey NO_LANGUAGE_CLAIMS =
            TextKey.of("jsc.virtual_studio.no_language_claims", "%s: no language claims this file");
    static final TextKey NO_LANGUAGE_CALLED =
            TextKey.of("jsc.virtual_studio.no_language_called", "%s: no language called %s");
    static final TextKey NO_SOURCES = TextKey.of("jsc.virtual_studio.no_sources", "%s: no sources to build");
    static final TextKey BUILT_LISTING = TextKey.of("jsc.virtual_studio.built_listing", "%s -> %s  (%s lines)");
    static final TextKey BUILD_SUCCEEDED_NAMED =
            TextKey.of("jsc.virtual_studio.build_succeeded_named", "Build succeeded: %s");
    static final TextKey BUILD_SUCCEEDED = TextKey.of("jsc.virtual_studio.build_succeeded", "Build succeeded");
    static final TextKey COMPLAINT = TextKey.of("jsc.virtual_studio.complaint", "%s: %s");
    static final TextKey BUILD_FAILED_NAMED =
            TextKey.of("jsc.virtual_studio.build_failed_named", "Build failed: %s, %s error(s)");
    static final TextKey BUILD_FAILED = TextKey.of("jsc.virtual_studio.build_failed", "Build failed");
    static final TextKey NO_STARTUP =
            TextKey.of("jsc.virtual_studio.no_startup", "No project to start: none builds a listing");
    static final TextKey DELETED_OF = TextKey.of("jsc.virtual_studio.deleted_of", "Deleted %s of %s");
    static final TextKey NO_PACKAGE = TextKey.of("jsc.virtual_studio.no_package", "No project to package");
    static final TextKey DELETED = TextKey.of("jsc.virtual_studio.deleted", "Deleted %s");

    private VirtualStudioTexts() {
    }
}
