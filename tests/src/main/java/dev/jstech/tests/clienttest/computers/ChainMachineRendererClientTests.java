/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest.computers;

import dev.jstech.computers.chain.ChainParts;
import dev.jstech.computers.chain.machine.ChainMachine;
import dev.jstech.computers.chain.machine.ChainMachineBlockEntity;
import dev.jstech.computers.chain.machine.ChainMachineBlocks;
import dev.jstech.computers.chain.machine.ChainMachines;
import dev.jstech.computers.client.chain.ChainMachineRenderer;
import dev.jstech.core.client.model.kit.KitModels;
import dev.jstech.core.jstechseries.SeriesCompounds;
import dev.jstech.core.machine.MachineStatus;
import dev.jstech.core.material.Forms;
import dev.jstech.core.material.MaterialUses;
import dev.jstech.core.model.ModelData;
import dev.jstech.core.model.ModelPose;
import dev.jstech.tests.clienttest.ClientTest;
import dev.jstech.tests.clienttest.ClientTestContext;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * What a chain machine's renderer shows of its model: the work going through it and the glow of its work only while
 * it works, so an idle machine stands empty and cold, drawn by what only an idle machine has (a crucible's cold
 * floor). An idle Crusher holds no ore, falling or lying on its bars; an idle Induction Furnace no melt, no charge.
 */
public final class ChainMachineRendererClientTests {

    private static final int SETTLE = 4;
    private static final int WAIT = 400;
    private static final BlockPos CRUSHER_AT = new BlockPos(2, 2, 4);
    private static final BlockPos FURNACE_AT = new BlockPos(8, 2, 4);
    private static final BlockPos STATION_AT = new BlockPos(14, 2, 4);
    private static final String MODE = "mode_";
    private static final String WORK = "work_";
    private static final String IDLE = "idle_";
    private static final String TOOL = "tool_";
    private static final int LOTS = 8;

    private ChainMachineRendererClientTests() {
    }

    /* An idle Lithography Station shows the mask a player left in its slot, the memory chip's, and none with none. */
    @ClientTest(timeoutTicks = 1200)
    public static void lithography_showsTheMaskLeftInItsSlotIdle(final ClientTestContext ctx) {
        final ChainMachine machine = ChainMachines.byId("lithography_vintage").orElseThrow();
        final BlockPos terminal = terminalOf(machine, STATION_AT);
        ctx.thenBuild(0, world -> build(world::setBlock, machine, STATION_AT))
                .thenWaitUntilServer(level -> serverMachine(ctx, level, terminal).formed(), SETTLE * 30,
                        "the station to form", level -> "it stands unformed")
                .thenWaitUntil(() -> clientMachine(ctx, terminal) != null && clientMachine(ctx, terminal).formed(),
                        SETTLE * 30, "the station to form on the client")
                .thenAssert(SETTLE, () -> !bones(machine, TOOL).isEmpty(), "the station's model has its mask")
                .thenAssert(0, () -> shown(ctx, machine, terminal, TOOL).isEmpty(),
                        "with no mask in its slot, the station shows none")
                .thenServer(0, level -> serverMachine(ctx, level, terminal).getInventory().insertItem(0,
                        new ItemStack(ChainParts.MEMORY_MASKS.getFirst().get()), false))
                .thenWaitUntil(() -> !clientMachine(ctx, terminal).tools().isEmpty(), SETTLE * 30,
                        "the client to learn of the mask")
                .thenAssert(0, () -> !shown(ctx, machine, terminal, TOOL).isEmpty() && shown(ctx, machine, terminal,
                                TOOL).stream().allMatch(name -> name.startsWith(MODE + "memory_")),
                        "idle, the station shows the memory mask left in its slot");
    }

    @ClientTest(timeoutTicks = 1200)
    public static void crusher_standsEmptyIdleAndShowsItsOreAtWork(final ClientTestContext ctx) {
        idleThenAtWork(ctx, "crusher", CRUSHER_AT, List.of(new ItemStack(ChainParts.FERRITE.get(), LOTS)));
    }

    @ClientTest(timeoutTicks = 1200)
    public static void inductionFurnace_standsColdIdleAndMeltsAtWork(final ClientTestContext ctx) {
        idleThenAtWork(ctx, "induction_furnace", FURNACE_AT, List.of(new ItemStack(Items.CALCITE, LOTS),
                new ItemStack(MaterialUses.item(SeriesCompounds.COKE, Forms.LUMP).get(), LOTS)));
    }

