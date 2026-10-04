/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.os;

import dev.jstech.computers.operation.payload.IsmsActionPayload;
import dev.jstech.computers.operation.payload.OperationRecord;
import dev.jstech.core.client.gui.component.ContextMenu;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.TextKey;
import dev.jstech.core.util.ShortId;
import java.util.ArrayList;
import java.util.List;

/**
 * The menus of the IQL Server Management Studio, every item of which does something: the seven of its menu bar with
 * their keys, the menu of each kind of Object Explorer node, the toolbar's server picker and an Operation of the
 * Activity Monitor. The keys written beside an item are the same ones the studio answers to.
 */
final class IsmsMenus {

    private final IsmsApp app;

    // The keys, as each item writes them.
    static final String NEW_KEYS = "Ctrl+N";
    static final String OPEN_KEYS = "Ctrl+O";
    static final String CLOSE_KEYS = "Ctrl+F4";
    static final String SAVE_KEYS = "Ctrl+S";
    static final String SAVE_ALL_KEYS = "Ctrl+Shift+S";
    static final String PRINT_KEYS = "Ctrl+P";
    static final String UNDO_KEYS = "Ctrl+Z";
    static final String REDO_KEYS = "Ctrl+Y";
    static final String CUT_KEYS = "Ctrl+X";
    static final String COPY_KEYS = "Ctrl+C";
    static final String PASTE_KEYS = "Ctrl+V";
    static final String SELECT_ALL_KEYS = "Ctrl+A";
    static final String FIND_KEYS = "Ctrl+F";
    static final String REPLACE_KEYS = "Ctrl+H";
    static final String GO_TO_KEYS = "Ctrl+G";
    static final String COMMENT_KEYS = "Ctrl+K";
    static final String UPPER_KEYS = "Ctrl+Shift+U";
    static final String LOWER_KEYS = "Ctrl+U";
    static final String MEMBERS_KEYS = "Ctrl+J";
    static final String COMPLETE_KEYS = "Ctrl+Space";
    static final String EXPLORER_KEYS = "F8";
    static final String DETAILS_KEYS = "F7";
    static final String PROPERTIES_KEYS = "F4";
    static final String RESULTS_PANE_KEYS = "Ctrl+R";
    static final String EXECUTE_KEYS = "F5";
    static final String SELECTION_KEYS = "Ctrl+E";
    static final String CANCEL_KEYS = "Alt+Break";
    static final String PARSE_KEYS = "Ctrl+F5";
    static final String PLAN_KEYS = "Ctrl+L";
    static final String GRID_KEYS = "Ctrl+D";
    static final String TEXT_KEYS = "Ctrl+T";
    static final String FILE_KEYS = "Ctrl+Shift+F";
    static final String TEMPLATE_KEYS = "Ctrl+Shift+M";
    static final String NEXT_TAB_KEYS = "Ctrl+Tab";
    static final String PREVIOUS_TAB_KEYS = "Ctrl+Shift+Tab";
    static final String HELP_KEYS = "F1";

    IsmsMenus(final IsmsApp app) {
        this.app = app;
    }

    List<ContextMenu.Item> file() {
        final IsmsDocument doc = app.current();
        final boolean query = doc != null && doc.isQuery();
        final List<ContextMenu.Item> recent = new ArrayList<>();
        for (final String path : app.settings().recent) {
            recent.add(new ContextMenu.Item(path, true, () -> app.openPath(path)));
        }
        return List.of(
                keyed(app.look().analyzer() ? IsmsTexts.NEW : IsmsTexts.NEW_QUERY, NEW_KEYS, true, app::newQuery),
                keyed(IsmsTexts.OPEN_FILE, OPEN_KEYS, true, app::chooseOpen),
                ContextMenu.Item.submenu(GameText.resolve(IsmsTexts.RECENT_FILES), recent),
                ContextMenu.Item.separator(),
                keyed(IsmsTexts.CLOSE, CLOSE_KEYS, doc != null, () -> app.closeDocument(doc)),
                keyed(IsmsTexts.SAVE, SAVE_KEYS, query, () -> app.save(doc)),
                item(IsmsTexts.SAVE_AS, query, () -> app.chooseSaveAs(doc)),
                keyed(IsmsTexts.SAVE_ALL, SAVE_ALL_KEYS, true, app::saveAll),
                ContextMenu.Item.separator(),
                keyed(PrintTexts.PRINT_MENU, PRINT_KEYS, query, app::print),
                ContextMenu.Item.separator(),
                item(IsmsTexts.EXIT, true, app::exit));
    }

