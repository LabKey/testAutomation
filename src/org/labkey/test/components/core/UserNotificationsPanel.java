/*
 * Copyright (c) 2016 LabKey Corporation
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

import org.jetbrains.annotations.Nullable;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.Locators;
import org.labkey.test.components.Component;
import org.labkey.test.components.ComponentElements;
import org.labkey.test.selenium.LazyWebElement;
import org.openqa.selenium.By;
import org.openqa.selenium.SearchContext;
import org.openqa.selenium.WebElement;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UserNotificationsPanel extends Component
{

    private static final Locator.XPathLocator menuBar = Locator.xpath("//div[@id='menubar']");
    private static final Locator.XPathLocator inboxIcon = menuBar.append("//a[contains(@onclick, 'LABKEY.Notification.showPanel')]");
    private static final Locator.XPathLocator inboxCount = inboxIcon.append("//span[2]");

    protected WebElement _notificationPanel;

    public UserNotificationsPanel(WebElement we)
    {
        _notificationPanel = we;
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
        return new UserNotificationsPanel(Locator.css("div.labkey-notification-panel").findElement(test.getDriver()));
    }

    public boolean isNotificationPanelVisible()
    {
        return _notificationPanel.isDisplayed();
    }

    public List<String> getNotificationTypesShown()
    {
        List<String> types = new ArrayList<>();
        for(WebElement we : _notificationPanel.findElements(By.cssSelector(" div.labkey-notification-area div.labkey-notification-type")))
        {
            types.add(we.getText());
        }

        return types;
    }

    public int getNotificationCount()
    {
        int count;

        if(elements().notificationArea.getAttribute("style").toLowerCase().contains("display: none"))
        {
            count = 0;
        }
        else
        {
            count = _notificationPanel.findElements(By.cssSelector(" div.labkey-notification:not([style='display: none;'])")).size();
        }

        return count;
    }

    public List<NotificationPanelItem> getAllNotifications()
    {
        List<NotificationPanelItem> notifications = new ArrayList<>();
        for(WebElement we : _notificationPanel.findElements(By.cssSelector(" div.labkey-notification:not([style='display: none;'])")))
        {
            notifications.add(new NotificationPanelItem(we));
        }

        return notifications;
    }

    public NotificationPanelItem getNotificationAtIndex(int idx)
    {
        List<WebElement> notifications;
        notifications = _notificationPanel.findElements(By.cssSelector(" div.labkey-notification:not([style='display: none;'])"));
        return new NotificationPanelItem(notifications.get(idx));
    }

    public List<NotificationPanelItem> getNotificationsOfType(NotificationTypes notificationType)
    {
        List<NotificationPanelItem> notificationItemListlist = new ArrayList<>();
        List<WebElement> notifications;
        String tagId;

        switch(notificationType)
        {
            case ISSUES:
                tagId = NotificationTypes.ISSUES.tagId;
                break;
            case STUDY:
                tagId = NotificationTypes.STUDY.tagId;
                break;
            default:
                tagId = "";
                break;
        }

        // This will return all div's that are a sibling of the type you are looking for.
        // Unfortunately this will include any other div of a different type (they are still a sibling).
        // So loop through the list of elements returned and add any element that is of class labkey-notification,
        // once we hit a div not of that class it means we are in a new section, so we can stop.

        notifications = elements().findElements(By.cssSelector(" div#" + tagId + " ~ div:not([style='display: none;'])"));
        for(WebElement we : notifications)
        {
            if(we.getAttribute("class").equals("labkey-notification"))
            {
                NotificationPanelItem ni = new NotificationPanelItem(we);
                notificationItemListlist.add(ni);
            }
            else
            {
                break;
            }
        }

        return notificationItemListlist;
    }

    public NotificationPanelItem findNotificationInList(String searchBody, @Nullable NotificationTypes notificationType)
    {
        int noticeIndex;
        boolean noticeFound;
        List<NotificationPanelItem> notificationItemList = new ArrayList<>();

        if(notificationType == null)
        {
            notificationItemList =getAllNotifications();
        }
        else
        {
            notificationItemList = getNotificationsOfType(notificationType);
        }

        Pattern pattern = Pattern.compile(searchBody);
        Matcher matcher;
        noticeIndex = 0;
        noticeFound = false;
        for(NotificationPanelItem ni : notificationItemList)
        {
            matcher = pattern.matcher(ni.getBody());
            if(matcher.find())
            {
                noticeFound = true;
                break;
            }
            else
            {
                noticeIndex++;
            }

        }

        if(noticeFound)
            return notificationItemList.get(noticeIndex);
        else
            return null;

    }


    @Override
    public WebElement getComponentElement()
    {
        return _notificationPanel;
    }

    public Elements elements()
    {
        return new Elements();
    }

    public class Elements extends ComponentElements
    {
        @Override
        protected SearchContext getContext()
        {
            return getComponentElement();
        }

        private final WebElement  notificationArea = new LazyWebElement(Locator.css("div.labkey-notification-panel div.labkey-notification-area"), this);
        public final WebElement clearAll = new LazyWebElement(Locator.css("div.labkey-notification-panel div.labkey-notification-clear-all"), this);
        public final WebElement noNotifications = new LazyWebElement(Locator.css("div.labkey-notification-panel div.labkey-notification-none"), this);
        public final WebElement viewAll = new LazyWebElement(Locator.css("div.labkey-notification-panel div.labkey-notification-footer"), this);
    }

    public enum NotificationTypes
    {
        ISSUES("Issues", "notificationtype-Issues"),
        STUDY("Study", "notificationtype-Study");

        private final String textValue;
        private final String tagId;

        NotificationTypes(String value, String id)
        {
            textValue = value;
            tagId = id;
        }
    }

}
