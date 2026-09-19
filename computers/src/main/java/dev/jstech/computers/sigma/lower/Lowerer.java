/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.sigma.lower;

import dev.jstech.computers.sigma.DiagnosticBag;
import dev.jstech.computers.sigma.ast.CompilationUnit;
import dev.jstech.computers.sigma.ast.IDecl;
import dev.jstech.computers.sigma.ast.IExpr;
import dev.jstech.computers.sigma.ast.INode;
import dev.jstech.computers.sigma.ast.IStmt;
import dev.jstech.computers.sigma.ast.Operator;
import dev.jstech.computers.sigma.lex.TokenKind;
import dev.jstech.computers.sigma.sem.BuiltIns;
import dev.jstech.computers.sigma.sem.IBinding;
import dev.jstech.computers.sigma.sem.IMemberSymbol;
import dev.jstech.computers.sigma.sem.ITypeSymbol;
import dev.jstech.computers.sigma.sem.NamedType;
import dev.jstech.computers.sigma.sem.TypeRules;
import dev.jstech.computers.sigma.sem.SemanticModel;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The stage between knowing what a program means and writing it down: the shapes a player writes turned into
 * the fewer shapes a machine is given.
 *
 * <p>It runs after the checking and before the writing, and that order is the whole of why it exists. Before
 * the checking, a tree reduced is a tree that can no longer be asked what was written in it, and questions are
 * still being asked of it: what language a source is allowed to be turns on telling a string with holes apart
 * from the additions somebody typed, and once it is additions there is nothing left to tell. After the
 * checking, nobody asks any more, and the writing is easier for having fewer shapes to know.
 *
 * <p>Nothing it produces is new to the language. Every shape it makes is one a player could have written by
 * hand, which is what lets it leave the meaning of a program exactly as the checker found it, and what lets the
 * listing come out the same to the byte.
 */
public final class Lowerer {

    private final SemanticModel model;
    /*
     * The stage after this one asks what everything's type is, and the shapes made here were never put to the
     * checker, so they are given their types as they are made. A shape handed on without one reads as having no
     * type at all, which is not an error anywhere and comes out as a program quietly doing the wrong thing.
     */
    private final BuiltIns builtIns;
    private final TypeRules rules;
    /** What a method's lambdas keep hold of, which is a question about the tree and is answered here. */
    private final CaptureFinder finder;
    /** How many locks are held where the statement being reduced was written. */
    private final Deque<Integer> loopLocks = new ArrayDeque<>();
    /**
     * What each method's statements came to, by the declaration they were written in.
     *
     * <p>Kept here rather than in the model beside the types and the bindings, because it belongs to this stage
     * and the model belongs to the one before it: the checker has no business knowing what its answers were
     * later reduced to.
     */
    private final Map<Object, IrStmt> bodies = new IdentityHashMap<>();
    /** What each method's lambdas keep, by the declaration they were written in. */
    private final Map<Object, Captures> captures = new IdentityHashMap<>();
    /** What each assignment and each step came to, by the expression the player wrote. */
    private final Map<IExpr, IrWrite> writes = new IdentityHashMap<>();
    /**
     * The values that are copied when they are stored or handed over, rather than shared.
     *
     * <p>A struct is a value: two names for one are two of it, so handing one over hands over a copy. The one
     * exception is a struct fresh from new, which nobody else is holding yet and so has nothing to be copied
     * away from. Both of those are questions about types and about what was written, which is why the answer
     * is kept here rather than asked again at every line that stores something.
     */
    private final Set<IExpr> copied = Collections.newSetFromMap(new IdentityHashMap<>());

    private int locksOpen;
    /** What the lambdas of the method being walked keep, which is what makes a variable not a slot. */
    private Captures kept;

    public Lowerer(final SemanticModel model, final BuiltIns builtIns, final TypeRules rules,
                   final DiagnosticBag diagnostics) {
        this.model = model;
        this.builtIns = builtIns;
        this.rules = rules;
        this.finder = new CaptureFinder(model, diagnostics);
    }

    /**
     * A method's statements, with the shorthand gone.
     *
     * <p>Holding a lock disappears into what it was always shorthand for: the object put somewhere of its own,
     * the lock taken, and the lock let go again on every way out. How many locks a way out lets go of is
     * counted here, because it is a fact about where the way out was written and not about stacks.
     */
    private IrStmt body(final IStmt.Block block) {
        this.locksOpen = 0;
        this.loopLocks.clear();
        return this.statements(block);
    }

