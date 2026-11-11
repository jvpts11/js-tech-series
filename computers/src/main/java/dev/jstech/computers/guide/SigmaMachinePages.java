/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.guide;

import dev.jstech.core.guide.GuideCell;
import dev.jstech.core.guide.GuideChapter;
import dev.jstech.core.guide.ModGuide;

/**
 * Pages of the library chapter of the Book of Sigma on what the machine reads of itself, which came with the third
 * version: its power, its heat and its Wi-Fi.
 */
final class SigmaMachinePages {

    private SigmaMachinePages() {
    }

    /** Declares the section and the pages of the machine's own readings, in reading order. */
    static void declare(final ModGuide guide, final GuideChapter chapter) {
        final ModGuide.SectionRef section = guide.section("library_readings").titled("The machine's own readings")
                .in(chapter).register();
        power(guide, section);
        heat(guide, section);
        wireless(guide, section);
        disks(guide, section);
        settings(guide, section);
        sensors(guide, section);
        signals(guide, section);
    }

    private static void power(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("power_library", section).titled("Power: the draw, the supply and the UPS")
                .paragraph("A program can read what its machine takes from the line, the power supply that feeds it,"
                        + " and the UPS that carries it. You read them through Power, a built-in type of"
                        + " System.Machine, the same way you read Computer. Power came with Sigma# 3.")
                .table("Power members", "Member", "What it gives")
                .row(GuideCell.code("Power.DrawWatts"), GuideCell.words("What the machine takes from the line now, in"
                        + " watts (W): what its parts draw, plus what its supply loses. It gives 0 while the machine"
                        + " is off."))
                .row(GuideCell.code("Power.Supply"), GuideCell.words("The power supply, as a SupplyInfo record, or null"
                        + " when the machine has none."))
                .row(GuideCell.code("Power.Ups"), GuideCell.words("The UPS that carries the machine, as a UpsInfo"
                        + " record, or null when no UPS does."))
                .table("Records of Power", "Record", "Fields", "What each field holds")
                .row(GuideCell.code("SupplyInfo"), GuideCell.code("SupplyInfo.Name, SupplyInfo.RatingWatts, "
                                + "SupplyInfo.Units, SupplyInfo.UnitsFailed, SupplyInfo.Degraded, SupplyInfo.Failed"),
                        GuideCell.words("Name: the supply's name. RatingWatts: the most it gives, in watts. Units: how"
                                + " many units it has, more than one for a redundant supply. UnitsFailed: how many of"
                                + " them have failed. Degraded: yes when a unit is worn or has failed and the rest"
                                + " carry on. Failed: yes when every unit has failed."))
                .row(GuideCell.code("UpsInfo"), GuideCell.code("UpsInfo.Name, UpsInfo.OnBattery, UpsInfo.Bypass, "
                                + "UpsInfo.ChargePercent, UpsInfo.MinutesLeft"),
                        GuideCell.words("Name: the UPS's name. OnBattery: yes when the line is gone and the battery"
                                + " carries the machine. Bypass: yes when the machine draws more than the UPS carries,"
                                + " so it runs straight from the line, unprotected. ChargePercent: how full the battery"
                                + " is, from 0 to 100. MinutesLeft: how long the battery lasts at this draw."))
                .paragraph("A server in a rack reads its own supply, and the rack's UPS, which carries every server in"
                        + " the rack.")
                .listing("Power.sgs",
                        "UpsInfo ups = Power.Ups;",
                        "if (ups != null && ups.OnBattery && ups.MinutesLeft < 5) {",
                        "    Console.PrintLine(\"Saving: the battery is almost empty\");",
                        "}")
                .define("UPS", "Uninterruptible power supply: a battery that carries a machine when the line goes"
                        + " down.")
                .define("Redundant supply", "A power supply made of several units, where the others carry on when one"
                        + " fails.")
                .ifSomethingGoesWrong(
                        "Power.Ups gives null.",
                        "No UPS carries the machine. A UPS block stands against the computer; a rack takes a Rack UPS.",
                        "Power.DrawWatts gives 0.",
                        "The machine is off, or it has no build yet.")
                .seeAlso("jsc:power_supplies", "jsc:upses", "jsc:computer_info")
                .register();
    }

