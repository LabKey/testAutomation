package org.labkey.test.components.study;

import org.labkey.test.Locator;
import org.labkey.test.pages.LabKeyPage;
import org.openqa.selenium.WebDriver;

public class ViewPreferencesPage extends LabKeyPage
{
    public ViewPreferencesPage(WebDriver driver)
    {
        super(driver);
    }

    public void selectDefaultGrid(String gridName)
    {
        clickAndWait(Locator.xpath("//TR[@data-viewLabel='GRID1']//a[normalize-space()='select']"));
        clickButton("Done");
    }
}
