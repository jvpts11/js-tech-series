/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.portable;

import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextCodecs;
import dev.jstech.core.text.TextTags;
import io.netty.buffer.ByteBuf;
import java.util.Objects;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * An alert a portable got from its network (computers/docs/design/wireless.md): its kind, what it says and the line
 * under it, the game time it came, and whether its player has seen it, which the badge on the app's icon counts.
 */
public record PortableAlert(AlertKind kind, Text title, Text detail, long at, boolean seen) {

    public static final StreamCodec<ByteBuf, PortableAlert> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.stringUtf8(16).map(AlertKind::byName, AlertKind::serializedName), PortableAlert::kind,
            TextCodecs.STREAM_CODEC, PortableAlert::title,
            TextCodecs.STREAM_CODEC, PortableAlert::detail,
            ByteBufCodecs.VAR_LONG, PortableAlert::at,
            ByteBufCodecs.BOOL, PortableAlert::seen,
            PortableAlert::new);

    public PortableAlert {
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(title, "title");
        Objects.requireNonNull(detail, "detail");
    }

    /** The alert read back from what {@link #write} wrote. */
    public static PortableAlert read(final CompoundTag tag) {
        return new PortableAlert(AlertKind.byName(tag.getString("Kind")), TextTags.read(tag.getCompound("Title")),
                TextTags.read(tag.getCompound("Detail")), tag.getLong("At"), tag.getBoolean("Seen"));
    }

    /** The same alert, seen. */
    public PortableAlert asSeen() {
        return seen ? this : new PortableAlert(kind, title, detail, at, true);
    }

    /** The alert as it is saved. */
    public CompoundTag write() {
        final CompoundTag tag = new CompoundTag();
        tag.putString("Kind", kind.serializedName());
        tag.put("Title", TextTags.write(title));
        tag.put("Detail", TextTags.write(detail));
        tag.putLong("At", at);
        tag.putBoolean("Seen", seen);
        return tag;
    }
}
