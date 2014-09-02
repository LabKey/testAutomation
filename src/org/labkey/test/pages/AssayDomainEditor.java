package org.labkey.test.pages;

import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;

import java.io.File;

import static org.junit.Assert.assertTrue;

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
        _test.fireEvent(Locator.xpath("//input[@id='AssayDesignerName']"), BaseWebDriverTest.SeleniumEvent.change);
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

    public void addTransformScript(File transformScript)
    {
        int index = _test.getElementCount(Locator.xpath("//input[starts-with(@id, 'AssayDesignerTransformScript')]"));
        _test.click(Locator.lkButton("Add Script"));

        setTransformScript(transformScript, index);
    }

    public void setTransformScript(File transformScript)
    {
        setTransformScript(transformScript, 0);
    }

    public void setTransformScript(File transformScript, int index)
    {
        assertTrue("Unable to locate the transform script: " + transformScript, transformScript.exists());

        _test.setFormElement(Locator.xpath("//input[@id='AssayDesignerTransformScript" + index + "']"), transformScript.getAbsolutePath());
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

    public void setBackgroundImport(boolean set)
    {
        if (set)
            _test.checkCheckbox(Locator.checkboxByName("backgroundUpload"));
        else
            _test.uncheckCheckbox(Locator.checkboxByName("backgroundUpload"));
    }

    @Override
    public void saveAndClose()
    {
        super.saveAndClose();
        _test.waitForElement(Locator.css("table.labkey-data-region")); // 'Runs' or 'AssayList'
    }

    public enum MetadataInputFormat
    {
        MANUAL,
        FILE_BASED
    }
}
