/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.sigma.sem;

import dev.jstech.computers.sigma.DiagnosticBag;
import dev.jstech.computers.sigma.LanguageLevel;
import dev.jstech.computers.sigma.SigmaError;
import dev.jstech.computers.sigma.ast.CompilationUnit;
import dev.jstech.computers.sigma.ast.IDecl;
import dev.jstech.computers.sigma.ast.IExpr;
import dev.jstech.computers.sigma.ast.INode;
import dev.jstech.computers.sigma.ast.IStmt;
import dev.jstech.computers.sigma.ast.TypeRef;
import dev.jstech.computers.sigma.lower.Lowerer;
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * What the subset does not have, reported where it was written.
 *
 * <p>Every message names the thing and then says what to write instead, because there is a way to write all of
 * it: an interface is a class whose methods are virtual, a record is a struct, a foreach is a for over the
 * array's length. A subset without those answers would be a list of refusals rather than a language.
 *
 * <p>The walk is over the tree rather than over the symbols, so a construct is reported at the place it was
 * typed and one that never resolved is still reported. The switches over the tree's own kinds are exhaustive on
 * purpose: these hierarchies are sealed, so a node type added later stops this file compiling until somebody has
 * decided whether the subset has it.
 */
@TextHolder
public final class SubsetRules {

    /** The types the subset's library does not carry, whatever a program tries to name them for. */
    private static final Set<String> ABSENT_TYPES = Set.of("List", "Map", "Thread");

    /**
     * What each type of the subset's library offers, which is a fraction of what the same type offers the full
     * language. The rest of a type is not hidden so much as absent: these machines had no floating point worth
     * rounding, no second process to message and no list to hand anybody.
     */
    private static final Map<String, Set<String>> LIBRARY = Map.of(
            "Console", Set.of("Print", "PrintLine", "Clear", "ReadLine", "ReadInt", "ReadBool"),
            "File", Set.of("Exists", "Read", "Write", "Append", "Delete"),
            "Program", Set.of("Name", "Args", "Exit"),
            "Math", Set.of("Abs", "Min", "Max", "Floor", "Sqrt", "Pow"),
            "Convert", Set.of("ToInt", "ToDouble", "ToBool", "ToString"),
            "Time", Set.of("Tick", "Day"),
            "Computer", Set.of("Name", "RamMb", "Online"),
            "Script", Set.of("OnInit", "OnTick", "OnDestroy"));

