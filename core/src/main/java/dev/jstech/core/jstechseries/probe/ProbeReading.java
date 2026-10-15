/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.jstechseries.probe;

import dev.jstech.core.JsCore;
import dev.jstech.core.energy.Energies;
import dev.jstech.core.format.UnitFormatter;
import dev.jstech.core.grid.LineSurvey;
import dev.jstech.core.id.IStableName;
import dev.jstech.core.id.StableCodecs;
import dev.jstech.core.text.Text;
import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * What a network probe read of the part of the line it is linked to, sent to the player holding it a few times a
 * second: what the line carries and in what unit, its cable and how many cables the part has, the flow each second of
 * the last minute, what was asked of it and what it lost, its narrowest cable, and the blocks it plugs into, grouped by
 * what they are to the line and by kind of block. Amounts are each second's mean a tick, in the line's own unit.
 *
 * @param present   whether there is anything to read where it is linked
 * @param unit      what the line carries
 * @param energy    the energy, for a line of energy
 * @param lineClass the line's class, for an energy with classes; empty otherwise
 * @param cable     the cable of the wire it is linked to
 * @param cables    how many cables the part has
 * @param moved     the flow, the last second's mean a tick
 * @param wanted    what was asked of it, the same way; {@link LineSurvey#UNKNOWN} when the line cannot tell
 * @param lost      what it lost on the way, the same way
 * @param narrowest the narrowest cable: where, which, and the most it carries a tick; none for a part with no limit
 * @param history   the flow each second of the last minute, the oldest first
 * @param groups    the blocks it plugs into, by role and kind
 */
public record ProbeReading(boolean present, Unit unit, Optional<ResourceLocation> energy, String lineClass,
                           ResourceLocation cable, int cables, long moved, long wanted, long lost,
                           Optional<LineSurvey.Narrowest> narrowest, List<Long> history, List<Group> groups)
        implements CustomPacketPayload {

    /** The most groups and the most places of a group a reading carries. */
    public static final int MOST_GROUPS = 48;
    public static final int MOST_PLACES = 16;
    /** How many seconds of flow it keeps. */
    public static final int SECONDS = 60;

    public static final Type<ProbeReading> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(JsCore.MODID,
            "probe_reading"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ProbeReading> STREAM_CODEC = StreamCodec.of(
            (buf, reading) -> reading.write(buf), ProbeReading::read);

    private static final StreamCodec<ByteBuf, Unit> UNIT_CODEC = ByteBufCodecs.fromCodec(
            StableCodecs.byName(Unit.class));
    private static final StreamCodec<ByteBuf, LineSurvey.Role> ROLE_CODEC = ByteBufCodecs.fromCodec(
            StableCodecs.byName(LineSurvey.Role.class));
    private static final ResourceLocation NOTHING = ResourceLocation.fromNamespaceAndPath(JsCore.MODID, "nothing");

    public ProbeReading {
        Objects.requireNonNull(unit, "unit");
        Objects.requireNonNull(energy, "energy");
        Objects.requireNonNull(lineClass, "lineClass");
        Objects.requireNonNull(cable, "cable");
        Objects.requireNonNull(narrowest, "narrowest");
        history = List.copyOf(history.size() > SECONDS ? history.subList(history.size() - SECONDS, history.size())
                : history);
        groups = List.copyOf(groups.size() > MOST_GROUPS ? groups.subList(0, MOST_GROUPS) : groups);
    }

    /** Nothing to read. */
    public static ProbeReading nothing() {
        return new ProbeReading(false, Unit.FLUID, Optional.empty(), "", NOTHING, 0, 0L, LineSurvey.UNKNOWN, 0L,
                Optional.empty(), List.of(), List.of());
    }

    /** How many blocks of {@code role} the part plugs into. */
    public int count(final LineSurvey.Role role) {
        int count = 0;
        for (final Group group : this.groups) {
            if (group.role() == role) {
                count += group.count();
            }
        }
        return count;
    }

    /** What the blocks of {@code role} gave or took, each second's mean a tick. */
    public long total(final LineSurvey.Role role) {
        long total = 0L;
        for (final Group group : this.groups) {
            if (group.role() == role) {
                total += group.perTick();
            }
        }
        return total;
    }

    /** {@code perTick} a tick, in the line's own unit, as a player reads it. */
    public Text rate(final long perTick) {
        final long amount = Math.max(0L, perTick);
        return switch (this.unit) {
            case ENERGY -> this.energy.flatMap(Energies::byId).map(found -> found.rate(amount))
                    .orElseGet(() -> Text.literal(String.valueOf(amount)));
            case FLUID -> NetworkProbe.RATE_FLUID.with(UnitFormatter.forCurrentLocale().compact(amount));
            case ITEMS -> NetworkProbe.RATE_ITEMS.with(UnitFormatter.forCurrentLocale().compact(amount));
            case MECHANICAL -> {
                final UnitFormatter.Scaled watts = UnitFormatter.forCurrentLocale().si(amount);
                yield NetworkProbe.RATE_WATTS.with(watts.number(), watts.prefix());
            }
            case NONE -> Text.literal(String.valueOf(amount));
        };
    }

    /** Whether the line carries all its narrowest cable lets through. */
    public boolean full() {
        return this.narrowest.isPresent() && this.narrowest.get().ceiling() > 0
                && this.moved >= this.narrowest.get().ceiling();
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private void write(final RegistryFriendlyByteBuf buf) {
        buf.writeBoolean(this.present);
        UNIT_CODEC.encode(buf, this.unit);
        buf.writeBoolean(this.energy.isPresent());
        this.energy.ifPresent(buf::writeResourceLocation);
        buf.writeUtf(this.lineClass);
        buf.writeResourceLocation(this.cable);
        buf.writeVarInt(this.cables);
        buf.writeVarLong(this.moved);
        buf.writeLong(this.wanted);
        buf.writeVarLong(this.lost);
        buf.writeBoolean(this.narrowest.isPresent());
        this.narrowest.ifPresent(found -> {
            buf.writeLong(found.pos());
            buf.writeUtf(found.cable());
            buf.writeVarLong(found.ceiling());
        });
        buf.writeVarInt(this.history.size());
        for (final long second : this.history) {
            buf.writeVarLong(second);
        }
        buf.writeVarInt(this.groups.size());
        for (final Group group : this.groups) {
            ROLE_CODEC.encode(buf, group.role());
            buf.writeResourceLocation(group.block());
            buf.writeVarInt(group.count());
            buf.writeVarLong(group.perTick());
            buf.writeVarInt(group.places().size());
            for (final long place : group.places()) {
                buf.writeLong(place);
            }
        }
    }

    private static ProbeReading read(final RegistryFriendlyByteBuf buf) {
        final boolean present = buf.readBoolean();
        final Unit unit = UNIT_CODEC.decode(buf);
        final Optional<ResourceLocation> energy = buf.readBoolean() ? Optional.of(buf.readResourceLocation())
                : Optional.empty();
        final String lineClass = buf.readUtf();
        final ResourceLocation cable = buf.readResourceLocation();
        final int cables = buf.readVarInt();
        final long moved = buf.readVarLong();
        final long wanted = buf.readLong();
        final long lost = buf.readVarLong();
        final Optional<LineSurvey.Narrowest> narrowest = buf.readBoolean() ? Optional.of(new LineSurvey.Narrowest(
                buf.readLong(), buf.readUtf(), buf.readVarLong())) : Optional.empty();
        final int seconds = Math.min(SECONDS, buf.readVarInt());
        final List<Long> history = new ArrayList<>(seconds);
        for (int k = 0; k < seconds; k++) {
            history.add(buf.readVarLong());
        }
        final int count = Math.min(MOST_GROUPS, buf.readVarInt());
        final List<Group> groups = new ArrayList<>(count);
        for (int k = 0; k < count; k++) {
            final LineSurvey.Role role = ROLE_CODEC.decode(buf);
            final ResourceLocation block = buf.readResourceLocation();
            final int blocks = buf.readVarInt();
            final long perTick = buf.readVarLong();
            final int placed = Math.min(MOST_PLACES, buf.readVarInt());
            final List<Long> places = new ArrayList<>(placed);
            for (int p = 0; p < placed; p++) {
                places.add(buf.readLong());
            }
            groups.add(new Group(role, block, blocks, perTick, places));
        }
        return new ProbeReading(present, unit, energy, lineClass, cable, cables, moved, wanted, lost, narrowest,
                history, groups);
    }

    /**
     * The blocks of one kind that are one thing to the line.
     *
     * @param role    what they are to it
     * @param block   their block
     * @param count   how many
     * @param perTick what they gave, or took, each second's mean a tick
     * @param places  where the first of them are
     */
    public record Group(LineSurvey.Role role, ResourceLocation block, int count, long perTick, List<Long> places) {

        public Group {
            Objects.requireNonNull(role, "role");
            Objects.requireNonNull(block, "block");
            places = List.copyOf(places.size() > MOST_PLACES ? places.subList(0, MOST_PLACES) : places);
        }
    }

    /** What a line carries, and so the unit its amounts are in. */
    public enum Unit implements IStableName {
        /** An energy, in its own unit. */
        ENERGY("energy"),
        /** Liquids, gases and the other states of matter, in millibuckets. */
        FLUID("fluid"),
        /** Items, one by one. */
        ITEMS("items"),
        /** The power a network of shafts carries, in watts. */
        MECHANICAL("mechanical"),
        /** Nothing: a line that only joins. */
        NONE("none");

        private final String serializedName;

        Unit(final String serializedName) {
            this.serializedName = serializedName;
        }

        @Override
        public String serializedName() {
            return this.serializedName;
        }

        /** Whether what it carries loses on the way: energy along its cables, power to the parts' friction. */
        public boolean loses() {
            return this == ENERGY || this == MECHANICAL;
        }
    }
}
