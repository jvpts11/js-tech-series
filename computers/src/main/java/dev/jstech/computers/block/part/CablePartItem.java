/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.block.part;

import dev.jstech.computers.advancement.JscEvents;
import dev.jstech.computers.block.DataWires;
import dev.jstech.computers.storage.ExternalDataPort;
import dev.jstech.core.cable.CableBlockEntity;
import dev.jstech.core.cable.Cables;
import dev.jstech.core.cable.Wire;
import dev.jstech.core.multipart.IFacePart;
import dev.jstech.core.multipart.PartType;
import dev.jstech.core.network.DataLine;
import dev.jstech.core.network.DataLink;
import dev.jstech.core.network.NetworkSystem;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import dev.jstech.core.tier.HardwareEra;
import dev.jstech.core.uuid.NetworkUuid;
import java.util.List;
import java.util.function.Supplier;
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
 * A part item: right-clicking a data cable attaches an {@link IFacePart} of this item's type to one of the cable's
 * faces, like an AE2 bus snapping onto a cable.
 */
@TextHolder
public class CablePartItem extends Item {

    private final Supplier<? extends PartType<?>> type;

    private static final TextKey IMPORT_TOOLTIP = TextKey.of("item.jsc.import_bus.tooltip",
            "Right-click a data cable to attach; pulls items into the network");
    private static final TextKey EXPORT_TOOLTIP = TextKey.of("item.jsc.export_bus.tooltip",
            "Right-click a data cable to attach; pushes the filtered item out");
    private static final TextKey EXTERNAL_TOOLTIP = TextKey.of("item.jsc.external_storage_bus.tooltip",
            "Right-click a data cable beside an inventory; the network uses the inventory as its storage");
    private static final TextKey ROUTER_TOOLTIP = TextKey.of("item.jsc.crafting_input_router.tooltip",
            "On the cable that leaves a Crafting Interface, against one input face of a machine with several");
    private static final TextKey RECEIVING_TOOLTIP = TextKey.of("item.jsc.receiving_bus.tooltip",
            "On the crafting cable, against a machine's output; takes back what its interface's jobs made");
    private static final TextKey INTERFACE_TOOLTIP = TextKey.of("item.jsc.crafting_interface.tooltip",
            "On the crafting cable; holds .craft files and feeds the machine it sits against, or its own cable");
    private static final TextKey INTERFACE_HOLDS = TextKey.of("item.jsc.crafting_interface.holds",
            "Holds %s patterns");
    private static final TextKey ON_CRAFTING_CABLES = TextKey.of("item.jsc.bus.on_crafting_cables",
            "Crafting parts mount on crafting cables");
    private static final TextKey ON_DATA_CABLES = TextKey.of("item.jsc.bus.on_data_cables",
            "Storage buses mount on access and backbone cables of their era or an earlier one");

    public CablePartItem(final Properties properties, final Supplier<? extends PartType<?>> type) {
        super(properties);
        this.type = type;
    }

    /** The kind of part this item mounts. */
    public PartType<?> partType() {
        return type.get();
    }

    @Override
    public void appendHoverText(final ItemStack stack, final TooltipContext context,
                                final List<Component> tooltip, final TooltipFlag flag) {
        final PartType<?> kind = type.get();
        final TextKey what;
        if (ComputingParts.isImport(kind)) {
            what = IMPORT_TOOLTIP;
        } else if (ComputingParts.isExport(kind)) {
            what = EXPORT_TOOLTIP;
        } else if (ComputingParts.isExternal(kind)) {
            what = EXTERNAL_TOOLTIP;
        } else if (kind == ComputingParts.ROUTER.get()) {
            what = ROUTER_TOOLTIP;
        } else if (ComputingParts.isInterface(kind)) {
            what = INTERFACE_TOOLTIP;
        } else {
            what = RECEIVING_TOOLTIP;
        }
        tooltip.add(GameText.component(what).withStyle(ChatFormatting.GRAY));
        if (ComputingParts.isInterface(kind) && kind.create() instanceof CraftingInterfacePart part) {
            tooltip.add(GameText.component(INTERFACE_HOLDS.with(part.capacity())).withStyle(ChatFormatting.GRAY));
        }
    }

