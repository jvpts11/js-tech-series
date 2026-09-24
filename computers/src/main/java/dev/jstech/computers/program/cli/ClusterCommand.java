/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.program.cli;

import dev.jstech.computers.blockentity.ClusterManagementComputerBlockEntity;
import dev.jstech.computers.blockentity.HbwInterfaceBlockEntity;
import dev.jstech.computers.blockentity.ServerRackBlockEntity;
import dev.jstech.computers.os.HostScope;
import dev.jstech.computers.os.OsDef;
import dev.jstech.computers.os.IOsHost;
import dev.jstech.computers.os.OsRegistry;
import dev.jstech.computers.rack.RackChassis;
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The shell's way into the clusters, for players who prefer a prompt or run a system with no desktop:
 * the same actions as the Cluster Manager, on the same machine only.
 *
 * <p>Its listings are tables whose columns are spaced where the line is read, once every cell is in the reader's
 * language, so a translated word that is longer or shorter still leaves the next column in line.
 */
@TextHolder
public final class ClusterCommand implements ICliCommand {

    /** The word it is typed as, which is also what its complaints open with. */
    private static final String NAME = "cluster";

    private static final TextKey SUMMARY = TextKey.of("jsc.cli.cluster.summary",
            "manage the supercomputers and datacenters on the network (Cluster Management Computer)");
    private static final TextKey USAGE = TextKey.of("jsc.cli.cluster.usage",
            "list | nodes <name> | power <name> [R:U] on|off | install <name> system|program | status | cancel");
    private static final TextKey POWER_USAGE =
            TextKey.of("jsc.cli.cluster.power_usage", "power <name> [R:U] on|off");
    private static final TextKey POWER_NODE_USAGE =
            TextKey.of("jsc.cli.cluster.power_node_usage", "power <name> R:U on|off");
    private static final TextKey INSTALL_USAGE =
            TextKey.of("jsc.cli.cluster.install_usage", "install <name> system|program");
    private static final TextKey ONLY_ON_CMC = TextKey.of("jsc.cli.cluster.only_on_cmc",
            "this command only exists on a Cluster Management Computer");
    private static final TextKey NO_CARD =
            TextKey.of("jsc.cli.cluster.no_card", "no cluster interface card installed");
    private static final TextKey CANCELLED =
            TextKey.of("jsc.cli.cluster.cancelled", "job cancelled after the nodes being written");
    private static final TextKey NO_JOB = TextKey.of("jsc.cli.cluster.no_job", "no job running");
    private static final TextKey LAST_JOB = TextKey.of("jsc.cli.cluster.last_job", "last job %s");
    private static final TextKey NONE_REACHED =
            TextKey.of("jsc.cli.cluster.none_reached", "no clusters reached on this network");
    private static final TextKey NO_CLUSTER = TextKey.of("jsc.cli.cluster.no_cluster", "no cluster named %s");
    private static final TextKey NO_NODE = TextKey.of("jsc.cli.cluster.no_node", "no node at %s");

    private static final TextKey COL_KIND = TextKey.of("jsc.cli.cluster.col.kind", "KIND");
    private static final TextKey COL_NAME = TextKey.of("jsc.cli.cluster.col.name", "NAME");
    private static final TextKey COL_STATE = TextKey.of("jsc.cli.cluster.col.state", "STATE");
    private static final TextKey COL_LOAD = TextKey.of("jsc.cli.cluster.col.load", "LOAD");
    private static final TextKey COL_RACK = TextKey.of("jsc.cli.cluster.col.rack", "RACK");
    private static final TextKey COL_UNIT = TextKey.of("jsc.cli.cluster.col.unit", "U");
    private static final TextKey COL_NODE = TextKey.of("jsc.cli.cluster.col.node", "NODE");
    private static final TextKey COL_SYSTEM = TextKey.of("jsc.cli.cluster.col.system", "SYSTEM");
    private static final TextKey COL_POWER = TextKey.of("jsc.cli.cluster.col.power", "POWER");

    /* Where each column of the two listings begins. */
    private static final int LIST_NAME_AT = 15;
    private static final int LIST_STATE_AT = 38;
    private static final int LIST_LOAD_AT = 47;
    private static final int NODES_UNIT_AT = 5;
    private static final int NODES_NODE_AT = 10;
    private static final int NODES_SYSTEM_AT = 29;
    private static final int NODES_POWER_AT = 44;

