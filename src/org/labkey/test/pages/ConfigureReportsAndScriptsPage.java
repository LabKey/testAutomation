/*
 * Copyright (c) 2014-2017 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.labkey.test.pages;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;
import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.util.Ext4Helper;
import org.labkey.test.util.LogMethod;
import org.labkey.test.util.LoggedParam;
import org.labkey.test.util.TestLogger;
import org.openqa.selenium.WebElement;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.labkey.test.components.ext4.Window.Window;

/**
 * org.labkey.query.reports.ReportsController.ConfigureReportsAndScriptsAction
 */
public class ConfigureReportsAndScriptsPage extends LabKeyPage
{
    private static final String DEFAULT_ENGINE = "Mozilla Rhino";
    private static final String EDIT_WINDOW_TITLE = "Edit Engine Configuration";

    public ConfigureReportsAndScriptsPage(WebDriverWrapper test)
    {
        super(test);
        waitForEnginesGrid();
    }

    public void waitForEnginesGrid()
    {
        waitForElement(Locators.enginesGridRowForName(DEFAULT_ENGINE));
    }

    public boolean isEnginePresentForLanguage(String language)
    {
        return isElementPresent(Locators.enginesGridRowForLanguage(language));
    }

    public boolean isEnginePresent(String engineName)
    {
        return isElementPresent(Locators.enginesGridRowForName(engineName));
    }

    @LogMethod
    public void addEngineWithDefaults(@LoggedParam EngineType type)
    {
        addEngine(type, new EngineConfig(null));
    }

    @LogMethod
    public void addEngine(@LoggedParam EngineType type, EngineConfig config)
    {
        Map<Locator, String> configMap = config.getConfigMap();

        String menuText = "New " + type + " Engine";
        _ext4Helper.clickExt4MenuButton(false, Locator.id("btn_addEngine"), false, menuText);
        WebElement menuItem = Locator.menuItem(menuText).findElementOrNull(getDriver());
        if (menuItem != null)
        {
            mouseOver(menuItem);
            menuItem.click(); // Retry for unresponsive button
        }
        Window(getDriver()).withTitle(EDIT_WINDOW_TITLE).waitFor();

        for (Map.Entry<Locator, String> entry : configMap.entrySet())
        {
            setFormElement(entry.getKey(), entry.getValue());
        }

        String language = getFormElement(Locator.id("editEngine_languageName-inputEl"));

        clickButton("Submit", 0);
        waitForElementToDisappear(ConfigureReportsAndScriptsPage.Locators.editEngineWindow);
        waitForElement(Locators.enginesGridRowForLanguage(language));
    }

    public void deleteEngine(String engineName)
    {
        WebElement engineRow = selectEngineNamed(engineName);

        deleteSelectedEngine(engineRow);
    }

    @LogMethod
    public void deleteEnginesFromList(@LoggedParam List<String> engineNames)
    {
        String defaultName = null;
        for (String engineName : engineNames)
        {
            if (isEnginePresent(engineName)) {
                WebElement engineRow = selectEngineNamed(engineName);
                if (engineRow.getText().contains("default : true"))
                {
                    defaultName = engineName;
                }
                else
                {
                    deleteSelectedEngine(engineRow);
                }
            }
        }

        // delete default engine last
        if (defaultName != null)
        {
            WebElement defaultRow = selectEngineNamed(defaultName);
            defaultRow.click();
            deleteSelectedEngine(defaultRow);
        }
    }

    @LogMethod
    public void deleteAllREngines(boolean sandboxed)
    {
        WebElement engineRow = selectFirstREngine(sandboxed);
        WebElement defaultR = null;

        while (engineRow != null)
        {
            if (engineRow.getText().contains("default : true"))
            {
                defaultR = engineRow;
                List<WebElement> elements = getREngineLoc(sandboxed).findElements(getDriver());
                if (elements.size() < 2)
                {
                    engineRow = null;
                }
                else
                {
                    engineRow = elements.get(1);
                }
            }
            else
            {
                deleteSelectedEngine(engineRow);
                engineRow = selectFirstREngine(sandboxed);
            }
        }

        // delete default engine last
        if (defaultR != null)
            deleteSelectedEngine(defaultR);
    }

    public void deleteAllNonSandboxedREngines()
    {
        deleteAllREngines(false);
    }

    public void deleteAllSandboxedREngines()
    {
        deleteAllREngines(true);
    }

    @LogMethod
    public void deleteAllREngines()
    {
        deleteAllNonSandboxedREngines();
        deleteAllSandboxedREngines();
    }

    @LogMethod(quiet = true)
    public void editEngine(@LoggedParam String engineName)
    {
        selectEngineNamed(engineName);

        clickButton("Edit", 0);

        Window(getDriver()).withTitle(EDIT_WINDOW_TITLE).waitFor();
    }

