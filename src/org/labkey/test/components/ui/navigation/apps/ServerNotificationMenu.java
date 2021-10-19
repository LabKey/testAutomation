package org.labkey.test.components.ui.navigation.apps;

import org.labkey.test.Locator;
import org.labkey.test.components.react.BaseBootstrapMenu;
import org.labkey.test.components.react.MultiMenu;
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
        WebElement badge = Locator.tagWithClass("span", "badge").findElementOrNull(this);

        if(badge != null)
        {
            String text = badge.getText().trim();
            return Integer.parseInt(text);
        }
        else
        {
            return 0;
        }
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

        if(getWrapper().isElementPresent(elementCache().notificationsContainerLocator))
        {
            return elementCache().notifications();
        }
        else
        {
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
            return notificationsContainerLocator.findWhenNeeded(this);
        }

        public final List<ServerNotificationItem> notifications()
        {
            return new ServerNotificationItem.AppNotificationEntryFinder(getDriver()).findAll(notificationsContainer());
        }

        public final WebElement statusIcon()
        {
            return Locator.tagWithClass("i", "fa").findWhenNeeded(this);
        }

        public final WebElement noNotificationsElement()
        {
            return Locator.tagWithClass("div", "server-notifications-footer").findWhenNeeded(this);
        }

        public final WebElement markAll()
        {
            return Locator.tagWithClass("h3", "navbar-menu-header")
                    .child("div[contains(@class,'server-notifications-link')]").findWhenNeeded(this);
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
