/*
 * Copyright (c) 2011-2019 LabKey Corporation
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
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.remoteapi.CommandException;
import org.labkey.remoteapi.CommandResponse;
import org.labkey.remoteapi.Connection;
import org.labkey.remoteapi.PostCommand;
import org.labkey.remoteapi.query.Filter;
import org.labkey.remoteapi.query.InsertRowsCommand;
import org.labkey.remoteapi.query.SaveRowsResponse;
import org.labkey.remoteapi.query.SelectRowsCommand;
import org.labkey.remoteapi.query.SelectRowsResponse;
import org.labkey.remoteapi.query.Sort;
import org.labkey.remoteapi.query.UpdateRowsCommand;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.categories.Daily;
import org.labkey.test.categories.Data;
import org.labkey.test.params.FieldDefinition;
import org.labkey.test.params.list.IntListDefinition;
import org.labkey.test.util.JSONHelper;
import org.labkey.test.util.LogMethod;
import org.labkey.test.util.Maps;
import org.labkey.test.util.query.QueryApiHelper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Test JavaScript validation in file-based modules.
 */
@Category({Daily.class, Data.class})
@BaseWebDriverTest.ClassTimeout(minutes = 5)
public class ScriptValidationTest extends BaseWebDriverTest
{
    public static final String MODULE_NAME = "simpletest";
    public static final String VEHICLE_SCHEMA = "vehicle";

    public static final String VEHICLES_TABLE = "vehicles";

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
    public static void initTest() throws Exception
    {
        ScriptValidationTest init = (ScriptValidationTest) getCurrentTest();
        init.doSetup();
    }

    @LogMethod
    protected void doSetup() throws Exception
    {
        _containerHelper.createProject(getProjectName(), null);
        _containerHelper.enableModule(getProjectName(), MODULE_NAME);
        new QueryApiHelper(createDefaultConnection(), getProjectName(), VEHICLE_SCHEMA, VEHICLES_TABLE).truncateTable();
        new QueryApiHelper(createDefaultConnection(), getProjectName(), VEHICLE_SCHEMA, "Colors").truncateTable();
    }

    @Test
    public void testSteps() throws Exception
    {
        clickProject(getProjectName());
        doTestTransformation();
        doTestValidation();
        doTestCrossFolderSaveRows();

        log("Create list to prevent query validation failure");
        new IntListDefinition("People", "Key")
                .setFields(List.of(
                        new FieldDefinition("Name", FieldDefinition.ColumnType.String),
                        new FieldDefinition("Age", FieldDefinition.ColumnType.Integer),
                        new FieldDefinition("Crazy", FieldDefinition.ColumnType.Boolean)))
                .create(createDefaultConnection(), getProjectName());
    }

