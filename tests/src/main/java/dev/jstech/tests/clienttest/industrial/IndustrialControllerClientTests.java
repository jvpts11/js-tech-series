/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest.industrial;

import dev.jstech.computers.blockentity.PersonalComputerBlockEntity;
import dev.jstech.computers.hardware.GpuSpec;
import dev.jstech.core.cable.CableBlockEntity;
import dev.jstech.core.cable.CablePlugs;
import dev.jstech.core.cable.Cables;
import dev.jstech.core.multipart.ModelLayout;
import dev.jstech.core.multipart.PlacedModel;
import dev.jstech.industrial.IndustrialModule;
import dev.jstech.industrial.JsIndustrial;
import dev.jstech.industrial.integration.computers.FieldbusCardItem;
import dev.jstech.industrial.integration.computers.FieldbusCards;
import dev.jstech.industrial.integration.computers.IndustrialControlCable;
import dev.jstech.industrial.integration.computers.IndustrialControllerBlock;
import dev.jstech.industrial.integration.computers.IndustrialControllers;
import dev.jstech.tests.clienttest.ClientTest;
import dev.jstech.tests.clienttest.ClientTestContext;
import dev.jstech.tests.testkit.computers.EraKit;
import dev.jstech.tests.testkit.computers.EraParts;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

/**
 * The Industrial Controller Computers as a player sees them: one of each age in a row, each built with its era's
 * Fieldbus Card, and the industrial control cable out of its back to a Compressor, ending in the card's plug at the
 * controller and in an M12 at the machine.
 */
public final class IndustrialControllerClientTests {

    private static final BlockPos STAND = new BlockPos(0, 2, 0);
    /* The row of controllers, their fronts to the north; the cable south of each, and the machine at its end. */
    private static final int ROW_Z = 6;
    private static final int MACHINE_Z = ROW_Z + 3;
    private static final List<Built> ROW = List.of(
            new Built(-4, IndustrialControllers.AESTHETIC_INDUSTRIAL_CONTROLLER_COMPUTER, EraKit.STANDARD,
                    FieldbusCards.ETHERNET_IP, "fieldbus_rj45"),
            new Built(-2, IndustrialControllers.VINTAGE_INDUSTRIAL_CONTROLLER_COMPUTER, EraKit.VINTAGE,
                    FieldbusCards.RS485, "rs485"),
            new Built(0, IndustrialControllers.LEGACY_INDUSTRIAL_CONTROLLER_COMPUTER, EraKit.LEGACY,
                    FieldbusCards.PROFIBUS, "profibus"),
            new Built(2, IndustrialControllers.TRANSITION_INDUSTRIAL_CONTROLLER_COMPUTER, EraKit.TRANSITION,
                    FieldbusCards.PROFINET, "fieldbus_rj45"),
            new Built(4, IndustrialControllers.ADVANCED_HIGH_PERFORMANCE_INDUSTRIAL_CONTROLLER_COMPUTER,
                    EraKit.ADVANCED, FieldbusCards.ETHERCAT, "fieldbus_rj45"));

    private IndustrialControllerClientTests() {
    }

    @ClientTest(timeoutTicks = 800)
    public static void controllers_areDrawnWithTheirCardsAndTheCableEndsInTheirPlugs(final ClientTestContext ctx) {
        ctx.thenTeleport(0, STAND, Direction.SOUTH)
                .thenServer(0, level -> {
                    for (final Built built : ROW) {
                        build(ctx, level, built);
                    }
                })
                .thenWaitUntil(() -> {
                    for (final Built built : ROW) {
                        if (!plug(built.plug()).equals(drawnPlug(ctx, new BlockPos(built.x(), 2, ROW_Z + 1),
                                Direction.NORTH))
                                || !plug("m12").equals(drawnPlug(ctx, new BlockPos(built.x(), 2, ROW_Z + 2),
                                Direction.SOUTH))) {
                            return false;
                        }
                    }
                    return true;
                }, 200, "each cable drawn with its card's plug at the controller and an M12 at the machine",
                        () -> drawn(ctx))
                .then(0, () -> ctx.lookAt(new Vec3(ctx.abs(new BlockPos(0, 2, ROW_Z)).getX() + 0.5,
                        ctx.abs(STAND).getY() + 0.5, ctx.abs(new BlockPos(0, 2, ROW_Z)).getZ() + 0.5)))
                .thenScreenshot(10, "industrial-controllers-fronts")
                .thenTeleport(0, new BlockPos(7, 3, ROW_Z + 1), Direction.WEST)
                .then(0, () -> ctx.lookAt(Vec3.atCenterOf(ctx.abs(new BlockPos(2, 2, ROW_Z + 1)))))
                .thenScreenshot(10, "industrial-controllers-cables")
                // The side off, close: each card in the slot under the graphics card, its connectors at the back.
                .thenServer(0, level -> {
                    for (final Built built : ROW) {
                        if (level.getBlockEntity(ctx.abs(new BlockPos(built.x(), 2, ROW_Z)))
                                instanceof PersonalComputerBlockEntity computer) {
                            computer.toggleSidePanel();
                        }
                    }
                })
                .thenTeleport(0, new BlockPos(8, 2, ROW_Z), Direction.WEST)
                .then(0, () -> ctx.lookAt(inside(ctx, 4)))
                .thenScreenshot(10, "industrial-controller-inside")
                .thenServer(0, level -> {
                    for (final Built built : ROW) {
                        for (int z = ROW_Z; z <= MACHINE_Z; z++) {
                            level.removeBlock(ctx.abs(new BlockPos(built.x(), 2, z)), false);
                        }
                    }
                });
    }

