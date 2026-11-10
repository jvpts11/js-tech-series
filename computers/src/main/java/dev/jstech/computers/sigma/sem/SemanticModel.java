/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.sigma.sem;

import dev.jstech.computers.sigma.ast.IExpr;
import dev.jstech.computers.sigma.ast.INode;
import dev.jstech.computers.sigma.ast.IStmt;
import dev.jstech.computers.sigma.ast.Operator;
import dev.jstech.computers.vm.listing.Shape;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * What the checker learned: the types a program declares, and what every expression in it turned out
 * to be.
 *
 * <p>The tree stays exactly as it was parsed and the answers are kept beside it, keyed by the node
 * itself. The stage that emits the assembly walks the same tree and asks this model what each node
 * meant, so nothing has to be worked out twice and the tree never has to be rebuilt.
 */
public final class SemanticModel {

    private final Map<IExpr, ITypeSymbol> types = new IdentityHashMap<>();
    private final Map<IExpr, IBinding> bindings = new IdentityHashMap<>();
    private final Map<INode, IMemberSymbol> calls = new IdentityHashMap<>();
    private final Map<INode, IBinding.Variable> places = new IdentityHashMap<>();
    /**
     * What each expression was reduced to before being written down, where it was reduced at all.
     *
     * <p>It is kept beside the tree rather than put into it, so the tree stays what the player wrote: a
     * message still points at the string they typed and not at the additions it turned into.
     */
    private final Map<IExpr, IExpr> lowered = new IdentityHashMap<>();
    /**
     * What each call of {@code printf} prints, or of {@code sprintf} gives back: the text of its format and the
     * values that go in its holes, in the order they are joined, each a {@code String} or the {@link IExpr} of a
     * value.
     */
    private final Map<IExpr.Call, List<Object>> formatted = new IdentityHashMap<>();
    /**
     * What each call of an old bare function stands for: the call or the value of the library a player would have
     * written the long way, checked and typed, over the very values the player handed the short one.
     */
    private final Map<IExpr.Call, IExpr> longWays = new IdentityHashMap<>();
    /** Where each goto and each comefrom goes, and the statement it lands inside, which it may not enter. */
    private final Map<INode, Jump> jumps = new IdentityHashMap<>();
    /** The comefrom that takes each labelled statement's way out, and the regions that keep it from firing. */
    private final Map<IStmt.Labeled, IStmt.ComeFrom> comeFroms = new IdentityHashMap<>();
    private final Map<IStmt.ComeFrom, List<String>> guards = new IdentityHashMap<>();
    /** The labelled statements an abstainfrom or a reinstate names, whose regions can be switched off. */
    private final Set<IStmt.Labeled> abstainable = Collections.newSetFromMap(new IdentityHashMap<>());
    /** The calls whose params array is made from the values handed over one by one. */
    private final Set<INode> expanded = Collections.newSetFromMap(new IdentityHashMap<>());
    /** Each call of an extension method, with what the static method is handed: the value it was called on first. */
    private final Map<IExpr.Call, List<IExpr>> extensions = new IdentityHashMap<>();
    /** The method of an indexer each written index writes with; the one it reads with is its call. */
    private final Map<IExpr.Index, IMemberSymbol.MethodSymbol> setters = new IdentityHashMap<>();
    /** The type each catch takes, Exception for one written without a type. */
    private final Map<IStmt.Catch, ITypeSymbol> caught = new IdentityHashMap<>();
    /** The catch each throw that throws on stands in, whose exception it throws on. */
    private final Map<IStmt.Throw, IStmt.Catch> rethrows = new IdentityHashMap<>();
    /** Whether the program uses the language's exceptions at all, which their types then go out with. */
    private boolean usesExceptions;
    /** The variables that are local functions, which nothing writes to and which are made where their block starts. */
    private final Set<IBinding.Variable> localFunctions = Collections.newSetFromMap(new IdentityHashMap<>());
    /** The locals and parameters whose address a program may take. */
    private final Set<IBinding.Variable> addressable = Collections.newSetFromMap(new IdentityHashMap<>());
    /** The ones whose address it took, which live in raw memory. */
    private final Set<IBinding.Variable> inMemory = Collections.newSetFromMap(new IdentityHashMap<>());
    private final List<NamedType> declared = new ArrayList<>();
    private final List<DeclaredVariable> variables = new ArrayList<>();
    private String file = "";
    private NamedType entryPoint;
    private Shape shape = Shape.SCRIPT;

    /**
     * One variable and where it came into being, for an editor asking what a name at the caret could
     * be: the file and the line it was declared on, and the variable itself.
     */
    public record DeclaredVariable(String file, int line, IBinding.Variable variable) {
    }

    /**
     * Where a goto or a comefrom goes: the labelled statement or the switch's section it lands at, and the statement
     * that holds the place it lands, which every jump to it is already inside.
     */
    public record Jump(INode target, INode container) {
    }

