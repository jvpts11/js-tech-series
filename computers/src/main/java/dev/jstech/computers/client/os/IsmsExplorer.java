/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.os;

import dev.jstech.computers.JsComputers;
import dev.jstech.computers.gui.layout.IsmsLayout;
import dev.jstech.computers.machine.IqlTables;
import dev.jstech.computers.operation.index.IndexHealth;
import dev.jstech.computers.operation.payload.IsmsSchemaPayload;
import dev.jstech.core.client.gui.component.Draw;
import dev.jstech.core.client.gui.component.Texts;
import dev.jstech.core.palette.Palette;
import dev.jstech.core.palette.PaletteHolder;
import dev.jstech.core.palette.Palettes;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.TextKey;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import org.jetbrains.annotations.Nullable;

/**
 * The IQL Server Management Studio's Object Explorer: the network as a tree of what it holds and runs. The tables
 * with their columns and how many rows each has, the saved views and procedures, the servers, the index and the
 * items held by hand, and the Automation Agent's jobs, built again from every schema the network sends while
 * keeping what the player opened open and what they picked picked.
 *
 * <p>It also keeps, for each table, when the studio last saw its row count change, which the Object Explorer
 * Details shows as the table's last change.
 */
@PaletteHolder
final class IsmsExplorer {

    private final Set<String> expanded = new HashSet<>();
    private final List<Row> visible = new ArrayList<>();
    private final Map<String, Seen> seen = new HashMap<>();
    private Node root = new Node(Kind.ROOT, "", "", "", "");
    @Nullable
    private IsmsSchemaPayload schema;
    @Nullable
    private Node selected;
    private int scroll;

    /** The colours of the nodes' icons, {@code jsc:app/isms/icons}, the same in every age of the studio. */
    private static final Palette<Icons> ICONS = Palettes.declare(JsComputers.MODID, "app/isms/icons",
            new Icons(0xFF5077A8, 0xFF41608A, 0xFFF2CA4C, 0xFFC79A1F, 0xFFECC233, 0xFFB08A00, 0xFFB07FD8, 0xFFE08A8A,
                    0xFFF0A64E, 0xFF9AB0C8, 0xFFC8C8C8, 0xFF5AA05A, 0xFF808080, 0xFFFFFFFF));
    private static final int ICON = 7;
    private static final String TABLES_KEY = "tables";

    IsmsExplorer() {
        expanded.add(Kind.ROOT.name());
        expanded.add(TABLES_KEY);
        rebuild(null, IsmsLook.STUDIO_2012, "");
    }

    /**
     * Builds the tree again from {@code payload}, keeping what was open and what was picked, and notes the time
     * {@code now} against every table whose row count changed.
     */
    void rebuild(@Nullable final IsmsSchemaPayload payload, final IsmsLook look, final String now) {
        this.schema = payload;
        final String key = selected == null ? null : selected.key;
        if (payload != null) {
            for (int i = 0; i < IqlTables.TABLES.size() && i < payload.tableRows().size(); i++) {
                final String table = IqlTables.TABLES.get(i);
                final int rows = payload.tableRows().get(i);
                final Seen before = seen.get(table);
                if (before == null || before.rows() != rows) {
                    seen.put(table, new Seen(rows, now));
                }
            }
        }
        root = build(payload, look);
        selected = key == null ? null : find(root, key);
        refresh();
    }

    /** The node picked, or null. */
    @Nullable
    Node selected() {
        return selected;
    }

    void select(@Nullable final Node node) {
        this.selected = node;
    }

    /** The node of {@code key}, as {@link Node#key} names nodes, or null. */
    @Nullable
    Node node(final String key) {
        return find(root, key);
    }

    Node root() {
        return root;
    }

    /** Opens or closes {@code node}. */
    void toggle(final Node node) {
        if (!node.children.isEmpty() && !expanded.remove(node.key)) {
            expanded.add(node.key);
        }
        refresh();
    }

