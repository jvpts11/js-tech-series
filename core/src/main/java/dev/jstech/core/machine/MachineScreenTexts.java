/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.machine;

import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;

/** The words engraved on the machine screen's steel, and what its LCDs and plates say. */
@TextHolder
public final class MachineScreenTexts {

    public static final TextKey RUN = TextKey.of("jscore.machine.screen.run", "RUN");
    public static final TextKey WAIT = TextKey.of("jscore.machine.screen.wait", "WAIT");
    public static final TextKey FAULT = TextKey.of("jscore.machine.screen.fault", "FAULT");
    public static final TextKey FE = TextKey.of("jscore.machine.screen.fe", "FE");
    public static final TextKey TAB_MAIN = TextKey.of("jscore.machine.screen.tab.main", "Main");
    public static final TextKey TAB_SIDES = TextKey.of("jscore.machine.screen.tab.sides", "Sides");
    public static final TextKey TAB_REDSTONE = TextKey.of("jscore.machine.screen.tab.redstone", "Redstone");
    public static final TextKey TAB_UPGRADES = TextKey.of("jscore.machine.screen.tab.upgrades", "Upgrades");
    public static final TextKey TAB_PROTECTION = TextKey.of("jscore.machine.screen.tab.protection", "Protection");

    public static final TextKey PER_MINUTE = TextKey.of("jscore.machine.screen.per_minute", "%s/min");
    public static final TextKey PROGRESS = TextKey.of("jscore.machine.screen.progress", "%s%%");
    public static final TextKey ON = TextKey.of("jscore.machine.screen.on", "ON");
    public static final TextKey OFF = TextKey.of("jscore.machine.screen.off", "OFF");
    public static final TextKey LOCKED = TextKey.of("jscore.machine.screen.locked",
            "Locked to %s: Ctrl and click to unlock");
    public static final TextKey LOCK_HINT = TextKey.of("jscore.machine.screen.lock_hint",
            "Ctrl and click to lock it to what it holds");
    public static final TextKey METER_RANGE = TextKey.of("jscore.machine.screen.meter_range", "x%s");
    public static final TextKey NO_COUNT = TextKey.of("jscore.machine.screen.no_count", "--/min");
    public static final TextKey LCD_WORKING = TextKey.of("jscore.machine.screen.lcd.working", "RUNNING");
    public static final TextKey LCD_NOT_FORMED = TextKey.of("jscore.machine.screen.lcd.not_formed", "UNBUILT");
    public static final TextKey LCD_WAITING = TextKey.of("jscore.machine.screen.lcd.waiting", "NO INPUT");
    public static final TextKey LCD_LOCKED = TextKey.of("jscore.machine.screen.lcd.locked", "LOCKED");
    public static final TextKey LCD_OUTPUT_FULL = TextKey.of("jscore.machine.screen.lcd.output_full", "OUT FULL");
    public static final TextKey LCD_NO_ENERGY = TextKey.of("jscore.machine.screen.lcd.no_energy", "NO POWER");
    public static final TextKey LCD_UNDERVOLTAGE = TextKey.of("jscore.machine.screen.lcd.undervoltage", "LOW LINE");
    public static final TextKey LCD_OVERVOLTAGE = TextKey.of("jscore.machine.screen.lcd.overvoltage", "HI LINE");
    public static final TextKey LCD_FUSE_BLOWN = TextKey.of("jscore.machine.screen.lcd.fuse_blown", "FUSE OUT");
    public static final TextKey LCD_OVERHEATED = TextKey.of("jscore.machine.screen.lcd.overheated", "OVERHEAT");
    public static final TextKey LCD_TOO_SLOW = TextKey.of("jscore.machine.screen.lcd.too_slow", "TOO SLOW");
    public static final TextKey LCD_DAMAGED = TextKey.of("jscore.machine.screen.lcd.damaged", "DAMAGED");
    public static final TextKey LCD_OFF_BY_REDSTONE = TextKey.of("jscore.machine.screen.lcd.off_by_redstone",
            "REDSTONE");
    public static final TextKey LCD_SWITCHED_OFF = TextKey.of("jscore.machine.screen.lcd.switched_off", "OFF");
    public static final TextKey TIP_METER = TextKey.of("jscore.machine.screen.tip.meter",
            "Drawing %s; each mark on the scale is %s");
    public static final TextKey TIP_SWITCH = TextKey.of("jscore.machine.screen.tip.switch",
            "Its switch, %s: click to throw it");
    public static final TextKey TIP_RECIPES = TextKey.of("jscore.machine.screen.tip.recipes", "Show recipes");
    public static final TextKey TIP_TANK = TextKey.of("jscore.machine.screen.tip.tank", "%s: %s of %s mB");
    public static final TextKey TIP_EMPTY_TANK = TextKey.of("jscore.machine.screen.tip.empty_tank",
            "An empty tank of %s mB");
    public static final TextKey TIP_STORED = TextKey.of("jscore.machine.screen.tip.stored", "Energy: %s of %s");
    public static final TextKey TIP_NAME = TextKey.of("jscore.machine.screen.tip.name", "%s, owned by %s");
    public static final TextKey MOVE_ELSEWHERE = TextKey.of("jscore.machine.screen.move_elsewhere",
            "This machine does not work these recipes");
    public static final TextKey MOVE_MISSING = TextKey.of("jscore.machine.screen.move_missing",
            "You do not have everything this recipe takes");
    public static final TextKey MOVE_NO_ROOM = TextKey.of("jscore.machine.screen.move_no_room",
            "This machine has too few input slots for this recipe");

