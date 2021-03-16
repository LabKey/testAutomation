package org.labkey.test.components.ui.workflow;

import org.labkey.test.components.bootstrap.ModalDialog;
import org.labkey.test.components.react.FilteringReactSelect;
import org.labkey.test.pages.samplemanagement.workflow.WorkflowJobDesignPage;
import org.openqa.selenium.WebDriver;

import java.util.List;

public class ChooseJobTemplateDialog extends ModalDialog
{

    public ChooseJobTemplateDialog(WebDriver driver)
    {
        super(new ModalDialogFinder(driver).withTitle("Choose a Template").waitFor().getComponentElement(), driver);
    }

    /**
     * Pick the template with the given name and apply it. If there are two templates with the same name, the
     * first one will be chosen.
     *
     * @param templateName The name of the template to apply.
     * @return A new instance of a {link org.labkey.test.pages.samplemanagement.workflow.WorkflowJobDesignPage};
     */
    public WorkflowJobDesignPage applyTemplate(String templateName)
    {
        elementCache().templateList.typeAheadSelect(templateName);
        dismiss("Choose Template");
        return new WorkflowJobDesignPage(getWrapper());
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
