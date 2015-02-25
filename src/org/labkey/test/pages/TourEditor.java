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
        _test.click(Locators.saveButton);
    }

    public void saveAndClose()
    {
        _test.click(Locators.saveAndCloseButton);
    }

    public void cancel()
    {
        _test.click(Locators.cancelButton);
    }

    public void clear()
    {
        _test.click(Locators.clearButton);
    }

    public void addStep()
    {
        _test.click(Locators.addStepButton);
    }

    public void importTour(String JSON)
    {
        _test.click(Locators.importTourButton);
        _test._extHelper.setCodeMirrorValue("export-script-textarea", JSON);
        _test.click(Locator.tagContainingText("span", "Import").index(2));
    }

    public String export()
    {
        _test.click(Locators.exportButton);
        _test.waitForElement(Ext4Helper.Locators.window("Export Tour"));
        return _test._extHelper.getCodeMirrorValue("export-script-textarea");
    }

    public void setTitle(String title)
    {
        _test.setFormElement(Locators.titleTextArea, title);
    }

    public void setMode(TourMode mode)
    {
        switch(mode)
        {
            case RUNALWAYS:
            _test.selectOptionByText(Locators.setModeCombo, "Run Always");
            break;

            case RUNONCE:
            _test.selectOptionByText(Locators.setModeCombo, "Run Once");
            break;

            case OFF:
            _test.selectOptionByText(Locators.setModeCombo, "Off");
        }
    }

    public void setDescription(String description)
    {
        _test.setFormElement(Locators.descriptionTextArea, description);
    }

    //index is 1 based
    public void setSelector(int index, String selector)
    {
        _test.setFormElement(Locators.getSelectorTextArea(index), selector);
    }

    //index is 1 based
    public void setStep(int index, String step)
    {
        _test.setFormElement(Locators.getStepTextArea(index), step);
    }

    public enum TourMode
    {
        OFF,
        RUNONCE,
        RUNALWAYS
    }

    public static class Locators
    {
        public static Locator saveButton = Locator.xpath("//span[contains(.,'Save')]");
        public static Locator saveAndCloseButton = Locator.xpath("//span[contains(.,'Save & Close')]");
        public static Locator cancelButton = Locator.xpath("//span[contains(.,'Cancel')]");
        public static Locator clearButton = Locator.xpath("//span[contains(.,'Clear')]");
        public static Locator addStepButton = Locator.xpath("//span[contains(.,'Add Step')]");
        public static Locator importTourButton = Locator.xpath("//span[contains(.,'Import')]");
        public static Locator exportButton = Locator.xpath("//span[contains(.,'Export')]");
        public static Locator.XPathLocator setModeCombo = Locator.tagWithId("select", "tour-mode");
        public static Locator descriptionTextArea = Locator.xpath("//textarea[@id='tour-description']");
        public static Locator titleTextArea = Locator.xpath("//input[@id='tour-title']");
        public static Locator getSelectorTextArea(int index) {return Locator.xpath("//input[@id='tour-selector"+index+"']");}
        public static Locator getStepTextArea(int index) {return Locator.xpath("//textarea[@id='tour-step"+index+"']/../div//textarea");}
    }
}
