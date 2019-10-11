package org.labkey.test.components.domain;

import org.labkey.test.Locator;
import org.labkey.test.components.bootstrap.ModalDialog;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.List;

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


    List<RegexValidatorPanel> validationPanels()
    {
        return new RegexValidatorPanel.RegexValidatorPanelFinder(getDriver())
                .findAll(this);
    }
    public RegexValidatorPanel getValidationPanel()
    {
        return validationPanels().get(0);
    }
    public RegexValidatorPanel getValidationPanel(int index)
    {
        return new RegexValidatorPanel.RegexValidatorPanelFinder(getDriver())
                .withIndex(index).find(this);
    }
    public RegexValidatorPanel getValidationPanel(String name)
    {
        return new RegexValidatorPanel.RegexValidatorPanelFinder(getDriver())
                .openedByName(name).find(this);
    }

    public RegexValidatorPanel addValidationPanel(String name)
    {
        int targetIndex = validationPanels().size();
        elementCache().addValidatorButton.click();      // adds a new validator clause panel to the dialog
        RegexValidatorPanel panel = new RegexValidatorPanel.RegexValidatorPanelFinder(getDriver())
                .withIndex(targetIndex).find(this);     // find it by assuming its ID will have index lastId +1
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