    /* A controller built of its era's parts and its card, its cable out of its back and a Compressor at the end. */
    private static void build(final ClientTestContext ctx, final ServerLevel level, final Built built) {
        final BlockPos at = ctx.abs(new BlockPos(built.x(), 2, ROW_Z));
        level.setBlockAndUpdate(at, built.controller().get().defaultBlockState()
                .setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH));
        if (level.getBlockEntity(at) instanceof PersonalComputerBlockEntity computer) {
            final ItemStackHandler hardware = computer.getHardware();
            final Item board = built.kit().computerBoard();
            hardware.setStackInSlot(PersonalComputerBlockEntity.MOTHERBOARD_SLOT, new ItemStack(board));
            hardware.setStackInSlot(PersonalComputerBlockEntity.CPU_SLOT, new ItemStack(EraParts.cpuFor(board)));
            hardware.setStackInSlot(PersonalComputerBlockEntity.RAM_SLOTS_START,
                    new ItemStack(EraParts.ramFor(board)));
            hardware.setStackInSlot(PersonalComputerBlockEntity.PSU_SLOT,
                    new ItemStack(EraParts.psuFor(built.kit().era())));
            hardware.setStackInSlot(PersonalComputerBlockEntity.GPU_SLOTS_START,
                    new ItemStack(EraParts.cardFor(board, GpuSpec.class)));
            hardware.setStackInSlot(PersonalComputerBlockEntity.GPU_SLOTS_START + 1,
                    new ItemStack(built.card().get()));
        }
        Cables.lay(level, ctx.abs(new BlockPos(built.x(), 2, ROW_Z + 1)), IndustrialControlCable.CABLE.get());
        Cables.lay(level, ctx.abs(new BlockPos(built.x(), 2, ROW_Z + 2)), IndustrialControlCable.CABLE.get());
        level.setBlockAndUpdate(ctx.abs(new BlockPos(built.x(), 2, MACHINE_Z)),
                IndustrialModule.COMPRESSOR.get().defaultBlockState());
    }

    /* The plug the player's game draws where the cable at {@code relative} meets {@code face}, or null. */
    private static @Nullable ResourceLocation drawnPlug(final ClientTestContext ctx, final BlockPos relative,
                                                        final Direction face) {
        if (ctx.mc().level == null
                || !(ctx.mc().level.getBlockEntity(ctx.abs(relative)) instanceof CableBlockEntity cable)) {
            return null;
        }
        final ModelLayout layout = CablePlugs.placed(cable.getModelData(), ctx.mc().level, cable.getBlockPos())
                .get(ModelLayout.PROPERTY);
        if (layout == null) {
            return null;
        }
        for (final PlacedModel placed : layout.models()) {
            if (placed.facing() == face) {
                return placed.model();
            }
        }
        return null;
    }

    /* The middle of the controller at {@code x}, a little low, where its cards hang. */
    private static Vec3 inside(final ClientTestContext ctx, final int x) {
        final BlockPos at = ctx.abs(new BlockPos(x, 2, ROW_Z));
        return new Vec3(at.getX() + 0.5, at.getY() + 0.45, at.getZ() + 0.5);
    }

    /* What each cable is drawn ending in, at the controller and at the machine, for a failure to say. */
    private static String drawn(final ClientTestContext ctx) {
        final StringBuilder said = new StringBuilder();
        for (final Built built : ROW) {
            said.append(built.x()).append(": ")
                    .append(drawnPlug(ctx, new BlockPos(built.x(), 2, ROW_Z + 1), Direction.NORTH)).append(" / ")
                    .append(drawnPlug(ctx, new BlockPos(built.x(), 2, ROW_Z + 2), Direction.SOUTH)).append("; ");
        }
        return said.toString();
    }

    private static ResourceLocation plug(final String name) {
        return ResourceLocation.fromNamespaceAndPath(JsIndustrial.MODID, "block/cable/plug/" + name);
    }

    /* A controller of the row: where it stands, its block, its era's parts, its card and the plug the card takes. */
    private record Built(int x, Supplier<IndustrialControllerBlock> controller, EraKit kit,
                         Supplier<FieldbusCardItem> card, String plug) {
    }
}
