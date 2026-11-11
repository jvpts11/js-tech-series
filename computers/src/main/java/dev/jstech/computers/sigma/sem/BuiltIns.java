/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.sigma.sem;

import dev.jstech.computers.sigma.ast.IDecl;
import dev.jstech.computers.vm.system.ConstructorSpec;
import dev.jstech.computers.vm.system.EventSpec;
import dev.jstech.computers.vm.system.IMemberSpec;
import dev.jstech.computers.vm.system.MemberId;
import dev.jstech.computers.vm.system.MethodSpec;
import dev.jstech.computers.vm.system.PropertySpec;
import dev.jstech.computers.vm.system.SystemApi;
import dev.jstech.computers.vm.system.TypeSpec;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The types the language brings with it, before a single line of a player's program is read.
 *
 * <p>This is the language's own core: the root, strings, the two collections, the delegates a handler
 * is written as and the entry-point interface. Everything else a program reaches for, the library and
 * the machine it runs on, is declared once by the system and read in from those declarations.
 */
public final class BuiltIns {

    private static final Set<IDecl.Modifier> PUBLIC = Set.of(IDecl.Modifier.PUBLIC);
    private static final Set<IDecl.Modifier> PUBLIC_STATIC = Set.of(IDecl.Modifier.PUBLIC, IDecl.Modifier.STATIC);
    /** What the base class offers: a body of its own that does nothing, for a script to put its own in place of. */
    private static final Set<IDecl.Modifier> PUBLIC_VIRTUAL = Set.of(IDecl.Modifier.PUBLIC, IDecl.Modifier.VIRTUAL);
    /** How a declaration marks a parameter the method fills in. */
    private static final String OUT = "out ";

    private final Map<String, NamedType> types = new LinkedHashMap<>();

    private final NamedType objectType;
    private final NamedType stringType;
    private final NamedType listType;
    private final NamedType mapType;
    private final NamedType actionType;
    private final NamedType actionOfType;
    private final NamedType funcType;
    /** A function of two values, which a sort compares two elements with. */
    private final NamedType funcOfTwoType;
    private final NamedType queueType;
    private final NamedType stackType;
    private final NamedType setType;
    private final NamedType scriptType;
    private final NamedType scriptBase;
    private final NamedType systemType;
    private final NamedType desktopType;

    public BuiltIns() {
        this.objectType = this.declare("object", NamedType.Kind.CLASS);
        this.stringType = this.declare("string", NamedType.Kind.CLASS);
        this.listType = this.declare("List", NamedType.Kind.CLASS, "T");
        this.mapType = this.declare("Map", NamedType.Kind.CLASS, "K", "V");
        this.actionType = this.declare("Action", NamedType.Kind.DELEGATE);
        this.actionOfType = this.declare("Action", NamedType.Kind.DELEGATE, "T");
        this.funcType = this.declare("Func", NamedType.Kind.DELEGATE, "T", "R");
        this.funcOfTwoType = this.declare("Func", NamedType.Kind.DELEGATE, "T1", "T2", "R");
        this.queueType = this.declare("Queue", NamedType.Kind.CLASS, "T");
        this.stackType = this.declare("Stack", NamedType.Kind.CLASS, "T");
        this.setType = this.declare("Set", NamedType.Kind.CLASS, "T");
        this.scriptType = this.declare("IScript", NamedType.Kind.INTERFACE);
        this.scriptBase = this.declare("Script", NamedType.Kind.CLASS);
        this.systemType = this.declare("ISystem", NamedType.Kind.INTERFACE);
        this.desktopType = this.declare("IDesktop", NamedType.Kind.INTERFACE);

        this.fillString();
        this.fillCharacter(this.declare("char", NamedType.Kind.CLASS));
        this.fillList();
        this.fillMap();
        this.fillQueueAndStack();
        this.fillSet();
        this.fillDelegates();
        this.fillScript();
        this.fillDeclared();
        this.fillSystem();
        this.fillExceptions();
    }

