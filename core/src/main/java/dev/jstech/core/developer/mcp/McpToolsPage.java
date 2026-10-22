/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.developer.mcp;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

/**
 * The page of the MCP servers' tools, written from the tools the mods declared: for each game a server runs in, every
 * tool with the mod that declared it, whether it only reads or acts, whether it is off until the player turns its
 * mod's tools on, what it does, and each argument it takes with its type, its bounds and whether it must be given,
 * with the whole schema under it. The server hands it out at {@link McpGuard#TOOLS}, and {@code /jstech dump mcp}
 * writes it beside the other dumps.
 */
public final class McpToolsPage {

    private static final Gson PRETTY = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final String STYLE = """
            :root{--bg:#f7f6f2;--fg:#1f2328;--dim:#5d6470;--line:#d6d3ca;--panel:#ffffff;--read:#1d6f42;\
            --act:#9a4a00;--off:#6a5d9a;color-scheme:light}
            @media (prefers-color-scheme:dark){:root{--bg:#15171a;--fg:#e6e3dc;--dim:#9aa1ab;--line:#2c3036;\
            --panel:#1c1f23;--read:#6fd39a;--act:#f0a35e;--off:#b3a6e6;color-scheme:dark}}
            body{margin:0;background:var(--bg);color:var(--fg);font:15px/1.55 system-ui,sans-serif}
            main{max-width:60rem;margin:0 auto;padding:1.5rem 1rem 4rem}
            h1{font-size:1.6rem;margin:.2rem 0}h2{margin-top:2.4rem;border-bottom:1px solid var(--line)}
            h3{font-family:ui-monospace,Consolas,monospace;font-size:1.05rem;margin:0}
            section.tool{background:var(--panel);border:1px solid var(--line);padding:.8rem 1rem;margin:.8rem 0}
            .meta{color:var(--dim);font-size:.85rem}.read{color:var(--read)}.act{color:var(--act)}\
            .off{color:var(--off)}
            table{border-collapse:collapse;width:100%;font-size:.9rem}th,td{text-align:left;padding:.25rem .5rem;\
            border-top:1px solid var(--line);vertical-align:top}code,pre{font-family:ui-monospace,Consolas,monospace}
            pre{overflow-x:auto;font-size:.8rem}nav ul{columns:14rem;padding-left:1.2rem}
            """;

    private McpToolsPage() {
    }

    /** The page of the tools declared in this game, on or off as they are now. */
    public static String html() {
        final Map<McpSide, List<McpTools.Declared>> sides = new EnumMap<>(McpSide.class);
        for (final McpSide side : McpSide.values()) {
            sides.put(side, McpTools.declared(side));
        }
        return html(sides, declared -> McpTools.isOn(declared.modid()));
    }

    /** The page of the tools of {@code side} declared in this game, on or off as they are now. */
    public static String html(final McpSide side) {
        return html(Map.of(side, McpTools.declared(side)), declared -> McpTools.isOn(declared.modid()));
    }

    /** The page of {@code sides}' tools, each side's in the order given, {@code on} telling which are on. */
    public static String html(final Map<McpSide, List<McpTools.Declared>> sides,
                              final Predicate<McpTools.Declared> on) {
        final StringBuilder out = new StringBuilder("<!doctype html>\n<html lang=\"en\"><head><meta charset=\"utf-8\">")
                .append("<meta name=\"viewport\" content=\"width=device-width,initial-scale=1\">")
                .append("<title>MCP tools</title><style>").append(STYLE).append("</style></head><body><main>\n")
                .append("<h1>The tools of the MCP servers</h1>\n<p class=\"meta\">Written by the game from the tools ")
                .append("its mods declared. A client pairs at <code>").append(McpGuard.PATH).append("</code> on the ")
                .append("port the developer's settings give; the player is asked, and tools that act need their ")
                .append("leave to act. A mod's tools are off until the player turns them on.</p>\n");
        for (final McpSide side : McpSide.values()) {
            final List<McpTools.Declared> tools = sides.get(side);
            if (tools == null) {
                continue;
            }
            out.append("<h2 id=\"").append(anchor(side)).append("\">").append(sideName(side)).append(" (")
                    .append(tools.size()).append(tools.size() == 1 ? " tool" : " tools").append(")</h2>\n");
            if (tools.isEmpty()) {
                out.append("<p class=\"meta\">None declared in this game.</p>\n");
                continue;
            }
            out.append("<nav><ul>");
            for (final McpTools.Declared declared : tools) {
                out.append("<li><a href=\"#").append(anchor(side)).append('-').append(declared.tool().name())
                        .append("\"><code>").append(declared.tool().name()).append("</code></a></li>");
            }
            out.append("</ul></nav>\n");
            for (final McpTools.Declared declared : tools) {
                tool(out, side, declared, on.test(declared));
            }
        }
        return out.append("</main></body></html>\n").toString();
    }

