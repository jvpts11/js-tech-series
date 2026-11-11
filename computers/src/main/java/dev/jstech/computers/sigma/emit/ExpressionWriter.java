/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.sigma.emit;

import dev.jstech.computers.sigma.SigmaError;
import dev.jstech.computers.sigma.ast.IExpr;
import dev.jstech.computers.sigma.ast.Operator;
import dev.jstech.computers.sigma.ast.TypeRef;
import dev.jstech.computers.sigma.lower.IrWrite;
import dev.jstech.computers.sigma.lower.Place;
import dev.jstech.computers.sigma.lower.TakenApart;
import dev.jstech.computers.sigma.sem.IBinding;
import dev.jstech.computers.sigma.sem.IMemberSymbol;
import dev.jstech.computers.sigma.sem.ITypeSymbol;
import dev.jstech.computers.sigma.sem.NamedType;
import dev.jstech.computers.vm.listing.IOperand;
import dev.jstech.computers.vm.listing.Opcode;
import dev.jstech.computers.vm.system.IntrinsicTypes;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import java.util.ArrayList;
import java.util.List;

/**
 * Writes down a value: lines that leave what an expression comes to on top of the stack.
 *
 * <p>One recursive walk, because that is what an expression is. Everything here is a case of it or
 * something a case needs, and the cases reach back into the walk for their own parts, so they stay
 * together: a sum writes its two sides and then the instruction, and each of those sides can be a
 * sum again.
 *
 * <p>A few shapes of the language have no instruction of their own and are written in terms of ones
 * that do. A property is a field. A short-circuit is a branch. An enum value is a number.
 */
@TextHolder
final class ExpressionWriter {

    private final Emitter emitter;
    private final MethodBody body;
    private final CallWriter calls;

    /*
     * The machine's own types, named from the one place that names them. The compiler writes a call to one of
     * these into a listing and the machine looks it up by the same name, so a second spelling here would be a
     * listing that loads and a call that nothing answers.
     */
    private static final String DELEGATE = IntrinsicTypes.DELEGATE;
    private static final String STRING = IntrinsicTypes.TEXT;

    /** What is not built yet when a string with holes reaches this stage without having been reduced. */
    private static final TextKey UNREDUCED_INTERPOLATION = TextKey.of(
            "jsc.sigma.expression_writer.unreduced_interpolation", "a string with holes that was never reduced");
    /** The bits a byte keeps, and the largest number it holds. */
    private static final int BYTE_BITS = 0xFF;

    /*
     * The one place the two fold back on each other: a call is a value, and the values a call takes are
     * values in their own right. Made here rather than passed in, because there is no order in which the
     * two could be built separately; it is only kept, never used, while this constructor runs.
     */
    ExpressionWriter(final Emitter emitter, final MethodBody body) {
        this.emitter = emitter;
        this.body = body;
        this.calls = new CallWriter(emitter, body, this);
    }

    /** The writer of calls that belongs to this one, for whoever writes a call that is not a value. */
    CallWriter calls() {
        return this.calls;
    }

    void value(final IExpr expression, final ITypeSymbol wanted) {
        if (expression == null) {
            return;
        }
        // Where an exception thrown by what follows says it was thrown.
        this.body.within(expression.line(), expression.column());
        /*
         * What a richer shape was reduced to is what gets written. The tree still holds what the player
         * wrote, because a message has to point at that; only this stage follows the simpler one.
         */
        final IExpr simpler = this.emitter.model.loweredOf(expression);
        if (simpler != null) {
            this.value(simpler, wanted);
            return;
        }
        if (this.shortcut(expression, wanted)) {
            return;
        }
        // A place in raw memory is read through its address, as wide as what lies there.
        if (this.emitter.model.inRawMemory(expression)) {
            final ITypeSymbol type = this.emitter.model.typeOf(expression);
            this.address(expression);
            this.emitter.raw.read(type, this.body, this.body::emit);
            this.coerce(type, wanted);
            return;
        }
        switch (expression) {
            case IExpr.Literal literal -> this.constant(literal);
            case IExpr.Name name -> this.name(name);
            case IExpr.This ignored -> this.body.pushThis();
            case IExpr.Base ignored -> this.body.pushThis();
            case IExpr.Member member -> this.member(member);
            case IExpr.Index index -> this.index(index);
            case IExpr.Call call -> this.calls.call(call);
            case IExpr.New created -> this.calls.created(created);
            case IExpr.NewArray created -> this.createdArray(created);
            case IExpr.Cast cast -> this.cast(cast);
            case IExpr.TypeTest test -> this.typeTest(test);
            case IExpr.Unary unary -> this.unary(unary);
            case IExpr.Binary binary -> this.binary(binary);
            case IExpr.Conditional conditional -> this.conditional(conditional, wanted);
            case IExpr.Assign assign -> this.assign(assign, true);
            case IExpr.Lambda lambda -> this.lambda(lambda);
            case IExpr.OutArgument ignored -> { }
            /*
             * Never reached: a string with holes is reduced to additions before anything is written, and
             * the line above follows what it was reduced to. Getting here means the reducing did not run,
             * which would otherwise show up as a program quietly missing a line it printed.
             */
            case IExpr.Interpolation written -> this.emitter.diagnostics.error(written.line(),
                    written.column(), SigmaError.NOT_YET_BUILT, UNREDUCED_INTERPOLATION);
            case IExpr.Tuple tuple -> this.tuple(tuple);
            case IExpr.StackAlloc created -> this.stackAlloc(created);
            // Never reached: a sizeof is the number it comes to, written in as the checker worked it out.
            case IExpr.SizeOf sized -> this.body.emit(Opcode.LDC_I4, new IOperand.I4(this.emitter.declarations
                    .layout().sizeOf(this.emitter.declarations.resolve(sized.type(), this.body.owner()))));
        }
        this.coerce(this.emitter.model.typeOf(expression), wanted);
    }

