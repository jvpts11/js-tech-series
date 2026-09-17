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
import java.util.List;
import java.util.Map;
import java.util.Set;

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

    private final DiagnosticBag diagnostics;

    public SubsetRules(final DiagnosticBag diagnostics) {
        this.diagnostics = diagnostics;
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
                            "the '" + reached + "' library",
                            "everything Sigma has is in " + BuiltIns.SUBSET_LIBRARY);
                }
            }
            for (final IDecl.ITypeDecl type : unit.types()) {
                this.type(type);
            }
        }
    }

    private void type(final IDecl.ITypeDecl declaration) {
        switch (declaration) {
            case IDecl.InterfaceDecl face -> this.refuse(face, "interfaces",
                    "write a class whose methods are virtual and stand on it instead");
            case IDecl.DelegateDecl delegate -> this.refuse(delegate, "delegates",
                    "call the method by name, or keep the object and call it through that");
            case IDecl.EnumDecl ignored -> { } // an enum is a name for a number, which those machines had
            case IDecl.ClassDecl klass -> this.classDecl(klass);
        }
    }

    private void classDecl(final IDecl.ClassDecl klass) {
        if (klass.flavour() == IDecl.ClassDecl.Flavour.RECORD) {
            this.refuse(klass, "records", "write a struct with the same fields");
            return;
        }
        if (klass.modifiers().contains(IDecl.Modifier.ABSTRACT)) {
            this.refuse(klass, "abstract classes",
                    "write a class with virtual methods that do nothing, and stand on that");
        }
        for (final IDecl.IMemberDecl member : klass.members()) {
            this.member(member);
        }
    }

    private void member(final IDecl.IMemberDecl member) {
        switch (member) {
            case IDecl.EventDecl event -> this.refuse(event, "events",
                    "keep the object that wants telling and call a method on it");
            case IDecl.PropertyDecl property -> this.refuse(property, "properties",
                    "write a field, or a method that gives the value back");
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
            this.refuse(method, "abstract methods", "give it a body that does nothing and write virtual");
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
            this.refuse(type, "types with arguments in angle brackets",
                    "write an array, or a class that holds exactly what you need");
        } else if (ABSENT_TYPES.contains(type.name())) {
            this.refuse(type, "'" + type.name() + "'", switch (type.name()) {
                case "List" -> "use an array, and a count of your own beside it";
                case "Map" -> "use two arrays, or walk one array looking for the key";
                default -> "these machines run one thing at a time";
            });
        } else if ("var".equals(type.name())) {
            this.refuse(type, "var", "write the type out");
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
                this.refuse(each, "foreach", "write a for over the array's Length");
                this.expression(each.source());
                this.statement(each.body());
            }
            case IStmt.Lock held -> {
                this.refuse(held, "lock", "these machines run one thing at a time, so nothing is being raced for");
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
                this.refuse(lambda, "lambdas",
                        "write a method and call it, since there is nothing here that takes code as a value");
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
        if (!(member.target() instanceof IExpr.Name owner)) {
            return;
        }
        final Set<String> offered = LIBRARY.get(owner.identifier());
        if (offered != null && !offered.contains(member.name())) {
            this.refuse(member, "'" + owner.identifier() + "." + member.name() + "'",
                    "Sigma's " + owner.identifier() + " has "
                            + String.join(", ", new java.util.TreeSet<>(offered)));
        }
    }

    private void refuse(final INode where, final String what, final String instead) {
        this.diagnostics.error(where.line(), where.column(), SigmaError.NOT_IN_THE_SUBSET, what, instead);
    }
}
