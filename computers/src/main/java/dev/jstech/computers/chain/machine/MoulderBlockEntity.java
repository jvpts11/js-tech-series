/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.chain.machine;

import dev.jstech.core.blockentity.ValueField;
import dev.jstech.core.machine.ProcessingInput;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * The Injection Moulder's terminal: it moulds the plastic bodies of its recipes in the mould each job lends it, and,
 * while it moulds nothing, cuts the mould its computer's lithography program picked on the Mould Blank it holds
 * ({@link MouldCutting}), setting the mould where its blank was.
 */
public final class MoulderBlockEntity extends ChainMachineBlockEntity {

    /* The mould the lithography program asked for, kept till it is cut; none when none was asked for. */
    private final ValueField<Item> cuts;
    /* The mould it is cutting, while the work it works is that. */
    private MouldCutting.@Nullable Cutting cutting;

    public MoulderBlockEntity(final ChainMachine machine, final BlockPos pos, final BlockState state) {
        super(machine, pos, state);
        this.cuts = fields().nullable("Cuts", BuiltInRegistries.ITEM.byNameCodec()).save();
    }

    /** The mould it is to cut on the blank it holds, or null when none was asked for. */
    public @Nullable Item cuts() {
        return cuts.get();
    }

    /** Whether it is cutting a mould, part of the way through it. */
    public boolean cutting() {
        return cutting != null && getProgress() > 0;
    }

    /** Whether it is moulding a part, part of the way through it. */
    public boolean moulding() {
        return cutting == null && getProgress() > 0;
    }

    /** What its input slots hold, the first first. */
    public List<ItemStack> inputs() {
        final List<ItemStack> out = new ArrayList<>();
        for (int i = 0; i < layout().inputs(); i++) {
            out.add(getInventory().getStackInSlot(layout().laneInput(0, i)));
        }
        return out;
    }

    /* A moulder is listed for the computers that look for theirs while it is in the world. */
    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && !level.isClientSide()) {
            LithoStations.loaded(this);
        }
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        if (level != null && !level.isClientSide()) {
            LithoStations.gone(this);
        }
    }

    /** Has it cut {@code mould} on the blank it holds, as soon as it may ({@link MouldCutting#cut}). */
    void cut(final Item mould) {
        cuts.set(mould);
    }

    /* The mould asked for when its blank is there and its computer runs; its recipes otherwise. */
    @Override
    protected Optional<Processing> process(final Level level, final ProcessingInput input) {
        cutting = level instanceof ServerLevel server ? MouldCutting.work(server, this, input, cuts.get()).orElse(null)
                : null;
        if (cutting != null) {
            return run(level, input, cutting.work(), null);
        }
        return super.process(level, input);
    }

    /* A mould cut stands where its blank stood, and nothing more is cut till the program asks again. */
    @Override
    protected void madeThings(final int count, final long gameTime) {
        super.madeThings(count, gameTime);
        if (cutting != null) {
            setInPlace(cutting);
            cuts.set(null);
            cutting = null;
        }
    }

    /* The mould {@code cut} made, moved from its outputs to the slot its blank stood in once that is empty. */
    private void setInPlace(final MouldCutting.Cutting cut) {
        final int slot = layout().laneInput(0, cut.slot());
        if (!getInventory().getStackInSlot(slot).isEmpty()) {
            return;
        }
        for (int i = 0; i < layout().outputs(); i++) {
            final int out = layout().laneOutput(0, i);
            if (getInventory().getStackInSlot(out).is(cut.mould())) {
                getInventory().setStackInSlot(slot, getInventory().extractItem(out, 1, false));
                return;
            }
        }
    }
}
