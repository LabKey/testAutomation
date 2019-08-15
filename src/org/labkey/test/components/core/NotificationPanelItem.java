/*
 * Copyright (c) 2016-2019 LabKey Corporation
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

import org.labkey.test.Locator;
import org.labkey.test.components.Component;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

import static org.junit.Assert.fail;
import static org.labkey.test.WebDriverWrapper.waitFor;

public class NotificationPanelItem extends Component
{
    private final UserNotificationsPanel _panel;
    private final WebElement _el;
    private final WebElement _createdBy = Locator.css("div.labkey-notification-createdby").findWhenNeeded(this);
    private final WebElement _notificationBody = Locator.css("div.labkey-notification-body").findWhenNeeded(this);
    private final WebElement _markAsRead = Locator.css("div.labkey-notification-times").findWhenNeeded(this);
    private final WebElement _icon = Locator.css("div.labkey-notification-icon").findWhenNeeded(this);

    public NotificationPanelItem(WebElement rowItem, UserNotificationsPanel panel)
    {
        _el = rowItem;
        _panel = panel;
    }

    @Override
    public WebElement getComponentElement()
    {
        return _el;
    }

    public String getCreatedBy()
    {
        return _createdBy.getText();
    }

    public String getBody()
    {
        return _notificationBody.getText();
    }

    public String getIconType()
    {
        String[] classValue;
        String iconName = "";
        classValue = _icon.getAttribute("class").split(" ");
        for(String str : classValue)
        {
            if(str.startsWith("fa-"))
            {
                iconName = str;
                break;
            }
        }

        return iconName;
    }

    public void markAsRead()
    {
        final int initialCount = _panel.getNotificationCount();
        final String notificationId = _el.getAttribute("id");
        _markAsRead.click();
        try
        {
            waitFor(() -> ExpectedConditions.invisibilityOf(_el).apply(null)
                && _panel.getNotificationCount() == initialCount - 1,
                "Notification did not go away when marked as read: " + notificationId, 2000);

            if (!_panel.isNotificationPanelVisible())
                fail("Notification panel closed unexectedly when marking a notificaiton as read");
        }
        catch (StaleElementReferenceException stale)
        {
            fail("Unexpected navigation when marking a notificaiton as read");
        }
    }

    public void toggleExpand()
    {
        boolean initiallyExpanded = isExpanded();
        final String notificationId = _el.getAttribute("id");
        Locator.css(" div.labkey-notification-toggle").findElement(this).click();
        try
        {
            waitFor(() -> isExpanded() != initiallyExpanded,
                    "Notification did not toggle expansion: " + notificationId, 2000);

            if (!_panel.isNotificationPanelVisible())
                fail("Notification panel closed unexectedly when toggling a notification");
        }
        catch (StaleElementReferenceException stale)
        {
            fail("Unexpected navigation when  toggling a notification");
        }
    }

    private boolean isExpanded()
    {
        return _notificationBody.getAttribute("class").contains("expand");
    }

    public void click()
    {
        _el.click();
    }
}
