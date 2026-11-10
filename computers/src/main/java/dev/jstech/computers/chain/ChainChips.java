/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.chain;

import static dev.jstech.computers.chain.ChainParts.englishName;
import static dev.jstech.computers.chain.ChainParts.part;

import dev.jstech.core.content.ItemBuilder;
import dev.jstech.core.content.ItemEntry;
import dev.jstech.core.jstechseries.tier.Era;
import dev.jstech.core.jstechseries.tier.HardwareEra;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.world.item.Item;

/**
 * The dies the chain makes chips of (computers/docs/design/survival.md, "How a chip is made"), each with its chip and
 * the mask a Lithography Station exposes it through: a processor and a graphics die for each microarchitecture of each
 * era the catalogue's parts are built on, Quince's own processors and the Phi's MIC among them, Blockwright's hashing
 * die and the portables' four families. Beside them, the Vintage's Layouts, the chip drawings its station writes a mask
 * from by photo-reduction; the mask blank every mask is written on; the masks and the finished wafers of the light,
 * the laser diodes' and the LEDs'; and each era's wafer in process, which goes from machine to machine layer by layer.
 * The memory and controller chips and their masks are each era's own ({@link ChainParts}).
 */
public final class ChainChips {

    public static final ItemEntry<Item> MASK_BLANK = part("mask_blank", HardwareEra.VINTAGE).named("Mask Blank")
            .register();

    /** The processor dies, each era's from the oldest, in the order the catalogue first builds on them. */
    public static final List<Die> PROCESSORS = List.of(
            processor(HardwareEra.VINTAGE, "m68k").named("68000"),
            processor(HardwareEra.VINTAGE, "i486").named("486"),
            processor(HardwareEra.VINTAGE, "p5").named("P5"),
            processor(HardwareEra.VINTAGE, "k5").named("K5"),
            processor(HardwareEra.VINTAGE, "p6").named("P6"),
            processor(HardwareEra.VINTAGE, "k6").named("K6"),
            processor(HardwareEra.LEGACY, "p6").named("P6"),
            processor(HardwareEra.LEGACY, "k7").named("K7"),
            processor(HardwareEra.LEGACY, "netburst").named("NetBurst"),
            processor(HardwareEra.LEGACY, "k8").named("K8"),
            processor(HardwareEra.LEGACY, "g3").named("PowerQ G3"),
            processor(HardwareEra.LEGACY, "g4").named("PowerQ G4"),
            processor(HardwareEra.TRANSITION, "netburst").named("NetBurst"),
            processor(HardwareEra.TRANSITION, "k8").named("K8"),
            processor(HardwareEra.TRANSITION, "centro").named("Centro"),
            processor(HardwareEra.TRANSITION, "k10").named("K10"),
            processor(HardwareEra.TRANSITION, "nehalem").named("Nehalem"),
            processor(HardwareEra.TRANSITION, "westmere").named("Westmere"),
            processor(HardwareEra.STANDARD, "sandy_bridge").named("Sandy Bridge"),
            processor(HardwareEra.STANDARD, "ivy_bridge").named("Ivy Bridge"),
            processor(HardwareEra.STANDARD, "haswell").named("Haswell"),
            processor(HardwareEra.STANDARD, "bulldozer").named("Bulldozer"),
            processor(HardwareEra.STANDARD, "piledriver").named("Piledriver"),
            processor(HardwareEra.STANDARD, "mic").named("MIC"),
            processor(HardwareEra.ADVANCED, "kaby_lake").named("Kaby Lake"),
            processor(HardwareEra.ADVANCED, "coffee_lake").named("Coffee Lake"),
            processor(HardwareEra.ADVANCED, "comet_lake").named("Comet Lake"),
            processor(HardwareEra.ADVANCED, "alder_lake").named("Alder Lake"),
            processor(HardwareEra.ADVANCED, "raptor_lake").named("Raptor Lake"),
            processor(HardwareEra.ADVANCED, "arrow_lake").named("Arrow Lake"),
            processor(HardwareEra.ADVANCED, "way_1").named("Way 1"),
            processor(HardwareEra.ADVANCED, "way_1_plus").named("Way 1+"),
            processor(HardwareEra.ADVANCED, "way_2").named("Way 2"),
            processor(HardwareEra.ADVANCED, "way_3").named("Way 3"),
            processor(HardwareEra.ADVANCED, "way_4").named("Way 4"),
            processor(HardwareEra.ADVANCED, "way_5").named("Way 5"),
            processor(HardwareEra.ADVANCED, "skylake_sp").named("Skylake-SP"),
            processor(HardwareEra.ADVANCED, "cascade_lake").named("Cascade Lake"),
            processor(HardwareEra.ADVANCED, "ice_lake_sp").named("Ice Lake-SP"),
            processor(HardwareEra.ADVANCED, "cooper_lake").named("Cooper Lake"),
            processor(HardwareEra.ADVANCED, "sapphire_rapids").named("Sapphire Rapids"),
            processor(HardwareEra.ADVANCED, "emerald_rapids").named("Emerald Rapids"),
            processor(HardwareEra.ADVANCED, "q1").named("Quince Q1"),
            processor(HardwareEra.ADVANCED, "q2").named("Quince Q2"),
            processor(HardwareEra.ADVANCED, "q3").named("Quince Q3"));