    List<ContextMenu.Item> edit() {
        final IsmsDocument doc = app.current();
        final boolean query = doc != null && doc.isQuery();
        return List.of(
                keyed(IsmsTexts.UNDO, UNDO_KEYS, query, () -> app.editKey(doc, 'Z')),
                keyed(IsmsTexts.REDO, REDO_KEYS, query, () -> app.editKey(doc, 'Y')),
                ContextMenu.Item.separator(),
                keyed(IsmsTexts.CUT, CUT_KEYS, query, () -> app.editKey(doc, 'X')),
                keyed(IsmsTexts.COPY, COPY_KEYS, query, () -> app.editKey(doc, 'C')),
                keyed(IsmsTexts.PASTE, PASTE_KEYS, query, () -> app.editKey(doc, 'V')),
                keyed(IsmsTexts.SELECT_ALL, SELECT_ALL_KEYS, query, () -> app.editKey(doc, 'A')),
                ContextMenu.Item.separator(),
                keyed(IsmsTexts.FIND, FIND_KEYS, query, () -> app.find(false)),
                keyed(IsmsTexts.REPLACE, REPLACE_KEYS, query, () -> app.find(false)),
                keyed(IsmsTexts.GO_TO_LINE, GO_TO_KEYS, query, () -> app.find(true)),
                ContextMenu.Item.separator(),
                keyed(IsmsTexts.COMMENT, COMMENT_KEYS, query, () -> app.comment(doc)),
                keyed(IsmsTexts.UPPERCASE, UPPER_KEYS, query, () -> app.changeCase(doc, true)),
                keyed(IsmsTexts.LOWERCASE, LOWER_KEYS, query, () -> app.changeCase(doc, false)),
                ContextMenu.Item.submenu(GameText.resolve(IsmsTexts.INTELLISENSE), List.of(
                        keyed(IsmsTexts.LIST_MEMBERS, MEMBERS_KEYS, query, () -> app.listMembers(doc)),
                        keyed(IsmsTexts.COMPLETE_WORD, COMPLETE_KEYS, query, () -> app.completeWord(doc)))));
    }

    List<ContextMenu.Item> view() {
        return List.of(
                keyed(app.look().analyzer() ? IsmsTexts.OBJECT_BROWSER : IsmsTexts.OBJECT_EXPLORER, EXPLORER_KEYS,
                        true, app::toggleExplorer),
                keyed(IsmsTexts.EXPLORER_DETAILS, DETAILS_KEYS, true, app::openDetails),
                keyed(IsmsTexts.PROPERTIES_WINDOW, PROPERTIES_KEYS, true, app::toggleProperties),
                item(IsmsTexts.TEMPLATE_EXPLORER, true, () -> app.openPage(IsmsDocument.Kind.TEMPLATES,
                        IsmsTexts.TEMPLATE_EXPLORER)),
                keyed(IsmsTexts.RESULTS_PANE, RESULTS_PANE_KEYS, true, app::toggleResults),
                ContextMenu.Item.separator(),
                item(IsmsTexts.REFRESH_EXPLORER, true, app::askSchema));
    }

