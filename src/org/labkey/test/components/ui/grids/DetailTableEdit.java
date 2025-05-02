package org.labkey.test.components.ui.grids;

import org.junit.Assert;
import org.labkey.remoteapi.CommandException;
import org.labkey.test.BootstrapLocators;
import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.components.Component;
import org.labkey.test.components.WebDriverComponent;
import org.labkey.test.components.html.Checkbox;
import org.labkey.test.components.html.Input;
import org.labkey.test.components.react.FilteringReactSelect;
import org.labkey.test.components.react.ReactDateTimePicker;
import org.labkey.test.components.react.ReactSelect;
import org.labkey.test.components.ui.files.FileUploadField;
import org.labkey.test.params.FieldDefinition;
import org.labkey.test.util.AuditLogHelper;
import org.labkey.test.util.EscapeUtil;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.labkey.test.WebDriverWrapper.WAIT_FOR_JAVASCRIPT;

/**
 * automates /QueryModel/DetailPanel.tsx in its editable mode
 */
public class DetailTableEdit extends WebDriverComponent<DetailTableEdit.ElementCache>
{
    private final WebElement _formElement;
    private final WebDriver _driver;
    private String _title;
    private int _readyTimeout = WAIT_FOR_JAVASCRIPT;
    protected int _changeCounter = 0;

    protected DetailTableEdit(WebElement formElement, WebDriver driver)
    {
        _formElement = formElement;
        _driver = driver;
    }

    @Override
    protected WebDriver getDriver()
    {
        return _driver;
    }

    @Override
    public WebElement getComponentElement()
    {
        return _formElement;
    }

    public String getTitle()
    {
        if (_title == null)
            _title = elementCache().header.getText();
        return _title;
    }

    public DetailTableEdit setReadyTimeout(int readyTimeout)
    {
        _readyTimeout = readyTimeout;
        return this;
    }

    /**
     * If for some reason your test set a field but it actually didn't change the value, you can use this helper
     * to set the change counter to a specific value.
     */
    public DetailTableEdit adjustChangeCounter(int change)
    {
        _changeCounter = _changeCounter + change;
        return this;
    }

    public boolean isFieldPresent(String fieldLabel)
    {
        return elementCache().valueCellWithLabel(fieldLabel) != null;
    }
    /**
     * Check to see if a field is editable. Could be state dependent, that is it returns false if the field is
     * loading but if checked later could return true.
     *
     * @param fieldLabel The label of the field to check.
     * @return True if it is false otherwise.
     **/
    public boolean isFieldEditable(String fieldLabel)
    {
        // TODO Could put a check here to see if a field is loading then return false, or wait.
        WebElement fieldValueElement = elementCache().valueCellWithLabel(fieldLabel);
        return isEditableField(fieldValueElement);
    }

    private boolean isEditableField(WebElement element)
    {
        // If the div does not have the class value of 'field__un-editable' then it is an editable field.
        return Locator.css("div:not(.field__un-editable)").findOptionalElement(element).isPresent();
    }

    /**
     * Get the value of a read only field.
     *
     * @param fieldLabel The label of the field to get.
     * @return The value in the field.
     **/
    public String getReadOnlyField(String fieldLabel)
    {
        WebElement fieldValueElement = elementCache().valueCellWithLabel(fieldLabel);
        return fieldValueElement.findElement(By.xpath("./div/*")).getText();
    }

    /**
     * Get the value of a text field.
     *
     * @param fieldLabel The label of the field to get.
     * @return The value in the field.
     **/
    public String getTextField(String fieldLabel)
    {
        WebElement fieldValueElement = elementCache().valueCellWithLabel(fieldLabel);
        WebElement textElement = fieldValueElement.findElement(By.xpath("./div/div/*"));
        if(textElement.getTagName().equalsIgnoreCase("textarea"))
            return textElement.getText();
        else
            return textElement.getAttribute("value");
    }

