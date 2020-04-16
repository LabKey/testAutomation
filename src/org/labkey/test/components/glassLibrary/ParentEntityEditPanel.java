package org.labkey.test.components.glassLibrary;

import org.labkey.test.BootstrapLocators;
import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.components.Component;
import org.labkey.test.components.WebDriverComponent;
import org.labkey.test.components.glassLibrary.components.FilteringReactSelect;
import org.labkey.test.components.glassLibrary.components.ReactSelect;
import org.labkey.test.pages.samplemanagement.samples.SampleOverviewPage;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import java.util.List;

/**
 * <p>
 * This is a base class for the edit lineage panels that are shown on the sample overview page. Samples can have two
 * "types" of lineage, another sample (Parent) and a source (Source). The panel is the same for both. It has two
 * select/combos on it. The first selects the type (i.e. the name of the Sample Type) and the second selects the
 * specific entity to app (i.e. the sample).
 * </p>
 * <p>
 *  It is not intended that this base class to be exposed directly from a page. Rather the derived classes
 *  EditParentsPanel and EditSourcesPanel are the classes that should be exposed on the form.
 * @see org.labkey.test.pages.samplemanagement.samples.EditParentsPanel
 * @see org.labkey.test.pages.samplemanagement.samples.EditSourcesPanel
 * @see <a href="https://github.com/LabKey/labkey-ui-components/blob/master/packages/components/src/components/entities/ParentEntityEditPanel.tsx">ParentEntityEditPanel.tsx</a>
 *
 * </p>
 */
public class ParentEntityEditPanel extends WebDriverComponent<ParentEntityEditPanel.ElementCache>
{
    private final WebDriver _driver;
    private final WebElement _editingDiv;

    /** Identifies if this is a Source or Parent (sample type) of lineage element. */
    private final ParentType _parentType;

    /**
     * Constructor for the panel.
     *
     * @param element The WebElement containing the panel.
     * @param driver A reference to the WebDriver
     * @param parentType An enum of type @ParentType used to identify the type of lineage entity is being edited.
     *                   This is used to set the expected text when searching for web elements.
     */
    public ParentEntityEditPanel(WebElement element, WebDriver driver, ParentType parentType)
    {
        _driver = driver;
        _editingDiv = element;
        _parentType = parentType;
    }

    @Override
    public WebElement getComponentElement()
    {
        return _editingDiv;
    }

    @Override
    protected WebDriver getDriver()
    {
        return _driver;
    }

    /**
     * Click the 'Cancel' button. The edit panel will go away and the user is returned to the Sample Overview page.
     *
     * @return A new reference to the Sample Overview page.
     */
    public SampleOverviewPage clickCancel()
    {
        elementCache()
                .button("Cancel", "Editing " + _parentType.getTextValue() + " Details")
                .click();

        return new SampleOverviewPage(getWrapper());
    }

    /** Click the 'Save' button The edit panel will go away and the user is returned to the Sample Overview page.
     *
     * @return A new reference to the Sample Overview page.
     */
    public SampleOverviewPage clickSave()
    {
        elementCache()
                .button("Save", "Editing " + _parentType.getTextValue() + " Details")
                .click();

        getWrapper().waitForElementToDisappear(Locator.tagWithClass("div", "detail__editing"));

        return new SampleOverviewPage(getWrapper());
    }

    /**
     * Expect and error after clicking 'Save' button.
     *
     * @return The error message shown in error banner.
     */
    public String clickSaveExpectingError()
    {
        elementCache()
                .button("Save", "Editing " + _parentType.getTextValue() + " Details")
                .click();
        return BootstrapLocators.errorBanner.waitForElement(this, 1_000).getText();
    }

    /**
     * Is the 'Save' button enabled.
     *
     * @return True if it is, false otherwise.
     */
    public boolean isSaveButtonEnabled()
    {
        // If element is enabled the disabled attribute will not be there and getAttribute will return null.
        return null == elementCache()
                .button("Save", "Editing " + _parentType.getTextValue() + " Details")
                .getAttribute("disabled");
    }

