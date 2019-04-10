/*
 * Copyright (c) 2014-2018 LabKey Corporation
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
import org.labkey.test.WebTestHelper;
import org.labkey.test.components.ext4.Window;
import org.labkey.test.util.Ext4Helper;
import org.labkey.test.util.LogMethod;
import org.labkey.test.util.LoggedParam;
import org.labkey.test.util.TestLogger;
import org.openqa.selenium.WebElement;

import java.io.File;
import java.util.ArrayList;
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

    public static ConfigureReportsAndScriptsPage beginAt(WebDriverWrapper driver)
    {
        driver.beginAt(WebTestHelper.buildURL("core", "configureReportsAndScripts"));
        return new ConfigureReportsAndScriptsPage(driver);
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

    public void setSiteDefault(String engineName)
    {
        log("Ensure " + engineName + " is the site default engine");
        editEngine(engineName);
        Locator.XPathLocator defaultCheckbox = Locator.id("editEngine_default-inputEl");
        if (_ext4Helper.isChecked(defaultCheckbox))
        {
            log(engineName + " is already the site default engine");
            click(Locator.linkWithText("Cancel"));
            _ext4Helper.waitForMaskToDisappear();
            return;
        }
        log("Change site default engine to " + engineName);
        _ext4Helper.checkCheckbox(defaultCheckbox);
        click(Locator.linkWithText("Submit"));
        acceptAlert();
        _ext4Helper.waitForMaskToDisappear();
    }

    @LogMethod
    public void addEngineWithDefaults(@LoggedParam EngineType type)
    {
        addEngine(type, new EngineConfig(null));
    }

    @LogMethod
    public void addEngine(@LoggedParam EngineType type, EngineConfig engineConfig)
    {
        String menuText = "New " + type + " Engine";
        _ext4Helper.clickExt4MenuButton(false, Locator.id("btn_addEngine"), false, menuText);
        WebElement menuItem = Locator.menuItem(menuText).findOptionalElement(getDriver()).orElse(null);
        if (menuItem != null)
        {
            mouseOver(menuItem);
            menuItem.click(); // Retry for unresponsive button
        }
        Window window = Window(getDriver()).withTitle(EDIT_WINDOW_TITLE).waitFor();
        engineConfig.configureEngine(type,window, this);

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

    public void deleteAllDockerEngines()
    {
        List<String> dockerEngineNames = new ArrayList<>();
        String defaultDockerR = null;

        List<WebElement> engines = getREngineLoc(true).findElements(getDriver());
        for (WebElement engine : engines)
        {
            engine.click();
            clickButton("Edit", 0);
            Window(getDriver()).withTitle(EDIT_WINDOW_TITLE).waitFor();
            if (isElementPresent(Locator.id("dockerimage-imageName-labelEl")))
            {
                String engineName = Locator.id("editEngine_name-inputEl").findElement(getDriver()).getAttribute("value");
                Locator.XPathLocator defaultCheckbox = Locator.id("editEngine_default-inputEl");
                if (_ext4Helper.isChecked(defaultCheckbox))
                {
                    defaultDockerR = engineName;
                }
                else
                {
                    dockerEngineNames.add(engineName);
                }
            }
            click(Locator.linkWithText("Cancel"));
        }

        deleteEnginesFromList(dockerEngineNames);
        if (defaultDockerR != null)
            deleteEngine(defaultDockerR);
    }

    @LogMethod
    public void deleteAllREngines()
    {
        WebElement engineRow = selectFirstREngine();
        WebElement defaultR = null;

        while (engineRow != null)
        {
            if (engineRow.getText().contains("default : true"))
            {
                defaultR = engineRow;
                List<WebElement> elements = Locators.enginesGridRowForLanguage("R").findElements(getDriver());
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
                engineRow = selectFirstREngine();
            }
        }

        // delete default engine last
        if (defaultR != null)
            deleteSelectedEngine(defaultR);
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
        sleep(5000); //wait for store and view update
    }

    private WebElement selectEngineNamed(String engineName)
    {
        Locator engineRowLoc = Locators.enginesGridRowForName(engineName);
        waitForElement(engineRowLoc);
        WebElement engineRow = engineRowLoc.findElement(getDriver());
        engineRow.click();
        return engineRow;
    }

    @Nullable
    private WebElement selectFirstREngine()
    {
        Locator engineLoc = Locators.enginesGridRowForLanguage("R");
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
        REMOTE_R
            {
                @Override
                public String toString()
                {
                    return "Remote R";
                }
            },
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

        protected Map<Locator, String> configMap;

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

        public void addToConfigMap(Locator field, String value)
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

        public void configureEngine(EngineType type, Window configWindow, WebDriverWrapper wrapper)
        {
            if(! type.equals(EngineType.REMOTE_R))
                configMap = getConfigMap();

            for (Map.Entry<Locator, String> entry : configMap.entrySet())
            {
                wrapper.setFormElement(entry.getKey(), entry.getValue());
            }
        }
    }

    public static class RServeEngineConfig extends EngineConfig
    {

        private String _userName;
        private String _password;
        private String _remoteReportsTemp;
        private String _remoteDate;
        private String _portNumber;
        private String _machine;

        public RServeEngineConfig(File path, String userName, String password, String remoteReportsTemp, String remoteDate)
        {
            super(path);
            _userName = userName;
            _password = password;
            _remoteReportsTemp = remoteReportsTemp;
            _remoteDate = remoteDate;
        }

        public String getUserName()
        {
            return _userName;
        }

        public EngineConfig setUserName(String userName)
        {
            _userName = userName;
            return this;
        }

        public String getPassword()
        {
            return _password;
        }

        public EngineConfig setPassword(String password)
        {
            _password = password;
            return this;
        }

        public String getRemoteReportsTemp()
        {
            return _remoteReportsTemp;
        }

        public EngineConfig setRemoteReportsTemp(String remoteReportsTemp)
        {
            _remoteReportsTemp = remoteReportsTemp;
            return this;
        }

        public String getRemoteDate()
        {
            return _remoteDate;
        }

        public EngineConfig setRemoteDate(String remoteData)
        {
            _remoteDate = remoteData;
            return this;
        }

        public String getPortNumber()
        {
            return _portNumber;
        }

        public EngineConfig setPortNumber(String portNumber)
        {
            _portNumber = portNumber;
            return this;
        }

        public String getMachine()
        {
            return _machine;
        }

        public EngineConfig setMachine(String machine)
        {
            _machine = machine;
            return this;
        }

        public Map<Locator, String> getConfigMapRemoteR()
        {
            configMap = new HashMap<>();
            addToConfigMap(Locator.id("editEngine_name-inputEl"), getName());
            addToConfigMap(Locator.id("editEngine_languageName-inputEl"), getLanguageName());
            addToConfigMap(Locator.id("editEngine_extensions-inputEl"), getExtensions());
            addToConfigMap(Locator.id("editEngine_user-inputEl"), getUserName());
            addToConfigMap(Locator.id("editEngine_password-inputEl"), getPassword());
            addToConfigMap(Locator.id("editEngine_port-inputEl"), getPortNumber());
            addToConfigMap(Locator.id("editEngine_machine-inputEl"), getMachine());
            addToConfigMap(Locator.id("editEngine_outputFileName-inputEl"), getOutputFileName());

            return configMap;
        }

        public void configureEngine(EngineType type, Window configWindow, WebDriverWrapper wrapper)
        {
            int i = 0;
            super.configureEngine(type, configWindow, wrapper);

            wrapper.log("Configuring the path mapping");
            wrapper.click(Locator.tagWithClassContaining("td", "remoteURI").index(i++));
            Locator.name("remoteURI").findElement(wrapper.getDriver()).sendKeys(_remoteReportsTemp);

            wrapper.click(Locator.tagWithClassContaining("td", "remoteURI").index(i++));
            Locator.name("remoteURI").findElement(wrapper.getDriver()).sendKeys(_remoteDate);

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