    /** The type every exception stands on. */
    public NamedType exceptionType() {
        return this.types.get(key(EXCEPTION, 0));
    }

    /**
     * The exceptions (computers/docs/design/sigma.md), each standing on the one above it: what they hold, read but
     * never written by a program, and the three constructors each has, as C#'s have them.
     */
    private void fillExceptions() {
        final NamedType root = this.declare(EXCEPTION, NamedType.Kind.CLASS);
        final ITypeSymbol text = this.stringType;
        this.property(root, "Message", text, PUBLIC);
        this.property(root, "File", text, PUBLIC);
        this.property(root, "Line", ITypeSymbol.Primitive.INT, PUBLIC);
        this.property(root, "Trace", new ITypeSymbol.ArrayType(text), PUBLIC);
        this.property(root, "Inner", root, PUBLIC);
        for (final Map.Entry<String, String> one : EXCEPTIONS.entrySet()) {
            final NamedType made = one.getKey().equals(EXCEPTION) ? root
                    : this.declare(one.getKey(), NamedType.Kind.CLASS);
            if (one.getValue() != null) {
                made.setBase(this.types.get(key(one.getValue(), 0)));
            }
            made.addMember(new IMemberSymbol.ConstructorSymbol(made, List.of(), PUBLIC));
            made.addMember(new IMemberSymbol.ConstructorSymbol(made,
                    List.of(IMemberSymbol.ParameterSymbol.of("message", text)), PUBLIC));
            made.addMember(new IMemberSymbol.ConstructorSymbol(made, List.of(IMemberSymbol.ParameterSymbol
                    .of("message", text), IMemberSymbol.ParameterSymbol.of("inner", root)), PUBLIC));
        }
    }

    /** The interface an operating system a player writes implements (computers/docs/design/player-systems.md). */
    public NamedType systemType() {
        return this.systemType;
    }

    /** The interface a desktop a player writes implements. */
    public NamedType desktopType() {
        return this.desktopType;
    }

    /** The root of every reference type. */
    public NamedType objectType() {
        return this.objectType;
    }

    /** Text. */
    public NamedType stringType() {
        return this.stringType;
    }

    /** The growable sequence. */
    public NamedType listType() {
        return this.listType;
    }

    /** The keyed collection. */
    public NamedType mapType() {
        return this.mapType;
    }

    /** The interface a program's entry point implements, which the base class below also carries. */
    public NamedType scriptType() {
        return this.scriptType;
    }

    /** The class a script may stand on instead, filling in only the three it uses. */
    public NamedType scriptBase() {
        return this.scriptBase;
    }

    /**
     * The type of that name, or null. Two delegates share the name Action and are told apart by how
     * many arguments they were given; a name written with the wrong number still resolves, so the
     * mistake is reported as the wrong count rather than as an unknown name.
     */
    public NamedType type(final String name, final int arity) {
        final NamedType exact = this.types.get(key(name, arity));
        if (exact != null) {
            return exact;
        }
        for (final NamedType type : this.types.values()) {
            if (type.name().equals(name)) {
                return type;
            }
        }
        return null;
    }

    /** Every built-in type, in the order they were declared. */
    public List<NamedType> all() {
        return List.copyOf(this.types.values());
    }

    /** Whether a name belongs to the language rather than to the player. */
    public boolean isReserved(final String name) {
        return this.type(name, 0) != null;
    }

    /*
     * Where the language keeps its types. Everything a program reaches for lives under System, sorted
     * by what it is about, and has to be brought in with a using before its bare name means anything;
     * only the two roots every program is made of, the text and the object, belong to no namespace.
     */
    /** The root of the language's own namespaces. */
    public static final String SYSTEM = "System";
    /**
     * The one namespace the smaller language can reach.
     *
     * <p>Its types are a handful on purpose: these are the machines that had a screen, a disk, a clock and not
     * much else, and a library they could not have held would be a lie about them. Every name in it is a type the
     * full language also has, under the namespace it has always had, so a call written in the subset compiles to
     * the very same line of assembly; what the subset does not get is every other type, and most of the members of
     * these.
     */
    public static final String SUBSET_LIBRARY = "Standard";

