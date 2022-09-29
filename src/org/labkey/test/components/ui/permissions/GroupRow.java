package org.labkey.test.components.ui.permissions;

import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.components.WebDriverComponent;
import org.labkey.test.components.react.ReactSelect;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.util.List;
import java.util.stream.Collectors;

public class GroupRow extends WebDriverComponent<GroupRow.ElementCache>
{
    private final WebElement _el;
    private final WebDriver _driver;

    protected GroupRow(WebElement el, WebDriver driver)
    {
        _el = el;
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

    public GroupRow addUser(String email)
    {
        expand();
        elementCache().memberSelect.select(email);
        WebDriverWrapper.waitFor(()-> getMemberEmails().contains(email),
                "user was not added in time", 2000);
        return this;
    }

    public GroupRow removeUser(String email)
    {
        expand();
        elementCache().findUser(email).remove();
        return this;
    }

    public GroupRow addGroup(String groupName)
    {
        expand();
        elementCache().memberSelect.select(groupName);
        WebDriverWrapper.waitFor(()-> getGroups().contains(groupName),
                "group was not added in time", 2000);
        return this;
    }

    public GroupRow removeGroup(String groupName)
    {
        expand();
        elementCache().findGroup(groupName).remove();
        return this;
    }

    public List<String> getMemberEmails()
    {
        expand();
        return elementCache().findUsers().stream()
                .map(PermissionsMember::getName)
                .collect(Collectors.toList());
    }

    public List<String> getGroups()
    {
        expand();
        return elementCache().findGroups().stream()
                .map(PermissionsMember::getName)
                .collect(Collectors.toList());
    }

    public GroupRow selectUser(String email)
    {
        expand();
        elementCache().findUser(email).select();
        return this;
    }

    public GroupRow selectGroup(String name)
    {
        expand();
        elementCache().findGroup(name).select();
        return this;
    }



    public boolean isDeleteEmptyButtonDisabled()
    {
        return elementCache().deleteEmptyGroupBtn().getAttribute("class")
                .contains("disabled-button-with-tooltip");
    }

    public void clickDeleteEmptyGroup()
    {
        expand();
        var btn = elementCache().deleteEmptyGroupBtn();
        btn.click();
        getWrapper().shortWait().until(ExpectedConditions.stalenessOf(btn));
    }

    @Override
    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    @Override
    protected ElementCache elementCache()
    {
        return super.elementCache();
    }

    protected class ElementCache extends WebDriverComponent<RoleRow.ElementCache>.ElementCache
    {
        final WebElement rowContainer = Locator.byClass("container-expandable-grey").findWhenNeeded(this);
        final WebElement memberBtnsRow = Locator.tagWithClass("div", "expandable-container__member-buttons")
                .findWhenNeeded(this);
        final WebElement usersList = Locator.tagWithText("div","Users:").followingSibling("ul")
                .findWhenNeeded(memberBtnsRow).withTimeout(2000);
        final WebElement groupsList = Locator.tagWithText("div","Groups:").followingSibling("ul")
                .findWhenNeeded(memberBtnsRow).withTimeout(2000);
        final WebElement actionContainer = Locator.tagWithClass("div", "expandable-container__action-container")
                .findWhenNeeded(this);
        final ReactSelect memberSelect = ReactSelect.finder(getDriver()).timeout(2000).findWhenNeeded(actionContainer)
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

        final WebElement deleteEmptyGroupBtn()
        {
            return Locator.tagWithText("button", "Delete Empty Group")
                    .findElement(actionContainer);
        }
    }

    public static class GroupRowFinder extends WebDriverComponentFinder<GroupRow, GroupRow.GroupRowFinder>
    {
        private final Locator.XPathLocator _baseLocator = Locator.tagWithClass("div", "container-expandable")
                .withChild(Locator.tagWithClass("div", "container-expandable-grey"));
        private String _groupName = null;

        public GroupRowFinder(WebDriver driver)
        {
            super(driver);
        }

        public GroupRowFinder forGroup(String groupName)
        {
            _groupName = groupName;
            return this;
        }

        @Override
        protected GroupRow construct(WebElement el, WebDriver driver)
        {
            return new GroupRow(el, driver);
        }

        @Override
        protected Locator locator()
        {
            if (_groupName != null)
                return _baseLocator.withDescendant(Locator.byClass("permissions-title").withText(_groupName));
            else
                return _baseLocator;
        }
    }

}
