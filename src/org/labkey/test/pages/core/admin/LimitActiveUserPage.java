package org.labkey.test.pages.core.admin;

import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.WebTestHelper;
import org.labkey.test.components.html.Input;
import org.labkey.test.pages.LabKeyPage;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.Select;

public class LimitActiveUserPage extends LabKeyPage<LimitActiveUserPage.ElementCache>
{
    public LimitActiveUserPage(WebDriver driver)
    {
        super(driver);
        waitForPage();
    }

    public static LimitActiveUserPage beginAt(WebDriverWrapper wrapper)
    {
        wrapper.beginAt(WebTestHelper.buildURL("user", "limitActiveUsers"));
        return new LimitActiveUserPage(wrapper.getDriver());
    }

    public LimitActiveUserPage userWarning(String value)
    {
        elementCache().userWarning.selectByVisibleText(value);
        return this;
    }

    public LimitActiveUserPage limitActiveUsers(String value)
    {
        elementCache().limitActiveUsers.selectByVisibleText(value);
        return this;
    }

    public String getUserWarningLevel()
    {
        return elementCache().userWarningLevel.get();
    }

    public LimitActiveUserPage setUserWarningLevel(String value)
    {
        elementCache().userWarningLevel.set(value);
        return this;
    }

    public String getUserLimitLevel()
    {
        return elementCache().userLimitLevel.get();
    }

    public LimitActiveUserPage setUserLimitLevel(String value)
    {
        elementCache().userLimitLevel.set(value);
        return this;
    }

    public String getUserWarningMessage()
    {
        return elementCache().userWarningMessage.getText();
    }

    public LimitActiveUserPage setUserWarningMessage(String value)
    {
        elementCache().userWarningMessage.clear();
        elementCache().userWarningMessage.sendKeys(value);
        return this;
    }

    public String getUserLimitMessage()
    {
        return elementCache().userLimitMessage.getText();
    }

    public LimitActiveUserPage setUserLimitMessage(String value)
    {
        elementCache().userLimitMessage.clear();
        elementCache().userLimitMessage.sendKeys(value);
        return this;
    }

    public String getErrorMessage()
    {
        return elementCache().errorMsg.getText();
    }
    public LimitActiveUserPage saveExpectingErrors()
    {
        elementCache().saveBtn.click();
        return this;
    }

    public ShowAdminPage save()
    {
        clickAndWait(elementCache().saveBtn);
        return new ShowAdminPage(getDriver());
    }

    public ShowAdminPage cancel()
    {
        clickAndWait(elementCache().cancelBtn);
        return new ShowAdminPage(getDriver());
    }

    @Override
    protected LimitActiveUserPage.ElementCache newElementCache()
    {
        return new LimitActiveUserPage.ElementCache();
    }

    protected class ElementCache extends LabKeyPage.ElementCache
    {
        protected final Select userWarning = new Select(Locator.id("userWarning").findWhenNeeded(this));
        protected final Input userWarningLevel = Input.Input(Locator.id("userWarningLevel"), getDriver()).findWhenNeeded(this);
        protected final WebElement userWarningMessage = Locator.id("userWarningMessage").findWhenNeeded(this);

        protected final Select limitActiveUsers = new Select(Locator.id("limitActiveUsers").findWhenNeeded(this));
        protected final Input userLimitLevel = Input.Input(Locator.id("userLimitLevel"), getDriver()).findWhenNeeded(this);
        protected final WebElement userLimitMessage = Locator.id("userLimitMessage").findWhenNeeded(this);

        protected final WebElement saveBtn = Locator.lkButton("Save").findWhenNeeded(this);
        protected final WebElement cancelBtn = Locator.lkButton("Cancel").findWhenNeeded(this);

        protected final WebElement errorMsg = Locator.tagWithClass("div","labkey-error").findWhenNeeded(this);
    }
}
