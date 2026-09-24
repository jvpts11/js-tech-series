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
 * Server to client: the list of {@code .iql} file names present on the Mainframe's system disk.
 *
 * <p>Sent in response to {@link RequestIqlFileListPayload} and after every successful
 * {@link SaveIqlFilePayload} write, so the NMS file picker always reflects the current disk state.
 *
 * <p>Each entry is the full file path, including the {@code .iql} extension. When the status field
 * is non-empty it reports the outcome of the preceding save (saved, or why not), read in the player's
 * language; on a plain list-request it is empty.
 *
 * @param files  the list of {@code .iql} file paths (may be empty)
 * @param status a short outcome message after a save, or empty for a plain list response
 * @param ok     true when the preceding operation succeeded (meaningful only when status is non-empty)
 */
@TextHolder
public record IqlFileListPayload(List<String> files, Text status, boolean ok)
        implements CustomPacketPayload {

    /** Maximum number of file entries sent in one payload. */
    public static final int MAX_FILES = 64;

    /** Maximum characters per file name ({@value SaveIqlFilePayload#MAX_NAME_LEN} + ".iql" + safety). */
    private static final int MAX_NAME = 40;

    // How a save went.
    public static final TextKey NO_MAINFRAME = TextKey.of("jsc.nms.file.no_mainframe", "no Mainframe on network");
    public static final TextKey NO_SYSTEM_DISK =
            TextKey.of("jsc.nms.file.no_system_disk", "Mainframe has no system disk");
    public static final TextKey NO_OS = TextKey.of("jsc.nms.file.no_os", "no OS installed on Mainframe disk");
    public static final TextKey SAVED = TextKey.of("jsc.nms.file.saved", "saved: %s");
    public static final TextKey DISK_FULL =
            TextKey.of("jsc.nms.file.disk_full", "disk full, free space on the Mainframe's system disk");
    public static final TextKey INVALID_NAME = TextKey.of("jsc.nms.file.invalid_name", "invalid file name");
    public static final TextKey READ_ONLY = TextKey.of("jsc.nms.file.read_only", "file type is read-only");

    public static final CustomPacketPayload.Type<IqlFileListPayload> TYPE =
            new CustomPacketPayload.Type<>(
                    ResourceLocation.fromNamespaceAndPath("jsc", "iql_file_list"));

    private record FileName(String value) {
        static final StreamCodec<RegistryFriendlyByteBuf, FileName> STREAM_CODEC =
                StreamCodec.composite(
                        ByteBufCodecs.stringUtf8(MAX_NAME), FileName::value,
                        FileName::new);
    }

    public static final StreamCodec<RegistryFriendlyByteBuf, IqlFileListPayload> STREAM_CODEC =
            StreamCodec.composite(
                    FileName.STREAM_CODEC.apply(ByteBufCodecs.list(MAX_FILES)),
                    payload -> payload.files().stream().map(FileName::new).toList(),
                    TextCodecs.STREAM_CODEC,
                    IqlFileListPayload::status,
                    ByteBufCodecs.BOOL,
                    IqlFileListPayload::ok,
                    (names, status, ok) -> new IqlFileListPayload(
                            names.stream().map(FileName::value).toList(), status, ok));

    /* Copied on the way in, so what the client is handed cannot change under it after it arrives. */
    public IqlFileListPayload {
        files = List.copyOf(files);
    }

    @Override
    public CustomPacketPayload.Type<IqlFileListPayload> type() {
        return TYPE;
    }
}
