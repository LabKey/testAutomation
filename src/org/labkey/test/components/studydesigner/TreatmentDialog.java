/*
 * Copyright (c) 2016-2017 LabKey Corporation
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
package org.labkey.test.components.studydesigner;

import org.labkey.test.Locator;
import org.labkey.test.components.ext4.Window;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.ArrayList;
import java.util.List;

public class TreatmentDialog extends Window
{

    public TreatmentDialog(WebDriver wd)
    {
        super("Treatment", wd);
    }

    public List<WebElement> sectionOptions(Sections section)
    {
        return findElements(Locator.xpath("//label[text()='" + section._text + "']/parent::div/following-sibling::div//td[contains(@class, 'x4-form-cb-wrap')]//label[not(contains(@class, 'x4-unselectable'))]"));
    }

    public List<WebElement> getSelectedValues()
    {
        List<WebElement> list = new ArrayList<>();

        list.addAll(getSelectedValues(Sections.Immunogen));
        list.addAll(getSelectedValues(Sections.Adjuvant));
        list.addAll(getSelectedValues(Sections.Challenge));

        return list;
    }

    public List<WebElement> getSelectedValues(Sections section)
    {
        List<WebElement> checkedOptions = new ArrayList<>();

        List<WebElement> list = sectionOptions(section);

        for(WebElement we : list)
        {
            if(we.findElement(By.xpath("ancestor::table[contains(@class, 'x4-form-type-checkbox')]")).getAttribute("class").toLowerCase().contains("x4-form-cb-checked"))
                checkedOptions.add(we);
        }

        return checkedOptions;
    }

    public TreatmentDialog selectOption(Sections section, String... labelValue)
    {
        List<WebElement> options = sectionOptions(section);

        for(WebElement we : options)
        {
            for(String label : labelValue)
            {
                if(we.getText().equals(label))
                    we.click();
            }
        }

        return this;
    }

    public TreatmentDialog selectOption(Sections section, int... index)
    {
        List<WebElement> options = sectionOptions(section);

        for(int indx : index)
        {
            options.get(indx).click();
        }

        return this;
    }

    public void clickCancel()
    {
        clickButton("Cancel", true);
    }

    public void clickOk()
    {
        clickButton("OK", true);
    }

    public enum Sections
    {
        Immunogen("Immunogen:"),
        Adjuvant("Adjuvant:"),
        Challenge("Challenge:");

        private final String _text;

        Sections(String text)
        {
            this._text = text;
        }
    }
}
