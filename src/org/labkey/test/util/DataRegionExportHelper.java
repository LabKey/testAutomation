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

    public DataRegionExportHelper(BaseWebDriverTest test, DataRegionTable drt)
    {
        _test = test;
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
        _test._extHelper.clickSideTab("Excel");
        if (exportSelected != null) chooseExportSelectedRows(exportSelected);
        _test.checkRadioButton(type.getRadioLocator());
        return _test.clickAndWaitForDownload(Locator.navButton("Export to Excel"), _expectedFileCount)[0];
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
        _test._extHelper.clickSideTab("Text");
        if (exportSelected != null) chooseExportSelectedRows(exportSelected);
        _test.selectOptionByValue(Locator.name("delim"), delim.toString());
        _test.selectOptionByValue(Locator.name("quote"), quote.toString());
        return _test.clickAndWaitForDownload(Locator.navButton("Export to Text"), _expectedFileCount)[0];
    }

    public String exportScript(ScriptExportType type)
    {
        expandExportPanel();
        _test._extHelper.clickSideTab("Script");
        _test.checkRadioButton(type.getRadioLocator());
        _test.click(Locator.navButton("Create Script"));

        Object[] windows = _test.getDriver().getWindowHandles().toArray();
        _test.getDriver().switchTo().window((String)windows[1]);

        String scriptText = _test.getDriver().getPageSource();

        _test.getDriver().close();
        _test.getDriver().switchTo().window((String)windows[0]);

        return scriptText;
    }

    public void expandExportPanel()
    {
        ExpectedCondition expandedPanel = LabKeyExpectedConditions.dataRegionPanelIsExpanded(Locator.id(_drt.getTableName()));
        if (expandedPanel.apply(_test.getDriver()) == null)
        {
            _test.clickButton("Export", 0);
            _test.shortWait().until(expandedPanel);
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
        R(0),
        PERL(1),
        JAVASCRIPT(2),
        SAS(3),
        URL(4);

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
