/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.vm.program;

import dev.jstech.computers.vm.listing.AsmMethod;
import dev.jstech.computers.vm.listing.AsmProgram;
import dev.jstech.computers.vm.listing.AsmType;
import dev.jstech.computers.vm.listing.AsmWriter;
import dev.jstech.computers.vm.listing.IOperand;
import dev.jstech.computers.vm.listing.Instruction;
import dev.jstech.computers.vm.listing.ListingError;
import dev.jstech.computers.vm.listing.ListingProblem;
import dev.jstech.computers.vm.listing.Opcode;
import dev.jstech.computers.vm.listing.Shape;
import dev.jstech.computers.vm.system.ConstructorSpec;
import dev.jstech.computers.vm.system.EventSpec;
import dev.jstech.computers.vm.system.IMemberSpec;
import dev.jstech.computers.vm.system.IntrinsicRegistry;
import dev.jstech.computers.vm.system.IntrinsicSpec;
import dev.jstech.computers.vm.system.PropertySpec;
import dev.jstech.computers.vm.system.SystemApi;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A program made ready to run.
 *
 * <p>The listing is text, and text is what a player reads, not what a machine should chase down a line at a time. So
 * it is worked out once, when the program loads: every branch knows the line it lands on, every call knows the method
 * of the program it reaches (or the function or the declaration the system answers it with), every {@code new} knows
 * its constructor and what the object weighs, and every type knows the methods it inherits and every type it is. From
 * then on a line looks nothing up by its name.
 *
 * <p>Nothing in it changes once it is made, so every process running the same program could share one.
 */
public final class ProgramImage {

    /**
     * A call as the program loaded it.
     *
     * @param named      the call as the listing writes it, for what the runtime answers by name
     * @param direct     the method of the program the call reaches on the type it names, or null when the program has
     *                   none and the runtime answers the call instead
     * @param signature  the number of the shape the call is written with, or null when no type of the program declares
     *                   a method of that shape
     * @param outs       which of its arguments the call fills in rather than hands over
     * @param constructs whether the call runs a constructor, which always runs on the type it names
     * @param intrinsic  the function the system answers the call with when the program has no method for it, or null
     *                   when the runtime answers it by name
     * @param declared   the system's declaration of the call when neither the program nor a function answers it, or
     *                   null when the system declares no such call
     * @param gives      whether the call leaves an answer on the stack
     * @param defaults   for each parameter the call fills in, what it holds when the function left it empty: what a
     *                   variable of the type the call is written with starts with
     */
    record CallSite(IOperand.Method named, MethodImage direct, Integer signature, boolean[] outs, boolean constructs,
                    IntrinsicSpec intrinsic, IMemberSpec declared, boolean gives, Object[] defaults) {
    }

    /**
     * A {@code new} as the program loaded it.
     *
     * @param made        the object as the listing writes it, for what the runtime makes itself
     * @param type        the type of the program it makes, or null when the runtime brings that type
     * @param constructor the constructor that runs, or null when the type has none taking those arguments
     * @param outs        which of its arguments are filled in rather than handed over
     * @param declared    the system's declaration of the constructor when the runtime brings the type, or null when
     *                    the program makes it or the system declares no such constructor
     */
    record Creation(IOperand.Constructor made, TypeImage type, MethodImage constructor, boolean[] outs,
                    ConstructorSpec declared) {
    }

    /** The values of the language's own core a listing may read, which the system does not declare. */
    static final Set<String> CORE_VALUES = Set.of("string.Length", "List.Count", "Map.Count");
    /** The collections of the language's own core a listing may make, which the system does not declare. */
    private static final Set<String> CORE_MADE = Set.of("List", "Map");

    private final Map<String, TypeImage> types = new LinkedHashMap<>();
    /** Every method shape a type of the program declares, numbered in the order they were first met. */
    private final Map<String, Integer> signatures = new HashMap<>();
    private final IntrinsicRegistry registry;
    private final String entryPoint;
    private final Shape shape;
    /** A checksum of the listing's text, so a snapshot can tell the listing it was taken from. */
    private final String checksum;

