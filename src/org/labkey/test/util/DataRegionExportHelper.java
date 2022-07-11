/*
 * Copyright (c) 2014-2019 LabKey Corporation
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

import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.jetbrains.annotations.Nullable;
import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.pages.pipeline.PipelineStatusDetailsPage;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

import static org.junit.Assert.assertEquals;

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
            scriptText.append(getWrapper().getHtmlSource().trim());
            return scriptText.toString().split("\n").length > 1; // All exported scripts should be longer than one line
        }, "Exported script was empty", 10000);
        verification.accept(scriptText.toString());

        getWrapper().getDriver().close();
        getWrapper().switchToMainWindow();

        return scriptText.toString();
    }

    public Sheet exportXLSAndVerifyRowCountAndHeader(int numRows, Set<String> expectedHeaders)
    {
        File exportedFile = exportExcel(DataRegionExportHelper.ExcelFileType.XLS);
        try (Workbook workbook = ExcelHelper.create(exportedFile))
        {
            Sheet sheet = workbook.getSheetAt(0);

            assertEquals("Wrong number of rows exported to " + exportedFile.getName(), numRows, sheet.getLastRowNum());
            if (expectedHeaders != null)
            {
                Set<String> actualHeaders = new HashSet<>(ExcelHelper.getRowData(sheet, 0));
                assertEquals("Column headers not as expected", expectedHeaders, actualHeaders);
            }

            return sheet;
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }

    public PipelineStatusDetailsPage exportXarToPipeline(XarLsidOutputType lsidType, String xarName)
    {
        startExportXar(lsidType, XarExportType.PIPELINE_FILE, xarName);
        getWrapper().clickAndWait(elementCache().findExportButton());
        return new PipelineStatusDetailsPage(getWrapper());
    }

    public File exportXar(XarLsidOutputType lsidType, String xarName)
    {
        startExportXar(lsidType, XarExportType.BROWSER_DOWNLOAD, xarName);
        return getWrapper().clickAndWaitForDownload(elementCache().findExportButton());
    }

    private void startExportXar(XarLsidOutputType lsidType, XarExportType exportType, String xarName)
    {
        expandPanel();
        elementCache().xarTab.click();
        elementCache().xarLsidOutputTypeSelect.set(lsidType);
        elementCache().xarExportTypeSelect.set(exportType);
        elementCache().xarFileNameInput.set(xarName);
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
