package org.labkey.test.util;

import org.jetbrains.annotations.Nullable;
import org.labkey.api.data.ColumnHeaderType;
import org.labkey.test.Locator;

public class DataRegionElecSignHelper extends AbstractDataRegionExportOrSignHelper
{
    private String _userName, _userPassword;

    public DataRegionElecSignHelper(DataRegionTable drt)
    {
        super(drt);
        _userName = PasswordUtil.getUsername();
        _userPassword = PasswordUtil.getPassword();
    }

    public DataRegionElecSignHelper(String userName, String userPassword, DataRegionTable drt)
    {
        super(drt);
        _userName = userName;
        _userPassword = userPassword;
    }

    public void signExcel(ColumnHeaderType headerType, ExcelFileType type, @Nullable Boolean selected, String reason)
    {
        startExcelExport(headerType, type, selected);
        signDocument(reason);
    }

    public void signText(ColumnHeaderType headerType, TextSeparator delim, TextQuote quote, @Nullable Boolean exportSelected, String reason)
    {
        startTextExport(headerType, delim, quote, exportSelected);
        signDocument(reason);
    }

    public void signDocument(String reason){
        Locator submit = Locator.linkWithText("Submit");
        getWrapper().waitForElement(submit);
        getWrapper().setFormElement(Locator.input("reason"), reason);
        getWrapper().setFormElement(Locator.input("email"), _userName);
        getWrapper().setFormElement(Locator.input("password"), _userPassword);
        getWrapper().click(submit);
        getWrapper().waitForElementToDisappear(submit);
        getWrapper().waitForText("Signed Snapshot");

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
