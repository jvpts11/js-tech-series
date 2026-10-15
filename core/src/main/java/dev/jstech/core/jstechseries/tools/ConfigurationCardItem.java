/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.jstechseries.tools;

import dev.jstech.core.machine.MachineBlockEntity;
import dev.jstech.core.machine.MachineSetup;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

/**
 * The Configuration Card, the series' shared tool for setting up many machines at once, acting before the block it is
 * used on. Sneaking and used on a
 * machine, it copies its setup (its sides, what it pushes and pulls and through which filters, its redstone and its
 * comparator, its input locks and the recipe picked); used on another machine it pastes it, whole into one of the same
 * kind and what fits into a compatible one. Used on two blocks that are not machines, it pastes it into every machine
 * in the box between them. Sneaking and used anywhere else, it forgets what it holds.
 */
@TextHolder
public final class ConfigurationCardItem extends Item {

    public static final TextKey COPIED = TextKey.of("jscore.configuration_card.copied", "Copied the setup of %s");
    public static final TextKey PASTED = TextKey.of("jscore.configuration_card.pasted", "Pasted the setup of %s");
    public static final TextKey NOTHING_FITS = TextKey.of("jscore.configuration_card.nothing_fits",
            "Nothing of this setup fits this machine, or it is not yours to set up");
    public static final TextKey EMPTY = TextKey.of("jscore.configuration_card.empty",
            "The card holds no setup: sneak and use it on a machine to copy one");
    public static final TextKey CORNER = TextKey.of("jscore.configuration_card.corner",
            "First corner marked: use the card on the opposite corner to paste over the box");
    public static final TextKey PASTED_OVER = TextKey.of("jscore.configuration_card.pasted_over",
            "Pasted into %s machines");
    public static final TextKey TOO_BIG = TextKey.of("jscore.configuration_card.too_big",
            "That box is too big: at most %s blocks");
    public static final TextKey CLEARED = TextKey.of("jscore.configuration_card.cleared", "The card is blank again");
    public static final TextKey HOLDS = TextKey.of("jscore.configuration_card.holds", "Holds the setup of %s");
    public static final TextKey HOW = TextKey.of("jscore.configuration_card.how",
            "Sneak and use on a machine to copy; use on machines to paste, or on two corners to paste over a box");

    public ConfigurationCardItem(final Properties properties) {
        super(properties);
    }

    /*
     * The card acts before the block it is used on: a machine would open its screen, a chest marked as a corner would
     * open too.
     */
    @Override
    public InteractionResult onItemUseFirst(final ItemStack stack, final UseOnContext context) {
        final Player player = context.getPlayer();
        final Level level = context.getLevel();
        if (player == null) {
            return InteractionResult.PASS;
        }
        if (!(level instanceof ServerLevel server) || !(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.SUCCESS;
        }
        final ItemStack card = stack;
        final BlockPos pos = context.getClickedPos();
        if (level.getBlockEntity(pos) instanceof MachineBlockEntity machine) {
            onMachine(serverPlayer, card, machine);
        } else if (player.isShiftKeyDown()) {
            clear(card);
            say(player, CLEARED.text());
        } else {
            onCorner(server, serverPlayer, card, pos);
        }
        return InteractionResult.CONSUME;
    }

    /* Sneaking and used in the air, it forgets what it holds. */
    @Override
    public InteractionResultHolder<ItemStack> use(final Level level, final Player player, final InteractionHand hand) {
        final ItemStack card = player.getItemInHand(hand);
        if (!player.isShiftKeyDown()) {
            return InteractionResultHolder.pass(card);
        }
        if (!level.isClientSide()) {
            clear(card);
            say(player, CLEARED.text());
        }
        return InteractionResultHolder.sidedSuccess(card, level.isClientSide());
    }

    @Override
    public void appendHoverText(final ItemStack stack, final TooltipContext context, final List<Component> lines,
                                final TooltipFlag flag) {
        super.appendHoverText(stack, context, lines, flag);
        final MachineSetup.Copied held = stack.get(SharedTools.SETUP.get());
        if (held != null) {
            lines.add(GameText.component(HOLDS.with(machineName(held))));
        }
        lines.add(GameText.component(HOW));
    }

    /* Copies the machine's setup when sneaking, pastes the card's into it otherwise. */
    private static void onMachine(final ServerPlayer player, final ItemStack card, final MachineBlockEntity machine) {
        if (player.isShiftKeyDown()) {
            if (!machine.mayOpen(player)) {
                say(player, NOTHING_FITS.text());
                return;
            }
            final MachineSetup.Copied copied = MachineSetup.copy(machine);
            card.set(SharedTools.SETUP.get(), copied);
            card.remove(SharedTools.CORNER.get());
            say(player, COPIED.with(machineName(copied)));
            return;
        }
        final MachineSetup.Copied held = card.get(SharedTools.SETUP.get());
        if (held == null) {
            say(player, EMPTY.text());
            return;
        }
        say(player, MachineSetup.paste(player, held, machine) ? PASTED.with(machineName(held))
                : NOTHING_FITS.text());
    }

    /* The first corner of a box, or the second, which pastes into every machine between them. */
    private static void onCorner(final ServerLevel level, final ServerPlayer player, final ItemStack card,
                                 final BlockPos pos) {
        final MachineSetup.Copied held = card.get(SharedTools.SETUP.get());
        if (held == null) {
            say(player, EMPTY.text());
            return;
        }
        final BlockPos first = card.get(SharedTools.CORNER.get());
        if (first == null) {
            card.set(SharedTools.CORNER.get(), pos.immutable());
            say(player, CORNER.text());
            return;
        }
        card.remove(SharedTools.CORNER.get());
        final int pasted = MachineSetup.pasteOver(level, player, held, first, pos);
        say(player, pasted < 0 ? TOO_BIG.with(MachineSetup.AREA_MOST) : PASTED_OVER.with(pasted));
    }

    private static void clear(final ItemStack card) {
        card.remove(SharedTools.SETUP.get());
        card.remove(SharedTools.CORNER.get());
    }

    private static Text machineName(final MachineSetup.Copied copied) {
        return GameText.of(BuiltInRegistries.BLOCK.get(copied.machine()).getName());
    }

    private static void say(final Player player, final Text text) {
        player.displayClientMessage(GameText.component(text), true);
    }
}
