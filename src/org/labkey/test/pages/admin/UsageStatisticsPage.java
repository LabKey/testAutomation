package org.labkey.test.pages.admin;

import org.labkey.test.BootstrapLocators;
import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.WebTestHelper;
import org.labkey.test.components.html.Input;
import org.labkey.test.pages.LabKeyPage;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.util.List;

public class UsageStatisticsPage extends LabKeyPage<UsageStatisticsPage.ElementCache>
{
    public UsageStatisticsPage(WebDriverWrapper driver)
    {
        super(driver);
    }

    public static UsageStatisticsPage beginAt(WebDriverWrapper webDriverWrapper)
    {
        webDriverWrapper.beginAt(WebTestHelper.buildURL("admin", "viewUsageStatistics"));
        return new UsageStatisticsPage(webDriverWrapper);
    }

    @Override
    protected void waitForPage()
    {
        WebDriverWrapper.waitFor(()-> Locator.id("stats-path").existsIn(getDriver()) &&
                !BootstrapLocators.loadingSpinner.existsIn(getDriver()),
                "the page did not become ready in time", WAIT_FOR_PAGE);
    }

    public UsageStatisticsPage clickReloadButton()
    {
        elementCache().reloadBtn.click();
        var spinner = BootstrapLocators.loadingSpinner.waitForElement(getDriver(), 2000);
        longWait().until(ExpectedConditions.stalenessOf(spinner));

        return this;
    }

    public UsageStatisticsPage clickClearButton()
    {
        elementCache().clearBtn.click();
        return this;
    }

    public void setJsonPathInput(String jsonPath)
    {
        var currentPath = getJsonPath();
        var validKeys = getValidKeys();
        elementCache().jsonPathInput.setValue(jsonPath);
        if (!currentPath.equals(jsonPath))
        {
            WebDriverWrapper.waitFor(()-> getValidKeys() != validKeys,
                    "The page did not update after setting a new path", WAIT_FOR_JAVASCRIPT);
        }
    }

    public String getJsonPath()
    {
        return elementCache().jsonPathInput.getValue();
    }

    /**
     * gets the value of the content element, which will be json if not at the end of a key-path, otherwise the value
     */
    public String getValue()
    {
        return elementCache().contentElement().getText();
    }

    public String getInvalidMessage()
    {
        return elementCache().getInvalidMessage();
    }

    public boolean isValidKeyPresent(String key)
    {
        return elementCache().validKeyLoc.withText(key).existsIn(elementCache().panelBody);
    }

    public UsageStatisticsPage clickValidKey(String key)
    {
        var keylink = elementCache().validKeyLoc.withText(key).waitForElement(elementCache().panelBody, 1000);
        keylink.click();
        shortWait().until(ExpectedConditions.stalenessOf(keylink));     // could also wait for content element to go stale and be re-drawn
        return this;
    }

    public List<String> getValidKeys()
    {
        return getTexts(elementCache().validKeyLoc.findElements(elementCache().panelBody));
    }

    @Override
    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    protected class ElementCache extends LabKeyPage<ElementCache>.ElementCache
    {
        final WebElement panelBody = Locator.tagWithClass("div", "usage-stats")
                .child(Locator.tagWithClass("div", "panel-body")).findWhenNeeded(getDriver());
        final WebElement reloadBtn = Locator.tagWithText("button", "Reload Usage Statistics").findWhenNeeded(panelBody);
        final WebElement clearBtn = Locator.tagWithText("button", "clear").findWhenNeeded(panelBody);
        final Input jsonPathInput = Input.Input(Locator.id("stats-path"), getDriver()).timeout(2000).findWhenNeeded(panelBody);

        final Locator invalidMsgLoc = Locator.tagWithClass("div", "usage-stats__invalid-message");
        final Locator.XPathLocator validKeysContainerLoc = Locator.tagWithClass("div", "usage-stats__valid-keys");
        final Locator validKeyLoc = validKeysContainerLoc.descendant(Locator.tagWithClass("li", "clickable-text"));
        public String getInvalidMessage()
        {
            if (!invalidMsgLoc.existsIn(panelBody))
                return null;
            else
                return invalidMsgLoc.findElement(panelBody).getText();
        }

        WebElement contentElement()
        {
            return Locator.tagWithClass("div", "usage-stats__search-result").child("pre").findElement(panelBody);
        }
    }
}
