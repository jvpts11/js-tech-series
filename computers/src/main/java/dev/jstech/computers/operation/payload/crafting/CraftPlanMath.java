/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.operation.payload.crafting;

import dev.jstech.computers.blockentity.MainframeBlockEntity;
import dev.jstech.computers.crafting.CraftingPattern;
import dev.jstech.computers.operation.payload.CraftPlanPayload;
import dev.jstech.computers.storage.StorageKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;

/**
 * The arithmetic of a craft plan: the recipe choices, how many runs a machine recipe needs, what it yields and how
 * long it takes.
 */
public final class CraftPlanMath {

    private CraftPlanMath() {
    }

    /**
     * Every recipe that makes {@code key}, described for the craft dialog's cards: name, kind, machines, stages,
     * time, and the direct inputs for {@code quantity} against the stock, with whether each input that is
     * short can be crafted by something else on the network.
     */
    static java.util.List<dev.jstech.computers.crafting.RecipeChoice> recipeChoices(
            final ServerLevel level, final MainframeBlockEntity mainframe,
            final java.util.List<dev.jstech.computers.crafting.NetworkRecipe> recipes, final StorageKey key,
            final long quantity, final java.util.List<dev.jstech.computers.crafting.ProcessingPattern> machines,
            final java.util.Map<StorageKey, Long> stock) {
        final java.util.List<dev.jstech.computers.crafting.RecipeChoice> out = new java.util.ArrayList<>();
        for (final var recipe : recipes) {
            if (out.size() >= CraftPlanPayload.MAX_OPTIONS) {
                break;
            }
            final String kind;
            final java.util.List<String> machineNames = new java.util.ArrayList<>();
            final java.util.List<CraftPlanPayload.Row> rows;
            final int estimate;
            final int stages;
            boolean feasible;
            if (recipe.usesMachine()) {
                final MachinePlan plan = planMachineRecipe(recipe, quantity, stock);
                if (plan == null) {
                    continue;
                }
                rows = plan.rows();
                estimate = plan.estimateTicks();
                stages = plan.stages();
                feasible = plan.feasible();
                if (recipe.proc().isPresent()) {
                    kind = dev.jstech.computers.crafting.RecipeChoice.KIND_PROCESSING;
                    machineNames.add(dev.jstech.computers.crafting.MachineCategory.label(recipe.proc().get().machineType()));
                } else {
                    kind = dev.jstech.computers.crafting.RecipeChoice.KIND_MULTI_STAGE;
                    for (final var stage : recipe.multi().get().stages()) {
                        machineNames.add(stage.proc().isPresent()
                                ? dev.jstech.computers.crafting.MachineCategory.label(stage.proc().get().machineType())
                                : "Bench");
                    }
                }
            } else {
                kind = dev.jstech.computers.crafting.RecipeChoice.KIND_BENCH;
                final CraftingPattern bench = recipe.bench().get();
                final long runs = ceilDiv(quantity, Math.max(1, bench.result().getCount()));
                rows = new java.util.ArrayList<>();
                for (final var in : bench.ingredientTotals().entrySet()) {
                    final long need = in.getValue() * runs;
                    rows.add(new CraftPlanPayload.Row(in.getKey().stack(1), need,
                            Math.min(stock.getOrDefault(in.getKey(), 0L), need)));
                }
                final var patterns = mainframe.patternsPreferring(bench);
                final var plan = dev.jstech.computers.crafting.CraftPlanner.plan(key, quantity, patterns, machines, stock);
                estimate = estimateTicks(level, mainframe, plan);
                stages = Math.max(1, plan.steps().size());
                feasible = plan.feasible();
            }
            final java.util.List<dev.jstech.computers.crafting.RecipeChoice.Input> inputs = new java.util.ArrayList<>();
            boolean shortCraftable = true;
            for (final CraftPlanPayload.Row row : rows) {
                if (inputs.size() >= CraftPlanPayload.MAX_INPUTS) {
                    break;
                }
                final StorageKey inputKey = StorageKey.of(row.item());
                final boolean craftable = mainframe.anythingMakes(inputKey);
                if (!row.satisfied() && !craftable) {
                    shortCraftable = false;
                }
                inputs.add(new dev.jstech.computers.crafting.RecipeChoice.Input(
                        row.item().getHoverName().getString(), row.need(), stock.getOrDefault(inputKey, 0L), craftable));
            }
            // A processing run whose short inputs something makes runs as one tree, so it is feasible after all.
            if (!feasible && recipe.proc().isPresent() && shortCraftable) {
                feasible = dev.jstech.computers.crafting.CraftPlanner
                        .plan(key, quantity, mainframe.networkPatterns(), machines, stock).feasible();
            }
            out.add(new dev.jstech.computers.crafting.RecipeChoice(recipe.displayName(), kind, machineNames, stages,
                    estimate, inputs, feasible));
        }
        return out;
    }

