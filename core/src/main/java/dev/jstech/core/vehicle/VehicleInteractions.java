/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.vehicle;

import dev.jstech.core.text.GameText;
import dev.jstech.core.text.Text;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.Tags;

/**
 * What a player does with a vehicle by using an item on it, where they use it: a part in the hand goes into the empty
 * place of its kind nearest; a wrench takes out the part there with the sneak key held, or mends it with its material
 * in the other hand; a dye paints it; its key locks and unlocks it; the sneak key with an empty hand hitches a trailer
 * to what is near its hitch, or lets it go; and anything else gets in. Only whom the owner lets set things up changes
 * its parts and paint.
 */
final class VehicleInteractions {

    /* What a mending takes the part back up by, of new. */
    private static final float MENDS = 0.25F;
    private static final int PERCENT = 100;

    private VehicleInteractions() {
    }

    /** {@code player} uses what is in {@code hand} on the vehicle at {@code hit}, from the vehicle's middle. */
    static InteractionResult interact(final VehicleEntity vehicle, final Player player, final InteractionHand hand,
                                      final Vec3 hit) {
        final VehicleDef def = vehicle.def();
        if (def == null) {
            return InteractionResult.PASS;
        }
        final ItemStack held = player.getItemInHand(hand);
        if (!(player instanceof ServerPlayer server)) {
            return handles(vehicle, def, player, held) ? InteractionResult.SUCCESS : InteractionResult.PASS;
        }
        final Vec3 local = local(vehicle, hit);
        if (held.has(Vehicles.PART.get())) {
            return mayChange(vehicle, server) ? fit(vehicle, def, server, held, local) : InteractionResult.CONSUME;
        }
        if (held.is(Tags.Items.TOOLS_WRENCH)) {
            if (!mayChange(vehicle, server)) {
                return InteractionResult.CONSUME;
            }
            return player.isSecondaryUseActive() ? takeOut(vehicle, def, server, local)
                    : mend(vehicle, def, server, local);
        }
        if (held.getItem() instanceof DyeItem dye && !def.paints().isEmpty()) {
            return mayChange(vehicle, server) ? paint(vehicle, def, server, held, dye.getDyeColor())
                    : InteractionResult.CONSUME;
        }
        if (held.has(Vehicles.KEY.get()) && player.isSecondaryUseActive()) {
            if (!VehicleAccess.opens(held, vehicle)) {
                tell(server, VehicleTexts.WRONG_KEY.text());
                return InteractionResult.CONSUME;
            }
            final boolean lock = !vehicle.switches().locked();
            vehicle.setSwitches(vehicle.switches().locked(lock));
            tell(server, (lock ? VehicleTexts.LOCKED_NOW : VehicleTexts.UNLOCKED_NOW).text());
            return InteractionResult.CONSUME;
        }
        if (held.isEmpty() && player.isSecondaryUseActive() && def.hitching().hitch().isPresent()) {
            if (vehicle.towedBy() != null) {
                vehicle.hitchTo(null);
                tell(server, VehicleTexts.UNHITCHED.text());
            } else {
                tell(server, (VehicleHitch.hitchNearest(vehicle, def) ? VehicleTexts.HITCHED
                        : VehicleTexts.NOTHING_TO_HITCH).text());
            }
            return InteractionResult.CONSUME;
        }
        return board(vehicle, server);
    }

    /* Whether the player's game should show the hand used: whatever the server would do something with. */
    private static boolean handles(final VehicleEntity vehicle, final VehicleDef def, final Player player,
                                   final ItemStack held) {
        return held.has(Vehicles.PART.get()) || held.is(Tags.Items.TOOLS_WRENCH)
                || held.getItem() instanceof DyeItem && !def.paints().isEmpty() || held.has(Vehicles.KEY.get())
                || player.isSecondaryUseActive() && held.isEmpty() && def.hitching().hitch().isPresent()
                || !player.isSecondaryUseActive() && vehicle.canAddPassengerFrom(player);
    }

    private static InteractionResult board(final VehicleEntity vehicle, final ServerPlayer player) {
        if (player.isSecondaryUseActive()) {
            return InteractionResult.PASS;
        }
        if (!VehicleAccess.mayBoard(vehicle, player)) {
            tell(player, VehicleTexts.LOCKED.text());
            return InteractionResult.CONSUME;
        }
        if (!vehicle.canAddPassengerFrom(player)) {
            tell(player, VehicleTexts.NO_SEAT.text());
            return InteractionResult.CONSUME;
        }
        return player.startRiding(vehicle) ? InteractionResult.CONSUME : InteractionResult.PASS;
    }

    /* Fits the part in the hand into the empty place of its kind nearest {@code at}. */
    private static InteractionResult fit(final VehicleEntity vehicle, final VehicleDef def, final ServerPlayer player,
                                         final ItemStack held, final Vec3 at) {
        final VehiclePartStack stack = held.get(Vehicles.PART.get());
        final Optional<VehiclePartDef> part = stack == null ? Optional.empty()
                : VehicleParts.get(vehicle.level(), stack.part());
        if (part.isEmpty()) {
            return InteractionResult.CONSUME;
        }
        VehicleDef.Place best = null;
        for (final VehicleDef.Place place : def.places()) {
            if (place.kind() == part.get().kind() && vehicle.parts().at(place.name()).isEmpty()
                    && (best == null || place.at().distanceTo(at) < best.at().distanceTo(at))) {
                best = place;
            }
        }
        if (best == null) {
            tell(player, VehicleTexts.NO_PLACE.text());
            return InteractionResult.CONSUME;
        }
        vehicle.parts().fit(best.name(), stack.part(), stack.health());
        vehicle.partsChanged();
        held.consume(1, player);
        tell(player, VehicleTexts.FITTED.with(named(part.get().name())));
        return InteractionResult.CONSUME;
    }