    /** Opens {@code node} and every node above it, so it can be seen. */
    void reveal(final Node node) {
        expanded.add(node.key);
        refresh();
    }

    /** Closes every node, the root staying open, as the explorer's own collapse button does. */
    void collapseAll() {
        expanded.clear();
        expanded.add(Kind.ROOT.name());
        refresh();
    }

    boolean isExpanded(final Node node) {
        return expanded.contains(node.key);
    }

    void scrollBy(final int rows, final int fit) {
        scroll = Math.max(0, Math.min(Math.max(0, visible.size() - fit), scroll + rows));
    }

    /** The row under {@code y}, counted from the tree's top, or null below the last. */
    @Nullable
    Row rowAt(final int top, final int y, final int fit) {
        final int index = scrollStart(fit) + (y - top) / IsmsLayout.TREE_PITCH;
        return y >= top && index >= 0 && index < visible.size() ? visible.get(index) : null;
    }

    /** Where {@code node}'s row is drawn, its top counted from the tree's top, or -1 when it is not shown. */
    int rowTop(final Node node, final int fit) {
        for (int i = scrollStart(fit); i < visible.size() && i < scrollStart(fit) + fit; i++) {
            if (visible.get(i).node() == node) {
                return (i - scrollStart(fit)) * IsmsLayout.TREE_PITCH;
            }
        }
        return -1;
    }

    /** Draws the tree in its rectangle, the picked node lit. */
    void render(final GuiGraphics g, final Font font, final IsmsLook.Colours c, final int x, final int y, final int w,
                final int h, final int mouseX, final int mouseY) {
        final int fit = Math.max(1, h / IsmsLayout.TREE_PITCH);
        g.fill(x, y, x + w, y + h, c.panel());
        Draw.pushScissor(g, x, y, x + w, y + h);
        final int start = scrollStart(fit);
        for (int i = start; i < visible.size() && i < start + fit; i++) {
            final Row row = visible.get(i);
            final int ry = y + (i - start) * IsmsLayout.TREE_PITCH;
            final boolean lit = row.node() == selected;
            if (lit) {
                g.fill(x, ry, x + w, ry + IsmsLayout.TREE_PITCH, c.select());
                Draw.outline(g, x, ry, w, IsmsLayout.TREE_PITCH, c.selectEdge());
            } else if (mouseX >= x && mouseX < x + w && mouseY >= ry && mouseY < ry + IsmsLayout.TREE_PITCH) {
                g.fill(x, ry, x + w, ry + IsmsLayout.TREE_PITCH, c.hover());
            }
            final int indent = x + 3 + row.depth() * IsmsLayout.INDENT;
            if (!row.node().children.isEmpty()) {
                twisty(g, indent, ry + 3, isExpanded(row.node()), c.dim());
            }
            icon(g, row.node().kind, indent + 6, ry + 1);
            final int labelX = indent + 6 + ICON + 3;
            final String label = Texts.clip(font, row.node().label, x + w - labelX - 2);
            final int ground = lit ? c.select() : c.panel();
            Draw.text(g, font, label, labelX, ry + 1, c.text(), ground);
            if (!row.node().note.isEmpty()) {
                final int noteX = labelX + font.width(label) + 3;
                Draw.text(g, font, Texts.clip(font, row.node().note, x + w - noteX - 2), noteX, ry + 1, c.dim(),
                        ground);
            }
        }
        Draw.popScissor(g);
    }

