/*
 * Copyright (c) 2010-2013 LabKey Corporation
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

import org.junit.experimental.categories.Category;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.TestTimeoutException;
import org.labkey.test.categories.BVT;
import org.labkey.test.categories.Wiki;
import org.labkey.test.util.ListHelperWD;
import org.labkey.test.util.PortalHelper;

/**
 * User: brittp
 * Date: Apr 28, 2010 2:38:02 PM
 */
@Category({BVT.class, Wiki.class})
public class ButtonCustomizationTest extends BaseWebDriverTest
{
    protected final static String PROJECT_NAME = "ButtonVerifyProject";
    private static final String LIST_NAME = "Cities";
    private static final String METADATA_OVERRIDE_BUTTON = "Metadata Button";
    private static final String METADATA_OVERRIDE_ON_CLICK_BUTTON = "On-click handler";
    private static final String METADATA_OVERRIDE_ON_CLICK_MSG = "Metadata Button Clicked!";
    private static final String METADATA_OVERRIDE_LINK_BUTTON = "Standard link";
    private static final String METADATA_OVERRIDE_LINK = "announcements/begin";
    private static final String METADATA_GET_BUTTON = "GET Button";
    private static final String METADATA_GET_URL = "project/begin";
    private static final String METADATA_LINK_BUTTON = "LINK Button";
    private static final String METADATA_LINK_URL = "project/begin";

    private static final String JAVASCRIPT_LINK_BUTTON_TEXT = "JavaScript Link Button";
    private static final String JAVASCRIPT_ONCLICK_BUTTON_TEXT = "JavaScript OnClick Button";
    private static final String JAVASCRIPT_HANDLER_BUTTON_TEXT = "JavaScript Handler Button";
    private static final String JAVASCRIPT_MENU_BUTTON_TEXT = "JavaScript Menu Button";
    private static final String JAVASCRIPT_MENU_SUBBUTTON1_TEXT = "JavaScript Menu1";
    private static final String JAVASCRIPT_MENU_SUBBUTTON2_TEXT = "JavaScript Menu2";
    private static final String JAVASCRIPT_MENU_SUBSUBBUTTON_TEXT = "JavaScript Fly Out";

    private static final String JAVASCRIPT_MENU_ONCLICK_ALERT_TEXT = "JavaScript OnClick Alert";
    private static final String JAVASCRIPT_MENU_HANDLER_ALERT1_TEXT = "JavaScript Handler Alert 1";
    private static final String JAVASCRIPT_MENU_HANDLER_ALERT2_TEXT = "JavaScript Handler Alert 2";
    private static final String JAVASCRIPT_MENU_HANDLER_ALERT3_TEXT = "JavaScript Handler Alert 3";

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

    private static final String PARAM_ECHO_JAVASCRIPT =
            "<script type=\"text/javascript\">\n" +
            "Ext.onReady(function()\n" +
            "{\n" +
            "    var html = \"\";\n" +
            "    var parameters = LABKEY.ActionURL.getParameters();\n" +
            "    for (var parameter in parameters)\n" +
            "        html += parameter + \": \" + parameters[parameter] + \"<br>\";\n" +
            "    document.getElementById(\"params\").innerHTML = html;\n" +
            "});\n" +
            "</script>\n" +
            "<div id=\"params\">";

