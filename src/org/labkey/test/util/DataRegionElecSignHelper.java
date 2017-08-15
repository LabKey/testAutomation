package org.labkey.test.util;

import org.jetbrains.annotations.Nullable;
import org.labkey.api.data.ColumnHeaderType;
import org.labkey.test.Locator;

public class DataRegionElecSignHelper extends AbstractDataRegionExportOrSignHelper
{
    public DataRegionElecSignHelper(DataRegionTable drt)
    {
        super(drt);
    }

    public void signExcel(ColumnHeaderType headerType, ExcelFileType type, @Nullable Boolean selected, String reason)
    {
        startExcelExport(headerType, type, selected);
        Locator submit = Locator.linkWithText("Submit");
        getWrapper().waitForElement(submit);
        getWrapper().setFormElement(Locator.input("reason"), reason);
        getWrapper().setFormElement(Locator.input("email"), PasswordUtil.getUsername());
        getWrapper().setFormElement(Locator.input("password"), PasswordUtil.getPassword());
        getWrapper().click(submit);
    }

    public void signText(ColumnHeaderType headerType, TextSeparator delim, TextQuote quote, @Nullable Boolean exportSelected, String reason)
    {
        startTextExport(headerType, delim, quote, exportSelected);
        Locator submit = Locator.linkWithText("Submit");
        getWrapper().waitForElement(submit);
        getWrapper().setFormElement(Locator.input("reason"), reason);
        getWrapper().setFormElement(Locator.input("email"), PasswordUtil.getUsername());
        getWrapper().setFormElement(Locator.input("password"), PasswordUtil.getPassword());
        getWrapper().click(submit);
    }

    @Override
    protected String getExcelActionButtonText()
    {
        return "Sign Data";
    }

    @Override
    protected String getTextActionButtonText()
    {
        return "Sign Data";
    }

    @Override
    protected String getShowPanelButtonText()
    {
        return "Export";
    }
}