    private IrStmt statements(final IStmt.Block block) {
        final List<IrStmt> made = new ArrayList<>();
        if (block != null) {
            for (final IStmt statement : block.statements()) {
                made.add(this.reduce(statement));
            }
        }
        return new IrStmt.Block(made);
    }

    /*
     * A statement holding no other statement is handed on as it was written, since there is nothing inside it
     * left to reduce; one holding others never is, or what it holds would be hidden from here.
     */
    private IrStmt reduce(final IStmt statement) {
        return switch (statement) {
            case null -> new IrStmt.Block(List.of());
            case IStmt.Block block -> this.statements(block);
            case IStmt.If branch -> new IrStmt.If(branch.condition(), this.reduce(branch.then()),
                    branch.otherwise() == null ? null : this.reduce(branch.otherwise()));
            case IStmt.While loop -> new IrStmt.While(loop.condition(), this.inLoop(loop.body()));
            case IStmt.DoWhile loop -> new IrStmt.DoWhile(loop.condition(), this.inLoop(loop.body()));
            case IStmt.For loop -> new IrStmt.For(this.each(loop.initializers()), loop.condition(),
                    loop.updates(), this.inLoop(loop.body()));
            case IStmt.ForEach loop -> this.walked(loop);
            case IStmt.Switch chosen -> this.chosen(chosen);
            case IStmt.Lock held -> this.held(held);
            case IStmt.Break ignored -> new IrStmt.Break(false, this.leavingTheLoop());
            case IStmt.Continue ignored -> new IrStmt.Break(true, this.leavingTheLoop());
            case IStmt.Return give -> new IrStmt.Return(give.value(), this.locksOpen);
            default -> new IrStmt.Source(statement);
        };
    }

    private List<IrStmt> each(final List<IStmt> statements) {
        final List<IrStmt> made = new ArrayList<>(statements.size());
        for (final IStmt statement : statements) {
            made.add(this.reduce(statement));
        }
        return made;
    }

    private IrStmt chosen(final IStmt.Switch choice) {
        final List<IrStmt.Switch.Section> sections = new ArrayList<>(choice.sections().size());
        for (final IStmt.SwitchSection section : choice.sections()) {
            sections.add(new IrStmt.Switch.Section(section.labels(), this.each(section.statements()),
                    section.fallback()));
        }
        return new IrStmt.Switch(choice.value(), sections);
    }

    /** Holding a lock, said as the three things it has always been. */
    private IrStmt held(final IStmt.Lock lock) {
        final IrStmt.Temporary place = new IrStmt.Temporary();
        this.locksOpen++;
        final IrStmt body = this.reduce(lock.body());
        this.locksOpen--;
        return new IrStmt.Block(List.of(
                new IrStmt.Keep(place, lock.target(), true),
                new IrStmt.MonitorEnter(place),
                body,
                new IrStmt.MonitorExit(place)));
    }

    /** A loop body, remembering how many locks were held when the loop began. */
    private IrStmt inLoop(final IStmt body) {
        this.loopLocks.push(this.locksOpen);
        final IrStmt made = this.reduce(body);
        this.loopLocks.pop();
        return made;
    }

    /** How many locks a break or a continue lets go of: the ones taken inside the loop it is leaving. */
    private int leavingTheLoop() {
        return this.loopLocks.isEmpty() ? 0 : this.locksOpen - this.loopLocks.peek();
    }

    /**
     * A walk over a collection, with what the walking has to ask it already worked out.
     *
     * <p>An array answers to a length of its own and hands over a place by number; anything else answers to a
     * count and is asked for each element by name. Both are questions about the type being walked, so they are
     * answered here rather than again at the line that writes the loop.
     */
    private IrStmt walked(final IStmt.ForEach loop) {
        final ITypeSymbol source = this.model.typeOf(loop.source());
        final boolean overAnArray = source instanceof ITypeSymbol.ArrayType;
        final ITypeSymbol held = overAnArray ? null : this.rules.elementOf(source);
        return new IrStmt.ForEach(loop.source(), overAnArray,
                overAnArray ? null : this.builtIns.listType().name(),
                overAnArray || held == null ? "object" : held.describe(),
                this.model.declaredAt(loop), this.copiesEachTurn(loop), this.inLoop(loop.body()));
    }

