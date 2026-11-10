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
import dev.jstech.computers.sigma.ast.IStmt;
import dev.jstech.computers.sigma.ast.Operator;
import dev.jstech.computers.sigma.ast.TypeRef;
import dev.jstech.computers.sigma.lex.Preprocessor;
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import java.util.List;

/**
 * What came in version 3 of the language, reported where it was written when the build is held to an older one
 * (computers/docs/design/sigma.md).
 *
 * <p>The parser reads the whole of version 3 whatever the build is held to, so a program is read the same way on every
 * machine, and the version is a question asked here, once, of the tree. A word the older versions already kept, such
 * as {@code const}, is refused the way they refused it, with what to write until then; anything else that came later is
 * said to need the later version. The switches over the tree's own kinds are exhaustive on purpose: a form added to the
 * language stops this file compiling until somebody has said which version brought it.
 */
@TextHolder
public final class VersionRules {

    private final DiagnosticBag diagnostics;
    /** The language and the version of it the build is held to, which the messages name. */
    private LanguageLevel level = LanguageLevel.SIGMA_SHARP;
    private int version;

    /** The version that brought every form checked here. */
    private static final int THIRD = 3;

    private static final TextKey CONST_INSTEAD = TextKey.of("jsc.sigma.version_rules.const_instead",
            "write a plain variable");
    private static final TextKey CONST_FIELD_INSTEAD = TextKey.of("jsc.sigma.version_rules.const_field_instead",
            "write a static readonly field");
    private static final TextKey GOTO_INSTEAD = TextKey.of("jsc.sigma.version_rules.goto_instead",
            "write a loop, and leave it with break or continue");
    private static final TextKey SWITCH_INSTEAD = TextKey.of("jsc.sigma.version_rules.switch_instead",
            "keep a bool, and test it with if");
    private static final TextKey EXCEPTIONS_INSTEAD = TextKey.of("jsc.sigma.version_rules.exceptions_instead",
            "a mistake halts the program, and a call that can fail answers instead");
    private static final TextKey LABELS = TextKey.of("jsc.sigma.version_rules.labels", "labels");
    private static final TextKey STATIC_CLASSES = TextKey.of("jsc.sigma.version_rules.static_classes",
            "static classes");
    private static final TextKey LOCAL_FUNCTIONS = TextKey.of("jsc.sigma.version_rules.local_functions",
            "local functions");
    private static final TextKey TUPLES = TextKey.of("jsc.sigma.version_rules.tuples", "tuples");
    private static final TextKey EXTENSION_METHODS = TextKey.of("jsc.sigma.version_rules.extension_methods",
            "extension methods");
    private static final TextKey OPERATORS = TextKey.of("jsc.sigma.version_rules.operators",
            "operators of your own types");
    private static final TextKey INDEXERS = TextKey.of("jsc.sigma.version_rules.indexers", "indexers");
    private static final TextKey POINTERS = TextKey.of("jsc.sigma.version_rules.pointers", "pointers");

    public VersionRules(final DiagnosticBag diagnostics) {
        this.diagnostics = diagnostics;
    }

    /** Reports everything in {@code units} that came after {@code version} of {@code level}. */
    public void check(final List<CompilationUnit> units, final LanguageLevel level, final int version) {
        if (version >= THIRD) {
            return;
        }
        this.level = level;
        this.version = version;
        for (final CompilationUnit unit : units) {
            this.diagnostics.setFile(unit.file());
            for (final Preprocessor.Directive directive : unit.directives()) {
                this.later(directive.line(), directive.column(), Text.literal("#" + directive.word()));
            }
            for (final IDecl.ITypeDecl type : unit.types()) {
                this.type(type);
            }
        }
    }

    private void type(final IDecl.ITypeDecl declaration) {
        this.sealed(declaration);
        switch (declaration) {
            case IDecl.ClassDecl klass -> {
                if (klass.modifiers().contains(IDecl.Modifier.STATIC)) {
                    this.later(klass.line(), klass.column(), STATIC_CLASSES.text());
                }
                for (final IDecl.IMemberDecl member : klass.members()) {
                    this.member(member);
                }
            }
            case IDecl.EnumDecl numbers -> {
                for (final IDecl.EnumConstant constant : numbers.constants()) {
                    this.expression(constant.value());
                }
            }
            case IDecl.InterfaceDecl face -> face.methods().forEach(method -> {
                this.typeRef(method.returnType());
                this.parameters(method.parameters());
            });
            case IDecl.DelegateDecl delegate -> {
                this.typeRef(delegate.returnType());
                this.parameters(delegate.parameters());
            }
        }
    }