    List<ContextMenu.Item> query() {
        final IsmsDocument doc = app.current();
        final boolean query = doc != null && doc.isQuery();
        final boolean running = query && doc.running();
        return List.of(
                keyed(IsmsTexts.EXECUTE, EXECUTE_KEYS, query && !running, () -> app.queries().execute(doc, false)),
                keyed(IsmsTexts.EXECUTE_SELECTION, SELECTION_KEYS, query && !running,
                        () -> app.queries().execute(doc, true)),
                keyed(IsmsTexts.CANCEL_QUERY, CANCEL_KEYS, running, () -> app.queries().cancel(doc)),
                keyed(IsmsTexts.PARSE, PARSE_KEYS, query, () -> app.queries().parse(doc)),
                ContextMenu.Item.separator(),
                keyed(IsmsTexts.ESTIMATED_PLAN, PLAN_KEYS, query, () -> app.queries().plan(doc)),
                ContextMenu.Item.separator(),
                keyed(IsmsTexts.TO_GRID, GRID_KEYS, query, () -> app.resultsTo(doc, IsmsSettings.Results.GRID)),
                keyed(IsmsTexts.TO_TEXT, TEXT_KEYS, query, () -> app.resultsTo(doc, IsmsSettings.Results.TEXT)),
                keyed(IsmsTexts.TO_FILE, FILE_KEYS, query, () -> app.resultsTo(doc, IsmsSettings.Results.FILE)),
                ContextMenu.Item.separator(),
                keyed(IsmsTexts.TEMPLATE_VALUES, TEMPLATE_KEYS, query, () -> app.dialogs().templateValues(doc)),
                item(IsmsTexts.QUERY_OPTIONS, query, () -> app.dialogs().queryOptions(doc)));
    }

    List<ContextMenu.Item> tools() {
        return List.of(
                item(IsmsTexts.PROFILER, true, app::openProfiler),
                item(IsmsTexts.ACTIVITY_MONITOR, true, app::openActivity),
                ContextMenu.Item.separator(),
                item(IsmsTexts.INDEX_MAINTENANCE, app.connected(), app::indexMaintenance),
                ContextMenu.Item.separator(),
                item(IsmsTexts.OPTIONS, true, () -> app.dialogs().options()));
    }

    List<ContextMenu.Item> window() {
        final List<ContextMenu.Item> items = new ArrayList<>(List.of(
                keyed(IsmsTexts.NEXT_TAB, NEXT_TAB_KEYS, app.documents().size() > 1, () -> app.stepTab(1)),
                keyed(IsmsTexts.PREVIOUS_TAB, PREVIOUS_TAB_KEYS, app.documents().size() > 1,
                        () -> app.stepTab(-1)),
                item(IsmsTexts.CLOSE_ALL, !app.documents().isEmpty(), app::closeAll),
                item(IsmsTexts.RESET_LAYOUT, true, app::resetLayout),
                ContextMenu.Item.separator()));
        final List<IsmsDocument> docs = app.documents();
        for (int i = 0; i < docs.size(); i++) {
            final int index = i;
            items.add(new ContextMenu.Item(GameText.resolve(IsmsTexts.WINDOW_ENTRY.with(i + 1, docs.get(i).title())),
                    true, () -> app.activate(index)));
        }
        return items;
    }

    List<ContextMenu.Item> help() {
        return List.of(
                keyed(IsmsTexts.IQL_REFERENCE, HELP_KEYS, true, () -> app.openPage(IsmsDocument.Kind.REFERENCE,
                        IsmsTexts.IQL_REFERENCE)),
                item(IsmsTexts.SHORTCUTS, true, () -> app.openPage(IsmsDocument.Kind.SHORTCUTS, IsmsTexts.SHORTCUTS)),
                ContextMenu.Item.separator(),
                new ContextMenu.Item(GameText.resolve(IsmsTexts.ABOUT.with(app.productName())), true, app::about));
    }

