/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.testkit;

import dev.jstech.core.language.ILanguageProcess;
import dev.jstech.core.language.IProgrammingLanguage;
import dev.jstech.tests.JsTests;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * A language of two files and no ceremony, for the tests of the language API: source counts, compiled text counts
 * louder.
 *
 * <p>The game-test server registers it while the game loads, so a test finds it the way a player finds an addon's
 * language. What it does is deliberately unlike Σ#, so an assumption about Σ# leaking into the machines shows up as a
 * failure.
 */
public final class ToyLanguage implements IProgrammingLanguage {

    /** The id the game-test server registers it under. */
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(JsTests.MODID, "toy");

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
        if (sources.isEmpty() || !sources.getFirst().text().startsWith("count ")) {
            return CompileResult.failed(List.of(new Complaint(
                    sources.isEmpty() ? "" : sources.getFirst().name(), 1, 1, "T001", "a toy program counts")));
        }
        return CompileResult.of(sources.getFirst().text());
    }

    @Override
    public List<Token> tokenize(final String text) {
        return List.of(new Token(1, 1, text.length(), Kind.NUMBER));
    }

    @Override
    public ILanguageProcess start(final String binary, final long heapBytes, final BlockEntity machine) {
        return new ToyProcess(howMany(binary), 0);
    }

    @Override
    public ILanguageProcess restore(final String binary, final CompoundTag saved, final BlockEntity machine) {
        return new ToyProcess(howMany(binary), saved.getInt("Counted"));
    }

    private static int howMany(final String binary) {
        try {
            return Integer.parseInt(binary.substring("count ".length()).trim());
        } catch (final NumberFormatException | IndexOutOfBoundsException notANumber) {
            return 0;
        }
    }

    /** A toy program: one line of output a step, for as many steps as it says. */
    private static final class ToyProcess implements ILanguageProcess {

        private final int wanted;
        private final List<String> console = new ArrayList<>();
        private int counted;

        ToyProcess(final int wanted, final int counted) {
            this.wanted = wanted;
            this.counted = counted;
        }

        @Override
        public int step(final int budget) {
            int used = 0;
            while (used < budget && this.counted < this.wanted) {
                this.counted++;
                this.console.add(Integer.toString(this.counted));
                used++;
            }
            return used;
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
        public List<String> console() {
            return List.copyOf(this.console);
        }

        @Override
        public long written() {
            return this.console.size();
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
        public long heapBytes() {
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
