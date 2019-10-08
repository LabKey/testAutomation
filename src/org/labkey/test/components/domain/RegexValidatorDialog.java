package org.labkey.test.components.domain;

import org.labkey.test.Locator;
import org.labkey.test.components.bootstrap.ModalDialog;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

public class RegexValidatorDialog extends ModalDialog
{
    private DomainFieldRow _row;

    public RegexValidatorDialog(DomainFieldRow row, ModalDialogFinder finder, WebDriver driver)
    {
        super(finder.waitFor().getComponentElement(), driver);
        _row = row;
    }

    public RegexValidatorDialog(DomainFieldRow row, WebDriver driver)
    {
        this(row, new ModalDialogFinder(driver).withTitle("" + row.getName()), driver);
    }

    public RegexValidatorPanel getValidationPanel()
    {
        return new RegexValidatorPanel.RegexValidatorPanelFinder(getDriver())
                .find(this);
    }

    public RegexValidatorPanel addValidationPanel(String name)
    {
        elementCache().addValidatorButton.click();
        RegexValidatorPanel panel = new RegexValidatorPanel.RegexValidatorPanelFinder(getDriver())
                .openedByName(null).find(this);
        panel.setName(name);
        return panel;
    }

    public DomainFieldRow clickApply()
    {
        dismiss("Apply");
        return _row;
    }

    public DomainFieldRow clickCancel()
    {
        dismiss("Cancel");
        return _row;
    }

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
        final WebElement addValidatorButton = Locator.tagWithClass("div", "domain-validation-add-btn")
                .findWhenNeeded(this);
    }

}
