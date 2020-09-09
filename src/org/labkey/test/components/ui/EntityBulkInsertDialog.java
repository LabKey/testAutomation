package org.labkey.test.components.ui;

import org.labkey.test.BootstrapLocators;
import org.labkey.test.Locator;
import org.labkey.test.components.bootstrap.ModalDialog;
import org.labkey.test.components.glassLibrary.components.FilteringReactSelect;
import org.labkey.test.components.html.Checkbox;
import org.labkey.test.components.html.Input;
import org.openqa.selenium.Keys;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebElement;

import java.util.List;

public class EntityBulkInsertDialog extends ModalDialog
{
    private EntityInsertPanel _panel;

    public EntityBulkInsertDialog(EntityInsertPanel panel)
    {
        this(new ModalDialogFinder(panel.getDriver()).withTitle("Bulk Creation of"));
        _panel = panel;
    }

    private EntityBulkInsertDialog(ModalDialogFinder finder)
    {
        super(finder);
    }

    public EntityBulkInsertDialog setQuantity(int quantity)
    {
        return setQuantity(Integer.toString(quantity));
    }

    public EntityBulkInsertDialog setQuantity(String quantity)
    {
        getWrapper().setFormElement(elementCache().quantity, quantity);
        return this;
    }

    public String getQuantity()
    {
        return getWrapper().getFormElement(elementCache().quantity);
    }

    public EntityBulkInsertDialog setDescription(String description)
    {
        getWrapper().setFormElement(elementCache().description, description);
        return this;
    }

    public String getDescription()
    {
        return getWrapper().getFormElement(elementCache().description);
    }

    public EntityBulkInsertDialog setTextField(String fieldCaption, String value)
    {
        String forField = Locator.xpath("//div[@class='modal-body']//label[./span[contains(text(), '" + fieldCaption + "')]]").findElement(getComponentElement()).getAttribute("for");
        getWrapper().setFormElement(Locator.tagWithId("input", forField), value);
        return this;
    }

    public String getTextField(String fieldCaption)
    {
        String forField = Locator.xpath("//div[@class='modal-body']//label[./span[contains(text(), '" + fieldCaption + "')]]").findElement(getComponentElement()).getAttribute("for");
        return getWrapper().getFormElement(Locator.tagWithId("input", forField));
    }

    public EntityBulkInsertDialog setSelectionField(String fieldCaption, List<String> selectValues)
    {
        FilteringReactSelect reactSelect = FilteringReactSelect.finder(getDriver()).followingLabelWithSpan(fieldCaption).find();
        selectValues.forEach(s -> {reactSelect.filterSelect(s);});
        return this;
    }

    public List<String> getSelectionField(String fieldCaption)
    {
        FilteringReactSelect reactSelect = FilteringReactSelect.finder(getDriver()).followingLabelWithSpan(fieldCaption).find();
        return reactSelect.getSelections();
    }

    public EntityBulkInsertDialog setFieldWithId(String id, String value)
    {
        getWrapper().setFormElement(Locator.tagWithId("input", id), value);
        return this;
    }

    public String getFieldWithId(String id)
    {
        return getWrapper().getFormElement(Locator.tagWithId("input", id));
    }

    public EntityBulkInsertDialog setDateField(String fieldKey, String dateString)
    {
        Input input = elementCache().textInput(fieldKey);
        input.set(dateString);
        input.getComponentElement().sendKeys(Keys.ENTER);               // enter to collapse the datepicker
        getWrapper().waitFor(()-> !isDatePickerExpanded(), 1500);
        return this;
    }

    private boolean isDatePickerExpanded()
    {
        return Locator.tagWithClass("div", "react-datepicker-popper").existsIn(this);
    }

    public String getDateField(String fieldKey)
    {
        return elementCache().textInput(fieldKey).get();
    }

    public EntityBulkInsertDialog setBooleanField(String fieldKey, boolean checked)
    {
        Checkbox box = getCheckBox(fieldKey);
        box.set(checked);
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

    public void clickAddRows()
    {
        elementCache().addRowsButton.click();
        waitForClose();

        // todo: maybe wait for the grid in the entityinsertPanel to update

        try
        {
            if (BootstrapLocators.errorBanner.findElement(getComponentElement()).isDisplayed())
                throw new IllegalStateException("Error message on dialog: '" + BootstrapLocators.errorBanner.findElement(getComponentElement()).getText() + "'.");
        }
        catch(StaleElementReferenceException stale)
        {
            // Do nothing if stale.
        }
    }

    public void clickCancel()
    {
        elementCache().cancelButton.click();
        waitForClose();

        try
        {
            if (BootstrapLocators.errorBanner.findElement(getComponentElement()).isDisplayed())
                throw new IllegalStateException("Error message on dialog: '" + BootstrapLocators.errorBanner.findElement(getComponentElement()).getText() + "'.");

        }
        catch (StaleElementReferenceException stale)
        {
            // Do nothing if stale.
        }
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
                    .findElement(this);
        }

        public Input textInput(String fieldKey)
        {
            WebElement inputEl = textInputLoc.findElement(elementCache().formRow(fieldKey));
            return new Input(inputEl, getDriver());
        }

        WebElement cancelButton = Locator.tagWithClass("button", "test-loc-cancel-button")
                .findWhenNeeded(getComponentElement());

        WebElement addRowsButton = Locator.tagWithClass("button", "test-loc-submit-for-edit-button")
                .findWhenNeeded(getComponentElement());


        WebElement quantity = Locator.tagWithId("input", "numItems")
                .findWhenNeeded(getComponentElement());

        WebElement description = Locator.tagWithId("textarea", "Description")
                .findWhenNeeded(getComponentElement());

        final Locator textInputLoc = Locator.tagWithAttribute("input", "type", "text");
        final Locator numberInputLoc = Locator.tagWithAttribute("input", "type", "number");
        final Locator checkBoxLoc = Locator.tagWithAttribute("input", "type", "checkbox");
    }

}
