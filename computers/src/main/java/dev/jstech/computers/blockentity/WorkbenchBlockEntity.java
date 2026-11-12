/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.blockentity;

import com.mojang.serialization.Codec;
import dev.jstech.computers.block.WorkbenchBlock;
import dev.jstech.computers.chain.ChainBenches;
import dev.jstech.core.blockentity.FieldItemHandler;
import dev.jstech.core.blockentity.SyncedBlockEntity;
import dev.jstech.core.blockentity.ValueField;
import dev.jstech.core.energy.EnergyStore;
import dev.jstech.core.energy.IEnergyPort;
import dev.jstech.core.jstechseries.energy.JsEnergy;
import dev.jstech.core.jstechseries.tier.Era;
import dev.jstech.core.jstechseries.tier.HardwareEra;
import dev.jstech.core.machine.ProcessingInput;
import dev.jstech.core.machine.ProcessingKind;
import dev.jstech.core.machine.ProcessingRecipe;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.energy.IEnergyStorage;
import org.jetbrains.annotations.Nullable;

/**
 * An Electronics Workbench at work (computers/docs/design/survival.md): it assembles the part whose recipe its nine
 * trays hold, its era's or an older era's, as a furnace smelts, spending its era's watts from the energy behind it while
 * it works, the window open or closed; the finished part waits in its output tray. A recipe is worked only when the
 * trays hold its ingredients and nothing else, each in a tray of its own and in any order. Its trays are the player's
 * own: nothing reaches them through its sides.
 */
public class WorkbenchBlockEntity extends SyncedBlockEntity {

    private final EnergyStore store = new EnergyStore("Energy", JsEnergy.ENERGY, JsEnergy.LOW, 0L, TAKES_A_TICK, 0L,
            this::setChanged);
    private final FieldItemHandler trays = fields().items("Trays", TRAYS).save().dropsWhenBroken()
            .onChange(slot -> this.changed = true).onLoad(() -> this.changed = true);
    /* Only the bench puts into it: its menu's slot takes nothing from the player, and no side reaches it. */
    private final FieldItemHandler output = fields().items("Output", 1).save().dropsWhenBroken();
    private final ValueField<Integer> progress = fields().value("Progress", Codec.INT, 0).save();
    private final TickDraw draw = new TickDraw();
    /* The recipe the trays hold, found again whenever what is in them changes. */
    @Nullable
    private Match working;
    private boolean changed = true;
    private boolean powered;

    /** How many trays it lays a part's ingredients in. */
    public static final int TRAYS = 9;
    /* The most its store takes from the line in a tick: far more than any bench draws. */
    private static final long TAKES_A_TICK = 1_000L;

    public WorkbenchBlockEntity(final BlockPos pos, final BlockState state) {
        super(ChainBenches.BENCH_BE.get(), pos, state);
        fields().part("Energy", store).save();
    }

    public static void serverTick(final Level level, final BlockPos pos, final BlockState state,
                                  final WorkbenchBlockEntity bench) {
        if (level instanceof ServerLevel server) {
            bench.tick(server);
        }
    }

    /** The era whose parts, and the older ones', it assembles. */
    public Era era() {
        return getBlockState().getBlock() instanceof WorkbenchBlock block ? block.era() : HardwareEra.VINTAGE;
    }

    /** What it draws while it works, in watts: 40, 60, 300, 80 and 120 from the Vintage to the Advanced. */
    public static int watts(final Era era) {
        return HardwareEra.pick(era, 40, 60, 300, 80, 120);
    }

    /** The nine trays the player lays a part's ingredients in. */
    public FieldItemHandler trays() {
        return trays;
    }

    /** The tray the finished part waits in; nothing is put into it from outside. */
    public FieldItemHandler output() {
        return output;
    }

    /** How far the part under way has come, from 0 to 1; 0 while nothing is under way. */
    public double progress() {
        return working == null ? 0.0 : Math.min(1.0, (double) progress.get() / working.recipe().ticks());
    }

