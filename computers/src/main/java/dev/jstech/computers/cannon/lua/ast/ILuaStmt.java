/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.cannon.lua.ast;

import java.util.List;

/** A statement of Lua. */
public sealed interface ILuaStmt extends ILuaNode {

    /** {@code local names = values}. */
    record Local(List<String> names, List<ILuaExpr> values, int line, int column) implements ILuaStmt {

        public Local {
            names = List.copyOf(names);
            values = List.copyOf(values);
        }
    }

    /** {@code targets = values}. */
    record Assign(List<ILuaExpr> targets, List<ILuaExpr> values, int line, int column) implements ILuaStmt {

        public Assign {
            targets = List.copyOf(targets);
            values = List.copyOf(values);
        }
    }

    /** A call on its own, whose values are dropped. */
    record CallStmt(ILuaExpr call, int line, int column) implements ILuaStmt {
    }

    /** {@code do ... end}. */
    record Do(LuaBlock body, int line, int column) implements ILuaStmt {
    }

    /** {@code while condition do ... end}. */
    record While(ILuaExpr condition, LuaBlock body, int line, int column) implements ILuaStmt {
    }

    /** {@code repeat ... until condition}: the condition sees the body's locals. */
    record Repeat(LuaBlock body, ILuaExpr condition, int line, int column) implements ILuaStmt {
    }

    /** {@code if ... then ... elseif ... else ... end}. */
    record If(List<Clause> clauses, LuaBlock otherwise, int line, int column) implements ILuaStmt {

        public If {
            clauses = List.copyOf(clauses);
        }
    }

    /** One {@code if} or {@code elseif} arm. */
    record Clause(ILuaExpr condition, LuaBlock body, int line, int column) implements ILuaNode {
    }

    /** {@code for name = start, limit, step do ... end}. */
    record NumericFor(String name, ILuaExpr start, ILuaExpr limit, ILuaExpr step, LuaBlock body,
                      int line, int column) implements ILuaStmt {
    }

    /** {@code for names in values do ... end}. */
    record GenericFor(List<String> names, List<ILuaExpr> values, LuaBlock body, int line, int column)
            implements ILuaStmt {

        public GenericFor {
            names = List.copyOf(names);
            values = List.copyOf(values);
        }
    }

    /**
     * {@code function a.b.c:m(...) ... end}: the path is where the function is stored; a method
     * name adds {@code self} in front of the parameters.
     */
    record FunctionDecl(List<String> path, String method, LuaFunctionBody body, int line, int column)
            implements ILuaStmt {

        public FunctionDecl {
            path = List.copyOf(path);
        }
    }

    /** {@code local function name(...) ... end}: the name is in scope inside the body. */
    record LocalFunction(String name, LuaFunctionBody body, int line, int column) implements ILuaStmt {
    }

    /** {@code return values}. */
    record Return(List<ILuaExpr> values, int line, int column) implements ILuaStmt {

        public Return {
            values = List.copyOf(values);
        }
    }

    /** {@code break}. */
    record Break(int line, int column) implements ILuaStmt {
    }
}
