/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.chain.machine;

import dev.jstech.core.blockentity.DerivedInt;
import dev.jstech.core.blockentity.ValueField;
import dev.jstech.core.machine.MachineStatus;
import dev.jstech.core.machine.ProcessingInput;
import dev.jstech.core.text.Text;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The Prebuilder at work (computers/docs/design/survival.md): it takes a computer's empty case and its parts through
 * its inlet and puts them together by the rules of the computer's own assembly window, about ten seconds a computer;
 * what leaves its outlet is the computer with its parts inside, its board's firmware and its disk's system as they
 * came, never started. Parts that do not go together it leaves where they are, saying why. The computer it is
 * building, and how far, is kept for the players' games, which draw it going together.
 */
public final class PrebuilderBlockEntity extends ChainMachineBlockEntity {

    private final ValueField<ItemStack> building;
    private final DerivedInt shown;
    /* The inputs last worked out and what they made, so a kit is put together once and not every tick. */
    private List<ItemStack> lastInputs = List.of();
    private Optional<Processing> lastWork = Optional.empty();
    private Text lastRefusal = Text.EMPTY;

    /** Its inputs: one case and as many parts as the kit of a computer it builds holds. */
    public static final int INPUTS = 17;
    /* How far through its work it is, in thousandths, as the players' games draw it. */
    private static final int WHOLE = 1_000;
    private static final int TICKS_A_SECOND = 20;

    public PrebuilderBlockEntity(final ChainMachine machine, final BlockPos pos, final BlockState state) {
        super(machine, pos, state);
        this.building = fields().value("Building", ItemStack.OPTIONAL_CODEC, ItemStack.EMPTY).toClient();
        this.shown = fields().derived("Shown", () -> getMaxProgress() <= 0 ? 0
                : (int) ((long) getProgress() * WHOLE / getMaxProgress())).toClient();
    }

    /** The computer it is putting together, as it will come out; empty while it builds nothing. */
    public ItemStack building() {
        return building.get();
    }

    /** How far through the computer it is, from 0 to 1, as its players see it. */
    public float done() {
        return shown.getAsInt() / (float) WHOLE;
    }

    /* The case and every other part in its inputs, put together; the parts it cannot put together, why. */
    @Override
    protected Optional<Processing> process(final Level level, final ProcessingInput input) {
        if (!sameStacks(lastInputs, input.items())) {
            lastInputs = input.items().stream().map(ItemStack::copy).toList();
            workOut(input.items());
        }
        note(lastRefusal);
        lastWork.ifPresent(work -> {
            if (!ItemStack.matches(building.get(), work.outputs().getFirst())) {
                building.set(work.outputs().getFirst());
            }
        });
        return lastWork;
    }

    /* Idle, it shows nothing being built; with nothing given it says nothing more. */
    @Override
    protected void settled(final ServerLevel level, final MachineStatus status) {
        super.settled(level, status);
        if (status != MachineStatus.WORKING && !building.get().isEmpty()) {
            building.set(ItemStack.EMPTY);
        }
        if (inputsEmpty()) {
            note(Text.EMPTY);
        }
    }

    private void workOut(final List<ItemStack> items) {
        int caseAt = -1;
        for (int i = 0; i < items.size() && caseAt < 0; i++) {
            if (ComputerKit.isCase(items.get(i))) {
                caseAt = i;
            }
        }
        final List<Take> takes = new ArrayList<>();
        final List<ItemStack> parts = new ArrayList<>();
        for (int i = 0; i < items.size(); i++) {
            if (i != caseAt && !items.get(i).isEmpty()) {
                takes.add(new Take(i, items.get(i).getCount()));
                parts.add(items.get(i));
            }
        }
        lastWork = Optional.empty();
        lastRefusal = caseAt < 0 && !parts.isEmpty() ? ComputerKit.NOT_A_CASE.text() : Text.EMPTY;
        if (caseAt < 0 || parts.isEmpty()) {
            return;
        }
        final ComputerKit.Built built = ComputerKit.assemble(items.get(caseAt), parts);
        lastRefusal = built.refusal();
        if (built.built()) {
            takes.add(new Take(caseAt, 1));
            lastWork = Optional.of(new Processing(takes, List.of(), List.of(built.computer()), List.of(),
                    machine().seconds() * TICKS_A_SECOND, 0));
        }
    }

    private boolean inputsEmpty() {
        for (int slot = 0; slot < INPUTS; slot++) {
            if (!getInventory().getStackInSlot(slot).isEmpty()) {
                return false;
            }
        }
        return true;
    }
}
