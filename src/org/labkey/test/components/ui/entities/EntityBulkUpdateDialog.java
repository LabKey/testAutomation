package org.labkey.test.components.ui.entities;

import org.labkey.remoteapi.CommandException;
import org.labkey.test.BootstrapLocators;
import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.WebTestHelper;
import org.labkey.test.components.Component;
import org.labkey.test.components.UpdatingComponent;
import org.labkey.test.components.bootstrap.ModalDialog;
import org.labkey.test.components.html.Checkbox;
import org.labkey.test.components.html.Input;
import org.labkey.test.components.react.FilteringReactSelect;
import org.labkey.test.components.react.ReactDateTimePicker;
import org.labkey.test.components.react.ToggleButton;
import org.labkey.test.components.ui.files.FileAttachmentContainer;
import org.labkey.test.params.FieldDefinition;
import org.labkey.test.util.AuditLogHelper;
import org.labkey.test.util.EscapeUtil;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Automates product component src/components/forms/QueryInfoForms, with BulkUpdateForm.d.ts
 */
public class EntityBulkUpdateDialog extends ModalDialog
{
    private final int WAIT_TIMEOUT = 2000;
    private final UpdatingComponent _updatingComponent;
    private int _changeCounter = 0;

    public EntityBulkUpdateDialog(WebDriver driver)
    {
        this(driver, UpdatingComponent.NO_OP);
    }

    public EntityBulkUpdateDialog(WebDriver driver, UpdatingComponent updatingComponent)
    {
        super(new ModalDialogFinder(driver).withTitle("Update "));
        _updatingComponent = updatingComponent;
        getWrapper().mouseOver(elementCache().title); // avoid accidentally triggering tooltips
    }

    /**
     * If for some reason your test set a field but it actually didn't change the value, you can use this helper
     * to set the change counter to a specific value.
     */
    public EntityBulkUpdateDialog adjustChangeCounter(int change)
    {
        _changeCounter = _changeCounter + change;
        return this;
    }

    // enable/disable field editable state

    public boolean isFieldEnabled(String fieldKey)
    {
        return elementCache().getToggle(fieldKey).isOn();
    }

    public EntityBulkUpdateDialog setEditableState(String fieldKey, boolean enable)
    {
        elementCache().getToggle(fieldKey).set(enable);
        if (enable) _changeCounter++;
        else _changeCounter--;
        return this;
    }

    private WebDriverWait waiter()
    {
        return new WebDriverWait(getDriver(), Duration.ofMillis(WAIT_TIMEOUT));
    }

    // For use when the field is of an unknown type, as can occur in fuzz tests
    public void setValue(FieldDefinition field, Object newValue)
    {
        if (field.getType() == FieldDefinition.ColumnType.TextChoice || field.getLookup() != null)
            setSelectionField(EscapeUtil.fieldKeyEncodePart(field.getName()), newValue instanceof String ? List.of((String) newValue) : (List<String>) newValue);
        else if (field.getType() == FieldDefinition.ColumnType.Integer || field.getType() == FieldDefinition.ColumnType.Decimal || field.getType() == FieldDefinition.ColumnType.Double)
            setNumericField(EscapeUtil.fieldKeyEncodePart(field.getName()), String.valueOf(newValue));
        else if (field.getType() == FieldDefinition.ColumnType.Date || field.getType() == FieldDefinition.ColumnType.DateAndTime || field.getType() == FieldDefinition.ColumnType.Time)
            setDateField(EscapeUtil.fieldKeyEncodePart(field.getName()), (String) newValue);
        else if (field.getType() == FieldDefinition.ColumnType.Boolean)
            setBooleanField(EscapeUtil.fieldKeyEncodePart(field.getName()), (Boolean) newValue);
        else if (field.getType() == FieldDefinition.ColumnType.MultiLine)
            setTextArea(EscapeUtil.fieldKeyEncodePart(field.getName()), (String) newValue);
        else
            setTextField(EscapeUtil.fieldKeyEncodePart(field.getName()), (String) newValue);
    }

    // interact with selection fields

    public EntityBulkUpdateDialog setSelectionField(String fieldKey, List<String> selectValues)
    {
        setEditableState(fieldKey, true);
        FilteringReactSelect reactSelect = elementCache().getSelect(fieldKey);
        WebDriverWrapper.waitFor(reactSelect::isEnabled,
                "the ["+fieldKey+"] reactSelect did not become enabled in time", WAIT_TIMEOUT);
        selectValues.forEach(reactSelect::filterSelect);
        return this;
    }

