/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.sigma.sem;

import dev.jstech.computers.sigma.SigmaError;
import dev.jstech.computers.sigma.ast.IDecl;
import dev.jstech.computers.sigma.ast.IExpr;
import dev.jstech.computers.sigma.ast.Operator;
import dev.jstech.computers.sigma.lower.Lowerer;
import java.util.List;

/**
 * Works out what an expression is, and writes the answer into the model.
 *
 * <p>One walk over the shape a value can take, giving each piece a type and tying each name to the thing
 * it stands for. What comes back is never null: an expression it cannot make sense of is the error type,
 * which fits everywhere, so one unknown name produces one message rather than one for every line using it.
 *
 * <p>What is expected of an expression is carried down into it, because two shapes have no type of their
 * own until they know: a lambda, and a method handed over without brackets. Both become the delegate they
 * are being handed to, or a mistake if nothing is waiting for one.
 *
 * <p>Reaching a member, calling something and checking statements are three readers of their own. They are
 * made here because the walk is what reaches them, and handed on to whoever else needs them.
 */
final class ExpressionChecker {

    private final BodyScope scope;
    private final MemberChecker members;
    private final CallChecker calls;
    private final StatementChecker statements;

    /*
     * The three below are made here, holding this one, because every one of them ends up needing a value
     * worked out and there is no order in which they could be built apart. Each only keeps it.
     */
    ExpressionChecker(final BodyScope scope) {
        this.scope = scope;
        this.members = new MemberChecker(scope, this);
        this.calls = new CallChecker(scope, this, this.members);
        this.statements = new StatementChecker(scope, this);
    }

    /** Whether an argument is an out argument written as {@code var}, which waits for the version chosen. */
    static boolean waitsForItsParameter(final IExpr argument) {
        return argument instanceof IExpr.OutArgument outward && StatementChecker.isInferred(outward.type());
    }

    MemberChecker members() {
        return this.members;
    }

    CallChecker calls() {
        return this.calls;
    }

    StatementChecker statements() {
        return this.statements;
    }

    ITypeSymbol check(final IExpr expression, final ITypeSymbol expected) {
        if (expression == null) {
            return ITypeSymbol.Special.ERROR;
        }
        final ITypeSymbol type = switch (expression) {
            case IExpr.Literal literal -> this.literalType(literal);
            case IExpr.Name name -> this.nameType(name, expected);
            case IExpr.This self -> this.thisType(self);
            case IExpr.Base base -> this.baseType(base);
            case IExpr.Unary unary -> this.unaryType(unary);
            case IExpr.Binary binary -> this.binaryType(binary);
            case IExpr.Assign assign -> this.assignType(assign);
            case IExpr.Conditional conditional -> this.conditionalType(conditional, expected);
            case IExpr.Call call -> this.calls.callType(call);
            case IExpr.Member member -> this.members.memberType(member, expected);
            case IExpr.Index index -> this.indexType(index);
            case IExpr.New created -> this.newType(created);
            case IExpr.NewArray created -> this.newArrayType(created);
            case IExpr.Cast cast -> this.castType(cast);
            case IExpr.TypeTest test -> this.typeTestType(test);
            case IExpr.Lambda lambda -> this.lambdaType(lambda, expected);
            case IExpr.OutArgument outward -> this.outArgumentType(outward, expected);
            case IExpr.Interpolation written -> this.interpolationType(written);
        };
        this.scope.model().setType(expression, type);
        return type;
    }

    /**
     * A string with holes is text, whatever is in the holes.
     *
     * <p>Each hole is checked so that a mistake inside one is reported where it was written, and then nothing
     * more is asked of what it turned out to be: anything at all can be made into text, which is the whole
     * point of writing it this way rather than converting each piece by hand.
     */
    private ITypeSymbol interpolationType(final IExpr.Interpolation written) {
        for (final IExpr hole : Lowerer.holesOf(written)) {
            this.check(hole, null);
        }
        return this.scope.builtIns().stringType();
    }

