package org.labkey.test.components.ui.grids;

import org.junit.Assert;
import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.components.bootstrap.ModalDialog;
import org.openqa.selenium.Keys;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Dialog used to manage saved views. Wraps ManageViewsModal.tsx in UI components.
 */
public class ManageViewsDialog extends ModalDialog
{

    public ManageViewsDialog(WebDriver driver)
    {
        super(new ModalDialogFinder(driver).withTitle("Manage Saved Views"));
    }

    /**
     * Get the list of views.
     *
     * @return List of views.
     */
    public List<String> getViewNames()
    {
        return elementCache().viewRows().stream().map(WebElement::getText).collect(Collectors.toList());
    }

    /**
     * Check to see if a given view name can be changed.
     *
     * @param viewName Name of the view.
     * @return True if name can be edited, false otherwise.
     */
    public boolean canViewNameBeChanged(String viewName)
    {
        return Locator.tagWithClass("i", "fa-pencil").findWhenNeeded(elementCache().viewRow(viewName)).isDisplayed();
    }

    /**
     * Check to see if a given view can be deleted.
     *
     * @param viewName Name of the view.
     * @return True if the view can be deleted, false otherwise.
     */
    public boolean canViewBeDeleted(String viewName)
    {
        return Locator.tagWithClass("i", "fa-trash-o").findWhenNeeded(elementCache().viewRow(viewName)).isDisplayed();
    }

    /**
     * Change the name of a view.
     *
     * @param currentName The current name of the view.
     * @param newName The new name.
     * @return This dialog.
     */
    public ManageViewsDialog changeViewName(String currentName, String newName)
    {
        Assert.assertTrue(String.format("View named '%s' is not editable.", currentName), canViewNameBeChanged(currentName));

        Locator.tagWithClass("i", "fa-pencil").findElement(elementCache().viewRow(currentName)).click();

        WebDriverWrapper.waitFor(()->elementCache().viewNameInput.isDisplayed(),
                String.format("View name input for view '%s' did not show up in time.", currentName), 500);

        // Clear the current name.
        getWrapper().actionClear(elementCache().viewNameInput);

        Actions replaceCurrentText = new Actions(getDriver());
        replaceCurrentText
                .sendKeys(newName)
                .sendKeys(Keys.TAB)
                .perform();

        WebDriverWrapper.waitFor(()->{
            try {
                return !elementCache().viewNameInput.isDisplayed() && getViewNames().contains(newName);
            }
            catch (StaleElementReferenceException | NoSuchElementException exp)
            {
                // Sometimes there is a race condition with the elementCache().viewRows refreshing after a view has been renamed.
                return false;
            }
        },
                String.format("New view name '%s' was not added to the list of views.", newName), 1_500);

        return this;
    }

    /**
     * Delete the given view, wait for confirm.
     *
     * @param viewName The name of the view to delete.
     * @return This dialog.
     */
    public ManageViewsDialog deleteView(String viewName)
    {
        Assert.assertTrue(String.format("View named '%s' cannot be deleted.", viewName), canViewBeDeleted(viewName));

        Locator.tagWithClass("i", "fa-trash-o").findElement(elementCache().viewRow(viewName)).click();

        return this;
    }

    /**
     * Delete the view and confirm (click 'Yes' button).
     *
     * @param viewName Name pf view to delete.
     * @return This dialog.
     */
    public ManageViewsDialog deleteViewAndConfirm(String viewName)
    {
        deleteView(viewName)
                .confirmDelete();
        return this;
    }

    /**
     * Get the confirmation text asking if you really want to delete this view.
     *
     * @return The confirmation text.
     */
    public String getDeleteConfirmationText()
    {
        return elementCache().deleteConfirmText.getText();
    }

    /**
     * Click the 'Yes' button when asked to delete a button.
     *
     * @return This dialog.
     *
     */
    public ManageViewsDialog confirmDelete()
    {
        elementCache().deleteYesButton.click();
        return this;
    }

    /**
     * Click 'No' button to not delete a button.
     *
     * @return This dialog.
     */
    public ManageViewsDialog cancelDelete()
    {
        elementCache().deleteNoButton.click();
        return this;
    }

    /**
     * Click the 'Revert' text for the default view. If default view has not been changed this will have no affect.
     * Note: This is only available in the app, not from the core-components.view page.
     *
     * @return This dialog.
     */
    public ManageViewsDialog revertDefaultView()
    {
        WebElement tag = elementCache().revertDefault;

        if(tag.isDisplayed())
        {
            elementCache().revertDefault.click();
        }
        return this;
    }

    /**
     * Check if the 'Revert' text for the default view has the 'clickable-text' class.
     * Note: This is only available in the app, not from the core-components.view page.
     *
     * @return True if 'Revert' is clickable, false otherwise.
     */
    public boolean canDefaultBeReverted()
    {
        WebElement tag = elementCache().revertDefault;

        if(tag.isDisplayed())
        {
            return tag.getAttribute("class").equalsIgnoreCase("clickable-text");
        }

        return false;

    }

    /**
     * Make the selected view the default view for the grid.
     * Note: This is only available in the app, not from the core-components.view page.
     *
     * @param viewName The name of the view to make default.
     * @return This dialog.
     */
    public ManageViewsDialog makeViewDefault(String viewName)
    {
        Locator.tagWithText("span", "Make default").findElement(elementCache().viewRow(viewName)).click();

        return this;
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

        protected List<WebElement> viewRows()
        {
            return Locator.tagWithClass("div", "row").childTag("div").withClass("col-xs-8").findElements(this);
        }

        protected WebElement viewRow(String viewName)
        {
            return Locator.tagWithText("div", viewName).parent("div").withClass("row").findElement(this);
        }

        protected final WebElement viewNameInput = Locator.tagWithName("input", "gridViewName")
                .refindWhenNeeded(this);

        protected final WebElement deleteConfirmText = Locator.tagWithClass("span", "inline-confirmation__label")
                .refindWhenNeeded(this);

        protected final WebElement deleteYesButton = Locator.button("Yes").refindWhenNeeded(this);
        protected final WebElement deleteNoButton = Locator.button("No").refindWhenNeeded(this);

        protected final WebElement revertDefault = Locator.tagWithText("span", "Revert").refindWhenNeeded(this);
    }
}
