package org.labkey.test.components.core;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

public class NotificationPanelItem
{
    WebElement _parent;

    public NotificationPanelItem(WebElement rowItem)
    {
        _parent = rowItem;
    }

    public String getCreatedBy()
    {
        return _parent.findElement(By.cssSelector(" div.labkey-notification-createdby")).getText();
    }

    public String getBody()
    {
        return _parent.findElement(By.cssSelector(" div.labkey-notification-body")).getText();
    }

    public String getIconType()
    {
        String[] classValue;
        String iconName = "";
        classValue = _parent.findElement(By.cssSelector(" div.labkey-notification-icon")).getAttribute("class").split(" ");
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

    public void markAsRead()
    {
        _parent.findElement(By.cssSelector(" div.labkey-notification-times")).click();
    }

    public void toggleExpand()
    {
        _parent.findElement(By.cssSelector(" div.labkey-notification-toggle")).click();
    }

    public void click()
    {
        _parent.click();
    }

}
