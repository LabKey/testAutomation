package org.labkey.test.components.ui.navigation.apps;

import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.components.WebDriverComponent;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

/**
 * <p>
 *     This covers a couple of components under component/notifications. Mostly NotificationItem.tsx and Notification.tsx
 * </p>
 * <p>
 *     Since several of these components may or may not be present depending upon the current state of the import the
 *     'getters' for the component will return an empty string if they are not present (rather than error out). For
 *     example while a pipeline job is running there is no 'View' link/text but there is a 'UserName'. However once the
 *     pipeline job has finished there is a 'View' link but no 'UserName'.
 * </p>
 *
 */
public class ServerNotificationItem extends WebDriverComponent<ServerNotificationItem.ElementCache>
{
    WebElement componentElement;
    WebDriver driver;

    protected ServerNotificationItem(WebElement componentElement, WebDriver driver)
    {
        this.componentElement = componentElement;
        this.driver = driver;
    }

    @Override
    protected WebDriver getDriver()
    {
        return driver;
    }

    @Override
    public WebElement getComponentElement()
    {
        return componentElement;
    }

    /**
     * Get the message status as indicated by the icon.
     *
     * @return A {@link MessageStatus} enum value.
     */
    public MessageStatus getStatus()
    {
        String status = elementCache().status.getAttribute("class").toLowerCase();

        if(status.contains("has-error"))
        {
            return MessageStatus.ERROR;
        }
        else if (status.contains("is-complete"))
        {
            return MessageStatus.COMPLETE;
        }
        else
        {
            return MessageStatus.RUNNING;
        }
    }

    /**
     * Get the message displayed for the item. If the import had an error this includes the subject and detail of the message.
     *
     * @return The text of the item details and subject if present. Empty string if neither are present.
     */
    public String getMessage()
    {
        if(elementCache().message.isDisplayed())
        {
            return elementCache().message.getText();
        }
        else
        {
            return "";
        }
    }

    /**
     * The date of the import.
     *
     * @return Date, empty if not present.
     */
    public String getDate()
    {
        if(elementCache().date.isDisplayed())
        {
            return elementCache().date.getText();
        }
        else
        {
            return "";
        }
    }

    /**
     * The displayed username while an import is in progress. Once the import completes this is not present.
     *
     * @return The username displayed, empty string if not present.
     */
    public String getUserName()
    {
        if(elementCache().userName.isDisplayed())
        {
            return elementCache().userName.getText();
        }
        else
        {
            return "";
        }
    }

    /**
     * The text for the 'View' link. Will not be present during an import but is visible for both a successful and unsuccessful import.
     *
     * @return Text of the 'View' link, empty string if not present.
     */
    public String getViewLinkText()
    {
        if(elementCache().link.isDisplayed())
        {
            return elementCache().link.getText();
        }
        else
        {
            return "";
        }
    }

    /**
     * <p>
     *     Click the 'View' link in the message entry.
     * </p>
     * <p>
     *     This may cause a navigation to occur. For example if the import failed this will take you to the error
     *     report, if a success it will go someplace else depending upon the app.
     * </p>
     * <p>
     *     This will wait until the url changes. It is up to the consuming code to know which page the navigation went to.
     * </p>
     */
    public void clickViewLink()
    {
        String currentUrl = getDriver().getCurrentUrl().toLowerCase();
        String targetUrl = elementCache().link.getAttribute("href").toLowerCase();

        elementCache().link.click();

        // Check if currently on the target page.
        if(!currentUrl.contains(targetUrl))
        {
            WebDriverWrapper.waitFor(()->!getDriver().getCurrentUrl().toLowerCase().contains(currentUrl),
                    "Clicking the 'View' link in the notification did not navigate in time.", 5_000);
        }

    }

    @Override
    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    protected class ElementCache extends WebDriverComponent.ElementCache
    {
        private final WebElement status = Locator.tagWithClass("i", "fa").findWhenNeeded(this);
        private final WebElement message = Locator.tagWithClass("span", "server-notification-message").findWhenNeeded(this);
        private final WebElement date = Locator.tagWithClass("div", "server-notification-data").findWhenNeeded(this);
        private final WebElement userName = Locator.tagWithClass("span", "server-notification-data").findWhenNeeded(this);
        private final WebElement link = Locator.tagWithClass("span", "server-notifications-link").childTag("a").findWhenNeeded(this);
    }


    public static class AppNotificationEntryFinder extends WebDriverComponentFinder<ServerNotificationItem, AppNotificationEntryFinder>
    {

        private final Locator.XPathLocator locator = Locator.tag("li");

        public AppNotificationEntryFinder(WebDriver driver)
        {
            super(driver);
        }

        @Override
        protected AppNotificationEntryFinder getThis()
        {
            return this;
        }

        @Override
        protected ServerNotificationItem construct(WebElement el, WebDriver driver)
        {
            return new ServerNotificationItem(el, driver);
        }

        @Override
        protected Locator locator()
        {
            return locator;
        }
    }

    /**
     * The various states of the notification item.
     */
    public enum MessageStatus
    {
        COMPLETE,
        ERROR,
        RUNNING
    }

}
