/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.operation.payload.update;

import dev.jstech.computers.JsComputers;
import dev.jstech.computers.blockentity.MainframeBlockEntity;
import dev.jstech.computers.blockentity.PersonalComputerBlockEntity;
import dev.jstech.computers.client.os.UpdatePopup;
import dev.jstech.computers.engine.EngineVerb;
import dev.jstech.computers.operation.MoveLabels;
import dev.jstech.computers.operation.NetworkStorage;
import dev.jstech.computers.operation.NetworkUpdateOperation;
import dev.jstech.computers.operation.index.ItemLocation;
import dev.jstech.computers.operation.payload.ClientPayloadHandlers;
import dev.jstech.computers.operation.payload.ComputerAccess;
import dev.jstech.computers.operation.payload.network.NetworkLookup;
import dev.jstech.computers.operation.payload.operations.OperationsPayloads;
import dev.jstech.computers.operation.payload.terminal.TerminalHosts;
import dev.jstech.computers.operation.payload.workshop.WorkshopStatePayload;
import dev.jstech.computers.storage.StorageKey;
import dev.jstech.computers.workshop.UpdateAction;
import dev.jstech.computers.workshop.UpdateDoor;
import dev.jstech.computers.workshop.UpdateRequest;
import dev.jstech.computers.workshop.Workshop;
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import dev.jstech.core.uuid.NetworkUuid;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import org.jetbrains.annotations.Nullable;

/**
 * The Update window's server side: what it shows for an item, read from the network and the asking computer's
 * cards, and the UPDATE it sends, through the same door and the same checks as the network's language.
 */
@TextHolder
@EventBusSubscriber(modid = JsComputers.MODID)
public final class UpdatePayloads {

    public static final TextKey QUEUED = TextKey.of("jsc.update.queued", "UPDATE queued: %s %s");
    public static final TextKey NOT_STARTED = TextKey.of("jsc.update.not_started", "the network did not start it");
    public static final TextKey NOTHING_HELD = TextKey.of("jsc.update.nothing_held", "the network holds no %s");

    private UpdatePayloads() {
    }

    @SubscribeEvent
    public static void register(final RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar("1");
        ComputerAccess.accept(registrar, UpdatePreviewRequestPayload.TYPE, UpdatePreviewRequestPayload.STREAM_CODEC,
                ComputerAccess.machineAt(UpdatePreviewRequestPayload::hostPos, UpdatePreviewRequestPayload::monitorPos),
                UpdatePayloads::handlePreview);
        ComputerAccess.accept(registrar, UpdateSubmitPayload.TYPE, UpdateSubmitPayload.STREAM_CODEC,
                ComputerAccess.machineAt(UpdateSubmitPayload::hostPos, UpdateSubmitPayload::monitorPos),
                UpdatePayloads::handleSubmit);
        registrar.playToClient(UpdatePreviewPayload.TYPE, UpdatePreviewPayload.STREAM_CODEC,
                ClientPayloadHandlers.onMainThread((payload, player) -> UpdatePopup.accept(payload)));
    }

    /**
     * What the window shows for {@code key} on {@code computer}'s network, with {@code name} in its name field, for
     * {@code player}, with {@code status} as the last ask's answer.
     */
    public static UpdatePreviewPayload preview(final ServerPlayer player, final ServerLevel level,
                                               @Nullable final PersonalComputerBlockEntity computer,
                                               final StorageKey key, final String name, final Text status,
                                               final boolean sent) {
        final NetworkUuid net = computer == null ? null : computer.networkUuid();
        final MainframeBlockEntity mainframe = net == null ? null : NetworkLookup.resolveMainframe(level, net);
        final Map<StorageKey, Long> stock = net == null ? Map.of() : NetworkStorage.of(level, net).query();
        final ItemStack one = key.stack(1);
        int accepts = 0;
        for (final UpdateAction action : UpdateAction.values()) {
            if (UpdateDoor.accepts(level, key, action)) {
                accepts |= 1 << action.id();
            }
        }
        final List<WorkshopStatePayload.Offer> offers = new ArrayList<>();
        for (final Workshop.Offer offer : Workshop.offersFor(player, one)) {
            offers.add(new WorkshopStatePayload.Offer(offer.required(), offer.clue(), offer.levels()));
        }
        final StorageKey wornKey = mostWorn(stock, key);
        final ItemStack worn = wornKey.stack(1);
        final StorageKey materialKey = UpdateDoor.material(stock, worn);
        final int used = UpdateDoor.materialNeeded(worn);
        final String given = name.isEmpty() ? worn.getHoverName().getString() : name;
        final Workshop.AnvilResult anvil = materialKey == null ? Workshop.anvilFor(player, worn, ItemStack.EMPTY, given)
                : Workshop.anvilFor(player, worn, materialKey.stack(used), given);
        final Workshop.Smelt smelt = Workshop.smelt(level, one);
        int workshopDone = 0;
        int workshopTotal = 0;
        boolean networkBusy = false;
        int furnaceSpeed = 0;
        if (computer != null) {
            final Workshop workshop = computer.workshop();
            if (!workshop.slot(Workshop.FURNACE_IN).isEmpty()) {
                workshopDone = workshop.slot(Workshop.FURNACE_OUT).getCount();
                workshopTotal = workshopDone + workshop.slot(Workshop.FURNACE_IN).getCount();
            }
            networkBusy = computer.updateDrawer().smelting();
            furnaceSpeed = computer.furnaceSpeed();
        }
        return new UpdatePreviewPayload(one, computer == null ? 0 : computer.workshopCards(), accepts,
                computer == null ? "" : computer.hostname(), stock.getOrDefault(key, 0L),
                mainframe == null ? List.of() : holders(level, mainframe, key), offers, player.experienceLevel, worn,
                materialKey == null ? ItemStack.EMPTY : materialKey.stack(1),
                materialKey == null ? 0L : stock.getOrDefault(materialKey, 0L), used, anvil.result(),
                anvil.anvilLevels(), anvil.levels(), given, smelt == null ? ItemStack.EMPTY : smelt.result(),
                furnaceSpeed, workshopDone, workshopTotal, networkBusy, status, sent);
    }