    /** What the Properties Window lists for {@code node}: a name for each fact and what it is. */
    List<Property> properties(final Node node) {
        final List<Property> out = new ArrayList<>();
        out.add(new Property(IsmsTexts.NAME, node.name.isEmpty() ? node.label : node.name));
        out.add(new Property(IsmsTexts.KIND, GameText.resolve(kindWord(node.kind))));
        final IsmsSchemaPayload s = schema;
        switch (node.kind) {
            case TABLE -> {
                out.add(new Property(IsmsTexts.ROWS_COLUMN, Integer.toString(rowsOf(node.name))));
                out.add(new Property(IsmsTexts.COLUMNS, String.join(", ", columnsOf(node.name))));
                out.add(new Property(IsmsTexts.LAST_CHANGE, lastChange(node.name)));
            }
            case JOB -> {
                final IsmsSchemaPayload.Job job = s == null ? null : jobNamed(s, node.name);
                if (job != null) {
                    out.add(new Property(IsmsTexts.STATE, GameText.resolve(job.paused() ? IsmsTexts.PAUSED
                            : IsmsTexts.RUNNING)));
                    out.add(new Property(IsmsTexts.TRIGGER, job.trigger()));
                }
            }
            case INDEX -> {
                if (s != null) {
                    out.add(new Property(IsmsTexts.STATE, GameText.resolve(health(s.index().state()))));
                    out.add(new Property(IsmsTexts.ROWS_COLUMN, Integer.toString(s.index().catalog())));
                }
            }
            case LOCK -> out.add(new Property(IsmsTexts.QUANTITY, node.note));
            case ROOT -> {
                if (s != null) {
                    out.add(new Property(IsmsTexts.VERSION, s.engine().version()));
                    out.add(new Property(IsmsTexts.STATE, GameText.resolve(s.engine().state().word())));
                }
            }
            default -> {
                // A folder, a column, a view, a procedure or a server says its name and kind, and nothing more.
            }
        }
        return out;
    }

    /**
     * What the Object Explorer Details lists for {@code node}: its children, or for a node with none its parent's,
     * with the columns that suit them, narrowed to the names holding {@code search}.
     */
    Details details(final Node node, final String search) {
        final Node shown = node.children.isEmpty() ? parentOf(root, node) : node;
        final Node at = shown == null ? root : shown;
        final String needle = search.toLowerCase(Locale.ROOT);
        final List<List<String>> rows = new ArrayList<>();
        final List<TextKey> columns;
        if (at.kind == Kind.TABLES) {
            columns = List.of(IsmsTexts.NAME, IsmsTexts.ROWS_COLUMN, IsmsTexts.COLUMNS, IsmsTexts.HOLDS,
                    IsmsTexts.LAST_CHANGE);
            for (final Node table : at.children) {
                final List<String> names = columnsOf(table.name);
                rows.add(List.of(table.name, String.format(Locale.ROOT, "%,d", rowsOf(table.name)),
                        Integer.toString(names.size()), String.join(", ", names), lastChange(table.name)));
            }
        } else if (at.kind == Kind.JOBS) {
            columns = List.of(IsmsTexts.NAME, IsmsTexts.STATE, IsmsTexts.TRIGGER);
            for (final Node job : at.children) {
                final IsmsSchemaPayload.Job found = schema == null ? null : jobNamed(schema, job.name);
                rows.add(List.of(job.name, GameText.resolve(found != null && found.paused() ? IsmsTexts.PAUSED
                        : IsmsTexts.RUNNING), found == null ? "" : found.trigger()));
            }
        } else if (at.kind == Kind.LOCKS) {
            columns = List.of(IsmsTexts.NAME, IsmsTexts.QUANTITY);
            for (final Node lock : at.children) {
                rows.add(List.of(lock.label, lock.note));
            }
        } else {
            columns = List.of(IsmsTexts.NAME, IsmsTexts.KIND);
            for (final Node child : at.children) {
                rows.add(List.of(child.label, GameText.resolve(kindWord(child.kind))));
            }
        }
        rows.removeIf(row -> !needle.isEmpty() && !row.get(0).toLowerCase(Locale.ROOT).contains(needle));
        final List<String> headings = new ArrayList<>();
        columns.forEach(column -> headings.add(GameText.resolve(column)));
        return new Details(crumbs(at), headings, rows);
    }

