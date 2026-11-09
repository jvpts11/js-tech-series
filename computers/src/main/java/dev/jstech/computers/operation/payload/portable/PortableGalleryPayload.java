/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.operation.payload.portable;

import dev.jstech.computers.JsComputers;
import dev.jstech.computers.portable.PortablePicture;
import dev.jstech.computers.portable.PortableState;
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextCodecs;
import io.netty.buffer.ByteBuf;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Server to client: what a portable's gallery shows (computers/docs/design/wireless.md): its pictures, each by the
 * small copy the server keeps of it, and the computers of its network a picture can be sent to.
 *
 * @param device   the device's id
 * @param pictures its pictures, the oldest first
 * @param targets  the computers of its network that take a picture
 * @param note     what the last thing done in it came to: a picture kept or sent, or why not; empty for nothing
 */
public record PortableGalleryPayload(UUID device, List<Picture> pictures, List<Target> targets, Text note)
        implements CustomPacketPayload {

    /** The most computers listed. */
    public static final int MOST_TARGETS = 16;
    private static final int MOST_NAME = 48;

    public static final CustomPacketPayload.Type<PortableGalleryPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(JsComputers.MODID,
                    "portable_gallery"));

    public static final StreamCodec<RegistryFriendlyByteBuf, PortableGalleryPayload> STREAM_CODEC =
            StreamCodec.composite(
                    UUIDUtil.STREAM_CODEC, PortableGalleryPayload::device,
                    Picture.STREAM_CODEC.apply(ByteBufCodecs.list(PortableState.MOST_PICTURES)),
                    PortableGalleryPayload::pictures,
                    Target.STREAM_CODEC.apply(ByteBufCodecs.list(MOST_TARGETS)), PortableGalleryPayload::targets,
                    TextCodecs.STREAM_CODEC, PortableGalleryPayload::note,
                    PortableGalleryPayload::new);

    public PortableGalleryPayload {
        pictures = List.copyOf(pictures.subList(0, Math.min(PortableState.MOST_PICTURES, pictures.size())));
        targets = List.copyOf(targets.subList(0, Math.min(MOST_TARGETS, targets.size())));
    }

    @Override
    public CustomPacketPayload.Type<PortableGalleryPayload> type() {
        return TYPE;
    }

    /**
     * A picture as its gallery lists it.
     *
     * @param number    its number, which names its file
     * @param width     how wide it is
     * @param height    how tall
     * @param day       the day it was taken
     * @param thumbnail its small copy, a PNG
     */
    public record Picture(int number, int width, int height, long day, byte[] thumbnail) {

        public static final StreamCodec<ByteBuf, Picture> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, Picture::number,
                ByteBufCodecs.VAR_INT, Picture::width,
                ByteBufCodecs.VAR_INT, Picture::height,
                ByteBufCodecs.VAR_LONG, Picture::day,
                ByteBufCodecs.byteArray(PortablePicture.MOST_THUMBNAIL), Picture::thumbnail,
                Picture::new);

        public Picture {
            thumbnail = thumbnail.clone();
        }

        /** The picture as its gallery lists it. */
        public static Picture of(final PortablePicture picture) {
            return new Picture(picture.number(), picture.width(), picture.height(), picture.day(),
                    picture.thumbnail());
        }

        /** Its file's name, as the gallery calls it. */
        public String fileName() {
            return PortablePicture.fileNameOf(number);
        }

        /** The small copy, a copy of it. */
        @Override
        public byte[] thumbnail() {
            return thumbnail.clone();
        }
    }

    /**
     * A computer of the network a picture can be sent to.
     *
     * @param node its node on the network, written out
     * @param name what the network calls it
     * @param link how it is on the network: its cable, or Wi-Fi
     */
    public record Target(String node, String name, String link) {

        /** The link of a computer on the network by Wi-Fi. */
        public static final String WIFI = "wifi";

        public static final StreamCodec<ByteBuf, Target> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.stringUtf8(MOST_NAME), Target::node,
                ByteBufCodecs.stringUtf8(MOST_NAME), Target::name,
                ByteBufCodecs.stringUtf8(MOST_NAME), Target::link,
                Target::new);
    }
}
