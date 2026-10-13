/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.os;

import static dev.jstech.computers.client.os.VirtualStudioLoader.join;

import dev.jstech.computers.os.edit.project.ProjectFile;
import dev.jstech.computers.os.edit.project.ProjectTemplate;
import dev.jstech.computers.os.edit.project.SolutionFile;
import dev.jstech.computers.sigma.LanguageLevel;
import dev.jstech.core.JsCore;
import dev.jstech.core.client.gui.component.Button;
import dev.jstech.core.client.gui.component.Checkbox;
import dev.jstech.core.client.gui.component.Draw;
import dev.jstech.core.client.gui.component.Label;
import dev.jstech.core.client.gui.component.ListView;
import dev.jstech.core.client.gui.component.Popup;
import dev.jstech.core.client.gui.component.TextField;
import dev.jstech.core.client.gui.component.UiComponent;
import dev.jstech.core.client.gui.component.UiContext;
import dev.jstech.core.language.IProgrammingLanguage;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextKey;
import dev.jstech.core.text.TextLists;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;

/**
 * The New Project wizard of the Virtual Studio: a first page that picks a template, narrowed by what is typed and
 * by language, platform and kind, and a second page that names the project and says where it goes.
 *
 * <p>Creating writes the solution file, the project file and the template's first source, then has the loader open
 * the solution around them.
 */
final class VirtualStudioWizard {

    private final BlockPos host;
    private final CodeWorkspace workspace;
    private final VirtualStudioLoader loader;
    /** The system's file window, for choosing where the solution goes. */
    private final FileDialog dialog;
    private final Supplier<OsSkin> skin;
    private final Popup templates = new Popup(GameText.resolve(VirtualStudioTexts.CREATE_PROJECT), 302, 172)
            .setLayouter(this::layoutTemplates);
    private final TextField templateSearch = new TextField(32);
    private final ListView<ProjectTemplate.Offer> templateList;
    private final ListView<ProjectTemplate.Offer> recentList;
    private final Button templateKind;
    private final Button templateLanguage;
    private final Button templatePlatform;
    private final Button templateNext;
    private final Button templateBack;
    private final Popup configure = new Popup(GameText.resolve(VirtualStudioTexts.CONFIGURE_TITLE), 240, 124)
            .setLayouter(this::layoutConfigure);
    private final TextField projectName = new TextField(32);
    private final TextField locationField = new TextField(64);
    private final Button browse;
    private final TextField solutionName = new TextField(32);
    private final Label configureNote;
    private final Button create;
    private String kindFilter = "";
    private String languageFilter = "";
    private String platformFilter = "";
    private boolean sameFolder = true;
    private ProjectTemplate.Offer chosen = FIRST_OFFER;
    private boolean addingToSolution;
    /** Where the wizard puts the solution: the machine's program folder until the player picks another. */
    private String location = CodeWorkspace.HOME;

    /** A template row: its title, what it is, and its tags, one under the other. */
    private static final int TEMPLATE_ROW_H = 28;
    private static final int RECENT_ROW_H = 19;

    /** What the wizard opens on: the shape most programs are, in the language most of them are written in. */
    private static final ProjectTemplate.Offer FIRST_OFFER =
            new ProjectTemplate.Offer(ProjectTemplate.CONSOLE_APP, LanguageLevel.SIGMA_SHARP);

    /** The templates used lately on each machine, newest first, for the wizard's left pane. */
    private static final Map<BlockPos, Deque<ProjectTemplate.Offer>> RECENT_TEMPLATES = new LinkedHashMap<>();

