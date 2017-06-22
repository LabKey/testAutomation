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

import org.labkey.test.Locator;
import org.labkey.test.components.Component;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

import static org.labkey.test.WebDriverWrapper.waitFor;

public class NotificationPanelItem extends Component
{
    WebElement _el;
    WebElement _createdBy = Locator.css("div.labkey-notification-createdby").findWhenNeeded(this);
    WebElement _notificationBody = Locator.css("div.labkey-notification-body").findWhenNeeded(this);
    WebElement _markAsRead = Locator.css("div.labkey-notification-times").findWhenNeeded(this);
    WebElement _icon = Locator.css("div.labkey-notification-icon").findWhenNeeded(this);

    public NotificationPanelItem(WebElement rowItem)
    {
        _el = rowItem;
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
        _markAsRead.click();
        waitFor(() -> ExpectedConditions.stalenessOf(_el).apply(null), 1000);
    }

    public void toggleExpand()
    {
        final WebElement notificationBody = Locator.css(".labkey-notification-body").findElement(this);
        Locator.css(" div.labkey-notification-toggle").findElement(this).click();
    }

    public void click()
    {
        _el.click();
    }
}