    private static void handlePreview(final UpdatePreviewRequestPayload payload, final ServerPlayer player,
                                      final ServerLevel level) {
        PacketDistributor.sendToPlayer(player, preview(player, level, computer(player, level, payload.hostPos(),
                payload.monitorPos()), StorageKey.of(payload.item()), payload.name(), Text.EMPTY, false));
    }

    private static void handleSubmit(final UpdateSubmitPayload payload, final ServerPlayer player,
                                     final ServerLevel level) {
        final PersonalComputerBlockEntity computer = computer(player, level, payload.hostPos(), payload.monitorPos());
        final StorageKey key = StorageKey.of(payload.item());
        final Submitted outcome = submit(player, level, computer, key, payload);
        final Text refused = outcome.refused();
        if (refused == null && computer != null && computer.networkUuid() != null) {
            OperationsPayloads.dispatchTerminalOpsLog(player, computer.networkUuid(), level);
            OperationsPayloads.dispatchActiveOperations(player, computer.networkUuid(), level);
        }
        PacketDistributor.sendToPlayer(player, preview(player, level, computer, key, payload.name(),
                refused == null ? QUEUED.with(outcome.quantity(), key.displayName().getString())
                        : refused, refused == null));
    }

    /* What a submit came to: why it was refused, or null with the quantity that was really queued. */
    private record Submitted(@Nullable Text refused, long quantity) {

        static Submitted refusedWith(final Text why) {
            return new Submitted(why, 0L);
        }
    }

    /* Sends the UPDATE the window set up; why not, or null when it went, with the quantity that was queued. */
    private static Submitted submit(final ServerPlayer player, final ServerLevel level,
                               @Nullable final PersonalComputerBlockEntity computer, final StorageKey key,
                               final UpdateSubmitPayload payload) {
        final UpdateAction action = UpdateAction.byId(payload.action()).orElse(null);
        if (action == null) {
            return Submitted.refusedWith(NOT_STARTED.text());
        }
        final Text refused = UpdateDoor.refusal(level, computer, key, action, player);
        if (refused != null || computer == null || computer.networkUuid() == null) {
            return Submitted.refusedWith(refused != null ? refused : UpdateDoor.PC_ONLY.text());
        }
        final MainframeBlockEntity mainframe = NetworkLookup.resolveMainframe(level, computer.networkUuid());
        if (mainframe == null) {
            return Submitted.refusedWith(NOT_STARTED.text());
        }
        final Text unavailable = mainframe.networkOperations().refusal(EngineVerb.UPDATE);
        if (unavailable != null) {
            return Submitted.refusedWith(unavailable);
        }
        final Map<StorageKey, Long> stock = NetworkStorage.of(level, computer.networkUuid()).query();
        final long held = stock.getOrDefault(key, 0L);
        if (held <= 0L) {
            return Submitted.refusedWith(NOTHING_HELD.with(key.displayName().getString()));
        }
        StorageKey with = null;
        if (action == UpdateAction.REPAIR) {
            with = UpdateDoor.material(stock, key.stack(1));
            if (with == null) {
                return Submitted.refusedWith(UpdateDoor.NO_MATERIAL.with(key.displayName().getString()));
            }
        }
        final long quantity = Math.max(1L, Math.min(held, payload.quantity()));
        final NetworkUpdateOperation operation = mainframe.networkOperations().update(new UpdateRequest(
                computer.getBlockPos(), key, quantity, action, payload.offer(), payload.name(), with, null,
                player.getUUID(), computer.originLabel(MoveLabels.TERMINAL)));
        return operation == null ? Submitted.refusedWith(NOT_STARTED.text()) : new Submitted(null, quantity);
    }

    /* The most worn of {@code key}'s kind the network holds, which a repair takes; the key itself with none worn. */
    private static StorageKey mostWorn(final Map<StorageKey, Long> stock, final StorageKey key) {
        StorageKey worn = key;
        int most = key.stack(1).getDamageValue();
        for (final Map.Entry<StorageKey, Long> entry : stock.entrySet()) {
            if (entry.getKey().item() == key.item() && entry.getValue() > 0L) {
                final int damage = entry.getKey().stack(1).getDamageValue();
                if (damage > most) {
                    worn = entry.getKey();
                    most = damage;
                }
            }
        }
        return worn;
    }

    /* The servers that hold {@code key}, the one with the most first, by the names the network gives them. */
    private static List<String> holders(final ServerLevel level, final MainframeBlockEntity mainframe,
                                        final StorageKey key) {
        final List<ItemLocation> places = new ArrayList<>(mainframe.networkIndex().locations(key));
        places.sort(Comparator.comparingLong(ItemLocation::quantity).reversed());
        final List<String> names = new ArrayList<>();
        for (final ItemLocation place : places) {
            final String label = NetworkLookup.serverLabel(level, place.server());
            if (!names.contains(label)) {
                names.add(label);
            }
        }
        return names;
    }

    /* The Personal Computer at {@code hostPos}, if the player is at one of its monitors; null for anything else. */
    @Nullable
    private static PersonalComputerBlockEntity computer(final ServerPlayer player, final ServerLevel level,
                                                        final BlockPos hostPos, final BlockPos monitorPos) {
        return TerminalHosts.niHost(player, level, hostPos, monitorPos) instanceof PersonalComputerBlockEntity pc
                ? pc : null;
    }
}
