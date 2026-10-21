/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.developer;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import dev.jstech.core.client.gui.component.UiComponent;
import dev.jstech.core.client.gui.data.BuiltNode;
import dev.jstech.core.client.gui.data.DataScreenView;
import dev.jstech.core.client.gui.data.PieceView;
import dev.jstech.core.client.gui.data.ScreenFiles;
import dev.jstech.core.gui.flex.FlexBox;
import dev.jstech.core.gui.flex.Placed;
import dev.jstech.core.gui.screen.ElementSpec;
import dev.jstech.core.gui.screen.ScreenFile;
import dev.jstech.core.gui.screen.ScreenFileLines;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.TextKey;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.resources.language.ClientLanguage;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

/**
 * What the developer's inspector shows of a screen declared in a file, worked out again whenever the screen is built
 * or laid out again: the tree of what it is built of, the problems found in it, and the card of a component.
 */
final class InspectorModel {

    private final DataScreenView view;
    private final long revision;
    private final Map<FlexBox, Placed> placedByBox = new IdentityHashMap<>();
    private final Map<ElementSpec, List<Integer>> paths = new IdentityHashMap<>();
    private final Map<List<Integer>, Integer> lines;
    private final List<Problem> problems = new ArrayList<>();

    /** The languages a text is checked in, the series' two. */
    private static final List<String> LANGUAGES = List.of("en_us", "pt_br");
    private static final List<String> TEXT_KEYS = List.of("text", "format", "tooltip", "narration", "placeholder");
    private static final String BIND = "bind";
    private static final String ACTION = "action";

    /* Each language's texts, loaded once while the inspector is open, as the language of the game changes. */
    private static final Map<String, Language> LOADED = new ConcurrentHashMap<>();

    InspectorModel(final DataScreenView view) {
        this.view = view;
        this.revision = view.revision();
        view.placed().ifPresent(root -> root.all().forEach(placed -> placedByBox.put(placed.box(), placed)));
        ScreenFiles.get(view.file()).ifPresent(file -> walk(file.root(), new ArrayList<>()));
        this.lines = readLines(view.file());
        findProblems();
    }

    /** Whether the screen was built or laid out again since this was worked out. */
    boolean stale() {
        return view.revision() != revision;
    }

    /** The rows of the tree, its nodes in order, the ones under a folded node left out. */
    List<Row> rows(final Set<BuiltNode> folded) {
        final List<Row> out = new ArrayList<>();
        view.tree().ifPresent(root -> collect(root, 0, folded, out));
        return out;
    }

    /** The problems found. */
    List<Problem> problems() {
        return problems;
    }

    /** Where the layout put {@code node}, on the game's screen. */
    Optional<int[]> boxOf(final BuiltNode node) {
        final Placed placed = placedByBox.get(node.box());
        if (placed == null) {
            return Optional.empty();
        }
        return Optional.of(new int[] {view.left() + placed.x(), view.top() + placed.y(), placed.width(),
            placed.height()});
    }

    /** The deepest node whose box holds the point, or none. */
    Optional<BuiltNode> at(final double x, final double y) {
        final BuiltNode[] found = new BuiltNode[1];
        view.tree().ifPresent(root -> root.forEach(node -> boxOf(node).ifPresent(box -> {
            if (x >= box[0] && x < box[0] + box[2] && y >= box[1] && y < box[1] + box[3]) {
                found[0] = node;
            }
        })));
        return Optional.ofNullable(found[0]);
    }

    /** The name of the deepest named node whose box holds the point, or none. */
    Optional<String> namedAt(final double x, final double y) {
        final String[] found = new String[1];
        view.tree().ifPresent(root -> root.forEach(node -> node.id().ifPresent(id -> boxOf(node).ifPresent(box -> {
            if (x >= box[0] && x < box[0] + box[2] && y >= box[1] && y < box[1] + box[3]) {
                found[0] = id;
            }
        }))));
        return Optional.ofNullable(found[0]);
    }

    /** The deepest node built from the file whose box holds the point, or none. */
    Optional<BuiltNode> filedAt(final double x, final double y) {
        final BuiltNode[] found = new BuiltNode[1];
        view.tree().ifPresent(root -> root.forEach(node -> {
            if (view.specOf(node).map(paths::containsKey).orElse(false)) {
                boxOf(node).ifPresent(box -> {
                    if (x >= box[0] && x < box[0] + box[2] && y >= box[1] && y < box[1] + box[3]) {
                        found[0] = node;
                    }
                });
            }
        }));
        return Optional.ofNullable(found[0]);
    }

    /** Where in the file's tree {@code node} was written, or none for a part a component made of itself. */
    Optional<List<Integer>> pathOf(final BuiltNode node) {
        return view.specOf(node).map(paths::get);
    }

    /** The node built from the place {@code path} of the file's tree, or none. */
    Optional<BuiltNode> nodeAt(final List<Integer> path) {
        final BuiltNode[] found = new BuiltNode[1];
        view.tree().ifPresent(root -> root.forEach(node -> {
            if (pathOf(node).filter(path::equals).isPresent()) {
                found[0] = node;
            }
        }));
        return Optional.ofNullable(found[0]);
    }