    VirtualStudioWizard(final BlockPos host, final CodeWorkspace workspace, final VirtualStudioLoader loader,
                        final FileDialog dialog, final Supplier<OsSkin> skin) {
        this.host = host;
        this.workspace = workspace;
        this.loader = loader;
        this.dialog = dialog;
        this.skin = skin;
        /*
         * The wizard's first page: the templates used lately on the left, and on the right the whole
         * list, narrowed by what is typed and by language, platform and kind.
         */
        this.templates.add(new Label(GameText.resolve(VirtualStudioTexts.RECENT_TEMPLATES), Label.Tone.DIM));
        this.recentList = this.templates.add(new ListView<>(this::recentTemplates, RECENT_ROW_H, this::drawRecent)
                .setOnClick((index, button, mx, my) -> pickRecent(index)));
        this.templates.add(this.templateSearch.setPlaceholder(GameText.resolve(VirtualStudioTexts.SEARCH_TEMPLATES)));
        // Three filters side by side in the small text, the way a row of drop-downs would sit.
        this.templateLanguage = this.templates.add(new Button(GameText.resolve(VirtualStudioTexts.ALL_LANGUAGES),
                this::cycleLanguage).setLabelScale(0.75f));
        this.templatePlatform = this.templates.add(new Button(GameText.resolve(VirtualStudioTexts.ALL_PLATFORMS),
                this::cyclePlatform).setLabelScale(0.75f));
        this.templateKind = this.templates.add(new Button(GameText.resolve(VirtualStudioTexts.ALL_TYPES),
                this::cycleKind).setLabelScale(0.75f));
        this.templateList = this.templates.add(
                new ListView<>(this::matchingTemplates, TEMPLATE_ROW_H, this::drawTemplate)
                .setOnClick((index, button, mx, my) -> pickTemplate(index)));
        this.templateBack = this.templates.add(new Button(GameText.resolve(VirtualStudioTexts.BACK), () -> { }));
        this.templateBack.setEnabled(false);
        this.templateNext = this.templates.add(new Button(GameText.resolve(VirtualStudioTexts.NEXT), this::toConfigure)
                .setPrimary(true));
        this.templates.add(new Button(GameText.resolve(StudioTexts.CANCEL), this.templates::close));
        // The second page: the template chosen, the names, and where it all goes.
        this.configure.add(new Label(() -> GameText.resolve(this.chosen.title())));
        this.configure.add(new Label(() -> GameText.resolve(TextLists.join("  ", this.chosen.shownTags())),
                Label.Tone.DIM));
        this.configure.add(new Label(GameText.resolve(VirtualStudioTexts.PROJECT_NAME), Label.Tone.DIM));
        this.configure.add(this.projectName);
        this.configure.add(new Label(GameText.resolve(VirtualStudioTexts.LOCATION), Label.Tone.DIM));
        this.configure.add(this.locationField);
        this.browse = this.configure.add(new Button("...", this::browseLocation));
        this.configure.add(new Label(GameText.resolve(VirtualStudioTexts.SOLUTION_NAME), Label.Tone.DIM));
        this.configure.add(this.solutionName);
        this.configure.add(new Checkbox(() -> GameText.resolve(VirtualStudioTexts.SAME_FOLDER),
                () -> this.sameFolder, () -> this.sameFolder = !this.sameFolder));
        this.configureNote = this.configure.add(new Label(() -> GameText.resolve(configureNoteText()),
                Label.Tone.DIM));
        this.configure.add(new Button(GameText.resolve(VirtualStudioTexts.BACK), () -> {
            this.configure.close();
            this.templates.open();
        }));
        this.create = this.configure.add(new Button(GameText.resolve(VirtualStudioTexts.CREATE), this::createProject)
                .setPrimary(true));
        this.projectName.setOnEdit(() -> {
            if (!this.addingToSolution) {
                this.solutionName.set(this.projectName.edit());
            }
        });
    }

    /** The wizard's first page, which is also the popup the studio draws it in. */
    Popup templates() {
        return this.templates;
    }

    /** The wizard's second page. */
    Popup configure() {
        return this.configure;
    }

    /** Whether the wizard is up, on either of its pages. */
    boolean isOpen() {
        return this.templates.isOpen() || this.configure.isOpen();
    }

    /** Opens the wizard on its template page, to start a solution or, with {@code intoSolution}, to add to one. */
    void open(final boolean intoSolution) {
        this.addingToSolution = intoSolution && this.loader.solution() != null;
        this.templateSearch.set("");
        this.kindFilter = "";
        this.languageFilter = "";
        this.platformFilter = "";
        this.templateLanguage.setLabel(GameText.resolve(VirtualStudioTexts.ALL_LANGUAGES));
        this.templatePlatform.setLabel(GameText.resolve(VirtualStudioTexts.ALL_PLATFORMS));
        this.templateKind.setLabel(GameText.resolve(VirtualStudioTexts.ALL_TYPES));
        this.templateList.setSelected(0);
        this.chosen = FIRST_OFFER;
        this.templates.open();
    }

    /** Takes the wizard from its template page to its names page with {@code template} chosen. */
    void chooseTemplate(final ProjectTemplate.Offer template) {
        this.chosen = template;
        openConfigure();
    }

    /**
     * Creates a new solution around one project of {@code template}, as the wizard's Create does with
     * the same directory for both, and opens it.
     */
    void createProject(final ProjectTemplate.Offer template, final String name) {
        create(template, name, name, false);
    }

    /** The languages the machine knows, which is what the language filter cycles through. */
    private List<String> languageNames() {
        final List<String> out = new ArrayList<>();
        for (final IProgrammingLanguage language : JsCore.languages().all()) {
            out.add(language.displayName());
        }
        return out;
    }

