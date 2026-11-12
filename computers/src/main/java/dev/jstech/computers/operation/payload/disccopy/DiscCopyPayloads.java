/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.operation.payload.disccopy;

import dev.jstech.computers.audio.VinylDiscs;
import dev.jstech.computers.client.os.EasyDiscCreatorApp;
import dev.jstech.computers.machine.DiscCopies;
import dev.jstech.computers.operation.payload.ClientPayloadHandlers;
import dev.jstech.computers.operation.payload.ComputerAccess;
import dev.jstech.computers.operation.payload.DiscCopyActionPayload;
import dev.jstech.computers.operation.payload.DiscCopyStatePayload;
import dev.jstech.computers.os.IOsHost;
import dev.jstech.computers.os.media.MediaItem;
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.JukeboxSong;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * Easy Disc Creator and its computer (computers/docs/design/peripherals.md): the drives it copies with and the copy
 * under way, and Copy, OK to what it asked, and Cancel, each answered with how things stand afterwards.
 */
@TextHolder
public final class DiscCopyPayloads {

    private static final TextKey VINYL_DRIVE = TextKey.of("jsc.disc_copier.vinyl_drive", "Vinyl RW Drive");

    private DiscCopyPayloads() {
    }

    /** Registers the payloads this class handles. */
    public static void register(final PayloadRegistrar registrar) {
        ComputerAccess.accept(registrar, DiscCopyActionPayload.TYPE, DiscCopyActionPayload.STREAM_CODEC,
                ComputerAccess.machine(DiscCopyActionPayload::hostPos), DiscCopyPayloads::handle);
        registrar.playToClient(DiscCopyStatePayload.TYPE, DiscCopyStatePayload.STREAM_CODEC,
                ClientPayloadHandlers.onMainThread((state, player) -> EasyDiscCreatorApp.accept(state)));
    }

    private static void handle(final DiscCopyActionPayload payload, final ServerPlayer player,
                               final ServerLevel level) {
        if (!(level.getBlockEntity(payload.hostPos()) instanceof IOsHost host)) {
            return;
        }
        final Text answer = switch (payload.action()) {
            case DiscCopyActionPayload.START -> DiscCopies.start(level, host, payload.hostPos(), player,
                    payload.source(), payload.target(), payload.copies());
            case DiscCopyActionPayload.GO_ON -> DiscCopies.proceed(level, host, payload.hostPos());
            case DiscCopyActionPayload.STOP -> {
                DiscCopies.cancel(level, payload.hostPos());
                yield Text.EMPTY;
            }
            default -> Text.EMPTY;
        };
        final List<DiscCopyStatePayload.Place> places = new ArrayList<>();
        for (final DiscCopies.Place place : DiscCopies.places(level, host)) {
            if (places.size() < DiscCopyStatePayload.MOST_PLACES) {
                places.add(placeOf(level, place));
            }
        }
        final DiscCopyStatePayload.Copy copy = DiscCopies.of(level, payload.hostPos())
                .map(job -> new DiscCopyStatePayload.Copy(job.phase().id(),
                        Character.toString(job.sourceLetter()), Character.toString(job.targetLetter()),
                        (int) Math.round(job.done() * 1000), job.bytes(), job.left(), job.ask(), job.note()))
                .orElse(DiscCopyStatePayload.Copy.NONE);
        PacketDistributor.sendToPlayer(player, new DiscCopyStatePayload(payload.hostPos(), places, copy, answer));
    }

    /*
     * A drive and what it holds, as data: the item and the name the player gave it, whether it is a blank or holds the
     * network's storage, its bytes, and a music disc's song, which the program words in the player's own language.
     */
    private static DiscCopyStatePayload.Place placeOf(final ServerLevel level, final DiscCopies.Place place) {
        final ItemStack medium = place.medium();
        final Component name = medium.get(DataComponents.CUSTOM_NAME);
        final Holder<JukeboxSong> song = place.vinyl()
                ? VinylDiscs.songOf(medium, level.registryAccess()).orElse(null) : null;
        final String songId = song == null ? "" : song.unwrapKey().map(key -> key.location().toString()).orElse("");
        return new DiscCopyStatePayload.Place(place.pos(), Character.toString(place.letter()),
                place.vinyl() ? VINYL_DRIVE.text() : place.type() == null ? Text.EMPTY : place.type().driveName(),
                BuiltInRegistries.ITEM.getKey(medium.getItem()), cut(name == null ? "" : name.getString()),
                DiscCopies.blank(medium), place.vinyl(), MediaItem.data(medium).total() > 0,
                DiscCopies.bytesOf(medium), cut(songId), song == null ? 0 : (int) song.value().lengthInSeconds());
    }

    private static String cut(final String words) {
        return words.length() <= DiscCopyStatePayload.Place.MOST_NAME ? words
                : words.substring(0, DiscCopyStatePayload.Place.MOST_NAME);
    }
}