    private static void heat(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("thermal_library", section).titled("Thermal: how hot the machine runs")
                .paragraph("A program can read its machine's heat: how hot it is, the heat its parts make, the heat"
                        + " its cooling takes away, and how much of its clock the heat leaves it. You read it through"
                        + " Thermal, a built-in type of System.Machine. Thermal came with Sigma# 3.")
                .table("Thermal members", "Member", "What it gives")
                .row(GuideCell.code("Thermal.Known"), GuideCell.words("Yes when the machine has a temperature of its"
                        + " own. When it gives no, every reading below gives its resting value."))
                .row(GuideCell.code("Thermal.Celsius"), GuideCell.words("How hot the machine is now, in degrees Celsius"
                        + " (°C)."))
                .row(GuideCell.code("Thermal.AmbientCelsius"), GuideCell.words("How hot its surroundings are, in °C."))
                .row(GuideCell.code("Thermal.HeatWatts"), GuideCell.words("The heat its parts make, in watts."))
                .row(GuideCell.code("Thermal.BudgetWatts"), GuideCell.words("Its thermal budget: the heat its cooling"
                        + " takes away, in watts."))
                .row(GuideCell.code("Thermal.ClockPercent"), GuideCell.words("The share of its clock the heat leaves"
                        + " it, from 25 to 100. Programs run at this share."))
                .row(GuideCell.code("Thermal.Throttled"), GuideCell.words("Yes while the machine slows its clock to"
                        + " hold 90 °C."))
                .row(GuideCell.code("Thermal.LastTripCelsius"), GuideCell.words("How hot it was when heat last shut it"
                        + " down, in °C, or 0 when heat never did."))
                .paragraph("A machine whose parts make more heat than its budget gets hotter until it throttles. A"
                        + " server in a rack reads the rack's heat, which every server in it shares.")
                .listing("Heat.sgs",
                        "if (Thermal.Throttled) {",
                        "    Console.PrintLine(\"Too hot: running at \" + Thermal.ClockPercent + \"%\");",
                        "}")
                .define("Thermal budget", "The heat a machine's cooling takes away, in watts.")
                .define("Throttling", "Slowing the clock to make less heat, so the machine stays at a safe"
                        + " temperature.")
                .ifSomethingGoesWrong(
                        "Thermal.Known gives no.",
                        "The machine has no temperature of its own; something outside it cools it.",
                        "Thermal.Throttled stays yes.",
                        "The parts make more heat than the cooling takes away. Fit a better cooler or more fans.")
                .seeAlso("jsc:cooling", "jsc:rack_cooling_and_heat", "jsc:computer_info")
                .register();
    }

    private static void wireless(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("wireless_library", section).titled("Wireless: the machine's Wi-Fi")
                .paragraph("A computer with a Wi-Fi card joins its network through an access point. A program can"
                        + " read how: whether it joined, through which access point, at which standard, and how fast"
                        + " the link carries. You read it through Wireless, a built-in type of System.Machine."
                        + " Wireless came with Sigma# 3.")
                .table("Wireless members", "Member", "What it gives")
                .row(GuideCell.code("Wireless.Joined"), GuideCell.words("Yes when the machine is on its network"
                        + " through an access point."))
                .row(GuideCell.code("Wireless.AccessPoint"), GuideCell.words("The access point's name, as its"
                        + " network lists it, or empty text when the machine has not joined one."))
                .row(GuideCell.code("Wireless.Standard"), GuideCell.words("The standard of the machine's Wi-Fi card,"
                        + " such as 802.11n, or empty text when it has no card."))
                .row(GuideCell.code("Wireless.Speed"), GuideCell.words("How many items a tick the link carries: the"
                        + " speed of the older of the card and the access point. It gives 0 when the machine has not"
                        + " joined."))
                .row(GuideCell.code("Wireless.Signal"), GuideCell.words("How strong the access point's signal is"
                        + " where the machine stands, from 100 beside it to 0 at the edge of its reach. It gives 0 when"
                        + " the machine has not joined."))
                .paragraph("The signal only tells you where the machine stands: the link carries at full speed"
                        + " anywhere in the access point's reach.")
                .paragraph("A machine on a data cable has not joined by Wi-Fi, even with a card in it: the cable"
                        + " carries it.")
                .define("Access point", "A network device that gives the wireless network to the computers and the"
                        + " portable devices around it.")
                .ifSomethingGoesWrong(
                        "Wireless.Joined gives no, though the machine has a card.",
                        "No access point reaches it, or a data cable carries it instead.",
                        "Wireless.Standard gives empty text.",
                        "The machine has no Wi-Fi card.")
                .seeAlso("jsc:wireless", "jsc:computer_info")
                .register();
    }

