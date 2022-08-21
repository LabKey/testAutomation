package org.labkey.test.components.ui.notifications;

import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.components.react.BaseBootstrapMenu;
import org.labkey.test.components.react.MultiMenu;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;


/**
 * This covers a couple of components under component/notifications. Mostly ServerNotifications.tsx and ServerActivityList.tsx
 */
public class ServerNotificationMenu extends BaseBootstrapMenu
{

    protected ServerNotificationMenu(WebElement element, WebDriver driver)
    {
        super(element, driver);
    }

    public static SimpleWebDriverComponentFinder<ServerNotificationMenu> finder(WebDriver driver)
    {
        return new MultiMenu.MultiMenuFinder(driver).withButtonId("server-notifications-button").wrap(ServerNotificationMenu::new);
    }

    /**
     * Get the number of unread notifications. If no number is shown the implication is that there are no (0) unread notifications.
     *
     * @return The number of unread notifications.
     */
    public int getUnreadCount()
    {
        String text = "0";
        boolean stale = true;

        // Bit of a challenge to get the count. The element can update (because, you know it's async) if it does then a
        // StaleElementException will happen. Try to protect against that by getting the element again f stale.
        // If it is not there at all it will return null and exit the loop gracefully.
        while(stale)
        {
            WebElement badge = Locator.tagWithClass("span", "badge").findElementOrNull(this);

            if (badge != null)
            {
                try
                {
                    text = badge.getText().trim();
                    stale = false;
                }
                catch (StaleElementReferenceException exception)
                {
                    // If the element went stale it may have been updated.
                    stale = true;
                }
            }
            else
            {
                stale = false;
            }
        }

        return Integer.parseInt(text);

    }

    /**
     * Check to see if a loading indicator, a spinner, is present.
     *
     * @return True if loading, false otherwise.
     */
    public boolean isLoading()
    {
        return getStatus().equals(MenuStatus.RUNNING);
    }

    /**
     * Get the status as indicated by the icon shown.
     *
     * @return A {@link MenuStatus} enum.
     */
    public MenuStatus getStatus()
    {
        String status = elementCache().statusIcon().getAttribute("class").toLowerCase();

        if(status.contains("fa-bell"))
        {
            return MenuStatus.COMPLETE;
        }
        else
        {
            return MenuStatus.RUNNING;
        }
    }

    /**
     * Is the 'Mark all as read' link visible?
     *
     * @return True if visible, false otherwise.
     */
    public boolean isMarkAllVisible()
    {
        return elementCache().markAll().isDisplayed();
    }

    /**
     * Click the 'Mark all as read' link. This will cause the list to refresh, any references you have to a
     * {@link ServerNotificationItem} will need to be reacquired.
     */
    public void clickMarkAll()
    {
        expand();
        elementCache().markAll().click();
    }

    /**
     * Private helper to make sure that the list of notifications has been populated.
     *
     * @return The web element containing the list.
     */
    private WebElement waitForNotificationList()
    {

        expand();

        // Wait for the listing container to show up.
        Locator notificationsContainerLocator = Locator.tagWithClass("div", "server-notifications-listing-container");
        WebDriverWrapper.waitFor(()-> notificationsContainerLocator.refindWhenNeeded(this).isDisplayed(),
                "List container did not render.", 500);

        // Find again (lambda requires a final reference to the component).
        WebElement listContainer = notificationsContainerLocator.refindWhenNeeded(this);

        // It may be a moment before any notifications show up.
        WebDriverWrapper.waitFor(()-> Locator.tagWithClass("ul", "server-notifications-listing")
                        .refindWhenNeeded(listContainer)
                        .isDisplayed(),
                "There are no notifications in the drop down.", 1_000);

        // Just wait for a moment in case the list is slow to update with the most recent notification.
        WebDriverWrapper.sleep(500);

        // If we are here there is confidence that the menu dropdown contains some notification elements. However, there
        // is no guarantee that it contains the most recent message. For example if an import is in progress a message
        // will appear in the list saying as much. When the import is done the 'in progress' message will go away and
        // be replaced with a new 'complete' message.

        // Find the container again, don't return listContainer WebElement previously found. If the list was slow to
        // update with the most recent notification the old reference will be stale.
        return notificationsContainerLocator.findElement(this);
    }

    /**
     * Expand the menu and return the notification at the given index.
     *
     * @param notificationIndex Zero based index of the notification. The newest notification is at index 0.
     * @return A {@link ServerNotificationItem} object.
     */
    public ServerNotificationItem getNotification(int notificationIndex)
    {
        WebElement listContainer = waitForNotificationList();
        return new ServerNotificationItem.ServerNotificationItemFinder(getDriver()).atIndex(notificationIndex).refindWhenNeeded(listContainer);
    }

    /**
     * Expand the menu and return that has the given message content (must match completely).
     *
     * @param msgText Text of the message.
     * @return A {@link ServerNotificationItem} object.
     */
    public ServerNotificationItem getNotification(String msgText)
    {
        WebElement listContainer = waitForNotificationList();
        return new ServerNotificationItem.ServerNotificationItemFinder(getDriver()).withMessageContaining(msgText).refindWhenNeeded(listContainer);

    }

    /**
     * If no notifications get the message that is present.
     *
     * @return Message shown if there are no notifications, and empty string of there are notifications.
     */
    public String getNoNotificationsMessage()
    {

        expand();

        if(elementCache().noNotificationsElement().isDisplayed())
        {
            return elementCache().noNotificationsElement().getText();
        }
        else
        {
            return "";
        }

    }

    @Override
    protected Locator getToggleLocator()
    {
        return Locator.tagWithId("button", "server-notifications-button");
    }

    @Override
    protected ElementCache elementCache()
    {
        return (ElementCache) super.elementCache();
    }

    @Override
    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    protected class ElementCache extends BaseBootstrapMenu.ElementCache
    {
        public final Locator notificationList = Locator.tagWithClass("ul", "server-notifications-listing");

        public final WebElement statusIcon()
        {
            return Locator.byClass("navbar-header-icon").refindWhenNeeded(this);
        }

        public final WebElement noNotificationsElement()
        {
            return Locator.tagWithClass("div", "server-notifications-footer").refindWhenNeeded(this);
        }

        public final WebElement markAll()
        {
            return Locator.tagWithClass("h3", "navbar-menu-header")
                    .child(Locator.tagWithClass("div", "server-notifications-link"))
                    .refindWhenNeeded(this);
        }

    }

    /**
     * The two status the menu can display with the icon.
     */
    public enum MenuStatus
    {
        COMPLETE,
        RUNNING
    }

}