    /**
     * Leaves the address of a place in raw memory on the stack: what a pointer holds, moved past {@code i} values for
     * {@code p[i]}; the block of a variable whose address was taken; or that, moved to where a struct's field sits.
     */
    void address(final IExpr place) {
        switch (place) {
            case IExpr.Unary unary when unary.operator() == Operator.DEREFERENCE -> this.value(unary.operand(), null);
            case IExpr.Index index -> {
                this.value(index.target(), null);
                this.moved(index.index(), this.emitter.model.typeOf(index.target()), Opcode.ADD);
            }
            case IExpr.Name name when this.emitter.model.bindingOf(name) instanceof IBinding.Variable variable ->
                    this.body.emit(Opcode.LDLOC, new IOperand.Slot(this.body.addressOf(variable)));
            case IExpr.Member member when this.emitter.model.bindingOf(member) instanceof IBinding.Member held -> {
                this.address(member.target());
                final NamedType struct = this.emitter.rules.named(this.emitter.model.typeOf(member.target()));
                final int offset = this.emitter.declarations.layout().offsetOf(struct, held.member().name());
                if (offset != 0) {
                    this.body.emit(Opcode.LDC_I8, new IOperand.I8(offset));
                    this.body.emit(Opcode.ADD);
                }
            }
            default -> this.value(place, null);
        }
    }

    /** Puts the value on top of the stack into a variable whose address was taken, which lives in raw memory. */
    void storeInMemory(final IBinding.Variable variable) {
        final int held = this.body.hidden();
        this.body.emit(Opcode.STLOC, new IOperand.Slot(held));
        this.body.emit(Opcode.LDLOC, new IOperand.Slot(this.body.addressOf(variable)));
        this.body.emit(Opcode.LDLOC, new IOperand.Slot(held));
        this.emitter.raw.write(variable.type(), this.body, this.body::emit);
    }

    /*
     * The two values that need no work of their own where they go: null where a pointer is wanted is the address 0,
     * and a whole number written out where a byte is wanted already fits it.
     */
    private boolean shortcut(final IExpr expression, final ITypeSymbol wanted) {
        if (wanted instanceof ITypeSymbol.PointerType && expression instanceof IExpr.Literal literal
                && literal.value() == null) {
            this.body.emit(Opcode.LDC_I8, new IOperand.I8(0));
            return true;
        }
        if (wanted == ITypeSymbol.Primitive.BYTE && expression instanceof IExpr.Literal literal
                && literal.value() instanceof Integer number && number >= 0 && number <= BYTE_BITS) {
            this.body.emit(Opcode.LDC_I4, new IOperand.I4(number));
            return true;
        }
        return false;
    }

    /*
     * A whole number of values of what the pointer on the stack points at, added to it or taken from it: the number
     * made a long, and counted in bytes.
     */
    private void moved(final IExpr count, final ITypeSymbol pointer, final Opcode how) {
        final ITypeSymbol pointee = ((ITypeSymbol.PointerType) pointer).pointee();
        // A count written out is a distance known now: p[0] is p itself, and p[3] is p and a number of bytes.
        final IExpr known = this.emitter.model.loweredOf(count) != null ? this.emitter.model.loweredOf(count) : count;
        if (known instanceof IExpr.Literal literal && literal.value() instanceof Number number
                && !(number instanceof Float) && !(number instanceof Double)) {
            final long bytes = number.longValue() * this.emitter.declarations.layout().sizeOf(pointee);
            if (bytes != 0) {
                this.body.emit(Opcode.LDC_I8, new IOperand.I8(bytes));
                this.body.emit(how);
            }
            return;
        }
        this.value(count, ITypeSymbol.Primitive.LONG);
        this.emitter.raw.step(pointee, this.body::emit);
        this.body.emit(how);
    }

    /* stackalloc T[n]: n values' room, made zeroed and freed when the call returns, its address left on the stack. */
    private void stackAlloc(final IExpr.StackAlloc created) {
        this.value(created.length(), ITypeSymbol.Primitive.LONG);
        if (this.emitter.model.typeOf(created) instanceof ITypeSymbol.PointerType pointer) {
            this.emitter.raw.step(pointer.pointee(), this.body::emit);
        }
        this.body.emit(Opcode.LOCALLOC);
    }

    /**
     * The expression's value as a store or a hand-over keeps it: a struct is copied, unless it is
     * fresh from new and nobody else holds it, and everything else is itself.
     */
    void copied(final IExpr expression, final ITypeSymbol wanted) {
        this.value(expression, wanted);
        if (this.emitter.lowered.copies(expression)) {
            this.body.emit(Opcode.COPY);
        }
    }

    /** Whether a value of that type is a struct: copied whenever it is stored or handed over. */
    boolean isStruct(final ITypeSymbol type) {
        return this.emitter.rules.named(type) instanceof NamedType named
                && named.kind() == NamedType.Kind.STRUCT;
    }

    /** Puts the object a member is read from on the stack, unless the member belongs to a type. */
    void receiver(final IExpr target) {
        if (target instanceof IExpr.Member member) {
            if (this.emitter.model.bindingOf(member) instanceof IBinding.TypeName) {
                return;
            }
            this.value(member, null);
            return;
        }
        if (this.emitter.model.bindingOf(target) instanceof IBinding.TypeName) {
            return;
        }
        this.value(target, null);
    }

    /* A tuple written as its values: each put in as its place's type, then the struct made from them. */
    private void tuple(final IExpr.Tuple expression) {
        if (!(this.emitter.model.typeOf(expression) instanceof ITypeSymbol.TupleType tuple)) {
            this.body.emit(Opcode.LDNULL);
            return;
        }
        final List<ITypeSymbol> places = tuple.places();
        final List<String> written = new ArrayList<>(places.size());
        for (int i = 0; i < places.size(); i++) {
            this.copied(expression.elements().get(i), places.get(i));
            written.add(places.get(i).describe());
        }
        this.body.emit(Opcode.NEWOBJ, new IOperand.Constructor(tuple.describe(), written));
    }

    /** A tuple taken apart: kept in a place of its own, then each place written from the field that holds it. */
    void takenApart(final TakenApart parts) {
        this.copied(parts.value(), parts.kept().type());
        this.body.emit(Opcode.STLOC, new IOperand.Slot(this.body.slot(parts.kept())));
        for (final IExpr.Assign write : parts.writes()) {
            this.assign(write, false);
        }
    }