    /** The part under way, as its recipe makes it, or an empty stack. */
    public ItemStack partUnderWay() {
        return working == null || working.recipe().outputs().isEmpty() ? ItemStack.EMPTY
                : working.recipe().outputs().getFirst().copy();
    }

    /** Whether energy reached it on its last tick, as its lamp shows. */
    public boolean powered() {
        return powered;
    }

    /** How full its store of energy is, from 0 to 1. */
    public double stored() {
        return store.capacity() == 0 ? 0.0 : Math.min(1.0, (double) store.stored() / store.capacity());
    }

    /** Its energy port, behind it and nowhere else. */
    @Nullable
    public IEnergyPort energyPort(@Nullable final Direction side) {
        return side == WorkbenchBlock.cableFace(getBlockState()) ? store : null;
    }

    /** What the game's FE capability sees there: the same store, while the series speaks FE. */
    @Nullable
    public IEnergyStorage feEnergy(@Nullable final Direction side) {
        return side == WorkbenchBlock.cableFace(getBlockState()) && JsEnergy.speaksFe() ? store.asFe() : null;
    }

    /*
     * A tick: the recipe the trays hold, found again only when they changed; worked a tick while its part has room in
     * the output tray and the line gives the bench its watts; taken and put out once its time is up.
     */
    private void tick(final ServerLevel level) {
        if (changed) {
            changed = false;
            final Match found = find(level);
            // Loaded with none found yet, the bench goes on with the progress it saved on the part its trays hold.
            if (found == null || working != null && !found.holder().id().equals(working.holder().id())) {
                progress.set(0);
            }
            working = found;
        }
        final boolean drew = working != null && fits(working.recipe()) && draw.draw(store, watts(era()));
        powered = store.stored() > 0;
        if (!drew) {
            return;
        }
        progress.set(progress.get() + 1);
        if (progress.get() >= working.recipe().ticks()) {
            finish(working);
            progress.set(0);
            changed = true;
        }
        setChanged();
    }

    /*
     * The first recipe of the bench's kinds whose ingredients the trays hold, each in a tray, with nothing besides:
     * the bench's kinds take exactly their ingredients.
     */
    @Nullable
    private Match find(final ServerLevel level) {
        final List<ItemStack> items = new ArrayList<>(TRAYS);
        for (int slot = 0; slot < TRAYS; slot++) {
            items.add(trays.getStackInSlot(slot));
        }
        final ProcessingInput input = new ProcessingInput(items, List.of());
        for (final ProcessingKind kind : ChainBenches.assembledAt(era())) {
            for (final RecipeHolder<ProcessingRecipe> holder : kind.findAll(level, input)) {
                final int[] slots = holder.value().slotsFor(input);
                if (slots != null) {
                    return new Match(holder, slots);
                }
            }
        }
        return null;
    }

    /* Whether every part the recipe makes has room in the output tray. */
    private boolean fits(final ProcessingRecipe recipe) {
        for (final ItemStack made : recipe.outputs()) {
            if (!output.insertItem(0, made.copy(), true).isEmpty()) {
                return false;
            }
        }
        return !recipe.outputs().isEmpty();
    }

    /* Takes each ingredient from its tray and puts the parts in the output tray. */
    private void finish(final Match match) {
        final ProcessingRecipe recipe = match.recipe();
        for (int i = 0; i < match.slots().length; i++) {
            trays.extractItem(match.slots()[i], recipe.inputs().get(i).count(), false);
        }
        for (final ItemStack made : recipe.outputs()) {
            output.insertItem(0, made.copy(), false);
        }
    }

    /**
     * A recipe the trays hold, and the tray each of its ingredients is taken from.
     *
     * @param holder the recipe
     * @param slots  the tray of each ingredient, in the recipe's order
     */
    private record Match(RecipeHolder<ProcessingRecipe> holder, int[] slots) {

        Match {
            slots = slots.clone();
        }

        ProcessingRecipe recipe() {
            return holder.value();
        }
    }
}
