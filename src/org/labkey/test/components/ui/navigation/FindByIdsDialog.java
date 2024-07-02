package org.labkey.test.components.ui.navigation;

import org.apache.commons.lang3.StringUtils;
import org.labkey.test.Locator;
import org.labkey.test.components.bootstrap.ModalDialog;
import org.labkey.test.components.html.RadioButton;
import org.labkey.test.components.react.BaseReactSelect;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import java.util.List;

import static org.labkey.test.WebDriverWrapper.waitFor;

/**
 * Wraps 'labkey-ui-component' defined in <code>internal/components/search/FindByIdsModal.tsx</code>
 * TODO: Move to package: 'org.labkey.test.components.ui.search'
 */
public class FindByIdsDialog extends ModalDialog
{
    public static final String TITLE = "Find Samples";

    public FindByIdsDialog(WebDriver driver)
    {
        super(new ModalDialog.ModalDialogFinder(driver).withTitle(TITLE));
    }

    public boolean isBarcodeChecked()
    {
        return elementCache().barcodeRadio.isChecked();
    }

    public boolean isSampleIdChecked()
    {
        return elementCache().sampleIDsRadio.isChecked();
    }

    public FindByIdsDialog chooseBarcodes()
    {
        elementCache().barcodeRadio.check();
        return this;
    }

    public FindByIdsDialog chooseSampleIDs()
    {
        elementCache().sampleIDsRadio.check();
        return this;
    }

    public FindByIdsDialog addIds(List<String> ids)
    {
        elementCache().idTextArea.sendKeys(StringUtils.join(ids, "\n"));
        return this;
    }

    public void clickCancel()
    {
        elementCache().cancelButton.click();
        waitForClose();
    }

    public void clickFindSamples()
    {
        elementCache().findSamplesButton.click();
        waitForClose();
    }

    public String clickFindSamplesExpectError()
    {
        elementCache().findSamplesButton.click();
        waitFor(()-> Locator.tagWithClass("div", "alert-danger").isDisplayed(getComponentElement()), 10_000);
        return Locator.tagWithClass("div", "alert-danger").findElement(getComponentElement()).getText();

    }

    @Override
    protected FindByIdsDialog.ElementCache newElementCache()
    {
        return new FindByIdsDialog.ElementCache();
    }

    @Override
    protected FindByIdsDialog.ElementCache elementCache()
    {
        return (FindByIdsDialog.ElementCache) super.elementCache();
    }

    protected class ElementCache extends ModalDialog.ElementCache
    {
        final RadioButton barcodeRadio = RadioButton.RadioButton(Locator.radioButtonByName("uniqueIds")).findWhenNeeded(getComponentElement());
        final RadioButton sampleIDsRadio = RadioButton.RadioButton(Locator.radioButtonByName("sampleIds")).findWhenNeeded(getComponentElement());
        final WebElement idTextArea = Locator.tag("textarea").findWhenNeeded(getComponentElement());

        final WebElement cancelButton = Locator.tagWithText("button", "Cancel")
                .findWhenNeeded(getComponentElement());
        final WebElement findSamplesButton = Locator.tagWithText("button", "Find Samples")
                .findWhenNeeded(getComponentElement());
    }
}
