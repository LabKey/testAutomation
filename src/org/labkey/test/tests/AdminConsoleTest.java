/*
 * Copyright (c) 2013-2019 LabKey Corporation
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

import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.remoteapi.CommandException;
import org.labkey.remoteapi.Connection;
import org.labkey.remoteapi.SimpleGetCommand;
import org.labkey.remoteapi.SimplePostCommand;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.WebTestHelper;
import org.labkey.test.util.OptionalFeatureHelper;
import org.labkey.test.categories.Daily;
import org.labkey.test.pages.core.admin.CustomizeSitePage;
import org.labkey.test.pages.core.admin.OptionalFeaturesPage;
import org.labkey.test.pages.core.login.LoginConfigRow;
import org.labkey.test.pages.core.login.LoginConfigurePage;
import org.labkey.test.util.LogMethod;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

@Category({Daily.class})
@BaseWebDriverTest.ClassTimeout(minutes = 3)
public class AdminConsoleTest extends AbstractAdminConsoleTest
{
    @Test
    public void testServerHttpHeaderSetting()
    {
        CustomizeSitePage customizeSitePage = goToAdminConsole().clickSiteSettings();
        boolean originalValue = customizeSitePage.isEnableServerHttpHeader();

        // Try with the setting on
        if (!originalValue)
        {
            customizeSitePage.setEnableServerHttpHeader(true).save();
        }

        String serverHeader = getServerHeader();
        assertTrue("Expected to get a Server header, but got " + serverHeader, serverHeader != null && serverHeader.startsWith("LabKey/"));

        // Try with the setting off
        customizeSitePage = goToAdminConsole().clickSiteSettings();
        customizeSitePage.setEnableServerHttpHeader(false).save();

        serverHeader = getServerHeader();
        assertNull("Expected to get no Server header, but got " + serverHeader, serverHeader);

        if (originalValue)
        {
            // Turn the setting back on
            customizeSitePage = goToAdminConsole().clickSiteSettings();
            customizeSitePage.setEnableServerHttpHeader(true).save();
        }
    }

    private static class GetServerHeaderCommand extends SimpleGetCommand
    {
        private String _server;
        public GetServerHeaderCommand()
        {
            super("project", "begin");
        }

        @Override
        protected Response _execute(Connection connection, String folderPath) throws CommandException, IOException
        {
            Response response = super._execute(connection, folderPath);
            _server = response.getHeaderValue("Server");
            return response;
        }
    }

    @LogMethod(quiet = true)
    private String getServerHeader()
    {
        Connection cn = createDefaultConnection();
        try
        {
            GetServerHeaderCommand command = new GetServerHeaderCommand();
            command.execute(cn, "/home");
            return command._server;
        }
        catch (IOException | CommandException e)
        {
            throw new RuntimeException("Failed to get Server HTTP response header", e);
        }
    }

    @Test
    public void testBogusSiteSettings()
    {
        // Issue 53562: Cap max BLOB size
        CustomizeSitePage customizeSitePage = goToAdminConsole().clickSiteSettings();
        customizeSitePage.setMaxBLOBSize(Integer.toString(200 * 1024 * 1024 + 1));
        customizeSitePage.setSslPort("-4");
        customizeSitePage.setMemoryUsageDumpInterval("-3");
        customizeSitePage.setReadOnlyHttpRequestTimeout("-1");
        customizeSitePage.save();
        assertTextPresent(
            "Maximum BLOB size cannot be set higher than 209715200 bytes",
            "HTTPS port must be between 1 and 65,535",
            "Memory logging frequency must be non-negative",
            "HTTP timeout must be non-negative"
        );

        customizeSitePage = new CustomizeSitePage(getDriver());
        customizeSitePage.setMaxBLOBSize("-10");
        customizeSitePage.setSslPort(Integer.toString(256 * 256)); // 2^16
        customizeSitePage.save();
        assertTextPresent(
            "Maximum BLOB size cannot be negative",
            "HTTPS port must be between 1 and 65,535"
        );
    }
    
    @Test
    public void testRibbonBar()
    {
        CustomizeSitePage customizeSitePage = goToAdminConsole().clickSiteSettings();
        customizeSitePage.setRibbonMessage(null);

        //only select if not already checked
        if (!customizeSitePage.isShowRibbonMessage())
            customizeSitePage.setShowRibbonMessage(true);

        customizeSitePage.save();

        waitForElement(Locator.xpath("//div[contains(text(), 'Cannot enable the ribbon message without providing a message to show')]"));

        String linkText = "and also click this...";
        String html = "READ ME!!!  <a href='<%=contextPath%>" + "/home/project-begin.view'>" + linkText + "</a>";

        customizeSitePage = new CustomizeSitePage(getDriver());
        //only check if not already checked
        if (!customizeSitePage.isShowRibbonMessage())
            customizeSitePage.setShowRibbonMessage(true);

        customizeSitePage.setRibbonMessage(html).save();

        Locator ribbon = Locator.tagWithClass("div", "alert alert-warning").containing("READ ME!!!");
        waitForElement(ribbon);

        Locator ribbonLink = Locator.tagWithClassContaining("div", "alert").append(Locator.linkContainingText("and also click this..."));
        assertElementPresent(ribbonLink);
        String href = ribbonLink.findElement(getDriver()).getAttribute("href");
        String expected = WebTestHelper.getBaseURL() + "/home/project-begin.view";
        assertEquals("Incorrect URL", expected, href);

        goToHome();
        impersonateRole("Reader");
        assertElementPresent(ribbon);
        assertElementPresent(ribbonLink);
        stopImpersonating();

        customizeSitePage = goToAdminConsole().clickSiteSettings();
        customizeSitePage.setShowRibbonMessage(false).save();
        assertElementNotPresent(ribbon);
        assertElementNotPresent(ribbonLink);
    }

    // Issue 51843  allow site banner configuration via api
    @Test
    public void testSiteBannerAPIConfiguration() throws Exception
    {
        goToAdminConsole();

        String bannerMessage = "test banner message" + TRICKY_CHARACTERS;
        Locator bannerLoc = Locator.tagWithClass("div", "lk-dismissable-warn")
                .containing("test banner message" + TRICKY_CHARACTERS);

        //As site admin
        // set the message and show it
        var showBannerCmd = new SimplePostCommand("admin", "setRibbonMessage.api");
        showBannerCmd.setParameters(Map.of("message", bannerMessage,
                "show", true));
        showBannerCmd.execute(createDefaultConnection(), "/");
        refresh();
        // verify it is shown
        WebDriverWrapper.waitFor(()-> bannerLoc.isDisplayed(getDriver()), 1000);
        if (checker().withScreenshot("banner not shown or not as expected")
                .verifyTrue("expect banner to be shown", bannerLoc.isDisplayed(getDriver())))
        {
            // hide the banner
            var hideBannerCmd = new SimplePostCommand("admin", "setRibbonMessage.api");
            hideBannerCmd.setParameters(Map.of("show", false));
            hideBannerCmd.execute(createDefaultConnection(), "/");
            refresh();
            WebDriverWrapper.waitFor(()-> !bannerLoc.isDisplayed(getDriver()), 1000);
            // verify it is hidden
            checker().withScreenshot("banner is shown when not expected")
                    .verifyFalse("expect banner not to be shown", bannerLoc.isDisplayed(getDriver()));
        }

        // restore it with the previous banner value
        var restoreBannerCmd = new SimplePostCommand("admin", "setRibbonMessage.api");
        restoreBannerCmd.setParameters(Map.of("show", true));
        restoreBannerCmd.execute(createDefaultConnection(), "/");
        refresh();
        // verify it is restored with the previous value
        checker().withScreenshot("banner not shown or not as expected")
                .verifyTrue("expect banner to be shown", bannerLoc.isDisplayed(getDriver()));

        // as app admin
        impersonateRole("Application Admin");
        var reHideBannerCmd = new SimplePostCommand("admin", "setRibbonMessage.api");
        reHideBannerCmd.setParameters(Map.of("show", false));
        try
        {
            reHideBannerCmd.execute(createDefaultConnection(), "/");
            fail("expect exception trying to call this API as app admin");
        }
        catch (CommandException e)
        {
            // success, caller with app admin failed to hit this api
        }
        refresh();
        // verify it remains shown
        checker().withScreenshot("banner is hidden when not expected")
                .verifyTrue("expect banner to be shown", bannerLoc.isDisplayed(getDriver()));

        stopImpersonating();

        // clean up after ourselves as site admin
        var clearAndHideBannerCmd = new SimplePostCommand("admin", "setRibbonMessage.api");
        clearAndHideBannerCmd.setParameters(Map.of("show", false, "message", ""));
        clearAndHideBannerCmd.execute(createDefaultConnection(), "/");
        refresh();
        checker().withScreenshot("banner is shown when not expected")
                .verifyFalse("expect banner not to be shown", bannerLoc.isDisplayed(getDriver()));
    }

    @Test
    public void testUIOptionalFeatures()
    {
        goToAdminConsole();

        var featureIds = List.of("extendedMetrics", "StageFileUploads");

        verifyOptionalFeatures("optional features", featureIds, OptionalFeaturesPage.OptionalFeatureType.Optional);
    }

    @Test
    public void testUIExperimentalFeatures()
    {
        goToAdminConsole();

        var featureIds = List.of("queryBasedDatasets", "LinkedDatasetCheck", "blockMaliciousClients");

        verifyOptionalFeatures("experimental features", featureIds, OptionalFeaturesPage.OptionalFeatureType.Experimental);
    }

    @Test
    public void testAppAdminRole()
    {
        Locator siteAdminLoc = Locator.pageHeader("Site Administration");
        
        // log out as siteAdmin, log in as appAdmin
        signOut();
        signIn(APP_ADMIN_USER);

        // verify that all the following links are visible to AppAdmin:
        goToAdminConsole().goToSettingsSection();
        List<String> expectedLinkTexts = new ArrayList<>(Arrays.asList("change user properties",
                "experimental features",
                "deprecated features",
                "folder types",
                "look and feel settings",
                "missing value indicators",
                "profiler",
                "project display order",
                "short urls",
                "audit log",
                "full-text search",
                "pipeline",
                "site-wide terms of use",
                "actions",
                "caches",
                "credits",
                "data sources",
                "dump heap",
                "memory usage",
                "queries",
                "reset site errors",
                "running threads",
                "site validation",
                "view all site errors",
                "view all site errors since reset",
                "view primary site log file"));
        if (_containerHelper.getAllModules().contains("dataintegration"))
            expectedLinkTexts.addAll(Arrays.asList("etl - all job histories", "etl - run site scope etls"));

        expectedLinkTexts.removeIf(linkText -> isElementPresent(Locator.linkWithText(linkText)));
        assertTrue("Missing expected admin console links: " + expectedLinkTexts, expectedLinkTexts.isEmpty());

        // confirm that NONE of the following are visible to AppAdmin:
        List<String> notShownLinks = Arrays.asList(
                "files",
                "flow cytometry",
                "mascot server",
                "ldap sync admin",
                "notification service",
                "ms2",
                "check database",
                "loggers",
                "sql scripts",
                "environment variables",
                "system properties");
        for (String linkText: notShownLinks)
        {
            assertElementNotPresent(Locator.linkWithText(linkText));
        }

        //analytics settings
        goToAdminConsole().clickAnalyticsSettings();
        assertElementNotPresent(Locator.button("submit"));
        clickButton("done");
        assertElementPresent("expect to return to admin console", siteAdminLoc, 1);

        //authentication
        LoginConfigurePage configurePage = goToAdminConsole().clickAuthentication();
        List<LoginConfigRow> configRows = configurePage.getPrimaryConfigurations();
        assertFalse("expect 'edit' links not to be available for auth configs", configRows.stream().anyMatch(LoginConfigRow::canEdit));
        assertFalse("expect 'add configuration' menu to be absent for AppAdmin", configurePage.canAddConfiguration());
        clickButton("Done");
        assertElementPresent("expect to return to admin console", siteAdminLoc, 1);

        //email customization
        goToAdminConsole().clickEmailCustomization();
        clickButton("Cancel");
        assertElementPresent("expect to return to admin console", siteAdminLoc, 1);

        //site settings
        goToAdminConsole().clickSiteSettings();
        clickButton("Done");
        assertElementPresent("expect to return to admin console", siteAdminLoc, 1);

        //system maintenance
        goToAdminConsole().clickSystemMaintenance();
        clickButton("Done");
        assertElementPresent("expect to return to admin console", siteAdminLoc, 1);

        // views and scripting
        goToAdminConsole().clickViewsAndScripting();
        assertNull(Locator.buttonContainingText("Edit").findElementOrNull(getDriver()));
        clickButton("Done");
        assertElementPresent("expect to return to admin console", siteAdminLoc, 1);
    }

    @Test
    public void testConfigureReturnURL()
    {
        String host = "google.com";
        goToAdminConsole().clickAllowedExternalRedirectHosts();

        log("Verifying host cannot be blank ");
        clickButton("Save");
        assertElementPresent(Locator.css(".labkey-error").withText("Redirect host name must not be blank."));

        log("Setting the host URL");
        setFormElement(Locator.name("newValue"), host);
        clickButton("Save");

        log("Verifying url got added correctly");
        assertEquals(host, getFormElement(Locator.name("existingValue1")));

        log("Verifying cannot be duplicate");
        setFormElement(Locator.name("newValue"), host);
        clickButton("Save");
        assertElementPresent(Locator.css(".labkey-error").withText("'" + host + "' already exists. Duplicate values not allowed."));
    }

    /*
       Test coverage : Issue 46587: Add test for display of credits page
       https://www.labkey.org/home/Developer/issues/issues-details.view?issueId=46587
    */
    @Test
    public void testAdminConsoleCredits()
    {
        goToAdminConsole().clickCredits();
        log("Verifying the page is properly loaded");
        assertTextPresent("JAR Files Distributed with the API Module");
    }

    private void verifyOptionalFeatures(String linkText, List<String> featureIds, OptionalFeaturesPage.OptionalFeatureType optionalFeatureType)
    {
        waitAndClickAndWait(Locator.linkWithText(linkText));
        var optionalFeaturesPage = new OptionalFeaturesPage(getDriver());
        var cn = createDefaultConnection();

        for (String testId : featureIds) {
            // capture initial state
            boolean initialState = OptionalFeatureHelper.isOptionalFeatureEnabled(cn, testId);

            // ensure the UI reflects the same state
            boolean initialUIState = optionalFeaturesPage.getFeatureStatus(testId);
            checker().withScreenshot("initial state not as expected")
                    .wrapAssertion(()-> Assertions.assertThat(initialUIState)
                            .as("expect ui to align with API initial state")
                            .isEqualTo(initialState));

            // toggle it the other way
            optionalFeaturesPage.setFeatureStatus(testId, !initialState);
            checker().withScreenshot("toggled state not as expected")
                    .awaiting(Duration.ofMillis(500), ()-> Assertions.assertThat(OptionalFeatureHelper.isOptionalFeatureEnabled(cn, testId))
                            .as("expect toggling the UI to update the server status for the feature")
                            .isEqualTo(!initialState));

            // use the API to restore the initial state
            OptionalFeatureHelper.setOptionalFeature(cn, testId, initialState);
            optionalFeaturesPage.goToAdminConsole();

            optionalFeaturesPage = OptionalFeaturesPage.beginAt(this, optionalFeatureType);

            // verify the page state reflects the API change after a reload
            checker().withScreenshot("state not as expected after api set and refresh")
                    .verifyEquals("expect page to reflect state after api config",
                            initialState, optionalFeaturesPage.getFeatureStatus(testId));
        }
    }
}
