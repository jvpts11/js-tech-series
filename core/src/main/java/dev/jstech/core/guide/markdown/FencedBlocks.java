/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.guide.markdown;

import dev.jstech.core.guide.GuideBlock;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * The blocks of a manual a Markdown file writes as a fence named for them, each line of the fence a name, a colon and
 * its value, as short as each needs. Pure.
 *
 * <pre>{@code
 * ```recipes
 * type: mymod:firing
 * making: mymod:brick
 * ```
 * ```multiblock
 * id: mymod:coke_oven
 * caption: The coke oven, built
 * height: 120
 * ```
 * ```table
 * source: mymod:cable_reach
 * caption: How far each cable reaches
 * heads: Cable | Reach
 * ```
 * ```session
 * look: mymod:guide/terminal/dos
 * title: MC-DOS 6.22
 * prompt: C:\>
 * > dir
 *  Directory of C:\
 * ```
 * }</pre>
 *
 * <p>Besides those, {@code structure} (as {@code multiblock}, of a saved structure), {@code command} ({@code id}),
 * {@code plan} ({@code caption}, then {@code part: item | label} and {@code optional: label}), {@code views}
 * ({@code item}, then {@code callout: number view u v | text}), {@code custom kind height} and
 * {@code drawing kind height caption}, the last two holding their data as JSON.
 */
public final class FencedBlocks {

    /** The names of the fences it reads. */
    public static final Set<String> KINDS = Set.of("recipes", "multiblock", "structure", "command", "table",
            "session", "plan", "views", "custom", "drawing");

    private static final String TYPED = ">";
    private static final String PROMPT = "prompt";
    private static final String BETWEEN = "|";
    /** How tall a scene of a multiblock or a structure is drawn, in pixels, when its fence does not say. */
    private static final int SCENE_HEIGHT = 120;

    private FencedBlocks() {
    }

    /**
     * The block a fence of {@code kind} writes, its opening line's words {@code words} and its lines {@code body};
     * none, with why in {@code problems} at {@code line}, when it does not write one.
     */
    public static Optional<GuideBlock> read(final String kind, final String[] words, final List<String> body,
                                            final int line, final List<ManualFileException> problems) {
        final Map<String, String> said = fields(body);
        try {
            return Optional.of(switch (kind) {
                case "recipes" -> new GuideBlock.Recipes(needed(said, "type"), said.getOrDefault("making", ""));
                case "multiblock" -> new GuideBlock.Multiblock(needed(said, "id"), view(said),
                        number(said, "height", SCENE_HEIGHT), said.getOrDefault("caption", ""));
                case "structure" -> new GuideBlock.Structure(needed(said, "id"), view(said),
                        number(said, "height", SCENE_HEIGHT), said.getOrDefault("caption", ""));
                case "command" -> new GuideBlock.Command(needed(said, "id"));
                case "table" -> new GuideBlock.Table(said.getOrDefault("caption", ""), heads(said),
                        List.of(), needed(said, "source"));
                case "session" -> session(said, body);
                case "plan" -> plan(said, body);
                case "views" -> views(said, body);
                case "custom" -> new GuideBlock.Custom(word(words, 1), Integer.parseInt(word(words, 2)),
                        String.join("\n", body).strip());
                default -> new GuideBlock.Picture("", word(words, 1), String.join("\n", body).strip(), 0,
                        Integer.parseInt(word(words, 2)), MarkdownText.of(String.join(" ", List.of(words)
                        .subList(Math.min(3, words.length), words.length))), 1, 1);
            });
        } catch (final IllegalArgumentException | IndexOutOfBoundsException wrong) {
            problems.add(new ManualFileException(line, "a " + kind + " fence does not say what it needs: "
                    + wrong.getMessage()));
            return Optional.empty();
        }
    }

    private static GuideBlock.Session session(final Map<String, String> said, final List<String> body) {
        final List<GuideBlock.SessionLine> lines = new ArrayList<>();
        String prompt = said.getOrDefault(PROMPT, "");
        boolean head = true;
        for (final String line : body) {
            final String name = nameOf(line);
            if (head && (name.equals("look") || name.equals("title"))) {
                continue;
            }
            head = false;
            if (name.equals(PROMPT)) {
                prompt = line.substring(line.indexOf(':') + 1).strip();
            } else if (line.startsWith(TYPED)) {
                lines.add(new GuideBlock.SessionLine(prompt, line.substring(TYPED.length()).strip(), true));
            } else {
                lines.add(new GuideBlock.SessionLine("", line, false));
            }
        }
        return new GuideBlock.Session(needed(said, "look"), said.getOrDefault("title", ""), lines);
    }

