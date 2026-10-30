/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.launcher;

import dev.jstech.core.content.BlockBuilder;
import dev.jstech.core.content.BlockEntry;
import dev.jstech.core.content.ContentTab;
import dev.jstech.core.content.Drops;
import dev.jstech.core.content.IBlockLook;
import dev.jstech.core.content.IBlockModel;
import dev.jstech.space.SpaceContent;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * The mobile launcher's kit of blocks, the faces of the approved kit: the deck's plates, the frame lining the exhaust
 * opening under the engines, the hold-down arms that keep the rocket down until its engines reach full power, the tail
 * service masts that carry the lines to its base, the red umbilical tower's frame, the swing arms out to its tanks and
 * its Umbilical Plate, the crew access arm to its hatch, and the elevator up the tower. A hangar lays them under the
 * rocket it builds; once a part of them is whole, each takes its own formed shape.
 */
public final class LauncherBlocks {

    /** Its row of the mod's tab. */
    public static final ContentTab.Section LAUNCHER = SpaceContent.TAB.section();
    /* The pieces drawn whole once formed: the deck's by its measuring hold-down arm, an arm's by its end. */
    private static final Set<LauncherPieceBlock.Kind> DRAWN = EnumSet.of(LauncherPieceBlock.Kind.DECK,
            LauncherPieceBlock.Kind.EXHAUST, LauncherPieceBlock.Kind.HOLD_DOWN, LauncherPieceBlock.Kind.MAST,
            LauncherPieceBlock.Kind.SWING_ARM, LauncherPieceBlock.Kind.CREW_ARM);
    public static final BlockEntry<Block> LAUNCHER_DECK = piece("launcher_deck", LauncherPieceBlock.Kind.DECK,
            Set.of()).named("Launcher Deck").register();
    public static final BlockEntry<Block> EXHAUST_FRAME = piece("exhaust_frame", LauncherPieceBlock.Kind.EXHAUST,
            Set.of()).named("Exhaust Frame").register();
    public static final BlockEntry<HoldDownArmBlock> HOLD_DOWN_ARM = piece("hold_down_arm",
            LauncherPieceBlock.Kind.HOLD_DOWN, Set.of(), HoldDownArmBlock::new).named("Hold-Down Arm").register();
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<LauncherDeckBlockEntity>>
            HOLD_DOWN_ENTITY = SpaceContent.CONTENT.blockEntity("hold_down_arm", LauncherDeckBlockEntity::new,
            HOLD_DOWN_ARM);
    public static final BlockEntry<Block> TAIL_SERVICE_MAST = piece("tail_service_mast", LauncherPieceBlock.Kind.MAST,
            Set.of(LauncherPieceBlock.Role.TOP)).named("Tail Service Mast").register();
    public static final BlockEntry<TowerFrameBlock> TOWER_FRAME = SpaceContent.CONTENT.block("tower_frame",
                    TowerFrameBlock::new).properties(properties -> metal(properties, LauncherPieceBlock.Kind.TOWER))
            .look(IBlockLook.facingByState(LauncherBlocks::towerModel)).item().drops(Drops.SELF)
            .tag(BlockTags.MINEABLE_WITH_PICKAXE).tab(LAUNCHER).named("Tower Frame").register();
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CraneBlockEntity>> CRANE_ENTITY =
            SpaceContent.CONTENT.blockEntity("crane", CraneBlockEntity::new, TOWER_FRAME);
    public static final BlockEntry<ArmBlock> SWING_ARM = piece("swing_arm", LauncherPieceBlock.Kind.SWING_ARM,
            Set.of(), properties -> new ArmBlock(properties, LauncherPieceBlock.Kind.SWING_ARM)).named("Swing Arm")
            .register();
    public static final BlockEntry<ArmBlock> CREW_ACCESS_ARM = piece("crew_access_arm",
            LauncherPieceBlock.Kind.CREW_ARM, Set.of(), properties -> new ArmBlock(properties,
                    LauncherPieceBlock.Kind.CREW_ARM)).named("Crew Access Arm").register();
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ArmBlockEntity>> ARM_ENTITY =
            SpaceContent.CONTENT.blockEntity("arm", ArmBlockEntity::new, SWING_ARM, CREW_ACCESS_ARM);
    public static final BlockEntry<Block> TOWER_ELEVATOR = piece("tower_elevator", LauncherPieceBlock.Kind.ELEVATOR,
            Set.of(LauncherPieceBlock.Role.TOP, LauncherPieceBlock.Role.END)).named("Tower Elevator")
            .tag(BlockTags.CLIMBABLE).register();
    /** A pedestal of the launch pad's kit, which the launcher stands on in its hangar as on the pad. */
    public static final BlockEntry<Block> PAD_PEDESTAL = piece("pad_pedestal", LauncherPieceBlock.Kind.PEDESTAL,
            Set.of(LauncherPieceBlock.Role.TOP, LauncherPieceBlock.Role.END)).named("Pad Pedestal").register();

