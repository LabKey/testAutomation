package org.labkey.test.components.ui.entities;

import org.junit.Assert;
import org.labkey.test.BootstrapLocators;
import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.components.Component;
import org.labkey.test.components.WebDriverComponent;
import org.labkey.test.components.react.FilteringReactSelect;
import org.labkey.test.components.react.ReactSelect;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.Arrays;
import java.util.List;

/**
 * <p>
 * This is a base class for the edit lineage panels that are shown on the sample overview page. Samples can have two
 * "types" of lineage, another sample or a source. The panel is the same for both. It has two select controls on it. The
 * first selects the type (i.e. the name of the Sample Type) and the second selects the specific parent (i.e. the sample).
 * </p>
 * @see <a href="https://github.com/LabKey/labkey-ui-components/blob/master/packages/components/src/components/entities/ParentEntityEditPanel.tsx">ParentEntityEditPanel.tsx</a>
 */
public class ParentEntityEditPanel extends WebDriverComponent<ParentEntityEditPanel.ElementCache>
{
    private final WebDriver driver;
    private final WebElement editingDiv;

    /**
     * Constructor for the panel.
     *
     * @param element The WebElement containing the panel.
     * @param driver A reference to the WebDriver
     */
    public ParentEntityEditPanel(WebElement element, WebDriver driver)
    {
        this.driver = driver;
        editingDiv = element;
    }

    @Override
    public WebElement getComponentElement()
    {
        return editingDiv;
    }

    @Override
    protected WebDriver getDriver()
    {
        return driver;
    }

    @Override
    protected void waitForReady()
    {

        // The panel is ready if:
        // 1. There are no spinners.
        // 2. If there are parents (i.e. FilterReactSelect controls present) they should all be interactive (not in the process of loading).
        // 3. If there are no parents there should be only one entity type select (ReactSelect) and it should be interactive and have a list of options.
        WebDriverWrapper.waitFor(()->
        {
            if(BootstrapLocators.loadingSpinner.findWhenNeeded(this).isDisplayed())
            {
                return false;
            }
            else
            {

                try
                {

                    // If there are existing parents then all the FilteringReactSelect controls (i.e. the parents) should
                    // be interactive. When this panel is in a dialog waitForReady can be called after the parent entity type is selected.
                    // In that scenario the parent selector will be empty, but it should be interactive.
                    List<FilteringReactSelect> filteringReactSelects = FilteringReactSelect.finder(getDriver()).findAll(this);

                    if(filteringReactSelects.size() > 1)
                    {
                        for (FilteringReactSelect select : filteringReactSelects)
                        {
                            if (!select.isInteractive())
                            {
                                // There is a parent selector but it is not interactive. Not ready.
                                return false;
                            }
                        }

                        // All the parent selectors are interactive, panel is ready.
                        return true;
                    }
                    else
                    {
                        // If there are no existing parents check to see if this is the 'first' parent.
                        ReactSelect firstParentSelector = ReactSelect.finder(getDriver())
                                .withNamedInput("entityType0")
                                .find(this);

                        // For the first parent the panel is ready if the entity type selector is ready and the options have been populated.
                        return firstParentSelector.isInteractive() && !firstParentSelector.getOptions().isEmpty();

                    }
                }
                catch (NoSuchElementException | StaleElementReferenceException exception)
                {
                    return false;
                }

            }
        }, "The ParentEntityEdit panel did not become active in timely fashion.", 5_000);
    }

    private void clickButtonWaitForPanel(WebElement button)
    {
        clickButtonWaitForPanel(button, 2_500);
    }

