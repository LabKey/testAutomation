/*
 * Copyright (c) 2016-2018 LabKey Corporation
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
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.util.Arrays;

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
        _markAsRead.click();
        waitFor(() -> ExpectedConditions.invisibilityOfAllElements(Arrays.asList(_el)).apply(null)
                && _panel.getNotificationCount() == initialCount - 1,
                "Notification did not go away when marked as read: " + _el.getAttribute("id"), 2000);
    }

    public void toggleExpand()
    {
        boolean initiallyExpanded = isExpanded();
        Locator.css(" div.labkey-notification-toggle").findElement(this).click();
        waitFor(() -> isExpanded() != initiallyExpanded,
                "Notification did not toggle expansion: " + _el.getAttribute("id"), 2000);
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
