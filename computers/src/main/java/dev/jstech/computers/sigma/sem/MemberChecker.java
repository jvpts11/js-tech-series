/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.sigma.sem;

import dev.jstech.computers.sigma.SigmaError;
import dev.jstech.computers.sigma.ast.IExpr;
import dev.jstech.computers.sigma.ast.INode;
import java.util.List;

/**
 * Reaching something that belongs to a type, and deciding whether it may be reached that way.
 *
 * <p>A dot is not one thing. It can be a namespace in front of a type, a type in front of a type nested
 * inside it, or a value in front of one of its members, and they are tried in that order because a local
 * called Tools has to win over a namespace called Tools.
 *
 * <p>Once what is on the left is known, what is on the right has to line up with it: something belonging
 * to the type cannot be read through a value and something belonging to a value cannot be read through
 * the type. A method is the exception, since a method written without brackets is a value only when
 * something is waiting for a delegate of exactly its shape.
 */
final class MemberChecker {

    private final BodyScope scope;
    private final ExpressionChecker expressions;

    MemberChecker(final BodyScope scope, final ExpressionChecker expressions) {
        this.scope = scope;
        this.expressions = expressions;
    }

    ITypeSymbol memberType(final IExpr.Member member, final ITypeSymbol expected) {
        /*
         * A type named with its namespace in front, Tools.Counter, is read as a member access, so the
         * names before the dot are tried as a namespace before they are tried as anything else. Only
         * when they spell nothing a local or a field could be, since a local called Tools comes first.
         */
        final String spelled = spell(member.target());
        if (spelled != null && this.scope.scope().lookup(spelled) == null
                && this.scope.declarations().isNamespace(spelled)) {
            final NamedType named = this.scope.declarations().lookup(spelled + "." + member.name(),
                    this.scope.currentType());
            if (named != null) {
                this.scope.model().setBinding(member, new IBinding.TypeName(named));
                return named;
            }
        }
        final ITypeSymbol target = this.expressions.check(member.target(), null);
        if (this.scope.rules().isError(target)) {
            return ITypeSymbol.Special.ERROR;
        }
        final BodyScope.Access access = this.scope.model().bindingOf(member.target()) instanceof IBinding.TypeName
                ? BodyScope.Access.TYPE : BodyScope.Access.INSTANCE;
        /*
         * A type named through the type it is nested in, Outer.Inner: the name before the dot is a
         * type, and what follows is one of the types inside it rather than one of its members.
         */
        if (access == BodyScope.Access.TYPE && this.scope.rules().named(target) instanceof NamedType outer
                && BodyScope.lookup(outer, member.name()).isEmpty()) {
            final NamedType inside = this.scope.declarations().lookup(
                    outer.qualifiedName() + "." + member.name(), this.scope.currentType());
            if (inside != null) {
                this.scope.model().setBinding(member, new IBinding.TypeName(inside));
                return inside;
            }
        }
        final List<IMemberSymbol> found = this.membersOf(target, member.name(), member);
        return found.isEmpty()
                ? ITypeSymbol.Special.ERROR
                : this.bindMember(member, target, found, expected, access);
    }

    List<IMemberSymbol> membersOf(final ITypeSymbol target, final String name, final INode at) {
        final NamedType named = this.scope.rules().named(target);
        final List<IMemberSymbol> found = named == null ? List.of() : BodyScope.lookup(named, name);
        if (found.isEmpty()) {
            this.scope.report(at.line(), at.column(),
                    SigmaError.NO_SUCH_MEMBER, target.describe(), name);
        }
        return found;
    }

    /*
     * A name that turned out to be a member: a value if it holds one, and a method only where a
     * delegate of the same shape is wanted, which is how a handler is handed over without brackets.
     */
    ITypeSymbol bindMember(final IExpr expression, final ITypeSymbol receiver,
                           final List<IMemberSymbol> members, final ITypeSymbol expected,
                           final BodyScope.Access access) {
        final IMemberSymbol first = members.getFirst();
        if (first instanceof IMemberSymbol.MethodSymbol) {
            return this.methodGroupType(expression, receiver, members, expected, access);
        }
        if (!this.checkAccess(expression, first, access)) {
            return ITypeSymbol.Special.ERROR;
        }
        final List<ITypeSymbol> arguments = this.scope.rules().arguments(receiver);
        final ITypeSymbol type = switch (first) {
            case IMemberSymbol.FieldSymbol field -> this.scope.rules().substitute(field.type(), arguments);
            case IMemberSymbol.PropertySymbol property ->
                    this.scope.rules().substitute(property.type(), arguments);
            case IMemberSymbol.EventSymbol event -> event.delegateType();
            default -> ITypeSymbol.Special.ERROR;
        };
        this.scope.model().setBinding(expression, new IBinding.Member(first, type));
        return type;
    }

