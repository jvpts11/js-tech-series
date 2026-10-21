/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.studio;

import dev.jstech.core.text.GameText;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * What a thing is made of and what is wrong across its parts, as the studio's readers write it down: a row for each
 * part, in the order they are written, and each problem found. The tests that name it are gathered from every reader
 * into one row, written last.
 */
public final class StudioSheet {

    private final List<Part> parts = new ArrayList<>();
    private final List<Check> checks = new ArrayList<>();
    private final List<String> tests = new ArrayList<>();

    /** Writes a row of what the thing is made of. */
    public void part(final Part part) {
        this.parts.add(Objects.requireNonNull(part, "part"));
    }

    /** Writes a problem found across its parts. */
    public void check(final Check check) {
        this.checks.add(Objects.requireNonNull(check, "check"));
    }

    /** Writes tests that name it, as a reader counts them: {@code 6 GameTests}. */
    public void tests(final String counted) {
        this.tests.add(Objects.requireNonNull(counted, "counted"));
    }

    /** Every row, and the tests' last: none, when no test names it. */
    public List<Part> parts() {
        final List<Part> out = new ArrayList<>(this.parts);
        out.add(new Part(new ItemStack(Items.SPYGLASS), GameText.resolve(StudioTexts.TESTS),
                this.tests.isEmpty() ? GameText.resolve(StudioTexts.NONE) : String.join(", ", this.tests),
                this.tests.isEmpty() ? Mark.NONE : Mark.FINE, Optional.empty()));
        return Collections.unmodifiableList(out);
    }

    /** Whether a test names it. */
    public boolean tested() {
        return !this.tests.isEmpty();
    }

    /** Every problem, the errors first. */
    public List<Check> checks() {
        final List<Check> out = new ArrayList<>(this.checks);
        out.sort((a, b) -> b.mark().compareTo(a.mark()));
        return Collections.unmodifiableList(out);
    }

    /** The worst of its problems, or {@link Mark#NONE} for none. */
    public Mark worst() {
        return this.checks.stream().map(Check::mark).max(Mark::compareTo).orElse(Mark.NONE);
    }

    /** How a part stands, or how bad a problem is, from nothing to say to an error. */
    public enum Mark {
        /** Nothing to say: a part it has none of, as a block with no screen. */
        NONE,
        /** There, and right. */
        FINE,
        /** Likely wrong. */
        WARNING,
        /** Wrong. */
        ERROR
    }

    /**
     * A file of a mod, where it stands among the mod's resources: {@code assets/mymod/blockstates/press.json}.
     *
     * @param modid the mod
     * @param path  where it stands
     */
    public record File(String modid, String path) {

        public File {
            Objects.requireNonNull(modid, "modid");
            Objects.requireNonNull(path, "path");
        }
    }

    /**
     * A row of what a thing is made of.
     *
     * @param icon   what stands for the part
     * @param name   the part, as the player reads it
     * @param detail what it is: its file, or what it holds
     * @param mark   how it stands
     * @param file   the file it is, which Open opens; none for a part no file holds
     */
    public record Part(ItemStack icon, String name, String detail, Mark mark, Optional<File> file) {

        public Part {
            Objects.requireNonNull(icon, "icon");
            Objects.requireNonNull(name, "name");
            Objects.requireNonNull(detail, "detail");
            Objects.requireNonNull(mark, "mark");
            Objects.requireNonNull(file, "file");
        }
    }

    /**
     * A problem across a thing's parts.
     *
     * @param mark   how bad: a warning or an error
     * @param title  what is wrong
     * @param detail where, or why
     * @param file   the file to open to mend it; none when no one file does
     */
    public record Check(Mark mark, String title, String detail, Optional<File> file) {

        public Check {
            Objects.requireNonNull(mark, "mark");
            Objects.requireNonNull(title, "title");
            Objects.requireNonNull(detail, "detail");
            Objects.requireNonNull(file, "file");
        }
    }
}