    private void member(final IDecl.IMemberDecl member) {
        if (!(member instanceof IDecl.TypeMember)) {
            this.sealed(member);
        }
        switch (member) {
            case IDecl.FieldDecl field -> {
                if (field.modifiers().contains(IDecl.Modifier.CONST)) {
                    this.kept(field.line(), field.column(), "const", CONST_FIELD_INSTEAD);
                }
                this.typeRef(field.type());
                this.expression(field.initializer());
            }
            case IDecl.MethodDecl method -> {
                if (method.modifiers().contains(IDecl.Modifier.OPERATOR)) {
                    this.later(method.line(), method.column(), OPERATORS.text());
                }
                if (method.modifiers().contains(IDecl.Modifier.INDEXER)) {
                    this.later(method.line(), method.column(), INDEXERS.text());
                }
                this.typeRef(method.returnType());
                this.parameters(method.parameters());
                this.statement(method.body());
            }
            case IDecl.ConstructorDecl constructor -> {
                this.parameters(constructor.parameters());
                if (constructor.chained() != null) {
                    constructor.chained().arguments().forEach(this::expression);
                }
                this.statement(constructor.body());
            }
            case IDecl.PropertyDecl property -> {
                this.typeRef(property.type());
                this.expression(property.value());
            }
            case IDecl.EventDecl ignored -> { }
            case IDecl.TypeMember nested -> this.type(nested.type());
        }
    }

