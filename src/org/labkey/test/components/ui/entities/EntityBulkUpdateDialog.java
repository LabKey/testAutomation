package org.labkey.test.components.ui.entities;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.labkey.remoteapi.CommandException;
import org.labkey.test.BootstrapLocators;
import org.labkey.test.Locator;
import org.labkey.test.TestProperties;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.WebTestHelper;
import org.labkey.test.components.Component;
import org.labkey.test.components.UpdatingComponent;
import org.labkey.test.components.react.FilteringReactSelect;
import org.labkey.test.components.react.ToggleButton;
import org.labkey.test.components.ui.files.AttachmentCard;
import org.labkey.test.components.ui.files.FileAttachmentContainer;
import org.labkey.test.components.ui.files.FileUploadField;
import org.labkey.test.params.FieldDefinition;
import org.labkey.test.params.FieldKey;
import org.labkey.test.util.AuditLogHelper;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.util.List;

/**
 * Automates product component src/components/forms/QueryInfoForms, with BulkUpdateForm.d.ts <br>
 * <br>
 * `fieldIdentifier` arguments accept field names or {@link FieldKey}s
 */
public class EntityBulkUpdateDialog extends EntityBulkDialog
{
    private final int WAIT_TIMEOUT = 2000;
    private final UpdatingComponent _updatingComponent;

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

    /**
     * @param fieldIdentifier Identifier for the field; name ({@link String}) or fieldKey ({@link FieldKey})
     */
    public boolean isFieldEnabled(CharSequence fieldIdentifier)
    {
        return elementCache().getToggle(fieldIdentifier).isOn();
    }

    /**
     * @param fieldIdentifier Identifier for the field; name ({@link String}) or fieldKey ({@link FieldKey})
     */
    public EntityBulkUpdateDialog setEditableState(CharSequence fieldIdentifier, boolean enable)
    {
        ToggleButton toggle = elementCache().getToggle(fieldIdentifier);
        if (toggle.isOn() != enable)
        {
            toggle.set(enable);
            if (enable) _changeCounter++;
            else _changeCounter--;
            getWrapper().mouseOut(); // Toggle is dangerously close to field info tooltip
        }
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
            setSelectionField(field.getName(), newValue instanceof String ? List.of((String) newValue) : (List<String>) newValue);
        else if (field.getType() == FieldDefinition.ColumnType.Integer || field.getType() == FieldDefinition.ColumnType.Decimal || field.getType() == FieldDefinition.ColumnType.Double)
            setNumericField(field.getName(), String.valueOf(newValue));
        else if (field.getType() == FieldDefinition.ColumnType.Date || field.getType() == FieldDefinition.ColumnType.DateAndTime || field.getType() == FieldDefinition.ColumnType.Time)
            setDateField(field.getName(), (String) newValue);
        else if (field.getType() == FieldDefinition.ColumnType.Boolean)
            setBooleanField(field.getName(), (Boolean) newValue);
        else if (field.getType() == FieldDefinition.ColumnType.MultiLine)
            setTextArea(field.getName(), (String) newValue);
        else
            setTextField(field.getName(), (String) newValue);
    }

    // interact with selection fields

    /**
     * @param fieldIdentifier Identifier for the field; name ({@link String}) or fieldKey ({@link FieldKey})
     * @param selectValues value to select
     * @return this component
     */
    public EntityBulkUpdateDialog setSelectionField(CharSequence fieldIdentifier, List<String> selectValues)
    {
        FilteringReactSelect reactSelect = enableSelectionField(fieldIdentifier);
        selectValues.forEach(reactSelect::filterSelect);
        return this;
    }

    /**
     * Clear the field (fieldIdentifier).
     *
     * @param fieldIdentifier Identifier for the field; name ({@link String}) or fieldKey ({@link FieldKey})
     * @return this component
     */
    public EntityBulkUpdateDialog clearSelection(CharSequence fieldIdentifier)
    {
        FilteringReactSelect reactSelect = enableSelectionField(fieldIdentifier);
        reactSelect.clearSelection();
        return this;
    }

    /**
     * @param fieldIdentifier Identifier for the field; name ({@link String}) or fieldKey ({@link FieldKey})
     * @param selectValue value to select
     * @return this component
     */
    public EntityBulkUpdateDialog setSelectionField(CharSequence fieldIdentifier, String selectValue)
    {
        return setSelectionField(fieldIdentifier, List.of(selectValue));
    }