    private static final TextKey SUPERCOMPUTER = TextKey.of("jsc.cli.cluster.supercomputer", "supercomputer");
    private static final TextKey DATACENTER = TextKey.of("jsc.cli.cluster.datacenter", "datacenter");
    private static final TextKey ONLINE = TextKey.of("jsc.cli.cluster.online", "online");
    private static final TextKey OFFLINE = TextKey.of("jsc.cli.cluster.offline", "offline");
    private static final TextKey EMPTY = TextKey.of("jsc.cli.cluster.empty", "empty");
    private static final TextKey CRAFTS = TextKey.of("jsc.cli.cluster.crafts", "%s/%s crafts");
    private static final TextKey SERVERS = TextKey.of("jsc.cli.cluster.servers", "%s servers");
    private static final TextKey OUT_OF_REACH =
            TextKey.of("jsc.cli.cluster.out_of_reach", "  (out of this card's reach)");
    private static final TextKey NO_SYSTEM = TextKey.of("jsc.cli.cluster.no_system", "none");
    private static final TextKey ON = TextKey.of("jsc.cli.cluster.on", "on");
    private static final TextKey OFF = TextKey.of("jsc.cli.cluster.off", "off");

    private static final TextKey BAY_SWITCHED = TextKey.of("jsc.cli.cluster.bay_switched", "%s bay switched %s");
    private static final TextKey BAYS_SWITCHED =
            TextKey.of("jsc.cli.cluster.bays_switched", "%s bays switched %s");
    private static final TextKey ALREADY = TextKey.of("jsc.cli.cluster.already", "already %s");
    private static final TextKey STARTED = TextKey.of("jsc.cli.cluster.started",
            "%s · %s at a time; follow it with 'cluster status'");
    private static final TextKey JOB = TextKey.of("jsc.cli.cluster.job",
            "job: install %s · %s done · %s writing · %s queued · %s skipped");
    private static final TextKey JOB_CANCELLING = TextKey.of("jsc.cli.cluster.job_cancelling",
            "job: install %s · %s done · %s writing · %s queued · %s skipped · cancelling");

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public CommandGroup group() {
        return CommandGroup.NETWORK;
    }

    @Override
    public Text summary() {
        return SUMMARY.text();
    }

    @Override
    public Text usage() {
        return USAGE.text();
    }

    @Override
    public CommandScope scope() {
        return CommandScope.everywhere().onHost(HostScope.CLUSTER_MANAGEMENT_COMPUTER);
    }

    @Override
    public void run(final CliContext ctx) {
        if (!(ctx.computer().hostBlock() instanceof ClusterManagementComputerBlockEntity cmc)) {
            ctx.out().error(CliTexts.SAID_BY.with(name(), ONLY_ON_CMC));
            return;
        }
        if (cmc.clusterCard() == null) {
            ctx.out().error(CliTexts.SAID_BY.with(name(), NO_CARD));
            return;
        }
        final String sub = ctx.arg(0).toLowerCase(Locale.ROOT);
        switch (sub) {
            case "list", "ls", "" -> list(ctx, cmc);
            case "nodes" -> nodes(ctx, cmc, ctx.arg(1));
            case "power" -> power(ctx, cmc);
            case "install" -> install(ctx, cmc);
            case "status" -> status(ctx, cmc);
            case "cancel" -> ctx.out().info(cmc.cancelJob() ? CANCELLED : NO_JOB);
            default -> ctx.out().error(CliTexts.USAGE.with(name(), usage()));
        }
    }

    /** A named cluster on the machine's network: supercomputers by interface name, sections by label. */
    private record Named(String name, ClusterManagementComputerBlockEntity.ClusterRef ref, Text state, Text load) {
    }

    private static List<Named> clusters(final ClusterManagementComputerBlockEntity cmc) {
        final List<Named> out = new ArrayList<>();
        int i = 1;
        for (final HbwInterfaceBlockEntity hub : cmc.supercomputers()) {
            // Unnamed supercomputers are numbered in the order the network registered their interfaces.
            final String name = ClusterManagementComputerBlockEntity.supercomputerName(hub, i - 1);
            out.add(new Named(name, new ClusterManagementComputerBlockEntity.ClusterRef(
                    RackChassis.RackType.SUPERCOMPUTER, hub.getBlockPos(), null),
                    hub.clusterOnline() ? ONLINE.text() : OFFLINE.text(),
                    CRAFTS.with(hub.craftSlotsInUse(), hub.parallelCrafts())));
            i++;
        }
        for (final ClusterManagementComputerBlockEntity.SectionRef section : cmc.datacenterSections()) {
            out.add(new Named(section.label(), new ClusterManagementComputerBlockEntity.ClusterRef(
                    RackChassis.RackType.SERVER, section.routerPos(), section.face()),
                    section.section().serverCount() > 0 ? ONLINE.text() : EMPTY.text(),
                    SERVERS.with(section.section().serverCount())));
        }
        return out;
    }

