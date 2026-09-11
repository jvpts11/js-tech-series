/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.cannon.lua;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.jstech.computers.cannon.Diagnostic;
import dev.jstech.computers.cannon.DiagnosticBag;
import dev.jstech.computers.cannon.SourceFile;
import dev.jstech.computers.cannon.asm.AsmProgram;
import dev.jstech.computers.cannon.asm.AsmReader;
import dev.jstech.computers.cannon.run.IHost;
import dev.jstech.computers.cannon.run.Loaded;
import dev.jstech.computers.cannon.run.Process;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Lua programs compiled to the assembly and run on the runtime, checked by what they print.
 *
 * <p>Every program goes through the listing and back, as one started on a machine does, so what is
 * checked is what a player gets.
 */
class LuaExecutionTest {

    private static final long ROOM = 1024L * 1024;
    private static final int PLENTY = 1_000_000;
    private static final int ROUNDS = 200;

    static Loaded load(final String source) {
        final LuaCompiler.Result built = LuaCompiler.compile(new SourceFile("test.lua", source));
        assertTrue(built.ok(), () -> String.join("\n", built.lines()));
        final DiagnosticBag bag = new DiagnosticBag("test.asm");
        final AsmProgram program = new AsmReader(built.assembly(), bag).read();
        assertFalse(bag.hasErrors(), () -> String.join("\n",
                bag.sorted().stream().map(Diagnostic::format).toList()) + "\n" + built.assembly());
        return Loaded.of(program);
    }

    static Process start(final Loaded program, final long room) {
        final Process process = new Process(program, room, IHost.still());
        process.beginStatic(program.entryPoint(), "Main");
        return process;
    }

    static Process run(final String source) {
        final Process process = start(load(source), ROOM);
        for (int i = 0; i < ROUNDS && process.state() == Process.State.RUNNING; i++) {
            process.step(PLENTY);
        }
        return process;
    }

    /** What the program printed, having finished without an error. */
    static List<String> output(final String source) {
        final Process process = run(source);
        assertEquals(Process.State.FINISHED, process.state(),
                () -> process.message() + "\n" + String.join("\n", process.console()));
        return process.console();
    }

    @Test
    void print_separatesItsArgumentsWithTabs() {
        assertEquals(List.of("hello\tworld\t1\tnil\ttrue"), output("print('hello', \"world\", 1, nil, true)"));
    }

    @Test
    void arithmetic_keepsWholeNumbersWholeAndDividesToReals() {
        assertEquals(List.of("7", "2.5", "2", "1", "8.0", "-3", "3.0", "1e+100"),
                output("""
                        print(3 + 4)
                        print(5 / 2)
                        print(5 // 2)
                        print(-5 % 3)
                        print(2 ^ 3)
                        print(-3)
                        print(1.5 * 2)
                        print(1e100)
                        """));
    }

    @Test
    void strings_concatenateNumbersAndCompareByCharacter() {
        assertEquals(List.of("a1b2.5", "true", "false", "3"), output("""
                print("a" .. 1 .. "b" .. 2.5)
                print("abc" < "abd")
                print("b" < "a")
                print(#"abc")
                """));
    }

    @Test
    void locals_shadowAndGlobalsAreShared() {
        assertEquals(List.of("2", "1", "10"), output("""
                local x = 1
                do
                  local x = 2
                  print(x)
                end
                print(x)
                function setg() g = 10 end
                setg()
                print(g)
                """));
    }

    @Test
    void closures_keepTheirOwnVariableForEveryTurnOfALoop() {
        assertEquals(List.of("1 2 3"), output("""
                local fns = {}
                for i = 1, 3 do
                  fns[i] = function() return i end
                end
                print(fns[1]() .. " " .. fns[2]() .. " " .. fns[3]())
                """));
    }

    @Test
    void closures_shareOneVariableBetweenTwoFunctions() {
        assertEquals(List.of("1", "2", "12"), output("""
                local function counter()
                  local n = 0
                  local function up() n = n + 1; return n end
                  local function add(k) n = n + k; return n end
                  return up, add
                end
                local up, add = counter()
                print(up())
                print(up())
                print(add(10))
                """));
    }

