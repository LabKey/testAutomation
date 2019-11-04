package org.labkey.test.components.domain;

import org.labkey.test.Locator;
import org.labkey.test.components.PropertiesEditor;
import org.labkey.test.components.bootstrap.ModalDialog;
import org.labkey.test.components.html.Checkbox;
import org.labkey.test.components.html.EnumSelect;
import org.labkey.test.components.html.SelectWrapper;
import org.labkey.test.pages.LabKeyPage;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;

import java.util.stream.Collectors;

public class AdvancedSettingsDialog extends ModalDialog
{
    private DomainFieldRow _row;

    private AdvancedSettingsDialog(DomainFieldRow row, ModalDialogFinder finder, WebDriver driver)
    {
        super(finder.waitFor().getComponentElement(), driver);
        _row = row;
    }

    public AdvancedSettingsDialog(DomainFieldRow row, WebDriver driver)
    {
        this(row, new ModalDialogFinder(driver).withTitle("Advanced Settings and Properties"), driver);
    }

    public boolean showInDefaultView()
    {
        return elementCache().showInDefaultView.get();
    }
    public AdvancedSettingsDialog showInDefaultView(boolean checked)
    {
        elementCache().showInDefaultView.set(checked);
        return this;
    }

    public boolean showOnInsertView()
    {
        return elementCache().showInInsertView.get();
    }
    public AdvancedSettingsDialog showOnInsertView(boolean checked)
    {
        elementCache().showInInsertView.set(checked);
        return this;
    }

    public boolean showOnUpdateView()
    {
        return elementCache().showInUpdateView.get();
    }
    public AdvancedSettingsDialog showOnUpdateView(boolean checked)
    {
        elementCache().showInUpdateView.set(checked);
        getWrapper().waitFor(()-> elementCache().showInUpdateView.get().equals(checked),
                "showInUpdateView checkbox was not set as expected", 1000);
        return this;
    }

    public boolean showOnDetailsView()
    {
        return elementCache().showInDetailsView.get();
    }
    public AdvancedSettingsDialog showOnDetailsView(boolean checked)
    {
        elementCache().showInDetailsView.set(checked);
        getWrapper().waitFor(()-> elementCache().showInDetailsView.get().equals(checked),
                "showInDetailsView checkbox was not set as expected", 1000);
        return this;
    }

    // default value options
    public String getDefaultValueType()
    {
        return elementCache().defaultTypeSelect.getFirstSelectedOption().getText();
    }
    public AdvancedSettingsDialog setDefaultValueType(PropertiesEditor.DefaultType type)
    {
        getWrapper().waitFor(()->  elementCache().defaultTypeSelect.getOptions()
                        .stream().map(WebElement::getText).collect(Collectors.toList()).contains(type.getText()),
                "default value select did not contain expected option in time", 1500);
        elementCache().defaultTypeSelect.set(type);
        return this;
    }

    public LabKeyPage clickDefaultValuesLink()
    {
        getWrapper().clickAndWait(Locator.linkWithText("Set Default Values"));
        return new LabKeyPage(getDriver());  // todo: return more strongly-typed page
    }

    public String getPHILevel()
    {
        return elementCache().phiSelect.getFirstSelectedOption().getText();
    }
    public AdvancedSettingsDialog setPHILevel(PropertiesEditor.PhiSelectType phiLevel)
    {
        getWrapper().waitFor(()->  elementCache().phiSelect.getOptions()
                        .stream().map(WebElement::getText).collect(Collectors.toList()).contains(phiLevel.getText()),
                "phiSelect did not contain phiLevel ["+phiLevel.getText()+"] in time", 1500);
        elementCache().phiSelect.selectByVisibleText(phiLevel.getText());
        return this;
    }

    public boolean enableExcludeDateShifting()
    {
        return elementCache().excludeDateShifting.get();
    }
    public AdvancedSettingsDialog enableExcludeDateShifting(boolean checked)
    {
        getWrapper().shortWait().until(ExpectedConditions.elementToBeClickable(
                elementCache().excludeDateShifting.getComponentElement()));
        elementCache().excludeDateShifting.set(checked);
        return this;
    }

