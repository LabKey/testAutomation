package org.labkey.test.pages;

import org.labkey.test.BootstrapLocators;
import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.WebTestHelper;
import org.openqa.selenium.Keys;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;

import java.util.List;
import java.util.stream.Collectors;

import static org.labkey.test.util.selenium.WebElementUtils.getTextContent;

public class TestChatPage extends LabKeyPage<TestChatPage.ElementCache>
{

    private int _numOfResponses = 0;

    public TestChatPage(WebDriver driver)
    {
        super(driver);
    }

    public static TestChatPage beginAt(WebDriverWrapper driver)
    {
        driver.beginAt(WebTestHelper.buildURL("test", "chat"));
        return new TestChatPage(driver.getDriver());
    }

    @Override
    protected void waitForPage()
    {
        waitFor(() -> {
            try
            {
                return !BootstrapLocators.loadingSpinner.areAnyVisible(getDriver())
                        && Locator.tagWithId("textarea", "chatPrompt")
                        .refindWhenNeeded(getDriver()).isDisplayed()
                        && Locator.tagWithClass("div", "genaiResponse")
                        .findElements(getDriver()).size() == 1;
            }
            catch (NoSuchElementException | StaleElementReferenceException | TimeoutException retry)
            {
                return false;
            }
        }, "There is a problem loading the chat page.", 30_000);

    }

    public void enterPrompt(String prompt)
    {

        _numOfResponses = Locator.tagWithClass("div", "genaiResponse")
                .findElements(getDriver()).size();

        elementCache().chatPrompt.click();

        Actions actions = new Actions(getDriver());
        actions.sendKeys(prompt)
                .keyDown(Keys.SHIFT)
                .keyDown(Keys.ENTER)
                .keyUp(Keys.ENTER)
                .keyUp(Keys.SHIFT)
                .build()
                .perform();

        sleep(500);

    }

    public String getMostRecentResponse()
    {
        log("getResponse: Current num of responses: " +
                Locator.tagWithClass("div", "genaiResponse").findElements(getDriver()).size());

        waitFor(() -> {
            try
            {
                return !BootstrapLocators.loadingSpinner.areAnyVisible(getDriver())
                        && elementCache().chatPrompt.isDisplayed()
                        && Locator.tagWithClass("div", "genaiResponse")
                        .findElements(getDriver()).size() > _numOfResponses;
            }
            catch (NoSuchElementException | StaleElementReferenceException | TimeoutException retry)
            {
                return false;
            }
        }, "I haven't seen a new response.", 120_000);

        _numOfResponses = Locator.tagWithClass("div", "genaiResponse")
                .findElements(getDriver()).size();

        log("getResponse: Num of responses: " + _numOfResponses);

        return Locator.tagWithClass("div", "genaiResponse")
                .findElements(getDriver()).getLast().getText();
    }

    public List<String> getAllResponses()
    {
            waitFor(() -> {
                try
                {
                    return !BootstrapLocators.loadingSpinner.areAnyVisible(getDriver())
                            && elementCache().chatPrompt.isDisplayed();
                }
                catch (NoSuchElementException | StaleElementReferenceException | TimeoutException retry)
                {
                    return false;
                }
            }, "Timed out waiting for the current process to stop.", 120_000);

        List<WebElement> responses = Locator.tagWithClass("div", "genaiResponse").findElements(getDriver());
        return responses.stream().map(el -> getTextContent(el).trim()).collect(Collectors.toList());

    }

    @Override
    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    protected class ElementCache extends LabKeyPage<?>.ElementCache
    {
        WebElement chatPrompt = Locator.tagWithId("textarea", "chatPrompt")
                .refindWhenNeeded(this);
    }
}