    private ProgramImage(final AsmProgram program, final IntrinsicRegistry registry) {
        this.registry = registry;
        this.entryPoint = program.entryPoint();
        this.shape = program.shape();
        this.checksum = checksumOf(AsmWriter.write(program));
        for (final AsmType type : program.types()) {
            this.types.put(type.name(), new TypeImage(type, this.signatures));
        }
        for (final TypeImage type : this.types.values()) {
            type.link(this.types);
        }
        for (final TypeImage type : this.types.values()) {
            type.complete(this.types, this.signatures);
        }
        for (final TypeImage type : this.types.values()) {
            type.resolve(this);
        }
    }

    /** Makes a program ready to run, with the language's pure functions answering what it does not declare. */
    public static ProgramImage of(final AsmProgram program) {
        return of(program, PureFunctions.REGISTRY);
    }

    /** Makes a program ready to run, with {@code registry} answering the calls it does not declare. */
    public static ProgramImage of(final AsmProgram program, final IntrinsicRegistry registry) {
        return new ProgramImage(program, registry);
    }

    /** The type of that name, or null when the runtime provides it instead of the program. */
    public TypeImage type(final String name) {
        return this.types.get(name);
    }

    /** The class the runtime starts from, or null for a library. */
    public String entryPoint() {
        return this.entryPoint;
    }

    /** Whether this is a program that runs at a terminal or one that stays up. */
    public Shape shape() {
        return this.shape;
    }

    /**
     * A checksum of the listing, taken over the text it is written as, so two loads of one listing agree and a listing
     * compiled again from a changed source does not.
     */
    public String checksum() {
        return this.checksum;
    }

