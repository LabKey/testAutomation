package org.labkey.test.components.bootstrap;

import org.labkey.test.Locator;
import org.labkey.test.components.html.Checkbox;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

public class AdvancedSettingsDialog extends ModalDialog
{
    private AdvancedSettingsDialog(ModalDialogFinder finder, WebDriver driver)
    {
        super(finder.waitFor().getComponentElement(), driver);
    }

    public AdvancedSettingsDialog(WebDriver driver)
    {
        this(new ModalDialogFinder(driver).withTitle("Advanced Settings and Properties"), driver);
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
    public AdvancedSettingsDialog setPHILevel(String phiLevel)
    {
        getWrapper().setFormElement(elementCache().phiSelect, phiLevel);
        return this;
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
    }


}