    /** The graphics dies, each era's from the oldest, in the order the catalogue first builds on them. */
    public static final List<Die> GRAPHICS = List.of(
            graphics(HardwareEra.VINTAGE, "vga").named("VGA"),
            graphics(HardwareEra.VINTAGE, "rendition").named("Rendition"),
            graphics(HardwareEra.VINTAGE, "threedfx").named("3dfx"),
            graphics(HardwareEra.VINTAGE, "nv3").named("NV3"),
            graphics(HardwareEra.VINTAGE, "rage").named("Rage"),
            graphics(HardwareEra.VINTAGE, "fahrenheit").named("Fahrenheit"),
            graphics(HardwareEra.LEGACY, "fahrenheit").named("Fahrenheit"),
            graphics(HardwareEra.LEGACY, "celsius").named("Celsius"),
            graphics(HardwareEra.LEGACY, "r100").named("R100"),
            graphics(HardwareEra.LEGACY, "r200").named("R200"),
            graphics(HardwareEra.LEGACY, "kelvin").named("Kelvin"),
            graphics(HardwareEra.LEGACY, "r300").named("R300"),
            graphics(HardwareEra.LEGACY, "rankine").named("Rankine"),
            graphics(HardwareEra.LEGACY, "r400").named("R400"),
            graphics(HardwareEra.LEGACY, "curie").named("Curie"),
            graphics(HardwareEra.TRANSITION, "curie").named("Curie"),
            graphics(HardwareEra.TRANSITION, "r500").named("R500"),
            graphics(HardwareEra.TRANSITION, "tesla").named("Tesla"),
            graphics(HardwareEra.TRANSITION, "terascale").named("TeraScale"),
            graphics(HardwareEra.TRANSITION, "terascale_2").named("TeraScale 2"),
            graphics(HardwareEra.TRANSITION, "fermi").named("Fermi"),
            graphics(HardwareEra.STANDARD, "fermi").named("Fermi"),
            graphics(HardwareEra.STANDARD, "kepler").named("Kepler"),
            graphics(HardwareEra.STANDARD, "gcn").named("GCN"),
            graphics(HardwareEra.STANDARD, "maxwell").named("Maxwell"),
            graphics(HardwareEra.STANDARD, "pascal").named("Pascal"),
            graphics(HardwareEra.ADVANCED, "pascal").named("Pascal"),
            graphics(HardwareEra.ADVANCED, "gcn").named("GCN"),
            graphics(HardwareEra.ADVANCED, "volta").named("Volta"),
            graphics(HardwareEra.ADVANCED, "turing").named("Turing"),
            graphics(HardwareEra.ADVANCED, "rdna").named("RDNA"),
            graphics(HardwareEra.ADVANCED, "ampere").named("Ampere"),
            graphics(HardwareEra.ADVANCED, "rdna_2").named("RDNA 2"),
            graphics(HardwareEra.ADVANCED, "hopper").named("Hopper"),
            graphics(HardwareEra.ADVANCED, "ada").named("Ada"),
            graphics(HardwareEra.ADVANCED, "rdna_3").named("RDNA 3"),
            graphics(HardwareEra.ADVANCED, "blackwell").named("Blackwell"),
            graphics(HardwareEra.ADVANCED, "rdna_4").named("RDNA 4"));

    /** Blockwright's hashing die, a mining rig's, in the two eras it has one. */
    public static final List<Die> HASHING = List.of(
            hashing(HardwareEra.STANDARD).named("Blockwright"),
            hashing(HardwareEra.ADVANCED).named("Blockwright"));

