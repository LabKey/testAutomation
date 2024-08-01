package org.labkey.test.pages.study;

import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.WebTestHelper;
import org.labkey.test.components.html.Checkbox;
import org.labkey.test.components.html.Input;
import org.labkey.test.components.html.SelectWrapper;
import org.labkey.test.pages.LabKeyPage;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.Select;

public class CreateNewVisitPage extends LabKeyPage<CreateNewVisitPage.ElementCache>
{
    public CreateNewVisitPage(WebDriver driver)
    {
        super(driver);
    }

    public static CreateNewVisitPage beginAt(WebDriverWrapper webDriverWrapper)
    {
        return beginAt(webDriverWrapper, webDriverWrapper.getCurrentContainerPath());
    }

    public static CreateNewVisitPage beginAt(WebDriverWrapper webDriverWrapper, String containerPath)
    {
        webDriverWrapper.beginAt(WebTestHelper.buildURL("study", containerPath, "createVisit"));
        return new CreateNewVisitPage(webDriverWrapper.getDriver());
    }

    public CreateNewVisitPage setLabel(String label)
    {
        elementCache().label.set(label);
        return this;
    }

    public CreateNewVisitPage setMinSequence(String range)
    {
        elementCache().sequenceNumMin.set(range);
        return this;
    }

    public CreateNewVisitPage setMaxSequence(String range)
    {
        elementCache().sequenceNumMax.set(range);
        return this;
    }

    public CreateNewVisitPage setDescription(String value)
    {
        setFormElement(elementCache().description, value);
        return this;
    }

    public CreateNewVisitPage setType(String option)
    {
        elementCache().type.selectByVisibleText(option);
        return this;
    }

    public CreateNewVisitPage setVisitHandling(String value)
    {
        elementCache().sequenceNumHandling.selectByVisibleText(value);
        return this;
    }

    public CreateNewVisitPage setShowByDefault(boolean value)
    {
        elementCache().showByDefault.set(value);
        return this;
    }

    public ManageVisitPage clickSave()
    {
        clickAndWait(elementCache().save);
        return new ManageVisitPage(getDriver());
    }

    @Override
    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    protected class ElementCache extends LabKeyPage<?>.ElementCache
    {
        final Input label = new Input(Locator.name("label").findWhenNeeded(this), getDriver());
        final Input sequenceNumMin = new Input(Locator.name("sequenceNumMin").findWhenNeeded(this), getDriver());
        final Input sequenceNumMax = new Input(Locator.name("sequenceNumMax").findWhenNeeded(this), getDriver());
        final WebElement description = Locator.textarea("description").findWhenNeeded(this);
        final Select type = SelectWrapper.Select(Locator.name("typeCode")).findWhenNeeded(this);
        final Select sequenceNumHandling = SelectWrapper.Select(Locator.name("sequenceNumHandling")).findWhenNeeded(this);
        final Checkbox showByDefault = new Checkbox(Locator.name("showByDefault").findWhenNeeded(this));
        final WebElement save = Locator.linkWithText("Save").findWhenNeeded(this);
        final WebElement cancel = Locator.linkWithText("Cancel").findWhenNeeded(this);
    }
}
