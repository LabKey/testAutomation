package org.labkey.test.components.issues;

import org.labkey.test.BaseWebDriverTest;
import org.openqa.selenium.WebDriver;

public class FilePicker extends org.labkey.test.components.core.FilePicker
{
    public FilePicker(WebDriver driver)
    {
        super(driver);
    }

    @Override
    protected String getTableId()
    {
        if (BaseWebDriverTest.IS_BOOTSTRAP_LAYOUT)
            return "filePickerTable";
        else
            return super.getTableId();
    }

    @Override
    protected String getLinkId()
    {
        if (BaseWebDriverTest.IS_BOOTSTRAP_LAYOUT)
            return "filePickerLink";
        else
            return super.getLinkId();
    }
}
