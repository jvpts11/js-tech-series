/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.cannon.asm.lua;

/**
 * The few things a translated program cannot be written without: whole division, a field of an object, an
 * array, a cast, and the rest of what the runtime here does between two instructions.
 *
 * <p>They are written out once at the head of every translated program. Each is a handful of lines put
 * together here rather than kept as text anywhere, because they are part of the translation: the machine
 * on the other side has no whole numbers and no arrays, and what it does have has to be made to behave
 * like what a program written here expects.
 */
final class LuaPrimitives {

    private LuaPrimitives() {
    }

    /** Writes them all, in the order they need each other. */
    static void write(final StringBuilder out) {
        whole(out);
        arithmetic(out);
        objects(out);
        arrays(out);
        kinds(out);
        collections(out);
        handlers(out);
        waiting(out);
        gateway(out);
    }

    /*
     * Our own world, from over there: a Gateway is a peripheral on that computer's network, and it is
     * what every question about our machines and our network goes through. It is looked for once.
     */
    private static void gateway(final StringBuilder out) {
        out.append("local _found = nil\n");
        out.append("local function _gateway()\n");
        out.append("  if _found ~= nil then return _found end\n");
        out.append("  if peripheral == nil then error(\"this computer has no peripherals\", 0) end\n");
        out.append("  for _, name in ipairs(peripheral.getNames()) do\n");
        out.append("    if peripheral.getType(name) == \"jsc_gateway\" then\n");
        out.append("      _found = name\n");
        out.append("      return _found\n");
        out.append("    end\n");
        out.append("  end\n");
        out.append("  error(\"there is no Gateway this computer can reach\", 0)\n");
        out.append("end\n");
        /*
         * The same question a program standing on one of our machines asks its own machine: the name of
         * the thing, the name of the member, and what it takes. The Gateway answers with our prices and
         * our rules, so a program that moved over there is held to exactly what it was held to here.
         */
        out.append("local function _ask(owner, member, ...)\n");
        out.append("  return peripheral.call(_gateway(), \"ask\", owner, member, ...)\n");
        out.append("end\n");
    }

    /** What a field or a place in an array holds before anything is put in it. */
    static String startingValue(final String type) {
        return switch (type) {
            case "int", "long", "float", "double", "char" -> "0";
            case "bool" -> "false";
            default -> "nil";
        };
    }

    /*
     * Whole numbers. The other side counts in one kind of number only, so a division that was between
     * whole numbers here has to throw the fraction away there, towards zero, as it does here.
     */
    private static void whole(final StringBuilder out) {
        out.append("local function _int(x)\n");
        out.append("  if x == nil then return 0 end\n");
        out.append("  if x >= 0 then return math.floor(x) end\n");
        out.append("  return -math.floor(-x)\n");
        out.append("end\n");
        /* A real number stays a real one even when nothing is left after the point, as it does here. */
        out.append("local function _real(x)\n");
        out.append("  if x == nil then return 0.0 end\n");
        out.append("  return x + 0.0\n");
        out.append("end\n");
        out.append("local function _quotient(a, b)\n");
        out.append("  if b == 0 then error(\"divided by zero\", 0) end\n");
        out.append("  return _int(a / b)\n");
        out.append("end\n");
        out.append("local function _wholerest(a, b)\n");
        out.append("  if b == 0 then error(\"took the remainder of a division by zero\", 0) end\n");
        out.append("  return a - _quotient(a, b) * b\n");
        out.append("end\n");
        out.append("local function _divide(a, b)\n");
        out.append("  if b == 0 then error(\"divided by zero\", 0) end\n");
        out.append("  return a / b\n");
        out.append("end\n");
        out.append("local function _rest(a, b)\n");
        out.append("  if b == 0 then error(\"took the remainder of a division by zero\", 0) end\n");
        out.append("  return a - _int(a / b) * b\n");
        out.append("end\n");
    }

