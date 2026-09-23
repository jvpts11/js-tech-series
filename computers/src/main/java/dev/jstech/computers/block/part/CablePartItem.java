/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.block.part;

import dev.jstech.computers.advancement.JscEvents;
import dev.jstech.computers.block.DataCableBlock;
import dev.jstech.computers.blockentity.DataCableBlockEntity;
import dev.jstech.core.network.DataTier;
import dev.jstech.core.network.NetworkSystem;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import dev.jstech.core.uuid.NetworkUuid;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SoundType;
import org.jetbrains.annotations.Nullable;

/**
 * A part item: right-clicking a data cable attaches an {@link ICablePart} of this item's type to one of the cable's faces, like an AE2 bus snapping onto a cable.
 */
@TextHolder
public class CablePartItem extends Item {

    private final CablePartType type;

    private static final TextKey IMPORT_TOOLTIP = TextKey.of("item.jsc.import_bus.tooltip",
            "Right-click a data cable to attach; pulls items into the network");
    private static final TextKey EXPORT_TOOLTIP = TextKey.of("item.jsc.export_bus.tooltip",
            "Right-click a data cable to attach; pushes the filtered item out");
    private static final TextKey INPUT_TOOLTIP = TextKey.of("item.jsc.input_bus.tooltip",
            "Right-click a crafting cable to attach; marks the face machine crafts deliver inputs through");
    private static final TextKey RECEIVING_TOOLTIP = TextKey.of("item.jsc.receiving_bus.tooltip",
            "Right-click a crafting cable to attach; marks the face machine crafts collect outputs from");
    private static final TextKey ON_CRAFTING_CABLES = TextKey.of("item.jsc.bus.on_crafting_cables",
            "Crafting buses mount on crafting cables");
    private static final TextKey ON_DATA_CABLES = TextKey.of("item.jsc.bus.on_data_cables",
            "Storage buses mount on data cables");

    public CablePartItem(final Properties properties, final CablePartType type) {
        super(properties);
        this.type = type;
    }

    @Override
    public void appendHoverText(final ItemStack stack, final TooltipContext context,
                                final List<Component> tooltip, final TooltipFlag flag) {
        final TextKey what = switch (type) {
            case IMPORT -> IMPORT_TOOLTIP;
            case EXPORT -> EXPORT_TOOLTIP;
            case INPUT -> INPUT_TOOLTIP;
            case RECEIVING -> RECEIVING_TOOLTIP;
        };
        tooltip.add(GameText.component(what).withStyle(ChatFormatting.GRAY));
    }

    @Override
    public InteractionResult useOn(final UseOnContext context) {
        final Level level = context.getLevel();
        final BlockPos clicked = context.getClickedPos();

        /*
         * Clicked a data cable directly: mount on it (clicked face, snapping to an adjacent
         * inventory when there is exactly one).
         */
        if (level.getBlockState(clicked).getBlock() instanceof DataCableBlock
                && level.getBlockEntity(clicked) instanceof DataCableBlockEntity cable) {
            return place(context, cable, chooseFace(cable, context.getClickedFace()));
        }

        /*
         * Clicked another block (e.g. a chest): mount on an adjacent cable so the bus faces that
         * block. Prefer the cable behind the clicked face, then any other adjacent cable.
         */
        final Direction behind = context.getClickedFace().getOpposite();
        final InteractionResult viaBehind = tryAdjacentCable(context, clicked, behind);
        if (viaBehind != InteractionResult.PASS) {
            return viaBehind;
        }
        for (final Direction direction : Direction.values()) {
            if (direction != behind) {
                final InteractionResult result = tryAdjacentCable(context, clicked, direction);
                if (result != InteractionResult.PASS) {
                    return result;
                }
            }
        }
        return InteractionResult.PASS;
    }

