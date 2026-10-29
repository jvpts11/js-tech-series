/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.life;

import dev.jstech.core.air.AirMix;
import dev.jstech.core.material.Material;
import dev.jstech.core.material.Materials;
import dev.jstech.core.matter.IMatterHandler;
import dev.jstech.core.matter.MatterCapabilities;
import dev.jstech.core.room.RoomLiving;
import dev.jstech.core.room.RoomVent;
import dev.jstech.core.room.SealedRooms;
import dev.jstech.core.text.GameText;
import java.util.List;
import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

/**
 * An oxygen bottle, half an hour of breathing when full. Used, it pours its air into the tanks of the suit worn, or
 * of a suit clicked with it in the inventory; where none takes it, in a sealed room, it fills from the room's air,
 * taking the oxygen out of it. Used on a block that offers oxygen, a gas tank or a line, it fills from that.
 */
public final class OxygenBottleItem extends Item {

    /** How many seconds of breathing a full bottle holds: half an hour. */
    public static final int FULL = 1_800;
    /* The oxygen a second of breathing takes, in moles, as J's Core's lungs take it; the bar's length and colour. */
    private static final double MOLES_A_SECOND = RoomLiving.PLAYER_BREATH;
    private static final int BAR = 13;
    private static final int BAR_BLUE = 0x4C8DE0;
    private static final int SECONDS_A_MINUTE = 60;

    public OxygenBottleItem(final Properties properties) {
        super(properties.stacksTo(1));
    }

    /** The seconds of breathing {@code bottle} holds. */
    public static int air(final ItemStack bottle) {
        return bottle.getOrDefault(SuitData.BOTTLE_AIR.get(), 0);
    }

    /** Sets the seconds of breathing {@code bottle} holds, never past a full bottle's. */
    public static void setAir(final ItemStack bottle, final int seconds) {
        bottle.set(SuitData.BOTTLE_AIR.get(), Math.max(0, Math.min(FULL, seconds)));
    }

    /** Pours what {@code bottle} holds into the tanks of the suit whose chest piece {@code chest} is; how much went. */
    public static int pour(final Level level, final ItemStack bottle, final ItemStack chest) {
        final int poured = SuitAir.refill(level, chest, air(bottle));
        if (poured > 0) {
            setAir(bottle, air(bottle) - poured);
        }
        return poured;
    }

    /**
     * Fills {@code bottle} from the air of the sealed room {@code pos} stands in, taking the oxygen out of it; how many
     * seconds of breathing went in. Nothing where no room is.
     */
    public static int fillFromRoom(final ServerLevel level, final ItemStack bottle, final BlockPos pos) {
        if (SealedRooms.roomAt(level, pos).isEmpty()) {
            return 0;
        }
        final double moles = SealedRooms.take(level, pos, AirMix.OXYGEN, (FULL - air(bottle)) * MOLES_A_SECOND);
        final int seconds = (int) Math.floor(moles / MOLES_A_SECOND + 1e-9);
        setAir(bottle, air(bottle) + seconds);
        return seconds;
    }

    /** Fills {@code bottle} from the oxygen {@code gas} holds; how many seconds of breathing went in. */
    public static int fillFromGas(final ItemStack bottle, final IMatterHandler gas) {
        final Optional<Material> oxygen = Materials.byId(AirMix.OXYGEN);
        if (oxygen.isEmpty()) {
            return 0;
        }
        final long wanted = (long) Math.ceil((FULL - air(bottle)) * MOLES_A_SECOND * RoomVent.MILLIBUCKETS_PER_MOLE);
        final long taken = gas.extract(oxygen.get(), wanted, false);
        final int seconds = (int) Math.min(FULL - air(bottle),
                Math.floor(taken / RoomVent.MILLIBUCKETS_PER_MOLE / MOLES_A_SECOND + 1e-9));
        setAir(bottle, air(bottle) + seconds);
        return seconds;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(final Level level, final Player player, final InteractionHand hand) {
        final ItemStack bottle = player.getItemInHand(hand);
        if (!(level instanceof ServerLevel server)) {
            return InteractionResultHolder.pass(bottle);
        }
        final BlockPos eyes = BlockPos.containing(player.getEyePosition());
        if (pour(level, bottle, player.getItemBySlot(EquipmentSlot.CHEST)) > 0
                || air(bottle) < FULL && fillFromRoom(server, bottle, eyes) > 0) {
            player.playSound(SoundEvents.BUCKET_FILL);
            return InteractionResultHolder.success(bottle);
        }
        return InteractionResultHolder.pass(bottle);
    }

    @Override
    public InteractionResult useOn(final UseOnContext context) {
        final Level level = context.getLevel();
        final IMatterHandler gas = level.getCapability(MatterCapabilities.GAS, context.getClickedPos(),
                context.getClickedFace());
        if (gas == null || air(context.getItemInHand()) >= FULL) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        return fillFromGas(context.getItemInHand(), gas) > 0 ? InteractionResult.SUCCESS : InteractionResult.PASS;
    }

    @Override
    public boolean overrideStackedOnOther(final ItemStack bottle, final Slot slot, final ClickAction action,
                                          final Player player) {
        if (action != ClickAction.SECONDARY || !slot.hasItem()) {
            return false;
        }
        if (pour(player.level(), bottle, slot.getItem()) > 0) {
            slot.setChanged();
            player.playSound(SoundEvents.BUCKET_EMPTY);
            return true;
        }
        return false;
    }

    @Override
    public boolean isBarVisible(final ItemStack bottle) {
        return true;
    }

    @Override
    public int getBarWidth(final ItemStack bottle) {
        return Math.round((float) BAR * air(bottle) / FULL);
    }

    @Override
    public int getBarColor(final ItemStack bottle) {
        return BAR_BLUE;
    }

    @Override
    public void appendHoverText(final ItemStack bottle, final TooltipContext context, final List<Component> lines,
                                final TooltipFlag flag) {
        final int seconds = air(bottle);
        lines.add(GameText.component(SuitSupplies.AIR_IN_THE_BOTTLE.with(String.format("%d:%02d",
                seconds / SECONDS_A_MINUTE, seconds % SECONDS_A_MINUTE))).withStyle(ChatFormatting.GRAY));
    }
}