    private ITypeSymbol literalType(final IExpr.Literal literal) {
        return switch (literal.kind()) {
            case INT_LITERAL -> ITypeSymbol.Primitive.INT;
            case LONG_LITERAL -> ITypeSymbol.Primitive.LONG;
            case FLOAT_LITERAL -> ITypeSymbol.Primitive.FLOAT;
            case DOUBLE_LITERAL -> ITypeSymbol.Primitive.DOUBLE;
            case CHAR_LITERAL -> ITypeSymbol.Primitive.CHAR;
            case STRING_LITERAL -> this.scope.builtIns().stringType();
            case TRUE, FALSE -> ITypeSymbol.Primitive.BOOL;
            default -> ITypeSymbol.Special.NULL;
        };
    }

    private ITypeSymbol nameType(final IExpr.Name name, final ITypeSymbol expected) {
        final IBinding.Variable variable = this.scope.scope().lookup(name.identifier());
        if (variable != null) {
            this.scope.model().setBinding(name, variable);
            return variable.type();
        }
        if (this.scope.currentType() != null) {
            final List<IMemberSymbol> found = BodyScope.lookup(this.scope.currentType(), name.identifier());
            if (!found.isEmpty()) {
                return this.members.bindMember(name, this.scope.currentType(), found, expected,
                        BodyScope.Access.IMPLICIT);
            }
        }
        final ITypeSymbol type = this.scope.declarations().lookup(name.identifier(), this.scope.currentType());
        if (type != null) {
            this.scope.model().setBinding(name, new IBinding.TypeName(type));
            return type;
        }
        this.scope.declarations().reportUnknown(name.line(), name.column(), name.identifier());
        return ITypeSymbol.Special.ERROR;
    }

    private ITypeSymbol thisType(final IExpr.This self) {
        if (this.scope.staticContext()) {
            this.scope.report(self.line(), self.column(), SigmaError.THIS_IN_STATIC, "this");
            return ITypeSymbol.Special.ERROR;
        }
        return this.scope.currentType() == null ? ITypeSymbol.Special.ERROR : this.scope.currentType();
    }

    private ITypeSymbol baseType(final IExpr.Base base) {
        if (this.scope.staticContext()) {
            this.scope.report(base.line(), base.column(), SigmaError.THIS_IN_STATIC, "base");
            return ITypeSymbol.Special.ERROR;
        }
        if (this.scope.currentType() == null || this.scope.currentType().base() == null) {
            this.scope.report(base.line(), base.column(), SigmaError.NO_BASE_CLASS,
                    this.scope.currentType() == null ? "?" : this.scope.currentType().name());
            return ITypeSymbol.Special.ERROR;
        }
        return this.scope.currentType().base();
    }

    private ITypeSymbol unaryType(final IExpr.Unary unary) {
        final ITypeSymbol operand = this.check(unary.operand(), null);
        final ITypeSymbol result = this.scope.rules().unaryResult(unary.operator(), operand);
        if (result == null) {
            this.scope.report(unary.line(), unary.column(), SigmaError.OPERATOR_ON_TYPE,
                    unary.operator().text(), operand.describe());
            return ITypeSymbol.Special.ERROR;
        }
        return result;
    }

    private ITypeSymbol binaryType(final IExpr.Binary binary) {
        final ITypeSymbol left = this.check(binary.left(), null);
        final ITypeSymbol right = this.check(binary.right(), null);
        final ITypeSymbol result = this.scope.rules().binaryResult(binary.operator(), left, right);
        if (result == null) {
            this.scope.report(binary.line(), binary.column(), SigmaError.OPERATOR_ON_TYPES,
                    binary.operator().text(), left.describe(), right.describe());
            return ITypeSymbol.Special.ERROR;
        }
        return result;
    }