    private void cycleLanguage() {
        final List<String> names = languageNames();
        if (this.languageFilter.isEmpty()) {
            this.languageFilter = names.isEmpty() ? "" : names.get(0);
        } else {
            final int at = names.indexOf(this.languageFilter);
            this.languageFilter = at + 1 < names.size() ? names.get(at + 1) : "";
        }
        this.templateLanguage.setLabel(this.languageFilter.isEmpty()
                ? GameText.resolve(VirtualStudioTexts.ALL_LANGUAGES) : this.languageFilter);
    }

    /* The filter holds the kind's word in English, which is what a template matches; the button shows it translated. */
    private void cycleKind() {
        final List<TextKey> kinds = ProjectTemplate.kindWords();
        int at = -1;
        for (int i = 0; i < kinds.size(); i++) {
            if (kinds.get(i).english().equals(this.kindFilter)) {
                at = i;
            }
        }
        final TextKey next = at + 1 < kinds.size() ? kinds.get(at + 1) : null;
        this.kindFilter = next == null ? "" : next.english();
        this.templateKind.setLabel(GameText.resolve(next == null ? VirtualStudioTexts.ALL_TYPES : next));
    }

    /** The icon a template shows in the wizard: the page its first file would have. */
    private static FileIcons.Kind iconOf(final ProjectTemplate template) {
        return switch (template) {
            case CONSOLE_APP -> FileIcons.Kind.PROGRAM;
            case SCRIPT -> FileIcons.Kind.SOURCE;
            case CLASS_LIBRARY -> FileIcons.Kind.BUNDLE;
            case EMPTY_PROJECT -> FileIcons.Kind.FOLDER;
        };
    }

    private void drawRecent(final GuiGraphics g, final UiContext ctx, final ProjectTemplate.Offer offer,
                            final int index, final int x, final int y, final int width, final int height,
                            final boolean hovered, final boolean selected) {
        ctx.skin().listRow(g, x, y, width, height, hovered, selected);
        FileIcons.draw(g, x + 2, y + 1, iconOf(offer.template()), this.skin.get().iconSet());
        final int textX = x + 2 + FileIcons.SIZE + 3;
        final int textW = x + width - 2 - textX;
        Draw.text(g, ctx.font(), ctx.font().plainSubstrByWidth(GameText.resolve(offer.title()), textW), textX, y + 1,
                ctx.skin().listRowText(selected));
        final String kind = GameText.resolve(offer.kindText());
        Draw.text(g, ctx.font(), ctx.font().plainSubstrByWidth(offer.language().mark() + "  " + kind, textW),
                textX, y + 10, ctx.skin().dim());
    }

    /** The templates that fit what was typed and the three filters, each in every language it comes in. */
    private List<ProjectTemplate.Offer> matchingTemplates() {
        final List<ProjectTemplate.Offer> out = new ArrayList<>();
        final String typed = this.templateSearch.edit().toLowerCase(Locale.ROOT).trim();
        for (final ProjectTemplate.Offer template : ProjectTemplate.Offer.all()) {
            if (!template.template().isKind(this.kindFilter)) {
                continue;
            }
            if (!this.languageFilter.isEmpty() && !template.tags().contains(this.languageFilter)) {
                continue;
            }
            if (!this.platformFilter.isEmpty() && !template.tags().contains(this.platformFilter)) {
                continue;
            }
            // The search reads what the player reads, in their language.
            if (!typed.isEmpty() && !GameText.resolve(template.title()).toLowerCase(Locale.ROOT).contains(typed)
                    && !GameText.resolve(template.description()).toLowerCase(Locale.ROOT).contains(typed)) {
                continue;
            }
            out.add(template);
        }
        return out;
    }

    private void drawTemplate(final GuiGraphics g, final UiContext ctx, final ProjectTemplate.Offer template,
                              final int index, final int x, final int y, final int width, final int height,
                              final boolean hovered, final boolean selected) {
        ctx.skin().listRow(g, x, y, width, height, hovered, selected);
        FileIcons.draw(g, x + 3, y + 2, iconOf(template.template()), this.skin.get().iconSet());
        final int textX = x + 3 + FileIcons.SIZE + 3;
        final int textW = x + width - 3 - textX;
        Draw.text(g, ctx.font(), GameText.resolve(template.title()), textX, y + 1, ctx.skin().listRowText(selected));
        // The language at the far end of the title's row, since two rows of one shape differ in nothing else there.
        final String mark = template.language().mark();
        Draw.text(g, ctx.font(), mark, x + width - 4 - ctx.font().width(mark), y + 1, ctx.skin().dim());
        Draw.text(g, ctx.font(), ctx.font().plainSubstrByWidth(GameText.resolve(template.description()), textW),
                textX, y + 9, ctx.skin().dim());
        final String tags = GameText.resolve(TextLists.join("  ", template.shownTags()));
        Draw.text(g, ctx.font(), ctx.font().plainSubstrByWidth(tags, textW), textX, y + 17,
                ctx.skin().dim());
    }

