/*
 * Copyright (c) 2010-2011 LabKey Corporation
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

import org.labkey.test.BaseSeleniumWebTest;
import org.labkey.test.Locator;

/**
 * Created by IntelliJ IDEA.
 * User: kevink
 * Date: Sep 13, 2010
 * Time: 1:41:34 PM
 */
public class ReagentTest extends BaseSeleniumWebTest
{
    protected static final String PROJECT_NAME = "ReagentProject";
    protected static final String FOLDER_NAME = "ReagentFolder";

    @Override
    protected boolean isDatabaseSupported(DatabaseInfo info)
    {
        return info.productName.equals("PostgreSQL");
    }

    @Override
    public String getAssociatedModuleDirectory()
    {
        return "server/customModules/reagent";
    }

    @Override
    protected String getProjectName()
    {
        return PROJECT_NAME;
    }

    @Override
    protected void doCleanup() throws Exception
    {
        try { deleteProject(PROJECT_NAME); } catch (Throwable t) { }
    }

    @Override
    protected void doTestSteps() throws Exception
    {
        createProject();
        _testInsert();
        _testUpdate();
        _testBulkUpdate();
    }

    public void createProject()
    {
        log("** Create Project");
        createProject(PROJECT_NAME);
        createSubfolder(PROJECT_NAME, FOLDER_NAME, new String[] { "Query" });
        enableModule("reagent", false);

        addWebPart("Query");
        setFormElement("schemaName", "reagent");
        submit();

        beginAt("reagent/" + PROJECT_NAME + "/" + FOLDER_NAME + "/initialize.view");
        clickButton("Initialize", 0);
        waitForText("Done.", 2*WAIT_FOR_JAVASCRIPT);
    }

    public void _testInsert()
    {
        log("** Inserting new Reagent");
        beginAt("query/" + PROJECT_NAME + "/" + FOLDER_NAME + "/executeQuery.view?schemaName=reagent&query.queryName=Reagents");
        clickButton("Insert New");

        waitForElement(Locator.extButton("Cancel"), WAIT_FOR_JAVASCRIPT);

        log("** Selecting AntigenId from ComboBox list");
        // click on ComboBox trigger image
        click(Locator.xpath("//input[@name='AntigenId']/../img"));
        waitForElement(Locator.xpath("//div[contains(@class, 'x-combo-list-item')]//b[text()='AVDLSHFLK']"), WAIT_FOR_JAVASCRIPT);
        click(Locator.xpath("//div[contains(@class, 'x-combo-list-item')]//b[text()='AVDLSHFLK']"));
        assertFormElementEquals(Locator.xpath("//input[@name='AntigenId']/../input[contains(@class, 'x-form-field')]"), "AVDLSHFLK");

        log("** Filtering LabelId ComboBox by 'Alexa'");
        click(Locator.xpath("//input[@name='LabelId']/../input[contains(@class, 'x-form-field')]"));
        selenium.typeKeys("//input[@name='LabelId']/../input[contains(@class, 'x-form-field')]", "Alexa");
        Number alexaLabels = selenium.getXpathCount("//div[contains(@class, 'x-combo-list')]//b[text()='Alexa 405']/../../..//b");
        assertEquals("Expected to find 5 Alexa labels", 5, alexaLabels.intValue());

        pressDownArrow("//input[@name='LabelId']/../input[contains(@class, 'x-form-field')]");
        pressDownArrow("//input[@name='LabelId']/../input[contains(@class, 'x-form-field')]");
        pressEnter("//input[@name='LabelId']/../input[contains(@class, 'x-form-field')]");
        assertFormElementEquals(Locator.xpath("//input[@name='LabelId']/../input[contains(@class, 'x-form-field')]"), "Alexa 680");
    }

    public void _testUpdate()
    {

    }

    public void _testBulkUpdate()
    {
        
    }
}