    /* What is done to two values at once: on yes and no it is logic, on numbers it is the bits. */
    private static void arithmetic(final StringBuilder out) {
        truth(out);
        out.append("local _bits = bit32 or bit\n");
        out.append("local function _and(a, b)\n");
        out.append("  if type(a) == \"boolean\" then return a and b end\n");
        out.append("  return _bits.band(a, b)\n");
        out.append("end\n");
        out.append("local function _or(a, b)\n");
        out.append("  if type(a) == \"boolean\" then return a or b end\n");
        out.append("  return _bits.bor(a, b)\n");
        out.append("end\n");
        out.append("local function _xor(a, b)\n");
        out.append("  if type(a) == \"boolean\" then return a ~= b end\n");
        out.append("  return _bits.bxor(a, b)\n");
        out.append("end\n");
        out.append("local function _shift(a, b, left)\n");
        out.append("  if left then return _bits.lshift(a, b) end\n");
        out.append("  return _bits.arshift(a, b)\n");
        out.append("end\n");
        out.append("local function _not(x)\n");
        out.append("  if type(x) == \"boolean\" then return not x end\n");
        out.append("  return _bits.bnot(x)\n");
        out.append("end\n");
    }

    /*
     * Whether something counts as true, and whether two things say the same.
     *
     * <p>The assembly writes a yes and a no as a one and a zero, so a number stands for a yes or a no
     * wherever one was meant, and a comparison between the two has to read what they mean rather than
     * what they are. Two things held by value are the same when everything in them is, field by field,
     * however far down that goes, which is the rule on this side as well.
     */
    private static void truth(final StringBuilder out) {
        out.append("local function _truth(x)\n");
        out.append("  if x == nil then return false end\n");
        out.append("  if type(x) == \"boolean\" then return x end\n");
        out.append("  if type(x) == \"number\" then return x ~= 0 end\n");
        out.append("  return true\n");
        out.append("end\n");
        out.append("local function _eq(P, a, b)\n");
        out.append("  if a == nil or b == nil then return a == b end\n");
        out.append("  if type(a) == \"boolean\" or type(b) == \"boolean\" then\n");
        out.append("    return _truth(a) == _truth(b)\n");
        out.append("  end\n");
        out.append("  if type(a) == \"table\" and type(b) == \"table\"");
        out.append(" and a.type ~= nil and a.type == b.type then\n");
        out.append("    local known = P[a.type]\n");
        out.append("    if known ~= nil and known.byValue then\n");
        out.append("      for k, v in pairs(a) do\n");
        out.append("        if not _eq(P, v, b[k]) then return false end\n");
        out.append("      end\n");
        out.append("      for k in pairs(b) do\n");
        out.append("        if a[k] == nil then return false end\n");
        out.append("      end\n");
        out.append("      return true\n");
        out.append("    end\n");
        out.append("  end\n");
        out.append("  return a == b\n");
        out.append("end\n");
    }

    /* Objects: what they hold, and saying so when there is no object at all. */
    private static void objects(final StringBuilder out) {
        out.append("local function _field(o, name)\n");
        out.append("  if o == nil then error(\"there is no object to read \" .. name .. \" from\", 0) end\n");
        out.append("  if type(o) == \"string\" then\n");
        out.append("    if name == \"Length\" then return #o end\n");
        out.append("    error(\"there is no \" .. name .. \" to read here\", 0)\n");
        out.append("  end\n");
        /* A list and a map say how many they hold, and the number lives where they keep it. */
        out.append("  if name == \"Count\" and (o.type == \"List\" or o.type == \"Map\") then return o.n end\n");
        out.append("  return o[name]\n");
        out.append("end\n");
        out.append("local function _setfield(o, name, x)\n");
        out.append("  if o == nil then error(\"there is no object to write \" .. name .. \" on\", 0) end\n");
        out.append("  o[name] = x\n");
        out.append("end\n");
        out.append("local function _copy(x)\n");
        out.append("  if type(x) ~= \"table\" then return x end\n");
        out.append("  local made = {}\n");
        out.append("  for k, v in pairs(x) do made[k] = v end\n");
        out.append("  return made\n");
        out.append("end\n");
    }

