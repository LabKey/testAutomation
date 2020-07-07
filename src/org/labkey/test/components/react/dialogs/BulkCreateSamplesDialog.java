package org.labkey.test.components.react.dialogs;

import org.labkey.test.BootstrapLocators;
import org.labkey.test.Locator;
import org.labkey.test.components.bootstrap.ModalDialog;
import org.labkey.test.components.glassLibrary.components.FilteringReactSelect;
import org.labkey.test.components.glassLibrary.components.ReactSelect;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.List;

public class BulkCreateSamplesDialog extends ModalDialog
{

    public BulkCreateSamplesDialog(WebDriver driver)
    {
        this(new ModalDialogFinder(driver).withTitle("Bulk Creation of Samples"));
    }

    private BulkCreateSamplesDialog(ModalDialogFinder finder)
    {
        super(finder);
    }

    public BulkCreateSamplesDialog setQuantity(int quantity)
    {
        return setQuantity(Integer.toString(quantity));
    }

    public BulkCreateSamplesDialog setQuantity(String quantity)
    {
        getWrapper().setFormElement(elementCache().quantity, quantity);
        return this;
    }

    public String getQuantity()
    {
        return getWrapper().getFormElement(elementCache().quantity);
    }

    public BulkCreateSamplesDialog setDescription(String description)
    {
        getWrapper().setFormElement(elementCache().description, description);
        return this;
    }

    public String getDescription()
    {
        return getWrapper().getFormElement(elementCache().description);
    }

    public BulkCreateSamplesDialog setTextField(String fieldCaption, String value)
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

    private boolean waitForReactSelectReady(ReactSelect select)
    {
        getWrapper().waitFor(()->!select.getSelections().contains("Loading..."), "The reactSelect field did not populate in time.", 1_000);
        return true;
    }

    public BulkCreateSamplesDialog setSelectionField(String fieldCaption, List<String> selectValues)
    {
        FilteringReactSelect reactSelect = FilteringReactSelect.finder(getDriver()).followingLabelWithSpan(fieldCaption).find();
        getWrapper().waitFor(()-> reactSelect.isInteractive(), 5000);
        selectValues.forEach(s -> {reactSelect.typeAheadSelect(s);});
        return this;
    }

    public List<String> getSelectionField(String fieldCaption)
    {
        ReactSelect reactSelect = ReactSelect.finder(getDriver()).followingLabelWithSpan(fieldCaption).find();
        waitForReactSelectReady(reactSelect);
        return reactSelect.getSelections();
    }

    public BulkCreateSamplesDialog setFieldWithId(String id, String value)
    {
        getWrapper().setFormElement(Locator.tagWithId("input", id), value);
        return this;
    }

    public String getFieldWithId(String id)
    {
        return getWrapper().getFormElement(Locator.tagWithId("input", id));
    }

    public void clickAddRows()
    {
        elementCache().addRowsButton.click();

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
        return (ElementCache) super.elementCache();
    }

    @Override
    protected ElementCache elementCache()
    {
        return new ElementCache();
    }

    protected class ElementCache extends ModalDialog.ElementCache
    {
        WebElement cancelButton = Locator.tagWithClass("button", "test-loc-cancel-button")
                .findWhenNeeded(getComponentElement());

        WebElement addRowsButton = Locator.tagWithClass("button", "test-loc-submit-for-edit-button")
                .findWhenNeeded(getComponentElement());


        WebElement quantity = Locator.tagWithId("input", "numItems")
                .findWhenNeeded(getComponentElement());

        WebElement description = Locator.tagWithId("textarea", "Description")
                .findWhenNeeded(getComponentElement());

    }

}