    /*
     * What the subset does not have, and what to write instead, in pairs: the first fills "Sigma has no %s", the
     * second the advice after it. A construct named by its keyword or a type by its name is not here, since those
     * are code and read the same in every language.
     */
    private static final TextKey LIBRARY_NAMED = TextKey.of("jsc.sigma.subset_rules.library", "the '%s' library");
    private static final TextKey LIBRARY_INSTEAD = TextKey.of("jsc.sigma.subset_rules.library_instead",
            "everything Sigma has is in %s");
    private static final TextKey LIBRARY_MEMBERS = TextKey.of("jsc.sigma.subset_rules.library_members",
            "Sigma's %s has %s");
    private static final TextKey INTERFACES = TextKey.of("jsc.sigma.subset_rules.interfaces", "interfaces");
    private static final TextKey INTERFACES_INSTEAD = TextKey.of("jsc.sigma.subset_rules.interfaces_instead",
            "write a class whose methods are virtual and stand on it instead");
    private static final TextKey DELEGATES = TextKey.of("jsc.sigma.subset_rules.delegates", "delegates");
    private static final TextKey DELEGATES_INSTEAD = TextKey.of("jsc.sigma.subset_rules.delegates_instead",
            "call the method by name, or keep the object and call it through that");
    private static final TextKey RECORDS = TextKey.of("jsc.sigma.subset_rules.records", "records");
    private static final TextKey RECORDS_INSTEAD = TextKey.of("jsc.sigma.subset_rules.records_instead",
            "write a struct with the same fields");
    private static final TextKey ABSTRACT_CLASSES = TextKey.of("jsc.sigma.subset_rules.abstract_classes",
            "abstract classes");
    private static final TextKey ABSTRACT_CLASSES_INSTEAD = TextKey.of(
            "jsc.sigma.subset_rules.abstract_classes_instead",
            "write a class with virtual methods that do nothing, and stand on that");
    private static final TextKey EVENTS = TextKey.of("jsc.sigma.subset_rules.events", "events");
    private static final TextKey EVENTS_INSTEAD = TextKey.of("jsc.sigma.subset_rules.events_instead",
            "keep the object that wants telling and call a method on it");
    private static final TextKey PROPERTIES = TextKey.of("jsc.sigma.subset_rules.properties", "properties");
    private static final TextKey PROPERTIES_INSTEAD = TextKey.of("jsc.sigma.subset_rules.properties_instead",
            "write a field, or a method that gives the value back");
    private static final TextKey ABSTRACT_METHODS = TextKey.of("jsc.sigma.subset_rules.abstract_methods",
            "abstract methods");
    private static final TextKey ABSTRACT_METHODS_INSTEAD = TextKey.of(
            "jsc.sigma.subset_rules.abstract_methods_instead", "give it a body that does nothing and write virtual");
    private static final TextKey TYPE_ARGUMENTS = TextKey.of("jsc.sigma.subset_rules.type_arguments",
            "types with arguments in angle brackets");
    private static final TextKey TYPE_ARGUMENTS_INSTEAD = TextKey.of("jsc.sigma.subset_rules.type_arguments_instead",
            "write an array, or a class that holds exactly what you need");
    private static final TextKey LIST_INSTEAD = TextKey.of("jsc.sigma.subset_rules.list_instead",
            "use an array, and a count of your own beside it");
    private static final TextKey MAP_INSTEAD = TextKey.of("jsc.sigma.subset_rules.map_instead",
            "use two arrays, or walk one array looking for the key");
    private static final TextKey THREAD_INSTEAD = TextKey.of("jsc.sigma.subset_rules.thread_instead",
            "these machines run one thing at a time");
    private static final TextKey VAR_INSTEAD = TextKey.of("jsc.sigma.subset_rules.var_instead",
            "write the type out");
    private static final TextKey FOREACH_INSTEAD = TextKey.of("jsc.sigma.subset_rules.foreach_instead",
            "write a for over the array's Length");
    private static final TextKey LOCK_INSTEAD = TextKey.of("jsc.sigma.subset_rules.lock_instead",
            "these machines run one thing at a time, so nothing is being raced for");
    private static final TextKey LAMBDAS = TextKey.of("jsc.sigma.subset_rules.lambdas", "lambdas");
    private static final TextKey LAMBDAS_INSTEAD = TextKey.of("jsc.sigma.subset_rules.lambdas_instead",
            "write a method and call it, since there is nothing here that takes code as a value");
    private static final TextKey INTERPOLATION = TextKey.of("jsc.sigma.subset_rules.interpolation",
            "strings with holes in them");
    private static final TextKey INTERPOLATION_INSTEAD = TextKey.of("jsc.sigma.subset_rules.interpolation_instead",
            "add the pieces together with +");

    private final DiagnosticBag diagnostics;

    public SubsetRules(final DiagnosticBag diagnostics) {
        this.diagnostics = diagnostics;
    }

    /** Whether the smaller language's library has a type of that name. */
    public static boolean hasType(final String name) {
        return LIBRARY.containsKey(name);
    }

    /**
     * Whether the smaller language's version of that type has that member, which is what an editor asks before
     * it offers one: a list that offered what the compiler then refuses would be worse than no list.
     */
    public static boolean hasMember(final String type, final String member) {
        final Set<String> offered = LIBRARY.get(type);
        return offered != null && offered.contains(member);
    }

    /** Reports everything in {@code units} that {@code level} does not have. */
    public void check(final List<CompilationUnit> units, final LanguageLevel level) {
        if (level.full()) {
            return;
        }
        for (final CompilationUnit unit : units) {
            this.diagnostics.setFile(unit.file());
            for (final CompilationUnit.Using using : unit.usings()) {
                /*
                 * One namespace, and it is the only one. A program's own namespaces are its own business; what
                 * it may not do is reach into the full language's library, because the machines this language is
                 * for never had most of it.
                 */
                final String reached = using.name();
                if (reached.equals("System") || reached.startsWith("System.")) {
                    this.diagnostics.error(using.line(), using.column(), SigmaError.NOT_IN_THE_SUBSET,
                            LIBRARY_NAMED.with(reached), LIBRARY_INSTEAD.with(BuiltIns.SUBSET_LIBRARY));
                }
            }
            for (final IDecl.ITypeDecl type : unit.types()) {
                this.type(type);
            }
        }
    }

    private void type(final IDecl.ITypeDecl declaration) {
        switch (declaration) {
            case IDecl.InterfaceDecl face -> this.refuse(face, INTERFACES.text(), INTERFACES_INSTEAD);
            case IDecl.DelegateDecl delegate -> this.refuse(delegate, DELEGATES.text(), DELEGATES_INSTEAD);
            case IDecl.EnumDecl ignored -> { } // an enum is a name for a number, which those machines had
            case IDecl.ClassDecl klass -> this.classDecl(klass);
        }
    }