    private static void disks(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("disk_health_library", section).titled("DiskHealth: what happened to the disks")
                .paragraph("A program can read the health of its machine's drives as the Defrag reads it: whether a"
                        + " disk died, how damaged it is, its bad sectors, the items corruption holds, and when a"
                        + " Defrag pass last ended on it. You read it through DiskHealth, a built-in type of"
                        + " System.Machine."
                        + " DiskHealth came with Sigma# 3.")
                .table("DiskHealth members", "Member", "What it gives")
                .row(GuideCell.code("DiskHealth.Of(drive)"), GuideCell.words("The health of the disk in that drive,"
                        + " named by its letter such as C, or null when no disk is there."))
                .row(GuideCell.code("DiskHealth.All()"), GuideCell.words("The health of every drive with a disk in it,"
                        + " in letter order."))
                .row(GuideCell.code("DiskHealth.Drive"), GuideCell.words("The drive's letter."))
                .row(GuideCell.code("DiskHealth.Dead"), GuideCell.words("Yes when the disk died and holds nothing any"
                        + " more."))
                .row(GuideCell.code("DiskHealth.DamagePercent"), GuideCell.words("How damaged the disk is, from 0 to"
                        + " 100."))
                .row(GuideCell.code("DiskHealth.BadSectorsPercent"), GuideCell.words("How much of the disk bad sectors"
                        + " have taken, from 0 to 100."))
                .row(GuideCell.code("DiskHealth.CorruptedItems"), GuideCell.words("How many items and files corruption"
                        + " holds on the disk."))
                .row(GuideCell.code("DiskHealth.LastDefragTick"), GuideCell.words("The world tick a Defrag pass last"
                        + " ended on the disk, which you compare with Time.Tick, or -1 when none has."))
                .listing("Health.sgs",
                        "foreach (DiskHealth disk in DiskHealth.All()) {",
                        "    if (disk.CorruptedItems > 0) {",
                        "        Console.PrintLine(disk.Drive + \": run the Defrag\");",
                        "    }",
                        "}")
                .define("Bad sector", "A part of a disk that no longer holds what is written to it. A Defrag pass mends"
                        + " some of them.")
                .ifSomethingGoesWrong(
                        "DiskHealth.Of gives null.",
                        "No disk is in that drive. Check the letter with Computer.Disks().",
                        "DiskHealth.LastDefragTick gives -1.",
                        "No Defrag pass has ended on that disk on this machine.")
                .seeAlso("jsc:corruption_and_the_defrag", "jsc:disks", "jsc:computer_info")
                .register();
    }

