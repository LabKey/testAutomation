package org.labkey.test.pages;

import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;

public class AssayDomainEditor extends DomainEditor
{
    public AssayDomainEditor(BaseWebDriverTest test)
    {
        super(test);
    }

    @Override
    public void waitForReady()
    {
        super.waitForReady();
        _test.waitForElement(Locator.id("AssayDesignerDescription"), BaseWebDriverTest.WAIT_FOR_JAVASCRIPT);
    }

    public void setName(String name)
    {
        _test.setFormElement(Locator.id("AssayDesignerName"), name);
    }

    public void setDescription(String description)
    {
        _test.setFormElement(Locator.id("AssayDesignerDescription"), description);
    }

    public void setAutoCopyData(boolean set)
    {
        if (set)
            _test.checkCheckbox(Locator.checkboxByName("autoCopy"));
        else
            _test.uncheckCheckbox(Locator.checkboxByName("autoCopy"));
    }

    public void setAutoCopyTarget(String containerPath)
    {
        _test.selectOptionByText(Locator.id("autoCopyTarget"), containerPath);
    }

    public void setPlateTemplate(String template)
    {
        _test.selectOptionByText(Locator.id("plateTemplate"), template);
    }

    public void setMetaDataInputFormat(MetadataInputFormat format)
    {
        _test.selectOptionByValue(Locator.id("metadataInputFormat"), format.name());
    }

    public void setSaveScriptData(boolean set)
    {
        if (set)
            _test.checkCheckbox(Locator.checkboxByName("debugScript"));
        else
            _test.uncheckCheckbox(Locator.checkboxByName("debugScript"));
    }

    public void setEditableRuns(boolean set)
    {
        if (set)
            _test.checkCheckbox(Locator.checkboxByName("editableRunProperties"));
        else
            _test.uncheckCheckbox(Locator.checkboxByName("editableRunProperties"));
    }

    public enum MetadataInputFormat
    {
        MANUAL,
        FILE_BASED
    }
}
