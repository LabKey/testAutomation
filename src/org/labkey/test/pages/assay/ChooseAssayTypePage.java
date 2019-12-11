package org.labkey.test.pages.assay;

import org.labkey.test.Locator;
import org.labkey.test.Locators;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.WebTestHelper;
import org.labkey.test.components.html.RadioButton;
import org.labkey.test.components.html.SelectWrapper;
import org.labkey.test.pages.LabKeyPage;
import org.labkey.test.pages.ReactAssayDesignerPage;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.Select;

public class ChooseAssayTypePage extends LabKeyPage<ChooseAssayTypePage.ElementCache>
{
    public ChooseAssayTypePage(WebDriver driver)
    {
        super(driver);
    }

    public static ChooseAssayTypePage beginAt(WebDriverWrapper webDriverWrapper)
    {
        return beginAt(webDriverWrapper, webDriverWrapper.getCurrentContainerPath());
    }

    public static ChooseAssayTypePage beginAt(WebDriverWrapper webDriverWrapper, String containerPath)
    {
        webDriverWrapper.beginAt(WebTestHelper.buildURL("assay", containerPath, "chooseAssayType"));
        return new ChooseAssayTypePage(webDriverWrapper.getDriver());
    }

    public ChooseAssayTypePage selectType(String providerName)
    {
        elementCache().getProviderRadioButton(providerName).check();
        return this;
    }

    public ChooseAssayTypePage selectAssayContainer(String optionText)
    {
        elementCache().locationSelect.selectByVisibleText(optionText);
        return this;
    }

    public void clickCancel()
    {
        clickAndWait(elementCache().cancelButton);
    }

    public ReactAssayDesignerPage clickNext()
    {
        clickAndWait(elementCache().nextButton);

        return new ReactAssayDesignerPage(getDriver());
    }

    public String clickNextExpectingError()
    {
        clickAndWait(elementCache().nextButton);

        clearCache();
        return Locators.labkeyError.waitForElement(getDriver(), 10000).getText();
    }

    @Override
    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    protected class ElementCache extends LabKeyPage.ElementCache
    {
        WebElement uploadXarLink = Locator.linkWithText("Upload").findWhenNeeded(this);

        RadioButton getProviderRadioButton(String providerName)
        {
            return RadioButton.finder().withNameAndValue("providerName", providerName).find(this);
        }

        Select locationSelect = SelectWrapper.Select(Locator.id("assayContainer")).findWhenNeeded(this);

        WebElement nextButton = Locator.lkButton("Next").findWhenNeeded(this);
        WebElement cancelButton = Locator.lkButton("Cancel").findWhenNeeded(this);
    }
}
