/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.sigma;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * The versions of the language, one number for both: Σ N is the subset of Σ# N, so a project has a single number
 * and a program carried from Σ to Σ# keeps it.
 *
 * <p>A version only ever adds. What the first version took, every later one takes and compiles to the same listing,
 * so a newer compiler never breaks an older program; what a version holds a program to is only what came after it.
 * The number a build is held to is the newest the installed compiler knows, lowered by the project or at the
 * prompt, and never the other way: a compiler cannot be asked for a version it does not have.
 */
public final class SigmaVersions {

    /** The language as it first shipped under these names. */
    public static final int FIRST = 1;
    /** The newest version this compiler knows, which is what a build is held to when nothing lowers it. */
    public static final int NEWEST = 3;
    /**
     * The symbol a build for the debugger defines, which is what makes it one: such a build carries the columns and the
     * names of the variables the debugger shows where it stops, as {@code sgsc --debug} and the Debug configuration of
     * Virtual Studio build.
     */
    public static final String DEBUG = "DEBUG";
    /** The version that brought the directives, and the first whose symbol a build defines. */
    private static final int DIRECTIVES = 3;

    /** The types of the library that came after the first version, with the version each came in. */
    private static final Map<String, Integer> LIBRARY_TYPES = Map.ofEntries(Map.entry("Sound", 2),
            Map.entry("Speaker", 2), Map.entry("char", 2), Map.entry("FILE", 2), Map.entry("Bus", 2),
            Map.entry("BusItem", 2), Map.entry("Redstone", 2), Map.entry("CraftInterface", 2),
            Map.entry("CraftRouter", 2), Map.entry("TextArea", 2), Map.entry("NumberBox", 2), Map.entry("Slider", 2),
            Map.entry("RadioGroup", 2), Map.entry("ComboBox", 2), Map.entry("TabView", 2), Map.entry("GroupBox", 2),
            Map.entry("ScrollView", 2), Map.entry("Table", 2), Map.entry("TreeView", 2), Map.entry("MenuBar", 2),
            Map.entry("ContextMenu", 2), Map.entry("StatusBar", 2), Map.entry("Image", 2), Map.entry("Chart", 2),
            Map.entry("LogView", 2), Map.entry("OpenFileDialog", 2), Map.entry("SaveFileDialog", 2),
            Map.entry("ItemSlot", 2), Map.entry("ItemPicker", 2), Map.entry("OperationView", 2),
            Map.entry("ComponentAction", 2), Map.entry("GenericComponent", 2),
            // The exceptions came with try and catch.
            Map.entry("Exception", 3), Map.entry("NullReferenceException", 3), Map.entry("ObjectDisposedException", 3),
            Map.entry("DivideByZeroException", 3), Map.entry("IndexOutOfRangeException", 3),
            Map.entry("ArgumentException", 3), Map.entry("ArgumentOutOfRangeException", 3),
            Map.entry("InvalidOperationException", 3), Map.entry("FormatException", 3), Map.entry("IOException", 3),
            Map.entry("FileNotFoundException", 3), Map.entry("NetworkException", 3),
            Map.entry("AccessViolationException", 3),
            // The network's event log came to programs with Network.OnEvent.
            Map.entry("NetworkEvent", 3),
            // The machine's own readings: its power, its heat, its Wi-Fi.
            Map.entry("Power", 3), Map.entry("SupplyInfo", 3), Map.entry("UpsInfo", 3), Map.entry("Thermal", 3),
            Map.entry("Wireless", 3), Map.entry("DiskHealth", 3), Map.entry("Settings", 3), Map.entry("Sensors", 3),
            Map.entry("SensorReading", 3), Map.entry("Signals", 3), Map.entry("RadioInfo", 3),
            Map.entry("HeardSignal", 3),
            // The library of the third version.
            Map.entry("Calendar", 3), Map.entry("Stopwatch", 3), Map.entry("Path", 3), Map.entry("Directory", 3),
            Map.entry("FileEvent", 3), Map.entry("StringBuilder", 3), Map.entry("Regex", 3), Map.entry("Json", 3),
            Map.entry("JsonValue", 3), Map.entry("Csv", 3), Map.entry("Hash", 3), Map.entry("Base64", 3),
            Map.entry("Queue", 3), Map.entry("Stack", 3), Map.entry("Set", 3), Map.entry("EventLog", 3),
            Map.entry("Channel", 3), Map.entry("ChannelMessage", 3),
            // Flying J's Core's moving structures, and Teracoin.
            Map.entry("Navigation", 3), Map.entry("Wallet", 3), Map.entry("Chain", 3), Map.entry("Miner", 3),
            Map.entry("Pool", 3));
    /**
     * The members that came after the first version on a type that was already there, each written as its owner,
     * its name and the types it takes, with the version it came in.
     */
    private static final Map<String, Integer> LIBRARY_MEMBERS = Map.ofEntries(
            Map.entry("string.Compare(string, string)", 2), Map.entry("Convert.ToString(int, int)", 2),
            Map.entry("Convert.ToInt(string, int)", 2), Map.entry("Console.Print(char)", 2),
            Map.entry("Console.Read()", 2), Map.entry("Console.Scan(out int)", 2),
            Map.entry("Console.Scan(out long)", 2), Map.entry("Console.Scan(out double)", 2),
            Map.entry("Console.Scan(out string)", 2), Map.entry("Console.Scan(out char)", 2),
            Map.entry("File.Open(string, string)", 2), Map.entry("File.Move(string, string)", 2),
            Map.entry("Operations.Update(string, string, long)", 2),
            // The calls of raw memory came with pointers.
            Map.entry("Memory.Alloc(long)", 3), Map.entry("Memory.AllocZeroed(long, long)", 3),
            Map.entry("Memory.Resize(void*, long)", 3), Map.entry("Memory.Free(void*)", 3),
            Map.entry("Memory.Copy(void*, void*, long)", 3), Map.entry("Memory.Fill(void*, int, long)", 3),
            Map.entry("Memory.Compare(void*, void*, long)", 3),
            // The two more sources of events.
            Map.entry("Operations.OnSettled(Action<OperationInfo>)", 3),
            Map.entry("Operations.OnSettled(string, Action<OperationInfo>)", 3),
            Map.entry("Network.OnEvent(Action<NetworkEvent>)", 3),
            // A file's size and the tick it last changed, and being told when it changes.
            Map.entry("File.Size(string)", 3), Map.entry("File.Changed(string)", 3),
            Map.entry("File.Watch(string, Action<FileEvent>)", 3),
            // A list searched and sorted with a function of the program's own.
            Map.entry("List.Find(Func<T, bool>)", 3), Map.entry("List.Sort(Func<T, T, int>)", 3));
    /**
     * The types the smaller language's library took in later than the full one had them, with the version each
     * came in there: the full language always had Random, and the smaller one gained it with the old rand.
     */
    private static final Map<String, Integer> SUBSET_LATER = Map.of("Random", 2);