    /* Arrays: a run of places counted from zero, each starting at what its kind starts at. */
    private static void arrays(final StringBuilder out) {
        out.append("local function _array(length, kind)\n");
        out.append("  if length < 0 then error(\"an array cannot have \" .. length .. \" places\", 0) end\n");
        out.append("  local start = nil\n");
        out.append("  if kind == \"bool\" then start = false end\n");
        out.append("  if kind == \"int\" or kind == \"long\" or kind == \"float\"");
        out.append(" or kind == \"double\" or kind == \"char\" then start = 0 end\n");
        out.append("  local made = { n = length, v = {} }\n");
        out.append("  for i = 1, length do made.v[i] = start end\n");
        out.append("  return made\n");
        out.append("end\n");
        out.append("local function _element(a, i)\n");
        out.append("  if a == nil then error(\"there is no array here\", 0) end\n");
        out.append("  if i < 0 or i >= a.n then error(\"place \" .. i .. \" is outside the array\", 0) end\n");
        out.append("  return a.v[i + 1]\n");
        out.append("end\n");
        out.append("local function _setelement(a, i, x)\n");
        out.append("  if a == nil then error(\"there is no array here\", 0) end\n");
        out.append("  if i < 0 or i >= a.n then error(\"place \" .. i .. \" is outside the array\", 0) end\n");
        out.append("  a.v[i + 1] = x\n");
        out.append("end\n");
    }

    /* What kind of thing something is, which a cast asks and a test asks more politely. */
    private static void kinds(final StringBuilder out) {
        /* A name is another one when it is it, or when anything it stands on is, classes and the rest. */
        out.append("local function _isa(P, name, kind)\n");
        out.append("  if name == kind then return true end\n");
        out.append("  local known = P[name]\n");
        out.append("  if known == nil then return false end\n");
        out.append("  for i = 1, #known.bases do\n");
        out.append("    if _isa(P, known.bases[i], kind) then return true end\n");
        out.append("  end\n");
        out.append("  return false\n");
        out.append("end\n");
        out.append("local function _is(P, x, kind)\n");
        out.append("  if type(x) ~= \"table\" or x.type == nil then return false end\n");
        out.append("  return _isa(P, x.type, kind)\n");
        out.append("end\n");
        /*
         * Which method runs is the object's to say, not the name's: a call written on what a program
         * holds reaches the method of the thing it is actually holding, looking up what that stands on.
         */
        out.append("local function _method(P, owner, key, self)\n");
        out.append("  if type(self) == \"table\" and self.type ~= nil then\n");
        out.append("    local at = self.type\n");
        out.append("    while at ~= nil do\n");
        out.append("      local known = P[at]\n");
        out.append("      if known == nil then break end\n");
        out.append("      local own = known.methods[key]\n");
        out.append("      if own ~= nil then return own end\n");
        out.append("      at = known.base\n");
        out.append("    end\n");
        out.append("  end\n");
        out.append("  local named = P[owner]\n");
        out.append("  if named == nil then error(\"there is no \" .. owner .. \" here\", 0) end\n");
        out.append("  local fn = named.methods[key]\n");
        out.append("  if fn == nil then error(\"there is no \" .. key .. \" to call\", 0) end\n");
        out.append("  return fn\n");
        out.append("end\n");
        out.append("local function _cast(P, x, kind)\n");
        out.append("  if x == nil or _is(P, x, kind) then return x end\n");
        out.append("  error(\"this is not a \" .. kind, 0)\n");
        out.append("end\n");
    }

    /* The lists and maps the language brings with it, as tables of the other side. */
    private static void collections(final StringBuilder out) {
        out.append("local function _list()\n");
        out.append("  return { type = \"List\", n = 0, v = {} }\n");
        out.append("end\n");
        out.append("local function _listadd(l, x)\n");
        out.append("  l.n = l.n + 1\n");
        out.append("  l.v[l.n] = x\n");
        out.append("end\n");
        out.append("local function _listat(l, i)\n");
        out.append("  if i < 0 or i >= l.n then error(\"place \" .. i .. \" is outside the list\", 0) end\n");
        out.append("  return l.v[i + 1]\n");
        out.append("end\n");
        out.append("local function _listput(l, i, x)\n");
        out.append("  if i < 0 or i >= l.n then error(\"place \" .. i .. \" is outside the list\", 0) end\n");
        out.append("  l.v[i + 1] = x\n");
        out.append("end\n");
        out.append("local function _listremove(l, i)\n");
        out.append("  table.remove(l.v, i + 1)\n");
        out.append("  l.n = l.n - 1\n");
        out.append("end\n");
        out.append("local function _map()\n");
        out.append("  return { type = \"Map\", n = 0, v = {} }\n");
        out.append("end\n");
        out.append("local function _mapput(m, k, x)\n");
        out.append("  if m.v[k] == nil then m.n = m.n + 1 end\n");
        out.append("  m.v[k] = x\n");
        out.append("end\n");
        out.append("local function _mapat(m, k)\n");
        out.append("  return m.v[k]\n");
        out.append("end\n");
        out.append("local function _maptake(m, k)\n");
        out.append("  if m.v[k] ~= nil then m.n = m.n - 1 end\n");
        out.append("  m.v[k] = nil\n");
        out.append("end\n");
    }

