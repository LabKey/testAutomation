package org.labkey.test.bvt;

import org.labkey.test.BaseSeleniumWebTest;
import org.labkey.test.util.ListHelper;

/**
 * Copyright (c) 2010 LabKey Corporation
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * <p/>
 * <p/>
 * User: brittp
 * Date: Apr 28, 2010 2:38:02 PM
 */
public class ButtonCustomizationTest extends BaseSeleniumWebTest
{
    protected final static String PROJECT_NAME = "ButtonVerifyProject";
    private static final String LIST_NAME = "Cities";
    private static final String METADATA_OVERRIDE_BUTTON = "Metadata Button";
    private static final String METADATA_OVERRIDE_ON_CLICK_BUTTON = "On-click handler";
    private static final String METADATA_OVERRIDE_ON_CLICK_MSG = "Metadata Button Clicked!";
    private static final String METADATA_OVERRIDE_LINK_BUTTON = "Standard link";
    private static final String METADATA_OVERRIDE_LINK = "announcements/begin";

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
        "                <ns:url>" + METADATA_OVERRIDE_LINK + "</ns:url>\n" +
        "            </ns:item>\n" +
        "        </ns:item>\n" +
        "    </ns:buttonBarOptions>  \n" +
        "  </ns:table>\n" +
        "</ns:tables>";
    }

    private String getJavaScriptCustomizer()
    {
        return "<div id='queryTestDiv1'/>\n" +
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
    protected void doTestSteps() throws Exception
    {
        createProject(PROJECT_NAME);

        ListHelper.ListColumn[] columns = new ListHelper.ListColumn[] {
                new ListHelper.ListColumn("name", "Name", ListHelper.ListColumnType.String, "")
        };

        ListHelper.createList(this, PROJECT_NAME, LIST_NAME, ListHelper.ListColumnType.AutoInteger, "Key", columns);

        clickNavButton("Done");
        clickLinkWithText("view data");
        assertNavButtonPresent("Insert New");
        assertNavButtonNotPresent(METADATA_OVERRIDE_BUTTON);

        // assert custom buttons can be added to the standard set:
        beginAt("/query/" + PROJECT_NAME + "/schema.view?schemaName=lists");
        selectQuery("lists", "Cities");
        waitForText("edit metadata", 10000);
        clickLinkWithText("edit metadata");
        // wait for the domain editor to appear:
        waitForText("Label", 10000);
        clickNavButton("Edit Source");
        setText("ff_metadataText", getMetadataXML(true));
        clickNavButton("View Data");
        assertNavButtonPresent(METADATA_OVERRIDE_BUTTON);
        assertNavButtonPresent("Insert New");

        // assert custom buttons can REPLACE the standard set:
        beginAt("/query/" + PROJECT_NAME + "/schema.view?schemaName=lists");
        selectQuery("lists", "Cities");
        waitForText("edit metadata", 10000);
        clickLinkWithText("edit metadata");
        waitForText("Edit Source", 10000);
        clickNavButton("Edit Source");
        setText("ff_metadataText", getMetadataXML(false));
        clickNavButton("View Data");
        verifyMetadataButtons();

        // Create a wiki page to hold a query webpart with JavaScript-based button customization:
        clickLinkWithText(PROJECT_NAME);
        addWebPart("Wiki");
        createNewWikiPage("HTML");
        setFormElement("name", "buttonTest");
        setFormElement("title", "buttonTest");
        setWikiBody(getJavaScriptCustomizer());
        clickNavButton("Save & Close");

        waitForText(JAVASCRIPT_LINK_BUTTON_TEXT, 10000);
        clickNavButton(JAVASCRIPT_LINK_BUTTON_TEXT);
        assertTextPresent("No messages");
        clickLinkWithText(PROJECT_NAME);

        waitForText(JAVASCRIPT_ONCLICK_BUTTON_TEXT, 10000);
        clickNavButton(JAVASCRIPT_ONCLICK_BUTTON_TEXT, 0);
        assertAlert(JAVASCRIPT_MENU_ONCLICK_ALERT_TEXT);

        clickNavButton(JAVASCRIPT_HANDLER_BUTTON_TEXT, 0);
        assertAlert(JAVASCRIPT_MENU_HANDLER_ALERT1_TEXT);

        clickMenuButtonAndContinue(JAVASCRIPT_MENU_BUTTON_TEXT, JAVASCRIPT_MENU_SUBBUTTON1_TEXT);
        assertAlert(JAVASCRIPT_MENU_HANDLER_ALERT2_TEXT);

        clickMenuButtonAndContinue(JAVASCRIPT_MENU_BUTTON_TEXT, JAVASCRIPT_MENU_SUBBUTTON2_TEXT, JAVASCRIPT_MENU_SUBSUBBUTTON_TEXT);
        assertAlert(JAVASCRIPT_MENU_HANDLER_ALERT3_TEXT);

        // Verify that the JavaScript button override added to the metadata-defined button bar, rather than replacing it:
        verifyMetadataButtons();
    }

    private void verifyMetadataButtons()
    {
        assertNavButtonPresent(METADATA_OVERRIDE_BUTTON);
        assertNavButtonNotPresent("Insert New");

        clickMenuButtonAndContinue(METADATA_OVERRIDE_BUTTON, METADATA_OVERRIDE_ON_CLICK_BUTTON);
        assertAlert(METADATA_OVERRIDE_ON_CLICK_MSG);

        clickMenuButton(METADATA_OVERRIDE_BUTTON, METADATA_OVERRIDE_LINK_BUTTON);
        assertTextPresent("No messages");
    }

    @Override
    protected void doCleanup() throws Exception
    {
        try {deleteProject(PROJECT_NAME); } catch (Throwable t) {}
    }

    @Override
    public String getAssociatedModuleDirectory()
    {
        return "list";
    }
}
