/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.operation.payload.automation;

import dev.jstech.computers.blockentity.MainframeBlockEntity;
import dev.jstech.computers.client.os.AutomationManagerApp;
import dev.jstech.computers.operation.payload.AutomationPayload;
import dev.jstech.computers.operation.payload.ClientPayloadHandlers;
import dev.jstech.computers.operation.payload.ComputerAccess;
import dev.jstech.computers.operation.payload.CreateAutomationJobPayload;
import dev.jstech.computers.operation.payload.JobActionPayload;
import dev.jstech.computers.operation.payload.RequestAutomationPayload;
import dev.jstech.computers.os.fs.DiskFilesystem;
import dev.jstech.computers.os.fs.FileType;
import dev.jstech.computers.program.iql.IqlDefinition;
import dev.jstech.computers.program.iql.IqlDuration;
import dev.jstech.computers.program.iql.IqlSavedObject;
import java.util.Locale;
import java.util.Optional;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import java.util.ArrayList;
import java.util.List;

import static dev.jstech.computers.operation.payload.files.FileAccess.filesystemKindOf;
import static dev.jstech.computers.operation.payload.network.NetworkLookup.resolveMainframe;
import static dev.jstech.computers.operation.payload.terminal.TerminalHosts.niHost;

/**
 * The Automation Manager's payloads: the jobs on the Mainframe, creating one and acting on one.
 */
public final class AutomationPayloads {

    private AutomationPayloads() {
    }

    /** Registers the payloads this class handles. */
    public static void register(final PayloadRegistrar registrar) {
        ComputerAccess.accept(registrar, RequestAutomationPayload.TYPE, RequestAutomationPayload.STREAM_CODEC,
                ComputerAccess.machine(RequestAutomationPayload::host), AutomationPayloads::handleRequestAutomation);
        registrar.playToClient(AutomationPayload.TYPE, AutomationPayload.STREAM_CODEC,
                ClientPayloadHandlers.onMainThread(AutomationPayloads::handleAutomation));
        ComputerAccess.accept(registrar, CreateAutomationJobPayload.TYPE, CreateAutomationJobPayload.STREAM_CODEC,
                ComputerAccess.machine(CreateAutomationJobPayload::host), AutomationPayloads::handleCreateAutomationJob);
        ComputerAccess.accept(registrar, JobActionPayload.TYPE, JobActionPayload.STREAM_CODEC,
                ComputerAccess.machine(JobActionPayload::host), AutomationPayloads::handleJobAction);
    }

    private static void handleRequestAutomation(final RequestAutomationPayload payload, final ServerPlayer player,
                                                final ServerLevel level) {
        final var host = niHost(player, level, payload.host(), payload.monitorPos());
        if (host != null && host.networkUuid() != null) {
            PacketDistributor.sendToPlayer(player,
                    buildAutomation(resolveMainframe(level, host.networkUuid())));
        }
    }

    private static void handleAutomation(final AutomationPayload payload, final Player player) {
        AutomationManagerApp.accept(payload);
    }

    private static AutomationPayload buildAutomation(final MainframeBlockEntity mf) {
        if (mf == null) {
            return new AutomationPayload(false, "no Mainframe", List.of(), List.of());
        }
        final boolean online = mf.isAutomationEngineActive() || mf.isIqlEngineActive();
        final String label = mf.isAutomationEngineInstalled() ? "Automation Engine"
                : mf.isIqlEngineInstalled() ? "IQL Engine" : "none";
        final List<AutomationPayload.JobRow> rows = new ArrayList<>();
        for (final var job : mf.iqlCatalog().ofType(
                IqlDefinition.ObjectType.JOB)) {
            if (rows.size() >= AutomationPayload.MAX_JOBS) {
                break;
            }
            rows.add(new AutomationPayload.JobRow(job.name(), inferJobType(job.body(), job.triggerKind()),
                    triggerSummary(job.triggerKind(), job.triggerSpec()), mf.isJobPaused(job.name())));
        }
        // The .iql scripts saved on the Mainframe's system disk, so an IQL-Script job can pick one.
        final List<String> files = new ArrayList<>();
        final ItemStack sysDisk = mf.systemDisk();
        if (!sysDisk.isEmpty()) {
            for (final DiskFilesystem.FileEntry entry
                    : DiskFilesystem.list(sysDisk, "", filesystemKindOf(mf))) {
                if (entry.type() == FileType.IQL && files.size() < AutomationPayload.MAX_FILES) {
                    files.add(entry.path());
                }
            }
        }
        return new AutomationPayload(online, label, rows, files);
    }

    private static String inferJobType(final String body,
            final IqlDefinition.TriggerKind kind) {
        final String b = body.trim().toUpperCase(Locale.ROOT);
        if (b.startsWith("MOVE")) {
            return "Periodic Move";
        }
        if (b.startsWith("CRAFT")) {
            return kind == dev.jstech.computers.program.iql
                    .IqlDefinition.TriggerKind.WHEN ? "Keep Stock" : "Batch Craft";
        }
        return "Custom";
    }