    private static Named find(final ClusterManagementComputerBlockEntity cmc, final String name) {
        for (final Named c : clusters(cmc)) {
            if (c.name().equalsIgnoreCase(name)) {
                return c;
            }
        }
        return null;
    }

    private static void list(final CliContext ctx, final ClusterManagementComputerBlockEntity cmc) {
        final List<Named> all = clusters(cmc);
        if (all.isEmpty()) {
            ctx.out().dim(NONE_REACHED);
            return;
        }
        ctx.out().line(CliLine.of(CliSpan.of(COL_KIND, CliStyle.HEADER), CliSpan.pad(LIST_NAME_AT),
                CliSpan.of(COL_NAME, CliStyle.HEADER), CliSpan.pad(LIST_STATE_AT),
                CliSpan.of(COL_STATE, CliStyle.HEADER), CliSpan.pad(LIST_LOAD_AT),
                CliSpan.of(COL_LOAD, CliStyle.HEADER)));
        for (final Named c : all) {
            final Text kind = c.ref().kind() == RackChassis.RackType.SUPERCOMPUTER
                    ? SUPERCOMPUTER.text() : DATACENTER.text();
            final CliLine.Builder row = CliLine.build().add(CliLine.of(CliSpan.plain(kind),
                    CliSpan.pad(LIST_NAME_AT), CliSpan.plain(c.name()), CliSpan.pad(LIST_STATE_AT),
                    CliSpan.plain(c.state()), CliSpan.pad(LIST_LOAD_AT), CliSpan.plain(c.load())));
            if (!cmc.reaches(c.ref().kind())) {
                row.plain(OUT_OF_REACH.text());
            }
            ctx.out().line(row.done());
        }
    }

    private static void nodes(final CliContext ctx, final ClusterManagementComputerBlockEntity cmc, final String name) {
        final Named c = find(cmc, name);
        if (c == null) {
            ctx.out().error(CliTexts.SAID_BY.with(NAME, NO_CLUSTER.with(name)));
            return;
        }
        ctx.out().line(CliLine.of(CliSpan.of(COL_RACK, CliStyle.HEADER), CliSpan.pad(NODES_UNIT_AT),
                CliSpan.of(COL_UNIT, CliStyle.HEADER), CliSpan.pad(NODES_NODE_AT),
                CliSpan.of(COL_NODE, CliStyle.HEADER), CliSpan.pad(NODES_SYSTEM_AT),
                CliSpan.of(COL_SYSTEM, CliStyle.HEADER), CliSpan.pad(NODES_POWER_AT),
                CliSpan.of(COL_POWER, CliStyle.HEADER)));
        int rackIndex = 0;
        BlockPos lastRack = null;
        for (final ClusterManagementComputerBlockEntity.NodeRef node : cmc.nodesOf(c.ref())) {
            if (!node.rack().equals(lastRack)) {
                rackIndex++;
                lastRack = node.rack();
            }
            if (!(cmc.getLevel().getBlockEntity(node.rack()) instanceof ServerRackBlockEntity rack)) {
                continue;
            }
            final IOsHost host = rack.unitHost(node.row());
            final ResourceLocation osId = host.installedOsId();
            final OsDef os = osId == null ? null : OsRegistry.getOs(osId);
            /* The rack and the unit are figures, the same in every language, so they keep their fixed widths. */
            ctx.out().line(CliLine.of(
                    CliSpan.plain(Text.literal(String.format(Locale.ROOT, "R%-2d U%-4d ", rackIndex, node.row() + 1))),
                    CliSpan.plain(ClusterManagementComputerBlockEntity.nodeName(rack, node.row())),
                    CliSpan.pad(NODES_SYSTEM_AT),
                    CliSpan.plain(os == null ? NO_SYSTEM.text() : Text.literal(os.displayName())),
                    CliSpan.pad(NODES_POWER_AT),
                    CliSpan.plain(rack.bayPowerOn(node.row()) ? ON.text() : OFF.text())));
        }
    }

