/*
 * Copyright (c) 2010-2016 LabKey Corporation
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
import org.labkey.test.TestFileUtils;
import org.labkey.test.TestTimeoutException;
import org.labkey.test.categories.DailyA;
import org.labkey.test.categories.Wiki;
import org.labkey.test.util.DataRegionTable;
import org.labkey.test.util.ListHelper;
import org.labkey.test.util.PortalHelper;
import org.labkey.test.util.WikiHelper;

import java.io.File;
import java.util.Arrays;
import java.util.List;

@Category({DailyA.class, Wiki.class})
public class ButtonCustomizationTest extends BaseWebDriverTest
{
    protected final static String PROJECT_NAME = "ButtonVerifyProject";
    private static final String LIST_NAME = "Cities"; // If changed, update in CUSTOMIZER_FILE as well.
    private static final String METADATA_OVERRIDE_BUTTON = "Metadata Button";
    private static final String METADATA_OVERRIDE_ON_CLICK_BUTTON = "On-click handler";
    private static final String METADATA_OVERRIDE_ON_CLICK_MSG = "Metadata Button Clicked!";
    private static final String METADATA_OVERRIDE_LINK_BUTTON = "Standard link";
    private static final String METADATA_OVERRIDE_LINK = "announcements/begin";
    private static final String METADATA_GET_BUTTON = "GET Button";
    private static final String METADATA_GET_URL = "project/begin";
    private static final String METADATA_LINK_BUTTON = "LINK Button";
    private static final String METADATA_LINK_URL = "project/begin";

    private static final String JAVASCRIPT_MENU_ONCLICK_ALERT_TEXT = "JavaScript OnClick Alert";
    private static final String JAVASCRIPT_MENU_HANDLER_ALERT1_TEXT = "JavaScript Handler Alert 1";
    private static final String JAVASCRIPT_MENU_HANDLER_ALERT2_TEXT = "JavaScript Handler Alert 2";
    private static final String JAVASCRIPT_MENU_HANDLER_ALERT3_TEXT = "JavaScript Handler Alert 3";

    private static final String PARAM_ECHO_CONTENT_FILE = "buttonCustomizationEcho.html";
    private static final String CUSTOMIZER_FILE = "buttonCustomizationCustomizer.html";

    private String getMetadataXML(boolean includeStandardButtons)
    {
        return "<ns:tables xmlns:ns=\"http://labkey.org/data/xml\">\n" +
        "  <ns:table tableName=\"" + LIST_NAME + "\" tableDbType=\"NOT_IN_DB\">\n" +
        "    <ns:columns></ns:columns>\n" +
        "    <ns:buttonBarOptions position=\"top\" includeStandardButtons=\"" + includeStandardButtons + "\">\n" +
        "        <ns:item text=\"" + METADATA_OVERRIDE_BUTTON + "\">\n" +
        "            <ns:item text=\"" + METADATA_OVERRIDE_ON_CLICK_BUTTON + "\">\n" +
        "                <ns:onClick>alert('" + METADATA_OVERRIDE_ON_CLICK_MSG + "');</ns:onClick>\n" +
        "            </ns:item>\n" +
        "            <ns:item text=\"" + METADATA_OVERRIDE_LINK_BUTTON + "\">\n" +
        "                <ns:target>" + METADATA_OVERRIDE_LINK + "</ns:target>\n" +
        "            </ns:item>\n" +
        "        </ns:item>\n" +
        "        <ns:item requiresSelection=\"true\" text=\"" + METADATA_GET_BUTTON + "\">\n" +
        "            <ns:target method=\"GET\">" + METADATA_GET_URL + "</ns:target>\n" +
        "        </ns:item>\n" +
        "        <ns:item requiresSelection=\"true\" text=\"" + METADATA_LINK_BUTTON + "\">\n" +
        "            <ns:target method=\"LINK\">" + METADATA_LINK_URL + "</ns:target>\n" +
        "        </ns:item>\n" +
        "    </ns:buttonBarOptions>  \n" +
        "  </ns:table>\n" +
        "</ns:tables>";
    }

    @Override
    protected String getProjectName()
    {
        return PROJECT_NAME;
    }

    @Test
    public void testSteps()
    {
        _containerHelper.createProject(PROJECT_NAME, null);

        ListHelper.ListColumn[] columns = new ListHelper.ListColumn[] {
                new ListHelper.ListColumn("name", "Name", ListHelper.ListColumnType.String, "")
        };

        _listHelper.createList(PROJECT_NAME, LIST_NAME, ListHelper.ListColumnType.AutoInteger, "Key", columns);

        clickButton("Done");
        clickAndWait(Locator.linkWithText(LIST_NAME));
        assertButtonNotPresent(METADATA_OVERRIDE_BUTTON);
        DataRegionTable.findDataRegion(this).clickInsertNewRowDropdown();
        setFormElement(Locator.name("quf_name"), "Seattle");
        clickButton("Submit");
        DataRegionTable.findDataRegion(this).clickInsertNewRowDropdown();
        setFormElement(Locator.name("quf_name"), "Portland");
        clickButton("Submit");
        
        // assert custom buttons can be added to the standard set:
        beginAt("/query/" + PROJECT_NAME + "/schema.view?schemaName=lists");
        selectQuery("lists", "Cities");
        waitForText(10000, "edit metadata");
        clickAndWait(Locator.linkWithText("edit metadata"));
        // wait for the domain editor to appear:
        clickButton("Edit Source", defaultWaitForPage);
        _extHelper.clickExtTab("XML Metadata");
        setCodeEditorValue("metadataText", getMetadataXML(true));
        _extHelper.clickExtTab("Source");
        clickButton("Save", 0);
        waitForText(WAIT_FOR_JAVASCRIPT, "Saved");
        clickButton("Execute Query", 0);
        waitForText(WAIT_FOR_JAVASCRIPT, "Seattle");
        assertButtonPresent(METADATA_OVERRIDE_BUTTON);
        _extHelper.clickExtTab("Source");
        clickButton("Save & Finish");
        assertButtonPresent(METADATA_OVERRIDE_BUTTON);
        assertButtonPresent("Insert");

        // assert custom buttons can REPLACE the standard set:
        beginAt("/query/" + PROJECT_NAME + "/schema.view?schemaName=lists");
        selectQuery("lists", "Cities");
        waitForText(10000, "edit metadata");
        clickAndWait(Locator.linkWithText("edit metadata"));
        clickButton("Edit Source", defaultWaitForPage);
        _extHelper.clickExtTab("XML Metadata");
        setCodeEditorValue("metadataText", getMetadataXML(false));
        clickButton("Save", 0);
        waitForElement(Locator.id("status").withText("Saved"));
        _extHelper.clickExtTab("Source");
        clickButton("Save & Finish");
        verifyMetadataButtons();

        // Create a wiki page to hold a query webpart with JavaScript-based button customization:
        PortalHelper portalHelper = new PortalHelper(this);
        WikiHelper wikiHelper = new WikiHelper(this);
        clickProject(PROJECT_NAME);
        portalHelper.addWebPart("Wiki");
        wikiHelper.createNewWikiPage("HTML");
        setFormElement(Locator.name("name"), "buttonTest");
        setFormElement(Locator.name("title"), "buttonTest");
        wikiHelper.setWikiBody(TestFileUtils.getFileContents(new File(TestFileUtils.getApiScriptFolder(), CUSTOMIZER_FILE)));
        clickButton("Save & Close");

        portalHelper.addWebPart("Wiki");
        wikiHelper.createNewWikiPage("HTML");
        setFormElement(Locator.name("name"), "paramEcho");
        setFormElement(Locator.name("title"), "Parameter Echo");
        wikiHelper.setWikiBody(TestFileUtils.getFileContents(new File(TestFileUtils.getApiScriptFolder(), PARAM_ECHO_CONTENT_FILE)));
        clickButton("Save & Close");

        DataRegionTable.findDataRegionWithinWebpart(this, "buttonTest");
        clickButton("JavaScript Link Button");
        assertTextPresent("No messages");
        clickProject(PROJECT_NAME);

        DataRegionTable buttonRegion = DataRegionTable.findDataRegionWithinWebpart(this, "buttonTest");
        clickButton("JavaScript OnClick Button", 0);
        assertAlert(JAVASCRIPT_MENU_ONCLICK_ALERT_TEXT);

        clickButton("JavaScript Handler Button", 0);
        assertAlert(JAVASCRIPT_MENU_HANDLER_ALERT1_TEXT);

        _extHelper.clickMenuButton(false, "JavaScript Menu Button", "JavaScript Menu1");
        assertAlert(JAVASCRIPT_MENU_HANDLER_ALERT2_TEXT);

        _extHelper.clickMenuButton(false, "JavaScript Menu Button", "JavaScript Menu2", "JavaScript Fly Out");
        assertAlert(JAVASCRIPT_MENU_HANDLER_ALERT3_TEXT);

        assertButtonNotPresent(METADATA_GET_BUTTON);
        assertButtonNotPresent(METADATA_LINK_BUTTON);

        buttonRegion.checkCheckbox(buttonRegion.getIndexWhereDataAppears("Portland", "Name"));
        // wait for the button to enable:
        waitForElement(Locator.lkButton(METADATA_LINK_BUTTON), 10000);

        // Verify that link buttons don't send parameters at all:
        clickButton(METADATA_LINK_BUTTON);
        assertElementNotPresent(Locator.id("params").containing(".select"));

        // wait for the button to enable:
        waitForElement(Locator.lkButton(METADATA_GET_BUTTON), 10000);
        
        // Verify that GET buttons to send form values as GET parameters:
        clickButton(METADATA_GET_BUTTON);
        assertElementPresent(Locator.id("params").containing(".select: 2"));

        // Verify that the JavaScript button override added to the metadata-defined button bar, rather than replacing it:
        verifyMetadataButtons();
    }

    private void verifyMetadataButtons()
    {
        // The query view webpart populates asynchronously, so we may need to wait for it to appear:
        waitForElement(Locator.lkButton(METADATA_OVERRIDE_BUTTON), 10000);

        assertButtonNotPresent(DataRegionTable.getInsertNewButtonText());

        _extHelper.clickMenuButton(false, METADATA_OVERRIDE_BUTTON, METADATA_OVERRIDE_ON_CLICK_BUTTON);
        assertAlert(METADATA_OVERRIDE_ON_CLICK_MSG);

        sleep(100); // Menu button sometimes isn't ready to open right away

        _extHelper.clickMenuButton(METADATA_OVERRIDE_BUTTON, METADATA_OVERRIDE_LINK_BUTTON);
        assertTextPresent("No messages");
    }

    @Override
    protected void doCleanup(boolean afterTest) throws TestTimeoutException
    {
        deleteProject(getProjectName(), afterTest);
    }

    @Override
    public List<String> getAssociatedModules()
    {
        return Arrays.asList("list");
    }
}
