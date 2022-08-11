package org.labkey.test.components.ui.grids;

import org.junit.Assert;
import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.components.bootstrap.ModalDialog;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Dialog used to manage saved views.
 */
public class ManageGridViewsDialog extends ModalDialog
{
    QueryGrid grid;

    public ManageGridViewsDialog(WebDriver driver, QueryGrid grid)
    {
        super(new ModalDialogFinder(driver).withTitle("Manage Saved Views"));
        this.grid = grid;
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
    public ManageGridViewsDialog changeViewName(String currentName, String newName)
    {
        Assert.assertTrue(String.format("View named '%s' is not editable.", currentName), canViewNameBeChanged(currentName));

        Locator.tagWithClass("i", "fa-pencil").findElement(elementCache().viewRow(currentName)).click();

        WebDriverWrapper.waitFor(()->elementCache().viewNameInput.isDisplayed(),
                String.format("View name input for view '%s' did not show up in time.", currentName), 500);

        Actions replaceCurrentText = new Actions(getDriver());
        replaceCurrentText.sendKeys(Keys.END)
                .keyDown(Keys.SHIFT)
                .sendKeys(Keys.HOME)
                .keyUp(Keys.SHIFT)
                .sendKeys(newName)
                .sendKeys(Keys.TAB)
                .perform();

        WebDriverWrapper.waitFor(()->!elementCache().viewNameInput.isDisplayed() && getViewNames().contains(newName),
                String.format("New view name '%s' was not added to the list of views.", newName), 1_500);

        return this;
    }

    /**
     * Delete the given view.
     *
     * @param viewName The name of the view to delete.
     * @return This dialog.
     */
    public ManageGridViewsDialog deleteView(String viewName)
    {
        Assert.assertTrue(String.format("View named '%s' cannot be deleted.", viewName), canViewBeDeleted(viewName));

        Locator.tagWithClass("i", "fa-trash-o").findWhenNeeded(elementCache().viewRow(viewName)).click();

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
     */
    public ManageGridViewsDialog clickDeleteYesButton()
    {
        elementCache().deleteYesButton.click();
        return this;
    }

    /**
     * Click 'No' button to not delete a button.
     *
     * @return This dialog.
     */
    public ManageGridViewsDialog clickDeleteNoButton()
    {
        elementCache().deleteNoButton.click();
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

        protected final Locator viewRowLocator = Locator.tagWithClass("div", "row");

        protected List<WebElement> viewRows()
        {
            return viewRowLocator.findElements(this);
        }

        protected WebElement viewRow(String viewName)
        {
            return viewRowLocator.withText(viewName).findElement(this);
        }

        protected final WebElement viewNameInput = Locator.tagWithName("input", "gridViewName")
                .refindWhenNeeded(this);

        protected final WebElement deleteConfirmText = Locator.tagWithClass("span", "inline-confirmation__label")
                .refindWhenNeeded(this);

        protected final WebElement deleteYesButton = Locator.button("Yes").refindWhenNeeded(this);
        protected final WebElement deleteNoButton = Locator.button("No").refindWhenNeeded(this);
    }
}