    private String getJavaScriptCustomizer()
    {
        return "<div id='queryTestDiv1'></div>\n" +
                "<script type=\"text/javascript\">\n" +
                "var qwp1 = new LABKEY.QueryWebPart({\n" +
                "    renderTo: 'queryTestDiv1',\n" +
                "    title: 'My Query Web Part',\n" +
                "    schemaName: 'lists',\n" +
                "    queryName: '" + LIST_NAME + "',\n" +
                "    buttonBar: {\n" +
                "        includeStandardButtons: true,\n" +
                "        items:[\n" +
                "          {text: '" + JAVASCRIPT_LINK_BUTTON_TEXT + "', url: LABKEY.ActionURL.buildURL('announcements', 'begin')},\n" +
                "          {text: '" + JAVASCRIPT_ONCLICK_BUTTON_TEXT + "', onClick: \"alert('" + JAVASCRIPT_MENU_ONCLICK_ALERT_TEXT + "'); return false;\"},\n" +
                "          {text: '" + JAVASCRIPT_HANDLER_BUTTON_TEXT + "', handler: onTestHandler},\n" +
                "          {text: '" + JAVASCRIPT_MENU_BUTTON_TEXT + "', items: [\n" +
                "            '-', //separator\n" +
                "            {text: '" + JAVASCRIPT_MENU_SUBBUTTON1_TEXT + "', handler: onItem1Handler},\n" +
                "\t\t    {text: '" + JAVASCRIPT_MENU_SUBBUTTON2_TEXT + "', items: [\n" +
                "              {text: '" + JAVASCRIPT_MENU_SUBSUBBUTTON_TEXT + "', handler: onItem2Handler}\n" +
                "            ]},\n" +
                "          ]}\n" +
                "        ]\n" +
                "    }\n" +
                "});\n" +
                "\n" +
                "function onTestHandler(dataRegion)\n" +
                "{\n" +
                "    alert(\"" + JAVASCRIPT_MENU_HANDLER_ALERT1_TEXT + "\");\n" +
                "    return false;\n" +
                "}\n" +
                "\n" +
                "function onItem1Handler(dataRegion)\n" +
                "{\n" +
                "    alert(\"" + JAVASCRIPT_MENU_HANDLER_ALERT2_TEXT + "\");\n" +
                "}\n" +
                "\n" +
                "function onItem2Handler(dataRegion)\n" +
                "{\n" +
                "    alert(\"" + JAVASCRIPT_MENU_HANDLER_ALERT3_TEXT + "\");\n" +
                "}\n" +
                "\n" +
                "</script>";
    }

    @Override
    protected String getProjectName()
    {
        return PROJECT_NAME;
    }

