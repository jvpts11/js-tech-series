/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.machine;

import com.mojang.logging.LogUtils;
import dev.jstech.computers.blockentity.AbstractComputerBlockEntity;
import dev.jstech.computers.vm.program.IHost;
import dev.jstech.computers.vm.program.IWorldFunction;
import dev.jstech.computers.vm.system.MemberId;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.slf4j.Logger;

/**
 * The clock a program running on a real machine reads.
 *
 * <p>It asks the machine for its world every time rather than holding on to one, because a block entity
 * is read out of a save before it is placed in a world and its programs come back with it.
 */
public record MachineHost(BlockEntity machine) implements IHost {

    /** The length of a Minecraft day in ticks. */
    static final long DAY = 24_000L;

    private static final Logger LOGGER = LogUtils.getLogger();

    @Override
    public long tick() {
        final Level level = this.machine.getLevel();
        return level == null ? 0 : level.getGameTime();
    }

    @Override
    public long dayTime() {
        final Level level = this.machine.getLevel();
        return level == null ? 0 : level.getDayTime() % DAY;
    }

    @Override
    public long day() {
        final Level level = this.machine.getLevel();
        return level == null ? 0 : level.getDayTime() / DAY;
    }

    @Override
    public void fault(final String process, final int line, final RuntimeException cause) {
        LOGGER.error("Σ# program '{}' on the machine at {} failed inside the runtime at instruction {}",
                process, this.machine.getBlockPos(), line, cause);
    }

    @Override
    public void programEnded(final int program) {
        if (this.machine instanceof dev.jstech.computers.blockentity.AbstractComputerBlockEntity self) {
            self.programs().ended(program);
        }
    }

    /**
     * The calls the machine answers with one of its services, bound to this machine's; a block entity that runs no
     * programs of its own answers none of them this way.
     */
    @Override
    public IWorldFunction bind(final MemberId id) {
        final MachineCalls.Binding<?> binding = MachineCalls.find(id);
        return binding != null && this.machine instanceof AbstractComputerBlockEntity self
                ? binding.on(self.services()) : null;
    }

    /** Whether the machine boots to a desktop; one in no world, or no person could sit at, has none to open on. */
    @Override
    public boolean hasDesktop() {
        if (!(this.machine instanceof AbstractComputerBlockEntity self)) {
            return false;
        }
        final ComputerInfoService computer = self.services().computer();
        return computer != null && computer.hasDesktop();
    }

    /** Whether a program this machine, or a computer of its network, lists under that number is still going. */
    @Override
    public boolean programRunning(final int program, final String host) {
        if (!(this.machine instanceof AbstractComputerBlockEntity self)) {
            return false;
        }
        final ProgramService programs = self.services().programs();
        return programs != null && programs.running(program, host);
    }
}