    /**
     * Get the combo that will list the types, either Sources or Samples. Because this is an edit panel there will
     * always be at least one type combo. This will return the combo that can be used to add a new parent type.
     *
     * @return A reference to the appropriate combo that will be for the new parent sample or source type.
     */
    protected ReactSelect getAddNewTypeCombo()
    {
        int numOfTypes = numberOfTypeFields();

        ReactSelect typeCombo = getTypeCombo(_parentType.getTextValue() + " Type " + numOfTypes);

        // If the 'last' combo in the list contains the text "Select a..." it can be used to add a new type.
        // If it does not contain that then the addButton must be clicked.
        if(!typeCombo.getSelections().contains("Select a Source Type ...") &&
                        !typeCombo.getSelections().contains("Select a Parent Type ..."))
        {
            // Since there is already a parent need to click the "Add" button to add a new one.
            elementCache().addButton.click();

            // Now need to go find the new combo but wait until it shows up.
            WebDriverWrapper.waitFor(()-> numberOfTypeFields() > numOfTypes,
                    "The new type field did not show up in a timely fashion.",
                    1_000);

            typeCombo = ReactSelect.finder(getDriver())
                    .followingLabelWithSpan(_parentType.getTextValue() + " Type " + (numOfTypes + 1))
                    .find(this);

        }

        return typeCombo;

    }

    /**
     * Get the type combos by it's index (zero based). This would include any 'add new' combos.
     *
     * @return A combo at the given position in the colection of combos..
     */
    protected ReactSelect getTypeCombo(int index)
    {
        return getAllTypeCombo().get(index);
    }

    /**
     * Get the type combos by it's index (zero based). This would include any 'add new' combos.
     *
     * @return A combo at the given position in the colection of combos..
     */
    protected ReactSelect getTypeCombo(String labelText)
    {
        return ReactSelect.finder(getDriver())
                .followingLabelWithSpan(labelText)
                .find(this);
    }

    /**
     * Get the type combos by it's index (zero based). This would include any 'add new' combos.
     *
     * @return A combo at the given position in the colection of combos..
     */
    protected ReactSelect getTypeComboWithSelection(String selection)
    {
        ReactSelect theCombo = null;
        List<ReactSelect> allCombos = getAllTypeCombo();
        for(ReactSelect combo : allCombos)
        {
            if(combo.getSelections().contains(selection))
            {
                theCombo = combo;
                break;
            }
        }

        return theCombo;
    }

    /**
     * Get all of the type combos in the panel. This includes those already set and any 'add new' combos as well.
     *
     * @return A collection of all of the combos that select the type.
     */
    protected List<ReactSelect> getAllTypeCombo()
    {
        return ReactSelect.finder(getDriver()).findAll(this);
    }

    /**
     * Get the combo that will list the individual elements, either samples or sources, that will become a parent.
     * This combo will only show up after a type is selected.
     *
     * @return A reference to the appropriate combo that will be for the new parent sample or source.
     */
    protected FilteringReactSelect getIdCombo()
    {
        // Will get the ID Combo for the last field. A new parent is added to the end.
        int numOfTypes = numberOfTypeFields();

        return FilteringReactSelect.finder(getDriver())
                .followingLabelWithSpan(_parentType.getTextValue() + " IDs")
                .findAll(this).get(numOfTypes - 1);

    }

    /**
     * Count how many type fields have already been added. Because this is an edit panel the count will always be at
     * least one for the new parent sample/source that is being added.
     *
     * @return The count of type fields in the panel.
     */
    private int numberOfTypeFields()
    {
        Locator loc = Locator.xpath(
                "//label[contains(@class,'entity-insert--parent-label')]/span[contains(text(),'Type')]");

        return loc.findElements(this).size();
    }