    /**
     * Set a text field.
     *
     * @param fieldLabel The label of the field to set.
     * @param value The value to set the field to.
     * @return A reference to this editable detail table.
     **/
    public DetailTableEdit setTextField(String fieldLabel, String value)
    {
        if(isFieldEditable(fieldLabel))
        {
            WebElement fieldValueElement = elementCache().valueCellWithLabel(fieldLabel);

            WebElement editableElement = fieldValueElement.findElement(By.xpath("./div/div/*"));
            String elementType = editableElement.getTagName().toLowerCase().trim();

            switch(elementType)
            {
                case "textarea":
                case "input":
                    editableElement.clear();
                    WebDriverWrapper.waitFor(()->editableElement.getText().isEmpty(), 500);
                    editableElement.sendKeys(value);
                    break;
                default:
                    throw new NoSuchElementException("This doesn't look like an 'input' or 'textarea' element, are you sure you are calling the correct method?");
            }
        }
        else
        {
            throw new IllegalArgumentException("Field with label '" + fieldLabel + "' is read-only. This field can not be set.");
        }

        _changeCounter++;
        return this;
    }

    public DetailTableEdit setInputByFieldName(String fieldName, String value)
    {
        Locator inputloc = Locator.tagWithClass("input", "form-control")
            .withAttribute("name", fieldName);
        Input input = Input.Input(inputloc,
                getDriver()).waitFor();
        input.set(value);
        _changeCounter++;
        return this;
    }

    public DetailTableEdit setTextareaByFieldName(String fieldName, String value)
    {
        Locator inputloc = Locator.tagWithClass("textarea", "form-control")
                .withAttribute("name", fieldName);
        Input input = Input.Input(inputloc,
                getDriver()).waitFor();
        input.set(value);
        _changeCounter++;
        return this;
    }

    /**
     * Get the value of a boolean field.
     *
     * @param fieldLabel The label of the field to get.
     * @return The value of the field.
     **/
    public boolean getBooleanField(String fieldLabel)
    {
        // The text used in the field label and the value of the name attribute in the checkbox don't always have the same case.
        WebElement editableElement = Locator.tag("input").findElement(elementCache().valueCellWithLabel(fieldLabel));
        String elementType = editableElement.getDomAttribute("type").toLowerCase().trim();

        Assert.assertEquals(String.format("Field '%s' is not a checkbox. Cannot be get true/false value.", fieldLabel), "checkbox", elementType);

        return new Checkbox(editableElement).isChecked();
    }

    /**
     * Set a boolean field (a checkbox).
     *
     * @param fieldLabel The label of the field to set.
     * @param value True will check it, false will uncheck it.
     * @return A reference to this editable detail table.
     **/
    public DetailTableEdit setBooleanField(String fieldLabel, boolean value)
    {

        WebElement fieldValueElement = elementCache().valueCellWithLabel(fieldLabel);
        Assert.assertTrue(String.format("Field '%s' is not editable and cannot be set.", fieldLabel), isEditableField(fieldValueElement));
        getWrapper().scrollIntoView(fieldValueElement);

        WebElement editableElement = fieldValueElement.findElement(By.xpath("./div/div/input"));
        String elementType = editableElement.getDomAttribute("type").toLowerCase().trim();

        Assert.assertEquals(String.format("Field '%s' is not a checkbox. Cannot be set to true/false.", fieldLabel), "checkbox", elementType);

        Checkbox checkbox = new Checkbox(editableElement);

        checkbox.set(value);

        _changeCounter++;
        return this;
    }

    /**
     * Get the value of an int field. You could also call getTextField
     *
     * @param fieldLabel The label of the field to get.
     * @return The value of the field as an int.
     **/
    public int getIntField(String fieldLabel)
    {
        return Integer.getInteger(getTextField(fieldLabel));
    }

    /**
     * Set an int field.
     *
     * @param fieldLabel The label of the field to set.
     * @param value The int value to set the field to.
     * @return A reference to this editable detail table.
     **/
    public DetailTableEdit setIntField(String fieldLabel, int value)
    {
        return setTextField(fieldLabel, Integer.toString(value));
    }

    public FileUploadField getFileField(String fieldLabel)
    {
        return elementCache().fileField(fieldLabel);
    }

    public DetailTableEdit setFileField(String fieldLabel, File file)
    {
        getFileField(fieldLabel)
                .setFile(file);

        _changeCounter++;
        return this;
    }