    void assign(final IExpr.Assign expression, final boolean leavesValue) {
        final TakenApart parts = this.emitter.lowered.takenApartOf(expression);
        if (parts != null) {
            this.takenApart(parts);
            return;
        }
        final IBinding binding = this.emitter.model.bindingOf(expression.target());
        if (binding instanceof IBinding.Member member
                && member.member() instanceof IMemberSymbol.EventSymbol event) {
            this.subscribe(expression, event);
            return;
        }
        final IrWrite written = this.emitter.lowered.writeOf(expression);
        if (written == null) {
            return; // not somewhere a value can be put, which has already been complained about
        }
        this.write(written, leavesValue);
    }

    /**
     * Writes a value into a place: the one shape three of them were reduced to.
     *
     * <p>Which place, which value and which answer were all settled before anything got here. What is decided
     * here is only how the stack has to be arranged for it, and that turns on two things: whether reaching the
     * place leaves what it belongs to underneath, and whether the answer is the value put there or the value
     * that was there.
     *
     * <p>An assignment answers with what it put there, which a slot can take on the way in with a duplicate,
     * since nothing of it is on the stack to bury; everywhere else it is read back afterwards. One more or one
     * less answers with the value on one side of the change, which is taken with a duplicate as it goes past,
     * kept in a place of its own where the duplicate would be buried.
     */
    private void write(final IrWrite written, final boolean answerWanted) {
        final Place place = written.place();
        /*
         * A local's slot is settled first, so that the number it gets is the one it would have had when the
         * place was worked out rather than one taken by something written in between.
         */
        if (place instanceof Place.Local local) {
            this.body.slot(local.variable());
        }
        // A plain write through a pointer that nobody reads back needs its address once, and nowhere to keep it.
        if (place instanceof Place.Memory memory && written.answer() == IrWrite.Answer.WRITTEN && !answerWanted
                && written.operator() == Operator.ASSIGN) {
            this.address(memory.target());
            this.combine(written);
            this.emitter.raw.write(written.type(), this.body, this.body::emit);
            return;
        }
        if (written.answer() == IrWrite.Answer.WRITTEN) {
            final Element kept = this.prepare(place);
            if (written.operator() != Operator.ASSIGN) {
                this.loadOver(place, kept);
            }
            this.combine(written);
            if (answerWanted && place instanceof Place.Local) {
                this.body.emit(Opcode.DUP);
            }
            this.store(place, kept);
            if (answerWanted && !(place instanceof Place.Local)) {
                this.reload(place, kept);
            }
            return;
        }
        if (place instanceof Place.Captured captured) {
            this.stepThroughAnObject(written, captured);
            return;
        }
        final int kept = place.overSomething() ? this.body.hidden() : -1;
        final Element inside = this.prepare(place);
        this.loadOver(place, inside);
        this.answer(written.answer() == IrWrite.Answer.BEFORE, kept);
        this.operate(written, null);
        this.answer(written.answer() == IrWrite.Answer.AFTER, kept);
        this.store(place, inside);
        if (kept >= 0) {
            this.body.emit(Opcode.LDLOC, new IOperand.Slot(kept));
        }
    }

    private void constant(final IExpr.Literal literal) {
        final Object held = literal.value();
        switch (literal.kind()) {
            case INT_LITERAL -> this.body.emit(Opcode.LDC_I4, new IOperand.I4((Integer) held));
            case LONG_LITERAL -> this.body.emit(Opcode.LDC_I8, new IOperand.I8((Long) held));
            case FLOAT_LITERAL -> this.body.emit(Opcode.LDC_R4, new IOperand.R4((Float) held));
            case DOUBLE_LITERAL -> this.body.emit(Opcode.LDC_R8, new IOperand.R8((Double) held));
            case CHAR_LITERAL -> this.body.emit(Opcode.LDC_I4, new IOperand.I4((Character) held));
            case STRING_LITERAL -> this.body.emit(Opcode.LDSTR, new IOperand.Text((String) held));
            case TRUE -> this.body.emit(Opcode.LDC_I4, new IOperand.I4(1));
            case FALSE -> this.body.emit(Opcode.LDC_I4, new IOperand.I4(0));
            default -> this.body.emit(Opcode.LDNULL);
        }
    }

    private void name(final IExpr.Name name) {
        final IBinding binding = this.emitter.model.bindingOf(name);
        if (binding instanceof IBinding.Variable variable) {
            if (this.body.kept(variable)) {
                this.body.loadKept(variable);
            } else {
                this.body.emit(Opcode.LDLOC, new IOperand.Slot(this.body.slot(variable)));
            }
            return;
        }
        if (binding instanceof IBinding.Member member) {
            this.loadMember(null, member.member());
        }
    }

    private void member(final IExpr.Member expression) {
        final IBinding binding = this.emitter.model.bindingOf(expression);
        if (binding instanceof IBinding.Member member) {
            this.loadMember(expression.target(), member.member());
        } else if (this.emitter.model.typeOf(expression.target()) instanceof ITypeSymbol.ArrayType) {
            // How many places an array has, the one thing it answers to.
            this.value(expression.target(), null);
            this.body.emit(Opcode.LDLEN);
        }
    }

    /*
     * A field, a property and an event are all read the same way, because in the assembly they
     * are the same thing: a named place on an object.
     */
    private void loadMember(final IExpr target, final IMemberSymbol member) {
        if (member instanceof IMemberSymbol.MethodSymbol method) {
            this.handler(target, method);
            return;
        }
        if (member.isStatic()) {
            this.body.emit(Opcode.LDSFLD, new IOperand.Field(member.owner().qualifiedName(), member.name()));
            return;
        }
        if (target == null) {
            this.body.pushThis();
        } else {
            this.receiver(target);
        }
        this.body.emit(Opcode.LDFLD, new IOperand.Field(this.body.ownerOf(member), member.name()));
    }

