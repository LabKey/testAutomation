package org.labkey.test.components.query;

import org.labkey.test.components.QueryMetadataEditorPage;
import org.labkey.test.components.bootstrap.ModalDialog;
import org.labkey.test.components.glassLibrary.components.ReactSelect;


public class AliasFieldDialog extends ModalDialog
{
    private QueryMetadataEditorPage _page;

    public AliasFieldDialog(QueryMetadataEditorPage page)
    {
        super(new ModalDialogFinder(page.getDriver()).withTitle("Choose a field to wrap"));
        _page = page;
    }

    public AliasFieldDialog selectAliasField(String fieldName)
    {
        ReactSelect.finder(getDriver()).waitFor(this).select(fieldName);
        return this;
    }

    public QueryMetadataEditorPage clickApply()
    {
        dismiss("OK");
        return _page;
    }
}