    private static final String COLLECTIONS = "System.Collections";
    private static final String IO = "System.IO";
    private static final String UTILS = "System.Utils";
    private static final String MACHINE = "System.Machine";
    private static final String NETWORK = "System.Network";
    private static final String OPERATIONS = "System.Operations";
    private static final String EXECUTION = "System.Execution";
    private static final String UI = "System.UI";
    private static final String SOUND = "System.Sound";

    /** What a system and a desktop both say of themselves, as text: their name, their version, who wrote them. */
    public static final List<String> SYSTEM_TEXTS = List.of("Name", "Version", "Author");
    /**
     * What a desktop says of its look and its programs, as text: the word on its menu button, where its panel is
     * ("top" or "bottom"), the style of its window frames, its colours, the folder of its icons, its wallpaper, and its
     * programs, by name and in their order on the menu.
     */
    public static final List<String> DESKTOP_TEXTS = List.of("Menu", "PanelAt", "Frames", "DesktopColour",
            "PanelColour", "TextColour", "AccentColour", "WindowColour", "TitleColour", "Icons", "Wallpaper",
            "Programs");

    /** The type every exception stands on. */
    public static final String EXCEPTION = "Exception";
    /** The exceptions, each with the one it stands on, a type standing before any that stands on it. */
    public static final Map<String, String> EXCEPTIONS = exceptions();

    private static final Map<String, String> HOMES = homes();

    /**
     * Every type of the subset's library, by the name it has in both languages.
     *
     * <p>A type keeps its own name here rather than taking a second one, because it is the same type: naming the
     * machine {@code Machine} under one namespace and {@code Computer} under the other would be two names for one
     * thing with nothing gained, and a program moved between the languages would have to be rewritten to say it.
     */
    private static final Set<String> SUBSET_TYPES =
            Set.of("Console", "File", "Program", "Math", "Convert", "Time", "Computer", "Script", "Sound", "Speaker",
                    "Random", "FILE", "Bus", "BusItem", "Redstone", "CraftInterface", "CraftRouter",
                    // The calls of a player's system (computers/docs/design/player-systems.md), as Standard has them.
                    "Boot", "Panic", "Screen", "Processes", "Disk", "Memory", "Services", "Drivers", "Desktop");

    /**
     * The type known by exactly {@code fullName}, its namespace in front ({@code System.IO.Console}),
     * with {@code arity} arguments or, when there is none of that count, whichever there is; {@code -1}
     * asks for whichever. A bare name of a type that lives in a namespace is not found here.
     */
    public NamedType qualified(final String fullName, final int arity) {
        /*
         * The smaller language's whole library lives under one namespace, and it is the same handful of types
         * the bigger one keeps scattered across System.IO, System.Utils and the rest. They are one type seen
         * under two names rather than two types: a call written in the subset has to compile to the very same
         * line of assembly the full language would write, or a program carried from an old machine to a new one
         * would not be the same program.
         */
        if (fullName.startsWith(SUBSET_LIBRARY + ".")) {
            final String simple = fullName.substring(SUBSET_LIBRARY.length() + 1);
            if (SUBSET_TYPES.contains(simple)) {
                // None of these takes arguments, so the count asked for decides nothing and may be "whichever".
                for (final NamedType type : this.types.values()) {
                    if (type.name().equals(simple)) {
                        return type;
                    }
                }
            }
        }
        NamedType any = null;
        for (final NamedType type : this.types.values()) {
            if (!type.fullName().equals(fullName)) {
                continue;
            }
            if (type.typeParameters().size() == arity) {
                return type;
            }
            if (any == null) {
                any = type;
            }
        }
        return any;
    }