    @Test
    void closures_reachThroughMoreThanOneFunction() {
        assertEquals(List.of("6"), output("""
                local a = 1
                local function outer()
                  local b = 2
                  return function()
                    local c = 3
                    return function() return a + b + c end
                  end
                end
                print(outer()()())
                """));
    }

    @Test
    void recursion_throughALocalFunction() {
        assertEquals(List.of("120", "55"), output("""
                local function fact(n) if n <= 1 then return 1 end return n * fact(n - 1) end
                print(fact(5))
                function fib(n) if n < 2 then return n end return fib(n - 1) + fib(n - 2) end
                print(fib(10))
                """));
    }

    @Test
    void varargs_countAndSelect() {
        assertEquals(List.of("3", "b\tc", "0", "a\tnil"), output("""
                local function count(...) return select('#', ...) end
                print(count(1, nil, 3))
                print(select(2, "a", "b", "c"))
                print(count())
                local function pair(...) local x, y = ... return x, y end
                print(pair("a"))
                """));
    }

    @Test
    void multipleResults_spreadOnlyAtTheEndOfAList() {
        assertEquals(List.of("1\t2\t3", "1\t10", "1", "3"), output("""
                local function three() return 1, 2, 3 end
                print(three())
                print(three(), 10)
                print((three()))
                local t = {three()}
                print(#t)
                """));
    }

    @Test
    void multipleAssignment_swapsAndPadsWithNil() {
        assertEquals(List.of("2\t1", "1\t2\tnil"), output("""
                local a, b = 1, 2
                a, b = b, a
                print(a, b)
                local x, y, z = 1, 2
                print(x, y, z)
                """));
    }

    @Test
    void tables_constructorsLengthAndFields() {
        assertEquals(List.of("3", "10", "x", "4", "nil"), output("""
                local t = {10, 20, 30, name = "x", ["k" .. 1] = 4}
                print(#t)
                print(t[1])
                print(t.name)
                print(t.k1)
                print(t.missing)
                """));
    }

    @Test
    void pairs_walksEveryKeyAndIpairsStopsAtTheFirstHole() {
        assertEquals(List.of("a=1", "b=2", "c=3", "1:x", "2:y"), output("""
                local t = {}
                t.a = 1; t.b = 2; t.c = 3
                for k, v in pairs(t) do print(k .. "=" .. v) end
                local list = {"x", "y", nil, "z"}
                for i, v in ipairs(list) do print(i .. ":" .. v) end
                """));
    }

    @Test
    void numericFor_countsDownAndInReals() {
        assertEquals(List.of("3 2 1 ", "0.0 0.5 1.0 "), output("""
                local s = ""
                for i = 3, 1, -1 do s = s .. i .. " " end
                print(s)
                s = ""
                for x = 0, 1, 0.5 do s = s .. x .. " " end
                print(s)
                """));
    }

    @Test
    void loops_whileBreakAndRepeatSeeingItsOwnLocals() {
        assertEquals(List.of("5", "3"), output("""
                local i = 0
                while true do
                  i = i + 1
                  if i == 5 then break end
                end
                print(i)
                local n = 0
                repeat
                  local done = n >= 2
                  n = n + 1
                until done
                print(n)
                """));
    }

    @Test
    void logicalOperators_giveOneOfTheirOperands() {
        assertEquals(List.of("2", "nil", "default", "false", "true"), output("""
                print(1 and 2)
                print(nil and 2)
                print(nil or "default")
                print(not 1)
                print(not nil)
                """));
    }

    @Test
    void metatables_answerIndexNewindexCallAndTostring() {
        assertEquals(List.of("fallback", "set k=v", "called with 7", "<point>"), output("""
                local base = {greet = "fallback"}
                local t = setmetatable({}, {__index = base})
                print(t.greet)
                local log = setmetatable({}, {__newindex = function(tbl, k, v) print("set " .. k .. "=" .. v) end})
                log.k = "v"
                local f = setmetatable({}, {__call = function(self, x) return "called with " .. x end})
                print(f(7))
                local p = setmetatable({}, {__tostring = function() return "<point>" end})
                print(p)
                """));
    }

