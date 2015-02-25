package org.labkey.test.pages;

import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.util.Ext4Helper;

/**
 * Created by RyanS on 2/24/2015.
 */
public class TourEditor
{
    private BaseWebDriverTest _test;

    public TourEditor(BaseWebDriverTest test)
    {
        _test = test;
    }

    public void save()
    {
        _test.click(MyLocators.saveLocator);
    }

    public void saveAndClose()
    {
        _test.click(MyLocators.saveAndCloseLocator);
    }

    public void cancel()
    {
        _test.click(MyLocators.cancelLocator);
    }

    public void clear()
    {
        _test.click(MyLocators.clearLocator);
    }

    public void addStep()
    {
        _test.click(MyLocators.addStepLocator);
    }

    public void importTour(String JSON)
    {
        _test.click(MyLocators.importTourLocator);
        _test._extHelper.setCodeMirrorValue("export-script-textarea", JSON);
        _test.click(Locator.tagContainingText("span", "Import").index(2));
    }

    public String export()
    {
        _test.click(MyLocators.exportLocator);
        _test.waitForElement(Ext4Helper.Locators.window("Export Tour"));
        return _test._extHelper.getCodeMirrorValue("export-script-textarea");
    }

    public void setTitle(String title)
    {
        _test.setFormElement(MyLocators.titleLocator, title);
    }

    public void setMode(TourMode mode)
    {
        switch(mode)
        {
            case RUNALWAYS:
            _test.selectOptionByText(MyLocators.setModeSelector, "Run Always");
            break;

            case RUNONCE:
            _test.selectOptionByText(MyLocators.setModeSelector, "Run Once");
            break;

            case OFF:
            _test.selectOptionByText(MyLocators.setModeSelector, "Off");
        }
    }

    public void setDescription(String description)
    {
        _test.setFormElement(MyLocators.descriptionLocator, description);
    }

    //index is 1 based
    public void setSelector(int index, String selector)
    {
        _test.setFormElement(MyLocators.getSelectorLocator(index), selector);
    }

    //index is 1 based
    public void setStep(int index, String step)
    {
        _test.setFormElement(MyLocators.getStepLocator(index), step);
    }

    public enum TourMode
    {
        OFF,
        RUNONCE,
        RUNALWAYS
    }

    public static class MyLocators
    {
        public static Locator saveLocator = Locator.xpath("//span[contains(.,'Save')]");
        public static Locator saveAndCloseLocator = Locator.xpath("//span[contains(.,'Save & Close')]");
        public static Locator cancelLocator = Locator.xpath("//span[contains(.,'Cancel')]");
        public static Locator clearLocator = Locator.xpath("//span[contains(.,'Clear')]");
        public static Locator addStepLocator = Locator.xpath("//span[contains(.,'Add Step')]");
        public static Locator importTourLocator = Locator.xpath("//span[contains(.,'Import')]");
        public static Locator exportLocator = Locator.xpath("//span[contains(.,'Export')]");
        public static Locator.XPathLocator setModeSelector = Locator.tagWithId("select", "tour-mode");
        public static Locator descriptionLocator = Locator.xpath("//textarea[@id='tour-description']");
        public static Locator titleLocator = Locator.xpath("//input[@id='tour-title']");
        public static Locator getSelectorLocator(int index) {return Locator.xpath("//input[@id='tour-selector"+index+"']");}
        public static Locator getStepLocator(int index) {return Locator.xpath("//textarea[@id='tour-step"+index+"']/../div//textarea");}
    }
}
