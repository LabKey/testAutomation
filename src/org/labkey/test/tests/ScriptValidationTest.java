/*
 * Copyright (c) 2010-2017 LabKey Corporation
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

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.remoteapi.CommandException;
import org.labkey.remoteapi.Connection;
import org.labkey.remoteapi.query.Filter;
import org.labkey.remoteapi.query.InsertRowsCommand;
import org.labkey.remoteapi.query.SaveRowsResponse;
import org.labkey.remoteapi.query.SelectRowsCommand;
import org.labkey.remoteapi.query.SelectRowsResponse;
import org.labkey.remoteapi.query.UpdateRowsCommand;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.categories.DailyB;
import org.labkey.test.categories.Data;
import org.labkey.test.util.JSONHelper;
import org.labkey.test.util.ListHelper;
import org.labkey.test.util.LogMethod;
import org.labkey.test.util.Maps;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Test JavaScript validation in file-based modules.
 */
@Category({DailyB.class, Data.class})
public class ScriptValidationTest extends BaseWebDriverTest
{
    public static final String MODULE_NAME = "simpletest";
    public static final String VEHICLE_SCHEMA = "vehicle";

    public static class ColorRecord
    {
        public String name, hex;

        public ColorRecord(String name, String hex)
        {
            this.name = name;
            this.hex = hex;
        }

        public Map<String, Object> toMap()
        {
            return Maps.of("Name", name, "Hex", hex);
        }

        public static ColorRecord fromMap(Map<String, Object> map)
        {
            return new ColorRecord((String)map.get("Name"), (String)map.get("Hex"));
        }
    }

    @BeforeClass
    public static void initTest()
    {
        ScriptValidationTest init = (ScriptValidationTest) getCurrentTest();
        init.doSetup();
    }

    @LogMethod
    protected void doSetup()
    {
        _containerHelper.createProject(getProjectName(), null);
        _containerHelper.enableModule(getProjectName(), MODULE_NAME);
    }

    @Test
    public void testSteps() throws Exception
    {
        clickProject(getProjectName());
        doTestTransformation();
        doTestValidation();

        log("Create list to prevent query validation failure");
        _listHelper.createList(getProjectName(), "People",
                ListHelper.ListColumnType.AutoInteger, "Key",
                new ListHelper.ListColumn("Name", "Name", ListHelper.ListColumnType.String, "Name"),
                new ListHelper.ListColumn("Age", "Age", ListHelper.ListColumnType.Integer, "Age"),
                new ListHelper.ListColumn("Crazy", "Crazy", ListHelper.ListColumnType.Boolean, "Crazy?"));
    }

    private void doTestTransformation() throws Exception
    {
        List<ColorRecord> inserted = insertColors(Arrays.asList(
                new ColorRecord("Yellow", "#f00"),
                new ColorRecord("Cyan", "#0f0")
        ));
        assertEquals("Yellow!", inserted.get(0).name);
        assertEquals("Cyan!", inserted.get(1).name);

        List<ColorRecord> updated = updateColors(inserted);
        assertEquals("Yellow?", updated.get(0).name);
        assertEquals("Cyan?", updated.get(1).name);
    }

