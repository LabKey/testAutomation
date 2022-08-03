package org.labkey.test.components.domain;

import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.labkey.api.exp.query.ExpSchema;
import org.labkey.test.BootstrapLocators;
import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.components.WebDriverComponent;
import org.labkey.test.components.bootstrap.ModalDialog;
import org.labkey.test.components.html.Checkbox;
import org.labkey.test.components.html.Input;
import org.labkey.test.components.html.RadioButton;
import org.labkey.test.components.html.SelectWrapper;
import org.labkey.test.components.ui.ontology.ConceptPickerDialog;
import org.labkey.test.params.FieldDefinition;
import org.labkey.test.util.LabKeyExpectedConditions;
import org.openqa.selenium.ElementNotInteractableException;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.SearchContext;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.FluentWait;
import org.openqa.selenium.support.ui.Select;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.junit.Assert.assertTrue;
import static org.labkey.test.WebDriverWrapper.WAIT_FOR_JAVASCRIPT;

public class DomainFieldRow extends WebDriverComponent<DomainFieldRow.ElementCache>
{
    public static final String ALL_SAMPLES_OPTION_TEXT = "All Samples";
    final WebElement _el;
    final WebDriver _driver;
    final DomainFormPanel _formPanel;

    public DomainFieldRow(DomainFormPanel panel, WebElement element, WebDriver driver)
    {
        _el = element;
        _driver = driver;
        _formPanel = panel;
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

    // basic field properties

    public String getName()
    {
        return elementCache().fieldNameInput.getValue();
    }

    public DomainFieldRow setName(String name)
    {
        elementCache().fieldNameInput.setValue(name);
        return this;
    }

    public Input nameInput()
    {
        return elementCache().fieldNameInput;
    }

    public WebElement typeInput()
    {
        return elementCache().fieldTypeSelectInput.getWrappedElement();
    }

    public FieldDefinition.ColumnType getType()
    {
        // The previous code doesn't work:
        //  String typeString = getWrapper().getFormElement(elementCache().fieldTypeSelectInput);
        //  return Enum.valueOf(FieldDefinition.ColumnType.class, typeString);
        // getFormElement get's the value attribute which is not the same as the text shown and not the
        // same as the Enum.valueOf. To get a match get the text shown in the control and compare it
        // to the enum's label attribute.

        String typeString = getWrapper().getSelectedOptionText(elementCache().fieldTypeSelectInput.getWrappedElement());
        for (FieldDefinition.ColumnType ct : FieldDefinition.ColumnType.values())
        {
            if (ct.getLabel().equalsIgnoreCase(typeString))
                return ct;
        }
        return null;
    }

    /**
     * Selects the field data type.
     */
    public DomainFieldRow setType(FieldDefinition.ColumnType columnType)
    {
        return setType(columnType, false);
    }

    /**
     * Selects the field data type.
     * @param confirmDialogExpected boolean indicating if this field data type change expects a confirm dialog
     */
    public DomainFieldRow setType(FieldDefinition.ColumnType columnType, boolean confirmDialogExpected)
    {
        elementCache().fieldTypeSelectInput.selectByVisibleText(columnType.getLabel());

        if (confirmDialogExpected)
        {
            ModalDialog confirmDialog = new ModalDialog.ModalDialogFinder(getDriver())
                    .withTitle("Confirm Data Type Change").timeout(1000).waitFor();
            confirmDialog.dismiss("Yes, Change Data Type");
        }

        return this;
    }

    public boolean getRequiredField()
    {
        return elementCache().fieldRequiredCheckbox.get();
    }

    public DomainFieldRow setRequiredField(boolean checked)
    {
        elementCache().fieldRequiredCheckbox.set(checked);
        return this;
    }

    public DomainFieldRow setSelectRowField(boolean checked)
    {
        elementCache().fieldSelectCheckbox.set(checked);
        return this;
    }

    public String detailsMessage()
    {
        return elementCache().fieldDetailsMessage.getText().trim();
    }

    public int getIndex()
    {
        String itemIndexAttribute = getComponentElement().getAttribute("tabindex");
        return Integer.parseInt(itemIndexAttribute);
    }

    public boolean isExpanded()
    {
        return elementCache().collapseToggleLoc.existsIn(this);
    }

    /**
     * Remove the field from the domain designer.
     *
     * @param confirmDialogExpected boolean indicating if this field removal expects a confirm dialog
     */
    public void clickRemoveField(boolean confirmDialogExpected)
    {
        getWrapper().mouseOver(elementCache().removeField);
        elementCache().removeField.click();

        if (confirmDialogExpected)
        {
            ModalDialog confirmDialog = new ModalDialog.ModalDialogFinder(getDriver())
                    .withTitle("Confirm Remove Field").timeout(1000).waitFor();
            confirmDialog.dismiss("Yes, Remove Field");
        }
    }

    public DomainFieldRow setAdvancedSettings(Map<AdvancedFieldSetting, Object> settings)
    {
        clickAdvancedSettings()
                .setAdvancedFieldSettings(settings)
                .apply();
        return this;
    }

    public AdvancedSettingsDialog clickAdvancedSettings()
    {
        expand();
        WebDriverWrapper.waitFor(() -> elementCache().advancedSettingsBtn.isEnabled(),
                "the Advanced Settings button did not become enabled", 5000);
        int trycount = 0;
        do
        {
            getWrapper().log("clicking advanced settings button try=[" + trycount + "]");
            elementCache().advancedSettingsBtn.click();
            getWrapper().shortWait().until(LabKeyExpectedConditions.animationIsDone(getComponentElement()));
            trycount++;
            assertTrue("advanced settings dialog did not appear in time", trycount < 4);
        }
        while (!Locator.tagWithClass("div", "modal-backdrop").existsIn(getDriver()));

        return new AdvancedSettingsDialog(this);
    }

    public DomainFieldRow expand()
    {
        if (!isExpanded())
        {
            getWrapper().shortWait().until(ExpectedConditions.elementToBeClickable(elementCache().expandToggle));

            for (int i = 0; i < 3; i++)
            {
                elementCache().expandToggle.click();
                getWrapper().shortWait().until(LabKeyExpectedConditions.animationIsDone(getComponentElement())); // wait for transition to happen
                if (WebDriverWrapper.waitFor(this::isExpanded, 1000))
                    break;
            }
            WebDriverWrapper.waitFor(this::isExpanded,
                    "the field row did not become expanded", 1500);
        }
        return this;
    }

    public DomainFieldRow collapse()
    {
        if (isExpanded())
        {
            elementCache().collapseToggle.click();
            getWrapper().shortWait().until(LabKeyExpectedConditions.animationIsDone(getComponentElement())); // wait for transition to happen
            WebDriverWrapper.waitFor(() -> elementCache().expandToggleLoc.existsIn(this),
                    "the field row did not collapse", 1500);
        }
        return this;
    }

    /**
     * indicates that the field has been added, the user will need to save changes to persist.
     * New fields can have their type changed
     */
    public boolean isNewField()
    {
        return Locator.tagWithAttributeContaining("span", "id", "domainpropertiesrow-details")
                .withText("New Field").existsIn(this);
    }

    /**
     * indicates that the field has been edited (but is not new). The user will need to save changes to persist
     */
    public boolean isEditedField()
    {
        return Locator.tagWithAttributeContaining("span", "id", "domainpropertiesrow-details")
                .withText("Updated").existsIn(this);
    }

    //
    // common field options

    public String getDescription()
    {
        expand();
        return getWrapper().getFormElement(elementCache().descriptionTextArea);
    }

    public DomainFieldRow setDescription(String description)
    {
        expand();
        getWrapper().shortWait().until(ExpectedConditions.elementToBeClickable(elementCache().descriptionTextArea));
        try
        {
            getWrapper().setFormElement(elementCache().descriptionTextArea, description);
        }
        catch (ElementNotInteractableException retry)
        {
            WebDriverWrapper.sleep(500);
            getWrapper().setFormElement(elementCache().descriptionTextArea, description);
        }
        return this;
    }

    public String getLabel()
    {
        expand();
        return elementCache().labelInput.getValue();
    }

    public DomainFieldRow setLabel(String label)
    {
        expand();
        elementCache().labelInput.setValue(label);
        return this;
    }

    public String getImportAliases()
    {
        expand();
        return elementCache().importAliasesInput.getValue();
    }

    public DomainFieldRow setImportAliases(String aliases)
    {
        expand();
        elementCache().importAliasesInput.setValue(aliases);
        return this;
    }

    public String getUrl()
    {
        expand();
        return elementCache().urlInput.getValue();
    }

    public DomainFieldRow setUrl(String url)
    {
        expand();
        elementCache().urlInput.setValue(url);
        return this;
    }

    //
    // numeric field options.

    public String getNumberFormat()
    {
        expand();
        return elementCache().numericFormatInput.getValue();
    }

    public DomainFieldRow setNumberFormat(String format)
    {
        expand();
        elementCache().numericFormatInput.set(format);
        return this;
    }

    public FieldDefinition.ScaleType getScaleType()
    {
        expand();
        String scaleTypeString = getWrapper().getFormElement(elementCache().defaultScaleTypeSelect.getWrappedElement());
        return Enum.valueOf(FieldDefinition.ScaleType.class, scaleTypeString.toUpperCase());
    }

    public DomainFieldRow setScaleType(FieldDefinition.ScaleType scaleType)
    {
        expand();
        elementCache().defaultScaleTypeSelect.selectByVisibleText(scaleType.getText());
        return this;
    }

    //
    // string field options.

    public DomainFieldRow allowMaxChar()
    {
        expand();
        elementCache().allowMaxCharCountRadio.set(true);
        return this;
    }

    public boolean isMaxCharDefault()
    {
        expand();
        return elementCache().allowMaxCharCountRadio.isChecked();
    }

    public DomainFieldRow setCharCount(int maxCharCount)
    {
        expand();
        String strCharCount = Integer.toString(maxCharCount);
        elementCache().setCharCountRadio.set(true);
        WebDriverWrapper.waitFor(() -> !isCharCountDisabled(),
                "character count input did not become enabled in time", 1000);
        elementCache().charScaleInput.setValue(strCharCount);
        return this;
    }

    public boolean isCharCountDisabled()
    {
        return elementCache().charScaleInput.getComponentElement().getAttribute("disabled") != null;
    }

    public boolean isFieldProtected()
    {
        return elementCache().fieldNameInput.getComponentElement().getAttribute("disabled") != null;
    }

    public boolean isCustomCharSelected()
    {
        expand();
        return elementCache().setCharCountRadio.isChecked();
    }

    public Integer getCustomCharCount()
    {
        expand();
        return Integer.parseInt(elementCache().charScaleInput.getValue());
    }

    public boolean isMaxTextLengthPresent(int rowIndex)
    {
        return getWrapper().isElementPresent(elementCache().getCharScaleInputLocForRow(rowIndex));
    }

    //
    // date field options.

    public String getDateFormat()
    {
        expand();
        return elementCache().dateFormatInput.getValue();
    }

    public DomainFieldRow setDateFormat(String formatString)
    {
        expand();
        elementCache().dateFormatInput.setValue(formatString);
        return this;
    }

    public DomainFieldRow setLookup(FieldDefinition.LookupInfo lookupInfo)
    {
        setType(FieldDefinition.ColumnType.Lookup);
        setFromFolder(lookupInfo.getFolder());
        setFromSchema(lookupInfo.getSchema());
        if (lookupInfo.getTableType() == null)
            throw new IllegalArgumentException("No lookup type specified for " + lookupInfo.getTable());
        String tableType = lookupInfo.getTableType().name();
        setFromTargetTable(lookupInfo.getTable() + " (" + tableType + ")");
        return this;
    }

    public String getFromFolder()
    {
        expand();
        return elementCache().lookupContainerSelect.getFirstSelectedOption().getText();
    }

    public DomainFieldRow setFromFolder(String containerPath)
    {
        expand();
        if (StringUtils.isBlank(containerPath) || containerPath.equals("Current Folder") || containerPath.equals("Current Project"))
        {
            containerPath = "";
        }
        WebDriverWrapper.waitFor(() -> Locator.tagContainingText("option","Current Project")
                .findOptionalElement(elementCache().lookupContainerSelect.getWrappedElement()).isPresent(), 2000);
        String initialValue = elementCache().lookupContainerSelect.getFirstSelectedOption().getAttribute("value");
        if (!containerPath.equals(initialValue))
        {
            elementCache().lookupContainerSelect.selectByValue(containerPath);
            getWrapper().shortWait().withMessage("Schema select didn't clear after selecting lookup container")
                    .until(ExpectedConditions.attributeToBe(elementCache().getLookupSchemaSelect().getWrappedElement(), "value", ""));
        }

        return this;
    }

    public String getFromSchema()
    {
        expand();
        return elementCache().getLookupSchemaSelect().getFirstSelectedOption().getText();
    }

    public DomainFieldRow setFromSchema(String schemaName)
    {
        expand();
        WebDriverWrapper.waitFor(() -> Locator.tagContainingText("option","core")
                .findOptionalElement(elementCache().getLookupSchemaSelect().getWrappedElement()).isPresent(), 2000);
        String initialValue = elementCache().getLookupSchemaSelect().getFirstSelectedOption().getText();
        if (!schemaName.equals(initialValue))
        {
            elementCache().getLookupSchemaSelect().selectByVisibleText(schemaName);
            getWrapper().shortWait().withMessage("Query select didn't update after selecting lookup schema")
                    .until(ExpectedConditions.attributeToBe(elementCache().getLookupQuerySelect().getWrappedElement(), "value", ""));
        }
        return this;
    }

    public String getFromTargetTable()
    {
        expand();
        return elementCache().getLookupQuerySelect().getFirstSelectedOption().getText();
    }

    public DomainFieldRow setFromTargetTable(String targetTable)
    {
        expand();
        elementCache().getLookupQuerySelect().selectByVisibleText(targetTable);
        return this;
    }

    public boolean getLookupValidatorEnabled()
    {
        expand();
        return elementCache().getLookupValidatorEnabledCheckbox().get();
    }

    public DomainFieldRow setLookupValidatorEnabled(boolean checked)
    {
        expand();
        elementCache().getLookupValidatorEnabledCheckbox().set(checked);
        return this;
    }

    // ontology lookup settings

    public DomainFieldRow setOntology(String ontology, String importField, String labelField)
    {
        setType(FieldDefinition.ColumnType.OntologyLookup);
        setSelectedOntology(ontology)
                .setConceptImportField(importField)
                .setConceptLabelField(labelField);
        return this;
    }

    public String getSelectedOntology()
    {
        expand();
        return elementCache().getOntologySelect().getFirstSelectedOption().getAttribute("value");
    }

    public DomainFieldRow setSelectedOntology(String ontology)
    {
        expand();
        elementCache().getOntologySelect().selectByValue(ontology);
        return this;
    }

    public DomainFieldRow setConceptImportField(String importField)
    {
        expand();
        elementCache().getConceptImportFieldSelect().selectByVisibleText(importField);
        return this;
    }

    public String getConceptImportField()
    {
        expand();
        return elementCache().getConceptImportFieldSelect().getFirstSelectedOption().getText();
    }

    /**
     * Allows test code to get which ontologies are available for selection
     *
     * @return a list of the full text shown in the options
     */
    public List<String> getConceptImportSelectOptions()
    {
        expand();
        return getWrapper().getTexts(elementCache().getOntologySelect().getOptions());
    }

    public DomainFieldRow setConceptLabelField(String labelField)
    {
        expand();
        elementCache().getConceptLabelFieldSelect().selectByVisibleText(labelField);
        return this;
    }

    public String getConceptLabelField()
    {
        expand();
        return elementCache().getConceptLabelFieldSelect().getFirstSelectedOption().getText();
    }

    public ConceptPickerDialog clickExpectedVocabulary()
    {
        expand();
        elementCache().expectedVocabularyButton().click();
        return new ConceptPickerDialog(new ModalDialog.ModalDialogFinder(getDriver()).withTitle("Expected Vocabulary"));
    }

    public ConceptPickerDialog clickSelectConcept()
    {
        expand();
        elementCache().selectConceptButton().click();
        return new ConceptPickerDialog(new ModalDialog.ModalDialogFinder(getDriver()).withTitle("Select Concept"));
    }

    public Optional<WebElement> optionalExpectedVocabularyLink()
    {
        expand();
        return elementCache().selectedVocabularyLink();
    }

    public DomainFieldRow clickRemoveExpectedVocabulary()
    {
        expand();
        WebDriverWrapper.waitFor(() -> elementCache().removeSelectedVocabularyLink().isPresent(),
                "the expected vocabulary link is not present", 2000);
        elementCache().removeSelectedVocabularyLink().get().click();
        return this;
    }

    public Optional<WebElement> optionalOntologyConceptLink()
    {
        expand();
        return elementCache().selectedConceptLink();
    }

    public DomainFieldRow clickRemoveOntologyConcept()
    {
        expand();
        WebDriverWrapper.waitFor(() -> elementCache().selectedConceptLink().isPresent(),
                "the expected ontology link is not present", 2000);
        elementCache().removeSelectedConceptLink().get().click();
        return this;
    }

    // TextChoice Field settings
    //
    // To a user a TextChoice field looks and behaves a lot like a lookup, even though it is implemented using a validator
    // behind the scenes. Because of that the validator aspect of the TextChoice field is hidden from the user (just like
    // it is in the product).

    /**
     * Set the list of allowed values for a TextChoice field.
     *
     * @param values List of values (Strings).
     * @return A reference to this field row.
     */
    public DomainFieldRow setTextChoiceValues(List<String> values)
    {
        Locator.tagWithClass("span", "container--action-button").withText("Add Values").findElement(this).click();

        TextChoiceValueDialog addValuesDialog = new TextChoiceValueDialog(this);
        addValuesDialog.addValues(values);
        return addValuesDialog.clickApply();
    }

    /**
     * Get the displayed list of allowed values a TextChoice field.
     *
     * @return Values shown in the list for the TextChoice field.
     */
    public List<String> getTextChoiceValues()
    {
        // Intentionally using '.collect(Collectors.toList())' so the returned list is mutable (e.g. can sort it when
        // comparing against expected).
        return Locator.tagWithClass("button", "list-group-item").findElements(this)
                .stream().map(WebElement::getText).collect(Collectors.toList());
    }

    /**
     * Get a list of the values that are 'locked' for a TextChoice field. A locked value is one that has been applied and cannot be deleted.
     *
     * @return List of locked values.
     */
    public List<String> getLockedTextChoiceValues()
    {
        // Intentionally using '.collect(Collectors.toList())' so the returned list is mutable (e.g. can sort it when
        // comparing against expected).
        return Locator.tagWithClass("button", "list-group-item")
                .withChild(Locator.tagWithClass("span", "choices-list__locked"))
                .findElements(this)
                .stream().map(WebElement::getText).collect(Collectors.toList());
    }

    /**
     * Get a list of the values that are 'unlocked' for a TextChoice field. An unlocked value is one that has not been used.
     *
     * @return List of unlocked values.
     */
    public List<String> getUnlockedTextChoiceValues()
    {
        List<String> allChoices = getTextChoiceValues();
        allChoices.removeAll(getLockedTextChoiceValues());
        return allChoices;
    }

    /**
     * Enter a value into the search field for TextChoice values. If no values have been this will error with a
     * no-such-element exception.
     *
     * @param searchValue Value to search for.
     * @return List of values matching the search value.
     */
    public List<String> searchForTextChoiceValue(String searchValue)
    {
        WebElement searchTxtBox = Locator.tagWithClass("input", "domain-text-choices-search").findElement(this);
        getWrapper().setFormElement(searchTxtBox, searchValue);
        return getTextChoiceValues();
    }

    /**
     * Click on a value in the list of values for a TextChoice field. If the value is already selected this will have
     * no effect.
     *
     * @param value The value to click on.
     * @return This field row.
     */
    public DomainFieldRow selectTextChoiceValue(String value)
    {
        Locator.tagWithClass("button", "list-group-item").withText(value).findElement(this).click();
        return this;
    }

    /**
     * Check if the 'Apply' button is enabled for a TextChoice field. I will not be enabled if the value has not been
     * changed, or if it conflicts with an existing value. Note: The button is only visible if a value is selected.
     *
     * @return True if the button is enabled, false otherwise.
     */
    public boolean isTextChoiceApplyButtonEnabled()
    {
        WebElement button = Locator.button("Apply").findElement(this);
        return button.isEnabled();
    }

    /**
     * Enter text into the update textbox for a value. Note: The field is only visible if a value is selected.
     *
     * @param updatedValue Value to enter into the update textbox.
     * @return This field row.
     */
    public DomainFieldRow setUpdateTextChoiceValue(String updatedValue)
    {
        WebDriverWrapper.waitFor(this::isTextChoiceUpdateFieldEnabled,
                "The edit field for the TextChoice value did not become enabled in time.", 1_000);
        getWrapper().setFormElement(Locator.tagWithName("input", "value").findElement(this), updatedValue);
        return this;
    }

    private void updateValue(String originalValue, String newValue, boolean clickApply)
    {
        selectTextChoiceValue(originalValue);
        setUpdateTextChoiceValue(newValue);

        if(clickApply)
        {
            WebDriverWrapper.waitFor(this::isTextChoiceApplyButtonEnabled, "'Apply' button is not enabled.", 1_000);
            Locator.button("Apply").findElement(this).click();
        }
    }

    /**
     * Update the selected value in the TextChoice field. This will select the given field.
     *
     * @param originalValue The original value to select and change.
     * @param newValue The new value to replace it.
     * @return This field row.
     */
    public DomainFieldRow updateTextChoiceValue(String originalValue, String newValue)
    {
        WebElement alert = Locator.tagWithClass("div", "alert").findWhenNeeded(this);
        updateValue(originalValue, newValue, true);

        Assert.assertFalse("An unexpected alert was present after applying the change.",
                alert.isDisplayed());

        return this;
    }

    /**
     * Update a TextChoice value and expect an error. Can happen if the new value already exists in the list of values.
     *
     * @param originalValue The original value to select and change.
     * @param newValue The new value to replace it.
     * @return The error message.
     */
    public String updateTextChoiceValueExpectError(String originalValue, String newValue)
    {
        updateValue(originalValue, newValue, false);
        WebElement alert = BootstrapLocators.errorBanner.waitForElement(this, 1_000);
        return alert.getText();
    }

    /**
     * Update a TextChoice value that is currently being used and expect an info message with the number updated.
     *
     * @param originalValue The original value to select and change.
     * @param newValue The new value to replace it.
     * @return The info message indicating the number of entites updated.
     */
    public String updateLockedTextChoiceValue(String originalValue, String newValue)
    {
        updateValue(originalValue, newValue, true);
        WebElement alert = BootstrapLocators.infoBanner.waitForElement(this, 1_000);
        return alert.getText();
    }

    /**
     * Select a TextChoice value and check if the edit field is enabled.
     *
     * @param value The TextChoice value to select.
     * @return True if enabled, false otherwise.
     */
    public boolean isTextChoiceUpdateFieldEnabled(String value)
    {
        selectTextChoiceValue(value);
        return isTextChoiceUpdateFieldEnabled();
    }

    /**
     * Check if the edit field is enabled for the selected TextChoice value. A value must be selected.
     *
     * @return True if enabled, false otherwise.
     */
    public boolean isTextChoiceUpdateFieldEnabled()
    {
        WebElement updateField = Locator.tagWithName("input", "value").findWhenNeeded(this);

        Assert.assertTrue("Update field is not visible, make sure a TextChoice value is selected.",
                updateField.isDisplayed());

        return updateField.isEnabled();
    }

    /**
     * Check to see if the 'Delete' button is enabled for a TextChoice field. Note: The button is only visible if a
     * value is selected.
     *
     * @return True if the button is enabled, false otherwise.
     */
    public boolean isTextChoiceDeleteButtonEnabled()
    {
        WebElement button = Locator.buttonContainingText("Delete").findElement(this);
        return button.isEnabled();
    }

    /**
     * Delete a value from the list of values for a TextChoice field. This will select the value before deleting it.
     *
     * @param value The value to delete.
     * @return This field row.
     */
    public DomainFieldRow deleteTextChoiceValue(String value)
    {
        selectTextChoiceValue(value);
        WebElement button = Locator.buttonContainingText("Delete").refindWhenNeeded(this);
        WebDriverWrapper.waitFor(button::isEnabled, "'Delete' button is not enabled.", 1_000);
        button.click();
        WebDriverWrapper.waitFor(()->!button.isDisplayed(), "Deleting TextChoice value did not complete.", 1_000);
        return this;
    }

    // advanced settings

    public DomainFieldRow showFieldOnDefaultView(boolean checked)
    {
        clickAdvancedSettings()
                .showInDefaultView(checked)
                .apply();
        return this;
    }

    public DomainFieldRow showFieldOnInsertView(boolean checked)
    {
        clickAdvancedSettings()
                .showInInsertView(checked)
                .apply();
        return this;
    }

    public DomainFieldRow showFieldOnUpdateView(boolean checked)
    {
        clickAdvancedSettings()
                .showInUpdateView(checked)
                .apply();
        return this;
    }

    public DomainFieldRow showFieldOnDetailsView(boolean checked)
    {
        clickAdvancedSettings()
                .showInDetailsView(checked)
                .apply();
        return this;
    }

    public DomainFieldRow setPHILevel(FieldDefinition.PhiSelectType phiLevel)
    {
        clickAdvancedSettings()
                .setPHILevel(phiLevel)
                .apply();
        return this;
    }

    public DomainFieldRow setExcludeFromDateShifting(boolean shift)
    {
        clickAdvancedSettings()
                .excludeFromDateShifting(shift)
                .apply();
        return this;
    }

    public DomainFieldRow setMeasure(boolean checked)
    {
        clickAdvancedSettings()
                .setMeasure(checked)
                .apply();
        return this;
    }

    public DomainFieldRow setDimension(boolean checked)
    {
        clickAdvancedSettings()
                .setDimension(checked)
                .apply();
        return this;
    }

    public DomainFieldRow setRecommendedVariable(boolean checked)
    {
        clickAdvancedSettings()
                .setRecommendedVariable(checked)
                .apply();
        return this;
    }

    public DomainFieldRow setMissingValuesEnabled(boolean checked)
    {
        clickAdvancedSettings()
                .setMissingValuesEnabled(checked)
                .apply();
        return this;
    }

    // error and warning

    public boolean hasFieldError()
    {
        return getComponentElement().getAttribute("class").contains("domain-row-border-error");
    }

    public DomainFieldRow waitForError()
    {
        WebDriverWrapper.waitFor(this::hasFieldError, WAIT_FOR_JAVASCRIPT);
        return this;
    }

    public boolean hasFieldWarning()
    {
        return getComponentElement().getAttribute("class").contains("domain-row-border-warning");
    }

    public DomainFieldRow waitForWarning()
    {
        WebDriverWrapper.waitFor(this::hasFieldWarning, WAIT_FOR_JAVASCRIPT);
        return this;
    }

    // conditional formatting and validation options

    /**
     * appends the supplied validators to any existing ones (including any empty default ones that might exist)
     */
    public DomainFieldRow addRangeValidators(List<FieldDefinition.RangeValidator> validators)
    {
        RangeValidatorDialog dialog = clickRangeButton();
        for (FieldDefinition.RangeValidator val : validators)
        {
            dialog.addValidator(val);
        }
        dialog.clickApply();
        return this;
    }

    /**
     * sets the supplied validators on the field, assumes none are already present
     */
    public DomainFieldRow setRangeValidators(List<FieldDefinition.RangeValidator> validators)
    {
        RangeValidatorDialog dialog = clickRangeButton();
        for (int i = 0; i < validators.size(); i++)
        {
            dialog.setValidator(i, validators.get(i));
        }
        dialog.clickApply();
        return this;
    }

    public RangeValidatorDialog clickRangeButton()
    {
        expand();
        getWrapper().shortWait().until(ExpectedConditions.elementToBeClickable(elementCache().rangeButton()));
        elementCache().rangeButton().click();
        return new RangeValidatorDialog(this);
    }

    /**
     * adds the specified regexValidators to the field.  Assumes that any range panels in the dialog
     * are complete (by default, the first one is already shown but is empty)
     * When adding the first/only regex validators to the field, use setRegularExpressions
     */
    public DomainFieldRow addRegularExpressions(List<FieldDefinition.RegExValidator> validators)
    {
        RegexValidatorDialog dialog = clickRegexButton();
        for (FieldDefinition.RegExValidator val : validators)
        {
            dialog.addValidator(val);
        }
        dialog.clickApply();
        return this;
    }

    public DomainFieldRow setRegExValidators(List<FieldDefinition.RegExValidator> validators)
    {
        RegexValidatorDialog dialog = clickRegexButton();
        for (int i = 0; i < validators.size(); i++)
        {
            dialog.setValidator(i, validators.get(i));
        }
        dialog.clickApply();
        return this;
    }

    public RegexValidatorDialog clickRegexButton()
    {
        expand();
        getWrapper().shortWait().until(ExpectedConditions.elementToBeClickable(elementCache().regexValidatorButton()));
        elementCache().regexValidatorButton().click();
        return new RegexValidatorDialog(this);
    }

    public ConditionalFormatDialog clickConditionalFormatButton()
    {
        expand();
        getWrapper().shortWait().until(ExpectedConditions.elementToBeClickable(elementCache().conditionalFormatButton()));
        elementCache().conditionalFormatButton().click();
        return new ConditionalFormatDialog(this);
    }

    @Override
    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    public String getSampleType()
    {
        expand();
        return elementCache().getLookupSampleTypeSelect().getFirstSelectedOption().getText();
    }

    public DomainFieldRow setSampleType(String sampleTypeName)
    {
        expand();
        elementCache().getLookupSampleTypeSelect().selectByVisibleText(sampleTypeName);
        return this;
    }

    public DomainFieldRow setAliquotOption(ExpSchema.DerivationDataScopeType option)
    {
        expand();
        elementCache().aliquotOption(option).check();
        return this;
    }

    public RadioButton getAliquotOptionRadioButton(ExpSchema.DerivationDataScopeType option)
    {
        expand();
        return elementCache().aliquotOption(option);
    }

    public ExpSchema.DerivationDataScopeType getSelectedAliquotOption()
    {
        expand();
        for (ExpSchema.DerivationDataScopeType option : ExpSchema.DerivationDataScopeType.values())
        {
            if (elementCache().aliquotOption(option).isChecked())
                return option;
        }
        return null;
    }

    public boolean hasAliquotOptionWarning()
    {
        expand();
        return getWrapper().isElementPresent(elementCache().aliquotWarningAlert);
    }

    public String getAliquotOptionWarning()
    {
        return elementCache().aliquotWarningAlert.findElement(this).getText();
    }

    public static class DomainFieldRowFinder extends WebDriverComponentFinder<DomainFieldRow, DomainFieldRowFinder>
    {
        private final Locator.XPathLocator _baseLocator = Locator.tagWithClassContaining("div", "domain-field-row").withoutClass("domain-floating-hdr");
        private String _title = null;
        private DomainFormPanel _domainFormPanel;

        public DomainFieldRowFinder(DomainFormPanel panel, WebDriver driver)
        {
            super(driver);
            _domainFormPanel = panel;
        }

        public DomainFieldRowFinder withTitle(String title)
        {
            _title = title;
            return this;
        }

        @Override
        protected DomainFieldRow construct(WebElement el, WebDriver driver)
        {
            return new DomainFieldRow(_domainFormPanel, el, driver);
        }

        @Override
        protected Locator locator()
        {
            if (_title != null)
                return _baseLocator.withAttribute("title", _title);
            else
                return _baseLocator;
        }
    }

    protected class ElementCache extends WebDriverComponent.ElementCache
    {
        // base row controls
        public final Input fieldNameInput = new Input(Locator.tagWithAttributeContaining("input", "id", "domainpropertiesrow-name-")
                .findWhenNeeded(this), getDriver());
        public final Select fieldTypeSelectInput = SelectWrapper.Select(Locator.tagWithAttributeContaining("select", "id", "domainpropertiesrow-type-"))
                .findWhenNeeded(this);
        public final Checkbox fieldRequiredCheckbox = new Checkbox(Locator.tagWithAttributeContaining("input", "id", "domainpropertiesrow-required-")
                .findWhenNeeded(this));

        public final Checkbox fieldSelectCheckbox = new Checkbox(Locator.tagWithAttributeContaining("input", "id", "domainpropertiesrow-selected")
                .findWhenNeeded(this));

        public final WebElement fieldDetailsMessage = Locator.css(".domain-field-details, .domain-field-details-expanded")
                .findWhenNeeded(this);


        public final Locator expandToggleLoc = Locator.tagWithClass("div", "field-icon")
                .child(Locator.tagWithClassContaining("svg", "fa-plus-square"));
        public final Locator collapseToggleLoc = Locator.tagWithClass("div", "field-icon")
                .child(Locator.tagWithClassContaining("svg", "fa-minus-square"));
        public final WebElement expandToggle = expandToggleLoc.findWhenNeeded(this);

        public final Locator removeFieldLoc = Locator.tagWithClass("span", "field-icon")
                .child(Locator.tagWithClassContaining("svg", "domain-field-delete-icon"));
        public final WebElement removeField = removeFieldLoc.findWhenNeeded(this);

        // controls revealed when expanded
        public final WebElement advancedSettingsBtn = Locator.button("Advanced Settings")      // not enabled for now, placeholder
                .refindWhenNeeded(this).withTimeout(WAIT_FOR_JAVASCRIPT);
        public final WebElement collapseToggle = collapseToggleLoc.refindWhenNeeded(this).withTimeout(WAIT_FOR_JAVASCRIPT);


        // common field options
        public final WebElement descriptionTextArea = Locator.tagWithAttributeContaining("textarea", "id", "domainpropertiesrow-description-")
                .refindWhenNeeded(this);
        public final Input labelInput = new Input(Locator.tagWithAttributeContaining("input", "id", "domainpropertiesrow-label-")
                .refindWhenNeeded(this), getDriver());
        public final Input importAliasesInput = new Input(Locator.tagWithAttributeContaining("input", "id", "domainpropertiesrow-importAliases-")
                .refindWhenNeeded(this), getDriver());
        public final Input urlInput = new Input(Locator.tagWithAttributeContaining("input", "id", "domainpropertiesrow-URL-")
                .refindWhenNeeded(this), getDriver());

        // numeric field options
        public final Input numericFormatInput = new Input(Locator.tagWithAttributeContaining("input", "id", "domainpropertiesrow-format")
                .refindWhenNeeded(this), getDriver());
        public final Select defaultScaleTypeSelect = SelectWrapper.Select(Locator.name("domainpropertiesrow-defaultScale"))
                .findWhenNeeded(this);

        // text field options
        public final RadioButton allowMaxCharCountRadio = new RadioButton(Locator.tagWithAttributeContaining("input", "id", "domainpropertiesrow-maxLength-")
                .refindWhenNeeded(this));
        public final RadioButton setCharCountRadio = new RadioButton(Locator.tagWithAttributeContaining("input", "id", "domainpropertiesrow-customLength-")
                .refindWhenNeeded(this));
        public final Input charScaleInput = new Input(Locator.tagWithAttributeContaining("input", "id", "domainpropertiesrow-scale")
                .refindWhenNeeded(this), getDriver());
        // date field options
        public final Input dateFormatInput = new Input(Locator.tagWithAttributeContaining("input", "id", "domainpropertiesrow-format")
                .refindWhenNeeded(this), getDriver());
        //lookup field options
        public final Select lookupContainerSelect = SelectWrapper.Select(Locator.name("domainpropertiesrow-lookupContainer"))
                .findWhenNeeded(this);
        Locator.XPathLocator selectVocabularyBtnLoc = Locator.tagWithAttribute("button", "name", "domainpropertiesrow-principalConceptCode")
                .withText("Expected Vocabulary");
        Locator.XPathLocator selectConceptBtnLoc = Locator.tagWithAttribute("button", "name", "domainpropertiesrow-principalConceptCode")
                .withText("Select Concept");

        Locator.XPathLocator aliquotWarningAlert = Locator.tagWithClassContaining("div", "aliquot-alert-warning");

        public Locator.XPathLocator getCharScaleInputLocForRow(int rowIndex)
        {
            return Locator.tagWithId("input", "domainpropertiesrow-scale-0-" + rowIndex);
        }

        public Select getLookupSchemaSelect()
        {
            Select select = SelectWrapper.Select(Locator.name("domainpropertiesrow-lookupSchema")).find(this);
            return waitForSelectToLoad(select);
        }

        public Select getLookupQuerySelect()
        {
            Select select = SelectWrapper.Select(Locator.name("domainpropertiesrow-lookupQueryValue")).find(this);
            return waitForSelectToLoad(select);
        }

        public Select getLookupSampleTypeSelect()
        {
            Select select = SelectWrapper.Select(Locator.name("domainpropertiesrow-sampleTypeSelect")).find(this);
            return waitForSelectToLoad(select);
        }

        // ontology lookup settings
        public Select getOntologySelect()
        {
            Select select = SelectWrapper.Select(Locator.tagWithAttributeContaining("select", "id", "domainpropertiesrow-sourceOntology")).waitFor(this);
            return waitForSelectToLoad(select);
        }

        public Select getConceptImportFieldSelect()
        {
            Select select = SelectWrapper.Select(
                    Locator.tagWithAttributeContaining("select", "id", "domainpropertiesrow-conceptImportColumn"))
                    .waitFor(this);
            return waitForSelectToLoad(select);
        }

        public Select getConceptLabelFieldSelect()
        {
            Select select = SelectWrapper.Select(
                    Locator.tagWithAttributeContaining("select", "id", "domainpropertiesrow-conceptLabelColumn"))
                    .waitFor(this);
            return waitForSelectToLoad(select);
        }

        public WebElement expectedVocabularyButton()
        {
            return selectVocabularyBtnLoc.waitForElement(this, 2000);
        }

        public WebElement selectConceptButton()
        {
            return selectConceptBtnLoc.waitForElement(this, 2000);
        }

        Optional<WebElement> selectedVocabularyLink()
        {
            return Locator.tagWithClass("table", "domain-annotation-table")
                    .withDescendant(selectVocabularyBtnLoc)
                    .descendant(Locator.tagWithClass("td", "content")
                            .child(Locator.tagWithClass("a", "domain-annotation-item")))
                    .findOptionalElement(this);
        }

        Optional<WebElement> selectedConceptLink()
        {
            return Locator.tagWithClass("table", "domain-annotation-table")
                    .withDescendant(selectConceptBtnLoc)
                    .descendant(Locator.tagWithClass("td", "content")
                            .child(Locator.tagWithClass("a", "domain-annotation-item")))
                    .findOptionalElement(this);
        }

        Optional<WebElement> removeSelectedVocabularyLink()
        {
            return Locator.tagWithClass("table", "domain-annotation-table")
                    .withDescendant(selectVocabularyBtnLoc)
                    .descendant(Locator.tagWithClass("td", "content")
                            .child(Locator.tagWithClass("a", "domain-validator-link")
                                    .child(Locator.tagWithClass("i", "fa-remove"))))
                    .findOptionalElement(this);
        }

        Optional<WebElement> removeSelectedConceptLink()
        {
            return Locator.tagWithClass("table", "domain-annotation-table")
                    .withDescendant(selectConceptBtnLoc)
                    .descendant(Locator.tagWithClass("td", "content")
                            .child(Locator.tagWithClass("a", "domain-validator-link")
                                    .child(Locator.tagWithClass("i", "fa-remove"))))
                    .findOptionalElement(this);
        }


        private Select waitForSelectToLoad(Select select)
        {
            Locator.XPathLocator loadingOption = Locator.tagWithText("option", "Loading...");
            if (!WebDriverWrapper.waitFor(() -> !loadingOption.existsIn(select.getWrappedElement()), WAIT_FOR_JAVASCRIPT))
            {
                throw new NoSuchElementException("Select got stuck loading: " + select.getWrappedElement().toString());
            }
            return select;
        }

        public Checkbox getLookupValidatorEnabledCheckbox()
        {
            return new Checkbox(Locator.checkboxByName("domainpropertiesrow-lookupValidator").findElement(this));
        }

        public WebElement rangeButton()
        {
            return Locator.waitForAnyElement(new FluentWait<SearchContext>(this).withTimeout(Duration.ofMillis(WAIT_FOR_JAVASCRIPT)),
                    Locator.button("Add Range"), Locator.button("Edit Ranges"));
        }

        public WebElement regexValidatorButton()
        {
            return Locator.waitForAnyElement(new FluentWait<SearchContext>(this).withTimeout(Duration.ofMillis(WAIT_FOR_JAVASCRIPT)),
                    Locator.button("Add Regex"), Locator.button("Edit Regex"));
        }

        public WebElement conditionalFormatButton()
        {
            return Locator.waitForAnyElement(new FluentWait<SearchContext>(this).withTimeout(Duration.ofMillis(WAIT_FOR_JAVASCRIPT)),
                    Locator.button("Add Format"), Locator.button("Edit Formats"));
        }

        public RadioButton aliquotOption(ExpSchema.DerivationDataScopeType option)
        {
            return new RadioButton.RadioButtonFinder().withValue(option.name()).find(this);
        }


    }
}
