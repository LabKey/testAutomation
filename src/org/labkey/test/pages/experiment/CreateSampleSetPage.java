package org.labkey.test.pages.experiment;

import org.labkey.test.WebDriverWrapper;
import org.labkey.test.WebTestHelper;
import org.labkey.test.components.domain.DomainFormPanel;
import org.labkey.test.components.labkey.ui.samples.SampleTypeDesigner;
import org.labkey.test.pages.LabKeyPage;
import org.labkey.test.params.FieldDefinition;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.List;


public class CreateSampleSetPage extends LabKeyPage<CreateSampleSetPage.ElementCache>
{
    public CreateSampleSetPage(WebDriver driver)
    {
        super(driver);
    }

    public static CreateSampleSetPage beginAt(WebDriverWrapper driver)
    {
        return beginAt(driver, driver.getCurrentContainerPath());
    }

    public static CreateSampleSetPage beginAt(WebDriverWrapper driver, String containerPath)
    {
        driver.beginAt(WebTestHelper.buildURL("experiment", containerPath, "createSampleSet"));
        return new CreateSampleSetPage(driver.getDriver());
    }

    public CreateSampleSetPage setName(String name)
    {
        elementCache()._designer.setName(name);
        return this;
    }

    public String getName()
    {
        return elementCache()._designer.getName();
    }

    public CreateSampleSetPage setDescription(String desc)
    {
        elementCache()._designer.setDescription(desc);
        return this;
    }

    public String getDescrption()
    {
        return elementCache()._designer.getDescription();
    }

    public CreateSampleSetPage setNameExpression(String nameExp)
    {
        elementCache()._designer.setNameExpression(nameExp);
        return this;
    }

    public String getNameExpression()
    {
        return elementCache()._designer.getNameExpression();
    }

    public CreateSampleSetPage addParentAlias(String importHeader, String materialInputName)
    {
        elementCache()._designer.addParentAlias(importHeader, materialInputName);

        return this;
    }

    public CreateSampleSetPage addParentAlias(String importHeader)
    {
        return addParentAlias(importHeader, SampleTypeDesigner.CURRENT_SAMPLE_TYPE);
    }

    public CreateSampleSetPage setParentAlias(int index, String alias, String optionDisplayText)
    {
        elementCache()._designer.setParentAlias(index, alias, optionDisplayText);
        return this;
    }

    public String getParentAlias(int index)
    {
        return elementCache()._designer.getParentAlias(index);
    }

    public String getParentAliasSelectText(int index)
    {
        return elementCache()._designer.getParentAliasSelectText(index);
    }

    public CreateSampleSetPage removeParentAlias(int index)
    {
        elementCache()._designer.removeParentAlias(index);
        return this;
    }

    public CreateSampleSetPage removeParentAlias(String parentAlias)
    {
        elementCache()._designer.removeParentAlias(parentAlias);
        return this;
    }

    public CreateSampleSetPage addFields(List<FieldDefinition> fields)
    {
        for (FieldDefinition fieldDefinition : fields)
        {
            getDomainEditor().addField(fieldDefinition);
        }
        return this;
    }

    public DomainFormPanel getDomainEditor()
    {
        return elementCache()._designer.getDomainEditor();
    }

    public CreateSampleSetPage expandPropertiesPanel()
    {
        elementCache()._designer.expandPropertiesPanel();
        return this;
    }

    public void clickSave()
    {
        doAndWaitForPageToLoad(() -> elementCache()._designer.clickSave());
    }

    public List<WebElement> clickSaveExpectingError()
    {
        return elementCache()._designer.clickSaveExpectingError();
    }

    public void clickCancel()
    {
        elementCache()._designer.clickCancel();
    }

    @Override
    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    protected class ElementCache extends LabKeyPage<?>.ElementCache
    {
        SampleTypeDesigner _designer = new SampleTypeDesigner(getDriver());
    }
}
