/*
 * Copyright (c) 2015-2017 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.labkey.test.pages;

import org.labkey.test.Locator;
import org.labkey.test.util.Ext4Helper;
import org.openqa.selenium.WebDriver;

public class TourEditor extends LabKeyPage
{
    public TourEditor(WebDriver driver)
    {
        super(driver);
    }

    public void save()
    {
        click(Locators.saveButton);
        waitForElement(Locator.id("status").withText("Saved."), 1000, false);
    }

    public void saveAndClose()
    {
        clickAndWait(Locators.saveAndCloseButton);
    }

    public void cancel()
    {
        clickAndWait(Locators.cancelButton);
    }

    public void clear()
    {
        click(Locators.clearButton);
    }

    public void addStep()
    {
        click(Locators.addStepButton);
    }

    public void importTour(String JSON)
    {
        click(Locators.importTourButton);
        _extHelper.setCodeMirrorValue("export-script-textarea", JSON);
        _ext4Helper.clickWindowButton("Import Tour", "Import", 0, 0);
        waitForElementToDisappear(Ext4Helper.Locators.window("Import Tour"));
    }

    public String export()
    {
        click(Locators.exportButton);
        waitForElement(Ext4Helper.Locators.window("Export Tour"));
        return _extHelper.getCodeMirrorValue("export-script-textarea");
    }

    public void setTitle(String title)
    {
        setFormElement(Locators.titleTextArea, title);
    }

    public void setMode(TourMode mode)
    {
        switch(mode)
        {
            case RUNALWAYS:
            selectOptionByText(Locators.setModeCombo, "Run Always");
            break;

            case RUNONCE:
            selectOptionByText(Locators.setModeCombo, "Run Once");
            break;

            case OFF:
            selectOptionByText(Locators.setModeCombo, "Off");
        }
    }

    public void setDescription(String description)
    {
        setFormElement(Locators.descriptionTextArea, description);
    }

    //index is 1 based
    public void setSelector(int index, String selector)
    {
        setFormElement(Locators.getSelectorTextArea(index), selector);
    }

    //index is 1 based
    public void setStep(int index, String step)
    {
        _extHelper.setCodeMirrorValue("tour-step" + index, step);
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
    }
}
