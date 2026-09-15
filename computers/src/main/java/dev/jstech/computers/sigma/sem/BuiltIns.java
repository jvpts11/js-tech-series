/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.sigma.sem;

import dev.jstech.computers.sigma.ast.IDecl;
import dev.jstech.computers.vm.system.IMemberSpec;
import dev.jstech.computers.vm.system.MemberId;
import dev.jstech.computers.vm.system.MethodSpec;
import dev.jstech.computers.vm.system.PropertySpec;
import dev.jstech.computers.vm.system.SystemApi;
import dev.jstech.computers.vm.system.TypeSpec;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The types the language brings with it, before a single line of a player's program is read.
 *
 * <p>This is the language's own universe: the root, strings, the two collections, the delegates a
 * handler is written as, the entry-point interface, and the parts of the library that are pure
 * calculation. The objects that reach out into the world get declared where they are implemented,
 * beside the network they read, so nothing here has to know that a network exists.
 */
public final class BuiltIns {

    private static final Set<IDecl.Modifier> PUBLIC = Set.of(IDecl.Modifier.PUBLIC);
    private static final Set<IDecl.Modifier> PUBLIC_STATIC = Set.of(IDecl.Modifier.PUBLIC, IDecl.Modifier.STATIC);
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
    private final NamedType scriptType;

