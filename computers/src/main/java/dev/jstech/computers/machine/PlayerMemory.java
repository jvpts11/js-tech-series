/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.machine;

import dev.jstech.computers.os.IOsHost;
import dev.jstech.computers.os.OsDef;
import dev.jstech.computers.os.RamLedger;
import dev.jstech.computers.os.player.PlayerSystem;
import dev.jstech.computers.os.player.PlayerSystems;
import dev.jstech.computers.vm.program.Process;
import dev.jstech.computers.vm.system.SystemApi;
import dev.jstech.core.schedule.TickScheduler;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.jetbrains.annotations.Nullable;

/**
 * A machine running a player's system on a kernel of its own, run out of memory
 * (computers/docs/design/player-systems.md): the kernel refuses, ends what holds the most, or goes down, as it chose;
 * or the system answers {@code OnOutOfMemory},
 * given who asked and how much, and refuses, ends a process (and is asked again if that frees too little, each process
 * at most once, then it refuses), or goes down on its own page.
 *
 * <p>A system that is itself running when the memory runs short cannot be asked anything until it stops, so then its
 * kernel's rule decides; a fault in its answer brings the machine down, as any fault of the system does.
 */
@TextHolder
public final class PlayerMemory {

    /* What a system answers to say what happens to a machine run out of memory. */
    private static final String ON_OUT_OF_MEMORY = "OnOutOfMemory";
    /* The most a system's answer may take, in instructions, which come off its machine's next tick. */
    private static final int ANSWER_BUDGET = 4 * TickScheduler.QUANTUM;
    private static final String REFUSE = "refuse";
    private static final String DOWN = "down";

    private static final TextKey RAN_OUT = TextKey.of("jsc.machine.player_memory.ran_out", "%s ran out of memory.");
    private static final TextKey ASKED = TextKey.of("jsc.machine.player_memory.asked",
            "%s asked for %s MB that the machine did not have");

    private PlayerMemory() {
    }

    /** The memory rule of the player's own kernel the machine runs, or null when it runs none: refuse, end or down. */
    @Nullable
    public static String rule(final IOsHost machine) {
        final OsDef os = machine.installedOs();
        final PlayerSystem system = os == null ? null : PlayerSystems.withFiles(os.id());
        return system == null || !system.facts().kernel().own() ? null : system.facts().kernel().memoryRule();
    }

    /** What the kernel makes of {@code asking} wanting {@code needMb} more than the machine has. */
    public static Verdict whenShort(final IOsHost machine, final String asking, final int needMb, final String rule) {
        final MachinePrograms programs = machine.programs();
        final SigmaProgram system = programs == null ? null : programs.system();
        if (system != null && !system.busy() && system.answers(ON_OUT_OF_MEMORY, 2)) {
            return asked(machine, programs, system, asking, needMb);
        }
        return switch (rule) {
            case REFUSE -> Verdict.REFUSED;
            case DOWN -> {
                goDown(machine, asking, needMb);
                yield Verdict.DOWN;
            }
            default -> Verdict.END_HEAVIEST;
        };
    }

    /* The system's own answer, asked again after each process it ends while the room is still short. */
    private static Verdict asked(final IOsHost machine, final MachinePrograms programs, final SigmaProgram system,
                                 final String asking, final int needMb) {
        final Set<Integer> ended = new HashSet<>();
        while (true) {
            final Process.Answer answer = system.answer(ON_OUT_OF_MEMORY, List.of(asking, needMb), ANSWER_BUDGET);
            programs.owe(answer.used());
            if (!answer.given() || !(answer.value() instanceof Number said)) {
                return Verdict.REFUSED;
            }
            final int pid = said.intValue();
            if (pid == SystemApi.MEMORY_GO_DOWN) {
                goDown(machine, asking, needMb);
                return Verdict.DOWN;
            }
            if (pid == MachinePrograms.SYSTEM_PID || ended.contains(pid) || programs.byId(pid) == null) {
                return Verdict.REFUSED;
            }
            programs.stop(pid);
            ended.add(pid);
            if (fits(machine, needMb)) {
                return Verdict.ROOM;
            }
        }
    }

    private static boolean fits(final IOsHost machine, final int needMb) {
        final RamLedger ledger = machine.ramLedger();
        return (long) ledger.usedMb() + Math.max(0, needMb) <= ledger.totalMb();
    }

    private static void goDown(final IOsHost machine, final String asking, final int needMb) {
        final OsDef os = machine.installedOs();
        final PlayerSystem system = os == null ? null : PlayerSystems.withFiles(os.id());
        final String name = system == null ? asking : system.facts().name();
        PlayerSystemBoot.panic(machine, os, RAN_OUT.with(name).english(),
                List.of(ASKED.with(asking, needMb).english()));
    }

    /** What the kernel made of a machine run out of memory. */
    public enum Verdict {
        /** There is room now. */
        ROOM,
        /** What asked does not get it. */
        REFUSED,
        /** The machine went down on the system's page. */
        DOWN,
        /** The kernel ends what holds the most until it fits, as the mod's Unix kernels do. */
        END_HEAVIEST
    }
}
