/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.chain.machine;

import dev.jstech.core.jstechseries.Element;
import dev.jstech.core.jstechseries.Elements;
import dev.jstech.core.machine.ProcessingInput;
import dev.jstech.core.matter.MatterStack;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The Air Separation Unit at work (computers/docs/design/survival.md, "The chain's numbers"): its compressor draws the
 * air round it, which no inlet gives, and each step separates it into 780 mB of nitrogen, 210 of oxygen, 9 of argon
 * and 1 of krypton, out of its gas outlets. Krypton is raised well above its real share, or the Legacy's laser would
 * take hours to fill. It works while it has room for all four.
 */
public class AirSeparationBlockEntity extends ChainMachineBlockEntity {

    private static final int TICKS = 20;

    public AirSeparationBlockEntity(final ChainMachine machine, final BlockPos pos, final BlockState state) {
        super(machine, pos, state);
    }

    /* A step of the air round it, always. */
    @Override
    protected Optional<Processing> process(final Level level, final ProcessingInput input) {
        final List<MatterStack> gases = List.of(new MatterStack(Elements.of(Element.NITROGEN), 780),
                new MatterStack(Elements.of(Element.OXYGEN), 210), new MatterStack(Elements.of(Element.ARGON), 9),
                new MatterStack(Elements.of(Element.KRYPTON), 1));
        return run(level, input, new Processing(List.of(), List.of(), List.of(), List.of(), machine().seconds() * TICKS,
                0, List.of(), gases, List.of(), List.of()), null);
    }
}