    private SigmaVersions() {
    }

    /**
     * The version a type of the library came in, in {@code level}; a type that has always been there came in the
     * first.
     */
    public static int sinceType(final LanguageLevel level, final String name) {
        final Integer later = level.full() ? null : SUBSET_LATER.get(name);
        return later != null ? later : LIBRARY_TYPES.getOrDefault(name, FIRST);
    }

    /**
     * The version a member of the library came in, in {@code level}: the version of its type, or a later one when
     * the member was added to a type that was already there.
     *
     * @param takes the types it takes, as a signature writes them between its brackets: {@code int, int}
     */
    public static int sinceMember(final LanguageLevel level, final String owner, final String name,
                                  final String takes) {
        return Math.max(sinceType(level, owner),
                LIBRARY_MEMBERS.getOrDefault(owner + "." + name + "(" + takes + ")", FIRST));
    }

    /**
     * The version a build asked for {@code asked} is held to: the newest when it asks for none (zero or less), and
     * never past the newest or below the first.
     */
    public static int held(final int asked) {
        return asked <= 0 ? NEWEST : Math.max(FIRST, Math.min(NEWEST, asked));
    }

    /** Whether {@code asked} is a version this compiler has. */
    public static boolean known(final int asked) {
        return asked >= FIRST && asked <= NEWEST;
    }

    /**
     * The symbols a build held to {@code asked} defines for its directives, which came with the third version:
     * {@code SIGMA_3}, and one for every version after it the build reaches.
     */
    public static Set<String> defined(final int asked) {
        final Set<String> symbols = new LinkedHashSet<>();
        for (int version = DIRECTIVES; version <= held(asked); version++) {
            symbols.add("SIGMA_" + version);
        }
        return symbols;
    }

    /**
     * The symbol a build defines for an instruction set or an era by its id: the id's name in capitals, {@code X86_64}
     * for {@code jsc:x86_64} and {@code STANDARD} for the Standard, which a directive can name where the id, with its
     * colon, could not be.
     */
    public static String symbolOf(final String id) {
        final int colon = id.indexOf(':');
        return (colon < 0 ? id : id.substring(colon + 1)).toUpperCase(Locale.ROOT).replace('-', '_');
    }

    /**
     * The version a compiler installed at {@code packageVersion} knows, which is its major number: sgsc 2.0 is Σ# 2.
     * A package with no version on record predates the record and is taken at the newest, as is one whose number
     * cannot be read.
     */
    public static int ofPackage(final String packageVersion) {
        if (packageVersion == null || packageVersion.isBlank()) {
            return NEWEST;
        }
        final int dot = packageVersion.indexOf('.');
        try {
            return held(Integer.parseInt(dot < 0 ? packageVersion.trim() : packageVersion.substring(0, dot).trim()));
        } catch (final NumberFormatException unreadable) {
            return NEWEST;
        }
    }
}