    /** What the menu of {@code node} offers. */
    List<ContextMenu.Item> node(final IsmsExplorer.Node node) {
        final boolean on = app.connected();
        return switch (node.kind) {
            case ROOT -> List.of(
                    item(IsmsTexts.NEW_QUERY, true, app::newQuery),
                    item(IsmsTexts.REFRESH, true, app::askSchema),
                    ContextMenu.Item.separator(),
                    item(IsmsTexts.START, true, () -> app.action(IsmsActionPayload.ENGINE_START, "", 0)),
                    item(IsmsTexts.STOP, on, () -> app.action(IsmsActionPayload.ENGINE_STOP, "", 0)),
                    item(IsmsTexts.RESTART, on, () -> app.action(IsmsActionPayload.ENGINE_RESTART, "", 0)),
                    ContextMenu.Item.separator(),
                    keyed(IsmsTexts.PROPERTIES, PROPERTIES_KEYS, true, app::showProperties));
            case TABLE -> List.of(
                    item(IsmsTexts.TOP_ROWS, on, () -> app.runNew(node.name, IsmsTemplates.topRows(node.name))),
                    item(IsmsTexts.COUNT_ROWS, true, () -> app.countRows(node.name)),
                    ContextMenu.Item.submenu(GameText.resolve(IsmsTexts.SCRIPT_TABLE), List.of(
                            item(IsmsTexts.QUERY_TO_WINDOW, true, () -> app.openNew(node.name,
                                    IsmsTemplates.query(node.name))),
                            item(IsmsTexts.QUERY_TO_CLIPBOARD, true, () -> app.copy(IsmsTemplates.query(node.name))))),
                    ContextMenu.Item.separator(),
                    keyed(IsmsTexts.PROPERTIES, PROPERTIES_KEYS, true, app::showProperties),
                    item(IsmsTexts.REFRESH, true, app::askSchema));
            case VIEW -> List.of(
                    item(IsmsTexts.QUERY_VIEW, on, () -> app.runNew(node.name, IsmsTemplates.query(node.name))),
                    ContextMenu.Item.submenu(GameText.resolve(IsmsTexts.SCRIPT_VIEW), List.of(
                            item(IsmsTexts.CREATE_TO_WINDOW, true, () -> app.scriptCreate(node)),
                            item(IsmsTexts.DROP_TO_WINDOW, true, () -> app.openNew(node.name,
                                    IsmsTemplates.dropView(node.name))))),
                    ContextMenu.Item.separator(),
                    keyed(IsmsTexts.PROPERTIES, PROPERTIES_KEYS, true, app::showProperties));
            case PROCEDURE -> List.of(
                    item(IsmsTexts.EXECUTE_PROCEDURE, on, () -> app.runNew(node.name, IsmsTemplates.exec(node.name))),
                    item(IsmsTexts.MODIFY, true, () -> app.modify(node)),
                    ContextMenu.Item.submenu(GameText.resolve(IsmsTexts.SCRIPT_PROCEDURE), List.of(
                            item(IsmsTexts.CREATE_TO_WINDOW, true, () -> app.scriptCreate(node)),
                            item(IsmsTexts.DROP_TO_WINDOW, true, () -> app.openNew(node.name,
                                    IsmsTemplates.dropProcedure(node.name))))),
                    ContextMenu.Item.separator(),
                    keyed(IsmsTexts.PROPERTIES, PROPERTIES_KEYS, true, app::showProperties));
            case SERVER -> List.of(
                    item(IsmsTexts.QUERY_SERVER, on, () -> app.runNew(node.name, IsmsTemplates.queryServer(node.name))),
                    keyed(IsmsTexts.PROPERTIES, PROPERTIES_KEYS, true, app::showProperties));
            case INDEX -> List.of(
                    item(IsmsTexts.ANALYZE, on, () -> app.runNew(node.label, IsmsTemplates.maintenance("ANALYZE"))),
                    item(IsmsTexts.REBUILD, on, () -> app.runNew(node.label, IsmsTemplates.maintenance("REINDEX"))),
                    item(IsmsTexts.VACUUM, on, () -> app.runNew(node.label, IsmsTemplates.maintenance("VACUUM"))),
                    ContextMenu.Item.separator(),
                    keyed(IsmsTexts.PROPERTIES, PROPERTIES_KEYS, true, app::showProperties));
            case LOCK -> List.of(
                    item(IsmsTexts.UNLOCK, on, () -> app.action(IsmsActionPayload.UNLOCK, node.name, 0)),
                    keyed(IsmsTexts.PROPERTIES, PROPERTIES_KEYS, true, app::showProperties));
            case LOG -> List.of(
                    item(IsmsTexts.ACTIVITY_MONITOR, true, app::openActivity),
                    item(IsmsTexts.QUERY_LOG, on, () -> app.runNew(node.label, IsmsTemplates.queryLog())));
            case JOB -> List.of(
                    item(IsmsTexts.START_JOB, on, () -> app.action(IsmsActionPayload.JOB_START, node.name, 0)),
                    item(IsmsTexts.PAUSE_JOB, on, () -> app.action(IsmsActionPayload.JOB_PAUSE, node.name, 0)),
                    item(IsmsTexts.DELETE, on, () -> app.dialogs().ask(IsmsTexts.DELETE_JOB.with(node.name),
                            () -> app.action(IsmsActionPayload.JOB_DELETE, node.name, 0))),
                    ContextMenu.Item.separator(),
                    keyed(IsmsTexts.PROPERTIES, PROPERTIES_KEYS, true, app::showProperties));
            case JOBS, AGENT -> List.of(
                    item(IsmsTexts.NEW_JOB, true, () -> app.openTemplate(IsmsTexts.TEMPLATE_JOB)),
                    item(IsmsTexts.REFRESH, true, app::askSchema));
            default -> List.of(
                    item(IsmsTexts.REFRESH, true, app::askSchema),
                    keyed(IsmsTexts.PROPERTIES, PROPERTIES_KEYS, true, app::showProperties));
        };
    }