    // A method handed over without brackets becomes a delegate bound to whatever it belongs to.
    private void handler(final IExpr target, final IMemberSymbol.MethodSymbol method) {
        if (method.isStatic()) {
            this.body.emit(Opcode.LDNULL);
        } else if (target == null) {
            this.body.pushThis();
        } else {
            this.receiver(target);
        }
        this.body.emit(Opcode.LDFN, CallWriter.methodRef(method));
    }

    private void index(final IExpr.Index expression) {
        final ITypeSymbol target = this.emitter.model.typeOf(expression.target());
        this.value(expression.target(), null);
        this.key(expression);
        this.readElement(target, expression);
    }

    /* The key in an index's brackets: handed to an indexer as what it takes, and to the rest as it stands. */
    private void key(final IExpr.Index index) {
        final ITypeSymbol key = this.keyOf(index);
        if (key == null) {
            this.value(index.index(), null);
        } else {
            this.copied(index.index(), key);
        }
    }

    /* What an indexer of the program's own type takes in its brackets, which the key goes in as; null for the rest. */
    private ITypeSymbol keyOf(final IExpr.Index index) {
        final IMemberSymbol.MethodSymbol reads = this.indexerOf(index, false);
        final IMemberSymbol.MethodSymbol writes = this.indexerOf(index, true);
        final IMemberSymbol.MethodSymbol either = reads != null ? reads : writes;
        return either == null ? null : either.parameters().getFirst().type();
    }

    /* The method of an indexer an index reads with, or writes with, or null when no indexer is behind it. */
    private IMemberSymbol.MethodSymbol indexerOf(final IExpr.Index index, final boolean writes) {
        if (writes) {
            return this.emitter.model.setterOf(index);
        }
        return this.emitter.model.callOf(index) instanceof IMemberSymbol.MethodSymbol method && method.isIndexer()
                ? method : null;
    }

    /**
     * The thing and the place an index names, each worked out once and put away.
     *
     * <p>Both sides of an index are expressions and either may do something on its way to a value. Reading
     * what is in a place and writing what replaces it are one visit to ONE place, so naming it twice in the
     * assembly is not a repetition but a second, different place: {@code n[Next()] += 5} would call Next
     * twice and could read one element and write another. They are worked out once and pushed from where
     * they were kept whenever the stack wants them underneath.
     */
    private Element keepElement(final IExpr.Index index) {
        final ITypeSymbol of = this.emitter.model.typeOf(index.target());
        final int thing = this.body.hidden();
        final int at = this.body.hidden();
        this.value(index.target(), null);
        this.body.emit(Opcode.STLOC, new IOperand.Slot(thing));
        this.key(index);
        this.body.emit(Opcode.STLOC, new IOperand.Slot(at));
        return new Element(thing, at, of, index);
    }

    /** Puts the thing and the place back on the stack, for the read or the write that follows. */
    private void pushElement(final Element element) {
        this.body.emit(Opcode.LDLOC, new IOperand.Slot(element.thing()));
        this.body.emit(Opcode.LDLOC, new IOperand.Slot(element.at()));
    }

    /**
     * Pushes what the write will need under the value, and hands back the kept element when there is one.
     *
     * <p>A place inside a collection is the only one whose address is worked out rather than named, so it
     * is the only one with anything to hand back.
     */
    private Element prepare(final Place place) {
        return switch (place) {
            case Place.Captured ignored -> {
                this.body.pushClosure();
                yield null;
            }
            case Place.Held held -> {
                this.pushHolder(held);
                yield null;
            }
            case Place.Element at -> {
                final Element kept = this.keepElement(at.index());
                this.pushElement(kept);
                yield kept;
            }
            // The address is worked out once and kept, for a read before the write and for an answer after it.
            case Place.Memory at -> {
                final int address = this.body.hidden();
                this.address(at.target());
                this.body.emit(Opcode.STLOC, new IOperand.Slot(address));
                this.body.emit(Opcode.LDLOC, new IOperand.Slot(address));
                yield new Element(address, -1, this.emitter.model.typeOf(at.target()), null);
            }
            case Place.Local ignored -> null;
            case Place.Shared ignored -> null;
        };
    }

    /** Pushes what is in the place now, leaving whatever {@link #prepare} pushed still underneath it. */
    private void loadOver(final Place place, final Element kept) {
        switch (place) {
            case Place.Local local ->
                    this.body.emit(Opcode.LDLOC, new IOperand.Slot(this.body.slot(local.variable())));
            case Place.Captured captured -> this.body.loadKept(captured.variable());
            case Place.Shared shared -> this.body.emit(Opcode.LDSFLD, sharedField(shared));
            case Place.Held held -> {
                this.body.emit(Opcode.DUP);
                this.body.emit(Opcode.LDFLD,
                        new IOperand.Field(this.body.ownerOf(held.member()), held.member().name()));
            }
            case Place.Element ignored -> {
                this.pushElement(kept);
                this.readElement(kept.of(), kept.node());
            }
            case Place.Memory ignored -> this.readKept(kept);
        }
    }

    /* What lies at the address a place in raw memory keeps. */
    private void readKept(final Element kept) {
        this.body.emit(Opcode.LDLOC, new IOperand.Slot(kept.thing()));
        this.emitter.raw.read(kept.of(), this.body, this.body::emit);
    }

    /** Writes the value on top into the place, using up whatever {@link #prepare} pushed. */
    private void store(final Place place, final Element kept) {
        switch (place) {
            case Place.Local local ->
                    this.body.emit(Opcode.STLOC, new IOperand.Slot(this.body.slot(local.variable())));
            case Place.Captured captured -> this.body.storeKept(captured.variable());
            case Place.Shared shared -> this.body.emit(Opcode.STSFLD, sharedField(shared));
            case Place.Held held -> this.body.emit(Opcode.STFLD,
                    new IOperand.Field(this.body.ownerOf(held.member()), held.member().name()));
            case Place.Element ignored -> this.writeElement(kept.of(), kept.node());
            case Place.Memory ignored -> this.emitter.raw.write(kept.of(), this.body, this.body::emit);
        }
    }

