/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.chain;

import com.google.common.base.Suppliers;
import dev.jstech.computers.ComputingModule;
import dev.jstech.computers.JsComputers;
import dev.jstech.computers.block.PrinterBlock;
import dev.jstech.computers.chain.ChainChips;
import dev.jstech.computers.chain.machine.AirSeparationBlockEntity;
import dev.jstech.computers.chain.machine.ChainFaces;
import dev.jstech.computers.chain.machine.ChainMachine;
import dev.jstech.computers.chain.machine.ChainMachines;
import dev.jstech.computers.chain.machine.MaskWriting;
import dev.jstech.computers.chain.machine.MouldCutting;
import dev.jstech.computers.chain.machine.WaferPages;
import dev.jstech.computers.lithography.FilmPrinting;
import dev.jstech.computers.os.media.InstallerStacks;
import dev.jstech.computers.program.Programs;
import dev.jstech.core.client.recipeview.ProcessingRecipeViews;
import dev.jstech.core.client.recipeview.RecipeFaces;
import dev.jstech.core.gui.layout.GuiLayout;
import dev.jstech.core.jstechseries.tier.Era;
import dev.jstech.core.jstechseries.tier.HardwareEra;
import dev.jstech.core.machine.FacePicture;
import dev.jstech.core.machine.MachineScreenLayout;
import dev.jstech.core.machine.MachineScreenTexts;
import dev.jstech.core.machine.MachineTexts;
import dev.jstech.core.material.MaterialLiquids;
import dev.jstech.core.matter.MatterStack;
import dev.jstech.core.recipeview.RecipeView;
import dev.jstech.core.recipeview.RecipeViewKind;
import dev.jstech.core.recipeview.RecipeViews;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextKey;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.fluids.FluidStack;
import org.jetbrains.annotations.Nullable;

/**
 * The chain's pages no recipe holds, as J's Core's kinds of recipe view, so JEI, EMI and REI show them alike and an
 * agent reads them (computers/docs/design/survival.md, "How a chip is made" and "Masks"): a wafer's steps, each page
 * the step on its machine's own face at work, a kind a step; a mask a Lithography Station's computer writes; and the
 * gases the Air Separation Unit draws out of the air round it, on its face.
 */
public final class ChainRecipePages {

    private static final String AIR_SEPARATION_UNIT = "air_separation_unit";
    private static final String INJECTION_MOULDER = "injection_moulder";
    /* A Mask Writing page: its size, and where its things stand, in the GUI's pixels. */
    private static final int MASK_WIDTH = 232;
    private static final int MASK_HEIGHT = 130;
    private static final int IN_SLOT_X = 4;
    private static final int NEVER_X = 92;
    private static final int BLANK_X = 35;
    private static final int MASK_X = 119;
    private static final int SLOT_Y = 8;
    private static final int ARROW_X = 74;
    private static final int ARROW_Y = 8;
    private static final int ARROW_WIDTH = 24;
    private static final int NAME_X = 141;
    private static final int UNDER_Y = 30;
    private static final int RULE_Y = 41;
    private static final int ROW_X = 7;
    private static final int ROW_TEXT_X = 30;
    private static final int FIRST_ROW = 48;
    private static final int ROW_PITCH = 22;
    private static final int LINE = 9;
    private static final int FOOT_Y = 121;
    /* A slot is laid by its frame, a pixel round the item it shows. */
    private static final int INSET = 1;
    private static final int TICKS_A_SECOND = 20;
    /* Each step's pages, worked out the first time a viewer asks, with each page's number among its step's. */
    private static final Supplier<Map<WaferPages.Step, List<WaferPages.Page>>> WAFER =
            Suppliers.memoize(ChainRecipePages::waferPages);
    private static final Supplier<Map<WaferPages.Page, Integer>> NUMBERS =
            Suppliers.memoize(ChainRecipePages::numbers);
    /* The kinds, once declared. */
    private static final Map<WaferPages.Step, RecipeViewKind<WaferPages.Page>> STEPS =
            new EnumMap<>(WaferPages.Step.class);
    private static @Nullable RecipeViewKind<MaskPage> maskWriting;