    public BuiltIns() {
        this.objectType = this.declare("object", NamedType.Kind.CLASS);
        this.stringType = this.declare("string", NamedType.Kind.CLASS);
        this.listType = this.declare("List", NamedType.Kind.CLASS, "T");
        this.mapType = this.declare("Map", NamedType.Kind.CLASS, "K", "V");
        this.actionType = this.declare("Action", NamedType.Kind.DELEGATE);
        this.actionOfType = this.declare("Action", NamedType.Kind.DELEGATE, "T");
        this.funcType = this.declare("Func", NamedType.Kind.DELEGATE, "T", "R");
        this.scriptType = this.declare("IScript", NamedType.Kind.INTERFACE);

        this.fillString();
        this.fillList();
        this.fillMap();
        this.fillDelegates();
        this.fillScript();
        this.fillDeclared();
        this.fillUi();
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

    /** The interface a program's entry point implements. */
    public NamedType scriptType() {
        return this.scriptType;
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
    private static final String COLLECTIONS = "System.Collections";
    private static final String IO = "System.IO";
    private static final String UTILS = "System.Utils";
    private static final String MACHINE = "System.Machine";
    private static final String NETWORK = "System.Network";
    private static final String OPERATIONS = "System.Operations";
    private static final String EXECUTION = "System.Execution";
    private static final String UI = "System.UI";

    private static final Map<String, String> HOMES = Map.ofEntries(
            Map.entry("IScript", SYSTEM), Map.entry("Action", SYSTEM), Map.entry("Func", SYSTEM),
            Map.entry("List", COLLECTIONS), Map.entry("Map", COLLECTIONS),
            Map.entry("Widget", UI), Map.entry("Window", UI), Map.entry("Row", UI), Map.entry("Column", UI),
            Map.entry("Label", UI), Map.entry("Button", UI), Map.entry("TextBox", UI), Map.entry("CheckBox", UI),
            Map.entry("ProgressBar", UI), Map.entry("ListBox", UI), Map.entry("Canvas", UI),
            Map.entry("MessageBox", UI));

    /**
     * The type known by exactly {@code fullName}, its namespace in front ({@code System.IO.Console}),
     * with {@code arity} arguments or, when there is none of that count, whichever there is; {@code -1}
     * asks for whichever. A bare name of a type that lives in a namespace is not found here.
     */
    public NamedType qualified(final String fullName, final int arity) {
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
        return List.of(SYSTEM, COLLECTIONS, IO, UTILS, MACHINE, NETWORK, OPERATIONS, EXECUTION, UI);
    }

    private static String key(final String name, final int arity) {
        return name + "/" + arity;
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

    /** A property a program may write as well as read, which is how a widget is changed. */
    private void writable(final NamedType owner, final String name, final ITypeSymbol type) {
        owner.addMember(new IMemberSymbol.PropertySymbol(owner, name, type, true, true, PUBLIC, Set.of()));
    }

    /** A way of making one of these, since a built-in type has no constructor of its own otherwise. */
    private void constructor(final NamedType owner, final ITypeSymbol... takes) {
        final List<IMemberSymbol.ParameterSymbol> parameters = new ArrayList<>();
        for (int i = 0; i < takes.length; i++) {
            parameters.add(IMemberSymbol.ParameterSymbol.of("a" + i, takes[i]));
        }
        owner.addMember(new IMemberSymbol.ConstructorSymbol(owner, parameters, PUBLIC));
    }

    /** Something a program can be told about, which it answers with a method of its own. */
    private void event(final NamedType owner, final String name, final NamedType handler) {
        owner.addMember(new IMemberSymbol.EventSymbol(owner, name, handler, PUBLIC));
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
    }

    private void fillScript() {
        this.method(this.scriptType, "OnInit", ITypeSymbol.Primitive.VOID, PUBLIC);
        this.method(this.scriptType, "OnTick", ITypeSymbol.Primitive.VOID, PUBLIC);
        this.method(this.scriptType, "OnDestroy", ITypeSymbol.Primitive.VOID, PUBLIC);
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

    /**
     * The windows a program can open on the desktop of the machine it runs on.
     *
     * <p>A widget asks for the size it needs and the machine's own system draws it, so the same program
     * looks like whichever system it is running under. Rows and columns share out the room; a weight of
     * one or more takes a share of what is left over, and nothing else takes any.
     */
    private void fillUi() {
        final ITypeSymbol integer = ITypeSymbol.Primitive.INT;
        final ITypeSymbol flag = ITypeSymbol.Primitive.BOOL;
        final ITypeSymbol text = this.stringType;
        final ITypeSymbol nothing = ITypeSymbol.Primitive.VOID;

        final NamedType widget = this.declare("Widget", NamedType.Kind.CLASS);
        final NamedType window = this.declare("Window", NamedType.Kind.CLASS);
        final NamedType row = this.declare("Row", NamedType.Kind.CLASS);
        final NamedType column = this.declare("Column", NamedType.Kind.CLASS);
        final NamedType label = this.declare("Label", NamedType.Kind.CLASS);
        final NamedType button = this.declare("Button", NamedType.Kind.CLASS);
        final NamedType textBox = this.declare("TextBox", NamedType.Kind.CLASS);
        final NamedType checkBox = this.declare("CheckBox", NamedType.Kind.CLASS);
        final NamedType progress = this.declare("ProgressBar", NamedType.Kind.CLASS);
        final NamedType listBox = this.declare("ListBox", NamedType.Kind.CLASS);
        final NamedType canvas = this.declare("Canvas", NamedType.Kind.CLASS);
        final NamedType messageBox = this.declare("MessageBox", NamedType.Kind.CLASS);
        for (final NamedType kind : List.of(row, column, label, button, textBox, checkBox, progress, listBox,
                canvas)) {
            kind.setBase(widget);
        }

        // What every widget has: whether it shows, whether it answers, and a size of its own if it wants one.
        this.writable(widget, "Visible", flag);
        this.writable(widget, "Enabled", flag);
        this.writable(widget, "Width", integer);
        this.writable(widget, "Height", integer);

        this.constructor(window, text, integer, integer);
        this.writable(window, "Title", text);
        this.writable(window, "Content", widget);
        this.property(window, "Open", flag, PUBLIC);
        this.method(window, "Show", nothing, PUBLIC);
        this.method(window, "Close", nothing, PUBLIC);
        // A widget put exactly where the program says, for one that lays itself out.
        this.method(window, "Add", nothing, PUBLIC, widget, integer, integer, integer, integer);
        this.event(window, "OnClose", this.actionType);

        for (final NamedType box : List.of(row, column)) {
            this.constructor(box);
            this.method(box, "Add", nothing, PUBLIC, widget);
            this.method(box, "Add", nothing, PUBLIC, widget, integer);
            this.writable(box, "Spacing", integer);
            this.method(box, "Clear", nothing, PUBLIC);
        }

        this.constructor(label);
        this.constructor(label, text);
        this.writable(label, "Text", text);

        this.constructor(button);
        this.constructor(button, text);
        this.writable(button, "Text", text);
        this.event(button, "OnClick", this.actionType);

        this.constructor(textBox);
        this.constructor(textBox, text);
        this.writable(textBox, "Text", text);
        this.event(textBox, "OnChange", this.actionType);
        this.event(textBox, "OnSubmit", this.actionType);

        this.constructor(checkBox);
        this.constructor(checkBox, text);
        this.constructor(checkBox, text, flag);
        this.writable(checkBox, "Text", text);
        this.writable(checkBox, "Checked", flag);
        this.event(checkBox, "OnToggle", this.actionType);

        this.constructor(progress);
        this.constructor(progress, integer, integer);
        this.writable(progress, "Value", integer);
        this.writable(progress, "Least", integer);
        this.writable(progress, "Most", integer);

        this.constructor(listBox);
        this.method(listBox, "Add", nothing, PUBLIC, text);
        this.method(listBox, "Add", nothing, PUBLIC, text, text);
        this.method(listBox, "Clear", nothing, PUBLIC);
        this.property(listBox, "Count", integer, PUBLIC);
        this.writable(listBox, "Selected", integer);
        this.event(listBox, "OnSelect", this.actionType);

        this.constructor(canvas);
        this.constructor(canvas, integer, integer);
        this.method(canvas, "Clear", nothing, PUBLIC, integer);
        this.method(canvas, "FillRect", nothing, PUBLIC, integer, integer, integer, integer, integer);
        this.method(canvas, "DrawLine", nothing, PUBLIC, integer, integer, integer, integer, integer);
        this.method(canvas, "DrawText", nothing, PUBLIC, text, integer, integer, integer);
        this.method(canvas, "SetPixel", nothing, PUBLIC, integer, integer, integer);
        this.property(canvas, "ClickX", integer, PUBLIC);
        this.property(canvas, "ClickY", integer, PUBLIC);
        this.event(canvas, "OnClick", this.actionType);

        this.method(messageBox, "Show", nothing, PUBLIC_STATIC, text, text);
    }
}
