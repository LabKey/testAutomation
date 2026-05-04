package org.labkey.test.components.ui.grids;

import org.junit.Assert;
import org.labkey.remoteapi.CommandException;
import org.labkey.test.BootstrapLocators;
import org.labkey.test.Locator;
import org.labkey.test.TestProperties;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.WebTestHelper;
import org.labkey.test.components.Component;
import org.labkey.test.components.WebDriverComponent;
import org.labkey.test.components.html.Checkbox;
import org.labkey.test.components.html.Input;
import org.labkey.test.components.react.FilteringReactSelect;
import org.labkey.test.components.react.ReactDateTimePicker;
import org.labkey.test.components.react.ReactSelect;
import org.labkey.test.components.ui.files.FileUploadField;
import org.labkey.test.params.FieldDefinition;
import org.labkey.test.params.FieldKey;
import org.labkey.test.util.AuditLogHelper;
import org.labkey.test.util.LogMethod;
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
import java.util.ArrayList;
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

    /**
     * @param columnIdentifier fieldKey, name, or label
     */
    public boolean isFieldPresent(CharSequence columnIdentifier)
    {
        try
        {
            elementCache().findValueCell(columnIdentifier);
            return true;
        }
        catch (NoSuchElementException e)
        {
            return false;
        }
    }
    /**
     * Check to see if a field is editable. Could be state dependent, that is it returns false if the field is
     * loading but if checked later could return true.
     *
     * @param columnIdentifier fieldKey, name, or label
     * @return True if it is false otherwise.
     **/
    public boolean isFieldEditable(CharSequence columnIdentifier)
    {
        // TODO Could put a check here to see if a field is loading then return false, or wait.
        WebElement fieldValueElement = elementCache().findValueCell(columnIdentifier);
        return isEditableField(fieldValueElement);
    }

    private boolean isEditableField(WebElement element)
    {
        // If the div does not have the class value of 'field__un-editable' then it is an editable field.
        return !Locator.byClass("field__un-editable").existsIn(element);
    }

    /**
     * Get the value of a read only field.
     *
     * @param columnIdentifier fieldKey, name, or label
     * @return The value in the field.
     **/
    public String getReadOnlyField(CharSequence columnIdentifier)
    {
        WebElement fieldValueElement = elementCache().findValueCell(columnIdentifier);
        return Locator.xpath("./div/*").findElement(fieldValueElement).getText();
    }

    /**
     * Get the value of a text field.
     *
     * @param columnIdentifier fieldKey, name, or label
     * @return The value in the field.
     **/
    public String getTextField(CharSequence columnIdentifier)
    {
        return elementCache().findInput(columnIdentifier).get();
    }

    /**
     * Set a text field.
     *
     * @param columnIdentifier fieldKey, name, or label
     * @param value The value to set the field to.
     * @return A reference to this editable detail table.
     **/
    public DetailTableEdit setTextField(CharSequence columnIdentifier, String value)
    {
        if (isFieldEditable(columnIdentifier))
        {
            Input input = elementCache().findInput(columnIdentifier);
            input.setValue(value);
        }
        else
        {
            throw new IllegalArgumentException("Field '" + columnIdentifier + "' is read-only. This field can not be set.");
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
     * @param columnIdentifier fieldKey, name, or label
     * @return The value of the field.
     **/
    public boolean getBooleanField(CharSequence columnIdentifier)
    {
        // The text used in the field label and the value of the name attribute in the checkbox don't always have the same case.
        WebElement editableElement = Locator.tag("input").findElement(elementCache().findValueCell(columnIdentifier));
        String elementType = editableElement.getDomAttribute("type").toLowerCase().trim();

        Assert.assertEquals(String.format("Field '%s' is not a checkbox. Cannot be get true/false value.", columnIdentifier), "checkbox", elementType);

        return new Checkbox(editableElement).isChecked();
    }

    /**
     * Set a boolean field (a checkbox).
     *
     * @param columnIdentifier fieldKey, name, or label
     * @param value True will check it, false will uncheck it.
     * @return A reference to this editable detail table.
     **/
    public DetailTableEdit setBooleanField(CharSequence columnIdentifier, boolean value)
    {

        WebElement fieldValueElement = elementCache().findValueCell(columnIdentifier);
        Assert.assertTrue(String.format("Field '%s' is not editable and cannot be set.", columnIdentifier), isEditableField(fieldValueElement));
        getWrapper().scrollIntoView(fieldValueElement);

        WebElement editableElement = fieldValueElement.findElement(By.xpath("./div/div/input"));
        String elementType = editableElement.getDomAttribute("type").toLowerCase().trim();

        Assert.assertEquals(String.format("Field '%s' is not a checkbox. Cannot be set to true/false.", columnIdentifier), "checkbox", elementType);

        Checkbox checkbox = new Checkbox(editableElement);

        checkbox.set(value);

        _changeCounter++;
        return this;
    }

    /**
     * Get the value of an int field. You could also call getTextField
     *
     * @param columnIdentifier fieldKey, name, or label
     * @return The value of the field as an int.
     **/
    public int getIntField(CharSequence columnIdentifier)
    {
        return Integer.getInteger(getTextField(columnIdentifier));
    }

    /**
     * Set an int field.
     *
     * @param columnIdentifier fieldKey, name, or label
     * @param value The int value to set the field to.
     * @return A reference to this editable detail table.
     **/
    public DetailTableEdit setIntField(CharSequence columnIdentifier, int value)
    {
        return setTextField(columnIdentifier, Integer.toString(value));
    }

    public FileUploadField getFileField(CharSequence columnIdentifier)
    {
        return elementCache().fileField(columnIdentifier);
    }

    public DetailTableEdit setFileField(CharSequence columnIdentifier, File file)
    {
        getFileField(columnIdentifier)
                .setFile(file);

        _changeCounter++;
        return this;
    }

    public DetailTableEdit removeFileField(CharSequence columnIdentifier)
    {
        getFileField(columnIdentifier).removeFile();

        _changeCounter++;
        return this;
    }

    public boolean isFileFieldBlank(CharSequence columnIdentifier)
    {
        return !getFileField(columnIdentifier)
                .hasAttachedFile();
    }

    public FilteringReactSelect getSelectField(CharSequence columnIdentifier)
    {
        return elementCache().findSelect(columnIdentifier);
    }

    /**
     * Get the value of a select field.
     *
     * @param columnIdentifier fieldKey, name, or label
     * @return The selected value.
     **/
    public String getSelectedValue(CharSequence columnIdentifier)
    {
        return getSelectField(columnIdentifier).getValue();
    }

    /**
     * This allows you to query a given select in the edit panel to see what options it offers.
     *
     * @param columnIdentifier fieldKey, name, or label
     * @return List of strings for the values in the list.
     **/
    public List<String> getSelectOptions(CharSequence columnIdentifier)
    {
        return getSelectField(columnIdentifier).getOptions();
    }

    /**
     * Select a single value from a select list.
     *
     * @param columnIdentifier fieldKey, name, or label
     * @param selectValue The value to select from the list.
     * @return A reference to this editable detail table.
     **/
    public DetailTableEdit setSelectValue(CharSequence columnIdentifier, String selectValue)
    {
        List<String> selection = Arrays.asList(selectValue);
        return setSelectValue(columnIdentifier, selection);
    }

    public DetailTableEdit createSelectValue(CharSequence columnIdentifier, String value)
    {
        var select = ReactSelect.finder(getDriver()).waitFor(elementCache().findValueCell(columnIdentifier));
        select.createValue(value);
        return this;
    }

    /**
     * Select multiple values from a select list.
     *
     * @param columnIdentifier fieldKey, name, or label
     * @param selectValues The value to select from the list.
     * @return A reference to this editable detail table.
     **/
    public DetailTableEdit setSelectValue(CharSequence columnIdentifier, List<String> selectValues)
    {
        FilteringReactSelect reactSelect = getSelectField(columnIdentifier);
        selectValues.forEach(reactSelect::typeAheadSelect);
        _changeCounter++;
        return this;
    }

    /**
     * Clear a given select field.
     *
     * @param columnIdentifier fieldKey, name, or label
     * @return A reference to this editable detail table.
     **/
    public DetailTableEdit clearSelectValue(CharSequence columnIdentifier)
    {
        return clearSelectValue(columnIdentifier, true, true);
    }

    /**
     * Clear a given select field.
     *
     * @param columnIdentifier fieldKey, name, or label
     * @param waitForSelection If true, wait for the select to have a selection before clearing it
     * @param assertSelection  If true, assert if no selection appears (note: does nothing if waitForSelection is not true)
     * @return A reference to this editable detail table.
     */
    public DetailTableEdit clearSelectValue(CharSequence columnIdentifier, boolean waitForSelection, boolean assertSelection)
    {
        var select = getSelectField(columnIdentifier);
        if (waitForSelection)
        {
            if (assertSelection)
            {
                WebDriverWrapper.waitFor(select::hasSelection,
                        String.format("The %s select did not have any selection in time", columnIdentifier), _readyTimeout);
            }
            else
                WebDriverWrapper.waitFor(select::hasSelection, 1_000);
        }
        select.clearSelection();
        _changeCounter++;
        return this;
    }

    /**
     * Set a DateTime, Date or Time field.
     * @param columnIdentifier fieldKey, name, or label
     * @param dateTime Will be used to determine what kind of field is being set and how to set it. If the parameter
     *                 is a LocalDateTime object then it is assumed that field is a DateTime field. If the parameter is
     *                 a LocalDate object then it is assumed to be a date-only field. And I think you can guess what
     *                 happens with a LocalTime object type. If the type is a string it is used as a literal value that
     *                 is typed into the field (no picker is used).
     * @return A reference to this DetailTableEdit object.
     */
    public DetailTableEdit setDateTimeField(CharSequence columnIdentifier, Object dateTime)
    {
        ReactDateTimePicker dateTimePicker = getDateTimePicker(columnIdentifier);
        if (dateTime instanceof LocalDateTime localDateTime)
        {
            dateTimePicker.select(localDateTime);
        }
        else if (dateTime instanceof LocalDate localDate)
        {
            dateTimePicker.selectDate(localDate);
        }
        else if (dateTime instanceof LocalTime localTime)
        {
            dateTimePicker.selectTime(localTime);
        }
        else if (dateTime instanceof String setValue)
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

    public String getDateTimeField(CharSequence columnIdentifier)
    {
        ReactDateTimePicker dateTimePicker = getDateTimePicker(columnIdentifier);
        return dateTimePicker.get();
    }

    public void clearDateTimeField(CharSequence columnIdentifier)
    {
        ReactDateTimePicker dateTimePicker = getDateTimePicker(columnIdentifier);
        dateTimePicker.clear();
        _changeCounter++;
    }

    private ReactDateTimePicker getDateTimePicker(CharSequence columnIdentifier)
    {
        return new ReactDateTimePicker.ReactDateTimeInputFinder(getDriver()).find(elementCache().findValueCell(columnIdentifier));
    }

    // For use when the field is of an unknown type, as can occur in fuzz tests
    public void setDetails(FieldDefinition field, Object newValue)
    {
        if (newValue == null)
            return;

        if (field.getType() == FieldDefinition.ColumnType.TextChoice)
            setSelectValue(field.getName(), (List<String>) newValue);
        else if (field.getType() == FieldDefinition.ColumnType.Date || field.getType() == FieldDefinition.ColumnType.DateAndTime || field.getType() == FieldDefinition.ColumnType.Time)
            setDateTimeField(field.getName(), newValue);
        else if (field.getType() == FieldDefinition.ColumnType.Boolean)
            setBooleanField(field.getName(), (Boolean) newValue);
        else
            setTextField(field.getName(), String.valueOf(newValue));
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
        if (elementCache().validationMsg.existsIn(this))
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

    public DetailDataPanel clickSave(boolean skipAuditEventCheck)
    {
        String title = getSourceTitle();
        var componentEl = getComponentElement();
        getWrapper().shortWait().until(ExpectedConditions.elementToBeClickable(elementCache().saveButton));
        elementCache().saveButton.click();

        // If save causes some update, wait until it is completed.
        getWrapper().longWait().withMessage("Update took too long to complete.")
                .until(ExpectedConditions.stalenessOf(elementCache().saveButton));

        // check for the expected number of Data Changes in the latest audit event records
        AuditLogHelper auditLogHelper = new AuditLogHelper(getWrapper(), () -> WebTestHelper.getRemoteApiConnection(false));
        AuditLogHelper.AuditEvent auditEventName = auditLogHelper.getAuditEventNameFromURL();
        if (!skipAuditEventCheck && auditEventName != null && !TestProperties.isTrialServer())
        {
            try
            {
                int changeCounter = auditLogHelper.isSourcesRoute() ? _changeCounter + 1 : _changeCounter; // Source updates include the name value in the diff (even when not changed)
                auditLogHelper.checkAuditEventDiffCountForLastTransaction(getWrapper().getCurrentContainerPath(), auditEventName, changeCounter, 1);
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
        WebDriverWrapper.waitFor(errorBanner::isDisplayed, "No error message was shown.", 1_000);
        return errorBanner.getText();
    }

    public DetailDataPanel clickCancel()
    {
        String title = getSourceTitle();
        elementCache().cancelButton.click();
        getWrapper().shortWait().until(ExpectedConditions.stalenessOf(elementCache().cancelButton));
        return new DetailDataPanel.DetailDataPanelFinder(getDriver()).withTitle(title).waitFor();
    }

    public String clickCancelExpectingError()
    {
        elementCache().cancelButton.click();
        WebElement errorBanner = BootstrapLocators.errorBanner.findWhenNeeded(this);
        WebDriverWrapper.waitFor(errorBanner::isDisplayed, "No error message was shown.", 1_000);
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

        public final WebElement header = Locator.byClass("panel-heading")
                .findWhenNeeded(this);
        public final WebElement editPanel = Locator.tagWithClass("div", "detail__editing")
                .findWhenNeeded(this);

        public WebElement findValueCell(CharSequence columnIdentifier)
        {
            return getFieldManager().findFieldReference(columnIdentifier).getElement();
        }

        private FieldReferenceManager _fieldReferenceManager;

        @LogMethod
        private FieldReferenceManager getFieldManager()
        {
            if (_fieldReferenceManager == null)
            {
                List<DetailTableEditFieldReference> columnHeaders = new ArrayList<>();

                List<WebElement> valueCells = Locator.tagWithAttribute("td", "data-fieldkey").findElements(this);
                // Use JavaScript to get fieldKeys and captions in one operation, rather than making 2N calls to 'WebElement.getDomAttribute'
                List<List<String>> captionsAndKeys = getWrapper().executeScript(
                    """
                    var cells = arguments[0];
                    var captions = [];
                    var fieldkeys = [];
                    for (var i = 0; i < cells.length; i++)
                    {
                        captions.push(cells[i].dataset.caption);
                        fieldkeys.push(cells[i].dataset.fieldkey);
                    }
                    return [captions, fieldkeys];
                    """, List.class,
                valueCells);
                List<String> captions = captionsAndKeys.get(0);
                List<String> fieldkeys = captionsAndKeys.get(1);
                for (int i = 0; i < valueCells.size(); i++)
                {
                    columnHeaders.add(new DetailTableEditFieldReference(valueCells.get(i), i, fieldkeys.get(i), captions.get(i)));
                }

                _fieldReferenceManager = new FieldReferenceManager(columnHeaders);
            }

            return _fieldReferenceManager;
        }

        public FileUploadField fileField(CharSequence columnIdentifier)
        {
            return new FileUploadField(findValueCell(columnIdentifier), getDriver());
        }

        public Locator validationMsg = Locator.tagWithClass("span", "validation-message");

        public WebElement saveButton = Locator.tagWithAttribute("button", "type", "submit")
                .containing("Save").findWhenNeeded(this);
        public WebElement cancelButton = Locator.tagWithAttribute("button", "type", "button")
                .withText("Cancel").findWhenNeeded(this);

        public WebElement commentInput = Locator.tagWithId("textarea", "actionComments").refindWhenNeeded(getDriver());

        public FilteringReactSelect findSelect(CharSequence columnIdentifier)
        {
            return FilteringReactSelect.finder(_driver).timeout(_readyTimeout).waitFor(findValueCell(columnIdentifier));
        }

        public Input findInput(CharSequence columnIdentifier)
        {
            return Input.Input(Locator.xpath("./div/div/*[self::input or self::textarea]"), getDriver()).find(findValueCell(columnIdentifier));
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

    private static class DetailTableEditFieldReference extends FieldReferenceManager.FieldReference
    {
        private final FieldKey _fieldKey;
        private final String _label;

        public DetailTableEditFieldReference(WebElement element, int domIndex, String fieldKey, String label)
        {
            super(element, domIndex);
            _fieldKey = FieldKey.fromFieldKey(fieldKey);
            _label = label;
        }

        @Override
        public FieldKey getFieldKey()
        {
            return _fieldKey;
        }

        @Override
        public String getLabel()
        {
            return _label;
        }
    }
}