    /** A plan preview: the raw-ingredient rows (need vs have), whether it is feasible, and how many are. */
    record PlanPreview(dev.jstech.computers.crafting.CraftPlanner.Plan plan,
                       java.util.List<CraftPlanPayload.Row> rows, boolean feasible, long maxFeasible) {
    }

    /** Plans {@code quantity} of {@code key} and shapes the dialog's rows; pure over its inputs. */
    static PlanPreview planPreview(final StorageKey key, final long quantity,
                                   final java.util.List<dev.jstech.computers.crafting.CraftingPattern> patterns,
                                   final java.util.List<dev.jstech.computers.crafting.ProcessingPattern> machines,
                                   final java.util.Map<StorageKey, Long> stock) {
        final var plan = dev.jstech.computers.crafting.CraftPlanner.plan(key, quantity, patterns, machines, stock);
        // Raw-ingredient rows: total needed (consumed + still missing) vs what the network has.
        final java.util.Map<StorageKey, Long> need = new java.util.LinkedHashMap<>(plan.rawConsumption());
        plan.missing().forEach((k, v) -> need.merge(k, v, Long::sum));
        final java.util.List<CraftPlanPayload.Row> rows = new java.util.ArrayList<>();
        for (final var entry : need.entrySet()) {
            if (rows.size() >= CraftPlanPayload.MAX_ROWS) {
                break;
            }
            final ItemStack icon = entry.getKey().stack(1);
            if (!icon.isEmpty()) {
                rows.add(new CraftPlanPayload.Row(icon, entry.getValue(),
                        Math.min(stock.getOrDefault(entry.getKey(), 0L), entry.getValue())));
            }
        }
        final boolean feasible = plan.feasible();
        final long maxFeasible = feasible ? quantity
                : dev.jstech.computers.crafting.CraftPlanner.maxFeasible(key, quantity, patterns, machines, stock);
        return new PlanPreview(plan, java.util.List.copyOf(rows), feasible, maxFeasible);
    }

    static int estimateTicks(final ServerLevel level, final MainframeBlockEntity mainframe,
                             final dev.jstech.computers.crafting.CraftPlanner.Plan plan) {
        long units = 0;
        long machineTicks = 0;
        for (final var step : plan.steps()) {
            units += step.runs() * step.unitsPerRun();
            if (step.isMachine()) {
                machineTicks += step.machine().timeoutTicks();
            }
        }
        long rate = 0;
        for (final net.minecraft.core.BlockPos pos : mainframe.craftingComputerPositions()) {
            if (level.getBlockEntity(pos)
                    instanceof dev.jstech.computers.blockentity.CraftingComputerBlockEntity cc
                    && cc.canCraft()) {
                rate = Math.max(rate, cc.craftingThroughput());
            }
        }
        if (rate <= 0) {
            return 0;
        }
        return (int) Math.max(1, (units + rate - 1) / rate + machineTicks);
    }

    /** A machine recipe's plan for the request popup: direct rows (need vs have), feasibility, max, estimate, stages. */
    record MachinePlan(java.util.List<CraftPlanPayload.Row> rows, boolean feasible, long maxFeasible,
                       int estimateTicks, boolean plainMachine, int stages) {
    }

