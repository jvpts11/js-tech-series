/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.sigma.sem;

import dev.jstech.computers.sigma.DiagnosticBag;
import dev.jstech.computers.sigma.SigmaError;
import dev.jstech.computers.sigma.ast.IDecl;
import dev.jstech.computers.sigma.ast.IExpr;
import dev.jstech.computers.sigma.ast.Operator;
import java.util.List;

/**
 * Checks what every method actually does against what the types say.
 *
 * <p>It runs once the symbols exist, so a body can mention anything the program declares whatever
 * order it was written in. Each expression is given a type and each name is tied to the thing it
 * stands for; both are kept in the model for the stage that emits the assembly.
 *
 * <p>A mistake never stops the walk. An expression it cannot make sense of becomes the error type,
 * which fits everywhere, so one unknown name produces one message instead of one for every line that
 * used it.
 *
 * <p>This part is the way in and the outer layer: each type, and inside it each field, method and
 * constructor, set up with what is true of that body before the body itself is walked. What a method
 * does, what its values are, what its members reach and what it calls are four readers of their own,
 * all working through one shared {@link BodyScope}.
 */
public final class BodyChecker {

    private final BodyScope scope;
    private final ExpressionChecker expressions;
    private final StatementChecker statements;

    public BodyChecker(final BuiltIns builtIns, final TypeRules rules, final Declarations declarations,
                       final DiagnosticBag diagnostics, final SemanticModel model) {
        this.scope = new BodyScope(builtIns, rules, declarations, model, diagnostics);
        this.expressions = new ExpressionChecker(this.scope);
        this.statements = this.expressions.statements();
    }

    /** The number an enum's constant was written as, or null if it was written some other way. */
    public static Integer numberOf(final IExpr expression) {
        if (expression instanceof IExpr.Literal literal && literal.value() instanceof Integer value) {
            return value;
        }
        if (expression instanceof IExpr.Unary unary && unary.operator() == Operator.NEGATE
                && !unary.postfix() && unary.operand() instanceof IExpr.Literal literal
                && literal.value() instanceof Integer value) {
            return -value;
        }
        return null;
    }

    /** Checks every body in every type the program declares. */
    public void check(final List<NamedType> types) {
        for (final NamedType type : types) {
            final IDecl.ITypeDecl source = this.scope.declarations().source(type);
            this.scope.file(this.scope.declarations().fileOf(type));
            if (source instanceof IDecl.ClassDecl declaration) {
                this.checkClass(type, declaration);
            } else if (source instanceof IDecl.EnumDecl declaration) {
                this.checkEnum(type, declaration);
            }
        }
    }

    private void checkClass(final NamedType type, final IDecl.ClassDecl declaration) {
        this.scope.currentType(type);
        for (final IDecl.IMemberDecl member : declaration.members()) {
            switch (member) {
                case IDecl.FieldDecl field -> this.checkFieldInitializer(field);
                case IDecl.MethodDecl method -> this.checkMethod(method);
                case IDecl.ConstructorDecl constructor -> this.checkConstructor(type, constructor);
                case IDecl.PropertyDecl ignored -> { }
                case IDecl.EventDecl ignored -> { }
                case IDecl.TypeMember ignored -> { } // checked as a type of its own, in its turn
            }
        }
    }

    // An enum's numbers are the one place outside a body where an expression can appear.
    private void checkEnum(final NamedType type, final IDecl.EnumDecl declaration) {
        this.scope.currentType(type);
        this.scope.begin(true, ITypeSymbol.Primitive.VOID, false);
        for (final IDecl.EnumConstant constant : declaration.constants()) {
            if (constant.value() == null) {
                continue;
            }
            this.scope.expect(this.expressions.check(constant.value(), ITypeSymbol.Primitive.INT),
                    ITypeSymbol.Primitive.INT, constant.value());
            /*
             * The number has to be there in the source, not worked out from it: an enum's numbers are
             * what the assembly and every saved file are written with, so they are read, never computed.
             */
            if (numberOf(constant.value()) == null) {
                this.scope.report(constant.value().line(), constant.value().column(),
                        SigmaError.ENUM_VALUE_MUST_BE_WRITTEN);
            }
        }
    }

    private void checkFieldInitializer(final IDecl.FieldDecl field) {
        if (field.initializer() == null) {
            return;
        }
        final ITypeSymbol declared = this.scope.declarations().resolve(field.type(), this.scope.currentType());
        this.scope.begin(field.modifiers().contains(IDecl.Modifier.STATIC), ITypeSymbol.Primitive.VOID, false);
        this.scope.expect(this.expressions.check(field.initializer(), declared), declared, field.initializer());
    }

    private void checkMethod(final IDecl.MethodDecl method) {
        if (method.body() == null) {
            return;
        }
        final ITypeSymbol declared = this.scope.declarations()
                .resolve(method.returnType(), this.scope.currentType());
        this.scope.begin(method.modifiers().contains(IDecl.Modifier.STATIC), declared, false);
        this.declareParameters(method.parameters());
        this.statements.checkBlock(method.body(), false);
        this.scope.checkOutParameters(method.parameters(), method.body(), method);
        if (declared != ITypeSymbol.Primitive.VOID && !StatementChecker.alwaysReturns(method.body())) {
            this.scope.report(method.line(), method.column(),
                    SigmaError.MISSING_RETURN_VALUE, declared.describe());
        }
    }

    private void checkConstructor(final NamedType type, final IDecl.ConstructorDecl constructor) {
        this.scope.begin(false, ITypeSymbol.Primitive.VOID, true);
        this.declareParameters(constructor.parameters());
        if (constructor.chained() != null) {
            this.checkChainedCall(type, constructor.chained());
        }
        if (constructor.body() != null) {
            this.statements.checkBlock(constructor.body(), false);
        }
        this.scope.checkOutParameters(constructor.parameters(), constructor.body(), constructor);
    }

    private void checkChainedCall(final NamedType type, final IDecl.ConstructorCall chained) {
        final NamedType target = chained.base() ? type.base() : type;
        if (target == null) {
            this.scope.report(chained.line(), chained.column(), SigmaError.NO_BASE_CLASS, type.name());
            this.expressions.calls().checkArguments(chained.arguments());
            return;
        }
        this.expressions.calls().callConstructor(target, target, chained.arguments(), chained);
    }

    private void declareParameters(final List<IDecl.Parameter> parameters) {
        for (final IDecl.Parameter parameter : parameters) {
            final ITypeSymbol type = this.scope.declarations()
                    .resolve(parameter.type(), this.scope.currentType());
            final IBinding.Variable variable = new IBinding.Variable(parameter.name(), type, true);
            if (!this.scope.scope().declare(variable)) {
                this.scope.report(parameter.line(), parameter.column(),
                        SigmaError.DUPLICATE_DECLARATION, parameter.name());
            }
            this.scope.model().setDeclared(parameter, variable);
        }
    }
}
