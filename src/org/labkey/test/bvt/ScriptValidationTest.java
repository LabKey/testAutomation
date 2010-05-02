package org.labkey.test.bvt;

import org.labkey.remoteapi.Connection;
import org.labkey.remoteapi.query.InsertRowsCommand;
import org.labkey.remoteapi.query.SaveRowsResponse;
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
//        doTestValidation();
    }

    private void doTestTransformation() throws Exception
    {
        List<ColorRecord> inserted = insertColors(Arrays.asList(
                new ColorRecord("Red", "#f00"),
                new ColorRecord("Blue", "#0f0")
        ));

        assertEquals("Red!", inserted.get(0).name);
        assertEquals("Blue!", inserted.get(1).name);
    }

    private void doTestValidation() throws Exception
    {
        try
        {
            insertColors(Arrays.asList(
                    new ColorRecord("ShouldError", "not a hex value")
            ));
            fail("Should throw an exception for invalid hex values");
        }
        catch (Exception e)
        {
            assertTrue(e.getMessage().contains("Hex color value must start with '#'"));
        }

        try
        {
            insertColors(Arrays.asList(
                    new ColorRecord("ShouldError", "#still not a hex value")
            ));
            fail("Should throw an exception for invalid hex values");
        }
        catch (Exception e)
        {
            assertTrue(e.getMessage().contains("Hex color value must be of the form #abc or #aabbcc"));
        }
    }

    private List<ColorRecord> insertColors(List<ColorRecord> colors) throws Exception
    {
        ArrayList<Map<String, Object>> list = new ArrayList<Map<String, Object>>(colors.size());
        for (ColorRecord color : colors)
            list.add(color.toMap());

        log("** Inserting colors...");
        Connection cn = new Connection(getBaseURL(), PasswordUtil.getUsername(), PasswordUtil.getPassword());
        InsertRowsCommand insertCmd = new InsertRowsCommand(VEHICLE_SCHEMA, "Colors");
        insertCmd.getRows().addAll(list);
        SaveRowsResponse insertResp = insertCmd.execute(cn, getProjectName());
        assertEquals("Expected to insert " + colors.size() + " rows.", colors.size(), insertResp.getRowsAffected().intValue());

        ArrayList<ColorRecord> results = new ArrayList<ColorRecord>();
        for (Map<String, Object> map : insertResp.getRows())
            results.add(ColorRecord.fromMap(map));
        return results;
    }
}