    public List<String> getSelectionOptions(String fieldKey)
    {
        return enableAndWait(fieldKey, elementCache().getSelect(fieldKey)).getOptions();
    }

    public List<String> getSelectionFieldValues(String fieldKey)
    {
        return enableAndWait(fieldKey, elementCache().getSelect(fieldKey)).getSelections();
    }

    public EntityBulkUpdateDialog setTextArea(String fieldKey, String text)
    {
        enableAndWait(fieldKey, elementCache().textArea(fieldKey)).set(text);
        return this;
    }

    public String getTextArea(String fieldKey)
    {
        return elementCache().textArea(fieldKey).get();
    }

    // get/set text fields with ID

    public EntityBulkUpdateDialog setTextField(String fieldKey, String value)
    {
        enableAndWait(fieldKey, elementCache().textInput(fieldKey)).set(value);
        return this;
    }

    public String getTextField(String fieldKey)
    {
        return enableAndWait(fieldKey, elementCache().textInput(fieldKey)).get();
    }

    public EntityBulkUpdateDialog setNumericField(String fieldKey, String value)
    {
        enableAndWait(fieldKey, elementCache().numericInput(fieldKey)).set(value);
        return this;
    }

    public String getNumericField(String fieldKey)
    {
        return elementCache().numericInput(fieldKey).get();
    }

    public EntityBulkUpdateDialog setDateField(String fieldKey, String dateString)
    {
        enableAndWait(fieldKey, elementCache().dateInput(fieldKey)).set(dateString);
        return this;
    }

    public String getDateField(String fieldKey)
    {
        return elementCache().dateInput(fieldKey).get();
    }

    public FileAttachmentContainer getFileField(String fieldKey)
    {
        return elementCache().fileUploadField(fieldKey);
    }

    public EntityBulkUpdateDialog removeFile(String fieldKey)
    {
        getFileField(fieldKey).removeFile();
        _changeCounter++;
        return this;
    }

    public EntityBulkUpdateDialog setBooleanField(String fieldKey, boolean checked)
    {
        enableAndWait(fieldKey, getCheckBox(fieldKey)).set(checked);
        return this;
    }

    private <T extends Component<?>> T enableAndWait(String fieldKey, T formItem)
    {
        setEditableState(fieldKey, true);
        // "Clickable" means visible and enabled
        waiter().until(ExpectedConditions.elementToBeClickable(formItem.getComponentElement()));
        return formItem;
    }

    public boolean getBooleanField(String fieldKey)
    {
        return getCheckBox(fieldKey).get();
    }

    private Checkbox getCheckBox(String fieldKey)
    {
        WebElement row = elementCache().formRow(fieldKey);
        return new Checkbox(elementCache().checkBoxLoc.findElement(row));
    }

    public String getErrorAlertText()
    {
        return BootstrapLocators.errorBanner.waitForElement(elementCache(), getWrapper().defaultWaitForPage).getText();
    }

    public String getWarningAlertText()
    {
        return BootstrapLocators.warningBanner.waitForElement(elementCache(), getWrapper().defaultWaitForPage).getText();
    }

    public List<String> getFieldNames()
    {
        List<WebElement> labels = Locator.tagWithClass("label", "control-label").withAttribute("for")
                .waitForElements(elementCache(), 2_000);

        return labels.stream().map(a -> EscapeUtil.fieldKeyDecodePart(a.getDomAttribute("for"))).toList();
    }

    public EntityBulkUpdateDialog waitForFieldsToBe(List<String> expectedFieldNames, int waitMilliseconds)
    {
        WebDriverWrapper.waitFor(()-> expectedFieldNames.equals(getFieldNames()),
                "Wrong editable fields", waitMilliseconds);
        return this;
    }

    public boolean isCommentInputPresent()
    {
        return elementCache().commentInputLocator.findOptionalElement(getDriver()).isPresent();
    }

    public EntityBulkUpdateDialog setActionComment(String comment)
    {
        elementCache().commentInput.sendKeys(comment);
        return this;
    }

    public EntityBulkUpdateDialog clearActionComment()
    {
        elementCache().commentInput.clear();
        return this;
    }

