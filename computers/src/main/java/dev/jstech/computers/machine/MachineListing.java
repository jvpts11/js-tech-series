/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.machine;

import com.mojang.logging.LogUtils;
import dev.jstech.computers.vm.listing.AsmProgram;
import dev.jstech.computers.vm.listing.AsmReader;
import dev.jstech.computers.vm.listing.ListingProblem;
import dev.jstech.computers.vm.listing.Shape;
import dev.jstech.computers.vm.program.Process;
import dev.jstech.computers.vm.program.ProgramImage;
import dev.jstech.computers.vm.program.SnapshotException;
import dev.jstech.computers.vm.program.Values;
import java.util.List;
import java.util.Locale;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

/**
 * The listings a machine runs itself.
 *
 * <p>A {@code .asm} file is what Σ# compiles to, but it belongs to the machine rather than to the language: the machine
 * reads it, runs it and saves it, and no language in the registry may claim the extension. That leaves a language free
 * to do nothing but compile, and keeps every compiled program running in a pack that took Σ# out of the registry.
 */
public final class MachineListing {

    /** The extension of a listing, without the dot. */
    public static final String EXTENSION = "asm";

    /** What a listing is called in front of a person. */
    public static final String LABEL = "Σ# program";

    private static final Logger LOGGER = LogUtils.getLogger();

    private MachineListing() {
    }

    /** Whether files with that extension, written without the dot, are the machine's own listings. */
    public static boolean claims(final String extension) {
        return extension != null && EXTENSION.equals(extension.toLowerCase(Locale.ROOT));
    }

    /**
     * Starts the program a listing holds.
     *
     * @param heapBytes how much memory the program may hold at once
     * @param machine   the machine it runs on, for its clock and for everything the program reaches through it
     * @return the running program, or null when the text is not a listing with somewhere to start
     */
    @Nullable
    public static IMachineRuntime start(final String listing, final long heapBytes, final BlockEntity machine,
                                        final List<String> arguments) {
        final ProgramImage program = read(listing);
        if (program == null || program.entryPoint() == null) {
            return null;
        }
        final Process process = new Process(program, heapBytes, new MachineHost(machine));
        process.setArgs(arguments);
        if (program.shape() == Shape.CONSOLE) {
            process.beginStatic(program.entryPoint(), "Main");
        } else {
            final Values.Obj script = process.create(program.entryPoint());
            if (script == null) {
                return null;
            }
            process.begin(script, "OnInit");
        }
        return new SigmaProgram(process);
    }

    /**
     * Brings back a program saved from a listing.
     *
     * @return the program carrying on, or null when it cannot come back; the machine and its other programs load
     *         either way
     */
    @Nullable
    public static IMachineRuntime restore(final String listing, final CompoundTag saved, final BlockEntity machine) {
        final ProgramImage program = read(listing);
        if (program == null) {
            return null;
        }
        try {
            return new SigmaProgram(Process.restore(program,
                    SnapshotTag.read(SigmaProgram.snapshotOf(saved)), new MachineHost(machine)));
        } catch (final SnapshotException damaged) {
            // One program that cannot come back is left out; the machine and the rest of its programs load.
            LOGGER.warn("A saved program on the machine at {} was left out: {}", machine.getBlockPos(),
                    damaged.getMessage());
            return null;
        } catch (final RuntimeException fault) {
            LOGGER.error("A saved program on the machine at {} could not be brought back and was left out",
                    machine.getBlockPos(), fault);
            return null;
        }
    }

    /**
     * The first thing wrong with a listing, or null when nothing is: what a person is told when the listing does not
     * start. Only asked once a listing has been refused, so reading it again costs a start that failed anyway.
     */
    @Nullable
    public static ListingProblem firstProblem(final String listing) {
        final List<ListingProblem> problems = load(listing).problems();
        return problems.isEmpty() ? null : problems.getFirst();
    }

    /** Reads a listing, or null when it is not one or something in it has nothing to answer it. */
    @Nullable
    private static ProgramImage read(final String listing) {
        final Loaded loaded = load(listing);
        return loaded.problems().isEmpty() ? loaded.image() : null;
    }

    private static Loaded load(final String listing) {
        final AsmReader reader = new AsmReader(listing);
        final AsmProgram program = reader.read();
        if (reader.hasProblems()) {
            return new Loaded(null, reader.problems());
        }
        final ProgramImage image = ProgramImage.of(program);
        return new Loaded(image, image.problems());
    }

    /** A listing made ready, with everything wrong with it; there is no image when the text could not be read. */
    private record Loaded(@Nullable ProgramImage image, List<ListingProblem> problems) {
    }
}