    private InteractionResult tryAdjacentCable(final UseOnContext context, final BlockPos clicked,
                                               final Direction toCable) {
        final Level level = context.getLevel();
        final BlockPos cablePos = clicked.relative(toCable);
        if (level.getBlockState(cablePos).getBlock() instanceof DataCableBlock
                && level.getBlockEntity(cablePos) instanceof DataCableBlockEntity cable
                && !cable.hasPart(toCable.getOpposite())) {
            // The cable's face pointing back at the clicked block is toCable's opposite.
            return place(context, cable, toCable.getOpposite());
        }
        return InteractionResult.PASS;
    }

    private InteractionResult place(final UseOnContext context, final DataCableBlockEntity cable,
                                    @Nullable final Direction face) {
        if (face == null) {
            return InteractionResult.PASS; // every candidate face is taken
        }
        final Level level = context.getLevel();
        /*
         * Crafting buses belong on crafting cables and storage buses on data cables. A storage bus on a
         * crafting cable would autonomously move items the crafting engine is accounting for (and vice versa
         * the crafting buses are inert), so a mismatched mount is refused with a hint instead.
         */
        final boolean craftingPart = type == CablePartType.INPUT || type == CablePartType.RECEIVING;
        final boolean craftingCable =
                cable.tier() == DataTier.CRAFTING;
        if (craftingPart != craftingCable) {
            if (!level.isClientSide() && context.getPlayer() != null) {
                context.getPlayer().displayClientMessage(
                        GameText.component(craftingPart ? ON_CRAFTING_CABLES : ON_DATA_CABLES), true);
            }
            return InteractionResult.FAIL;
        }
        if (!level.isClientSide()) {
            cable.addPart(face, type.create());
            if (level instanceof ServerLevel server && context.getPlayer() != null
                    && (type == CablePartType.IMPORT || type == CablePartType.EXPORT)) {
                reportPair(server, cable, context.getPlayer());
            }
            level.playSound(null, cable.getBlockPos(), SoundType.METAL.getPlaceSound(),
                    SoundSource.BLOCKS, 1.0F, 0.8F);
            if (context.getPlayer() != null && !context.getPlayer().getAbilities().instabuild) {
                context.getItemInHand().shrink(1);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    @Nullable
    private static Direction chooseFace(final DataCableBlockEntity cable, final Direction clicked) {
        if (!cable.hasPart(clicked) && !cable.neighborPort(clicked).isEmpty()) {
            return clicked;
        }
        Direction firstFree = null;
        Direction dataFace = null;
        int dataCount = 0;
        for (final Direction direction : Direction.values()) {
            if (cable.hasPart(direction)) {
                continue;
            }
            if (firstFree == null) {
                firstFree = direction;
            }
            /*
             * Any face touching a block that offers data (items, fluids, or chemicals) is a candidate, so a
             * fluid- or chemical-only machine face (e.g. a chemical tank side) snaps the bus the same way an
             * inventory does, now that buses carry every kind of data.
             */
            if (!cable.neighborPort(direction).isEmpty()) {
                dataFace = direction;
                dataCount++;
            }
        }
        if (dataCount == 1) {
            return dataFace;
        }
        if (!cable.hasPart(clicked)) {
            return clicked;
        }
        return firstFree;
    }

    /* An Import Bus and an Export Bus on one network: items now come in and go out on their own. */
    private void reportPair(final ServerLevel level, final DataCableBlockEntity placedOn, final Player player) {
        final CablePartType other = type == CablePartType.IMPORT ? CablePartType.EXPORT : CablePartType.IMPORT;
        final NetworkSystem system = NetworkSystem.get(level);
        final NetworkUuid network = system.connectivity().networkOf(placedOn.getBlockPos().asLong()).orElse(null);
        if (network == null) {
            return;
        }
        for (final long encoded : system.connectivity().positionsOf(network)) {
            if (level.getBlockEntity(BlockPos.of(encoded)) instanceof DataCableBlockEntity cable) {
                for (final Direction face : Direction.values()) {
                    final ICablePart part = cable.getPart(face);
                    if (part != null && part.type() == other) {
                        JscEvents.award(player, JscEvents.BUSES_PAIRED);
                        return;
                    }
                }
            }
        }
    }
}
