package org.labkey.test.components.ui.permissions;

import org.labkey.test.BootstrapLocators;
import org.labkey.test.Locator;
import org.labkey.test.components.Component;
import org.labkey.test.components.WebDriverComponent;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

public class GroupDetailsPanel extends WebDriverComponent<GroupDetailsPanel.ElementCache>
{
    protected static final Locator LOC = Locator.byClass("group-details-panel");
    private final WebElement _el;
    private final WebDriver _driver;

    protected GroupDetailsPanel(WebElement element, WebDriver driver)
    {
        _el = element;
        _driver = driver;
    }

    @Override
    public WebElement getComponentElement()
    {
        return _el;
    }

    @Override
    public WebDriver getDriver()
    {
        return _driver;
    }

    public String getGroupName()
    {
        return elementCache().title.getText();
    }

    public String getUserCount()
    {
        return elementCache().detailValueEl("User Count").getText();
    }

    public String getGroupCount()
    {
        return elementCache().detailValueEl("Group Count").getText();
    }


    @Override
    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }


    protected class ElementCache extends Component<?>.ElementCache
    {
        final WebElement title = Locator.tagWithClass("p", "panel-heading").findWhenNeeded(this);

        WebElement detailValueEl(String label)
        {
            return Locator.tagWithClass("div", "principal-detail-label").startsWith(label)
                    .followingSibling("div").findElement(this);
        }
    }


}
