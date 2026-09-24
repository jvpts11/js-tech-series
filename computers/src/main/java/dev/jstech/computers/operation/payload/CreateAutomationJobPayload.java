/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.operation.payload;

import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Client to server: the Automation Manager's "New job" form. The server compiles the fields into a saved
 * IQL job (so no IQL is typed by the player): Keep Stock becomes {@code WHEN qty(item) < amount AS CRAFT
 * amount item}, Batch Craft becomes {@code EVERY interval AS CRAFT amount item}, and Periodic Move becomes
 * {@code EVERY interval AS MOVE amount item FROM from TO to}. Fields not used by a type are ignored.
 */
@TextHolder
public record CreateAutomationJobPayload(BlockPos host, BlockPos monitorPos, int jobType, String name,
                                         String item, long amount, String from, String to, String interval)
        implements CustomPacketPayload {

    public static final int TYPE_KEEP_STOCK = 0;
    public static final int TYPE_BATCH_CRAFT = 1;
    public static final int TYPE_PERIODIC_MOVE = 2;
    /** Runs a stored .iql script (its name in {@code item}) every {@code interval}. */
    public static final int TYPE_IQL_SCRIPT = 3;

    // Why a form did not make a job.
    public static final TextKey NEEDS_NAME = TextKey.of("jsc.automation.error.needs_name", "Give the job a name.");
    public static final TextKey KEEP_STOCK_NEEDS_ITEM =
            TextKey.of("jsc.automation.error.keep_stock_needs_item", "Keep Stock needs an item.");
    public static final TextKey BATCH_CRAFT_NEEDS = TextKey.of("jsc.automation.error.batch_craft_needs",
            "Batch Craft needs an item and a valid interval (e.g. 30s, 5m).");
    public static final TextKey PERIODIC_MOVE_NEEDS = TextKey.of("jsc.automation.error.periodic_move_needs",
            "Periodic Move needs FROM, TO, and a valid interval (e.g. 30s).");
    public static final TextKey SCRIPT_JOB_NEEDS = TextKey.of("jsc.automation.error.script_job_needs",
            "An IQL Script job needs a .iql file and a valid interval (e.g. 30s).");
    public static final TextKey SCRIPT_NOT_FOUND =
            TextKey.of("jsc.automation.error.script_not_found", "Script not found on the Mainframe disk: %s");

    /*
     * What each field may hold. They arrive from a client and are kept in a job every viewer is sent again, so
     * each is held to a size a form fills in: a name, an item or script, a bus or server, and a period.
     */
    private static final int NAME_MOST = 64;
    private static final int ITEM_MOST = 128;
    private static final int PLACE_MOST = 64;
    private static final int INTERVAL_MOST = 32;

    public static final CustomPacketPayload.Type<CreateAutomationJobPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("jsc", "create_automation_job"));

    public static final StreamCodec<RegistryFriendlyByteBuf, CreateAutomationJobPayload> STREAM_CODEC =
            StreamCodec.of(CreateAutomationJobPayload::encode, CreateAutomationJobPayload::decode);

    public CreateAutomationJobPayload {
        name = PayloadText.clip(name, NAME_MOST);
        item = PayloadText.clip(item, ITEM_MOST);
        from = PayloadText.clip(from, PLACE_MOST);
        to = PayloadText.clip(to, PLACE_MOST);
        interval = PayloadText.clip(interval, INTERVAL_MOST);
    }

    private static void encode(final RegistryFriendlyByteBuf buf, final CreateAutomationJobPayload p) {
        BlockPos.STREAM_CODEC.encode(buf, p.host);
        BlockPos.STREAM_CODEC.encode(buf, p.monitorPos);
        buf.writeVarInt(p.jobType);
        buf.writeUtf(p.name, NAME_MOST);
        buf.writeUtf(p.item, ITEM_MOST);
        buf.writeVarLong(p.amount);
        buf.writeUtf(p.from, PLACE_MOST);
        buf.writeUtf(p.to, PLACE_MOST);
        buf.writeUtf(p.interval, INTERVAL_MOST);
    }

    private static CreateAutomationJobPayload decode(final RegistryFriendlyByteBuf buf) {
        final BlockPos host = BlockPos.STREAM_CODEC.decode(buf);
        final BlockPos monitorPos = BlockPos.STREAM_CODEC.decode(buf);
        final int type = buf.readVarInt();
        final String name = buf.readUtf(NAME_MOST);
        final String item = buf.readUtf(ITEM_MOST);
        final long amount = buf.readVarLong();
        final String from = buf.readUtf(PLACE_MOST);
        final String to = buf.readUtf(PLACE_MOST);
        final String interval = buf.readUtf(INTERVAL_MOST);
        return new CreateAutomationJobPayload(host, monitorPos, type, name, item, amount, from, to, interval);
    }

    @Override
    public CustomPacketPayload.Type<CreateAutomationJobPayload> type() {
        return TYPE;
    }
}