    private static String triggerSummary(
            final IqlDefinition.TriggerKind kind,
            final String spec) {
        return switch (kind) {
            case EVERY -> "every " + spec;
            case WHEN -> spec;
            default -> "manual";
        };
    }

    private static void handleCreateAutomationJob(final CreateAutomationJobPayload payload, final ServerPlayer player,
                                                  final ServerLevel level) {
        final var host = niHost(player, level, payload.host(), payload.monitorPos());
        if (host == null || host.networkUuid() == null) {
            return;
        }
        final MainframeBlockEntity mf = resolveMainframe(level, host.networkUuid());
        if (mf == null) {
            return;
        }
        final var def = compileJob(player, mf, payload);
        if (def != null) {
            mf.iqlCatalog().put(
                    IqlSavedObject.from(def));
            mf.markIqlCatalogChanged();
            PacketDistributor.sendToPlayer(player, buildAutomation(mf));
        }
    }

    private static IqlDefinition compileJob(
            final ServerPlayer player, final MainframeBlockEntity mf, final CreateAutomationJobPayload p) {
        final String name = p.name().trim();
        if (name.isEmpty()) {
            jobError(player, "Give the job a name.");
            return null;
        }
        final String item = p.item().trim();
        final long amount = Math.max(1, p.amount());
        final var type = IqlDefinition.ObjectType.JOB;
        final var every = IqlDefinition.TriggerKind.EVERY;
        final var when = IqlDefinition.TriggerKind.WHEN;
        switch (p.jobType()) {
            case CreateAutomationJobPayload.TYPE_KEEP_STOCK -> {
                if (item.isEmpty()) {
                    jobError(player, "Keep Stock needs an item.");
                    return null;
                }
                return IqlDefinition.create(
                        type, name, "CRAFT " + amount + " " + item, when, "qty(" + item + ") < " + amount);
            }
            case CreateAutomationJobPayload.TYPE_BATCH_CRAFT -> {
                if (item.isEmpty() || !validInterval(p.interval())) {
                    jobError(player, "Batch Craft needs an item and a valid interval (e.g. 30s, 5m).");
                    return null;
                }
                return IqlDefinition.create(
                        type, name, "CRAFT " + amount + " " + item, every, p.interval().trim());
            }
            case CreateAutomationJobPayload.TYPE_PERIODIC_MOVE -> {
                final String from = p.from().trim();
                final String to = p.to().trim();
                if (from.isEmpty() || to.isEmpty() || !validInterval(p.interval())) {
                    jobError(player, "Periodic Move needs FROM, TO, and a valid interval (e.g. 30s).");
                    return null;
                }
                final String what = item.isEmpty() ? "*" : amount + " " + item;
                return IqlDefinition.create(
                        type, name, "MOVE " + what + " FROM " + from + " TO " + to, every, p.interval().trim());
            }
            case CreateAutomationJobPayload.TYPE_IQL_SCRIPT -> {
                // The chosen .iql filename rides in the item field; its content becomes the job body.
                if (item.isEmpty() || !validInterval(p.interval())) {
                    jobError(player, "An IQL Script job needs a .iql file and a valid interval (e.g. 30s).");
                    return null;
                }
                final ItemStack sysDisk = mf.systemDisk();
                final var content = sysDisk.isEmpty() ? Optional.<String>empty()
                        : DiskFilesystem.read(sysDisk, item);
                if (content.isEmpty() || content.get().isBlank()) {
                    jobError(player, "Script not found on the Mainframe disk: " + item);
                    return null;
                }
                return IqlDefinition.create(
                        type, name, content.get(), every, p.interval().trim());
            }
            default -> {
                return null;
            }
        }
    }

    private static boolean validInterval(final String spec) {
        try {
            return IqlDuration.toTicks(spec.trim()) > 0;
        } catch (final RuntimeException e) {
            return false;
        }
    }

    private static void jobError(final ServerPlayer player, final String message) {
        player.displayClientMessage(Component.literal(message), false);
    }

    private static void handleJobAction(final JobActionPayload payload, final ServerPlayer player,
                                        final ServerLevel level) {
        final var host = niHost(player, level, payload.host(), payload.monitorPos());
        if (host == null || host.networkUuid() == null) {
            return;
        }
        final MainframeBlockEntity mf = resolveMainframe(level, host.networkUuid());
        if (mf == null) {
            return;
        }
        switch (payload.action()) {
            case JobActionPayload.ACTION_PAUSE -> mf.pauseJob(payload.name());
            case JobActionPayload.ACTION_RESUME -> mf.restartJob(payload.name());
            case JobActionPayload.ACTION_DELETE -> {
                mf.iqlCatalog().remove(
                        IqlDefinition.ObjectType.JOB,
                        payload.name());
                mf.markIqlCatalogChanged();
            }
            default -> { }
        }
        PacketDistributor.sendToPlayer(player, buildAutomation(mf));
    }
}