    /** Whether {@code prefix} is one of the language's namespaces, or the start of one: System, System.IO. */
    public boolean isNamespace(final String prefix) {
        if (SUBSET_LIBRARY.equals(prefix)) {
            return true;
        }
        for (final String home : HOMES.values()) {
            if (home.equals(prefix) || home.startsWith(prefix + ".")) {
                return true;
            }
        }
        for (final TypeSpec declared : SystemApi.types()) {
            if (declared.namespace().equals(prefix) || declared.namespace().startsWith(prefix + ".")) {
                return true;
            }
        }
        return false;
    }

    /** The namespace a bare name would be found in, or null when the language has no type of that name there. */
    public String homeOf(final String name) {
        final String home = HOMES.get(name);
        if (home != null) {
            return home;
        }
        final TypeSpec declared = SystemApi.type(name);
        return declared == null ? null : declared.namespace();
    }

    /** Every namespace the language has, System first, for a list that offers them. */
    public List<String> namespaces() {
        return List.of(SYSTEM, COLLECTIONS, IO, SystemApi.TEXT, UTILS, MACHINE, NETWORK, OPERATIONS, EXECUTION, UI,
                SOUND, SystemApi.OS, SUBSET_LIBRARY);
    }

    private static String key(final String name, final int arity) {
        return name + "/" + arity;
    }

    /* The exceptions, in an order that puts a type before every one standing on it. */
    private static Map<String, String> exceptions() {
        final Map<String, String> made = new LinkedHashMap<>();
        made.put(EXCEPTION, null);
        made.put("NullReferenceException", EXCEPTION);
        made.put("InvalidOperationException", EXCEPTION);
        made.put("ObjectDisposedException", "InvalidOperationException");
        made.put("DivideByZeroException", EXCEPTION);
        made.put("IndexOutOfRangeException", EXCEPTION);
        made.put("ArgumentException", EXCEPTION);
        made.put("ArgumentOutOfRangeException", "ArgumentException");
        made.put("FormatException", EXCEPTION);
        made.put("IOException", EXCEPTION);
        made.put("FileNotFoundException", "IOException");
        made.put("NetworkException", EXCEPTION);
        made.put("AccessViolationException", EXCEPTION);
        return Collections.unmodifiableMap(made);
    }

    /* Where each type of the language lives that is not one of the system's declared types. */
    private static Map<String, String> homes() {
        final Map<String, String> made = new HashMap<>(Map.ofEntries(
                Map.entry("IScript", SYSTEM), Map.entry("Script", SUBSET_LIBRARY),
                Map.entry("ISystem", SystemApi.OS), Map.entry("IDesktop", SystemApi.OS),
                Map.entry("Action", SYSTEM), Map.entry("Func", SYSTEM),
                Map.entry("List", COLLECTIONS), Map.entry("Map", COLLECTIONS), Map.entry("Queue", COLLECTIONS),
                Map.entry("Stack", COLLECTIONS), Map.entry("Set", COLLECTIONS)));
        // The exceptions live under System itself, as C#'s do.
        EXCEPTIONS.keySet().forEach(name -> made.put(name, SYSTEM));
        return Collections.unmodifiableMap(made);
    }

    private NamedType declare(final String name, final NamedType.Kind kind, final String... parameters) {
        final NamedType type = new NamedType(name, kind, List.of(parameters), true);
        type.setNamespace(HOMES.getOrDefault(name, ""));
        this.types.put(key(name, parameters.length), type);
        return type;
    }

    private void method(final NamedType owner, final String name, final ITypeSymbol returns,
                        final Set<IDecl.Modifier> modifiers, final ITypeSymbol... takes) {
        final List<IMemberSymbol.ParameterSymbol> parameters = new ArrayList<>();
        for (int i = 0; i < takes.length; i++) {
            parameters.add(IMemberSymbol.ParameterSymbol.of("a" + i, takes[i]));
        }
        owner.addMember(new IMemberSymbol.MethodSymbol(owner, name, returns, parameters, modifiers));
    }

