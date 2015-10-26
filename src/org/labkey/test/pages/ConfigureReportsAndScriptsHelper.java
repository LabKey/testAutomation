/*
 * Copyright (c) 2014 LabKey Corporation
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
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.util.ExtHelper;
import org.labkey.test.util.LogMethod;
import org.labkey.test.util.LoggedParam;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * org.labkey.query.reports.ReportsController.ConfigureReportsAndScriptsAction
 */
public class ConfigureReportsAndScriptsHelper
{
    private static final String DEFAULT_ENGINE = "Mozilla Rhino";

    BaseWebDriverTest _test;

    public ConfigureReportsAndScriptsHelper(BaseWebDriverTest test)
    {
        _test = test;
        waitForEnginesGrid();
    }

    public void waitForEnginesGrid()
    {
        _test.waitForElement(Locators.enginesGridRowForName(DEFAULT_ENGINE));
    }

    public boolean isEnginePresent(String language)
    {
        return _test.isElementPresent(Locators.enginesGridRowForLanguage(language));
    }

    @LogMethod
    public void addEngine(@LoggedParam EngineType type, EngineConfig config)
    {
        _test._extHelper.clickExtMenuButton(false, Locator.id("btn_addEngine"), "New " + type + " Engine");
        WebElement window = _test.waitForElement(Locators.editEngineWindow);

        Map<Locator, String> configMap = config.getConfigMap();

        for (Map.Entry<Locator, String> entry : configMap.entrySet())
        {
            _test.setFormElement(entry.getKey(), entry.getValue());
        }

        String language = _test.getFormElement(Locator.id("editEngine_languageName"));

        _test.clickButton("Submit", 0);
        _test.shortWait().until(ExpectedConditions.stalenessOf(window));
        _test.waitForElement(Locators.enginesGridRowForLanguage(language));
    }

    @LogMethod(quiet = true)
    public void deleteEngine(@LoggedParam String engineName)
    {
        Locator engine = Locators.enginesGridRowForName(engineName);
        _test.click(engine);

        _test.clickButton("Delete", 0);

        _test._extHelper.waitForExtDialog("Delete Engine Configuration", BaseWebDriverTest.WAIT_FOR_JAVASCRIPT);

        _test.clickButton("Yes", 0);
        _test.waitForElementToDisappear(engine, BaseWebDriverTest.WAIT_FOR_JAVASCRIPT);
    }

    @LogMethod(quiet = true)
    public void editEngine(@LoggedParam String engineName)
    {
        Locator engine = Locators.enginesGridRowForName(engineName);
        _test.click(engine);

        _test.clickButton("Edit", 0);

        _test.waitForElement(Locators.editEngineWindow);
    }

    public static enum EngineType
    {
        PERL,
        R,
        EXTERNAL;

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
            addToConfigMap(Locator.id("editEngine_name"), getName());
            addToConfigMap(Locator.id("editEngine_languageName"), getLanguageName());
            addToConfigMap(Locator.id("editEngine_languageVersion"), getLanguageVersion());
            addToConfigMap(Locator.id("editEngine_extensions"), getExtensions());
            addToConfigMap(Locator.id("editEngine_exePath"), getPath().getAbsolutePath());
            addToConfigMap(Locator.id("editEngine_exeCommand"), getCommand());
            addToConfigMap(Locator.id("editEngine_outputFileName"), getOutputFileName());

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

        public void setName(String name)
        {
            _name = name;
        }

        public String getLanguageName()
        {
            return _language;
        }

        public void setLanguage(String language)
        {
            _language = language;
        }

        public String getLanguageVersion()
        {
            return _version;
        }

        public void setVersion(String version)
        {
            _version = version;
        }

        public String getExtensions()
        {
            return _extensions;
        }

        public void setExtensions(String extensions)
        {
            _extensions = extensions;
        }

        public File getPath()
        {
            return _path;
        }

        public void setPath(File path)
        {
            _path = path;
        }

        public String getCommand()
        {
            return _command;
        }

        public void setCommand(String command)
        {
            _command = command;
        }

        public String getOutputFileName()
        {
            return _outputFileName;
        }

        public void setOutputFileName(String outputFileName)
        {
            _outputFileName = outputFileName;
        }
    }

    public static class Locators
    {
        private static final int nameColumnIndex = 0;
        private static final int languageColumnIndex = 1;

        public static Locator.XPathLocator enginesGrid = Locator.id("enginesGrid");

        private static Locator enginesGridRowForName(String engineName)
        {
            return enginesGrid.append(Locator.tagWithClass("div", "x-grid3-row").withPredicate(Locator.xpath(String.format("//td[%d]", nameColumnIndex + 1)).containing(engineName + "enabled")));
        }

        public static Locator enginesGridRowForLanguage(String engineLanguage)
        {
            return enginesGrid.append(Locator.tagWithClass("div", "x-grid3-row").withPredicate(Locator.xpath(String.format("//td[%d]", languageColumnIndex + 1)).withText(engineLanguage)));
        }

        public static Locator editEngineWindow = ExtHelper.Locators.window("Edit Engine Configuration");
    }
}
