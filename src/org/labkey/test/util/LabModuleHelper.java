/*
 * Copyright (c) 2012-2014 LabKey Corporation
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
package org.labkey.test.util;

import org.apache.commons.lang3.StringUtils;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.util.ext4cmp.Ext4CmpRef;
import org.labkey.test.util.ext4cmp.Ext4ComboRef;
import org.labkey.test.util.ext4cmp.Ext4FieldRef;
import org.labkey.test.util.ext4cmp.Ext4GridRef;

import java.io.File;
import java.net.URISyntaxException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Random;
import java.util.regex.Pattern;

import static org.labkey.test.BaseWebDriverTest.WAIT_FOR_PAGE;

import static org.junit.Assert.*;

public class LabModuleHelper
{
    private BaseWebDriverTest _test;
    public final static SimpleDateFormat _dateFormat = new SimpleDateFormat("yyyy-MM-dd");
    private final Random _random = new Random(System.currentTimeMillis());
    public static final String IMPORT_DATA_TEXT = "Import Data";
    public static final String UPLOAD_RESULTS_TEXT = "Upload Results";
    public static final String LAB_HOME_TEXT = "Types of Data:";

    public static final String POS_COLOR = "rgb(255, 192, 203)";
    public static final String UNKNOWN_COLOR = "rgb(0, 0, 255)";
    public static final String NEG_COLOR = "rgb(255, 255, 0)";
    public static final String STD_COLOR = "rgb(0, 128, 0)";

    public LabModuleHelper(BaseWebDriverTest test)
    {
        _test = test;
    }

    public void defineAssay(String provider, String label)
    {
        _test.log("Defining a test assay at the project level");
        //define a new assay at the project level
        //the pipeline must already be setup
        _test.goToProjectHome();

        //copied from old test
        _test.goToManageAssays();
        _test.clickButton("New Assay Design");
        _test.checkCheckbox(Locator.radioButtonByNameAndValue("providerName", provider));
        _test.clickButton("Next");

        Locator l = Locator.xpath("//input[@id='AssayDesignerName']");
        _test.waitForElement(l, BaseWebDriverTest.WAIT_FOR_JAVASCRIPT);
        l.findElement(_test.getDriver()).sendKeys(label + "\t");
        Locator desc = Locator.xpath("//textarea[@id='AssayDesignerDescription']");
        _test.setFormElement(desc, "This is an assay");
        _test.log(l.findElement(_test.getDriver()).getAttribute("value"));

        _test.waitForText("Result Fields");

        _test.sleep(1000);
        _test.clickButton("Save", 0);
        _test.waitForText("Save successful.", 20000);
        _test.assertTextNotPresent("Unknown");
    }

    public static Locator getNavPanelItem(String label, String itemText)
    {
        //NOTE: this should return only visible items
        return Locator.tag("div").withClass("ldk-navpanel-section-row").withDescendant(Locator.tag("span").withText(label)).append(Locator.linkWithText(itemText)).notHidden();
    }

    //NOTE: uses 1-based index
    public static Locator getNavPanelItem(String label, int index)
    {
        index--;
        return Locator.tag("div").withClass("ldk-navpanel-section-row").withDescendant(Locator.tag("span").withText(label)).append("//a").index(index).notHidden();
    }

    public void clickNavPanelItemAndWait(String label, int index)
    {
        Locator l = getNavPanelItem(label, index);
        _test.waitForElement(l);
        _test.waitAndClickAndWait(l);
    }

    public void clickNavPanelItem(String label, String itemText)
    {
        Locator l = getNavPanelItem(label, itemText);
        _test.waitForElement(l);
        _test.waitAndClick(l);
    }

    public static Locator getNavPanelRow(String label)
    {
        return Locator.tag("div").withClass("ldk-navpanel-section-row").withDescendant(Locator.tagContainingText("span",label));
    }

    public void goToLabHome()
    {
        _test.goToProjectHome();
        _test.waitForElement(Locator.tagWithText("span", LAB_HOME_TEXT));
    }

    public void verifyNavPanelRowItemPresent(String label)
    {
        _test.log("Verifying NavPanel row present with label: " + label);
        assertTrue("Row missing: " + label, _test.isElementPresent(getNavPanelRow(label)));
    }

    public static Locator webpartTitle(String title)
    {
        return Locator.xpath("//span[contains(@class, 'labkey-wp-title-text') and text() = '" + title + "']");
    }

    public String createWorkbook(String projectName, String workbookTitle, String workbookDescription)
    {
        return createWorkbook(projectName, workbookTitle, workbookDescription, true);
    }

    public String createWorkbook(String projectName, String workbookTitle, String workbookDescription, boolean shouldNavigateToTab)
    {
        if (shouldNavigateToTab)
            _test.clickTab("Workbooks");

        _test.clickButton("Create New Workbook", 0);
        _test.waitForElement(Ext4Helper.Locators.window("Create Workbook"));
        _test.setFormElement(Locator.name("title"), workbookTitle);
        _test.setFormElement(Locator.name("description"), workbookDescription);
        _test.clickButton("Submit");
        _test.waitForText("Workbook Summary");

        try
        {
            String path = _test.getURL().toURI().getPath();
            String prefix = projectName.replaceAll("begin.view", "");
            path = path.replaceAll(".*" + Pattern.quote(prefix) + "/", "");
            path = path.replaceAll("/begin.view", "");
            return path;
        }
        catch (URISyntaxException e)
        {
            throw new RuntimeException(e);
        }
    }

    public int getRandomInt()
    {
        return _random.nextInt(10000);
    }

    public String getNameForQueryWebpart(String title)
    {
        Locator l = Locator.xpath("//table[@name='webpart' and ./*/*/*/a//span[text()='" + title + "' or starts-with(text(), '" + title + ":')]]//table[starts-with(@id,'dataregion_') and not(contains(@id, 'header'))]");
        _test.waitForElement(l, BaseWebDriverTest.WAIT_FOR_JAVASCRIPT * 3);
        return _test.getAttribute(l, "id").substring(11);
    }

    public DataRegionTable getDrForQueryWebpart(String title)
    {
        return new DataRegionTable(getNameForQueryWebpart(title), _test);
    }

    public void setFormField(String name, String value)
    {
        _test.setFormElement(Locator.name(name), value);
        //there is a deliberate delay after user input for a change to commit in the Ext store
        _test.sleep(250);
    }

    public void waitForField(final String label)
    {
        waitForField(label, BaseWebDriverTest.WAIT_FOR_JAVASCRIPT);
    }

    public void waitForField(final String label, int wait)
    {
        _test.waitFor(new BaseWebDriverTest.Checker()
        {
            @Override
            public boolean check()
            {
                return Ext4FieldRef.isFieldPresent(_test, label);
            }
        }, "Field did not appear: " + label, wait);
    }

    public void waitForCmp(final String query)
    {
        _test.waitFor(new BaseWebDriverTest.Checker()
        {
            @Override
            public boolean check()
            {
                return _test._ext4Helper.queryOne(query, Ext4CmpRef.class) != null;
            }
        }, "Ext4 component did not appear for query: " + query, BaseWebDriverTest.WAIT_FOR_JAVASCRIPT);
    }

    public void waitForDisabled(final Ext4CmpRef cmp, final boolean state)
    {
        _test.waitFor(new BaseWebDriverTest.Checker()
        {
            @Override
            public boolean check()
            {
                return (Boolean)cmp.getEval("isDisabled() == arguments[0]", state);
            }
        }, "Component did not change to disabled = " + state, BaseWebDriverTest.WAIT_FOR_JAVASCRIPT);
    }

    public void addRecordsToAssayTemplate(String[][] data)
    {
        addRecordsToAssayTemplate(data, null);
    }

    public void addRecordsToAssayTemplate(String[][] data, List<String> expectedColumns)
    {
        _test.log("Setting assay template");

        StringBuilder sb = new StringBuilder();
        for (String[] row : data)
        {
            sb.append(StringUtils.join(row, '\t'));
            sb.append(System.getProperty("line.separator"));
        }

        _test.waitForText("Sample Information");
        _test.waitAndClick(Ext4Helper.Locators.ext4Button("Add From Spreadsheet"));
        _test.waitForElement(Ext4Helper.Locators.window("Spreadsheet Import"));

        if (expectedColumns != null)
        {
            Ext4CmpRef win = _test._ext4Helper.queryOne("window", Ext4CmpRef.class);
            _test.sleep(1000);
            String fields = (String)win.getEval("getFieldsInTemplateTest()");
            String[] fieldArray = fields.split(";");
            assertEquals("Incorrect column number in template", expectedColumns.size(), fieldArray.length);

            for (String field : fieldArray)
            {
                assertTrue("Field present in template that should not be: " + field, expectedColumns.contains(field));
            }
        }

        Ext4FieldRef textArea = _test._ext4Helper.queryOne("#textField", Ext4FieldRef.class);
        textArea.setValue(sb.toString());
        _test.waitAndClick(Ext4Helper.Locators.ext4Button("Submit"));

        String[] lastRow = data[data.length - 1];
        String cell = lastRow[0];
        _test.waitForElement(Ext4GridRef.locateExt4GridCell(cell));
    }

    public Locator getAssayWell(String text, String color)
    {
        return Locator.xpath("//div[contains(@style, '" + color + "') and normalize-space() = '" + text + "']");
    }

    public String getPageText()
    {
        //the browser converts line breaks to spaces.  this is a hack to get them back
        String text = _test.getDriver().getPageSource().replaceAll("<[^>]+>|&[^;]+;", "");
        text = text.replaceAll(" {2,}", " ");
        text = text.replaceAll(", ", ",\n").replaceAll("] ", "]\n");
        return text;
    }

    public void switchTabs()
    {
        String winHandleBefore = _test.getDriver().getWindowHandle();

    }

    public void goToAssayResultImport(String assayName)
    {
        goToAssayResultImport(assayName, true);
    }

    public void goToAssayResultImport(String assayName, boolean supportsTemplates)
    {
        goToLabHome();
        clickNavPanelItem(assayName + ":", IMPORT_DATA_TEXT);
        if (supportsTemplates)
            _test._ext4Helper.clickExt4MenuItem(UPLOAD_RESULTS_TEXT);

        _test.waitForElement(Ext4Helper.Locators.window(supportsTemplates ? UPLOAD_RESULTS_TEXT : IMPORT_DATA_TEXT));
        _test.waitAndClickAndWait(Ext4Helper.Locators.ext4Button("Submit"));
        _test.waitForText("Data Import");
    }

    public String getExampleData()
    {
        String currentWindow = _test.getDriver().getWindowHandle();
        for (String handle : _test.getDriver().getWindowHandles())
        {
            if (!currentWindow.equals(handle))
            {
                _test.getDriver().switchTo().window(handle);
                String text = getPageText();
                _test.getDriver().close();
                _test.getDriver().switchTo().window(currentWindow);
                return text;
            }
        }
        return null;
    }

    public Locator toolIcon(String name)
    {
        return Locator.tag("div").withClass("tool-icon").append(Locator.tagContainingText("a", name));
    }

    public void waitForDataRegion(String name)
    {
        _test.waitForElement(Locator.xpath("//table[@id=" + Locator.xq("dataregion_" + name) + "]"));
    }

    public void addDataSource(String type, String label, String reportCategory, String containerPath, String schema, String query)
    {
        Ext4CmpRef addBtn = _test._ext4Helper.queryOne("#manageDataSources button[text='Add New']", Ext4CmpRef.class);
        _test.waitAndClick(Locator.id(addBtn.getId()));
        _test.waitForElement(Ext4Helper.ext4Window("Add Data Source"));

        _test.waitForElementToDisappear(Locator.xpath("//div[contains(text(), 'Loading...')]"), BaseWebDriverTest.WAIT_FOR_JAVASCRIPT);
        waitForField("Item Type");
        Ext4FieldRef.getForLabel(_test, "Item Type").setValue(type);
        Ext4FieldRef.getForLabel(_test, "Label").setValue(label);
        Ext4FieldRef.getForLabel(_test, "Report Category").setValue(reportCategory);
        if (containerPath != null)
        {
            Ext4ComboRef combo = new Ext4ComboRef(Ext4FieldRef.getForLabel(_test, "Container (optional)"), _test);
            combo.setComboByDisplayValue(containerPath);
            _test.waitForElementToDisappear(Locator.xpath("//div[contains(text(), 'Loading...')]"), BaseWebDriverTest.WAIT_FOR_JAVASCRIPT);
        }

        Ext4FieldRef schemaField = Ext4FieldRef.getForLabel(_test, "Schema");
        schemaField.waitForEnabled();
        schemaField.setValue(schema);
        _test.waitForElementToDisappear(Locator.tagWithText("div", "Loading..."), BaseWebDriverTest.WAIT_FOR_JAVASCRIPT * 3);

        Ext4FieldRef queryField = Ext4FieldRef.getForLabel(_test, "Query");
        queryField.waitForEnabled();
        queryField.setValue(query);

        _test.waitAndClick(Ext4Helper.Locators.ext4Button("Save"));
        _test.waitForElement(Ext4Helper.ext4Window("Success"));
        _test.click(Ext4Helper.Locators.ext4Button("OK"));

        if (containerPath == null)
            _test.waitForElement(Locator.linkContainingText(schema + "." + query));
        else
            _test.waitForElement(Locator.linkContainingText(schema + "." + query + " (" + containerPath + ")"));
    }

    public void addDemographicsSource(String label, String containerPath, String schema, String query, String targetColumn, boolean expectSuccess, boolean isDuplicate)
    {
        Ext4CmpRef addBtn = _test._ext4Helper.queryOne("#manageDemographicsSources button[text='Add New']", Ext4CmpRef.class);
        _test.waitAndClick(Locator.id(addBtn.getId()));
        _test.waitForElement(Ext4Helper.ext4Window("Add Demographics Source"));

        _test.waitForElementToDisappear(Locator.xpath("//div[contains(text(), 'Loading...')]"), BaseWebDriverTest.WAIT_FOR_JAVASCRIPT);
        waitForField("Label");
        Ext4FieldRef.getForLabel(_test, "Label").setValue(label);

        if (containerPath != null)
        {
            Ext4ComboRef combo = new Ext4ComboRef(Ext4FieldRef.getForLabel(_test, "Container (optional)"), _test);
            combo.setComboByDisplayValue(containerPath);
            _test.waitForElementToDisappear(Locator.xpath("//div[contains(text(), 'Loading...')]"), BaseWebDriverTest.WAIT_FOR_JAVASCRIPT);
        }

        Ext4FieldRef.getForLabel(_test, "Schema").setValue(schema);
        _test.waitForElementToDisappear(Locator.xpath("//div[contains(text(), 'Loading...')]"), BaseWebDriverTest.WAIT_FOR_JAVASCRIPT * 3);

        Ext4FieldRef.getForLabel(_test, "Query").setValue(query);
        _test.waitForElementToDisappear(Locator.xpath("//div[contains(text(), 'Loading...')]"), BaseWebDriverTest.WAIT_FOR_JAVASCRIPT);

        if (targetColumn != null)
            Ext4FieldRef.getForLabel(_test, "Target Column").setValue(targetColumn);

        _test.waitAndClick(Ext4Helper.Locators.ext4Button("Save"));

        if (expectSuccess)
        {
            _test.waitForElement(Ext4Helper.ext4Window("Success"));
            _test.click(Ext4Helper.Locators.ext4Button("OK"));

            if (containerPath == null)
                _test.waitForElement(Locator.linkContainingText(schema + "." + query));
            else
                _test.waitForElement(Locator.linkContainingText(schema + "." + query + " (" + containerPath + ")"));
        }
        else
        {
            if (!isDuplicate)
            {
                //this indicates we did not expect this to be successful, so we make sure the right errors are shown
                _test.waitForElement(Ext4Helper.ext4Window("Error"));
                _test.click(Ext4Helper.Locators.ext4Button("OK"));
                _test.click(Ext4Helper.Locators.ext4Button("Cancel"));
            }
            else
            {
                _test.waitForElement(Ext4Helper.ext4Window("Error"));
                _test.click(Ext4Helper.Locators.ext4Button("OK"));
                _test.click(Ext4Helper.Locators.ext4Button("Cancel"));
            }
        }
    }

    public String getLegalNameFromName(String name)
    {
        if (name == null)
            return null;

        if (name.length() == 0)
            return null;

        StringBuilder buf = new StringBuilder(name.length());
        char[] chars = new char[name.length()];
        name.getChars(0, name.length(), chars, 0);
        //Different rule for first character
        int i = 0;
        while (i < name.length() && !Character.isJavaIdentifierStart(chars[i]))
            i++;
        //If no characters are identifier start (i.e. numeric col name), prepend "col" and try again..
        if (i == name.length())
        {
            buf.append("column");
            i = 0;
        }

        for (; i < name.length(); i++)
            if (Character.isJavaIdentifierPart(chars[i]))
                buf.append(chars[i]);

        return buf.toString();
    }

    public void waitForFile(final File file)
    {
        _test.waitFor(new BaseWebDriverTest.Checker()
        {
            @Override
            public boolean check()
            {
                return file.exists();
            }
        }, "Unable to find file: " + file.getPath(), WAIT_FOR_PAGE);
    }
}
