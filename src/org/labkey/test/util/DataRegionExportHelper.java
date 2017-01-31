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
import org.labkey.api.data.ColumnHeaderType;
import org.labkey.test.Locator;
import org.labkey.test.components.Component;
import org.labkey.test.components.WebDriverComponent;
import org.labkey.test.selenium.EphemeralWebElement;
import org.labkey.test.selenium.LazyWebElement;
import org.labkey.test.selenium.RefindingWebElement;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.io.File;
import java.util.function.Consumer;

public class DataRegionExportHelper extends WebDriverComponent<DataRegionExportHelper.Elements>
{
    private final WebElement panelEl;
    private final DataRegionTable _drt;
    private int _expectedFileCount;

    public DataRegionExportHelper(DataRegionTable drt)
    {
        panelEl = new RefindingWebElement(Locator.name("Export-panel"), drt.getComponentElement())
                .withRefindListener(el -> clearElementCache());
        _drt = drt;
        _expectedFileCount = 1;
    }

    @Override
    public WebElement getComponentElement()
    {
        return panelEl;
    }

    @Override
    protected WebDriver getDriver()
    {
        return _drt.getDriver();
    }

    public void setExpectedFileCount(int count)
    {
        _expectedFileCount = count;
    }

    public File exportExcel(ExcelFileType type)
    {
        return exportExcel(ColumnHeaderType.Caption, type, null);
    }

    public File exportExcel(ColumnHeaderType exportHeaderType, ExcelFileType type, @Nullable Boolean exportSelected)
    {
        expandExportPanel();
        elementCache().excelTab.click();
        if (exportSelected != null) chooseExportSelectedRows(exportSelected);
        getWrapper().checkRadioButton(type.getRadioLocator());
        getWrapper().selectOptionByValue(Locator.name("xls_header_type"), exportHeaderType.name());
        getWrapper().scrollIntoView(Locator.lkButton("Export to Excel"));
        return getWrapper().clickAndWaitForDownload(Locator.lkButton("Export to Excel"), _expectedFileCount)[0];
    }

    public File exportText()
    {
        return exportText(ColumnHeaderType.Caption, TextSeparator.TAB, TextQuote.DOUBLE, null);
    }

    public File exportText(TextSeparator delim)
    {
        return exportText(ColumnHeaderType.Caption, delim, TextQuote.DOUBLE, null);
    }

    public File exportText(ColumnHeaderType exportHeaderType, TextSeparator delim)
    {
        return exportText(exportHeaderType, delim, TextQuote.DOUBLE, null);
    }

    public File exportText(ColumnHeaderType exportHeaderType, TextSeparator delim, TextQuote quote, @Nullable Boolean exportSelected)
    {
        expandExportPanel();
        elementCache().textTab.click();
        if (exportSelected != null) chooseExportSelectedRows(exportSelected);
        getWrapper().selectOptionByValue(Locator.name("delim"), delim.toString());
        getWrapper().selectOptionByValue(Locator.name("quote"), quote.toString());
        getWrapper().selectOptionByValue(Locator.name("txt_header_type"), exportHeaderType.name());
        return getWrapper().clickAndWaitForDownload(Locator.lkButton("Export to Text"), _expectedFileCount)[0];
    }

    public String exportScript(ScriptExportType type)
    {
        return exportAndVerifyScript(type, a -> {});
    }

    public String exportAndVerifyScript(ScriptExportType type, Consumer<String> verification)
    {
        expandExportPanel();
        elementCache().scriptTab.click();
        getWrapper().checkRadioButton(type.getRadioLocator());
        getWrapper().click(Locator.lkButton("Create Script"));

        getWrapper().switchToWindow(1);
        String scriptText = getWrapper().getDriver().getPageSource();
        verification.accept(scriptText);

        getWrapper().getDriver().close();
        getWrapper().switchToMainWindow();

        return scriptText;
    }

    public DataRegionExportHelper expandExportPanel()
    {
        if (!isExportPanelExpanded())
        {
            getWrapper().doAndWaitForPageSignal(() ->
                    _drt.clickHeaderButtonByText("Export"),
                    DataRegionTable.PANEL_SHOW_SIGNAL);
        }
        return this;
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
            getWrapper().checkCheckbox(elementCache().exportSelectedCheckbox);
        else
            getWrapper().uncheckCheckbox(elementCache().exportSelectedCheckbox);
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

    @Override
    protected Elements newElementCache()
    {
        return new Elements();
    }

    protected class Elements extends Component.ElementCache
    {
        public WebElement navTabs = new LazyWebElement(Locator.css("ul.nav-tabs"), this);
        public WebElement excelTab = new LazyWebElement(Locator.linkWithText("Excel"), navTabs);
        public WebElement textTab = new LazyWebElement(Locator.linkWithText("Text"), navTabs);
        public WebElement scriptTab = new LazyWebElement(Locator.linkWithText("Script"), navTabs);
        public WebElement exportSelectedCheckbox = new EphemeralWebElement(Locator.css("div.tab-pane.active input[value=exportSelected]"), this);
    }
}