    public static final TextKey FRONT_VIEW = TextKey.of("jscore.machine.screen.front_view", "FRONT VIEW");
    public static final TextKey REAR_VIEW = TextKey.of("jscore.machine.screen.rear_view", "REAR VIEW");
    public static final TextKey FACE_TITLE = TextKey.of("jscore.machine.screen.face_title", "%s FACE");
    public static final TextKey TOP = TextKey.of("jscore.machine.screen.top", "TOP");
    public static final TextKey BOTTOM = TextKey.of("jscore.machine.screen.bottom", "BOTTOM");
    public static final TextKey FRONT = TextKey.of("jscore.machine.screen.front", "FRONT");
    public static final TextKey BACK = TextKey.of("jscore.machine.screen.back", "BACK");
    public static final TextKey LEFT = TextKey.of("jscore.machine.screen.left", "LEFT");
    public static final TextKey RIGHT = TextKey.of("jscore.machine.screen.right", "RIGHT");
    public static final TextKey MODE_IN = TextKey.of("jscore.machine.screen.mode.in", "IN");
    public static final TextKey MODE_OUT = TextKey.of("jscore.machine.screen.mode.out", "OUT");
    public static final TextKey MODE_BOTH = TextKey.of("jscore.machine.screen.mode.both", "BOTH");
    public static final TextKey MODE_OFF = TextKey.of("jscore.machine.screen.mode.off", "OFF");
    public static final TextKey MATTER = TextKey.of("jscore.machine.screen.matter", "MATTER");
    public static final TextKey PUSH = TextKey.of("jscore.machine.screen.push", "PUSH");
    public static final TextKey PULL = TextKey.of("jscore.machine.screen.pull", "PULL");
    public static final TextKey FILTER = TextKey.of("jscore.machine.screen.filter", "FILTER");
    public static final TextKey WHEEL_ITEMS = TextKey.of("jscore.machine.screen.wheel_items", "Items a second");
    public static final TextKey WHEEL_HUNDREDS = TextKey.of("jscore.machine.screen.wheel_hundreds",
            "Hundreds a second: millibuckets, or energy");
    public static final TextKey WHEEL_HINT = TextKey.of("jscore.machine.screen.wheel_hint",
            "Click a drum's top to turn it up, its bottom to turn it down");
    public static final TextKey TIP_RESOURCE = TextKey.of("jscore.machine.screen.tip.resource",
            "Show what each face does with %s");
    public static final TextKey TIP_MATTER = TextKey.of("jscore.machine.screen.tip.matter",
            "Show what each face does with %s; click again for the next state of matter");
    public static final TextKey TIP_FACE = TextKey.of("jscore.machine.screen.tip.face", "%s face, %s: %s");
    public static final TextKey TIP_FACE_USE = TextKey.of("jscore.machine.screen.tip.face_use",
            "Click to pick this face, right-click to turn its knob");
    public static final TextKey TIP_KNOB_USE = TextKey.of("jscore.machine.screen.tip.knob_use",
            "Click a word to turn the knob to it, or the knob to turn it on");
    public static final TextKey TIP_PUSH = TextKey.of("jscore.machine.screen.tip.push",
            "Pushes %s out of its giving faces by itself: %s");
    public static final TextKey TIP_PULL = TextKey.of("jscore.machine.screen.tip.pull",
            "Pulls %s in through its taking faces by itself: %s");
    public static final TextKey TIP_FILTER_ALL = TextKey.of("jscore.machine.screen.tip.filter_all",
            "Everything is pushed and pulled");
    public static final TextKey TIP_FILTER_ONLY = TextKey.of("jscore.machine.screen.tip.filter_only",
            "Only %s is pushed and pulled");
    public static final TextKey TIP_FILTER_USE = TextKey.of("jscore.machine.screen.tip.filter_use",
            "Click holding a thing to let only it pass, with nothing in hand to let everything pass");

