/*
 * Copyright (c) 2016-2017 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.labkey.test.components.core;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.Locators;
import org.labkey.test.components.Component;
import org.labkey.test.components.WebDriverComponent;
import org.labkey.test.selenium.LazyWebElement;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class UserNotificationsPanel extends WebDriverComponent<UserNotificationsPanel.Elements>
{
    private static final Locator inboxIcon = Locator.xpath("//a[contains(@onclick, 'LABKEY.Notification.showPanel')]");
    private static final Locator inboxCount = Locator.byClass("labkey-notification-inbox").followingSibling("span").attributeStartsWith("id", "labkey-notifications-count");
    private static final Locator.XPathLocator visibleNotification = Locator.byClass("labkey-notification");

    protected final WebDriver _driver;
    protected final WebElement _notificationPanel;

    public UserNotificationsPanel(WebDriver driver)
    {
        _driver = driver;
        _notificationPanel = Locator.tagWithClass("div", "labkey-notification-panel").findElement(driver);
    }

    public static int getInboxCount(BaseWebDriverTest test)
    {
        String s = test.getText(inboxCount);
        if (StringUtils.isEmpty(s))
            return 0;
        return Integer.parseInt(s);
    }

    public static UserNotificationsPanel clickInbox(BaseWebDriverTest test)
    {
        test.click(inboxIcon);
        test.waitForElement(Locators.pageSignal("notificationPanelShown"), 2000);
        return new UserNotificationsPanel(test.getDriver());
    }

    @Override
    protected WebDriver getDriver()
    {
        return _driver;
    }

    public boolean isNotificationPanelVisible()
    {
        return getComponentElement().isDisplayed();
    }

    public List<String> getNotificationTypesShown()
    {
        return getWrapper().getTexts(Locator.css("div.labkey-notification-area div.labkey-notification-type-label").findElements(this));
    }

    public int getNotificationCount()
    {
        int count;

        if (elementCache().notificationArea.getAttribute("style").toLowerCase().contains("display: none"))
        {
            count = 0;
        }
        else
        {
            count = visibleNotification.findElements(this).size();
        }

        return count;
    }

    public List<NotificationPanelItem> getAllNotifications()
    {
        List<NotificationPanelItem> notifications = new ArrayList<>();
        for (WebElement we : visibleNotification.findElements(this))
        {
            notifications.add(new NotificationPanelItem(we));
        }

        return notifications;
    }

    public NotificationPanelItem getNotificationAtIndex(int idx)
    {
        List<WebElement> notifications;
        notifications = visibleNotification.findElements(this);
        return new NotificationPanelItem(notifications.get(idx));
    }

    public List<NotificationPanelItem> getNotificationsOfType(String notificationType)
    {
        List<NotificationPanelItem> notificationItemListlist = new ArrayList<>();
        List<WebElement> notifications;

        notifications = elementCache().findNotificationsOfType(notificationType);
        for (WebElement we : notifications)
        {
            NotificationPanelItem ni = new NotificationPanelItem(we);
            notificationItemListlist.add(ni);
        }

        return notificationItemListlist;
    }

    public NotificationPanelItem findNotificationInList(String searchBody, @Nullable String notificationType)
    {
        List<NotificationPanelItem> notificationItemList;

        if (notificationType == null)
        {
            notificationItemList = getAllNotifications();
        }
        else
        {
            notificationItemList = getNotificationsOfType(notificationType);
        }

        Pattern pattern = Pattern.compile(searchBody);
        for (NotificationPanelItem ni : notificationItemList)
        {
            if (pattern.matcher(ni.getBody()).find())
            {
                return ni;
            }
        }
        return null;
    }

    @Override
    public WebElement getComponentElement()
    {
        return _notificationPanel;
    }

    @Deprecated
    public Elements elements()
    {
        return elementCache();
    }

    @Override
    protected Elements newElementCache()
    {
        return new Elements();
    }

    public class Elements extends Component.ElementCache
    {
        private final WebElement notificationArea = new LazyWebElement(Locator.css("div.labkey-notification-area"), this);
        public final WebElement clearAll = new LazyWebElement(Locator.css("div.labkey-notification-clear-all"), this);
        public final WebElement noNotifications = new LazyWebElement(Locator.css("div.labkey-notification-none"), this);
        public final WebElement viewAll = new LazyWebElement(Locator.css("div.labkey-notification-footer"), this);
        protected List<WebElement> findNotificationsOfType(String notificationType)
        {
            return Locator.id("notificationtype-" + notificationType).child(visibleNotification).findElements(this);
        }
    }
}
