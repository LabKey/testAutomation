/*
 * Copyright (c) 2009 LabKey Corporation
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
package org.labkey.test.bvt;

import org.labkey.test.BaseSeleniumWebTest;
import org.labkey.test.util.ListHelper;

/*
* User: Dave
* Date: Mar 25, 2009
* Time: 10:50:18 AM
*
* Tests the simple module and file-based resources introduced in version 9.1
*/
public class SimpleModuleTest extends BaseSeleniumWebTest
{
    public static final String PROJECT_NAME = "Simple Module Verfiy Project";
    public static final String MODULE_NAME = "simpletest";
    public static final String LIST_NAME = "People";
    public static final String LIST_DATA = "Name\tAge\tCrazy\n" +
            "Dave\t39\tTrue\n" +
            "Adam\t65\tTrue\n" +
            "Britt\t30\tFalse\n" +
            "Josh\t30\tTrue";

    protected void doTestSteps() throws Exception
    {
        assertModuleDeployed(MODULE_NAME);
        createProject(PROJECT_NAME);
        enableModule(PROJECT_NAME, MODULE_NAME);
        enableModule(PROJECT_NAME, "Query");

        clickLinkWithText(PROJECT_NAME);
        doTestViews();
        doTestWebParts();
        createList();
        doTestQueries();
        doTestQueryViews();
        doTestReports();
    }

    private void doTestViews()
    {
        log("Testing views in modules...");
        //begin.view should display when clicking on the module's tab
        clickTab(MODULE_NAME);
        assertTextPresent("This is the begin view from the test module");

        //navigate to other view
        clickLinkWithText("other view");
        assertTextPresent("This is another view in the simple test module");
    }

    private void doTestWebParts()
    {
        log("Testing web parts in modules...");
        //go to project portal
        clickLinkWithText(PROJECT_NAME);

        //add Simple Module Web Part
        addWebPart("Simple Module Web Part");
        assertTextPresent("This is a web part view in the simple test module");
    }

    private void createList()
    {
        //create a list for our query
        clickLinkWithText(PROJECT_NAME);
        addWebPart("Lists");

        log("Creating list for query/view/report test...");
        ListHelper.createList(this, PROJECT_NAME, LIST_NAME,
                ListHelper.ListColumnType.AutoInteger, "Key",
                new ListHelper.ListColumn("Name", "Name", ListHelper.ListColumnType.String, "Name"),
                new ListHelper.ListColumn("Age", "Age", ListHelper.ListColumnType.Integer, "Age"),
                new ListHelper.ListColumn("Crazy", "Crazy", ListHelper.ListColumnType.Boolean, "Crazy?"));

        log("Importing some data...");
        clickLinkWithText("import data");
        setFormElement("ff_data", LIST_DATA);
        submit();
    }

    private void doTestQueries()
    {
        log("Testing queries in modules...");

        //go to query module portal
        clickLinkWithText(PROJECT_NAME);
        clickTab("Query");
        clickLinkWithText("lists");
        clickLinkWithText("TestQuery");

        assertTextPresent("Adam");
        assertTextPresent("Dave");
        assertTextPresent("Josh");
        assertTextNotPresent("Britt");
    }

    private void doTestQueryViews()
    {
        log("Testing module-based custom query views...");
        clickLinkWithText(PROJECT_NAME);
        clickLinkWithText(LIST_NAME);

        clickMenuButton("Views", "Views:Crazy People");
        assertTextPresent("Adam");
        assertTextPresent("Dave");
        assertTextPresent("Josh");
        assertTextNotPresent("Britt");

        //custom view has a sort by age descending
        assertTextBefore("Adam", "Josh");
    }

    private void doTestReports()
    {
        //check that R script engine is insalled, otherwise skip
        clickLinkWithText("Admin Console");
        clickLinkWithText("views and scripting");
        if(!isREngineConfigured())
        {
            log("R scripting engine is not configured--skipping R report test.");
            return;
        }

        log("Testing module-based reports...");
        clickLinkWithText(PROJECT_NAME);
        clickLinkWithText(LIST_NAME);
        clickMenuButton("Views", "Views:Super Cool R Report");
        assertTextPresent("\"name\"");
        assertTextPresent("\"age\"");
        assertTextPresent("\"crazy\"");
    }

    private void assertModuleDeployed(String moduleName)
    {
        log("Ensuring that that '" + moduleName + "' module is deployed");
        clickLinkWithText("Admin Console");
        assertTextPresent(moduleName);
    }

    protected void doCleanup() throws Exception
    {
        try
        {
            deleteProject(PROJECT_NAME);
        }
        catch(Throwable ignore) {}
        log("Cleaned up SimpleModuleTest project.");
    }

    public String getAssociatedModuleDirectory()
    {
        //return "none" to skip verification of module directory
        //as it won't exist until after the test starts running the first time
        return "none";
    }
}
