package org.labkey.test.components;

import org.labkey.test.Locator;
import org.labkey.test.components.ext4.Window;
import org.openqa.selenium.WebDriver;

public abstract class ChartWizardDialog<EC extends ChartWizardDialog.ElementCache> extends Window<EC>
{
    public ChartWizardDialog(String title, WebDriver driver)
    {
        super(Locator.tagWithClass("div", "chart-wizard-dialog")
                .withDescendant(Locator.tagWithClass("div", "title-panel")
                        .withText(title))
                .waitForElement(driver, 10000), driver);
    }

    @Override
    public void close()
    {
        clickCancel();
    }

    public void clickCancel()
    {
        clickButton("Cancel", 0);
        waitForClose();
    }

    class ElementCache extends Window.Elements
    {
        public ElementCache()
        {
            title = Locator.css("div.title-panel").findWhenNeeded(this);
            body = Locator.css("div.chart-wizard-panel").findWhenNeeded(this);
        }
    }
}