    private void statement(final IStmt statement) {
        if (statement == null) {
            return;
        }
        switch (statement) {
            case IStmt.Block block -> block.statements().forEach(this::statement);
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
                this.statement(loop.body());
                this.expression(loop.condition());
            }
            case IStmt.For loop -> {
                loop.initializers().forEach(this::statement);
                this.expression(loop.condition());
                loop.updates().forEach(this::expression);
                this.statement(loop.body());
            }
            case IStmt.ForEach loop -> {
                this.typeRef(loop.type());
                this.expression(loop.source());
                this.statement(loop.body());
            }
            case IStmt.Switch choice -> {
                this.expression(choice.value());
                for (final IStmt.SwitchSection section : choice.sections()) {
                    section.labels().forEach(this::expression);
                    section.statements().forEach(this::statement);
                }
            }
            case IStmt.Return give -> this.expression(give.value());
            case IStmt.LocalDecl local -> {
                if (local.constant()) {
                    this.kept(local.line(), local.column(), "const", CONST_INSTEAD);
                }
                this.typeRef(local.type());
                this.expression(local.initializer());
            }
            case IStmt.ExprStmt standing -> this.expression(standing.expression());
            case IStmt.Dispose freed -> this.expression(freed.target());
            case IStmt.Lock held -> {
                this.expression(held.target());
                this.statement(held.body());
            }
            case IStmt.Unsafe unsafe -> {
                this.later(unsafe.line(), unsafe.column(), Text.literal(IDecl.Modifier.UNSAFE.text()));
                this.statement(unsafe.body());
            }
            case IStmt.Labeled labeled -> {
                this.later(labeled.line(), labeled.column(), LABELS.text());
                this.statement(labeled.statement());
            }
            case IStmt.Goto jump -> {
                this.kept(jump.line(), jump.column(), "goto", GOTO_INSTEAD);
                this.expression(jump.caseValue());
            }
            case IStmt.ComeFrom from -> this.kept(from.line(), from.column(), "comefrom", GOTO_INSTEAD);
            case IStmt.Abstain switched -> this.kept(switched.line(), switched.column(),
                    switched.reinstate() ? "reinstate" : "abstainfrom", SWITCH_INSTEAD);
            case IStmt.Ignore place -> {
                this.later(place.line(), place.column(), Text.literal(place.remember() ? "remember" : "ignore"));
                this.expression(place.place());
            }
            case IStmt.LocalFunction function -> {
                this.later(function.line(), function.column(), LOCAL_FUNCTIONS.text());
                this.typeRef(function.returnType());
                this.parameters(function.body().parameters());
                this.expression(function.body().body());
                this.statement(function.body().block());
            }
            case IStmt.Deconstruct taken -> {
                this.later(taken.line(), taken.column(), TUPLES.text());
                this.expression(taken.value());
            }
            case IStmt.Try guarded -> {
                this.kept(guarded.line(), guarded.column(), "try", EXCEPTIONS_INSTEAD);
                this.statement(guarded.body());
                for (final IStmt.Catch one : guarded.catches()) {
                    this.typeRef(one.type());
                    this.statement(one.body());
                }
                this.statement(guarded.cleanup());
            }
            case IStmt.Throw thrown -> {
                this.kept(thrown.line(), thrown.column(), "throw", EXCEPTIONS_INSTEAD);
                this.expression(thrown.value());
            }
            case IStmt.Break ignored -> { }
            case IStmt.Continue ignored -> { }
            case IStmt.Empty ignored -> { }
        }
    }

    private void expression(final IExpr expression) {
        if (expression == null) {
            return;
        }
        switch (expression) {
            case IExpr.Unary unary -> {
                if (unary.operator() == Operator.ADDRESS_OF || unary.operator() == Operator.DEREFERENCE) {
                    this.later(unary.line(), unary.column(), POINTERS.text());
                }
                this.expression(unary.operand());
            }
            case IExpr.Binary binary -> {
                this.expression(binary.left());
                this.expression(binary.right());
            }
            case IExpr.Assign assign -> {
                this.expression(assign.target());
                this.expression(assign.value());
            }
            case IExpr.Conditional conditional -> {
                this.expression(conditional.condition());
                this.expression(conditional.whenTrue());
                this.expression(conditional.whenFalse());
            }
            case IExpr.Call call -> {
                this.expression(call.callee());
                call.arguments().forEach(this::expression);
            }
            case IExpr.Member member -> this.expression(member.target());
            case IExpr.Index index -> {
                this.expression(index.target());
                this.expression(index.index());
            }
            case IExpr.New created -> {
                this.typeRef(created.type());
                created.arguments().forEach(this::expression);
            }
            case IExpr.NewArray created -> {
                this.typeRef(created.elementType());
                this.expression(created.length());
            }
            case IExpr.Cast cast -> {
                this.typeRef(cast.type());
                this.expression(cast.value());
            }
            case IExpr.TypeTest test -> {
                this.typeRef(test.type());
                this.expression(test.value());
            }
            case IExpr.Lambda lambda -> {
                lambda.parameters().forEach(parameter -> this.typeRef(parameter.type()));
                this.expression(lambda.body());
                this.statement(lambda.block());
            }
            case IExpr.Tuple tuple -> {
                this.later(tuple.line(), tuple.column(), TUPLES.text());
                tuple.elements().forEach(this::expression);
            }
            case IExpr.Interpolation written -> {
                for (final Object part : written.parts()) {
                    if (part instanceof IExpr hole) {
                        this.expression(hole);
                    }
                }
            }
            case IExpr.StackAlloc made -> {
                this.later(made.line(), made.column(), Text.literal("stackalloc"));
                this.expression(made.length());
            }
            case IExpr.SizeOf sized -> this.later(sized.line(), sized.column(), Text.literal("sizeof"));
            case IExpr.Literal ignored -> { }
            case IExpr.Name ignored -> { }
            case IExpr.This ignored -> { }
            case IExpr.Base ignored -> { }
            case IExpr.OutArgument outward -> this.typeRef(outward.type());
        }
    }

    /* A type written with a tuple, a pointer or a byte in it, reported once, at the outermost of them. */
    private void typeRef(final TypeRef type) {
        if (type == null) {
            return;
        }
        if (type.isTuple()) {
            this.later(type.line(), type.column(), TUPLES.text());
            return;
        }
        if (type.isPointer()) {
            this.later(type.line(), type.column(), POINTERS.text());
            return;
        }
        if (type.name().equals(ITypeSymbol.Primitive.BYTE.describe())) {
            this.later(type.line(), type.column(), Text.literal(type.name()));
            return;
        }
        type.arguments().forEach(this::typeRef);
    }

    /* A declaration written sealed, or unsafe. */
    private void sealed(final IDecl declaration) {
        if (declaration.modifiers().contains(IDecl.Modifier.SEALED)) {
            this.later(declaration.line(), declaration.column(), Text.literal(IDecl.Modifier.SEALED.text()));
        }
        if (declaration.modifiers().contains(IDecl.Modifier.UNSAFE)) {
            this.later(declaration.line(), declaration.column(), Text.literal(IDecl.Modifier.UNSAFE.text()));
        }
    }

    /* A parameter written with params, or of a tuple's type. */
    private void parameters(final List<IDecl.Parameter> parameters) {
        for (final IDecl.Parameter parameter : parameters) {
            this.typeRef(parameter.type());
            if (parameter.many()) {
                this.later(parameter.line(), parameter.column(), Text.literal("params"));
            }
            if (parameter.receiver()) {
                this.later(parameter.line(), parameter.column(), EXTENSION_METHODS.text());
            }
        }
    }

    /* A word the older versions kept and refused, refused again the way they did, with what to write until then. */
    private void kept(final int line, final int column, final String word, final TextKey instead) {
        this.diagnostics.error(line, column, SigmaError.NOT_IN_THE_LANGUAGE, Text.literal(word), instead.text());
    }

    /* Anything else version 3 brought, said to need it, as a member of the library that came later is. */
    private void later(final int line, final int column, final Text what) {
        this.diagnostics.error(line, column, SigmaError.NEEDS_A_LATER_VERSION, what, this.level.mark(), THIRD,
                this.level.mark(), this.version);
    }
}
