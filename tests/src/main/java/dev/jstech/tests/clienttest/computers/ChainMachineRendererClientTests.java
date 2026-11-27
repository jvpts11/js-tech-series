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
import dev.jstech.core.machine.MachineStatus;
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
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * What a chain machine's renderer shows of its model: the work going through it only while it works, so an idle
 * Crusher stands empty, with no ore falling into it nor lying on its bars.
 */
public final class ChainMachineRendererClientTests {

    private static final int SETTLE = 4;
    private static final int WAIT = 400;
    private static final BlockPos CORNER = new BlockPos(2, 2, 4);
    private static final String WORK = "work_";
    private static final int FERRITES = 8;

    private ChainMachineRendererClientTests() {
    }

    @ClientTest(timeoutTicks = 1200)
    public static void crusher_standsEmptyIdleAndShowsItsOreAtWork(final ClientTestContext ctx) {
        final ChainMachine machine = ChainMachines.byId("crusher").orElseThrow();
        final BlockPos terminal = terminalOf(machine);
        ctx.thenBuild(0, world -> build(world::setBlock, machine))
                .thenWaitUntilServer(level -> crusher(ctx, level, terminal).formed(), SETTLE * 30,
                        "the Crusher to form", level -> "it stands unformed")
                .thenWaitUntil(() -> clientCrusher(ctx, terminal) != null && clientCrusher(ctx, terminal).formed(),
                        SETTLE * 30, "the Crusher to form on the client")
                .thenAssert(SETTLE, () -> !workBones(machine).isEmpty(), "the Crusher's model has its ore as work")
                .thenAssert(0, () -> workShown(ctx, machine, terminal).isEmpty(),
                        "idle, the Crusher shows no ore falling nor lying on its bars")
                .thenServer(0, level -> crusher(ctx, level, terminal).getInventory()
                        .insertItem(0, new ItemStack(ChainParts.FERRITE.get(), FERRITES), false))
                .thenWaitUntil(() -> clientCrusher(ctx, terminal).status() == MachineStatus.WORKING, WAIT,
                        "the Crusher to work")
                .thenAssert(0, () -> workShown(ctx, machine, terminal).size() == workBones(machine).size(),
                        "at work, the Crusher shows all its ore");
    }

    /* The names of the bones of {@code machine}'s model that are the work going through it. */
    private static List<String> workBones(final ChainMachine machine) {
        final ModelData model = KitModels.get(machine.modelId()).orElseThrow().data();
        final List<String> out = new ArrayList<>();
        model.bones().forEach(bone -> {
            if (bone.name().startsWith(WORK)) {
                out.add(bone.name());
            }
        });
        return out;
    }

    /* The work bones the renderer leaves shown, posing the machine at its terminal as it stands now. */
    private static List<String> workShown(final ClientTestContext ctx, final ChainMachine machine,
                                          final BlockPos terminal) {
        final ModelPose pose = new ModelPose(KitModels.get(machine.modelId()).orElseThrow().data());
        ChainMachineRenderer.of(machine).move(clientCrusher(ctx, terminal), pose, 0.0);
        final List<String> out = new ArrayList<>();
        for (int bone = 0; bone < pose.bones(); bone++) {
            final String name = pose.model().bones().get(bone).name();
            if (name.startsWith(WORK) && !pose.hides(bone)) {
                out.add(name);
            }
        }
        return out;
    }

    /* Lays {@code machine}'s blocks from {@link #CORNER} facing north, its Machine Terminal at its place. */
    private static void build(final Placer world, final ChainMachine machine) {
        final List<List<String>> layers = machine.layers();
        for (int y = 0; y < layers.size(); y++) {
            for (int z = 0; z < layers.get(y).size(); z++) {
                final String row = layers.get(y).get(z);
                for (int x = 0; x < row.length(); x++) {
                    final char letter = row.charAt(x);
                    final Block block = letter == '#' ? ChainMachines.terminalOf(machine).get()
                            : ChainMachineBlocks.byId(machine.legend().get(letter)).get();
                    final BlockState state = block.defaultBlockState();
                    world.place(CORNER.offset(x, y, z), state.hasProperty(HorizontalDirectionalBlock.FACING)
                            ? state.setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH) : state);
                }
            }
        }
    }

    /* Where {@link #build} lays the Machine Terminal of {@code machine}. */
    private static BlockPos terminalOf(final ChainMachine machine) {
        final List<List<String>> layers = machine.layers();
        for (int y = 0; y < layers.size(); y++) {
            for (int z = 0; z < layers.get(y).size(); z++) {
                final int x = layers.get(y).get(z).indexOf('#');
                if (x >= 0) {
                    return CORNER.offset(x, y, z);
                }
            }
        }
        throw new IllegalArgumentException(machine.id() + " has no terminal");
    }

    private static ChainMachineBlockEntity crusher(final ClientTestContext ctx, final ServerLevel level,
                                                   final BlockPos terminal) {
        return (ChainMachineBlockEntity) level.getBlockEntity(ctx.abs(terminal));
    }

    private static ChainMachineBlockEntity clientCrusher(final ClientTestContext ctx, final BlockPos terminal) {
        return ctx.mc().level.getBlockEntity(ctx.abs(terminal)) instanceof ChainMachineBlockEntity found ? found
                : null;
    }

    /* Where the machine's blocks are set: the test's builder. */
    @FunctionalInterface
    private interface Placer {
        void place(BlockPos at, BlockState state);
    }
}