    public boolean enableMeasure()
    {
        return elementCache().enableMeasure.get();
    }
    public AdvancedSettingsDialog enableMeasure(boolean checked)
    {
        getWrapper().shortWait().until(ExpectedConditions.elementToBeClickable(
                elementCache().enableMeasure.getComponentElement()));
        elementCache().enableMeasure.set(checked);
        return this;
    }

    public boolean enableDimension()
    {
        return elementCache().enableDimension.get();
    }
    public AdvancedSettingsDialog enableDimension(boolean checked)
    {
        getWrapper().shortWait().until(ExpectedConditions.elementToBeClickable(
                elementCache().enableDimension.getComponentElement()));
        elementCache().enableDimension.set(checked);
        getWrapper().waitFor(()-> elementCache().enableDimension.get().equals(checked),
                "enableDimension checkbox was not set as expected", 1000);
        return this;
    }

    public boolean enableRecommendedVariable()
    {
        return elementCache().recommendedVariable.get();
    }
    public AdvancedSettingsDialog enableRecommendedVariable(boolean checked)
    {
        elementCache().recommendedVariable.set(checked);
        getWrapper().waitFor(()-> elementCache().recommendedVariable.get().equals(checked),
                "recommendedVariable checkbox was not set as expected", 1000);
        return this;
    }

    public boolean missingValueEnabled()
    {
        return elementCache().enableMissingValues.get();
    }
    public AdvancedSettingsDialog enableMissingValue(boolean checked)
    {
        elementCache().enableMissingValues.set(checked);
        getWrapper().waitFor(()-> elementCache().enableMissingValues.get().equals(checked),
                "missingValue checkbox was not set as expected", 1000);
        return this;
    }

    public DomainFieldRow apply()
    {
        dismiss("Apply");
        return _row;
    }

    public DomainFieldRow cancel()
    {
        dismiss("Cancel");
        return _row;
    }

    @Override
    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    @Override
    protected ElementCache elementCache()
    {
        return  (ElementCache) super.elementCache();
    }

    protected class ElementCache extends ModalDialog.ElementCache
    {
        // display options checkboxes
        public Checkbox showInDefaultView = new Checkbox(
                Locator.input("domainpropertiesrow-hidden").findWhenNeeded(this));
        public Checkbox showInUpdateView = new Checkbox(
                Locator.input("domainpropertiesrow-shownInUpdateView").findWhenNeeded(this));
        public Checkbox showInInsertView = new Checkbox(
                Locator.input("domainpropertiesrow-shownInInsertView").findWhenNeeded(this));
        public Checkbox showInDetailsView = new Checkbox(
                Locator.input("domainpropertiesrow-showInDetailsView").findWhenNeeded(this));

        // default value options
        private final EnumSelect<PropertiesEditor.DefaultType> defaultTypeSelect =
                EnumSelect.EnumSelect(Locator.tagWithName("select", "domainpropertiesrow-defaultValueType"), PropertiesEditor.DefaultType.class)
                        .findWhenNeeded(this);

        // misc options
        public Select phiSelect = SelectWrapper.Select(Locator.tagWithAttribute("select", "name", "domainpropertiesrow-PHI"))
                .findWhenNeeded(this);
        public Checkbox excludeDateShifting = new Checkbox(
                Locator.input("domainpropertiesrow-excludeFromShifting").findWhenNeeded(this));
        public Checkbox enableMeasure = new Checkbox(
                Locator.input("domainpropertiesrow-measure").findWhenNeeded(this));
        public Checkbox enableDimension = new Checkbox(
                Locator.input("domainpropertiesrow-dimension").findWhenNeeded(this));
        public Checkbox recommendedVariable = new Checkbox(
                Locator.input("domainpropertiesrow-recommendedVariable").findWhenNeeded(this));
        public Checkbox enableMissingValues = new Checkbox(
                Locator.input("domainpropertiesrow-mvEnabled").findWhenNeeded(this));
    }

}