    private void property(final NamedType owner, final String name, final ITypeSymbol type,
                          final Set<IDecl.Modifier> modifiers) {
        owner.addMember(new IMemberSymbol.PropertySymbol(owner, name, type, true, false, modifiers, Set.of()));
    }

    private void fillString() {
        final ITypeSymbol text = this.stringType;
        final ITypeSymbol integer = ITypeSymbol.Primitive.INT;
        final ITypeSymbol flag = ITypeSymbol.Primitive.BOOL;
        this.property(this.stringType, "Length", integer, PUBLIC);
        this.method(this.stringType, "Substring", text, PUBLIC, integer);
        this.method(this.stringType, "Substring", text, PUBLIC, integer, integer);
        this.method(this.stringType, "IndexOf", integer, PUBLIC, text);
        this.method(this.stringType, "Contains", flag, PUBLIC, text);
        this.method(this.stringType, "StartsWith", flag, PUBLIC, text);
        this.method(this.stringType, "EndsWith", flag, PUBLIC, text);
        this.method(this.stringType, "ToUpper", text, PUBLIC);
        this.method(this.stringType, "ToLower", text, PUBLIC);
        this.method(this.stringType, "Trim", text, PUBLIC);
        this.method(this.stringType, "Replace", text, PUBLIC, text, text);
        this.method(this.stringType, "Split", new ITypeSymbol.GenericType(this.listType, List.of(text)),
                PUBLIC, ITypeSymbol.Primitive.CHAR);
        this.method(this.stringType, "Format", text, PUBLIC_STATIC, text, this.objectType);
        this.method(this.stringType, "Format", text, PUBLIC_STATIC, text, this.objectType, this.objectType);
        // Which of two texts comes first, character by character: below zero, zero when they are the same, above.
        this.method(this.stringType, "Compare", integer, PUBLIC_STATIC, text, text);
    }

    /**
     * What the language says about a single character, asked of the type by its keyword as C# asks it:
     * {@code char.IsDigit(c)}. A character itself has no members, since it runs as its number; the type is only
     * reached for these.
     */
    private void fillCharacter(final NamedType character) {
        final ITypeSymbol letter = ITypeSymbol.Primitive.CHAR;
        final ITypeSymbol flag = ITypeSymbol.Primitive.BOOL;
        this.method(character, "ToUpper", letter, PUBLIC_STATIC, letter);
        this.method(character, "ToLower", letter, PUBLIC_STATIC, letter);
        this.method(character, "IsDigit", flag, PUBLIC_STATIC, letter);
        this.method(character, "IsLetter", flag, PUBLIC_STATIC, letter);
        this.method(character, "IsWhiteSpace", flag, PUBLIC_STATIC, letter);
    }

    private void fillList() {
        final ITypeSymbol item = new ITypeSymbol.TypeParameter("T", 0);
        final ITypeSymbol integer = ITypeSymbol.Primitive.INT;
        final ITypeSymbol flag = ITypeSymbol.Primitive.BOOL;
        final ITypeSymbol nothing = ITypeSymbol.Primitive.VOID;
        this.property(this.listType, "Count", integer, PUBLIC);
        this.method(this.listType, "Add", nothing, PUBLIC, item);
        this.method(this.listType, "Insert", nothing, PUBLIC, integer, item);
        this.method(this.listType, "RemoveAt", nothing, PUBLIC, integer);
        this.method(this.listType, "Remove", flag, PUBLIC, item);
        this.method(this.listType, "Clear", nothing, PUBLIC);
        this.method(this.listType, "Contains", flag, PUBLIC, item);
        this.method(this.listType, "IndexOf", integer, PUBLIC, item);
        this.method(this.listType, "Get", item, PUBLIC, integer);
        this.method(this.listType, "Set", nothing, PUBLIC, integer, item);
        this.method(this.listType, "Sort", nothing, PUBLIC);
        /*
         * The third version's, each taking a function the compiler writes the walk around (ListLambdas): the first
         * element a test says yes to, or the type's empty value; and a sort by a comparison of two elements, below zero
         * when the first comes first.
         */
        this.method(this.listType, "Find", item, PUBLIC,
                new ITypeSymbol.GenericType(this.funcType, List.of(item, flag)));
        this.method(this.listType, "Sort", nothing, PUBLIC,
                new ITypeSymbol.GenericType(this.funcOfTwoType, List.of(item, item, integer)));
    }

