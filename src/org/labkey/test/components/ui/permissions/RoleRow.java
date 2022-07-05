package org.labkey.test.components.ui.permissions;

import org.labkey.test.Locator;
import org.labkey.test.components.Component;
import org.labkey.test.components.WebDriverComponent;
import org.labkey.test.components.react.ReactSelect;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.util.List;
import java.util.stream.Collectors;

public class RoleRow extends WebDriverComponent<RoleRow.ElementCache>
{
    private static final Locator.XPathLocator MEMBER_LOC = Locator.byClass("permissions-member-li");

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
        return elementCache().findMembers().stream()
                .map(PermissionsMember::getName)
                .collect(Collectors.toList());
    }

    public RoleRow selectMember(String email)
    {
        expand();
        elementCache().findMember(email).select();
        return this;
    }

    public RoleRow removeMember(String email)
    {
        expand();
        elementCache().findMember(email).remove();
        return this;
    }

    public RoleRow addMember(String email)
    {
        expand();
        elementCache().memberSelect.select(email);
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
        final ReactSelect memberSelect = ReactSelect.finder(getDriver()).findWhenNeeded(this)
                .setOptionLocator(ReactSelect.Locators.option::startsWith);

        List<PermissionsMember> findMembers()
        {
            return MEMBER_LOC
                    .findElements(assignmentsRow).stream()
                    .map(PermissionsMember::new)
                    .collect(Collectors.toList());
        }

        PermissionsMember findMember(String email)
        {
            return new PermissionsMember(email);
        }
    }

    private class PermissionsMember extends Component
    {
        private final WebElement _el;
        private final WebElement _removeButton = Locator.byClass("btn")
                .withChild(Locator.byClass("fa-remove")).findWhenNeeded(this);
        private final WebElement _nameButton = Locator.byClass("permissions-button-display")
                .findWhenNeeded(this);

        public PermissionsMember(WebElement el)
        {
            _el = el;
        }

        private PermissionsMember(String email)
        {
            this(MEMBER_LOC.startsWith(email)
                    .waitForElement(RoleRow.this, 5000));
        }

        @Override
        public WebElement getComponentElement()
        {
            return _el;
        }

        public void remove()
        {
            _removeButton.click();
            getWrapper().shortWait().until(ExpectedConditions.stalenessOf(_el));
        }

        public void select()
        {
            _nameButton.click();
            getWrapper().shortWait().until(ExpectedConditions.attributeContains(_nameButton, "class", "primary"));
        }

        public String getName()
        {
            return _nameButton.getText();
        }
    }

    public static class RoleRowFinder extends WebDriverComponentFinder<RoleRow, RoleRow.RoleRowFinder>
    {
        private final Locator.XPathLocator _baseLocator = Locator.byClass("permissions-assignment-panel")
                .child(Locator.byClass("row"));
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
