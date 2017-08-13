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
import org.labkey.test.WebDriverWrapper;

import java.io.File;
import java.util.function.Consumer;

public class DataRegionExportHelper extends AbstractDataRegionExportOrSignHelper
{
    public DataRegionExportHelper(DataRegionTable drt)
    {
        super(drt);
    }

    public File exportExcel(ExcelFileType type)
    {
        return exportExcel(ColumnHeaderType.Caption, type, null);
    }

    public File exportExcel(ColumnHeaderType exportHeaderType, ExcelFileType type, @Nullable Boolean exportSelected)
    {
        return getWrapper().doAndWaitForDownload(() -> startExcelExport(exportHeaderType, type, exportSelected), getExpectedFileCount())[0];
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
        return getWrapper().doAndWaitForDownload(() -> startTextExport(exportHeaderType, delim, quote, exportSelected), getExpectedFileCount())[0];
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
        StringBuilder scriptText = new StringBuilder();
        WebDriverWrapper.waitFor(() -> {
            scriptText.append(getWrapper().getDriver().getPageSource().trim());
            return !scriptText.toString().isEmpty();
        }, "Exported script was empty", 10000);
        verification.accept(scriptText.toString());

        getWrapper().getDriver().close();
        getWrapper().switchToMainWindow();

        return scriptText.toString();
    }

    public AbstractDataRegionExportOrSignHelper expandExportPanel()
    {
        return expandPanel();
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
}