    private ChainRecipePages() {
    }

    /** Declares the pages once the game's client is set up, before the recipe viewers ask for them. */
    public static void listen(final IEventBus modBus) {
        modBus.addListener(FMLClientSetupEvent.class, event -> event.enqueueWork(ChainRecipePages::declare));
    }

    /** The kind of a wafer's step {@code step}, once declared. */
    public static Optional<RecipeViewKind<WaferPages.Page>> step(final WaferPages.Step step) {
        return Optional.ofNullable(STEPS.get(step));
    }

    /** The kind of the masks a station's computer writes, once declared. */
    public static Optional<RecipeViewKind<MaskPage>> maskWriting() {
        return Optional.ofNullable(maskWriting);
    }

    /** One page a mask a computer writes, on every station that writes it. */
    public static List<MaskPage> maskPages() {
        final List<MaskPage> out = new ArrayList<>();
        for (final ChainMachine station : ChainMachines.all()) {
            for (final MaskWriting.Offer offer : MaskWriting.offers(station, HardwareEra.SERIES.getLast())) {
                out.add(new MaskPage(station, offer));
            }
        }
        return out;
    }

    private static void declare() {
        for (final WaferPages.Step step : WaferPages.Step.values()) {
            final String name = step.name().toLowerCase(Locale.ROOT);
            STEPS.put(step, RecipeViews.of(JsComputers.MODID, "wafer_" + name, title(step), WaferPages.Page.class)
                    .recipes(level -> WAFER.get().get(step))
                    .workstations(() -> machines(WAFER.get().get(step)))
                    .icon(() -> new ItemStack(terminal(iconMachine(step))))
                    .size(RecipeFaces.width(), RecipeFaces.height(ChainFaces.of(iconMachine(step)).orElseThrow()))
                    .layout(ChainRecipePages::wafer)
                    .ids(page -> ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, "wafer/" + name + "/"
                            + NUMBERS.get().get(page)))
                    .declare());
        }
        maskWriting = RecipeViews.of(JsComputers.MODID, "mask_writing", ChainPagesLook.MASK_WRITING, MaskPage.class)
                .recipes(level -> maskPages())
                .workstations(ChainRecipePages::maskStations)
                .icon(() -> new ItemStack(ChainChips.MASK_BLANK.get()))
                .size(MASK_WIDTH, MASK_HEIGHT)
                .layout(ChainRecipePages::mask)
                .ids(page -> ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, "mask_writing/"
                        + page.station().id() + "/" + BuiltInRegistries.ITEM.getKey(page.offer().mask()).getPath()))
                .declare();
        RecipeViews.of(JsComputers.MODID, "mould_cutting", ChainPagesLook.MOULD_CUTTING, MouldCutting.Offer.class)
                .recipes(level -> MouldCutting.offers())
                .workstations(() -> List.of(new ItemStack(terminal(INJECTION_MOULDER))))
                .icon(() -> new ItemStack(ChainChips.MOULD_BLANK.get()))
                .size(MASK_WIDTH, MASK_HEIGHT)
                .layout(ChainRecipePages::mould)
                .ids(offer -> ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, "mould_cutting/"
                        + BuiltInRegistries.ITEM.getKey(offer.mould()).getPath()))
                .declare();
        RecipeViews.of(JsComputers.MODID, "film_printing", ChainPagesLook.FILM_PRINTING, FilmPrinting.Offer.class)
                .recipes(level -> FilmPrinting.offers())
                .workstations(() -> List.of(new ItemStack(ComputingModule.VINTAGE_PRINTER.get()),
                        new ItemStack(ComputingModule.LEGACY_PRINTER.get()),
                        new ItemStack(ComputingModule.TRANSITION_PRINTER.get()),
                        new ItemStack(ComputingModule.PRINTER.get()),
                        new ItemStack(ComputingModule.ADVANCED_PRINTER.get())))
                .icon(() -> new ItemStack(ChainChips.BLANK_FILM.get()))
                .size(MASK_WIDTH, MASK_HEIGHT)
                .layout(ChainRecipePages::film)
                .ids(offer -> ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, "film_printing/"
                        + BuiltInRegistries.ITEM.getKey(offer.film()).getPath()))
                .declare();
        RecipeViews.of(JsComputers.MODID, "air_separation", ChainPagesLook.AIR_SEPARATION, ChainMachine.class)
                .recipes(level -> List.of(ChainMachines.byId(AIR_SEPARATION_UNIT).orElseThrow()))
                .workstations(() -> List.of(new ItemStack(terminal(AIR_SEPARATION_UNIT))))
                .size(RecipeFaces.width(), RecipeFaces.height())
                .layout(ChainRecipePages::airSeparation)
                .ids(machine -> ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, "air_separation"))
                .declare();
    }

    /*
     * A wafer's step on its machine's face: what it takes and gives in the face's ports, the liquids it takes in their
     * sight glasses, for a viewer to look them up, and its gases in theirs, filled with each gas's colour.
     */
    private static void wafer(final WaferPages.Page page, final RecipeView.Builder view) {
        final ChainMachine machine = page.machine();
        final FacePicture picture = ChainFaces.of(machine).orElseThrow();
        for (final FacePicture.Port port : picture.ports()) {
            final List<ItemStack> held = page.slots().get(port.slot());
            if (held == null || held.isEmpty()) {
                continue;
            }
            if (machine.layout().isOutput(port.slot())) {
                view.output(port.x(), port.y(), held);
            } else if (WaferPages.tool(held.getFirst())) {
                view.catalyst(port.x(), port.y(), held).tooltip(ProcessingRecipeViews.catalyst());
            } else {
                view.input(port.x(), port.y(), held);
            }
        }
        // A machine that sorts what it makes into bins shows them under its face, as its screen does.
        if (picture.tray().isPresent() && Minecraft.getInstance().level != null) {
            final List<ItemStack> bins = WaferPages.bins(page, Minecraft.getInstance().level.getRecipeManager());
            for (int i = 0; i < bins.size() && i < picture.tray().get().slots().size(); i++) {
                final GuiLayout.SlotPosition bin = MachineScreenLayout.pageTraySlot(i);
                view.output(bin.x(), bin.y(), bins.get(i));
            }
        }
        final int liquids = machine.layout().inputTanks() + machine.layout().outputTanks();
        final Map<Integer, MatterStack> matter = new HashMap<>();
        for (final FacePicture.Glass glass : picture.glasses()) {
            final WaferPages.Fill fill = page.tanks().get(glass.tank());
            if (fill == null) {
                continue;
            }
            if (glass.tank() < liquids && MaterialLiquids.of(fill.material()).isPresent()) {
                RecipeFaces.fluid(view, glass, List.of(new FluidStack(MaterialLiquids.fluid(fill.material()).get(),
                        fill.amount())), fill.amount(), glass.tank() < machine.layout().inputTanks());
            } else {
                matter.put(glass.tank(), new MatterStack(fill.material(), fill.amount()));
            }
        }
        view.face(face(machine, picture, matter));
    }

    /*
     * The Air Separation Unit at work on its face: nothing taken, as it draws the air round it, and each gas a step
     * gives in the glass of its tank, the tanks filling in the order the gases come.
     */
    private static void airSeparation(final ChainMachine machine, final RecipeView.Builder view) {
        final FacePicture picture = ChainFaces.of(machine).orElseThrow();
        final int first = machine.layout().inputTanks() + machine.layout().outputTanks();
        final Map<Integer, MatterStack> matter = new HashMap<>();
        final List<MatterStack> gases = AirSeparationBlockEntity.step();
        for (int i = 0; i < gases.size(); i++) {
            matter.put(first + i, gases.get(i));
        }
        view.face(face(machine, picture, matter));
    }

    /*
     * A mask a computer writes: the blank in the station's mask slot becoming the mask in one layer's time, never used
     * up; under the rule, the station, the computer it must be linked to and the program that writes it, each an icon
     * with its two lines; and how much faster a newer computer writes it.
     */
    private static void mask(final MaskPage page, final RecipeView.Builder view) {
        final int seconds = page.station().seconds();
        toolPage(view, new Tool(ChainChips.MASK_BLANK.get(), page.offer().mask(), seconds, ChainPagesLook.IN_THE_SLOT,
                ChainPagesLook.NEVER_USED_UP, new ItemStack(ChainMachines.terminalOf(page.station()).get()),
                ChainPagesLook.ITS_SLOT, page.offer().computer(), ChainPagesLook.LINKED,
                ChainPagesLook.PICK.with(page.offer().name())));
        page.offer().faster().ifPresent(newer -> view.line(ROW_X, FOOT_Y, MASK_WIDTH - ROW_X,
                GameText.component(ChainPagesLook.FASTER.with(ChainPagesLook.SECONDS.with(Math.max(1, seconds / 2)),
                        newer.name())), ChainPagesLook.PALETTE.get().dim()));
    }

    /*
     * A mould a computer cuts, laid out as a mask is: the Mould Blank in the Injection Moulder's mould slot becoming
     * the mould in one step's time, never used up; under the rule, the moulder, any computer linked to it and the
     * program that cuts it.
     */
    private static void mould(final MouldCutting.Offer offer, final RecipeView.Builder view) {
        final ChainMachine moulder = ChainMachines.byId(INJECTION_MOULDER).orElseThrow();
        toolPage(view, new Tool(ChainChips.MOULD_BLANK.get(), offer.mould(), moulder.seconds(),
                ChainPagesLook.IN_THE_MOULD_SLOT, ChainPagesLook.NEVER_USED_UP, new ItemStack(terminal(
                INJECTION_MOULDER)), ChainPagesLook.ITS_MOULD_SLOT, HardwareEra.VINTAGE, ChainPagesLook.LINKED,
                ChainPagesLook.PICK_CUT.with(offer.name())));
    }

    /*
     * A film a computer's printer prints, laid out as a mask is: the Blank Film in the printer's tray becoming the film
     * in a page's time of the Vintage's printer, a phototool never used up and a Layout used up writing its mask; under
     * the rule, the printer, any computer it is linked to and the program that prints it.
     */
    private static void film(final FilmPrinting.Offer offer, final RecipeView.Builder view) {
        final PrinterBlock printer = ComputingModule.VINTAGE_PRINTER.get();
        toolPage(view, new Tool(ChainChips.BLANK_FILM.get(), offer.film(), Math.max(1, printer.model().pageTicks()
                / TICKS_A_SECOND), ChainPagesLook.IN_THE_TRAY, offer.kind() == FilmPrinting.Kind.PHOTOTOOLS
                ? ChainPagesLook.NEVER_USED_UP : ChainPagesLook.USED_FOR_ITS_MASK, new ItemStack(printer),
                ChainPagesLook.ITS_TRAY, HardwareEra.VINTAGE, ChainPagesLook.LINKED_PRINTER,
                ChainPagesLook.PICK_PRINT.with(offer.name())));
    }

    /*
     * A tool a computer makes on a blank in a machine's slot: the blank becoming the tool in its seconds, what becomes
     * of it, the tool's name over what it is for, as its item names it; under the rule, the machine, the computer it
     * must be linked to and the program, each an icon with its two lines.
     */
    private static void toolPage(final RecipeView.Builder view, final Tool made) {
        final ChainPagesLook.Colours ink = ChainPagesLook.PALETTE.get();
        final Component time = GameText.component(ChainPagesLook.SECONDS.with(made.seconds()));
        final Item tool = made.tool();
        view.input(BLANK_X - INSET, SLOT_Y - INSET, List.of(new ItemStack(made.blank())))
                .output(MASK_X - INSET, SLOT_Y - INSET, new ItemStack(tool))
                .arrow(ARROW_X, ARROW_Y, 0)
                .line(ARROW_X + (ARROW_WIDTH - Minecraft.getInstance().font.width(time)) / 2, 0, ARROW_WIDTH * 2,
                        time, ink.text());
        final String name = new ItemStack(tool).getHoverName().getString();
        final int open = name.indexOf(" (");
        view.line(NAME_X, SLOT_Y + 1, MASK_WIDTH - NAME_X, Component.literal(open < 0 ? name
                : name.substring(0, open)), ink.text());
        if (open >= 0 && name.endsWith(")")) {
            view.line(NAME_X, SLOT_Y + 1 + LINE, MASK_WIDTH - NAME_X, Component.literal(name.substring(open + 2,
                    name.length() - 1)), ink.dim());
        }
        view.line(IN_SLOT_X, UNDER_Y, NEVER_X - IN_SLOT_X, GameText.component(made.inTheSlot()), ink.dim())
                .line(NEVER_X, UNDER_Y, MASK_WIDTH - NEVER_X, GameText.component(made.kept()), ink.dim())
                .rule(ROW_X - 1, RULE_Y, MASK_WIDTH - 2 * ROW_X + 1, 1, ink.rule());
        row(view, ink, 0, made.station(), GameText.of(made.station().getHoverName()), made.itsSlot().text());
        row(view, ink, 1, new ItemStack(computerOf(made.computer())), ChainPagesLook.COMPUTER.with(
                made.computer().name()), made.linked().text());
        row(view, ink, 2, InstallerStacks.forProgram(Programs.LITHOGRAPHY), ChainPagesLook.PROGRAM.text(),
                made.pick());
    }

    /* A row under the rule: what the writing needs, an icon not used up, and its two lines beside it. */
    private static void row(final RecipeView.Builder view, final ChainPagesLook.Colours ink, final int index,
                            final ItemStack needed, final Text first, final Text second) {
        final int y = FIRST_ROW + index * ROW_PITCH;
        view.catalyst(ROW_X - INSET, y - INSET, List.of(needed)).unframed()
                .line(ROW_TEXT_X, y, MASK_WIDTH - ROW_TEXT_X, GameText.component(first), ink.text())
                .line(ROW_TEXT_X, y + LINE, MASK_WIDTH - ROW_TEXT_X, GameText.component(second), ink.dim());
    }

    /*
     * The face {@code machine} is laid on at work: its words in the player's language, its LCD saying that it runs and
     * for how long a step takes, and {@code matter} in the glasses of its tanks.
     */
    private static RecipeView.Face face(final ChainMachine machine, final FacePicture picture,
                                        final Map<Integer, MatterStack> matter) {
        final Map<String, String> said = new HashMap<>();
        final String name = picture.plate().orElse(machine.english());
        said.put(name, ChainFaceScreens.translated(name));
        for (final FacePicture.Word word : picture.words()) {
            said.put(word.text(), ChainFaceScreens.translated(word.text()));
        }
        return new RecipeView.Face(ChainFaceScreens.frames(machine.id()), List.of(lcd(GameText.resolve(
                MachineScreenTexts.LCD_WORKING)), lcd(GameText.resolve(MachineTexts.RECIPE_TIME.with(
                machine.seconds())))), said, matter, ItemStack.EMPTY);
    }

    private static Component lcd(final String words) {
        return Component.literal(words.toUpperCase(Locale.ROOT));
    }

    private static Map<WaferPages.Step, List<WaferPages.Page>> waferPages() {
        final Map<WaferPages.Step, List<WaferPages.Page>> out = new EnumMap<>(WaferPages.Step.class);
        for (final WaferPages.Step each : WaferPages.Step.values()) {
            out.put(each, new ArrayList<>());
        }
        for (final WaferPages.Page page : WaferPages.all()) {
            out.get(page.step()).add(page);
        }
        return out;
    }

    /* Each page's place among its step's, for its id. */
    private static Map<WaferPages.Page, Integer> numbers() {
        final Map<WaferPages.Page, Integer> out = new IdentityHashMap<>();
        for (final List<WaferPages.Page> pages : WAFER.get().values()) {
            for (int i = 0; i < pages.size(); i++) {
                out.put(pages.get(i), i);
            }
        }
        return out;
    }

    /* The machines {@code pages} are worked on, by their terminals. */
    private static List<ItemStack> machines(final List<WaferPages.Page> pages) {
        final List<ItemStack> out = new ArrayList<>();
        for (final WaferPages.Page page : pages) {
            final ItemStack terminal = new ItemStack(ChainMachines.terminalOf(page.machine()).get());
            if (out.stream().noneMatch(each -> each.is(terminal.getItem()))) {
                out.add(terminal);
            }
        }
        return out;
    }

    /* The stations that write masks, by their terminals. */
    private static List<ItemStack> maskStations() {
        final List<ItemStack> out = new ArrayList<>();
        for (final ChainMachine station : ChainMachines.all()) {
            if (!MaskWriting.offers(station, HardwareEra.SERIES.getLast()).isEmpty()) {
                out.add(new ItemStack(ChainMachines.terminalOf(station).get()));
            }
        }
        return out;
    }

    private static ItemLike terminal(final String machine) {
        return ChainMachines.terminalOf(ChainMachines.byId(machine).orElseThrow()).get();
    }

    /* A Personal Computer of {@code era}, the one a Mask Writing page shows. */
    private static ItemLike computerOf(final Era era) {
        if (era == HardwareEra.VINTAGE) {
            return ComputingModule.VINTAGE_PERSONAL_COMPUTER.get();
        }
        if (era == HardwareEra.LEGACY) {
            return ComputingModule.LEGACY_PERSONAL_COMPUTER.get();
        }
        return era == HardwareEra.TRANSITION ? ComputingModule.TRANSITION_PERSONAL_COMPUTER.get()
                : ComputingModule.PERSONAL_COMPUTER.get();
    }

    private static TextKey title(final WaferPages.Step step) {
        return switch (step) {
            case LITHOGRAPHY -> ChainPagesLook.LITHOGRAPHY;
            case ETCH -> ChainPagesLook.ETCH;
            case IMPLANT -> ChainPagesLook.IMPLANT;
            case DEPOSITION -> ChainPagesLook.DEPOSITION;
            case PACKAGING -> ChainPagesLook.PACKAGING;
        };
    }

    /* The machine whose terminal is a step's icon. */
    private static String iconMachine(final WaferPages.Step step) {
        return switch (step) {
            case LITHOGRAPHY -> "lithography_legacy";
            case ETCH -> "plasma_etcher";
            case IMPLANT -> "ion_implanter";
            case DEPOSITION -> "deposition_chamber";
            case PACKAGING -> "packaging_line";
        };
    }

    /**
     * One mask a computer writes.
     *
     * @param station the station that writes it
     * @param offer   the mask, as the lithography program offers it
     */
    public record MaskPage(ChainMachine station, MaskWriting.Offer offer) {
    }

    /*
     * What a tool's page shows: the blank, the tool and how many seconds the one takes to become the other, where the
     * blank stands and what becomes of the tool, the machine that makes it and where in it, the oldest computer that
     * has it made, how that computer is linked to the machine, and what the program is asked.
     */
    private record Tool(Item blank, Item tool, int seconds, TextKey inTheSlot, TextKey kept, ItemStack station,
                        TextKey itsSlot, Era computer, TextKey linked, Text pick) {
    }
}
