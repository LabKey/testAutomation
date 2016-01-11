package org.labkey.test.pages.study;

import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.pages.LabKeyPage;

/**
 * Created by susanh on 9/26/15.
 */
public class ManageParticipantGroupsPage extends LabKeyPage
{
    public ManageParticipantGroupsPage(BaseWebDriverTest test)
    {
        super(test);
    }

    public boolean isEditEnabled()
    {
        return _test.isElementPresent(Locator.linkContainingText("Edit Selected").enabled());
    }

    public boolean isDeleteEnabled()
    {
        return _test.isElementPresent(Locator.linkContainingText("Delete Selected").enabled());
    }

    public void selectGroup(String name)
    {
        Locator groupByName = Locator.xpath("//table[@role=\"presentation\"]/tbody/tr/td/div[contains(normalize-space(), '" + name + "')]");
        groupByName.findElement(getDriver()).click();
    }

    public void deleteGroup(String name)
    {
        selectGroup(name);
        Locator.linkContainingText("Delete Selected").findElement(getDriver()).click();
        Locator.linkContainingText("Yes").findElement(getDriver()).click();
    }
}