    private static GuideBlock.Plan plan(final Map<String, String> said, final List<String> body) {
        final List<GuideBlock.PlanPart> parts = new ArrayList<>();
        for (final String line : body) {
            final String name = nameOf(line);
            final String value = line.substring(line.indexOf(':') + 1).strip();
            if (name.equals("part")) {
                final int cut = value.indexOf(BETWEEN);
                parts.add(new GuideBlock.PlanPart(value.substring(0, Math.max(0, cut)).strip(),
                        MarkdownText.of(value.substring(cut + 1)), false));
            } else if (name.equals("optional")) {
                parts.add(new GuideBlock.PlanPart("", MarkdownText.of(value), true));
            }
        }
        return new GuideBlock.Plan(said.getOrDefault("caption", ""), parts);
    }

    private static GuideBlock.Views views(final Map<String, String> said, final List<String> body) {
        final List<GuideBlock.Callout> callouts = new ArrayList<>();
        for (final String line : body) {
            if (nameOf(line).equals("callout")) {
                final String value = line.substring(line.indexOf(':') + 1).strip();
                final int cut = value.indexOf(BETWEEN);
                final String[] where = value.substring(0, cut).strip().split("\\s+");
                callouts.add(new GuideBlock.Callout(Integer.parseInt(where[0]), viewNamed(where[1]),
                        Integer.parseInt(where[2]), Integer.parseInt(where[3]), MarkdownText.of(value.substring(cut
                        + 1))));
            }
        }
        return new GuideBlock.Views(needed(said, "item"), callouts);
    }

    private static GuideBlock.View viewNamed(final String name) {
        for (final GuideBlock.View view : GuideBlock.View.values()) {
            if (view.serializedName().equalsIgnoreCase(name)) {
                return view;
            }
        }
        throw new IllegalArgumentException("a callout stands on the top, the front or the side, not " + name);
    }

    private static GuideBlock.SceneView view(final Map<String, String> said) {
        final GuideBlock.SceneView corner = GuideBlock.SceneView.CORNER;
        return new GuideBlock.SceneView(decimal(said, "yaw", corner.yaw()), decimal(said, "pitch", corner.pitch()),
                decimal(said, "zoom", corner.zoom()));
    }

    private static List<String> heads(final Map<String, String> said) {
        final List<String> heads = new ArrayList<>();
        for (final String head : said.getOrDefault("heads", "").split("\\|")) {
            if (!head.isBlank()) {
                heads.add(MarkdownText.of(head));
            }
        }
        return heads;
    }

    /* Each line's name and value, the first of each name. */
    private static Map<String, String> fields(final List<String> body) {
        final Map<String, String> said = new LinkedHashMap<>();
        for (final String line : body) {
            final String name = nameOf(line);
            if (!name.isEmpty()) {
                said.putIfAbsent(name, line.substring(line.indexOf(':') + 1).strip());
            }
        }
        return said;
    }

    /* The name a line gives before its colon, a word alone; empty for a line that gives none. */
    private static String nameOf(final String line) {
        final int colon = line.indexOf(':');
        if (colon <= 0) {
            return "";
        }
        final String name = line.substring(0, colon).strip();
        return name.matches("[a-z_]+") ? name : "";
    }

    private static String needed(final Map<String, String> said, final String name) {
        final String value = said.getOrDefault(name, "");
        if (value.isEmpty()) {
            throw new IllegalArgumentException("its " + name);
        }
        return value;
    }

    private static int number(final Map<String, String> said, final String name, final int otherwise) {
        return said.containsKey(name) ? Integer.parseInt(said.get(name)) : otherwise;
    }

    private static float decimal(final Map<String, String> said, final String name, final float otherwise) {
        return said.containsKey(name) ? Float.parseFloat(said.get(name)) : otherwise;
    }

    private static String word(final String[] words, final int index) {
        if (index >= words.length) {
            throw new IllegalArgumentException("its kind and height after its name");
        }
        return words[index];
    }
}
