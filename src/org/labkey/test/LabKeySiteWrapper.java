/*
 * Copyright (c) 2015 LabKey Corporation
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
package org.labkey.test;

import org.apache.commons.lang3.StringUtils;
import org.json.simple.JSONObject;
import org.labkey.remoteapi.Command;
import org.labkey.remoteapi.CommandException;
import org.labkey.remoteapi.CommandResponse;
import org.labkey.remoteapi.Connection;
import org.labkey.remoteapi.PostCommand;
import org.labkey.test.util.Ext4Helper;
import org.labkey.test.util.LogMethod;
import org.labkey.test.util.Maps;
import org.labkey.test.util.PasswordUtil;
import org.labkey.test.util.ext4cmp.Ext4GridRef;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebElement;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.labkey.test.WebTestHelper.getHttpGetResponse;
import static org.labkey.test.WebTestHelper.getHttpPostResponse;

/**
 * TODO: Move non-JUnit related methods from BWDT.
 * Many existing helpers, components, and page classes will need refactor as well
 */
public abstract class LabKeySiteWrapper extends WebDriverWrapper
{
    private Stack<String> _impersonationStack = new Stack<>();

    public boolean isGuestModeTest()
    {
        return false;
    }

    // Just sign in & verify -- don't check for startup, upgrade, admin mode, etc.
    public void simpleSignIn()
    {
        if ( isGuestModeTest() )
        {
            goToHome();
            return;
        }

        if (!isTitleEqual("Sign In"))
            beginAt("/login/login.view?");

        if (PasswordUtil.getUsername().equals(getCurrentUser()))
        {
            goToHome();
            return;
        }

        // Sign in if browser isn't already signed in.  Otherwise, we'll be on the home page.
        if (isTitleEqual("Sign In"))
        {
            waitForElement(Locator.id("email"), defaultWaitForPage);
            assertElementPresent(Locator.tagWithName("form", "login"));
            setFormElement(Locator.name("email"), PasswordUtil.getUsername());
            setFormElement(Locator.name("password"), PasswordUtil.getPassword());
            acceptTermsOfUse(null, false);
            clickButton("Sign In", 0);

            if (!waitFor(() -> {
                if (isElementPresent(Locator.id("userMenuPopupLink")))
                    return true;
                bypassSecondaryAuthentication();
                return false;
            }, defaultWaitForPage))
            {
                bypassSecondaryAuthentication();
                String errors = StringUtils.join(getTexts(Locator.css(".labkey-error").findElements(getDriver())), "\n");

                if (errors.contains("The e-mail address and password you entered did not match any accounts on file."))
                    throw new IllegalStateException("Could not log in with the saved credentials.  Please verify that the test user exists on this installation or reset the credentials using 'ant setPassword'");
                else if (errors.contains("Your password does not meet the complexity requirements; please choose a new password."))
                    throw new IllegalStateException("Password complexity requirement was left on by a previous test");
                else if (errors.contains("log in and approve the terms of use."))
                    throw new IllegalStateException("Terms of use not accepted at login");
                else
                    throw new IllegalStateException("Unexpected error(s) during login." + errors);
            }
        }

        assertSignedInNotImpersonating();

        if (isElementPresent(Locator.css(".labkey-nav-page-header").withText("Startup Modules"))||
                isElementPresent(Locator.css(".labkey-nav-page-header").withText("Upgrade Modules")))
        {
            waitForElement(Locator.id("status-progress-bar").withText("Module startup complete"), WAIT_FOR_PAGE);
            clickButton("Next");
        }
    }

    @Deprecated // TODO: Remove after all 16.1.2 feature branches have been merged
    public void assertSignOutAndMyAccountPresent()
    {
        assertSignedInNotImpersonating();
    }

    public void assertSignedInNotImpersonating()
    {
        assertTrue("Not signed in", isSignedIn());
        assertFalse("Impersonating", isImpersonating());
        assertElementPresent(Locators.USER_MENU);
    }

    public void deleteSiteWideTermsOfUsePage()
    {
        try
        {
            log("Removing site-wide terms of use page");
            getHttpPostResponse(WebTestHelper.getBaseURL() + "/wiki/delete.view?name=_termsOfUse");
        }
        catch (IOException e)
        {
            log("Problem removing site-wide terms of use page.  Perhaps it does not exist.");
        }
    }

    protected void bypassSecondaryAuthentication()
    {
        try
        {
            //Select radio Yes
            checkRadioButton(Locator.radioButtonByNameAndValue("valid", "1"));

            //Click on button 'TestSecondary'
            clickAndWait(Locator.input("TestSecondary"));

            disableSecondaryAuthentication();
        }
        catch (NoSuchElementException ignored)
        {

        }
    }

    protected void acceptTermsOfUse(String termsText, boolean clickAgree)
    {
        if (isElementPresent(Locator.id("approvedTermsOfUse")))
        {
            Locator terms = Locator.id("approvedTermsOfUse");
            if ( terms.findElement(getDriver()).isDisplayed())
            {
                checkCheckbox(terms);
                if (null != termsText)
                {
                    assertTextPresent(termsText);
                }
                if (clickAgree)
                    clickButton("Agree");
            }
        }
    }

