package org.labkey.test.components.ui.permissions;

import org.labkey.test.BootstrapLocators;
import org.labkey.test.Locator;
import org.labkey.test.components.Component;
import org.labkey.test.components.WebDriverComponent;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.List;

public class UserDetailsPanel extends WebDriverComponent<Component<?>.ElementCache>
{
    protected static final Locator LOC = BootstrapLocators.panel("User Details");

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
        return Locator.byClass("principal-title-primary")
                .findOptionalElement(this)
                .map(WebElement::getText).orElse(null);
    }

    public List<String> getMemberships()
    {
        var membersList = Locator.tagWithClass("div", "principal-detail-label").withText("Member of:")
                .followingSibling("ul").findElement(this);
        return getWrapper().getTexts(Locator.tag("li").findElements(membersList));
    }

}
