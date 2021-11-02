package org.labkey.test.components.ui.notifications;

import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.components.react.BaseBootstrapMenu;
import org.labkey.test.components.react.MultiMenu;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.ArrayList;
import java.util.List;

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
     * Get the number of unread messages. If no number is shown the implication is that there are no (0) unread messages.
     *
     * @return The number of unread messages.
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
        return getStatus().equals(LoadingStatus.RUNNING);
    }

    /**
     * Get the status as indicated by the icon shown.
     *
     * @return A {@link LoadingStatus} enum.
     */
    public LoadingStatus getStatus()
    {
        String status = elementCache().statusIcon().getAttribute("class").toLowerCase();

        if(status.contains("fa-bell"))
        {
            return LoadingStatus.COMPLETE;
        }
        else
        {
            return LoadingStatus.RUNNING;
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
     * Click the 'Mark all as read' link. This will cause the list to refresh, so return a new reference to the
     * notification list.
     *
     * @return A new list of {@link ServerNotificationItem}.
     */
    public List<ServerNotificationItem> clickMarkAll()
    {
        if(!isExpanded())
        {
            expand();
        }

        elementCache().markAll().click();
        return getNotifications();
    }

    /**
     * Expand the menu and return the notification.
     *
     * @return A list of {@link ServerNotificationItem}, empty if there are none.
     */
    public List<ServerNotificationItem> getNotifications()
    {
        if(!isExpanded())
            expand();

        if(elementCache().noNotificationsElement().isDisplayed())
            return new ArrayList<>();

        if(getWrapper().isElementPresent(elementCache().notificationsContainerLocator))
        {
            // Because a query is used to populate the dropdown, it may be a moment before any notifications show up.
            WebDriverWrapper.waitFor(()->
                    {
                        try {
                            return !elementCache().notifications().isEmpty();
                        }
                        catch (StaleElementReferenceException exp)
                        {
                            return false;
                        }
                    },
                    "There are no notifications in the drop down.", 1_000);
            return elementCache().notifications();
        }
        else
        {
            // This is the path if the menu is expanded and there are no messages.
            return new ArrayList<>();
        }

    }

    /**
     * If no notifications get the message that is present.
     *
     * @return Message shown if there are no notifications, and empty string of there are notifications.
     */
    public String getNoNotificationsMessage()
    {

        if(!isExpanded())
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
        private final Locator notificationsContainerLocator = Locator.tagWithClass("div", "server-notifications-listing-container");

        public final WebElement notificationsContainer()
        {
            return notificationsContainerLocator.refindWhenNeeded(this);
        }

        public final List<ServerNotificationItem> notifications()
        {
            return new ServerNotificationItem.AppNotificationEntryFinder(getDriver()).findAll(notificationsContainer());
        }

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
    public enum LoadingStatus
    {
        COMPLETE,
        RUNNING
    }

}
