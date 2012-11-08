/*
 * Copyright (c) 2012 LabKey Corporation
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

import junit.framework.Assert;
import org.apache.commons.lang.StringUtils;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.util.ext4cmp.Ext4FieldRefWD;

import java.net.URISyntaxException;
import java.util.Random;

/**
 * Created by IntelliJ IDEA.
 * User: bbimber
 * Date: 5/28/12
 * Time: 8:56 PM
 */
public class LabModuleHelper
{
    private BaseWebDriverTest _test;
    private final Random _random = new Random(System.currentTimeMillis());

    public static final String CTL_COLOR = "rgb(255, 192, 203)";
    public static final String UNKNOWN_COLOR = "rgb(0, 0, 255)";
    public static final String NTC_COLOR = "rgb(255, 255, 0)";
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
        _test.checkRadioButton("providerName", provider);
        _test.clickButton("Next");

        Locator l = Locator.id("AssayDesignerName");
        _test.waitForElement(l, _test.WAIT_FOR_JAVASCRIPT);
        _test.setFormElement(l, label);

        _test.sleep(2000);
        _test.clickButton("Save", 0);
        _test.waitForText("Save successful.", 20000);
        _test.assertTextNotPresent("Unknown");
    }

    public static Locator getNavPanelItem(String label, String itemText)
    {
        //NOTE: this should return only visible items
        return Locator.xpath("//span[text() = '" + label + "']/../../../div[not(contains(@style, 'display: none'))]//span[text() = '" + itemText + "']");
    }

    public void clickNavPanelItem(String label, String itemText)
    {
        Locator l = getNavPanelItem(label, itemText);
        _test.waitForElement(l);
        _test.waitAndClick(l);
    }

    public void clickNavPanelItem(String itemText)
    {
        Locator l = Locator.xpath("//span[contains(text(), '" + itemText + "')]");
        _test.waitForElement(l);
        _test.waitAndClick(l);
    }

    public static Locator getNavPanelRow(String label)
    {
        return Locator.xpath("//div[descendant::span[text() = '" + label + "']]");
    }

    public void goToLabHome()
    {
        _test.goToProjectHome();
        _test.waitForText("Types of Data:");
    }

    public void verifyNavPanelRowItemPresent(String label)
    {
        _test.log("Verifying NavPanel row present with label: " + label);
        Assert.assertTrue("Row missing: " + label, _test.isElementPresent(getNavPanelRow(label)));
    }

    public static Locator webpartTitle(String title)
    {
        return Locator.xpath("//span[contains(@class, 'labkey-wp-title-text') and text() = '" + title + "']");
    }

    public String createWorkbook(String workbookTitle, String workbookDescription)
    {
        _test.clickTab("Workbooks");
        _test.clickButton("Create New Workbook", 0);
        _test.waitForElement(Ext4Helper.ext4Window("Create Workbook"));
        _test.setFormElement(Locator.name("title"), workbookTitle);
        _test.setFormElement(Locator.name("description"), workbookDescription);
        _test.clickButton("Submit");
        _test.waitForPageToLoad();

        try
        {
            String path = _test.getURL().toURI().getPath();
            path = path.replaceAll(".*/workbook-", "");
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
        _test.waitForElement(l, _test.WAIT_FOR_JAVASCRIPT * 3);
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
        _test.waitFor(new BaseWebDriverTest.Checker()
        {
            @Override
            public boolean check()
            {
                return Ext4FieldRefWD.isFieldPresent(_test, label);
            }
        }, "Field did not appear: " + label, BaseWebDriverTest.WAIT_FOR_JAVASCRIPT);
    }

    public void addRecordsToAssayTemplate(String[][] data)
    {
        _test.log("Setting assay template");

        StringBuilder sb = new StringBuilder();
        for (String[] row : data)
        {
            sb.append(StringUtils.join(row, '\t'));
            sb.append(System.getProperty("line.separator"));
        }

        _test.waitForText("Sample Information");
        _test.waitAndClick(Locator.ext4Button("Add From Spreadsheet"));
        _test.waitForElement(Ext4Helper.ext4Window("Spreadsheet Import"));

        Ext4FieldRefWD textArea = _test._ext4Helper.queryOne("#textField", Ext4FieldRefWD.class);
        textArea.setValue(sb.toString());
        _test.waitAndClick(Locator.ext4Button("Submit"));

        String[] lastRow = data[data.length - 1];
        String cell = lastRow[0];
        _test.waitForElement(ext4GridCell(cell));
    }

    public Locator ext4GridCell(String cell)
    {
        return Locator.xpath("//div[contains(@class, 'x4-grid-cell-inner') and text() = '" + cell + "']");
    }

    public Locator getAssayWell(String text, String color)
    {
        return Locator.xpath("//div[contains(@style, '" + color + "') and text() = '" + text + "']");
    }
}