    @Test
    void metatables_answerArithmeticComparisonAndLength() {
        assertEquals(List.of("4\t6", "true", "true", "false", "42", "ab"), output("""
                local V = {}
                V.__index = V
                V.__add = function(a, b) return setmetatable({x = a.x + b.x, y = a.y + b.y}, V) end
                V.__eq = function(a, b) return a.x == b.x and a.y == b.y end
                V.__lt = function(a, b) return a.x < b.x end
                V.__len = function() return 42 end
                V.__concat = function(a, b) return "ab" end
                local function v(x, y) return setmetatable({x = x, y = y}, V) end
                local s = v(1, 2) + v(3, 4)
                print(s.x, s.y)
                print(v(1, 1) == v(1, 1))
                print(v(1, 0) < v(2, 0))
                print(v(3, 0) <= v(2, 0))
                print(#v(0, 0))
                print(v(0, 0) .. v(0, 0))
                """));
    }

    @Test
    void classes_inheritThroughIndexChains() {
        assertEquals(List.of("Rex says woof", "Tom makes a sound"), output("""
                local Animal = {}
                Animal.__index = Animal
                function Animal.new(name) return setmetatable({name = name}, Animal) end
                function Animal:speak() return self.name .. " makes a sound" end
                local Dog = setmetatable({}, {__index = Animal})
                Dog.__index = Dog
                function Dog.new(name) local d = Animal.new(name); return setmetatable(d, Dog) end
                function Dog:speak() return self.name .. " says woof" end
                print(Dog.new("Rex"):speak())
                print(Animal.new("Tom"):speak())
                """));
    }

    @Test
    void pcall_catchesAnErrorAndGivesItsValue() {
        assertEquals(List.of("false\ttest.lua:1: boom", "false\ttable", "true\t3", "after"), output("""
                local ok, err = pcall(function() error("boom") end)
                print(ok, err)
                local ok2, e2 = pcall(error, {code = 1})
                print(ok2, type(e2))
                print(pcall(function(a, b) return a + b end, 1, 2))
                print("after")
                """));
    }

    @Test
    void errors_nameTheirPlaceAndWhatWasReachedFor() {
        assertEquals(List.of(
                "test.lua:2: attempt to call a nil value (global 'missing')",
                "test.lua:3: attempt to index a nil value (upvalue 't')",
                "test.lua:4: attempt to perform arithmetic on a string value",
                "test.lua:5: attempt to compare number with nil"), output("""
                local t
                print(select(2, pcall(function() missing() end)))
                print(select(2, pcall(function() return t.x end)))
                print(select(2, pcall(function() return "a" + 1 end)))
                print(select(2, pcall(function() return 1 < nil end)))
                """));
    }

    @Test
    void xpcall_handsTheErrorToItsHandler() {
        assertEquals(List.of("false\thandled: test.lua:1: bad"), output("""
                print(xpcall(function() error("bad") end, function(e) return "handled: " .. e end))
                """));
    }

    @Test
    void error_withoutPcallHaltsTheProgramWithItsMessage() {
        final Process process = run("print('before')\nerror('stop here')\nprint('never')");
        assertEquals(Process.State.HALTED, process.state());
        assertEquals("test.lua:2: stop here", process.message());
        assertEquals("before", process.console().getFirst());
    }

    @Test
    void coroutines_passValuesBothWays() {
        assertEquals(List.of("got 1", "yielded 10", "got 2", "yielded 20", "true\tdone", "dead"), output("""
                local co = coroutine.create(function(a)
                  print("got " .. a)
                  local b = coroutine.yield(a * 10)
                  print("got " .. b)
                  coroutine.yield(b * 10)
                  return "done"
                end)
                local _, v = coroutine.resume(co, 1)
                print("yielded " .. v)
                _, v = coroutine.resume(co, 2)
                print("yielded " .. v)
                print(coroutine.resume(co))
                print(coroutine.status(co))
                """));
    }

    @Test
    void coroutineWrap_makesAGenerator() {
        assertEquals(List.of("1 2 3 "), output("""
                local function range(n)
                  return coroutine.wrap(function() for i = 1, n do coroutine.yield(i) end end)
                end
                local s = ""
                for i in range(3) do s = s .. i .. " " end
                print(s)
                """));
    }