    public DetailTableEdit removeFileField(String fieldLabel)
    {
        getFileField(fieldLabel).removeFile();

        _changeCounter++;
        return this;
    }

    public boolean isFileFieldBlank(String fieldLabel)
    {
        return !getFileField(fieldLabel)
                .hasAttachedFile();
    }

    /**
     * Get the value of a select field.
     *
     * @param fieldLabel The label of the field to get.
     * @return The selected value.
     **/
    public String getSelectedValue(String fieldLabel)
    {
        FilteringReactSelect reactSelect = elementCache().findSelect(fieldLabel);
        return reactSelect.getValue();
    }

    /*
        This allows you to query a given select in the edit panel to see what options it offers
     */
    public List<String> getSelectOptions(String fieldLabel)
    {
        FilteringReactSelect reactSelect = elementCache().findSelect(fieldLabel);
        return reactSelect.getOptions();
    }

    /**
     * Select a single value from a select list.
     *
     * @param fieldLabel The label of the field to set.
     * @param selectValue The value to select from the list.
     * @return A reference to this editable detail table.
     **/
    public DetailTableEdit setSelectValue(String fieldLabel, String selectValue)
    {
        List<String> selection = Arrays.asList(selectValue);
        return setSelectValue(fieldLabel, selection);
    }

    public DetailTableEdit createSelectValue(String fieldLabel, String value)
    {
        WebElement container = Locator.tag("td").withAttribute("data-caption", fieldLabel).findElement(this);
        var select = ReactSelect.finder(getDriver()).waitFor(container);
        select.createValue(value);
        return this;
    }


    /**
     * Select multiple values from a select list.
     *
     * @param fieldLabel The label of the field to set.
     * @param selectValues The value to select from the list.
     * @return A reference to this editable detail table.
     **/
    public DetailTableEdit setSelectValue(String fieldLabel, List<String> selectValues)
    {
        FilteringReactSelect reactSelect = elementCache().findSelect(fieldLabel);
        selectValues.forEach(reactSelect::typeAheadSelect);
        _changeCounter++;
        return this;
    }

    /**
     * Clear a given select field.
     *
     * @param fieldLabel The label of the field to clear.
     * @return A reference to this editable detail table.
     **/
    public DetailTableEdit clearSelectValue(String fieldLabel)
    {
        return clearSelectValue(fieldLabel, true, true);
    }

    /**
     * Clear a given select field
     * @param fieldLabel The label of the field to clear.
     * @param waitForSelection If true, wait for the select to have a selection before clearing it
     * @param assertSelection  If true, assert if no selection appears (note: does nothing if waitForSelection is not true)
     * @return
     */
    public DetailTableEdit clearSelectValue(String fieldLabel, boolean waitForSelection, boolean assertSelection)
    {
        var select = elementCache().findSelect(fieldLabel);
        if (waitForSelection)
        {
            if (assertSelection) {
                WebDriverWrapper.waitFor(() -> select.hasSelection(),
                        String.format("The %s select did not have any selection in time", fieldLabel), _readyTimeout);
            }
            else {
                WebDriverWrapper.waitFor(() -> select.hasSelection(), 1000);
            }
        }
        select.clearSelection();
        _changeCounter++;
        return this;
    }

    /**
     * Set a DateTime, Date or Time field.
     * @param fieldName The name of the field to set.
     * @param dateTime Will be used to determine what kind of field is being set and how to set it. If the parameter
     *                 is a LocalDateTime object then it is assumed that field is a DateTime field. If the parameter is
     *                 a LocalDate object then it is assumed to be a date-only field. And I think you can guess what
     *                 happens with a LocalTime object type. If the type is a string it is used as a literal value that
     *                 is typed into the field (no picker is used).
     * @return A reference to this DetailTableEdit object.
     */
    public DetailTableEdit setDateTimeField(String fieldName, Object dateTime)
    {
        ReactDateTimePicker dateTimePicker = getDateTimePicker(fieldName);
        if(dateTime instanceof LocalDateTime localDateTime)
        {
            dateTimePicker.select(localDateTime);
        }
        else if(dateTime instanceof LocalDate localDate)
        {
            dateTimePicker.selectDate(localDate);
        }
        else if(dateTime instanceof LocalTime localTime)
        {
            dateTimePicker.selectTime(localTime);
        }
        else if(dateTime instanceof String setValue)
        {
            dateTimePicker.set(setValue, true);
        }
        else
        {
            throw new IllegalArgumentException(
                    String.format("Unable to use type %s to set a DateTime, Date or Time field.", dateTime.getClass()));
        }

        _changeCounter++;
        return this;
    }