    private void classDecl(final IDecl.ClassDecl klass) {
        if (klass.flavour() == IDecl.ClassDecl.Flavour.RECORD) {
            this.refuse(klass, RECORDS.text(), RECORDS_INSTEAD);
            return;
        }
        if (klass.modifiers().contains(IDecl.Modifier.ABSTRACT)) {
            this.refuse(klass, ABSTRACT_CLASSES.text(), ABSTRACT_CLASSES_INSTEAD);
        }
        for (final IDecl.IMemberDecl member : klass.members()) {
            this.member(member);
        }
    }

    private void member(final IDecl.IMemberDecl member) {
        switch (member) {
            case IDecl.EventDecl event -> this.refuse(event, EVENTS.text(), EVENTS_INSTEAD);
            case IDecl.PropertyDecl property -> this.refuse(property, PROPERTIES.text(), PROPERTIES_INSTEAD);
            case IDecl.FieldDecl field -> this.typeRef(field.type());
            case IDecl.MethodDecl method -> this.method(method);
            case IDecl.ConstructorDecl constructor -> {
                for (final IDecl.Parameter parameter : constructor.parameters()) {
                    this.typeRef(parameter.type());
                }
                this.block(constructor.body());
            }
            case IDecl.TypeMember nested -> this.type(nested.type());
        }
    }

    private void method(final IDecl.MethodDecl method) {
        if (method.modifiers().contains(IDecl.Modifier.ABSTRACT)) {
            this.refuse(method, ABSTRACT_METHODS.text(), ABSTRACT_METHODS_INSTEAD);
        }
        this.typeRef(method.returnType());
        for (final IDecl.Parameter parameter : method.parameters()) {
            this.typeRef(parameter.type());
        }
        this.block(method.body());
    }

    /** A type written anywhere: the absent ones, and any that was given arguments, since there are no generics. */
    private void typeRef(final TypeRef type) {
        if (type == null) {
            return;
        }
        if (!type.arguments().isEmpty()) {
            this.refuse(type, TYPE_ARGUMENTS.text(), TYPE_ARGUMENTS_INSTEAD);
        } else if (ABSENT_TYPES.contains(type.name())) {
            this.refuse(type, Text.literal("'" + type.name() + "'"), switch (type.name()) {
                case "List" -> LIST_INSTEAD;
                case "Map" -> MAP_INSTEAD;
                default -> THREAD_INSTEAD;
            });
        } else if ("var".equals(type.name())) {
            this.refuse(type, Text.literal("var"), VAR_INSTEAD);
        }
        for (final TypeRef argument : type.arguments()) {
            this.typeRef(argument);
        }
    }

    private void block(final IStmt.Block block) {
        if (block != null) {
            for (final IStmt statement : block.statements()) {
                this.statement(statement);
            }
        }
    }

    private void statement(final IStmt statement) {
        switch (statement) {
            case IStmt.ForEach each -> {
                this.refuse(each, Text.literal("foreach"), FOREACH_INSTEAD);
                this.expression(each.source());
                this.statement(each.body());
            }
            case IStmt.Lock held -> {
                this.refuse(held, Text.literal("lock"), LOCK_INSTEAD);
                this.expression(held.target());
                this.statement(held.body());
            }
            case IStmt.Block block -> this.block(block);
            case IStmt.If branch -> {
                this.expression(branch.condition());
                this.statement(branch.then());
                this.statement(branch.otherwise());
            }
            case IStmt.While loop -> {
                this.expression(loop.condition());
                this.statement(loop.body());
            }
            case IStmt.DoWhile loop -> {
                this.expression(loop.condition());
                this.statement(loop.body());
            }
            case IStmt.For loop -> {
                for (final IStmt first : loop.initializers()) {
                    this.statement(first);
                }
                this.expression(loop.condition());
                this.arguments(loop.updates());
                this.statement(loop.body());
            }
            case IStmt.Switch chosen -> {
                this.expression(chosen.value());
                for (final IStmt.SwitchSection section : chosen.sections()) {
                    for (final IExpr label : section.labels()) {
                        this.expression(label);
                    }
                    for (final IStmt inside : section.statements()) {
                        this.statement(inside);
                    }
                }
            }
            case IStmt.LocalDecl local -> {
                this.typeRef(local.type());
                this.expression(local.initializer());
            }
            case IStmt.ExprStmt expression -> this.expression(expression.expression());
            case IStmt.Return given -> this.expression(given.value());
            case IStmt.Dispose thrown -> this.expression(thrown.target());
            case IStmt.Break ignored -> { }
            case IStmt.Continue ignored -> { }
            case IStmt.Empty ignored -> { }
            case null -> { }
            default -> { } // a switch's sections are walked by the switch itself, above
        }
    }