    public static final TextKey RUNS = TextKey.of("jscore.machine.screen.runs", "RUNS");
    public static final TextKey COMPARATOR = TextKey.of("jscore.machine.screen.comparator", "COMPARATOR");
    public static final TextKey IGNORE = TextKey.of("jscore.machine.screen.ignore", "IGNORE");
    public static final TextKey HIGH = TextKey.of("jscore.machine.screen.high", "HIGH");
    public static final TextKey LOW = TextKey.of("jscore.machine.screen.low", "LOW");
    public static final TextKey PULSE = TextKey.of("jscore.machine.screen.pulse", "PULSE");
    public static final TextKey PROG = TextKey.of("jscore.machine.screen.prog", "PROG");
    public static final TextKey NRG = TextKey.of("jscore.machine.screen.nrg", "NRG");
    public static final TextKey SIGNAL_IN = TextKey.of("jscore.machine.screen.signal_in", "SIGNAL IN");
    public static final TextKey CMP_OUT = TextKey.of("jscore.machine.screen.cmp_out", "CMP OUT");
    public static final TextKey TIP_RUNS = TextKey.of("jscore.machine.screen.tip.runs", "How it answers redstone: %s");
    public static final TextKey TIP_READS = TextKey.of("jscore.machine.screen.tip.reads",
            "What a comparator reads of it: %s");
    public static final TextKey TIP_SIGNAL = TextKey.of("jscore.machine.screen.tip.signal",
            "Redstone coming in: %s of 15");
    public static final TextKey TIP_CMP_OUT = TextKey.of("jscore.machine.screen.tip.cmp_out",
            "A comparator beside it reads %s of 15");

    public static final TextKey UPGRADE_BAYS = TextKey.of("jscore.machine.screen.upgrade_bays", "UPGRADE BAYS");
    public static final TextKey EFFECT_SPEED = TextKey.of("jscore.machine.screen.effect.speed", "SPEED x%s");
    public static final TextKey EFFECT_ENERGY = TextKey.of("jscore.machine.screen.effect.energy", "ENERGY x%s");
    public static final TextKey EFFECT_PARALLEL = TextKey.of("jscore.machine.screen.effect.parallel", "AT ONCE %s");
    public static final TextKey EFFECT_YIELD = TextKey.of("jscore.machine.screen.effect.yield", "YIELD x%s");
    public static final TextKey EFFECT_BYPRODUCTS = TextKey.of("jscore.machine.screen.effect.byproducts",
            "BYPROD x%s");
    public static final TextKey EFFECT_USE = TextKey.of("jscore.machine.screen.effect.use", "USE x%s");
    public static final TextKey EFFECT_HEAT = TextKey.of("jscore.machine.screen.effect.heat", "HEAT x%s");
    public static final TextKey EFFECT_SOUND = TextKey.of("jscore.machine.screen.effect.sound", "SOUND x%s");
    public static final TextKey EFFECT_BUFFER = TextKey.of("jscore.machine.screen.effect.buffer", "BUFFER x%s");
    public static final TextKey NO_BAYS = TextKey.of("jscore.machine.screen.no_bays", "NO BAYS ON THIS MACHINE");
    public static final TextKey SPD = TextKey.of("jscore.machine.screen.spd", "SPD");
    public static final TextKey PAR = TextKey.of("jscore.machine.screen.par", "PAR");
    public static final TextKey YLD = TextKey.of("jscore.machine.screen.yld", "YLD");
    public static final TextKey BYP = TextKey.of("jscore.machine.screen.byp", "BYP");
    public static final TextKey USE = TextKey.of("jscore.machine.screen.use", "USE");
    public static final TextKey HEA = TextKey.of("jscore.machine.screen.hea", "HEA");
    public static final TextKey SND = TextKey.of("jscore.machine.screen.snd", "SND");
    public static final TextKey BUF = TextKey.of("jscore.machine.screen.buf", "BUF");
    public static final TextKey TIP_BAY = TextKey.of("jscore.machine.screen.tip.bay",
            "An upgrade bay: put an upgrade in it");
    public static final TextKey TIP_BANK = TextKey.of("jscore.machine.screen.tip.bank",
            "What its upgrades do: right of the middle mark is more than as it was built, left is less");

