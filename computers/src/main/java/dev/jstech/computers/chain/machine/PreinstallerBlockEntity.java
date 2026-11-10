/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.chain.machine;

import dev.jstech.core.blockentity.DerivedInt;
import dev.jstech.core.blockentity.IntField;
import dev.jstech.core.blockentity.ValueField;
import dev.jstech.core.id.StableCodecs;
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
import org.jetbrains.annotations.Nullable;

/**
 * The Preinstaller at work (computers/docs/design/survival.md): one job at a time, taken in through its inlet, written
 * as {@link Preinstalls} says, and sent out of its outlet, while the install media among its inputs are read and stay
 * where they are. It takes about ten seconds a job, and a second more for every hundred megabytes it writes. What it
 * works on, at which station, how far and how much of it is the firmware, is kept for the players' games, which draw
 * its console's two bars.
 */
public final class PreinstallerBlockEntity extends ChainMachineBlockEntity {

    private final ValueField<ItemStack> working;
    private final ValueField<Preinstalls.Station> station;
    private final IntField firmwareShare;
    private final DerivedInt shown;
    /* The inputs last worked out and what they made, so a job is worked out once and not every tick. */
    private List<ItemStack> lastInputs = List.of();
    private Optional<Processing> lastWork = Optional.empty();
    private Text lastRefusal = Text.EMPTY;
    private @Nullable Preinstalls.Job lastJob;

    /** Its inputs: the work, and the install media it reads, as many as a system on several floppies takes. */
    public static final int INPUTS = 9;
    /* How far through its job it is, and how much of the job the firmware takes, in thousandths. */
    private static final int WHOLE = 1_000;
    private static final int TICKS_A_SECOND = 20;

    public PreinstallerBlockEntity(final ChainMachine machine, final BlockPos pos, final BlockState state) {
        super(machine, pos, state);
        this.working = fields().value("Working", ItemStack.OPTIONAL_CODEC, ItemStack.EMPTY).toClient();
        this.station = fields().value("Station", StableCodecs.byName(Preinstalls.Station.class),
                Preinstalls.Station.CRADLE).toClient();
        this.firmwareShare = fields().integer("FirmwareShare", 0).toClient();
        this.shown = fields().derived("Shown", () -> getMaxProgress() <= 0 ? 0
                : (int) ((long) getProgress() * WHOLE / getMaxProgress())).toClient();
    }

    /** What it is writing onto, as it will come out; empty while it writes nothing. */
    public ItemStack working() {
        return working.get();
    }

    /** The station that does the job. */
    public Preinstalls.Station station() {
        return station.get();
    }

    /** How far through the job it is, from 0 to 1, as its players see it. */
    public float done() {
        return shown.getAsInt() / (float) WHOLE;
    }

    /** How much of the job the firmware takes, from 0 to 1: its console's amber bar, before the green one. */
    public float firmwareShare() {
        return firmwareShare.get() / (float) WHOLE;
    }

    /* The first thing among its inputs it writes onto, from the install media among them; why not, when it will not. */
    @Override
    protected Optional<Processing> process(final Level level, final ProcessingInput input) {
        if (!sameStacks(lastInputs, input.items())) {
            lastInputs = input.items().stream().map(ItemStack::copy).toList();
            workOut(input.items());
        }
        note(lastRefusal);
        if (lastJob != null && lastWork.isPresent()) {
            if (!ItemStack.matches(working.get(), lastJob.result())) {
                working.set(lastJob.result());
            }
            if (station.get() != lastJob.station()) {
                station.set(lastJob.station());
            }
            final int seconds = lastJob.seconds(machine().seconds());
            firmwareShare.set(lastJob.firmware() ? machine().seconds() * WHOLE / seconds : 0);
        }
        return lastWork;
    }

    /* Idle, it shows nothing at its stations; with nothing given it says nothing more. */
    @Override
    protected void settled(final ServerLevel level, final MachineStatus status) {
        super.settled(level, status);
        if (status != MachineStatus.WORKING && !working.get().isEmpty()) {
            working.set(ItemStack.EMPTY);
        }
        if (inputsEmpty()) {
            note(Text.EMPTY);
        }
    }

    private void workOut(final List<ItemStack> items) {
        int workAt = -1;
        final List<ItemStack> media = new ArrayList<>();
        for (int i = 0; i < items.size(); i++) {
            final ItemStack item = items.get(i);
            if (Preinstalls.isMedium(item)) {
                media.add(item);
            } else if (workAt < 0 && Preinstalls.isWork(item)) {
                workAt = i;
            }
        }
        lastWork = Optional.empty();
        lastJob = null;
        lastRefusal = Text.EMPTY;
        if (workAt < 0) {
            return;
        }
        final Preinstalls.Job job = Preinstalls.prepare(items.get(workAt), media);
        lastRefusal = job.refusal();
        if (job.done()) {
            lastJob = job;
            lastWork = Optional.of(new Processing(List.of(new Take(workAt, 1)), List.of(), List.of(job.result()),
                    List.of(), job.seconds(machine().seconds()) * TICKS_A_SECOND, 0));
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
