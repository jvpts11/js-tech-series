/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.hardware;

import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * An assembly of components on a motherboard, with a PSU.
 */
@TextHolder
public record ComputerBuild(MotherboardSpec motherboard,
                            List<CpuSpec> cpus,
                            List<IExpansionCardSpec> pcieCards,
                            List<RamSpec> rams,
                            PsuSpec psu,
                            List<DiskSpec> disks) {

    /* What keeps a build from being a working computer. */
    private static final TextKey NO_CPU = TextKey.of("jsc.build.no_cpu", "no CPU installed");
    private static final TextKey NO_RAM = TextKey.of("jsc.build.no_ram", "no RAM installed");
    private static final TextKey TOO_MANY_CPUS =
            TextKey.of("jsc.build.too_many_cpus", "too many CPUs: %s installed, %s sockets");
    private static final TextKey WRONG_SOCKET =
            TextKey.of("jsc.build.wrong_socket", "CPU socket %s does not fit board socket %s");
    private static final TextKey MIXED_ARCHITECTURES = TextKey.of("jsc.build.mixed_architectures",
            "CPU architecture %s does not match the %s of the other processors");
    private static final TextKey TOO_MANY_CARDS =
            TextKey.of("jsc.build.too_many_cards", "too many PCIe cards: %s installed, %s PCIe slots");
    private static final TextKey WRONG_BUS = TextKey.of("jsc.build.wrong_bus",
            "expansion card bus family %s is not compatible with board bus %s");
    private static final TextKey SOUND_CARD_ERA = TextKey.of("jsc.build.sound_card_era",
            "a sound card of the %s era does not sit on a board of the %s era");
    private static final TextKey TOO_MANY_SOUND_CARDS = TextKey.of("jsc.build.too_many_sound_cards",
            "one sound card per machine: %s installed");
    private static final TextKey TOO_MANY_RAM =
            TextKey.of("jsc.build.too_many_ram", "too many RAM modules: %s installed, %s slots");
    private static final TextKey WRONG_RAM =
            TextKey.of("jsc.build.wrong_ram", "RAM generation %s not accepted by board");
    private static final TextKey TOO_MANY_DISKS =
            TextKey.of("jsc.build.too_many_disks", "too many disks: %s installed, %s disk slots");
    private static final TextKey PSU_INSUFFICIENT =
            TextKey.of("jsc.build.psu_insufficient", "PSU insufficient: draw %sW exceeds %sW");

    public ComputerBuild {
        Objects.requireNonNull(motherboard, "motherboard must not be null");
        Objects.requireNonNull(psu, "psu must not be null");
        cpus = List.copyOf(cpus); // defensive copies, reject null elements
        pcieCards = List.copyOf(pcieCards);
        rams = List.copyOf(rams);
        disks = List.copyOf(disks);
    }

    public ComputerBuild(final MotherboardSpec motherboard, final List<CpuSpec> cpus,
                         final List<IExpansionCardSpec> pcieCards, final List<RamSpec> rams,
                         final PsuSpec psu) {
        this(motherboard, cpus, pcieCards, rams, psu, List.of());
    }

    /**
     * The architecture this machine runs, which is its processors'. A build with no processor has none, and
     * {@link #validate()} says so; past validation every processor here answers the same, because a build whose
     * processors disagree is refused there as well.
     */
    @Nullable
    public ArchitectureSpec architecture() {
        return this.cpus.isEmpty() ? null : this.cpus.get(0).architecture();
    }

    public List<GpuSpec> gpus() {
        final List<GpuSpec> out = new ArrayList<>();
        for (final IExpansionCardSpec card : pcieCards) {
            if (card instanceof GpuSpec gpu) {
                out.add(gpu);
            }
        }
        return out;
    }

    /**
     * The graphics memory this machine can actually use, in MB. A card newer than the slot it sits in
     * still works, but only at the older slot's bandwidth, so it delivers a fraction of what it holds:
     * a modern card in an ancient board runs, and runs badly, which is the honest outcome.
     *
     * <p>This is the single place the sum is computed, so every caller sees the same number.
     */
    public int effectiveVramMb() {
        double sum = 0;
        for (final GpuSpec gpu : gpus()) {
            sum += gpu.vramMb() * gpu.bus().bandwidthFactorIn(motherboard.pcieGeneration());
        }
        return (int) Math.round(sum);
    }

    /** Whether any card is seated in a slot older than itself, and so held below its rated speed. */
    public boolean hasBandwidthLimitedCard() {
        for (final IExpansionCardSpec card : pcieCards) {
            if (card.bus().bandwidthFactorIn(motherboard.pcieGeneration()) < 1.0) {
                return true;
            }
        }
        return false;
    }

    public List<IExpansionCardSpec> cardsOfKind(final ExpansionCardKind kind) {
        final List<IExpansionCardSpec> out = new ArrayList<>();
        for (final IExpansionCardSpec card : pcieCards) {
            if (card.kind() == kind) {
                out.add(card);
            }
        }
        return out;
    }

    public long totalCapacity() {
        long sum = 0L;
        for (final CpuSpec cpu : cpus) {
            sum += cpu.orchestrationCapacity();
        }
        return sum;
    }

    public int parallelQueues() {
        /*
         * One base CPU queue plus one extra parallel queue per installed GPU. GPUs are detected
         * the single canonical way, through gpus(), so this never drifts from the GPU accessor.
         */
        return 1 + gpus().size();
    }

    public long ramBuffer() {
        long sum = 0L;
        for (final RamSpec ram : rams) {
            sum += ram.bufferItems();
        }
        return sum;
    }

    public StorageTier fastestDiskTier() {
        StorageTier best = StorageTier.HDD;
        for (final DiskSpec disk : disks) {
            best = best.faster(disk.tier());
        }
        return best;
    }

    /**
     * The lowest staging latency across all installed RAM modules, in ticks. When no RAM is
     * installed this returns zero, and the caller already guards against an empty RAM list through
     * {@link #validate()}, so an empty list here means the build is invalid anyway.
     */
    public int bestRamLatencyTicks() {
        int best = Integer.MAX_VALUE;
        for (final RamSpec ram : rams) {
            best = Math.min(best, ram.latencyTicks());
        }
        return best == Integer.MAX_VALUE ? 0 : best;
    }

    public long totalStorageItems() {
        long sum = 0L;
        for (final DiskSpec disk : disks) {
            sum += disk.capacityItems();
        }
        return sum;
    }

    public long storageMb() {
        long sum = 0L;
        for (final DiskSpec disk : disks) {
            sum += disk.capacityMb();
        }
        return sum;
    }

    public int powerDraw() {
        int draw = 0;
        for (final CpuSpec cpu : cpus) {
            draw += cpu.tdpWatts();
        }
        for (final IExpansionCardSpec card : pcieCards) {
            draw += card.tdpWatts();
        }
        for (final RamSpec ram : rams) {
            draw += ram.tdpWatts();
        }
        for (final DiskSpec disk : disks) {
            draw += disk.tdpWatts();
        }
        return draw;
    }

    public BuildValidation validate() {
        final List<Text> problems = new ArrayList<>();

        if (cpus.isEmpty()) {
            problems.add(NO_CPU.text());
        }
        /*
         * Every computer needs RAM to do work: with a zero buffer the CPU has nothing to stage
         * through and can move nothing. A box without RAM is not a working computer.
         */
        if (rams.isEmpty()) {
            problems.add(NO_RAM.text());
        }
        if (cpus.size() > motherboard.cpuSlots()) {
            problems.add(TOO_MANY_CPUS.with(cpus.size(), motherboard.cpuSlots()));
        }
        for (final CpuSpec cpu : cpus) {
            if (!cpu.socket().equals(motherboard.socket())) {
                problems.add(WRONG_SOCKET.with(cpu.socket().display(), motherboard.socket().display()));
            }
        }
        /*
         * One machine, one instruction set. Two processors can share a socket and still understand different
         * instructions, and a program cannot run on half a machine, so a build that mixes them is not a computer.
         */
        if (!cpus.isEmpty()) {
            final ArchitectureSpec first = cpus.get(0).architecture();
            for (final CpuSpec cpu : cpus) {
                if (!cpu.architecture().equals(first)) {
                    problems.add(MIXED_ARCHITECTURES.with(cpu.architecture().name(), first.name()));
                    break;
                }
            }
        }

        if (pcieCards.size() > motherboard.pcieSlots()) {
            problems.add(TOO_MANY_CARDS.with(pcieCards.size(), motherboard.pcieSlots()));
        }
        for (final IExpansionCardSpec card : pcieCards) {
            if (!card.bus().compatibleWith(motherboard.pcieGeneration())) {
                problems.add(WRONG_BUS.with(card.bus().busFamily(), motherboard.pcieGeneration().busFamily()));
            }
            if (card instanceof SoundCardSpec sound && sound.era() != motherboard.era()) {
                problems.add(SOUND_CARD_ERA.with(sound.era().named(), motherboard.era().named()));
            }
        }
        final int soundCards = cardsOfKind(ExpansionCardKind.SOUND).size();
        if (soundCards > 1) {
            problems.add(TOO_MANY_SOUND_CARDS.with(soundCards));
        }

        if (rams.size() > motherboard.ramSlots()) {
            problems.add(TOO_MANY_RAM.with(rams.size(), motherboard.ramSlots()));
        }
        for (final RamSpec ram : rams) {
            if (!motherboard.acceptedRam().contains(ram.generation())) {
                problems.add(WRONG_RAM.with(ram.generation()));
            }
        }

        if (disks.size() > motherboard.diskSlots()) {
            problems.add(TOO_MANY_DISKS.with(disks.size(), motherboard.diskSlots()));
        }

        /*
         * An auto-scaling PSU dimensions its output to the build's draw, so it always satisfies the
         * power requirement; only a fixed-wattage PSU can come up short.
         */
        if (!psu.autoScaling()) {
            final int draw = powerDraw();
            if (draw > psu.wattage()) {
                problems.add(PSU_INSUFFICIENT.with(draw, psu.wattage()));
            }
        }

        return new BuildValidation(problems.isEmpty(), problems);
    }

    public boolean isPowered() {
        return validate().valid();
    }
}