    /**
     * Clicking either the 'Save' or 'Cancel' button will remove this edit panel. This helper function will click the
     * button sent to it and then wait until it has some indication the edit panel is gone and the default panel is
     * shown.
     *
     * @param button Button to click.
     * @param wait How long to wait for the panel.
     */
    private void clickButtonWaitForPanel(WebElement button, int wait)
    {
        // The count of panels not in edit mode.
        Locator defaultPanel = Locator.tagWithClass("div", "panel-default");
        int defaultCount = defaultPanel.findElements(getDriver()).size();

        // The count of panels in edit mode.
        Locator infoPanel = Locator.tagWithClass("div", "panel-info");
        int infoCount = infoPanel.findElements(getDriver()).size();

        Assert.assertTrue("Whoa, there appears to be more than one panel in edit mode. This should never happen.",
                infoCount <= 1);

        // A reference to the editing header title
        Locator editingLocator = Locator.tagWithClass("div", "panel-heading").startsWith("Editing");

        Assert.assertEquals("Cannot find a panel with 'Editing' in the header. There isn't a panel in edit mode.",
                1, editingLocator.findElements(getDriver()).size());

        // Shouldn't need to do this, but when tests fail, because the panel did not exit edit mode, the button is not in view.
        getWrapper().scrollIntoView(button);

        // Some tests appear to fail because the button is not enabled even after changes have been made to the form.
        // This may be a timing issue.
        WebDriverWrapper.waitFor(button::isEnabled, String.format("Button with text '%s' is not enabled.", button.getText()), 1_500);

        button.click();

        // Wait until the counts of panels not in edit mode increases and the editor heading is no longer visible.
        WebDriverWrapper.waitFor(()->
                        defaultPanel.findElements(getDriver()).size() > defaultCount &&
                                editingLocator.findElements(getDriver()).isEmpty(),
                "Panel did not change state.", wait);
    }

    /** Click the 'Cancel' button. This will make the edit panel go away. */
    public void clickCancel()
    {
        clickButtonWaitForPanel(elementCache().cancelButton);
    }

    /** Click the 'Save' button. */
    public void clickSave()
    {
        clickSave(5_000);
    }

    /** Click the 'Save' button.
     *
     * @param waitTime Amount of time to wait for the panel.
     */
    public void clickSave(int waitTime)
    {
        // Making changes, like to lineage, may cause a slight delay before the save button is enabled.
        WebDriverWrapper.waitFor(()->elementCache().saveButton.isEnabled(),
                "Save button is not enabled.", 2_500);

        // The wait time is used here to validate the panel exits edit mode.
        clickButtonWaitForPanel(elementCache().saveButton, waitTime);

        // After the panel exits edit mode the page might still be updating, wait for that to happen.
        WebElement progressbar = Locator.tagWithClass("div", "progress-bar").findWhenNeeded(getDriver());
        WebDriverWrapper.waitFor(()->!progressbar.isDisplayed(),
                "It looks like an update took too long.", waitTime);

    }

    /**
     * Expect and error after clicking 'Save' button.
     *
     * @return The error message shown in error banner.
     */
    public String clickSaveExpectingError()
    {
        elementCache().saveButton.click();
        return BootstrapLocators.errorBanner.waitForElement(this, 1_000).getText();
    }

    /**
     * Is the 'Save' button enabled.
     *
     * @return True if it is, false otherwise.
     */
    public boolean isSaveButtonEnabled()
    {
        return elementCache().saveButton.isEnabled();
    }

    /**
     * Check to see if the 'Add' button is enabled. Once all types have been added the button should not be enabled.
     *
     * @return True if enabled, false otherwise.
     */
    public boolean isAddButtonEnabled()
    {
        // If element is enabled the class attribute not contain the word 'disabled'.
        return !elementCache().addButton.getAttribute("class").toLowerCase().contains("disabled");
    }

    /**
     * Get the select control that can be used to add a new parent entity type (sources or sample types). If there isn't
     * a 'Select a type' select currently present the 'Add' button will be clicked to create one.
     *
     * Useful for test that validate parent entity types are available. Like for sub-folder testing.
     *
     * @return A reference to a select control that can be used to add a new parent type.
     */
    public ReactSelect getAddNewEntityTypeSelect()
    {
        var numOfTypes = getAllEntityTypes().size();
        var selectType = getEntityTypeByPosition(numOfTypes - 1);

        // If the last select in the list contains no selection, it can be used to add a new parent entity type.
        // If the last select does have items selected then the addButton must be clicked to create a new type select control.
        if (selectType.hasSelection())
        {
            // Since there is already a parent need to click the "Add" button to add a new one.
            elementCache().addButton.click();

            // Now need to go find the new select but wait until it shows up.
            WebDriverWrapper.waitFor(()-> getAllEntityTypes().size() > numOfTypes,
                    "The new type field did not show up in a timely fashion.",
                    1_000);

            selectType = getEntityTypeByPosition(numOfTypes);
        }

        return selectType;
    }