    public static final TextKey PORT = TextKey.of("jscore.machine.screen.port", "PORT  %s");
    public static final TextKey LINE_OK = TextKey.of("jscore.machine.screen.line_ok", "LINE OK");
    public static final TextKey LINE_LOW = TextKey.of("jscore.machine.screen.line_low", "LINE LOW");
    public static final TextKey LINE_HIGH = TextKey.of("jscore.machine.screen.line_high", "LINE HIGH");
    public static final TextKey OVERVOLT = TextKey.of("jscore.machine.screen.overvolt", "OVERVOLT");
    public static final TextKey FUSE = TextKey.of("jscore.machine.screen.fuse", "FUSE");
    public static final TextKey BLOWN = TextKey.of("jscore.machine.screen.blown", "BLOWN");
    public static final TextKey FUSE_OK = TextKey.of("jscore.machine.screen.fuse_ok", "READY");
    public static final TextKey NO_FUSE = TextKey.of("jscore.machine.screen.no_fuse", "NO FUSE");
    public static final TextKey MACHINE_OFF = TextKey.of("jscore.machine.screen.machine_off", "MACHINE OFF,");
    public static final TextKey UNHARMED = TextKey.of("jscore.machine.screen.unharmed", "UNHARMED");
    public static final TextKey UNPROTECTED = TextKey.of("jscore.machine.screen.unprotected", "UNPROTECTED");
    public static final TextKey GUARDING = TextKey.of("jscore.machine.screen.guarding", "GUARDING IT");
    public static final TextKey BATTERY = TextKey.of("jscore.machine.screen.battery", "BATTERY");
    public static final TextKey BATTERY_LEFT = TextKey.of("jscore.machine.screen.battery_left", "%s%%  %s");
    public static final TextKey NO_PORT = TextKey.of("jscore.machine.screen.no_port", "FE: NO FUSE OR BATTERY");
    public static final TextKey ACCESS = TextKey.of("jscore.machine.screen.access", "ACCESS");
    public static final TextKey PRIV = TextKey.of("jscore.machine.screen.priv", "PRIV");
    public static final TextKey TEAM = TextKey.of("jscore.machine.screen.team", "TEAM");
    public static final TextKey PUB = TextKey.of("jscore.machine.screen.pub", "PUB");
    public static final TextKey NOBODY = TextKey.of("jscore.machine.screen.nobody", "NOBODY");
    public static final TextKey OWNER_AWAY = TextKey.of("jscore.machine.screen.owner_away", "OWNER AWAY");
    public static final TextKey FUSE_RATING = TextKey.of("jscore.machine.screen.fuse_rating", "FUSE  %s");
    public static final TextKey FUSE_ANY = TextKey.of("jscore.machine.screen.fuse_any", "ANY LINE");
    public static final TextKey NO_TIME = TextKey.of("jscore.machine.screen.no_time", "--:--");
    public static final TextKey TIP_PORT = TextKey.of("jscore.machine.screen.tip.port",
            "Its port takes a line of %s; a lower one starves it, a higher one burns it");
    public static final TextKey TIP_FUSE = TextKey.of("jscore.machine.screen.tip.fuse",
            "A fuse blows when a line too high meets the port, cutting the machine off unharmed");
    public static final TextKey TIP_BATTERY = TextKey.of("jscore.machine.screen.tip.battery",
            "A battery charges while the line keeps it full, and runs the machine when the line goes down");
    public static final TextKey TIP_ACCESS = TextKey.of("jscore.machine.screen.tip.access",
            "Who may use it besides its owner: %s. Only its owner turns this key");
    public static final TextKey LCD_EFFICIENCY = TextKey.of("jscore.machine.screen.lcd.efficiency", "EFF %s%%");
    public static final TextKey LCD_SPAN = TextKey.of("jscore.machine.screen.lcd.span", "%sM");
    public static final TextKey TIP_LCD_STATISTICS = TextKey.of("jscore.machine.screen.tip.lcd_statistics",
            "Click: the last %s minutes");
    public static final TextKey TIP_LCD_READINGS = TextKey.of("jscore.machine.screen.tip.lcd_readings",
            "Click: back to the readings");
    public static final TextKey TIP_STATISTICS = TextKey.of("jscore.machine.screen.tip.statistics",
            "The last %s minutes");
    public static final TextKey TIP_NO_READING = TextKey.of("jscore.machine.screen.tip.no_reading",
            "No reading yet: one comes every %s s");
    public static final TextKey TIP_MADE = TextKey.of("jscore.machine.screen.tip.made",
            "Made: %s a minute, at most %s");
    public static final TextKey TIP_POWER = TextKey.of("jscore.machine.screen.tip.power",
            "Power: %s, %s on the whole");
    public static final TextKey TIP_EFFICIENCY = TextKey.of("jscore.machine.screen.tip.efficiency",
            "Efficiency: %s%%, working %s s of the last minute");
    public static final TextKey TIP_JAM_SECONDS = TextKey.of("jscore.machine.screen.tip.jam_seconds",
            "A jam %s s ago: %s");
    public static final TextKey TIP_JAM_MINUTE = TextKey.of("jscore.machine.screen.tip.jam_minute",
            "A jam a minute ago: %s");
    public static final TextKey TIP_JAM_MINUTES = TextKey.of("jscore.machine.screen.tip.jam_minutes",
            "A jam %s minutes ago: %s");

    private MachineScreenTexts() {
    }
}