    /** How many rows {@code table} held at the last schema, or 0 before one came. */
    int rowsOf(final String table) {
        final int index = IqlTables.TABLES.indexOf(table);
        return schema == null || index < 0 || index >= schema.tableRows().size() ? 0 : schema.tableRows().get(index);
    }

    /** The saved view or procedure named {@code name}, or null. */
    @Nullable
    IsmsSchemaPayload.Saved saved(final Kind kind, final String name) {
        if (schema == null) {
            return null;
        }
        for (final IsmsSchemaPayload.Saved object : kind == Kind.VIEW ? schema.views() : schema.procedures()) {
            if (object.name().equals(name)) {
                return object;
            }
        }
        return null;
    }

    /** Every name the explorer knows, which IntelliSense offers: tables, columns, views and procedures. */
    List<String> names() {
        final List<String> out = new ArrayList<>(IqlTables.TABLES);
        for (final String table : IqlTables.TABLES) {
            for (final String column : columnsOf(table)) {
                if (!out.contains(column)) {
                    out.add(column);
                }
            }
        }
        if (schema != null) {
            schema.views().forEach(view -> out.add(view.name()));
            schema.procedures().forEach(procedure -> out.add(procedure.name()));
        }
        return out;
    }

    /** When {@code table}'s row count last changed while the studio watched, as the world's clock read. */
    String lastChange(final String table) {
        final Seen when = seen.get(table);
        return when == null ? "" : when.at();
    }

    /** What a kind of node is called. */
    static TextKey kindWord(final Kind kind) {
        return switch (kind) {
            case ROOT -> IsmsTexts.KIND_NETWORK;
            case TABLE -> IsmsTexts.KIND_TABLE;
            case COLUMN -> IsmsTexts.KIND_COLUMN;
            case VIEW -> IsmsTexts.KIND_VIEW;
            case PROCEDURE -> IsmsTexts.KIND_PROCEDURE;
            case SERVER -> IsmsTexts.KIND_SERVER;
            case JOB -> IsmsTexts.KIND_JOB;
            case LOCK -> IsmsTexts.KIND_LOCK;
            case INDEX -> IsmsTexts.KIND_INDEX;
            case LOG -> IsmsTexts.KIND_LOG;
            default -> IsmsTexts.KIND_FOLDER;
        };
    }

    /** The word for how far the index is to be trusted. */
    static TextKey health(final IndexHealth.State state) {
        return switch (state) {
            case OK -> IsmsTexts.HEALTHY;
            case STALE -> IsmsTexts.STALE;
            case FRAGMENTED -> IsmsTexts.FRAGMENTED;
        };
    }

    private static List<String> columnsOf(final String table) {
        final List<String> columns = IqlTables.columnsOf(table);
        return columns == null ? List.of() : columns;
    }

    private int scrollStart(final int fit) {
        return Math.max(0, Math.min(scroll, Math.max(0, visible.size() - fit)));
    }

    private void refresh() {
        visible.clear();
        add(root, 0);
    }

    private void add(final Node node, final int depth) {
        visible.add(new Row(node, depth));
        if (isExpanded(node)) {
            for (final Node child : node.children) {
                add(child, depth + 1);
            }
        }
    }

