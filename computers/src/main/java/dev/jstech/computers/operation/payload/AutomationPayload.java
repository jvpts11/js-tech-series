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
 * Server to client: the Automation Manager's view. It reports whether a job-running engine (the Automation
 * Engine or the IQL Engine) is active on the network's Mainframe, a short label for it, and every saved job
 * with a friendly type, its trigger, and whether it is paused. Sent in reply to
 * {@link RequestAutomationPayload}. The label, the type and the trigger are read in the player's language.
 */
@TextHolder
public record AutomationPayload(boolean engineOnline, Text engineLabel, List<JobRow> jobs,
                               List<String> iqlFiles) implements CustomPacketPayload {

    public static final int MAX_JOBS = 64;
    public static final int MAX_FILES = 64;

    // What stands in for an engine's name when there is none to name.
    public static final TextKey NO_MAINFRAME = TextKey.of("jsc.automation.no_mainframe", "no Mainframe");
    public static final TextKey NO_ENGINE = TextKey.of("jsc.automation.no_engine", "none");
    // The kinds of job the list names, and a job that runs only when asked.
    public static final TextKey PERIODIC_MOVE = TextKey.of("jsc.automation.periodic_move", "Periodic Move");
    public static final TextKey KEEP_STOCK = TextKey.of("jsc.automation.keep_stock", "Keep Stock");
    public static final TextKey BATCH_CRAFT = TextKey.of("jsc.automation.batch_craft", "Batch Craft");
    public static final TextKey CUSTOM = TextKey.of("jsc.automation.custom", "Custom");
    public static final TextKey MANUAL = TextKey.of("jsc.automation.manual", "manual");

    /** One saved job as the list shows it: name, a friendly type, its trigger summary, and paused state. */
    public record JobRow(String name, Text type, Text trigger, boolean paused) {
        public static final StreamCodec<RegistryFriendlyByteBuf, JobRow> STREAM_CODEC =
                StreamCodec.composite(
                        ByteBufCodecs.STRING_UTF8, JobRow::name,
                        TextCodecs.STREAM_CODEC, JobRow::type,
                        TextCodecs.STREAM_CODEC, JobRow::trigger,
                        ByteBufCodecs.BOOL, JobRow::paused,
                        JobRow::new);
    }

    public static final CustomPacketPayload.Type<AutomationPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("jsc", "automation"));

    public static final StreamCodec<RegistryFriendlyByteBuf, AutomationPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.BOOL, AutomationPayload::engineOnline,
                    TextCodecs.STREAM_CODEC, AutomationPayload::engineLabel,
                    JobRow.STREAM_CODEC.apply(ByteBufCodecs.list(MAX_JOBS)), AutomationPayload::jobs,
                    ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list(MAX_FILES)), AutomationPayload::iqlFiles,
                    AutomationPayload::new);

    /* Copied on the way in, so what the client is handed cannot change under it after it arrives. */
    public AutomationPayload {
        jobs = List.copyOf(jobs);
        iqlFiles = List.copyOf(iqlFiles);
    }

    @Override
    public CustomPacketPayload.Type<AutomationPayload> type() {
        return TYPE;
    }
}
