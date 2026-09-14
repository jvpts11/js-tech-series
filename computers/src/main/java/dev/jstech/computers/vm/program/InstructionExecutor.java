/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.vm.program;

import dev.jstech.computers.vm.listing.IOperand;
import dev.jstech.computers.vm.listing.Instruction;
import dev.jstech.computers.vm.listing.Opcode;

/**
 * Carrying out a program's instructions: the next one of the thread whose turn it is, and what each one does.
 *
 * <p>Each family of instructions goes straight to the part that knows it (fields, calls, objects and arrays, type
 * checks) as a plain call on a field. There is no object per opcode and nothing virtual between the switch and the
 * work, because one call more per instruction is a share of what an instruction costs that shows.
 */
final class InstructionExecutor {

    /** What two values being the same means, which needs the program's own types to tell a struct from a class. */
    private final ValueSemantics values;
    /** What a cast and a type test make of a value. */
    private final TypeChecks types;
    private final Heap heap;
    private final ThreadScheduler scheduler;
    private final MonitorTable locks;
    private final FieldAccess fieldAccess;
    private final CallDispatch calls;
    private final ObjectMaking objects;

    InstructionExecutor(final ProgramImage program, final Heap heap, final ThreadScheduler scheduler,
                        final MonitorTable locks, final FieldAccess fieldAccess, final CallDispatch calls,
                        final ObjectMaking objects) {
        this.values = new ValueSemantics(program);
        this.types = new TypeChecks(program);
        this.heap = heap;
        this.scheduler = scheduler;
        this.locks = locks;
        this.fieldAccess = fieldAccess;
        this.calls = calls;
        this.objects = objects;
    }

    /** Runs the next instruction of the thread's top frame, or leaves a method that has run off its end. */
    void one(final ProgramThread thread) {
        final Frame frame = thread.frames.peek();
        if (frame.at >= frame.method.length()) {
            this.calls.leave(frame, null);
            return;
        }
        final Instruction instruction = frame.method.instruction(frame.at);
        frame.at++;
        this.run(thread, frame, instruction, frame.at);
    }

    /** Carries out one instruction of {@code frame}; {@code line} is where the frame already stands, one past it. */
    void run(final ProgramThread thread, final Frame frame, final Instruction instruction, final int line) {
        switch (instruction.opcode()) {
            case LDC_I4 -> frame.push(((IOperand.I4) instruction.operand()).value());
            case LDC_I8 -> frame.push(((IOperand.I8) instruction.operand()).value());
            case LDC_R4 -> frame.push(((IOperand.R4) instruction.operand()).value());
            case LDC_R8 -> frame.push(((IOperand.R8) instruction.operand()).value());
            case LDNULL -> frame.push(null);
            case LDSTR -> frame.push(this.heap.text(((IOperand.Text) instruction.operand()).value(), line));
            case LDTHIS -> frame.push(frame.self);
            case LDLOC -> frame.push(frame.slots[((IOperand.Slot) instruction.operand()).index()]);
            case STLOC -> frame.slots[((IOperand.Slot) instruction.operand()).index()] = frame.pop();
            case POP -> frame.pop();
            case COPY -> frame.push(this.objects.copyOf(frame.pop(), line));
            case DUP -> frame.push(frame.peek());
            case LDFLD -> this.fieldAccess.load(frame, (IOperand.Field) instruction.operand(), line);
            case STFLD -> this.fieldAccess.store(frame, (IOperand.Field) instruction.operand(), line);
            case LDSFLD -> this.fieldAccess.loadStatic(frame, (IOperand.Field) instruction.operand(), line);
            case STSFLD -> this.fieldAccess.storeStatic(frame, (IOperand.Field) instruction.operand(), line);
            case ADD, SUB, MUL, DIV, REM, AND, OR, XOR, SHL, SHR ->
                    this.arithmetic(frame, instruction.opcode(), line);
            case NEG -> frame.push(Numbers.negate(frame.pop()));
            case NOT -> frame.push(Numbers.complement(frame.pop()));
            case CONV_I4 -> frame.push(Numbers.toInt(frame.pop()));
            case CONV_I8 -> frame.push(Numbers.toLong(frame.pop()));
            case CONV_R4 -> frame.push(Numbers.toFloat(frame.pop()));
            case CONV_R8 -> frame.push(Numbers.toDouble(frame.pop()));
            case CEQ, CLT, CGT -> this.compare(frame, instruction.opcode());
            case BR -> frame.at = frame.method.jump(line - 1);
            case BRTRUE -> this.jumpIf(frame, line, ValueSemantics.truth(frame.pop()));
            case BRFALSE -> this.jumpIf(frame, line, !ValueSemantics.truth(frame.pop()));
            case BEQ, BNE, BLT, BLE, BGT, BGE -> this.jumpCompare(frame, instruction.opcode(), line);
            case NEWOBJ -> this.objects.newObject(frame, frame.method.creation(line - 1), line);
            case NEWARR -> this.objects.newArray(frame, (IOperand.Type) instruction.operand(), line);
            case LDELEM -> this.objects.loadElement(frame, line);
            case STELEM -> this.objects.storeElement(frame, line);
            case LDLEN -> frame.push(this.objects.array(frame.pop(), line).length());
            case DISPOSE -> this.heap.dispose(frame.pop(), line);
            case MONITOR_ENTER -> this.enterMonitor(thread, frame, line);
            case MONITOR_EXIT -> this.exitMonitor(thread, frame, line);
            case CASTCLASS -> frame.push(this.types.cast(frame.pop(),
                    ((IOperand.Type) instruction.operand()).name(), line));
            case ISINST -> frame.push(this.types.isInstance(frame.pop(),
                    ((IOperand.Type) instruction.operand()).name()));
            case LDFN -> this.calls.handler(frame, (IOperand.Method) instruction.operand(), line);
            case CALL, CALLVIRT -> this.calls.call(frame, frame.method.call(line - 1),
                    instruction.opcode() == Opcode.CALLVIRT, line);
            case SYS -> throw new Halt(Halt.Reason.NO_NETWORK, line,
                    "this computer is not on a network");
            case RET -> this.calls.leave(frame, frame.method.gives() ? frame.pop() : null);
            default -> { }
        }
    }