    /* The tree as the schema says the network stands, named in the look's words. */
    private static Node build(@Nullable final IsmsSchemaPayload s, final IsmsLook look) {
        final Node tables = folder(Kind.TABLES, TABLES_KEY, IsmsTexts.TABLES, false);
        for (int i = 0; i < IqlTables.TABLES.size(); i++) {
            final String table = IqlTables.TABLES.get(i);
            final Node node = new Node(Kind.TABLE, "table/" + table, table, "", table);
            for (final String column : columnsOf(table)) {
                node.children.add(new Node(Kind.COLUMN, "column/" + table + "/" + column, column, "", column));
            }
            tables.children.add(node);
        }
        final Node views = folder(Kind.FOLDER, "views", IsmsTexts.VIEWS, true);
        final Node procedures = folder(Kind.FOLDER, "procedures", IsmsTexts.PROCEDURES, true);
        final Node storage = folder(Kind.FOLDER, "storage", IsmsTexts.STORAGE, true);
        final Node locks = folder(Kind.LOCKS, "locks", IsmsTexts.LOCKS, true);
        final Node jobs = folder(Kind.JOBS, "jobs", IsmsTexts.JOBS, true);
        String indexNote = "";
        String rootNote = "";
        String network = "";
        if (s != null) {
            network = GameText.resolve(s.network());
            rootNote = s.engine().version().isEmpty() ? "" : "(" + GameText.resolve(IsmsTexts.SHORT_PRODUCT.with(
                    s.engine().version())) + ")";
            s.views().forEach(v -> views.children.add(new Node(Kind.VIEW, "view/" + v.name(), v.name(), "", v.name())));
            s.procedures().forEach(p -> procedures.children.add(new Node(Kind.PROCEDURE, "procedure/" + p.name(),
                    p.name(), "", p.name())));
            s.servers().forEach(name -> storage.children.add(new Node(Kind.SERVER, "server/" + name, name, "",
                    name)));
            for (final IsmsSchemaPayload.Lock lock : s.locks()) {
                locks.children.add(new Node(Kind.LOCK, "lock/" + lock.item(), GameText.resolve(lock.name()),
                        "x" + lock.qty(), lock.item()));
            }
            for (final IsmsSchemaPayload.Job job : s.jobs()) {
                jobs.children.add(new Node(Kind.JOB, "job/" + job.name(), job.name(), job.paused()
                        ? "(" + GameText.resolve(IsmsTexts.PAUSED) + ")" : "", job.name()));
            }
            indexNote = "(" + GameText.resolve(health(s.index().state())) + ")";
        }
        counted(views);
        counted(procedures);
        counted(storage);
        counted(locks);
        counted(jobs);
        final Node programmability = folder(Kind.FOLDER, "programmability", IsmsTexts.PROGRAMMABILITY, false);
        programmability.children.add(procedures);
        final Node management = folder(Kind.MANAGEMENT, "management", look.analyzer() ? IsmsTexts.MAINTENANCE
                : IsmsTexts.MANAGEMENT, false);
        management.children.add(new Node(Kind.INDEX, "index", GameText.resolve(IsmsTexts.INDEX), indexNote, ""));
        management.children.add(locks);
        management.children.add(new Node(Kind.LOG, "log", GameText.resolve(IsmsTexts.OPERATIONS_LOG), "", ""));
        final Node agent = folder(Kind.AGENT, "agent", IsmsTexts.AGENT, false);
        agent.children.add(jobs);
        final Node top = new Node(Kind.ROOT, Kind.ROOT.name(), network, rootNote, network);
        top.children.addAll(List.of(tables, views, programmability, storage, management, agent));
        return top;
    }

    private static Node folder(final Kind kind, final String key, final TextKey label, final boolean counts) {
        final Node node = new Node(kind, key, GameText.resolve(label), "", "");
        node.counts = counts;
        return node;
    }

    private static void counted(final Node folder) {
        if (folder.counts) {
            folder.note = "(" + folder.children.size() + ")";
        }
    }

