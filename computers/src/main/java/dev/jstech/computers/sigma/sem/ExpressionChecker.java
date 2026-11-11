/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.sigma.sem;

import dev.jstech.computers.sigma.LanguageLevel;
import dev.jstech.computers.sigma.SigmaError;
import dev.jstech.computers.sigma.ast.IDecl;
import dev.jstech.computers.sigma.ast.IExpr;
import dev.jstech.computers.sigma.ast.INode;
import dev.jstech.computers.sigma.ast.IStmt;
import dev.jstech.computers.sigma.ast.Operator;
import dev.jstech.computers.sigma.lex.TokenKind;
import dev.jstech.computers.sigma.lower.Lowerer;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

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
            case IExpr.Tuple tuple -> this.tupleType(tuple, expected);
            case IExpr.StackAlloc created -> this.stackAllocType(created);
            case IExpr.SizeOf sized -> this.sizeOf(this.scope.declarations().resolve(sized.type(),
                    this.scope.currentType()), sized);
        };
        if (type instanceof ITypeSymbol.PointerType) {
            // A call that answers with a pointer hands one over, which only unsafe code may hold.
            if (expression instanceof IExpr.Call) {
                this.needsUnsafe(expression);
            }
            // A read outside memory is an AccessViolationException in Sigma#, so the exceptions go out with it.
            if (this.scope.declarations().level() != LanguageLevel.SIGMA) {
                this.scope.model().setUsesExceptions();
            }
        }
        this.scope.model().setType(expression, type);
        if (expression instanceof IExpr.Name || expression instanceof IExpr.Member) {
            // A const read is written as its value.
            this.scope.constants().replaceRead(expression);
        }
        return type;
    }

    /**
     * Whether what a binding names is a const or a local function, which nothing writes to; it is reported at that line
     * and column when it is, as {@code name}.
     */
    boolean refusesWrite(final IBinding binding, final String name, final int line, final int column) {
        if (binding instanceof IBinding.Variable variable && this.scope.model().isLocalFunction(variable)) {
            this.scope.report(line, column, SigmaError.LOCAL_FUNCTION_ASSIGNED, name);
            return true;
        }
        final boolean constant = binding instanceof IBinding.Variable variable
                && this.scope.constants().isLocal(variable)
                || binding instanceof IBinding.Member member
                && member.member() instanceof IMemberSymbol.FieldSymbol field && field.isConstant();
        if (constant) {
            this.scope.report(line, column, SigmaError.CONST_ASSIGNED, name);
        }
        return constant;
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

    /*
     * A tuple written as its values: of the tuple type it is handed to when every value fits that type's places, each
     * then going in as its place's type; otherwise of its values' own types, with its places called nothing.
     */
    private ITypeSymbol tupleType(final IExpr.Tuple tuple, final ITypeSymbol expected) {
        final List<IExpr> elements = tuple.elements();
        final ITypeSymbol.TupleType wanted = expected instanceof ITypeSymbol.TupleType shape
                && shape.places().size() == elements.size() ? shape : null;
        final List<ITypeSymbol> into = wanted == null ? null : wanted.places();
        final List<ITypeSymbol> places = new ArrayList<>();
        boolean fits = wanted != null;
        for (int i = 0; i < elements.size(); i++) {
            final ITypeSymbol given = this.check(elements.get(i), into == null ? null : into.get(i));
            fits &= this.scope.rules().isAssignable(given, into == null ? ITypeSymbol.Primitive.VOID : into.get(i));
            places.add(given);
        }
        if (elements.size() < 2 || elements.size() > Declarations.MAX_TUPLE) {
            this.scope.report(tuple.line(), tuple.column(), SigmaError.TUPLE_SIZE, elements.size());
            return ITypeSymbol.Special.ERROR;
        }
        if (fits) {
            return wanted;
        }
        boolean known = true;
        for (int i = 0; i < places.size(); i++) {
            final ITypeSymbol place = places.get(i);
            if (place == ITypeSymbol.Special.NULL || place == ITypeSymbol.Primitive.VOID) {
                this.scope.report(elements.get(i).line(), elements.get(i).column(), SigmaError.TUPLE_PLACE_TYPE,
                        place.describe());
            }
            known &= !this.scope.rules().isError(place) && place != ITypeSymbol.Special.NULL
                    && place != ITypeSymbol.Primitive.VOID;
        }
        return known ? new ITypeSymbol.TupleType(this.scope.declarations().tupleOf(places), List.of())
                : ITypeSymbol.Special.ERROR;
    }

    /*
     * "(a, b) = (b, a)": a tuple taken apart into places that already exist, each written as an assignment of its own
     * would be, with every value read before any place is written. It gives nothing back, so it stands as a statement.
     */
    private ITypeSymbol deconstructType(final IExpr.Assign assign, final IExpr.Tuple places) {
        final ITypeSymbol value = this.check(assign.value(), null);
        final List<ITypeSymbol> from = value instanceof ITypeSymbol.TupleType tuple ? tuple.places() : null;
        final boolean fits = from != null && from.size() == places.elements().size();
        if (!fits && !this.scope.rules().isError(value)) {
            this.scope.report(assign.line(), assign.column(), SigmaError.DECONSTRUCT_SHAPE, value.describe(),
                    places.elements().size());
        }
        for (int i = 0; i < places.elements().size(); i++) {
            final IExpr place = places.elements().get(i);
            final ITypeSymbol target = this.place(place, Operator.ASSIGN);
            if (fits && !this.scope.rules().isError(target)
                    && this.writable(place, this.scope.model().bindingOf(place), assign)) {
                this.scope.expect(from.get(i), target, place);
            }
        }
        return ITypeSymbol.Primitive.VOID;
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
        final NamedType type = this.scope.declarations().lookup(name.identifier(), this.scope.currentType());
        if (type != null) {
            this.scope.declarations().reportIfNewer(type, name.line(), name.column());
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
        if (unary.operator() == Operator.ADDRESS_OF) {
            return this.addressType(unary);
        }
        if (unary.operator() == Operator.DEREFERENCE) {
            this.needsUnsafe(unary);
        }
        final boolean steps = unary.operator() == Operator.INCREMENT || unary.operator() == Operator.DECREMENT;
        final ITypeSymbol operand = steps ? this.place(unary.operand(), unary.operator())
                : this.check(unary.operand(), null);
        final IMemberSymbol.MethodSymbol declared = this.calls.operator(unary.operator(), List.of(unary.operand()),
                List.of(operand), unary);
        final ITypeSymbol result = declared != null ? declared.returnType()
                : this.scope.rules().unaryResult(unary.operator(), operand);
        if (result == null) {
            this.scope.report(unary.line(), unary.column(), SigmaError.OPERATOR_ON_TYPE,
                    unary.operator().text(), operand.describe());
            return ITypeSymbol.Special.ERROR;
        }
        final boolean changes = unary.operator() == Operator.INCREMENT || unary.operator() == Operator.DECREMENT;
        if (changes && unary.operand() instanceof IExpr.Member read && MemberChecker.ARRAY_LENGTH.equals(read.name())
                && this.scope.model().typeOf(read.target()) instanceof ITypeSymbol.ArrayType) {
            this.scope.report(unary.line(), unary.column(), SigmaError.CANNOT_ASSIGN_READONLY, read.name());
            return ITypeSymbol.Special.ERROR;
        }
        if (changes && this.refusesWrite(this.scope.model().bindingOf(unary.operand()), nameOf(unary.operand()),
                unary.line(), unary.column())) {
            return ITypeSymbol.Special.ERROR;
        }
        return result;
    }

    private ITypeSymbol binaryType(final IExpr.Binary binary) {
        final ITypeSymbol left = this.check(binary.left(), null);
        final ITypeSymbol right = this.check(binary.right(), null);
        // An operator the values' own types give a meaning to comes before the language's own.
        final IMemberSymbol.MethodSymbol declared = this.calls.operator(binary.operator(),
                List.of(binary.left(), binary.right()), List.of(left, right), binary);
        if (declared != null) {
            return declared.returnType();
        }
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
        return this.indexType(index, null);
    }

    /*
     * A place being written, with {@code writing} the operator it is written with: an indexer's has the method that
     * writes it chosen as well. Anything else is checked as it is read.
     */
    private ITypeSymbol place(final IExpr target, final Operator writing) {
        if (!(target instanceof IExpr.Index index)) {
            return this.check(target, null);
        }
        final ITypeSymbol type = this.indexType(index, writing);
        this.scope.model().setType(index, type);
        return type;
    }

    /*
     * An index: an array's place, a collection's, or what an indexer of the program's own type reads, and writes when
     * {@code writing} is the operator it is written with. One that is written by plain assignment needs no reading.
     */
    private ITypeSymbol indexType(final IExpr.Index index, final Operator writing) {
        final ITypeSymbol target = this.check(index.target(), null);
        final ITypeSymbol key = this.check(index.index(), null);
        final IMemberSymbol.MethodSymbol reads = this.calls.indexer(target, IDecl.INDEXER_GET, List.of(index.index()),
                List.of(key), index);
        final IMemberSymbol.MethodSymbol writes = writing == null ? null : this.calls.indexer(target,
                IDecl.INDEXER_SET, List.of(index.index()), List.of(key), index);
        if (reads != null || writes != null) {
            if (writing != null && writes == null) {
                this.scope.report(index.line(), index.column(), SigmaError.CANNOT_ASSIGN_READONLY, "this[]");
                return ITypeSymbol.Special.ERROR;
            }
            if (reads == null && writing != Operator.ASSIGN) {
                this.scope.report(index.line(), index.column(), SigmaError.CANNOT_INDEX, target.describe(),
                        key.describe());
                return ITypeSymbol.Special.ERROR;
            }
            if (reads != null) {
                this.scope.model().setCall(index, reads);
            }
            if (writes != null) {
                this.scope.model().setSetter(index, writes);
            }
            return reads != null ? reads.returnType() : writes.parameters().getLast().type();
        }
        final ITypeSymbol result = this.scope.rules().indexResult(target, key);
        if (result == null) {
            this.scope.report(index.line(), index.column(), SigmaError.CANNOT_INDEX,
                    target.describe(), key.describe());
            return ITypeSymbol.Special.ERROR;
        }
        // What was read from a JSON text is only read: a program builds its own text to write one.
        if (writing != null && this.scope.rules().isJsonValue(target)) {
            this.scope.report(index.line(), index.column(), SigmaError.CANNOT_ASSIGN_READONLY, "this[]");
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
        // A static class is a home for static members, with no object of its own to make.
        if (named.isStatic()) {
            this.scope.report(created.line(), created.column(), SigmaError.CANNOT_CREATE_STATIC, type.describe());
            this.calls.checkArguments(created.arguments());
            return type;
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
        // A pointer is cast to any other, as C# casts one: the address stays, and what it is read as changes.
        final boolean pointers = target instanceof ITypeSymbol.PointerType && value instanceof ITypeSymbol.PointerType;
        if (!pointers && !this.scope.rules().isAssignable(value, target)
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
        if (assign.target() instanceof IExpr.Tuple places) {
            return this.deconstructType(assign, places);
        }
        final ITypeSymbol target = this.place(assign.target(), assign.operator());
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
        final IMemberSymbol.MethodSymbol declared = this.calls.operator(assign.operator(),
                List.of(assign.target(), assign.value()), List.of(target, value), assign);
        final ITypeSymbol combined = declared != null ? declared.returnType()
                : this.scope.rules().binaryResult(assign.operator(), target, value);
        // C#'s b += 1 on a byte: worked out as an int and cut back to a byte, when what is added would fit one.
        final boolean narrowedBack = target == ITypeSymbol.Primitive.BYTE && this.scope.rules().isNumeric(combined)
                && (this.scope.rules().isAssignable(value, target)
                || this.scope.fitsByte(value, target, assign.value()));
        if (combined == null || !this.scope.rules().isAssignable(combined, target) && !narrowedBack) {
            this.scope.report(assign.line(), assign.column(), SigmaError.OPERATOR_ON_TYPES,
                    assign.operator().assignmentText(), target.describe(), value.describe());
        }
        return target;
    }

    private ITypeSymbol subscribe(final IExpr.Assign assign, final IMemberSymbol.EventSymbol event) {
        if (assign.operator() != Operator.ADD && assign.operator() != Operator.SUBTRACT) {
            this.scope.report(assign.line(), assign.column(), SigmaError.OPERATOR_ON_TYPE,
                    assign.operator().assignmentText(), event.delegateType().name());
            this.check(assign.value(), event.delegateType());
            return ITypeSymbol.Special.ERROR;
        }
        this.scope.expect(this.check(assign.value(), event.delegateType()), event.delegateType(),
                assign.value());
        return event.delegateType();
    }

    private boolean writable(final IExpr.Assign assign, final IBinding binding) {
        return this.writable(assign.target(), binding, assign);
    }

    /* Whether a place may be written to, reported at {@code at} when it may not. */
    private boolean writable(final IExpr target, final IBinding binding, final INode at) {
        if (target instanceof IExpr.Member read && MemberChecker.ARRAY_LENGTH.equals(read.name())
                && this.scope.model().typeOf(read.target()) instanceof ITypeSymbol.ArrayType) {
            // An array is as long as it was made, so its length is only ever read.
            this.scope.report(at.line(), at.column(), SigmaError.CANNOT_ASSIGN_READONLY, read.name());
            return false;
        }
        if (this.refusesWrite(binding, nameOf(target), at.line(), at.column())) {
            return false;
        }
        if (!(binding instanceof IBinding.Member member)) {
            return true;
        }
        if (member.member() instanceof IMemberSymbol.FieldSymbol field && field.isReadOnly()
                && !(this.scope.inConstructor() && field.owner() == this.scope.currentType())) {
            this.scope.report(at.line(), at.column(), SigmaError.CANNOT_ASSIGN_READONLY, field.name());
            return false;
        }
        if (member.member() instanceof IMemberSymbol.PropertySymbol property && !property.writable()) {
            this.scope.report(at.line(), at.column(), SigmaError.CANNOT_ASSIGN_READONLY, property.name());
            return false;
        }
        return true;
    }

    /**
     * A local function's body, checked as the lambda it is written as against its own shape. Unlike a lambda's, a
     * body that gives something back has to give it on every way out, as a method's does.
     */
    void localFunction(final IStmt.LocalFunction function, final ITypeSymbol shape) {
        this.scope.model().setType(function.body(), this.lambdaType(function.body(), shape, function));
    }

    private ITypeSymbol lambdaType(final IExpr.Lambda lambda, final ITypeSymbol expected) {
        return this.lambdaType(lambda, expected, null);
    }

    /* A lambda, against the delegate it is handed to; for a local function's, {@code returnsAt} is where it stands. */
    private ITypeSymbol lambdaType(final IExpr.Lambda lambda, final ITypeSymbol expected, final INode returnsAt) {
        final NamedType wanted = this.scope.rules().named(expected);
        if (wanted == null || wanted.kind() != NamedType.Kind.DELEGATE || wanted.invoke() == null) {
            if (!this.scope.rules().isError(expected)) {
                this.scope.report(lambda.line(), lambda.column(), SigmaError.LAMBDA_SHAPE,
                        expected == null ? StatementChecker.NOTHING : expected.describe());
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
        // A local function's shape was made from its own parameters, so they are not read a second time.
        this.declareLambdaParameters(lambda, shape, returnsAt == null);
        this.scope.returnType(shape.returnType());
        // A lambda's body is a body of its own, with labels of its own that the method's jumps cannot reach.
        final Set<String> methodRegions = this.scope.regions();
        // It stands in none of the method's catches or finally blocks, whatever it was written inside.
        final List<Object> handlers = this.scope.setHandlersAside();
        if (lambda.block() != null) {
            final Labels labels = Labels.of(lambda.block(), this.scope);
            this.scope.regions(labels.switchable());
            this.statements.checkBlock(lambda.block(), false);
            labels.resolve();
            if (returnsAt != null && shape.returnType() != ITypeSymbol.Primitive.VOID
                    && !StatementChecker.alwaysReturns(lambda.block(), labels.switchable())) {
                this.scope.report(returnsAt.line(), returnsAt.column(), SigmaError.MISSING_RETURN_VALUE,
                        shape.returnType().describe());
            }
        } else {
            final ITypeSymbol body = this.check(lambda.body(), shape.returnType());
            if (shape.returnType() != ITypeSymbol.Primitive.VOID) {
                this.scope.expect(body, shape.returnType(), lambda.body());
            }
        }
        if (lambda.block() != null) {
            this.scope.checkOutParameters(lambda.parameters(), lambda.block(), lambda);
        } else {
            this.scope.checkOutParameters(lambda.parameters(), lambda.body(), lambda);
        }
        this.scope.regions(methodRegions);
        this.scope.putHandlersBack(handlers);
        this.scope.scope(saved);
        this.scope.returnType(savedReturn);
        return expected;
    }

    private void declareLambdaParameters(final IExpr.Lambda lambda, final IMemberSymbol.MethodSymbol shape,
                                         final boolean readTypes) {
        for (int i = 0; i < lambda.parameters().size(); i++) {
            final IDecl.Parameter parameter = lambda.parameters().get(i);
            if (parameter.outward() != shape.parameters().get(i).outward()) {
                this.scope.report(parameter.line(), parameter.column(), shape.parameters().get(i).outward()
                        ? SigmaError.OUT_ARGUMENT_EXPECTED : SigmaError.OUT_ARGUMENT_UNEXPECTED,
                        shape.parameters().get(i).name());
            }
            final ITypeSymbol fromShape = shape.parameters().get(i).type();
            ITypeSymbol type = fromShape;
            if (readTypes && parameter.type() != null) {
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
        if (argument.ref()) {
            this.scope.report(argument.line(), argument.column(), SigmaError.REF_ONLY_FOR_STRCAT, argument.name());
            return ITypeSymbol.Special.ERROR;
        }
        return this.placeType(argument, expected);
    }

    /**
     * The place a {@code ref} argument names, which strcat reads and then writes: a variable or a field that already
     * exists, since there is nothing in a place declared right there to read.
     */
    ITypeSymbol refArgumentType(final IExpr.OutArgument argument) {
        return this.placeType(argument, null);
    }

    private ITypeSymbol placeType(final IExpr.OutArgument argument, final ITypeSymbol expected) {
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
            if (this.refusesWrite(variable, argument.name(), argument.line(), argument.column())) {
                return ITypeSymbol.Special.ERROR;
            }
            return variable.type();
        }
        final List<IMemberSymbol> found = this.scope.currentType() == null
                ? List.of() : BodyScope.lookup(this.scope.currentType(), argument.name());
        if (!found.isEmpty()) {
            if (found.getFirst() instanceof IMemberSymbol.FieldSymbol field && field.isConstant()) {
                this.scope.report(argument.line(), argument.column(), SigmaError.CONST_ASSIGNED, argument.name());
                return ITypeSymbol.Special.ERROR;
            }
            if (found.getFirst() instanceof IMemberSymbol.FieldSymbol field && !field.isReadOnly()) {
                if (!this.members.checkAccess(argument, field, BodyScope.Access.IMPLICIT)) {
                    return ITypeSymbol.Special.ERROR;
                }
                this.scope.model().setBinding(argument, new IBinding.Member(field, field.type()));
                return field.type();
            }
            this.scope.report(argument.line(), argument.column(), SigmaError.OUT_NOT_A_PLACE, argument.name());
            return ITypeSymbol.Special.ERROR;
        }
        this.scope.report(argument.line(), argument.column(), SigmaError.UNKNOWN_NAME, argument.name());
        return ITypeSymbol.Special.ERROR;
    }

    /**
     * sizeof of a type: the bytes a value of it takes in raw memory, written in as the number it is. Asked of the type
     * {@code sizeof(Node)} names as well, which reads as a call until nothing of the program's turns out to be sizeof.
     */
    ITypeSymbol sizeOf(final ITypeSymbol type, final IExpr at) {
        final int bytes = this.scope.declarations().layout().sizeOf(type);
        if (bytes == 0 && !this.scope.rules().isError(type)) {
            this.scope.report(at.line(), at.column(), SigmaError.SIZEOF_TYPE, type.describe());
        }
        final IExpr.Literal size = new IExpr.Literal(TokenKind.INT_LITERAL, bytes, at.line(), at.column());
        this.scope.model().setType(size, ITypeSymbol.Primitive.INT);
        this.scope.model().setLowered(at, size);
        return ITypeSymbol.Primitive.INT;
    }

    /* Reports a pointer used where unsafe code is not. */
    private void needsUnsafe(final INode at) {
        if (!this.scope.unsafe()) {
            this.scope.report(at.line(), at.column(), SigmaError.POINTERS_NEED_UNSAFE);
        }
    }

    /*
     * &x: the address of a local or a parameter, which from then on lives in raw memory for as long as its call runs;
     * or of a place a pointer already reaches, or of a field of a struct lying in either.
     */
    private ITypeSymbol addressType(final IExpr.Unary unary) {
        this.needsUnsafe(unary);
        final ITypeSymbol operand = this.check(unary.operand(), null);
        if (this.scope.rules().isError(operand)) {
            return ITypeSymbol.Special.ERROR;
        }
        if (!this.addressable(unary.operand())) {
            this.scope.report(unary.line(), unary.column(), SigmaError.ADDRESS_OF_WHAT);
            return ITypeSymbol.Special.ERROR;
        }
        if (!this.scope.declarations().layout().pointable(operand)) {
            this.scope.report(unary.line(), unary.column(), SigmaError.POINTER_TARGET, operand.describe());
            return ITypeSymbol.Special.ERROR;
        }
        return new ITypeSymbol.PointerType(operand);
    }

    /* Whether a place has an address, marking a variable it reaches as one that lives in raw memory. */
    private boolean addressable(final IExpr place) {
        return switch (place) {
            case IExpr.Name ignored when this.scope.model().bindingOf(place) instanceof IBinding.Variable variable -> {
                if (!this.scope.model().isAddressable(variable)) {
                    yield false;
                }
                this.scope.model().setInMemory(variable);
                yield true;
            }
            case IExpr.Unary unary -> unary.operator() == Operator.DEREFERENCE;
            case IExpr.Index index -> this.scope.rules().readablePointer(this.scope.model().typeOf(index.target()))
                    != null;
            case IExpr.Member member -> this.scope.model().bindingOf(member) instanceof IBinding.Member held
                    && !held.member().isStatic() && (held.member() instanceof IMemberSymbol.FieldSymbol
                    || held.member() instanceof IMemberSymbol.PropertySymbol)
                    && this.scope.declarations().layout().holdsOnlyValues(this.scope.model().typeOf(member.target()))
                    && this.addressable(member.target());
            default -> false;
        };
    }

    /* stackalloc int[n]: room for n values in raw memory, which goes when the call returns. */
    private ITypeSymbol stackAllocType(final IExpr.StackAlloc created) {
        this.needsUnsafe(created);
        final ITypeSymbol element = this.scope.declarations().resolve(created.elementType(), this.scope.currentType());
        this.scope.expect(this.check(created.length(), ITypeSymbol.Primitive.INT), ITypeSymbol.Primitive.INT,
                created.length());
        if (this.scope.rules().isError(element)) {
            return ITypeSymbol.Special.ERROR;
        }
        if (element == ITypeSymbol.Primitive.VOID || !this.scope.declarations().layout().pointable(element)) {
            this.scope.report(created.line(), created.column(), SigmaError.POINTER_TARGET, element.describe());
            return ITypeSymbol.Special.ERROR;
        }
        return new ITypeSymbol.PointerType(element);
    }

    /* The name a place was written with, which a message about writing to it quotes. */
    private static String nameOf(final IExpr place) {
        return switch (place) {
            case IExpr.Name name -> name.identifier();
            case IExpr.Member member -> member.name();
            default -> "?";
        };
    }
}
