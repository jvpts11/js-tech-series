/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.machine;

import com.mojang.logging.LogUtils;
import dev.jstech.computers.hardware.ComputerBuild;
import dev.jstech.computers.hardware.IsaFit;
import dev.jstech.computers.hardware.IsaSpec;
import dev.jstech.computers.hardware.IsaTranslator;
import dev.jstech.computers.hardware.Isas;
import dev.jstech.computers.os.IOsHost;
import dev.jstech.computers.os.OsDef;
import dev.jstech.computers.vm.listing.AsmProgram;
import dev.jstech.computers.vm.listing.AsmReader;
import dev.jstech.computers.vm.listing.ListingError;
import dev.jstech.computers.vm.listing.ListingProblem;
import dev.jstech.computers.vm.listing.Shape;
import dev.jstech.computers.vm.program.IHost;
import dev.jstech.computers.vm.program.Process;
import dev.jstech.computers.vm.program.ProgramImage;
import dev.jstech.computers.vm.program.SnapshotException;
import dev.jstech.computers.vm.program.Values;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import java.util.ArrayList;
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
@TextHolder
public final class MachineListing {

    /** The extension of a listing, without the dot. */
    public static final String EXTENSION = "asm";

    /** What a listing is called in front of a person, in their language. */
    public static final TextKey LABEL = TextKey.of("jsc.machine_listing.label", "Σ# program");

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
        final Loaded loaded = load(listing, machine);
        final ProgramImage program = loaded.problems().isEmpty() ? loaded.image() : null;
        if (program == null || program.entryPoint() == null) {
            return null;
        }
        // A player's operating system is booted by the machine, never started at a prompt.
        if (program.shape() == Shape.SYSTEM) {
            return null;
        }
        final Process process = new Process(program, heapBytes, hostOf(machine));
        process.setArgs(arguments);
        if (program.shape() == Shape.CONSOLE) {
            process.beginMain(program.entryPoint());
        } else {
            final Values.Obj script = process.create(program.entryPoint());
            if (script == null) {
                return null;
            }
            if (process.declares(script, "OnInit")) {
                process.begin(script, "OnInit");
            }
        }
        return new SigmaProgram(process, loaded.speedPercent());
    }

    /**
     * Boots the operating system a listing holds (computers/docs/design/player-systems.md): its object is made and told
     * the machine is coming up, through its {@code OnBoot}.
     *
     * @return the system running, or null when the listing holds no system this machine runs
     */
    @Nullable
    public static SigmaProgram boot(final String listing, final long heapBytes, final BlockEntity machine) {
        final Loaded loaded = load(listing, machine);
        final ProgramImage program = loaded.problems().isEmpty() ? loaded.image() : null;
        if (program == null || program.entryPoint() == null || program.shape() != Shape.SYSTEM) {
            return null;
        }
        final Process process = new Process(program, heapBytes, hostOf(machine));
        final Values.Obj system = process.create(program.entryPoint());
        if (system == null) {
            return null;
        }
        if (process.declares(system, "OnBoot")) {
            process.begin(system, "OnBoot");
        }
        return new SigmaProgram(process, loaded.speedPercent());
    }

    /** Whether the listing holds an operating system, which a machine boots rather than starts at a prompt. */
    public static boolean holdsSystem(final String listing) {
        final Loaded loaded = load(listing, null);
        return loaded.image() != null && loaded.image().shape() == Shape.SYSTEM;
    }

    /**
     * Brings back a program saved from a listing.
     *
     * @return the program carrying on, or null when it cannot come back; the machine and its other programs load
     *         either way
     */
    @Nullable
    public static IMachineRuntime restore(final String listing, final CompoundTag saved, final BlockEntity machine) {
        final Loaded loaded = load(listing, machine);
        final ProgramImage program = loaded.problems().isEmpty() ? loaded.image() : null;
        if (program == null) {
            return null;
        }
        try {
            return new SigmaProgram(Process.restore(program,
                    SnapshotTag.read(SigmaProgram.snapshotOf(saved)), hostOf(machine)), loaded.speedPercent());
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
     * The first thing wrong with a listing on that machine, or null when nothing is: what a person is told when the
     * listing does not start. Only asked once a listing has been refused, so reading it again costs a start that
     * failed anyway.
     */
    @Nullable
    public static ListingProblem firstProblem(final String listing, @Nullable final BlockEntity machine) {
        final List<ListingProblem> problems = load(listing, machine).problems();
        return problems.isEmpty() ? null : problems.getFirst();
    }

    /** The host a program on that machine runs on: the machine's own services, or a clock that never moves off one. */
    private static IHost hostOf(@Nullable final BlockEntity machine) {
        final MachineServices services = machine instanceof IOsHost host ? host.services() : null;
        return services != null ? services : IHost.still();
    }

    /**
     * Reads a listing and makes ready the build of it the machine runs: nothing when it is not a listing, when
     * something in it has nothing to answer it, or when the machine it was brought to runs none of its builds.
     */
    private static Loaded load(final String listing, @Nullable final BlockEntity machine) {
        final AsmReader reader = new AsmReader(listing);
        final List<AsmProgram> builds = reader.readAll();
        if (reader.hasProblems()) {
            return new Loaded(null, reader.problems(), IsaFit.FULL_SPEED);
        }
        final IOsHost computer = machine instanceof IOsHost host ? host : null;
        final ComputerBuild build = computer == null ? null : computer.currentBuild();
        final IsaSpec here = build == null ? null : build.isa();
        /*
         * Only a machine with a processor chooses, so a host that is not a computer runs the first build. A program is
         * instructions for a processor, and one that does not understand any of them cannot be made to, unless the
         * machine's system carries a translator for one; the listing is not at fault, and runs untouched elsewhere.
         */
        final IsaFit fit = here == null ? new IsaFit(0, IsaFit.FULL_SPEED, null)
                : IsaFit.of(here, isasOf(builds), translatorsOf(computer)).orElse(null);
        if (fit == null) {
            return new Loaded(null, List.of(new ListingProblem(builds.getFirst().isaLine(), 1,
                    ListingError.ISA_MISMATCH.code(), ListingError.ISA_MISMATCH.message(namesOf(builds), here.name()))),
                    IsaFit.FULL_SPEED);
        }
        final ProgramImage image = ProgramImage.of(builds.get(fit.build()));
        return new Loaded(image, image.problems(), fit.speedPercent());
    }

    /** The instruction sets the builds were built for, in the order the listing carries them. */
    private static List<String> isasOf(final List<AsmProgram> builds) {
        final List<String> isas = new ArrayList<>(builds.size());
        for (final AsmProgram one : builds) {
            isas.add(one.isa());
        }
        return isas;
    }

    /** What the machine's system carries to run the programs of another processor; nothing with no system. */
    private static List<IsaTranslator> translatorsOf(final IOsHost computer) {
        final OsDef os = computer.installedOs();
        return os == null ? List.of() : os.translators();
    }

    /**
     * The instruction sets a program was built for as a person reads them, each by the name it is written under, or by
     * its id when no mod has brought one under that name.
     */
    private static String namesOf(final List<AsmProgram> builds) {
        final List<String> names = new ArrayList<>(builds.size());
        for (final AsmProgram one : builds) {
            names.add(Isas.byId(one.isa()).map(IsaSpec::name).orElse(one.isa()));
        }
        return String.join(", ", names);
    }

    /**
     * A listing made ready, with everything wrong with it, and how fast the build the machine runs goes: there is no
     * image when the text could not be read.
     */
    private record Loaded(@Nullable ProgramImage image, List<ListingProblem> problems, int speedPercent) {
    }
}
