package org.labkey.test.pages.user;

import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.WebTestHelper;
import org.labkey.test.pages.LabKeyPage;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.Map;

public class UserDetailsPage extends LabKeyPage<UserDetailsPage.ElementCache>
{
    public UserDetailsPage(WebDriver driver)
    {
        super(driver);
    }

    public static UserDetailsPage beginAt(WebDriverWrapper webDriverWrapper, Integer userId)
    {
        webDriverWrapper.beginAt(WebTestHelper.buildURL("user", "details", Map.of("userId", userId.toString())));
        return new UserDetailsPage(webDriverWrapper.getDriver());
    }

    public UpdateUserDetailsPage clickEdit()
    {
        clickAndWait(elementCache().editButton);
        return new UpdateUserDetailsPage(getDriver());
    }

    public ClonePermissionsPage clickClonePermission()
    {
        clickAndWait(elementCache().cloneButton);
        return new ClonePermissionsPage(getDriver());
    }

    @Override
    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    protected class ElementCache extends LabKeyPage<?>.ElementCache
    {
        WebElement editButton = Locator.lkButton("Edit").findWhenNeeded(this);
        WebElement cloneButton = Locator.lkButton("Clone Permissions").findWhenNeeded(this);
    }
}
