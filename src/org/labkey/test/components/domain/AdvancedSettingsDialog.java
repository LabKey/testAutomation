package org.labkey.test.components.domain;

import org.labkey.test.Locator;
import org.labkey.test.components.PropertiesEditor;
import org.labkey.test.components.bootstrap.ModalDialog;
import org.labkey.test.components.html.Checkbox;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

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
        return this;
    }

    public boolean showOnDetailsView()
    {
        return elementCache().showInDetailsView.get();
    }
    public AdvancedSettingsDialog showOnDetailsView(boolean checked)
    {
        elementCache().showInDetailsView.set(checked);
        return this;
    }

    public String getPHILevel()
    {
        return getWrapper().getFormElement(elementCache().phiSelect);
    }
    public AdvancedSettingsDialog setPHILevel(PropertiesEditor.PhiSelectType phiLevel)
    {
        getWrapper().setFormElement(elementCache().phiSelect, phiLevel.getText());
        return this;
    }

    public boolean enableMeasure()
    {
        return elementCache().enableMeasure.get();
    }
    public AdvancedSettingsDialog enableMeasure(boolean checked)
    {
        elementCache().enableMeasure.set(checked);
        return this;
    }

    public boolean enableDimension()
    {
        return elementCache().enableDimension.get();
    }
    public AdvancedSettingsDialog enableDimension(boolean checked)
    {
        elementCache().enableDimension.set(checked);
        return this;
    }

    public boolean enableRecommendedVariable()
    {
        return elementCache().recommendedVariable.get();
    }
    public AdvancedSettingsDialog enableRecommendedVariable(boolean checked)
    {
        elementCache().recommendedVariable.set(checked);
        return this;
    }

    public boolean missingValueEnabled()
    {
        return elementCache().enableMissingValues.get();
    }
    public AdvancedSettingsDialog enableMissingValue(boolean checked)
    {
        elementCache().enableMissingValues.set(checked);
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

        // misc options
        public WebElement phiSelect = Locator.tagWithAttribute("select", "name", "domainpropertiesrow-PHI")
                .findWhenNeeded(this);

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
