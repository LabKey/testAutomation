package org.labkey.test.pages.core.admin;

import org.json.simple.JSONObject;
import org.labkey.remoteapi.CommandException;
import org.labkey.remoteapi.CommandResponse;
import org.labkey.remoteapi.Connection;
import org.labkey.remoteapi.PostCommand;
import org.labkey.test.Locator;
import org.labkey.test.Locators;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.WebTestHelper;
import org.labkey.test.components.html.Input;
import org.labkey.test.pages.LabKeyPage;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.Select;

import java.io.IOException;

public class LimitActiveUserPage extends LabKeyPage<LimitActiveUserPage.ElementCache>
{

    private static UserLimitSettings initialSettings;

    public LimitActiveUserPage(WebDriver driver)
    {
        super(driver);
    }

    public static LimitActiveUserPage beginAt(WebDriverWrapper wrapper)
    {
        wrapper.beginAt(WebTestHelper.buildURL("user", "limitActiveUsers"));
        return new LimitActiveUserPage(wrapper.getDriver());
    }

    @Override
    protected void waitForPage()
    {
        if (initialSettings == null)
        {
            initialSettings = new UserLimitSettings(this);
        }
    }

    public boolean isWarningEnabled()
    {
        return elementCache().userWarning.getFirstSelectedOption().getAttribute("value").equals("1");
    }

    public LimitActiveUserPage enableUserWarning(boolean enable)
    {
        elementCache().userWarning.selectByValue(enable ? "1" : "0");
        return this;
    }

    public boolean isLimitEnabled()
    {
        return elementCache().userLimit.getFirstSelectedOption().getAttribute("value").equals("1");
    }

    public LimitActiveUserPage enableUserLimit(boolean enable)
    {
        elementCache().userLimit.selectByValue(enable ? "1" : "0");
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

    public String saveExpectingError()
    {
        clickAndWait(elementCache().saveBtn);
        clearCache();
        return Locators.labkeyError.findOptionalElement(getDriver()).map(WebElement::getText).orElse(null);
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
    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    protected class ElementCache extends LabKeyPage<?>.ElementCache
    {
        protected final Select userWarning = new Select(Locator.id("userWarning").findWhenNeeded(this));
        protected final Input userWarningLevel = Input.Input(Locator.id("userWarningLevel"), getDriver()).findWhenNeeded(this);
        protected final WebElement userWarningMessage = Locator.id("userWarningMessage").findWhenNeeded(this);

        protected final Select userLimit = new Select(Locator.id("userLimit").findWhenNeeded(this));
        protected final Input userLimitLevel = Input.Input(Locator.id("userLimitLevel"), getDriver()).findWhenNeeded(this);
        protected final WebElement userLimitMessage = Locator.id("userLimitMessage").findWhenNeeded(this);

        protected final WebElement saveBtn = Locator.lkButton("Save").findWhenNeeded(this);
        protected final WebElement cancelBtn = Locator.lkButton("Cancel").findWhenNeeded(this);

    }

    public static void resetUserLimits(Connection cn) throws IOException, CommandException
    {
        if (initialSettings != null)
        {
            PostCommand<CommandResponse> command = new PostCommand<>("user", "limitActiveUsers");
            command.setJsonObject(initialSettings.toJsonObject());
            command.execute(cn, "/");
            initialSettings = null;
        }
    }

    public static class UserLimitSettings
    {
        final boolean userWarning;
        final String userWarningLevel;
        final String userWarningMessage;
        final boolean userLimit;
        final String userLimitLevel;
        final String userLimitMessage;

        public UserLimitSettings(LimitActiveUserPage activeUserPage)
        {
            userWarning = activeUserPage.isWarningEnabled();
            userWarningLevel = activeUserPage.getUserWarningLevel();
            userWarningMessage = activeUserPage.getUserWarningMessage();
            userLimit = activeUserPage.isLimitEnabled();
            userLimitLevel = activeUserPage.getUserLimitLevel();
            userLimitMessage = activeUserPage.getUserLimitMessage();
        }

        public JSONObject toJsonObject()
        {
            JSONObject json = new JSONObject();
            json.put("userWarning", userWarning);
            json.put("userWarningLevel", userWarningLevel);
            json.put("userWarningMessage", userWarningMessage);
            json.put("userLimit", userLimit);
            json.put("userLimitLevel", userLimitLevel);
            json.put("userLimitMessage", userLimitMessage);
            return json;
        }
    }
}
