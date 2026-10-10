/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.os;

import static dev.jstech.computers.client.os.VirtualStudioLoader.join;
import static dev.jstech.computers.client.os.VirtualStudioLoader.shortName;
import static dev.jstech.computers.client.os.VirtualStudioLoader.sourceSuffix;

import dev.jstech.computers.JsComputers;
import dev.jstech.computers.operation.payload.DeleteFilePayload;
import dev.jstech.computers.operation.payload.DiskFilesPayload;
import dev.jstech.computers.operation.payload.FolderContentPayload;
import dev.jstech.computers.gui.layout.StudioPropertiesLayout;
import dev.jstech.computers.hardware.IsaSpec;
import dev.jstech.computers.hardware.Isas;
import dev.jstech.computers.os.ProgramVersions;
import dev.jstech.computers.os.edit.InkPalette;
import dev.jstech.computers.os.edit.ProblemReport;
import dev.jstech.computers.os.edit.project.ProjectFile;
import dev.jstech.computers.os.edit.project.ProjectTemplate;
import dev.jstech.computers.os.edit.project.SolutionFile;
import dev.jstech.computers.sigma.LanguageLevel;
import dev.jstech.computers.sigma.SigmaError;
import dev.jstech.computers.sigma.SigmaVersions;
import dev.jstech.computers.vm.listing.AsmProgram;
import dev.jstech.core.JsCore;
import dev.jstech.core.client.gui.component.AmountStepper;
import dev.jstech.core.client.gui.component.Button;
import dev.jstech.core.client.gui.component.Checkbox;
import dev.jstech.core.client.gui.component.ContextMenu;
import dev.jstech.core.client.gui.component.Draw;
import dev.jstech.core.client.gui.component.Label;
import dev.jstech.core.client.gui.component.ListView;
import dev.jstech.core.client.gui.component.MenuBar;
import dev.jstech.core.client.gui.component.Panel;
import dev.jstech.core.client.gui.component.Popup;
import dev.jstech.core.client.gui.component.TabStrip;
import dev.jstech.core.client.gui.component.TextField;
import dev.jstech.core.client.gui.component.UiComponent;
import dev.jstech.core.client.gui.component.UiContext;
import dev.jstech.core.language.IProgrammingLanguage;
import dev.jstech.core.palette.Palette;
import dev.jstech.core.palette.PaletteHolder;
import dev.jstech.core.palette.Palettes;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextKey;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

/**
 * Virtual Studio: the whole workshop in one window, working in solutions and projects.
 *
 * <p>It opens on a Start Window. A solution is a folder with a solution file and a folder per project
 * under it; a project says what it is made of and what it builds. Create a new project picks a
 * template, filtered by language, platform and kind, names it, and opens the solution around it.
 * Build compiles every source of a project, with the libraries it references, to the listing its
 * project file names; Start builds the startup project and runs it at the terminal.
 *
 * <p>What it has that nothing else does is the price: the list of what can follow a name says what
 * each call will cost the program, before the line is written.
 */
@PaletteHolder
public final class VirtualStudioApp implements IDesktopApp, CodeFileReplies.IReader {

    private static final int TOOLBAR_H = 13;
    private static final int SIDE_W = 108;
    private static final int CAPTION_H = 9;
    private static final int TAB_H = 10;
    private static final int STATUS_H = 9;
    private static final int ROW_H = 9;
    /** The solution tree's rows hold a 16-pixel icon, so they are taller than the dense lists of the rest. */
    private static final int TREE_ROW_H = 17;
    private static final int TREE_TEXT_DY = 5;
    private static final int DOCK_H = 52;
    private static final int MIN_CODE_H = 36;
    /** The key the studio's window goes by, which is the program's id. */
    private static final String KEY = "jsc:virtual_studio";
    /** Where the example under the version buttons says the refused call is, inside the template's Main. */
    private static final int EXAMPLE_LINE = 4;
    private static final int EXAMPLE_COLUMN = 5;

    /**
     * The studio's own colours, {@code jsc:editor/virtual_studio}: a platform the project builds for and one it
     * does not, a complaint's code, and a build that succeeded or failed.
     */
    private static final Palette<Colours> PALETTE = Palettes.declare(JsComputers.MODID, "editor/virtual_studio",
            new Colours(0xFF2E7D32, 0xFFB35C00, 0xFFC0392B, 0xFF1E8449, 0xFFC0392B));

    private static final int DOCK_ERRORS = 0;
    private static final int DOCK_OUTPUT = 1;
    private static final int DOCK_TERMINAL = 2;
    /** How close to a divider a click has to land to take hold of it. */
    private static final int GRIP = 3;

    /* The two dividers the player can drag: how wide the Solution Explorer is, how tall the dock is. */
    private int sideW = SIDE_W;
    private int dockH = DOCK_H;
    /** Where the dividers were drawn last, so a click can find them. */
    private int dividerX;
    private int dividerY;
    private int bodyTop;
    private int bodyBottom;
    /** Which divider the mouse is holding: 0 none, 1 the explorer's, 2 the dock's. */
    private int holding;

    /** The machine's own console in the dock, where Start runs what was built. */
    private final ShellView terminal;
    /** Whether typing goes to the terminal rather than to the code. */
    private boolean typingInTerminal;
    /** Lines still to be typed at the terminal, the next once the machine has answered the one before. */
    private final Deque<String> typing = new ArrayDeque<>();

    /** Where solutions are made unless the player says otherwise. */
    private static final String LOCATION = CodeWorkspace.HOME;

    /** What the window is showing. */
    private enum Page { START, SOLUTION }

    /** What a row of the Solution Explorer stands for. */
    private enum NodeKind { SOLUTION, PROJECT, DEPENDENCIES, DEPENDENCY, PROPERTIES, SOURCE, OUTPUT, FOLDER_FILE }

    /** One row of the Solution Explorer. */
    private record Node(int depth, String label, NodeKind kind, String project, String path) {
    }

    private final BlockPos host;
    private final CodeWorkspace workspace;
    private OsSkin skin = OsSkin.fallback();
    private Page page = Page.START;
    private int tabSize = 4;
    private boolean completionsOn = true;

    /* The solution, the build and the New Project wizard, each kept by a class of its own */
    private final VirtualStudioLoader loader;
    private final VirtualStudioBuild build;
    private final VirtualStudioWizard wizard;
    private final Set<String> collapsed = new LinkedHashSet<>();
    private final Set<String> dependenciesOpen = new LinkedHashSet<>();

    /* The window */
    private final Panel root = new Panel();
    private final MenuBar menuBar = new MenuBar(96, 10);
    private final ListView<Node> explorer;
    private final TabStrip tabs;
    private final TabStrip dockTabs;
    private final ListView<ProblemReport.Row> errors;
    private final ListView<VirtualStudioBuild.OutputLine> outputList;
    private final Button start;
    private final CodeCompletions completions = new CodeCompletions().withCosts(true);
    private final CommandPalette palette = new CommandPalette();
    /** The system's file window, for everything the studio opens or saves by choosing on the disk. */
    private final FileDialog dialog;
    private final List<Link> links = new ArrayList<>();

    private record Link(String title, int x, int y, int width, int height, Runnable action) {
    }

    /* The small windows a command opens for one thing */
    private final Popup ask = new Popup(() -> this.askTitle, 150, 44).setLayouter(this::layoutAsk);
    /** The question a closing tab with changes asks. */
    private final Popup askClose = new Popup(() -> GameText.resolve(StudioTexts.SAVE_CHANGES.with(closingName())),
            176, 40).setLayouter(this::layoutAskClose);
    /** The question the tree asks before a file goes, and the row it is about. */
    private final Popup askDelete = new Popup(
            () -> GameText.resolve(VirtualStudioTexts.DELETE_QUESTION.with(deletingName())), 150, 40)
            .setLayouter(this::layoutAskDelete);
    private Node deleting;
    /** The right-button menu of the Solution Explorer. */
    private final ContextMenu treeContext = new ContextMenu(124, 11);
    /** Where the window was last drawn, for a menu opened from a row of the tree to stay inside it. */
    private int winX;
    private int winY;
    private int winW;
    private int winH;
    /** The tab being closed while the question is up. */
    private int closing = -1;
    private final TextField askField = new TextField(64);
    private final Button askOk;
    private String askTitle = "";
    private Consumer<String> askAction = value -> { };
    private final Popup properties = new Popup(GameText.resolve(VirtualStudioTexts.PROPERTIES_TITLE),
            StudioPropertiesLayout.WIDTH, StudioPropertiesLayout.HEIGHT).setLayouter(this::layoutProperties);
    private final List<Label> propertyLines = new ArrayList<>();
    private final Label platformLabel;
    /** One button per instruction set there is, in the order the series was built. */
    private final List<Button> platformButtons = new ArrayList<>();
    private final Label platformHint;
    private final Label versionLabel;
    /** Default, which follows the installed compiler, then one button for each version of the language. */
    private final List<Button> versionButtons = new ArrayList<>();
    /** What Default follows, what a lower version refuses, an example of it, and the line a choice writes. */
    private final List<Label> versionHints = new ArrayList<>();
    private final Button propertiesClose;
    private String propertiesOf = "";
    private final Popup options = new Popup(GameText.resolve(VirtualStudioTexts.OPTIONS_TITLE), 170, 60)
            .setLayouter(this::layoutOptions);
    private final AmountStepper tabStepper = new AmountStepper();
    private final Checkbox completionsBox;
    private final Button optionsClose;