    /**
     * Get the type select by its ordinal position.
     *
     * @return A select at this given ordinal position.
     */
    private ReactSelect getEntityTypeByPosition(int index)
    {
        return ReactSelect.finder(getDriver())
                .withNamedInput(String.format("entityType%d", index))
                .find(this);
    }

    public ReactSelect getEntityType(String entityName)
    {
        Locator input = Locator.tagWithAttribute("input", "value", entityName.toLowerCase());
        if(getWrapper().isElementPresent(input))
        {
            String inputName = input.findElement(this).getAttribute("name");
            return new ReactSelect.ReactSelectFinder(getDriver()).withNamedInput(inputName).find(this);
        }
        else
        {
            return null;
        }
    }

    /**
     * Return a list of entity types that are being used for parents.
     *
     * @return List of entity type names currently being used.
     */
    public List<String> getEntityTypeNames()
    {
        List<WebElement> labels = Locator.tagWithClass("label", "entity-insert--type-select").findElements(this);
        return labels.stream().map(WebElement::getText).toList();
    }

    /**
     * Get all the type select controls in the panel. This includes those already set and the 'add new' control if present.
     *
     * @return A collection of all the ReactSelect controls that select the parent type.
     */
    public List<ReactSelect> getAllEntityTypes()
    {
        return ReactSelect.finder(getDriver())
                .followingLabelWithClass("entity-insert--type-select")
                .findAll(this);
    }

    /**
     * Get a select for a given parent entity type.
     *
     * @param typeName Type name.
     * @return A {@link FilteringReactSelect}
     */
    public FilteringReactSelect getParent(String typeName)
    {
        return FilteringReactSelect.finder(getDriver()).withNamedInput(String.format("parentEntityValue_%s", typeName))
                .find(this);
    }

    /**
     * Get all the parent react select controls in the panel. This includes those already set and the 'add new' control as well.
     *
     * @return A collection of all the ReactSelect controls that select the sample or source parent.
     */
    public List<FilteringReactSelect> getAllParents()
    {
        return FilteringReactSelect.finder(getDriver())
                .followingLabelWithClass("entity-insert--parent-select")
                .findAll(this);
    }

    /**
     * Add a parent from the given entity type. This will add the entity type if it is not already present.
     *
     * @param typeName Entity type name.
     * @param parentId Id of the parent sample/source.
     * @return This panel.
     */
    public ParentEntityEditPanel addParent(String typeName, String parentId)
    {
        return addParents(typeName, Arrays.asList(parentId));
    }

    /**
     * Add a specific parents (samples or sources) from the given type. If the type is not currently being used for
     * parent elements it will be added.
     *
     * @param typeName The name of the type. For example if you have a Source Type named 'Sources01' the value of
     *                 this parameter would be "Sources01".
     * @param parentIds A list of the individuals samples or sources to add.
     * @return A reference to this panel.
     */
    public ParentEntityEditPanel addParents(String typeName, List<String> parentIds)
    {
        if(getEntityType(typeName) == null)
            getAddNewEntityTypeSelect().select(typeName);

        var selectParent = FilteringReactSelect.finder(getDriver())
                .withNamedInput(String.format("parentEntityValue_%s", typeName))
                .waitFor(this);

        // Adding for debugging (trying to understand why save button is not enabled after setting).
        getWrapper().log(String.format("Selections before adding: %s", selectParent.getSelections()));

        for (String id : parentIds)
        {
            selectParent.typeAheadSelect(id);
            WebDriverWrapper.waitFor(()-> selectParent.getSelections().contains(id) ,
                    String.format("Parent '%s' was not added to the list.", parentIds), 2_500);
        }

        // Adding for debugging (trying to understand why save button is not enabled after setting).
        getWrapper().log(String.format("Selections after adding: %s", selectParent.getSelections()));

        return this;
    }

