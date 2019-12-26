package org.labkey.test.pages.security;

import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.WebTestHelper;
import org.labkey.test.components.html.Checkbox;
import org.labkey.test.components.html.Input;
import org.labkey.test.pages.LabKeyPage;
import org.labkey.test.pages.user.ShowUsersPage;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.List;

public class AddUsersPage extends LabKeyPage<AddUsersPage.ElementCache>
{
    public AddUsersPage(WebDriver driver)
    {
        super(driver);
    }

    public static AddUsersPage beginAt(WebDriverWrapper webDriverWrapper)
    {
        webDriverWrapper.beginAt(WebTestHelper.buildURL("security", "addUsers"));
        return new AddUsersPage(webDriverWrapper.getDriver());
    }

    public AddUsersPage setNewUsers(List<String> newUsers)
    {
        elementCache().newUsersInput.set(String.join("\n", newUsers));
        return this;
    }

    public AddUsersPage setClonedUser(String clonedUser)
    {
        if (clonedUser != null)
        {
            elementCache().clonePermissionCheckbox.set(true);
            elementCache().cloneUserInput.set(clonedUser);
        }
        return this;
    }

    public AddUsersPage setSendNotification(boolean sendNotification)
    {
        elementCache().sendNotificationCheckbox.set(sendNotification);
        return this;
    }

    public AddUsersPage clickAddUsers()
    {
        clickAndWait(elementCache().addUsersButton);
        return new AddUsersPage(getDriver());
    }

    public ShowUsersPage clickDone()
    {
        clickAndWait(elementCache().doneButton);
        return new ShowUsersPage(getDriver());
    }

    @Override
    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    protected class ElementCache extends LabKeyPage.ElementCache
    {
        final Input newUsersInput = Input.Input(Locator.id("newUsers"), getDriver()).findWhenNeeded(this);
        final Checkbox clonePermissionCheckbox = Checkbox.Checkbox(Locator.id("cloneUserCheck")).findWhenNeeded(this);
        final Input cloneUserInput = Input.Input(Locator.id("cloneUser"), getDriver()).findWhenNeeded(this);
        final Checkbox sendNotificationCheckbox = Checkbox.Checkbox(Locator.id("sendMail")).findWhenNeeded(this);
        final WebElement addUsersButton = Locator.lkButton("Add Users").findWhenNeeded(this);
        final WebElement doneButton = Locator.lkButton("Done").findWhenNeeded(this);
    }
}
