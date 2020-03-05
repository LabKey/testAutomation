package org.labkey.test.components.domain;

import org.apache.commons.lang3.StringUtils;
import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.components.PropertiesEditor;
import org.labkey.test.components.WebDriverComponent;
import org.labkey.test.components.bootstrap.ModalDialog;
import org.labkey.test.components.html.Checkbox;
import org.labkey.test.components.html.Input;
import org.labkey.test.components.html.RadioButton;
import org.labkey.test.components.html.SelectWrapper;
import org.labkey.test.params.FieldDefinition;
import org.labkey.test.util.LabKeyExpectedConditions;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.SearchContext;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.FluentWait;
import org.openqa.selenium.support.ui.Select;

import java.time.Duration;
import java.util.List;

import static org.junit.Assert.assertTrue;
import static org.labkey.test.WebDriverWrapper.WAIT_FOR_JAVASCRIPT;

public class DomainFieldRow extends WebDriverComponent<DomainFieldRow.ElementCache>
{
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

    /**
     * selects the field data type.  Note: after the field is initially created, the select will be disabled
     *
     * @param columnType
     * @return
     */
    public DomainFieldRow setType(FieldDefinition.ColumnType columnType)
    {
        elementCache().fieldTypeSelectInput.selectByVisibleText(columnType.getLabel());
        return this;
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
        for(FieldDefinition.ColumnType ct : FieldDefinition.ColumnType.values())
        {
            if(ct.getLabel().equalsIgnoreCase(typeString))
                return ct;
        }
        return null;
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

    public String detailsMessage()
    {
        return elementCache().fieldDetailsMessage.getText();
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

    public AdvancedSettingsDialog clickAdvancedSettings()
    {
        expand();
        getWrapper().waitFor(() -> elementCache().advancedSettingsBtn.isEnabled(),
                "the Advanced Settings button did not become enabled", 5000);
        int trycount = 0;
        do
        {
            getWrapper().log("clicking advanced settings button try=["+trycount+"]");
            elementCache().advancedSettingsBtn.click();
            getWrapper().shortWait().until(LabKeyExpectedConditions.animationIsDone(getComponentElement()));
            trycount++;
            assertTrue("advanced settings dialog did not appear in time",trycount < 4);
        }while (!Locator.tagWithClass("div", "modal-backdrop").existsIn(getDriver()));

        return new AdvancedSettingsDialog(this);
    }

    public DomainFieldRow expand()
    {
        if (!isExpanded())
        {
            getWrapper().shortWait().until(ExpectedConditions.elementToBeClickable(elementCache().expandToggle));

            for (int i=0; i < 3; i++)
            {
                elementCache().expandToggle.click();
                getWrapper().shortWait().until(LabKeyExpectedConditions.animationIsDone(getComponentElement())); // wait for transition to happen
                if (getWrapper().waitFor(() -> isExpanded(), 1000))
                    break;
            }
            getWrapper().waitFor(() -> isExpanded(),
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
            getWrapper().waitFor(() -> elementCache().expandToggleLoc.existsIn(this),
                    "the field row did not collapse", 1500);
        }
        return this;
    }

    /**
     * indicates that the field has been added, the user will need to save changes to persist.
     * New fields can have their type changed
     *
     * @return
     */
    public boolean isNewField()
    {
        return Locator.tagWithAttributeContaining("span", "id", "domainpropertiesrow-details")
                .withText("New Field").existsIn(this);
    }

    /**
     * indicates that the field has been edited (but is not new). The user will need to save changes to persist
     *
     * @return
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
        getWrapper().setFormElement(elementCache().descriptionTextArea, description);
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

    public DomainFieldRow setScaleType(PropertiesEditor.ScaleType scaleType)
    {
        expand();
        elementCache().defaultScaleTypeSelect.selectByVisibleText(scaleType.getText());
        return this;
    }

    public PropertiesEditor.ScaleType getScaleType(String scaleType)
    {
        expand();
        String scaleTypeString = getWrapper().getFormElement(elementCache().defaultScaleTypeSelect.getWrappedElement());
        return Enum.valueOf(PropertiesEditor.ScaleType.class, scaleTypeString.toUpperCase());
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
        getWrapper().waitFor(() -> !isCharCountDisabled(),
                "character count input did not become enabled in time", 1000);
        elementCache().charScaleInput.setValue(strCharCount);
        return this;
    }

    public boolean isCharCountDisabled()
    {
        return elementCache().charScaleInput.getComponentElement().getAttribute("disabled") != null;
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
        String tableType = lookupInfo.getTableType();
        if (tableType == null)
            throw new IllegalArgumentException("No lookup type specified for " + lookupInfo.getTable());
        setFromTargetTable(lookupInfo.getTable() + " (" + tableType.substring(0,1).toUpperCase() + tableType.substring(1) + ")");
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

    public DomainFieldRow setLookupValidatorEnabled(boolean checked)
    {
        expand();
        elementCache().getLookupValidatorEnabledCheckbox().set(checked);
        return this;
    }
    public boolean getLookupValidatorEnabled()
    {
        expand();
        return elementCache().getLookupValidatorEnabledCheckbox().get();
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
                .showOnInsertView(checked)
                .apply();
        return this;
    }

    public DomainFieldRow showFieldOnUpdateView(boolean checked)
    {
        clickAdvancedSettings()
                .showOnUpdateView(checked)
                .apply();
        return this;
    }

    public DomainFieldRow setPHILevel(PropertiesEditor.PhiSelectType phiLevel)
    {
        clickAdvancedSettings()
                .setPHILevel(phiLevel)
                .apply();
        return this;
    }

    public DomainFieldRow setDateShift(boolean shift)
    {
        clickAdvancedSettings()
                .enableExcludeDateShifting(shift)
                .apply();
        return this;
    }

    public DomainFieldRow setMeasure(boolean checked)
    {
        clickAdvancedSettings()
                .enableMeasure(checked)
                .apply();
        return this;
    }

    public DomainFieldRow setDimension(boolean checked)
    {
        clickAdvancedSettings()
                .enableDimension(checked)
                .apply();
        return this;
    }

    public DomainFieldRow setRecommendedVariable(boolean checked)
    {
        clickAdvancedSettings()
                .enableRecommendedVariable(checked)
                .apply();
        return this;
    }

    public DomainFieldRow setMissingValue(boolean checked)
    {
        clickAdvancedSettings()
                .enableMissingValue(checked)
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
        getWrapper().waitFor(()-> this.hasFieldError(), WAIT_FOR_JAVASCRIPT);
        return this;
    }

    public boolean hasFieldWarning()
    {
        return getComponentElement().getAttribute("class").contains("domain-row-border-warning");
    }

    public DomainFieldRow waitForWarning()
    {
        getWrapper().waitFor(()-> this.hasFieldWarning(), WAIT_FOR_JAVASCRIPT);
        return this;
    }

    // conditional formatting and validation options

    /**
     * appends the supplied validators to any existing ones (including any empty default ones that might exist)
     * @param validators
     * @return
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
     * @param validators
     * @return
     */
    public DomainFieldRow setRangeValidators(List<FieldDefinition.RangeValidator> validators)
    {
        RangeValidatorDialog dialog = clickRangeButton();
        for (int i =0; i< validators.size(); i++)
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
     * @param validators
     * @return
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

    public DomainFieldRow setRegularExpressions(List<FieldDefinition.RegExValidator> validators)
    {
        RegexValidatorDialog dialog = clickRegexButton();
        for (int i =0; i< validators.size(); i++)
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

    public static class DomainFieldRowFinder extends WebDriverComponentFinder<DomainFieldRow, DomainFieldRowFinder>
    {
        private final Locator.XPathLocator _baseLocator = Locator.tagWithClassContaining("div", "domain-field-row");
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
        public Input fieldNameInput = new Input(Locator.tagWithAttributeContaining("input", "id", "domainpropertiesrow-name-")
                .findWhenNeeded(this), getDriver());
        public Select fieldTypeSelectInput = SelectWrapper.Select(Locator.tagWithAttributeContaining("select", "id", "domainpropertiesrow-type-"))
                .findWhenNeeded(this);
        public Checkbox fieldRequiredCheckbox = new Checkbox(Locator.tagWithAttributeContaining("input", "id", "domainpropertiesrow-required-")
                .findWhenNeeded(this));

        public WebElement fieldDetailsMessage = Locator.css(".domain-field-details, .domain-field-details-expanded")
                .findWhenNeeded(this);


        public Locator expandToggleLoc = Locator.tagWithClass("div", "field-icon")
                .child(Locator.tagWithClassContaining("svg", "fa-plus-square"));
        public Locator collapseToggleLoc = Locator.tagWithClass("div", "field-icon")
                .child(Locator.tagWithClassContaining("svg",  "fa-minus-square"));
        public WebElement expandToggle = expandToggleLoc.findWhenNeeded(this);

        public Locator removeFieldLoc = Locator.tagWithClass("span", "field-icon")
                .child(Locator.tagWithClassContaining("svg", "domain-field-delete-icon"));
        public WebElement removeField = removeFieldLoc.findWhenNeeded(this);

        // controls revealed when expanded
        public WebElement advancedSettingsBtn = Locator.button("Advanced Settings")      // not enabled for now, placeholder
                .refindWhenNeeded(this).withTimeout(WAIT_FOR_JAVASCRIPT);
        public WebElement collapseToggle = collapseToggleLoc.refindWhenNeeded(this).withTimeout(WAIT_FOR_JAVASCRIPT);


        // common field options
        public WebElement descriptionTextArea = Locator.tagWithAttributeContaining("textarea", "id", "domainpropertiesrow-description-")
                .refindWhenNeeded(this);
        public Input labelInput = new Input(Locator.tagWithAttributeContaining("input", "id", "domainpropertiesrow-label-")
                .refindWhenNeeded(this), getDriver());
        public Input importAliasesInput = new Input(Locator.tagWithAttributeContaining("input", "id", "domainpropertiesrow-importAliases-")
                .refindWhenNeeded(this), getDriver());
        public Input urlInput = new Input(Locator.tagWithAttributeContaining("input", "id", "domainpropertiesrow-URL-")
                .refindWhenNeeded(this), getDriver());

        // numeric field options
        public Input numericFormatInput = new Input(Locator.tagWithAttributeContaining("input", "id", "domainpropertiesrow-format")
                .refindWhenNeeded(this), getDriver());
        public Select defaultScaleTypeSelect = SelectWrapper.Select(Locator.name("domainpropertiesrow-defaultScale"))
                .findWhenNeeded(this);

        // text field options
        public RadioButton allowMaxCharCountRadio = new RadioButton(Locator.tagWithAttributeContaining("input", "id", "domainpropertiesrow-maxLength-")
                .refindWhenNeeded(this));
        public RadioButton setCharCountRadio = new RadioButton(Locator.tagWithAttributeContaining("input", "id", "domainpropertiesrow-customLength-")
                .refindWhenNeeded(this));
        public Input charScaleInput = new Input(Locator.tagWithAttributeContaining("input", "id", "domainpropertiesrow-scale")
                .refindWhenNeeded(this), getDriver());

        public Locator.XPathLocator getCharScaleInputLocForRow(int rowIndex)
        {
            return Locator.tagWithId("input", "domainpropertiesrow-scale-0-" + rowIndex);
        }

        // date field options
        public Input dateFormatInput = new Input(Locator.tagWithAttributeContaining("input", "id", "domainpropertiesrow-format")
                .refindWhenNeeded(this), getDriver());

        //lookup field options
        public Select lookupContainerSelect = SelectWrapper.Select(Locator.name("domainpropertiesrow-lookupContainer"))
                .findWhenNeeded(this);

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
                    Locator.button("Add Format"),  Locator.button("Edit Formats"));
        }
    }
}