    private ITypeSymbol conditionalType(final IExpr.Conditional conditional, final ITypeSymbol expected) {
        this.statements.condition(conditional.condition());
        final ITypeSymbol whenTrue = this.check(conditional.whenTrue(), expected);
        final ITypeSymbol whenFalse = this.check(conditional.whenFalse(), expected);
        if (this.scope.rules().isAssignable(whenFalse, whenTrue)) {
            return whenTrue;
        }
        if (this.scope.rules().isAssignable(whenTrue, whenFalse)) {
            return whenFalse;
        }
        this.scope.report(conditional.line(), conditional.column(),
                SigmaError.CANNOT_CONVERT, whenFalse.describe(), whenTrue.describe());
        return ITypeSymbol.Special.ERROR;
    }

    private ITypeSymbol indexType(final IExpr.Index index) {
        final ITypeSymbol target = this.check(index.target(), null);
        final ITypeSymbol key = this.check(index.index(), null);
        final ITypeSymbol result = this.scope.rules().indexResult(target, key);
        if (result == null) {
            this.scope.report(index.line(), index.column(), SigmaError.CANNOT_INDEX,
                    target.describe(), key.describe());
            return ITypeSymbol.Special.ERROR;
        }
        return result;
    }

    private ITypeSymbol newType(final IExpr.New created) {
        final ITypeSymbol type = this.scope.declarations().resolve(created.type(), this.scope.currentType());
        final NamedType named = this.scope.rules().named(type);
        if (named == null || !named.kind().classLike()) {
            if (!this.scope.rules().isError(type)) {
                this.scope.report(created.line(), created.column(),
                        SigmaError.CANNOT_CREATE, type.describe());
            }
            this.calls.checkArguments(created.arguments());
            return this.scope.rules().isError(type) ? ITypeSymbol.Special.ERROR : type;
        }
        /*
         * An abstract class is a shape for others to fill, and part of it has no body at all, so there is
         * nothing to make. The object still has to be some class further down that answered every one.
         */
        if (named.isAbstract()) {
            this.scope.report(created.line(), created.column(),
                    SigmaError.CANNOT_CREATE_ABSTRACT, type.describe());
        }
        this.calls.callConstructor(named, type, created.arguments(), created);
        return type;
    }

    private ITypeSymbol newArrayType(final IExpr.NewArray created) {
        final ITypeSymbol element = this.scope.declarations()
                .resolve(created.elementType(), this.scope.currentType());
        this.scope.expect(this.check(created.length(), ITypeSymbol.Primitive.INT),
                ITypeSymbol.Primitive.INT, created.length());
        return new ITypeSymbol.ArrayType(element);
    }

    private ITypeSymbol castType(final IExpr.Cast cast) {
        final ITypeSymbol target = this.scope.declarations().resolve(cast.type(), this.scope.currentType());
        final ITypeSymbol value = this.check(cast.value(), null);
        if (!this.scope.rules().isAssignable(value, target)
                && !this.scope.rules().isAssignable(target, value)) {
            this.scope.report(cast.line(), cast.column(),
                    SigmaError.CANNOT_CONVERT, value.describe(), target.describe());
        }
        return target;
    }

    private ITypeSymbol typeTestType(final IExpr.TypeTest test) {
        final ITypeSymbol value = this.check(test.value(), null);
        final ITypeSymbol target = this.scope.declarations().resolve(test.type(), this.scope.currentType());
        final String written = test.conversion() ? "as" : "is";
        if (!this.scope.rules().isError(value) && !this.scope.rules().isReference(value)) {
            this.scope.report(test.line(), test.column(),
                    SigmaError.OPERATOR_ON_TYPE, written, value.describe());
        }
        if (test.conversion() && !this.scope.rules().isError(target)
                && !this.scope.rules().isReference(target)) {
            this.scope.report(test.line(), test.column(),
                    SigmaError.OPERATOR_ON_TYPE, written, target.describe());
            return ITypeSymbol.Special.ERROR;
        }
        return test.conversion() ? target : ITypeSymbol.Primitive.BOOL;
    }

