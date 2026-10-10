/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.program;

import dev.jstech.computers.audio.SoundOutput;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ComputerSettingsTest {

    private ComputerSettings settings;

    @BeforeEach
    void setUp() {
        settings = new ComputerSettings();
    }

    @Test
    void tileKey_pinsMovesAndResizesAStartTile() {
        settings.startTiles().setEncoded(List.of("files:m"));
        assertTrue(SettingKey.apply(settings, "tile", "jsc:paint w 0"));
        assertEquals(List.of("paint:w", "files:m"), settings.startTiles().encoded());
        assertTrue(SettingKey.apply(settings, "TILE", "files s 0"));
        assertEquals(List.of("files:s", "paint:w"), settings.startTiles().encoded());
    }

    @Test
    void tileKey_refusesAValueItCannotRead() {
        assertFalse(SettingKey.apply(settings, "tile", "paint"));
        assertFalse(SettingKey.apply(settings, "tile", "paint q 0"));
        assertFalse(SettingKey.apply(settings, "tile", "paint m first"));
    }

    @Test
    void untileKey_takesATileOffStart() {
        settings.startTiles().setEncoded(List.of("files:m", "paint:s"));
        assertTrue(SettingKey.apply(settings, "untile", "files"));
        assertEquals(List.of("paint:s"), settings.startTiles().encoded());
        assertFalse(SettingKey.apply(settings, "untile", ""));
    }

    @Test
    void setGuiScale_holdsBetweenHalfAndWhole() {
        settings.setGuiScale(9);
        assertEquals(50, settings.guiScale(), "a percentage below half is held at half");
        settings.setGuiScale(120);
        assertEquals(100, settings.guiScale(), "nothing bigger than the designed size");
        settings.setGuiScale(75);
        assertEquals(75, settings.guiScale());
        settings.setGuiScale(0);
        assertEquals(0, settings.guiScale(), "zero stands for the default");
    }

    @Test
    void setDefaultApp_forgetsTheOldestChoicePastTheCap() {
        for (int i = 0; i <= ComputerSettings.MAX_DEFAULT_APPS; i++) {
            settings.setDefaultApp("x" + i, "editor");
        }
        assertEquals(ComputerSettings.MAX_DEFAULT_APPS, settings.defaultApps().size());
        assertEquals("", settings.defaultApp("x0"), "the first choice made room");
        assertEquals("editor", settings.defaultApp("x" + ComputerSettings.MAX_DEFAULT_APPS));
    }

    @Test
    void setBrightness_clampsBelowZeroToZero() {
        settings.setBrightness(-40);
        assertEquals(0, settings.brightness());
    }

    @Test
    void setVolume_holdsBetweenSilentAndFull() {
        settings.setVolume(-5);
        assertEquals(0, settings.volume());
        settings.setVolume(150);
        assertEquals(100, settings.volume());
    }

    @Test
    void soundLevel_followsTheVolumeAndIsNothingWhileMuted() {
        settings.setVolume(40);
        assertEquals(0.4F, settings.soundLevel(), 1.0E-6F);
        settings.setMuted(true);
        assertEquals(0.0F, settings.soundLevel(), "muted plays nothing");
        settings.setMuted(false);
        assertEquals(0.4F, settings.soundLevel(), 1.0E-6F, "and turning it back on keeps the volume");
    }

    @Test
    void applySetting_volumeTakesAPercentageWithOrWithoutItsSign() {
        assertTrue(settings.applySetting("volume", "60%"));
        assertEquals(60, settings.volume());
        assertTrue(settings.applySetting("VOLUME", "35"));
        assertEquals(35, settings.volume());
        assertFalse(settings.applySetting("volume", "loud"));
    }

    @Test
    void applySetting_muteReadsOnAndOff() {
        assertTrue(settings.applySetting("mute", "on"));
        assertTrue(settings.muted());
        assertTrue(settings.applySetting("mute", "off"));
        assertFalse(settings.muted());
        assertFalse(settings.applySetting("mute", "maybe"));
    }

    @Test
    void applySetting_outputTakesMonitorSpeakersOrBoth() {
        assertEquals(SoundOutput.BOTH, settings.soundOutput(), "a fresh machine plays out of both");
        assertTrue(settings.applySetting("output", "speakers"));
        assertEquals(SoundOutput.SPEAKERS, settings.soundOutput());
        assertFalse(settings.applySetting("output", "headphones"));
        assertEquals(SoundOutput.SPEAKERS, settings.soundOutput(), "a refused value changes nothing");
    }

    @Test
    void applySetting_clock12hSetsTrue() {
        assertTrue(settings.applySetting("clock", "12h"));
        assertTrue(settings.clock12h());
    }

    @Test
    void applySetting_clock24hSetsFalse() {
        settings.setClock12h(true);
        assertTrue(settings.applySetting("clock", "24h"));
        assertFalse(settings.clock12h());
    }

    @Test
    void applySetting_clockInvalidValueIsRejected() {
        assertFalse(settings.applySetting("clock", "noon"));
    }

    @Test
    void applySetting_themeSystemClearsToDefault() {
        settings.setThemePreset("ocean");
        assertTrue(settings.applySetting("theme", "system"));
        assertEquals("", settings.themePreset());
    }

    @Test
    void applySetting_accentParsesSixHexDigits() {
        assertTrue(settings.applySetting("accent", "3A6AE0"));
        assertEquals(0xFF3A6AE0, settings.accent());
    }

    @Test
    void applySetting_accentAcceptsHashPrefix() {
        assertTrue(settings.applySetting("accent", "#12A26F"));
        assertEquals(0xFF12A26F, settings.accent());
    }

    @Test
    void applySetting_accentRejectsBadHex() {
        assertFalse(settings.applySetting("accent", "ZZZ"));
    }

    @Test
    void applySetting_autoopenOffTurnsItOff() {
        assertTrue(settings.applySetting("autoopen", "off"));
        assertFalse(settings.removableAutoOpen());
    }

    @Test
    void applySetting_savedriveUppercasesTheLetter() {
        assertTrue(settings.applySetting("savedrive", "d"));
        assertEquals('D', settings.defaultSaveDrive());
    }

    @Test
    void applySetting_guiscaleClampsThroughApply() {
        assertTrue(settings.applySetting("guiscale", "10"));
        assertEquals(50, settings.guiScale());
    }

    @Test
    void applySetting_defaultAppPrefixStoresPerExtension() {
        assertTrue(settings.applySetting("defaultapp:txt", "jsc:editor"));
        assertEquals("jsc:editor", settings.defaultApp("TXT"));
    }

    @Test
    void applySetting_unknownKeyReturnsFalse() {
        assertFalse(settings.applySetting("frobnicate", "1"));
    }

    @Test
    void setDefaultApp_blankValueRemovesTheEntry() {
        settings.setDefaultApp("txt", "jsc:editor");
        settings.setDefaultApp("txt", "");
        assertEquals("", settings.defaultApp("txt"));
    }

    @Test
    void taskbarCentered_defaultsToTrue() {
        assertTrue(settings.taskbarCentered());
    }

    @Test
    void applySetting_taskbarLeftClearsCentered() {
        assertTrue(settings.applySetting("taskbar", "left"));
        assertFalse(settings.taskbarCentered());
    }

    @Test
    void applySetting_taskbarCenterSetsCentered() {
        settings.setTaskbarCentered(false);
        assertTrue(settings.applySetting("taskbar", "center"));
        assertTrue(settings.taskbarCentered());
    }

    @Test
    void applySetting_taskbarInvalidValueIsRejected() {
        assertFalse(settings.applySetting("taskbar", "diagonal"));
    }

    @Test
    void darkMode_defaultsToFalse() {
        assertFalse(settings.darkMode());
    }

    @Test
    void applySetting_darkmodeOnEnablesIt() {
        assertTrue(settings.applySetting("darkmode", "on"));
        assertTrue(settings.darkMode());
    }

    @Test
    void applySetting_darkmodeOffDisablesIt() {
        settings.setDarkMode(true);
        assertTrue(settings.applySetting("darkmode", "off"));
        assertFalse(settings.darkMode());
    }

    @Test
    void applySetting_darkmodeInvalidValueIsRejected() {
        assertFalse(settings.applySetting("darkmode", "sepia"));
    }

    @Test
    void pinned_startsWithTheFileExplorer() {
        assertEquals(List.of("files"), settings.pinned());
        assertTrue(settings.isPinned("jsc:files"), "a namespaced id names the same program");
    }

    @Test
    void pin_appendsOnceAndKeepsTheOrder() {
        assertTrue(settings.pin("editor"));
        assertFalse(settings.pin("Editor"), "pinning again changes nothing");
        assertTrue(settings.pin("jsc:command_prompt"));
        assertEquals(List.of("files", "editor", "command_prompt"), settings.pinned());
    }

    @Test
    void unpin_takesTheProgramOff() {
        assertTrue(settings.unpin("files"));
        assertFalse(settings.unpin("files"), "already gone");
        assertTrue(settings.pinned().isEmpty());
    }

    @Test
    void pin_stopsAtTheCap() {
        for (int i = 0; i < ComputerSettings.MAX_PINNED + 3; i++) {
            settings.pin("program" + i);
        }
        assertEquals(ComputerSettings.MAX_PINNED, settings.pinned().size());
    }

    @Test
    void setPinned_replacesTheListDroppingBlanksAndRepeats() {
        settings.setPinned(List.of("editor", "", "editor", "files"));
        assertEquals(List.of("editor", "files"), settings.pinned());
        settings.setPinned(List.of());
        assertTrue(settings.pinned().isEmpty(), "an empty list is a choice, not the default");
    }

    @Test
    void applySetting_pinAndUnpinRouteToTheList() {
        assertTrue(settings.applySetting("pin", "editor"));
        assertTrue(settings.applySetting("pin", "editor"), "asking for what is already so is fine");
        assertTrue(settings.isPinned("editor"));
        assertTrue(settings.applySetting("unpin", "editor"));
        assertFalse(settings.isPinned("editor"));
        assertFalse(settings.applySetting("pin", "  "), "nothing to pin");
    }

    @Test
    void favourite_starsOnceInOrderAndUnfavouriteTakesTheStarOff() {
        assertTrue(settings.favourite("item|minecraft:iron_ingot"));
        assertFalse(settings.favourite("item|minecraft:iron_ingot"), "starring again changes nothing");
        assertTrue(settings.favourite("fluid|minecraft:water"));
        assertEquals(List.of("item|minecraft:iron_ingot", "fluid|minecraft:water"), settings.favourites());
        assertTrue(settings.isFavourite("fluid|minecraft:water"));
        assertTrue(settings.unfavourite("item|minecraft:iron_ingot"));
        assertFalse(settings.unfavourite("item|minecraft:iron_ingot"), "already gone");
        assertEquals(List.of("fluid|minecraft:water"), settings.favourites());
    }

    @Test
    void favourite_stopsAtTheCap() {
        for (int i = 0; i < ComputerSettings.MAX_FAVOURITES + 5; i++) {
            settings.favourite("item|mod:thing" + i);
        }
        assertEquals(ComputerSettings.MAX_FAVOURITES, settings.favourites().size());
    }

    @Test
    void applySetting_favouriteUnfavouriteAndRecipeRouteToTheirStores() {
        assertTrue(settings.applySetting("favourite", "item|minecraft:coal"));
        assertTrue(settings.applySetting("favourite", "item|minecraft:coal"), "asking for what is already so is fine");
        assertTrue(settings.isFavourite("item|minecraft:coal"));
        assertTrue(settings.applySetting("unfavourite", "item|minecraft:coal"));
        assertFalse(settings.isFavourite("item|minecraft:coal"));
        assertFalse(settings.applySetting("favourite", " "), "nothing to star");

        assertTrue(settings.applySetting("recipe", "item|jsc:steel_ingot=1"));
        assertEquals(1, settings.recipeChoice("item|jsc:steel_ingot"));
        assertEquals(-1, settings.recipeChoice("item|jsc:iron_ingot"), "an item never chosen for has no choice");
        assertTrue(settings.applySetting("recipe", "item|jsc:steel_ingot=-1"));
        assertEquals(-1, settings.recipeChoice("item|jsc:steel_ingot"), "a negative index forgets the choice");
        assertFalse(settings.applySetting("recipe", "item|jsc:steel_ingot"), "no index given");
        assertFalse(settings.applySetting("recipe", "item|jsc:steel_ingot=two"), "not a number");
    }

    @Test
    void setRecipeChoice_forgetsTheOldestPastTheCap() {
        for (int i = 0; i < ComputerSettings.MAX_RECIPE_CHOICES + 1; i++) {
            settings.setRecipeChoice("item|mod:thing" + i, i);
        }
        assertEquals(ComputerSettings.MAX_RECIPE_CHOICES, settings.recipeChoices().size());
        assertEquals(-1, settings.recipeChoice("item|mod:thing0"), "the first choice made room");
        assertEquals(ComputerSettings.MAX_RECIPE_CHOICES, settings.recipeChoice("item|mod:thing" + ComputerSettings.MAX_RECIPE_CHOICES));
    }

    @Test
    void setVariable_keepsTheNameInUpperCaseAndForgetsItOnABlankValue() {
        settings.setVariable("base", "C:\\work");

        assertEquals("C:\\work", settings.variables().get("BASE"), "a name is a name whatever case it is written in");
        settings.setVariable("BASE", "");
        assertFalse(settings.variables().containsKey("BASE"), "and nothing after the equals sign forgets it");
    }

    @Test
    void setVariable_forgetsTheOldestPastTheCap() {
        for (int i = 0; i < ComputerSettings.MAX_VARIABLES + 1; i++) {
            settings.setVariable("NAME" + i, "value " + i);
        }

        assertEquals(ComputerSettings.MAX_VARIABLES, settings.variables().size());
        assertFalse(settings.variables().containsKey("NAME0"), "the first name made room");
        assertTrue(settings.variables().containsKey("NAME" + ComputerSettings.MAX_VARIABLES));
    }

    @Test
    void putVariables_replacesWhatWasThere() {
        settings.setVariable("GONE", "x");
        settings.putVariables(Map.of("KEPT", "y"));

        assertFalse(settings.variables().containsKey("GONE"));
        assertEquals("y", settings.variables().get("KEPT"));
    }

    @Test
    void setEffect_switchesAnEffectOffAndBackOn() {
        assertTrue(settings.effectsOff().isEmpty(), "every effect is on until it is switched off");
        settings.setEffect("Minimize", false);
        settings.setEffect("menus", false);
        assertEquals(Set.of("minimize", "menus"), settings.effectsOff());
        settings.setEffect("minimize", true);
        assertEquals(Set.of("menus"), settings.effectsOff());
    }

    @Test
    void setEffect_ignoresANameThatIsNoPlainWord() {
        settings.setEffect("two words", false);
        settings.setEffect("", false);
        settings.setEffect("x".repeat(40), false);
        assertTrue(settings.effectsOff().isEmpty());
    }

    @Test
    void setEffect_keepsNoMoreThanTheMost() {
        for (int i = 0; i < ComputerSettings.MAX_EFFECTS_OFF + 5; i++) {
            settings.setEffect("effect_" + i, false);
        }
        assertEquals(ComputerSettings.MAX_EFFECTS_OFF, settings.effectsOff().size());
    }

    @Test
    void setEffectSpeed_holdsBetweenAtOnceAndTheSlowest() {
        assertEquals(ComputerSettings.EFFECT_SPEED_NORMAL, settings.effectSpeed());
        settings.setEffectSpeed(-10);
        assertEquals(0, settings.effectSpeed());
        settings.setEffectSpeed(5000);
        assertEquals(ComputerSettings.EFFECT_SPEED_SLOWEST, settings.effectSpeed());
    }

    @Test
    void applySetting_takesAnEffectAndItsSpeed() {
        assertTrue(settings.applySetting("effect", "squash off"));
        assertTrue(settings.effectsOff().contains("squash"));
        assertTrue(settings.applySetting("effect", "squash on"));
        assertFalse(settings.effectsOff().contains("squash"));
        assertFalse(settings.applySetting("effect", "squash maybe"));
        assertFalse(settings.applySetting("effect", "squash"));
        assertTrue(settings.applySetting("effectspeed", "140%"));
        assertEquals(140, settings.effectSpeed());
    }

    @Test
    void share_aDifferentFolderWithTheSameNameIsRefused() {
        assertTrue(settings.share("C:\\pub", false));

        assertFalse(settings.share("D:\\work\\pub", true));
        assertEquals("C:\\pub", settings.shareNamed("pub").path());
        assertFalse(settings.shareNamed("pub").writable());
    }

    @Test
    void share_theSameFolderAgainChangesWriteAccess() {
        assertTrue(settings.share("C:\\pub", false));

        assertTrue(settings.share("c:\\PUB", true));
        assertTrue(settings.shareNamed("pub").writable());
    }

    @Test
    void summaryLines_reportEveryOwnedSetting() {
        settings.setClock12h(true);
        settings.setBrightness(80);
        settings.setTaskbarCentered(false);
        final String joined = String.join("\n", settings.summaryLines());
        assertTrue(joined.contains("clock"));
        assertTrue(joined.contains("12h"));
        assertTrue(joined.contains("brightness"));
        assertTrue(joined.contains("80"));
        assertTrue(joined.contains("accent"));
        assertTrue(joined.contains("taskbar"));
        assertTrue(joined.contains("left"));
        assertTrue(joined.contains("volume"));
        assertTrue(joined.contains("mute"));
        assertTrue(joined.contains("output"));
        assertTrue(joined.contains("effect"));
        assertTrue(joined.contains("effectspeed"));
    }
}
