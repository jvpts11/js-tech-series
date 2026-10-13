/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.operation.payload;

import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextCodecs;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

/**
 * Server to client: what the network's repository is keeping, and the comparison the window is showing.
 *
 * <p>The comparison comes worked out rather than as two whole texts, because the window only draws it and
 * sending two revisions to be compared on the client would cost several times as much for the same
 * picture.
 */
@TextHolder
public record KnotStatePayload(Service service, List<String> files, List<Revision> revisions,
                               int shownRevision, List<DiffLine> diff) implements CustomPacketPayload {

    public KnotStatePayload {
        files = List.copyOf(files);
        revisions = List.copyOf(revisions);
        diff = List.copyOf(diff);
    }

    /**
     * What the service is and what it costs.
     *
     * <p>Grouped for the same reason the messenger's is: a stream codec is built of at most six pairs, and
     * these three are the service rather than the source it keeps.
     */
    public record Service(boolean online, String host, long bytes, Text note) {

        /** The longest name of the machine keeping it that travels; a machine may be named far longer. */
        public static final int MAX_HOST = 64;

        /** What the machine has to say about the last thing asked of it, or nothing. */
        public Service(final boolean online, final String host, final long bytes) {
            this(online, host, bytes, Text.EMPTY);
        }

        public static final StreamCodec<RegistryFriendlyByteBuf, Service> STREAM_CODEC =
                StreamCodec.composite(
                        ByteBufCodecs.BOOL, Service::online,
                        ByteBufCodecs.stringUtf8(MAX_HOST), Service::host,
                        ByteBufCodecs.VAR_LONG, Service::bytes,
                        TextCodecs.STREAM_CODEC, Service::note,
                        Service::new);
    }

    // What the machine says about a push or a pull; a file's name is data.
    public static final TextKey NO_COMPUTER = TextKey.of("jsc.knot.note.no_computer", "No computer");
    public static final TextKey NO_SYSTEM_DISK = TextKey.of("jsc.knot.note.no_system_disk", "No system disk");
    public static final TextKey NO_SUCH_FILE = TextKey.of("jsc.knot.note.no_such_file", "No %s on this machine");
    public static final TextKey NOTHING_CHANGED =
            TextKey.of("jsc.knot.note.nothing_changed", "Nothing changed since the last revision");
    public static final TextKey PUSHED = TextKey.of("jsc.knot.note.pushed", "Pushed %s");
    public static final TextKey NO_SUCH_REVISION = TextKey.of("jsc.knot.note.no_such_revision", "No such revision");
    public static final TextKey NO_ROOM = TextKey.of("jsc.knot.note.no_room", "Not enough free space");
    public static final TextKey COULD_NOT_WRITE = TextKey.of("jsc.knot.note.could_not_write", "Could not write %s");
    public static final TextKey WROTE = TextKey.of("jsc.knot.note.wrote", "Wrote r%s to %s");

    /** How many revisions the history panel lists. */
    public static final int MAX_REVISIONS = 64;

    /** How many lines of a comparison travel at a time. */
    public static final int MAX_DIFF = 128;

    /** How many files are listed. */
    public static final int MAX_FILES = 64;

    /**
     * The longest path that travels, which is exactly what a push may name.
     *
     * <p>The two have to agree. A path longer than this one but short enough for the request was committed
     * and then could not be sent back, which left the window refusing to draw anything at all for a
     * repository that was perfectly sound.
     */
    public static final int MAX_PATH = KnotActionPayload.MAX_PATH;

    /** One saved state of a file, as the history lists it. */
    public record Revision(int number, String author, String message, String file) {

        public static final StreamCodec<RegistryFriendlyByteBuf, Revision> STREAM_CODEC =
                StreamCodec.composite(
                        ByteBufCodecs.VAR_INT, Revision::number,
                        ByteBufCodecs.stringUtf8(48), Revision::author,
                        ByteBufCodecs.stringUtf8(KnotActionPayload.MAX_MESSAGE), Revision::message,
                        ByteBufCodecs.stringUtf8(MAX_PATH), Revision::file,
                        Revision::new);
    }

    /** One line of the comparison: 0 is context, 1 arrived, 2 went away. */
    public record DiffLine(int kind, String text) {

        public static final int CONTEXT = 0;
        public static final int ADDED = 1;
        public static final int REMOVED = 2;

        public static final StreamCodec<RegistryFriendlyByteBuf, DiffLine> STREAM_CODEC =
                StreamCodec.composite(
                        ByteBufCodecs.VAR_INT, DiffLine::kind,
                        ByteBufCodecs.stringUtf8(256), DiffLine::text,
                        DiffLine::new);
    }

    public static final CustomPacketPayload.Type<KnotStatePayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("jsc", "knot_state"));

    public static final StreamCodec<RegistryFriendlyByteBuf, KnotStatePayload> STREAM_CODEC =
            StreamCodec.composite(
                    Service.STREAM_CODEC, KnotStatePayload::service,
                    ByteBufCodecs.stringUtf8(MAX_PATH).apply(ByteBufCodecs.list(MAX_FILES)),
                    KnotStatePayload::files,
                    Revision.STREAM_CODEC.apply(ByteBufCodecs.list(MAX_REVISIONS)),
                    KnotStatePayload::revisions,
                    ByteBufCodecs.VAR_INT, KnotStatePayload::shownRevision,
                    DiffLine.STREAM_CODEC.apply(ByteBufCodecs.list(MAX_DIFF)), KnotStatePayload::diff,
                    KnotStatePayload::new);

    @Override
    public CustomPacketPayload.Type<KnotStatePayload> type() {
        return TYPE;
    }
}
