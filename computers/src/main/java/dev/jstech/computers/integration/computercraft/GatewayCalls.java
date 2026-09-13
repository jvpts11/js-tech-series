/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.integration.computercraft;

import dan200.computercraft.api.lua.IArguments;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.api.lua.MethodResult;
import dan200.computercraft.api.lua.ObjectArguments;
import dan200.computercraft.api.peripheral.IPeripheral;
import dev.jstech.computers.gateway.GatewayValues;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * Calling a ComputerCraft peripheral from our side, and turning what it answers into values a program of
 * ours can hold.
 *
 * <p>A peripheral's methods are the ones it marks for Lua, which is a public thing to ask of it, so this
 * finds them by that mark and calls them. What it cannot reach it says so about rather than guessing: a
 * peripheral that builds its methods as it goes wants a computer of ComputerCraft's own to call it (there
 * is none here), and a method that asks for one of ComputerCraft's own objects wants the same.
 *
 * <p>Numbers cross as Lua has them: a whole one comes back whole, anything else as a real. A table comes
 * back as a list when it is a run from one, and as a map otherwise, which is the same rule the Lua side
 * of this mod uses.
 */
final class GatewayCalls {

    private GatewayCalls() {
    }

    /** The names of the methods a peripheral answers, in the order it declares them. */
    static List<String> methodsOf(final IPeripheral peripheral) {
        final List<String> names = new ArrayList<>();
        for (final Method method : peripheral.getClass().getMethods()) {
            final LuaFunction mark = method.getAnnotation(LuaFunction.class);
            if (mark == null) {
                continue;
            }
            if (mark.value().length == 0) {
                names.add(method.getName());
            } else {
                names.addAll(List.of(mark.value()));
            }
        }
        return names;
    }

    /**
     * Calls one of them and gives back what it answered, as one value or a list of them.
     *
     * @throws LuaException when there is no such method, when it cannot be called from here, or when the
     *                      peripheral itself refused
     */
    static Object call(final IPeripheral peripheral, final String name, final List<Object> arguments)
            throws LuaException {
        final Method found = methodOf(peripheral, name);
        if (found == null) {
            throw new LuaException("no such method " + name + " on " + peripheral.getType());
        }
        final Object[] passed = bind(found, arguments);
        if (passed == null) {
            throw new LuaException(name + " cannot be called from here");
        }
        try {
            return GatewayValues.fromLua(answerOf(found.invoke(peripheral, passed)));
        } catch (final IllegalAccessException | IllegalArgumentException wrong) {
            throw new LuaException(name + " cannot be called from here");
        } catch (final InvocationTargetException failed) {
            final Throwable cause = failed.getCause();
            if (cause instanceof LuaException refused) {
                throw refused;
            }
            throw new LuaException(name + " failed: "
                    + (cause == null ? failed.toString() : String.valueOf(cause.getMessage())));
        }
    }

    private static Method methodOf(final IPeripheral peripheral, final String name) {
        for (final Method method : peripheral.getClass().getMethods()) {
            final LuaFunction mark = method.getAnnotation(LuaFunction.class);
            if (mark == null) {
                continue;
            }
            if (mark.value().length == 0 ? method.getName().equals(name) : List.of(mark.value()).contains(name)) {
                return method;
            }
        }
        return null;
    }

    /** What to hand the method, or null when it wants something only ComputerCraft itself can give. */
    private static Object[] bind(final Method method, final List<Object> arguments) {
        final Class<?>[] wants = method.getParameterTypes();
        final Object[] passed = new Object[wants.length];
        int at = 0;
        for (int i = 0; i < wants.length; i++) {
            final Class<?> want = wants[i];
            if (want == IArguments.class) {
                passed[i] = new ObjectArguments(GatewayValues.toLuaAll(
                        arguments.subList(Math.min(at, arguments.size()), arguments.size())));
                at = arguments.size();
                continue;
            }
            final Object given = at < arguments.size() ? arguments.get(at++) : null;
            final Object bound = coerce(want, given);
            if (bound == NOTHING) {
                return null;
            }
            passed[i] = bound;
        }
        return passed;
    }

    /** Stands for an argument that cannot be bound at all, which no value ever is. */
    private static final Object NOTHING = new Object();

    private static Object coerce(final Class<?> want, final Object given) {
        if (want == String.class) {
            return given == null ? null : String.valueOf(given);
        }
        if ((want == int.class || want == Integer.class) && given instanceof Number number) {
            return number.intValue();
        }
        if ((want == long.class || want == Long.class) && given instanceof Number number) {
            return number.longValue();
        }
        if ((want == double.class || want == Double.class) && given instanceof Number number) {
            return number.doubleValue();
        }
        if ((want == boolean.class || want == Boolean.class) && given instanceof Boolean flag) {
            return flag;
        }
        if (want == Object.class) {
            return GatewayValues.toLua(given);
        }
        return NOTHING;
    }

    /** What a method gave back: nothing, one value, or several. */
    private static Object answerOf(final Object returned) {
        if (returned instanceof MethodResult result) {
            final Object[] values = result.getResult();
            return values == null || values.length == 0 ? null : (values.length == 1 ? values[0] : values);
        }
        return returned;
    }

}