    public String getDateTimeField(String fieldName)
    {
        ReactDateTimePicker dateTimePicker = getDateTimePicker(fieldName);
        return dateTimePicker.get();
    }

    public void clearDateTimeField(String fieldName)
    {
        ReactDateTimePicker dateTimePicker = getDateTimePicker(fieldName);
        dateTimePicker.clear();
        _changeCounter++;
    }

    private ReactDateTimePicker getDateTimePicker(String fieldName)
    {
        return new ReactDateTimePicker.ReactDateTimeInputFinder(getDriver()).find(elementCache().valueCellWithName(fieldName));
    }

    // For use when the field is of an unknown type, as can occur in fuzz tests
    public void setDetails(FieldDefinition field, Object newValue)
    {
        if (newValue == null)
            return;

        if (field.getType() == FieldDefinition.ColumnType.TextChoice)
            setSelectValue(field.getLabel(), (List<String>) newValue);
        else if (field.getType() == FieldDefinition.ColumnType.Date || field.getType() == FieldDefinition.ColumnType.DateAndTime || field.getType() == FieldDefinition.ColumnType.Time)
            setDateTimeField(field.getName(), newValue);
        else if (field.getType() == FieldDefinition.ColumnType.Boolean)
            setBooleanField(field.getLabel(), (Boolean) newValue);
        else
            setTextField(field.getLabel(), String.valueOf(newValue));
    }

    /**
     * Get the field names shown on the form.
     *
     * @return A list of string with the displayed field names.
     */
    public List<String> getDisplayedfieldLabels()
    {
        return Locator.tagWithAttribute("td", "data-fieldkey").findElements(this)
                .stream().map(el -> el.getDomAttribute("data-caption")).collect(Collectors.toList());
    }

    private String getSourceTitle()
    {
        return getTitle().replace("Editing ", "");
    }

    /**
     * A validation message happens if a value of a particular field is out of bounds or incorrect in some other way.
     *
     * @return The text of the validation message or an empty string if there is none.
     */
    public String getValidationMessage()
    {
        if(elementCache().validationMsg.existsIn(this))
            return elementCache().validationMsg.findElement(getDriver()).getText();
        else
            return "";
    }

    public boolean isSaveButtonEnabled()
    {
        return elementCache().saveButton.isEnabled();
    }

    public DetailDataPanel clickSave()
    {
        return clickSave(false);
    }

    public DetailDataPanel clickSave(boolean skipChangeCounterCheck)
    {
        String title = getSourceTitle();
        var componentEl = getComponentElement();
        getWrapper().shortWait().until(ExpectedConditions.elementToBeClickable(elementCache().saveButton));
        elementCache().saveButton.click();

        // If save causes some update, wait until it is completed.
        getWrapper().longWait().withMessage("Update took too long to complete.")
                .until(ExpectedConditions.stalenessOf(elementCache().saveButton));

        // check for the expected number of Data Changes in the latest audit event records
        AuditLogHelper auditLogHelper = new AuditLogHelper(getWrapper());
        String auditEventName = auditLogHelper.getAuditEventNameFromURL();
        if (!skipChangeCounterCheck && auditEventName != null)
        {
            try
            {
                int changeCounter = auditLogHelper.isSourcesRoute() ? _changeCounter + 1 : _changeCounter; // Source updates include the name value in the diff (even when not changed)
                auditLogHelper.checkTimelineAuditEventDiffCountForLastTransaction(getWrapper().getCurrentContainerPath(), auditEventName, changeCounter, 1);
            }
            catch (CommandException | IOException e)
            {
                throw new RuntimeException(e);
            }
        }

        // ensure we don't find the current component; wait for it to become stale before searching
        getWrapper().shortWait().until(ExpectedConditions.stalenessOf(componentEl));

        return new DetailDataPanel.DetailDataPanelFinder(getDriver()).withTitle(title).waitFor();
    }

