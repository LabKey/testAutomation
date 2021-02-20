package org.labkey.test.components.ui.user;

import org.labkey.test.WebDriverWrapper;
import org.labkey.test.components.ui.ConfirmationDialog;
import org.labkey.test.components.ui.DeleteConfirmationDialog;
import org.labkey.test.pages.LabKeyPage;

import java.util.function.Supplier;

/**
 * Wraps the UI component defined by 'internal/components/user/UserDeleteConfirmModal.tsx'
 */
public class UserDeleteConfirmModal<SourcePage extends WebDriverWrapper, ConfirmPage extends LabKeyPage<?>> extends DeleteConfirmationDialog<SourcePage, ConfirmPage>
{
    public UserDeleteConfirmModal(SourcePage sourcePage)
    {
        this(sourcePage, () -> null);
    }

    public UserDeleteConfirmModal(SourcePage sourcePage, Supplier<ConfirmPage> confirmPageSupplier)
    {
        super("delete", sourcePage, confirmPageSupplier);
    }

    @Override
    protected String getConfirmButtonText()
    {
        return "Yes, Permanently Delete";
    }
}
