/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.operation.payload;

import dev.jstech.computers.engine.nextgre.NextgreEngine;
import dev.jstech.computers.engine.nextgre.NextgrePlanView;
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextCodecs;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Server to client: what the Nextgre Planner Studio window {@code window} shows. The engine and its version, the
 * network, what the last request came to, the plan asked for (if any), the plans made lately, the planner's rules and
 * hints, and its statistics.
 *
 * @param ok         whether what was asked went through
 * @param engine     the engine with its version, as the status bar shows it
 * @param network    the network's name
 * @param message    what the last request came to, or empty
 * @param plan       the plan the window is to show, when one was asked for
 * @param history    the plans made lately, the newest first
 * @param rules      the planner's rules and the dialect's hints
 * @param statistics what the planner reckons with
 */
public record NextgreStudioPayload(int window, boolean ok, Text engine, Text network, Text message,
                                   Optional<NextgrePlanView> plan, List<HistoryRow> history,
                                   List<NextgreEngine.RuleRow> rules, List<NextgreEngine.StatRow> statistics)
        implements CustomPacketPayload {

    /** The most rows of each list sent, so a packet stays small whatever other mods add. */
    public static final int MAX_ROWS = 64;

    public static final CustomPacketPayload.Type<NextgreStudioPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("jsc", "nextgre_studio"));

    public static final StreamCodec<RegistryFriendlyByteBuf, NextgreStudioPayload> STREAM_CODEC = StreamCodec.of(
            NextgreStudioPayload::write, NextgreStudioPayload::read);

    public NextgreStudioPayload {
        history = List.copyOf(history.subList(0, Math.min(history.size(), MAX_ROWS)));
        rules = List.copyOf(rules.subList(0, Math.min(rules.size(), MAX_ROWS)));
        statistics = List.copyOf(statistics.subList(0, Math.min(statistics.size(), MAX_ROWS)));
    }

    @Override
    public CustomPacketPayload.Type<NextgreStudioPayload> type() {
        return TYPE;
    }

    private static void write(final RegistryFriendlyByteBuf buf, final NextgreStudioPayload payload) {
        buf.writeVarInt(payload.window());
        buf.writeBoolean(payload.ok());
        TextCodecs.STREAM_CODEC.encode(buf, payload.engine());
        TextCodecs.STREAM_CODEC.encode(buf, payload.network());
        TextCodecs.STREAM_CODEC.encode(buf, payload.message());
        buf.writeBoolean(payload.plan().isPresent());
        payload.plan().ifPresent(plan -> NextgrePlanView.STREAM_CODEC.encode(buf, plan));
        buf.writeVarInt(payload.history().size());
        for (final HistoryRow row : payload.history()) {
            buf.writeVarInt(row.id());
            ByteBufCodecs.STRING_UTF8.encode(buf, row.statement());
            buf.writeVarLong(row.cost() + 1);
            buf.writeVarLong(row.executionTicks() + 1);
            buf.writeByte(row.state());
            buf.writeVarLong(row.at());
            buf.writeBoolean(row.analyze());
        }
        buf.writeVarInt(payload.rules().size());
        for (final NextgreEngine.RuleRow rule : payload.rules()) {
            ByteBufCodecs.STRING_UTF8.encode(buf, rule.id());
            TextCodecs.STREAM_CODEC.encode(buf, rule.name());
            TextCodecs.STREAM_CODEC.encode(buf, rule.tells());
            buf.writeBoolean(rule.on());
            buf.writeByte(rule.kind());
        }
        buf.writeVarInt(payload.statistics().size());
        for (final NextgreEngine.StatRow stat : payload.statistics()) {
            TextCodecs.STREAM_CODEC.encode(buf, stat.name());
            TextCodecs.STREAM_CODEC.encode(buf, stat.value());
        }
    }

    private static NextgreStudioPayload read(final RegistryFriendlyByteBuf buf) {
        final int window = buf.readVarInt();
        final boolean ok = buf.readBoolean();
        final Text engine = TextCodecs.STREAM_CODEC.decode(buf);
        final Text network = TextCodecs.STREAM_CODEC.decode(buf);
        final Text message = TextCodecs.STREAM_CODEC.decode(buf);
        final Optional<NextgrePlanView> plan = buf.readBoolean()
                ? Optional.of(NextgrePlanView.STREAM_CODEC.decode(buf)) : Optional.empty();
        final int historyCount = Math.min(buf.readVarInt(), MAX_ROWS);
        final List<HistoryRow> history = new ArrayList<>(historyCount);
        for (int i = 0; i < historyCount; i++) {
            history.add(new HistoryRow(buf.readVarInt(), ByteBufCodecs.STRING_UTF8.decode(buf),
                    buf.readVarLong() - 1, buf.readVarLong() - 1, buf.readByte(), buf.readVarLong(),
                    buf.readBoolean()));
        }
        final int ruleCount = Math.min(buf.readVarInt(), MAX_ROWS);
        final List<NextgreEngine.RuleRow> rules = new ArrayList<>(ruleCount);
        for (int i = 0; i < ruleCount; i++) {
            rules.add(new NextgreEngine.RuleRow(ByteBufCodecs.STRING_UTF8.decode(buf),
                    TextCodecs.STREAM_CODEC.decode(buf), TextCodecs.STREAM_CODEC.decode(buf), buf.readBoolean(),
                    buf.readByte()));
        }
        final int statCount = Math.min(buf.readVarInt(), MAX_ROWS);
        final List<NextgreEngine.StatRow> statistics = new ArrayList<>(statCount);
        for (int i = 0; i < statCount; i++) {
            statistics.add(new NextgreEngine.StatRow(TextCodecs.STREAM_CODEC.decode(buf),
                    TextCodecs.STREAM_CODEC.decode(buf)));
        }
        return new NextgreStudioPayload(window, ok, engine, network, message, plan, history, rules, statistics);
    }

    /**
     * A plan of the history, as its list shows it.
     *
     * @param id             its number
     * @param statement      the statement it was for
     * @param cost           what the chosen plan was reckoned to take, in ticks, or -1 when none was chosen
     * @param executionTicks what it took, or -1 when it did not run or runs still
     * @param state          where it stands, one of {@link NextgrePlanView}'s states
     * @param at             the game time it was planned at
     * @param analyze        whether it was run to be measured
     */
    public record HistoryRow(int id, String statement, long cost, long executionTicks, byte state, long at,
                             boolean analyze) {
    }
}
