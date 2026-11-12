/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.teracoin;

import dev.jstech.computers.blockentity.AbstractComputerBlockEntity;
import dev.jstech.computers.blockentity.ServerServices;
import dev.jstech.computers.hardware.ComputerBuild;
import dev.jstech.computers.hardware.CpuSpec;
import dev.jstech.computers.os.IOsHost;
import dev.jstech.computers.program.Programs;
import dev.jstech.computers.program.cli.CliLine;
import dev.jstech.computers.program.cli.CliSpan;
import dev.jstech.computers.program.cli.CliStyle;
import dev.jstech.computers.registry.HardwareRegistry;
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextKey;
import dev.jstech.core.time.GameCalendar;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * A computer's miner as its prompt reaches it (computers/docs/design/teracoin.md, "Mining"): its settings, the
 * processors and cards it hashes with, the chain's line, and how it is doing, alone or for its pool. A miner without
 * a pool shows its own figures, like a real miner's console.
 */
public final class MinerConsole {

    private final IOsHost host;
    private final ServerLevel level;

    /* The column a status line's value starts at, and a device line's hashrate. */
    private static final int LABEL = 9;
    private static final int ROW_VALUE = 38;
    private static final long SECONDS_PER_HOUR = 3_600L;

    public MinerConsole(final IOsHost host, final ServerLevel level) {
        this.host = host;
        this.level = level;
    }

    /** The miner's settings, kept with the machine. */
    public MinerSettings settings() {
        return host.console().miner();
    }

    /** The settings changed: the machine is saved with them. */
    public void changed() {
        host.markChanged();
    }

    /** Whether the machine has the miner installed. */
    public boolean installed() {
        return host.console() != null && host.console().isInstalled(Programs.MINER.toString());
    }

    /** The machine, as the miners tell each other apart. */
    public String machine() {
        return MinerRunner.machineKey(host, null, null);
    }

    /** How the miner and the chain stand now. */
    public Snapshot snapshot() {
        final TeracoinChain chain = TeracoinState.of(level.getServer()).chain();
        final ComputerBuild build = host.currentBuild();
        final long hashes = build == null ? 0L : TeracoinHashrate.of(build, onRisers());
        final MinerSettings miner = settings();
        return new Snapshot(miner.status(), hashes, build == null ? 0 : build.powerDraw(), chain.height(),
                chain.difficulty(worldDays()), chain.nextReward(), miner.poolShown(), miner.poolHost(),
                miner.feePermille(), miner.address(), chain.latestBlocks(1));
    }

    /** The pool the miner would mine for now, or null for one mining alone. */
    @Nullable
    public ServerServices.Host pool() {
        final MinerSettings miner = settings();
        return miner.alone() ? null : MiningPools.chosen(level, host.networkUuid(), miner.pool());
    }

    /** The line that says whom it mines for and whom it pays. */
    public CliLine whom() {
        final MinerSettings miner = settings();
        final String pays = TeracoinAddress.shortForm(miner.address());
        if (miner.alone()) {
            return new CliLine(MinerTexts.ALONE_CHOSEN.with(pays), CliStyle.PLAIN);
        }
        final ServerServices.Host pool = pool();
        if (pool == null) {
            return miner.pool().isEmpty() ? new CliLine(MinerTexts.ALONE_NO_POOL.with(pays), CliStyle.PLAIN)
                    : new CliLine(MinerTexts.NO_POOL.with(miner.pool()), CliStyle.ERROR);
        }
        return new CliLine(MinerTexts.FOR_POOL.with(MiningPools.name(pool), MiningPools.server(pool),
                percent(MiningPools.state(pool).feePermille()), pays), CliStyle.PLAIN);
    }

    /** One line for each processor and card it hashes with, and its hashrate. */
    public List<CliLine> devices() {
        final List<CliLine> out = new ArrayList<>();
        final ComputerBuild build = host.currentBuild();
        if (build == null) {
            return out;
        }
        final List<String> names = processorNames();
        for (int i = 0; i < build.cpus().size(); i++) {
            final CpuSpec cpu = build.cpus().get(i);
            final String name = i < names.size() ? names.get(i) : "";
            out.add(row(MinerTexts.CPU_ROW.with(MinerTexts.CORES.with(name, cpu.cores())),
                    TeracoinHashrate.format(cpu.orchestrationCapacity())));
        }
        final List<Long> cards = TeracoinHashrate.cards(build, onRisers());
        for (int i = 0; i < cards.size(); i++) {
            final TextKey row = onRisers() ? MinerTexts.GPU_RISER_ROW : MinerTexts.GPU_ROW;
            out.add(row(row.with(i, i + 1), TeracoinHashrate.format(cards.get(i))));
        }
        return out;
    }

