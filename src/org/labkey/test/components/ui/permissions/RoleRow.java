package org.labkey.test.components.ui.permissions;

import org.labkey.test.Locator;
import org.labkey.test.components.WebDriverComponent;
import org.labkey.test.components.react.ReactSelect;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.List;
import java.util.stream.Collectors;

public class RoleRow extends WebDriverComponent<RoleRow.ElementCache>
{
    private static final Locator.XPathLocator MEMBER_LOC = Locator.byClass("permissions-groups-member-li");

    private final WebElement _el;
    private final WebDriver _driver;

    protected RoleRow(WebElement element, WebDriver driver)
    {
        _el = element;
        _driver = driver;
    }

    private void expand()
    {
        if (!isExpanded())
        {
            elementCache().rowContainer.click();
        }
    }

    private boolean isExpanded()
    {
        return elementCache().rowContainer.getAttribute("class").contains("container-expandable-child__inactive");
    }

    public String getTitle()
    {
        return Locator.byClass("permissions-title").findElement(this).getText();
    }

    public List<String> getMemberEmails()
    {
        expand();
        return elementCache().findUsers().stream()
                .map(PermissionsMember::getName)
                .collect(Collectors.toList());
    }

    public RoleRow selectMember(String email)
    {
        expand();
        elementCache().findUser(email).select();
        return this;
    }

    public RoleRow removeMember(String email)
    {
        expand();
        elementCache().findUser(email).remove();
        return this;
    }

    public RoleRow addMember(String email)
    {
        expand();
        elementCache().memberSelect.select(email);
        return this;
    }

    public List<String> getMemberGroups()
    {
        expand();
        return elementCache().findGroups().stream()
                .map(PermissionsMember::getName)
                .collect(Collectors.toList());
    }

    public RoleRow selectMemberGroups(String name)
    {
        expand();
        elementCache().findGroup(name).select();
        return this;
    }

    public RoleRow removeMemberGroup(String name)
    {
        expand();
        elementCache().findGroup(name).remove();
        return this;
    }

    public RoleRow addMemberGroup(String name)
    {
        expand();
        elementCache().memberSelect.select(name);
        return this;
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

    @Override
    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    protected class ElementCache extends WebDriverComponent<RoleRow.ElementCache>.ElementCache
    {
        final WebElement rowContainer = Locator.byClass("container-expandable-grey").findWhenNeeded(this);
        final WebElement assignmentsRow = Locator.byClass("permissions-assignments-row").findWhenNeeded(this);
        final WebElement usersList = Locator.tagWithText("div","Users:").followingSibling("ul")
                .findElement(assignmentsRow);
        final WebElement groupsList = Locator.tagWithText("div","Groups:").followingSibling("ul")
                .findElement(assignmentsRow);

        final ReactSelect memberSelect = ReactSelect.finder(getDriver()).findWhenNeeded(this)
                .setOptionLocator(ReactSelect.Locators.option::startsWith);

        List<PermissionsMember> findUsers()
        {
            return new PermissionsMember.PermissionsMemberFinder(getDriver()).findAll(usersList);
        }

        PermissionsMember findUser(String email)
        {
            return new PermissionsMember.PermissionsMemberFinder(getDriver()).withTitle(email).waitFor(usersList);
        }

        List<PermissionsMember> findGroups()
        {
            return new PermissionsMember.PermissionsMemberFinder(getDriver()).findAll(groupsList);
        }

        PermissionsMember findGroup(String name)
        {
            return new PermissionsMember.PermissionsMemberFinder(getDriver()).withTitle(name).waitFor(groupsList);
        }
    }

    public static class RoleRowFinder extends WebDriverComponentFinder<RoleRow, RoleRow.RoleRowFinder>
    {
        private final Locator.XPathLocator _baseLocator = Locator.tagWithClass("div", "container-expandable")
                .withChild(Locator.tagWithClass("div", "container-expandable-grey"));
        private String _roleTitle = null;

        public RoleRowFinder(WebDriver driver)
        {
            super(driver);
        }

        public RoleRow.RoleRowFinder forRole(String roleTitle)
        {
            _roleTitle = roleTitle;
            return this;
        }

        @Override
        protected RoleRow construct(WebElement el, WebDriver driver)
        {
            return new RoleRow(el, driver);
        }

        @Override
        protected Locator locator()
        {
            if (_roleTitle != null)
                return _baseLocator.withDescendant(Locator.byClass("permissions-title").withText(_roleTitle));
            else
                return _baseLocator;
        }
    }
}