    public VirtualStudioApp(final BlockPos host) {
        this.host = host;
        this.workspace = new CodeWorkspace(host);
        this.loader = new VirtualStudioLoader(host, this.workspace, this, () -> this.page = Page.SOLUTION);
        this.workspace.setProjectVersion(this.loader::projectVersionOf);
        this.build = new VirtualStudioBuild(host, this.workspace, this.loader, this,
                () -> showDock(DOCK_OUTPUT), () -> showDock(DOCK_ERRORS), this::runInTerminal);
        this.dialog = new FileDialog(host, this);
        this.wizard = new VirtualStudioWizard(host, this.workspace, this.loader, this.dialog, () -> this.skin);
        this.explorer = this.root.add(new ListView<>(this::nodes, TREE_ROW_H, this::drawNode)).setOnClick(this::onNode);
        this.tabs = this.root.add(new TabStrip(this.workspace::tabLabels).fitToLabels(10).setUnderline(false));
        this.tabs.setOnSelect(this.workspace::setCurrent);
        this.dockTabs = this.root.add(new TabStrip(List.of(GameText.resolve(VirtualStudioTexts.ERROR_LIST_TAB),
                GameText.resolve(VirtualStudioTexts.OUTPUT_TAB), GameText.resolve(StudioTexts.TERMINAL_TAB)))
                .fitToLabels(12).setUnderline(true));
        this.errors = this.root.add(new ListView<>(this::errorRows, ROW_H, this::drawErrorRow)).setOnClick(this::onError);
        this.outputList = this.root.add(new ListView<>(() -> this.build.output(), ROW_H, this::drawOutputRow));
        this.terminal = this.root.add(new ShellView(host, false, ""));
        this.terminal.setOnIdle(this::typeNext);
        this.tabs.setCloseable(this::closeTab);
        buildPrompts();
        this.start = this.root.add(new Button(GameText.resolve(StudioTexts.START), this::startProgram)
                .setPrimary(true));
        this.root.add(this.menuBar);
        buildMenuBar();

        this.ask.add(this.askField);
        this.askOk = this.ask.add(new Button(GameText.resolve(StudioTexts.OK), () -> {
            this.ask.close();
            this.askAction.accept(this.askField.edit().trim());
        }).setPrimary(true));
        for (int i = 0; i < StudioPropertiesLayout.LINES; i++) {
            final int line = i;
            this.propertyLines.add(this.properties.add(new Label(() -> GameText.resolve(propertyText(line)))));
        }
        this.platformLabel = this.properties.add(new Label(GameText.resolve(VirtualStudioTexts.PLATFORM_TARGET),
                Label.Tone.DIM));
        for (final IsaSpec isa : Isas.all()) {
            this.platformButtons.add(this.properties.add(new Button(isa.name(),
                    () -> setPlatform(isa.id())).setLabelScale(0.75f)));
        }
        this.platformHint = this.properties.add(new Label(() -> GameText.resolve(platformHintText()))
                .setColor(this::platformHintColor));
        this.versionLabel = this.properties.add(new Label(GameText.resolve(VirtualStudioTexts.LANGUAGE_VERSION),
                Label.Tone.DIM));
        for (int version = 0; version <= SigmaVersions.NEWEST; version++) {
            final int chosen = version;
            this.versionButtons.add(this.properties.add(new Button(() -> versionButtonText(chosen),
                    () -> setLanguageVersion(chosen)).setLabelScale(StudioPropertiesLayout.SMALL)));
        }
        for (int line = 0; line < StudioPropertiesLayout.VERSION_HINTS; line++) {
            final int shown = line;
            final Label hint = this.properties.add(new Label(() -> GameText.resolve(versionHintText(shown)))
                    .setScale(StudioPropertiesLayout.SMALL));
            // The example of what a lower version refuses is an error, in the colour of a decision that costs.
            this.versionHints.add(shown == 2 ? hint.setColor(() -> PALETTE.get().platformOff())
                    : hint.setTone(Label.Tone.DIM));
        }
        this.propertiesClose = this.properties.add(new Button(GameText.resolve(StudioTexts.CLOSE),
                this.properties::close).setPrimary(true));
        this.options.add(new Label(GameText.resolve(StudioTexts.TAB_SIZE), Label.Tone.DIM));
        this.options.add(this.tabStepper.setRange(2, 8).setAmount(4).setOnChange(v -> setTabSize((int) v)));
        this.completionsBox = this.options.add(new Checkbox(() -> GameText.resolve(VirtualStudioTexts.SUGGEST),
                () -> this.completionsOn, () -> this.completionsOn = !this.completionsOn));
        this.optionsClose = this.options.add(new Button(GameText.resolve(StudioTexts.CLOSE), this.options::close)
                .setPrimary(true));
    }

    /**
     * The two questions the editor asks before doing something it cannot take back: whether to save a file
     * being closed, and whether a file really is to be deleted. Neither offers its dangerous answer first.
     */
    private void buildPrompts() {
        this.askClose.add(new Button(GameText.resolve(StudioTexts.SAVE), () -> {
            this.askClose.close();
            this.workspace.setCurrent(this.closing);
            this.workspace.save();
            this.workspace.close(this.closing);
        }).setPrimary(true));
        this.askClose.add(new Button(GameText.resolve(StudioTexts.DONT_SAVE), () -> {
            this.askClose.close();
            this.workspace.close(this.closing);
        }));
        this.askClose.add(new Button(GameText.resolve(StudioTexts.CANCEL), this.askClose::close));
        this.askDelete.add(new Button(GameText.resolve(VirtualStudioTexts.DELETE), () -> {
            this.askDelete.close();
            deleteNode();
        }).setPrimary(true));
        this.askDelete.add(new Button(GameText.resolve(StudioTexts.CANCEL), this.askDelete::close));
    }

    /** The eight menus across the top, each filled when it is opened rather than now. */
    private void buildMenuBar() {
        this.menuBar.add(GameText.resolve(StudioTexts.FILE_MENU), this::fileMenu)
                .add(GameText.resolve(StudioTexts.EDIT_MENU), this::editMenu)
                .add(GameText.resolve(StudioTexts.VIEW_MENU), this::viewMenu)
                .add(GameText.resolve(StudioTexts.PROJECT_MENU), this::projectMenu)
                .add(GameText.resolve(VirtualStudioTexts.BUILD), this::buildMenu)
                .add(GameText.resolve(VirtualStudioTexts.DEBUG_MENU), this::debugMenu)
                .add(GameText.resolve(VirtualStudioTexts.TOOLS_MENU), this::toolsMenu)
                .add(GameText.resolve(StudioTexts.HELP_MENU), this::helpMenu);
    }

    /* What a test, or "Open with", asks of it */

    /** The file being edited, or empty when none is. */
    public String openFile() {
        final CodeWorkspace.Doc doc = this.workspace.current();
        return doc == null ? "" : doc.path();
    }

    /** The text of the file being edited. */
    public String text() {
        final CodeWorkspace.Doc doc = this.workspace.current();
        return doc == null ? "" : doc.area().text();
    }

    /** The name of the open solution, or empty on the Start Window. */
    public String solutionName() {
        return this.loader.solution() == null ? "" : this.loader.solution().name();
    }

    /** The names of the projects in the open solution. */
    public List<String> projectNames() {
        return new ArrayList<>(this.loader.projects().keySet());
    }

    /** The names the suggestion list offers, top to bottom; empty when none is up. */
    public List<String> completionLabels() {
        return this.completions.labels();
    }

    /** What the Output pane says, one line after another. */
    public List<String> outputLines() {
        return this.build.output().stream().map(line -> GameText.resolve(line.text())).toList();
    }

    /** Whether the window is on its Start Window. */
    public boolean onStartWindow() {
        return this.page == Page.START;
    }

    /** Opens a file, as picking this program with "Open with" does; a solution file opens its solution. */
    @Override
    public void openFile(final String path) {
        if (path.endsWith("." + SolutionFile.EXTENSION)) {
            final int slash = path.lastIndexOf('/');
            this.loader.openSolutionFolder(slash > 0 ? path.substring(0, slash) : "");
            return;
        }
        if (ProjectFile.isProjectFile(path)) {
            final int slash = path.lastIndexOf('/');
            final String projectDir = slash > 0 ? path.substring(0, slash) : "";
            final int up = projectDir.lastIndexOf('/');
            this.loader.openSolutionFolder(up > 0 ? projectDir.substring(0, up) : "");
            return;
        }
        if (this.page == Page.START) {
            final int slash = path.lastIndexOf('/');
            this.loader.openFolder(slash > 0 ? path.substring(0, slash) : "");
        }
        this.workspace.open(path);
    }

    @Override
    public void onContent(final String path, final String content, final boolean exists) {
        this.loader.onContent(path, content, exists);
    }

    @Override
    public void onFolder(final FolderContentPayload folder) {
        this.build.onFolder(folder);
    }

    /** Opens the solution kept in {@code dir}: its file, then each project's, then the folders. */
    public void openSolutionFolder(final String dir) {
        this.loader.openSolutionFolder(dir);
    }

    /** Opens a plain folder, with no solution around it: the files are the tree. */
    public void openFolder(final String dir) {
        this.loader.openFolder(dir);
    }

    /** Builds every project that makes a listing, in the order the solution lists them. */
    public void buildSolution() {
        this.build.buildSolution();
    }

    /** Start: builds the startup project and, when it built, runs it at the terminal. */
    public void startProgram() {
        this.build.startProgram();
    }

    /**
     * Creates a new solution around one project of {@code template}, as the wizard's Create does with
     * the same directory for both, and opens it.
     */
    public void createProject(final ProjectTemplate.Offer template, final String name) {
        this.wizard.createProject(template, name);
    }

    /**
     * The solution and the tabs, for the machine to hand back after the game itself was closed: which
     * kind of thing is open, where, then the workspace's own account of its files.
     */
    @Override
    public String saveState() {
        if (this.page != Page.SOLUTION || this.loader.dir().isEmpty()) {
            return "";
        }
        return (this.loader.solution() != null ? "solution" : "folder") + "\n" + this.loader.dir() + "\n"
                + this.workspace.describeOpen().substring(this.workspace.folder().length() + 1);
    }

    @Override
    public void restoreState(final String state) {
        final int firstBreak = state.indexOf('\n');
        if (firstBreak < 0) {
            return;
        }
        final String kind = state.substring(0, firstBreak);
        final String rest = state.substring(firstBreak + 1);
        final String dir = CodeWorkspace.folderOf(rest);
        if (dir.isEmpty()) {
            return;
        }
        if ("solution".equals(kind)) {
            // The files wait for the solution: the machine answers one file at a time, and the solution goes first.
            final String[] lines = rest.split("\n", -1);
            final List<String> files = new ArrayList<>();
            for (int i = 1; i < lines.length - 1; i++) {
                if (!lines[i].isEmpty()) {
                    files.add(lines[i]);
                }
            }
            this.loader.openSolution(dir, files, lines.length > 2 ? lines[lines.length - 1] : "");
            return;
        }
        this.loader.openFolder(dir);
        this.workspace.reopen(rest);
    }

    private void closeSolution() {
        this.loader.close();
        this.workspace.closeAll();
        this.build.clearResults();
        this.page = Page.START;
    }

    /* The Solution Explorer */

    private List<Node> nodes() {
        final List<Node> out = new ArrayList<>();
        // The listing is rebuilt on every call, so it is taken once and shared by every project.
        final List<CodeWorkspace.TreeRow> tree = this.workspace.tree();
        if (this.loader.solution() == null) {
            // A plain folder: what is in it, the way an explorer shows it.
            out.add(new Node(0, shortName(this.loader.dir()).isEmpty() ? "C:\\" : shortName(this.loader.dir()),
                    NodeKind.SOLUTION, "", this.loader.dir()));
            for (final CodeWorkspace.TreeRow row : tree) {
                final String mark = row.file().directory()
                        ? (this.workspace.isExpanded(row.file().path()) ? "v " : "> ") : "";
                out.add(new Node(row.depth() + 1, mark + shortName(row.file().path()), NodeKind.FOLDER_FILE, "",
                        row.file().path()));
            }
            return out;
        }
        out.add(new Node(0, GameText.resolve(VirtualStudioTexts.SOLUTION_NODE.with(this.loader.solution().name(),
                this.loader.projects().size())), NodeKind.SOLUTION, "", this.loader.dir()));
        for (final ProjectFile project : this.loader.projects().values()) {
            final boolean open = !this.collapsed.contains(project.name());
            final boolean startup = project.name().equals(this.loader.startupName());
            out.add(new Node(1, (open ? "v " : "> ") + project.name() + (startup ? " *" : ""), NodeKind.PROJECT,
                    project.name(), this.loader.projectDir(project.name())));
            if (!open) {
                continue;
            }
            final boolean deps = this.dependenciesOpen.contains(project.name());
            out.add(new Node(2, (deps ? "v " : "> ") + GameText.resolve(VirtualStudioTexts.DEPENDENCIES),
                    NodeKind.DEPENDENCIES, project.name(), ""));
            if (deps) {
                out.add(new Node(3, dependencyName(project), NodeKind.DEPENDENCY, project.name(), ""));
                for (final String reference : project.references()) {
                    out.add(new Node(3, reference, NodeKind.DEPENDENCY, project.name(), ""));
                }
            }
            out.add(new Node(2, GameText.resolve(VirtualStudioTexts.PROPERTIES), NodeKind.PROPERTIES, project.name(),
                    ""));
            for (final String source : project.sources()) {
                out.add(new Node(2, source, NodeKind.SOURCE, project.name(),
                        join(this.loader.projectDir(project.name()), source)));
            }
            final String buildDir = join(this.loader.projectDir(project.name()), "build");
            for (final CodeWorkspace.TreeRow row : tree) {
                if (!row.file().directory() && row.file().path().startsWith(buildDir + "/")) {
                    out.add(new Node(2, "build/" + shortName(row.file().path()), NodeKind.OUTPUT, project.name(),
                            row.file().path()));
                }
            }
        }
        return out;
    }

