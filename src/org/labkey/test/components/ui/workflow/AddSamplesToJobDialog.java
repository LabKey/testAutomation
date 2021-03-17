package org.labkey.test.components.ui.workflow;

import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.components.bootstrap.ModalDialog;
import org.labkey.test.components.react.ReactSelect;
import org.labkey.test.pages.LabKeyPage;
import org.openqa.selenium.WebElement;

import java.util.function.Supplier;

import static org.labkey.test.WebDriverWrapper.waitFor;

public class AddSamplesToJobDialog<SourcePage extends WebDriverWrapper, ConfirmPage extends LabKeyPage> extends ModalDialog
{
    private  SourcePage _sourcePage;
    private  Supplier<ConfirmPage> _confirmPageSupplier;

    public AddSamplesToJobDialog(SourcePage sourcePage)
    {
        this(sourcePage, () -> null);
    }

    public AddSamplesToJobDialog(SourcePage sourcePage, Supplier<ConfirmPage> confirmPageSupplier)
    {
        this("Choose the existing job to", sourcePage, confirmPageSupplier);
    }

    protected AddSamplesToJobDialog(String partialBodyText, SourcePage sourcePage, Supplier<ConfirmPage> confirmPageSupplier)
    {
        this(new ModalDialog.ModalDialogFinder(sourcePage.getDriver()).withBodyTextContaining(partialBodyText), sourcePage, confirmPageSupplier);
    }

    protected AddSamplesToJobDialog(ModalDialogFinder finder, SourcePage sourcePage, Supplier<ConfirmPage> confirmPageSupplier)
    {
        super(finder);
        _sourcePage = _sourcePage;
        _confirmPageSupplier = _confirmPageSupplier;
    }

    public ConfirmPage addToExistingJob(String jobName)
    {
        waitFor( ()-> elementCache().jobList.getComponentElement().isDisplayed(), 5_000 );

        elementCache().jobList.select(jobName);
        dismiss("Add to Job");

        return _confirmPageSupplier.get();
    }

    public SourcePage clickBackToSamplesGrid()
    {
        dismiss("Back to Samples Grid");
        return _sourcePage;
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

        WebElement addToExistingJobButton = Locator.tagWithClassContaining("button", "btn-block")
                .withText("Add to Existing Job")
                .findWhenNeeded(getComponentElement());

        WebElement createNewJobButton = Locator.tagWithClassContaining("button", "btn-block")
                .withText("Create New Job")
                .findWhenNeeded(getComponentElement());

        ReactSelect jobList = ReactSelect.finder(getDriver()).findWhenNeeded();
    }

}