    protected enum PasswordRule {Weak, Strong}
    protected enum PasswordExpiration {Never, FiveSeconds, ThreeMonths, SixMonths, OneYear}

    private PasswordRule oldStrength = null;
    private PasswordExpiration oldExpiration = null;
    protected void setDbLoginConfig(PasswordRule strength, PasswordExpiration expiration)
    {
        PasswordRule curStrength = null;
        PasswordExpiration curExpiration = null;

        pushLocation();

        beginAt("/login/configureDbLogin.view");


        if ( oldStrength == null || oldExpiration == null )
        {
            // Remember old login settings.
            curStrength = PasswordRule.valueOf(getText(Locator.xpath("//input[@name='strength' and @value='Weak']/.."))); // getAttribute broken on IE
            curExpiration = PasswordExpiration.valueOf(getFormElement(Locator.name("expiration")));
        }

        if ( strength != null && curStrength != strength)
        {
            if ( oldStrength == null ) oldStrength = curStrength;
            click(Locator.radioButtonByNameAndValue("strength", strength.toString()));
        }

        if ( expiration != null && curExpiration != expiration)
        {
            if ( oldExpiration == null ) oldExpiration = curExpiration;
            setFormElement(Locator.name("expiration"), expiration.toString());
        }

        clickButton("Save");

        popLocation();
    }

    @LogMethod
    protected void resetDbLoginConfig()
    {
        if ( oldStrength != null || oldExpiration != null )
        {
            pushLocation();

            if (isElementPresent(Locator.id("userMenuPopupLink")))
            {
                click(Locator.id("userMenuPopupLink"));
                if (isTextPresent("Stop Impersonating"))
                {
                    stopImpersonating();
                }
            }

            beginAt("/login/configureDbLogin.view");

            if ( oldStrength != null ) click(Locator.radioButtonByNameAndValue("strength", oldStrength.toString()));
            if ( oldExpiration != null ) setFormElement(Locator.name("expiration"), oldExpiration.toString());

            clickButton("Save");

            if ( oldStrength != null ) assertEquals("Unable to reset password strength.", oldStrength, PasswordRule.valueOf(getText(Locator.xpath("//input[@name='strength' and @value='Weak']/.."))));
            if ( oldExpiration != null ) assertEquals("Unable to reset password expiration.", oldExpiration, PasswordExpiration.valueOf(getFormElement(Locator.name("expiration"))));

            // Back to default.
            oldStrength = null;
            oldExpiration = null;

            popLocation();
        }
    }

    @LogMethod(quiet = true)
    public boolean disableMiniProfiler()
    {
        boolean restoreMiniProfiler = isMiniProfilerEnabled();
        setMiniProfilerEnabled(false);
        return restoreMiniProfiler;
    }

    @LogMethod(quiet = true)
    public boolean isMiniProfilerEnabled()
    {
        Connection cn = createDefaultConnection(false);
        Command command = new Command("mini-profiler", "enabled");
        try
        {
            CommandResponse r = command.execute(cn, null);
            Map<String, Object> response = r.getParsedData();
            if (response.containsKey("success") && (Boolean)response.get("success"))
            {
                Map<String, Object> data = (Map<String, Object>)response.get("data");
                return (Boolean)data.get("enabled");
            }
        }
        catch (IOException | CommandException e)
        {
            throw new RuntimeException("Failed to get mini-profiler enabled state", e);
        }

        return false;
    }

    @LogMethod
    public void setMiniProfilerEnabled(boolean enabled)
    {
        Connection cn = createDefaultConnection(false);
        PostCommand setEnabled = new PostCommand("mini-profiler", "enabled");
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("enabled", enabled);
        setEnabled.setJsonObject(jsonObject);
        try
        {
            CommandResponse r = setEnabled.execute(cn, null);
            Map<String, Object> response = r.getParsedData();
            if (response.containsKey("success") && (Boolean)response.get("success"))
            {
                Map<String, Object> data = (Map<String, Object>)response.get("data");
                log("MiniProfiler state updated, enabled=" + data.get("enabled"));
            }
        }
        catch (IOException | CommandException e)
        {
            throw new RuntimeException("Failed to " + (enabled ? "enable" : "disable") + " mini-profiler", e);
        }
    }

    @LogMethod (quiet = true)
    public void enableEmailRecorder()
    {
        try
        {
            assertEquals("Failed to enable email recording", 200, getHttpGetResponse(WebTestHelper.getBaseURL() + "/dumbster/setRecordEmail.view?record=true", PasswordUtil.getUsername(), PasswordUtil.getPassword()));
        }
        catch (IOException e)
        {
            throw new RuntimeException("Failed to enable email recorder", e);
        }
    }

