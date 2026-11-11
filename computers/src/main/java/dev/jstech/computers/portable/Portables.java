/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.portable;

import dev.jstech.computers.blockentity.AccessPointBlockEntity;
import dev.jstech.computers.machine.MirrorService;
import dev.jstech.computers.operation.payload.MessengerActionPayload;
import dev.jstech.computers.operation.payload.portable.PortableActionPayload;
import dev.jstech.computers.operation.payload.portable.PortableViewPayload;
import dev.jstech.computers.registry.ComputingComponents;
import dev.jstech.computers.storage.StorageKey;
import dev.jstech.computers.wireless.AccessPoints;
import dev.jstech.computers.wireless.WirelessLink;
import dev.jstech.computers.wireless.WirelessStandard;
import dev.jstech.core.text.Text;
import dev.jstech.core.util.ShortId;
import dev.jstech.core.uuid.NetworkUuid;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.function.Consumer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

/**
 * What a portable device does while it is carried (computers/docs/design/wireless.md), on the server: it takes its
 * id and its state the first time, draws its battery while it is on, joins the network of the nearest access point
 * that reaches its player once a second, at the speed of the older end, and tells its player's game what it keeps
 * whenever that changes. A device whose battery goes flat switches off; it comes back on out of its dock.
 */
public final class Portables {

    /** A carried device on draws its battery this often, that many ticks' worth at once, so its bar moves in steps. */
    public static final int DRAW_EVERY = 100;
    /** A carried device on looks for its access point, and says what it keeps, this often. */
    public static final int LOOK_EVERY = 20;

    /* What each player's game was last told of each device it carries: its state's version and the Mirror's reach. */
    private static final Map<ServerPlayer, Map<UUID, Told>> TOLD = new WeakHashMap<>();
    private static volatile Consumer<InteractionHand> opener = hand -> {
    };

    private Portables() {
    }

    /** Holds up the device in {@code hand}, on the player's game; the game says how, as it starts. */
    public static void open(final InteractionHand hand) {
        opener.accept(hand);
    }

    /** Says what holds a device up: the player's game does, once, as it starts. */
    public static void useOpener(final Consumer<InteractionHand> what) {
        opener = Objects.requireNonNull(what, "what");
    }

    /** The id of the device in {@code stack}, or null for one never carried. */
    @Nullable
    public static UUID id(final ItemStack stack) {
        return stack.get(ComputingComponents.PORTABLE_ID.get());
    }

    /** What its battery holds, in millijoules: a device never carried is full. */
    public static int battery(final ItemStack stack, final PortableModel model) {
        final Integer stored = stack.get(ComputingComponents.PORTABLE_BATTERY.get());
        return stored == null ? PortableBattery.capacity(model) : stored;
    }

    /** Whether the device is on: switched on, its battery not flat. */
    public static boolean isOn(final ItemStack stack) {
        return stack.has(ComputingComponents.PORTABLE_ON_SINCE.get());
    }

    /** The game time the device was switched on, or -1 while it is off. */
    public static long onSince(final ItemStack stack) {
        final Long since = stack.get(ComputingComponents.PORTABLE_ON_SINCE.get());
        return since == null ? -1L : since;
    }

    /** Switches the device on at {@code now}, which plays its start, when its battery has anything in it. */
    public static void switchOn(final ItemStack stack, final PortableModel model, final long now) {
        if (battery(stack, model) > 0) {
            stack.set(ComputingComponents.PORTABLE_ON_SINCE.get(), now);
        }
    }

    /** Switches the device off: it leaves its network and its link goes. */
    public static void switchOff(final ServerLevel level, final ItemStack stack) {
        stack.remove(ComputingComponents.PORTABLE_ON_SINCE.get());
        stack.remove(ComputingComponents.PORTABLE_LINK.get());
        final UUID id = id(stack);
        if (id != null) {
            PortableLinks.left(level, id);
        }
    }

    /** Sets what the device's battery holds, in millijoules, from none to full. */
    public static void setBattery(final ItemStack stack, final PortableModel model, final int millijoules) {
        stack.set(ComputingComponents.PORTABLE_BATTERY.get(),
                Math.max(0, Math.min(PortableBattery.capacity(model), millijoules)));
    }