    public Integer getCountFromTitle()
    {
        // expecting title to be like "Update N items"
        String title = getTitle();
        String[] parts = title.split(" ");
        if (parts.length > 1)
        {
            try
            {
                return Integer.parseInt(parts[1]);
            }
            catch (NumberFormatException nfe)
            {
                return null;
            }
        }
        return null;
    }

    // dismiss the dialog

    public void clickEditWithGrid()
    {
        dismiss("Edit with Grid");
    }

    public String clickUpdateExpectingError()
    {
        elementCache().updateButton.click();

        return BootstrapLocators.errorBanner.waitForElement(getDriver(), 2000).getText();
    }

    public void clickUpdate()
    {
        clickUpdate(false);
    }

    public void clickUpdate(boolean skipAuditEventCheck)
    {
        Integer rowCount = getCountFromTitle();

        _updatingComponent.doAndWaitForUpdate(() ->
        {
            elementCache().updateButton.click();
            waitForClose();
        });

        // check for the expected number of Data Changes in the latest audit event records
        AuditLogHelper auditLogHelper = new AuditLogHelper(getWrapper(), () -> WebTestHelper.getRemoteApiConnection(false));
        String auditEventName = auditLogHelper.getAuditEventNameFromURL();
        if (!skipAuditEventCheck && auditEventName != null)
        {
            try
            {
                int changeCounter = auditLogHelper.isSourcesRoute() ? _changeCounter + 1 : _changeCounter; // Source updates include the name value in the diff (even when not changed)
                auditLogHelper.checkAuditEventDiffCountForLastTransaction(getWrapper().getCurrentContainerPath(), auditEventName, changeCounter, rowCount);
            }
            catch (CommandException | IOException e)
            {
                throw new RuntimeException(e);
            }
        }
    }

    public void clickCancel()
    {
        dismiss("Cancel");
    }

    @Override
    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    @Override
    protected ElementCache elementCache()
    {
        return (ElementCache) super.elementCache();
    }

    protected class ElementCache extends ModalDialog.ElementCache
    {
        public WebElement formRow(String fieldKey)
        {
            return Locator.tagWithClass("div", "row")
                    .withChild(Locator.tagWithAttribute("label", "for", fieldKey))
                    .waitForElement(this, WAIT_TIMEOUT);
        }

        public ToggleButton getToggle(String fieldKey)
        {
            return new ToggleButton.ToggleButtonFinder(getDriver()).waitFor(formRow(fieldKey));
        }

        public FilteringReactSelect getSelect(String fieldKey)
        {
            return FilteringReactSelect.finder(getDriver()).withNamedInput(fieldKey).refindWhenNeeded(this);
        }

        public Input textInput(String fieldKey)
        {
            WebElement inputEl = textInputLoc.waitForElement(formRow(fieldKey), WAIT_TIMEOUT);
            return new Input(inputEl, getDriver());
        }

        public Input textArea(String fieldKey)
        {
            WebElement inputEl = Locator.textarea(fieldKey).waitForElement(formRow(fieldKey), WAIT_TIMEOUT);
            return new Input(inputEl, getDriver());
        }

        public Input numericInput(String fieldKey)
        {
            WebElement inputEl = numberInputLoc.waitForElement(formRow(fieldKey), WAIT_TIMEOUT);
            return new Input(inputEl, getDriver());
        }

        public ReactDateTimePicker dateInput(String fieldKey)
        {
            return new ReactDateTimePicker.ReactDateTimeInputFinder(getDriver())
                    .withInputId(fieldKey).waitFor(formRow(fieldKey));
        }

        public FileAttachmentContainer fileUploadField(String fieldKey)
        {
            return new FileAttachmentContainer(formRow(fieldKey), getDriver());
        }

        final Locator textInputLoc = Locator.tagWithAttribute("input", "type", "text");
        final Locator numberInputLoc = Locator.tagWithAttribute("input", "type", "number");
        final Locator checkBoxLoc = Locator.tagWithAttribute("input", "type", "checkbox");
        final Locator.XPathLocator commentInputLocator = Locator.tagWithId("textarea", "actionComments");
        final WebElement commentInput = commentInputLocator.refindWhenNeeded(this);

        final WebElement updateButton = Locator.tagWithClass("button", "btn-success").findWhenNeeded(this);
    }

}