    private void doTestValidation() throws Exception
    {
        JSONHelper json = new JSONHelper();

        /* The following catch CommandExceptions in order to be able to access specific properties. */

        try
        {
            /* The following exception is logged to the server as it is meant to simulate an unexpected error in
             * script validation */
            log("** Test errors: throw Error()'");
            insertColors(Arrays.asList(new ColorRecord("ShouldError", "not a hex value")));
            fail("Should throw an exception");
        }
        catch (CommandException e)
        {
            assertTrue("Expected \"color value must start with '#'\", got: \"" + e.getMessage() + "\"",
                    e.getMessage().contains("color value must start with '#'"));
        }

        try
        {
            log("** Test errors: Field='message' and test extraContext is echoed back");
            insertColors(Arrays.asList(new ColorRecord("TestFieldErrorMessage", null)), Maps.of("A", "a", "B", 3));
            fail("Should throw an exception");
        }
        catch (CommandException e)
        {
            assertEquals("single message", e.getMessage());
            JSONObject properties = (JSONObject)e.getProperties();
            JSONObject expected = (JSONObject)JSONValue.parse("{" +
                "\"exception\":\"single message\"," +
                "\"extraContext\":{" +
                    "\"A\":\"a\"," +
                    "\"B\":3" +
                "}," +
                "\"errors\":[{" +
                    "\"errors\":[{" +
                        "\"id\":\"Hex\"," +
                        "\"field\":\"Hex\"," +
                        "\"message\":\"single message\"," +
                        "\"msg\":\"single message\"" +
                    "}]," +
                    "\"schemaName\":\"vehicle\"," +
                    "\"queryName\":\"Colors\"," +
                    "\"exception\":\"single message\"," +
                    "\"rowNumber\":1," +
                    "\"row\":{" +
                        "\"Name\":\"TestFieldErrorMessage!\"," +
                        "\"Hex\":null" +
                        "\"_rowNumber\":1" +
                    "}" +
                "}]" +
            "}");
            
            json.assertEquals("FAILED", expected, properties);
        }

        try
        {
            log("** Test errors: Field=[array of messages]");
            insertColors(Arrays.asList(new ColorRecord("TestFieldErrorArray", null)));
            fail("Should throw an exception");
        }
        catch (CommandException e)
        {
            assertEquals("one error message", e.getMessage());
            JSONObject properties = (JSONObject)e.getProperties();
            JSONObject expected = (JSONObject)JSONValue.parse("{" +
                "\"exception\":\"one error message\"," +
                "\"errors\":[{" +
                    "\"errors\":[" +
                        "{\"id\":\"Name\",\"field\":\"Name\",\"message\":\"one error message\",\"msg\":\"one error message\"}," +
                        "{\"id\":\"Name\",\"field\":\"Name\",\"message\":\"two error message!\",\"msg\":\"two error message!\"}," +
                        "{\"id\":\"Name\",\"field\":\"Name\",\"message\":\"ha ha ha!\",\"msg\":\"ha ha ha!\"}," +
                        "{\"id\":\"Hex\",\"field\":\"Hex\",\"message\":\"also an error here\",\"msg\":\"also an error here\"}" +
                    "]," +
                    "\"schemaName\":\"vehicle\"," +
                    "\"queryName\":\"Colors\"," +
                    "\"exception\":\"one error message\"," +
                    "\"rowNumber\":1," +
                    "\"row\":{" +
                        "\"Name\":\"TestFieldErrorArray!\"," +
                        "\"Hex\":null" +
                        "\"_rowNumber\":1" +
                    "}" +
                "}]" +
            "}");

            json.assertEquals("FAILED", expected, properties);
        }

        try
        {
            log("** Test errors: row level error");
            insertColors(Arrays.asList(new ColorRecord("TestRowError", null)));
            fail("Should throw an exception");
        }
        catch (CommandException e)
        {
            assertEquals("boring error message", e.getMessage());

            JSONObject properties = (JSONObject)e.getProperties();
            JSONObject expected = (JSONObject)JSONValue.parse("{" +
                "\"exception\":\"boring error message\"," +
                "\"errors\":[{" +
                    "\"errors\":[{" +
                        "\"message\":\"boring error message\"," +
                        "\"msg\":\"boring error message\"" +
                    "}]," +
                    "\"schemaName\":\"blarg\"," +
                    "\"queryName\":\"zorg\"," +
                    "\"exception\":\"boring error message\"," +
                    "\"rowNumber\":1000," +
                    "\"row\":{" +
                        "\"Name\":\"TestRowError!\"," +
                        "\"Hex\":null" +
                        "\"_rowNumber\":1" +
                    "}" +
                "}]" +
            "}");
            json.assertEquals("FAILED", expected, properties);
        }

        try
        {
            log("** Test errors: returning false");
            insertColors(Arrays.asList(new ColorRecord("TestReturnFalse", "")));
            fail("Should throw an exception");
        }
        catch (CommandException e)
        {
            assertEquals("beforeInsert validation failed", e.getMessage());

            JSONObject properties = (JSONObject)e.getProperties();
            JSONObject expected = (JSONObject)JSONValue.parse("{" +
                "\"exception\":\"beforeInsert validation failed\"," +
                "\"errorCount\":1," +
                "\"errors\":[{" +
                    "\"errors\":[{" +
                        "\"message\":\"beforeInsert validation failed\"," +
                        "\"msg\":\"beforeInsert validation failed\"" +
                    "}]," +
                    "\"schemaName\":\"vehicle\"," +
                    "\"queryName\":\"Colors\"," +
                    "\"exception\":\"beforeInsert validation failed\"," +
                    "\"rowNumber\":1," +
                    "\"row\":{" +
                        "\"Name\":\"TestReturnFalse\"," +
                        "\"Hex\":null" +
                        "\"_rowNumber\":1" +
                    "}" +
                "}]," +
                "\"success\":false," +
                "\"extraContext\":{}," +
            "}");
            json.assertEquals("FAILED", expected, properties);
        }

        try
        {
            log("** Test errors: error in complete");
            insertColors(Arrays.asList(
                    new ColorRecord("Yellow", "#f00"),
                    new ColorRecord("Cyan", "#0f0"),
                    new ColorRecord("TestErrorInComplete", "")
            ));
            fail("Should throw an exception");
        }
        catch (CommandException e)
        {
            assertEquals("TestErrorInComplete error global four!", e.getMessage());
            JSONObject properties = (JSONObject)e.getProperties();
            JSONObject expected = (JSONObject)JSONValue.parse("{" +
                "\"exception\":\"TestErrorInComplete error global four!\"," +
                "\"success\":false" +
                "\"errors\":[{" +
                    "\"errors\":[{" +
                        "\"id\":\"Name\"," +
                        "\"field\":\"Name\"," +
                        "\"message\":\"TestErrorInComplete error global four!\"," +
                        "\"msg\":\"TestErrorInComplete error global four!\"" +
                    "}]," +
                    "\"exception\":\"TestErrorInComplete error global four!\"," +
                    "\"rowNumber\":2," +
                    "\"row\":{" +
                        "\"a\":\"A\"," +
                        "\"b\":\"B\"" +
                    "}" +
                "}]" +
            "}");
            json.assertEquals("FAILED", expected, properties);
        }

        try
        {
            log("** Test errors: adding array of errors in complete");
            insertColors(Arrays.asList(new ColorRecord("TestFieldErrorArrayInComplete", "")));
            fail("Should throw an exception");
        }
        catch (CommandException e)
        {
            assertEquals("one error message", e.getMessage());
            JSONObject properties = (JSONObject)e.getProperties();
            JSONObject expected = (JSONObject)JSONValue.parse("{" +
                "\"exception\":\"one error message\"," +
                "\"success\":false" +
                "\"errors\":[{" +
                    "\"errors\":[{" +
                        "\"id\":\"Name\"," +
                        "\"field\":\"Name\"," +
                        "\"message\":\"one error message\"," +
                        "\"msg\":\"one error message\"" +
                    "},{" +
                        "\"id\":\"Name\"," +
                        "\"field\":\"Name\"," +
                        "\"message\":\"two error message\"," +
                        "\"msg\":\"two error message\"" +
                    "},{" +
                        "\"id\":\"Hex\"," +
                        "\"field\":\"Hex\"," +
                        "\"message\":\"three error message\"," +
                        "\"msg\":\"three error message\"" +
                    "}]," +
                    "\"exception\":\"one error message\"," +
                    "\"rowNumber\":0" +
                "},{" +
                   "\"errors\":[{" +
                       "\"id\":\"Hex\"," +
                       "\"field\":\"Hex\"," +
                       "\"message\":\"four error message\"," +
                       "\"msg\":\"four error message\"" +
                    "}]," +
                    "\"exception\":\"four error message\"," +
                    "\"rowNumber\":0" +
                "}]" +
            "}");
            json.assertEquals("FAILED", expected, properties);
        }

        try
        {
            log("** Test errors: script level variables, color regular expression");
            insertColors(Arrays.asList(new ColorRecord("ShouldError", "#still not a hex value")));
            fail("Should throw an exception");
        }
        catch (CommandException e)
        {
            assertEquals("color value must be of the form #abc or #aabbcc", e.getMessage());
        }

        try
        {
            log("** Test errors: updating hex value");
            ColorRecord yellow = selectColor("Yellow?").get(0);
            yellow.hex = "shouldn't happen";
            updateColors(Arrays.asList(yellow));
            fail("Should throw an exception");
        }
        catch (CommandException e)
        {
            assertEquals("once set, cannot be changed", e.getMessage());
        }
    }