    /** Reads the place again, for an answer that is wanted after the writing is done. */
    private void reload(final Place place, final Element kept) {
        switch (place) {
            case Place.Local local ->
                    this.body.emit(Opcode.LDLOC, new IOperand.Slot(this.body.slot(local.variable())));
            case Place.Captured captured -> this.body.loadKept(captured.variable());
            case Place.Shared shared -> this.loadMember(null, shared.member());
            case Place.Held held -> this.loadMember(held.target(), held.member());
            case Place.Element ignored -> { } // an element written as a value answers nothing, as before
            case Place.Memory ignored -> this.readKept(kept);
        }
    }

    /** What it belongs to, for a place on an object: what the program named, or this object. */
    private void pushHolder(final Place.Held held) {
        if (held.target() == null) {
            this.body.pushThis();
        } else {
            this.receiver(held.target());
        }
    }

    /**
     * The same, for a member expression that has not been read as a place: an event has no place, and a field a
     * statement writes to names the field, not what holds it.
     */
    void receiverOf(final IExpr target) {
        this.pushHolder(new Place.Held(target instanceof IExpr.Member member ? member.target() : null, null));
    }

    private static IOperand.Field sharedField(final Place.Shared shared) {
        return new IOperand.Field(shared.member().owner().qualifiedName(), shared.member().name());
    }

    /**
     * Reads the place the index names: an array by its number, a list or a map by its own way in, and a program's own
     * type through the method its indexer reads with.
     */
    private void readElement(final ITypeSymbol target, final IExpr.Index node) {
        final IMemberSymbol.MethodSymbol reads = this.indexerOf(node, false);
        if (reads != null) {
            this.body.emit(Opcode.CALL, CallWriter.methodRef(reads));
            return;
        }
        if (target instanceof ITypeSymbol.ArrayType) {
            this.body.emit(Opcode.LDELEM);
            return;
        }
        if (this.emitter.rules.isJsonValue(target)) {
            // A member by its name, an element by its number: the value's own Get and At.
            final boolean byName = this.emitter.model.typeOf(node.index()) == this.emitter.builtIns.stringType();
            this.body.emit(Opcode.CALL, new IOperand.Method(target.describe(), byName ? "Get" : "At",
                    List.of(byName ? "string" : "int"), target.describe()));
            return;
        }
        final NamedType named = this.emitter.rules.named(target);
        final List<ITypeSymbol> held = this.emitter.rules.arguments(target);
        final boolean isMap = named == this.emitter.builtIns.mapType();
        this.body.emit(Opcode.CALL, new IOperand.Method(named == null ? "object" : named.qualifiedName(), "Get",
                List.of(isMap ? held.getFirst().describe() : "int"),
                held.isEmpty() ? "object" : held.getLast().describe()));
    }

    /** Writes the place the index names, with the thing, the place and the value already on the stack. */
    private void writeElement(final ITypeSymbol target, final IExpr.Index node) {
        final IMemberSymbol.MethodSymbol writes = this.indexerOf(node, true);
        if (writes != null) {
            this.body.emit(Opcode.CALL, CallWriter.methodRef(writes));
            return;
        }
        if (target instanceof ITypeSymbol.ArrayType) {
            this.body.emit(Opcode.STELEM);
            return;
        }
        final NamedType named = this.emitter.rules.named(target);
        final List<ITypeSymbol> held = this.emitter.rules.arguments(target);
        final boolean isMap = named == this.emitter.builtIns.mapType();
        this.body.emit(Opcode.CALL, new IOperand.Method(named == null ? "object" : named.qualifiedName(),
                isMap ? "Put" : "Set",
                List.of(isMap ? held.getFirst().describe() : "int",
                        held.isEmpty() ? "object" : held.getLast().describe()), "void"));
    }

    private void createdArray(final IExpr.NewArray expression) {
        this.value(expression.length(), ITypeSymbol.Primitive.INT);
        this.body.emit(Opcode.NEWARR, new IOperand.Type(expression.elementType().describe()));
    }

    private void cast(final IExpr.Cast expression) {
        final ITypeSymbol from = this.emitter.model.typeOf(expression.value());
        final ITypeSymbol to = this.emitter.model.typeOf(expression);
        // A pointer cast to another keeps its address; only what it is read as changes, which is the compiler's.
        if (to instanceof ITypeSymbol.PointerType) {
            this.value(expression.value(), to);
            return;
        }
        this.value(expression.value(), null);
        if (this.emitter.rules.isNumeric(from) && this.emitter.rules.isNumeric(to)) {
            this.convert(to);
            return;
        }
        this.body.emit(Opcode.CASTCLASS, new IOperand.Type(this.named(expression.type())));
    }

    /*
     * A type as the runtime knows it, which is the whole name. What was written may be the short
     * one, and a short name matches nothing at all once the program lives in a namespace.
     */
    private String named(final TypeRef reference) {
        return this.emitter.declarations.resolve(reference, this.body.owner()).describe();
    }

    /*
     * "is" asks and gives back an answer; "as" converts when it can and gives back nothing when
     * it cannot, which is the same question asked first and acted on.
     */
    private void typeTest(final IExpr.TypeTest expression) {
        final String written = this.named(expression.type());
        if (!expression.conversion()) {
            this.value(expression.value(), null);
            this.body.emit(Opcode.ISINST, new IOperand.Type(written));
            return;
        }
        final String otherwise = this.body.label();
        final String end = this.body.label();
        this.value(expression.value(), null);
        this.body.emit(Opcode.DUP);
        this.body.emit(Opcode.ISINST, new IOperand.Type(written));
        this.body.emit(Opcode.BRFALSE, new IOperand.Label(otherwise));
        this.body.emit(Opcode.CASTCLASS, new IOperand.Type(written));
        this.body.emit(Opcode.BR, new IOperand.Label(end));
        this.body.mark(otherwise);
        this.body.emit(Opcode.POP);
        this.body.emit(Opcode.LDNULL);
        this.body.mark(end);
    }