    /** A queue takes from the front what it was given first; a stack takes from the top what it was given last. */
    private void fillQueueAndStack() {
        final ITypeSymbol item = new ITypeSymbol.TypeParameter("T", 0);
        final ITypeSymbol nothing = ITypeSymbol.Primitive.VOID;
        for (final NamedType type : List.of(this.queueType, this.stackType)) {
            this.property(type, "Count", ITypeSymbol.Primitive.INT, PUBLIC);
            this.method(type, "Peek", item, PUBLIC);
            this.method(type, "Clear", nothing, PUBLIC);
            this.method(type, "Contains", ITypeSymbol.Primitive.BOOL, PUBLIC, item);
        }
        this.method(this.queueType, "Enqueue", nothing, PUBLIC, item);
        this.method(this.queueType, "Dequeue", item, PUBLIC);
        this.method(this.stackType, "Push", nothing, PUBLIC, item);
        this.method(this.stackType, "Pop", item, PUBLIC);
    }

    /** A set holds each value once: adding one it holds changes nothing, and says so. */
    private void fillSet() {
        final ITypeSymbol item = new ITypeSymbol.TypeParameter("T", 0);
        final ITypeSymbol flag = ITypeSymbol.Primitive.BOOL;
        this.property(this.setType, "Count", ITypeSymbol.Primitive.INT, PUBLIC);
        this.method(this.setType, "Add", flag, PUBLIC, item);
        this.method(this.setType, "Remove", flag, PUBLIC, item);
        this.method(this.setType, "Contains", flag, PUBLIC, item);
        this.method(this.setType, "Clear", ITypeSymbol.Primitive.VOID, PUBLIC);
        this.method(this.setType, "Items", new ITypeSymbol.GenericType(this.listType, List.of(item)), PUBLIC);
    }

    private void fillMap() {
        final ITypeSymbol key = new ITypeSymbol.TypeParameter("K", 0);
        final ITypeSymbol value = new ITypeSymbol.TypeParameter("V", 1);
        final ITypeSymbol nothing = ITypeSymbol.Primitive.VOID;
        this.property(this.mapType, "Count", ITypeSymbol.Primitive.INT, PUBLIC);
        this.method(this.mapType, "Put", nothing, PUBLIC, key, value);
        this.method(this.mapType, "Get", value, PUBLIC, key);
        this.method(this.mapType, "ContainsKey", ITypeSymbol.Primitive.BOOL, PUBLIC, key);
        /*
         * The one lookup that answers both questions at once: whether the key was there, and what it
         * held. It is why the language has an outward parameter at all.
         */
        this.mapType.addMember(new IMemberSymbol.MethodSymbol(this.mapType, "TryGet",
                ITypeSymbol.Primitive.BOOL,
                List.of(IMemberSymbol.ParameterSymbol.of("key", key),
                        new IMemberSymbol.ParameterSymbol("value", value, true)), PUBLIC));
        this.method(this.mapType, "Remove", ITypeSymbol.Primitive.BOOL, PUBLIC, key);
        this.method(this.mapType, "Keys", new ITypeSymbol.GenericType(this.listType, List.of(key)), PUBLIC);
        this.method(this.mapType, "Values", new ITypeSymbol.GenericType(this.listType, List.of(value)), PUBLIC);
    }

