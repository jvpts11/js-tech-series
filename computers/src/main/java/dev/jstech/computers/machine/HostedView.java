/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.machine;

import dev.jstech.computers.vm.program.ProgramConsole;
import dev.jstech.core.language.IMachineView;
import java.util.List;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;

/**
 * What a program of another language sees of the machine it runs on: the machine's clock, the console the machine keeps
 * for it, and how much memory it may hold.
 *
 * <p>The clock asks the machine for its world every time, as {@link MachineServices} does, because a machine read out
 * of a save comes back before it is placed in a world. The console keeps a program's lines to the same limits as any
 * other program's.
 */
final class HostedView implements IMachineView {

    @Nullable
    private final BlockEntity machine;
    private final long quota;
    private final ProgramConsole console = new ProgramConsole();

    /**
     * @param machine the machine the program runs on, or null where there is none to ask the time of
     * @param quota   how many bytes the program may hold at once
     */
    HostedView(@Nullable final BlockEntity machine, final long quota) {
        this.machine = machine;
        this.quota = quota;
    }

    @Override
    public long tick() {
        final Level level = this.level();
        return level == null ? 0 : level.getGameTime();
    }

    @Override
    public long dayTime() {
        final Level level = this.level();
        return level == null ? 0 : level.getDayTime() % MachineServices.DAY;
    }

    @Override
    public long day() {
        final Level level = this.level();
        return level == null ? 0 : level.getDayTime() / MachineServices.DAY;
    }

    @Override
    public void print(final String line) {
        this.console.write(String.valueOf(line));
    }

    @Override
    public long memoryQuota() {
        return this.quota;
    }

    /** What the program has written, oldest kept line first. */
    List<String> lines() {
        return this.console.lines();
    }

    /** How many lines it has written, the ones no longer kept included. */
    long written() {
        return this.console.written();
    }

    @Nullable
    private Level level() {
        return this.machine == null ? null : this.machine.getLevel();
    }
}
