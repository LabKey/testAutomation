package org.labkey.test.pages.experiment;

import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.WebTestHelper;
import org.labkey.test.components.domain.DomainFormPanel;
import org.labkey.test.components.labkey.ui.samples.SampleTypeDesigner;
import org.labkey.test.pages.LabKeyPage;
import org.labkey.test.params.FieldDefinition;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.List;


public class CreateSampleSetPage extends LabKeyPage<CreateSampleSetPage.ElementCache> implements SampleTypeDesigner.SavesSampleType<CreateSampleSetPage, LabKeyPage<?>>
{
    public final static String CURRENT_SAMPLE_TYPE_OPTION_TEXT = "(Current Sample Type)";
    public final static String CURRENT_SAMPLE_SET_OPTION_TEXT = "(Current Sample Set)";
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

    @Override
    public Locator getBodyLocator()
    {
        return Locator.id("app");
    }

    @Override
    public LabKeyPage<?> getSaveDestination()
    {
        return null;
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

    public CreateSampleSetPage addParentColumnAlias(int index, String importHeader, String materialInputName)
    {
        elementCache()._designer.addParentAlias();
        elementCache()._designer.setParentAlias(index, importHeader, materialInputName);

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

    public CreateSampleSetPage addFields(List<FieldDefinition> fields)
    {
        fieldsPanel().expand();
        for (FieldDefinition fieldDefinition : fields)
        {
            fieldsPanel().addField(fieldDefinition);
        }
        return this;
    }

    public DomainFormPanel fieldsPanel()
    {
        return elementCache()._designer.getDomainEditor();
    }

    public LabKeyPage clickSave()
    {
        elementCache()._designer.clickSave(false);
        return new LabKeyPage(getDriver());
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
