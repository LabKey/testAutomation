package org.labkey.test.components.ui.permissions;

import org.labkey.test.Locator;
import org.labkey.test.components.Component;
import org.labkey.test.components.WebDriverComponent;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.List;

public class UserDetailsPanel extends WebDriverComponent<Component<?>.ElementCache>
{
    protected static final Locator LOC = Locator.byClass("user-details-panel");

    private final WebElement _el;
    private final WebDriver _driver;

    protected UserDetailsPanel(WebElement element, WebDriver driver)
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

    public String getSelectedUser()
    {
        return Locator.byClass("panel-heading")
                .findOptionalElement(this)
                .map(WebElement::getText).orElse(null);
    }

    public List<String> getGroups()
    {
        var membersList = Locator.tagWithClass("div", "principal-detail-label").withText("Groups")
                .parent().descendant("ul").findElement(this);
        return getWrapper().getTexts(Locator.tag("li").findElements(membersList));
    }

}
