package org.labkey.test.components.ui.workflow;

import org.labkey.test.WebDriverWrapper;
import org.labkey.test.components.bootstrap.ModalDialog;
import org.labkey.test.components.react.FilteringReactSelect;
import org.labkey.test.pages.LabKeyPage;

import java.util.List;
import java.util.function.Supplier;

public class ChooseJobTemplateDialog<SourcePage extends WebDriverWrapper, ConfirmPage extends LabKeyPage> extends ModalDialog
{
    private final SourcePage _sourcePage;
    private final Supplier<ConfirmPage> _confirmPageSupplier;

    public ChooseJobTemplateDialog(SourcePage sourcePage)
    {
        this(sourcePage, () -> null);
    }

    public ChooseJobTemplateDialog(SourcePage sourcePage, Supplier<ConfirmPage> confirmPageSupplier)
    {
        this("Choose a Template", sourcePage, confirmPageSupplier);
    }

    protected ChooseJobTemplateDialog(String partialTitle, SourcePage sourcePage, Supplier<ConfirmPage> confirmPageSupplier)
    {
        this(new ModalDialog.ModalDialogFinder(sourcePage.getDriver()).withTitle(partialTitle), sourcePage, confirmPageSupplier);
    }

    protected ChooseJobTemplateDialog(ModalDialogFinder finder, SourcePage sourcePage, Supplier<ConfirmPage> confirmPageSupplier)
    {
        super(finder);
        _sourcePage = sourcePage;
        _confirmPageSupplier = confirmPageSupplier;
    }

    /**
     * Pick the template with the given name and apply it. If there are two templates with the same name, the
     * first one will be chosen.
     *
     * @param templateName The name of the template to apply.
     * @return A new instance of a {link org.labkey.test.pages.samplemanagement.workflow.WorkflowJobDesignPage};
     */
    public ConfirmPage applyTemplate(String templateName)
    {
        elementCache().templateList.typeAheadSelect(templateName);
        dismiss("Choose Template");
        return _confirmPageSupplier.get();
    }

    /**
     * Select a template from the list of available templates. Do not apply it. Most likely used when testing the cancel option of the dialog.
     *
     * @param templateName Name of the template to select.
     * @return A reference to this dialog.
     */
    public ChooseJobTemplateDialog selectTemplate(String templateName)
    {
        elementCache().templateList.typeAheadSelect(templateName);
        return this;
    }

    /**
     * Get a list of templates from the drop down selection.
     *
     * @return List of text from the selection.
     */
    List<String> getTemplateList()
    {
      return elementCache().templateList.getOptions();
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
        FilteringReactSelect templateList = FilteringReactSelect.finder(getDriver()).findWhenNeeded(this);
    }

}
