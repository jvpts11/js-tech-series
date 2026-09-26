/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.audio.media;

import dev.jstech.core.JsCore;
import dev.jstech.core.client.audio.media.MediaCache;
import dev.jstech.core.client.audio.media.MediaPlayer;
import dev.jstech.core.client.audio.media.MediaUploader;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * What the recordings send over the network: a client asking for one and the server sending it in pieces, a player
 * offering one and sending it, and a recording starting and stopping near a player.
 */
@EventBusSubscriber(modid = JsCore.MODID)
public final class MediaNetwork {

    private static final String VERSION = "1";

    private MediaNetwork() {
    }

    @SubscribeEvent
    public static void onRegisterPayloads(final RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar(VERSION);
        registrar.playToServer(MediaWantPayload.TYPE, MediaWantPayload.STREAM_CODEC, MediaNetwork::onWant);
        registrar.playToServer(MediaOfferPayload.TYPE, MediaOfferPayload.STREAM_CODEC, MediaNetwork::onOffer);
        registrar.playToServer(MediaUploadPiecePayload.TYPE, MediaUploadPiecePayload.STREAM_CODEC,
                MediaNetwork::onUploadPiece);
        registrar.playToClient(MediaPiecePayload.TYPE, MediaPiecePayload.STREAM_CODEC, MediaNetwork::onPiece);
        registrar.playToClient(MediaMissingPayload.TYPE, MediaMissingPayload.STREAM_CODEC, MediaNetwork::onMissing);
        registrar.playToClient(MediaOfferReplyPayload.TYPE, MediaOfferReplyPayload.STREAM_CODEC,
                MediaNetwork::onOfferReply);
        registrar.playToClient(MediaUploadDonePayload.TYPE, MediaUploadDonePayload.STREAM_CODEC,
                MediaNetwork::onUploadDone);
        registrar.playToClient(MediaPlayPayload.TYPE, MediaPlayPayload.STREAM_CODEC, MediaNetwork::onPlay);
        registrar.playToClient(MediaStopPayload.TYPE, MediaStopPayload.STREAM_CODEC, MediaNetwork::onStop);
    }

    private static void onWant(final MediaWantPayload payload, final IPayloadContext context) {
        context.enqueueWork(() -> MediaDownloads.request((ServerPlayer) context.player(), payload.media()));
    }

    private static void onOffer(final MediaOfferPayload payload, final IPayloadContext context) {
        context.enqueueWork(() -> MediaUploads.offer((ServerPlayer) context.player(), payload));
    }

    private static void onUploadPiece(final MediaUploadPiecePayload payload, final IPayloadContext context) {
        context.enqueueWork(() -> MediaUploads.piece((ServerPlayer) context.player(), payload));
    }

    /* Runs on a client only: the payload is only ever sent to one. The same goes for the ones below. */
    private static void onPiece(final MediaPiecePayload payload, final IPayloadContext context) {
        context.enqueueWork(() -> MediaCache.onPiece(payload));
    }

    private static void onMissing(final MediaMissingPayload payload, final IPayloadContext context) {
        context.enqueueWork(() -> MediaCache.onMissing(payload.hash()));
    }

    private static void onOfferReply(final MediaOfferReplyPayload payload, final IPayloadContext context) {
        context.enqueueWork(() -> MediaUploader.onReply(payload));
    }

    private static void onUploadDone(final MediaUploadDonePayload payload, final IPayloadContext context) {
        context.enqueueWork(() -> MediaUploader.onDone(payload));
    }

    private static void onPlay(final MediaPlayPayload payload, final IPayloadContext context) {
        context.enqueueWork(() -> MediaPlayer.onPlay(payload));
    }

    private static void onStop(final MediaStopPayload payload, final IPayloadContext context) {
        context.enqueueWork(() -> MediaPlayer.onStop(payload.key()));
    }
}