    /** Whether each turn of a walk takes its own copy, which it does when what it finds is a value. */
    private boolean copiesEachTurn(final IStmt.ForEach loop) {
        final IBinding.Variable walker = this.model.declaredAt(loop);
        return walker != null && this.rules.named(walker.type()) != null
                && this.rules.named(walker.type()).kind() == NamedType.Kind.STRUCT;
    }

    /** What each method's statements came to, for the stage that writes them down. */
    public IrStmt bodyOf(final Object written) {
        return this.bodies.get(written);
    }

    /** What a method's lambdas keep hold of, or nothing when none of them keeps anything. */
    public Captures capturesOf(final Object written) {
        return this.captures.get(written);
    }

    /** What each assignment and each step came to, for the stage that writes them down. */
    public IrWrite writeOf(final IExpr expression) {
        return this.writes.get(expression);
    }

    /** Whether storing or handing over what this expression comes to takes a copy of it. */
    public boolean copies(final IExpr expression) {
        return this.copied.contains(expression);
    }

    /** Asks what a method's lambdas keep, for the bodies that have one to ask about. */
    private Captures keptBy(final INode written, final List<IDecl.Parameter> parameters,
                            final IStmt.Block body) {
        if (body == null) {
            return null;
        }
        final Captures found = this.finder.forMethod(parameters, body, written);
        if (found != null) {
            this.captures.put(written, found);
        }
        return found;
    }

    /**
     * Where an expression says a value lives, or nothing when it names no place at all.
     *
     * <p>Read by everything that writes: plain assignment, assignment that combines, and one more or one
     * less. An event is not a place; joining and parting handlers is something else entirely and says so by
     * not being here.
     */
    private Place placeOf(final IExpr target) {
        final IBinding binding = this.model.bindingOf(target);
        if (binding instanceof IBinding.Variable variable) {
            return this.kept != null && this.kept.fields().containsKey(variable)
                    ? new Place.Captured(variable) : new Place.Local(variable);
        }
        if (target instanceof IExpr.Index index) {
            return new Place.Element(index);
        }
        if (!(binding instanceof IBinding.Member held)) {
            return null;
        }
        final IMemberSymbol member = held.member();
        if (!(member instanceof IMemberSymbol.FieldSymbol)
                && !(member instanceof IMemberSymbol.PropertySymbol)) {
            return null;
        }
        return member.isStatic() ? new Place.Shared(member)
                : new Place.Held(target instanceof IExpr.Member at ? at.target() : null, member);
    }

    /** An assignment said as a place, a value and which value the line is worth. */
    private void written(final IExpr.Assign assign) {
        final Place place = this.placeOf(assign.target());
        if (place == null) {
            return;
        }
        this.writes.put(assign, new IrWrite(place, assign.operator(), assign.value(),
                this.model.typeOf(assign.target()), IrWrite.Answer.WRITTEN));
    }

    /**
     * One more or one less said as the combining assignment it is.
     *
     * <p>The one is made as a number of the kind the place holds, so that nothing has to be converted on the
     * way and the line comes out as short as it was when this shape had a path of its own.
     */
    private void stepped(final IExpr.Unary step) {
        final Place place = this.placeOf(step.operand());
        if (place == null) {
            return;
        }
        final ITypeSymbol type = this.model.typeOf(step.operand());
        this.writes.put(step, new IrWrite(place,
                step.operator() == Operator.INCREMENT ? Operator.ADD : Operator.SUBTRACT,
                this.one(type, step), type,
                step.postfix() ? IrWrite.Answer.BEFORE : IrWrite.Answer.AFTER));
    }

    /** The number one, written as the kind of number that place holds, and typed as it is made. */
    private IExpr one(final ITypeSymbol type, final IExpr at) {
        final IExpr made;
        if (type == ITypeSymbol.Primitive.LONG) {
            made = new IExpr.Literal(TokenKind.LONG_LITERAL, 1L, at.line(), at.column());
        } else if (type == ITypeSymbol.Primitive.FLOAT) {
            made = new IExpr.Literal(TokenKind.FLOAT_LITERAL, 1.0F, at.line(), at.column());
        } else if (type == ITypeSymbol.Primitive.DOUBLE) {
            made = new IExpr.Literal(TokenKind.DOUBLE_LITERAL, 1.0D, at.line(), at.column());
        } else {
            made = new IExpr.Literal(TokenKind.INT_LITERAL, 1, at.line(), at.column());
        }
        this.model.setType(made, type == ITypeSymbol.Primitive.LONG || type == ITypeSymbol.Primitive.FLOAT
                || type == ITypeSymbol.Primitive.DOUBLE ? type : ITypeSymbol.Primitive.INT);
        return made;
    }

