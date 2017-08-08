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
package org.labkey.test.util;

import org.jetbrains.annotations.Nullable;
import org.labkey.api.data.ColumnHeaderType;
import org.labkey.test.Locator;
import org.labkey.test.selenium.EphemeralWebElement;
import org.labkey.test.selenium.LazyWebElement;

import java.io.File;
import java.util.function.Consumer;

public class DataRegionExportHelper extends AbstractDataRegionExportOrSignHelper
{
    public DataRegionExportHelper(DataRegionTable drt)
    {
        super(drt, Locator.name("Export-panel"));
    }

    public File exportExcel(ExcelFileType type)
    {
        return exportExcel(ColumnHeaderType.Caption, type, null);
    }

    public File exportExcel(ColumnHeaderType exportHeaderType, ExcelFileType type, @Nullable Boolean exportSelected)
    {
        exportOrSignExcel(exportHeaderType, type, exportSelected);
        return getWrapper().clickAndWaitForDownload(Locator.lkButton(getActionButtonText()).index(0), _expectedFileCount)[0];
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
        exportOrSignText(exportHeaderType, delim, quote, exportSelected);
        return getWrapper().clickAndWaitForDownload(Locator.lkButton(getActionButtonText()).index(1), _expectedFileCount)[0];
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

    public AbstractDataRegionExportOrSignHelper expandExportPanel()
    {
        return expandExportOrSignPanel();
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
        Elements elements = new Elements();
        elements.navTabs = new LazyWebElement(Locator.css("ul.nav-tabs"), this);
        elements.excelTab = new LazyWebElement(Locator.linkWithText("Excel"), elements.navTabs);
        elements.textTab = new LazyWebElement(Locator.linkWithText("Text"), elements.navTabs);
        elements.scriptTab = new LazyWebElement(Locator.linkWithText("Script"), elements.navTabs);
        elements.exportSelectedCheckbox = new EphemeralWebElement(Locator.css("div.tab-pane.active input[value=exportSelected]"), this);
        return elements;
    }

    @Override
    protected String getActionButtonText()
    {
        return "Export";
    }
}