    /** Says which file the bodies being checked are in, so a variable is recorded with it. */
    public void setFile(final String value) {
        this.file = value == null ? "" : value;
    }

    /** Every variable the program declares, in the order the checker met them. */
    public List<DeclaredVariable> variables() {
        return List.copyOf(this.variables);
    }

    /** Records what an expression's type is. */
    public void setType(final IExpr expression, final ITypeSymbol type) {
        this.types.put(expression, type == null ? ITypeSymbol.Special.ERROR : type);
    }

    /** The type of an expression, or null if it was never checked. */
    public ITypeSymbol typeOf(final IExpr expression) {
        return this.types.get(expression);
    }

    /** Records the simpler shape an expression is written down as; nothing means it is written as it stands. */
    public void setLowered(final IExpr expression, final IExpr simpler) {
        if (simpler != null) {
            this.lowered.put(expression, simpler);
        }
    }

    /** The simpler shape an expression is written down as, or null when it is written as it stands. */
    public IExpr loweredOf(final IExpr expression) {
        return this.lowered.get(expression);
    }

    /** Records what a formatting call comes to, once its format has been read against its values. */
    public void setFormatted(final IExpr.Call call, final List<Object> pieces) {
        this.formatted.put(call, List.copyOf(pieces));
    }

    /** What a formatting call comes to, or null for a call that is not one. */
    public List<Object> formattedOf(final IExpr.Call call) {
        return this.formatted.get(call);
    }

    /** Records what a call of an old bare function stands for, written the long way. */
    public void setLongWay(final IExpr.Call call, final IExpr longWay) {
        this.longWays.put(call, longWay);
    }

    /** What a call of an old bare function stands for, written the long way, or null for a call that is not one. */
    public IExpr longWayOf(final IExpr.Call call) {
        return this.longWays.get(call);
    }

    /** Records what a name turned out to be. */
    public void setBinding(final IExpr expression, final IBinding binding) {
        this.bindings.put(expression, binding);
    }

    /** What a name turned out to be, or null. */
    public IBinding bindingOf(final IExpr expression) {
        return this.bindings.get(expression);
    }

    /** Records which method or constructor a call resolved to. */
    public void setCall(final INode expression, final IMemberSymbol member) {
        this.calls.put(expression, member);
    }

    /** The method or constructor a call resolved to, or null. */
    public IMemberSymbol callOf(final INode expression) {
        return this.calls.get(expression);
    }

    /** Records that a call hands its params array over as the values written one by one. */
    public void setExpanded(final INode call) {
        this.expanded.add(call);
    }

    /** Whether a call hands its params array over as the values written one by one, which make the array. */
    public boolean expanded(final INode call) {
        return this.expanded.contains(call);
    }

    /** Records that a call is of an extension method, handing its static method those values. */
    public void setExtension(final IExpr.Call call, final List<IExpr> handed) {
        this.extensions.put(call, List.copyOf(handed));
    }

    /** What a call of an extension method hands its static method, the value it was called on first, or null. */
    public List<IExpr> extensionOf(final IExpr.Call call) {
        return this.extensions.get(call);
    }

    /** Records the type a catch takes. */
    public void setCaught(final IStmt.Catch one, final ITypeSymbol type) {
        this.caught.put(one, type);
    }

    /** The type a catch takes, or null for one never checked. */
    public ITypeSymbol caughtBy(final IStmt.Catch one) {
        return this.caught.get(one);
    }

    /** Records the catch a throw that throws on stands in. */
    public void setRethrows(final IStmt.Throw thrown, final IStmt.Catch around) {
        this.rethrows.put(thrown, around);
    }

    /** The catch a throw that throws on stands in, or null for one that throws something of its own. */
    public IStmt.Catch rethrowsFrom(final IStmt.Throw thrown) {
        return this.rethrows.get(thrown);
    }

    /** Says the program uses the language's exceptions: a try, a throw, or one of their types named. */
    public void setUsesExceptions() {
        this.usesExceptions = true;
    }

    /** Whether the program uses the language's exceptions, which their types and where it throws then go out with. */
    public boolean usesExceptions() {
        return this.usesExceptions;
    }

    /** Records the method of an indexer a written index writes with. */
    public void setSetter(final IExpr.Index index, final IMemberSymbol.MethodSymbol setter) {
        this.setters.put(index, setter);
    }

    /** The method of an indexer a written index writes with, or null for an index with no indexer behind it. */
    public IMemberSymbol.MethodSymbol setterOf(final IExpr.Index index) {
        return this.setters.get(index);
    }

    /** Records that a variable is a local function. */
    public void setLocalFunction(final IBinding.Variable variable) {
        this.localFunctions.add(variable);
    }

    /** Whether a variable is a local function, which holds its lambda and is never written to. */
    public boolean isLocalFunction(final IBinding.Variable variable) {
        return this.localFunctions.contains(variable);
    }

