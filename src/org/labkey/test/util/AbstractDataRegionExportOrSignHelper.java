/*
 * Copyright (c) 2017 LabKey Corporation
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
import org.labkey.test.components.html.Checkbox;
import org.labkey.test.components.html.EnumSelect;
import org.labkey.test.components.html.RadioButton;
import org.labkey.test.selenium.LazyWebElement;
import org.labkey.test.selenium.RefindingWebElement;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import static org.labkey.test.components.html.EnumSelect.EnumSelect;

public abstract class AbstractDataRegionExportOrSignHelper extends WebDriverComponent<AbstractDataRegionExportOrSignHelper.Elements>
{
    private final WebElement _panelEl;
    private final DataRegionTable _drt;
    private int _expectedFileCount;

    public AbstractDataRegionExportOrSignHelper(DataRegionTable drt)
    {
        _panelEl = new RefindingWebElement(getPanelLocator(), drt.getComponentElement())
                .withRefindListener(el -> clearElementCache());
        _drt = drt;
        _expectedFileCount = 1;
    }

    protected Locator getPanelLocator()
    {
        return Locator.tag("div").withAttributeContaining("name", "-panel").withAttributeContaining("id", "PanelButtonContent");
    }

    protected String getExcelActionButtonText()
    {
        return "Export";
    }

    protected String getTextActionButtonText()
    {
        return "Export";
    }

    @Override
    public WebElement getComponentElement()
    {
        return _panelEl;
    }

    @Override
    protected WebDriver getDriver()
    {
        return _drt.getDriver();
    }

    protected int getExpectedFileCount()
    {
        return _expectedFileCount;
    }

    public AbstractDataRegionExportOrSignHelper setExpectedFileCount(int count)
    {
        _expectedFileCount = count;
        return this;
    }

    protected boolean isPanelExpanded()
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

    protected DataRegionTable getDataRegionTable()
    {
        return _drt;
    }

    protected void startExcelExport(ColumnHeaderType exportHeaderType, DataRegionExportHelper.ExcelFileType type, @Nullable Boolean exportSelected)
    {
        expandPanel();
        elementCache().excelTab.click();
        if (exportSelected != null)
            elementCache().exportSelectedCheckbox().set(exportSelected);
        elementCache().findExcelFileTypeRadio(type).check();
        elementCache().excelFileTypeSelect.set(exportHeaderType);
        elementCache().findExportButton(getExcelActionButtonText()).click();
    }

    protected void startTextExport(ColumnHeaderType exportHeaderType, TextSeparator delim, TextQuote quote, @Nullable Boolean exportSelected)
    {
        expandPanel();
        elementCache().textTab.click();
        if (exportSelected != null)
            elementCache().exportSelectedCheckbox().set(exportSelected);
        elementCache().delimiterSelect.set(delim);
        elementCache().quoteSelect.set(quote);
        elementCache().columnHeaderSelect.set(exportHeaderType);
        elementCache().findExportButton(getTextActionButtonText()).click();
    }

    public AbstractDataRegionExportOrSignHelper expandPanel()
    {
        if (!isPanelExpanded())
        {
            getWrapper().doAndWaitForPageSignal(() -> getDataRegionTable().clickExportButton(), DataRegionTable.PANEL_SHOW_SIGNAL);
        }
        return this;
    }

    public enum ExcelFileType
    {
        XLSX(0),
        XLS(1),
        IQY(2);

        private int _radioIndex;

        ExcelFileType(int radioIndex)
        {
            _radioIndex = radioIndex;
        }

        public Locator getRadioLocator()
        {
            return Locator.radioButtonByName("excelExportType").index(_radioIndex);
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

    protected Elements newElementCache()
    {
        return new Elements();
    }

    protected class Elements extends Component.ElementCache
    {
        protected final WebElement navTabs = new LazyWebElement(Locator.css("ul.nav-tabs"), this);
        protected final WebElement excelTab = new LazyWebElement(Locator.linkWithText("Excel"), navTabs);
        protected RadioButton findExcelFileTypeRadio(ExcelFileType type)
        {
            return new RadioButton(type.getRadioLocator().findElement(this));
        }
        protected EnumSelect<ColumnHeaderType> excelFileTypeSelect = EnumSelect(Locator.name("xls_header_type"), ColumnHeaderType.class).findWhenNeeded(this);

        protected final WebElement textTab = new LazyWebElement(Locator.linkWithText("Text"), navTabs);
        protected EnumSelect<TextSeparator> delimiterSelect = EnumSelect(Locator.name("delim"), TextSeparator.class).findWhenNeeded(this);
        protected EnumSelect<TextQuote> quoteSelect = EnumSelect(Locator.name("quote"), TextQuote.class).findWhenNeeded(this);
        protected EnumSelect<ColumnHeaderType> columnHeaderSelect = EnumSelect(Locator.name("txt_header_type"), ColumnHeaderType.class).findWhenNeeded(this);

        protected final WebElement scriptTab = new LazyWebElement(Locator.linkWithText("Script"), navTabs);

        private WebElement findActiveTab()
        {
            return Locator.css("div.tab-pane.active").findElement(this);
        }

        Checkbox exportSelectedCheckbox()
        {
            return new Checkbox(Locator.css("input[value=exportSelected]").findElement(findActiveTab()));
        }

        protected WebElement findExportButton(String buttonText)
        {
            return Locator.lkButton(buttonText).findElement(findActiveTab());
        }
    }
}
