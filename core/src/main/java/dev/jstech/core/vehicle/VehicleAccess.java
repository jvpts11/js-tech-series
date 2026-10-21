/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.vehicle;

import dev.jstech.core.team.Ownership;
import java.util.Optional;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Who may do what with a vehicle: its owner and whom the owner lets use it drive it ({@link Ownership}), and so does
 * whoever holds a key cut for it, when its kind has keys; anyone gets in while it is unlocked, but a driving seat is
 * only for those who may drive; and only whom its owner trusts to break things breaks it. A vehicle nobody owns is
 * anyone's.
 */
public final class VehicleAccess {

    private VehicleAccess() {
    }

    /** Whether {@code entity} may drive {@code vehicle}. */
    public static boolean mayDrive(final VehicleEntity vehicle, final Entity entity) {
        if (!(entity instanceof ServerPlayer player)) {
            // A robot drives as its task says, and a pig drives nothing; a player's own game is told by the server
            // whom it seats.
            return entity instanceof IDriver || entity instanceof Player && vehicle.level().isClientSide();
        }
        final Optional<Ownership> owner = vehicle.ownership();
        return owner.isEmpty() || owner.get().mayUse(player) || holdsKey(vehicle, player);
    }

    /** Whether {@code player} may get in: the doors unlocked, or they may drive it. */
    public static boolean mayBoard(final VehicleEntity vehicle, final Player player) {
        return !vehicle.switches().locked() || mayDrive(vehicle, player);
    }

    /** Whether {@code player} may break it back into its item. */
    public static boolean mayBreak(final VehicleEntity vehicle, final ServerPlayer player) {
        return vehicle.ownership().map(owner -> owner.mayBreak(player)).orElse(true);
    }

    /** Whether {@code player} carries a key cut for it, of its keys' generation. */
    public static boolean holdsKey(final VehicleEntity vehicle, final Player player) {
        for (final ItemStack stack : player.getInventory().items) {
            if (opens(stack, vehicle)) {
                return true;
            }
        }
        return opens(player.getOffhandItem(), vehicle);
    }

    /** Whether {@code stack} is a key cut for {@code vehicle}, of its keys' generation. */
    public static boolean opens(final ItemStack stack, final VehicleEntity vehicle) {
        final VehicleKey key = stack.get(Vehicles.KEY.get());
        return key != null && key.vehicle().equals(vehicle.keyId()) && key.generation() == vehicle.keyGeneration();
    }

    /**
     * The seat {@code passenger} would sit in getting in: a driving seat for one who may drive, else the first free
     * seat of another role; -1 when none it may sit in is free.
     */
    public static int freeSeat(final VehicleEntity vehicle, final VehicleDef def, final Entity passenger) {
        final boolean drives = mayDrive(vehicle, passenger);
        int other = -1;
        for (int seat = 0; seat < def.seats().size(); seat++) {
            if (taken(vehicle, seat, passenger)) {
                continue;
            }
            final boolean driving = VehicleRoles.of(def.seats().get(seat).role()).drives();
            if (driving && drives) {
                return seat;
            }
            if (!driving && other < 0) {
                other = seat;
            }
        }
        return other;
    }

    /** Whether {@code passenger} may sit in {@code seat}: a driving seat only if it may drive. */
    public static boolean maySit(final VehicleEntity vehicle, final VehicleDef def, final Entity passenger,
                                 final int seat) {
        return !VehicleRoles.of(def.seats().get(seat).role()).drives() || mayDrive(vehicle, passenger);
    }

    /* Whether someone besides {@code passenger} sits in {@code seat}. */
    private static boolean taken(final VehicleEntity vehicle, final int seat, final Entity passenger) {
        for (final Entity riding : vehicle.getPassengers()) {
            if (riding != passenger && vehicle.seatOf(riding) == seat) {
                return true;
            }
        }
        return false;
    }
}
