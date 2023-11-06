package org.labkey.test.components.query;

import org.labkey.test.components.bootstrap.ModalDialog;
import org.labkey.test.components.react.ReactSelect;
import org.labkey.test.pages.query.EditMetadataPage;


public class AliasFieldDialog extends ModalDialog
{
    private final EditMetadataPage _page;

    public AliasFieldDialog(EditMetadataPage page)
    {
        super(new ModalDialogFinder(page.getDriver()).withTitle("Choose a field to wrap"));
        _page = page;
    }

    public AliasFieldDialog selectAliasField(String fieldName)
    {
        ReactSelect.finder(getDriver()).waitFor(this).select(fieldName);
        return this;
    }

    public EditMetadataPage clickApply()
    {
        dismiss("OK");
        return _page;
    }
}
