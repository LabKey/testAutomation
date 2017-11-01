/*
 * Copyright (c) 2009-2017 LabKey Corporation
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

import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.TestTimeoutException;
import org.labkey.test.categories.DailyA;
import org.labkey.test.util.ListHelper;

import java.util.Arrays;
import java.util.List;

@Category({DailyA.class})
public class SchemaBrowserTest extends BaseWebDriverTest
{
    public static final String PROJECT_NAME = "Schema Browser Test Project";
    public static final String TEST_DESC_BOOKS = "This is a test description on books";
    public static final String TEST_DESC_AUTHORS = "This is a test description on authors";
    public static final String TEST_DESC_PUBLISHERS = "This is a test description on publishers";
    public static final String AUTHORS_LIST = "Authors" + TRICKY_CHARACTERS_NO_QUOTES;
    public static final String PUBLISHERS_LIST = "Publishers" + TRICKY_CHARACTERS_NO_QUOTES;
    public static final String BOOKS_LIST = "Books" + TRICKY_CHARACTERS_NO_QUOTES;

    public List<String> getAssociatedModules()
    {
        return Arrays.asList("query");
    }

    @Override
    protected BrowserType bestBrowser()
    {
        return BrowserType.CHROME;
    }

    @Override
    protected String getProjectName()
    {
        return PROJECT_NAME;
    }

    protected void doCleanup(boolean afterTest) throws TestTimeoutException
    {
        _containerHelper.deleteProject(getProjectName(), afterTest);
    }

    @Test
    public void testSteps()
    {
        _containerHelper.createProject(PROJECT_NAME, "Collaboration");
        createLists();
        goToSchemaBrowser();

        // test home page links
        waitAndClick(Locator.tagWithClass("span", "labkey-link").withText("core"));
        waitForElement(Locator.tag("h2").withClasses("x4-component", "x4-component-default").withText("core Schema"));

        selectQuery("lists", BOOKS_LIST);

        waitForElement(Locator.xpath("//td[contains(text(), '" + TEST_DESC_BOOKS + "')]"), WAIT_FOR_JAVASCRIPT);
        assertTextPresent("Title", "Subtitle", "AuthorId", "PublisherId", "TitleId");

        //test lookup links, tab management
        click(Locator.lookupLink("lists", AUTHORS_LIST, "AuthorId"));
        waitForElement(Locator.xpath("//td[contains(text(), '" + TEST_DESC_AUTHORS + "')]"), WAIT_FOR_JAVASCRIPT);

        _ext4Helper.closeExtTab("lists." + AUTHORS_LIST);
        sleep(500);
        assertTextNotPresent(TEST_DESC_AUTHORS);

        click(Locator.lookupLink("lists", PUBLISHERS_LIST, "PublisherId"));
        waitForElement(Locator.xpath("//td[contains(text(), '" + TEST_DESC_PUBLISHERS + "')]"), WAIT_FOR_JAVASCRIPT);

        _ext4Helper.closeExtTab("lists." + PUBLISHERS_LIST);
        sleep(500);
        assertTextNotPresent(TEST_DESC_PUBLISHERS);

        //test in-place fk expansion
        clickFkExpando("lists", BOOKS_LIST, "AuthorId");
        waitForText(WAIT_FOR_JAVASCRIPT, "AuthorId/FirstName");
        assertTextPresent("AuthorId/LastName");
        assertElementPresent(Locator.tag("td").withText(TEST_DESC_AUTHORS).notHidden());

        clickFkExpando("lists", BOOKS_LIST, "PublisherId");
        waitForText(WAIT_FOR_JAVASCRIPT, "PublisherId/Name");
        assertElementPresent(Locator.tag("td").withText(TEST_DESC_PUBLISHERS).notHidden());

        clickFkExpando("lists", BOOKS_LIST, "AuthorId");
        assertElementNotPresent(Locator.tag("td").withText(TEST_DESC_AUTHORS).notHidden());

        clickFkExpando("lists", BOOKS_LIST, "PublisherId");
        assertElementNotPresent(Locator.tag("td").withText(TEST_DESC_PUBLISHERS).notHidden());

        validateQueries(true);
    }

    public void createLists()
    {
        _listHelper.createList(PROJECT_NAME, AUTHORS_LIST,
                ListHelper.ListColumnType.AutoInteger, "AuthorId",
                new ListHelper.ListColumn("FirstName", "First Name", ListHelper.ListColumnType.String, TEST_DESC_AUTHORS),
                new ListHelper.ListColumn("LastName", "Last Name", ListHelper.ListColumnType.String, "")
        );

        _listHelper.createList(PROJECT_NAME, PUBLISHERS_LIST,
                ListHelper.ListColumnType.AutoInteger, "PublisherId",
                new ListHelper.ListColumn("Name", "Name", ListHelper.ListColumnType.String, TEST_DESC_PUBLISHERS)
        );

        _listHelper.createList(PROJECT_NAME, BOOKS_LIST,
                ListHelper.ListColumnType.AutoInteger, "TitleId",
                new ListHelper.ListColumn("Title", "Title", ListHelper.ListColumnType.String, TEST_DESC_BOOKS),
                new ListHelper.ListColumn("Subtitle", "Subtitle", ListHelper.ListColumnType.String, ""),
                new ListHelper.ListColumn("AuthorId", "AuthorId", ListHelper.ListColumnType.Integer, "", new ListHelper.LookupInfo("", "lists", AUTHORS_LIST)),
                new ListHelper.ListColumn("PublisherId", "PublisherId", ListHelper.ListColumnType.Integer, "", new ListHelper.LookupInfo("", "lists", PUBLISHERS_LIST))
        );
    }
}