    private LauncherBlocks() {
    }

    /** Declares the blocks, once, from the mod's constructor, before its content registers. */
    public static void declare() {
        // Loading the class declares them.
    }

    /** Every block of the kit, in its order. */
    public static List<BlockEntry<? extends Block>> all() {
        return List.of(LAUNCHER_DECK, EXHAUST_FRAME, HOLD_DOWN_ARM, TAIL_SERVICE_MAST, TOWER_FRAME, SWING_ARM,
                CREW_ACCESS_ARM, TOWER_ELEVATOR, PAD_PEDESTAL);
    }

    /*
     * A piece of the kit: the kit's face on all six sides, and once formed its own shape, a model for its body and
     * one for each of the roles in {@code shaped} it looks different in.
     */
    private static BlockBuilder<Block> piece(final String id, final LauncherPieceBlock.Kind kind,
                                             final Set<LauncherPieceBlock.Role> shaped) {
        return piece(id, kind, shaped, properties -> new LauncherPieceBlock(properties, kind));
    }

    /*
     * A piece made by {@code made}. The deck's own pieces, its plates, the opening's frame, the clamps and the masts,
     * and the arms' lengths show nothing of their own once formed: the deck and each arm are drawn whole, as the
     * approved launcher draws them.
     */
    private static <B extends Block> BlockBuilder<B> piece(final String id, final LauncherPieceBlock.Kind kind,
                                                           final Set<LauncherPieceBlock.Role> shaped,
                                                           final Function<BlockBehaviour.Properties, B> made) {
        final IBlockModel.CubeAll face = new IBlockModel.CubeAll(id, "block/launcher/" + id);
        final IBlockModel drawn = new IBlockModel.ParticleOnly(id + "_drawn", "block/launcher/" + id);
        final IBlockLook look = IBlockLook.facingByState(state -> {
            if (!state.getValue(LauncherPieceBlock.FORMED)) {
                return face;
            }
            if (DRAWN.contains(kind)) {
                return drawn;
            }
            final LauncherPieceBlock.Role role = state.getValue(LauncherPieceBlock.ROLE);
            return new IBlockModel.Handmade("launcher/" + id + "_" + (shaped.contains(role) ? role
                    .getSerializedName() : "formed"));
        });
        return SpaceContent.CONTENT.block(id, made).properties(properties -> metal(properties, kind)).look(look)
                .item().drops(Drops.SELF).tag(BlockTags.MINEABLE_WITH_PICKAXE).tab(LAUNCHER);
    }

    /*
     * How a tower frame looks: the kit's face; once formed its piece of the great truss by its bay; the crane's house
     * and boom on the tower's top show nothing of their own, the boom's tip drawing the crane whole.
     */
    private static IBlockModel towerModel(final BlockState state) {
        if (!state.getValue(LauncherPieceBlock.FORMED)) {
            return new IBlockModel.CubeAll("tower_frame", "block/launcher/tower_frame");
        }
        return switch (state.getValue(LauncherPieceBlock.ROLE)) {
            case TOP, END -> new IBlockModel.ParticleOnly("tower_frame_drawn", "block/launcher/tower_frame");
            case BODY -> new IBlockModel.Handmade("launcher/tower_frame_bay_" + state.getValue(TowerFrameBlock.BAY));
        };
    }

    /* Metal, as hard as iron; the tower red. */
    private static BlockBehaviour.Properties metal(final BlockBehaviour.Properties properties,
                                                   final LauncherPieceBlock.Kind kind) {
        return properties.mapColor(kind == LauncherPieceBlock.Kind.TOWER ? MapColor.COLOR_RED : MapColor.METAL)
                .strength(3.0F, 6.0F).requiresCorrectToolForDrops().sound(SoundType.METAL);
    }
}
