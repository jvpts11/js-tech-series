/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.config.format;

import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.comments.CommentLine;
import org.yaml.snakeyaml.comments.CommentType;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import org.yaml.snakeyaml.error.YAMLException;
import org.yaml.snakeyaml.nodes.MappingNode;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.NodeTuple;
import org.yaml.snakeyaml.nodes.ScalarNode;
import org.yaml.snakeyaml.representer.Representer;

/**
 * YAML, read and written with SnakeYAML, which the Core carries inside its jar.
 *
 * <p>A file is read with the safe constructor only, which builds maps, lists, text, numbers and booleans and never a
 * class a file names: the versions of SnakeYAML before 2.0 built whatever a file asked for, which is how a YAML file
 * could run code on the machine reading it. The reading is also held to a few aliases, a shallow nesting and a small
 * size, since a settings file needs none of those and a file built to exhaust the reader uses all three.
 */
public final class YamlConfigFormat implements IConfigFormat {

    /** How many aliases a file may use; a settings file needs none, and a thousand-fold alias bomb needs many. */
    private static final int MOST_ALIASES = 16;
    private static final int DEEPEST_NESTING = 32;
    /** The largest file read, in characters: far more than any settings file, far less than one built to choke. */
    private static final int LARGEST_FILE = 1 << 20;
    private static final int INDENT = 2;

    YamlConfigFormat() {
    }

    @Override
    public String extension() {
        return "yaml";
    }

    @Override
    public boolean keepsComments() {
        return true;
    }

    @Override
    public Map<String, Object> read(final byte[] file) throws ConfigFormatException {
        final Object loaded;
        try {
            loaded = yaml().load(new String(file, StandardCharsets.UTF_8));
        } catch (final YAMLException e) {
            throw new ConfigFormatException("not YAML: " + e.getMessage(), e);
        }
        if (loaded == null) {
            return new LinkedHashMap<>();
        }
        if (!(loaded instanceof Map<?, ?> map)) {
            throw new ConfigFormatException("not a YAML map at the top of the file");
        }
        return PlainValues.map(map);
    }

    @Override
    public byte[] write(final Map<String, Object> values, final IConfigComments comments) {
        final Yaml yaml = yaml();
        final Node root = yaml.represent(values);
        final List<String> header = comments.at(List.of());
        final StringBuilder out = new StringBuilder();
        if (root instanceof MappingNode map && !map.getValue().isEmpty()) {
            comment(map, List.of(), comments);
            if (!header.isEmpty()) {
                final Node first = map.getValue().get(0).getKeyNode();
                final List<CommentLine> lines = new ArrayList<>(block(header));
                lines.add(new CommentLine(null, null, "", CommentType.BLANK_LINE));
                if (first.getBlockComments() != null) {
                    lines.addAll(first.getBlockComments());
                }
                first.setBlockComments(lines);
            }
            final StringWriter writer = new StringWriter();
            yaml.serialize(root, writer);
            out.append(writer);
        } else {
            for (final String line : header) {
                out.append(line.isEmpty() ? "#" : "# " + line).append('\n');
            }
            out.append("{}\n");
        }
        return out.toString().getBytes(StandardCharsets.UTF_8);
    }

    /** Puts each place's comment above its key, through every map under this one. */
    private static void comment(final MappingNode map, final List<String> at, final IConfigComments comments) {
        boolean first = true;
        for (final NodeTuple tuple : map.getValue()) {
            if (!(tuple.getKeyNode() instanceof ScalarNode key)) {
                continue;
            }
            final List<String> path = new ArrayList<>(at);
            path.add(key.getValue());
            final List<String> lines = comments.at(path);
            if (!lines.isEmpty()) {
                final List<CommentLine> above = new ArrayList<>();
                if (!first) {
                    // A described value stands apart from the one above it.
                    above.add(new CommentLine(null, null, "", CommentType.BLANK_LINE));
                }
                above.addAll(block(lines));
                key.setBlockComments(above);
            }
            first = false;
            if (tuple.getValueNode() instanceof MappingNode inner) {
                comment(inner, path, comments);
            }
        }
    }

    private static List<CommentLine> block(final List<String> lines) {
        final List<CommentLine> out = new ArrayList<>(lines.size());
        for (final String line : lines) {
            out.add(new CommentLine(null, null, line.isEmpty() ? "" : " " + line, CommentType.BLOCK));
        }
        return out;
    }

    private static Yaml yaml() {
        final LoaderOptions loading = new LoaderOptions();
        loading.setMaxAliasesForCollections(MOST_ALIASES);
        loading.setNestingDepthLimit(DEEPEST_NESTING);
        loading.setCodePointLimit(LARGEST_FILE);
        loading.setAllowDuplicateKeys(false);
        loading.setAllowRecursiveKeys(false);
        final DumperOptions dumping = new DumperOptions();
        dumping.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        dumping.setIndent(INDENT);
        dumping.setIndicatorIndent(INDENT);
        dumping.setIndentWithIndicator(true);
        dumping.setProcessComments(true);
        dumping.setSplitLines(false);
        return new Yaml(new SafeConstructor(loading), new Representer(dumping), dumping, loading);
    }
}