    /**
     * Remove a lineage type from the sample. Clicking this will remove all parents of that type. For example if the
     * given sample has added 'Sources01' source type and parent sources 'S1', 'S2' and 'S3', clicking this will remove this
     * source type and all the parent sources.
     *
     * @param typeName The name of the type to remove.
     * @return A reference to this panel.
     */
    public ParentEntityEditPanel removeEntityType(String typeName)
    {
        // Find all the ReactSelects
        List<ReactSelect> selectControls = getAllEntityTypes();

        int index = 0;
        boolean found = false;
        for (ReactSelect reactSelect : selectControls)
        {
            if (reactSelect.getSelections().contains(typeName))
            {
                found = true;
                break;
            }

            index++;
        }

        if (found)
        {
           elementCache().removeButton(index).click();

           // Need to check if this is removing the last/only type.
           if (selectControls.size() > 1)
           {
               // If it is not, removing the last one can simply check that the count of select controls is as expected.
               WebDriverWrapper.waitFor(() -> getAllEntityTypes().size() < selectControls.size(),
                       "The type '" + typeName + "' was not successfully removed.",
                       1_000);
           }
           else
           {
               // If this is removing the last/only one the count of select controls will still be 1 ('add new' one is shown),
               // so need a different check.
               ReactSelect rs = getAllEntityTypes().get(0);
               WebDriverWrapper.waitFor(() -> rs.getSelections().isEmpty(),
                       "The type '" + typeName + "' was not successfully removed.",
                       1_000);
           }
        }

        return this;
    }

    public ParentEntityEditPanel removeParent(String parentEntity, String parentId)
    {
        getParent(parentEntity).removeSelection(parentId);
        return this;
    }

    /**
     * Simple finder for this panel.
     */
    public static class ParentEntityEditPanelFinder extends WebDriverComponentFinder<ParentEntityEditPanel, ParentEntityEditPanelFinder>
    {
        public ParentEntityEditPanelFinder(WebDriver driver)
        {
            super(driver);
        }

        @Override
        protected ParentEntityEditPanel construct(WebElement element, WebDriver driver)
        {
            return new ParentEntityEditPanel(element, driver);
        }

        @Override
        protected Locator locator()
        {
            return Locator
                    .tagContainingText("div", "Editing")
                    .withClass("panel-heading")
                    .parent()
                    .child(Locator.tagWithClass("div", "panel-body"));
        }
    }

    public static class DataClassAddParentEntityPanelFinder extends WebDriverComponentFinder<ParentEntityEditPanel, DataClassAddParentEntityPanelFinder>
    {
        public DataClassAddParentEntityPanelFinder(WebDriver driver)
        {
            super(driver);
        }

        @Override
        protected ParentEntityEditPanel construct(WebElement element, WebDriver driver)
        {
            return new ParentEntityEditPanel(element, driver);
        }

        @Override
        protected Locator locator()
        {
            return Locator.tagWithClass("div", "dataclass-insert-add-parent-panel");
        }
    }

    @Override
    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    protected class ElementCache extends Component<?>.ElementCache
    {
        final WebElement saveButton = Locator.byClass("btn-success").withText("Save").findWhenNeeded(this);
        final WebElement cancelButton = Locator.byClass("btn-default").withText("Cancel").findWhenNeeded(this);

        // This is the 'Add' button that is contained inside the panel.
        final WebElement addButton = Locator
                .tagContainingText("span", "Add")
                .findWhenNeeded(this);

        // This is the remove button that is used to remove a lineage entity.
        WebElement removeButton(int index)
        {
            return Locator
                    .tagWithClass("span", "container--action-button")
                    .findElements(this).get(index);
        }
    }

}