    /** Reduces every body of every type in these files. */
    public void lower(final List<CompilationUnit> units) {
        for (final CompilationUnit unit : units) {
            for (final IDecl.ITypeDecl type : unit.types()) {
                this.type(type);
            }
        }
    }

    private void type(final IDecl.ITypeDecl declaration) {
        if (!(declaration instanceof IDecl.ClassDecl klass)) {
            return;
        }
        for (final IDecl.IMemberDecl member : klass.members()) {
            switch (member) {
                /*
                 * What the lambdas keep is asked first, because walking the body works out where each value
                 * goes and a variable a lambda keeps does not go in a slot of the method's own.
                 */
                case IDecl.MethodDecl method -> {
                    this.kept = this.keptBy(method, method.parameters(), method.body());
                    this.block(method.body());
                    this.bodies.put(method, this.body(method.body()));
                }
                case IDecl.ConstructorDecl constructor -> {
                    this.kept = this.keptBy(constructor, constructor.parameters(), constructor.body());
                    this.block(constructor.body());
                    this.bodies.put(constructor, this.body(constructor.body()));
                }
                case IDecl.FieldDecl field -> {
                    this.kept = null; // a field's starting value is not inside any method, so it keeps nothing
                    this.replaceIn(field.initializer());
                }
                case IDecl.TypeMember nested -> this.type(nested.type());
                default -> { }
            }
        }
    }

    private void block(final IStmt.Block block) {
        if (block != null) {
            for (final IStmt statement : block.statements()) {
                this.statement(statement);
            }
        }
    }

    /*
     * The tree is walked rather than rebuilt, because what is reduced is reached through the expression that
     * holds it: the holder is asked to put the simpler shape in place of the written one. A statement that
     * holds nothing to reduce is walked into all the same, since what it holds might.
     */
    private void statement(final IStmt statement) {
        switch (statement) {
            case null -> { }
            case IStmt.Block block -> this.block(block);
            case IStmt.LocalDecl local -> this.replaceIn(local.initializer());
            case IStmt.ExprStmt expression -> this.replaceIn(expression.expression());
            case IStmt.Return give -> this.replaceIn(give.value());
            case IStmt.Dispose thrown -> this.replaceIn(thrown.target());
            case IStmt.If branch -> {
                this.replaceIn(branch.condition());
                this.statement(branch.then());
                this.statement(branch.otherwise());
            }
            case IStmt.While loop -> {
                this.replaceIn(loop.condition());
                this.statement(loop.body());
            }
            case IStmt.DoWhile loop -> {
                this.replaceIn(loop.condition());
                this.statement(loop.body());
            }
            case IStmt.For loop -> {
                loop.initializers().forEach(this::statement);
                this.replaceIn(loop.condition());
                loop.updates().forEach(this::replaceIn);
                this.statement(loop.body());
            }
            case IStmt.ForEach loop -> {
                this.replaceIn(loop.source());
                this.statement(loop.body());
            }
            case IStmt.Lock held -> {
                this.replaceIn(held.target());
                this.statement(held.body());
            }
            case IStmt.Switch chosen -> {
                this.replaceIn(chosen.value());
                for (final IStmt.SwitchSection section : chosen.sections()) {
                    section.labels().forEach(this::replaceIn);
                    section.statements().forEach(this::statement);
                }
            }
            default -> { }
        }
    }