    /** The portables' dies: Hightide's, Quince's qA, Motoroller's MX and Sunsung's Exo, each in its eras. */
    public static final List<Die> MOBILE = List.of(
            mobile(HardwareEra.LEGACY, "ht").named("Hightide"),
            mobile(HardwareEra.TRANSITION, "ht").named("Hightide"),
            mobile(HardwareEra.TRANSITION, "qa").named("Quince qA"),
            mobile(HardwareEra.TRANSITION, "mx").named("Motoroller MX"),
            mobile(HardwareEra.STANDARD, "ht").named("Hightide"),
            mobile(HardwareEra.STANDARD, "qa").named("Quince qA"),
            mobile(HardwareEra.STANDARD, "ex").named("Sunsung Exo"),
            mobile(HardwareEra.ADVANCED, "qa").named("Quince qA"),
            mobile(HardwareEra.ADVANCED, "ex").named("Sunsung Exo"));

    /* The Vintage's Layouts: the memory's and the controller's, then each processor and graphics die's. */
    public static final ItemEntry<Item> MEMORY_LAYOUT = part("layout_memory", HardwareEra.VINTAGE)
            .named("Memory Layout").register();
    public static final ItemEntry<Item> CONTROLLER_LAYOUT = part("layout_controller", HardwareEra.VINTAGE)
            .named("Controller Layout").register();
    public static final List<Layout> PROCESSOR_LAYOUTS = List.of(
            processorLayout("m68k").named("68000"),
            processorLayout("i486").named("486"),
            processorLayout("p5").named("P5"),
            processorLayout("k5").named("K5"),
            processorLayout("p6").named("P6"),
            processorLayout("k6").named("K6"));
    public static final List<Layout> GRAPHICS_LAYOUTS = List.of(
            graphicsLayout("vga").named("VGA"),
            graphicsLayout("rendition").named("Rendition"),
            graphicsLayout("threedfx").named("3dfx"),
            graphicsLayout("nv3").named("NV3"),
            graphicsLayout("rage").named("Rage"),
            graphicsLayout("fahrenheit").named("Fahrenheit"));

    /* The light: the laser diodes' mask from the Legacy, the LEDs' from the Standard, and their finished wafers. */
    public static final ItemEntry<Item> LASER_DIODE_MASK = part("mask_laser_diode", HardwareEra.LEGACY)
            .named("Laser Diode Mask").register();
    public static final ItemEntry<Item> LED_MASK = part("mask_led", HardwareEra.STANDARD).named("LED Mask")
            .register();
    public static final ItemEntry<Item> FINISHED_GALLIUM_ARSENIDE_WAFER = part("finished_gallium_arsenide_wafer",
            HardwareEra.LEGACY).named("Finished Gallium Arsenide Wafer").register();
    public static final ItemEntry<Item> FINISHED_ALGAINP_WAFER = part("finished_algainp_wafer",
            HardwareEra.TRANSITION).named("Finished AlGaInP Wafer").register();
    public static final ItemEntry<Item> FINISHED_GALLIUM_NITRIDE_WAFER = part("finished_gallium_nitride_wafer",
            HardwareEra.STANDARD).named("Finished Gallium Nitride Wafer").register();

    /** Each era's wafer in process, from the oldest. */
    public static final List<ItemEntry<Item>> WAFERS_IN_PROCESS = inProcess();

    private ChainChips() {
    }

    /** Declares the dies, from the mod's constructor. */
    public static void declare() {
        // Loading the class declares them.
    }

    /** Every die, of every kind. */
    public static List<Die> all() {
        final List<Die> out = new ArrayList<>(PROCESSORS);
        out.addAll(GRAPHICS);
        out.addAll(HASHING);
        out.addAll(MOBILE);
        return List.copyOf(out);
    }

    /** The die whose chip or mask {@code item} is, if it is one. */
    public static Optional<Die> dieOf(final Item item) {
        return all().stream().filter(die -> die.chip().get() == item || die.mask().get() == item).findFirst();
    }

    /** The wafer in process of {@code era}. */
    public static ItemEntry<Item> waferInProcess(final Era era) {
        return WAFERS_IN_PROCESS.get(ChainParts.ERAS.indexOf(era));
    }