    private void fillDelegates() {
        this.actionType.setInvoke(new IMemberSymbol.MethodSymbol(this.actionType, "Invoke",
                ITypeSymbol.Primitive.VOID, List.of(), PUBLIC));
        this.actionOfType.setInvoke(new IMemberSymbol.MethodSymbol(this.actionOfType, "Invoke",
                ITypeSymbol.Primitive.VOID,
                List.of(IMemberSymbol.ParameterSymbol.of("value", new ITypeSymbol.TypeParameter("T", 0))), PUBLIC));
        this.funcType.setInvoke(new IMemberSymbol.MethodSymbol(this.funcType, "Invoke",
                new ITypeSymbol.TypeParameter("R", 1),
                List.of(IMemberSymbol.ParameterSymbol.of("value", new ITypeSymbol.TypeParameter("T", 0))), PUBLIC));
        this.funcOfTwoType.setInvoke(new IMemberSymbol.MethodSymbol(this.funcOfTwoType, "Invoke",
                new ITypeSymbol.TypeParameter("R", 2),
                List.of(IMemberSymbol.ParameterSymbol.of("first", new ITypeSymbol.TypeParameter("T1", 0)),
                        IMemberSymbol.ParameterSymbol.of("second", new ITypeSymbol.TypeParameter("T2", 1))), PUBLIC));
    }

    /**
     * The two ways of writing a program that stays up, which are one thing underneath.
     *
     * <p>{@code IScript} is the full language's and always was: a class says it is a script and writes all three.
     * {@code Script} is a class to stand on, whose three do nothing, so a script fills in only what it uses. The
     * subset has no interfaces at all, so it is the only form there, and the full language keeps both, which is
     * what lets a subset program compile unchanged as a full one.
     *
     * <p>{@code Script} implements {@code IScript}, so "does this type descend from IScript" remains the single
     * question that finds a script, and nothing further down learns a second shape to look for.
     */
    private void fillScript() {
        this.method(this.scriptType, "OnInit", ITypeSymbol.Primitive.VOID, PUBLIC);
        this.method(this.scriptType, "OnTick", ITypeSymbol.Primitive.VOID, PUBLIC);
        this.method(this.scriptType, "OnDestroy", ITypeSymbol.Primitive.VOID, PUBLIC);
        this.scriptBase.addInterface(this.scriptType);
        this.method(this.scriptBase, "OnInit", ITypeSymbol.Primitive.VOID, PUBLIC_VIRTUAL);
        this.method(this.scriptBase, "OnTick", ITypeSymbol.Primitive.VOID, PUBLIC_VIRTUAL);
        this.method(this.scriptBase, "OnDestroy", ITypeSymbol.Primitive.VOID, PUBLIC_VIRTUAL);
    }

    /*
     * What a player's system and desktop declare and answer to. Filled after the library, whose eras, instruction
     * sets, kernels and input events they speak of. The values are properties written with an arrow, which the build
     * reads; the system's methods are what the machine calls on it.
     */
    private void fillSystem() {
        final ITypeSymbol text = this.stringType;
        for (final String name : SYSTEM_TEXTS) {
            this.property(this.systemType, name, text, PUBLIC);
            this.property(this.desktopType, name, text, PUBLIC);
        }
        this.property(this.systemType, "Era", this.types.get(key("Era", 0)), PUBLIC);
        this.property(this.systemType, "Isa", this.types.get(key("Isa", 0)), PUBLIC);
        this.property(this.systemType, "MemoryMb", ITypeSymbol.Primitive.INT, PUBLIC);
        this.property(this.systemType, "Kernel", this.types.get(key("Kernel", 0)), PUBLIC);
        this.method(this.systemType, "OnBoot", ITypeSymbol.Primitive.VOID, PUBLIC);
        this.method(this.systemType, "OnTick", ITypeSymbol.Primitive.VOID, PUBLIC);
        this.method(this.systemType, "OnInput", ITypeSymbol.Primitive.VOID, PUBLIC,
                this.types.get(key("InputEvent", 0)));
        this.method(this.systemType, "OnShutdown", ITypeSymbol.Primitive.VOID, PUBLIC);
        for (final String name : DESKTOP_TEXTS) {
            this.property(this.desktopType, name, text, PUBLIC);
        }
    }

