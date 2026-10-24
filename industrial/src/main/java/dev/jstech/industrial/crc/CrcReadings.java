/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Industrial.
 */
package dev.jstech.industrial.crc;

import dev.jstech.core.id.StableCodecs;
import dev.jstech.core.machine.MachineStatus;
import dev.jstech.core.research.Discovery;
import dev.jstech.core.research.Research;
import io.netty.buffer.ByteBuf;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.DyeColor;

/**
 * What a Computational Research Centre's screen shows of it that only the server knows, worked out once a second
 * while the screen is open: what its processors make and draw and what its cooling sheds, how its team stands on every
 * discovery, the data its team holds in its group, and its group as the Group tab draws it.
 *
 * @param points   the research it made its last second, in points a second
 * @param draw     the power its processors draw now, in watts
 * @param made     the heat they made its last second, in watts
 * @param shed     the heat its cooling took away its last second, in watts
 * @param coolant  whether its coolant flowed through its Liquid Coolers its last second
 * @param heatPort whether its heat left through its Heat Port its last second
 * @param team     the name of its owner's team, whose research it works; empty for a CRC nobody owns
 * @param shares   how much of each discovery its team has made, those begun only
 * @param known    what its team knows of what the discoveries come after
 * @param held     the data its team holds in its group, by the id of each kind
 * @param pace     its team's pace in its group, in points a second
 * @param outcome  what came of its team's research its last second; none before its first
 * @param group    its group, as the Group tab draws it
 */
