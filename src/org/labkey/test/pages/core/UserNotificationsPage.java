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
package org.labkey.test.pages.core;

import org.jetbrains.annotations.Nullable;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.pages.LabKeyPage;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UserNotificationsPage extends LabKeyPage
{

    public UserNotificationsPage(WebDriver test)
    {
        super(test);
        waitForReady();
    }

    private void waitForReady()
    {
        waitForText(BaseWebDriverTest.WAIT_FOR_JAVASCRIPT, "User Notifications");
        waitForElement(Locators.allNotificationsPanel);
    }

    public List<String> getGroupHeaders()
    {
        List<String> headerText = new ArrayList<>();
        List<WebElement> headers = Locators.headerText.findElements(getDriver());
        for(WebElement we : headers)
        {
            headerText.add(we.getText());
        }

        return headerText;
    }

    public int getNotificationCount()
    {
        int count;

        count = Locators.notificationBody.findElements(getDriver()).size();

        return count;
    }

    public List<NotificationItem> getAllNotifications()
    {
        List<NotificationItem> notifications = new ArrayList<>();
        for(WebElement we : Locators.notificationBody.findElements(getDriver()))
        {
            notifications.add(new NotificationItem(we));
        }

        return notifications;
    }

    public NotificationItem getNotificationAtIndex(int idx)
    {
        return getAllNotifications().get(idx);
    }

    public List<NotificationItem> getNotificationsOfType(NotificationTypes notificationType)
    {
        List<WebElement> panels = new ArrayList<>();
        WebElement targetPanel = null;
        List<NotificationItem> notifications = new ArrayList<>();

        // Search for the panel that has the given title.
        panels = Locators.notificationGroupPanel.findElements(getDriver());
        for(WebElement we : panels)
        {
            if(we.findElement(By.cssSelector("span.x4-panel-header-text")).getText().toLowerCase().contains(notificationType.textValue.toLowerCase()))
            {
                targetPanel = we;
                break;
            }
        }

        // If found get all of the notification-body WebElements and use them to create new NotificationItems
        if(targetPanel != null)
        {
            for(WebElement msg : targetPanel.findElements(By.cssSelector("div.notification-body")))
            {
                notifications.add(new NotificationItem(msg));
            }
        }

        return notifications;
    }

    public NotificationItem findNotificationInPage(String searchBody, @Nullable NotificationTypes notificationType)
    {
        int noticeIndex;
        boolean noticeFound;
        List<NotificationItem> notificationItemList = new ArrayList<>();

        if(notificationType == null)
        {
            notificationItemList = getAllNotifications();
        }
        else
        {
            notificationItemList = getNotificationsOfType(notificationType);
        }

        Pattern pattern = Pattern.compile(searchBody);
        Matcher matcher;
        noticeIndex = 0;
        noticeFound = false;
        for(NotificationItem ni : notificationItemList)
        {
            matcher = pattern.matcher(ni.getBodyText());
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

    public void toggleNotificationPanel()
    {
        // Not really useful will only toggle the first panel. Not sure how useful this really is.
        click(Locators.headerToggle);
    }

    public static class Locators
    {
        public static final Locator.CssLocator allNotificationsPanel = Locator.css("div#view-all-notifications");
        public static final Locator.CssLocator notificationGroupPanel = allNotificationsPanel.append(" div.notification-group-panel");
        public static final Locator.CssLocator headerText = notificationGroupPanel.append(" span.x4-panel-header-text");
        public static final Locator.CssLocator headerToggle = notificationGroupPanel.append(" div.x4-tool-after-title");
        public static final Locator.CssLocator panelBody = notificationGroupPanel.append(" div.x4-panel-body");
        public static final Locator.CssLocator notificationBody = panelBody.append(" div.notification-body");
        public static final Locator.CssLocator markAllAsRead = allNotificationsPanel.append(" a.notification-all-read");
        public static final Locator.CssLocator deleteAll = allNotificationsPanel.append(" a.notification-all-delete");
    }

    public enum NotificationTypes
    {
        ISSUES("Issues"),
        STUDY("Study");

        private final String textValue;

        NotificationTypes(String value)
        {
            textValue = value;
        }
    }

    public class NotificationItem
    {
        WebElement _parent;

        public NotificationItem(WebElement rowItem)
        {
            _parent = rowItem;
        }

        public String getHeaderText()
        {
            return _parent.findElement(By.cssSelector("div.notification-header")).getText();
        }

        public String getBodyText()
        {
            return _parent.findElement(By.cssSelector("div.notification-content div")).getText();
        }

        public boolean isRead()
        {
            String[] classValue;
            boolean isRead = true;

            classValue = _parent.findElement(By.cssSelector("div.notification-header")).getAttribute("class").split(" ");
            for(String str : classValue)
            {
                if(str.contains("notification-header-unread"))
                {
                    isRead = false;
                    break;
                }
            }

            return isRead;
        }

        public String getReadOnText()
        {
            return _parent.findElement(By.cssSelector("span.notification-readon")).getText();
        }

        public String getIconName()
        {
            String[] classValue;
            String iconName = "";

            classValue = _parent.findElement(By.cssSelector("div.notification-header span")).getAttribute("class").split(" ");
            for(String str : classValue)
            {
                if(str.contains("fa-"))
                {
                    iconName = str;
                    break;
                }
            }

            return iconName;
        }

        public void clickMarkAsRead()
        {
            _parent.findElement(By.cssSelector("a.notification-mark-as-read")).click();
        }

        public void clickView()
        {
            _parent.findElements(By.cssSelector("span.notification-link")).get(1).click();
            try
            {
                waitForElementToDisappear(Locator.css("div#" + _parent.getAttribute("id")));
            }
            catch(org.openqa.selenium.StaleElementReferenceException sere)
            {
                log("Caught a stale element exception in clickView, this can happen if the page has already changed. So we are good to go.");
            }
        }

        public void clickDelete()
        {
            _parent.findElements(By.cssSelector("span.notification-link")).get(0).click();
            try
            {
                waitForElementToDisappear(Locator.css("div#" + _parent.getAttribute("id")));
            }
            catch(org.openqa.selenium.StaleElementReferenceException sere)
            {
                log("Caught a stale element exception in clickDismiss, this can happen if the page has already changed. So we are good to go.");
            }
        }

    }

}