    /** Records that a variable's address may be taken: a local declared as one, or a parameter a call hands a value. */
    public void setAddressable(final IBinding.Variable variable) {
        this.addressable.add(variable);
    }

    /** Whether a variable's address may be taken. */
    public boolean isAddressable(final IBinding.Variable variable) {
        return this.addressable.contains(variable);
    }

    /** Records that a variable's address was taken, so it lives in raw memory for as long as its call runs. */
    public void setInMemory(final IBinding.Variable variable) {
        this.inMemory.add(variable);
    }

    /** Whether a variable lives in raw memory, read and written through its address. */
    public boolean isInMemory(final IBinding.Variable variable) {
        return this.inMemory.contains(variable);
    }

    /**
     * Whether an expression already checked names a place in raw memory, read and written through an address: what a
     * pointer reaches ({@code *p}, {@code p[i]}, {@code p->F}), a variable whose address was taken, or a field of a
     * struct lying in either.
     */
    public boolean inRawMemory(final IExpr expression) {
        return switch (expression) {
            case IExpr.Unary unary -> unary.operator() == Operator.DEREFERENCE;
            case IExpr.Index index -> this.typeOf(index.target()) instanceof ITypeSymbol.PointerType;
            case IExpr.Name name -> this.bindingOf(name) instanceof IBinding.Variable variable
                    && this.inMemory.contains(variable);
            case IExpr.Member member -> this.bindingOf(member) instanceof IBinding.Member held
                    && !held.member().isStatic() && (held.member() instanceof IMemberSymbol.FieldSymbol
                    || held.member() instanceof IMemberSymbol.PropertySymbol) && this.inRawMemory(member.target());
            case null, default -> false;
        };
    }

    /**
     * Records where a variable comes into being: a parameter, a local, the name a foreach walks with,
     * or a variable declared at a call that fills it in.
     *
     * <p>The stage that emits the assembly needs this to number the places a method keeps its values
     * in. Every mention of the name binds to the same variable this records, so numbering the
     * declarations is enough to number every use.
     */
    public void setDeclared(final INode site, final IBinding.Variable variable) {
        this.places.put(site, variable);
        this.variables.add(new DeclaredVariable(this.file, site.line(), variable));
    }

    /** The variable a declaration site brings into being, or null. */
    public IBinding.Variable declaredAt(final INode site) {
        return this.places.get(site);
    }

    /** Records a type the program declares. */
    public void addDeclared(final NamedType type) {
        this.declared.add(type);
    }

    /** The types the program declares, in the order they were written. */
    public List<NamedType> declaredTypes() {
        return List.copyOf(this.declared);
    }

    /** The declared type of that name, or null. */
    public NamedType declaredType(final String name) {
        for (final NamedType type : this.declared) {
            if (type.qualifiedName().equals(name)) {
                return type;
            }
        }
        for (final NamedType type : this.declared) {
            if (type.name().equals(name)) {
                return type;
            }
        }
        return null;
    }

    /** The class the runtime starts the program from, or null when there is not exactly one. */
    public NamedType entryPoint() {
        return this.entryPoint;
    }

    /** Whether that class is a program that runs at a terminal or one that stays up. */
    public Shape shape() {
        return this.shape;
    }

    /** Records the class the runtime starts from, and which kind of program it makes. */
    public void setEntryPoint(final NamedType entryPoint, final Shape shape) {
        this.entryPoint = entryPoint;
        this.shape = shape;
    }

    /** Records where a goto or a comefrom goes. */
    public void setJump(final INode jump, final Jump to) {
        this.jumps.put(jump, to);
    }

    /** Where a goto or a comefrom goes, or null when it goes nowhere the checker found. */
    public Jump jumpOf(final INode jump) {
        return this.jumps.get(jump);
    }

    /**
     * Records the comefrom that takes a labelled statement's way out, and the regions it stands in, any of which,
     * switched off, keeps it from firing.
     */
    public void setComeFrom(final IStmt.Labeled labeled, final IStmt.ComeFrom from, final List<String> regions) {
        this.comeFroms.put(labeled, from);
        this.guards.put(from, List.copyOf(regions));
    }

    /** The comefrom that takes a labelled statement's way out, or null when none does. */
    public IStmt.ComeFrom comeFromOf(final IStmt.Labeled labeled) {
        return this.comeFroms.get(labeled);
    }

    /** The regions a comefrom stands in, by their labels. */
    public List<String> guardsOf(final IStmt.ComeFrom from) {
        return this.guards.getOrDefault(from, List.of());
    }

    /** Records that an abstainfrom or a reinstate names a labelled statement, whose region can then be switched off. */
    public void setAbstainable(final IStmt.Labeled labeled) {
        this.abstainable.add(labeled);
    }

    /** Whether a labelled statement's region can be switched off. */
    public boolean abstainable(final IStmt.Labeled labeled) {
        return this.abstainable.contains(labeled);
    }
}