    /**
     * The library's and the machine's types, as the system declares them. Every one is named before any is filled in,
     * so a member may speak of a type declared after its own.
     */
    private void fillDeclared() {
        final List<TypeSpec> specs = SystemApi.types();
        final List<NamedType> named = new ArrayList<>(specs.size());
        for (final TypeSpec spec : specs) {
            final NamedType type = this.declare(spec.name(), NamedType.Kind.CLASS);
            type.setNamespace(spec.namespace());
            named.add(type);
        }
        for (int i = 0; i < specs.size(); i++) {
            if (!specs.get(i).base().isEmpty()) {
                named.get(i).setBase(this.known(specs.get(i).base(), 0));
            }
        }
        for (int i = 0; i < specs.size(); i++) {
            final NamedType type = named.get(i);
            for (final IMemberSpec member : specs.get(i).members()) {
                type.addMember(this.member(type, member));
            }
        }
    }

    /** The compiler's picture of one declared member. */
    private IMemberSymbol member(final NamedType owner, final IMemberSpec member) {
        final Set<IDecl.Modifier> modifiers = member.isStatic() ? PUBLIC_STATIC : PUBLIC;
        return switch (member) {
            case MethodSpec method -> new IMemberSymbol.MethodSymbol(owner, method.id().name(),
                    this.resolve(method.returns()), this.parameters(method.id()), modifiers);
            case PropertySpec property -> new IMemberSymbol.PropertySymbol(owner, property.id().name(),
                    this.resolve(property.type()), true, property.writable(), modifiers, Set.of());
            case ConstructorSpec constructor -> new IMemberSymbol.ConstructorSymbol(owner,
                    this.parameters(constructor.id()), PUBLIC);
            case EventSpec event -> new IMemberSymbol.EventSymbol(owner, event.id().name(),
                    this.known(event.handler(), 0), modifiers);
        };
    }

    /** What a declared call takes, each named by where it stands, since nothing ever shows a built-in's names. */
    private List<IMemberSymbol.ParameterSymbol> parameters(final MemberId id) {
        final List<IMemberSymbol.ParameterSymbol> parameters = new ArrayList<>();
        for (final String written : id.parameters()) {
            final boolean outward = written.startsWith(OUT);
            parameters.add(new IMemberSymbol.ParameterSymbol("a" + parameters.size(),
                    this.resolve(outward ? written.substring(OUT.length()) : written), outward));
        }
        return parameters;
    }

    /**
     * The type a declaration names, written as a listing writes it: {@code int}, {@code string},
     * {@code List<DiskInfo>}, {@code Action<StockEvent>}.
     */
    private ITypeSymbol resolve(final String written) {
        final String text = written.strip();
        if (text.endsWith("*")) {
            return new ITypeSymbol.PointerType(this.resolve(text.substring(0, text.length() - 1)));
        }
        final int open = text.indexOf('<');
        if (open > 0 && text.endsWith(">")) {
            final List<ITypeSymbol> arguments = new ArrayList<>();
            int depth = 0;
            int from = open + 1;
            for (int i = from; i < text.length() - 1; i++) {
                final char c = text.charAt(i);
                if (c == '<') {
                    depth++;
                } else if (c == '>') {
                    depth--;
                } else if (c == ',' && depth == 0) {
                    arguments.add(this.resolve(text.substring(from, i)));
                    from = i + 1;
                }
            }
            arguments.add(this.resolve(text.substring(from, text.length() - 1)));
            return new ITypeSymbol.GenericType(this.known(text.substring(0, open), arguments.size()), arguments);
        }
        final ITypeSymbol.Primitive primitive = ITypeSymbol.Primitive.written(text);
        return primitive != null ? primitive : this.known(text, 0);
    }

    /** The type of that name and arity, which a declaration may only name once it exists. */
    private NamedType known(final String name, final int arity) {
        final NamedType type = this.type(name, arity);
        if (type == null) {
            throw new IllegalStateException("the system declares a member with the unknown type " + name);
        }
        return type;
    }
}