    public String clickSaveExpectingError()
    {
        elementCache().saveButton.click();
        WebElement errorBanner = BootstrapLocators.errorBanner.findWhenNeeded(this);
        WebDriverWrapper.waitFor(()->errorBanner.isDisplayed(),
                "No error message was shown.", 1_000);
        return errorBanner.getText();
    }

    public DetailDataPanel clickCancel()
    {
        String title = getSourceTitle();
        elementCache().cancelButton.click();
        return new DetailDataPanel.DetailDataPanelFinder(getDriver()).withTitle(title).waitFor();
    }

    public String clickCancelExpectingError()
    {
        elementCache().cancelButton.click();
        WebElement errorBanner = BootstrapLocators.errorBanner.findWhenNeeded(this);
        WebDriverWrapper.waitFor(()->errorBanner.isDisplayed(),
                "No error message was shown.", 1_000);
        return errorBanner.getText();
    }

    public void setActionComment(String comment)
    {
        elementCache().commentInput.sendKeys(comment);
    }

    public void clearActionComment()
    {
        elementCache().commentInput.clear();
    }

    @Override
    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    protected class ElementCache extends Component<?>.ElementCache
    {
        public ElementCache()
        {
            // Wait for selects to finish loading
            getWrapper().shortWait().until(ExpectedConditions
                    .invisibilityOfAllElements(Locator.byClass("select-input__loading-indicator").findElements(this)));
        }

        public WebElement header = Locator.tagWithClass("div", "panel-heading")
                .findWhenNeeded(this);
        public WebElement editPanel = Locator.tagWithClass("div", "detail__editing")
                .findWhenNeeded(this);

        public WebElement valueCellWithLabel(String label)
        {
            return Locator.tagWithAttribute("td", "data-caption", label).findElementOrNull(editPanel);
        }

        public WebElement valueCellWithName(String fieldName)
        {
            return Locator.tagWithAttribute("td", "data-fieldkey", EscapeUtil.fieldKeyEncodePart(fieldName).toLowerCase()).findElement(editPanel);
        }

        public FileUploadField fileField(String label)
        {
            return new FileUploadField(valueCellWithLabel(label), getDriver());
        }

        public Locator validationMsg = Locator.tagWithClass("span", "validation-message");

        public WebElement saveButton = Locator.tagWithAttribute("button", "type", "submit")
                .findWhenNeeded(this);
        public WebElement cancelButton = Locator.tagWithAttribute("button", "type", "button")
                .findWhenNeeded(this);

        public WebElement commentInput = Locator.tagWithId("textarea", "actionComments").refindWhenNeeded(getDriver());

        public FilteringReactSelect findSelect(String fieldLabel)
        {
            WebElement container = Locator.tag("td").withAttribute("data-caption", fieldLabel).findElement(this);
            return FilteringReactSelect.finder(_driver).timeout(_readyTimeout).waitFor(container);
        }
    }

    public static class DetailTableEditFinder extends WebDriverComponent.WebDriverComponentFinder<DetailTableEdit, DetailTableEditFinder>
    {
        private final Locator.XPathLocator _baseLocator = Locator.tag("form")
                .withDescendant(Locator.tagWithClass("table", "detail-component--table__fixed"));
        private Locator _locator;

        public DetailTableEditFinder(WebDriver driver)
        {
            super(driver);
            _locator= _baseLocator;
        }

        public DetailTableEditFinder withTitle(String title)
        {
            _locator = _baseLocator.withDescendant(Locator.tagWithClass("span", "detail__edit--heading")
                .parent().withText(title));
            return this;
        }

        @Override
        protected DetailTableEdit construct(WebElement el, WebDriver driver)
        {
            return new DetailTableEdit(el, driver);
        }

        @Override
        protected Locator locator()
        {
            return _locator;
        }
    }

}
