package org.labkey.test.components.ui.notifications;

import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.components.Component;
import org.labkey.test.components.WebDriverComponent;
import org.labkey.test.util.TestLogger;
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
     * Get the status as indicated by the icon.
     *
     * @return A {@link ItemStatus} enum value.
     */
    public ItemStatus getStatus()
    {
        String status = elementCache().status.getAttribute("class").toLowerCase();

        if(status.contains("has-error"))
        {
            return ItemStatus.ERROR;
        }
        else if (status.contains("is-complete"))
        {
            return ItemStatus.COMPLETE;
        }
        else
        {
            return ItemStatus.RUNNING;
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
     * Click the message. Should mark it as read. Note, if the message is unread this will change the state and make
     * the current reference to this notification item stale. You will need to get a new reference to the item.
     */
    public void clickMessage()
    {
        elementCache().message.click();
    }

    /**
     * See if the notification is marked as unread.
     *
     * @return True if unread class attribute is present, false otherwise.
     */
    public boolean isUnread()
    {
        return elementCache().message.getAttribute("class").toLowerCase().contains("is-unread");
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
     *     Click the 'View' link.
     * </p>
     * <p>
     *     This may cause a navigation to occur. For example if the import failed this will take you to the error
     *     report, if a success it will go someplace else depending upon the app, or the app may already be on the page.
     * </p>
     * <p>
     *     This function will wait until the current url matches the url in the link. It is up to the calling code to
     *     know which page the navigation is going to.
     * </p>
     */
    public void clickViewLink()
    {
        String currentUrl = getDriver().getCurrentUrl().toLowerCase();
        String targetUrl = elementCache().link.getAttribute("href").toLowerCase();

        TestLogger.log(String.format("ServerNotificationItem.clickViewLink: currentUrl: %s", currentUrl));
        TestLogger.log(String.format("ServerNotificationItem.clickViewLink: targetUrl: %s", targetUrl));

        elementCache().link.click();

        // If the url before clicking the link is the same as the target then don't wait for a navigation.
        if(!targetUrl.equalsIgnoreCase(currentUrl))
        {
            WebDriverWrapper.waitFor(()->!getDriver().getCurrentUrl().equalsIgnoreCase(currentUrl),
                    "Clicking the 'View' link in the notification did not navigate in time.", 5_000);
        }

    }

    @Override
    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    protected class ElementCache extends Component<?>.ElementCache
    {
        private final WebElement status = Locator.tagWithClass("i", "fa").refindWhenNeeded(this);
        private final WebElement message = Locator.tagWithClass("span", "server-notification-message").refindWhenNeeded(this);
        private final WebElement date = Locator.tagWithClass("div", "server-notification-data").refindWhenNeeded(this);
        private final WebElement userName = Locator.tagWithClass("span", "server-notification-data").refindWhenNeeded(this);
        private final WebElement link = Locator.tagWithClass("span", "server-notifications-link").childTag("a").refindWhenNeeded(this);
    }


    static class ServerNotificationItemFinder extends WebDriverComponentFinder<ServerNotificationItem, ServerNotificationItemFinder>
    {

        private int index = 0;
        private String msgText = null;

        public ServerNotificationItemFinder(WebDriver driver)
        {
            super(driver);
        }

        public ServerNotificationItemFinder atIndex(int index)
        {
            this.index = index;
            return this;
        }

        public ServerNotificationItemFinder withMessageContaining(String msgText)
        {
            this.msgText = msgText;
            return this;
        }

        @Override
        protected ServerNotificationItemFinder getThis()
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
            if(msgText != null)
            {
                return Locator.tagWithClass("ul", "server-notifications-listing").childTag("li")
                        .withDescendant(Locator.tagWithClass("span", "server-notifications-item-subject")
                                .withText(msgText));
            }
            else
            {
                return Locator.tagWithClass("ul", "server-notifications-listing").childTag("li")
                        .index(index);
            }
        }

    }

    /**
     * The various states of the notification item.
     */
    public enum ItemStatus
    {
        COMPLETE,
        ERROR,
        RUNNING
    }

}