    /** The miner's status, as {@code miner status} prints it. */
    public List<CliLine> status() {
        final List<CliLine> out = new ArrayList<>();
        final MinerSettings miner = settings();
        final Snapshot now = snapshot();
        final ComputerBuild build = host.currentBuild();
        switch (miner.status()) {
            case POOL -> out.add(labelled(MinerTexts.LABEL_POOL, MinerTexts.STATUS_POOL.with(now.pool(),
                    now.poolHost(), percent(now.feePermille())), CliStyle.INFO));
            case ALONE -> out.add(labelled(MinerTexts.LABEL_POOL, MinerTexts.STATUS_ALONE.text(), CliStyle.INFO));
            default -> out.add(stall(miner.status(), now.pool()));
        }
        out.add(labelled(MinerTexts.LABEL_PAYS, Text.literal(TeracoinAddress.shortForm(miner.address())),
                CliStyle.PLAIN));
        if (build != null) {
            final List<Long> cards = TeracoinHashrate.cards(build, onRisers());
            long cardsTotal = 0L;
            boolean same = true;
            for (final long card : cards) {
                cardsTotal += card;
                same &= card == cards.get(0);
            }
            if (cards.size() > 1 && same) {
                out.add(labelled(MinerTexts.LABEL_GPUS.with(cards.size() - 1), MinerTexts.STATUS_EACH.with(
                        TeracoinHashrate.format(cards.get(0)), TeracoinHashrate.format(cardsTotal)), CliStyle.PLAIN));
            } else {
                for (int i = 0; i < cards.size(); i++) {
                    out.add(labelled(MinerTexts.LABEL_GPU.with(i), Text.literal(TeracoinHashrate.format(cards.get(i))),
                            CliStyle.PLAIN));
                }
            }
            out.add(labelled(MinerTexts.LABEL_CPU, Text.literal(TeracoinHashrate.format(
                    TeracoinHashrate.processors(build))), CliStyle.PLAIN));
        }
        out.add(labelled(MinerTexts.LABEL_TOTAL, MinerTexts.STATUS_TOTAL.with(TeracoinHashrate.format(now.hashrate()),
                String.format(Locale.ROOT, "%,d", now.watts())), CliStyle.OK));
        if (miner.status() == MinerSettings.Status.POOL) {
            poolLines(out, miner.address());
        } else if (now.hashrate() > 0L) {
            out.add(labelled(MinerTexts.LABEL_EXPECTED, MinerTexts.STATUS_EXPECTED.with(duration(
                    now.difficulty() / Math.max(1L, now.hashrate()))), CliStyle.PLAIN));
        }
        return out;
    }

    /** The line of a miner that cannot mine, for {@code why}. */
    public CliLine stall(final MinerSettings.Status why, final String poolName) {
        return switch (why) {
            case NO_ADDRESS -> new CliLine(MinerTexts.NO_ADDRESS.text(), CliStyle.ERROR);
            case NO_POOL -> new CliLine(MinerTexts.NO_POOL.with(poolName), CliStyle.ERROR);
            case WAITING_FOR_OWNER -> new CliLine(MinerTexts.WAITING.with(poolName), CliStyle.WARN);
            case NOTHING_HASHES -> new CliLine(MinerTexts.NOTHING_HASHES.text(), CliStyle.ERROR);
            default -> new CliLine(MinerTexts.NOT_RUNNING.text(), CliStyle.DIM);
        };
    }

    /** The world's clock now, as the miner stamps its lines: 11:58:02. */
    public String clock() {
        final long dayTime = level.getServer().overworld().getDayTime();
        final long seconds = Math.floorMod(dayTime, GameCalendar.HOUR_TICKS) * 60L * 60L / GameCalendar.HOUR_TICKS;
        return String.format(Locale.ROOT, "%02d:%02d:%02d", GameCalendar.hourOf(dayTime),
                GameCalendar.minuteOf(dayTime), seconds % 60L);
    }