    /**
     * The device in {@code stack}, of {@code model}, carried by {@code player} this tick: given its id and its state
     * the first time; switched on when it is carried off with charge in it (new, or out of its dock); then drawn,
     * joined and its player told as often as each is due.
     */
    public static void carried(final ServerPlayer player, final ItemStack stack, final PortableModel model) {
        final ServerLevel level = player.serverLevel();
        final long now = level.getGameTime();
        UUID id = id(stack);
        if (id == null) {
            id = UUID.randomUUID();
            stack.set(ComputingComponents.PORTABLE_ID.get(), id);
            setBattery(stack, model, battery(stack, model));
        }
        final PortableDevices devices = PortableDevices.get(level.getServer());
        final PortableState state = devices.of(id, model);
        // In an inventory, on or flat, it is not lost.
        if (now % LOOK_EVERY == 0 || state.seenAt() == 0L) {
            devices.seen(id, now);
        }
        if (!isOn(stack)) {
            switchOn(stack, model, now);
            if (!isOn(stack)) {
                return;
            }
        }
        if (now % DRAW_EVERY == 0) {
            final int left = battery(stack, model) - DRAW_EVERY * PortableBattery.DRAWN_A_TICK;
            setBattery(stack, model, left);
            if (left <= 0) {
                switchOff(level, stack);
                return;
            }
        }
        if (now % LOOK_EVERY == 0) {
            final NetworkUuid network = join(level, player, stack, model, id, now);
            tell(player, id, state, MirrorService.servingOn(level, network));
        }
    }

    /**
     * What {@code player} did on the device in their hand that {@code payload} names: on its system, which changes
     * what it keeps, or in one of its apps, which asks the network or the world and is answered by that app. A device
     * switched off does nothing.
     */
    public static void act(final PortableActionPayload payload, final ServerPlayer player, final ServerLevel level) {
        final ItemStack held = held(player, payload.device());
        if (held.isEmpty() || !(held.getItem() instanceof PortableItem item) || !isOn(held)) {
            return;
        }
        final UUID device = payload.device();
        final PortableDevices devices = PortableDevices.get(level.getServer());
        final PortableState state = devices.of(device, item.model());
        // An app's work is done only on a device that has that app.
        final MobileApp needs = appOf(payload.action());
        if (needs != null && !state.installed().contains(needs)) {
            return;
        }
        final PortableLinks.Joined joined = PortableLinks.of(level, device);
        final boolean mirror = joined != null && MirrorService.servingOn(level, joined.network());
        final StorageKey key = payload.key().orElse(null);
        final long amount = payload.amount();
        switch (payload.action()) {
            case INSTALL -> {
                final MobileApp app = MobileApp.byName(payload.named());
                if (app != null && mirror) {
                    devices.install(device, app);
                }
            }
            case SEE_ALERTS -> devices.seeAll(device);
            case CLEAR_ALERTS -> devices.clearAlerts(device);
            case TOGGLE_ALERT -> devices.toggle(device, AlertKind.byName(payload.named()));
            case WATCH -> {
                if (key != null) {
                    devices.watch(device, key, amount);
                }
            }
            case UNWATCH -> {
                if (key != null) {
                    devices.unwatch(device, key);
                }
            }
            case NETWORK -> PortableNetwork.view(player, level, device, state);
            case REQUEST -> {
                if (key != null) {
                    PortableNetwork.request(player, level, device, state, key, amount);
                }
            }
            case TAKE -> {
                if (key != null) {
                    PortableNetwork.take(player, level, device, key, amount);
                }
            }
            case RETURN -> {
                if (key != null) {
                    PortableNetwork.giveBack(player, level, device, state, key, amount);
                }
            }
            case DEPOSIT -> PortableNetwork.deposit(player, level, device, state, (int) amount);
            case DEPOSIT_ALL -> PortableNetwork.depositAll(player, level, device, state);
            case CRAFT -> {
                if (key != null) {
                    PortableNetwork.craft(player, level, device, state, key, amount);
                }
            }
            case PLAN -> {
                if (key != null) {
                    PortableNetwork.plan(player, level, device, key, amount);
                }
            }
            case GALLERY -> PortableGallery.view(player, level, device, state, Text.EMPTY);
            case SEND_PICTURE -> PortableGallery.send(player, level, device, state, (int) amount, payload.named());
            case DELETE_PICTURE -> PortableGallery.delete(player, level, device, (int) amount);
            case MESSENGER -> PortableMessenger.talk(player, level, device, MessengerActionPayload.LOOK,
                    payload.named(), "");
            case SAY -> PortableMessenger.talk(player, level, device, MessengerActionPayload.SAY, payload.named(),
                    payload.text());
            case LEAVE -> PortableMessenger.talk(player, level, device, MessengerActionPayload.LEAVE,
                    payload.named(), "");
            case MAP -> PortableMaps.map(player, level, device);
            case PROSPECT -> PortableMaps.prospect(player, level, device);
            case WALLET, WALLET_CREATE, WALLET_SEND -> PortableWallet.act(player, level, devices, state, joined != null,
                    payload);
        }
        tell(player, device, state, mirror);
    }