    @Test
    void coroutines_reportAnErrorToWhoeverResumed() {
        assertEquals(List.of("false\ttest.lua:1: inside", "false\tcannot resume dead coroutine"), output("""
                local co = coroutine.create(function() error("inside") end)
                print(coroutine.resume(co))
                print(coroutine.resume(co))
                """));
    }

    @Test
    void tableSort_ordersWithAndWithoutAComparator() {
        assertEquals(List.of("1 2 3 5 8", "8 5 3 2 1", "a b c"), output("""
                local t = {5, 3, 8, 1, 2}
                table.sort(t)
                print(table.concat(t, " "))
                table.sort(t, function(a, b) return a > b end)
                print(table.concat(t, " "))
                local s = {"c", "a", "b"}
                table.sort(s)
                print(table.concat(s, " "))
                """));
    }

    @Test
    void tableLibrary_insertsRemovesAndUnpacks() {
        assertEquals(List.of("a b c", "x a b c", "c", "x a b", "1\t2"), output("""
                local t = {"a", "b"}
                table.insert(t, "c")
                print(table.concat(t, " "))
                table.insert(t, 1, "x")
                print(table.concat(t, " "))
                print(table.remove(t))
                print(table.concat(t, " "))
                print(table.unpack({1, 2}))
                """));
    }

    @Test
    void stringLibrary_findsMatchesAndFormats() {
        assertEquals(List.of("3\t5", "hello", "12\tab", "hxllx wxrld\t3", "[  42] 3.14 ff", "x=1, y=2", "HELLO",
                "ell", "aaa"), output("""
                print(string.find("abcde", "cde"))
                print(string.match("  hello  ", "%a+"))
                print(string.match("ab12", "(%d+)"), string.match("ab12", "^(%a+)"))
                print(string.gsub("hello world", "[eo]", "x"))
                print(string.format("[%4d] %.2f %x", 42, 3.14159, 255))
                print(("x=%d, y=%d"):format(1, 2))
                print(("hello"):upper())
                print(("hello"):sub(2, -2))
                print(("a"):rep(3))
                """));
    }

    @Test
    void gmatch_andGsubWithAFunction() {
        assertEquals(List.of("one", "two", "three", "ONE-TWO\t2"), output("""
                for w in string.gmatch("one two three", "%a+") do print(w) end
                print(string.gsub("one-two", "%a+", function(w) return w:upper() end))
                """));
    }

    @Test
    void tostringAndTonumber_convertBothWays() {
        assertEquals(List.of("10", "nil", "255", "3.5", "number", "string"), output("""
                print(tonumber("10"))
                print(tonumber("ten"))
                print(tonumber("ff", 16))
                print(tonumber(" 3.5 "))
                print(type(tonumber("1")))
                print(type(tostring(1)))
                """));
    }

    @Test
    void ioRead_waitsForALineAndCarriesOnWithIt() {
        final Process process = start(load("""
                io.write("name? ")
                local name = io.read()
                print("hi " .. name)
                """), ROOM);
        process.step(PLENTY);
        assertEquals(Process.State.PARKED, process.state());
        assertTrue(process.waitingForInput());
        assertEquals(List.of("name? "), process.console());
        process.offerInput("Ana");
        process.step(PLENTY);
        assertEquals(Process.State.FINISHED, process.state(), process::message);
        assertEquals(List.of("name? ", "hi Ana"), process.console());
    }

    @Test
    void collector_keepsAProgramThatMakesGarbageWithinASmallHeap() {
        final Process process = start(load("""
                local keep = {}
                for i = 1, 20000 do
                  local t = {i, tostring(i), {nested = i}}
                  if i % 1000 == 0 then keep[#keep + 1] = t end
                end
                print(#keep, keep[20][2])
                """), 64L * 1024);
        for (int i = 0; i < 1000 && process.state() == Process.State.RUNNING; i++) {
            process.step(PLENTY);
        }
        assertEquals(Process.State.FINISHED, process.state(), process::message);
        assertEquals(List.of("20\t20000"), process.console());
        assertTrue(process.heap().used() < 64L * 1024, "what is left fits in the heap");
    }

