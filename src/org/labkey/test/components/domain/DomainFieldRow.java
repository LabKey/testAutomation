package org.labkey.test.components.domain;

import org.apache.commons.lang3.StringUtils;
import org.labkey.test.Locator;
import org.labkey.test.components.PropertiesEditor;
import org.labkey.test.components.WebDriverComponent;
import org.labkey.test.components.bootstrap.ModalDialog;
import org.labkey.test.components.html.Checkbox;
import org.labkey.test.components.html.Input;
import org.labkey.test.components.html.RadioButton;
import org.labkey.test.components.html.SelectWrapper;
import org.labkey.test.params.FieldDefinition;
import org.openqa.selenium.SearchContext;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.FluentWait;
import org.openqa.selenium.support.ui.Select;

import java.time.Duration;
import java.util.List;
import java.util.NoSuchElementException;

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
        setType(columnType.getLabel());
        return this;
    }

    public WebElement typeInput()
    {
        return elementCache().fieldTypeSelectInput;
    }

    public FieldDefinition.ColumnType getType()
    {
        String typeString = getWrapper().getFormElement(elementCache().fieldTypeSelectInput);
        return Enum.valueOf(FieldDefinition.ColumnType.class, typeString);
    }

    private DomainFieldRow setType(String columnType)
    {
        getWrapper().setFormElement(elementCache().fieldTypeSelectInput, columnType);
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
     * begins the process of removing the field
     *
     * @return a modal dialog prompting the user to confirm or cancel deletion
     */
    public ModalDialog clickRemoveField()
    {
        expand();
        getWrapper().shortWait().until(ExpectedConditions.elementToBeClickable(elementCache().removeFieldBtn));
        getWrapper().mouseOver(elementCache().removeFieldBtn);

        // re-try until the dialog appears or until attempts are exhausted
        for (int i=0; i < 3; i++)
        {
            try
            {
                elementCache().removeFieldBtn.click();
                new ModalDialog.ModalDialogFinder(getDriver())
                        .withTitle("Confirm Field Deletion").timeout(1000).waitFor();
                break;
            }catch (NoSuchElementException notFound) {}
        }

        ModalDialog confirmDeletionDlg = new ModalDialog.ModalDialogFinder(getDriver())
                .withTitle("Confirm Field Deletion").find();
        return confirmDeletionDlg;
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
            getWrapper().sleep(250);
            trycount++;
            assertTrue("advanced settings dialog did not appear in time",trycount < 4);
        }while (!Locator.tagWithClass("div", "modal-backdrop").existsIn(getDriver()));

        return new AdvancedSettingsDialog(this, getDriver());
    }

    public DomainFieldRow expand()
    {
        if (!isExpanded())
        {
            getWrapper().shortWait().until(ExpectedConditions.elementToBeClickable(elementCache().expandToggle));

            for (int i=0; i < 3; i++)
            {
                elementCache().expandToggle.click();
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
        getWrapper().waitFor(() -> elementCache().charScaleInput.getComponentElement().getAttribute("disabled") == null,
                "character count input did not become enabled in time", 1000);
        elementCache().charScaleInput.setValue(strCharCount);
        return this;
    }

    public boolean isCustomCharSelected()
    {
        expand();
        return elementCache().setCharCountRadio.isChecked();
    }

    public Integer customCharCount()
    {
        expand();
        return Integer.parseInt(elementCache().charScaleInput.getValue());
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
        setFromTargetTable(lookupInfo.getTable() + " (" + tableType + ")");
        return this;
    }

    public String getFromFolder()
    {
        expand();
        return elementCache().fromFolderInput.getFirstSelectedOption().getText();
    }

    public DomainFieldRow setFromFolder(String containerPath)
    {
        expand();
        if (StringUtils.isEmpty(containerPath) || containerPath.equals("Current Folder") || containerPath.equals("Current Project"))
        {
            containerPath = "";
        }
        elementCache().fromFolderInput.selectByValue(containerPath);

        return this;
    }

    public String getFromSchema()
    {
        expand();
        return elementCache().getFromSchemaInput().getFirstSelectedOption().getText();
    }

    public DomainFieldRow setFromSchema(String schemaName)
    {
        expand();
        getWrapper().waitFor(()-> elementCache().getFromSchemaInput().getOptions().stream().anyMatch(a-> a.getText().equals(schemaName)),
                "the select option [" + schemaName + "] did not appear in time", 3000);
        elementCache().getFromSchemaInput().selectByVisibleText(schemaName);
        return this;
    }

    public String getFromTargetTable()
    {
        expand();
        return elementCache().getFromTargetTableInput().getFirstSelectedOption().getText();
    }

    public DomainFieldRow setFromTargetTable(String targetTable)
    {
        expand();
        // give the option some time to appear before attempting to select it
        getWrapper().waitFor(()-> elementCache().getFromTargetTableInput().getOptions().stream().anyMatch(a-> a.getText().equals(targetTable)),
                "the select option [" + targetTable + "] did not appear in time", 5000);
        elementCache().getFromTargetTableInput().selectByVisibleText(targetTable);
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

    public boolean hasFieldWarning()
    {
        return getComponentElement().getAttribute("class").contains("domain-row-border-warning");
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
        return new RangeValidatorDialog(this, getDriver());
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
        return new RegexValidatorDialog(this, getDriver());
    }

    public ConditionalFormatDialog clickConditionalFormatButton()
    {
        expand();
        getWrapper().shortWait().until(ExpectedConditions.elementToBeClickable(elementCache().conditionalFormatButton()));
        elementCache().conditionalFormatButton().click();
        return new ConditionalFormatDialog(this, getDriver());
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
        public WebElement fieldTypeSelectInput = Locator.tagWithAttributeContaining("select", "id", "domainpropertiesrow-type-")
                .findWhenNeeded(this);
        public Checkbox fieldRequiredCheckbox = new Checkbox(Locator.tagWithAttributeContaining("input", "id", "domainpropertiesrow-required-")
                .findWhenNeeded(this));

        public WebElement fieldDetailsMessage = Locator.css(".domain-field-details, .domain-field-details-expanded")
                .findWhenNeeded(this);


        public Locator expandToggleLoc = Locator.tagWithClass("div", "field-icon")
                .child(Locator.tagWithAttribute("svg", "data-icon", "plus-square"));
        public Locator collapseToggleLoc = Locator.tagWithAttribute("svg", "data-icon", "minus-square");

        public WebElement expandToggle = expandToggleLoc.findWhenNeeded(this);


        // controls revealed when expanded
        public WebElement removeFieldBtn = Locator.tagWithAttributeContaining("button", "id", "domainpropertiesrow-delete-")
                .refindWhenNeeded(this).withTimeout(WAIT_FOR_JAVASCRIPT);
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

        // date field options
        public Input dateFormatInput = new Input(Locator.tagWithAttributeContaining("input", "id", "domainpropertiesrow-format")
                .refindWhenNeeded(this), getDriver());

        //lookup field options
        public Select fromFolderInput = SelectWrapper.Select(Locator.name("domainpropertiesrow-lookupContainer"))
                .findWhenNeeded(this);

        public Select getFromSchemaInput()
        {
            Select select = SelectWrapper.Select(Locator.name("domainpropertiesrow-lookupSchema")).find(this);
            getWrapper().waitFor(()-> select.getOptions().size() > 0 && !select.getOptions().get(0).getText().equals("Loading..."),
                    "select did not have options in the expected time", WAIT_FOR_JAVASCRIPT);
            return select;
        }

        public Select getFromTargetTableInput()
        {
            Select select = SelectWrapper.Select(Locator.name("domainpropertiesrow-lookupQueryValue")).find(this);
            getWrapper().waitFor(()-> select.getOptions().size() > 0 && !select.getOptions().get(0).getText().equals("Loading..."),
                    "select did not have options in the expected time", WAIT_FOR_JAVASCRIPT);
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
