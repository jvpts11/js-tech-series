/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.operation.payload;

import dev.jstech.computers.bus.BusActivity;
import dev.jstech.computers.bus.BusCondition;
import dev.jstech.computers.bus.BusSettings;
import dev.jstech.core.id.StableIds;
import dev.jstech.core.tier.HardwareEra;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Server to client: everything an open bus window shows that is not a slot: how the bus is set, what its filter
 * lists (to draw), whether it reaches its network, how fast it moves on its cable, which era's skin the window wears,
 * and what it did lately. Sent when the window opens and whenever any of it changes while it is open.
 *
 * @param containerId the window it is for
 * @param settings    how the bus is set
 * @param filter      what each filter slot lists, an empty stack for an empty slot
 * @param linked      whether its cable reaches a network
 * @param speed       how many items a tick it moves on its cable
 * @param carries     how many its cable carries a tick
 * @param skin        the era whose skin the window wears: the bus's, or for a crafting bus its Mainframe's
 * @param activity    what it did lately, newest first
 */
public record BusStatePayload(int containerId, BusSettings settings, List<ItemStack> filter, boolean linked,
                              long speed, long carries, HardwareEra skin, List<BusActivity.Entry> activity)
        implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<BusStatePayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("jsc", "bus_state"));

    /** The longest text the payload carries: a name, an id, a program's name. */
    private static final int MAX_TEXT = 128;
    private static final int MOST_LISTED = 8;
    private static final int MOST_ENTRIES = 32;

    public static final StreamCodec<RegistryFriendlyByteBuf, BusStatePayload> STREAM_CODEC =
            StreamCodec.of(BusStatePayload::write, BusStatePayload::read);

    public BusStatePayload {
        filter = List.copyOf(filter);
        activity = List.copyOf(activity);
    }

    @Override
    public CustomPacketPayload.Type<BusStatePayload> type() {
        return TYPE;
    }

    private static void write(final RegistryFriendlyByteBuf buf, final BusStatePayload p) {
        buf.writeVarInt(p.containerId);
        final BusSettings s = p.settings;
        writeText(buf, s.name());
        buf.writeVarInt(s.era().level());
        writeStrings(buf, s.filter());
        buf.writeBoolean(s.exclude());
        buf.writeVarInt(s.keep());
        buf.writeVarInt(s.max());
        writeInts(buf, s.itemKeep());
        writeInts(buf, s.itemMax());
        buf.writeVarInt(s.priority());
        buf.writeVarInt(Math.min(MOST_LISTED, s.conditions().size()));
        for (final BusCondition c : s.conditions().subList(0, Math.min(MOST_LISTED, s.conditions().size()))) {
            buf.writeVarInt(c.kind().id());
            writeText(buf, c.subject());
            buf.writeVarLong(c.below());
            buf.writeVarInt(c.fromHour());
            buf.writeVarInt(c.toHour());
        }
        writeStrings(buf, s.tags());
        buf.writeBoolean(s.fuzzy());
        buf.writeBoolean(s.powered());
        buf.writeBoolean(s.onDemand());
        buf.writeVarInt(Math.min(MOST_LISTED, s.setBy().size()));
        s.setBy().entrySet().stream().limit(MOST_LISTED).forEach(entry -> {
            writeText(buf, entry.getKey());
            writeText(buf, entry.getValue());
        });
        buf.writeVarInt(Math.min(MOST_LISTED, p.filter.size()));
        p.filter.stream().limit(MOST_LISTED).forEach(stack -> ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, stack));
        buf.writeBoolean(p.linked);
        buf.writeVarLong(p.speed);
        buf.writeVarLong(p.carries);
        buf.writeVarInt(p.skin.level());
        buf.writeVarInt(Math.min(MOST_ENTRIES, p.activity.size()));
        for (final BusActivity.Entry e : p.activity.subList(0, Math.min(MOST_ENTRIES, p.activity.size()))) {
            buf.writeVarLong(e.time());
            writeText(buf, e.what());
            buf.writeVarLong(e.amount());
            buf.writeByte(e.status());
            buf.writeByte(e.reason());
            buf.writeVarLong(e.detail());
        }
    }

    private static BusStatePayload read(final RegistryFriendlyByteBuf buf) {
        final int containerId = buf.readVarInt();
        final String name = buf.readUtf(MAX_TEXT);
        final HardwareEra era = HardwareEra.fromLevel(buf.readVarInt());
        final List<String> filter = readStrings(buf);
        final boolean exclude = buf.readBoolean();
        final int keep = buf.readVarInt();
        final int max = buf.readVarInt();
        final List<Integer> itemKeep = readInts(buf);
        final List<Integer> itemMax = readInts(buf);
        final int priority = buf.readVarInt();
        final int conditionCount = Math.min(MOST_LISTED, buf.readVarInt());
        final List<BusCondition> conditions = new ArrayList<>();
        for (int i = 0; i < conditionCount; i++) {
            final BusCondition.Kind kind = StableIds.of(BusCondition.Kind.class).find(buf.readVarInt());
            final String subject = buf.readUtf(MAX_TEXT);
            final long below = buf.readVarLong();
            final int from = buf.readVarInt();
            final int to = buf.readVarInt();
            if (kind != null) {
                conditions.add(new BusCondition(kind, subject, below, from, to));
            }
        }
        final List<String> tags = readStrings(buf);
        final boolean fuzzy = buf.readBoolean();
        final boolean powered = buf.readBoolean();
        final boolean onDemand = buf.readBoolean();
        final int marks = Math.min(MOST_LISTED, buf.readVarInt());
        final Map<String, String> setBy = new LinkedHashMap<>();
        for (int i = 0; i < marks; i++) {
            setBy.put(buf.readUtf(MAX_TEXT), buf.readUtf(MAX_TEXT));
        }
        final int stacks = Math.min(MOST_LISTED, buf.readVarInt());
        final List<ItemStack> filterStacks = new ArrayList<>();
        for (int i = 0; i < stacks; i++) {
            filterStacks.add(ItemStack.OPTIONAL_STREAM_CODEC.decode(buf));
        }
        final boolean linked = buf.readBoolean();
        final long speed = buf.readVarLong();
        final long carries = buf.readVarLong();
        final HardwareEra skin = HardwareEra.fromLevel(buf.readVarInt());
        final int entries = Math.min(MOST_ENTRIES, buf.readVarInt());
        final List<BusActivity.Entry> activity = new ArrayList<>();
        for (int i = 0; i < entries; i++) {
            activity.add(new BusActivity.Entry(buf.readVarLong(), buf.readUtf(MAX_TEXT), buf.readVarLong(),
                    buf.readByte(), buf.readByte(), buf.readVarLong()));
        }
        final BusSettings settings = new BusSettings(name, era, filter, exclude, keep, max, itemKeep, itemMax,
                priority, conditions, tags, fuzzy, powered, onDemand, setBy);
        return new BusStatePayload(containerId, settings, filterStacks, linked, speed, carries, skin, activity);
    }

    private static void writeStrings(final FriendlyByteBuf buf, final List<String> values) {
        buf.writeVarInt(Math.min(MOST_LISTED, values.size()));
        values.stream().limit(MOST_LISTED).forEach(value -> writeText(buf, value));
    }

    /* A text cut to what the payload carries, so a long one is shortened rather than failing the whole window. */
    private static void writeText(final FriendlyByteBuf buf, final String value) {
        buf.writeUtf(value.length() > MAX_TEXT ? value.substring(0, MAX_TEXT) : value, MAX_TEXT);
    }

    private static List<String> readStrings(final FriendlyByteBuf buf) {
        final int count = Math.min(MOST_LISTED, buf.readVarInt());
        final List<String> values = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            values.add(buf.readUtf(MAX_TEXT));
        }
        return values;
    }

    private static void writeInts(final FriendlyByteBuf buf, final List<Integer> values) {
        buf.writeVarInt(Math.min(MOST_LISTED, values.size()));
        values.stream().limit(MOST_LISTED).forEach(buf::writeVarInt);
    }

    private static List<Integer> readInts(final FriendlyByteBuf buf) {
        final int count = Math.min(MOST_LISTED, buf.readVarInt());
        final List<Integer> values = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            values.add(buf.readVarInt());
        }
        return values;
    }
}
