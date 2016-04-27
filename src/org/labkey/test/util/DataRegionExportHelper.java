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
import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.components.Component;
import org.labkey.test.components.ComponentElements;
import org.labkey.test.selenium.EphemeralWebElement;
import org.labkey.test.selenium.LazyWebElement;
import org.labkey.test.selenium.RefindingWebElement;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.SearchContext;
import org.openqa.selenium.WebElement;

import java.io.File;

public class DataRegionExportHelper extends Component
{
    private WebDriverWrapper _driver;
    private DataRegionTable _drt;
    private int _expectedFileCount;
    private WebElement panelEl;
    private Elements _elements;

    public DataRegionExportHelper(DataRegionTable drt)
    {
        panelEl = new RefindingWebElement(DataRegionTable.isNewDataRegion ? Locator.name("Export-panel")
                : Locator.tagWithClass("div", "x-grouptabs-panel").withoutClass("customize-grid-panel"), drt.getComponentElement())
                .withRefindListener(el -> _elements = null);
        _driver = drt._driver;
        _drt = drt;
        _expectedFileCount = 1;
    }

    @Override
    public WebElement getComponentElement()
    {
        return panelEl;
    }

    protected Elements elements()
    {
        if (_elements == null)
            _elements = new Elements();
        return _elements;
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
        elements().excelTab.click();
        if (exportSelected != null) chooseExportSelectedRows(exportSelected);
        _driver.checkRadioButton(type.getRadioLocator());
        _driver.scrollIntoView(Locator.lkButton("Export to Excel"));
        return _driver.clickAndWaitForDownload(Locator.lkButton("Export to Excel"), _expectedFileCount)[0];
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
        elements().textTab.click();
        if (exportSelected != null) chooseExportSelectedRows(exportSelected);
        _driver.selectOptionByValue(Locator.name("delim"), delim.toString());
        _driver.selectOptionByValue(Locator.name("quote"), quote.toString());
        return _driver.clickAndWaitForDownload(Locator.lkButton("Export to Text"), _expectedFileCount)[0];
    }

    public String exportScript(ScriptExportType type)
    {
        expandExportPanel();
        elements().scriptTab.click();
        _driver.checkRadioButton(type.getRadioLocator());
        _driver.click(Locator.lkButton("Create Script"));

        _driver.switchToWindow(1);
        String scriptText = _driver.getDriver().getPageSource();

        _driver.getDriver().close();
        _driver.switchToMainWindow();

        return scriptText;
    }

    public void expandExportPanel()
    {
        if (!isExportPanelExpanded())
        {
            _driver.doAndWaitForPageSignal(() ->
                    _drt.clickHeaderButtonByText("Export"),
                    DataRegionTable.PANEL_SHOW_SIGNAL);
        }
    }

    private boolean isExportPanelExpanded()
    {
        try
        {
            return getComponentElement().isDisplayed();
        }
        catch (NoSuchElementException notCreated)
        {
            return false;
        }
    }

    private void chooseExportSelectedRows(boolean exportSelected)
    {
        if (exportSelected)
            _driver.checkCheckbox(elements().exportSelectedCheckbox);
        else
            _driver.uncheckCheckbox(elements().exportSelectedCheckbox);
    }

    protected WebDriverWrapper getTest()
    {
        return _driver;
    }

    protected DataRegionTable getDataRegionTable()
    {
        return _drt;
    }

    public enum ExcelFileType
    {
        XLSX(0),
        XLS(1),
        IQY(2);

        private Locator fileTypeRadio;

        ExcelFileType(int radioIndex)
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

    public enum TextSeparator
    {
        TAB("tsv"),
        COMMA("csv"),
        COLON("csv"),
        SEMICOLON("csv");

        private String fileExtension;

        TextSeparator(String ext)
        {
            fileExtension = ext;
        }

        public String getFileExtension()
        {
            return fileExtension;
        }
    }

    public enum TextQuote
    {
        DOUBLE,
        SINGLE
    }

    public enum ScriptExportType
    {
        JAVA(0),
        JAVASCRIPT(1),
        PERL(2),
        PYTHON(3),
        R(4),
        SAS(5),
        URL(6);

        private Locator fileTypeRadio;

        ScriptExportType(int radioIndex)
        {
            fileTypeRadio = Locator.radioButtonByName("scriptExportType").index(radioIndex);
        }

        public Locator getRadioLocator()
        {
            return fileTypeRadio;
        }
    }

    protected class Elements extends ComponentElements
    {
        @Override
        protected SearchContext getContext()
        {
            return getComponentElement();
        }

        public WebElement navTabs = new LazyWebElement(DataRegionTable.isNewDataRegion ? Locator.css("ul.nav-tabs")
                : Locator.css(".x-grouptabs-strip"), this);
        public WebElement excelTab = new LazyWebElement(DataRegionTable.isNewDataRegion ? Locator.linkWithText("Excel")
                : Locator.css(".x-grouptabs-main").withText("Excel"), navTabs);
        public WebElement textTab = new LazyWebElement(DataRegionTable.isNewDataRegion ? Locator.linkWithText("Text")
                : Locator.css(".x-grouptabs-main").withText("Text"), navTabs);
        public WebElement scriptTab = new LazyWebElement(DataRegionTable.isNewDataRegion ? Locator.linkWithText("Script")
                : Locator.css(".x-grouptabs-main").withText("Script"), navTabs);
        public WebElement exportSelectedCheckbox = new EphemeralWebElement(Locator.tagWithAttribute("input", "value", "exportSelected").notHidden(), this);
    }
}
