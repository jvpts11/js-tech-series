/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.testkit;

import dev.jstech.core.language.ILanguageProcess;
import dev.jstech.core.language.IMachineView;
import dev.jstech.core.language.IProgrammingLanguage;
import dev.jstech.tests.JsTests;
import java.util.List;
import java.util.Set;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

/**
 * A language of two files and no ceremony, for the tests of the language API: a toy program counts, or tells the time
 * or how much memory it has, and what it compiles to is its own text.
 *
 * <p>The game-test server registers it while the game loads, so a test finds it the way a player finds an addon's
 * language. What it does is deliberately unlike Σ#, so an assumption about Σ# leaking into the machines shows up as a
 * failure.
 */
public final class ToyLanguage implements IProgrammingLanguage {

    /** The id the game-test server registers it under. */
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(JsTests.MODID, "toy");

    /** A program that prints the machine's game tick, once. */
    public static final String CLOCK = "clock";

    /** A program that prints how many bytes it may hold, once. */
    public static final String QUOTA = "quota";

    /** How a program that counts starts: {@code count 3} prints 1, 2 and 3, a line a step. */
    private static final String COUNT = "count ";

    private final ResourceLocation id;
    private final Set<String> sources;
    private final Set<String> binaries;

    /** The one the game-test server registers: {@code .toy} to write and to run, {@code .toyb} to run. */
    public ToyLanguage() {
        this(ID, Set.of("toy"), Set.of("toy", "toyb"));
    }

    /** One under another id or with other extensions, for testing what a registry allows. */
    public ToyLanguage(final ResourceLocation id, final Set<String> sources, final Set<String> binaries) {
        this.id = id;
        this.sources = Set.copyOf(sources);
        this.binaries = Set.copyOf(binaries);
    }

    @Override
    public ResourceLocation id() {
        return this.id;
    }

    @Override
    public String displayName() {
        return "Toy";
    }

    @Override
    public Set<String> sourceExtensions() {
        return this.sources;
    }

    @Override
    public Set<String> binaryExtensions() {
        return this.binaries;
    }

    @Override
    public CompileResult compile(final List<SourceText> sources) {
        final String text = sources.isEmpty() ? "" : sources.getFirst().text().trim();
        if (!text.startsWith(COUNT) && !CLOCK.equals(text) && !QUOTA.equals(text)) {
            return CompileResult.failed(List.of(new Complaint(sources.isEmpty() ? "" : sources.getFirst().name(),
                    1, 1, "T001", "a toy program counts, or tells the time or its memory")));
        }
        return CompileResult.of(text);
    }

    @Override
    public List<Token> tokenize(final String text) {
        return List.of(new Token(1, 1, text.length(), Kind.NUMBER));
    }

    @Override
    public ILanguageProcess start(final String binary, final IMachineView machine, final List<String> arguments) {
        return new ToyProcess(binary.trim(), machine, 0);
    }

    @Override
    public ILanguageProcess restore(final String binary, final CompoundTag saved, final IMachineView machine) {
        return new ToyProcess(binary.trim(), machine, saved.getInt("Counted"));
    }

    /** How many lines a program prints: as many as it counts to, or one for the time or the memory. */
    private static int linesOf(final String program) {
        if (CLOCK.equals(program) || QUOTA.equals(program)) {
            return 1;
        }
        try {
            return Integer.parseInt(program.substring(COUNT.length()).trim());
        } catch (final NumberFormatException | IndexOutOfBoundsException notANumber) {
            return 0;
        }
    }

    /** A toy program: one line a step, written to the machine it was given, for as many steps as it has lines. */
    private static final class ToyProcess implements ILanguageProcess {

        private final String program;
        private final IMachineView machine;
        private final int wanted;
        private int counted;

        ToyProcess(final String program, final IMachineView machine, final int counted) {
            this.program = program;
            this.machine = machine;
            this.wanted = linesOf(program);
            this.counted = counted;
        }

        @Override
        public int step(final int budget) {
            int used = 0;
            while (used < budget && this.counted < this.wanted) {
                this.counted++;
                this.machine.print(this.line());
                used++;
            }
            return used;
        }

        /** The line this step prints. */
        private String line() {
            if (CLOCK.equals(this.program)) {
                return Long.toString(this.machine.tick());
            }
            if (QUOTA.equals(this.program)) {
                return Long.toString(this.machine.memoryQuota());
            }
            return Integer.toString(this.counted);
        }

        @Override
        public State state() {
            return this.counted < this.wanted ? State.RUNNING : State.FINISHED;
        }

        @Override
        public String message() {
            return "";
        }

        @Override
        public long spent() {
            return this.counted;
        }

        @Override
        public long heldBytes() {
            return 0L;
        }

        @Override
        public boolean isService() {
            return false;
        }

        @Override
        public void onTick() {
        }

        @Override
        public void onStop(final int budget) {
        }

        @Override
        public String name() {
            return "counter";
        }

        @Override
        public void save(final CompoundTag tag) {
            tag.putInt("Counted", this.counted);
        }
    }
}
