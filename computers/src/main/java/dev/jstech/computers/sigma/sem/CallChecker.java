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
import java.util.ArrayList;
import java.util.List;

/**
 * Checks one thing calling another, and picks which version of it was meant.
 *
 * <p>Three ways to be called and they end in the same place: a bare name that turns out to be a method
 * of the type around it, a method reached through a dot, and something holding a delegate, which is
 * called through the shape that delegate was declared with.
 *
 * <p>Choosing between versions of a method comes last and reads only the types, which is why a lambda
 * and a method handed over without brackets are left out of the choosing: neither has a type until it
 * is known what it is being handed to. They are checked afterwards, against the version that won.
 */
final class CallChecker {

    private final BodyScope scope;
    private final ExpressionChecker expressions;
    private final MemberChecker members;
    /** Choosing between the versions of a method that share a name, which is a question about types alone. */
    private final Overloads overloads;

    CallChecker(final BodyScope scope, final ExpressionChecker expressions, final MemberChecker members) {
        this.scope = scope;
        this.expressions = expressions;
        this.members = members;
        this.overloads = new Overloads(scope.rules());
    }

    ITypeSymbol callType(final IExpr.Call call) {
        if (call.callee() instanceof IExpr.Name name && this.scope.scope().lookup(name.identifier()) == null
                && this.scope.currentType() != null) {
            final List<IMemberSymbol> found = BodyScope.lookup(this.scope.currentType(), name.identifier());
            if (!found.isEmpty() && found.getFirst() instanceof IMemberSymbol.MethodSymbol) {
                return this.callMembers(call, name, this.scope.currentType(), found,
                        name.identifier(), BodyScope.Access.IMPLICIT);
            }
        }
        if (call.callee() instanceof IExpr.Member member) {
            return this.callThroughMember(call, member);
        }
        return this.invoke(call, this.expressions.check(call.callee(), null));
    }

    // A class with no constructor of its own can be made with no arguments and nothing else.
    void callConstructor(final NamedType named, final ITypeSymbol type, final List<IExpr> arguments,
                         final INode at) {
        final List<IMemberSymbol.MethodSymbol> candidates = new ArrayList<>();
        for (final IMemberSymbol member : named.members()) {
            if (member instanceof IMemberSymbol.ConstructorSymbol constructor) {
                candidates.add(new IMemberSymbol.MethodSymbol(named, named.name(), type,
                        constructor.parameters(), constructor.modifiers()));
            }
        }
        if (candidates.isEmpty()) {
            this.checkArguments(arguments);
            if (!arguments.isEmpty()) {
                this.scope.report(at.line(), at.column(),
                        SigmaError.NO_MATCHING_OVERLOAD, named.name());
            }
            return;
        }
        this.callWith(candidates, arguments, named.name(), at);
    }

    /*
     * A method read off a filled-in collection has its stand-in types replaced by what that
     * collection holds, so List<string>.Get gives back a string and not a T.
     */
    IMemberSymbol.MethodSymbol fill(final IMemberSymbol.MethodSymbol method,
                                    final List<ITypeSymbol> arguments) {
        if (arguments.isEmpty()) {
            return method;
        }
        final List<IMemberSymbol.ParameterSymbol> parameters = new ArrayList<>();
        for (final IMemberSymbol.ParameterSymbol parameter : method.parameters()) {
            parameters.add(new IMemberSymbol.ParameterSymbol(parameter.name(),
                    this.scope.rules().substitute(parameter.type(), arguments), parameter.outward()));
        }
        return new IMemberSymbol.MethodSymbol(method.owner(), method.name(),
                this.scope.rules().substitute(method.returnType(), arguments), parameters, method.modifiers());
    }

    void checkArguments(final List<IExpr> arguments) {
        for (final IExpr argument : arguments) {
            this.expressions.check(argument, null);
        }
    }

    private ITypeSymbol callThroughMember(final IExpr.Call call, final IExpr.Member member) {
        final ITypeSymbol target = this.expressions.check(member.target(), null);
        if (this.scope.rules().isError(target)) {
            this.checkArguments(call.arguments());
            return ITypeSymbol.Special.ERROR;
        }
        final BodyScope.Access access = this.scope.model().bindingOf(member.target()) instanceof IBinding.TypeName
                ? BodyScope.Access.TYPE : BodyScope.Access.INSTANCE;
        final List<IMemberSymbol> found = this.members.membersOf(target, member.name(), member);
        if (found.isEmpty()) {
            this.checkArguments(call.arguments());
            return ITypeSymbol.Special.ERROR;
        }
        if (found.getFirst() instanceof IMemberSymbol.MethodSymbol) {
            return this.callMembers(call, member, target, found, member.name(), access);
        }
        final ITypeSymbol held = this.members.bindMember(member, target, found, null, access);
        this.scope.model().setType(member, held);
        return this.invoke(call, held);
    }

    // Calling something that holds a delegate: a local, a parameter, a field, a property or an event.
    private ITypeSymbol invoke(final IExpr.Call call, final ITypeSymbol callee) {
        if (this.scope.rules().isError(callee)) {
            this.checkArguments(call.arguments());
            return ITypeSymbol.Special.ERROR;
        }
        final NamedType named = this.scope.rules().named(callee);
        if (named == null || named.kind() != NamedType.Kind.DELEGATE || named.invoke() == null) {
            this.scope.report(call.line(), call.column(), SigmaError.CANNOT_CALL, callee.describe());
            this.checkArguments(call.arguments());
            return ITypeSymbol.Special.ERROR;
        }
        this.checkEventRaise(call);
        final List<ITypeSymbol> arguments = this.scope.rules().arguments(callee);
        final IMemberSymbol.MethodSymbol shape = this.fill(named.invoke(), arguments);
        this.callWith(List.of(shape), call.arguments(), named.name(), call);
        return shape.returnType();
    }