    private void unary(final IExpr.Unary expression) {
        if (expression.operator() == Operator.ADDRESS_OF) {
            this.address(expression.operand());
            return;
        }
        if (expression.operator() == Operator.INCREMENT || expression.operator() == Operator.DECREMENT) {
            final IrWrite written = this.emitter.lowered.writeOf(expression);
            if (written != null) {
                this.write(written, true);
            }
            return;
        }
        if (this.emitter.model.callOf(expression) instanceof IMemberSymbol.MethodSymbol declared
                && declared.isOperator()) {
            this.operator(declared, List.of(expression.operand()));
            return;
        }
        this.value(expression.operand(), null);
        switch (expression.operator()) {
            case NOT -> {
                this.body.emit(Opcode.LDC_I4, new IOperand.I4(0));
                this.body.emit(Opcode.CEQ);
            }
            case NEGATE -> this.body.emit(Opcode.NEG);
            case COMPLEMENT -> this.body.emit(Opcode.NOT);
            default -> { }
        }
    }

    /* An operator a program's own type gives a meaning to: its values handed to its static method, in order. */
    private void operator(final IMemberSymbol.MethodSymbol declared, final List<IExpr> values) {
        for (int i = 0; i < values.size(); i++) {
            this.copied(values.get(i), declared.parameters().get(i).type());
        }
        this.body.emit(Opcode.CALL, CallWriter.methodRef(declared));
    }

    /*
     * The change a step or a combining write makes to the value on top of the stack: the language's arithmetic with
     * the value pushed first, or the operator's method of the place's type, handed the value when there is one.
     */
    private void operate(final IrWrite written, final ITypeSymbol wanted) {
        // A pointer moves by whole values of what it points at.
        if (written.type() instanceof ITypeSymbol.PointerType pointer) {
            this.moved(written.value(), pointer, arithmetic(written.operator()));
            return;
        }
        if (written.through() != null) {
            // A struct goes to the method as a copy of its own, so what was there before is still there to answer.
            if (this.isStruct(written.type())) {
                this.body.emit(Opcode.COPY);
            }
            if (written.value() != null) {
                this.copied(written.value(), written.through().parameters().getLast().type());
            }
            this.body.emit(Opcode.CALL, CallWriter.methodRef(written.through()));
            return;
        }
        this.value(written.value(), wanted);
        this.body.emit(arithmetic(written.operator()));
        // A byte worked on comes back as a whole number, which is cut to the byte it is written into.
        if (written.type() == ITypeSymbol.Primitive.BYTE) {
            this.body.emit(Opcode.LDC_I4, new IOperand.I4(BYTE_BITS));
            this.body.emit(Opcode.AND);
        }
    }

    /** Keeps the value that answers the expression: a duplicate on top, or one put away in a slot. */
    private void answer(final boolean now, final int kept) {
        if (!now) {
            return;
        }
        this.body.emit(Opcode.DUP);
        if (kept >= 0) {
            this.body.emit(Opcode.STLOC, new IOperand.Slot(kept));
        }
    }

    /*
     * A captured variable is a field of the object a lambda shares with the method around it, so both
     * reading and writing name that object. There is nothing to duplicate on the way past, and the value
     * is read again instead, before or after depending on which one was asked for.
     */
    private void stepThroughAnObject(final IrWrite written, final Place.Captured captured) {
        if (written.answer() == IrWrite.Answer.BEFORE) {
            this.body.loadKept(captured.variable());
        }
        this.prepare(captured);
        this.loadOver(captured, null);
        this.operate(written, null);
        this.store(captured, null);
        if (written.answer() == IrWrite.Answer.AFTER) {
            this.body.loadKept(captured.variable());
        }
    }

    private void binary(final IExpr.Binary expression) {
        if (this.emitter.model.callOf(expression) instanceof IMemberSymbol.MethodSymbol declared
                && declared.isOperator()) {
            this.operator(declared, List.of(expression.left(), expression.right()));
            return;
        }
        if (this.pointerArithmetic(expression)) {
            return;
        }
        switch (expression.operator()) {
            case AND, OR -> this.shortCircuit(expression);
            case ADD -> this.plus(expression);
            case EQUAL, NOT_EQUAL, LESS, LESS_EQUAL, GREATER, GREATER_EQUAL ->
                    this.compare(expression);
            default -> {
                final ITypeSymbol result = this.emitter.model.typeOf(expression);
                this.value(expression.left(), result);
                this.value(expression.right(), shiftKeepsItsOwn(expression) ? null : result);
                this.body.emit(arithmetic(expression.operator()));
                this.wholeDivision(expression, result);
            }
        }
    }

    /*
     * p + n, n + p and p - n move a pointer by whole values of what it points at; p - q is how many of them lie
     * between two. False for anything else, which is written as numbers are.
     */
    private boolean pointerArithmetic(final IExpr.Binary expression) {
        final ITypeSymbol left = this.emitter.model.typeOf(expression.left());
        final ITypeSymbol right = this.emitter.model.typeOf(expression.right());
        final boolean adds = expression.operator() == Operator.ADD;
        if (!adds && expression.operator() != Operator.SUBTRACT
                || !(left instanceof ITypeSymbol.PointerType) && !(right instanceof ITypeSymbol.PointerType)) {
            return false;
        }
        if (left instanceof ITypeSymbol.PointerType pointer && right instanceof ITypeSymbol.PointerType) {
            this.value(expression.left(), null);
            this.value(expression.right(), null);
            this.body.emit(Opcode.SUB);
            final int size = this.emitter.declarations.layout().sizeOf(pointer.pointee());
            if (size != 1) {
                this.body.emit(Opcode.LDC_I8, new IOperand.I8(size));
                this.body.emit(Opcode.DIV);
                this.body.emit(Opcode.CONV_I8);
            }
            return true;
        }
        final boolean pointerFirst = left instanceof ITypeSymbol.PointerType;
        this.value(pointerFirst ? expression.left() : expression.right(), null);
        this.moved(pointerFirst ? expression.right() : expression.left(), pointerFirst ? left : right,
                adds ? Opcode.ADD : Opcode.SUB);
        return true;
    }