    public void doTestCrossFolderSaveRows() throws Exception
    {
        Connection cn = createDefaultConnection();

        List<ColorRecord> colors = insertColors(Arrays.asList(
                new ColorRecord("Yellow2", "#f00")
        ));

        // Create manufacturers:
        ArrayList<Map<String, Object>> list1 = new ArrayList<>();
        list1.add(Map.of("Name", "Manufacturer1"));
        InsertRowsCommand cmd1 = new InsertRowsCommand(VEHICLE_SCHEMA, "Manufacturers");
        cmd1.getRows().addAll(list1);
        Object manufacturerId = cmd1.execute(cn, getProjectName()).getRows().get(0).get("rowid");

        // Create model:
        ArrayList<Map<String, Object>> list2 = new ArrayList<>();
        list2.add(Map.of("ManufacturerId", manufacturerId, "Name", "Model1"));
        InsertRowsCommand cmd2 = new InsertRowsCommand(VEHICLE_SCHEMA, "Models");
        cmd2.getRows().addAll(list2);
        Object modelId = cmd2.execute(cn, getProjectName()).getRows().get(0).get("RowId");

        PostCommand<CommandResponse> saveRowsCommand = prepareSaveRowsCommand("insertWithKeys", getProjectName(), VEHICLE_SCHEMA, VEHICLES_TABLE, "RowId",
                new String[]{"ModelId", "Color", "ModelYear", "Milage", "LastService"},
                new Object[][]{new Object[]{modelId, colors.get(0).name, 2000, 1234, new Date()}}, null);
        CommandResponse response = saveRowsCommand.execute(cn, "/home");
        Map<String, Object> row = (Map)((Map)((List)((Map)((List)response.getParsedData().get("result")).get(0)).get("rows")).get(0)).get("values");
        Object vehicleRowId = row.get("rowId");
        assertNotNull("RowId not returned for vehicles insert", vehicleRowId);

        SelectRowsCommand src = new SelectRowsCommand(VEHICLE_SCHEMA, VEHICLES_TABLE);
        src.setColumns(Arrays.asList("Container", "TriggerScriptContainer", "RowId", "ModelId", "Milage"));
        src.setSorts(Arrays.asList(new Sort("RowId", Sort.Direction.DESCENDING)));
        SelectRowsResponse sr2 = src.execute(cn, getProjectName());
        assertEquals("Incorrect model", modelId, sr2.getRows().get(0).get("ModelId"));
        assertEquals("Incorrect Milage", 1234, sr2.getRows().get(0).get("Milage"));
        assertEquals("Incorrect RowId for First Record", vehicleRowId, sr2.getRows().get(0).get("rowId"));

        // This should be true for all rows, including the one we just added:
        sr2.getRows().forEach(r -> {
            assertEquals("Containers should match, rowId: " + r.get("RowId"), r.get("Container"), r.get("TriggerScriptContainer"));
        });

    }