    /** A checksum of a text: the same text always gives the same checksum, and a changed text a different one. */
    public static String checksumOf(final String text) {
        try {
            final byte[] digest = MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (final NoSuchAlgorithmException missing) {
            // Every Java runtime has to offer SHA-256, so this can only be a runtime that is not one.
            throw new IllegalStateException("this Java runtime offers no SHA-256", missing);
        }
    }

    /** Every type the program declares. */
    public List<TypeImage> types() {
        return List.copyOf(this.types.values());
    }

    /**
     * The method of that shape on that type or on one it stands on, or null.
     *
     * <p>For what is named at run time rather than written on a line: a handler, a thread's body, a frame read back
     * from a snapshot.
     */
    public MethodImage method(final String owner, final String name, final List<String> parameters) {
        final TypeImage type = this.types.get(owner);
        return type == null ? null : type.method(this.signatures.get(key(name, parameters)));
    }

    /** Whether a type is, or stands on, another. */
    public boolean isA(final String type, final String other) {
        if (type == null || other == null) {
            return false;
        }
        if (type.equals(other)) {
            return true;
        }
        final TypeImage known = this.types.get(type);
        return known != null && known.isA(other);
    }

    CallSite callSite(final IOperand.Method called) {
        final Integer signature = this.signatures.get(key(called.name(), called.parameters()));
        final TypeImage owner = this.types.get(called.owner());
        final MethodImage direct = owner == null ? null : owner.method(signature);
        final IntrinsicSpec intrinsic = direct == null
                ? this.registry.find(called.owner(), called.name(), called.parameters()) : null;
        final IMemberSpec declared = direct == null && intrinsic == null
                ? SystemApi.member(called.owner(), called.name(), called.parameters()) : null;
        return new CallSite(called, direct, signature, MethodImage.outsOf(called.parameters()),
                AsmMethod.CONSTRUCTOR.equals(called.name()), intrinsic, declared, !"void".equals(called.returns()),
                defaultsOf(called.parameters()));
    }

    /** For each outward parameter, what a variable of its type starts with: nothing, or zero or false. */
    private static Object[] defaultsOf(final List<String> parameters) {
        final Object[] defaults = new Object[parameters.size()];
        for (int i = 0; i < defaults.length; i++) {
            final String written = parameters.get(i);
            if (written.startsWith("out ")) {
                defaults[i] = switch (written.substring("out ".length())) {
                    case "int" -> 0;
                    case "long" -> 0L;
                    case "float" -> 0.0f;
                    case "double" -> 0.0d;
                    case "bool" -> false;
                    case "char" -> '\0';
                    default -> null;
                };
            }
        }
        return defaults;
    }

    Creation creation(final IOperand.Constructor made) {
        final TypeImage type = this.types.get(made.owner());
        ConstructorSpec declared = null;
        if (type == null && SystemApi.member(made.owner(), ConstructorSpec.NAME, made.parameters())
                instanceof ConstructorSpec found) {
            declared = found;
        }
        return new Creation(made, type, type == null ? null : type.constructor(made.parameters().size()),
                MethodImage.outsOf(made.parameters()), declared);
    }

    /**
     * What in the program nothing answers, in the order it is written: a call, a new or a value that neither the
     * program, a function of the language, the language's own core nor a declaration of the system accounts for, and a
     * value written that can only be read.
     *
     * <p>A program with any of these would run until that line and stop there, so a machine refuses to start it and
     * says where. Loading never refuses by itself, so a runtime handed a host of its own can still run what it likes.
     */
    public List<ListingProblem> problems() {
        final List<ListingProblem> found = new ArrayList<>();
        for (final TypeImage type : this.types.values()) {
            for (final MethodImage method : type.methods().values()) {
                this.problemsOf(method, found);
            }
            if (type.setUp() != null) {
                this.problemsOf(type.setUp(), found);
            }
        }
        found.sort(Comparator.comparingInt(ListingProblem::line));
        return List.copyOf(found);
    }

    private void problemsOf(final MethodImage method, final List<ListingProblem> found) {
        for (int i = 0; i < method.length(); i++) {
            final Instruction instruction = method.instruction(i);
            final ListingError error = switch (instruction.opcode()) {
                case CALL -> answered(method.call(i)) ? null : ListingError.UNKNOWN_MEMBER;
                case NEWOBJ -> made(method.creation(i)) ? null : ListingError.UNKNOWN_MEMBER;
                case LDFLD, STFLD, LDSFLD, STSFLD -> this.valueProblem(instruction.opcode(),
                        (IOperand.Field) instruction.operand());
                default -> null;
            };
            if (error != null) {
                found.add(new ListingProblem(Math.max(1, method.lineOf(i)), 1, error.code(),
                        error.message(instruction.operand().write())));
            }
        }
    }

    /** Whether something answers a call: the program, a function of the language or a declaration of the system. */
    private static boolean answered(final CallSite site) {
        return site.direct() != null || site.intrinsic() != null || site.declared() != null;
    }

    /** Whether something makes what a new names: the program, the system, or one of the language's collections. */
    private static boolean made(final Creation creation) {
        return creation.type() != null || creation.declared() != null
                || CORE_MADE.contains(bare(creation.made().owner()));
    }

    /**
     * What is wrong with reading or writing a value of a type the program does not declare, or null when nothing is:
     * the value has to be one the system declares on that side of the type, or one of the core's, and a write needs a
     * value that can be written or an event to join.
     */
    private ListingError valueProblem(final Opcode opcode, final IOperand.Field field) {
        if (field.owner() == null || this.types.containsKey(field.owner())) {
            return null;
        }
        final boolean onType = opcode == Opcode.LDSFLD || opcode == Opcode.STSFLD;
        final boolean writes = opcode == Opcode.STFLD || opcode == Opcode.STSFLD;
        if (!onType && !writes && CORE_VALUES.contains(bare(field.owner()) + "." + field.name())) {
            return null;
        }
        for (final IMemberSpec member : SystemApi.members(field.owner(), field.name())) {
            if (member.isStatic() != onType) {
                continue;
            }
            if (member instanceof PropertySpec value) {
                return !writes || value.writable() ? null : ListingError.READ_ONLY_VALUE;
            }
            if (member instanceof EventSpec) {
                return null;
            }
        }
        return ListingError.UNKNOWN_MEMBER;
    }

    /** A type's name without the types it is given: {@code List} for {@code List<string>}. */
    private static String bare(final String type) {
        final int open = type.indexOf('<');
        return open < 0 ? type : type.substring(0, open);
    }

    /** How a method shape is written, which is what tells two overloads apart. */
    static String key(final String name, final List<String> parameters) {
        return name + "(" + String.join(", ", parameters) + ")";
    }
}