    private static List<ItemEntry<Item>> inProcess() {
        final List<ItemEntry<Item>> out = new ArrayList<>();
        for (final Era era : ChainParts.ERAS) {
            out.add(part("wafer_in_process_" + era.path(), era)
                    .named("Wafer in Process (" + englishName(era) + ")").register());
        }
        return List.copyOf(out);
    }

    private static DieDeclaration processor(final Era era, final String path) {
        return new DieDeclaration(Kind.PROCESSOR, era, path);
    }

    private static DieDeclaration graphics(final Era era, final String path) {
        return new DieDeclaration(Kind.GRAPHICS, era, path);
    }

    private static DieDeclaration hashing(final Era era) {
        return new DieDeclaration(Kind.HASHING, era, "");
    }

    private static DieDeclaration mobile(final Era era, final String path) {
        return new DieDeclaration(Kind.MOBILE, era, path);
    }

    private static LayoutDeclaration processorLayout(final String path) {
        return new LayoutDeclaration(Kind.PROCESSOR, path);
    }

    private static LayoutDeclaration graphicsLayout(final String path) {
        return new LayoutDeclaration(Kind.GRAPHICS, path);
    }

    /** What a die is the chip of: a processor, a graphics card, a hashing rig or a portable. */
    public enum Kind {
        PROCESSOR,
        GRAPHICS,
        HASHING,
        MOBILE
    }

    /**
     * One die: its kind, its era and its path among them, its English name, its chip and its mask.
     *
     * @param kind    what it is the chip of
     * @param era     the era it is made in
     * @param path    its microarchitecture or family, as the items' ids name it; empty for the one hashing die
     * @param english its name: its microarchitecture's or its family's
     * @param chip    its chip
     * @param mask    the mask a Lithography Station exposes its layers through
     */
    public record Die(Kind kind, Era era, String path, String english, ItemEntry<Item> chip, ItemEntry<Item> mask) {
    }

    /**
     * One of the Vintage's Layouts of a die: its kind, its microarchitecture's path, its English name and its item.
     *
     * @param kind    a processor's or a graphics die's
     * @param path    its microarchitecture, as the ids name it
     * @param english its microarchitecture's name
     * @param item    the Layout
     */
    public record Layout(Kind kind, String path, String english, ItemEntry<Item> item) {
    }

    /* A die declared by its kind, its era and its path, named last, which registers its chip and its mask. */
    private record DieDeclaration(Kind kind, Era era, String path) {

        Die named(final String english) {
            final String eraName = englishName(era());
            final String suffix = path.isEmpty() ? era().path() : era().path() + "_" + path;
            final ItemBuilder<Item> chip;
            final ItemBuilder<Item> mask;
            switch (kind) {
                case PROCESSOR -> {
                    chip = part("processor_chip_" + suffix, era()).named("Processor Chip (" + english + ", " + eraName
                            + ")");
                    mask = part("mask_processor_" + suffix, era()).named("Processor Mask (" + english + ", " + eraName
                            + ")");
                }
                case GRAPHICS -> {
                    chip = part("graphics_chip_" + suffix, era()).named("Graphics Chip (" + english + ", " + eraName
                            + ")");
                    mask = part("mask_graphics_" + suffix, era()).named("Graphics Mask (" + english + ", " + eraName
                            + ")");
                }
                case HASHING -> {
                    chip = part("hashing_chip_" + suffix, era()).named("Hashing Chip (" + eraName + ")");
                    mask = part("mask_hashing_" + suffix, era()).named("Hashing Mask (" + eraName + ")");
                }
                default -> {
                    chip = part("mobile_chip_" + suffix, era()).named("Mobile Chip (" + english + ", " + eraName + ")");
                    mask = part("mask_mobile_" + suffix, era()).named("Mobile Mask (" + english + ", " + eraName + ")");
                }
            }
            return new Die(kind, era(), path, english, chip.register(), mask.register());
        }
    }

    /* A Vintage Layout declared by its kind and its microarchitecture, named last, which registers it. */
    private record LayoutDeclaration(Kind kind, String path) {

        Layout named(final String english) {
            final ItemEntry<Item> item = kind == Kind.PROCESSOR
                    ? part("layout_processor_" + path, HardwareEra.VINTAGE).named("Processor Layout (" + english
                            + ")").register()
                    : part("layout_graphics_" + path, HardwareEra.VINTAGE).named("Graphics Layout (" + english
                            + ")").register();
            return new Layout(kind, path, english, item);
        }
    }
}