    private static void settings(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("settings_library", section).titled("Settings: a program's own settings")
                .paragraph("A program can keep its own settings, each a name and a line of text, and read them back"
                        + " the next time it runs. Its system keeps them in a file the way the real one did, so you can"
                        + " read that file with the system's own tools. You reach them through Settings, a built-in"
                        + " type of System.Machine. Settings came with Sigma# 3.")
                .table("Settings members", "Member", "What it does")
                .row(GuideCell.code("Settings.Get(name, fallback)"), GuideCell.words("The setting with that name, or"
                        + " the fallback when the program has none by that name."))
                .row(GuideCell.code("Settings.Set(name, value)"), GuideCell.words("Keeps the setting, in place of what"
                        + " it held. It gives no when the disk refuses the file."))
                .row(GuideCell.code("Settings.Remove(name)"), GuideCell.words("Takes the setting away. It gives no when"
                        + " the program had none by that name."))
                .row(GuideCell.code("Settings.Names()"), GuideCell.words("The names of every setting the program"
                        + " keeps."))
                .row(GuideCell.code("Settings.Path"), GuideCell.words("Where the settings are kept, written as the"
                        + " system writes paths."))
                .paragraph("Each program keeps its settings under the name of the class it was started from, so two"
                        + " programs never share one.")
                .table("Where each system keeps them", "System", "Where")
                .row(GuideCell.words("Frames"), GuideCell.words("In its registry, the file"
                        + " C:\\Frames\\System32\\config\\SOFTWARE, under HKEY_LOCAL_MACHINE\\SOFTWARE and the"
                        + " program's name."))
                .row(GuideCell.words("MC-DOS"), GuideCell.words("In an INI file of its own at the top of C:, its name"
                        + " cut to eight letters."))
                .row(GuideCell.words("Linux, the BSDs and UNIX"), GuideCell.words("In ~/.config, a folder of the"
                        + " program's own holding a file called settings. A program that stays up as a service keeps"
                        + " them in /etc instead, in a file of its name ending in .conf."))
                .row(GuideCell.words("Quince OS X and quinceOS"), GuideCell.words("In ~/Library/Preferences, a"
                        + " property list of the program's name."))
                .row(GuideCell.words("The classic Quince systems"), GuideCell.words("In the System Folder's"
                        + " Preferences."))
                .paragraph("A setting's name is letters, digits, dots, dashes and underscores, up to 64 of them. Its"
                        + " value is one line, up to 1024 characters. The file weighs on the disk like any other.")
                .listing("Settings.sgs",
                        "int runs = Convert.ToInt(Settings.Get(\"runs\", \"0\")) + 1;",
                        "Settings.Set(\"runs\", \"\" + runs);",
                        "Console.PrintLine(\"Run \" + runs + \", kept in \" + Settings.Path);")
                .define("Registry", "The one file Frames keeps every program's settings in, each under a key of its"
                        + " own.")
                .ifSomethingGoesWrong(
                        "The program stops and says the name is not a setting's name.",
                        "Use letters, digits, dots, dashes and underscores only, up to 64 of them.",
                        "The program stops and says a setting holds one line.",
                        "Take the line breaks out of the value, and keep it to 1024 characters.",
                        "Settings.Set gives no.",
                        "The system disk is full or cannot be written. Free some room on it.")
                .seeAlso("jsc:file_tree", "jsc:computer_info")
                .register();
    }

    private static void sensors(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("sensors_library", section).titled("Sensors: what the sensors around the machine read")
                .paragraph("A sensor of J's Core reads something of the world: heat, light, pressure, radiation,"
                        + " speed. A program reads a sensor joined to its machine through a Redstone Interface: link"
                        + " the interface to the machine and turn its lens toward the sensor. You read the sensor by"
                        + " the interface's name through Sensors, a built-in type of System.Machine. Sensors came with"
                        + " Sigma# 3.")
                .table("Sensors members", "Member", "What it gives")
                .row(GuideCell.code("Sensors.Read(name)"), GuideCell.words("What the sensor in front of the interface"
                        + " with that name reads, as a list of SensorReading records. The list is empty when nothing"
                        + " there reads anything, and the call gives null when no interface has that name."))
                .row(GuideCell.code("Sensors.Names()"), GuideCell.words("The names of the interfaces that have a sensor"
                        + " in front of them."))
                .table("Records of Sensors", "Record", "Fields", "What each field holds")
                .row(GuideCell.code("SensorReading"), GuideCell.code("SensorReading.Name, SensorReading.Value, "
                                + "SensorReading.Least, SensorReading.Most, SensorReading.Unit"),
                        GuideCell.words("Name: what it reads. Value: the reading now. Least and Most: the span the"
                                + " reading runs over. Unit: the unit of the value, or empty text when it has none."))
                .paragraph("A sensor can read several things at once, and each is a reading of its own. The redstone a"
                        + " sensor gives says one of them as a strength from 0 to 15; a program reads them all, as"
                        + " numbers.")
                .define("Sensor", "A block that reads something of the world and gives it as a signal.")
                .ifSomethingGoesWrong(
                        "Sensors.Read gives null.",
                        "No Redstone Interface linked to this machine has that name. Check the name in its window.",
                        "Sensors.Read gives an empty list.",
                        "The interface's lens does not face a sensor. Turn it toward the sensor.")
                .seeAlso("jsc:redstone_interfaces", "jsc:redstone_library")
                .register();
    }