    /** Walks an expression, putting the simpler shape in place of anything written in a richer one. */
    private void replaceIn(final IExpr expression) {
        switch (expression) {
            case null -> { }
            case IExpr.Unary unary -> {
                this.replaceIn(unary.operand());
                if (unary.operator() == Operator.INCREMENT || unary.operator() == Operator.DECREMENT) {
                    this.stepped(unary);
                }
            }
            case IExpr.Binary binary -> {
                this.replaceIn(binary.left());
                this.replaceIn(binary.right());
            }
            case IExpr.Assign assign -> {
                this.replaceIn(assign.target());
                this.replaceIn(assign.value());
                this.written(assign);
            }
            case IExpr.Conditional chosen -> {
                this.replaceIn(chosen.condition());
                this.replaceIn(chosen.whenTrue());
                this.replaceIn(chosen.whenFalse());
            }
            case IExpr.Call call -> {
                this.replaceIn(call.callee());
                call.arguments().forEach(this::replaceIn);
            }
            case IExpr.Member member -> this.replaceIn(member.target());
            case IExpr.Index index -> {
                this.replaceIn(index.target());
                this.replaceIn(index.index());
            }
            case IExpr.New made -> made.arguments().forEach(this::replaceIn);
            case IExpr.NewArray made -> this.replaceIn(made.length());
            case IExpr.Cast cast -> this.replaceIn(cast.value());
            case IExpr.TypeTest test -> this.replaceIn(test.value());
            case IExpr.Lambda lambda -> {
                this.replaceIn(lambda.body());
                this.block(lambda.block());
                if (lambda.block() != null) {
                    this.bodies.put(lambda, this.body(lambda.block()));
                }
            }
            case IExpr.Interpolation written -> holesOf(written).forEach(this::replaceIn);
            default -> { }
        }
        this.model.setLowered(expression, this.lowered(expression));
        if (!(expression instanceof IExpr.New) && this.isStruct(this.model.typeOf(expression))) {
            this.copied.add(expression);
        }
    }

    /** Whether a value of that type is a struct, which is copied whenever it is stored or handed over. */
    private boolean isStruct(final ITypeSymbol type) {
        return this.rules.named(type) instanceof NamedType named
                && named.kind() == NamedType.Kind.STRUCT;
    }

    /**
     * The simpler shape of one expression, or nothing when it is already as simple as it goes.
     *
     * <p>A string with holes becomes the pieces added together, left to right, each piece already reduced. An
     * empty one is the empty string; one that starts with a hole starts from the empty string, so that the
     * first addition is text joining text rather than whatever the hole turned out to hold.
     */
    private IExpr lowered(final IExpr expression) {
        if (expression instanceof IExpr.Call call && this.model.formattedOf(call) != null) {
            return this.printed(call);
        }
        if (!(expression instanceof IExpr.Interpolation written)) {
            return null;
        }
        return this.joined(written.parts(), written);
    }

    /**
     * A call of printf as what it comes down to: its pieces joined, handed to the console's own call.
     *
     * <p>The format was read when the call was checked, so nothing of it is left to read while the program
     * runs, and the line written down is the one joining the pieces by hand would have given.
     */
    private IExpr printed(final IExpr.Call call) {
        final IExpr made = new IExpr.Call(call.callee(), List.of(this.joined(this.model.formattedOf(call), call)),
                call.line(), call.column());
        this.model.setType(made, ITypeSymbol.Primitive.VOID);
        this.model.setCall(made, this.model.callOf(call));
        return made;
    }

    /** Pieces of text and values added together, left to right, each value already reduced. */
    private IExpr joined(final List<Object> parts, final IExpr at) {
        IExpr sum = null;
        for (final Object part : parts) {
            final IExpr piece = part instanceof IExpr inside ? this.reduced(inside) : this.text(part, at);
            if (sum == null) {
                sum = part instanceof String ? piece : this.joins(this.text("", at), piece, at);
            } else {
                sum = this.joins(sum, piece, at);
            }
        }
        return sum == null ? this.text("", at) : sum;
    }

    /** A piece of text, typed as it is made, placed where the expression it came of was written. */
    private IExpr text(final Object value, final IExpr at) {
        final IExpr made = new IExpr.Literal(TokenKind.STRING_LITERAL, value, at.line(), at.column());
        this.model.setType(made, this.builtIns.stringType());
        return made;
    }

    /** Two pieces joined, typed as it is made: joining anything to text gives text. */
    private IExpr joins(final IExpr left, final IExpr right, final IExpr at) {
        final IExpr made = new IExpr.Binary(Operator.ADD, left, right, at.line(), at.column());
        this.model.setType(made, this.builtIns.stringType());
        return made;
    }

    /** What an expression was reduced to, or itself when it was already simple enough. */
    private IExpr reduced(final IExpr expression) {
        final IExpr simpler = this.model.loweredOf(expression);
        return simpler == null ? expression : simpler;
    }

    /** The pieces of a string with holes, for a stage that has to look at them before they are added up. */
    public static List<IExpr> holesOf(final IExpr.Interpolation written) {
        final List<IExpr> inside = new ArrayList<>();
        for (final Object part : written.parts()) {
            if (part instanceof IExpr hole) {
                inside.add(hole);
            }
        }
        return inside;
    }
}
