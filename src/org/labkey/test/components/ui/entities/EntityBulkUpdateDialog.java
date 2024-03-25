package org.labkey.test.components.ui.entities;

import org.labkey.test.BootstrapLocators;
import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.components.Component;
import org.labkey.test.components.UpdatingComponent;
import org.labkey.test.components.bootstrap.ModalDialog;
import org.labkey.test.components.html.Checkbox;
import org.labkey.test.components.html.Input;
import org.labkey.test.components.react.FilteringReactSelect;
import org.labkey.test.components.react.ReactDateTimePicker;
import org.labkey.test.components.react.ToggleButton;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

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

    public EntityBulkUpdateDialog(WebDriver driver)
    {
        this(driver, UpdatingComponent.NO_OP);
    }

    public EntityBulkUpdateDialog(WebDriver driver, UpdatingComponent updatingComponent)
    {
        super(new ModalDialogFinder(driver).withTitle("Update "));
        _updatingComponent = updatingComponent;
    }

    // enable/disable field editable state

    public boolean isFieldEnabled(String columnTitle)
    {
        return elementCache().getToggle(columnTitle).isOn();
    }

    public EntityBulkUpdateDialog setEditableState(String columnTitle, boolean enable)
    {
        elementCache().getToggle(columnTitle).set(enable);
        return this;
    }

    private WebDriverWait waiter()
    {
        return new WebDriverWait(getDriver(), Duration.ofMillis(WAIT_TIMEOUT));
    }

    // interact with selection fields

    public EntityBulkUpdateDialog setSelectionField(String columnTitle, List<String> selectValues)
    {
        setEditableState(columnTitle, true);
        FilteringReactSelect reactSelect = elementCache().getSelect(columnTitle);
        WebDriverWrapper.waitFor(reactSelect::isEnabled,
                "the ["+columnTitle+"] reactSelect did not become enabled in time", WAIT_TIMEOUT);
        selectValues.forEach(reactSelect::filterSelect);
        return this;
    }

    public List<String> getSelectionOptions(String fieldKey)
    {
        return enableAndWait(fieldKey, elementCache().getSelect(fieldKey)).getOptions();
    }

    public List<String> getSelectionFieldValues(String fieldKey)
    {
        return elementCache().getSelect(fieldKey).getSelections();
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
        enableAndWait(fieldKey, elementCache().dateInput("sampleDate")).set(dateString);
        return this;
    }

    public String getDateField(String fieldKey)
    {
        return elementCache().dateInput(fieldKey).get();
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
        return BootstrapLocators.errorBanner.waitForElement(this, getWrapper().defaultWaitForPage).getText();
    }

    public List<String> getColumns()
    {
        List<WebElement> labels = Locator.tagWithClass("label", "control-label").withAttribute("for")
                .findElements(this);
        List<String> columns = new ArrayList<>();
        labels.stream().forEach(a -> columns.add(a.getAttribute("for")));
        return columns;
    }

    public EntityBulkUpdateDialog waitForColumnsToBe(List<String> expectedColumns, int waitMilliseconds)
    {
        WebDriverWrapper.waitFor(()-> expectedColumns.equals(getColumns()),
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
        _updatingComponent.doAndWaitForUpdate(() ->
        {
            elementCache().updateButton.click();
            waitForClose();
        });
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
        return new ElementCache();
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
            return FilteringReactSelect.finder(getDriver()).withNamedInput(fieldKey).findWhenNeeded(this);
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
            return new ReactDateTimePicker.ReactDateInputFinder(getDriver())
                    .withInputId(fieldKey).waitFor(formRow(fieldKey));
        }

        final Locator textInputLoc = Locator.tagWithAttribute("input", "type", "text");
        final Locator numberInputLoc = Locator.tagWithAttribute("input", "type", "number");
        final Locator checkBoxLoc = Locator.tagWithAttribute("input", "type", "checkbox");
        final Locator.XPathLocator commentInputLocator = Locator.tagWithId("textarea", "actionComments");
        final WebElement commentInput = commentInputLocator.refindWhenNeeded(getDriver());

        final WebElement updateButton = Locator.tagWithClass("button", "btn-success").findWhenNeeded(this);
    }

}
