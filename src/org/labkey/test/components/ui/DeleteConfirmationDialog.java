package org.labkey.test.components.ui;

import org.jetbrains.annotations.NotNull;
import org.labkey.remoteapi.CommandException;
import org.labkey.test.BootstrapLocators;
import org.labkey.test.Locator;
import org.labkey.test.TestProperties;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.WebTestHelper;
import org.labkey.test.components.UpdatingComponent;
import org.labkey.test.components.bootstrap.ModalDialog;
import org.labkey.test.components.html.Input;
import org.labkey.test.util.AuditLogHelper;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.io.IOException;
import java.util.function.Function;
import java.util.function.Supplier;

public class DeleteConfirmationDialog<ConfirmPage extends WebDriverWrapper> extends ModalDialog
{
    private final Function<Runnable, ConfirmPage> _confirmationSynchronizationFunction;
    private boolean skipAuditEventCheck = false;

    public DeleteConfirmationDialog(@NotNull WebDriverWrapper sourcePage, Supplier<ConfirmPage> confirmPageSupplier)
    {
        this(sourcePage, UpdatingComponent.NO_OP, confirmPageSupplier);
    }

    public DeleteConfirmationDialog(@NotNull ConfirmPage sourcePage, UpdatingComponent updatingComponent)
    {
        this(sourcePage, updatingComponent, () -> sourcePage);
    }

    public DeleteConfirmationDialog(@NotNull WebDriverWrapper sourcePage, WebElement staleOnConfirmElement)
    {
        this(sourcePage, staleOnConfirmElement, () -> null);
    }

    public DeleteConfirmationDialog(@NotNull WebDriverWrapper sourcePage, WebElement staleOnConfirmElement, Supplier<ConfirmPage> confirmPageSupplier)
    {
        this(sourcePage, runnable -> {
            runnable.run();
            sourcePage.longWait().until(ExpectedConditions.stalenessOf(staleOnConfirmElement));
        }, confirmPageSupplier);
    }

    public DeleteConfirmationDialog(@NotNull WebDriverWrapper sourcePage, UpdatingComponent updatingComponent, Supplier<ConfirmPage> confirmPageSupplier)
    {
        this("delete", sourcePage, runnable -> {
            updatingComponent.doAndWaitForUpdate(runnable);
            return confirmPageSupplier.get();
        });
    }

    protected DeleteConfirmationDialog(String partialTitle, @NotNull WebDriverWrapper sourcePage, Function<Runnable, ConfirmPage> confirmationSynchronizationFunction)
    {
        this(new ModalDialog.ModalDialogFinder(sourcePage.getDriver()).withTitleIgnoreCase(partialTitle), confirmationSynchronizationFunction);
    }

    protected DeleteConfirmationDialog(ModalDialogFinder finder, Function<Runnable, ConfirmPage> confirmationSynchronizationFunction)
    {
        super(finder);
        _confirmationSynchronizationFunction = confirmationSynchronizationFunction;
    }

    @Override
    protected void waitForReady()
    {
        WebDriverWrapper.waitFor(()-> elementCache().body.isDisplayed() &&
                        !elementCache().title.getText().isEmpty() &&
                        !BootstrapLocators.loadingSpinner.existsIn(this),
                "The delete confirmation dialog did not become ready.", 1_000);
    }

    public DeleteConfirmationDialog<ConfirmPage> setSkipAuditEventCheck(boolean skipAuditEventCheck)
    {
        this.skipAuditEventCheck = skipAuditEventCheck;
        return this;
    }

    public void cancelDelete()
    {
        this.dismiss("Cancel");
    }

    public ConfirmPage confirmDelete()
    {
        return confirmDelete(10);
    }

    public ConfirmPage confirmDelete(Integer waitSeconds)
    {
        Integer count = getCountFromTitle();
        AuditLogHelper.AuditEvent auditEventName = getAuditEvent();

        var confirmPage = _confirmationSynchronizationFunction.apply(() -> this.dismiss("Yes, Delete", waitSeconds));

        if (!skipAuditEventCheck && count != null && auditEventName != null && !TestProperties.isTrialServer())
            verifyAuditEvents(getWrapper(), getWrapper().getCurrentProject(), auditEventName, count);

        return confirmPage;
    }

    public AuditLogHelper.AuditEvent getAuditEvent()
    {
        return new AuditLogHelper(getWrapper()).getAuditEventNameFromURL();
    }

    public static void verifyAuditEvents(WebDriverWrapper wrapper, String containerPath, AuditLogHelper.AuditEvent auditEventName, int entityCount)
    {
        try
        {
            AuditLogHelper auditLogHelper = new AuditLogHelper(wrapper, () -> WebTestHelper.getRemoteApiConnection(false));
            auditLogHelper.checkAuditEventDiffCountForLastTransaction(containerPath, auditEventName, 0, entityCount);
        }
        catch (CommandException | IOException e)
        {
            throw new RuntimeException(e);
        }
    }

    public Boolean isDeleteEnabled()
    {
        return isDismissEnabled("Yes, Delete");
    }

    public ConfirmPage confirmPermanentlyDelete()
    {
        return confirmPermanentlyDelete(10);
    }

    public ConfirmPage confirmPermanentlyDelete(Integer waitSeconds)
    {
        return _confirmationSynchronizationFunction.apply(() -> this.dismiss("Yes, Permanently Delete", waitSeconds));
    }

    public void clickDismiss()
    {
        this.dismiss("Dismiss");
    }

    public DeleteConfirmationDialog<ConfirmPage> setUserComment(String comment)
    {

        WebDriverWrapper.waitFor(()-> elementCache().commentInput.getComponentElement().isDisplayed(),
                "The 'Comment' field is not visible.", 2_500);

        elementCache().commentInput.set(comment);
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
        return (ElementCache) super.elementCache();
    }

    protected class ElementCache extends ModalDialog.ElementCache
    {
        Input commentInput = Input.Input(Locator.tagWithClass("textarea", "form-control"), getDriver()).timeout(2000)
                .refindWhenNeeded(this);

    }
}
