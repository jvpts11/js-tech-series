/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.sigma.sem;

import dev.jstech.computers.sigma.DesktopFacts;
import dev.jstech.computers.sigma.DiagnosticBag;
import dev.jstech.computers.sigma.SigmaError;
import dev.jstech.computers.sigma.SystemFacts;
import dev.jstech.computers.sigma.ast.IDecl;
import dev.jstech.computers.sigma.ast.IExpr;
import dev.jstech.computers.sigma.ast.INode;
import dev.jstech.computers.sigma.ast.Operator;
import dev.jstech.computers.sigma.lex.TokenKind;
import dev.jstech.computers.vm.system.SystemApi;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Reads what a player's operating system and desktop say they are (computers/docs/design/player-systems.md), from the
 * properties they write with an arrow, and holds each value to what the machine can do with it.
 *
 * <p>A system's identity is read rather than run: its name, its era and its kernel decide which machines it installs
 * on and how they start it, so the build has to know them before anything of it runs, and the values are ones the
 * compiler already holds to being known.
 */
@TextHolder
public final class SystemDeclarations {

    private final Declarations declarations;
    private final BuiltIns builtIns;
    private final DiagnosticBag bag;
    private final List<String> services = new ArrayList<>();
    private SystemFacts system;
    private DesktopFacts desktop;

    /* What a value has to be one of, where no list of words says it. */
    private static final TextKey WHOLE_NUMBERS = TextKey.of("jsc.sigma.system_declarations.whole_numbers",
            "the whole numbers from 1");
    private static final TextKey COLOURS_WRITTEN = TextKey.of("jsc.sigma.system_declarations.colours_written",
            "the colours written #RRGGBB");
    private static final TextKey SLICES = TextKey.of("jsc.sigma.system_declarations.slices",
            "the slices from %s to %s");

    public SystemDeclarations(final Declarations declarations, final BuiltIns builtIns, final DiagnosticBag bag) {
        this.declarations = declarations;
        this.builtIns = builtIns;
        this.bag = bag;
    }

    /** Reads the system and the desktop among {@code types}, reporting what is missing or out of range. */
    public void read(final List<NamedType> types) {
        final List<NamedType> systems = new ArrayList<>();
        final List<NamedType> desktops = new ArrayList<>();
        this.services.clear();
        for (final NamedType type : types) {
            if (type.kind() == NamedType.Kind.CLASS && type.isOrDescendsFrom(this.builtIns.systemType())) {
                systems.add(type);
            }
            if (type.kind() == NamedType.Kind.CLASS && type.isOrDescendsFrom(this.builtIns.scriptType())) {
                this.services.add(type.name());
            }
            if (type.kind() == NamedType.Kind.CLASS && type.isOrDescendsFrom(this.builtIns.desktopType())) {
                desktops.add(type);
            }
        }
        if (systems.size() > 1 || desktops.size() > 1) {
            final NamedType extra = systems.size() > 1 ? systems.get(1) : desktops.get(1);
            final IDecl.ITypeDecl at = this.declarations.source(extra);
            this.bag.setFile(this.declarations.fileOf(extra));
            this.bag.error(at.line(), at.column(), SigmaError.ONE_SYSTEM, systems.size(), desktops.size());
            return;
        }
        if (!systems.isEmpty()) {
            this.system = this.systemOf(systems.getFirst());
        }
        if (!desktops.isEmpty()) {
            this.desktop = this.desktopOf(desktops.getFirst());
        }
    }

    /** The system the program declares, or null when it declares none or declared it wrongly. */
    public SystemFacts system() {
        return this.system;
    }

    /** The desktop the program declares, or null when it declares none or declared it wrongly. */
    public DesktopFacts desktop() {
        return this.desktop;
    }

    private SystemFacts systemOf(final NamedType type) {
        final Reader read = new Reader(type);
        final String name = read.written("Name");
        final String version = read.written("Version");
        final String author = read.written("Author");
        final String era = read.constant("Era", "Era", SystemApi.ERAS);
        final String isa = read.constant("Isa", "Isa", SystemApi.ISAS);
        final Integer memory = read.number("MemoryMb");
        final SystemFacts.Kernel kernel = read.kernel("Kernel");
        if (!read.clean) {
            return null;
        }
        return new SystemFacts(name, version, author, era, isa, memory, kernel, this.services);
    }

