/*
 * Copyright (c) 2010 LabKey Corporation
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

import org.apache.commons.lang.StringUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.labkey.remoteapi.CommandException;
import org.labkey.remoteapi.Connection;
import org.labkey.remoteapi.query.*;
import org.labkey.test.util.Maps;
import org.labkey.test.util.PasswordUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Test JavaScript validation in file-based modules.
 */
public class ScriptValidationTest extends SimpleModuleTest
{
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
            return Maps.<String, Object>of("Name", name, "Hex", hex);
        }

        public static ColorRecord fromMap(Map<String, Object> map)
        {
            return new ColorRecord((String)map.get("Name"), (String)map.get("Hex"));
        }
    }

    @Override
    protected void doTestSteps() throws Exception
    {
        assertModuleDeployed(MODULE_NAME);
        createProject(getProjectName());
        enableModule(getProjectName(), MODULE_NAME);
        enableModule(getProjectName(), "Query");

        clickLinkWithText(getProjectName());
        doTestTransformation();
        doTestValidation();
    }

    private void doTestTransformation() throws Exception
    {
        List<ColorRecord> inserted = insertColors(Arrays.asList(
                new ColorRecord("Red", "#f00"),
                new ColorRecord("Blue", "#0f0")
        ));
        assertEquals("Red!", inserted.get(0).name);
        assertEquals("Blue!", inserted.get(1).name);

        List<ColorRecord> updated = updateColors(inserted);
        assertEquals("Red?", updated.get(0).name);
        assertEquals("Blue?", updated.get(1).name);
    }

    private void doTestValidation() throws Exception
    {
        try
        {
            log("** Test errors: throw Error()'");
            insertColors(Arrays.asList(new ColorRecord("ShouldError", "not a hex value")));
            fail("Should throw an exception");
        }
        catch (Exception e)
        {
            assertTrue("Expected \"color value must start with '#'\", got: \"" + e.getMessage() + "\"",
                    e.getMessage().contains("color value must start with '#'"));
        }

        try
        {
            log("** Test errors: Field='message'");
            insertColors(Arrays.asList(new ColorRecord("TestFieldErrorMessage", null)));
            fail("Should throw an exception");
        }
        catch (CommandException e)
        {
            assertEquals("single message", e.getMessage());

            JSONObject properties = (JSONObject)e.getProperties();
            JSONArray errors = (JSONArray)properties.get("errors");
            assertEquals(1, errors.size());

            JSONObject error = (JSONObject)errors.get(0);
            assertEquals("Hex", error.get("field"));
            assertEquals("single message", error.get("message"));
        }

        try
        {
            log("** Test errors: Field=[array of messages]");
            insertColors(Arrays.asList(new ColorRecord("TestFieldErrorArray", null)));
            fail("Should throw an exception");
        }
        catch (CommandException e)
        {
            assertEquals("one error message; two error message!; ha ha ha!; also an error here", e.getMessage());

            JSONObject properties = (JSONObject)e.getProperties();
            assertEquals(0, ((Number)properties.get("rowNumber")).intValue());
            
            JSONArray errors = (JSONArray)properties.get("errors");
            assertEquals(4, errors.size());

            JSONObject error0 = (JSONObject)errors.get(0);
            assertEquals("Name", error0.get("field"));
            assertEquals("one error message", error0.get("message"));

            JSONObject error1 = (JSONObject)errors.get(1);
            assertEquals("Name", error1.get("field"));
            assertEquals("two error message!", error1.get("message"));

            JSONObject error2 = (JSONObject)errors.get(2);
            assertEquals("Name", error2.get("field"));
            assertEquals("ha ha ha!", error2.get("message"));

            JSONObject error3 = (JSONObject)errors.get(3);
            assertEquals("Hex", error3.get("field"));
            assertEquals("also an error here", error3.get("message"));
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
            assertEquals(0, ((Number)properties.get("rowNumber")).intValue());

            JSONArray errors = (JSONArray)properties.get("errors");
            assertEquals(1, errors.size());

            JSONObject error0 = (JSONObject)errors.get(0);
//            assertNull(error0.get("field"));
            assertEquals("boring error message", error0.get("message"));
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
            assertEquals(0, ((Number)properties.get("rowNumber")).intValue());

            JSONArray errors = (JSONArray)properties.get("errors");
            assertEquals(1, errors.size());

            JSONObject error0 = (JSONObject)errors.get(0);
            assertFalse(error0.containsKey("field"));
            assertEquals("beforeInsert validation failed", error0.get("message"));
        }

        try
        {
            log("** Test errors: error in complete");
            insertColors(Arrays.asList(
                    new ColorRecord("Red", "#f00"),
                    new ColorRecord("Blue", "#0f0"),
                    new ColorRecord("TestErrorInComplete", "")
            ));
            fail("Should throw an exception");
        }
        catch (CommandException e)
        {
            assertEquals("Row 2: TestErrorInComplete error one; TestErrorInComplete error two\n" +
                    "Row 2: TestErrorInComplete error three!", e.getMessage());

            JSONObject properties = (JSONObject)e.getProperties();

            JSONArray errors = (JSONArray)properties.get("errors");
            assertEquals(2, errors.size());

            JSONObject error0 = (JSONObject)errors.get(0);
            assertFalse(error0.containsKey("field"));
            assertFalse(error0.containsKey("message"));
            assertEquals(2, ((Number)error0.get("rowNumber")).intValue());

            JSONArray error0errors = (JSONArray)error0.get("errors");
            assertEquals(2, error0errors.size());
            assertEquals("Hex", ((JSONObject)error0errors.get(0)).get("field"));
            assertEquals("TestErrorInComplete error one", ((JSONObject)error0errors.get(0)).get("message"));
            assertEquals("Hex", ((JSONObject)error0errors.get(1)).get("field"));
            assertEquals("TestErrorInComplete error two", ((JSONObject)error0errors.get(1)).get("message"));


            JSONObject error1 = (JSONObject)errors.get(1);
            JSONArray error1errors = (JSONArray)error1.get("errors");
            assertEquals(2, ((Number)error1.get("rowNumber")).intValue());
            assertEquals("Name", ((JSONObject)error1errors.get(0)).get("field"));
            assertEquals("TestErrorInComplete error three!", ((JSONObject)error1errors.get(0)).get("message"));
        }

        try
        {
            log("** Test errors: adding array of errors in complete");
            insertColors(Arrays.asList(new ColorRecord("TestFieldErrorArrayInComplete", "")));
            fail("Should throw an exception");
        }
        catch (CommandException e)
        {
            assertEquals("one error message; two error message", e.getMessage());
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
            ColorRecord red = selectColor("Red?").get(0);
            red.hex = "shouldn't happen";
            updateColors(Arrays.asList(red));
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

        Connection cn = new Connection(getBaseURL(), PasswordUtil.getUsername(), PasswordUtil.getPassword());
        SelectRowsCommand cmd = new SelectRowsCommand(VEHICLE_SCHEMA, "Colors");
        cmd.addFilter("Name", StringUtils.join(names, ";"), Filter.Operator.IN);
        SelectRowsResponse response = cmd.execute(cn, getProjectName());
        assertEquals("Expected to select " + names.length + " rows.", names.length, response.getRowCount().intValue());

        ArrayList<ColorRecord> results = new ArrayList<ColorRecord>();
        for (Map<String, Object> map : response.getRows())
            results.add(ColorRecord.fromMap(map));
        return results;
    }

    private List<ColorRecord> insertColors(List<ColorRecord> colors) throws Exception
    {
        ArrayList<Map<String, Object>> list = new ArrayList<Map<String, Object>>(colors.size());
        for (ColorRecord color : colors)
            list.add(color.toMap());

        log("** Inserting colors...");
        Connection cn = new Connection(getBaseURL(), PasswordUtil.getUsername(), PasswordUtil.getPassword());
        InsertRowsCommand cmd = new InsertRowsCommand(VEHICLE_SCHEMA, "Colors");
        cmd.getRows().addAll(list);
        SaveRowsResponse response = cmd.execute(cn, getProjectName());
        assertEquals("Expected to insert " + colors.size() + " rows.", colors.size(), response.getRowsAffected().intValue());

        ArrayList<ColorRecord> results = new ArrayList<ColorRecord>();
        for (Map<String, Object> map : response.getRows())
            results.add(ColorRecord.fromMap(map));
        return results;
    }

    private List<ColorRecord> updateColors(List<ColorRecord> colors) throws Exception
    {
        ArrayList<Map<String, Object>> list = new ArrayList<Map<String, Object>>(colors.size());
        for (ColorRecord color : colors)
            list.add(color.toMap());

        log("** Updating colors...");
        Connection cn = new Connection(getBaseURL(), PasswordUtil.getUsername(), PasswordUtil.getPassword());
        UpdateRowsCommand cmd = new UpdateRowsCommand(VEHICLE_SCHEMA, "Colors");
        cmd.getRows().addAll(list);
        SaveRowsResponse response = cmd.execute(cn, getProjectName());
        assertEquals("Expected to update " + colors.size() + " rows.", colors.size(), response.getRowsAffected().intValue());

        ArrayList<ColorRecord> results = new ArrayList<ColorRecord>();
        for (Map<String, Object> map : response.getRows())
            results.add(ColorRecord.fromMap(map));
        return results;
    }
}