    @Nullable
    private static Node find(final Node node, final String key) {
        if (node.key.equals(key)) {
            return node;
        }
        for (final Node child : node.children) {
            final Node found = find(child, key);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    @Nullable
    private static Node parentOf(final Node node, final Node child) {
        for (final Node c : node.children) {
            if (c == child) {
                return node;
            }
            final Node found = parentOf(c, child);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    /* The way from the root to {@code node}, a name a step, as the Details page's crumbs show it. */
    private String crumbs(final Node node) {
        final List<String> way = new ArrayList<>();
        Node at = node;
        while (at != null) {
            way.add(0, at.label);
            at = parentOf(root, at);
        }
        return String.join(" > ", way);
    }

    @Nullable
    private static IsmsSchemaPayload.Job jobNamed(final IsmsSchemaPayload s, final String name) {
        for (final IsmsSchemaPayload.Job job : s.jobs()) {
            if (job.name().equals(name)) {
                return job;
            }
        }
        return null;
    }

    private static void twisty(final GuiGraphics g, final int x, final int y, final boolean open, final int colour) {
        if (open) {
            for (int i = 0; i < 3; i++) {
                g.fill(x + i, y + i, x + 5 - i, y + i + 1, colour);
            }
        } else {
            for (int i = 0; i < 3; i++) {
                g.fill(x + i, y + i - 1, x + i + 1, y + 4 - i, colour);
            }
        }
    }

    private static void icon(final GuiGraphics g, final Kind kind, final int x, final int y) {
        final Icons c = ICONS.get();
        switch (kind) {
            case ROOT -> box(g, x, y, c.network(), c.networkEdge());
            case TABLE -> {
                box(g, x, y, c.tableFill(), c.tableEdge());
                g.fill(x + 1, y + 1, x + ICON - 1, y + 3, c.tableTop());
            }
            case COLUMN -> g.fill(x + 2, y + 2, x + ICON - 2, y + ICON - 2, c.column());
            case VIEW -> box(g, x, y, c.view(), c.view());
            case PROCEDURE -> box(g, x, y, c.procedure(), c.procedure());
            case JOB -> box(g, x, y, c.job(), c.job());
            case SERVER -> box(g, x, y, c.server(), c.server());
            case INDEX, LOCK, LOG, LOCKS, MANAGEMENT -> box(g, x, y, c.gear(), c.column());
            case AGENT, JOBS -> box(g, x, y, c.agent(), c.agent());
            default -> box(g, x, y, c.folder(), c.folderEdge());
        }
    }

    private static void box(final GuiGraphics g, final int x, final int y, final int fill, final int edge) {
        g.fill(x, y, x + ICON, y + ICON, edge);
        g.fill(x + 1, y + 1, x + ICON - 1, y + ICON - 1, fill);
    }

    /** What a node is, which picks its icon and its menu. */
    enum Kind { ROOT, FOLDER, TABLES, TABLE, COLUMN, VIEW, PROCEDURE, SERVER, MANAGEMENT, INDEX, LOCKS, LOCK, LOG,
        AGENT, JOBS, JOB }

    /**
     * A node of the tree.
     *
     * @see #key
     */
    static final class Node {

        final Kind kind;
        /** What names the node from one schema to the next, so it stays open and picked. */
        final String key;
        final String label;
        /** The dim words after the label: a count, a state, a version. */
        String note;
        /** The name of the thing the node stands for, which its menu acts on. */
        final String name;
        final List<Node> children = new ArrayList<>();
        boolean counts;

        Node(final Kind kind, final String key, final String label, final String note, final String name) {
            this.kind = kind;
            this.key = key;
            this.label = label;
            this.note = note;
            this.name = name;
        }
    }

    /** A row of the tree as it is drawn: a node and how deep it sits. */
    record Row(Node node, int depth) {
    }

    /** A fact the Properties Window lists. */
    record Property(TextKey name, String value) {
    }

    /** What the Object Explorer Details shows: where it is, its columns, and a row for each thing. */
    record Details(String crumbs, List<String> columns, List<List<String>> rows) {
    }

    /** A table's row count as last seen, and when it was seen to change. */
    private record Seen(int rows, String at) {
    }

    /** The icons' colours. */
    private record Icons(int network, int networkEdge, int folder, int folderEdge, int tableTop, int tableEdge,
                         int view, int procedure, int job, int server, int gear, int agent, int column,
                         int tableFill) {
    }
}