    /*
     * Raising an event is only allowed where it was declared, as in the language this one borrows
     * from: everywhere else an event is something to subscribe to, not something to fire.
     */
    private void checkEventRaise(final IExpr.Call call) {
        if (this.scope.model().bindingOf(call.callee()) instanceof IBinding.Member member
                && member.member() instanceof IMemberSymbol.EventSymbol event
                && event.owner() != this.scope.currentType()) {
            this.scope.report(call.line(), call.column(), SigmaError.EVENT_OUTSIDE_ITS_TYPE);
        }
    }

    private ITypeSymbol callMembers(final IExpr.Call call, final IExpr callee, final ITypeSymbol receiver,
                                    final List<IMemberSymbol> found, final String name,
                                    final BodyScope.Access access) {
        if (!this.members.checkAccess(callee, found.getFirst(), access)) {
            this.checkArguments(call.arguments());
            return ITypeSymbol.Special.ERROR;
        }
        final List<ITypeSymbol> arguments = this.scope.rules().arguments(receiver);
        final List<IMemberSymbol.MethodSymbol> candidates = new ArrayList<>();
        for (final IMemberSymbol member : found) {
            if (member instanceof IMemberSymbol.MethodSymbol method) {
                candidates.add(this.fill(method, arguments));
            }
        }
        final IMemberSymbol.MethodSymbol chosen = this.callWith(candidates, call.arguments(), name, call);
        if (chosen != null) {
            this.scope.model().setBinding(callee, new IBinding.Member(chosen, chosen.returnType()));
        }
        return chosen == null ? ITypeSymbol.Special.ERROR : chosen.returnType();
    }

    /**
     * Picks the version that fits and checks the arguments against it. A lambda has no type until it
     * knows what it is being handed to, so it is left out of the choosing and checked afterwards,
     * against the version that won.
     */
    private IMemberSymbol.MethodSymbol callWith(final List<IMemberSymbol.MethodSymbol> candidates,
                                                final List<IExpr> arguments, final String name, final INode at) {
        final List<ITypeSymbol> given = new ArrayList<>();
        for (final IExpr argument : arguments) {
            final boolean waits = argument instanceof IExpr.Lambda
                    || ExpressionChecker.waitsForItsParameter(argument) || this.members.isMethodGroup(argument);
            given.add(waits ? null : this.expressions.check(argument, null));
        }
        final List<IMemberSymbol.MethodSymbol> fitting = this.overloads.best(candidates, given, arguments);
        if (fitting.isEmpty()) {
            return this.reportNoFit(candidates, arguments, given, name, at);
        }
        if (fitting.size() > 1) {
            this.scope.report(at.line(), at.column(), SigmaError.AMBIGUOUS_CALL, name);
        }
        final IMemberSymbol.MethodSymbol chosen = fitting.getFirst();
        this.checkAgainst(chosen, arguments, given);
        if (at instanceof IExpr expression) {
            this.scope.model().setCall(expression, chosen);
        }
        return chosen;
    }

    /*
     * When only one version could have been meant, saying which argument is wrong beats saying that
     * none of them fit: with a single version there is nothing to choose between.
     */
    private IMemberSymbol.MethodSymbol reportNoFit(final List<IMemberSymbol.MethodSymbol> candidates,
                                                   final List<IExpr> arguments, final List<ITypeSymbol> given,
                                                   final String name, final INode at) {
        final List<IMemberSymbol.MethodSymbol> sameCount = new ArrayList<>();
        for (final IMemberSymbol.MethodSymbol candidate : candidates) {
            if (candidate.parameters().size() == given.size()) {
                sameCount.add(candidate);
            }
        }
        if (sameCount.size() == 1) {
            final IMemberSymbol.MethodSymbol only = sameCount.getFirst();
            this.checkAgainst(only, arguments, given);
            if (at instanceof IExpr expression) {
                this.scope.model().setCall(expression, only);
            }
            return only;
        }
        this.scope.report(at.line(), at.column(), SigmaError.NO_MATCHING_OVERLOAD, name);
        for (int i = 0; i < arguments.size(); i++) {
            if (given.get(i) == null) {
                this.expressions.check(arguments.get(i), ITypeSymbol.Special.ERROR);
            }
        }
        return null;
    }

    private void checkAgainst(final IMemberSymbol.MethodSymbol chosen, final List<IExpr> arguments,
                              final List<ITypeSymbol> given) {
        for (int i = 0; i < arguments.size(); i++) {
            final IMemberSymbol.ParameterSymbol parameter = chosen.parameters().get(i);
            final IExpr argument = arguments.get(i);
            final boolean outward = argument instanceof IExpr.OutArgument;
            if (parameter.outward() != outward) {
                this.scope.report(argument.line(), argument.column(), parameter.outward()
                        ? SigmaError.OUT_ARGUMENT_EXPECTED : SigmaError.OUT_ARGUMENT_UNEXPECTED,
                        parameter.name());
                continue;
            }
            if (given.get(i) == null) {
                this.expressions.check(argument, parameter.type());
            } else if (outward) {
                if (!given.get(i).equals(parameter.type())) {
                    this.scope.report(argument.line(), argument.column(),
                            SigmaError.OUT_TYPE_MUST_MATCH, parameter.type().describe());
                }
            } else {
                this.scope.expect(given.get(i), parameter.type(), argument);
            }
        }
    }
}
