package org.labkey.test.tests.core.admin;

import org.apache.hc.core5.http.HttpStatus;
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
import org.labkey.test.util.core.admin.CspConfigHelper.AllowedHost;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.labkey.test.util.PermissionsHelper.MemberType.user;

@Category({Daily.class})
public class CspResourceHostsTest extends BaseWebDriverTest
{
    private static final String APP_ADMIN = "csp_app_admin@cspresourcehoststest.test";
    private static final String TROUBLESHOOTER = "csp_troubleshooter@cspresourcehoststest.test";

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
        String add = "Add";
        String save = "Save";
        String delete = "Delete";
        String done = "Done";

        ExternalSourcesPage externalSourcesPage = ExternalSourcesPage.beginAt(this);
        externalSourcesPage.addHost(directive, host);
        List<AllowedHost> expectedHosts = List.of(new AllowedHost(directive, host));

        List<String> buttons = getTexts(Locator.lkButton().findElements(getDriver()));
        checker().verifyEqualsSorted("Form buttons", List.of(add, save, delete), buttons);
        checker().verifyEquals("Defined directives", expectedHosts, externalSourcesPage.getExistingHosts());
        checker().screenShotIfNewError("site_admin_csp");

        impersonate(TROUBLESHOOTER);
        externalSourcesPage = ShowAdminPage.beginAt(this).clickAllowedExternalResourceHosts();

        buttons = getTexts(Locator.lkButton().findElements(getDriver()));
        checker().verifyEqualsSorted("Form buttons", List.of(done), buttons);
        checker().verifyEquals("Defined directives", expectedHosts, externalSourcesPage.getExistingHosts());
        checker().screenShotIfNewError("troubleshooter_csp");

        clickAndWait(Locator.lkButton(done));
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
        List<AllowedHost> expectedDirectives = new ArrayList<>();

        impersonate(APP_ADMIN);
        ExternalSourcesPage externalSourcesPage = ExternalSourcesPage.beginAt(this);

        log("Verify that multiple directives can have the same host");
        for (Directive directive : Directive.values())
        {
            expectedDirectives.add(new AllowedHost(directive, host1));
            externalSourcesPage.addHost(directive, "  " + host1 + "  ");
        }

        log("Verify that each directive can't have duplicate entries that differ only by casing");
        for (Directive directive : Directive.values())
        {
            String host1UpperCase = host1.toUpperCase();
            List<String> errors = externalSourcesPage.addHostExpectingError(directive, "  " + host1UpperCase + "  ");
            assertThat(errors).as("error count").hasSize(1);
            assertThat(errors.get(0)).contains("Duplicate values are not allowed.", directive.name(), host1UpperCase);
        }

