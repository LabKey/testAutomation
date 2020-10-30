package org.labkey.test.pages.assay.elisa;

import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.WebTestHelper;
import org.labkey.test.components.glassLibrary.components.FilteringReactSelect;
import org.labkey.test.components.glassLibrary.components.ReactSelect;
import org.labkey.test.components.html.Checkbox;
import org.labkey.test.pages.LabKeyPage;
import org.labkey.test.pages.assay.AssayDataPage;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.util.List;
import java.util.Optional;

/**
 * To access this page, click 'run details' on a row in an ELISA assay.
 * It has recently been updated to be rendered in react, so expect it to be different from other assay run details views
 */
public class ElisaRunDetailsPage extends LabKeyPage<ElisaRunDetailsPage.ElementCache>
{
    public ElisaRunDetailsPage(WebDriver driver)
    {
        super(driver);
    }

    public static ElisaRunDetailsPage beginAt(WebDriverWrapper webDriverWrapper)
    {
        return beginAt(webDriverWrapper, webDriverWrapper.getCurrentContainerPath());
    }

    public static ElisaRunDetailsPage beginAt(WebDriverWrapper webDriverWrapper, String containerPath)
    {
        webDriverWrapper.beginAt(WebTestHelper.buildURL("assay", containerPath, "assayRunDetails"));
        return new ElisaRunDetailsPage(webDriverWrapper.getDriver());
    }

    @Override
    protected void waitForPage()
    {
        waitFor(()-> {
            try{
                return elementCache().calibrationCurvePanel.isDisplayed() &&
                        elementCache().dataSelectionsPanel.isDisplayed();
            }catch (NoSuchElementException retry)
            {
                return false;
            }
        },"the page did not initialize in time", WAIT_FOR_PAGE);
    }

    public Optional<WebElement> getAlertWarning()
    {
        return elementCache().alertWarning();
    }

    // Data Selections
    public boolean isPlateSelectPresent()
    {
        return elementCache().plateNameSelect().isPresent();
    }

    public ElisaRunDetailsPage selectPlate(String plateName)
    {
        elementCache().plateNameSelect().get().select(plateName);
        return this;
    }

    public String getSelectedPlate()
    {
        return elementCache().plateNameSelect().get().getValue();
    }

    public boolean isSpotSelectPresent()
    {
        return elementCache().spotSelect().isPresent();
    }

    public ElisaRunDetailsPage selectSpot(String spot)
    {
        elementCache().spotSelect().get().select(spot);
        return this;
    }

    public String getSelectedSpot()
    {
        return elementCache().spotSelect().get().getValue();
    }

    public ElisaRunDetailsPage setSelectedSamples(List<String> samples)
    {
        setShowAllSamples(false);
        for(String sample : samples)
            elementCache().sampleSelect.typeAheadSelect(sample);
        return this;
    }

    public List<String> getAvailableSamples()
    {
        setShowAllSamples(false);
        return elementCache().sampleSelect.getOptions();
    }

    public List<String> getSelectedSamples()
    {
        setShowAllSamples(false);
        return elementCache().sampleSelect.getSelections();
    }

    /**
     * to interact with the samples select, uncheck this box
     * @param checked
     * @return
     */
    public ElisaRunDetailsPage setShowAllSamples(boolean checked)
    {
        elementCache().showSamplesBox.set(checked);
        waitFor(()-> elementCache().showSamplesBox.isEnabled(), 1500);
        return this;
    }

    /**
     * to interact with the controls select, uncheck this box
     * @param checked
     * @return
     */
    public ElisaRunDetailsPage setShowAllControls(boolean checked)
    {
        elementCache().showControlsBox.set(checked);
        waitFor(()-> elementCache().controlSelect.isEnabled(), 1500);
        return this;
    }

    public ElisaRunDetailsPage setSelectedControls(List<String> samples)
    {
        setShowAllControls(false);
        for(String sample : samples)
            elementCache().controlSelect.typeAheadSelect(sample);
        return this;
    }
    public List<String> getSelectedControls()
    {
        return elementCache().controlSelect.getSelections();
    }

    public List<String> getAvailableControls()
    {
        elementCache().showControlsBox.uncheck();
        return elementCache().controlSelect.getOptions();
    }

    // CALIBRATION CURVE
    public AssayDataPage clickViewResultsGrid()
    {
        shortWait().until(ExpectedConditions.elementToBeClickable(elementCache().viewResultsGridBtn));
        openLinkInNewWindow(elementCache().viewResultsGridBtn);  // opens in a new window anyhow
        switchToWindow(1);
        return new AssayDataPage(getDriver());
    }

