/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.operation.payload.crafting;

import dev.jstech.computers.blockentity.MainframeBlockEntity;
import dev.jstech.computers.client.os.CraftPlannerApp;
import dev.jstech.computers.client.os.NetworkInteractorApp;
import dev.jstech.computers.crafting.AnyTagResolver;
import dev.jstech.computers.crafting.CraftPlanner;
import dev.jstech.computers.crafting.CraftingPattern;
import dev.jstech.computers.crafting.NetworkRecipe;
import dev.jstech.computers.crafting.ProcessingPattern;
import dev.jstech.computers.crafting.RecipeChoice;
import dev.jstech.computers.menu.ComputerTerminalMenu;
import dev.jstech.computers.menu.CraftingSwitchMenu;
import dev.jstech.computers.operation.MoveLabels;
import dev.jstech.computers.operation.NetworkStorage;
import dev.jstech.computers.operation.payload.ClientPayloadHandlers;
import dev.jstech.computers.operation.payload.ComputerAccess;
import dev.jstech.computers.operation.payload.CraftCatalogPayload;
import dev.jstech.computers.operation.payload.CraftPlanPayload;
import dev.jstech.computers.operation.payload.CraftPlanRequestPayload;
import dev.jstech.computers.operation.payload.CraftSubmitPayload;
import dev.jstech.computers.operation.payload.SetCraftingSwitchFacePayload;
import dev.jstech.computers.operation.payload.crafting.CraftPlanMath.PlanPreview;
import dev.jstech.computers.os.IOsHost;
import dev.jstech.computers.storage.StorageKey;
import dev.jstech.computers.terminal.IComputerTerminalHost;
import dev.jstech.core.operation.IOperationResult;
import dev.jstech.core.operation.OperationPriority;
import dev.jstech.core.uuid.NetworkUuid;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import java.util.List;

import static dev.jstech.computers.operation.payload.WireStrings.wire;
import static dev.jstech.computers.operation.payload.crafting.CraftPlanMath.estimateTicks;
import static dev.jstech.computers.operation.payload.crafting.CraftPlanMath.planMachineRecipe;
import static dev.jstech.computers.operation.payload.crafting.CraftPlanMath.planPreview;
import static dev.jstech.computers.operation.payload.crafting.CraftPlanMath.recipeChoices;
import static dev.jstech.computers.operation.payload.network.NetworkLookup.resolveMainframe;
import static dev.jstech.computers.operation.payload.operations.OperationsPayloads.dispatchActiveOperations;
import static dev.jstech.computers.operation.payload.operations.OperationsPayloads.dispatchTerminalOpsLog;
import static dev.jstech.computers.operation.payload.terminal.TerminalHosts.craftHost;

/**
 * The payloads that plan and submit a craft from a terminal, send the list of what the network can craft and set
 * the face of a crafting switch.
 */
public final class CraftingPayloads {

    private CraftingPayloads() {
    }

    /** Registers the payloads this class handles. */
    public static void register(final PayloadRegistrar registrar) {
        registrar.playToClient(CraftCatalogPayload.TYPE, CraftCatalogPayload.STREAM_CODEC,
                ClientPayloadHandlers.onMainThread(CraftingPayloads::handleCraftCatalog));
        ComputerAccess.accept(registrar, CraftPlanRequestPayload.TYPE, CraftPlanRequestPayload.STREAM_CODEC,
                ComputerAccess.machine(CraftPlanRequestPayload::hostPos), CraftingPayloads::handleCraftPlanRequest);
        registrar.playToClient(CraftPlanPayload.TYPE, CraftPlanPayload.STREAM_CODEC,
                ClientPayloadHandlers.onMainThread(CraftingPayloads::handleCraftPlan));
        ComputerAccess.accept(registrar, CraftSubmitPayload.TYPE, CraftSubmitPayload.STREAM_CODEC,
                ComputerAccess.machine(CraftSubmitPayload::hostPos), CraftingPayloads::handleCraftSubmit);
        ComputerAccess.accept(registrar, SetCraftingSwitchFacePayload.TYPE, SetCraftingSwitchFacePayload.STREAM_CODEC,
                ComputerAccess.menu(CraftingSwitchMenu.class,
                        CraftingSwitchMenu::switchPos, SetCraftingSwitchFacePayload::switchPos),
                CraftingPayloads::handleSetCraftingSwitchFace);
    }

    private static void handleCraftCatalog(final CraftCatalogPayload payload, final Player player) {
        if (player.containerMenu instanceof ComputerTerminalMenu menu) {
            menu.setCraftCatalog(payload.entries());
        } else {
            // The desktop Craft Planner has no container menu; route the catalogue to it.
            CraftPlannerApp.acceptCatalog(payload.entries());
        }
    }

