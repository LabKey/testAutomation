/*
 * Copyright (c) 2014-2016 LabKey Corporation
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
package org.labkey.test.util;

import org.jetbrains.annotations.Nullable;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.openqa.selenium.support.ui.ExpectedCondition;

import java.io.File;

public class DataRegionExportHelper
{
    private BaseWebDriverTest _test;
    private DataRegionTable _drt;
    private int _expectedFileCount;
    // TODO: DataRegion change.
//    private final boolean newRegion = true;
    private final boolean newRegion = false;

    public DataRegionExportHelper(DataRegionTable drt)
    {
        _test = drt._test;
        _drt = drt;
        _expectedFileCount = 1;
    }

    public void setExpectedFileCount(int count)
    {
        _expectedFileCount = count;
    }

    public File exportExcel(ExcelFileType type)
    {
        return exportExcel(type, null);
    }

    public File exportExcel(ExcelFileType type, @Nullable Boolean exportSelected)
    {
        expandExportPanel();
        clickTab("Excel");
        if (exportSelected != null) chooseExportSelectedRows(exportSelected);
        _test.checkRadioButton(type.getRadioLocator());
        _test.scrollIntoView(Locator.lkButton("Export to Excel"));
        return _test.clickAndWaitForDownload(Locator.lkButton("Export to Excel"), _expectedFileCount)[0];
    }

    public File exportText()
    {
        return exportText(TextSeparator.TAB, TextQuote.DOUBLE, null);
    }

    public File exportText(TextSeparator delim)
    {
        return exportText(delim, TextQuote.DOUBLE, null);
    }

    public File exportText(TextSeparator delim, TextQuote quote, @Nullable Boolean exportSelected)
    {
        expandExportPanel();
        clickTab("Text");
        if (exportSelected != null) chooseExportSelectedRows(exportSelected);
        _test.selectOptionByValue(Locator.name("delim"), delim.toString());
        _test.selectOptionByValue(Locator.name("quote"), quote.toString());
        return _test.clickAndWaitForDownload(Locator.lkButton("Export to Text"), _expectedFileCount)[0];
    }

    public String exportScript(ScriptExportType type)
    {
        expandExportPanel();
        clickTab("Script");
        _test.checkRadioButton(type.getRadioLocator());
        _test.click(Locator.lkButton("Create Script"));
        // it takes time to create the script before the new window is available
        // todo: figure a way to wait for new browser tab because this access of windows[1] happens too soon and throws npe without the sleep
        _test.sleep(1000);
        Object[] windows = _test.getDriver().getWindowHandles().toArray();
        _test.getDriver().switchTo().window((String)windows[1]);

        String scriptText = _test.getDriver().getPageSource();

        _test.getDriver().close();
        _test.getDriver().switchTo().window((String)windows[0]);

        return scriptText;
    }

    public void expandExportPanel()
    {
        ExpectedCondition expandedPanel = LabKeyExpectedConditions.dataRegionPanelIsExpanded(_drt);
        if (expandedPanel.apply(_test.getDriver()) == null || !_test.isElementPresent(Locator.tagWithClass("table", "labkey-export-tab-contents").notHidden()))
        {
            _test.clickButton("Export", 0);
            _test.shortWait().until(expandedPanel);
            if (newRegion)
                _test.sleep(1000); // wait for animation to complete
        }
    }

    private void chooseExportSelectedRows(boolean exportSelected)
    {
        Locator exportSelectedCheckbox = Locator.tagWithAttribute("input", "value", "exportSelected").notHidden();

        if (exportSelected)
            _test.checkCheckbox(exportSelectedCheckbox);
        else
            _test.uncheckCheckbox(exportSelectedCheckbox);
    }

    private void clickTab(String text)
    {
        if (newRegion)
            _test.click(Locator.xpath("//div[contains(@class, 'tabs-left')]").append(Locator.xpath("//a[@data-toggle='tab' and text()='" + text + "']")));
        else
            _test._extHelper.clickSideTab(text);
    }

    public static enum ExcelFileType
    {
        XLSX(0),
        XLS(1),
        IQY(2);

        private Locator fileTypeRadio;

        private ExcelFileType(int radioIndex)
        {
            fileTypeRadio = Locator.radioButtonByName("excelExportType").index(radioIndex);
        }

        public Locator getRadioLocator()
        {
            return fileTypeRadio;
        }

        public String getFileExtension()
        {
            return name().toLowerCase();
        }
    }

    public static enum TextSeparator
    {
        TAB("tsv"),
        COMMA("csv"),
        COLON("csv"),
        SEMICOLON("csv");

        private String fileExtension;

        private TextSeparator(String ext)
        {
            fileExtension = ext;
        }

        public String getFileExtension()
        {
            return fileExtension;
        }
    }

    public static enum TextQuote
    {
        DOUBLE,
        SINGLE
    }

    public static enum ScriptExportType
    {
        JAVA(0),
        JAVASCRIPT(1),
        PERL(2),
        PYTHON(3),
        R(4),
        SAS(5),
        URL(6);

        private Locator fileTypeRadio;

        private ScriptExportType(int radioIndex)
        {
            fileTypeRadio = Locator.radioButtonByName("scriptExportType").index(radioIndex);
        }

        public Locator getRadioLocator()
        {
            return fileTypeRadio;
        }
    }
}
