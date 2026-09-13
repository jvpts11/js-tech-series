/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.vm.program;

import dev.jstech.computers.vm.listing.AsmMethod;
import dev.jstech.computers.vm.listing.AsmType;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * One type made ready to run, with what it is worked out once.
 *
 * <p>A method it inherits is found in the same place as one it declares, a constructor by how many arguments it takes,
 * its size is summed over the classes it stands on, and every type it is, directly or through what it stands on, is
 * in one set. A type that ends up standing on itself, which only a listing edited by hand can say, stops inheriting
 * where the loop closes instead of sending the machine round it forever.
 */
public final class TypeImage {

    private final String name;
    private final AsmType.Kind kind;
    private final List<String> bases;
    private final List<AsmType.Field> fields;
    private final Map<String, Integer> values;
    /** The methods it declares, by how they are written, in the order they were written. */
    private final Map<String, MethodImage> methods;
    private final MethodImage invoke;
    private final MethodImage setUp;
    private final Map<Integer, MethodImage> constructors = new HashMap<>();

    /* Worked out while the program loads, once every type exists, and never changed afterwards. */
    private TypeImage base;
    /** Its own methods and the ones it inherits, by the number of the shape they are written with. */
    private final Map<Integer, MethodImage> dispatch = new HashMap<>();
    private Set<String> ancestors = Set.of();
    private long instanceSize;

    /**
     * Reads the type's own parts; {@code signatures} numbers every method shape of the program and gains this type's.
     *
     * <p>The method that puts the static fields in place is kept apart from the rest, because it runs once when the
     * program starts and is never called by name.
     */
    TypeImage(final AsmType type, final Map<String, Integer> signatures) {
        this.name = type.name();
        this.kind = type.kind();
        this.bases = type.bases();
        this.fields = type.fields();
        final Map<String, Integer> numbers = new LinkedHashMap<>();
        for (final AsmType.Value value : type.values()) {
            numbers.put(value.name(), value.number());
        }
        this.values = Collections.unmodifiableMap(numbers);
        final Map<String, MethodImage> declared = new LinkedHashMap<>();
        MethodImage staticSetUp = null;
        for (final AsmMethod method : type.methods()) {
            final MethodImage made = new MethodImage(this.name, method);
            if (method.isStatic() && AsmMethod.TYPE_SET_UP.equals(method.name()) && method.parameters().isEmpty()) {
                staticSetUp = made;
                continue;
            }
            final String key = ProgramImage.key(method.name(), method.parameters());
            declared.put(key, made);
            signatures.computeIfAbsent(key, ignored -> signatures.size());
            if (AsmMethod.CONSTRUCTOR.equals(method.name()) && !method.isStatic()) {
                this.constructors.putIfAbsent(method.parameters().size(), made);
            }
        }
        this.methods = Collections.unmodifiableMap(declared);
        this.setUp = staticSetUp;
        this.invoke = type.invoke() == null ? null : new MethodImage(this.name, type.invoke());
    }

    /** Finds the class it stands on: the first of its bases the program declares as a class. */
    void link(final Map<String, TypeImage> types) {
        for (final String written : this.bases) {
            final TypeImage other = types.get(written);
            if (other != null && other.kind == AsmType.Kind.CLASS) {
                this.base = other;
                return;
            }
        }
    }

    /** Works out what it inherits, what an object of it weighs and every type it is, once every type has its base. */
    void complete(final Map<String, TypeImage> types, final Map<String, Integer> signatures) {
        long bytes = Heap.HEADER;
        final Set<TypeImage> climbed = Collections.newSetFromMap(new IdentityHashMap<>());
        for (TypeImage at = this; at != null && climbed.add(at); at = at.base) {
            for (final Map.Entry<String, MethodImage> method : at.methods.entrySet()) {
                this.dispatch.putIfAbsent(signatures.get(method.getKey()), method.getValue());
            }
            for (final AsmType.Field field : at.fields) {
                if (!field.isStatic()) {
                    bytes += Heap.sizeOf(field.type());
                }
            }
        }
        this.instanceSize = bytes;
        final Set<String> all = new HashSet<>();
        collect(this.name, types, all);
        this.ancestors = Collections.unmodifiableSet(all);
    }

    /** Works out what every line of its methods reaches. */
    void resolve(final ProgramImage program) {
        for (final MethodImage method : this.methods.values()) {
            method.resolve(program);
        }
        if (this.setUp != null) {
            this.setUp.resolve(program);
        }
        if (this.invoke != null) {
            this.invoke.resolve(program);
        }
    }

    private static void collect(final String name, final Map<String, TypeImage> types, final Set<String> into) {
        if (!into.add(name)) {
            return;
        }
        final TypeImage type = types.get(name);
        if (type != null) {
            for (final String written : type.bases) {
                collect(written, types, into);
            }
        }
    }

    /** What the type is called. */
    public String name() {
        return this.name;
    }

    /** Which kind of type it is. */
    public AsmType.Kind kind() {
        return this.kind;
    }

    /** The class and the interfaces it stands on, as the listing writes them. */
    public List<String> bases() {
        return this.bases;
    }

    /** The fields it declares. */
    public List<AsmType.Field> fields() {
        return this.fields;
    }

    /** For an enum, the number behind each name. */
    public Map<String, Integer> values() {
        return this.values;
    }

    /** The methods it declares itself, by how they are written, in order. */
    public Map<String, MethodImage> methods() {
        return this.methods;
    }

    /** For a delegate, the shape of the method it stands for. */
    public MethodImage invoke() {
        return this.invoke;
    }

    /** The method that puts its static fields in place, or null when it has none. */
    public MethodImage setUp() {
        return this.setUp;
    }

    /** What an object of it weighs on the heap: a header and every instance field down to the last class it stands on. */
    public long instanceSize() {
        return this.instanceSize;
    }

    /** Its constructor taking that many arguments, or null. */
    public MethodImage constructor(final int arguments) {
        return this.constructors.get(arguments);
    }

    /** Whether it is that type, or stands on it, however far down. */
    public boolean isA(final String other) {
        return this.ancestors.contains(other);
    }

    /** The method of that shape it declares or inherits, or null. */
    MethodImage method(final Integer signature) {
        return signature == null ? null : this.dispatch.get(signature);
    }
}