    /** Branches when {@code go}; {@code line} is where the frame already stands, one past the branch. */
    private void jumpIf(final Frame frame, final int line, final boolean go) {
        if (go) {
            frame.at = frame.method.jump(line - 1);
        }
    }

    private void jumpCompare(final Frame frame, final Opcode opcode, final int line) {
        final Object right = frame.pop();
        final Object left = frame.pop();
        final boolean go = switch (opcode) {
            case BEQ -> this.values.same(left, right);
            case BNE -> !this.values.same(left, right);
            case BLT -> Numbers.compare(left, right) < 0;
            case BLE -> Numbers.compare(left, right) <= 0;
            case BGT -> Numbers.compare(left, right) > 0;
            default -> Numbers.compare(left, right) >= 0;
        };
        this.jumpIf(frame, line, go);
    }

    private void compare(final Frame frame, final Opcode opcode) {
        final Object right = frame.pop();
        final Object left = frame.pop();
        frame.push(switch (opcode) {
            case CEQ -> this.values.same(left, right);
            case CLT -> Numbers.compare(left, right) < 0;
            default -> Numbers.compare(left, right) > 0;
        });
    }

    private void arithmetic(final Frame frame, final Opcode opcode, final int line) {
        final Object right = frame.pop();
        final Object left = frame.pop();
        frame.push(Numbers.apply(opcode, left, right, line));
    }

    /**
     * Takes the lock of the object on top of the stack.
     *
     * <p>A thread that already holds it takes it once more, and has to let go as many times. One that finds it held
     * by another leaves the object where it is and waits, to ask again when it is free.
     */
    private void enterMonitor(final ProgramThread thread, final Frame frame, final int line) {
        final Object target = this.heap.alive(frame.peek(), line);
        if (this.locks.enter(thread, target)) {
            frame.pop();
            return;
        }
        frame.at--;
        this.scheduler.await(thread, new IWait.Lock(target));
    }

    private void exitMonitor(final ProgramThread thread, final Frame frame, final int line) {
        final Object target = this.heap.alive(frame.pop(), line);
        if (!this.locks.exit(thread, target)) {
            throw new Halt(Halt.Reason.NOT_LOCKED, line, "this thread is letting go of a lock it does not hold");
        }
    }
}
