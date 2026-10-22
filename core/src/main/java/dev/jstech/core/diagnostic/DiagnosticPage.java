/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.diagnostic;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The diagnostic file as a page a browser opens, written beside it: each of its sections one that folds away, the
 * lines that warn or say something went wrong marked, and a box that keeps only the lines holding what is typed in
 * it. Every word on the page is the file's own; only its markup, its style and its small script are the page's. With
 * no game types, so a unit test reads the page made from a file.
 */
public final class DiagnosticPage {

    /* How a section's heading is written in the file. */
    private static final String OPENS = "== ";
    private static final String CLOSES = " ==";
    /* Words that mark a line as one to look at. */
    private static final List<String> MARKED = List.of("error", "warn", "stall", "fail", "over budget", "missing");
    private static final String STYLE = """
            :root{--bg:#f6f5f1;--fg:#1e1f22;--dim:#6b6d72;--line:#d8d6cf;--mark:#fff1c7;--panel:#ffffff}
            @media (prefers-color-scheme:dark){:root{--bg:#17181b;--fg:#e4e2dc;--dim:#9a9ca2;--line:#2c2e33;\
            --mark:#4a3d12;--panel:#1f2024}}
            body{margin:0;padding:16px;background:var(--bg);color:var(--fg);font:14px/1.45 system-ui,sans-serif}
            header{display:flex;gap:12px;align-items:center;flex-wrap:wrap;margin-bottom:12px}
            input{flex:1;min-width:200px;padding:6px 8px;border:1px solid var(--line);background:var(--panel);\
            color:var(--fg);font:inherit}
            button{padding:6px 10px;border:1px solid var(--line);background:var(--panel);color:var(--fg);font:inherit}
            details{background:var(--panel);border:1px solid var(--line);margin:0 0 8px}
            summary{cursor:pointer;padding:8px 10px;font-weight:600}
            summary small{color:var(--dim);font-weight:400;margin-left:8px}
            pre{margin:0;padding:0 10px 10px;overflow-x:auto;font:12px/1.5 ui-monospace,Consolas,monospace}
            pre span{display:block}
            pre span.marked{background:var(--mark)}
            pre span.hidden,details.hidden{display:none}
            """;
    private static final String SCRIPT = """
            const box=document.getElementById('find');
            box.addEventListener('input',()=>{const t=box.value.toLowerCase();
            document.querySelectorAll('details').forEach(d=>{let any=false;
            d.querySelectorAll('pre span').forEach(s=>{const k=!t||s.textContent.toLowerCase().includes(t);
            s.classList.toggle('hidden',!k);any=any||k;});d.classList.toggle('hidden',!any);if(t&&any)d.open=true;});});
            document.getElementById('open').addEventListener('click',()=>document.querySelectorAll('details')
            .forEach(d=>d.open=true));
            document.getElementById('shut').addEventListener('click',()=>document.querySelectorAll('details')
            .forEach(d=>d.open=false));
            """;

    private DiagnosticPage() {
    }

    /** The page of {@code file}, the diagnostic file's text, titled {@code title}. */
    public static String html(final String title, final String file) {
        final List<Section> sections = sections(file);
        final StringBuilder out = new StringBuilder(file.length() * 2);
        out.append("<!doctype html><html><head><meta charset=\"utf-8\"><meta name=\"viewport\" ")
                .append("content=\"width=device-width,initial-scale=1\"><title>").append(escape(title))
                .append("</title><style>").append(STYLE).append("</style></head><body><header><strong>")
                .append(escape(title)).append("</strong><input id=\"find\" type=\"search\" placeholder=\"&#8981;\">")
                .append("<button id=\"open\" type=\"button\">+</button><button id=\"shut\" type=\"button\">&minus;")
                .append("</button></header>");
        for (int i = 0; i < sections.size(); i++) {
            final Section section = sections.get(i);
            out.append("<details").append(i == 0 ? " open" : "").append("><summary>").append(escape(section.title()))
                    .append("<small>").append(section.lines().size()).append("</small></summary><pre>");
            for (final String line : section.lines()) {
                out.append(marked(line) ? "<span class=\"marked\">" : "<span>").append(escape(line))
                        .append("</span>");
            }
            out.append("</pre></details>");
        }
        return out.append("<script>").append(SCRIPT).append("</script></body></html>").toString();
    }

    /** The file's sections in order: the lines before the first heading under the file's first line, then each. */
    public static List<Section> sections(final String file) {
        final List<Section> out = new ArrayList<>();
        String title = "";
        List<String> lines = new ArrayList<>();
        for (final String line : file.split("\n", -1)) {
            if (line.startsWith(OPENS) && line.endsWith(CLOSES) && line.length() > OPENS.length() + CLOSES.length()) {
                if (!title.isEmpty() || !lines.isEmpty()) {
                    out.add(new Section(title, trimmed(lines)));
                }
                title = line.substring(OPENS.length(), line.length() - CLOSES.length());
                lines = new ArrayList<>();
            } else if (title.isEmpty() && lines.isEmpty() && !line.isBlank()) {
                title = line;
            } else {
                lines.add(line);
            }
        }
        if (!title.isEmpty() || !lines.isEmpty()) {
            out.add(new Section(title, trimmed(lines)));
        }
        return out;
    }

    /** Whether {@code line} warns or says something went wrong. */
    public static boolean marked(final String line) {
        final String lower = line.toLowerCase(Locale.ROOT);
        return MARKED.stream().anyMatch(lower::contains);
    }

    /* The lines without the blank ones round them. */
    private static List<String> trimmed(final List<String> lines) {
        int from = 0;
        int to = lines.size();
        while (from < to && lines.get(from).isBlank()) {
            from++;
        }
        while (to > from && lines.get(to - 1).isBlank()) {
            to--;
        }
        return List.copyOf(lines.subList(from, to));
    }

    private static String escape(final String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }

    /**
     * A section of the file.
     *
     * @param title its heading
     * @param lines what it says, a line each
     */
    public record Section(String title, List<String> lines) {

        public Section {
            lines = List.copyOf(lines);
        }
    }
}