    private List<ColorRecord> selectColor(String... names) throws Exception
    {
        log("** Selecting colors...");

        Connection cn = createDefaultConnection(true);
        SelectRowsCommand cmd = new SelectRowsCommand(VEHICLE_SCHEMA, "Colors");
        cmd.addFilter("Name", StringUtils.join(names, ";"), Filter.Operator.IN);
        SelectRowsResponse response = cmd.execute(cn, getProjectName());
        assertEquals("Expected to select " + names.length + " rows.", names.length, response.getRowCount().intValue());

        ArrayList<ColorRecord> results = new ArrayList<>();
        for (Map<String, Object> map : response.getRows())
            results.add(ColorRecord.fromMap(map));
        return results;
    }

    private List<ColorRecord> insertColors(List<ColorRecord> colors) throws Exception
    {
        return insertColors(colors, null);
    }

    private List<ColorRecord> insertColors(List<ColorRecord> colors, Map<String, Object> extraContext) throws Exception
    {
        ArrayList<Map<String, Object>> list = new ArrayList<>(colors.size());
        for (ColorRecord color : colors)
            list.add(color.toMap());

        log("** Inserting colors...");
        Connection cn = createDefaultConnection(true);
        InsertRowsCommand cmd = new InsertRowsCommand(VEHICLE_SCHEMA, "Colors");
        cmd.getRows().addAll(list);
        cmd.setExtraContext(extraContext);
        SaveRowsResponse response = cmd.execute(cn, getProjectName());
        assertEquals("Expected to insert " + colors.size() + " rows.", colors.size(), response.getRowsAffected().intValue());

        ArrayList<ColorRecord> results = new ArrayList<>();
        for (Map<String, Object> map : response.getRows())
            results.add(ColorRecord.fromMap(map));
        return results;
    }