    /** Each argument of {@code schema}: its name, type, bounds or values, whether it must be given, what it is. */
    public static List<Argument> arguments(final JsonObject schema) {
        final List<Argument> out = new ArrayList<>();
        final JsonObject properties = schema.has("properties") ? schema.getAsJsonObject("properties")
                : new JsonObject();
        final Set<String> required = new HashSet<>();
        if (schema.has("required")) {
            schema.getAsJsonArray("required").forEach(name -> required.add(name.getAsString()));
        }
        for (final Map.Entry<String, JsonElement> property : properties.entrySet()) {
            final JsonObject about = property.getValue().getAsJsonObject();
            final List<String> bounds = new ArrayList<>();
            if (about.has("minimum") && about.has("maximum")) {
                bounds.add(about.get("minimum").getAsString() + " to " + about.get("maximum").getAsString());
            }
            if (about.has("enum")) {
                final List<String> values = new ArrayList<>();
                about.getAsJsonArray("enum").forEach(value -> values.add(value.getAsString()));
                bounds.add("one of " + String.join(", ", values));
            }
            if (about.has("properties")) {
                bounds.add("with " + String.join(", ", about.getAsJsonObject("properties").keySet()));
            }
            out.add(new Argument(property.getKey(), about.has("type") ? about.get("type").getAsString() : "any",
                    String.join("; ", bounds), required.contains(property.getKey()),
                    about.has("description") ? about.get("description").getAsString() : ""));
        }
        return out;
    }

    private static void tool(final StringBuilder out, final McpSide side, final McpTools.Declared declared,
                             final boolean on) {
        final McpTool tool = declared.tool();
        out.append("<section class=\"tool\" id=\"").append(anchor(side)).append('-').append(tool.name())
                .append("\"><h3>").append(tool.name()).append("</h3>\n<p class=\"meta\">")
                .append(escape(declared.modid())).append(" &middot; ");
        if (tool.needs() == McpPermission.READ) {
            out.append("<span class=\"read\">reads</span>");
        } else {
            out.append("<span class=\"act\">acts</span>");
        }
        if (!on) {
            out.append(" &middot; <span class=\"off\">off until the player turns this mod's tools on</span>");
        }
        out.append("</p>\n<p>").append(escape(tool.description())).append("</p>\n");
        final List<Argument> arguments = arguments(tool.inputSchema());
        if (arguments.isEmpty()) {
            out.append("<p class=\"meta\">It takes no arguments.</p>\n");
        } else {
            out.append("<table><tr><th>Argument</th><th>Type</th><th>Given</th><th>What it is</th></tr>\n");
            for (final Argument argument : arguments) {
                out.append("<tr><td><code>").append(escape(argument.name())).append("</code></td><td>")
                        .append(escape(argument.type()));
                if (!argument.bounds().isEmpty()) {
                    out.append(", ").append(escape(argument.bounds()));
                }
                out.append("</td><td>").append(argument.needed() ? "always" : "if wanted").append("</td><td>")
                        .append(escape(argument.about())).append("</td></tr>\n");
            }
            out.append("</table>\n");
        }
        out.append("<details><summary>Its schema</summary><pre>").append(escape(PRETTY.toJson(tool.inputSchema())))
                .append("</pre></details></section>\n");
    }

    private static String sideName(final McpSide side) {
        return switch (side) {
            case GAME -> "In the game, as the player plays";
            case SERVER -> "On the server, in the world";
        };
    }

    private static String anchor(final McpSide side) {
        return side == McpSide.GAME ? "game" : "server";
    }

    private static String escape(final String text) {
        final StringBuilder out = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            final char c = text.charAt(i);
            switch (c) {
                case '<' -> out.append("&lt;");
                case '>' -> out.append("&gt;");
                case '&' -> out.append("&amp;");
                case '"' -> out.append("&quot;");
                default -> out.append(c);
            }
        }
        return out.toString();
    }

    /**
     * One argument of a tool.
     *
     * @param name   its name
     * @param type   its JSON type
     * @param bounds the least and most it may be, the values it may take or the fields it holds; none when free
     * @param needed whether it must be given
     * @param about  what it is, in the tool's words
     */
    public record Argument(String name, String type, String bounds, boolean needed, String about) {
    }
}