    private static void handleCraftPlan(final CraftPlanPayload payload, final Player player) {
        if (player.containerMenu instanceof ComputerTerminalMenu menu) {
            menu.setCraftPlan(payload);
        } else {
            // The desktop Network Interactor has no container menu; route the plan to its craft popup.
            NetworkInteractorApp.acceptCraftPlan(payload);
        }
    }

    public static void dispatchCraftCatalog(final ServerPlayer player, final NetworkUuid net,
                                            final ServerLevel level) {
        PacketDistributor.sendToPlayer(player, new CraftCatalogPayload(buildCraftCatalog(level, net)));
    }

    /** The network's craft catalog (distinct ROM results + availability dots), shared by the terminal and the desktop. */
    public static List<CraftCatalogPayload.Entry> buildCraftCatalog(final ServerLevel level, final NetworkUuid net) {
        final MainframeBlockEntity mainframe = net == null ? null : resolveMainframe(level, net);
        if (mainframe == null) {
            return List.of();
        }
        final var stock = NetworkStorage
                .of(level, net).query();
        // "Any" cells are judged against what the network holds, the way a craft would resolve them.
        final var patterns = AnyTagResolver
                .resolveAll(mainframe.networkPatterns(), stock);
        final Map<StorageKey, CraftCatalogPayload.Entry> entries = new LinkedHashMap<>();
        for (final var pattern : patterns) {
            final StorageKey key = StorageKey.of(pattern.result());
            if (entries.containsKey(key)) {
                continue;
            }
            final byte dot;
            if (CraftPlanner
                    .plan(key, 1, patterns, stock).feasible()) {
                dot = CraftCatalogPayload.DOT_GREEN;
            } else {
                boolean any = false;
                for (final StorageKey ingredient : pattern.ingredientTotals().keySet()) {
                    if (stock.getOrDefault(ingredient, 0L) > 0L) {
                        any = true;
                        break;
                    }
                }
                dot = any ? CraftCatalogPayload.DOT_AMBER : CraftCatalogPayload.DOT_RED;
            }
            entries.put(key, new CraftCatalogPayload.Entry(pattern.result().copy(), dot,
                    mainframe.hasMultiStageRecipe(key), wire(pattern.name(), CraftCatalogPayload.MAX_LABEL)));
            if (entries.size() >= CraftCatalogPayload.MAX_ENTRIES) {
                break;
            }
        }
        // Machine recipes (processing / multi-stage) the network can run, shown by their primary item result.
        for (final var recipe : mainframe.networkMachineRecipes()) {
            if (entries.size() >= CraftCatalogPayload.MAX_ENTRIES) {
                break;
            }
            final StorageKey key = recipe.resultKey();
            if (key == null || entries.containsKey(key)) {
                continue;
            }
            final ItemStack result = key.stack(1);
            if (result.isEmpty()) {
                continue; // a fluid result: the item catalog cannot render it yet (v1)
            }
            byte dot = CraftCatalogPayload.DOT_AMBER;
            if (recipe.proc().isPresent()) {
                boolean all = true;
                boolean any = false;
                for (final var in : recipe.proc().get().inputs()) {
                    if (stock.getOrDefault(in.key(), 0L) >= in.amount()) {
                        any = true;
                    } else {
                        all = false;
                    }
                }
                dot = all ? CraftCatalogPayload.DOT_GREEN
                        : (any ? CraftCatalogPayload.DOT_AMBER : CraftCatalogPayload.DOT_RED);
            }
            final String label = recipe.proc().map(p -> p.name()).orElse(recipe.multi().map(m -> m.name()).orElse(""));
            entries.put(key, new CraftCatalogPayload.Entry(result, dot, recipe.multi().isPresent(),
                    wire(label, CraftCatalogPayload.MAX_LABEL)));
        }
        return List.copyOf(entries.values());
    }

