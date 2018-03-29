package org.labkey.test.pages.study;

import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.WebTestHelper;
import org.labkey.test.components.html.SelectWrapper;
import org.labkey.test.pages.LabKeyPage;
import org.labkey.test.util.StudyHelper;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.Select;

public class StudySecurityPage extends LabKeyPage<StudySecurityPage.ElementCache>
{
    public StudySecurityPage(WebDriver driver)
    {
        super(driver);
    }

    public static StudySecurityPage beginAt(WebDriverWrapper driver)
    {
        return beginAt(driver, driver.getCurrentContainerPath());
    }

    public static StudySecurityPage beginAt(WebDriverWrapper driver, String containerPath)
    {
        driver.beginAt(WebTestHelper.buildURL("study-security", containerPath, "begin"));
        return new StudySecurityPage(driver.getDriver());
    }

    public StudySecurityPage setSecurityType(StudyHelper.SecurityMode securityType)
    {
        elementCache().securityType.selectByValue(securityType.toString());
        clickAndWait(elementCache().updateTypeButton);
        clearCache();
        return this;
    }

    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    protected class ElementCache extends LabKeyPage.ElementCache
    {
        Select securityType = SelectWrapper.Select(Locator.name("securityString")).findWhenNeeded(this);
        WebElement updateTypeButton = Locator.lkButton("Update Type").findWhenNeeded(this);
    }
}