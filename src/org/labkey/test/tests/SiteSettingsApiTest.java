/*
 * Copyright (c) 2026 LabKey Corporation
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

import org.jetbrains.annotations.Nullable;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.remoteapi.CommandException;
import org.labkey.remoteapi.Connection;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.WebTestHelper;
import org.labkey.test.categories.Daily;
import org.labkey.test.pages.core.admin.CustomizeSitePage;
import org.labkey.test.util.ApiPermissionsHelper;
import org.labkey.test.util.AuditLogHelper;
import org.labkey.test.util.PermissionsHelper;
import org.labkey.test.util.SiteSettingsHelper;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.labkey.test.util.PermissionsHelper.TROUBLESHOOTER_ROLE;

@Category({Daily.class})
@BaseWebDriverTest.ClassTimeout(minutes = 4)
public class SiteSettingsApiTest extends BaseWebDriverTest
{
    private static final String TROUBLESHOOTER_USER = "troubleshooter@sitesettingsapi.test";
    private static final String NON_ADMIN_USER = "nonadmin@sitesettingsapi.test";
    private static final String SCRIPT_EXECUTION_TIMEOUT = "scriptExecutionTimeout";
    private static final String READ_ONLY_HTTP_REQUEST_TIMEOUT = "readOnlyHttpRequestTimeout";
    private static final String AUDIT_TABLE = "AppPropsEvent";

    // Static so a setting changed in one test method is restored by doCleanup on a later instance
    private static final SiteSettingsHelper SITE_SETTINGS = new SiteSettingsHelper();

    @Override
    protected void doCleanup(boolean afterTest)
    {
        SITE_SETTINGS.restoreSettings();
        _userHelper.deleteUsers(false, TROUBLESHOOTER_USER, NON_ADMIN_USER);
    }

    @BeforeClass
    public static void setupProject()
    {
        SiteSettingsApiTest init = getCurrentTest();
        init.doSetup();
    }

    private void doSetup()
    {
        _userHelper.createUser(TROUBLESHOOTER_USER);
        new ApiPermissionsHelper(this).addMemberToRole(TROUBLESHOOTER_USER, TROUBLESHOOTER_ROLE, PermissionsHelper.MemberType.user, "/");
        _userHelper.createUser(NON_ADMIN_USER);
    }

    @Test
    public void testGetSettings()
    {
        Map<String, Object> settings = SITE_SETTINGS.getSettings();
        assertThat(settings).as("Site settings")
            .containsKeys("baseServerURL", "exceptionReportingLevel", "sslRequired", "pipelineToolsDirectory", SCRIPT_EXECUTION_TIMEOUT);
        assertThat(settings.get(SCRIPT_EXECUTION_TIMEOUT)).as(SCRIPT_EXECUTION_TIMEOUT).isInstanceOf(Integer.class);
        assertThat(settings.get("sslRequired")).as("sslRequired").isInstanceOf(Boolean.class);
    }

    @Test
    public void testUpdateAndRestore()
    {
        int original = (Integer) SITE_SETTINGS.getSetting(SCRIPT_EXECUTION_TIMEOUT);
        Integer auditRowId = new AuditLogHelper(this).getLatestAuditRowId(AUDIT_TABLE);

        SITE_SETTINGS.setSetting(SCRIPT_EXECUTION_TIMEOUT, original + 5);
        assertEquals("Updated value", original + 5, SITE_SETTINGS.getSetting(SCRIPT_EXECUTION_TIMEOUT));
        assertTrue("Update wasn't audited", new AuditLogHelper(this).getLatestAuditRowId(AUDIT_TABLE) > auditRowId);

        SITE_SETTINGS.restoreSettings();
        assertEquals("Restored value", original, SITE_SETTINGS.getSetting(SCRIPT_EXECUTION_TIMEOUT));
    }

    @Test
    public void testRejectedUpdateSavesNothing()
    {
        Map<String, Object> original = SITE_SETTINGS.getSettings();
        int newTimeout = (Integer) original.get(SCRIPT_EXECUTION_TIMEOUT) + 5;

        assertUpdateRejected(Map.of(SCRIPT_EXECUTION_TIMEOUT, newTimeout, "noSuchSetting", 1), "Unknown site setting: noSuchSetting");
        assertUpdateRejected(Map.of(SCRIPT_EXECUTION_TIMEOUT, newTimeout, "navAccessOpen", "yes"), "Expected true or false");
        assertUpdateRejected(Map.of(SCRIPT_EXECUTION_TIMEOUT, newTimeout, "showRibbonMessage", true, "ribbonMessage", ""),
            "Cannot enable the ribbon message without providing a message to show");

        assertEquals("A rejected update changed settings", original, SITE_SETTINGS.getSettings());
    }

    private void assertUpdateRejected(Map<String, ?> settings, String expectedError)
    {
        CommandException e = assertThrows(CommandException.class, () -> SITE_SETTINGS.updateSettings(settings));
        assertEquals("Status for rejected update", 400, e.getStatusCode());
        assertThat(e.getMessage()).as("Error for rejected update").contains(expectedError);
    }

    @Test
    public void testPermissions() throws Exception
    {
        SiteSettingsHelper troubleshooter = new SiteSettingsHelper(impersonating(TROUBLESHOOTER_USER));
        assertThat(troubleshooter.getSettings()).as("Troubleshooter's view of site settings").containsKey(SCRIPT_EXECUTION_TIMEOUT);
        assertForbidden(() -> troubleshooter.updateSettings(Map.of(SCRIPT_EXECUTION_TIMEOUT, 1)));

        SiteSettingsHelper nonAdmin = new SiteSettingsHelper(impersonating(NON_ADMIN_USER));
        assertForbidden(nonAdmin::getSettings);
        assertForbidden(() -> nonAdmin.updateSettings(Map.of(SCRIPT_EXECUTION_TIMEOUT, 1)));
    }

    private Supplier<Connection> impersonating(String email) throws IOException, CommandException
    {
        Connection connection = WebTestHelper.getRemoteApiConnection(false);
        connection.impersonate(email);
        return () -> connection;
    }

    private void assertForbidden(ThrowingRunnable call)
    {
        Throwable thrown = assertThrows(Exception.class, call::run);
        CommandException e = thrown instanceof CommandException ce ? ce : (CommandException) thrown.getCause();
        assertEquals("Status for unauthorized request", 403, e.getStatusCode());
    }

    @FunctionalInterface
    private interface ThrowingRunnable
    {
        void run() throws Exception;
    }

    @Test
    public void testPageSaveUsesApi()
    {
        int original = (Integer) SITE_SETTINGS.getSetting(READ_ONLY_HTTP_REQUEST_TIMEOUT);
        // Remembers the original so doCleanup restores it
        SITE_SETTINGS.setSetting(READ_ONLY_HTTP_REQUEST_TIMEOUT, original);

        CustomizeSitePage.beginAt(this)
            .setReadOnlyHttpRequestTimeout(String.valueOf(original + 11))
            .save();
        assertEquals("Value saved by the page", original + 11, SITE_SETTINGS.getSetting(READ_ONLY_HTTP_REQUEST_TIMEOUT));

        List<String> errors = CustomizeSitePage.beginAt(this)
            .setReadOnlyHttpRequestTimeout("soon")
            .saveExpectingErrors();
        assertThat(errors).as("Page validation errors").singleElement().asString().contains("Expected an integer");
        assertEquals("Rejected page save changed the value", original + 11, SITE_SETTINGS.getSetting(READ_ONLY_HTTP_REQUEST_TIMEOUT));
    }

    @Override
    protected @Nullable String getProjectName()
    {
        return null;
    }

    @Override
    public List<String> getAssociatedModules()
    {
        return List.of("core");
    }
}