    private String languageName(final String id) {
        final IProgrammingLanguage language = id.contains(":")
                ? JsCore.languages().get(ResourceLocation.tryParse(id)) : null;
        return language == null ? id : language.displayName();
    }

    /**
     * The language a project builds against, as its Dependencies lists it: for a language with versions, the one
     * the project is held to, which is the installed compiler's unless the project names a lower one.
     */
    private String dependencyName(final ProjectFile project) {
        final LanguageLevel level = LanguageLevel.ofId(project.language());
        return level == null ? languageName(project.language())
                : level.mark() + " " + InstalledCompilers.held(level, project.languageVersion());
    }

    private void drawNode(final GuiGraphics g, final UiContext ctx, final Node node, final int index,
                          final int x, final int y, final int width, final int height,
                          final boolean hovered, final boolean selected) {
        ctx.skin().listRow(g, x, y, width, height, hovered, selected);
        final int color = node.kind() == NodeKind.DEPENDENCY || node.kind() == NodeKind.OUTPUT
                ? ctx.skin().dim() : ctx.skin().listRowText(selected);
        final int iconX = x + 2 + node.depth() * 8;
        FileIcons.draw(g, iconX, y, iconOf(node), this.skin.iconSet());
        final int textX = iconX + FileIcons.SIZE + 3;
        Draw.text(g, ctx.font(), ctx.font().plainSubstrByWidth(node.label(), x + width - 2 - textX), textX,
                y + TREE_TEXT_DY, color);
    }

    /** The icon beside a row of the tree: what the explorer gives the same file, and a few of the tree's own. */
    private FileIcons.Kind iconOf(final Node node) {
        return switch (node.kind()) {
            case SOLUTION -> FileIcons.Kind.BUNDLE;
            case PROJECT -> FileIcons.Kind.PROGRAM;
            case DEPENDENCIES -> FileIcons.Kind.FOLDER;
            case DEPENDENCY -> FileIcons.Kind.BIN;
            case PROPERTIES -> FileIcons.Kind.CFG;
            case SOURCE, OUTPUT -> FileIcons.kindOfPath(node.path(), false);
            case FOLDER_FILE -> {
                final DiskFilesPayload.WireFile file = fileAt(node.path());
                yield FileIcons.kindOfPath(node.path(), file != null && file.directory());
            }
        };
    }

    private void onNode(final int index, final int button, final double mx, final double my) {
        final List<Node> all = nodes();
        if (index < 0 || index >= all.size()) {
            return;
        }
        final Node node = all.get(index);
        if (button == 1) {
            final List<ContextMenu.Item> items = treeMenu(node);
            if (!items.isEmpty()) {
                this.treeContext.open(items, (int) mx, (int) my, this.winX, this.winY, this.winW, this.winH);
            }
            return;
        }
        switch (node.kind()) {
            case PROJECT -> {
                if (!this.collapsed.remove(node.project())) {
                    this.collapsed.add(node.project());
                }
            }
            case DEPENDENCIES -> {
                if (!this.dependenciesOpen.remove(node.project())) {
                    this.dependenciesOpen.add(node.project());
                }
            }
            case PROPERTIES -> showProperties(node.project());
            case SOURCE, OUTPUT -> this.workspace.open(node.path());
            case FOLDER_FILE -> {
                final DiskFilesPayload.WireFile file = fileAt(node.path());
                if (file != null && file.directory()) {
                    this.workspace.toggleFolder(node.path());
                } else {
                    this.workspace.open(node.path());
                }
            }
            default -> { }
        }
    }

    private DiskFilesPayload.WireFile fileAt(final String path) {
        for (final CodeWorkspace.TreeRow row : this.workspace.tree()) {
            if (row.file().path().equals(path)) {
                return row.file();
            }
        }
        return null;
    }

    /* Errors and output */

    private List<ProblemReport.Row> errorRows() {
        if (!this.build.errors().isEmpty()) {
            return ProblemReport.of(this.build.errors());
        }
        final CodeWorkspace.Doc doc = this.workspace.current();
        if (doc == null) {
            return List.of();
        }
        final Map<String, List<IProgrammingLanguage.Complaint>> live = new LinkedHashMap<>();
        live.put(doc.path(), doc.complaints());
        return ProblemReport.of(live);
    }

    private void drawErrorRow(final GuiGraphics g, final UiContext ctx, final ProblemReport.Row row,
                              final int index, final int x, final int y, final int width, final int height,
                              final boolean hovered, final boolean selected) {
        ctx.skin().listRow(g, x, y, width, height, hovered, selected);
        // Code, description, file and line, each in its column, the way the real error list lays them out.
        final int fileX = x + width - COL_LINE_W - COL_FILE_W;
        final int descriptionW = fileX - (x + 3 + COL_CODE_W) - 4;
        Draw.text(g, ctx.font(), row.complaint().code(), x + 3, y + 1, PALETTE.get().complaint());
        Draw.text(g, ctx.font(), ctx.font().plainSubstrByWidth(GameText.resolve(row.complaint().message()),
                descriptionW), x + 3 + COL_CODE_W, y + 1, ctx.skin().listRowText(selected));
        Draw.text(g, ctx.font(), ctx.font().plainSubstrByWidth(row.name(), COL_FILE_W - 4), fileX, y + 1,
                ctx.skin().dim());
        Draw.text(g, ctx.font(), String.valueOf(row.complaint().line()), x + width - COL_LINE_W, y + 1,
                ctx.skin().dim());
    }

    private void onError(final int index, final int button, final double mx, final double my) {
        final List<ProblemReport.Row> rows = errorRows();
        if (index < 0 || index >= rows.size()) {
            return;
        }
        final ProblemReport.Row row = rows.get(index);
        this.workspace.open(row.path());
        final CodeWorkspace.Doc doc = this.workspace.current();
        if (doc != null && doc.path().equals(row.path())) {
            doc.area().document().setCursor(row.complaint().line() - 1, Math.max(0, row.complaint().column() - 1));
        }
    }

    private void drawOutputRow(final GuiGraphics g, final UiContext ctx, final VirtualStudioBuild.OutputLine line,
                               final int index,
                               final int x, final int y, final int width, final int height,
                               final boolean hovered, final boolean selected) {
        ctx.skin().listRow(g, x, y, width, height, hovered, selected);
        final int ink = switch (line.tone()) {
            case SUCCEEDED -> PALETTE.get().succeeded();
            case FAILED -> PALETTE.get().failed();
            case PLAIN -> ctx.skin().text();
        };
        Draw.text(g, ctx.font(), ctx.font().plainSubstrByWidth(GameText.resolve(line.text()), width - 4), x + 2,
                y + 1, ink);
    }

    /** The point on the Start Window that opens {@code title}, once drawn, or null when it is not up. */
    public int[] startLinkCenter(final String title) {
        for (final Link link : this.links) {
            if (link.title().equals(title)) {
                return new int[] {link.x() + link.width() / 2, link.y() + link.height() / 2};
            }
        }
        return null;
    }

    /** Whether the New Project wizard is up, on either of its pages. */
    public boolean wizardOpen() {
        return this.wizard.isOpen();
    }

    /** Takes the wizard from its template page to its names page with {@code template} chosen. */
    public void chooseTemplate(final ProjectTemplate.Offer template) {
        this.wizard.chooseTemplate(template);
    }

    /* Properties, options, and the one-thing windows */

    private void showProperties(final String name) {
        this.propertiesOf = name;
        refreshPlatformButtons();
        refreshVersionButtons();
        this.properties.open();
    }

    private Text propertyText(final int line) {
        final ProjectFile project = this.loader.projects().get(this.propertiesOf);
        if (project == null) {
            return Text.EMPTY;
        }
        return switch (line) {
            case 0 -> VirtualStudioTexts.NAME_LINE.with(project.name());
            case 1 -> VirtualStudioTexts.KIND_LINE.with(project.kind().key());
            case 2 -> VirtualStudioTexts.LANGUAGE_LINE.with(languageName(project.language()));
            case 3 -> VirtualStudioTexts.ENTRY_LINE.with(project.entry().isEmpty()
                    ? VirtualStudioTexts.NO_ENTRY.text() : project.entry());
            default -> VirtualStudioTexts.SOURCES_LINE.with(project.sources().size(), project.references().size());
        };
    }

    private void layoutProperties(final Popup p) {
        final int x = p.x() + StudioPropertiesLayout.PAD;
        final int wide = p.width() - StudioPropertiesLayout.PAD * 2;
        for (int i = 0; i < this.propertyLines.size(); i++) {
            this.propertyLines.get(i).setBounds(x, p.y() + StudioPropertiesLayout.LINES_Y
                    + i * StudioPropertiesLayout.LINE_H, wide, StudioPropertiesLayout.LINE_H);
        }
        /*
         * A library has no platform of its own: it is compiled into whatever program references it, and that
         * program's target is the one that counts. So the row is not there rather than there and doing nothing.
         */
        final boolean shown = buildsForAPlatform();
        this.platformLabel.setVisible(shown);
        this.platformLabel.setBounds(x, p.y() + StudioPropertiesLayout.PLATFORM_LABEL_Y, wide,
                StudioPropertiesLayout.LINE_H);
        /*
         * The buttons take the row on their own rather than sitting beside the words, so that the row still holds
         * every instruction set when a mod has brought two of its own.
         */
        for (int i = 0; i < this.platformButtons.size(); i++) {
            this.platformButtons.get(i).setVisible(shown);
            this.platformButtons.get(i).setBounds(p.x() + StudioPropertiesLayout.platformButtonX(i),
                    p.y() + StudioPropertiesLayout.PLATFORM_BUTTONS_Y, StudioPropertiesLayout.PLATFORM_BUTTON_W,
                    StudioPropertiesLayout.BUTTON_H);
        }
        this.platformHint.setVisible(shown);
        this.platformHint.setBounds(x, p.y() + StudioPropertiesLayout.PLATFORM_HINT_Y, wide,
                StudioPropertiesLayout.LINE_H);
        // Only the languages that have versions have a version to hold a project to.
        final boolean versioned = shown && versionedLevel() != null;
        this.versionLabel.setVisible(versioned);
        this.versionLabel.setBounds(x, p.y() + StudioPropertiesLayout.VERSION_LABEL_Y, wide,
                StudioPropertiesLayout.LINE_H);
        for (int i = 0; i < this.versionButtons.size(); i++) {
            this.versionButtons.get(i).setVisible(versioned);
            this.versionButtons.get(i).setBounds(p.x() + StudioPropertiesLayout.versionButtonX(i),
                    p.y() + StudioPropertiesLayout.VERSION_BUTTONS_Y, StudioPropertiesLayout.versionButtonW(i),
                    StudioPropertiesLayout.BUTTON_H);
        }
        for (int i = 0; i < this.versionHints.size(); i++) {
            this.versionHints.get(i).setVisible(versioned);
            this.versionHints.get(i).setBounds(x, p.y() + StudioPropertiesLayout.versionHintY(i), wide,
                    StudioPropertiesLayout.SMALL_LINE_H);
        }
        // From the corner the popup really has, since a narrow studio window gives it less than it asked for.
        this.propertiesClose.setBounds(p.right() - (StudioPropertiesLayout.WIDTH - StudioPropertiesLayout.closeX()),
                p.bottom() - (StudioPropertiesLayout.HEIGHT - StudioPropertiesLayout.CLOSE_Y),
                StudioPropertiesLayout.CLOSE_W, StudioPropertiesLayout.BUTTON_H);
    }