    // CURVE FIT
    public ElisaRunDetailsPage setShowCurveFitLineCheckboxSelected(boolean show)
    {
        elementCache().showCurveFitLineBox.set(show);
        return this;
    }

    public boolean getShowCurveFitLineCheckboxSelected()
    {
        return elementCache().showCurveFitLineBox.get();
    }

    public boolean getCurveFitParamsShown()
    {
        return elementCache().fitParameters().isPresent();
    }
    public boolean getRSquaredShown()
    {
        return elementCache().rSquared().isPresent();
    }

    public String getCurveFitParams()
    {
        String params = elementCache().fitParameters().get().getText();
        return params.replace("\n", "")
                .replace(" ", "");    // strip newlines and spaces out here for ready comparison to db values
    }
    public String getRSquared()
    {
        return elementCache().rSquared().get().getText();
    }

    @Override
    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    protected class ElementCache extends LabKeyPage<?>.ElementCache
    {
        Locator dataPanelWithTitle(String title)
        {
            return Locator.tagWithClass("div", "panel-default")
                    .withChild(Locator.tagWithClass("div", "panel-heading").withText(title));
        }

        // data selections
        WebElement dataSelectionsPanel = dataPanelWithTitle("Data Selections").findWhenNeeded(this);
        Optional<ReactSelect> plateNameSelect()
        {
            return ReactSelect.finder(getDriver()).withName("plateName").timeout(WAIT_FOR_JAVASCRIPT)
                    .findOptional(dataSelectionsPanel);
        }
        Optional<ReactSelect> spotSelect()
        {
            return ReactSelect.finder(getDriver()).withName("spot").timeout(WAIT_FOR_JAVASCRIPT)
                    .findOptional(dataSelectionsPanel);
        }

        WebElement samplesRow = Locator.tagWithClass("div", "plot-options-input-row")
                .withChild(Locator.tagWithClass("div", "plot-options-field-label").containing("Samples"))
                .findWhenNeeded(dataSelectionsPanel);
        Checkbox showSamplesBox = new Checkbox(Locator.checkboxByName("showAllSamples").findWhenNeeded(samplesRow));
        FilteringReactSelect sampleSelect = FilteringReactSelect.finder(getDriver()).findWhenNeeded(samplesRow);

        WebElement controlsRow = Locator.tagWithClass("div", "plot-options-input-row")
                .withChild(Locator.tagWithClass("div", "plot-options-field-label").containing("Controls"))
                .findWhenNeeded(dataSelectionsPanel);
        Checkbox showControlsBox = new Checkbox(Locator.checkboxByName("showAllControls").findWhenNeeded(controlsRow));
        FilteringReactSelect controlSelect = FilteringReactSelect.finder(getDriver()).findWhenNeeded(controlsRow);

        // plot options
        WebElement plotOptionsPanel = dataPanelWithTitle("Plot Options").findWhenNeeded(this);

        // curve fit
        WebElement curveFitPanel = Locator.tagWithClass("div", "panel-default")
                .withChild(Locator.tagWithClass("div", "panel-heading").startsWith("Curve Fit:"))
                .findWhenNeeded(this);
        Checkbox showCurveFitLineBox = new Checkbox(Locator.checkbox().findWhenNeeded(curveFitPanel));
        Optional<WebElement> fitParameters()
        {
            return Locator.tagWithClass("div", "curve-fit-field-label").startsWith("Fit Parameters:")
                    .followingSibling("pre").findOptionalElement(curveFitPanel);
        }
        Optional<WebElement>  rSquared()
        {
            return Locator.tagWithClass("div", "curve-fit-field-label").startsWith("R Squared:")
                    .findOptionalElement(curveFitPanel);
        }

        // calibration curve
        WebElement calibrationCurvePanel = dataPanelWithTitle("Calibration Curve").findWhenNeeded(this);
        WebElement viewResultsGridBtn = Locator.linkWithText("View Results Grid").findWhenNeeded(calibrationCurvePanel);
        WebElement exportToPngBtn = Locator.linkWithText("Export To PNG").findWhenNeeded(calibrationCurvePanel);
        WebElement exportToPDFBtn = Locator.linkWithText("Export to PDF").findWhenNeeded(calibrationCurvePanel);

        Optional<WebElement> alertWarning()
        {
            return Locator.tagWithClass("div", "alert-warning").findOptionalElement(getDriver());
        }
    }
}
