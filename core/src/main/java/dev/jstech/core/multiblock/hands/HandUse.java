/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.multiblock.hands;

import dev.jstech.core.blockentity.ClientUpdates;
import dev.jstech.core.blockentity.FieldItemHandler;
import dev.jstech.core.machine.MachineBlockEntity;
import dev.jstech.core.multiblock.Orientation;
import dev.jstech.core.network.payload.ClientPayloads;
import dev.jstech.core.network.payload.IPayloadGate;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import org.jetbrains.annotations.ApiStatus;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * A player working a part of a formed structure's model by hand, on the server: allowed only to whoever may open the
 * structure's screen and is no spectator, and only when their own line of sight, as the server has it, meets the part.
 * A door opens or shuts; a place takes one of the thing in hand that fits it, or gives what it holds to an empty
 * hand; a control does what it does.
 */
public final class HandUse {

    private static final String NETWORK_VERSION = "1";
    /* How far from the terminal a click is heard at all, in blocks: past the far end of a long structure. */
    private static final double HEARD = 32;
    /* How much further than their reach the line of sight is followed, and how far round a part it may pass. */
    private static final double FURTHER = 1;
    private static final float SLACK = 4;

    private HandUse() {
    }

    /** Hands the mod's bus what this answers, once, from the Core's constructor. */
    @ApiStatus.Internal
    public static void listen(final IEventBus modBus) {
        modBus.addListener(RegisterPayloadHandlersEvent.class, event -> ClientPayloads.accept(
                event.registrar(NETWORK_VERSION), HandUsePayload.TYPE, HandUsePayload.STREAM_CODEC,
                IPayloadGate.near(HandUsePayload::terminal, HEARD),
                (payload, player, level) -> use(player, level, payload.terminal(), payload.part())));
    }

    /**
     * Works the part {@code part} of the formed structure whose terminal stands at {@code terminal}, for
     * {@code player}, when they may open its screen and their line of sight meets the part; whether it was worked.
     */
    public static boolean use(final ServerPlayer player, final ServerLevel level, final BlockPos terminal,
                              final int part) {
        if (player.isSpectator() || !(level.getBlockEntity(terminal) instanceof MachineBlockEntity machine)
                || !machine.mayOpen(player)) {
            return false;
        }
        final Optional<StructureHands> held = machine.hands();
        final Optional<Orientation> standing = machine.formedOrientation();
        if (held.isEmpty() || standing.isEmpty() || !machine.merged()) {
            return false;
        }
        final StructureHands hands = held.get();
        final HandGeometry geometry = hands.model().geometry();
        if (part < 0 || part >= geometry.parts().size()) {
            return false;
        }
        final float[] open = hands.openness(level.getGameTime());
        final Matrix4f back = HandGeometry.placed(standing.get(),
                (float) hands.model().model().definition().detail().scale(), new Matrix4f()).invert();
        final Vec3 eye = player.getEyePosition();
        final Vec3 end = eye.add(player.getViewVector(1.0F).scale(player.blockInteractionRange() + FURTHER));
        if (!geometry.meets(part, back.transformPosition(from(eye, terminal)),
                back.transformPosition(from(end, terminal)), open, SLACK)) {
            return false;
        }
        switch (geometry.parts().get(part)) {
            case IHandPart.Door door -> {
                final int number = geometry.doorNumber(door.bone());
                hands.setOpen(number, !hands.open(number), level.getGameTime());
            }
            case IHandPart.Place place -> {
                if (!take(player, machine.getInventory(), place.slot())) {
                    return false;
                }
            }
            case IHandPart.Control control -> hands.model().action(control.action()).press(machine, player);
        }
        ClientUpdates.sendNow(level, terminal);
        return true;
    }

    /* Where {@code at} lies from the lowest corner of the block {@code terminal}, taken apart before it is a float. */
    private static Vector3f from(final Vec3 at, final BlockPos terminal) {
        return new Vector3f((float) (at.x - terminal.getX()), (float) (at.y - terminal.getY()),
                (float) (at.z - terminal.getZ()));
    }

    /*
     * A place worked: one of the thing in hand set in its slot when that is empty and takes it, or what the slot
     * holds put in an empty hand; whether anything moved.
     */
    private static boolean take(final ServerPlayer player, final FieldItemHandler inventory, final int slot) {
        if (slot >= inventory.getSlots()) {
            return false;
        }
        final ItemStack hand = player.getMainHandItem();
        final ItemStack there = inventory.getStackInSlot(slot);
        if (hand.isEmpty()) {
            if (there.isEmpty()) {
                return false;
            }
            player.setItemInHand(InteractionHand.MAIN_HAND, inventory.extractItem(slot, there.getCount(), false));
            return true;
        }
        if (!there.isEmpty() || !inventory.isItemValid(slot, hand)) {
            return false;
        }
        if (!inventory.insertItem(slot, hand.copyWithCount(1), false).isEmpty()) {
            return false;
        }
        if (!player.getAbilities().instabuild) {
            hand.shrink(1);
        }
        return true;
    }
}