    private void expression(final IExpr expression) {
        switch (expression) {
            case IExpr.Lambda lambda -> {
                this.refuse(lambda, LAMBDAS.text(), LAMBDAS_INSTEAD);
                this.expression(lambda.body());
                this.block(lambda.block());
            }
            case IExpr.Unary unary -> this.expression(unary.operand());
            case IExpr.Binary binary -> {
                this.expression(binary.left());
                this.expression(binary.right());
            }
            case IExpr.Assign assign -> {
                this.expression(assign.target());
                this.expression(assign.value());
            }
            case IExpr.Conditional chosen -> {
                this.expression(chosen.condition());
                this.expression(chosen.whenTrue());
                this.expression(chosen.whenFalse());
            }
            case IExpr.Call call -> {
                this.expression(call.callee());
                this.arguments(call.arguments());
            }
            case IExpr.Member member -> {
                this.libraryMember(member);
                this.expression(member.target());
            }
            case IExpr.Index index -> {
                this.expression(index.target());
                this.expression(index.index());
            }
            case IExpr.New made -> {
                this.typeRef(made.type());
                this.arguments(made.arguments());
            }
            case IExpr.NewArray made -> {
                this.typeRef(made.elementType());
                this.expression(made.length());
            }
            case IExpr.Cast cast -> {
                this.typeRef(cast.type());
                this.expression(cast.value());
            }
            case IExpr.TypeTest test -> {
                this.typeRef(test.type());
                this.expression(test.value());
            }
            case IExpr.OutArgument out -> this.typeRef(out.type());
            case IExpr.Interpolation written -> {
                this.refuse(written, INTERPOLATION.text(), INTERPOLATION_INSTEAD);
                for (final IExpr hole : Lowerer.holesOf(written)) {
                    this.expression(hole);
                }
            }
            case IExpr.Literal ignored -> { }
            case IExpr.Name ignored -> { }
            case IExpr.This ignored -> { }
            case IExpr.Base ignored -> { }
            case null -> { }
        }
    }

    private void arguments(final List<IExpr> arguments) {
        if (arguments != null) {
            for (final IExpr argument : arguments) {
                this.expression(argument);
            }
        }
    }

    /**
     * A member of one of the library's types that the subset's smaller version of it does not have.
     *
     * <p>Read off the name as it was written, because these are all static and a program reaches them by writing
     * the type: {@code Console.ReadLong} and nothing else. That is also the limit of it, and it is the right
     * limit here, since the checker proper reports anything this misses as a member that is simply not there.
     */
    private void libraryMember(final IExpr.Member member) {
        /*
         * Written out in full, a name walks past the refused using: System.Utils.Random.Next() never asks for a
         * using at all. It is caught at the root of the chain, where the namespace is still a name of its own.
         */
        if (root(member) instanceof IExpr.Name first && "System".equals(first.identifier())) {
            this.refuse(first, LIBRARY_NAMED.with("System"), LIBRARY_INSTEAD.with(BuiltIns.SUBSET_LIBRARY));
            return;
        }
        if (!(member.target() instanceof IExpr.Name owner)) {
            return;
        }
        final Set<String> offered = LIBRARY.get(owner.identifier());
        if (offered != null && !offered.contains(member.name())) {
            this.refuse(member, Text.literal("'" + owner.identifier() + "." + member.name() + "'"),
                    LIBRARY_MEMBERS.with(owner.identifier(), String.join(", ", new TreeSet<>(offered))));
        }
    }

    /** The leftmost thing of a chain of members, which for a written-out name is the namespace it starts with. */
    private static IExpr root(final IExpr expression) {
        IExpr at = expression;
        while (at instanceof IExpr.Member member) {
            at = member.target();
        }
        return at;
    }

    private void refuse(final INode where, final Text what, final TextKey instead) {
        this.refuse(where, what, instead.text());
    }

    /** Reports {@code what} as missing from the subset: a phrase to translate, or code marked as data. */
    private void refuse(final INode where, final Text what, final Text instead) {
        this.diagnostics.error(where.line(), where.column(), SigmaError.NOT_IN_THE_SUBSET, what, instead);
    }
}