    /** The node's kind's name, as the tree and the card call it. */
    String name(final BuiltNode node) {
        return view.specOf(node).map(spec -> pascal(spec.type().id().getPath()))
                .orElseGet(() -> node.box().name());
    }

    /** "file, line" of the node, when the file says where it is. */
    Optional<String> where(final BuiltNode node) {
        return view.specOf(node).map(paths::get).map(lines::get).map(line -> GameText.resolve(
                DeveloperTexts.FILE_VALUE.with(fileName(), line)));
    }

    /** The card of {@code node}: its kind, its box, what it is bound to, its style, when it redraws, its file. */
    List<Component> card(final BuiltNode node) {
        final List<Component> out = new ArrayList<>();
        out.add(Component.literal(name(node)).withStyle(ChatFormatting.WHITE));
        boxOf(node).ifPresent(box -> out.add(line(DeveloperTexts.BOX, Component.literal(GameText.resolve(
                DeveloperTexts.BOX_VALUE.with(box[0] - view.left(), box[1] - view.top(), box[2], box[3]))),
                ChatFormatting.WHITE)));
        final Optional<String> bind = field(node, BIND);
        bind.ifPresent(name -> out.add(line(DeveloperTexts.BOUND, Component.literal(GameText.resolve(
                DeveloperTexts.BOUND_VALUE.with(name, String.valueOf(view.valueNow(name).orElse(null))))),
                ChatFormatting.LIGHT_PURPLE)));
        field(node, ACTION).ifPresent(action -> out.add(line(DeveloperTexts.DOES, Component.literal(action),
                ChatFormatting.LIGHT_PURPLE)));
        final UiComponent component = node.component();
        final String piece = component instanceof PieceView view1 ? " > " + view1.piece().name()
                .toLowerCase(Locale.ROOT) : "";
        out.add(line(DeveloperTexts.STYLE, Component.literal(view.style().id() + piece), ChatFormatting.WHITE));
        final Component redraws;
        if (component != null && view.stillOnes().contains(component)) {
            redraws = GameText.component(DeveloperTexts.NEVER);
        } else if (bind.isPresent()) {
            redraws = GameText.component(DeveloperTexts.WHEN.with(bind.get()));
        } else {
            redraws = GameText.component(DeveloperTexts.EVERY_FRAME);
        }
        out.add(line(DeveloperTexts.REDRAWS, redraws, ChatFormatting.WHITE));
        where(node).ifPresent(at -> out.add(line(DeveloperTexts.FILE, Component.literal(at), ChatFormatting.WHITE)));
        out.add(GameText.component(DeveloperTexts.COPIES).copy().withStyle(ChatFormatting.DARK_GRAY));
        return out;
    }

    /**
     * Each text of the screen that does not fit its room in one of the series' languages: a component given its width
     * holds its words inside it, any other may grow to the end of the component that holds it.
     */
    List<Overflow> tooLong(final Font font) {
        final List<Overflow> out = new ArrayList<>();
        view.tree().ifPresent(root -> overflows(root, null, font, out));
        return out;
    }

    /** The problem marked on {@code node}, the worst first; none when it has none. */
    Optional<Problem> problemOf(final BuiltNode node) {
        Problem worst = null;
        for (final Problem problem : problems) {
            if (problem.node() == node && (worst == null || problem.error() && !worst.error())) {
                worst = problem;
            }
        }
        return Optional.ofNullable(worst);
    }

    /** The screen's file as a path in its mod's resources, as the card and Ctrl+C give it. */
    String fileName() {
        final ResourceLocation file = view.file();
        return file.getNamespace() + ":" + ScreenFile.FOLDER + "/" + file.getPath() + ".json";
    }

    private void overflows(final BuiltNode node, @Nullable final BuiltNode holder, final Font font,
                           final List<Overflow> out) {
        final Optional<int[]> box = boxOf(node);
        final Optional<String> key = field(node, "text");
        if (box.isPresent() && key.isPresent()) {
            final int room = field(node, "width").isPresent() || holder == null ? box.get()[2]
                    : boxOf(holder).map(held -> held[0] + held[2] - box.get()[0]).orElse(box.get()[2]);
            for (final String code : LANGUAGES) {
                final int over = font.width(language(code).getOrDefault(key.get())) - room;
                if (over > 0) {
                    out.add(new Overflow(node.id().orElse(name(node)), code, over));
                }
            }
        }
        for (final BuiltNode child : node.children()) {
            overflows(child, node, font, out);
        }
    }

    private void collect(final BuiltNode node, final int depth, final Set<BuiltNode> folded, final List<Row> out) {
        out.add(new Row(depth, name(node), detail(node), node.component() == null, node,
                !node.children().isEmpty(), problemOf(node)));
        if (folded.contains(node)) {
            return;
        }
        for (final BuiltNode child : node.children()) {
            collect(child, depth + 1, folded, out);
        }
    }

    /* What a row says after the kind: how many it holds, else its name, its words or what it is bound to. */
    private String detail(final BuiltNode node) {
        if (!node.children().isEmpty()) {
            return Integer.toString(node.children().size());
        }
        if (node.id().isPresent()) {
            return node.id().get();
        }
        return field(node, "text").map(key -> "\"" + Component.translatable(key).getString() + "\"")
                .or(() -> field(node, BIND)).orElse("");
    }