    /**
     * @param fieldIdentifier Identifier for the field; name ({@link String}) or fieldKey ({@link FieldKey})
     * @return text displayed in the help block, if any, for the selection field
     */
    public @Nullable String getSelectionFieldHelpBlockText(CharSequence fieldIdentifier)
    {
        return elementCache().selectionField(fieldIdentifier).getHelpBlockText();
    }

    /**
     * @param fieldIdentifier Identifier for the field; name ({@link String}) or fieldKey ({@link FieldKey})
     * @return available options for the specified field
     */
    public List<String> getSelectionOptions(CharSequence fieldIdentifier)
    {
        return enableSelectionField(fieldIdentifier).getOptions();
    }

    /**
     * @param fieldIdentifier Identifier for the field; name ({@link String}) or fieldKey ({@link FieldKey})
     * @return selected options for the specified field
     */
    public List<String> getSelectionFieldValues(CharSequence fieldIdentifier)
    {
        return enableSelectionField(fieldIdentifier).getSelections();
    }

    private @NotNull FilteringReactSelect enableSelectionField(CharSequence fieldIdentifier)
    {
        setEditableState(fieldIdentifier, true);
        FilteringReactSelect reactSelect = elementCache().selectionField(fieldIdentifier);
        WebDriverWrapper.waitFor(reactSelect::isEnabled,
            "the ["+ fieldIdentifier +"] reactSelect did not become enabled in time", WAIT_TIMEOUT);
        return reactSelect;
    }

    /**
     * @param fieldIdentifier Identifier for the field; name ({@link String}) or fieldKey ({@link FieldKey})
     * @param value value to set
     * @return this component
     */
    public EntityBulkUpdateDialog setTextArea(CharSequence fieldIdentifier, String value)
    {
        enableAndWait(fieldIdentifier, elementCache().textArea(fieldIdentifier)).set(value);
        return this;
    }

    /**
     * @param fieldIdentifier Identifier for the field; name ({@link String}) or fieldKey ({@link FieldKey})
     * @param value value to set
     * @return this component
     */
    public EntityBulkUpdateDialog setTextField(CharSequence fieldIdentifier, String value)
    {
        enableAndWait(fieldIdentifier, elementCache().textInput(fieldIdentifier)).set(value);
        return this;
    }

    /**
     * @param fieldIdentifier Identifier for the field; name ({@link String}) or fieldKey ({@link FieldKey})
     * @return current value of the specified field
     */
    public String getTextField(CharSequence fieldIdentifier)
    {
        return enableAndWait(fieldIdentifier, elementCache().textInput(fieldIdentifier)).get();
    }

    /**
     * @param fieldIdentifier Identifier for the field; name ({@link String}) or fieldKey ({@link FieldKey})
     * @param value value to set
     * @return this component
     */
    public EntityBulkUpdateDialog setNumericField(CharSequence fieldIdentifier, String value)
    {
        enableAndWait(fieldIdentifier, elementCache().textInput(fieldIdentifier)).set(value);
        return this;
    }

    /**
     * @param fieldIdentifier Identifier for the field; name ({@link String}) or fieldKey ({@link FieldKey})
     * @param dateString string representation of date to set
     * @return this component
     */
    public EntityBulkUpdateDialog setDateField(CharSequence fieldIdentifier, String dateString)
    {
        enableAndWait(fieldIdentifier, elementCache().dateInput(fieldIdentifier)).set(dateString);
        return this;
    }

    /**
     * @param fieldIdentifier Identifier for the field; name ({@link String}) or fieldKey ({@link FieldKey})
     * @return current value of the specified field
     */
    public String getDateField(CharSequence fieldIdentifier)
    {
        return elementCache().dateInput(fieldIdentifier).get();
    }

    /**
     * @param fieldIdentifier Identifier for the field; name ({@link String}) or fieldKey ({@link FieldKey})
     * @return file attachment component
     */
    private FileAttachmentContainer getFileField(CharSequence fieldIdentifier)
    {
        FieldKey identifier = FileAttachmentContainer.fileUploadFieldKey(fieldIdentifier);
        return enableAndWait(identifier, elementCache().fileUploadField(identifier));
    }

