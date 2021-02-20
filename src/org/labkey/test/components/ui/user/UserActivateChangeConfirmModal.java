package org.labkey.test.components.ui.user;

import org.labkey.test.WebDriverWrapper;
import org.labkey.test.components.bootstrap.ModalDialog;
import org.labkey.test.components.ui.ConfirmationDialog;
import org.labkey.test.pages.LabKeyPage;

import java.util.function.Supplier;

/**
 * Wraps the UI component defined by 'internal/components/user/UserActivateChangeConfirmModal.tsx'
 */
public class UserActivateChangeConfirmModal<SourcePage extends WebDriverWrapper, ConfirmPage extends LabKeyPage<?>> extends ConfirmationDialog<SourcePage, ConfirmPage>
{
    private final boolean _activate;

    public UserActivateChangeConfirmModal(boolean activate, SourcePage sourcePage)
    {
        this(activate, sourcePage, () -> null);
    }

    protected UserActivateChangeConfirmModal(boolean activate, SourcePage sourcePage, Supplier<ConfirmPage> confirmPageSupplier)
    {
        super(new ModalDialog.ModalDialogFinder(sourcePage.getDriver()).withTitle("user"), sourcePage, confirmPageSupplier);
        _activate = activate;
    }

    public ConfirmPage confirmDeactivate()
    {
        return confirm();
    }

    public ConfirmPage confirmReactivate()
    {
        return confirm();
    }

    @Override
    protected String getConfirmButtonText()
    {
        return "Yes, " + (_activate ? "Reactivate" : "Deactivate");
    }
}