    /** The language the open project is written in when it is one with versions, or null. */
    private LanguageLevel versionedLevel() {
        final ProjectFile project = this.loader.projects().get(this.propertiesOf);
        return project == null ? null : LanguageLevel.ofId(project.language());
    }

    /** What a version's button says: Default, with the version it follows, then each version by its number. */
    private String versionButtonText(final int version) {
        final LanguageLevel level = versionedLevel();
        final String mark = level == null ? "" : level.mark();
        if (version == 0) {
            return GameText.resolve(VirtualStudioTexts.DEFAULT_VERSION.with(mark,
                    level == null ? SigmaVersions.NEWEST : InstalledCompilers.version(level)));
        }
        return mark + " " + version;
    }

    /**
     * The lines under the version buttons: what Default follows, the installed compiler named with its version;
     * that a lower version refuses what came after it, with the error it gives; and the line a choice writes.
     */
    private Text versionHintText(final int line) {
        final LanguageLevel level = versionedLevel();
        final ProjectFile project = this.loader.projects().get(this.propertiesOf);
        if (level == null || project == null) {
            return Text.EMPTY;
        }
        return switch (line) {
            case 0 -> VirtualStudioTexts.VERSION_FOLLOWS.with(level.compiler(),
                    installedPackageVersion(level));
            case 1 -> VirtualStudioTexts.VERSION_REFUSES.text();
            // Where a call would sit in the template's program, in the project's own source, as the build prints it.
            case 2 -> VirtualStudioTexts.VERSION_EXAMPLE.with(project.name() + "." + level.sourceExtension(),
                    EXAMPLE_LINE, EXAMPLE_COLUMN, SigmaError.NEEDS_A_LATER_VERSION.code(),
                    SigmaError.NEEDS_A_LATER_VERSION.message("puts", level.mark(), SigmaVersions.NEWEST,
                            level.mark(), SigmaVersions.FIRST));
            default -> VirtualStudioTexts.VERSION_WRITTEN.with(project.languageVersion() > 0
                    ? project.languageVersion() : SigmaVersions.FIRST);
        };
    }

    /** The version the machine's compiler of that language is installed at, or the one it ships at. */
    private static String installedPackageVersion(final LanguageLevel level) {
        final String installed = ActiveDesktop.installedVersion(level.compilerPackage());
        return installed == null || installed.isBlank() ? ProgramVersions.of(level.compilerPackage()) : installed;
    }

    /** Holds the project to that version of its language from now on; 0 lets it follow the compiler again. */
    private void setLanguageVersion(final int version) {
        final ProjectFile project = this.loader.projects().get(this.propertiesOf);
        if (project == null) {
            return;
        }
        this.loader.saveProject(project.withLanguageVersion(version));
        refreshVersionButtons();
    }

    /** Lights the version the open project is held to, Default when it names none, and unlights the rest. */
    private void refreshVersionButtons() {
        final ProjectFile project = this.loader.projects().get(this.propertiesOf);
        final int current = project == null ? 0 : project.languageVersion();
        for (int i = 0; i < this.versionButtons.size(); i++) {
            this.versionButtons.get(i).setPrimary(i == current);
        }
    }

    /** Whether the project the popup is open on builds something of its own, and so has a platform to pick. */
    private boolean buildsForAPlatform() {
        final ProjectFile project = this.loader.projects().get(this.propertiesOf);
        return project != null && project.buildsAListing();
    }

    /** Builds the project for that instruction set from now on, and lights the button that says so. */
    private void setPlatform(final String isa) {
        final ProjectFile project = this.loader.projects().get(this.propertiesOf);
        if (project == null) {
            return;
        }
        this.loader.saveProject(project.withPlatform(isa));
        refreshPlatformButtons();
    }

    /** Lights the instruction set the open project is built for, and unlights the rest. */
    private void refreshPlatformButtons() {
        final ProjectFile project = this.loader.projects().get(this.propertiesOf);
        final String current = project == null ? AsmProgram.DEFAULT_ISA : project.platform();
        final List<IsaSpec> all = Isas.all();
        for (int i = 0; i < this.platformButtons.size() && i < all.size(); i++) {
            this.platformButtons.get(i).setPrimary(all.get(i).id().equals(current));
        }
    }

    /** What the line under the buttons says: which machines the program will run on once it is built. */
    private Text platformHintText() {
        final ProjectFile project = this.loader.projects().get(this.propertiesOf);
        if (project == null) {
            return Text.EMPTY;
        }
        final IsaSpec built = Isas.byId(project.platform()).orElse(null);
        if (built == null) {
            return VirtualStudioTexts.NOTHING_ANSWERS.with(project.platform());
        }
        final List<String> runners = new ArrayList<>();
        for (final IsaSpec machine : Isas.all()) {
            if (machine.runs(built)) {
                runners.add(machine.name());
            }
        }
        return runners.isEmpty() ? VirtualStudioTexts.NO_MACHINE.text()
                : VirtualStudioTexts.RUNS_ON.with(String.join(GameText.resolve(VirtualStudioTexts.AND), runners));
    }

    /*
     * Green while the project is built for the instruction set a program gets when nobody asks, which is the one
     * that reaches every machine it could reach. Anything else is a decision to leave machines behind, and the line
     * goes amber so that it is a decision somebody sees themselves making.
     */
    private int platformHintColor() {
        final ProjectFile project = this.loader.projects().get(this.propertiesOf);
        return project != null && AsmProgram.DEFAULT_ISA.equals(project.platform())
                ? PALETTE.get().platformOn() : PALETTE.get().platformOff();
    }

    private void layoutOptions(final Popup p) {
        final List<UiComponent> c = p.children();
        c.get(0).setBounds(p.x() + 4, p.contentTop() + 4, 50, 9);
        this.tabStepper.setBounds(p.x() + 56, p.contentTop() + 2, 96, 12);
        this.completionsBox.setBounds(p.x() + 4, p.contentTop() + 18, p.width() - 8, 9);
        this.optionsClose.setBounds(p.right() - 38, p.bottom() - 14, 34, 11);
    }

    private void setTabSize(final int value) {
        this.tabSize = value;
        for (final CodeWorkspace.Doc doc : this.workspace.docs()) {
            doc.area().setTabSize(value);
        }
    }

    private void ask(final TextKey title, final String initial, final Consumer<String> action) {
        this.askTitle = GameText.resolve(title);
        this.askAction = action;
        this.askField.set(initial);
        // A file name opens with the caret before its extension, which is the part that gets typed over.
        final int dot = initial.lastIndexOf('.');
        this.askField.setCaret(dot > 0 ? dot : initial.length());
        this.ask.open();
        this.ask.focus(this.askField);
    }

    private void layoutAsk(final Popup p) {
        this.askField.setBounds(p.x() + 4, p.contentTop() + 3, p.width() - 8, 11);
        this.askOk.setBounds(p.right() - 38, p.bottom() - 15, 34, 11);
    }

    private String closingName() {
        final List<CodeWorkspace.Doc> docs = this.workspace.docs();
        return this.closing >= 0 && this.closing < docs.size() ? docs.get(this.closing).name() : "";
    }

    private void layoutAskClose(final Popup p) {
        final List<UiComponent> c = p.children();
        final int y = p.bottom() - 15;
        c.get(0).setBounds(p.x() + 4, y, 40, 11);
        c.get(1).setBounds(p.x() + 48, y, 60, 11);
        c.get(2).setBounds(p.right() - 44, y, 40, 11);
    }

    /** Closes a tab, asking first when it has changes the disk does not. */
    private void closeTab(final int index) {
        final List<CodeWorkspace.Doc> docs = this.workspace.docs();
        if (index < 0 || index >= docs.size()) {
            return;
        }
        if (docs.get(index).dirty()) {
            this.closing = index;
            this.askClose.open();
            return;
        }
        this.workspace.close(index);
    }

    private void showDock(final int tab) {
        this.dockTabs.setSelected(tab);
    }

    /** Types lines at the dock's terminal, one after the other as the machine answers each. */
    private void runInTerminal(final List<String> lines) {
        this.dockTabs.setSelected(DOCK_TERMINAL);
        this.typingInTerminal = true;
        this.typing.addAll(lines);
        if (!this.terminal.busy()) {
            typeNext();
        }
    }

    private void typeNext() {
        final String next = this.typing.poll();
        if (next != null) {
            this.terminal.run(next);
        }
    }

    /* The menus */

    private ContextMenu.Item item(final Text label, final boolean enabled, final Runnable action) {
        return new ContextMenu.Item(GameText.resolve(label), enabled, action);
    }

    private ContextMenu.Item item(final TextKey label, final boolean enabled, final Runnable action) {
        return item(label.text(), enabled, action);
    }

    private boolean hasDoc() {
        return this.workspace.current() != null;
    }

    private boolean hasSolution() {
        return this.loader.solution() != null;
    }

    private String currentProject() {
        final CodeWorkspace.Doc doc = this.workspace.current();
        if (doc != null) {
            for (final String name : this.loader.projects().keySet()) {
                if (doc.path().startsWith(this.loader.projectDir(name) + "/")) {
                    return name;
                }
            }
        }
        return this.loader.startupName();
    }

    private List<ContextMenu.Item> fileMenu() {
        final List<ContextMenu.Item> items = new ArrayList<>(List.of(
                item(VirtualStudioTexts.NEW_PROJECT, true, () -> this.wizard.open(false)),
                item(StudioTexts.NEW_FILE, this.page == Page.SOLUTION, this::newFile),
                ContextMenu.Item.separator(),
                item(VirtualStudioTexts.OPEN_SOLUTION_ITEM, true, this::openSolutionByPicker),
                item(StudioTexts.OPEN_FOLDER_ITEM, true, this::openFolderByPicker),
                item(StudioTexts.OPEN_FILE_ITEM, this.page == Page.SOLUTION, this::goToFile)));
        for (final String dir : this.loader.recent()) {
            items.add(item(StudioTexts.RECENT.with(shortName(dir)), true, () -> openSolutionFolder(dir)));
        }
        items.add(ContextMenu.Item.separator());
        items.add(item(StudioTexts.SAVE, hasDoc(), this.workspace::save));
        items.add(item(StudioTexts.SAVE_ALL, this.workspace.anyDirty(), this.workspace::saveAll));
        items.add(ContextMenu.Item.separator());
        items.add(item(VirtualStudioTexts.CLOSE_SOLUTION, this.page == Page.SOLUTION, this::closeSolution));
        items.add(item(StudioTexts.EXIT, true, () -> ActiveDesktop.requestClose(KEY)));
        return items;
    }

    private List<ContextMenu.Item> editMenu() {
        return List.of(
                item(StudioTexts.FIND_ITEM, hasDoc(), this::find),
                item(StudioTexts.GO_TO_LINE_ITEM, hasDoc(), this::goToLine),
                item(StudioTexts.TOGGLE_LINE_COMMENT, hasDoc(), this::toggleComment));
    }

    private List<ContextMenu.Item> viewMenu() {
        return List.of(
                item(VirtualStudioTexts.ERROR_LIST, true, () -> this.dockTabs.setSelected(DOCK_ERRORS)),
                item(VirtualStudioTexts.OUTPUT, true, () -> this.dockTabs.setSelected(DOCK_OUTPUT)),
                item(StudioTexts.TERMINAL, true, () -> {
                    this.dockTabs.setSelected(DOCK_TERMINAL);
                    this.typingInTerminal = true;
                }),
                item(VirtualStudioTexts.ASSEMBLY, hasSolution() && this.loader.projects().containsKey(currentProject()),
                        this::openAssembly),
                ContextMenu.Item.separator(),
                item(StudioTexts.ZOOM_IN, hasDoc(), () -> zoomEditor(1)),
                item(StudioTexts.ZOOM_OUT, hasDoc(), () -> zoomEditor(-1)),
                item(StudioTexts.RESET_ZOOM, hasDoc(), () -> setEditorScale(1.0f)),
                ContextMenu.Item.separator(),
                item(VirtualStudioTexts.START_WINDOW, true, this::closeSolution));
    }