    private static void signals(final ModGuide guide, final ModGuide.SectionRef section) {
        guide.page("signals_library", section).titled("Signals: radios and the frequencies heard")
                .paragraph("J's Core sends signals without wire by frequency: a transmitter sends a value from 0 to 15,"
                        + " and every receiver on its frequency gives it out. A program reaches a radio joined to its"
                        + " machine the way it reaches a sensor: through a Redstone Interface linked to the machine,"
                        + " with its lens toward the radio. You use them through Signals, a built-in type of"
                        + " System.Machine. Signals came with Sigma# 3.")
                .table("Signals members", "Member", "What it does")
                .row(GuideCell.code("Signals.Radio(name)"), GuideCell.words("The radio in front of the interface with"
                        + " that name, as a RadioInfo record, or null when no interface has that name or it faces no"
                        + " radio."))
                .row(GuideCell.code("Signals.Send(name, value)"), GuideCell.words("Sends the value, from 0 to 15,"
                        + " through the transmitter in front of the interface with that name, by setting the interface"
                        + " to emit it. It gives no when the interface faces no transmitter, or is not live."))
                .row(GuideCell.code("Signals.Heard()"), GuideCell.words("Every frequency heard where the machine"
                        + " stands, as a list of HeardSignal records, as a signal probe there would show them."))
                .table("Records of Signals", "Record", "Fields", "What each field holds")
                .row(GuideCell.code("RadioInfo"), GuideCell.code("RadioInfo.Role, RadioInfo.Band, RadioInfo.Frequency, "
                                + "RadioInfo.Value, RadioInfo.Clash"),
                        GuideCell.words("Role: transmitter, receiver or relay. Band: whose frequencies it is on."
                                + " Frequency: its number. Value: what a transmitter sends, or what a receiver hears."
                                + " Clash: yes when two transmitters of one owner send on its frequency."))
                .row(GuideCell.code("HeardSignal"), GuideCell.code("HeardSignal.Band, HeardSignal.Frequency, "
                                + "HeardSignal.Bars, HeardSignal.Value, HeardSignal.Clash, HeardSignal.Relayed"),
                        GuideCell.words("Band and Frequency: which frequency it is. Bars: how well it arrives. Value:"
                                + " what it carries. Clash: yes when it carries nothing for a clash. Relayed: yes when"
                                + " it arrives through a relay."))
                .paragraph("Send is the same as Redstone.Out on that interface: the transmitter sends what the lens"
                        + " gives it. The last setting wins, whichever call made it.")
                .define("Frequency", "A numbered line a transmitter sends one value on, from 1 to 9999.")
                .define("Clash", "Two transmitters of one owner on the same frequency. Its receivers give out nothing"
                        + " until one of them is taken off.")
                .ifSomethingGoesWrong(
                        "The program stops and says no Redstone Interface with that name is linked to this machine.",
                        "Link the interface to the machine with cable, and check its name in its window.",
                        "Signals.Send gives no.",
                        "The interface's lens does not face a transmitter, or the interface is not powered.")
                .seeAlso("jsc:redstone_interfaces", "jsc:redstone_library")
                .register();
    }
}