    private List<ColorRecord> updateColors(List<ColorRecord> colors) throws Exception
    {
        return updateColors(colors, null);
    }

    private List<ColorRecord> updateColors(List<ColorRecord> colors, Map<String, Object> extraContext) throws Exception
    {
        ArrayList<Map<String, Object>> list = new ArrayList<>(colors.size());
        for (ColorRecord color : colors)
            list.add(color.toMap());

        log("** Updating colors...");
        Connection cn = createDefaultConnection(true);
        UpdateRowsCommand cmd = new UpdateRowsCommand(VEHICLE_SCHEMA, "Colors");
        cmd.getRows().addAll(list);
        cmd.setExtraContext(extraContext);
        SaveRowsResponse response = cmd.execute(cn, getProjectName());
        assertEquals("Expected to update " + colors.size() + " rows.", colors.size(), response.getRowsAffected().intValue());

        ArrayList<ColorRecord> results = new ArrayList<>();
        for (Map<String, Object> map : response.getRows())
            results.add(ColorRecord.fromMap(map));
        return results;
    }

    @Override public BrowserType bestBrowser()
    {
        return BrowserType.CHROME;
    }

    @Override
    public List<String> getAssociatedModules()
    {
        return Arrays.asList("simpletest");
    }

    @Override
    protected @Nullable String getProjectName()
    {
        return getClass().getSimpleName() + " Project";
    }
}