    private static void handleCraftPlanRequest(final CraftPlanRequestPayload payload, final ServerPlayer player,
                                               final ServerLevel level) {
        final IComputerTerminalHost host = craftHost(player, level, payload.monitorPos(), payload.hostPos());
        if (host == null || host.networkUuid() == null || payload.quantity() <= 0L) {
            return;
        }
        final MainframeBlockEntity mainframe = resolveMainframe(level, host.networkUuid());
        if (mainframe == null) {
            return;
        }
        final var machines = mainframe.networkProcessingPatterns();
        final var stock = NetworkStorage
                .of(level, host.networkUuid()).query();
        final StorageKey key = StorageKey.of(payload.result());
        final long quantity = payload.quantity();
        final ItemStack result = payload.result();
        /*
         * Which recipe to plan with: the one the dialog named, else the one this machine remembers the
         * player picking for the item, else the first. The reply carries every recipe that makes the item
         * when there is more than one, so the dialog can offer the choice.
         */
        final List<NetworkRecipe> recipes = mainframe.recipesFor(key);
        int chosen = payload.recipe();
        if (chosen < 0 || chosen >= recipes.size()) {
            chosen = rememberedRecipe(host, key, recipes.size());
        }
        final List<RecipeChoice> options = recipes.size() > 1
                ? recipeChoices(level, mainframe, recipes, key, quantity, machines, stock) : List.of();
        final NetworkRecipe recipe = recipes.isEmpty() ? null : recipes.get(chosen);
        final var patterns = mainframe.patternsPreferring(recipe == null ? null : recipe.bench().orElse(null));
        final int recipeIndex = chosen;
        /*
         * A machine recipe plans by its own direct inputs, red where short, with a line per shortfall saying
         * what the network would craft to cover it (a processing run's whole tree covers it; a pipeline runs
         * on what is in stock). Otherwise the recursive planner expands bench and machine patterns alike, so
         * a machine-made ingredient shows up as the raw materials of its own recipe rather than as missing.
         */
        final var machinePlan = recipe == null || !recipe.usesMachine() ? null
                : planMachineRecipe(recipe, quantity, stock);
        if (machinePlan != null) {
            final Cover cover = coverShortfalls(machinePlan.rows(), patterns, machines, stock,
                    machinePlan.plainMachine());
            final boolean feasible = machinePlan.feasible()
                    || (machinePlan.plainMachine() && cover.covered()
                            && CraftPlanner.plan(key, quantity, patterns, machines, stock)
                                    .feasible());
            PacketDistributor.sendToPlayer(player, new CraftPlanPayload(
                    result, quantity, machinePlan.rows(), feasible,
                    feasible ? quantity : machinePlan.maxFeasible(), machinePlan.estimateTicks(),
                    recipeIndex, options, cover.lines(), machinePlan.stages()));
            return;
        }
        /*
         * The recursive plan is CPU work over immutable inputs: it runs on a virtual thread and the reply
         * goes out from the main thread when it is ready (the dialog shows "planning..." meanwhile). Without
         * a dispatcher the plan is made here and now instead.
         */
        final Supplier<PlanPreview> preview =
                () -> planPreview(key, quantity, patterns, machines, stock);
        final Consumer<PlanPreview> reply = made ->
                PacketDistributor.sendToPlayer(player, new CraftPlanPayload(result, quantity, made.rows(),
                        made.feasible(), made.maxFeasible(), estimateTicks(level, mainframe, made.plan()),
                        recipeIndex, options, unmakeableLines(made.plan()), Math.max(1, made.plan().steps().size())));
        final boolean queued = mainframe.submitOperation(task -> {
            final PlanPreview made = preview.get();
            task.onMainThread(() -> reply.accept(made));
            return IOperationResult.success();
        }, OperationPriority.MEDIUM);
        if (!queued) {
            reply.accept(preview.get());
        }
    }

    /** The recipe index this machine remembers for {@code key} when it is still one of {@code count}, else 0. */
    private static int rememberedRecipe(final IComputerTerminalHost host, final StorageKey key, final int count) {
        if (host instanceof IOsHost computer && computer.console() != null) {
            final int remembered = computer.console().settings().recipeChoice(key.id());
            if (remembered >= 0 && remembered < count) {
                return remembered;
            }
        }
        return 0;
    }

    /** What the network would craft to cover the short rows, one line each, and whether every one is coverable. */
    private record Cover(List<String> lines, boolean covered) {
    }