    /**
     * Plans {@code quantity} of a machine-made result from the direct inputs of its recipe: a processing
     * pattern's inputs over the runs its primary output needs, or a multi-stage pipeline's FIRST stage inputs
     * over that stage's demand (later stages consume what earlier ones make). Returns null when the recipe is a
     * bench one or an empty pipeline, so the bench planner handles it.
     */
    @org.jetbrains.annotations.Nullable
    static MachinePlan planMachineRecipe(final dev.jstech.computers.crafting.NetworkRecipe recipe,
                                         final long quantity, final java.util.Map<StorageKey, Long> stock) {
        final dev.jstech.computers.crafting.ProcessingPattern first;
        final long firstDemand;
        int estimate;
        final int stages;
        if (recipe.proc().isPresent()) {
            first = recipe.proc().get();
            firstDemand = quantity;
            estimate = first.timeoutTicks();
            stages = 1;
        } else if (recipe.multi().isPresent() && !recipe.multi().get().stages().isEmpty()) {
            final var multi = recipe.multi().get();
            final long[] demands = multi.stageDemands(quantity);
            final var stage = multi.stages().get(0);
            firstDemand = demands[0];
            estimate = 0;
            stages = multi.stages().size();
            for (final var s : multi.stages()) {
                estimate += s.proc().map(dev.jstech.computers.crafting.ProcessingPattern
                        ::timeoutTicks).orElse(20);
            }
            if (stage.proc().isPresent()) {
                first = stage.proc().get();
            } else {
                // A bench-first pipeline: its raw inputs are the bench pattern's ingredients per run.
                final var bench = stage.bench().get();
                final long runs = ceilDiv(firstDemand, Math.max(1, bench.result().getCount()));
                final java.util.List<CraftPlanPayload.Row> rows = new java.util.ArrayList<>();
                long maxRuns = Long.MAX_VALUE;
                for (final var in : bench.ingredientTotals().entrySet()) {
                    final long need = in.getValue() * runs;
                    final long have = stock.getOrDefault(in.getKey(), 0L);
                    maxRuns = Math.min(maxRuns, have / Math.max(1, in.getValue()));
                    rows.add(new CraftPlanPayload.Row(in.getKey().stack(1), need, Math.min(have, need)));
                }
                final long maxFirst = maxRuns == Long.MAX_VALUE ? 0 : maxRuns * bench.result().getCount();
                return new MachinePlan(java.util.List.copyOf(rows), maxFirst >= firstDemand,
                        Math.min(quantity, forwardYield(multi, maxFirst)), estimate, false, stages);
            }
        } else {
            return null;
        }
        final var primary = first.primaryOutput();
        final long perRun = primary == null ? 1 : Math.max(1, primary.amount());
        final long runs = ceilDiv(firstDemand, perRun);
        final java.util.List<CraftPlanPayload.Row> rows = new java.util.ArrayList<>();
        long maxRuns = Long.MAX_VALUE;
        for (final var in : first.inputs()) {
            final long need = in.amount() * runs;
            final long have = stock.getOrDefault(in.key(), 0L);
            maxRuns = Math.min(maxRuns, have / Math.max(1, in.amount()));
            final ItemStack icon = in.key().stack(1);
            rows.add(new CraftPlanPayload.Row(icon, need, Math.min(have, need)));
        }
        final long maxFirst = maxRuns == Long.MAX_VALUE ? 0 : maxRuns * perRun;
        final long maxFinal = recipe.multi().isPresent()
                ? forwardYield(recipe.multi().get(), maxFirst) : maxFirst;
        return new MachinePlan(java.util.List.copyOf(rows), maxFirst >= firstDemand,
                Math.min(quantity, maxFinal), estimate, recipe.proc().isPresent(), stages);
    }

    private static long ceilDiv(final long amount, final long perRun) {
        return (amount + perRun - 1) / perRun;
    }

    /** How much of the final result a pipeline yields when its first stage produces {@code firstOutput}. */
    private static long forwardYield(final dev.jstech.computers.crafting.MultiStagePattern multi,
                                     final long firstOutput) {
        // Walk the stage demands for one unit of final result to get each stage's output per final unit.
        final long[] perUnit = multi.stageDemands(1);
        return perUnit.length == 0 || perUnit[0] <= 0 ? firstOutput : firstOutput / perUnit[0];
    }
}
