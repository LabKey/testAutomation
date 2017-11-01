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
package org.labkey.test.pages.issues;

import org.labkey.test.Locator;
import org.labkey.test.components.IssueListDefDataRegion;
import org.labkey.test.components.ext4.Window;
import org.labkey.test.components.html.Input;
import org.labkey.test.components.html.OptionSelect;
import org.labkey.test.pages.LabKeyPage;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import static org.labkey.test.components.html.Input.Input;

public class InsertIssueDefPage extends LabKeyPage
{
    private final String _dataRegionName;
    private final Input _labelInput;

    private InsertIssueDefPage(WebDriver driver, String originDataRegionName)
    {
        super(driver);
        _dataRegionName = originDataRegionName;
        _labelInput = Input(Locator.input("quf_Label"), getDriver()).waitFor(getDriver());
    }

    private InsertIssueDefPage(WebDriver driver)
    {
        this(driver, IssueListDefDataRegion.NAME_IN_QUERY);
    }

    public InsertIssueDefPage(WebDriver driver, IssueListDefDataRegion originDataRegion)
    {
        this(driver, originDataRegion.getDataRegionName());
    }

    public InsertIssueDefPage setLabel(String label)
    {
        _labelInput.set(label);
        return this;
    }

    public InsertIssueDefPage selectKind(String kind)
    {
        WebElement input = Locator.tagWithName("select", "quf_Kind").findElement(getDriver());
        new OptionSelect(input).set(kind);
        return this;
    }

    public CreateListDefConfirmation clickSubmit()
    {
        click(Locator.lkButton("Submit"));
        return new CreateListDefConfirmation(getDriver());
    }

    public CreateListDefError clickSubmitError()
    {
        click(Locator.lkButton("Submit"));
        return new CreateListDefError(getDriver());
    }

    public IssueListDefDataRegion clickCancel()
    {
        clickAndWait(Locator.lkButton("Cancel"));
        return new IssueListDefDataRegion(_dataRegionName, getDriver());
    }

    public class CreateListDefConfirmation extends Window
    {
        private CreateListDefConfirmation(WebDriver driver)
        {
            super("Create Issue List Definition?", driver);
        }

        public AdminPage clickYes()
        {
            clickButton("Yes");
            return new AdminPage(getDriver());
        }

        public InsertIssueDefPage clickYesError()
        {
            clickButton("Yes");
            return new InsertIssueDefPage(getDriver(), _dataRegionName);
        }

        public InsertIssueDefPage clickNo()
        {
            clickButton("No", true);
            return InsertIssueDefPage.this;
        }
    }

    public class CreateListDefError extends Window
    {
        private CreateListDefError(WebDriver driver)
        {
            super("Error", driver);
        }

        public InsertIssueDefPage clickClose()
        {
            super.close();
            return InsertIssueDefPage.this;
        }
    }
}
