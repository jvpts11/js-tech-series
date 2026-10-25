/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.hardware;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.jstech.core.jstechseries.tier.Era;
import dev.jstech.core.jstechseries.tier.EraCodecs;
import dev.jstech.core.text.Text;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

/**
 * The sheets of the parts and the kinds hardware is made of, as data: what a modpack's startup script writes as an
 * object to declare a part of its own, read field by field under the names written here. A kind already declared is
 * named by its id, a bare name being one of this mod's own: {@code era: 'legacy'}, {@code generation: 'ddr4'}.
 */
public final class HardwareCodecs {

    public static final Codec<Era> ERA = EraCodecs.CODEC;
    public static final Codec<RamGeneration> MEMORY_GENERATION =
            byId("memory generation", RamGeneration::byId, RamGeneration::id);
    public static final Codec<PcieGeneration> SLOT_GENERATION =
            byId("slot generation", PcieGeneration::byId, PcieGeneration::id);
    public static final Codec<FormFactor> FORM_FACTOR = byId("form factor", FormFactor::byId, FormFactor::id);
    public static final Codec<ExpansionCardKind> CARD_KIND =
            byId("kind of card", ExpansionCardKind::byId, ExpansionCardKind::id);
    /** An instruction set by its id or by the name it is written under, as a player types it: "x86-64". */
    public static final Codec<IsaSpec> ISA = Codec.STRING.comapFlatMap(name -> Isas.find(name)
            .map(DataResult::success).orElseGet(() -> DataResult.error(() -> "no instruction set is " + name)),
            IsaSpec::id);
    public static final Codec<CpuSocketId> SOCKET = Codec.STRING.xmap(
            id -> new CpuSocketId(id.indexOf(':') < 0 ? HardwareIds.own(id) : id), CpuSocketId::id);
    public static final Codec<StorageTier> STORAGE_TIER = Codec.STRING.comapFlatMap(HardwareCodecs::tier,
            tier -> tier.name().toLowerCase(Locale.ROOT));
    public static final Codec<ExpansionBus> BUS_FAMILY = Codec.STRING.comapFlatMap(HardwareCodecs::family,
            family -> family.name().toLowerCase(Locale.ROOT));
    /** A design of cores this mod knows by its id, or one written out in full. */
    public static final Codec<Microarchitecture> MICROARCHITECTURE = Codec.withAlternative(
            Codec.STRING.comapFlatMap(HardwareCodecs::knownDesign, Microarchitecture::id),
            RecordCodecBuilder.create(i -> i.group(
                    Codec.STRING.fieldOf("id").forGetter(Microarchitecture::id),
                    Codec.STRING.fieldOf("name").forGetter(Microarchitecture::name),
                    Codec.INT.fieldOf("efficiency_milli").forGetter(Microarchitecture::efficiencyMilli),
                    Codec.INT.optionalFieldOf("cores_per_unit", 1).forGetter(Microarchitecture::coresPerUnit)
            ).apply(i, Microarchitecture::new)));
    public static final Codec<IntegratedGraphics> INTEGRATED_GRAPHICS = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.fieldOf("model").forGetter(IntegratedGraphics::model),
            Codec.INT.fieldOf("units").forGetter(IntegratedGraphics::units),
            Codec.INT.fieldOf("mhz").forGetter(IntegratedGraphics::mhz)
    ).apply(i, IntegratedGraphics::new));
    public static final Codec<CpuDesign> CPU_DESIGN = RecordCodecBuilder.create(i -> i.group(
            MICROARCHITECTURE.fieldOf("arch").forGetter(CpuDesign::arch),
            Codec.STRING.optionalFieldOf("codename", "").forGetter(CpuDesign::codename),
            Codec.BOOL.optionalFieldOf("smt", false).forGetter(CpuDesign::smt),
            Codec.INT.optionalFieldOf("efficiency_cores", 0).forGetter(CpuDesign::efficiencyCores),
            Codec.INT.optionalFieldOf("efficiency_mhz", 0).forGetter(CpuDesign::efficiencyMhz),
            MICROARCHITECTURE.optionalFieldOf("efficiency_arch", Microarchitectures.UNSPECIFIED)
                    .forGetter(CpuDesign::efficiencyArch),
            INTEGRATED_GRAPHICS.optionalFieldOf("graphics", IntegratedGraphics.NONE).forGetter(CpuDesign::graphics)
    ).apply(i, CpuDesign::new));
    public static final Codec<CpuSpec> PROCESSOR = RecordCodecBuilder.create(i -> i.group(
            ERA.fieldOf("era").forGetter(CpuSpec::era),
            ISA.fieldOf("isa").forGetter(CpuSpec::isa),
            SOCKET.fieldOf("socket").forGetter(CpuSpec::socket),
            Codec.INT.fieldOf("cores").forGetter(CpuSpec::cores),
            Codec.INT.fieldOf("freq_mhz").forGetter(CpuSpec::freqMhz),
            Codec.INT.fieldOf("tdp_watts").forGetter(CpuSpec::tdpWatts),
            Codec.BOOL.optionalFieldOf("alien", false).forGetter(CpuSpec::alien),
            CPU_DESIGN.optionalFieldOf("design", CpuDesign.UNSPECIFIED).forGetter(CpuSpec::design)
    ).apply(i, CpuSpec::new));
    public static final Codec<GpuSpec> GRAPHICS_CARD = RecordCodecBuilder.create(i -> i.group(
            ERA.fieldOf("era").forGetter(GpuSpec::era),
            SLOT_GENERATION.fieldOf("bus").forGetter(GpuSpec::bus),
            Codec.INT.fieldOf("cores").forGetter(GpuSpec::cores),
            Codec.INT.fieldOf("vram_mb").forGetter(GpuSpec::vramMb),
            Codec.INT.fieldOf("tdp_watts").forGetter(GpuSpec::tdpWatts),
            Codec.INT.fieldOf("clock_mhz").forGetter(GpuSpec::clockMhz),
            MICROARCHITECTURE.fieldOf("arch").forGetter(GpuSpec::arch),
            Codec.STRING.optionalFieldOf("chip", "").forGetter(GpuSpec::chip)
    ).apply(i, GpuSpec::new));
    public static final Codec<RamSpec> MEMORY = RecordCodecBuilder.create(i -> i.group(
            ERA.fieldOf("era").forGetter(RamSpec::era),
            MEMORY_GENERATION.fieldOf("generation").forGetter(RamSpec::generation),
            Codec.LONG.fieldOf("buffer_items").forGetter(RamSpec::bufferItems),
            Codec.INT.fieldOf("tdp_watts").forGetter(RamSpec::tdpWatts)
    ).apply(i, RamSpec::new));
    public static final Codec<DiskSpec> DISK = RecordCodecBuilder.create(i -> i.group(
            STORAGE_TIER.fieldOf("tier").forGetter(DiskSpec::tier),
            ERA.fieldOf("era").forGetter(DiskSpec::era),
            Codec.LONG.fieldOf("capacity_items").forGetter(DiskSpec::capacityItems),
            Codec.INT.fieldOf("tdp_watts").forGetter(DiskSpec::tdpWatts)
    ).apply(i, DiskSpec::new));
    public static final Codec<MotherboardSpec> MOTHERBOARD = RecordCodecBuilder.create(i -> i.group(
            FORM_FACTOR.fieldOf("form_factor").forGetter(MotherboardSpec::formFactor),
            ERA.fieldOf("era").forGetter(MotherboardSpec::era),
            SOCKET.fieldOf("socket").forGetter(MotherboardSpec::socket),
            Codec.INT.fieldOf("cpu_slots").forGetter(MotherboardSpec::cpuSlots),
            MEMORY_GENERATION.listOf().xmap(Set::copyOf, List::copyOf).fieldOf("memory")
                    .forGetter(MotherboardSpec::acceptedRam),
            Codec.INT.fieldOf("ram_slots").forGetter(MotherboardSpec::ramSlots),
            SLOT_GENERATION.fieldOf("bus").forGetter(MotherboardSpec::pcieGeneration),
            Codec.INT.fieldOf("slots").forGetter(MotherboardSpec::pcieSlots),
            Codec.INT.fieldOf("disk_slots").forGetter(MotherboardSpec::diskSlots),
            SLOT_GENERATION.optionalFieldOf("older_bus").forGetter(board -> Optional.ofNullable(board.olderBus()))
    ).apply(i, (form, era, socket, cpus, memory, ramSlots, bus, slots, disks, older) -> new MotherboardSpec(form,
            era, socket, cpus, memory, ramSlots, bus, slots, disks, older.orElse(null))));
    public static final Codec<PsuSpec> POWER_SUPPLY = RecordCodecBuilder.create(i -> i.group(
            Codec.INT.fieldOf("wattage").forGetter(PsuSpec::wattage),
            Codec.INT.fieldOf("efficiency_percent").forGetter(PsuSpec::efficiencyPercent),
            Codec.BOOL.optionalFieldOf("auto_scaling", false).forGetter(PsuSpec::autoScaling)
    ).apply(i, PsuSpec::new));
    /** A card of a kind of its own that asks nothing more of a build than its slot and its power. */
    public static final Codec<PlainCardSpec> CARD = RecordCodecBuilder.create(i -> i.group(
            CARD_KIND.fieldOf("kind").forGetter(PlainCardSpec::kind),
            ERA.fieldOf("era").forGetter(PlainCardSpec::era),
            SLOT_GENERATION.fieldOf("bus").forGetter(PlainCardSpec::bus),
            Codec.INT.fieldOf("tdp_watts").forGetter(PlainCardSpec::tdpWatts)
    ).apply(i, PlainCardSpec::new));
    /* The kinds a script declares, written out in full; a label is the same in every language, as this mod's are. */
    public static final Codec<RamGeneration> NEW_MEMORY_GENERATION = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.fieldOf("id").forGetter(RamGeneration::id),
            Codec.STRING.fieldOf("label").forGetter(generation -> generation.label().english()),
            Codec.INT.fieldOf("latency_ticks").forGetter(RamGeneration::latencyTicks)
    ).apply(i, (id, label, latency) -> new RamGeneration(id, Text.literal(label), latency)));
    public static final Codec<PcieGeneration> NEW_SLOT_GENERATION = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.fieldOf("id").forGetter(PcieGeneration::id),
            Codec.INT.fieldOf("generation").forGetter(PcieGeneration::generation),
            Codec.STRING.fieldOf("label").forGetter(generation -> generation.label().english()),
            Codec.STRING.fieldOf("slot_name").forGetter(generation -> generation.slotName().english()),
            BUS_FAMILY.fieldOf("family").forGetter(PcieGeneration::family)
    ).apply(i, (id, generation, label, slot, family) -> new PcieGeneration(id, generation, Text.literal(label),
            Text.literal(slot), family)));
    public static final Codec<FormFactor> NEW_FORM_FACTOR = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.fieldOf("id").forGetter(FormFactor::id),
            Codec.STRING.fieldOf("label").forGetter(FormFactor::label)
    ).apply(i, FormFactor::new));
    public static final Codec<ExpansionCardKind> NEW_CARD_KIND = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.fieldOf("id").forGetter(ExpansionCardKind::id)
    ).apply(i, ExpansionCardKind::new));

    private HardwareCodecs() {
    }

    /* A kind already declared, by its id (the lookup answers null for none); a bare name is one of this mod's own. */
    private static <T> Codec<T> byId(final String what, final Function<String, T> find,
                                     final Function<T, String> idOf) {
        return Codec.STRING.comapFlatMap(id -> {
            final T found = find.apply(id);
            return found != null ? DataResult.success(found) : DataResult.error(() -> "no " + what + " is " + id);
        }, idOf);
    }

    private static DataResult<StorageTier> tier(final String name) {
        for (final StorageTier tier : StorageTier.values()) {
            if (tier.name().equalsIgnoreCase(name)) {
                return DataResult.success(tier);
            }
        }
        return DataResult.error(() -> "no kind of disk is " + name + "; the kinds are hdd, ssd and nvme");
    }

    private static DataResult<ExpansionBus> family(final String name) {
        for (final ExpansionBus family : ExpansionBus.values()) {
            if (family.name().equalsIgnoreCase(name)) {
                return DataResult.success(family);
            }
        }
        return DataResult.error(() -> "no family of slot is " + name + "; the families are isa, pci, agp and pcie");
    }

    private static DataResult<Microarchitecture> knownDesign(final String id) {
        for (final Microarchitecture design : Microarchitectures.all()) {
            if (design.id().equals(id)) {
                return DataResult.success(design);
            }
        }
        return DataResult.error(() -> "no design of cores is " + id + "; write it out with its id, name and worth");
    }
}