    private void pickTemplate(final int index) {
        final List<ProjectTemplate.Offer> shown = matchingTemplates();
        if (index >= 0 && index < shown.size()) {
            this.chosen = shown.get(index);
            this.templateList.setSelected(index);
        }
    }

    private void toConfigure() {
        pickTemplate(this.templateList.selected() < 0 ? 0 : this.templateList.selected());
        openConfigure();
    }

    private void openConfigure() {
        this.templates.close();
        final String name = uniqueProjectName(
                this.chosen.template() == ProjectTemplate.CLASS_LIBRARY ? "Library" : "App");
        this.projectName.set(name);
        this.locationField.set(shownLocation(this.location));
        this.locationField.setEnabled(!this.addingToSolution);
        this.browse.setEnabled(!this.addingToSolution);
        this.solutionName.set(this.addingToSolution ? this.loader.solution().name() : name);
        this.solutionName.setEnabled(!this.addingToSolution);
        this.configure.open();
        this.configure.focus(this.projectName);
    }

    /** The location the way the wizard shows it: a drive letter, backslashes, a trailing one. */
    private static String shownLocation(final String dir) {
        return "C:\\" + dir.replace('/', '\\') + (dir.isEmpty() ? "" : "\\");
    }

    /** The location typed in the wizard, back to a path on the drive: no drive letter, forward slashes. */
    private static String typedLocation(final String shown) {
        String dir = shown.trim().replace('\\', '/');
        if (dir.regionMatches(true, 0, "C:", 0, 2)) {
            dir = dir.substring(2);
        }
        while (dir.startsWith("/")) {
            dir = dir.substring(1);
        }
        while (dir.endsWith("/")) {
            dir = dir.substring(0, dir.length() - 1);
        }
        return dir;
    }

    private String uniqueProjectName(final String base) {
        String name = base;
        int n = 1;
        while (this.loader.projects().containsKey(name)) {
            name = base + (++n);
        }
        return name;
    }

    private Text configureNoteText() {
        final String project = this.projectName.edit().trim();
        final String sol = this.solutionName.edit().trim();
        final String where = this.addingToSolution ? this.loader.dir()
                : join(typedLocation(this.locationField.edit()), this.sameFolder || sol.isEmpty() ? project : sol);
        return VirtualStudioTexts.WILL_BE_CREATED.with(shownLocation(join(where, project)));
    }

    /** Create, from the wizard's second page: what was typed there. */
    private void createProject() {
        final String project = this.projectName.edit().trim();
        final String typedSolution = this.solutionName.edit().trim();
        final String sol = this.addingToSolution ? this.loader.solution().name()
                : (typedSolution.isEmpty() || this.sameFolder ? project : typedSolution);
        if (!this.addingToSolution) {
            this.location = typedLocation(this.locationField.edit());
        }
        create(this.chosen, project, sol, this.addingToSolution);
    }

    /** The solution file, the project file and the first source, then the solution opens. */
    private void create(final ProjectTemplate.Offer template, final String project, final String sol,
                        final boolean intoSolution) {
        if (!ProjectTemplate.isValidProjectName(project)) {
            this.workspace.say(GameText.resolve(VirtualStudioTexts.ONE_WORD));
            return;
        }
        this.configure.close();
        this.chosen = template;
        rememberTemplate(template);
        this.addingToSolution = intoSolution && this.loader.solution() != null;
        final String dir = this.addingToSolution ? this.loader.dir() : join(this.location, sol);
        final ProjectFile file = this.chosen.project(project);
        final SolutionFile base = this.addingToSolution ? this.loader.solution()
                : new SolutionFile(sol, List.of(), "");
        final SolutionFile solutionFile = base.withProject(SolutionFile.projectPath(file));
        this.loader.saveFile(join(dir, SolutionFile.fileName(sol)), solutionFile.write());
        this.loader.saveFile(join(join(dir, project), file.fileName()), file.write());
        final String first = this.chosen.firstSource(project);
        if (!first.isEmpty()) {
            this.loader.saveFile(join(join(dir, project), first), this.chosen.source(project));
        }
        /*
         * The files are on their way; asking for the solution now reads them once they have landed. The
         * first source waits for the solution to be in, since one file is waited for at a time.
         */
        this.loader.openSolution(dir, first.isEmpty() ? List.of() : List.of(join(join(dir, project), first)), "");
    }