    private PostCommand<CommandResponse> prepareSaveRowsCommand(String command, String containerPath, String schema, String queryName, String pkName, String[] fieldNames, Object[][] rows, @Nullable Object[][] oldKeys)
    {
        PostCommand<CommandResponse> postCommand = new PostCommand<>("query", "saveRows");

        JSONObject commandJson = new JSONObject();
        commandJson.put("containerPath", containerPath);
        commandJson.put("schemaName", schema);
        commandJson.put("queryName", queryName);
        commandJson.put("command", command);
        JSONArray jsonRows = new JSONArray();
        int idx = 0;
        for (Object[] row : rows)
        {
            JSONObject oldKeyMap = new JSONObject();
            JSONObject values = new JSONObject();

            int position = 0;
            for (String name : fieldNames)
            {
                Object v = row[position];
                values.put(name, v);
                if (pkName.equals(name))
                    oldKeyMap.put(name, v);

                position++;
            }

            if (oldKeys != null && oldKeys.length > idx)
            {
                JSONObject obj = new JSONObject();
                int j = 0;
                for (String field : fieldNames)
                {
                    obj.put(field, oldKeys[idx][j]);
                    j++;
                }
                oldKeyMap = obj;
            }

            JSONObject ro = new JSONObject();
            ro.put("oldKeys", oldKeyMap);
            ro.put("values", values);
            jsonRows.put(ro);
        }
        commandJson.put("rows", jsonRows);

        JSONObject commands = new JSONObject();
        commands.put("commands", Collections.singletonList(commandJson));
        postCommand.setJsonObject(commands);
        return postCommand;
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
            JSONObject expected = new JSONObject("""
                    {
                        "exception":"single message",
                        "extraContext":{
                            "A":"a",
                            "B":3
                        },
                        "errors":[{
                            "errors":[{
                                "id":"Hex",
                                "field":"Hex",
                                "message":"single message",
                                "msg":"single message"
                            }],
                            "schemaName":"vehicle",
                            "queryName":"Colors",
                            "exception":"single message",
                            "rowNumber":1,
                            "row":{
                                "Name":"TestFieldErrorMessage!",
                                "Hex":null,
                                "_rowNumber":1
                            }
                        }]
                    }
                    """);
            
            json.assertEquals("FAILED", expected, new JSONObject(e.getProperties()));
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
            JSONObject expected = new JSONObject("""
                    {
                        "exception":"one error message",
                        "errors":[{
                            "errors":[
                                {"id":"Name","field":"Name","message":"one error message","msg":"one error message"},
                                {"id":"Name","field":"Name","message":"two error message!","msg":"two error message!"},
                                {"id":"Name","field":"Name","message":"ha ha ha!","msg":"ha ha ha!"},
                                {"id":"Hex","field":"Hex","message":"also an error here","msg":"also an error here"}
                            ],
                            "schemaName":"vehicle",
                            "queryName":"Colors",
                            "exception":"one error message",
                            "rowNumber":1,
                            "row":{
                                "Name":"TestFieldErrorArray!",
                                "Hex":null,
                                "_rowNumber":1
                            }
                        }]
                    }
                    """);

            json.assertEquals("FAILED", expected, new JSONObject(e.getProperties()));
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

            JSONObject expected = new JSONObject("""
                    {
                        "exception":"boring error message",
                        "errors":[{
                            "errors":[{
                                "message":"boring error message",
                                "msg":"boring error message"
                            }],
                            "schemaName":"blarg",
                            "queryName":"zorg",
                            "exception":"boring error message",
                            "rowNumber":1000,
                            "row":{
                                "Name":"TestRowError!",
                                "Hex":null,
                                "_rowNumber":1
                            }
                        }]
                    }
                    """);
            json.assertEquals("FAILED", expected, new JSONObject(e.getProperties()));
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

            JSONObject expected = new JSONObject("""
                    {
                        "exception":"beforeInsert validation failed",
                        "errorCount":1,
                        "errors":[{
                            "errors":[{
                                "message":"beforeInsert validation failed",
                                "msg":"beforeInsert validation failed"
                            }],
                            "schemaName":"vehicle",
                            "queryName":"Colors",
                            "exception":"beforeInsert validation failed",
                            "rowNumber":1,
                            "row":{
                                "Name":"TestReturnFalse",
                                "Hex":null,
                                "_rowNumber":1
                            }
                        }],
                        "success":false,
                        "extraContext":{},
                    }
                    """);
            json.assertEquals("FAILED", expected, new JSONObject(e.getProperties()));
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
            JSONObject expected = new JSONObject("""
                    {
                        "exception":"TestErrorInComplete error global four!",
                        "success":false,
                        "errors":[{
                            "errors":[{
                                "id":"Name",
                                "field":"Name",
                                "message":"TestErrorInComplete error global four!",
                                "msg":"TestErrorInComplete error global four!"
                            }],
                            "exception":"TestErrorInComplete error global four!",
                            "rowNumber":2,
                            "row":{
                                "a":"A",
                                "b":"B"
                            }
                        }]
                    }
                    """);
            json.assertEquals("FAILED", expected, new JSONObject(e.getProperties()));
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
            JSONObject expected = new JSONObject("""
                    {
                        "exception":"one error message",
                        "success":false,
                        "errors":[{
                            "errors":[{
                                "id":"Name",
                                "field":"Name",
                                "message":"one error message",
                                "msg":"one error message"
                            },{
                                "id":"Name",
                                "field":"Name",
                                "message":"two error message",
                                "msg":"two error message"
                            },{
                                "id":"Hex",
                                "field":"Hex",
                                "message":"three error message",
                                "msg":"three error message"
                            }],
                            "exception":"one error message",
                            "rowNumber":0
                        },{
                            "errors":[{
                                "id":"Hex",
                                "field":"Hex",
                                "message":"four error message",
                                "msg":"four error message"
                            }],
                            "exception":"four error message",
                            "rowNumber":0
                        }]
                    }
                    """);
            json.assertEquals("FAILED", expected, new JSONObject(e.getProperties()));
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

        Connection cn = createDefaultConnection();
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
        Connection cn = createDefaultConnection();
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
        Connection cn = createDefaultConnection();
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