    /*
     * A method handed over without brackets, and what calling one of those means.
     *
     * <p>One of these can hold a run of methods rather than one, and then all of them are called, in the
     * order they were joined; only the last one's answer is the answer, which is the rule here as well.
     */
    private static void handlers(final StringBuilder out) {
        out.append("local function _handler(target, owner, method)\n");
        out.append("  return { chain = { { target = target, owner = owner, method = method } } }\n");
        out.append("end\n");
        out.append("local function _invoke(P, h, ...)\n");
        out.append("  if h == nil or h.chain == nil or #h.chain == 0 then\n");
        out.append("    error(\"there is no handler to call\", 0)\n");
        out.append("  end\n");
        out.append("  local answer = nil\n");
        out.append("  for i = 1, #h.chain do\n");
        out.append("    local one = h.chain[i]\n");
        out.append("    local known = P[one.owner]\n");
        out.append("    if known == nil then error(\"there is no \" .. one.owner .. \" here\", 0) end\n");
        out.append("    local fn = known.methods[one.method]\n");
        out.append("    if fn == nil then error(\"there is no \" .. one.method .. \" to call\", 0) end\n");
        out.append("    if one.target ~= nil then\n");
        out.append("      answer = fn(one.target, ...)\n");
        out.append("    else\n");
        out.append("      answer = fn(...)\n");
        out.append("    end\n");
        out.append("  end\n");
        out.append("  return answer\n");
        out.append("end\n");
        out.append("local function _combine(a, b)\n");
        out.append("  if a == nil then return b end\n");
        out.append("  if b == nil then return a end\n");
        out.append("  local chain = {}\n");
        out.append("  for i = 1, #a.chain do chain[#chain + 1] = a.chain[i] end\n");
        out.append("  for i = 1, #b.chain do chain[#chain + 1] = b.chain[i] end\n");
        out.append("  return { chain = chain }\n");
        out.append("end\n");
        /* Parting takes the last one that matches, which is how a listener removes only what it added. */
        out.append("local function _part(a, b)\n");
        out.append("  if a == nil or b == nil then return a end\n");
        out.append("  local drop = b.chain[1]\n");
        out.append("  local chain = {}\n");
        out.append("  for i = 1, #a.chain do chain[i] = a.chain[i] end\n");
        out.append("  for i = #chain, 1, -1 do\n");
        out.append("    local one = chain[i]\n");
        out.append("    if one.owner == drop.owner and one.method == drop.method");
        out.append(" and one.target == drop.target then\n");
        out.append("      table.remove(chain, i)\n");
        out.append("      break\n");
        out.append("    end\n");
        out.append("  end\n");
        out.append("  if #chain == 0 then return nil end\n");
        out.append("  return { chain = chain }\n");
        out.append("end\n");
    }

    /* Waiting a tick, which is what a program that stays up does between one tick and the next. */
    private static void waiting(final StringBuilder out) {
        out.append("local function _wait()\n");
        out.append("  if os ~= nil and os.sleep ~= nil then os.sleep(0.05) end\n");
        out.append("end\n");
        /*
         * Stopping. A program that says it is done is done wherever it is standing, from as deep inside
         * itself as it likes, so the way out is a raise the start of the program catches and nobody else
         * does: the thing raised is a table nothing else can be, which is what makes it recognisable.
         */
        out.append("local _over = { \"the program stopped itself\" }\n");
        out.append("local _code = 0\n");
        out.append("local function _exit(code)\n");
        out.append("  _code = code or 0\n");
        out.append("  error(_over, 0)\n");
        out.append("end\n");
        /* A handler for messages sent to the program, kept as it is here; nothing here sends one. */
        out.append("local _told = nil\n");
        out.append("local function _onmessage(h)\n");
        out.append("  _told = h\n");
        out.append("end\n");
    }
}
