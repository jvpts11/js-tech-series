/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.conveyor;

import com.mojang.serialization.MapCodec;
import dev.jstech.core.inventory.ItemFilter;
import dev.jstech.core.inventory.ItemFilters;
import dev.jstech.core.machine.MachineWrench;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.Text;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/**
 * A diverter, of a kind a mod declares ({@link ConveyorType}): a belt that turns the items its filter names aside, to
 * the side it diverts to, and lets every other go straight on. Used with an item, it adds that item to its filter,
 * whatever the item's damage and components; used empty-handed while sneaking, it forgets its filter; a wrench turns it
 * to divert to its other side. With nothing in its filter it diverts nothing. It stands alone, its own run.
 *
 * <pre>{@code
 * public static final BlockEntry<DiverterBlock> DIVERTER = CONTENT.block("diverter",
 *         properties -> new DiverterBlock(properties, MyTypes.BELT, () -> MyContent.CONVEYOR.get()))
 *         .named("Diverter").look(IBlockLook.facingByState(state -> new IBlockModel.Handmade(
 *                 "diverter_" + state.getValue(DiverterBlock.SIDE).getSerializedName()))).register();
 * }</pre>
 */
public class DiverterBlock extends BeltPieceBlock {

    private final MapCodec<DiverterBlock> codec;

    /** The side it turns the items its filter names to. */
    public static final EnumProperty<BeltSide> SIDE = EnumProperty.create("side", BeltSide.class);
    private static final String LIST = ", ";

    public DiverterBlock(final Properties properties, final ConveyorType type,
                         final Supplier<? extends BlockEntityType<? extends ConveyorBlockEntity>> entity) {
        super(properties, type, entity);
        this.codec = simpleCodec(made -> new DiverterBlock(made, type, entity));
        registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH)
                .setValue(SIDE, BeltSide.LEFT).setValue(END, true));
    }

    /** Whether {@code filter} turns {@code item} aside: a filter that names nothing turns nothing. */
    public static boolean diverts(final ItemFilter filter, final ItemStack item) {
        return !filter.rules().isEmpty() && ItemFilters.allows(filter, item);
    }

    /* Aside, for an item its filter names; straight on, for any other. */
    @Override
    public List<ConveyorOutlet> outlets(final BlockState state, final BlockPos pos, final ConveyorBlockEntity belt,
                                        final ItemStack item) {
        final Direction facing = state.getValue(FACING);
        final Direction way = diverts(belt.filter(), item) ? state.getValue(SIDE).of(facing) : facing;
        return List.of(new ConveyorOutlet(pos.relative(way), way.getOpposite()));
    }

    /* From behind at its start, from its other side and above at its middle; nothing where it gives. */
    @Override
    public double intakeAt(final BlockState state, @Nullable final Direction side) {
        final Direction facing = state.getValue(FACING);
        if (side == facing || side == state.getValue(SIDE).of(facing)) {
            return NOWHERE;
        }
        return side == facing.getOpposite() ? START : MIDDLE;
    }

    @Override
    public boolean joinsRuns() {
        return false;
    }

    @Override
    public boolean filters() {
        return true;
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return this.codec;
    }

    @Override
    protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, SIDE, END);
    }

    /* A wrench turns it to its other side; any other item joins its filter. */
    @Override
    protected ItemInteractionResult useItemOn(final ItemStack stack, final BlockState state, final Level level,
                                              final BlockPos pos, final Player player, final InteractionHand hand,
                                              final BlockHitResult hit) {
        if (stack.isEmpty() || !(level.getBlockEntity(pos) instanceof ConveyorBlockEntity belt)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (!level.isClientSide()) {
            if (MachineWrench.isWrench(stack)) {
                final BeltSide side = state.getValue(SIDE).other();
                level.setBlock(pos, state.setValue(SIDE, side), Block.UPDATE_ALL);
                player.displayClientMessage(GameText.component(side == BeltSide.LEFT ? ConveyorTexts.DIVERTS_LEFT
                        : ConveyorTexts.DIVERTS_RIGHT), true);
            } else {
                final ItemFilter filter = belt.filter();
                if (!diverts(filter, stack)) {
                    belt.setFilter(filter.withMode(ItemFilter.Mode.ONLY).with(new ItemFilter.Fuzzy(
                            BuiltInRegistries.ITEM.getKey(stack.getItem()).toString(), 0L)));
                }
                tell(player, belt.filter());
            }
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide());
    }

    /* Sneaking with an empty hand, it forgets its filter. */
    @Override
    protected InteractionResult useWithoutItem(final BlockState state, final Level level, final BlockPos pos,
                                               final Player player, final BlockHitResult hit) {
        if (!player.isShiftKeyDown() || !(level.getBlockEntity(pos) instanceof ConveyorBlockEntity belt)) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide()) {
            belt.setFilter(ItemFilter.EVERYTHING);
            tell(player, belt.filter());
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    /* Tells {@code player} what the filter turns aside, item by item, each named in the player's language. */
    private static void tell(final Player player, final ItemFilter filter) {
        if (filter.rules().isEmpty()) {
            player.displayClientMessage(GameText.component(ConveyorTexts.DIVERTS_NOTHING), true);
            return;
        }
        final List<Component> names = new ArrayList<>();
        for (final ItemFilter.Rule rule : filter.rules()) {
            if (rule instanceof ItemFilter.Fuzzy fuzzy) {
                final ResourceLocation id = ResourceLocation.tryParse(fuzzy.item());
                if (id != null) {
                    names.add(BuiltInRegistries.ITEM.get(id).getDescription());
                }
            }
        }
        final Text listed = GameText.of(ComponentUtils.formatList(names, Component.literal(LIST)));
        player.displayClientMessage(GameText.component(ConveyorTexts.DIVERTS.with(listed)), true);
    }
}