    /**
     * @param fieldIdentifier Identifier for the field; name ({@link String}) or fieldKey ({@link FieldKey})
     * @param file file to attach
     * @return this component
     */
    public EntityBulkUpdateDialog attachFile(CharSequence fieldIdentifier, File file)
    {
        getFileField(fieldIdentifier).attachFile(file);
        return this;
    }

    /**
     * @param fieldIdentifier Identifier for the field; name ({@link String}) or fieldKey ({@link FieldKey})
     * @return this component
     */
    public EntityBulkUpdateDialog removeFile(CharSequence fieldIdentifier)
    {
        getFileField(fieldIdentifier).removeFile();
        return this;
    }

    /**
     * Removes an existing attachment displayed as an {@link AttachmentCard} in the bulk update dialog.
     * This is used when the rows being edited already have an attachment — the existing file is shown
     * as an AttachmentCard with a dropdown menu containing "Remove attachment".
     *
     * @param fieldIdentifier Identifier for the field; name ({@link String}) or fieldKey ({@link FieldKey})
     * @return this component
     */
    public EntityBulkUpdateDialog removeExistingAttachment(CharSequence fieldIdentifier)
    {
        var row = getFileField(fieldIdentifier);
        AttachmentCard card = new AttachmentCard.FileAttachmentCardFinder(getDriver()).waitFor(row);
        card.clickRemove();
        return this;
    }

    public FileUploadField getExistingFileCard(CharSequence fieldIdentifier)
    {
        FieldKey identifier = FileAttachmentContainer.fileUploadFieldKey(fieldIdentifier);
        return enableAndWait(identifier, elementCache().fileField(identifier));
    }

    /**
     * @param fieldIdentifier Identifier for the field; name ({@link String}) or fieldKey ({@link FieldKey})
     * @param checked value to set
     * @return this component
     */
    public EntityBulkUpdateDialog setBooleanField(CharSequence fieldIdentifier, boolean checked)
    {
        enableAndWait(fieldIdentifier, elementCache().checkbox(fieldIdentifier)).set(checked);
        return this;
    }

    private <T extends Component<?>> T enableAndWait(CharSequence fieldIdentifier, T formItem)
    {
        setEditableState(fieldIdentifier, true);
        // "Clickable" means visible and enabled
        waiter().until(ExpectedConditions.elementToBeClickable(formItem.getComponentElement()));
        return formItem;
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

        // Amount and Units is an example that has a "hide-label" for StoredAmount
        List<WebElement> hiddenLabels = Locator.tagWithClass("label", "hide-label").withAttribute("for")
                .findElements(elementCache());
        labels.addAll(hiddenLabels);

        return labels.stream().map(a -> FieldKey.fromFieldKey(a.getDomAttribute("for")).getFullName()).toList();
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

    // dismiss the dialog

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
        AuditLogHelper.AuditEvent auditEventName = auditLogHelper.getAuditEventNameFromURL();
        if (!skipAuditEventCheck && auditEventName != null && !TestProperties.isTrialServer())
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

    protected class ElementCache extends EntityBulkDialog.ElementCache
    {
        @Override
        public WebElement formRow(CharSequence fieldIdentifier)
        {
            String fieldKey = FieldKey.fromName(fieldIdentifier).toString();
            return Locator.tagWithClass("div", "row")
                    .withDescendant(Locator.tagWithAttribute("label", "for", fieldKey))
                    .waitForElement(this, WAIT_TIMEOUT);
        }

        public ToggleButton getToggle(CharSequence fieldIdentifier)
        {
            return new ToggleButton.ToggleButtonFinder(getDriver()).waitFor(formRow(fieldIdentifier));
        }

        public FileUploadField fileField(CharSequence fieldIdentifier)
        {
            return new FileUploadField(Locator.tagWithClass("div", "col-xs-12").findElementOrNull(formRow(fieldIdentifier)), getDriver());
        }

        final Locator.XPathLocator commentInputLocator = Locator.tagWithId("textarea", "actionComments");
        final WebElement commentInput = commentInputLocator.refindWhenNeeded(this);

        final WebElement updateButton = Locator.tagWithClass("button", "btn-success").findWhenNeeded(this);
    }

}
