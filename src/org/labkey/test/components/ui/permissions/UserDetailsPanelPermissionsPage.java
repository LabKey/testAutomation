package org.labkey.test.components.ui.permissions;

import org.labkey.test.Locator;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.List;
import java.util.stream.Collectors;

public class UserDetailsPanelPermissionsPage extends UserDetailsPanel
{
    protected UserDetailsPanelPermissionsPage(WebElement element, WebDriver driver)
    {
        super(element, driver);
    }

    public static SimpleWebDriverComponentFinder<UserDetailsPanelPermissionsPage> finder(WebDriver driver)
    {
        return new SimpleWebDriverComponentFinder<>(driver, LOC, UserDetailsPanelPermissionsPage::new);
    }

    public List<String> getEffectiveRoles()
    {
        var listContainer=  Locator.tagWithClass("div", "principal-detail-label").withText("Effective Roles")
                .parent().descendant("ul").waitForElement(this, 2000);
        return Locator.tag("li")
                .findElements(listContainer)
                .stream().map(WebElement::getText).collect(Collectors.toList());
    }
}
