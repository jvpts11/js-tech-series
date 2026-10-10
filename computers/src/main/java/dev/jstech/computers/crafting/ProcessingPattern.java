/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.crafting;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.jstech.computers.storage.StorageKey;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.Text;
import dev.jstech.core.util.Utf8Text;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A processing recipe: what goes into a machine and what is expected out of it, with a player-set timeout, and
 * nothing about which machine. Inputs and outputs are {@link StorageKey}s, so they carry items, fluids or chemicals
 * without distinction (fluid crafts only make sense here, never in a 3x3 bench {@link CraftingPattern}). The recipe
 * runs wherever a Crafting Interface holds it: the machine there is a black box that is fed the inputs, and the job
 * waits for the declared outputs and fails if none appear within {@code timeoutTicks}. Each output declares a CHANCE
 * %: 100 = guaranteed, less = a probabilistic byproduct that feeds planning but never blocks the craft (the real
 * yield is counted at runtime). Whatever else comes out is an unexpected output, which never fails a job.
 */
public record ProcessingPattern(List<ProcessingInput> inputs, List<ProcessingOutput> outputs, int timeoutTicks,
                                String name, String note) {

    public static final int DEFAULT_TIMEOUT_TICKS = 200;
    public static final int FULL_CHANCE = 100;

    /**
     * The most one input or output moves per run. A plan multiplies it by a number of runs that is itself held below
     * this, so the product always fits a long; whatever asked for more, a pattern file or a player's screen, is held
     * to it.
     */
    public static final long MOST_AMOUNT = Integer.MAX_VALUE;

    /** A recipe without an author's name or note: it goes by its primary output. */
    public ProcessingPattern(final List<ProcessingInput> inputs, final List<ProcessingOutput> outputs,
                             final int timeoutTicks) {
        this(inputs, outputs, timeoutTicks, "", "");
    }

    /**
     * One input: a key (item, fluid or chemical) and how much of it the machine consumes per run. An
     * {@code estimated} amount was worked out from a recipe that meters its input per tick (per-tick usage
     * times the machine's base duration) rather than confirmed by the author; it runs like any other, the
     * flag only lets the GUIs say so until the author edits or confirms it.
     */
    public record ProcessingInput(StorageKey key, long amount, boolean estimated) {
        public ProcessingInput {
            amount = Math.max(1L, Math.min(MOST_AMOUNT, amount));
        }

        public ProcessingInput(final StorageKey key, final long amount) {
            this(key, amount, false);
        }

        public static final Codec<ProcessingInput> CODEC = RecordCodecBuilder.create(i -> i.group(
                StorageKey.CODEC.fieldOf("key").forGetter(ProcessingInput::key),
                Codec.LONG.fieldOf("amount").forGetter(ProcessingInput::amount),
                Codec.BOOL.optionalFieldOf("estimated", false).forGetter(ProcessingInput::estimated)
        ).apply(i, ProcessingInput::new));

        public static final StreamCodec<RegistryFriendlyByteBuf, ProcessingInput> STREAM_CODEC =
                StreamCodec.composite(
                        StorageKey.STREAM_CODEC, ProcessingInput::key,
                        ByteBufCodecs.VAR_LONG, ProcessingInput::amount,
                        ByteBufCodecs.BOOL, ProcessingInput::estimated,
                        ProcessingInput::new);
    }

    /** One output: a key, how much it yields per run, and the percent chance it appears (100 = guaranteed). */
    public record ProcessingOutput(StorageKey key, long amount, int chancePercent) {
        public ProcessingOutput {
            amount = Math.max(1L, Math.min(MOST_AMOUNT, amount));
            chancePercent = Math.max(1, Math.min(FULL_CHANCE, chancePercent));
        }

        public boolean probabilistic() {
            return chancePercent < FULL_CHANCE;
        }

        /** Expected yield per run for planning: {@code amount * chance / 100}. */
        public double expectedYield() {
            return amount * (chancePercent / 100.0);
        }

        public static final Codec<ProcessingOutput> CODEC = RecordCodecBuilder.create(i -> i.group(
                StorageKey.CODEC.fieldOf("key").forGetter(ProcessingOutput::key),
                Codec.LONG.fieldOf("amount").forGetter(ProcessingOutput::amount),
                Codec.INT.fieldOf("chance").forGetter(ProcessingOutput::chancePercent)
        ).apply(i, ProcessingOutput::new));

        public static final StreamCodec<RegistryFriendlyByteBuf, ProcessingOutput> STREAM_CODEC =
                StreamCodec.composite(
                        StorageKey.STREAM_CODEC, ProcessingOutput::key,
                        ByteBufCodecs.VAR_LONG, ProcessingOutput::amount,
                        ByteBufCodecs.VAR_INT, ProcessingOutput::chancePercent,
                        ProcessingOutput::new);
    }

    public ProcessingPattern {
        inputs = List.copyOf(inputs);
        outputs = List.copyOf(outputs);
        timeoutTicks = Math.max(1, timeoutTicks);
        name = Utf8Text.field(name, CraftingPattern.MAX_NAME);
        note = Utf8Text.field(note, CraftingPattern.MAX_NOTE);
    }

    public static final Codec<ProcessingPattern> CODEC = RecordCodecBuilder.create(i -> i.group(
            ProcessingInput.CODEC.listOf().fieldOf("inputs").forGetter(ProcessingPattern::inputs),
            ProcessingOutput.CODEC.listOf().fieldOf("outputs").forGetter(ProcessingPattern::outputs),
            Codec.INT.fieldOf("timeout").forGetter(ProcessingPattern::timeoutTicks),
            Codec.STRING.optionalFieldOf("name", "").forGetter(ProcessingPattern::name),
            Codec.STRING.optionalFieldOf("note", "").forGetter(ProcessingPattern::note)
    ).apply(i, ProcessingPattern::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, ProcessingPattern> STREAM_CODEC =
            StreamCodec.composite(
                    ProcessingInput.STREAM_CODEC.apply(ByteBufCodecs.list(64)), ProcessingPattern::inputs,
                    ProcessingOutput.STREAM_CODEC.apply(ByteBufCodecs.list(64)), ProcessingPattern::outputs,
                    ByteBufCodecs.VAR_INT, ProcessingPattern::timeoutTicks,
                    ByteBufCodecs.stringUtf8(CraftingPattern.MAX_NAME), ProcessingPattern::name,
                    ByteBufCodecs.stringUtf8(CraftingPattern.MAX_NOTE), ProcessingPattern::note,
                    ProcessingPattern::new);

    /** What the recipe is called where it is listed: its name, or its primary output's when it has none. */
    public String displayName() {
        return displayText().english();
    }

    /** The same, as text: an output's name reaches each player in their own language. */
    public Text displayText() {
        if (!name.isEmpty()) {
            return Text.literal(name);
        }
        final ProcessingOutput primary = primaryOutput();
        return primary == null ? RecipeTexts.UNNAMED_RECIPE.text() : GameText.of(primary.key().displayName());
    }

    /** The same recipe under a new name and note. */
    public ProcessingPattern withName(final String newName, final String newNote) {
        return new ProcessingPattern(inputs, outputs, timeoutTicks, newName, newNote);
    }

    /** Total of each input key per run (merging duplicate keys), for reservation/planning. */
    public Map<StorageKey, Long> ingredientTotals() {
        final Map<StorageKey, Long> totals = new LinkedHashMap<>();
        for (final ProcessingInput in : inputs) {
            totals.merge(in.key(), in.amount(), Long::sum);
        }
        return totals;
    }

    /** The primary (first) output, what a craft of this pattern aims to produce; null if none declared. */
    @Nullable
    public ProcessingOutput primaryOutput() {
        return outputs.isEmpty() ? null : outputs.get(0);
    }

    /** Two patterns describe the same recipe when their inputs and outputs all match, whatever they are called. */
    public boolean sameRecipe(final ProcessingPattern other) {
        return inputs.equals(other.inputs) && outputs.equals(other.outputs);
    }

    /**
     * The recipe written as one line of its inputs and outputs, the same for every pattern of the same recipe
     * ({@link #sameRecipe}): what an interface tells one recipe from another by while it runs one at a time.
     */
    public String identity() {
        final StringBuilder line = new StringBuilder();
        for (final ProcessingInput in : inputs) {
            line.append(in.key().id()).append('*').append(in.amount()).append(';');
        }
        line.append("->");
        for (final ProcessingOutput out : outputs) {
            line.append(out.key().id()).append('*').append(out.amount()).append('@').append(out.chancePercent())
                    .append(';');
        }
        return line.toString();
    }
}
