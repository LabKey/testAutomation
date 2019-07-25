package org.labkey.test.components.wiki;

import org.labkey.test.components.core.FilePicker;
import org.openqa.selenium.WebDriver;

public class WikiFilePicker extends FilePicker
{
    public WikiFilePicker(WebDriver driver)
    {
        super(driver);
    }

    @Override
    protected String getTableId()
    {
        return "wiki-new-attachments";
    }
}
