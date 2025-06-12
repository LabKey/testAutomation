package org.labkey.test.tests.core.admin;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hc.core5.http.HttpStatus;
import org.assertj.core.api.Assertions;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.remoteapi.CommandException;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.categories.Daily;
import org.labkey.test.components.html.SelectWrapper;
import org.labkey.test.pages.admin.ExternalSourcesPage;
import org.labkey.test.pages.admin.ExternalSourcesPage.Directive;
import org.labkey.test.pages.core.admin.ShowAdminPage;
import org.labkey.test.util.ApiPermissionsHelper;
import org.labkey.test.util.core.admin.CspConfigHelper;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.labkey.test.util.PermissionsHelper.MemberType.user;

@Category({Daily.class})
public class CspResourceHostsTest extends BaseWebDriverTest
{
    private static final String APP_ADMIN = "csp_app_admin@cspresourcehoststest.test";
    private static final String TROUBLESHOOTER = "csp_troubleshooter@cspresourcehoststest.test";
    private static final Log log = LogFactory.getLog(CspResourceHostsTest.class);

    private final CspConfigHelper _cspConfigHelper = new CspConfigHelper(this);

    @Override
    protected void doCleanup(boolean afterTest)
    {
        _userHelper.deleteUsers(afterTest, APP_ADMIN, TROUBLESHOOTER);
    }

    @BeforeClass
    public static void setupProject()
    {
        CspResourceHostsTest init = getCurrentTest();

        init.doSetup();
    }

    private void doSetup()
    {
        _userHelper.createUser(APP_ADMIN);
        _userHelper.createUser(TROUBLESHOOTER);
        ApiPermissionsHelper apiPermissionsHelper = new ApiPermissionsHelper(this);
        apiPermissionsHelper.addUserAsAppAdmin(APP_ADMIN);
        apiPermissionsHelper.addMemberToRole(TROUBLESHOOTER, "Troubleshooter", user, null);
    }

    @Before
    public void preTest() throws Exception
    {
        _cspConfigHelper.clearAllowedHosts();
    }

    @Test
    public void testTroubleshooterPermissions() throws Exception
    {
        Directive directive = Directive.Connection;
        String host = "https://labkey.org";
        Locator addButton = Locator.lkButton("Add");
        Locator saveButton = Locator.lkButton("Save");
        Locator deleteButton = Locator.lkButton("Delete");
        Locator doneButton = Locator.lkButton("Done");

        ExternalSourcesPage externalSourcesPage = ExternalSourcesPage.beginAt(this);
        externalSourcesPage.addHost(directive, host);
        assertElementPresent(addButton);
        assertElementPresent(saveButton);
        assertElementPresent(deleteButton);
        assertElementNotPresent(doneButton);
        assertEquals("Defined directives", Map.of(directive, Set.of(host)), externalSourcesPage.getExistingHosts());

        impersonate(TROUBLESHOOTER);
        externalSourcesPage = ShowAdminPage.beginAt(this).clickAllowedExternalResourceHosts();
        assertElementNotPresent(addButton);
        assertElementNotPresent(saveButton);
        assertElementNotPresent(deleteButton);
        assertElementPresent(doneButton);
        assertEquals("Defined directives", Map.of(directive, Set.of(host)), externalSourcesPage.getExistingHosts());

        clickAndWait(doneButton);
        assertEquals("Done button destination", "/admin-showAdmin.view", getCurrentRelativeURL());

        try
        {
            _cspConfigHelper.clearAllowedHosts();
            Assert.fail("Troubleshooter should not be able to change allowed hosts");
        }
        catch (CommandException e)
        {
            if (e.getStatusCode() != HttpStatus.SC_FORBIDDEN)
                throw e;
        }
    }

    @Test
    public void testAddDuplicateHostsErrors()
    {
        String host1 = "https://labkey.org";
        Map<Directive, Set<String>> expectedDirectives = new HashMap<>();

        impersonate(APP_ADMIN);
        ExternalSourcesPage externalSourcesPage = ExternalSourcesPage.beginAt(this);

        log("Verify that multiple directives can have the same host");
        for (Directive directive : Directive.values())
        {
            expectedDirectives.put(directive, Set.of(host1));
            externalSourcesPage.addHost(directive, "  " + host1 + "  ");
        }

        log("Verify that each directive can't have duplicate entries that differ only by casing");
        for (Directive directive : Directive.values())
        {
            String host1UpperCase = host1.toUpperCase();
            List<String> errors = externalSourcesPage.addHostExpectingError(directive, "  " + host1UpperCase + "  ");
            Assertions.assertThat(errors).as("error count").hasSize(1);
            Assertions.assertThat(errors.get(0)).contains("Duplicate values are not allowed.", directive.name(), host1UpperCase);
        }

        assertEquals("Defined directives", expectedDirectives, externalSourcesPage.getExistingHosts());
    }

