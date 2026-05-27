/*
 * Copyright (c) 2025-2026 LabKey Corporation
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
package org.labkey.test.tests.upgrade;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.remoteapi.CommandException;
import org.labkey.remoteapi.Connection;
import org.labkey.remoteapi.query.SelectRowsCommand;
import org.labkey.test.params.FieldInfo;
import org.labkey.test.params.experiment.SampleTypeDefinition;
import org.labkey.test.util.TestUser;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertFalse;
import static org.labkey.test.util.PermissionsHelper.READER_ROLE;

@Category({})
public class BasicUpgradeTest extends BaseUpgradeTest
{
    private static final TestUser USER = new TestUser("basic_upgrade_reader@basicupgradetest.test");
    private static final String SAMPLE_TYPE = "UpgradeTestSamples";

    private static final FieldInfo STR_COL = new FieldInfo("String Col1");

    @Override
    protected void doCleanup(boolean afterTest)
    {
        _containerHelper.deleteProject(getProjectName(), afterTest);
        _userHelper.deleteUsers(afterTest, USER);
    }

    @Override
    protected void doSetup() throws Exception
    {
        _containerHelper.createProject(getProjectName(), null);
        USER.create(this);
        USER.setInitialPassword();
        USER.addPermission(READER_ROLE, getProjectName());

        new SampleTypeDefinition(SAMPLE_TYPE)
            .setFields(List.of(STR_COL.getFieldDefinition()))
            .create(createDefaultConnection(), getProjectName())
            .insertRows(createDefaultConnection(), List.of(Map.of(
                "Name", "S-1",
                STR_COL.getName(), "Test String Value"
            )) );
    }

    @Before
    public void preTest()
    {
        USER.load(this);
    }

    @Test
    public void testSampleRowsExist() throws Exception
    {
        // Use primary user to verify data
        queryData(createDefaultConnection());
    }

    @Test
    public void testUserPassword() throws Exception
    {
        // Use password authentication for USER
        queryData(USER.getUserConnection());
    }

    @Test
    public void testUserPermissions() throws Exception
    {
        // Impersonate to test USER's permission level
        queryData(createDefaultConnection().impersonate(USER.getEmail()));
    }

    private void queryData(Connection connection) throws IOException, CommandException
    {
        List<Map<String, Object>> rows = new SelectRowsCommand("samples", SAMPLE_TYPE)
            .execute(connection, getProjectName())
            .getRows();
        assertFalse("Expected at least one row", rows.isEmpty());
    }

    @Override
    protected String getProjectName()
    {
        return getClass().getSimpleName() + " Project";
    }
}
