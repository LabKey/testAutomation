package org.labkey.test.components;

import org.labkey.test.Locator;
import org.labkey.test.pages.LabKeyPage;
import org.openqa.selenium.WebDriver;

public class SetDefaultPageClass extends LabKeyPage
{
    public SetDefaultPageClass(WebDriver driver)
    {
        super(driver);
    }

    public void selectDefaultGrid(String gridName)
    {
        click(Locator.linkWithText("select").withAttributeContaining("onclick",gridName));
        clickAndWait(Locator.tagWithText("span","Done"));
    }
}