    boolean checkAccess(final IExpr expression, final IMemberSymbol member, final BodyScope.Access access) {
        switch (access) {
            case TYPE:
                if (!member.isStatic()) {
                    this.scope.report(expression.line(), expression.column(),
                            SigmaError.INSTANCE_THROUGH_TYPE, member.name());
                    return false;
                }
                return true;
            case INSTANCE:
                if (member.isStatic()) {
                    this.scope.report(expression.line(), expression.column(),
                            SigmaError.STATIC_THROUGH_INSTANCE, member.name());
                    return false;
                }
                return true;
            default:
                if (!member.isStatic() && this.scope.staticContext()) {
                    this.scope.report(expression.line(), expression.column(),
                            SigmaError.THIS_IN_STATIC, member.name());
                    return false;
                }
                return true;
        }
    }

    /*
     * A name or a member that turns out to be a method, written without brackets. Like a lambda, it
     * has no type of its own until it is known what it is being handed to.
     */
    boolean isMethodGroup(final IExpr expression) {
        if (expression instanceof IExpr.Name name) {
            if (this.scope.scope().lookup(name.identifier()) != null || this.scope.currentType() == null) {
                return false;
            }
            final List<IMemberSymbol> found = BodyScope.lookup(this.scope.currentType(), name.identifier());
            return !found.isEmpty() && found.getFirst() instanceof IMemberSymbol.MethodSymbol;
        }
        if (expression instanceof IExpr.Member member) {
            this.scope.hush();
            final ITypeSymbol target = this.expressions.check(member.target(), null);
            this.scope.speakAgain();
            final NamedType named = this.scope.rules().named(target);
            if (named == null) {
                return false;
            }
            final List<IMemberSymbol> found = BodyScope.lookup(named, member.name());
            return !found.isEmpty() && found.getFirst() instanceof IMemberSymbol.MethodSymbol;
        }
        return false;
    }

    /**
     * The dotted name an expression spells when it is nothing but names, {@code Tools.Counter} for the
     * member access written that way, or null when any part of it is something else.
     */
    private static String spell(final IExpr expression) {
        return switch (expression) {
            case IExpr.Name name -> name.identifier();
            case IExpr.Member member -> {
                final String head = spell(member.target());
                yield head == null ? null : head + "." + member.name();
            }
            default -> null;
        };
    }

    private ITypeSymbol methodGroupType(final IExpr expression, final ITypeSymbol receiver,
                                        final List<IMemberSymbol> members, final ITypeSymbol expected,
                                        final BodyScope.Access access) {
        final NamedType wanted = this.scope.rules().named(expected);
        if (wanted == null || wanted.kind() != NamedType.Kind.DELEGATE || wanted.invoke() == null) {
            this.scope.report(expression.line(), expression.column(),
                    SigmaError.METHOD_AS_VALUE, members.getFirst().name());
            return ITypeSymbol.Special.ERROR;
        }
        final List<ITypeSymbol> wantedArguments = this.scope.rules().arguments(expected);
        final List<ITypeSymbol> ownArguments = this.scope.rules().arguments(receiver);
        for (final IMemberSymbol member : members) {
            final IMemberSymbol.MethodSymbol candidate = (IMemberSymbol.MethodSymbol) member;
            if (this.matchesShape(candidate, wanted.invoke(), wantedArguments, ownArguments)
                    && this.checkAccess(expression, candidate, access)) {
                this.scope.model().setBinding(expression, new IBinding.Member(candidate, expected));
                this.scope.model().setCall(expression, candidate);
                return expected;
            }
        }
        this.scope.report(expression.line(), expression.column(),
                SigmaError.LAMBDA_SHAPE, expected.describe());
        return ITypeSymbol.Special.ERROR;
    }

    private boolean matchesShape(final IMemberSymbol.MethodSymbol candidate,
                                 final IMemberSymbol.MethodSymbol shape,
                                 final List<ITypeSymbol> wantedArguments, final List<ITypeSymbol> ownArguments) {
        if (candidate.parameters().size() != shape.parameters().size()) {
            return false;
        }
        for (int i = 0; i < shape.parameters().size(); i++) {
            if (shape.parameters().get(i).outward() != candidate.parameters().get(i).outward()) {
                return false;
            }
            final ITypeSymbol wanted = this.scope.rules()
                    .substitute(shape.parameters().get(i).type(), wantedArguments);
            final ITypeSymbol given = this.scope.rules()
                    .substitute(candidate.parameters().get(i).type(), ownArguments);
            if (!wanted.equals(given)) {
                return false;
            }
        }
        final ITypeSymbol wantedReturn = this.scope.rules().substitute(shape.returnType(), wantedArguments);
        return this.scope.rules().isAssignable(
                this.scope.rules().substitute(candidate.returnType(), ownArguments), wantedReturn);
    }
}