    /* Takes out the part nearest {@code at}, into the player's hands. */
    private static InteractionResult takeOut(final VehicleEntity vehicle, final VehicleDef def,
                                             final ServerPlayer player, final Vec3 at) {
        final Optional<VehicleDef.Place> place = nearestFitted(vehicle, def, at, any -> true);
        if (place.isEmpty()) {
            return InteractionResult.PASS;
        }
        final Optional<FittedParts.Fitted> taken = vehicle.parts().take(place.get().name());
        vehicle.partsChanged();
        taken.ifPresent(fitted -> {
            player.getInventory().placeItemBackInInventory(VehicleParts.get(vehicle.level(), fitted.part())
                    .map(part -> Vehicles.partItem(fitted.part(), fitted.health())).orElse(ItemStack.EMPTY));
            tell(player, VehicleTexts.TAKEN_OUT.with(VehicleParts.get(vehicle.level(), fitted.part())
                    .map(part -> named(part.name())).orElse(Text.literal(fitted.part().toString()))));
        });
        return InteractionResult.CONSUME;
    }

    /* Mends the worn part nearest {@code at} that the item in the other hand mends. */
    private static InteractionResult mend(final VehicleEntity vehicle, final VehicleDef def, final ServerPlayer player,
                                          final Vec3 at) {
        final ItemStack material = player.getOffhandItem();
        final ResourceLocation item = BuiltInRegistries.ITEM.getKey(material.getItem());
        final Optional<VehicleDef.Place> place = nearestFitted(vehicle, def, at, fitted -> fitted.health() < 1
                && VehicleParts.get(vehicle.level(), fitted.part()).flatMap(VehiclePartDef::repair)
                        .map(item::equals).orElse(false));
        if (material.isEmpty() || place.isEmpty()) {
            tell(player, VehicleTexts.NOTHING_TO_MEND.text());
            return InteractionResult.CONSUME;
        }
        final float now = vehicle.parts().mend(place.get().name(), MENDS);
        vehicle.partsChanged();
        material.consume(1, player);
        final String partName = vehicle.parts().at(place.get().name()).flatMap(fitted ->
                VehicleParts.get(vehicle.level(), fitted.part())).map(VehiclePartDef::name).orElse("");
        tell(player, VehicleTexts.MENDED.with(named(partName), Text.literal(Math.round(now * PERCENT) + "%")));
        return InteractionResult.CONSUME;
    }

    /* Paints it the next paint of the dye's colour, taking the dye. */
    private static InteractionResult paint(final VehicleEntity vehicle, final VehicleDef def,
                                           final ServerPlayer player, final ItemStack held, final DyeColor colour) {
        final List<VehicleDef.Paint> ofDye = def.paints().stream()
                .filter(paint -> paint.dye().map(colour::equals).orElse(false)).toList();
        if (ofDye.isEmpty()) {
            tell(player, VehicleTexts.NO_PAINT.text());
            return InteractionResult.CONSUME;
        }
        int next = 0;
        for (int i = 0; i < ofDye.size(); i++) {
            if (ofDye.get(i).id().equals(vehicle.paint())) {
                next = (i + 1) % ofDye.size();
            }
        }
        vehicle.setPaint(ofDye.get(next).id());
        held.consume(1, player);
        tell(player, VehicleTexts.PAINTED.with(named(ofDye.get(next).name())));
        return InteractionResult.CONSUME;
    }

    /* The fitted place nearest {@code at} whose part passes {@code wanted}. */
    private static Optional<VehicleDef.Place> nearestFitted(final VehicleEntity vehicle, final VehicleDef def,
                                                            final Vec3 at, final Predicate<FittedParts.Fitted> wanted) {
        VehicleDef.Place best = null;
        for (final VehicleDef.Place place : def.places()) {
            final Optional<FittedParts.Fitted> fitted = vehicle.parts().at(place.name());
            if (fitted.isPresent() && wanted.test(fitted.get())
                    && (best == null || place.at().distanceTo(at) < best.at().distanceTo(at))) {
                best = place;
            }
        }
        return Optional.ofNullable(best);
    }

    /* Whether {@code player} may change its parts and paint: whom its owner lets set it up, or anyone's. */
    private static boolean mayChange(final VehicleEntity vehicle, final ServerPlayer player) {
        return vehicle.ownership().map(owner -> owner.maySetUp(player)).orElse(true);
    }

    /* {@code hit}, from the vehicle's middle in the world's axes, in the vehicle's own: x to its left, z ahead. */
    private static Vec3 local(final VehicleEntity vehicle, final Vec3 hit) {
        final Vec3 ahead = Vec3.directionFromRotation(0, vehicle.getYRot());
        final Vec3 left = new Vec3(ahead.z, 0, -ahead.x);
        return new Vec3(hit.dot(left), hit.y, hit.dot(ahead));
    }

    private static Text named(final String translationKey) {
        return GameText.of(Component.translatable(translationKey));
    }

    private static void tell(final ServerPlayer player, final Text text) {
        player.displayClientMessage(GameText.component(text), true);
    }
}