    @LogMethod(quiet = true)
    public void enableSecondaryAuthentication()
    {
        Connection cn = createDefaultConnection(true);
        Command command = new Command("login", "enable");
        command.setParameters(new HashMap<>(Maps.of("provider", "Test Secondary Authentication")));
        try
        {
            command.execute(cn, null);
        }
        catch (IOException | CommandException e)
        {
            throw new RuntimeException("Failed to enable Secondary Authentication", e);
        }
    }

    @LogMethod(quiet = true)
    public void disableSecondaryAuthentication()
    {
        Connection cn = createDefaultConnection(true);
        Command command = new Command("login", "disable");
        command.setParameters(new HashMap<>(Maps.of("provider", "Test Secondary Authentication")));
        try
        {
            command.execute(cn, null);
        }
        catch (IOException | CommandException e)
        {
            throw new RuntimeException("Failed to disable Secondary Authentication", e);
        }
    }

    public void impersonateGroup(String group, boolean isSiteGroup)
    {
        _ext4Helper.clickExt4MenuButton(false, Locators.USER_MENU, false, "Impersonate", "Group");
        waitForElement(Ext4Helper.Locators.window("Impersonate Group"));
        _ext4Helper.selectComboBoxItem("Group:", Ext4Helper.TextMatchTechnique.STARTS_WITH, (isSiteGroup ? "Site: " : "") + group);
        clickAndWait(Ext4Helper.Locators.windowButton("Impersonate Group", "Impersonate"));
    }

    public void impersonateRole(String role)
    {
        impersonateRoles(role);
    }

    public void impersonateRoles(String oneRole, String... roles)
    {
        _ext4Helper.clickExt4MenuButton(false, Locators.USER_MENU, false, "Impersonate", "Roles");
        waitForElement(Ext4Helper.Locators.window("Impersonate Roles"));

        waitAndClick(Ext4GridRef.locateExt4GridCell(oneRole));
        for (String role : roles)
            waitAndClick(Ext4GridRef.locateExt4GridCell(role));

        clickAndWait(Ext4Helper.Locators.windowButton("Impersonate Roles", "Impersonate"));
    }

    public void stopImpersonatingRole()
    {
        clickUserMenuItem("Stop Impersonating");
        assertSignedInNotImpersonating();
        goToHome();
    }

    public void stopImpersonatingGroup()
    {
        clickUserMenuItem("Stop Impersonating");
        assertSignedInNotImpersonating();
        goToHome();
    }

    public void impersonate(String fakeUser)
    {
        scrollIntoView(Locators.USER_MENU);
        _ext4Helper.clickExt4MenuButton(false, Locators.USER_MENU, false, "Impersonate", "User");
        waitForElement(Ext4Helper.Locators.window("Impersonate User"));
        _ext4Helper.selectComboBoxItem("User:", Ext4Helper.TextMatchTechnique.STARTS_WITH, fakeUser + " (");
        clickAndWait(Ext4Helper.Locators.windowButton("Impersonate User", "Impersonate"));
        _impersonationStack.push(fakeUser);

        if (isElementPresent(Locator.lkButton("Home")))
        {
            clickAndWait(Locator.lkButton("Home"));
        }
    }

    public void stopImpersonating()
    {
        if (_impersonationStack.isEmpty())
        {
            throw new IllegalStateException("No impersonations are thought to be in progress, based on those that have been started within the test harness");
        }
        String fakeUser = _impersonationStack.pop();
        assertEquals(displayNameFromEmail(fakeUser), getDisplayName());
        clickUserMenuItem("Stop Impersonating");
        assertSignedInNotImpersonating();
        goToHome();
        assertFalse(displayNameFromEmail(fakeUser).equals(getDisplayName()));
    }

    protected void ensureNotImpersonating()
    {
        if (!onLabKeyPage())
            goToHome();

        if (!isSignedInAsAdmin())
            beginAt("/login/logout.view?");
    }

    protected final HashMap<String, String> usersAndDisplayNames = new HashMap<>();

    // assumes there are not collisions in the database causing unique numbers to be appended
    protected String displayNameFromEmail(String email)
    {
        if (usersAndDisplayNames.containsKey(email))
            return usersAndDisplayNames.get(email);
        else
            return getDefaultDisplayName(email);
    }

    protected String getDefaultDisplayName(String email)
    {
        String display = email.contains("@") ? email.substring(0,email.indexOf('@')) : email;
        display = display.replace('_', ' ');
        display = display.replace('.', ' ');
        return display.trim();
    }

    public void goToHome()
    {
        beginAt("/project/home/begin.view");
    }

    /**
     * @deprecated Use {@link #clickButton(String)}
     */
    @Deprecated public void submit()
    {
        WebElement form = getDriver().findElement(By.xpath("//td[@id='bodypanel']//form[1]"));
        doAndWaitForPageToLoad(form::submit);
    }

    /**
     * @deprecated Use {@link #clickButton(String)}
     */
    @Deprecated public void submit(Locator formLocator)
    {
        WebElement form = formLocator.findElement(getDriver());
        doAndWaitForPageToLoad(form::submit);
    }
}
