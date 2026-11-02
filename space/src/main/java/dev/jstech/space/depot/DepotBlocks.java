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
import dev.jstech.core.multiblock.BlockMatch;
import dev.jstech.core.multiblock.MultiblockPattern;
import dev.jstech.core.multiblock.MultiblockPatterns;
import dev.jstech.space.JsSpace;
import dev.jstech.space.SpaceContent;
import java.util.List;
import java.util.function.Function;
import net.minecraft.resources.ResourceLocation;
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
    /**
     * The depot as one multiblock, at the approved one's size, which the Structure Projector shows and the Multiblock
     * Builder builds; a depot of any other size the pump takes is built by hand.
     */
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(JsSpace.MODID, "propellant_depot");
    public static final MultiblockPattern PATTERN = MultiblockPatterns.declare(ID, pattern());
    /* The size it is shown at, as the approved depot stands: its sphere nine blocks wide, on legs two blocks tall. */
    private static final int SHOWN_EDGE = DepotLayout.LARGEST;
    private static final int SHOWN_LEGS = 2;

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

    /* The depot shown, layer by layer from the ground: the pump's column first, west of the cube's. */
    private static MultiblockPattern pattern() {
        final MultiblockPattern.Builder builder = MultiblockPattern.builder(ID.getPath());
        for (int y = 0; y <= SHOWN_LEGS + SHOWN_EDGE; y++) {
            final String[] rows = new String[SHOWN_EDGE];
            for (int z = 0; z < SHOWN_EDGE; z++) {
                final StringBuilder row = new StringBuilder(SHOWN_EDGE + 1);
                for (int x = 0; x <= SHOWN_EDGE; x++) {
                    row.append(letter(x, y, z));
                }
                rows[z] = row.toString();
            }
            builder.layer(rows);
        }
        return builder.where('S', BlockMatch.blocks(SPHERE_SHELL.getId().toString()))
                .where('L', BlockMatch.blocks(TANK_LEG.getId().toString()))
                .where('V', BlockMatch.blocks(VENT_STACK.getId().toString()))
                .terminal(TRANSFER_PUMP.getId().toString())
                .build();
    }

    /*
     * What stands at ({@code x}, {@code y}, {@code z}) of the depot shown: the Transfer Pump, its terminal, on the
     * ground against the north-west leg; a leg under each bottom corner of the cube; the cube's outside of shell,
     * hollow, as only its outside counts; the vent stack on the middle of its top.
     */
    private static char letter(final int x, final int y, final int z) {
        if (x == 0) {
            return y == 0 && z == 0 ? MultiblockPattern.TERMINAL_CHAR : MultiblockPattern.IGNORE_CHAR;
        }
        final int far = SHOWN_EDGE - 1;
        final int across = x - 1;
        if (y < SHOWN_LEGS) {
            return (across == 0 || across == far) && (z == 0 || z == far) ? 'L' : MultiblockPattern.IGNORE_CHAR;
        }
        final int up = y - SHOWN_LEGS;
        if (up == SHOWN_EDGE) {
            return across == SHOWN_EDGE / 2 && z == SHOWN_EDGE / 2 ? 'V' : MultiblockPattern.IGNORE_CHAR;
        }
        final boolean outside = across == 0 || up == 0 || z == 0 || across == far || up == far || z == far;
        return outside ? 'S' : MultiblockPattern.IGNORE_CHAR;
    }
}
