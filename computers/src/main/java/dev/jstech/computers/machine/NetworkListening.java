/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.machine;

import dev.jstech.computers.eventlog.NetworkEvent;
import dev.jstech.computers.operation.payload.OperationRecord;
import dev.jstech.computers.os.IOsHost;
import dev.jstech.computers.vm.program.SettledOperation;
import dev.jstech.core.util.ShortId;
import dev.jstech.core.uuid.NetworkUuid;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * The machines whose programs wait on their network: for an Operation to settle ({@code Operations.OnSettled}), or for
 * an entry of the network's event log ({@code Network.OnEvent}).
 *
 * <p>A machine is put here when a program of it starts waiting, or comes back from a save waiting, and is let go when
 * no program of it waits any more. What happens on a network goes to the machines here that are on that network now:
 * the network a machine is on is read when it happens, so a machine moved to another network hears that one. Nothing
 * is walked but the machines that wait, and only when something happens, which is not every tick.
 */
public final class NetworkListening {

    /** The machines that wait, by where they stand. */
    private static final Set<Place> LISTENING = new LinkedHashSet<>();
    /** Machines that began to wait before they stood in a world, as one being read from a save does. */
    private static final Set<BlockEntity> WAITING = new LinkedHashSet<>();

    private NetworkListening() {
    }

    /** Hands the game's bus what this answers, once, from the mod's constructor. */
    public static void listen() {
        NeoForge.EVENT_BUS.addListener(ServerTickEvent.Post.class, event -> placeWaiting());
        NeoForge.EVENT_BUS.addListener(ServerStoppedEvent.class, event -> {
            LISTENING.clear();
            WAITING.clear();
        });
    }

    /** Told by a machine that a program of it waits on its network. */
    public static void wants(final BlockEntity machine) {
        if (machine.getLevel() instanceof ServerLevel level) {
            LISTENING.add(new Place(level.dimension(), machine.getBlockPos().immutable()));
        } else if (machine.getLevel() == null) {
            WAITING.add(machine);
        }
    }

    /** Tells the programs waiting on {@code network} that one of its Operations settled. */
    public static void settled(final ServerLevel level, final NetworkUuid network, final OperationRecord record) {
        if (LISTENING.isEmpty() || network == null) {
            return;
        }
        final SettledOperation settled = new SettledOperation(record.hasId() ? ShortId.of(record.id().toString()) : "",
                OperationRecord.typeName(record.type()).toLowerCase(Locale.ROOT), record.name().getString(),
                record.moved(), record.requested(), OperationRecord.statusName(record.status()),
                record.priority().label().toLowerCase(Locale.ROOT));
        each(level.getServer(), network, programs -> programs.deliverSettled(settled));
    }

    /** Tells the programs waiting on {@code network} that an entry was written to its event log. */
    public static void logged(final ServerLevel level, final NetworkUuid network, final NetworkEvent event) {
        if (LISTENING.isEmpty() || network == null) {
            return;
        }
        final String said = event.message().english();
        final String grade = event.level().name().toLowerCase(Locale.ROOT);
        each(level.getServer(), network, programs -> programs.deliverNetworkEvent(grade, event.machineName(), said,
                event.time()));
    }

    /*
     * Hands what happened to the programs of every waiting machine on the network now, letting go of the machines that
     * are gone or whose programs no longer wait. A machine whose chunk is not loaded runs nothing, and is kept.
     */
    private static void each(final MinecraftServer server, final NetworkUuid network,
                             final Consumer<MachinePrograms> deliver) {
        if (server == null) {
            return;
        }
        for (final Place place : List.copyOf(LISTENING)) {
            final ServerLevel level = server.getLevel(place.dimension());
            if (level == null) {
                LISTENING.remove(place);
                continue;
            }
            if (!level.isLoaded(place.pos())) {
                continue;
            }
            final IOsHost host = level.getBlockEntity(place.pos()) instanceof IOsHost found ? found : null;
            final MachinePrograms programs = host == null ? null : host.programs();
            if (programs == null || !programs.listensToNetwork()) {
                LISTENING.remove(place);
                continue;
            }
            if (network.equals(host.networkUuid())) {
                deliver.accept(programs);
            }
        }
    }

    /* Puts here the machines that began to wait before they stood in a world, once they do. */
    private static void placeWaiting() {
        if (WAITING.isEmpty()) {
            return;
        }
        for (final BlockEntity machine : List.copyOf(WAITING)) {
            if (machine.isRemoved()) {
                WAITING.remove(machine);
            } else if (machine.getLevel() instanceof ServerLevel level) {
                WAITING.remove(machine);
                LISTENING.add(new Place(level.dimension(), machine.getBlockPos().immutable()));
            }
        }
    }

    /** Where a machine stands. */
    private record Place(ResourceKey<Level> dimension, BlockPos pos) {
    }
}
