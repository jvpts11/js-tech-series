/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.guide;

import dev.jstech.core.text.Text;
import java.util.List;
import java.util.Objects;

/**
 * What a manual prints about a command a mod's computers run, as the mod answers it from the command itself
 * ({@link GuideFacts#command}). A mod makes the command's page at its own prompts from the same command, so what the
 * manual says and what the machine says never disagree.
 *
 * <p>Words typed at the prompt (its name, its options, its examples) are the same in every language; what it says
 * about itself, and the words in angle brackets of what it takes, are {@link Text}, put in the reader's language as
 * the page is laid out.
 *
 * @param name        what is typed to run it, such as {@code dir}
 * @param summary     what it is for, in a line: "list the files in a folder"
 * @param usage       what it takes after its name, such as {@code [path] [/w]}, or nothing
 * @param description what it does, a paragraph each
 * @param options     its options, each with what it does
 * @param examples    lines typed with it, each with what it does
 * @param systems     the systems it runs on, each named as a reader knows it, such as "MC-DOS 6.22"
 * @param seeAlso     the names of the commands to read next
 */
public record GuideCommand(String name, Text summary, Text usage, List<Text> description, List<Option> options,
                           List<Example> examples, List<Text> systems, List<String> seeAlso) {

    public GuideCommand {
        Objects.requireNonNull(name, "name");
        summary = summary == null ? Text.EMPTY : summary;
        usage = usage == null ? Text.EMPTY : usage;
        description = List.copyOf(description);
        options = List.copyOf(options);
        examples = List.copyOf(examples);
        systems = List.copyOf(systems);
        seeAlso = List.copyOf(seeAlso);
    }

    /**
     * An option of a command, as it is typed, and what it does.
     *
     * @param flag how it is typed, such as {@code /w} or {@code -l}
     * @param what what it does
     */
    public record Option(String flag, Text what) {

        public Option {
            Objects.requireNonNull(flag, "flag");
            what = what == null ? Text.EMPTY : what;
        }
    }

    /**
     * A line typed with a command, and what it does.
     *
     * @param line the line, as typed at the prompt
     * @param what what it does, or nothing
     */
    public record Example(String line, Text what) {

        public Example {
            Objects.requireNonNull(line, "line");
            what = what == null ? Text.EMPTY : what;
        }
    }
}