    /**
     * Add a specific type(s) (sample or source) from the given type.
     *
     * @param typeName The name of the type. For example if you have a Source Type named 'Sources01' the value of
     *                 this parameter would be "Sources01".
     * @param ids A list of the individuals samples or sources to add.
     * @return A reference to this panel.
     */
    public ParentEntityEditPanel addType(String typeName, List<String> ids)
    {
        ReactSelect parentTypeCombo = getAddNewTypeCombo();

        getWrapper().scrollIntoView(parentTypeCombo.getComponentElement());

        try
        {
            // TODO Fix the reactSelect control Issue 40180: ReactSelect needs to deal with control being recreated after a selection is made.
            parentTypeCombo.select(typeName);
        }
        catch(StaleElementReferenceException stale)
        {
            // Do nothing.
            // Unfortunately the way the ReactSelect is written is it get the value of the selection after a choice is
            // made. In this panel however when a source is selected the select goes away, the panel is redrawn and the
            // select is recreated with the selected value.
        }

        FilteringReactSelect parentIdCombo = getIdCombo();

        getWrapper().scrollIntoView(parentIdCombo.getComponentElement());

        for(String id : ids)
        {
            int selCount = parentIdCombo.getSelections().size();
            parentIdCombo.typeAheadSelect(id);
            getWrapper().waitFor(()-> parentIdCombo.getSelections().size() > selCount, 500);
        }

        return this;
    }

    /**
     * Remove a lineage type from the sample. Clicking this will remove all parents of that type. For example if the
     * given sample has added 'Sources01' Source Type and sources 'S1', 'S2' and 'S3', clicking this will remove this
     * Source Type and all of the sources.
     *
     * @param typeName The name of the type to remove.
     * @return A reference to this panel.
     */
    public ParentEntityEditPanel removeType(String typeName)
    {
        // Find all the react selects
        List<ReactSelect> typeCombos = ReactSelect.finder(getDriver())
                .findAll(this);

        int index = 0;
        boolean found = false;
        for(ReactSelect reactSelect : typeCombos)
        {
            if(reactSelect.getSelections().contains(typeName))
            {
                found = true;
                break;
            }

            index++;
        }

        if(found)
        {
           elementCache().removeButton(index + 1).click();
           getWrapper().waitFor(()-> ReactSelect.finder(getDriver())
                   .findAll(this).size() < typeCombos.size(),
                   "The type '" + typeName + "' was not successfully removed.",
                   1_000);
        }

        return this;
    }

    @Override
    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    protected class ElementCache extends Component<?>.ElementCache
    {
        // The 'Save' and 'Cancel' buttons are actually on the page and not the panel, so the search context needs
        // to be the entire page. And since it is unknown how many edit panels are on the page at a given time, and
        // where this particular panel is in the collection use the panel title as a starting point to find the
        // buttons.
        WebElement button(String buttonText, String panelTitle)
        {
            return Locator.tagWithText("div", panelTitle)
                    .parent("div")
                    .parent("div")
                    .followingSibling("div")
                    .childTag("button")
                    .withText(buttonText)
                    .findElement(getDriver());
        }

        // This is the 'Add' button that is contained inside of the panel.
        final WebElement addButton = Locator
                .tagContainingText("span", "Add")
                .findWhenNeeded(this);

        // This is the remove button that is used to remove a lineage entity.
        WebElement removeButton(int index)
        {
            return Locator
                    .tagWithText("span", "Remove " + _parentType.getTextValue() + " Type " + index)
                    .findElement(this);
        }

    }

    /**
     * This enum is used to identify which type of lineage entity is being edited. An enum of this type is passed
     * into the constructor for the panel.
     */
    public enum ParentType
    {
        SAMPLE("Parent"),
        SOURCE("Source");

        private final String _textValue;

        public String getTextValue()
        {
            return _textValue;
        }

        ParentType(String textValue)
        {
            _textValue = textValue;
        }
    }

}