    @Override
    protected void doTestSteps() throws Exception
    {
        _containerHelper.createProject(PROJECT_NAME, null);

        ListHelperWD.ListColumn[] columns = new ListHelperWD.ListColumn[] {
                new ListHelperWD.ListColumn("name", "Name", ListHelperWD.ListColumnType.String, "")
        };

        _listHelper.createList(PROJECT_NAME, LIST_NAME, ListHelperWD.ListColumnType.AutoInteger, "Key", columns);

        clickButton("Done");
        clickAndWait(Locator.linkWithText(LIST_NAME));
        assertNavButtonNotPresent(METADATA_OVERRIDE_BUTTON);
        clickButton("Insert New");
        setFormElement(Locator.name("quf_name"), "Seattle");
        clickButton("Submit");
        clickButton("Insert New");
        setFormElement(Locator.name("quf_name"), "Portland");
        clickButton("Submit");
        
        // assert custom buttons can be added to the standard set:
        beginAt("/query/" + PROJECT_NAME + "/schema.view?schemaName=lists");
        selectQuery("lists", "Cities");
        waitForText("edit metadata", 10000);
        clickAndWait(Locator.linkWithText("edit metadata"));
        // wait for the domain editor to appear:
        waitAndClickButton("Edit Source");
        _extHelper.clickExtTab("XML Metadata");
        setCodeEditorValue("metadataText", getMetadataXML(true));
        _extHelper.clickExtTab("Source");
        clickButtonByIndex("Save", 1, 0);        // 0: source/save 1: metadata/save
        waitForText("Saved", WAIT_FOR_JAVASCRIPT);
        clickButton("Execute Query", 0);
        waitForText("Seattle", WAIT_FOR_JAVASCRIPT);
        assertNavButtonPresent(METADATA_OVERRIDE_BUTTON);
        _extHelper.clickExtTab("Source");
        clickButton("Save & Finish");
        assertNavButtonPresent(METADATA_OVERRIDE_BUTTON);
        assertNavButtonPresent("Insert New");

        // assert custom buttons can REPLACE the standard set:
        beginAt("/query/" + PROJECT_NAME + "/schema.view?schemaName=lists");
        selectQuery("lists", "Cities");
        waitForText("edit metadata", 10000);
        clickAndWait(Locator.linkWithText("edit metadata"));
        waitAndClickButton("Edit Source");
        _extHelper.clickExtTab("XML Metadata");
        setCodeEditorValue("metadataText", getMetadataXML(false));
        clickButton("Save", 0);
        waitForElement(Locator.id("status").withText("Saved"));
        _extHelper.clickExtTab("Source");
        clickButton("Save & Finish");
        verifyMetadataButtons();

        // Create a wiki page to hold a query webpart with JavaScript-based button customization:
        PortalHelper portalHelper = new PortalHelper(this);
        clickProject(PROJECT_NAME);
        portalHelper.addWebPart("Wiki");
        createNewWikiPage("HTML");
        setFormElement(Locator.name("name"), "buttonTest");
        setFormElement(Locator.name("title"), "buttonTest");
        setWikiBody(getJavaScriptCustomizer());
        clickButton("Save & Close");

        portalHelper.addWebPart("Wiki");
        createNewWikiPage("HTML");
        setFormElement(Locator.name("name"), "paramEcho");
        setFormElement(Locator.name("title"), "Parameter Echo");
        setWikiBody(PARAM_ECHO_JAVASCRIPT);
        clickButton("Save & Close");

        waitForElement(Locator.xpath("//*[starts-with(@id, 'aqwp')]"));
        clickButton(JAVASCRIPT_LINK_BUTTON_TEXT);
        assertTextPresent("No messages");
        clickProject(PROJECT_NAME);

        waitForElement(Locator.xpath("//*[starts-with(@id, 'aqwp')]"));
        clickButton(JAVASCRIPT_ONCLICK_BUTTON_TEXT, 0);
        assertAlert(JAVASCRIPT_MENU_ONCLICK_ALERT_TEXT);

        clickButton(JAVASCRIPT_HANDLER_BUTTON_TEXT, 0);
        assertAlert(JAVASCRIPT_MENU_HANDLER_ALERT1_TEXT);

        _extHelper.clickMenuButton(false, JAVASCRIPT_MENU_BUTTON_TEXT, JAVASCRIPT_MENU_SUBBUTTON1_TEXT);
        assertAlert(JAVASCRIPT_MENU_HANDLER_ALERT2_TEXT);

        _extHelper.clickMenuButton(false, JAVASCRIPT_MENU_BUTTON_TEXT, JAVASCRIPT_MENU_SUBBUTTON2_TEXT, JAVASCRIPT_MENU_SUBSUBBUTTON_TEXT);
        assertAlert(JAVASCRIPT_MENU_HANDLER_ALERT3_TEXT);

        assertNavButtonNotPresent(METADATA_GET_BUTTON);
        assertNavButtonNotPresent(METADATA_LINK_BUTTON);

        checkCheckboxByNameInDataRegion("Portland");
        // wait for the button to enable:
        waitForElement(Locator.navButton(METADATA_LINK_BUTTON), 10000);

        // Verify that link buttons don't send parameters at all:
        clickButton(METADATA_LINK_BUTTON);
        assertElementNotPresent(Locator.id("params").containing(".select"));

        // wait for the button to enable:
        waitForElement(Locator.navButton(METADATA_GET_BUTTON), 10000);
        
        // Verify that GET buttons to send form values as GET parameters:
        clickButton(METADATA_GET_BUTTON);
        assertElementPresent(Locator.id("params").containing(".select: 2"));

        // Verify that the JavaScript button override added to the metadata-defined button bar, rather than replacing it:
        verifyMetadataButtons();
    }

    private void verifyMetadataButtons()
    {
        // The query view webpart populates asynchronously, so we may need to wait for it to appear:
        waitForElement(Locator.navButton(METADATA_OVERRIDE_BUTTON), 10000);

        assertNavButtonNotPresent("Insert New");

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
    public String getAssociatedModuleDirectory()
    {
        return "server/modules/list";
    }
}