    /** What the toolbar's server picker drops: the engine's own controls. */
    List<ContextMenu.Item> server() {
        final boolean on = app.connected();
        return List.of(
                item(IsmsTexts.REFRESH, true, app::askSchema),
                ContextMenu.Item.separator(),
                item(IsmsTexts.START, true, () -> app.action(IsmsActionPayload.ENGINE_START, "", 0)),
                item(IsmsTexts.STOP, on, () -> app.action(IsmsActionPayload.ENGINE_STOP, "", 0)),
                item(IsmsTexts.RESTART, on, () -> app.action(IsmsActionPayload.ENGINE_RESTART, "", 0)));
    }

    /** What an Operation of the Activity Monitor offers. */
    List<ContextMenu.Item> operation(final OperationRecord operation) {
        final String id = operation.hasId() ? ShortId.of(operation.id().toString()) : "";
        return List.of(item(IsmsTexts.CANCEL_OPERATION, !id.isEmpty(),
                () -> app.action(IsmsActionPayload.CANCEL, id, 0)));
    }

    /** Every item of the menu bar that has keys, with its keys: what Keyboard Shortcuts lists. */
    List<ContextMenu.Item> keyedItems() {
        final List<ContextMenu.Item> out = new ArrayList<>();
        for (final List<ContextMenu.Item> menu : List.of(file(), edit(), view(), query(), help())) {
            collect(menu, out);
        }
        return out;
    }

    private static void collect(final List<ContextMenu.Item> items, final List<ContextMenu.Item> out) {
        for (final ContextMenu.Item item : items) {
            if (!item.keys().isEmpty()) {
                out.add(item);
            }
            collect(item.children(), out);
        }
    }

    private static ContextMenu.Item item(final TextKey label, final boolean enabled, final Runnable action) {
        return new ContextMenu.Item(GameText.resolve(label), enabled, action);
    }

    private static ContextMenu.Item keyed(final TextKey label, final String keys, final boolean enabled,
                                          final Runnable action) {
        return ContextMenu.Item.keyed(GameText.resolve(label), keys, enabled, action);
    }
}
