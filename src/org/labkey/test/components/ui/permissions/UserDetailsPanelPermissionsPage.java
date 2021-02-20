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
        return Locator.css(".permissions-ul > li")
                .findElements(this)
                .stream().map(WebElement::getText).collect(Collectors.toList());
    }
}