    /* A string field of the node's part of the file, as the file writes it. */
    private Optional<String> field(final BuiltNode node, final String name) {
        return view.specOf(node).flatMap(InspectorModel::json).map(json -> json.get(name))
                .filter(JsonElement::isJsonPrimitive).map(JsonElement::getAsString);
    }

    private static Optional<JsonObject> json(final ElementSpec spec) {
        return ElementSpec.CODEC.encodeStart(JsonOps.INSTANCE, spec).result().filter(JsonElement::isJsonObject)
                .map(JsonElement::getAsJsonObject);
    }

    private void walk(final ElementSpec spec, final List<Integer> path) {
        paths.put(spec, List.copyOf(path));
        for (int i = 0; i < spec.children().size(); i++) {
            path.add(i);
            walk(spec.children().get(i), path);
            path.remove(path.size() - 1);
        }
    }

    private void findProblems() {
        for (final String name : view.unwired()) {
            problems.add(new Problem(true, GameText.resolve(DeveloperTexts.UNWIRED.with(name)), null));
        }
        view.tree().ifPresent(root -> root.forEach(this::check));
        for (final String pair : view.guiLayout().overlaps()) {
            problems.add(new Problem(true, GameText.resolve(DeveloperTexts.OVERLAP.with(pair)), null));
        }
        for (final String name : view.guiLayout().outOfBounds()) {
            problems.add(new Problem(true, GameText.resolve(DeveloperTexts.OUTSIDE.with(name)), null));
        }
    }

    /* A node's texts in each language, and its words for the narrator when the keyboard reaches it. */
    private void check(final BuiltNode node) {
        for (final String key : TEXT_KEYS) {
            field(node, key).ifPresent(text -> {
                for (final String language : LANGUAGES) {
                    if (!language(language).has(text)) {
                        problems.add(new Problem(true, GameText.resolve(DeveloperTexts.NO_TEXT.with(name(node), text,
                                language)), node));
                    }
                }
            });
        }
        final UiComponent component = node.component();
        if (component != null && component.focusable() && component.narration().getString().isBlank()) {
            problems.add(new Problem(false, GameText.resolve(DeveloperTexts.NO_NARRATION.with(name(node))), node));
        }
    }

    private static Language language(final String code) {
        return LOADED.computeIfAbsent(code, language -> ClientLanguage.loadFrom(
                Minecraft.getInstance().getResourceManager(), List.of(language), false));
    }

    /** Forgets the languages' texts, so that the next inspection reads them as the resource packs have them now. */
    static void forgetLanguages() {
        LOADED.clear();
    }

    private static Map<List<Integer>, Integer> readLines(final ResourceLocation file) {
        final ResourceLocation at = ResourceLocation.fromNamespaceAndPath(file.getNamespace(),
                ScreenFile.FOLDER + "/" + file.getPath() + ".json");
        return Minecraft.getInstance().getResourceManager().getResource(at).map(resource -> {
            try (Reader reader = resource.openAsReader()) {
                final StringBuilder text = new StringBuilder();
                final char[] buffer = new char[4096];
                for (int read = reader.read(buffer); read >= 0; read = reader.read(buffer)) {
                    text.append(buffer, 0, read);
                }
                return ScreenFileLines.of(text.toString());
            } catch (final IOException unread) {
                return Map.<List<Integer>, Integer>of();
            }
        }).orElse(Map.of());
    }

    private static Component line(final TextKey label, final Component value, final ChatFormatting colour) {
        return GameText.component(DeveloperTexts.CARD_LINE.with(GameText.of(Component.literal(
                GameText.resolve(label.text())).withStyle(ChatFormatting.GRAY)), GameText.of(value.copy()
                .withStyle(colour)))).copy();
    }

    private static String pascal(final String path) {
        final StringBuilder out = new StringBuilder();
        boolean up = true;
        for (final char c : path.toCharArray()) {
            if (c == '_' || c == '/') {
                up = true;
            } else {
                out.append(up ? Character.toUpperCase(c) : c);
                up = false;
            }
        }
        return out.toString();
    }

    /**
     * One row of the tree.
     *
     * @param depth   how deep it is
     * @param name    its kind
     * @param detail  what follows its kind
     * @param layout  whether it only lays others out
     * @param node    what it is
     * @param parent  whether it holds others
     * @param problem the worst problem marked on it
     */
    record Row(int depth, String name, String detail, boolean layout, BuiltNode node, boolean parent,
               Optional<Problem> problem) {
    }

    /**
     * A problem found.
     *
     * @param error whether it is an error, or only a warning
     * @param text  what it says
     * @param node  where it is, when it is in one place
     */
    /**
     * A text past its room.
     *
     * @param component the component, by its name or its kind
     * @param language  the language it does not fit in
     * @param over      by how many pixels
     */
    record Overflow(String component, String language, int over) {
    }

    record Problem(boolean error, String text, @Nullable BuiltNode node) {
    }
}