    /**
     * The menu the right button opens on the code: the clipboard, then what the studio can do to the
     * code where the caret is, the way a studio keeps its refactorings a click away from the code.
     */
    private List<ContextMenu.Item> editorMenu() {
        final CodeWorkspace.Doc doc = this.workspace.current();
        final boolean selected = doc != null && doc.area().document().hasSelection();
        return List.of(
                item(StudioTexts.CUT, selected, () -> pressInEditor(GLFW.GLFW_KEY_X)),
                item(StudioTexts.COPY, hasDoc(), () -> pressInEditor(GLFW.GLFW_KEY_C)),
                item(StudioTexts.PASTE, hasDoc(), () -> pressInEditor(GLFW.GLFW_KEY_V)),
                item(StudioTexts.SELECT_ALL, hasDoc(), () -> pressInEditor(GLFW.GLFW_KEY_A)),
                ContextMenu.Item.separator(),
                item(StudioTexts.TOGGLE_LINE_COMMENT, hasDoc(), this::toggleComment),
                ContextMenu.Item.submenu(GameText.resolve(VirtualStudioTexts.QUICK_ACTIONS), List.of(
                        item(StudioTexts.IMPLEMENT_INTERFACE, hasDoc(), this::implementInterface))),
                ContextMenu.Item.separator(),
                item(StudioTexts.GO_TO_LINE_ITEM, hasDoc(), this::goToLine));
    }

    /** Sends a Ctrl key to the code area, which is where the clipboard commands live. */
    private void pressInEditor(final int key) {
        final CodeWorkspace.Doc doc = this.workspace.current();
        if (doc != null) {
            doc.area().keyPressed(key, 0, GLFW.GLFW_MOD_CONTROL);
        }
    }

    /** The size the code is drawn at, shared by every open file, as the status bar shows it. */
    private float editorScale = 1.0f;

    private void zoomEditor(final int steps) {
        setEditorScale(this.editorScale + steps * 0.25f);
    }

    private void setEditorScale(final float value) {
        this.editorScale = Math.max(0.75f, Math.min(2.0f, value));
        for (final CodeWorkspace.Doc doc : this.workspace.docs()) {
            doc.area().setScale(this.editorScale);
        }
    }

    /** The menu the right button opens on the code. */
    private final ContextMenu editorContext = new ContextMenu(110, 11);

    /** The desktop-local centre of the code area, where a test right-clicks the code; null with no file open. */
    public int[] editorCenter() {
        final CodeWorkspace.Doc doc = this.workspace.current();
        if (doc == null) {
            return null;
        }
        final CodeArea area = doc.area();
        return new int[] {area.x() + area.width() / 2, area.y() + area.height() / 2};
    }

    /** Whether the right-button menu on the code is up. */
    public boolean editorMenuOpen() {
        return this.editorContext.isOpen();
    }

    /** Escape closes whatever menu or window is up before it means anything to the desktop. */
    @Override
    public boolean wantsEscape() {
        return this.editorContext.isOpen() || this.treeContext.isOpen() || this.menuBar.isOpen()
                || this.palette.isOpen() || openPopup() != null;
    }

    /** The labels of the code's right-button menu, so a test can read what it offers. */
    public List<String> editorMenuLabels() {
        final List<String> out = new ArrayList<>();
        for (final ContextMenu.Item entry : this.editorContext.items()) {
            out.add(entry.label());
        }
        return out;
    }

    /** The size the code is drawn at, as the status bar shows it. */
    public int zoomPercent() {
        return Math.round(this.editorScale * 100);
    }

    private List<ContextMenu.Item> projectMenu() {
        final boolean has = hasSolution() && this.loader.projects().containsKey(currentProject());
        return List.of(
                item(VirtualStudioTexts.ADD_NEW_ITEM_ITEM, has, this::addNewItem),
                item(VirtualStudioTexts.ADD_EXISTING_ITEM, has, this::addExistingItem),
                item(VirtualStudioTexts.ADD_REFERENCE, has && this.loader.projects().size() > 1, this::addReference),
                item(VirtualStudioTexts.ADD_NEW_PROJECT, hasSolution(), () -> this.wizard.open(true)),
                ContextMenu.Item.separator(),
                item(VirtualStudioTexts.SET_STARTUP, has, this::setStartup),
                item(VirtualStudioTexts.PROPERTIES, has, () -> showProperties(currentProject())));
    }

    private List<ContextMenu.Item> buildMenu() {
        final String name = currentProject();
        return List.of(
                item(VirtualStudioTexts.BUILD_SOLUTION, this.page == Page.SOLUTION, this::buildSolution),
                item(VirtualStudioTexts.REBUILD_SOLUTION, this.page == Page.SOLUTION, this::buildSolution),
                item(VirtualStudioTexts.CLEAN_SOLUTION, hasSolution(), this.build::cleanSolution),
                item(name.isEmpty() ? VirtualStudioTexts.BUILD_PROJECT.text()
                        : VirtualStudioTexts.BUILD_NAMED.with(name), hasSolution() && !name.isEmpty(),
                        () -> this.build.buildProject(name)),
                ContextMenu.Item.separator(),
                item(VirtualStudioTexts.PACKAGE, hasSolution() && !this.loader.startupName().isEmpty(),
                        this.build::packageStartup));
    }

    private List<ContextMenu.Item> debugMenu() {
        return List.of(
                item(StudioTexts.START, this.page == Page.SOLUTION, this::startProgram),
                item(StudioTexts.STOP, this.terminal.busy(), this.terminal::interrupt));
    }

    private List<ContextMenu.Item> toolsMenu() {
        return List.of(item(VirtualStudioTexts.OPTIONS_ITEM, true, () -> {
            this.tabStepper.setAmount(this.tabSize);
            this.options.open();
        }));
    }

    private List<ContextMenu.Item> helpMenu() {
        return List.of(item(VirtualStudioTexts.ABOUT_ITEM, true,
                () -> this.workspace.say(GameText.resolve(VirtualStudioTexts.ABOUT))));
    }

    /* What the menus do */

    /** The kinds the Open Project/Solution window offers: what the studio opens as a solution, then everything. */
    private static List<FileDialog.Filter> solutionFilters() {
        return List.of(FileDialog.Filter.of(VirtualStudioTexts.SOLUTIONS, SolutionFile.EXTENSION,
                        LanguageLevel.SIGMA_SHARP.projectExtension(), LanguageLevel.SIGMA.projectExtension()),
                FileDialog.Filter.ALL);
    }

    private void openSolutionByPicker() {
        this.dialog.openFile(VirtualStudioTexts.OPEN_SOLUTION_TITLE.text(), LOCATION, solutionFilters(),
                this::openFile);
    }

    private void openFolderByPicker() {
        this.dialog.openFolder(StudioTexts.OPEN_FOLDER.text(), LOCATION, this::openFolder);
    }

    /** Shows the Open Project/Solution window, as the File menu does; a test drives it from here. */
    public void showOpenSolutionDialog() {
        openSolutionByPicker();
    }

    /** The system's file window this studio opens, for a test to drive. */
    public FileDialog dialog() {
        return this.dialog;
    }

    private void goToFile() {
        final List<CommandPalette.Entry> entries = new ArrayList<>();
        for (final CodeWorkspace.TreeRow row : this.workspace.tree()) {
            if (!row.file().directory()) {
                final String path = row.file().path();
                entries.add(new CommandPalette.Entry(shortName(path), "", () -> this.workspace.open(path)));
            }
        }
        for (final ProjectFile project : this.loader.projects().values()) {
            for (final String source : project.sources()) {
                final String path = join(this.loader.projectDir(project.name()), source);
                entries.add(new CommandPalette.Entry(project.name() + "/" + source, "", () -> this.workspace.open(path)));
            }
        }
        this.palette.open(entries, "");
    }

    private void newFile() {
        final String project = currentProject();
        ask(StudioTexts.NEW_FILE, "untitled" + sourceSuffix(this.loader.projects().get(project)), name -> {
            if (name.isEmpty()) {
                return;
            }
            final String dir = this.loader.projects().containsKey(project) ? this.loader.projectDir(project)
                    : this.loader.dir();
            this.workspace.newFile(join(dir, name));
            setTabSize(this.tabSize);
        });
    }

    private void addNewItem() {
        addNewItem(currentProject());
    }

    private void addNewItem(final String name) {
        final ProjectFile project = this.loader.projects().get(name);
        if (project == null) {
            return;
        }
        ask(VirtualStudioTexts.ADD_NEW_ITEM, "Class1" + sourceSuffix(project), file -> {
            if (file.isEmpty()) {
                return;
            }
            this.workspace.newFile(join(this.loader.projectDir(name), file));
            this.loader.saveProject(project.withSource(file));
        });
    }

    private void addExistingItem() {
        addExistingItem(currentProject());
    }

    private void addExistingItem(final String name) {
        final ProjectFile project = this.loader.projects().get(name);
        if (project == null) {
            return;
        }
        final List<CommandPalette.Entry> entries = new ArrayList<>();
        for (final CodeWorkspace.TreeRow row : this.workspace.tree()) {
            final String path = row.file().path();
            if (!row.file().directory() && path.startsWith(this.loader.projectDir(name) + "/")
                    && path.endsWith(sourceSuffix(project))) {
                final String relative = path.substring(this.loader.projectDir(name).length() + 1);
                if (!project.sources().contains(relative)) {
                    entries.add(new CommandPalette.Entry(relative, "",
                            () -> this.loader.saveProject(project.withSource(relative))));
                }
            }
        }
        this.palette.open(entries, "");
    }

    private void addReference() {
        addReference(currentProject());
    }

    private void addReference(final String name) {
        final ProjectFile project = this.loader.projects().get(name);
        if (project == null) {
            return;
        }
        final List<CommandPalette.Entry> entries = new ArrayList<>();
        for (final String other : this.loader.projects().keySet()) {
            if (!other.equals(name) && !project.references().contains(other)) {
                entries.add(new CommandPalette.Entry(other, "",
                        () -> this.loader.saveProject(project.withReference(other))));
            }
        }
        this.palette.open(entries, "");
    }

    private void setStartup() {
        setStartup(currentProject());
    }

    private void setStartup(final String name) {
        if (this.loader.solution() != null && this.loader.projects().containsKey(name)) {
            this.loader.setSolution(this.loader.solution().withStartup(name));
            this.loader.saveSolution();
        }
    }

    /* The tree's right-button menu */

