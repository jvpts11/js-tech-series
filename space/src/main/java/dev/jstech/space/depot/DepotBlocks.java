/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.depot;

import dev.jstech.core.content.BlockBuilder;
import dev.jstech.core.content.BlockEntry;
import dev.jstech.core.content.ContentTab;
import dev.jstech.core.content.Drops;
import dev.jstech.core.content.IBlockLook;
import dev.jstech.core.content.IBlockModel;
import dev.jstech.core.matter.MatterCapabilities;
import dev.jstech.space.SpaceContent;
import java.util.List;
import java.util.function.Function;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * The propellant depot's kit of J's Space: the welded Sphere Shell, the Tank Leg under it, the Vent Stack on its top
 * and the Transfer Pump that feeds the pad; each the kit's face until the depot is whole, which its pump then draws
 * whole as the approved one.
 */
public final class DepotBlocks {

    /** Its row of the mod's tab. */
    public static final ContentTab.Section DEPOT = SpaceContent.TAB.section();
    public static final BlockEntry<DepotPieceBlock> SPHERE_SHELL = piece("sphere_shell", DepotPieceBlock::new, true)
            .named("Sphere Shell").register();
    public static final BlockEntry<DepotPieceBlock> TANK_LEG = piece("tank_leg", DepotPieceBlock::new, true)
            .named("Tank Leg").register();
    public static final BlockEntry<DepotPieceBlock> VENT_STACK = piece("vent_stack", DepotPieceBlock::new, true)
            .named("Vent Stack").register();
    public static final BlockEntry<TransferPumpBlock> TRANSFER_PUMP = piece("transfer_pump", TransferPumpBlock::new,
            false).named("Transfer Pump").register();
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TransferPumpBlockEntity>> PUMP_ENTITY =
            SpaceContent.CONTENT.blockEntity("transfer_pump", TransferPumpBlockEntity::new, TRANSFER_PUMP);

    private DepotBlocks() {
    }

    /**
     * Declares the blocks, once, from the mod's constructor, before its content registers; a pump of liquid oxygen
     * gives its oxygen as gas, as a bottle held to it takes it.
     */
    public static void declare(final IEventBus modBus) {
        modBus.addListener(RegisterCapabilitiesEvent.class, event -> event.registerBlockEntity(MatterCapabilities.GAS,
                PUMP_ENTITY.get(), (pump, side) -> pump.gas()));
    }

    /** Every block of the kit, in its order. */
    public static List<BlockEntry<? extends Block>> all() {
        return List.of(SPHERE_SHELL, TANK_LEG, VENT_STACK, TRANSFER_PUMP);
    }

    /* A block of the kit: its face on all six sides, and nothing of its own once formed where it {@code forms}. */
    private static <B extends Block> BlockBuilder<B> piece(final String id,
                                                           final Function<BlockBehaviour.Properties, B> made,
                                                           final boolean forms) {
        final IBlockModel face = new IBlockModel.CubeAll(id, "block/depot/" + id);
        final IBlockModel drawn = new IBlockModel.ParticleOnly(id + "_drawn", "block/depot/" + id);
        final IBlockLook look = forms ? IBlockLook.byState(state -> state.getValue(DepotPieceBlock.FORMED) ? drawn
                : face) : IBlockLook.fixed(face);
        return SpaceContent.CONTENT.block(id, made).properties(properties -> properties.mapColor(MapColor.SNOW)
                        .strength(3.0F, 6.0F).requiresCorrectToolForDrops().sound(SoundType.METAL))
                .look(look).item().drops(Drops.SELF).tag(BlockTags.MINEABLE_WITH_PICKAXE).tab(DEPOT);
    }
}
