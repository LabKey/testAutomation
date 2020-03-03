package org.labkey.test.components.list;

import org.labkey.test.Locator;
import org.labkey.test.components.bootstrap.ModalDialog;
import org.labkey.test.pages.core.login.SvgCheckbox;
import org.labkey.test.pages.list.EditListDefinitionPage;

public class AdvancedListSettingsDialog extends ModalDialog
{
    private EditListDefinitionPage _page;



    public AdvancedListSettingsDialog(EditListDefinitionPage page)
    {
        super(new ModalDialogFinder(page.getDriver()).withTitle("Advanced List Settings"));
        _page = page;
    }

    public AdvancedListSettingsDialog setIndexFileAttachments(boolean checked)
    {
        elementCache().checkbox("Index file attachments").set(checked);
        return this;
    }

    public EditListDefinitionPage clickApply()
    {
        dismiss("Apply");
        return _page;
    }

    public EditListDefinitionPage clickCancel()
    {
        dismiss("Cancel");
        return _page;
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

        SvgCheckbox checkbox(String labelText)
        {
            Locator loc = Locator.tagWithClass("span", "list__advanced-settings-model__index-checkbox")
                    .withChild(Locator.tagWithText("span", labelText))
                    .child(Locator.tagWithClass("span", "list__properties__checkbox--no-highlight"));
            return new SvgCheckbox(loc.waitForElement(this, 2000), getDriver());
        }
    }
}