    /** What the right button offers on a row of the Solution Explorer, by what the row is. */
    private List<ContextMenu.Item> treeMenu(final Node node) {
        final boolean several = this.loader.projects().size() > 1;
        return switch (node.kind()) {
            case SOLUTION -> this.loader.solution() == null
                    ? List.of(item(VirtualStudioTexts.REFRESH, true, this.workspace::refresh))
                    : List.of(item(VirtualStudioTexts.BUILD_SOLUTION, true, this::buildSolution),
                            item(VirtualStudioTexts.CLEAN_SOLUTION, true, this.build::cleanSolution),
                            ContextMenu.Item.separator(),
                            item(VirtualStudioTexts.ADD_NEW_PROJECT, true, () -> this.wizard.open(true)));
            case PROJECT -> List.of(
                    item(VirtualStudioTexts.BUILD, true, () -> this.build.buildProject(node.project())),
                    item(VirtualStudioTexts.SET_STARTUP, true, () -> setStartup(node.project())),
                    ContextMenu.Item.separator(),
                    item(VirtualStudioTexts.ADD_NEW_ITEM_ITEM, true, () -> addNewItem(node.project())),
                    item(VirtualStudioTexts.ADD_EXISTING_ITEM, true, () -> addExistingItem(node.project())),
                    item(VirtualStudioTexts.ADD_REFERENCE, several, () -> addReference(node.project())),
                    ContextMenu.Item.separator(),
                    item(VirtualStudioTexts.PROPERTIES, true, () -> showProperties(node.project())));
            case SOURCE -> List.of(
                    item(VirtualStudioTexts.OPEN, true, () -> this.workspace.open(node.path())),
                    ContextMenu.Item.separator(),
                    item(VirtualStudioTexts.EXCLUDE, true, () -> excludeSource(node)),
                    item(VirtualStudioTexts.DELETE, true, () -> askDelete(node)));
            case OUTPUT -> List.of(
                    item(VirtualStudioTexts.OPEN, true, () -> this.workspace.open(node.path())),
                    ContextMenu.Item.separator(),
                    item(VirtualStudioTexts.DELETE, true, () -> askDelete(node)));
            case FOLDER_FILE -> {
                final DiskFilesPayload.WireFile file = fileAt(node.path());
                yield file == null || file.directory()
                        ? List.of(item(VirtualStudioTexts.OPEN, true, () -> this.workspace.toggleFolder(node.path())))
                        : List.of(item(VirtualStudioTexts.OPEN, true, () -> this.workspace.open(node.path())),
                                ContextMenu.Item.separator(),
                                item(VirtualStudioTexts.DELETE, true, () -> askDelete(node)));
            }
            default -> List.of();
        };
    }

    /** Takes the source out of its project's file, leaving the file itself on the disk. */
    private void excludeSource(final Node node) {
        final ProjectFile project = this.loader.projects().get(node.project());
        if (project != null) {
            this.loader.saveProject(project.withoutSource(relativeSource(project, node.path())));
        }
    }

    /** A source's name as the project file lists it: its path inside the project's folder. */
    private String relativeSource(final ProjectFile project, final String path) {
        final String prefix = this.loader.projectDir(project.name()) + "/";
        return path.startsWith(prefix) ? path.substring(prefix.length()) : shortName(path);
    }

    /** Asks before a file goes: deleting is the one thing the tree does that cannot be undone. */
    private void askDelete(final Node node) {
        this.deleting = node;
        this.askDelete.open();
    }

    private String deletingName() {
        return this.deleting == null ? "" : shortName(this.deleting.path());
    }

    private void layoutAskDelete(final Popup p) {
        final List<UiComponent> c = p.children();
        final int y = p.bottom() - 15;
        c.get(0).setBounds(p.x() + 4, y, 44, 11);
        c.get(1).setBounds(p.right() - 44, y, 40, 11);
    }

    /**
     * Deletes the file the question was about: off the disk, off its tab if it was open, and out of
     * its project's file when the project listed it.
     */
    private void deleteNode() {
        final Node node = this.deleting;
        this.deleting = null;
        if (node == null || node.path().isEmpty()) {
            return;
        }
        final List<CodeWorkspace.Doc> docs = this.workspace.docs();
        for (int i = 0; i < docs.size(); i++) {
            if (docs.get(i).path().equals(node.path())) {
                this.workspace.close(i);
                break;
            }
        }
        PacketDistributor.sendToServer(new DeleteFilePayload(this.host, node.path()));
        final ProjectFile project = this.loader.projects().get(node.project());
        if (node.kind() == NodeKind.SOURCE && project != null) {
            this.loader.saveProject(project.withoutSource(relativeSource(project, node.path())));
        }
        this.build.print(VirtualStudioTexts.DELETED.with(shortName(node.path())));
        FilesApps.diskChanged();
        this.workspace.refresh();
    }

    /** The labels of the tree's rows, top to bottom, so a test can find one. */
    public List<String> explorerLabels() {
        final List<String> out = new ArrayList<>();
        for (final Node node : nodes()) {
            out.add(node.label());
        }
        return out;
    }

    /** The middle of the tree row with that label, window-relative, or null when there is none. */
    public int[] explorerRowPoint(final String label) {
        final int index = explorerLabels().indexOf(label);
        return index < 0 ? null : this.explorer.rowCenter(index);
    }

    /** Whether the tree's right-button menu is up. */
    public boolean treeMenuOpen() {
        return this.treeContext.isOpen();
    }

    /** The labels of the tree's right-button menu, so a test can read what it offers. */
    public List<String> treeMenuLabels() {
        final List<String> out = new ArrayList<>();
        for (final ContextMenu.Item entry : this.treeContext.items()) {
            out.add(entry.label());
        }
        return out;
    }

    /** The middle of the tree menu's entry with that label, or null. */
    public int[] treeMenuPoint(final String label) {
        final int index = treeMenuLabels().indexOf(label);
        return index < 0 ? null : this.treeContext.itemCenter(index);
    }

    /** Whether the question before a delete is up. */
    public boolean deleteQuestionOpen() {
        return this.askDelete.isOpen();
    }

    /** Whether a question asking for a name is up. */
    public boolean askOpen() {
        return this.ask.isOpen();
    }

    /** Answers the name question up with {@code name}, as typing it and pressing OK does. */
    public void answerAsk(final String name) {
        if (this.ask.isOpen()) {
            this.askField.set(name);
            this.ask.close();
            this.askAction.accept(name.trim());
        }
    }

    /** Answers the question before a delete, as its Delete button does. */
    public void confirmDelete() {
        if (this.askDelete.isOpen()) {
            this.askDelete.close();
            deleteNode();
        }
    }

    /** Whether a project's Properties window is up. */
    public boolean propertiesOpen() {
        return this.properties.isOpen();
    }

    /** What the version buttons say, Default first, while the open project's language has versions. */
    public List<String> versionButtonLabels() {
        final List<String> out = new ArrayList<>();
        for (final Button button : this.versionButtons) {
            if (button.visible()) {
                out.add(button.label());
            }
        }
        return out;
    }

    /** Where the version button that says {@code label} is drawn, for a test to click it, or null. */
    public int[] versionButtonPoint(final String label) {
        for (final Button button : this.versionButtons) {
            if (button.visible() && button.label().equals(label)) {
                return new int[] {button.x() + button.width() / 2, button.y() + button.height() / 2};
            }
        }
        return null;
    }

    /** The lines under the version buttons, as the window shows them. */
    public List<String> versionHintLines() {
        final List<String> out = new ArrayList<>();
        for (final Label hint : this.versionHints) {
            if (hint.visible()) {
                out.add(hint.text());
            }
        }
        return out;
    }

    private void openAssembly() {
        final ProjectFile project = this.loader.projects().get(currentProject());
        if (project != null && project.buildsAListing()) {
            this.workspace.open(join(this.loader.projectDir(project.name()), project.entry()));
        }
    }

    private void goToLine() {
        ask(StudioTexts.GO_TO_LINE, "", value -> {
            final CodeWorkspace.Doc doc = this.workspace.current();
            try {
                if (doc != null) {
                    doc.area().document().setCursor(Integer.parseInt(value) - 1, 0);
                }
            } catch (final NumberFormatException ignored) {
                this.workspace.say(GameText.resolve(StudioTexts.NOT_A_LINE.with(value)));
            }
        });
    }

    private void find() {
        ask(StudioTexts.FIND, "", needle -> {
            final CodeWorkspace.Doc doc = this.workspace.current();
            if (doc != null && !doc.area().document().find(needle)) {
                this.workspace.say(GameText.resolve(StudioTexts.NO_RESULTS.with(needle)));
            }
        });
    }

    /** Writes the methods the class under the caret promised its interface and left out. */
    private void implementInterface() {
        final CodeWorkspace.Doc doc = this.workspace.current();
        if (doc != null) {
            this.workspace.say(GameText.resolve(this.workspace.implementInterface(doc)
                    ? StudioTexts.INTERFACE_IMPLEMENTED : StudioTexts.NOTHING_TO_IMPLEMENT));
        }
    }

    private void toggleComment() {
        final CodeWorkspace.Doc doc = this.workspace.current();
        if (doc != null) {
            doc.area().document().toggleLinePrefix("// ");
            this.workspace.edited();
        }
    }

    /* The window */

    @Override
    public void applySkin(final OsSkin osSkin) {
        this.skin = osSkin;
        this.workspace.setPalette(InkPalette.forGround(osSkin.isDark()));
        this.terminal.setSkin(osSkin);
    }

    @Override
    public String title() {
        final CodeWorkspace.Doc doc = this.workspace.current();
        final String where = this.loader.solution() != null ? this.loader.solution().name()
                : this.page == Page.SOLUTION ? shortName(this.loader.dir()) : "";
        if (doc == null) {
            return where.isEmpty() ? "Virtual Studio" : where + " - Virtual Studio";
        }
        return doc.name() + (doc.dirty() ? " *" : "") + " - " + (where.isEmpty() ? "" : where + " - ") + "Virtual Studio";
    }

    @Override
    public int defaultWidth() {
        return 320;
    }

    @Override
    public int defaultHeight() {
        return 200;
    }

    @Override
    public int minWidth() {
        return 240;
    }

    @Override
    public int minHeight() {
        return 130;
    }

    @Override
    public void onRestored() {
        if (this.page == Page.SOLUTION) {
            this.workspace.refresh();
        }
    }

    @Override
    public void onClosed() {
        this.workspace.release();
        this.dialog.release();
        this.terminal.release();
        CodeFileReplies.forget(this);
    }

    /** What the dock's terminal has printed, one line after another. */
    public String terminalText() {
        return this.terminal.scrollbackText();
    }

    @Override
    public boolean modalActive() {
        // The file window is a window of its own over this one; the desktop holds this one while it is up.
        return this.wizard.isOpen() || this.ask.isOpen()
                || this.properties.isOpen() || this.options.isOpen() || this.askClose.isOpen()
                || this.askDelete.isOpen();
    }

