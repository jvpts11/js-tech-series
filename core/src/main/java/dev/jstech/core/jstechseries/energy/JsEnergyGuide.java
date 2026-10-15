/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.jstechseries.energy;

import dev.jstech.core.guide.ModGuide;
import dev.jstech.core.registry.CoreItems;
import net.neoforged.neoforge.registries.DeferredBlock;

/**
 * The pages of J's Energy's things in the Core's chapter of the manual: the clamp meter, the basic cable, the basic
 * battery and the FE converters.
 */
public final class JsEnergyGuide {

    /* The Core's chapter's section on energy, where these pages go. */
    private static final ModGuide.SectionRef ENERGY = new ModGuide.SectionRef("energy");

    private JsEnergyGuide() {
    }

    /** Declares the pages, from the Core's constructor, after the Core's chapter. */
    public static void declare() {
        CoreItems.CONTENT.guide().page("clamp_meter", ENERGY).titled("Clamp Meter").icon(ClampMeter.ITEM::get)
                .covers(ClampMeter.ITEM::get)
                .whatItIs("A handheld instrument that reads energy. Clamped on a wire of a line, it reads what"
                        + " crosses the wire and which way; clamped on a machine or a store, what goes in and out of"
                        + " each of its faces.")
                .whatItIsFor("Finding where the energy goes, how much a line carries against its ceiling, and which"
                        + " machine draws it.")
                .table("Its keys and its dial")
                .property("HOLD", "Keeps the reading still")
                .property("MAX MIN", "Shows the most and the least that crossed in a tick since it was clamped")
                .property("SHOW", "Shows the reading beside the crosshair while it is in hand")
                .property("LIGHT", "Lights the display")
                .property("Dial", "Off, J's Energy's own units, or FE")
                .howToGetIt("Take it from the creative tab J's Tech.")
                .howToUseIt("Use it on a cable to clamp it on the energy wire you aim at.",
                        "Use it on a machine or a store to read it face by face.",
                        "Hold it: its reading shows beside the crosshair.",
                        "Use it in the air to hold it up and read the details on its display.")
                .whatCanGoWrong("It reads nothing.",
                        "Its dial is off, or what it was clamped on is gone. Turn the dial, or clamp it again.",
                        "A line reads no direction.",
                        "Nothing crossed the wire yet, or it was clamped a moment ago; wait a second.")
                .register();
        CoreItems.CONTENT.guide().page("insulated_copper_cable", ENERGY).titled("Insulated Copper Cable")
                .icon(SharedEnergy.INSULATED_COPPER_CABLE::asItem).covers(SharedEnergy.INSULATED_COPPER_CABLE::asItem)
                .whatItIs("The basic cable: a low voltage line of J's Energy, copper in black rubber, laid like every"
                        + " cable of the series in the shared cable block.")
                .whatItIsFor("Carrying energy from what makes or keeps it to what spends it, block to block. While"
                        + " J's Energy is off, as it comes, it carries FE to any mod's machine.")
                .table("What it carries")
                .property("Voltage", "Low, 400 V")
                .property("Most it carries", "10 kW, 500 J a tick")
                .property("What it loses", "A tenth of what it carries over a hundred blocks")
                .howToGetIt("Craft six of a row of copper ingots between two rows of dried kelp. With J's Industrial"
                        + " installed, it is that mod's first cable, made with its own recipe.")
                .howToUseIt("Lay it from a store or a generator to a machine.",
                        "A line longer than it needs loses more on the way: keep it short.")
                .whatCanGoWrong("The machine at the end gets less than it needs.",
                        "The line carries 10 kW at most, and loses some on the way; lay a second line, or keep it"
                                + " shorter.")
                .register();
        CoreItems.CONTENT.guide().page("lead_acid_battery_bank", ENERGY).titled("Lead-Acid Battery Bank")
                .icon(SharedEnergy.BATTERY::asItem).covers(SharedEnergy.BATTERY::asItem)
                .whatItIs("The basic battery: cells of lead plates in acid, in a steel case, at low voltage.")
                .whatItIsFor("Keeping energy for when nothing makes it, and smoothing what a line carries.")
                .table("What it keeps")
                .property("Store", "50 MJ")
                .property("Takes and gives", "10 kW, 500 J a tick")
                .property("Voltage", "Low, 400 V")
                .howToGetIt("Craft it of four iron ingots at the corners, a copper ingot above and below a water"
                        + " bucket, and redstone at its sides. With J's Industrial installed, it is that mod's first"
                        + " energy bank, made with its own recipe.")
                .howToUseIt("Place it on a line: it takes what the line has spare and gives it back when the line"
                        + " runs short.",
                        "The gauge on its sides shows how full it is, in four steps.")
                .whatCanGoWrong("It neither charges nor gives.",
                        "It takes and gives through any face, but only by a line it is plugged into; lay a cable to"
                                + " it.")
                .register();
        converters();
    }

    private static void converters() {
        final ModGuide.EntryBuilder page = CoreItems.CONTENT.guide().page("fe_converters", ENERGY)
                .titled("FE Converters").icon(EnergyConverters.CONFIGURABLE::asItem);
        for (final DeferredBlock<EnergyConverterBlock> each : EnergyConverters.all()) {
            page.covers(each::asItem);
        }
        page.whatItIs("Cabinets that turn the game's FE into J's Energy, or J's Energy into FE: one of each voltage"
                        + " class, and the Configurable Energy Converter, whose class you set.")
                .whatItIsFor("Joining the series' lines to the machines and cables of any other mod, either way.")
                .table("The most each carries")
                .property("FE Converter LV", "100 kW")
                .property("FE Converter MV", "10 MW")
                .property("FE Converter HV", "200 MW")
                .property("FE Converter EHV", "1.5 GW")
                .property("FE Converter UHV", "12 GW")
                .property("Configurable Energy Converter", "Its class's")
                .howToGetIt("Craft the low voltage one of iron, copper, redstone and gold, and each class above of"
                        + " the one below and dearer things. The configurable one is made from the ultra-high"
                        + " voltage one.")
                .howToUseIt("Facing its front, FE goes in or out on its left side and J's Energy on its right.",
                        "Turn the knob to choose the way: FE to JE, or JE to FE.",
                        "Set the most it moves on the thumbwheels: click a drum's upper half to turn it up, its"
                                + " lower half to turn it down, and the last drum for kilowatts, megawatts or"
                                + " gigawatts. It never goes above the class's ceiling.",
                        "On the configurable one, press a class key to set its class.",
                        "A joule is worth one FE unless the server sets otherwise; a converter rounds down, so it"
                                + " never makes energy.")
                .whatCanGoWrong("The FAULT lamp is lit and nothing crosses.",
                        "A line of another class meets its J's Energy side: set the class to the line's, or lay a"
                                + " line of the converter's class.",
                        "The LIMIT lamp blinks.",
                        "More is offered than its limit lets through; turn the thumbwheels up, or use a converter of"
                                + " a higher class.")
                .register();
    }
}