    /**
     * The device lying in the world as {@code entity} is gone for good, burned, fallen into the void or despawned:
     * whoever lost it loses what was on it, and its room on the server is freed.
     */
    public static void lost(final ItemEntity entity) {
        final UUID id = id(entity.getItem());
        if (id != null && entity.level() instanceof ServerLevel level) {
            PortableDevices.get(level.getServer()).erase(id);
        }
    }

    /** The device {@code device} in either of {@code player}'s hands, or an empty stack when neither holds it. */
    public static ItemStack held(final ServerPlayer player, final UUID device) {
        for (final InteractionHand hand : InteractionHand.values()) {
            final ItemStack stack = player.getItemInHand(hand);
            if (stack.getItem() instanceof PortableItem && device.equals(id(stack))) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }

    /*
     * Joins the device to the network of the nearest access point reaching its player, at the older end's speed, or
     * takes it off every network when none does; the network it is on, or null.
     */
    @Nullable
    private static NetworkUuid join(final ServerLevel level, final ServerPlayer player, final ItemStack stack,
                                    final PortableModel model, final UUID id, final long now) {
        final AccessPointBlockEntity point = AccessPoints.nearest(level, player.blockPosition());
        final NetworkUuid network = point == null ? null : point.serving();
        if (point == null || network == null) {
            stack.remove(ComputingComponents.PORTABLE_LINK.get());
            PortableLinks.left(level, id);
            return null;
        }
        final WirelessStandard speed = WirelessStandard.older(model.wifi(), point.standard());
        final PortableLink link = new PortableLink(ShortId.of(network.asString()),
                AccessPointBlockEntity.hostname(point.nodeUuid()), speed);
        if (!link.equals(stack.get(ComputingComponents.PORTABLE_LINK.get()))) {
            stack.set(ComputingComponents.PORTABLE_LINK.get(), link);
        }
        PortableLinks.seen(level, new PortableLinks.Joined(id, model, network,
                new WirelessLink(point.getBlockPos(), point.nodeUuid(), model.wifi(), point.standard()),
                onSince(stack), now));
        return network;
    }

    /* The app an action is the work of, or null for the system's own. */
    @Nullable
    private static MobileApp appOf(final PortableActionPayload.Action action) {
        return switch (action) {
            case INSTALL, SEE_ALERTS, CLEAR_ALERTS -> null;
            case TOGGLE_ALERT, WATCH, UNWATCH -> MobileApp.ALERTS;
            case NETWORK, REQUEST, TAKE, RETURN, DEPOSIT, DEPOSIT_ALL, CRAFT, PLAN -> MobileApp.NETWORK;
            case GALLERY, SEND_PICTURE, DELETE_PICTURE -> MobileApp.GALLERY;
            case MESSENGER, SAY, LEAVE -> MobileApp.MESSENGER;
            case MAP -> MobileApp.MAPS;
            case PROSPECT -> MobileApp.PROSPECTING;
            case WALLET, WALLET_CREATE, WALLET_SEND -> MobileApp.CRYPTO_WALLET;
        };
    }

    /* Tells {@code player}'s game what the device keeps, when that is not what it was last told. */
    private static void tell(final ServerPlayer player, final UUID id, final PortableState state,
                             final boolean mirror) {
        final Told now = new Told(state.version(), mirror);
        final Map<UUID, Told> told = TOLD.computeIfAbsent(player, any -> new HashMap<>());
        if (now.equals(told.get(id))) {
            return;
        }
        told.put(id, now);
        final List<PortableViewPayload.Watch> watched = new ArrayList<>();
        for (final Map.Entry<StorageKey, Long> watch : state.watched().entrySet()) {
            watched.add(new PortableViewPayload.Watch(watch.getKey(), watch.getValue()));
        }
        PacketDistributor.sendToPlayer(player, new PortableViewPayload(id, state.installed(), state.alerts(), mirror,
                state.alertsOn(), watched));
    }

    /* What a player's game was told of a device. */
    private record Told(int version, boolean mirror) {
    }
}