    private static void power(final CliContext ctx, final ClusterManagementComputerBlockEntity cmc) {
        final Named c = find(cmc, ctx.arg(1));
        if (c == null) {
            ctx.out().error(CliTexts.SAID_BY.with(NAME, NO_CLUSTER.with(ctx.arg(1))));
            return;
        }
        final String last = ctx.arg(ctx.argCount() - 1).toLowerCase(Locale.ROOT);
        final boolean on = last.equals("on");
        final Text switched = on ? ON.text() : OFF.text();
        if (!on && !last.equals("off")) {
            ctx.out().error(CliTexts.USAGE.with(NAME, POWER_USAGE));
            return;
        }
        if (ctx.argCount() >= 4) {
            // R:U addresses one node: rack index in cluster order, unit row from 1.
            final String[] parts = ctx.arg(2).toUpperCase(Locale.ROOT).replace("R", "").replace("U", "").split(":");
            try {
                final int rackIndex = Integer.parseInt(parts[0]);
                final int row = Integer.parseInt(parts[1]) - 1;
                int index = 0;
                BlockPos lastRack = null;
                for (final ClusterManagementComputerBlockEntity.NodeRef node : cmc.nodesOf(c.ref())) {
                    if (!node.rack().equals(lastRack)) {
                        index++;
                        lastRack = node.rack();
                    }
                    if (index == rackIndex && node.row() == row) {
                        final ServerRackBlockEntity rack =
                                (ServerRackBlockEntity) cmc.getLevel().getBlockEntity(node.rack());
                        if (rack != null && rack.bayPowerOn(row) != on && cmc.toggleNode(node.rack(), row)) {
                            ctx.out().ok(BAY_SWITCHED.with(ClusterManagementComputerBlockEntity.nodeName(rack, row),
                                    switched));
                        } else {
                            ctx.out().dim(ALREADY.with(switched));
                        }
                        return;
                    }
                }
                ctx.out().error(CliTexts.SAID_BY.with(NAME, NO_NODE.with(ctx.arg(2))));
            } catch (final RuntimeException badAddress) {
                ctx.out().error(CliTexts.USAGE.with(NAME, POWER_NODE_USAGE));
            }
            return;
        }
        final int changed = cmc.powerAll(c.ref(), on);
        ctx.out().ok(changed == 1 ? BAY_SWITCHED.with(changed, switched) : BAYS_SWITCHED.with(changed, switched));
    }

    private static void install(final CliContext ctx, final ClusterManagementComputerBlockEntity cmc) {
        final Named c = find(cmc, ctx.arg(1));
        if (c == null) {
            ctx.out().error(CliTexts.SAID_BY.with(NAME, NO_CLUSTER.with(ctx.arg(1))));
            return;
        }
        final String what = ctx.arg(2).toLowerCase(Locale.ROOT);
        final ClusterManagementComputerBlockEntity.JobKind kind;
        if (what.equals("system")) {
            kind = ClusterManagementComputerBlockEntity.JobKind.SYSTEM;
        } else if (what.equals("program")) {
            kind = ClusterManagementComputerBlockEntity.JobKind.PROGRAM;
        } else {
            ctx.out().error(CliTexts.USAGE.with(NAME, INSTALL_USAGE));
            return;
        }
        final String result = cmc.startJob(c.ref(), kind);
        if (cmc.job() != null) {
            ctx.out().ok(STARTED.with(result, cmc.parallelLanes()));
        } else {
            ctx.out().error(CliTexts.SAID_BY.with(NAME,result));
        }
    }

    private static void status(final CliContext ctx, final ClusterManagementComputerBlockEntity cmc) {
        final ClusterManagementComputerBlockEntity.InstallJob job = cmc.job();
        if (job == null) {
            ctx.out().dim(cmc.lastJobSummary().isEmpty() ? NO_JOB.text() : LAST_JOB.with(cmc.lastJobSummary()));
            return;
        }
        ctx.out().info((job.cancelled() ? JOB_CANCELLING : JOB).with(job.medium().label(), job.done(),
                job.lanes().size(), job.queued(), job.skipped()));
        for (final ClusterManagementComputerBlockEntity.Lane lane : job.lanes()) {
            ctx.out().line("  " + lane.name() + " ... " + (lane.permille() / 10) + "%");
        }
    }

}