    /*
     * Builds machine {@code id} at {@code corner}; idle, its renderer shows none of its work and all that only an
     * idle one has; fed {@code inputs} and at work, all its work and none of that.
     */
    private static void idleThenAtWork(final ClientTestContext ctx, final String id, final BlockPos corner,
                                       final List<ItemStack> inputs) {
        final ChainMachine machine = ChainMachines.byId(id).orElseThrow();
        final BlockPos terminal = terminalOf(machine, corner);
        ctx.thenBuild(0, world -> build(world::setBlock, machine, corner))
                .thenWaitUntilServer(level -> serverMachine(ctx, level, terminal).formed(), SETTLE * 30,
                        "the " + id + " to form", level -> "it stands unformed")
                .thenWaitUntil(() -> clientMachine(ctx, terminal) != null && clientMachine(ctx, terminal).formed(),
                        SETTLE * 30, "the " + id + " to form on the client")
                .thenAssert(SETTLE, () -> !bones(machine, WORK).isEmpty(), "the " + id + "'s model has its work")
                .thenAssert(0, () -> shown(ctx, machine, terminal, WORK).isEmpty(),
                        "idle, the " + id + " shows none of its work")
                .thenAssert(0, () -> shown(ctx, machine, terminal, IDLE).size() == bones(machine, IDLE).size(),
                        "idle, the " + id + " shows all that only an idle one has")
                .thenServer(0, level -> {
                    for (int slot = 0; slot < inputs.size(); slot++) {
                        serverMachine(ctx, level, terminal).getInventory().insertItem(slot, inputs.get(slot).copy(),
                                false);
                    }
                })
                .thenWaitUntil(() -> clientMachine(ctx, terminal).status() == MachineStatus.WORKING, WAIT,
                        "the " + id + " to work")
                .thenAssert(0, () -> shown(ctx, machine, terminal, WORK).size() == bones(machine, WORK).size(),
                        "at work, the " + id + " shows all its work")
                .thenAssert(0, () -> shown(ctx, machine, terminal, IDLE).isEmpty(),
                        "at work, the " + id + " shows nothing only an idle one has");
    }

    /* The names of the bones of {@code machine}'s model marked {@code mark}. */
    private static List<String> bones(final ChainMachine machine, final String mark) {
        final ModelData model = KitModels.get(machine.modelId()).orElseThrow().data();
        final List<String> out = new ArrayList<>();
        model.bones().forEach(bone -> {
            if (marked(bone.name(), mark)) {
                out.add(bone.name());
            }
        });
        return out;
    }

    /* The bones marked {@code mark} the renderer leaves shown, posing the machine at its terminal as it stands now. */
    private static List<String> shown(final ClientTestContext ctx, final ChainMachine machine,
                                      final BlockPos terminal, final String mark) {
        final ModelPose pose = new ModelPose(KitModels.get(machine.modelId()).orElseThrow().data());
        ChainMachineRenderer.of(machine).move(clientMachine(ctx, terminal), pose, 0.0);
        final List<String> out = new ArrayList<>();
        for (int bone = 0; bone < pose.bones(); bone++) {
            final String name = pose.model().bones().get(bone).name();
            if (marked(name, mark) && !pose.hides(bone)) {
                out.add(name);
            }
        }
        return out;
    }

    private static boolean marked(final String name, final String mark) {
        return name.startsWith(mark) || name.startsWith(MODE) && name.contains("_" + mark);
    }

    /* Lays {@code machine}'s blocks from {@code corner} facing north, its Machine Terminal at its place. */
    private static void build(final Placer world, final ChainMachine machine, final BlockPos corner) {
        final List<List<String>> layers = machine.layers();
        for (int y = 0; y < layers.size(); y++) {
            for (int z = 0; z < layers.get(y).size(); z++) {
                final String row = layers.get(y).get(z);
                for (int x = 0; x < row.length(); x++) {
                    final char letter = row.charAt(x);
                    final Block block = letter == '#' ? ChainMachines.terminalOf(machine).get()
                            : ChainMachineBlocks.byId(machine.legend().get(letter)).get();
                    final BlockState state = block.defaultBlockState();
                    world.place(corner.offset(x, y, z), state.hasProperty(HorizontalDirectionalBlock.FACING)
                            ? state.setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH) : state);
                }
            }
        }
    }

    /* Where {@link #build} lays the Machine Terminal of {@code machine} built from {@code corner}. */
    private static BlockPos terminalOf(final ChainMachine machine, final BlockPos corner) {
        final List<List<String>> layers = machine.layers();
        for (int y = 0; y < layers.size(); y++) {
            for (int z = 0; z < layers.get(y).size(); z++) {
                final int x = layers.get(y).get(z).indexOf('#');
                if (x >= 0) {
                    return corner.offset(x, y, z);
                }
            }
        }
        throw new IllegalArgumentException(machine.id() + " has no terminal");
    }

    private static ChainMachineBlockEntity serverMachine(final ClientTestContext ctx, final ServerLevel level,
                                                         final BlockPos terminal) {
        return (ChainMachineBlockEntity) level.getBlockEntity(ctx.abs(terminal));
    }

    private static ChainMachineBlockEntity clientMachine(final ClientTestContext ctx, final BlockPos terminal) {
        return ctx.mc().level.getBlockEntity(ctx.abs(terminal)) instanceof ChainMachineBlockEntity found ? found
                : null;
    }

    /* Where the machine's blocks are set: the test's builder. */
    @FunctionalInterface
    private interface Placer {
        void place(BlockPos at, BlockState state);
    }
}
