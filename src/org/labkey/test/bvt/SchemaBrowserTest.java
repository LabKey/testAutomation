/*
 * Copyright (c) 2009-2010 LabKey Corporation
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
import org.labkey.test.Locator;
import org.labkey.test.util.ExtHelper;
import org.labkey.test.util.ListHelper;

/*
* User: dave
* Date: Sep 23, 2009
* Time: 1:19:40 PM
*/
public class SchemaBrowserTest extends BaseSeleniumWebTest
{
    public static final String PROJECT_NAME = "Schema Browser Test Project";
    public static final String TEST_DESC_BOOKS = "This is a test description on books";
    public static final String TEST_DESC_AUTHORS = "This is a test description on authors";
    public static final String TEST_DESC_PUBLISHERS = "This is a test description on publishers";

    protected void doTestSteps() throws Exception
    {
        createProject(PROJECT_NAME, "Collaboration");
        createLists();
        goToSchemaBrowser();
        selectQuery("lists", "Books");

        waitForElement(Locator.xpath("//td[contains(text(), '" + TEST_DESC_BOOKS + "')]"), WAIT_FOR_JAVASCRIPT);
        assertTextPresent("Title");
        assertTextPresent("Subtitle");
        assertTextPresent("AuthorId");
        assertTextPresent("PublisherId");
        assertTextPresent("TitleId");

        //test lookup links, tab management
        clickLookupLink("lists", "Authors", "AuthorId");
        waitForElement(Locator.xpath("//td[contains(text(), '" + TEST_DESC_AUTHORS + "')]"), WAIT_FOR_JAVASCRIPT);

        ExtHelper.closeExtTab(this, "lists.Authors");
        sleep(500);
        assertTextNotPresent(TEST_DESC_AUTHORS);

        clickLookupLink("lists", "Publishers", "PublisherId");
        waitForElement(Locator.xpath("//td[contains(text(), '" + TEST_DESC_PUBLISHERS + "')]"), WAIT_FOR_JAVASCRIPT);

        ExtHelper.closeExtTab(this, "lists.Publishers");
        sleep(500);
        assertTextNotPresent(TEST_DESC_PUBLISHERS);

        //test in-place fk expansion
        clickFkExpando("lists", "Books", "AuthorId");
        waitForText("AuthorId/FirstName", WAIT_FOR_JAVASCRIPT);
        assertTextPresent("AuthorId/LastName");
        assertTextPresent(TEST_DESC_AUTHORS);

        clickFkExpando("lists", "Books", "PublisherId");
        waitForText("PublisherId/Name", WAIT_FOR_JAVASCRIPT);
        assertTextPresent(TEST_DESC_PUBLISHERS);

        clickFkExpando("lists", "Books", "AuthorId");
        assertTextNotPresent(TEST_DESC_AUTHORS); //it's really just hidden, but the new selenium won't report it as present

        clickFkExpando("lists", "Books", "PublisherId");
        assertTextNotPresent(TEST_DESC_PUBLISHERS);

        validateQueries();
    }

    public void createLists()
    {
        ListHelper.createList(this, PROJECT_NAME, "Authors",
                ListHelper.ListColumnType.AutoInteger, "AuthorId",
                new ListHelper.ListColumn("FirstName", "First Name", ListHelper.ListColumnType.String, TEST_DESC_AUTHORS),
                new ListHelper.ListColumn("LastName", "Last Name", ListHelper.ListColumnType.String, "")
        );

        ListHelper.createList(this, PROJECT_NAME, "Publishers",
                ListHelper.ListColumnType.AutoInteger, "PublisherId",
                new ListHelper.ListColumn("Name", "Name", ListHelper.ListColumnType.String, TEST_DESC_PUBLISHERS)
        );

        ListHelper.createList(this, PROJECT_NAME, "Books",
                ListHelper.ListColumnType.AutoInteger, "TitleId",
                new ListHelper.ListColumn("Title", "Title", ListHelper.ListColumnType.String, TEST_DESC_BOOKS),
                new ListHelper.ListColumn("Subtitle", "Subtitle", ListHelper.ListColumnType.String, ""),
                new ListHelper.ListColumn("AuthorId", "AuthorId", ListHelper.ListColumnType.Integer, "", new ListHelper.LookupInfo("", "lists", "Authors")),
                new ListHelper.ListColumn("PublisherId", "PublisherId", ListHelper.ListColumnType.Integer, "", new ListHelper.LookupInfo("", "lists", "Publishers"))
        );
    }

    protected void doCleanup() throws Exception
    {
        try
        {
            deleteProject(PROJECT_NAME);
        }
        catch(Exception ignore) {}
    }

    public String getAssociatedModuleDirectory()
    {
        return "server/modules/query";
    }
}