    private DesktopFacts desktopOf(final NamedType type) {
        final Reader read = new Reader(type);
        final String name = read.written("Name");
        final String version = read.written("Version");
        final String author = read.written("Author");
        final String menu = read.written("Menu");
        final String panelAt = read.oneOf("PanelAt", DesktopFacts.PANEL_PLACES);
        final String frames = read.oneOf("Frames", DesktopFacts.FRAMES);
        final Map<String, String> colours = new LinkedHashMap<>();
        for (final String role : DesktopFacts.COLOURS) {
            colours.put(role, read.colour(role));
        }
        final String icons = read.written("Icons");
        final String wallpaper = read.written("Wallpaper");
        final String programs = read.written("Programs");
        if (!read.clean) {
            return null;
        }
        return new DesktopFacts(name, version, author, menu, panelAt, frames, colours, icons, wallpaper,
                words(programs));
    }

    /* "watchman, notes files": the words of a list written in one text, by commas or spaces. */
    private static List<String> words(final String written) {
        final List<String> out = new ArrayList<>();
        for (final String word : written.split("[,\\s]+")) {
            if (!word.isBlank()) {
                out.add(word.toLowerCase(Locale.ROOT));
            }
        }
        return out;
    }

    /* The text a value spells: a string written out, a number, or several joined with +; null for anything else. */
    private static String spell(final IExpr value) {
        return switch (value) {
            case IExpr.Literal literal when literal.kind() == TokenKind.STRING_LITERAL -> (String) literal.value();
            case IExpr.Literal literal when literal.value() instanceof Number number -> number.toString();
            case IExpr.Binary binary when binary.operator() == Operator.ADD -> {
                final String left = spell(binary.left());
                final String right = spell(binary.right());
                yield left == null || right == null ? null : left + right;
            }
            default -> null;
        };
    }

    /* The library constant a value names, Era.Legacy: its name, when it is read off {@code owner}; else null. */
    private static String constantOf(final IExpr value, final String owner) {
        if (value instanceof IExpr.Member member && member.target() instanceof IExpr.Name name
                && owner.equals(name.identifier())) {
            return member.name();
        }
        return null;
    }

    /** The properties of one declaring class, read one by one, with whatever is wrong with them reported. */
    private final class Reader {

        private final NamedType type;
        private final Map<String, IDecl.PropertyDecl> arrows = new LinkedHashMap<>();
        private boolean clean = true;

        Reader(final NamedType type) {
            this.type = type;
            if (SystemDeclarations.this.declarations.source(type) instanceof IDecl.ClassDecl declaration) {
                for (final IDecl.IMemberDecl member : declaration.members()) {
                    if (member instanceof IDecl.PropertyDecl property && property.arrow()) {
                        this.arrows.put(property.name(), property);
                    }
                }
            }
        }

        String written(final String property) {
            final IExpr value = this.value(property);
            final String spelled = value == null ? null : spell(value);
            if (value != null && spelled == null) {
                this.refuse(value, SigmaError.VALUE_NOT_KNOWN, property);
            }
            return spelled == null ? "" : spelled;
        }

        Integer number(final String property) {
            final IExpr value = this.value(property);
            final Integer number = value == null ? null : BodyChecker.numberOf(value);
            if (value != null && (number == null || number < 1)) {
                this.refuse(value, SigmaError.NOT_ONE_OF, property, WHOLE_NUMBERS.text());
            }
            return number == null ? 0 : number;
        }

        String oneOf(final String property, final List<String> allowed) {
            final String written = this.written(property);
            if (this.clean && !allowed.contains(written)) {
                this.refuse(this.value(property), SigmaError.NOT_ONE_OF, written, String.join(", ", allowed));
            }
            return written;
        }

        String colour(final String property) {
            final String written = this.written(property);
            if (this.clean && !DesktopFacts.isColour(written)) {
                this.refuse(this.value(property), SigmaError.NOT_ONE_OF, written, COLOURS_WRITTEN.text());
            }
            return written;
        }

        String constant(final String property, final String owner, final List<String> allowed) {
            final IExpr value = this.value(property);
            final String named = value == null ? null : constantOf(value, owner);
            if (value != null && (named == null || !allowed.contains(named))) {
                this.refuse(value, SigmaError.NOT_ONE_OF, property, owner + "." + String.join(", " + owner + ".",
                        allowed));
            }
            return named == null ? "" : named;
        }

