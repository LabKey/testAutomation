package org.labkey.test.components;

import org.labkey.test.Locator;
import org.labkey.test.components.bootstrap.AdvancedSettingsDialog;
import org.labkey.test.components.bootstrap.ModalDialog;
import org.labkey.test.components.html.Checkbox;
import org.labkey.test.components.html.Input;
import org.labkey.test.components.html.RadioButton;
import org.labkey.test.params.FieldDefinition;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import static org.labkey.test.WebDriverWrapper.WAIT_FOR_JAVASCRIPT;

public class DomainFieldRow extends WebDriverComponent<DomainFieldRow.ElementCache>
{
    final WebElement _el;
    final WebDriver _driver;

    public DomainFieldRow(WebElement element, WebDriver driver)
    {
        _el = element;
        _driver = driver;
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
     * @param columnType
     * @return
     */
    public DomainFieldRow setType(FieldDefinition.ColumnType columnType)
    {
        getWrapper().setFormElement(elementCache().fieldTypeSelectInput, columnType.toString());
        return this;
    }
    public DomainFieldRow setType(String columnType)
    {
        getWrapper().setFormElement(elementCache().fieldTypeSelectInput, columnType);
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
        return elementCache().domainFieldDetailsSpan.getText();
    }

    public int getIndex()
    {
        String itemIndexAttribute = getComponentElement().getAttribute("tabindex");
        return Integer.parseInt(itemIndexAttribute);
    }

    public boolean isExpanded()
    {
        return getComponentElement().getAttribute("class").contains("domain-row-expanded");
    }

    /**
     * begins the process of removing the field
     * @return a modal dialog prompting the user to confirm or cancel deletion
     */
    public ModalDialog clickRemoveField()
    {
        expand();
        elementCache().removeFieldBtn.click();

        ModalDialog confirmDeletionDlg = new ModalDialog.ModalDialogFinder(getDriver()).withTitle("Confirm Field Deletion")
                .waitFor();
        return  confirmDeletionDlg;
    }

    public AdvancedSettingsDialog clickAdvancedSettings()
    {
        expand();
        getWrapper().waitFor(()-> elementCache().advancedSettingsBtn.isEnabled(),
                "the Advanced Settings button did not become enabled", 1500);
        elementCache().advancedSettingsBtn.click();
        return new AdvancedSettingsDialog(getDriver());
    }

    public DomainFieldRow expand()
    {
        if (!isExpanded())
        {
            elementCache().expandToggle.click();
            getWrapper().waitFor(()-> isExpanded(),
                     "the field row did not become expanded",1500);
        }
        return this;
    }

    public DomainFieldRow collapse()
    {
        if (isExpanded())
        {
            elementCache().expandToggle.click();
            getWrapper().waitFor(()-> !isExpanded(),
                    "the field row did not collapse",1500);
        }
        return this;
    }

    /**
     * indicates that the field has been added, the user will need to save changes to persist.
     * New fields can have their type changed
     * @return
     */
    public boolean isNewField()
    {
        return Locator.tagWithAttributeContaining("span", "id", "domainpropertiesrow-details")
                .withText("New Field").existsIn(this);
    }

    /**
     * indicates that the field has been edited (but is not new). The user will need to save changes to persist
     * @return
     */
    public boolean isEditedField()
    {
        return Locator.tagWithAttributeContaining("span", "id", "domainpropertiesrow-details")
                .withText("Updated").existsIn(this);
    }

    //
    // common field options

    public DomainFieldRow setDescription(String description)
    {
        expand();
        getWrapper().setFormElement(elementCache().descriptionTextArea, description);
        return this;
    }
    public String getDescription()
    {
        expand();
        return getWrapper().getFormElement(elementCache().descriptionTextArea);
    }

    public DomainFieldRow setLabel(String label)
    {
        expand();
        elementCache().labelInput.setValue(label);
        return this;
    }
    public String getLabel()
    {
        expand();
        return elementCache().labelInput.getValue();
    }

    public DomainFieldRow setImportAliases(String aliases)
    {
        expand();
        elementCache().importAliasesInput.setValue(aliases);
        return this;
    }
    public String getImportAliases()
    {
        expand();
        return elementCache().importAliasesInput.getValue();
    }

    public DomainFieldRow setUrl(String url)
    {
        expand();
        elementCache().urlInput.setValue(url);
        return this;
    }
    public String getUrl()
    {
        expand();
        return elementCache().urlInput.getValue();
    }

    //
    // numeric field options.

    public DomainFieldRow setNumberFormat(String format)
    {
        expand();
        elementCache().numericFormatInput.set(format);
        return this;
    }
    public String getNumberFormat()
    {
        expand();
        return elementCache().numericFormatInput.getValue();
    }

    public DomainFieldRow setScaleType(PropertiesEditor.ScaleType scaleType)
    {
        expand();
        getWrapper().setFormElement(elementCache().defaultScaleTypeSelect, scaleType.toString());
        return this;
    }
    public PropertiesEditor.ScaleType getScaleType(String scaleType)
    {
        expand();
        String scaleTypeString = getWrapper().getFormElement(elementCache().defaultScaleTypeSelect);
        return Enum.valueOf(PropertiesEditor.ScaleType.class, scaleTypeString);
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
        return  elementCache().allowMaxCharCountRadio.isChecked();
    }
    public DomainFieldRow setCharCount(int maxCharCount)
    {
        expand();
        String strCharCount = Integer.toString(maxCharCount);
        elementCache().setCharCountRadio.set(true);
        getWrapper().waitFor(()-> elementCache().charScaleInput.getComponentElement().getAttribute("disabled")==null,
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

    public DomainFieldRow setDateFormat(String formatString)
    {
        expand();
        elementCache().dateFormatInput.setValue(formatString);
        return this;
    }
    public String getDateFormat()
    {
        expand();
        return elementCache().dateFormatInput.getValue();
    }
    public DomainFieldRow setDateShift(boolean shift)
    {
        expand();
        elementCache().dateShiftBox.set(shift);
        return this;
    }
    public boolean getDateShift()
    {
        expand();
        return elementCache().dateShiftBox.get();
    }

    // advanced settings

    public DomainFieldRow showFieldOnDefaultView(boolean checked)
    {
        clickAdvancedSettings()
                .showInDefaultView(checked)
                .dismiss("Apply");
        return this;
    }

    public DomainFieldRow showFieldOnInsertView(boolean checked)
    {
        clickAdvancedSettings()
                .showOnInsertView(checked)
                .dismiss("Apply");
        return this;
    }

    public DomainFieldRow showFieldOnUpdateView(boolean checked)
    {
        clickAdvancedSettings()
                .showOnUpdateView(checked)
                .dismiss("Apply");
        return this;
    }

    public DomainFieldRow setPHILevel(String phiLevel)
    {
        clickAdvancedSettings()
                .setPHILevel(phiLevel)
                .dismiss("Apply");
        return this;
    }

    // error and warning

    public boolean hasFieldError()
    {
        return getComponentElement().getAttribute("class").contains("domain-field-row-error");
    }

    public boolean hasFieldWarning()
    {
        return getComponentElement().getAttribute("class").contains("domain-field-row-warning");
    }

    protected ElementCache newElementCache()
    {
        return new ElementCache();
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

        public WebElement domainFieldDetailsSpan = Locator.tagWithClass("span", "domain-field-details")
                .findWhenNeeded(this);

        public WebElement expandToggle = Locator.tagWithClass("div", "domain-field-icon")
                .child(Locator.tagWithAttribute("svg", "data-icon", "pencil-alt"))
                .findWhenNeeded(this);

        // controls revealed when expanded
        public WebElement removeFieldBtn = Locator.tagWithAttributeContaining("button", "id", "domainpropertiesrow-delete-")
                .refindWhenNeeded(this).withTimeout(WAIT_FOR_JAVASCRIPT);
        public WebElement advancedSettingsBtn = Locator.tagWithText("button", "Advanced Settings")      // not enabled for now, placeholder
                .refindWhenNeeded(this).withTimeout(WAIT_FOR_JAVASCRIPT);

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
        public WebElement defaultScaleTypeSelect = Locator.tagWithAttributeContaining("select", "id", "domainpropertiesrow-defaultScale-")
                .findWhenNeeded(this);

        // text field options
        public RadioButton allowMaxCharCountRadio = new RadioButton(Locator.tagWithAttributeContaining("input","id", "domainpropertiesrow-maxLength-")
                .refindWhenNeeded(this));
        public RadioButton setCharCountRadio = new RadioButton(Locator.tagWithAttributeContaining("input","id", "domainpropertiesrow-customLength-")
                .refindWhenNeeded(this));
        public Input charScaleInput = new Input(Locator.tagWithAttributeContaining("input", "id", "domainpropertiesrow-scale")
                .refindWhenNeeded(this), getDriver());

        // date field options
        public Input dateFormatInput = new Input(Locator.tagWithAttributeContaining("input", "id", "domainpropertiesrow-format")
                .refindWhenNeeded(this), getDriver());
        public Checkbox dateShiftBox = new Checkbox(Locator.tagWithAttributeContaining("input", "id", "domainpropertiesrow-excludeFromShifting")
                .refindWhenNeeded(this));
    }


    public static class DomainFieldRowFinder extends WebDriverComponentFinder<DomainFieldRow, DomainFieldRowFinder>
    {
        private final Locator.XPathLocator _baseLocator = Locator.tagWithClassContaining("div", "domain-field-row");
        private String _title = null;

        public DomainFieldRowFinder(WebDriver driver)
        {
            super(driver);
        }

        public DomainFieldRowFinder withTitle(String title)
        {
            _title = title;
            return this;
        }

        @Override
        protected DomainFieldRow construct(WebElement el, WebDriver driver)
        {
            return new DomainFieldRow(el, driver);
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
}
