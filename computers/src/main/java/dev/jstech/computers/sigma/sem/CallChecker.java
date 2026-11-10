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
import dev.jstech.computers.sigma.ast.INode;
import dev.jstech.computers.sigma.ast.Operator;
import dev.jstech.computers.sigma.ast.TypeRef;
import dev.jstech.computers.sigma.lex.TokenKind;
import dev.jstech.core.text.Text;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Checks one thing calling another, and picks which version of it was meant.
 *
 * <p>Three ways to be called and they end in the same place: a bare name that turns out to be a method
 * of the type around it, a method reached through a dot, and something holding a delegate, which is
 * called through the shape that delegate was declared with. The calls written with no type in front of them,
 * printf and the old names, are checked apart, and only once the program has said the name is not its own.
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
    /** The calls written with no type in front of them: printf, and the old names for what the library does. */
    private final BareFunctionChecker bare;

    /** C#'s word for the text of a name, a word only where nothing of the program's is called it. */
    private static final String NAMEOF = "nameof";
    /** The version of the language nameof came in. */
    private static final int NAMEOF_SINCE = 3;
    /** C#'s word for the bytes a type takes, which came with pointers and is a word only where nothing is called it. */
    private static final String SIZEOF = "sizeof";
    /** The version of the language sizeof came in. */
    private static final int SIZEOF_SINCE = 3;

    CallChecker(final BodyScope scope, final ExpressionChecker expressions, final MemberChecker members) {
        this.scope = scope;
        this.expressions = expressions;
        this.members = members;
        this.overloads = new Overloads(scope.rules());
        this.bare = new BareFunctionChecker(scope, expressions, this);
    }

    ITypeSymbol callType(final IExpr.Call call) {
        if (this.isNameof(call)) {
            return this.nameof(call);
        }
        if (this.isOwnWord(call, SIZEOF) && call.arguments().size() == 1
                && chain(call.arguments().getFirst()) != null) {
            this.scope.declarations().reportIfLater(SIZEOF, SIZEOF_SINCE, call.line(), call.column());
            final IExpr named = call.arguments().getFirst();
            return this.expressions.sizeOf(this.scope.declarations().resolve(
                    TypeRef.named(chain(named), named.line(), named.column()), this.scope.currentType()), call);
        }
        if (call.callee() instanceof IExpr.Name name && this.scope.scope().lookup(name.identifier()) == null
                && this.scope.currentType() != null) {
            final List<IMemberSymbol> found = BodyScope.lookup(this.scope.currentType(), name.identifier());
            if (!found.isEmpty() && found.getFirst() instanceof IMemberSymbol.MethodSymbol) {
                return this.callMembers(call, name, this.scope.currentType(), found,
                        name.identifier(), BodyScope.Access.IMPLICIT);
            }
        }
        // Only once the program has been asked: a function of its own under one of these names is its own.
        final BareFunctions.Function bareFunction = this.bare.named(call);
        if (bareFunction != null) {
            return this.bare.check(call, bareFunction);
        }
        if (call.callee() instanceof IExpr.Member member) {
            return this.callThroughMember(call, member);
        }
        return this.invoke(call, this.expressions.check(call.callee(), null));
    }

    /**
     * The method a program's own type gives this operator, chosen among those of both values' types as C# chooses, or
     * null when neither declares one that fits and the language's own meaning stands.
     */
    IMemberSymbol.MethodSymbol operator(final Operator operator, final List<IExpr> operands,
                                        final List<ITypeSymbol> types, final INode at) {
        final String name = operator.methodName();
        if (name == null) {
            return null;
        }
        final List<IMemberSymbol.MethodSymbol> candidates = new ArrayList<>();
        for (final ITypeSymbol type : types) {
            final NamedType named = this.scope.rules().named(type);
            if (named == null || named.isBuiltIn()) {
                continue;
            }
            for (final IMemberSymbol member : named.allMembers()) {
                if (member instanceof IMemberSymbol.MethodSymbol method && method.isOperator()
                        && method.name().equals(name) && !candidates.contains(method)) {
                    candidates.add(method);
                }
            }
        }
        final List<IMemberSymbol.MethodSymbol> fitting = candidates.isEmpty() ? List.of()
                : this.overloads.best(candidates, types, operands);
        if (fitting.isEmpty()) {
            return null;
        }
        if (fitting.size() > 1) {
            this.scope.report(at.line(), at.column(), SigmaError.AMBIGUOUS_CALL, operator.text());
        }
        this.scope.model().setCall(at, fitting.getFirst());
        return fitting.getFirst();
    }

    /**
     * The method an indexer of the target's type reads or writes with, called with these values, or null when the type
     * has none; the one chosen is kept for the place.
     */
    IMemberSymbol.MethodSymbol indexer(final ITypeSymbol target, final String name, final List<IExpr> values,
                                       final List<ITypeSymbol> types, final INode at) {
        final NamedType named = this.scope.rules().named(target);
        if (named == null || named.isBuiltIn()) {
            return null;
        }
        // The one that writes is chosen by its keys alone, the value it is handed last being whatever the place holds.
        final boolean writes = IDecl.INDEXER_SET.equals(name);
        final Map<IMemberSymbol.MethodSymbol, IMemberSymbol.MethodSymbol> byKeys = new LinkedHashMap<>();
        for (final IMemberSymbol member : named.allMembers()) {
            if (member instanceof IMemberSymbol.MethodSymbol method && method.isIndexer() && method.name().equals(name)
                    && this.members.visible(method) && !(writes && method.parameters().isEmpty())) {
                byKeys.put(!writes ? method : new IMemberSymbol.MethodSymbol(method.owner(), method.name(),
                        method.returnType(), method.parameters().subList(0, method.parameters().size() - 1),
                        method.modifiers()), method);
            }
        }
        final List<IMemberSymbol.MethodSymbol> fitting = byKeys.isEmpty() ? List.of()
                : this.overloads.best(new ArrayList<>(byKeys.keySet()), types, values);
        if (fitting.size() > 1) {
            this.scope.report(at.line(), at.column(), SigmaError.AMBIGUOUS_CALL, "this[]");
        }
        return fitting.isEmpty() ? null : byKeys.get(fitting.getFirst());
    }

    /*
     * Whether a call is C#'s nameof: written so, and nothing of the program's is called that, since a method or a
     * variable a program written before the word named nameof is still what it calls.
     */
    private boolean isNameof(final IExpr.Call call) {
        return this.isOwnWord(call, NAMEOF);
    }

    /* Whether a call is written with the language's own word, which nothing of the program's is called. */
    private boolean isOwnWord(final IExpr.Call call, final String word) {
        return call.callee() instanceof IExpr.Name name && word.equals(name.identifier())
                && this.scope.scope().lookup(word) == null
                && (this.scope.currentType() == null || BodyScope.lookup(this.scope.currentType(), word).isEmpty());
    }

    /*
     * nameof(x): the last name written, as text known when compiling, so that renaming x and forgetting the text is
     * caught where it was written. What it names has to be there, a local, a member, a type or a namespace, and nothing
     * of it is read or run.
     */
    private ITypeSymbol nameof(final IExpr.Call call) {
        this.scope.declarations().reportIfLater(NAMEOF, NAMEOF_SINCE, call.line(), call.column());
        if (!this.scope.declarations().level().full()) {
            this.scope.report(call.line(), call.column(), SigmaError.NOT_IN_THE_SUBSET, Text.literal(NAMEOF),
                    SubsetRules.NAMEOF_INSTEAD.text());
        }
        final IExpr named = call.arguments().size() == 1 ? call.arguments().getFirst() : null;
        final String written = switch (named) {
            case IExpr.Name name -> name.identifier();
            case IExpr.Member member when chain(member.target()) != null || member.target() instanceof IExpr.This
                    || member.target() instanceof IExpr.Base -> member.name();
            case null, default -> null;
        };
        final ITypeSymbol text = this.scope.builtIns().stringType();
        if (written == null) {
            this.scope.report(call.line(), call.column(), SigmaError.NAMEOF_NEEDS_NAME);
            this.checkArguments(call.arguments());
            return text;
        }
        this.namesSomething(named);
        final IExpr.Literal value = new IExpr.Literal(TokenKind.STRING_LITERAL, written, call.line(), call.column());
        this.scope.model().setType(value, text);
        this.scope.model().setLowered(call, value);
        return text;
    }

    /* Reports what nameof was handed when it names nothing here. */
    private void namesSomething(final IExpr named) {
        final String whole = chain(named);
        final Declarations declarations = this.scope.declarations();
        if (named instanceof IExpr.Name name) {
            if (this.scope.scope().lookup(whole) == null && (this.scope.currentType() == null
                    || BodyScope.lookup(this.scope.currentType(), whole).isEmpty())
                    && declarations.lookup(whole, this.scope.currentType()) == null
                    && !declarations.isNamespace(whole)) {
                declarations.reportUnknown(name.line(), name.column(), whole);
            }
            return;
        }
        final IExpr.Member member = (IExpr.Member) named;
        if (whole != null && (declarations.lookup(whole, this.scope.currentType()) != null
                || declarations.isNamespace(whole))) {
            return;
        }
        final String owner = chain(member.target());
        final String first = owner == null || !owner.contains(".") ? owner : owner.substring(0, owner.indexOf('.'));
        final boolean local = first != null && this.scope.scope().lookup(first) != null;
        final NamedType type = owner == null || local ? null : declarations.lookup(owner, this.scope.currentType());
        if (type != null) {
            this.members.membersOf(type, member.name(), member);
            return;
        }
        if (owner != null && !local && declarations.isNamespace(owner)) {
            declarations.reportUnknown(member.line(), member.column(), whole);
            return;
        }
        final ITypeSymbol target = this.expressions.check(member.target(), null);
        if (!this.scope.rules().isError(target)) {
            this.members.membersOf(target, member.name(), member);
        }
    }

    /* A run of names joined by dots as it was written, or null when something else is in it. */
    private static String chain(final IExpr expression) {
        return switch (expression) {
            case IExpr.Name name -> name.identifier();
            case IExpr.Member member -> {
                final String before = chain(member.target());
                yield before == null ? null : before + "." + member.name();
            }
            case null, default -> null;
        };
    }

    // A class with no constructor of its own can be made with no arguments and nothing else.
    void callConstructor(final NamedType named, final ITypeSymbol type, final List<IExpr> arguments,
                         final INode at) {
        final List<IMemberSymbol.MethodSymbol> candidates = new ArrayList<>();
        boolean hidden = false;
        for (final IMemberSymbol member : named.members()) {
            if (member instanceof IMemberSymbol.ConstructorSymbol constructor) {
                if (this.members.visible(constructor)) {
                    candidates.add(new IMemberSymbol.MethodSymbol(named, named.name(), type,
                            constructor.parameters(), constructor.modifiers()));
                } else {
                    hidden = true;
                }
            }
        }
        if (candidates.isEmpty() && hidden) {
            // Every constructor is private or protected to the type, so it cannot be made from here.
            this.checkArguments(arguments);
            this.scope.report(at.line(), at.column(), SigmaError.NOT_ACCESSIBLE, named.name(),
                    IDecl.Modifier.PRIVATE.text(), named.name());
            return;
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
                    this.scope.rules().substitute(parameter.type(), arguments), parameter.outward(), parameter.many(),
                    parameter.receiver()));
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
        final ITypeSymbol target = this.members.targetOf(member);
        if (this.scope.rules().isError(target)) {
            this.checkArguments(call.arguments());
            return ITypeSymbol.Special.ERROR;
        }
        final BodyScope.Access access = this.scope.model().bindingOf(member.target()) instanceof IBinding.TypeName
                ? BodyScope.Access.TYPE : BodyScope.Access.INSTANCE;
        // An extension method is reached only where the value has no member of that name: one of its own wins.
        final NamedType named = this.scope.rules().named(target);
        if (access == BodyScope.Access.INSTANCE && (named == null || BodyScope.lookup(named,
                target instanceof ITypeSymbol.TupleType tuple ? tuple.fieldOf(member.name()) : member.name())
                .isEmpty())) {
            final List<IMemberSymbol.MethodSymbol> extensions = this.scope.declarations()
                    .extensionsNamed(member.name(), this.scope.currentType()).stream()
                    .filter(this.members::visible).toList();
            if (!extensions.isEmpty()) {
                return this.callExtension(call, member, target, extensions);
            }
        }
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

    /*
     * A call of an extension method: the value it is called on handed over as its first argument, the versions chosen
     * among as a call of the static method written out in full would choose.
     */
    private ITypeSymbol callExtension(final IExpr.Call call, final IExpr.Member member, final ITypeSymbol target,
                                      final List<IMemberSymbol.MethodSymbol> extensions) {
        final List<IExpr> handed = new ArrayList<>(call.arguments().size() + 1);
        handed.add(member.target());
        handed.addAll(call.arguments());
        final IMemberSymbol.MethodSymbol chosen = this.callWith(extensions, handed, target, member.name(), call);
        if (chosen == null) {
            return ITypeSymbol.Special.ERROR;
        }
        this.scope.model().setBinding(member, new IBinding.Member(chosen, chosen.returnType()));
        this.scope.model().setExtension(call, handed);
        return chosen.returnType();
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
                                    final List<IMemberSymbol> all, final String name,
                                    final BodyScope.Access access) {
        // Only the overloads that can be reached from here are chosen among, as C# chooses.
        final List<IMemberSymbol> reachable = all.stream().filter(this.members::visible).toList();
        final List<IMemberSymbol> found = reachable.isEmpty() ? all : reachable;
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
            this.scope.declarations().reportIfNewer(chosen, call.line(), call.column());
        }
        return chosen == null ? ITypeSymbol.Special.ERROR : chosen.returnType();
    }

    /**
     * Picks the version that fits and checks the arguments against it. A lambda has no type until it
     * knows what it is being handed to, so it is left out of the choosing and checked afterwards,
     * against the version that won.
     */
    IMemberSymbol.MethodSymbol callWith(final List<IMemberSymbol.MethodSymbol> candidates,
                                        final List<IExpr> arguments, final String name, final INode at) {
        return this.callWith(candidates, arguments, null, name, at);
    }

    /* The same, with the type of the first argument worked out already when it is not null: an extension's value. */
    private IMemberSymbol.MethodSymbol callWith(final List<IMemberSymbol.MethodSymbol> candidates,
                                                final List<IExpr> arguments, final ITypeSymbol first,
                                                final String name, final INode at) {
        final List<ITypeSymbol> given = new ArrayList<>();
        for (final IExpr argument : arguments) {
            final boolean waits = argument instanceof IExpr.Lambda
                    || ExpressionChecker.waitsForItsParameter(argument) || this.members.isMethodGroup(argument);
            given.add(first != null && given.isEmpty() ? first : waits ? null : this.expressions.check(argument, null));
        }
        final List<IMemberSymbol.MethodSymbol> fitting = this.overloads.best(candidates, given, arguments);
        if (fitting.isEmpty()) {
            return this.reportNoFit(candidates, arguments, given, name, at);
        }
        if (fitting.size() > 1) {
            this.scope.report(at.line(), at.column(), SigmaError.AMBIGUOUS_CALL, name);
        }
        final IMemberSymbol.MethodSymbol chosen = fitting.getFirst();
        this.chose(chosen, arguments, given, this.overloads.expanded(chosen, given, arguments), at);
        return chosen;
    }

    /*
     * The version a call is made to, its arguments checked against it, and whether its params array is made from the
     * values handed over one by one, which the writing has to know.
     */
    private void chose(final IMemberSymbol.MethodSymbol chosen, final List<IExpr> arguments,
                       final List<ITypeSymbol> given, final boolean expanded, final INode at) {
        this.checkAgainst(chosen, arguments, given, expanded);
        this.scope.model().setCall(at, chosen);
        if (expanded) {
            this.scope.model().setExpanded(at);
        }
    }

    /*
     * When only one version could have been meant, saying which argument is wrong beats saying that
     * none of them fit: with a single version there is nothing to choose between. A version taking params could have
     * been meant with any number of values from its fixed parameters on, and is checked that way.
     */
    private IMemberSymbol.MethodSymbol reportNoFit(final List<IMemberSymbol.MethodSymbol> candidates,
                                                   final List<IExpr> arguments, final List<ITypeSymbol> given,
                                                   final String name, final INode at) {
        final List<IMemberSymbol.MethodSymbol> sameCount = new ArrayList<>();
        for (final IMemberSymbol.MethodSymbol candidate : candidates) {
            if (candidate.parameters().size() == given.size()
                    || candidate.takesMany() && given.size() >= candidate.parameters().size() - 1) {
                sameCount.add(candidate);
            }
        }
        if (sameCount.size() == 1) {
            final IMemberSymbol.MethodSymbol only = sameCount.getFirst();
            this.chose(only, arguments, given, only.takesMany(), at);
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

    /*
     * Each argument against its parameter; with the params array made from the values handed over, each of those
     * against the array's element.
     */
    private void checkAgainst(final IMemberSymbol.MethodSymbol chosen, final List<IExpr> arguments,
                              final List<ITypeSymbol> given, final boolean expanded) {
        final int fixed = expanded ? chosen.parameters().size() - 1 : chosen.parameters().size();
        for (int i = 0; i < arguments.size(); i++) {
            final IMemberSymbol.ParameterSymbol parameter = chosen.parameters().get(Math.min(i, fixed));
            final ITypeSymbol wanted = i < fixed ? parameter.type() : Overloads.elementOf(parameter);
            final IExpr argument = arguments.get(i);
            final boolean outward = argument instanceof IExpr.OutArgument;
            if (parameter.outward() != outward) {
                this.scope.report(argument.line(), argument.column(), parameter.outward()
                        ? SigmaError.OUT_ARGUMENT_EXPECTED : SigmaError.OUT_ARGUMENT_UNEXPECTED,
                        parameter.name());
                continue;
            }
            if (given.get(i) == null) {
                this.expressions.check(argument, wanted);
            } else if (outward) {
                if (!given.get(i).equals(wanted)) {
                    this.scope.report(argument.line(), argument.column(),
                            SigmaError.OUT_TYPE_MUST_MATCH, wanted.describe());
                }
            } else {
                this.scope.expect(given.get(i), wanted, argument);
            }
        }
    }
}