    @Test
    void outOfMemory_isAnErrorPcallCanCatch() {
        final Process process = start(load("""
                local ok = pcall(function()
                  local t = {}
                  for i = 1, 1e9 do t[i] = "some text that takes room " .. i end
                end)
                print(ok)
                """), 64L * 1024);
        for (int i = 0; i < 1000 && process.state() == Process.State.RUNNING; i++) {
            process.step(PLENTY);
        }
        assertEquals(Process.State.FINISHED, process.state(), process::message);
        assertEquals(List.of("false"), process.console());
    }

    @Test
    void load_compilesTextIntoAFunction() {
        assertEquals(List.of("42", "7"), output("""
                local f = load("return 1 + ...")
                print(f(41))
                local g = loadstring("local a, b = ... return a * b")
                print(g(7, 1))
                """));
    }

    @Test
    void load_givesNilAndTheComplaintForTextThatDoesNotRead() {
        assertEquals(List.of("nil\t[string \"x = = 1\"]:1: unexpected symbol near '='"), output("""
                print(load("x = = 1"))
                """));
    }

    @Test
    void load_runsTheChunkInTheEnvironmentItIsGiven() {
        assertEquals(List.of("5", "nil\t5"), output("""
                local env = {print = print}
                local f = load("y = 5; print(y)", "=chunk", "t", env)
                f()
                print(y, env.y)
                """));
    }

    @Test
    void error_atLevelTwoBlamesTheCaller() {
        assertEquals(List.of("test.lua:3: bad input"), output("""
                local function check(x) if not x then error("bad input", 2) end end
                local ok, e = pcall(function()
                  check(nil)
                end)
                print(e)
                """));
    }

    @Test
    void numbersAndStrings_convertWhereTheLanguageSaysTheyDo() {
        assertEquals(List.of("11", "true", "one", "1.5", "3", "table", "attempt to perform 'n//0'"), output("""
                print("10" + 1)
                print(1 == 1.0)
                local t = {}
                t[1.0] = "one"
                print(t[1])
                print(1.5 .. "")
                print(math.floor(3.7))
                print(type({}))
                print(select(2, pcall(function() return 1 // 0 end)):match("attempt.*"))
                """));
    }

    @Test
    void aProgramOfTheKindPeopleWrite_runsToTheEnd() {
        assertEquals(List.of(
                "Item              Count  Bar",
                "diamond              64  ##########",
                "iron_ingot           40  ######",
                "gold_ingot           12  ##",
                "total 116 in 3 kinds, low: gold_ingot"), output("""
                local Inventory = {}
                Inventory.__index = Inventory

                function Inventory.new() return setmetatable({items = {}}, Inventory) end

                function Inventory:add(name, count)
                  self.items[name] = (self.items[name] or 0) + count
                  return self
                end

                function Inventory:sorted()
                  local list = {}
                  for name, count in pairs(self.items) do list[#list + 1] = {name = name, count = count} end
                  table.sort(list, function(a, b) return a.count > b.count end)
                  return list
                end

                local function report(inv, ...)
                  local low = {}
                  local threshold = select(1, ...) or 16
                  print(string.format("%-16s %6s  %s", "Item", "Count", "Bar"))
                  local total, kinds = 0, 0
                  for _, row in ipairs(inv:sorted()) do
                    print(string.format("%-16s %6d  %s", row.name, row.count, ("#"):rep(math.floor(row.count / 6))))
                    total = total + row.count
                    kinds = kinds + 1
                    if row.count < threshold then low[#low + 1] = row.name end
                  end
                  print(("total %d in %d kinds, low: %s"):format(total, kinds, table.concat(low, ", ")))
                end

                local inv = Inventory.new():add("iron_ingot", 30):add("diamond", 64):add("gold_ingot", 12)
                inv:add("iron_ingot", 10)
                report(inv, 16)
                """));
    }

    @Test
    void scriptArgs_areTheChunksVarargs() {
        final Process process = start(load("print(select('#', ...), ...)"), ROOM);
        process.setArgs(List.of("a", "b"));
        process.step(PLENTY);
        assertEquals(List.of("2\ta\tb"), process.console(), process::message);
    }
}
