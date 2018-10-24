package org.labkey.test.pages.experiment;

import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.WebTestHelper;
import org.labkey.test.components.html.Input;
import org.labkey.test.pages.LabKeyPage;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

public class InsertDataClassPage extends LabKeyPage<InsertDataClassPage.ElementCache>
{
    public InsertDataClassPage(WebDriver driver)
    {
        super(driver);
    }

    public static InsertDataClassPage beginAt(WebDriverWrapper driver)
    {
        return beginAt(driver, driver.getCurrentContainerPath());
    }

    public static InsertDataClassPage beginAt(WebDriverWrapper driver, String containerPath)
    {
        driver.beginAt(WebTestHelper.buildURL("controller", containerPath, "action"));
        return new InsertDataClassPage(driver.getDriver());
    }

    public InsertDataClassPage setName(String name)
    {
        new Input(elementCache().nameEditElement, getDriver()).set(name);
        return new InsertDataClassPage(getDriver());
    }

    public InsertDataClassPage setDescription(String desc)
    {
        new Input(elementCache().descriptionEditElement, getDriver()).set(desc);
        return new InsertDataClassPage(getDriver());
    }

    public InsertDataClassPage setNameExpression(String nameExp)
    {
        new Input(elementCache().nameExpressionElement, getDriver()).set(nameExp);
        return new InsertDataClassPage(getDriver());
    }

    public InsertDataClassPage selectMaterialSourceId(String matSrcId)
    {
        setFormElement(elementCache().materialSourceIdElement, matSrcId);
        return new InsertDataClassPage(getDriver());
    }

    public void clickCreate()
    {
        clickAndWait(elementCache().createBtn);
    }
    public void clickCancel()
    {
        clickAndWait(elementCache().cancelBtn);
    }

    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    protected class ElementCache extends LabKeyPage.ElementCache
    {
        WebElement nameEditElement = Locator.tagWithName("input","name")
                .findWhenNeeded(getDriver()).withTimeout(WAIT_FOR_JAVASCRIPT);;
        WebElement descriptionEditElement = Locator.tagWithName("input","description")
                .findWhenNeeded(getDriver()).withTimeout(WAIT_FOR_JAVASCRIPT);;
        WebElement nameExpressionElement = Locator.tagWithName("input","nameExpression")
                .findWhenNeeded(getDriver()).withTimeout(WAIT_FOR_JAVASCRIPT);;
        WebElement materialSourceIdElement = Locator.tagWithName("select","materialSourceId")
                .findWhenNeeded(getDriver()).withTimeout(WAIT_FOR_JAVASCRIPT);;

        WebElement createBtn = Locator.lkButton("Create").findWhenNeeded(getDriver()).withTimeout(WAIT_FOR_JAVASCRIPT);;
        WebElement cancelBtn = Locator.lkButton("Cancel").findWhenNeeded(getDriver()).withTimeout(WAIT_FOR_JAVASCRIPT);
    }
}