    @Override
    public void renderContent(final GuiGraphics g, final Font font, final int x, final int y,
                              final int width, final int height, final int mouseX, final int mouseY,
                              final float partialTick) {
        final UiContext ctx = new UiContext(this.skin, font, mouseX, mouseY, partialTick);
        g.fill(x, y, x + width, y + height, this.skin.windowBg());
        this.root.setBounds(x, y, width, height);
        this.menuBar.setBounds(x, y, width, MenuBar.HEIGHT);
        this.menuBar.setWindow(x, y, width, height);
        this.winX = x;
        this.winY = y;
        this.winW = width;
        this.winH = height;
        final int top = y + MenuBar.HEIGHT;

        final CodeWorkspace.Doc doc = this.workspace.current();
        // What the Start Window does not show is hidden outright: a list with no room still has rows to draw.
        final boolean start = this.page == Page.START;
        for (final UiComponent part
                : List.of(this.start, this.explorer, this.tabs, this.dockTabs, this.errors, this.outputList,
                        this.terminal)) {
            part.setVisible(!start);
        }
        if (start) {
            drawStartWindow(g, font, x, top, width, height - MenuBar.HEIGHT - STATUS_H);
        } else {
            this.skin.panel(g, x, top, width, TOOLBAR_H);
            this.start.setBounds(x + 3, top + 2, 34, TOOLBAR_H - 4);
            final String config = GameText.resolve(VirtualStudioTexts.RELEASE);
            this.skin.field(g, x + 42, top + 2, 40, TOOLBAR_H - 4, false);
            Draw.text(g, font, font.plainSubstrByWidth(config, 36), x + 45, top + 3, this.skin.text());
            final int bodyY = top + TOOLBAR_H;
            final int bodyH = height - MenuBar.HEIGHT - TOOLBAR_H - STATUS_H;
            final int codeX = x;
            // The dividers hold their places between frames, within what the window can afford.
            this.sideW = Math.max(60, Math.min(width - 120, this.sideW));
            final int codeW = width - this.sideW;
            final int sideX = x + codeW;
            this.skin.panel(g, sideX, bodyY, this.sideW, bodyH);
            Draw.text(g, font, font.plainSubstrByWidth(GameText.resolve(VirtualStudioTexts.SOLUTION_EXPLORER),
                    this.sideW - 6), sideX + 3, bodyY + 1, this.skin.dim());
            this.explorer.setBounds(sideX, bodyY + CAPTION_H, this.sideW, bodyH - CAPTION_H);
            this.skin.panel(g, codeX, bodyY, codeW, TAB_H);
            this.tabs.setBounds(codeX, bodyY, codeW, TAB_H);
            this.tabs.setSelected(this.workspace.currentIndex());
            final int paneY = bodyY + TAB_H;
            final int paneH = bodyH - TAB_H;
            this.dockH = Math.max(TAB_H + ROW_H * 2, Math.min(paneH - MIN_CODE_H, this.dockH));
            final boolean dockShown = paneH - this.dockH >= MIN_CODE_H;
            final int codeH = dockShown ? paneH - this.dockH : paneH;
            if (doc != null) {
                doc.area().setBounds(codeX, paneY, codeW, codeH);
            }
            layoutDock(codeX, paneY + codeH, codeW, dockShown ? this.dockH : 0);
            if (dockShown && this.dockTabs.selected() == DOCK_ERRORS) {
                drawErrorHeader(g, font, codeX, paneY + codeH + TAB_H, codeW);
            }
            if (doc == null) {
                drawEmpty(g, font, codeX, paneY, codeW, codeH);
            }
            this.dividerX = sideX;
            this.dividerY = paneY + codeH;
            this.bodyTop = bodyY;
            this.bodyBottom = bodyY + bodyH;
        }
        this.root.render(g, ctx);
        if (doc != null && this.page == Page.SOLUTION) {
            doc.area().render(g, ctx);
        }
        drawStatus(g, font, x, y + height - STATUS_H, width, doc);
        this.completions.render(g, ctx);
        this.palette.render(g, ctx, x, top, width);
        for (final Popup popup : popups()) {
            if (popup.isOpen()) {
                popup.renderIn(g, ctx, x, y, width, height);
            }
        }
        this.editorContext.render(g, ctx);
        this.treeContext.render(g, ctx);
        this.menuBar.render(g, ctx);
    }

    private void layoutDock(final int x, final int y, final int width, final int height) {
        final boolean shown = height > 0;
        final int tab = this.dockTabs.selected();
        this.dockTabs.setVisible(shown);
        this.errors.setVisible(shown && tab == DOCK_ERRORS);
        this.outputList.setVisible(shown && tab == DOCK_OUTPUT);
        this.terminal.setVisible(shown && tab == DOCK_TERMINAL);
        this.dockTabs.setBounds(x, y, width, TAB_H);
        // The error list sits under its column headings; the others fill the dock.
        this.errors.setBounds(x, y + TAB_H + CAPTION_H, width, Math.max(0, height - TAB_H - CAPTION_H));
        this.outputList.setBounds(x, y + TAB_H, width, Math.max(0, height - TAB_H));
        this.terminal.setBounds(x, y + TAB_H, width, Math.max(0, height - TAB_H));
    }

    /* The Error List's columns: where each starts, as a share of the dock's width. */
    private static final int COL_CODE_W = 34;
    private static final int COL_FILE_W = 58;
    private static final int COL_LINE_W = 22;

    /** The headings over the Error List, in the columns the rows use. */
    private void drawErrorHeader(final GuiGraphics g, final Font font, final int x, final int y, final int width) {
        this.skin.panel(g, x, y, width, CAPTION_H);
        final int fileX = x + width - COL_LINE_W - COL_FILE_W;
        Draw.text(g, font, font.plainSubstrByWidth(GameText.resolve(VirtualStudioTexts.CODE_COLUMN), COL_CODE_W - 2),
                x + 3, y + 1, this.skin.dim());
        Draw.text(g, font, GameText.resolve(VirtualStudioTexts.DESCRIPTION_COLUMN), x + 3 + COL_CODE_W, y + 1,
                this.skin.dim());
        Draw.text(g, font, font.plainSubstrByWidth(GameText.resolve(VirtualStudioTexts.FILE_COLUMN), COL_FILE_W - 2),
                fileX, y + 1, this.skin.dim());
        Draw.text(g, font, font.plainSubstrByWidth(GameText.resolve(VirtualStudioTexts.LINE_COLUMN), COL_LINE_W - 2),
                x + width - COL_LINE_W, y + 1, this.skin.dim());
    }

    /** The Start Window: what was opened lately on the left, the ways to begin on the right. */
    private void drawStartWindow(final GuiGraphics g, final Font font, final int x, final int y,
                                 final int width, final int height) {
        final InkPalette palette = InkPalette.forGround(this.skin.isDark()).get();
        this.links.clear();
        // The recent list needs less room than the cards, whose titles are whole sentences.
        final int half = width * 2 / 5;
        this.skin.panel(g, x, y, half, height);
        g.fill(x + half, y, x + width, y + height, palette.ground());
        Draw.pushScissor(g, x, y, x + width, y + height);
        int ly = y + 6;
        Draw.text(g, font, GameText.resolve(VirtualStudioTexts.OPEN_RECENT), x + 6, ly, this.skin.text());
        ly += 12;
        final List<String> recent = this.loader.recent();
        if (recent.isEmpty()) {
            Draw.text(g, font, GameText.resolve(StudioTexts.NOTHING_YET), x + 6, ly, this.skin.dim());
        }
        for (final String dir : recent) {
            final String label = SolutionFile.fileName(shortName(dir));
            Draw.text(g, font, label, x + 6, ly, this.skin.accent());
            Draw.text(g, font, font.plainSubstrByWidth("C:\\" + dir.replace('/', '\\'), half - 12), x + 6, ly + 9,
                    this.skin.dim());
            this.links.add(new Link(label, x + 6, ly - 1, half - 12, 18, () -> openSolutionFolder(dir)));
            ly += 20;
        }
        int ry = y + 6;
        final int rx = x + half + 8;
        Draw.text(g, font, GameText.resolve(VirtualStudioTexts.GET_STARTED), rx, ry, palette.plain());
        ry += 12;
        final int cardW = width - half - 16;
        ry = card(g, font, rx, ry, cardW, VirtualStudioTexts.OPEN_SOLUTION_CARD, VirtualStudioTexts.OPEN_SOLUTION_HINT,
                this::openSolutionByPicker, palette);
        ry = card(g, font, rx, ry, cardW, VirtualStudioTexts.OPEN_FOLDER_CARD, VirtualStudioTexts.OPEN_FOLDER_HINT,
                this::openFolderByPicker, palette);
        ry = card(g, font, rx, ry, cardW, VirtualStudioTexts.OPEN_FILE_CARD, VirtualStudioTexts.OPEN_FILE_HINT,
                this::openFileFromStart, palette);
        card(g, font, rx, ry, cardW, VirtualStudioTexts.CREATE_PROJECT, VirtualStudioTexts.CREATE_PROJECT_HINT,
                () -> this.wizard.open(false), palette);
        Draw.popScissor(g);
    }

    private int card(final GuiGraphics g, final Font font, final int x, final int y, final int width,
                     final TextKey titleKey, final TextKey subKey, final Runnable action, final InkPalette palette) {
        final String title = GameText.resolve(titleKey);
        g.fill(x, y, x + width, y + 20, palette.gutter());
        Draw.text(g, font, font.plainSubstrByWidth(title, width - 6), x + 3, y + 2, palette.plain());
        Draw.text(g, font, font.plainSubstrByWidth(GameText.resolve(subKey), width - 6), x + 3, y + 11,
                palette.gutterText());
        this.links.add(new Link(title, x, y, width, 20, action));
        return y + 23;
    }

    /** Open a file from the Start Window: pick its folder, then the file. */
    private void openFileFromStart() {
        this.dialog.openFile(StudioTexts.OPEN_FILE.text(), LOCATION, FileDialog.Filter.sources(), this::openFile);
    }

    private void drawEmpty(final GuiGraphics g, final Font font, final int x, final int y,
                           final int width, final int height) {
        final InkPalette palette = InkPalette.forGround(this.skin.isDark()).get();
        g.fill(x, y, x + width, y + height, palette.ground());
        Draw.pushScissor(g, x, y, x + width, y + height);
        Draw.text(g, font, GameText.resolve(this.loader.solution() == null ? VirtualStudioTexts.EMPTY_FOLDER
                : VirtualStudioTexts.EMPTY_SOLUTION), x + 6, y + 6, palette.gutterText());
        Draw.popScissor(g);
    }

    private void drawStatus(final GuiGraphics g, final Font font, final int x, final int y,
                            final int width, final CodeWorkspace.Doc doc) {
        this.skin.statusBar(g, x, y, width, STATUS_H);
        final int errorCount = errorRows().size();
        final String left = GameText.resolve(errorCount == 0 ? VirtualStudioTexts.READY.text()
                : VirtualStudioTexts.ERRORS.with(errorCount));
        Draw.text(g, font, left, x + 3, y + 1, this.skin.dim());
        /*
         * The right-hand pieces are laid out first, so the message on the left is cut to the room
         * left before them: a saved path can be longer than the bar, and it stops rather than runs
         * under the zoom.
         */
        final int right = drawStatusRight(g, font, x, y, width, doc);
        if (!this.workspace.status().isEmpty()) {
            final int from = x + 5 + font.width(left) + 6;
            final int room = right - 6 - from;
            if (room > 8) {
                Draw.text(g, font, font.plainSubstrByWidth(this.workspace.status(), room), from, y + 1,
                        this.skin.dim());
            }
        }
    }

    /** Draws what the status bar keeps on its right, and says where that begins. */
    private int drawStatusRight(final GuiGraphics g, final Font font, final int x, final int y,
                                final int width, final CodeWorkspace.Doc doc) {
        int right = x + width - 3;
        if (this.loader.solution() != null) {
            right -= font.width(this.loader.solution().name());
            Draw.text(g, font, this.loader.solution().name(), right, y + 1, this.skin.dim());
            right -= 8;
        }
        if (doc != null) {
            final String where = GameText.resolve(EditorTexts.LINE_AND_COLUMN.with(
                    doc.area().document().cursorLine() + 1, doc.area().document().cursorCol() + 1));
            right -= font.width(where);
            Draw.text(g, font, where, right, y + 1, this.skin.dim());
            right -= 8;
            /*
             * The zoom, the way the studio keeps it at the bottom of the editor: the size in percent
             * with a minus and a plus either side, so a bigger or smaller text is one click away.
             */
            final String percent = Math.round(this.editorScale * 100) + "%";
            final int plusX = right - font.width("+") - 1;
            final int percentX = plusX - 3 - font.width(percent);
            final int minusX = percentX - 3 - font.width("-");
            Draw.text(g, font, "-", minusX, y + 1, this.skin.text());
            Draw.text(g, font, percent, percentX, y + 1, this.skin.dim());
            Draw.text(g, font, "+", plusX, y + 1, this.skin.text());
            this.zoomMinus = new int[] {minusX - 2, y, font.width("-") + 4, STATUS_H};
            this.zoomPlus = new int[] {plusX - 2, y, font.width("+") + 4, STATUS_H};
            right = minusX - 2;
        } else {
            this.zoomMinus = null;
            this.zoomPlus = null;
        }
        return right;
    }

