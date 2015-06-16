/*
 * Copyright (c) 2015 LabKey Corporation
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

import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.util.Ext4Helper;

/**
 * Created by susanh on 6/15/15.
 */
public class WorkflowTask
{
    protected BaseWebDriverTest _test;

    public WorkflowTask(BaseWebDriverTest test)
    {
        _test = test;
    }

    public void reassignTask(String buttonText, String userName)
    {
        _test.click(Locators.getReassignButton(buttonText));
        _test.waitForElement(Ext4Helper.Locators.window("Reassign Task"));
        _test._ext4Helper.selectComboBoxItem("User:", Ext4Helper.TextMatchTechnique.STARTS_WITH, userName);
        _test.clickAndWait(Ext4Helper.Locators.windowButton("Reassign Task", "Assign"));
    }

    public void submitForm(String submitText)
    {
        _test.clickButton(submitText);
    }

    public static class Locators
    {
        public static Locator getReassignButton(String buttonText) { return Locator.lkButtonContainingText(buttonText); }
    }

}