    /**
     * One line per short row. A processing run's tree crafts what is short when a pattern makes it
     * ({@code treeCovers}); a pipeline runs on what is in stock, so its line says to request the thing first.
     */
    private static Cover coverShortfalls(final List<CraftPlanPayload.Row> rows,
                                         final List<CraftingPattern> patterns,
                                         final List<ProcessingPattern> machines,
                                         final Map<StorageKey, Long> stock, final boolean treeCovers) {
        final List<String> lines = new ArrayList<>();
        boolean covered = true;
        for (final CraftPlanPayload.Row row : rows) {
            if (row.satisfied()) {
                continue;
            }
            final long shortfall = row.need() - row.have();
            final String name = row.item().getHoverName().getString();
            final var plan = CraftPlanner.plan(
                    StorageKey.of(row.item()), shortfall, patterns, machines, stock);
            if (plan.feasible() && !plan.steps().isEmpty()) {
                if (lines.size() < CraftPlanPayload.MAX_COVER) {
                    lines.add(treeCovers
                            ? "Missing " + shortfall + " " + name + " · will be crafted from " + rawSummary(plan)
                                    + " (" + firstStepName(plan) + " pattern) before the stages start"
                            : "Missing " + shortfall + " " + name + " · a pipeline runs on stock, craft it first ("
                                    + firstStepName(plan) + " pattern)");
                }
                covered &= treeCovers;
            } else {
                covered = false;
                if (lines.size() < CraftPlanPayload.MAX_COVER) {
                    lines.add("Missing " + shortfall + " " + name + " · nothing on the network makes it");
                }
            }
        }
        return new Cover(List.copyOf(lines), covered);
    }

    /** One line per thing a recursive plan found nothing to make (or not enough of in stock). */
    private static List<String> unmakeableLines(final CraftPlanner.Plan plan) {
        final List<String> lines = new ArrayList<>();
        for (final var missing : plan.missing().entrySet()) {
            if (lines.size() >= CraftPlanPayload.MAX_COVER) {
                break;
            }
            lines.add("Missing " + missing.getValue() + " " + missing.getKey().displayName().getString()
                    + " · nothing on the network makes it");
        }
        return lines;
    }

    /** "4 Logs, 2 Coal": the raw stock a plan consumes, at most three named. */
    private static String rawSummary(final CraftPlanner.Plan plan) {
        final StringBuilder out = new StringBuilder();
        int named = 0;
        for (final var raw : plan.rawConsumption().entrySet()) {
            if (named == 3) {
                out.append(", ...");
                break;
            }
            if (named > 0) {
                out.append(", ");
            }
            out.append(raw.getValue()).append(' ').append(raw.getKey().displayName().getString());
            named++;
        }
        return out.length() == 0 ? "stock" : out.toString();
    }

    private static String firstStepName(final CraftPlanner.Plan plan) {
        final var step = plan.steps().get(0);
        return step.isMachine() ? step.machine().displayName() : step.pattern().displayName();
    }

    private static void handleCraftSubmit(final CraftSubmitPayload payload, final ServerPlayer player,
                                          final ServerLevel level) {
        final IComputerTerminalHost host = craftHost(player, level, payload.monitorPos(), payload.hostPos());
        if (host == null || host.networkUuid() == null || payload.quantity() <= 0L) {
            return;
        }
        final NetworkUuid net = host.networkUuid();
        final MainframeBlockEntity mainframe = resolveMainframe(level, net);
        if (mainframe == null) {
            return;
        }
        final StorageKey resultKey = StorageKey.of(payload.result());
        final Runnable refresh = () -> {
            dispatchTerminalOpsLog(player, net, level);
            dispatchActiveOperations(player, net, level);
            dispatchCraftCatalog(player, net, level);
        };
        /*
         * The shared entry point runs a machine or multi-stage recipe directly, else plans a recursive
         * craft; onSettle refreshes the screen when it settles, and refresh.run() updates it now. A recipe
         * the dialog named runs as picked; without one, the multiStage flag picks the pipeline over the flat
         * recursive path when a result has both.
         */
        final var op = payload.recipe() >= 0
                ? mainframe.submitCraftRequest(resultKey, payload.quantity(), payload.partial(),
                        host.originLabel(MoveLabels.TERMINAL), refresh, payload.recipe())
                : mainframe.submitCraftRequest(resultKey, payload.quantity(), payload.partial(),
                        host.originLabel(MoveLabels.TERMINAL), refresh, payload.multiStage());
        if (op != null) {
            op.setPriority(payload.priority());
        }
        refresh.run();
    }

    private static void handleSetCraftingSwitchFace(final SetCraftingSwitchFacePayload payload,
                                                    final ServerPlayer player, final ServerLevel level) {
        final BlockPos pos = payload.switchPos();
        if (player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) > 64.0) {
            return; // out of reach
        }
        if (level.getBlockEntity(pos) instanceof dev.jstech.computers.blockentity
                .CraftingSwitchBlockEntity sw) {
            final Direction face =
                    Direction.from3DDataValue(payload.face());
            sw.setFaceName(face, payload.name());
            sw.setFaceActive(face, payload.active());
            sw.setFaceCategory(face, payload.category());
            final BlockState st = level.getBlockState(pos);
            level.sendBlockUpdated(pos, st, st, Block.UPDATE_CLIENTS);
        }
    }
}