        assertThat(externalSourcesPage.getExistingHosts()).as("Defined directives")
            .containsExactlyInAnyOrderElementsOf(expectedDirectives);
    }

    @Test
    public void testEditDuplicateHostsErrors()
    {
        String host1 = "https://labkey.org";
        String host2 = "https://labkey.com";
        List<AllowedHost> expectedDirectives = new ArrayList<>();

        impersonate(APP_ADMIN);
        ExternalSourcesPage externalSourcesPage = ExternalSourcesPage.beginAt(this);

        log("Setup directives with multiple hosts");
        for (Directive directive : Directive.values())
        {
            expectedDirectives.add(new AllowedHost(directive, host1));
            expectedDirectives.add(new AllowedHost(directive, host2));
            externalSourcesPage.addHost(directive, "  " + host1 + "  ");
            externalSourcesPage.addHost(directive, "  " + host2 + "  ");
        }

        log("Verify that directive can't be edited to have duplicate entries");
        for (Directive directive : Directive.values())
        {
            String host1UpperCase = host1.toUpperCase();
            externalSourcesPage.editHost(directive, host2, "  " + host1UpperCase + "  ");
            List<String> errors = externalSourcesPage.saveChangesExpectingError();
            assertThat(errors).as("error count").hasSize(1);
            assertThat(errors.get(0)).contains("Duplicate values are not allowed.", directive.name(), host1);
        }

        assertThat(externalSourcesPage.getExistingHosts()).as("Defined directives")
            .containsExactlyInAnyOrderElementsOf(expectedDirectives);
    }

    @Test
    public void testAddInvalidHostsErrors()
    {
        String host1 = "https://labkey.org";

        impersonate(APP_ADMIN);
        ExternalSourcesPage externalSourcesPage = ExternalSourcesPage.beginAt(this);

        log("Verify that each directive can't add invalid hosts");
        for (Directive directive : Directive.values())
        {
            String badHost1 = host1 + ";";
            List<String> errors = externalSourcesPage.addHostExpectingError(directive, badHost1);
            assertThat(errors).as("error").containsExactly("Semicolons are not allowed in host names");
        }

        log("Verify that each directive can't add blank hosts");
        for (Directive directive : Directive.values())
        {
            List<String> errors = externalSourcesPage.addHostExpectingError(directive, "");
            assertThat(errors).as("error").containsExactly("Host must not be blank");
        }

        assertEquals("Defined directives", Collections.emptyList(), externalSourcesPage.getExistingHosts());
    }

    @Test
    public void testEditInvalidHostsErrors()
    {
        String host = "https://labkey.org";
        List<AllowedHost> expectedDirectives = new ArrayList<>();

        impersonate(APP_ADMIN);
        ExternalSourcesPage externalSourcesPage = ExternalSourcesPage.beginAt(this);

        log("Setup directives with multiple hosts");
        for (Directive directive : Directive.values())
        {
            expectedDirectives.add(new AllowedHost(directive, host));
            externalSourcesPage.addHost(directive, "  " + host + "  ");
        }

        log("Verify that directive can't be edited to have semicolon");
        for (Directive directive : Directive.values())
        {
            String host1UpperCase = host.toUpperCase();
            externalSourcesPage.editHost(directive, host, host1UpperCase + " ;");
            List<String> errors = externalSourcesPage.saveChangesExpectingError();
            assertThat(errors).as("error").containsExactly("Semicolons are not allowed in host names");
        }

        log("Verify that directive can't be edited to be blank");
        for (Directive directive : Directive.values())
        {
            externalSourcesPage.editHost(directive, host, "");
            List<String> errors = externalSourcesPage.saveChangesExpectingError();
            assertThat(errors).as("error").containsExactly("Host must not be blank");
        }

        assertThat(externalSourcesPage.getExistingHosts()).as("Defined directives")
            .containsExactlyInAnyOrderElementsOf(expectedDirectives);
    }

    @Test
    public void testDeleteAvailableHosts()
    {
        String host1 = "https://labkey.org";
        String host2 = "https://labkey.com";
        List<AllowedHost> expectedDirectives = new ArrayList<>();

        impersonate(APP_ADMIN);
        ExternalSourcesPage externalSourcesPage = ExternalSourcesPage.beginAt(this);

        log("Setup directives with multiple hosts in a random order");
        for (Directive directive : Directive.values())
        {
            expectedDirectives.add(new AllowedHost(directive, host1));
            expectedDirectives.add(new AllowedHost(directive, host2));
        }
        Collections.shuffle(expectedDirectives);
        for (AllowedHost allowedHost : expectedDirectives)
        {
            externalSourcesPage.addHost(allowedHost.getDirective(), allowedHost.getHost());
        }

        assertThat(externalSourcesPage.getExistingHosts()).as("Defined directives")
            .containsExactlyInAnyOrderElementsOf(expectedDirectives);

        log("Delete directives in a random order");
        Collections.shuffle(expectedDirectives);
        Iterator<AllowedHost> iterator = expectedDirectives.iterator();
        while (iterator.hasNext())
        {
            AllowedHost allowedHost = iterator.next();
            iterator.remove();
            externalSourcesPage.deleteHost(allowedHost.getDirective(), allowedHost.getHost());

            assertThat(externalSourcesPage.getExistingHosts()).as("Defined directives")
                .containsExactlyInAnyOrderElementsOf(expectedDirectives);
        }

        assertThat(externalSourcesPage.getExistingHosts())
            .as("Defined directives after deleting all")
            .isEmpty();
    }

    @Test
    public void testAvailableDirectives()
    {
        List<String> expectedDirectives = Arrays.stream(Directive.values()).map(Directive::name).toList();

        ExternalSourcesPage.beginAt(this);
        List<String> availableDirectives = SelectWrapper.Select(Locator.id("newDirective")).find(getDriver())
            .getOptions().stream().map(el -> el.getDomProperty("value")).toList();

        assertThat(availableDirectives).as("Available directives")
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
