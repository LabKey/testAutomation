package org.labkey.test.pages.issues;

import org.labkey.test.WebDriverWrapper;
import org.labkey.test.WebTestHelper;
import org.labkey.test.pages.LabKeyPage;
import org.labkey.test.util.Maps;
import org.openqa.selenium.WebDriver;

public class DetailsListPage extends LabKeyPage
{
    public DetailsListPage(WebDriver driver)
    {
        super(driver);
    }

    public static DetailsListPage beginAt(WebDriverWrapper driver, String issueDefName)
    {
        return beginAt(driver, driver.getCurrentContainerPath(), issueDefName);
    }

    public static DetailsListPage beginAt(WebDriverWrapper driver, String containerPath, String issueDefName)
    {
        driver.beginAt(WebTestHelper.buildURL("issues", containerPath, "detailsList", Maps.of("issueDefName", issueDefName.toLowerCase())));
        return new DetailsListPage(driver.getDriver());
    }
}