    private void layoutTemplates(final Popup p) {
        final List<UiComponent> c = p.children();
        final int x = p.x() + 4;
        final int top = p.contentTop() + 2;
        final int bottom = p.bottom() - 16;
        // The left pane: what was used lately.
        final int leftW = 96;
        c.get(0).setBounds(x, top, leftW, 9);
        this.recentList.setBounds(x, top + 10, leftW, bottom - top - 10);
        // The right pane: search, the three filters in equal thirds, the list.
        final int rx = x + leftW + 6;
        final int rw = p.right() - 4 - rx;
        int y = top;
        this.templateSearch.setBounds(rx, y, rw, 11);
        y += 13;
        final int third = (rw - 8) / 3;
        this.templateLanguage.setBounds(rx, y, third, 11);
        this.templatePlatform.setBounds(rx + third + 4, y, third, 11);
        this.templateKind.setBounds(rx + 2 * (third + 4), y, rw - 2 * (third + 4), 11);
        y += 13;
        this.templateList.setBounds(rx, y, rw, bottom - y);
        final int by = p.bottom() - 14;
        c.get(c.size() - 1).setBounds(x, by, 40, 11);
        this.templateBack.setBounds(p.right() - 82, by, 36, 11);
        this.templateNext.setBounds(p.right() - 42, by, 38, 11);
    }

    private void layoutConfigure(final Popup p) {
        final int x = p.x() + 4;
        int y = p.contentTop() + 1;
        final int w = p.width() - 8;
        final List<UiComponent> c = p.children();
        // The template's name and tags, the way the page is headed.
        c.get(0).setBounds(x, y, w, 9);
        y += 9;
        c.get(1).setBounds(x, y, w, 9);
        y += 11;
        final int labelW = 72;
        c.get(2).setBounds(x, y + 1, labelW, 9);
        this.projectName.setBounds(x + labelW + 2, y, w - labelW - 2, 11);
        y += 13;
        c.get(4).setBounds(x, y + 1, labelW, 9);
        this.locationField.setBounds(x + labelW + 2, y, w - labelW - 2 - 22, 11);
        this.browse.setBounds(p.right() - 4 - 20, y, 20, 11);
        y += 13;
        c.get(7).setBounds(x, y + 1, labelW, 9);
        this.solutionName.setBounds(x + labelW + 2, y, w - labelW - 2, 11);
        y += 13;
        c.get(9).setBounds(x, y, w, 9);
        y += 11;
        this.configureNote.setBounds(x, y, w, 9);
        final int by = p.bottom() - 14;
        c.get(11).setBounds(p.right() - 82, by, 36, 11);
        this.create.setBounds(p.right() - 42, by, 38, 11);
    }

    /** The templates used lately on this machine, for the wizard's left pane. */
    private List<ProjectTemplate.Offer> recentTemplates() {
        return new ArrayList<>(RECENT_TEMPLATES.getOrDefault(this.host, new ArrayDeque<>()));
    }

    private void pickRecent(final int index) {
        final List<ProjectTemplate.Offer> recent = recentTemplates();
        if (index >= 0 && index < recent.size()) {
            this.chosen = recent.get(index);
            openConfigure();
        }
    }

    private void rememberTemplate(final ProjectTemplate.Offer template) {
        final Deque<ProjectTemplate.Offer> recent =
                RECENT_TEMPLATES.computeIfAbsent(this.host, h -> new ArrayDeque<>());
        recent.remove(template);
        recent.addFirst(template);
        while (recent.size() > 3) {
            recent.removeLast();
        }
    }

    private void cyclePlatform() {
        final List<String> platforms = ProjectTemplate.PLATFORMS;
        if (this.platformFilter.isEmpty()) {
            this.platformFilter = platforms.isEmpty() ? "" : platforms.get(0);
        } else {
            final int at = platforms.indexOf(this.platformFilter);
            this.platformFilter = at + 1 < platforms.size() ? platforms.get(at + 1) : "";
        }
        this.templatePlatform.setLabel(this.platformFilter.isEmpty()
                ? GameText.resolve(VirtualStudioTexts.ALL_PLATFORMS) : this.platformFilter);
    }

    /** Opens the folder picker for where the new solution goes. */
    private void browseLocation() {
        this.dialog.openFolder(VirtualStudioTexts.PROJECT_LOCATION.text(), typedLocation(this.locationField.edit()),
                dir -> {
                    this.location = dir;
                    this.locationField.set(shownLocation(dir));
                });
    }
}
