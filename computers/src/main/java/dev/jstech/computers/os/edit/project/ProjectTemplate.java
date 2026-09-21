/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.os.edit.project;

import dev.jstech.computers.hardware.Architectures;
import dev.jstech.computers.sigma.LanguageLevel;
import java.util.ArrayList;
import java.util.List;

/**
 * What a new project starts as: the shapes a program can have, each with the code that shape begins
 * with, so a player picks one and has something that already builds.
 *
 * <p>Every shape comes in both of the languages the mod ships, Σ# and the smaller Σ, because a studio on a modern
 * machine is where anybody would rather write either of them: the language being an old one is no reason to be
 * handed an empty file and left to it. A language an addon brings shows up in the studio's list with no shapes,
 * until it registers its own.
 */
public enum ProjectTemplate {

    CONSOLE_APP("Console App", ProjectFile.Kind.CONSOLE,
            "A program with a Main that runs at the terminal it was started from, prints as it goes, "
                    + "and is gone when it returns.",
            List.of("Console")),
    SCRIPT("Script", ProjectFile.Kind.SCRIPT,
            "A program that stays up: set up once, called every tick, told when it is stopped. "
                    + "For watching the network and reacting to it.",
            List.of("Script")),
    CLASS_LIBRARY("Class Library", ProjectFile.Kind.LIBRARY,
            "Classes shared by other projects. Has no entry point of its own and compiles into theirs.",
            List.of("Library")),
    EMPTY_PROJECT("Empty Project", ProjectFile.Kind.EMPTY,
            "A project with no files in it. For starting from nothing.",
            List.of());

    private final String title;
    private final ProjectFile.Kind kind;
    private final String description;
    private final List<String> tags;

    /** The platforms every template here runs on, for the filter. */
    public static final List<String> PLATFORMS = List.of("Frames", "Linux");

    ProjectTemplate(final String title, final ProjectFile.Kind kind, final String description,
                    final List<String> tags) {
        this.title = title;
        this.kind = kind;
        this.description = description;
        this.tags = tags;
    }

    /**
     * One line of the studio's list: a shape, in one of the languages it comes in.
     *
     * @param template the shape of program
     * @param language the language it starts written in
     */
    public record Offer(ProjectTemplate template, LanguageLevel language) {

        /** Every shape in every language, the full language first since it is what most projects are. */
        public static List<Offer> all() {
            final List<Offer> out = new ArrayList<>();
            for (final LanguageLevel language : List.of(LanguageLevel.SIGMA_SHARP, LanguageLevel.SIGMA)) {
                for (final ProjectTemplate template : values()) {
                    out.add(new Offer(template, language));
                }
            }
            return out;
        }

        public String title() {
            return this.template.title();
        }

        /** What the list says under the title, which for the smaller language says where it runs. */
        public String description() {
            return this.language.full() ? this.template.description()
                    : this.template.description() + " Runs on every machine, the earliest included.";
        }

        /** The words the filters match: the language, the platforms, and the kind. */
        public List<String> tags() {
            return this.template.tags(this.language);
        }

        public ProjectFile project(final String projectName) {
            return this.template.project(projectName, this.language);
        }

        public String firstSource(final String projectName) {
            return this.template.firstSource(projectName, this.language);
        }

        public String source(final String projectName) {
            return this.template.source(projectName, this.language);
        }
    }

    public String title() {
        return this.title;
    }

    public ProjectFile.Kind kind() {
        return this.kind;
    }

    public String description() {
        return this.description;
    }

    /** The words the filters match: the language, the platforms, and the kind. */
    public List<String> tags(final LanguageLevel language) {
        final List<String> out = new ArrayList<>();
        out.add(language.mark());
        out.addAll(PLATFORMS);
        out.addAll(this.tags);
        return out;
    }

    /** Whether the template's kind is the one asked for, or anything when nothing was. */
    public boolean isKind(final String kindFilter) {
        return kindFilter == null || kindFilter.isEmpty() || this.tags.contains(kindFilter);
    }

    /** The name of the first source a project of this template starts with, or empty for none. */
    public String firstSource(final String projectName, final LanguageLevel language) {
        return this == EMPTY_PROJECT ? "" : projectName + "." + language.sourceExtension();
    }

    /**
     * The project file a new project of this template starts as, built for the oldest machine its language
     * runs on, so that a Σ project is one a Vintage computer can run until somebody decides otherwise.
     */
    public ProjectFile project(final String projectName, final LanguageLevel language) {
        final String first = firstSource(projectName, language);
        return new ProjectFile(projectName, this.kind, language.id(),
                first.isEmpty() ? List.of() : List.of(first), List.of(),
                this.kind == ProjectFile.Kind.LIBRARY || this.kind == ProjectFile.Kind.EMPTY
                        ? "" : ProjectFile.defaultEntry(projectName),
                Architectures.oldestFor(language).id());
    }

    /**
     * The code the first source starts with: a program of the shape that already builds, named after
     * the project so the type is not a name the language has taken.
     */
    public String source(final String projectName, final LanguageLevel language) {
        return language.full() ? sharpSource(projectName) : sigmaSource(projectName);
    }

    private String sharpSource(final String projectName) {
        return switch (this) {
            case CONSOLE_APP -> """
                    using System.IO.*;

                    namespace %s;

                    class %s {
                        static void Main() {
                            Console.PrintLine("Hello from %s");
                        }
                    }
                    """.formatted(projectName, projectName, projectName);
            case SCRIPT -> """
                    using System.*;
                    using System.IO.*;

                    namespace %s;

                    class %s : IScript {
                        public void OnInit() {
                            Console.PrintLine("%s is up");
                        }

                        public void OnTick() {
                        }

                        public void OnDestroy() {
                        }
                    }
                    """.formatted(projectName, projectName, projectName);
            case CLASS_LIBRARY -> """
                    namespace %s;

                    public class %s {
                        public static int Answer() {
                            return 42;
                        }
                    }
                    """.formatted(projectName, projectName);
            case EMPTY_PROJECT -> "";
        };
    }

    /*
     * The same shapes in the smaller language. Its whole library is one namespace, and a program that stays up
     * stands on a class there rather than answering an interface, since the language has none; it fills in only
     * the calls it uses and says override on each. It prints the way the languages of those machines printed,
     * with printf and a line break written into the format.
     */
    private String sigmaSource(final String projectName) {
        return switch (this) {
            case CONSOLE_APP -> """
                    using Standard.*;

                    namespace %s;

                    class %s {
                        static void Main() {
                            printf("Hello from %s\\n");
                        }
                    }
                    """.formatted(projectName, projectName, projectName);
            case SCRIPT -> """
                    using Standard.*;

                    namespace %s;

                    class %s : Script {
                        public override void OnInit() {
                            printf("%s is up\\n");
                        }

                        public override void OnTick() {
                        }
                    }
                    """.formatted(projectName, projectName, projectName);
            case CLASS_LIBRARY -> """
                    namespace %s;

                    public class %s {
                        public static int Answer() {
                            return 42;
                        }
                    }
                    """.formatted(projectName, projectName);
            case EMPTY_PROJECT -> "";
        };
    }
}
