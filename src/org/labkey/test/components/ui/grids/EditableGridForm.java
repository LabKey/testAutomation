package org.labkey.test.components.ui.grids;

import org.labkey.test.Locator;
import org.openqa.selenium.SearchContext;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;

/**
 * Wrapper for components that toggle edit mode
 * @param <T> Component after saving changes or cancelling
 */
public abstract class EditableGridForm<T> extends EditableGrid
{
    private final SearchContext _outerScope;

    protected EditableGridForm(SearchContext outerScope, WebDriver driver)
    {
        super(new EditableGridFinder(driver).findWhenNeeded(outerScope));
        _outerScope = outerScope;
    }

    public T saveChanges()
    {
        Locator.tagWithClass("button", "btn-success").findElement(_outerScope).click();
        getWrapper().shortWait().until(ExpectedConditions.stalenessOf(getComponentElement()));
        return getComponentAfterSave();
    }

    protected abstract T getComponentAfterSave();
}