    /* Where the status bar's zoom minus and plus were drawn, so a click on them is known for what it is. */
    private int[] zoomMinus;
    private int[] zoomPlus;

    private boolean clickZoomControl(final double mx, final double my) {
        if (inRect(this.zoomMinus, mx, my)) {
            zoomEditor(-1);
            return true;
        }
        if (inRect(this.zoomPlus, mx, my)) {
            zoomEditor(1);
            return true;
        }
        return false;
    }

    private static boolean inRect(final int[] rect, final double mx, final double my) {
        return rect != null && mx >= rect[0] && mx < rect[0] + rect[2] && my >= rect[1] && my < rect[1] + rect[3];
    }

    /* Input */

    /** Every popup the window can show, in the order they are drawn. */
    private List<Popup> popups() {
        return List.of(this.wizard.templates(), this.wizard.configure(), this.ask, this.properties, this.options,
                this.askClose, this.askDelete);
    }

    private Popup openPopup() {
        for (final Popup popup : popups()) {
            if (popup.isOpen()) {
                return popup;
            }
        }
        return null;
    }

    @Override
    public void mouseClicked(final DesktopWindow window, final double mouseX, final double mouseY, final int button) {
        final Popup popup = openPopup();
        if (popup != null) {
            popup.mouseClicked(mouseX, mouseY, button);
            return;
        }
        if (this.palette.isOpen()) {
            this.palette.mouseClicked(mouseX, mouseY, button);
            return;
        }
        if (this.editorContext.isOpen()) {
            this.editorContext.mouseClicked(mouseX, mouseY, button);
            return;
        }
        if (this.treeContext.isOpen()) {
            this.treeContext.mouseClicked(mouseX, mouseY, button);
            return;
        }
        final CodeWorkspace.Doc onCode = this.workspace.current();
        if (button == 1 && onCode != null && this.page == Page.SOLUTION && onCode.area().contains(mouseX, mouseY)) {
            this.editorContext.open(editorMenu(), (int) mouseX, (int) mouseY, window.x(), window.y(),
                    window.width(), window.height());
            return;
        }
        if (clickZoomControl(mouseX, mouseY)) {
            return;
        }
        // An open menu gets the click first: on one of its items, on another title, or outside to close.
        if (this.menuBar.mouseClicked(mouseX, mouseY, button)) {
            return;
        }
        if (this.completions.mouseClicked(mouseX, mouseY, button)) {
            this.workspace.edited();
            return;
        }
        this.completions.close();
        for (final Link link : this.links) {
            if (this.page == Page.START && mouseX >= link.x() && mouseX < link.x() + link.width()
                    && mouseY >= link.y() && mouseY < link.y() + link.height()) {
                link.action().run();
                return;
            }
        }
        if (this.page == Page.SOLUTION) {
            // A click on a divider takes hold of it; the drag that follows moves it.
            if (Math.abs(mouseX - this.dividerX) <= GRIP && mouseY >= this.bodyTop && mouseY < this.bodyBottom) {
                this.holding = 1;
                return;
            }
            if (Math.abs(mouseY - this.dividerY) <= GRIP && mouseX < this.dividerX && this.dockTabs.visible()) {
                this.holding = 2;
                return;
            }
        }
        if (this.terminal.visible() && this.terminal.contains(mouseX, mouseY)) {
            this.typingInTerminal = true;
        }
        final CodeWorkspace.Doc doc = this.workspace.current();
        if (doc != null && this.page == Page.SOLUTION && doc.area().contains(mouseX, mouseY)) {
            this.typingInTerminal = false;
            doc.area().mouseClicked(mouseX, mouseY, button);
            return;
        }
        this.root.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void mouseDragged(final DesktopWindow window, final double mouseX, final double mouseY, final int button) {
        if (this.holding == 1) {
            this.sideW = (int) (this.dividerX + this.sideW - mouseX);
            return;
        }
        if (this.holding == 2) {
            this.dockH = (int) (this.dividerY + this.dockH - mouseY);
            return;
        }
        final CodeWorkspace.Doc doc = this.workspace.current();
        if (doc != null && this.page == Page.SOLUTION && !modalActive()) {
            doc.area().mouseDragged(mouseX, mouseY, button);
        }
    }

    @Override
    public void mouseReleased(final DesktopWindow window, final double mouseX, final double mouseY, final int button) {
        this.holding = 0;
        final CodeWorkspace.Doc doc = this.workspace.current();
        if (doc != null) {
            doc.area().mouseReleased(mouseX, mouseY, button);
        }
    }

    @Override
    public boolean keyReleased(final int key, final int scanCode, final int modifiers) {
        return this.typingInTerminal && this.terminal.visible()
                && this.terminal.keyReleased(key, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(final char c) {
        final Popup popup = openPopup();
        if (popup != null) {
            return popup.charTyped(c);
        }
        if (this.palette.isOpen()) {
            return this.palette.charTyped(c);
        }
        if (this.typingInTerminal && this.terminal.visible()) {
            return this.terminal.charTyped(c);
        }
        final CodeWorkspace.Doc doc = this.workspace.current();
        if (doc == null || this.page != Page.SOLUTION || !doc.area().charTyped(c)) {
            return false;
        }
        this.workspace.edited();
        if (this.completionsOn && (c == '.' || this.completions.isOpen())) {
            offerCompletions(doc);
        }
        return true;
    }

    @Override
    public boolean keyPressed(final int key, final int scanCode, final int modifiers) {
        final boolean ctrl = (modifiers & GLFW.GLFW_MOD_CONTROL) != 0;
        final boolean shift = (modifiers & GLFW.GLFW_MOD_SHIFT) != 0;
        final Popup popup = openPopup();
        if (popup != null) {
            return popup.keyPressed(key, scanCode, modifiers);
        }
        if (this.palette.isOpen()) {
            return this.palette.keyPressed(key, scanCode, modifiers);
        }
        if (this.editorContext.isOpen()) {
            return this.editorContext.keyPressed(key, scanCode, modifiers);
        }
        if (this.treeContext.isOpen()) {
            return this.treeContext.keyPressed(key, scanCode, modifiers);
        }
        if (this.menuBar.keyPressed(key, scanCode, modifiers)) {
            return true;
        }
        if (ctrl && shift && key == GLFW.GLFW_KEY_N) {
            this.wizard.open(false);
            return true;
        }
        if (ctrl && shift && key == GLFW.GLFW_KEY_B) {
            buildSolution();
            return true;
        }
        if (ctrl && key == GLFW.GLFW_KEY_S) {
            this.workspace.save();
            return true;
        }
        if (ctrl && key == GLFW.GLFW_KEY_F) {
            find();
            return true;
        }
        if (ctrl && key == GLFW.GLFW_KEY_G) {
            goToLine();
            return true;
        }
        if (key == GLFW.GLFW_KEY_F5) {
            startProgram();
            return true;
        }
        if (ctrl && key == GLFW.GLFW_KEY_W) {
            closeTab(this.workspace.currentIndex());
            return true;
        }
        if (ctrl && key == GLFW.GLFW_KEY_PERIOD) {
            implementInterface();
            return true;
        }
        if (ctrl && key == GLFW.GLFW_KEY_GRAVE_ACCENT) {
            this.dockTabs.setSelected(DOCK_TERMINAL);
            this.typingInTerminal = true;
            return true;
        }
        if (this.typingInTerminal && this.terminal.visible()) {
            return this.terminal.keyPressed(key, scanCode, modifiers);
        }
        if (this.completions.keyPressed(key, scanCode, modifiers)) {
            this.workspace.edited();
            return true;
        }
        final CodeWorkspace.Doc doc = this.workspace.current();
        if (doc == null || this.page != Page.SOLUTION) {
            return false;
        }
        if (ctrl && key == GLFW.GLFW_KEY_SPACE && this.completionsOn) {
            offerCompletions(doc);
            return true;
        }
        if (doc.area().keyPressed(key, scanCode, modifiers)) {
            this.workspace.edited();
            if (this.completions.isOpen()) {
                offerCompletions(doc);
            }
            return true;
        }
        return false;
    }

    private void offerCompletions(final CodeWorkspace.Doc doc) {
        final CodeArea area = doc.area();
        this.completions.offer(area, doc.path(), sourcesAround(doc),
                new int[] {area.x(), area.y(), area.width(), area.height()}, this.loader.projectVersionOf(doc.path()));
    }

    /**
     * The sources a completion list reads beside the open file: the rest of its folder, and the sources
     * of every library its project references, so a type a library declares is offered like the file's
     * own. A library folder nobody has read yet is asked for, and is there the next time the list opens.
     */
    private List<IProgrammingLanguage.SourceText> sourcesAround(final CodeWorkspace.Doc doc) {
        final List<IProgrammingLanguage.SourceText> out = new ArrayList<>(this.workspace.siblingsOf(doc));
        final String own = this.loader.projectOf(doc.path());
        if (own == null) {
            return out;
        }
        final Deque<String> order = new ArrayDeque<>();
        this.loader.collectOrder(own, order, new LinkedHashSet<>());
        for (final String each : order) {
            if (each.equals(own)) {
                continue;
            }
            this.workspace.ensureSurveyed(this.loader.projectDir(each));
            for (final String source : this.loader.projects().get(each).sources()) {
                final String text = this.workspace.textOf(join(this.loader.projectDir(each), source));
                if (text != null) {
                    out.add(new IProgrammingLanguage.SourceText(each + "/" + source, text));
                }
            }
        }
        return out;
    }

    @Override
    public boolean mouseScrolled(final double delta) {
        if (this.palette.isOpen()) {
            return this.palette.mouseScrolled(0, 0, delta);
        }
        final CodeWorkspace.Doc doc = this.workspace.current();
        if (doc == null || this.page != Page.SOLUTION) {
            return this.explorer.mouseScrolled(this.explorer.x(), this.explorer.y(), delta);
        }
        return doc.area().mouseScrolled(doc.area().x(), doc.area().y(), delta);
    }

    @Override
    public void renderTooltip(final GuiGraphics g, final Font font, final int x, final int y,
                              final int width, final int height, final int mouseX, final int mouseY) {
        if (this.page != Page.SOLUTION) {
            return;
        }
        // A row of the Error List shows the whole of what was said, which the column cannot always fit.
        if (this.errors.visible() && this.errors.contains(mouseX, mouseY)) {
            final int index = this.errors.rowAt(mouseX, mouseY);
            final List<ProblemReport.Row> rows = errorRows();
            if (index >= 0 && index < rows.size()) {
                final ProblemReport.Row row = rows.get(index);
                g.renderTooltip(font, Component.literal(GameText.resolve(VirtualStudioTexts.ERROR_TOOLTIP.with(
                        row.complaint().code(), row.complaint().message(), row.name(), row.complaint().line()))),
                        mouseX, mouseY);
                return;
            }
        }
        final CodeWorkspace.Doc doc = this.workspace.current();
        if (doc == null) {
            return;
        }
        final String message = doc.area().messageAt(mouseX, mouseY);
        if (!message.isEmpty()) {
            g.renderTooltip(font, Component.literal(message), mouseX, mouseY);
        }
    }

    /** The studio's colours, as the palette above names them. */
    private record Colours(int platformOn, int platformOff, int complaint, int succeeded, int failed) {
    }
}