    /**
     * Says in the assembly that a division or a remainder was between whole numbers.
     *
     * <p>Dividing two whole numbers throws the fraction away and dividing two real ones does not, and
     * which of those a line meant is something only the compiler knows: it reads the types. Writing
     * the conversion down after the division puts that knowledge in the assembly itself, where
     * anything that reads it later (a machine of another kind, another language) can see it; the
     * runtime it was written for hands a whole number straight back.
     */
    private void wholeDivision(final IExpr.Binary expression, final ITypeSymbol result) {
        if (expression.operator() != Operator.DIVIDE && expression.operator() != Operator.REMAINDER) {
            return;
        }
        if (result == ITypeSymbol.Primitive.INT || result == ITypeSymbol.Primitive.LONG) {
            this.convert(result);
        }
    }

    /*
     * A shift moves its left side by however much its right side says, and the two do not have
     * to be the same kind of number.
     */
    private static boolean shiftKeepsItsOwn(final IExpr.Binary expression) {
        return expression.operator() == Operator.SHIFT_LEFT
                || expression.operator() == Operator.SHIFT_RIGHT;
    }

    private void plus(final IExpr.Binary expression) {
        final ITypeSymbol result = this.emitter.model.typeOf(expression);
        if (result == this.emitter.builtIns.stringType()) {
            final String left = this.joined(expression.left());
            final String right = this.joined(expression.right());
            this.body.emit(Opcode.CALL, new IOperand.Method(STRING, "Concat", List.of(left, right), STRING));
            return;
        }
        this.value(expression.left(), result);
        this.value(expression.right(), result);
        this.body.emit(Opcode.ADD);
    }

    /**
     * Pushes a value that is about to be joined to a string, and says what type was pushed.
     *
     * <p>An object of a type that says how it reads, with a {@code ToString()} of its own, is asked
     * for that text here, so a record in a sentence reads as its fields rather than as its type's
     * name; anything else is joined as it is, and the runtime writes it the way it writes values.
     */
    private String joined(final IExpr expression) {
        final ITypeSymbol type = this.emitter.model.typeOf(expression);
        this.value(expression, null);
        /*
         * A character is kept as its number while a program runs, so joined as it stands it would read as that
         * number: 'a' in a sentence came out as 97. It is turned into the one-letter text it means first.
         */
        if (type == ITypeSymbol.Primitive.CHAR) {
            this.body.emit(Opcode.CALL, new IOperand.Method(STRING, "FromChar", List.of("char"), STRING));
            return STRING;
        }
        // A byte is a whole number while a program runs, and is joined as one.
        if (type == ITypeSymbol.Primitive.BYTE) {
            return ITypeSymbol.Primitive.INT.describe();
        }
        final IMemberSymbol.MethodSymbol reads = readsItself(type);
        if (reads == null) {
            return describe(type);
        }
        this.body.emit(Opcode.CALL, CallWriter.methodRef(reads));
        return STRING;
    }

    /** The {@code ToString()} a type declares for itself, or null when it reads as its name. */
    private static IMemberSymbol.MethodSymbol readsItself(final ITypeSymbol type) {
        // A tuple reads as the struct made for it does, which says how.
        final NamedType named = type instanceof ITypeSymbol.TupleType tuple ? tuple.struct()
                : type instanceof NamedType plain ? plain : null;
        if (named == null || named.kind() == NamedType.Kind.ENUM
                || named.kind() == NamedType.Kind.DELEGATE || named.isBuiltIn()) {
            return null;
        }
        for (final IMemberSymbol member : named.allMembers()) {
            if (member instanceof IMemberSymbol.MethodSymbol method && "ToString".equals(method.name())
                    && method.parameters().isEmpty() && !method.isStatic()) {
                return method;
            }
        }
        return null;
    }

    private void compare(final IExpr.Binary expression) {
        final ITypeSymbol left = this.emitter.model.typeOf(expression.left());
        final ITypeSymbol right = this.emitter.model.typeOf(expression.right());
        final ITypeSymbol common = this.emitter.rules.promote(left, right);
        if (left instanceof ITypeSymbol.PointerType || right instanceof ITypeSymbol.PointerType) {
            this.comparedAddress(expression.left(), left);
            this.comparedAddress(expression.right(), right);
        } else {
            this.value(expression.left(), common);
            this.value(expression.right(), common);
        }
        switch (expression.operator()) {
            case EQUAL -> this.body.emit(Opcode.CEQ);
            case NOT_EQUAL -> {
                this.body.emit(Opcode.CEQ);
                this.invert();
            }
            case LESS -> this.body.emit(Opcode.CLT);
            case GREATER -> this.body.emit(Opcode.CGT);
            case LESS_EQUAL -> {
                this.body.emit(Opcode.CGT);
                this.invert();
            }
            default -> {
                this.body.emit(Opcode.CLT);
                this.invert();
            }
        }
    }

    /*
     * One side of a comparison of pointers, as the address it holds: null is 0, and a pointer a field never given one
     * holds nothing, which is made 0 too, so the two read as the same.
     */
    private void comparedAddress(final IExpr side, final ITypeSymbol type) {
        if (!(type instanceof ITypeSymbol.PointerType)) {
            this.body.emit(Opcode.LDC_I8, new IOperand.I8(0));
            return;
        }
        this.value(side, null);
        this.body.emit(Opcode.CONV_I8);
    }

    private void invert() {
        this.body.emit(Opcode.LDC_I4, new IOperand.I4(0));
        this.body.emit(Opcode.CEQ);
    }

    /*
     * The right side of "and" and "or" is not run when the left side already settles the answer,
     * which is a branch and cannot be an instruction that takes both sides at once.
     */
    private void shortCircuit(final IExpr.Binary expression) {
        final String settled = this.body.label();
        final String end = this.body.label();
        final boolean isAnd = expression.operator() == Operator.AND;
        this.value(expression.left(), ITypeSymbol.Primitive.BOOL);
        this.body.emit(isAnd ? Opcode.BRFALSE : Opcode.BRTRUE, new IOperand.Label(settled));
        this.value(expression.right(), ITypeSymbol.Primitive.BOOL);
        this.body.emit(Opcode.BR, new IOperand.Label(end));
        this.body.mark(settled);
        this.body.emit(Opcode.LDC_I4, new IOperand.I4(isAnd ? 0 : 1));
        this.body.mark(end);
    }