    private ITypeSymbol assignType(final IExpr.Assign assign) {
        final ITypeSymbol target = this.check(assign.target(), null);
        final IBinding binding = this.scope.model().bindingOf(assign.target());
        if (binding instanceof IBinding.Member member
                && member.member() instanceof IMemberSymbol.EventSymbol event) {
            return this.subscribe(assign, event);
        }
        final ITypeSymbol value = this.check(assign.value(), target);
        if (this.scope.rules().isError(target) || !this.writable(assign, binding)) {
            return this.scope.rules().isError(target) ? ITypeSymbol.Special.ERROR : target;
        }
        if (assign.operator() == Operator.ASSIGN) {
            this.scope.expect(value, target, assign.value());
            return target;
        }
        final ITypeSymbol combined = this.scope.rules().binaryResult(assign.operator(), target, value);
        if (combined == null || !this.scope.rules().isAssignable(combined, target)) {
            this.scope.report(assign.line(), assign.column(), SigmaError.OPERATOR_ON_TYPES,
                    assign.operator().text() + "=", target.describe(), value.describe());
        }
        return target;
    }

    private ITypeSymbol subscribe(final IExpr.Assign assign, final IMemberSymbol.EventSymbol event) {
        if (assign.operator() != Operator.ADD && assign.operator() != Operator.SUBTRACT) {
            this.scope.report(assign.line(), assign.column(), SigmaError.OPERATOR_ON_TYPE,
                    assign.operator().text() + "=", event.delegateType().name());
            this.check(assign.value(), event.delegateType());
            return ITypeSymbol.Special.ERROR;
        }
        this.scope.expect(this.check(assign.value(), event.delegateType()), event.delegateType(),
                assign.value());
        return event.delegateType();
    }

    private boolean writable(final IExpr.Assign assign, final IBinding binding) {
        if (!(binding instanceof IBinding.Member member)) {
            return true;
        }
        if (member.member() instanceof IMemberSymbol.FieldSymbol field && field.isReadOnly()
                && !(this.scope.inConstructor() && field.owner() == this.scope.currentType())) {
            this.scope.report(assign.line(), assign.column(),
                    SigmaError.CANNOT_ASSIGN_READONLY, field.name());
            return false;
        }
        if (member.member() instanceof IMemberSymbol.PropertySymbol property && !property.writable()) {
            this.scope.report(assign.line(), assign.column(),
                    SigmaError.CANNOT_ASSIGN_READONLY, property.name());
            return false;
        }
        return true;
    }

    private ITypeSymbol lambdaType(final IExpr.Lambda lambda, final ITypeSymbol expected) {
        final NamedType wanted = this.scope.rules().named(expected);
        if (wanted == null || wanted.kind() != NamedType.Kind.DELEGATE || wanted.invoke() == null) {
            if (!this.scope.rules().isError(expected)) {
                this.scope.report(lambda.line(), lambda.column(), SigmaError.LAMBDA_SHAPE,
                        expected == null ? "nothing" : expected.describe());
            }
            return ITypeSymbol.Special.ERROR;
        }
        final IMemberSymbol.MethodSymbol shape = this.calls.fill(wanted.invoke(),
                this.scope.rules().arguments(expected));
        if (shape.parameters().size() != lambda.parameters().size()) {
            this.scope.report(lambda.line(), lambda.column(),
                    SigmaError.LAMBDA_SHAPE, expected.describe());
            return ITypeSymbol.Special.ERROR;
        }
        final Scope saved = this.scope.scope();
        final ITypeSymbol savedReturn = this.scope.returnType();
        this.scope.scope(new Scope(saved));
        this.declareLambdaParameters(lambda, shape);
        this.scope.returnType(shape.returnType());
        if (lambda.block() != null) {
            this.statements.checkBlock(lambda.block(), false);
        } else {
            final ITypeSymbol body = this.check(lambda.body(), shape.returnType());
            if (shape.returnType() != ITypeSymbol.Primitive.VOID) {
                this.scope.expect(body, shape.returnType(), lambda.body());
            }
        }
        this.scope.checkOutParameters(lambda.parameters(), lambda.block(), lambda);
        this.scope.scope(saved);
        this.scope.returnType(savedReturn);
        return expected;
    }

