package org.labkey.test.components.ui.grids;

import org.labkey.test.BootstrapLocators;
import org.labkey.test.Locator;
import org.labkey.test.components.bootstrap.ModalDialog;
import org.labkey.test.components.html.Checkbox;
import org.labkey.test.components.html.Input;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

/**
 * Dialog used to save a grid view in an app.
 */
public class SaveGridViewDialog extends ModalDialog
{
    QueryGrid grid;

    public SaveGridViewDialog(WebDriver driver, QueryGrid grid)
    {
        super(new ModalDialogFinder(driver).withTitle("Save Grid View"));
        this.grid = grid;
    }

    /**
     * Set the view name.
     *
     * @param viewName View name.
     * @return This dialog.
     */
    public SaveGridViewDialog setViewName(String viewName)
    {
        elementCache().viewNameInput.set(viewName);
        return this;
    }

    /**
     * Get the value of the View Name field.
     *
     * @return Value of View Name field.
     */
    public String getViewName()
    {
        return elementCache().viewNameInput.get();
    }

    /**
     * Check if the View Name field is enabled. It should be disabled if the 'Make default for all' checkbox is checked.
     *
     * @return True if enabled, false otherwise.
     */
    public boolean isViewNameEnabled()
    {
        return elementCache().viewNameInput.getComponentElement().isEnabled();
    }

    /**
     * Check or uncheck the 'Make default view' checkbox.
     *
     * @param checked True to check, false to uncheck.
     * @return This dialog.
     */
    public SaveGridViewDialog setMakeDefaultForAll(boolean checked)
    {
        elementCache().checkbox.set(checked);
        return this;
    }

    /**
     * Get the checked status of the 'Make default for all' checkbox.
     *
     * @return True if it is checked, false otherwise.
     */
    public boolean isMakeDefaultForAllChecked()
    {
        return elementCache().checkbox.isChecked();
    }

    /**
     * Save the view.
     */
    public void saveView()
    {
        dismiss("Save", 1);
    }

    /**
     * Click the 'Save' button but expect an error.
     *
     * @return The text of the error banner.
     */
    public String saveViewExpectingError()
    {
        dismiss("Save", 0);
        WebElement errorEl = BootstrapLocators.errorBanner.waitForElement(this, 5000);
        return errorEl.getText();
    }

    @Override
    protected ElementCache elementCache()
    {
        return (ElementCache) super.elementCache();
    }

    @Override
    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    protected class ElementCache extends ModalDialog.ElementCache
    {

        protected final Input viewNameInput = Input.Input(Locator.name("gridViewName"), getDriver())
                .findWhenNeeded(this);

        protected Checkbox checkbox = new Checkbox(Locator.input("setDefaultView").findWhenNeeded(this));

    }

}