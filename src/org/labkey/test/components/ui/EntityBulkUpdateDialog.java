package org.labkey.test.components.ui;

import org.labkey.test.Locator;
import org.labkey.test.components.bootstrap.ModalDialog;
import org.labkey.test.components.glassLibrary.components.FilteringReactSelect;
import org.labkey.test.components.html.Checkbox;
import org.labkey.test.components.html.Input;
import org.labkey.test.components.html.ToggleButton;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.List;


public class EntityBulkUpdateDialog extends ModalDialog
{
    public EntityBulkUpdateDialog(WebDriver driver)
    {
        this(new ModalDialogFinder(driver).withTitle("samples selected from"));
    }

    private EntityBulkUpdateDialog(ModalDialogFinder finder)
    {
        super(finder);
    }

    // enable/disable field editable state

    public boolean isFieldEnabled(String columnTitle)
    {
        return elementCache().getToggle(columnTitle).get();
    }

    public EntityBulkUpdateDialog setEditableState(String columnTitle, boolean enable)
    {
        elementCache().getToggle(columnTitle).set(enable);
        return this;
    }

    // interact with selection fields

    public EntityBulkUpdateDialog setSelectionField(String columnTitle, List<String> selectValues)
    {
        setEditableState(columnTitle, true);
        FilteringReactSelect reactSelect = elementCache().getSelect(columnTitle);
        getWrapper().waitFor(()-> !reactSelect.isDisabled(),
                "the ["+columnTitle+"] reactSelect did not become enabled in time", 2000);
        selectValues.forEach(s -> {reactSelect.filterSelect(s);});
        return this;
    }

    public List<String> getSelectionFieldValues(String fieldKey)
    {
        return elementCache().getSelect(fieldKey).getSelections();
    }

    public EntityBulkUpdateDialog setAliases(List<String> aliases)
    {
        return setSelectionField("Alias", aliases);
    }

    public List<String> getAliases()
    {
        return  getSelectionFieldValues("Alias");
    }

    public EntityBulkUpdateDialog setDescription(String description)
    {
        setEditableState("Description", true);
        Input input = new Input(Locator.textarea("Description").findElement(this), getDriver());
        getWrapper().waitFor(()-> input.getComponentElement().getAttribute("disabled")==null,
                "the input did not become enabled in time", 2000);
        input.set(description);
        return this;
    }

    public String getDescription()
    {
        return getWrapper().getFormElement(Locator.id("Description"));
    }

    // get/set text fields with ID

    public EntityBulkUpdateDialog setTextField(String fieldKey, String value)
    {
        Input input = textField(fieldKey);
        setEditableState(fieldKey, true);
        getWrapper().waitFor(()-> input.getComponentElement().getAttribute("disabled")==null,
                "the input did not become enabled in time", 2000);
        input.set(value);
        return this;
    }

    public String getTextField(String fieldKey)
    {
        return textField(fieldKey).get();
    }

    private Input textField(String fieldKey)
    {
        WebElement inputEl = elementCache().textInputLoc
                .findElement(elementCache().formRow(fieldKey));
        return new Input(inputEl, getDriver());
    }

    public EntityBulkUpdateDialog setFieldWithId(String id, String value)
    {
        getWrapper().setFormElement(Locator.id(id), value);
        return this;
    }

    public String getFieldWithId(String id)
    {
        return getWrapper().getFormElement(Locator.id(id));
    }


    public EntityBulkUpdateDialog setBooleanField(String fieldKey, boolean checked)
    {
        setEditableState(fieldKey, true);
        getCheckBox(fieldKey).set(checked);
        return this;
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

    // dismiss the dialog

    public void clickEditWithGrid()
    {
        dismiss("Edit with Grid");
    }

    public boolean isUpdateSamplesButtonEnabled()
    {
        WebElement btn = elementCache().updateSamplesButton.findElement(this);
        return btn.getAttribute("disabled") == null;
    }

    public void clickUpdateSamples()
    {
        if (!isUpdateSamplesButtonEnabled())
            getWrapper().log("the [Update Samples] button cannot be clicked, it is disabled");
        dismiss("Update Samples");
    }

    @Override
    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

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
                    .findElement(this);
        }

        public ToggleButton getToggle(String fieldKey)
        {
            return new ToggleButton.ToggleButtonFinder(getDriver()).find(formRow(fieldKey));
        }

        public FilteringReactSelect getSelect(String fieldKey)
        {
            return FilteringReactSelect.finder(getDriver()).find(formRow(fieldKey));
        }

        final Locator textInputLoc = Locator.tagWithAttribute("input", "type", "text");
        final Locator checkBoxLoc = Locator.tagWithAttribute("input", "type", "checkbox");

        Locator updateSamplesButton = Locator.tagWithClass("button", "test-loc-submit-button")
                .withText("Update Samples");
    }

}
