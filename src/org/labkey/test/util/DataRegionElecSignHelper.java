package org.labkey.test.util;

import org.jetbrains.annotations.Nullable;
import org.labkey.api.data.ColumnHeaderType;
import org.labkey.test.Locator;
import org.labkey.test.selenium.EphemeralWebElement;
import org.labkey.test.selenium.LazyWebElement;

/**
 * Created by davebradlee on 7/25/17.
 */
public class DataRegionElecSignHelper extends AbstractDataRegionExportOrSignHelper
{
    public DataRegionElecSignHelper(DataRegionTable drt)
    {
        super(drt, Locator.name("Electronically Sign-panel"));
    }

    public void signExcel(ColumnHeaderType headerType, ExcelFileType type, @Nullable Boolean selected, String reason)
    {
        exportOrSignExcel(headerType, type, selected);
        getWrapper().click(Locator.lkButton(getExcelActionButtonText()));
        Locator submit = Locator.linkWithText("Submit");
        getWrapper().waitForElement(submit);
        getWrapper().setFormElement(Locator.input("reason"), reason);
        getWrapper().click(submit);
    }

    public void signText(ColumnHeaderType headerType, TextSeparator delim, TextQuote quote, @Nullable Boolean exportSelected, String reason)
    {
        exportOrSignText(headerType, delim, quote, exportSelected);
        getWrapper().click(Locator.lkButton(getTextActionButtonText()));
        Locator submit = Locator.linkWithText("Submit");
        getWrapper().waitForElement(submit);
        getWrapper().setFormElement(Locator.input("reason"), reason);
        getWrapper().click(submit);
    }

    @Override
    protected Elements newElementCache()
    {
        Elements elements = new Elements();
        elements.navTabs = new LazyWebElement(Locator.css("ul.nav-tabs"), this);
        elements.excelTab = new LazyWebElement(Locator.linkWithText("Excel"), elements.navTabs);
        elements.textTab = new LazyWebElement(Locator.linkWithText("Text"), elements.navTabs);
        elements.exportSelectedCheckbox = new EphemeralWebElement(Locator.css("div.tab-pane.active input[value=exportSelected]"), this);
        return elements;
    }

    protected String getMainButtonText()
    {
        return "Electronically Sign";
    }
    protected String getExcelActionButtonText()
    {
        return "Sign Excel Snapshot";
    }
    protected String getTextActionButtonText()
    {
        return "Sign Text Snapshot";
    }
    protected String getXlsHeaderTypeName()
    {
        return "xls_header_type_sign";
    }
    protected String getTextHeaderTypeName()
    {
        return "txt_header_type_sign";
    }
    protected String getXlsFileTypeRadioName()
    {
        return "excelSignType";
    }
    protected String getDelimName()
    {
        return "delim_sign";
    }
    protected String getQuoteName()
    {
        return "quote_sign";
    }
}
