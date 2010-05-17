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
import org.labkey.remoteapi.Connection;
import org.labkey.remoteapi.query.*;
import org.labkey.test.BaseSeleniumWebTest;
import org.labkey.test.util.Maps;
import org.labkey.test.util.PasswordUtil;

import java.util.*;

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
            log("** Try inserting Glucose");
            insertColors(Arrays.asList(
                    new ColorRecord("Red", "#f00"),
                    new ColorRecord("Blue", "#0f0"),
                    new ColorRecord("Glucose", "")
            ));
            fail("Should throw an exception for Glucose");
        }
        catch (Exception e)
        {
            assertEquals("Row 2 has error: Name: Glucose isn't the name of a color!", e.getMessage());
        }

        try
        {
            log("** Try inserting bad hex value");
            insertColors(Arrays.asList(new ColorRecord("ShouldError", "not a hex value")));
            fail("Should throw an exception for invalid hex values");
        }
        catch (Exception e)
        {
            assertTrue("Expected \"color value must start with '#'\", got: \"" + e.getMessage() + "\"",
                    e.getMessage().contains("color value must start with '#'"));
        }

        try
        {
            log("** Try inserting bad hex value again");
            insertColors(Arrays.asList(new ColorRecord("ShouldError", "#still not a hex value")));
            fail("Should throw an exception for invalid hex values");
        }
        catch (Exception e)
        {
            assertEquals("Hex: color value must be of the form #abc or #aabbcc", e.getMessage());
        }

        try
        {
            log("** Try inserting Muave");
            insertColors(Arrays.asList(new ColorRecord("Muave", "")));
            fail("Should throw an exception for invalid hex values");
        }
        catch (Exception e)
        {
            assertEquals("beforeInsert validation failed", e.getMessage());
        }

        try
        {
            log("** try updating hex value");
            ColorRecord red = selectColor("Red?").get(0);
            red.hex = "shouldn't happen";
            updateColors(Arrays.asList(red));
            fail("Should throw an exception for trying to update hex");
        }
        catch (Exception e)
        {
            assertEquals("Hex: once set, cannot be changed", e.getMessage());
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