        SystemFacts.Kernel kernel(final String property) {
            final IExpr value = this.value(property);
            if (value == null) {
                return SystemFacts.Kernel.OWN;
            }
            final SystemFacts.Kernel kernel = this.kernelOf(value);
            if (kernel == null && this.clean) {
                this.refuse(value, SigmaError.VALUE_NOT_KNOWN, property);
            }
            return kernel == null ? SystemFacts.Kernel.OWN : kernel;
        }

        /* Kernel.Of("linux"), or Kernel.Own() and each policy chosen after it. */
        private SystemFacts.Kernel kernelOf(final IExpr value) {
            if (!(value instanceof IExpr.Call call) || !(call.callee() instanceof IExpr.Member member)) {
                return null;
            }
            final List<IExpr> args = call.arguments();
            if (member.target() instanceof IExpr.Name name && "Kernel".equals(name.identifier())) {
                return switch (member.name()) {
                    case "Of" -> args.size() == 1 && spell(args.getFirst()) != null
                            ? SystemFacts.Kernel.of(spell(args.getFirst())) : null;
                    case "Own" -> SystemFacts.Kernel.OWN;
                    default -> null;
                };
            }
            final SystemFacts.Kernel before = this.kernelOf(member.target());
            if (before == null) {
                return null;
            }
            if (!before.own()) {
                this.refuse(call, SigmaError.POLICY_OF_ITS_OWN, member.name());
                return before;
            }
            final String first = args.isEmpty() ? null : spell(args.getFirst());
            if (first == null) {
                return null;
            }
            final String chosen = first.trim().toLowerCase(Locale.ROOT);
            return switch (member.name()) {
                case "Scheduler" -> new SystemFacts.Kernel("", this.within(call, chosen, SystemFacts.SCHEDULERS),
                        args.size() > 1 ? this.slice(args.get(1)) : SystemFacts.DEFAULT_SLICE, before.fileSystem(),
                        before.drivers(), before.memoryRule(), before.shell());
                case "FileSystem" -> new SystemFacts.Kernel("", before.scheduler(), before.slice(),
                        this.within(call, chosen, SystemFacts.FILE_SYSTEMS), before.drivers(), before.memoryRule(),
                        before.shell());
                case "Drivers" -> new SystemFacts.Kernel("", before.scheduler(), before.slice(), before.fileSystem(),
                        this.classes(call, chosen), before.memoryRule(), before.shell());
                case "MemoryRule" -> new SystemFacts.Kernel("", before.scheduler(), before.slice(),
                        before.fileSystem(), before.drivers(), this.within(call, chosen, SystemFacts.MEMORY_RULES),
                        before.shell());
                case "Shell" -> new SystemFacts.Kernel("", before.scheduler(), before.slice(), before.fileSystem(),
                        before.drivers(), before.memoryRule(), this.within(call, chosen, SystemFacts.SHELLS));
                default -> null;
            };
        }

        private String within(final INode at, final String chosen, final List<String> allowed) {
            if (!allowed.contains(chosen)) {
                this.refuse(at, SigmaError.NOT_ONE_OF, chosen, String.join(", ", allowed));
            }
            return chosen;
        }

        private int slice(final IExpr value) {
            final Integer slice = BodyChecker.numberOf(value);
            if (slice == null || slice < SystemFacts.LEAST_SLICE || slice > SystemFacts.MOST_SLICE) {
                this.refuse(value, SigmaError.NOT_ONE_OF, slice == null ? "?" : slice.toString(),
                        SLICES.with(SystemFacts.LEAST_SLICE, SystemFacts.MOST_SLICE));
                return SystemFacts.DEFAULT_SLICE;
            }
            return slice;
        }

        private List<String> classes(final INode at, final String written) {
            final List<String> classes = words(written);
            for (final String each : classes) {
                this.within(at, each, SystemFacts.DRIVER_CLASSES);
            }
            return classes;
        }

        /* The arrow's value of that property, or null with the property reported as missing. */
        private IExpr value(final String property) {
            final IDecl.PropertyDecl found = this.arrows.get(property);
            if (found == null) {
                final IDecl.ITypeDecl at = SystemDeclarations.this.declarations.source(this.type);
                this.refuse(at, SigmaError.NOT_DECLARED, this.type.name(), property);
                return null;
            }
            return found.value();
        }

        private void refuse(final INode at, final SigmaError error, final Object... arguments) {
            this.clean = false;
            SystemDeclarations.this.bag.setFile(SystemDeclarations.this.declarations.fileOf(this.type));
            SystemDeclarations.this.bag.error(at.line(), at.column(), error, arguments);
        }
    }
}
