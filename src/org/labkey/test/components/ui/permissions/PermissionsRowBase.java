package org.labkey.test.components.ui.permissions;

import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.components.WebDriverComponent;
import org.labkey.test.components.react.ReactSelect;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.List;
import java.util.stream.Collectors;


public abstract class PermissionsRowBase<T extends PermissionsRowBase<T>> extends WebDriverComponent<PermissionsRowBase<?>.ElementCache>
{
    private final WebElement _el;
    private final WebDriver _driver;

    protected PermissionsRowBase(WebElement element, WebDriver driver)
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

    protected abstract WebElement getAssignmentContainer();
    protected abstract T getThis();

    protected WebElement userListContainer()
    {
        return Locator.tagWithText("div","Users:").followingSibling("ul")
                .findElement(getAssignmentContainer());
    }

    protected WebElement groupListContainer()
    {
        return Locator.tagWithText("div","Groups:").followingSibling("ul")
                .findElement(getAssignmentContainer());
    }

    protected void expand()
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
        return findUsers().stream()
                .map(PermissionsMember::getName)
                .collect(Collectors.toList());
    }

    public T selectMember(String email)
    {
        expand();
        findUser(email).select();
        return getThis();
    }

    public T addMember(String email)
    {
        expand();
        elementCache().memberSelect.select(email);
        WebDriverWrapper.waitFor(()-> getMemberEmails().contains(email),
                "member was not added in time", 2000);
        return getThis();
    }

    public T removeMember(String email)
    {
        expand();
        findUser(email).remove();
        return getThis();
    }

    public List<String> getGroups()
    {
        expand();
        return findGroups().stream()
                .map(PermissionsMember::getName)
                .collect(Collectors.toList());
    }

    public T selectGroup(String name)
    {
        expand();
        findGroup(name).select();
        return getThis();
    }

    public T addGroup(String name)
    {
        expand();
        elementCache().memberSelect.select(name);
        WebDriverWrapper.waitFor(()-> getGroups().contains(name),
                "group was not added in time", 2000);
        return getThis();
    }

    public T removeGroup(String name)
    {
        expand();
        findGroup(name).remove();
        return getThis();
    }

    protected List<PermissionsMember> findUsers()
    {
        return new PermissionsMember.PermissionsMemberFinder(getDriver()).findAll(userListContainer());
    }

    protected PermissionsMember findUser(String email)
    {
        return new PermissionsMember.PermissionsMemberFinder(getDriver()).withTitle(email).waitFor(userListContainer());
    }

    protected List<PermissionsMember> findGroups()
    {
        return new PermissionsMember.PermissionsMemberFinder(getDriver()).findAll(groupListContainer());
    }

    protected PermissionsMember findGroup(String name)
    {
        return new PermissionsMember.PermissionsMemberFinder(getDriver()).withTitle(name).waitFor(groupListContainer());
    }


    @Override
    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    protected class ElementCache extends WebDriverComponent<?>.ElementCache
    {
        final WebElement rowContainer = Locator.byClass("container-expandable-grey").findWhenNeeded(this);

        final ReactSelect memberSelect = ReactSelect.finder(getDriver()).findWhenNeeded(this)
                .setOptionLocator(ReactSelect.Locators.option::endsWith);
    }
}