    @Override
    public InteractionResult useOn(final UseOnContext context) {
        final Level level = context.getLevel();
        final BlockPos clicked = context.getClickedPos();

        /*
         * Clicked a cable directly: mount on it (clicked face, snapping to an adjacent
         * inventory when there is exactly one).
         */
        if (level.getBlockEntity(clicked) instanceof CableBlockEntity cable) {
            return place(context, cable, chooseFace(level, cable, context.getClickedFace()));
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
        if (level.getBlockEntity(cablePos) instanceof CableBlockEntity cable && free(cable, toCable.getOpposite())) {
            // The cable's face pointing back at the clicked block is toCable's opposite.
            return place(context, cable, toCable.getOpposite());
        }
        return InteractionResult.PASS;
    }

    private InteractionResult place(final UseOnContext context, final CableBlockEntity cable,
                                    @Nullable final Direction face) {
        if (face == null) {
            return InteractionResult.PASS; // every candidate face is taken
        }
        if (cable.hasPart(face) || !cable.wiresThrough(face).isEmpty() && !cutsOwnCable(cable, face)) {
            return InteractionResult.PASS;
        }
        final Level level = context.getLevel();
        /*
         * Crafting buses belong on crafting cables and storage buses on data cables. A storage bus on a
         * crafting cable would autonomously move items the crafting engine is accounting for (and vice versa
         * the crafting buses are inert), so a mismatched mount is refused with a hint instead. A block that
         * holds both takes both.
         */
        final boolean craftingPart = ComputingParts.isCrafting(type.get());
        final IFacePart made = type.get().create();
        final HardwareEra era = made instanceof AbstractBusPart bus ? bus.era() : HardwareEra.STANDARD;
        final boolean fits = craftingPart ? DataWires.holds(cable, DataWires::isCrafting)
                : DataWires.holds(cable, wire -> takes(era, wire));
        if (!fits) {
            if (!level.isClientSide() && context.getPlayer() != null) {
                context.getPlayer().displayClientMessage(
                        GameText.component(craftingPart ? ON_CRAFTING_CABLES : ON_DATA_CABLES), true);
            }
            return InteractionResult.FAIL;
        }
        if (!level.isClientSide()) {
            cable.addPart(face, type.get().create());
            final boolean mover = ComputingParts.isImport(type.get()) || ComputingParts.isExport(type.get());
            if (level instanceof ServerLevel server && context.getPlayer() != null && mover) {
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

    /*
     * Whether a Crafting Interface goes on {@code face} where only crafting wire crosses it: cutting that wire is what
     * an interface is for there, since the crafting cable beyond it becomes its own, apart from the network.
     */
    private boolean cutsOwnCable(final CableBlockEntity cable, final Direction face) {
        if (!ComputingParts.isInterface(type.get())) {
            return false;
        }
        for (final Wire wire : cable.wiresThrough(face)) {
            if (!DataWires.isCrafting(wire)) {
                return false;
            }
        }
        return true;
    }

    @Nullable
    private Direction chooseFace(final Level level, final CableBlockEntity cable, final Direction clicked) {
        if (ComputingParts.isInterface(type.get()) && !cable.hasPart(clicked) && cutsOwnCable(cable, clicked)
                && !cable.wiresThrough(clicked).isEmpty()) {
            return clicked;
        }
        if (free(cable, clicked) && !port(level, cable, clicked).isEmpty()) {
            return clicked;
        }
        Direction firstFree = null;
        Direction dataFace = null;
        int dataCount = 0;
        for (final Direction direction : Direction.values()) {
            if (!free(cable, direction)) {
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
            if (!port(level, cable, direction).isEmpty()) {
                dataFace = direction;
                dataCount++;
            }
        }
        if (dataCount == 1) {
            return dataFace;
        }
        if (free(cable, clicked)) {
            return clicked;
        }
        return firstFree;
    }

    /* Whether a storage bus of {@code era} mounts on {@code wire}: an access or backbone cable of its era or before. */
    private static boolean takes(final HardwareEra era, final Wire wire) {
        final DataLink link = DataWires.linkOf(wire);
        return link != null && (link.line() == DataLine.ACCESS || link.line() == DataLine.BACKBONE)
                && link.era().level() <= era.level();
    }

    /* A face a bus can go on: no part there, and no wire crossing it, which a bus would cut. */
    private static boolean free(final CableBlockEntity cable, final Direction face) {
        return !cable.hasPart(face) && cable.wiresThrough(face).isEmpty();
    }

    /* What the block beyond {@code face} holds, as a bus there would reach it; nothing on a player's game. */
    private static ExternalDataPort port(final Level level, final CableBlockEntity cable, final Direction face) {
        if (!(level instanceof ServerLevel server)) {
            return new ExternalDataPort(null, null);
        }
        return ExternalDataPort.at(server, cable.getBlockPos().relative(face), face.getOpposite());
    }

    /* An Import Bus and an Export Bus on one network: items now come in and go out on their own. */
    private void reportPair(final ServerLevel level, final CableBlockEntity placedOn, final Player player) {
        final boolean importing = ComputingParts.isImport(type.get());
        final NetworkSystem system = NetworkSystem.get(level);
        final NetworkUuid network = DataWires.networkOf(placedOn);
        if (network == null) {
            return;
        }
        for (final BlockPos pos : Cables.blocksOf(level, system.connectivity().positionsOf(network))) {
            if (level.getBlockEntity(pos) instanceof CableBlockEntity cable) {
                for (final Direction face : Direction.values()) {
                    final IFacePart part = cable.getPart(face);
                    if (part != null && (importing ? ComputingParts.isExport(part.type())
                            : ComputingParts.isImport(part.type()))) {
                        JscEvents.award(player, JscEvents.BUSES_PAIRED);
                        return;
                    }
                }
            }
        }
    }
}