    private void deleteSelectedEngine(WebElement selectedEngineRow)
    {
        selectedEngineRow.click();
        clickButton("Delete", 0);

        Window(getDriver()).withTitle("Delete Engine Configuration").waitFor();

        String confirmationMessage = Locator.byClass("x4-window-body").findElement(getDriver()).getText();

        String engineName = confirmationMessage.substring(confirmationMessage.indexOf(":") + 1).replace("?", "");
        TestLogger.log("Deleting: " + engineName);

        clickButton("Yes", 0);
        waitForElementToDisappear(Locators.enginesGridRowForName(engineName));
        waitForEnginesGrid();
        sleep(2000); //wait for store and view update
    }

    private WebElement selectEngineNamed(String engineName)
    {
        WebElement engineRow = Locators.enginesGridRowForName(engineName).findElement(getDriver());
        engineRow.click();
        return engineRow;
    }

    @Nullable
    private WebElement selectFirstREngine(boolean sandboxed)
    {
        Locator engineLoc = getREngineLoc(sandboxed);
        WebElement engineRow = engineLoc.findElementOrNull(getDriver());
        if (engineRow != null)
            engineRow.click();
        return engineRow;
    }

    public Locator getREngineLoc(boolean sandboxed)
    {
        return sandboxed ? Locators.enginesGridRowForLanguageSandboxed("R") : Locators.enginesGridRowForLanguageNotSandboxed("R");
    }

    public enum EngineType
    {
        PERL,
        R,
        EXTERNAL,
        R_DOCKER
                {
                    @Override
                    public String toString()
                    {
                        return "R Docker";
                    }
                };

        public String toString()
        {
            return StringUtils.capitalize(name().toLowerCase());
        }
    }

    public static class EngineConfig
    {
        private String _name;
        private String _language;
        private String _version;
        private String _extensions;
        private File _path;
        private String _command;
        private String _outputFileName;

        private Map<Locator, String> configMap;

        public EngineConfig(File path)
        {
            _path = path;
        }

        public Map<Locator, String> getConfigMap()
        {
            configMap = new HashMap<>();
            addToConfigMap(Locator.id("editEngine_name-inputEl"), getName());
            addToConfigMap(Locator.id("editEngine_languageName-inputEl"), getLanguageName());
            addToConfigMap(Locator.id("editEngine_languageVersion-inputEl"), getLanguageVersion());
            addToConfigMap(Locator.id("editEngine_extensions-inputEl"), getExtensions());
            addToConfigMap(Locator.id("editEngine_exePath-inputEl"), getPath() != null ? getPath().getAbsolutePath() : null);
            addToConfigMap(Locator.id("editEngine_exeCommand-inputEl"), getCommand());
            addToConfigMap(Locator.id("editEngine_outputFileName-inputEl"), getOutputFileName());

            return configMap;
        }

        private void addToConfigMap(Locator field, String value)
        {
            if (value != null)
                configMap.put(field, value);
        }

        public String getName()
        {
            return _name;
        }

        public EngineConfig setName(String name)
        {
            _name = name;
            return this;
        }

        public String getLanguageName()
        {
            return _language;
        }

        public EngineConfig setLanguage(String language)
        {
            _language = language;
            return this;
        }

        public String getLanguageVersion()
        {
            return _version;
        }

        public EngineConfig setVersion(String version)
        {
            _version = version;
            return this;
        }

        public String getExtensions()
        {
            return _extensions;
        }

        public EngineConfig setExtensions(String extensions)
        {
            _extensions = extensions;
            return this;
        }

        public File getPath()
        {
            return _path;
        }

        public EngineConfig setPath(File path)
        {
            _path = path;
            return this;
        }

        public String getCommand()
        {
            return _command;
        }

        public EngineConfig setCommand(String command)
        {
            _command = command;
            return this;
        }

        public String getOutputFileName()
        {
            return _outputFileName;
        }

        public EngineConfig setOutputFileName(String outputFileName)
        {
            _outputFileName = outputFileName;
            return this;
        }
    }

    public static class Locators
    {
        private static final int nameColumnIndex = 0;
        private static final int languageColumnIndex = 1;

        public static Locator.XPathLocator enginesGrid = Locator.id("enginesGrid");

        public static Locator enginesGridRowForName(String engineName)
        {
            return enginesGrid.append(Locator.tagWithClass("tr", "x4-grid-row").withDescendant(Locator.tagWithText("b", engineName)));
        }

        public static Locator.XPathLocator enginesGridRowForLanguage(String engineLanguage)
        {
            return enginesGrid.append(Locator.tagWithClass("tr", "x4-grid-row").withPredicate(Locator.xpath(String.format("//td[%d]", languageColumnIndex + 1)).withText(engineLanguage)));
        }

        public static Locator enginesGridRowForLanguageSandboxed(String engineLanguage)
        {
            return enginesGridRowForLanguage(engineLanguage).withPredicate(Locator.xpath(String.format("//td[%d]", nameColumnIndex + 1)).containing("sandboxed : true"));
        }

        public static Locator enginesGridRowForLanguageNotSandboxed(String engineLanguage)
        {
            return enginesGridRowForLanguage(engineLanguage).withoutPredicate(Locator.xpath(String.format("//td[%d]", nameColumnIndex + 1)).containing("sandboxed : true"));
        }

        public static Locator editEngineWindow = Ext4Helper.Locators.window("Edit Engine Configuration");
    }
}