    @Test
    public void testEditDuplicateHostsErrors() throws Exception
    {
        String host1 = "https://labkey.org";
        String host2 = "https://labkey.com";
        Map<Directive, Set<String>> expectedDirectives = new HashMap<>();

        impersonate(APP_ADMIN);
        ExternalSourcesPage externalSourcesPage = ExternalSourcesPage.beginAt(this);

        log("Setup directives with multiple hosts");
        for (Directive directive : Directive.values())
        {
            expectedDirectives.put(directive, Set.of(host1, host2));
            externalSourcesPage.addHost(directive, "  " + host1 + "  ");
            externalSourcesPage.addHost(directive, "  " + host2 + "  ");
        }

        log("Verify that directive can't be edited to have duplicate entries");
        for (Directive directive : Directive.values())
        {
            String host1UpperCase = host1.toUpperCase();
            externalSourcesPage.editExistingSource(directive, host2, "  " + host1UpperCase + "  ");
            List<String> errors = externalSourcesPage.saveChangesExpectingError();
            Assertions.assertThat(errors).as("error count").hasSize(1);
            Assertions.assertThat(errors.get(0)).contains("Duplicate values are not allowed.", directive.name(), host1);
        }

        assertEquals("Defined directives", expectedDirectives, externalSourcesPage.getExistingHosts());
    }

    @Test
    public void testAddInvalidHostsErrors() throws Exception
    {
        String host1 = "https://labkey.org";

        impersonate(APP_ADMIN);
        ExternalSourcesPage externalSourcesPage = ExternalSourcesPage.beginAt(this);

        log("Verify that each directive can't add invalid hosts");
        for (Directive directive : Directive.values())
        {
            String badHost1 = host1 + ";";
            List<String> errors = externalSourcesPage.addHostExpectingError(directive, badHost1);
            Assertions.assertThat(errors).as("error").containsExactly("Semicolons are not allowed in host names");
        }

        log("Verify that each directive can't add blank hosts");
        for (Directive directive : Directive.values())
        {
            List<String> errors = externalSourcesPage.addHostExpectingError(directive, "");
            Assertions.assertThat(errors).as("error").containsExactly("Host must not be blank");
        }

        assertEquals("Defined directives", Collections.emptyMap(), externalSourcesPage.getExistingHosts());
    }

    @Test
    public void testEditInvalidHostsErrors() throws Exception
    {
        String host1 = "https://labkey.org";
        Map<Directive, Set<String>> expectedDirectives = new HashMap<>();

        impersonate(APP_ADMIN);
        ExternalSourcesPage externalSourcesPage = ExternalSourcesPage.beginAt(this);

        log("Setup directives with multiple hosts");
        for (Directive directive : Directive.values())
        {
            expectedDirectives.put(directive, Set.of(host1));
            externalSourcesPage.addHost(directive, "  " + host1 + "  ");
        }

        log("Verify that directive can't be edited to have semicolon");
        for (Directive directive : Directive.values())
        {
            String host1UpperCase = host1.toUpperCase();
            externalSourcesPage.editExistingSource(directive, host1, host1UpperCase + " ;");
            List<String> errors = externalSourcesPage.saveChangesExpectingError();
            Assertions.assertThat(errors).as("error").containsExactly("Semicolons are not allowed in host names");
        }

        log("Verify that directive can't be edited to be blank");
        for (Directive directive : Directive.values())
        {
            externalSourcesPage.editExistingSource(directive, host1, "");
            List<String> errors = externalSourcesPage.saveChangesExpectingError();
            Assertions.assertThat(errors).as("error").containsExactly("Host must not be blank");
        }

        assertEquals("Defined directives", expectedDirectives, externalSourcesPage.getExistingHosts());
    }

    @Test
    public void testAvailableDirectives()
    {
        List<String> expectedDirectives = Arrays.stream(Directive.values()).map(Directive::name).toList();

        ExternalSourcesPage.beginAt(this);
        List<String> availableDirectives = SelectWrapper.Select(Locator.id("newDirective")).find(getDriver())
            .getOptions().stream().map(el -> el.getDomProperty("value")).toList();

        Assertions.assertThat(availableDirectives).as("Available directives")
            .containsExactlyInAnyOrderElementsOf(expectedDirectives);
    }

    @Override
    protected String getProjectName()
    {
        return null;
    }

    @Override
    public List<String> getAssociatedModules()
    {
        return Arrays.asList();
    }
}
