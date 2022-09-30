package org.labkey.test.components.ui.permissions;

import org.labkey.test.Locator;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

public class GroupRow extends PermissionsRowBase<GroupRow>
{
    protected GroupRow(WebElement el, WebDriver driver)
    {
        super(el, driver);
    }

    @Override
    protected WebElement getAssignmentContainer()
    {
        return Locator.tagWithClass("div", "expandable-container__member-buttons")
                .waitForElement(getComponentElement(), 2000);
    }

    @Override
    protected GroupRow getThis()
    {
        return this;
    }

    protected boolean isDeleteEmptyButtonDisabled()
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
        return (ElementCache) super.elementCache();
    }

    protected class ElementCache extends PermissionsRowBase<GroupRow>.ElementCache
    {
        final WebElement actionContainer = Locator.tagWithClass("div", "expandable-container__action-container")
                .findWhenNeeded(this);

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