public record CrcReadings(float points, long draw, long made, long shed, boolean coolant, boolean heatPort,
                          String team, Map<ResourceLocation, Float> shares, Set<ResourceLocation> known,
                          Map<ResourceLocation, Integer> held, float pace, Optional<Research.Outcome> outcome,
                          Group group) {

    /** Nothing, until the server's first word. */
    public static final CrcReadings NONE = new CrcReadings(0F, 0L, 0L, 0L, false, false, "", Map.of(), Set.of(),
            Map.of(), 0F, Optional.empty(), Group.NONE);
    public static final StreamCodec<RegistryFriendlyByteBuf, CrcReadings> STREAM_CODEC = StreamCodec.of(
            CrcReadings::write, CrcReadings::read);
    private static final StreamCodec<FriendlyByteBuf, Map<ResourceLocation, Float>> SHARES = ByteBufCodecs.map(
            HashMap::new, ResourceLocation.STREAM_CODEC, ByteBufCodecs.FLOAT);
    private static final StreamCodec<FriendlyByteBuf, Map<ResourceLocation, Integer>> COUNTS = ByteBufCodecs.map(
            HashMap::new, ResourceLocation.STREAM_CODEC, ByteBufCodecs.VAR_INT);
    private static final StreamCodec<ByteBuf, Optional<Research.Outcome>> OUTCOME = ByteBufCodecs.optional(
            StableCodecs.byId(Research.Outcome.class, Research.Outcome.ABSENT));

    public CrcReadings {
        Objects.requireNonNull(team, "team");
        Objects.requireNonNull(outcome, "outcome");
        Objects.requireNonNull(group, "group");
        shares = Map.copyOf(shares);
        known = Set.copyOf(known);
        held = Map.copyOf(held);
    }

    /** How much of {@code discovery} its team has made, from 0 to 1. */
    public double share(final ResourceLocation discovery) {
        return this.shares.getOrDefault(discovery, 0F);
    }

    /** Whether its team has made the whole of {@code discovery}. */
    public boolean made(final ResourceLocation discovery) {
        return share(discovery) >= 1.0;
    }

    /** Whether its team knows everything {@code discovery} comes after, so its research may start. */
    public boolean ready(final Discovery discovery) {
        return this.known.containsAll(discovery.after());
    }

    /** The data of kind {@code kind} its team holds in its group. */
    public int held(final ResourceLocation kind) {
        return this.held.getOrDefault(kind, 0);
    }

    private static void write(final RegistryFriendlyByteBuf buf, final CrcReadings read) {
        buf.writeFloat(read.points);
        buf.writeVarLong(read.draw);
        buf.writeVarLong(read.made);
        buf.writeVarLong(read.shed);
        buf.writeBoolean(read.coolant);
        buf.writeBoolean(read.heatPort);
        buf.writeUtf(read.team);
        SHARES.encode(buf, read.shares);
        buf.writeCollection(read.known, FriendlyByteBuf::writeResourceLocation);
        COUNTS.encode(buf, read.held);
        buf.writeFloat(read.pace);
        OUTCOME.encode(buf, read.outcome);
        Group.STREAM_CODEC.encode(buf, read.group);
    }

    private static CrcReadings read(final RegistryFriendlyByteBuf buf) {
        return new CrcReadings(buf.readFloat(), buf.readVarLong(), buf.readVarLong(), buf.readVarLong(),
                buf.readBoolean(), buf.readBoolean(), buf.readUtf(), SHARES.decode(buf),
                buf.readCollection(HashSet::new, FriendlyByteBuf::readResourceLocation), COUNTS.decode(buf),
                buf.readFloat(), OUTCOME.decode(buf), Group.STREAM_CODEC.decode(buf));
    }

    /**
     * A CRC's group as the Group tab draws it.
     *
     * @param cable   the dye of the cable its Link Socket joins, by its id; {@link #UNDYED}, or {@link #NO_CABLE}
     * @param members its CRCs, the one read first
     * @param shares  how much of the group's discovery each team of the group has made, by the team's id
     */
    public record Group(int cable, List<Member> members, Map<String, Float> shares) {

        /** A cable that is not dyed. */
        public static final int UNDYED = -1;
        /** No cable: the CRC has no Link Socket, or nothing joins it. */
        public static final int NO_CABLE = -2;
        public static final Group NONE = new Group(NO_CABLE, List.of(), Map.of());
        public static final StreamCodec<RegistryFriendlyByteBuf, Group> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT.map(id -> id + NO_CABLE, cable -> cable - NO_CABLE), Group::cable,
                Member.STREAM_CODEC.apply(ByteBufCodecs.list()), Group::members,
                ByteBufCodecs.map(HashMap::new, ByteBufCodecs.STRING_UTF8, ByteBufCodecs.FLOAT), Group::shares,
                Group::new);

        public Group {
            members = List.copyOf(members);
            shares = Map.copyOf(shares);
        }

        /** The cable's dye; none for a cable not dyed, or no cable. */
        public Optional<DyeColor> dye() {
            return this.cable >= 0 ? Optional.of(DyeColor.byId(this.cable)) : Optional.empty();
        }

        /** How much of the group's discovery team {@code team} has made, from 0 to 1. */
        public double share(final String team) {
            return this.shares.getOrDefault(team, 0F);
        }
    }

    /**
     * A CRC of the group, as the Group tab's table shows it.
     *
     * @param pos      where its terminal stands
     * @param team     its owner's team, by id; empty for a CRC nobody owns
     * @param teamName its owner's team, by name
     * @param computes the research it made its last second, in points a second
     * @param gives    what its team counts of it: the whole for its team's strongest, 90% for each other
     * @param status   its state
     */
    public record Member(BlockPos pos, String team, String teamName, float computes, float gives,
                         MachineStatus status) {

        public static final StreamCodec<RegistryFriendlyByteBuf, Member> STREAM_CODEC = StreamCodec.composite(
                BlockPos.STREAM_CODEC, Member::pos,
                ByteBufCodecs.STRING_UTF8, Member::team,
                ByteBufCodecs.STRING_UTF8, Member::teamName,
                ByteBufCodecs.FLOAT, Member::computes,
                ByteBufCodecs.FLOAT, Member::gives,
                ByteBufCodecs.fromCodec(StableCodecs.byName(MachineStatus.class)), Member::status,
                Member::new);

        public Member {
            Objects.requireNonNull(pos, "pos");
            Objects.requireNonNull(team, "team");
            Objects.requireNonNull(teamName, "teamName");
            Objects.requireNonNull(status, "status");
        }
    }
}