    /** How long {@code seconds} is, in the largest unit that reads well. */
    public static Text duration(final long seconds) {
        if (seconds < 2L * SECONDS_PER_HOUR) {
            final long minutes = Math.max(1L, seconds / 60L);
            return minutes == 1L ? MinerTexts.MINUTE.text() : MinerTexts.MINUTES.with(minutes);
        }
        if (seconds < 3L * 24L * SECONDS_PER_HOUR) {
            return MinerTexts.HOURS.with(seconds / SECONDS_PER_HOUR);
        }
        return MinerTexts.DAYS.with(seconds / (24L * SECONDS_PER_HOUR));
    }

    /** A fee in thousandths, as a percentage: 1%, 2.5%. */
    public static String percent(final int permille) {
        return permille % 10 == 0 ? permille / 10 + "%" : String.format(Locale.ROOT, "%.1f%%", permille / 10.0);
    }

    private void poolLines(final List<CliLine> out, final String address) {
        final ServerServices.Host host = pool();
        if (host == null) {
            return;
        }
        final MiningPool pool = MiningPools.state(host);
        final long now = level.getServer().overworld().getGameTime();
        if (pool.roundStart() >= 0L) {
            out.add(labelled(MinerTexts.LABEL_ROUND, MinerTexts.STATUS_ROUND.with(duration(
                    (now - pool.roundStart()) / 20L)), CliStyle.PLAIN));
        }
        for (final MiningPool.RoundShare share : pool.roundShares()) {
            if (share.address().equals(address)) {
                out.add(labelled(MinerTexts.LABEL_SHARE, MinerTexts.STATUS_SHARE.with(
                        String.format(Locale.ROOT, "%.1f%%", share.permille() / 10.0)), CliStyle.PLAIN));
            }
        }
        final TeracoinChain chain = TeracoinState.of(level.getServer()).chain();
        final String name = MiningPools.name(host);
        for (final LedgerEntry entry : chain.history(address, 32)) {
            if (entry.kind() == LedgerEntry.Kind.PAYOUT && entry.label().equals(name)) {
                final TextKey paid = entry.pending() ? MinerTexts.STATUS_PAID_WAITING : MinerTexts.STATUS_PAID;
                out.add(labelled(MinerTexts.LABEL_PAID, paid.with(Trc.format(entry.amount(), 2),
                        String.format(Locale.ROOT, "%,d", entry.block())), CliStyle.WARN));
                break;
            }
        }
    }

    private boolean onRisers() {
        return host instanceof AbstractComputerBlockEntity computer && computer.cardsOnRisers();
    }

    private long worldDays() {
        return level.getServer().overworld().getDayTime() / GameCalendar.DAY_TICKS;
    }

    /* The processors' names, as their items call them, in the order they sit. */
    private List<String> processorNames() {
        final List<String> out = new ArrayList<>();
        for (final ItemStack stack : host.hardwareStacks()) {
            if (HardwareRegistry.cpu(stack) != null) {
                out.add(stack.getHoverName().getString());
            }
        }
        return out;
    }

    private static CliLine row(final Text left, final String right) {
        return CliLine.of(CliSpan.plain("  "), CliSpan.plain(left), CliSpan.pad(ROW_VALUE), CliSpan.plain(right));
    }

    private static CliLine labelled(final TextKey label, final Text value, final CliStyle style) {
        return labelled(label.text(), value, style);
    }

    private static CliLine labelled(final Text label, final Text value, final CliStyle style) {
        return CliLine.of(new CliSpan(label, CliStyle.INFO), CliSpan.pad(LABEL), new CliSpan(value, style));
    }

    /**
     * How the miner and the chain stand: its status and hashrate, what the machine draws, the chain's height,
     * difficulty and reward, the pool it mines for, the address it pays, and the newest block.
     */
    public record Snapshot(MinerSettings.Status status, long hashrate, int watts, int height, long difficulty,
                           long reward, String pool, String poolHost, int feePermille, String address,
                           List<FoundBlock> newest) {

        public Snapshot {
            newest = List.copyOf(newest);
        }
    }
}
