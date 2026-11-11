/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.os.disk;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * A partition's place in a ZFS pool (computers/docs/design/storage-devices.md): the pool it belongs to, whether the
 * pool is a mirror of two disks, and whether this is the member the pool is read from, which holds what the pool
 * keeps, or the second disk that repairs it. What the pool repaired and its last scrub are counted on the first, as
 * {@code zpool status} reports them.
 *
 * @param pool          the pool it belongs to
 * @param primary       whether it is the member the pool is read from, or a pool's only one
 * @param mirror        whether the pool is a mirror of two disks
 * @param repaired      the data weight the pool has repaired from the other member since it was made
 * @param repairs       how many times it found damage and repaired it, the checksum errors {@code zpool status} counts
 * @param scrubbedAt    the world's time of day of its last scrub, in ticks, or {@link #NEVER}
 * @param scrubRepaired the data weight its last scrub repaired
 */
public record ZfsMember(UUID pool, boolean primary, boolean mirror, long repaired, int repairs, long scrubbedAt,
                        long scrubRepaired) {

    /** The time of a scrub that never ran. */
    public static final long NEVER = -1L;

    public static final Codec<ZfsMember> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            UUIDUtil.CODEC.fieldOf("pool").forGetter(ZfsMember::pool),
            Codec.BOOL.fieldOf("primary").forGetter(ZfsMember::primary),
            Codec.BOOL.optionalFieldOf("mirror", true).forGetter(ZfsMember::mirror),
            Codec.LONG.optionalFieldOf("repaired", 0L).forGetter(ZfsMember::repaired),
            Codec.INT.optionalFieldOf("repairs", 0).forGetter(ZfsMember::repairs),
            Codec.LONG.optionalFieldOf("scrubbed_at", NEVER).forGetter(ZfsMember::scrubbedAt),
            Codec.LONG.optionalFieldOf("scrub_repaired", 0L).forGetter(ZfsMember::scrubRepaired)
    ).apply(inst, ZfsMember::new));

    // Seven parts are more than StreamCodec.composite takes, so the member is written out by hand.
    public static final StreamCodec<ByteBuf, ZfsMember> STREAM_CODEC = StreamCodec.of(
            (buf, member) -> {
                UUIDUtil.STREAM_CODEC.encode(buf, member.pool());
                buf.writeBoolean(member.primary());
                buf.writeBoolean(member.mirror());
                ByteBufCodecs.VAR_LONG.encode(buf, member.repaired());
                ByteBufCodecs.VAR_INT.encode(buf, member.repairs());
                buf.writeLong(member.scrubbedAt());
                ByteBufCodecs.VAR_LONG.encode(buf, member.scrubRepaired());
            },
            buf -> new ZfsMember(UUIDUtil.STREAM_CODEC.decode(buf), buf.readBoolean(), buf.readBoolean(),
                    ByteBufCodecs.VAR_LONG.decode(buf), ByteBufCodecs.VAR_INT.decode(buf), buf.readLong(),
                    ByteBufCodecs.VAR_LONG.decode(buf)));

    public ZfsMember {
        repaired = Math.max(0L, repaired);
        repairs = Math.max(0, repairs);
        scrubRepaired = Math.max(0L, scrubRepaired);
    }

    /** A member of a mirror just made: nothing repaired, never scrubbed. */
    public static ZfsMember ofMirror(final UUID pool, final boolean primary) {
        return new ZfsMember(pool, primary, true, 0L, 0, NEVER, 0L);
    }

    /** A pool on one disk, never scrubbed. */
    public static ZfsMember alone(final UUID pool) {
        return new ZfsMember(pool, true, false, 0L, 0, NEVER, 0L);
    }

    /** The same member, having found damage and repaired {@code weight} of it from the other. */
    public ZfsMember repairedMore(final long weight) {
        return weight <= 0L ? this : new ZfsMember(this.pool, this.primary, this.mirror, this.repaired + weight,
                this.repairs + 1, this.scrubbedAt, this.scrubRepaired);
    }

    /** The same member, scrubbed at {@code time}, the scrub having repaired {@code weight}. */
    public ZfsMember scrubbed(final long time, final long weight) {
        return new ZfsMember(this.pool, this.primary, this.mirror, this.repaired, this.repairs, time,
                Math.max(0L, weight));
    }
}
