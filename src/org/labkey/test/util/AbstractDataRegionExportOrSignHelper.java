package org.labkey.test.util;

import org.jetbrains.annotations.Nullable;
import org.labkey.api.data.ColumnHeaderType;
import org.labkey.test.Locator;
import org.labkey.test.components.Component;
import org.labkey.test.components.WebDriverComponent;
import org.labkey.test.selenium.RefindingWebElement;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

/**
 * Created by davebradlee on 7/24/17.
 */
public abstract class AbstractDataRegionExportOrSignHelper<EC extends AbstractDataRegionExportOrSignHelper.Elements> extends WebDriverComponent<EC>
{
    protected final WebElement _panelEl;
    protected final DataRegionTable _drt;
    protected int _expectedFileCount;

    public AbstractDataRegionExportOrSignHelper(DataRegionTable drt, Locator locator)
    {
        _panelEl = new RefindingWebElement(locator, drt.getComponentElement())
                .withRefindListener(el -> clearElementCache());
        _drt = drt;
        _expectedFileCount = 1;
    }

    protected abstract String getActionButtonText();
    protected String getMainButtonText()
    {
        return "Export";
    }
    protected String getXlsHeaderTypeName()
    {
        return "xls_header_type";
    }
    protected String getTextHeaderTypeName()
    {
        return "txt_header_type";
    }
    protected String getXlsFileTypeRadioName()
    {
        return "excelExportType";
    }
    protected String getDelimName()
    {
        return "delim";
    }
    protected String getQuoteName()
    {
        return "quote";
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

    public void setExpectedFileCount(int count)
    {
        _expectedFileCount = count;
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

    public void exportOrSignExcel(ColumnHeaderType exportHeaderType, DataRegionExportHelper.ExcelFileType type, @Nullable Boolean exportSelected)
    {
        expandExportOrSignPanel();
        elementCache().excelTab.click();
        if (exportSelected != null) chooseExportSelectedRows(exportSelected);
        getWrapper().checkRadioButton(type.getRadioLocator(getXlsFileTypeRadioName()));
        getWrapper().selectOptionByValue(Locator.name(getXlsHeaderTypeName()), exportHeaderType.name());
        getWrapper().scrollIntoView(Locator.lkButton(getActionButtonText()));
    }

    public void exportOrSignText(ColumnHeaderType exportHeaderType, TextSeparator delim, TextQuote quote, @Nullable Boolean exportSelected)
    {
        expandExportOrSignPanel();
        elementCache().textTab.click();
        if (exportSelected != null) chooseExportSelectedRows(exportSelected);
        getWrapper().selectOptionByValue(Locator.name(getDelimName()), delim.toString());
        getWrapper().selectOptionByValue(Locator.name(getQuoteName()), quote.toString());
        getWrapper().selectOptionByValue(Locator.name(getTextHeaderTypeName()), exportHeaderType.name());
    }

    public AbstractDataRegionExportOrSignHelper expandExportOrSignPanel()
    {
        if (!isPanelExpanded())
        {
            getWrapper().doAndWaitForPageSignal(() -> _drt.clickHeaderButtonByText(getMainButtonText()), DataRegionTable.PANEL_SHOW_SIGNAL);
        }
        return this;
    }

    protected void chooseExportSelectedRows(boolean exportSelected)
    {
        if (exportSelected)
            getWrapper().checkCheckbox(elementCache().exportSelectedCheckbox);
        else
            getWrapper().uncheckCheckbox(elementCache().exportSelectedCheckbox);
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

        public Locator getRadioLocator(String fileTypeRadio)
        {
            return Locator.radioButtonByName(fileTypeRadio).index(_radioIndex);
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

    protected EC elementCache()
    {
        return super.elementCache();
    }

    protected class Elements extends Component.ElementCache
    {
        public WebElement navTabs;
        public WebElement excelTab;
        public WebElement textTab;
        public WebElement scriptTab;
        public WebElement exportSelectedCheckbox;
    }
}
