/*
 * Copyright (c) 2009-2013 LabKey Corporation
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

package org.labkey.test.tests;

import org.labkey.test.Locator;

public class StudyDatasetsTest extends StudyBaseTest
{
    @Override
    protected void doCreateSteps()
    {
        importStudy();
        // wait for study (but not specimens) to finish loading
        waitForPipelineJobsToComplete(1, "study import", false);
        runTest();
    }

    public void goToManageDatasets()
    {
        goToManageStudy();
        waitForText("Manage Datasets");
        click(Locator.xpath("//a[text()='Manage Datasets']"));
    }

    protected void runTest()
    {
        goToManageDatasets();
        createDataset("A");
        renameDataset("A", "Original A");
        createDataset("A");
        deleteFields("A");

        checkFieldsPresent("Original A", "YTest", "ZTest");

        System.out.println("WAAAAH");


    }

    protected void createDataset(String name)
    {
        waitForText("Create New Dataset");
        click(Locator.xpath("//a[text()='Create New Dataset']"));
        waitForElement(Locator.xpath("//input[@name='typeName']"));
        setFormElement(Locator.xpath("//input[@name='typeName']"), name);
        clickButton("Next");

        waitForElement(Locator.xpath("//input[@id='name0-input']"));
        assertTextNotPresent("XTest");
        setFormElement(Locator.xpath("//input[@id='name0-input']"), "XTest");
        clickButtonContainingText("Add Field", 0);
        waitForElement(Locator.xpath("//input[@id='name1-input']"));
        assertTextNotPresent("YTest");
        setFormElement(Locator.xpath("//input[@id='name1-input']"), "YTest");
        clickButtonContainingText("Add Field", 0);
        waitForElement(Locator.xpath("//input[@id='name2-input']"));
        assertTextNotPresent("ZTest");
        setFormElement(Locator.xpath("//input[@id='name2-input']"), "ZTest");
        clickButton("Save");

        goToManageDatasets();
    }

    protected void renameDataset(String orgName, String newName)
    {
        waitForElement(Locator.xpath("//a[text()='" + orgName + "']"));
        click(Locator.xpath("//a[text()='" + orgName + "']"));
        waitForText("Edit Definition");
        clickButton("Edit Definition");

        waitForElement(Locator.xpath("//input[@name='dsName']"));
        setFormElement(Locator.xpath("//input[@name='dsName']"), newName);
        setFormElement(Locator.xpath("//input[@name='dsLabel']"), newName);

        assertTextPresent("XTest");
        assertTextPresent("YTest");
        assertTextPresent("ZTest");
        clickButton("Save");

        goToManageDatasets();
    }

    protected void deleteFields(String name)
    {
        waitForElement(Locator.xpath("//a[text()='" + name + "']"));
        click(Locator.xpath("//a[text()='" + name + "']"));
        waitForText("Edit Definition");
        clickButton("Edit Definition");

        waitForElement(Locator.xpath("//div[@id='partdelete_2']"));
        mouseClick(Locator.id("partdelete_2").toString());
        clickButtonContainingText("OK", 0);
        waitForElement(Locator.xpath("//div[@id='partdelete_1']"));
        mouseClick(Locator.id("partdelete_1").toString());

        assertTextPresent("XTest");
        assertElementNotPresent(Locator.xpath("//input[@id='name1-input']"));
        assertElementNotPresent(Locator.xpath("//input[@id='name2-input']"));
        clickButton("Save");

        goToManageDatasets();
    }

    protected void checkFieldsPresent(String name, String... items)
    {
        waitForElement(Locator.xpath("//a[text()='" + name + "']"));
        click(Locator.xpath("//a[text()='" + name + "']"));
        waitForText("Edit Definition");
        clickButton("Edit Definition");

        for(String item : items)
        {
            waitForText(item);
        }

        goToManageDatasets();
    }

    @Override
    protected void doVerifySteps()
    {


    }
}