    private void conditional(final IExpr.Conditional expression, final ITypeSymbol wanted) {
        final ITypeSymbol result = wanted != null ? wanted : this.emitter.model.typeOf(expression);
        final String otherwise = this.body.label();
        final String end = this.body.label();
        this.value(expression.condition(), ITypeSymbol.Primitive.BOOL);
        this.body.emit(Opcode.BRFALSE, new IOperand.Label(otherwise));
        this.value(expression.whenTrue(), result);
        this.body.emit(Opcode.BR, new IOperand.Label(end));
        this.body.mark(otherwise);
        this.value(expression.whenFalse(), result);
        this.body.mark(end);
    }

    // assignment

    private void combine(final IrWrite written) {
        if (written.operator() == Operator.ASSIGN) {
            this.copied(written.value(), written.type());
            return;
        }
        if (written.type() == this.emitter.builtIns.stringType() && written.through() == null) {
            final String added = this.joined(written.value());
            this.body.emit(Opcode.CALL,
                    new IOperand.Method(STRING, "Concat", List.of(STRING, added), STRING));
            return;
        }
        this.operate(written, written.type());
    }

    /*
     * Joining and parting handlers is what the runtime does with a delegate, so the assembly asks
     * it rather than pretending an event is a kind of arithmetic.
     */
    private void subscribe(final IExpr.Assign expression, final IMemberSymbol.EventSymbol event) {
        final String delegate = event.delegateType().qualifiedName();
        if (!event.isStatic()) {
            this.receiverOf(expression.target());
            this.body.emit(Opcode.DUP);
        }
        this.body.emit(event.isStatic() ? Opcode.LDSFLD : Opcode.LDFLD,
                new IOperand.Field(event.isStatic() ? event.owner().qualifiedName()
                        : this.body.ownerOf(event), event.name()));
        this.value(expression.value(), event.delegateType());
        this.body.emit(Opcode.CALL, new IOperand.Method(DELEGATE,
                expression.operator() == Operator.ADD ? "Combine" : "Remove",
                List.of(delegate, delegate), delegate));
        this.body.emit(event.isStatic() ? Opcode.STSFLD : Opcode.STFLD,
                new IOperand.Field(event.isStatic() ? event.owner().qualifiedName()
                        : this.body.ownerOf(event), event.name()));
    }

    /*
     * A lambda becomes a method of its own, and what is written here is a delegate bound to whatever
     * that method has to be reached through: the object the method belonged to, or the object it
     * shares with the lambdas inside it.
     */
    private void lambda(final IExpr.Lambda lambda) {
        final ITypeSymbol type = this.emitter.model.typeOf(lambda);
        final NamedType delegate = this.emitter.rules.named(type);
        if (delegate == null || delegate.invoke() == null) {
            this.body.emit(Opcode.LDNULL);
            return;
        }
        final List<ITypeSymbol> filled = this.emitter.rules.arguments(type);
        final IMemberSymbol.MethodSymbol shape = delegate.invoke();
        final ITypeSymbol gives = this.emitter.rules.substitute(shape.returnType(), filled);
        final List<String> written = new ArrayList<>();
        for (final IMemberSymbol.ParameterSymbol parameter : shape.parameters()) {
            written.add(Emitter.parameterText(parameter.outward(),
                    this.emitter.rules.substitute(parameter.type(), filled)));
        }

        final Closure closure = this.body.closure();
        final String name = this.emitter.lambdaMethod(lambda, this.body, gives, written);
        if (closure == null) {
            this.body.pushThis();
            this.body.emit(Opcode.LDFN, new IOperand.Method(this.body.owner().qualifiedName(), name,
                    written, gives.describe()));
            return;
        }
        this.body.pushClosure();
        this.body.emit(Opcode.LDFN,
                new IOperand.Method(closure.type(), name, written, gives.describe()));
    }

    // conversions

    private void coerce(final ITypeSymbol from, final ITypeSymbol to) {
        if (from == null || to == null || from.equals(to)) {
            return;
        }
        if (this.emitter.rules.isNumeric(from) && this.emitter.rules.isNumeric(to)) {
            this.convert(to);
        }
    }

    private void convert(final ITypeSymbol to) {
        if (to == ITypeSymbol.Primitive.BYTE) {
            // A byte keeps the low eight bits of the whole number, as a cast to one does in C#.
            this.body.emit(Opcode.CONV_I4);
            this.body.emit(Opcode.LDC_I4, new IOperand.I4(BYTE_BITS));
            this.body.emit(Opcode.AND);
        } else if (to == ITypeSymbol.Primitive.LONG) {
            this.body.emit(Opcode.CONV_I8);
        } else if (to == ITypeSymbol.Primitive.FLOAT) {
            this.body.emit(Opcode.CONV_R4);
        } else if (to == ITypeSymbol.Primitive.DOUBLE) {
            this.body.emit(Opcode.CONV_R8);
        } else {
            this.body.emit(Opcode.CONV_I4);
        }
    }

    private static String describe(final ITypeSymbol type) {
        return type == null ? "object" : type.describe();
    }

    private static Opcode arithmetic(final Operator operator) {
        return switch (operator) {
            case ADD -> Opcode.ADD;
            case SUBTRACT -> Opcode.SUB;
            case MULTIPLY -> Opcode.MUL;
            case DIVIDE -> Opcode.DIV;
            case REMAINDER -> Opcode.REM;
            case BIT_AND -> Opcode.AND;
            case BIT_OR -> Opcode.OR;
            case BIT_XOR -> Opcode.XOR;
            case SHIFT_LEFT -> Opcode.SHL;
            default -> Opcode.SHR;
        };
    }

    /**
     * One place inside an array, a list, a map or a program's own type with an indexer, as the slots holding the thing
     * and the place in it, and the index that named it. A place in raw memory is kept the same way, as the slot holding
     * its address and the type of what lies there, with no place in it and no index.
     */
    private record Element(int thing, int at, ITypeSymbol of, IExpr.Index node) {
    }
}