    private void declareLambdaParameters(final IExpr.Lambda lambda, final IMemberSymbol.MethodSymbol shape) {
        for (int i = 0; i < lambda.parameters().size(); i++) {
            final IDecl.Parameter parameter = lambda.parameters().get(i);
            if (parameter.outward() != shape.parameters().get(i).outward()) {
                this.scope.report(parameter.line(), parameter.column(), shape.parameters().get(i).outward()
                        ? SigmaError.OUT_ARGUMENT_EXPECTED : SigmaError.OUT_ARGUMENT_UNEXPECTED,
                        shape.parameters().get(i).name());
            }
            final ITypeSymbol fromShape = shape.parameters().get(i).type();
            ITypeSymbol type = fromShape;
            if (parameter.type() != null) {
                type = this.scope.declarations().resolve(parameter.type(), this.scope.currentType());
                if (!type.equals(fromShape)) {
                    this.scope.report(parameter.line(), parameter.column(),
                            SigmaError.CANNOT_CONVERT, fromShape.describe(), type.describe());
                }
            }
            final IBinding.Variable variable = new IBinding.Variable(parameter.name(), type, true);
            if (!this.scope.scope().declare(variable)) {
                this.scope.report(parameter.line(), parameter.column(),
                        SigmaError.DUPLICATE_DECLARATION, parameter.name());
            }
            this.scope.model().setDeclared(parameter, variable);
        }
    }

    /*
     * The place a method is being asked to write into: a local it declares here, a local that already
     * exists, or a field. Written as var, the local takes whatever the method fills in, which is only
     * known once the version of the method has been chosen.
     */
    private ITypeSymbol outArgumentType(final IExpr.OutArgument argument, final ITypeSymbol expected) {
        if (argument.type() != null) {
            final ITypeSymbol type = StatementChecker.isInferred(argument.type())
                    ? expected : this.scope.declarations().resolve(argument.type(), this.scope.currentType());
            if (type == null || this.scope.rules().isError(type)) {
                return ITypeSymbol.Special.ERROR;
            }
            final IBinding.Variable variable = new IBinding.Variable(argument.name(), type, false);
            if (!this.scope.scope().declare(variable)) {
                this.scope.report(argument.line(), argument.column(),
                        SigmaError.DUPLICATE_DECLARATION, argument.name());
            }
            this.scope.model().setBinding(argument, variable);
            this.scope.model().setDeclared(argument, variable);
            return type;
        }
        final IBinding.Variable variable = this.scope.scope().lookup(argument.name());
        if (variable != null) {
            this.scope.model().setBinding(argument, variable);
            return variable.type();
        }
        final List<IMemberSymbol> found = this.scope.currentType() == null
                ? List.of() : BodyScope.lookup(this.scope.currentType(), argument.name());
        if (!found.isEmpty()) {
            if (found.getFirst() instanceof IMemberSymbol.FieldSymbol field && !field.isReadOnly()) {
                this.scope.model().setBinding(argument, new IBinding.Member(field, field.type()));
                return field.type();
            }
            this.scope.report(argument.line(), argument.column(), SigmaError.OUT_NOT_A_PLACE, argument.name());
            return ITypeSymbol.Special.ERROR;
        }
        this.scope.report(argument.line(), argument.column(), SigmaError.UNKNOWN_NAME, argument.name());
        return ITypeSymbol.Special.ERROR;
    }
}
