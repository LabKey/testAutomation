/*
 * Copyright (c) 2008 LabKey Corporation
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

package org.labkey.test.drt;

import org.labkey.test.util.PasswordUtil;
import org.labkey.test.BaseSeleniumWebTest;
import org.labkey.test.Locator;
import org.labkey.test.WebTestHelper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by IntelliJ IDEA.
 * User: Mark Igra
 * Date: March 23, 2007
 */
public class SecurityTest extends BaseSeleniumWebTest
{
    protected static final String PROJECT_NAME = "SecurityVerifyProject";
    protected static final String ADMIN_USER_TEMPLATE = "_admin.template@security.test";
    protected static final String NORMAL_USER_TEMPLATE = "_user.template@security.test";
    protected static final String BOGUS_USER_TEMPLATE = "bogus@bogus@bogus";
    protected static final String ADMIN_USER = "admin@security.test";
    protected static final String NORMAL_USER = "user@security.test";

    public String getAssociatedModuleDirectory()
    {
        return "core";
    }

    protected void doCleanup()
    {
        try {deleteProject(PROJECT_NAME); } catch (Throwable t) {}

        deleteUser(ADMIN_USER_TEMPLATE);
        deleteUser(NORMAL_USER_TEMPLATE);
        deleteUser(ADMIN_USER);
        deleteUser(NORMAL_USER);
    }

    protected void doTestSteps()
    {
        displayNameTest();
        clonePermissionsTest();
        tokenAuthenticationTest();
    }

    private void displayNameTest()
    {
        //set display name to user's email minus domain
        clickLinkWithText("My Account");
        String userName = PasswordUtil.getUsername();
        assertTextPresent(userName);
        clickNavButton("Edit");
        String displayName;
        if (userName.contains("@"))
        {
            displayName = userName.substring(0, userName.indexOf("@"));
        }
        else
        {
            displayName = userName;
        }
        setFormElement("displayName", displayName);
        clickNavButton("Submit");

        //now set it back
        clickNavButton("Edit");
        setFormElement("displayName", userName);
        clickNavButton("Submit");
    }

    private void clonePermissionsTest()
    {
        // create admin templates, plus test bogus & duplicate email addresses
        createUser(ADMIN_USER_TEMPLATE + '\n' + NORMAL_USER_TEMPLATE + '\n' + NORMAL_USER_TEMPLATE + '\n' + BOGUS_USER_TEMPLATE, null);
        assertTextPresent("Failed to create user bogus@bogus@bogus: Invalid email address");
        assertTextPresent(NORMAL_USER_TEMPLATE + " was already a registered system user. Click here to see this user's profile and history.");

        // create the project and set permissions
        createProject(PROJECT_NAME);
        clickLinkWithText("manage group", 0);
        setFormElement("names", ADMIN_USER_TEMPLATE);
        uncheckCheckbox("sendEmail");
        clickNavButton("Update Group Membership", "large");
        clickLinkWithText("Permissions");

        createPermissionsGroup("Testers");
        assertPermissionSetting("Testers", "No Permissions");
        setPermissions("Testers", "Editor");
        clickLinkWithText("manage group", 1);
        setFormElement("names", NORMAL_USER_TEMPLATE);
        uncheckCheckbox("sendEmail");
        clickNavButton("Update Group Membership", "large");

        // create users and verify permissions
        createUser(ADMIN_USER, ADMIN_USER_TEMPLATE);
        createUser(NORMAL_USER, NORMAL_USER_TEMPLATE);

        // verify permissions
        checkGroupMembership(ADMIN_USER, "SecurityVerifyProject/Administrators");
        checkGroupMembership(NORMAL_USER, "SecurityVerifyProject/Testers");
    }

    private void checkGroupMembership(String userName, String groupName)
    {
        clickLinkWithText("Site Users");

        Locator userAccessLink = Locator.xpath("//td[text()='" + userName + "']/..//td/a[contains(@href,'userAccess.view')]");
        if (isElementPresent(userAccessLink))
        {
            clickLink(userAccessLink);
            Locator groupMembershipLink = Locator.xpath("//td[@id='bodypanel']//td/a[text()='SecurityVerifyProject']/../../td[3]/a");
            if (isElementPresent(groupMembershipLink))
            {
                String text = getText(groupMembershipLink);
                assertEquals("The group membership does not match what is expected", groupName, text);
                return;
            }
            
        }
        fail("Unable to verify group membership of cloned user privileges");
    }

    private void tokenAuthenticationTest()
    {
        clickLinkWithText(PROJECT_NAME);
        String homePageUrl = removeUrlParameters(getURL().toString());  // Absolute URL for redirect, get rid of '?'
        String baseUrl = removeUrlParameters(getCurrentRelativeURL()).replaceAll("/project/", "/login/").replaceAll("begin.view", "");

        // Attempt to verify bogus token -- should result in failure
        String xml = retrieveFromUrl(baseUrl + "verifyToken.view?labkeyToken=ABC");
        assertFailureAuthenticationToken(xml);

        beginAt(baseUrl + "createToken.view?returnUrl=" + homePageUrl);
        // Make sure we redirected to the right place
        assertTrue(removeUrlParameters(getURL().toString()).equals(homePageUrl));

        Map<String, String> params = getUrlParameters();
        String email = params.get("labkeyEmail");
        String emailName;
        String userName = PasswordUtil.getUsername();
        // If we are using IE, then the email will be stripped of its @etc.
        if (!userName.contains("@"))
        {
            emailName = email.substring(0, email.indexOf("@"));
        }
        else
        {
            emailName = email;
        }
        assertTrue(emailName.equals(userName));
        String token = params.get("labkeyToken");
        xml = retrieveFromUrl(baseUrl + "verifyToken.view?labkeyToken=" + token);
        assertSuccessAuthenticationToken(xml, token, email, 65535);

        beginAt(baseUrl + "invalidateToken.view?labkeyToken=" + token + "&returnUrl=" + homePageUrl);
        // Make sure we redirected to the right place
        assertTrue(removeUrlParameters(getURL().toString()).equals(homePageUrl));
        // Should fail now
        xml = retrieveFromUrl(baseUrl + "verifyToken.view?labkeyToken=" + token);
        assertFailureAuthenticationToken(xml);

        impersonate(NORMAL_USER);

        beginAt(baseUrl + "createToken.view?returnUrl=" + homePageUrl);
        // Make sure we redirected to the right place
        assertTrue(removeUrlParameters(getURL().toString()).equals(homePageUrl));

        params = getUrlParameters();
        email = params.get("labkeyEmail");
        assertTrue(email.equals(NORMAL_USER));
        token = params.get("labkeyToken");
        xml = retrieveFromUrl(baseUrl + "verifyToken.view?labkeyToken=" + token);
        assertSuccessAuthenticationToken(xml, token, email, 223);

        // Test that LabKey Server sign out invalidates the token
        signOut();
        xml = retrieveFromUrl(baseUrl + "verifyToken.view?labkeyToken=" + token);
        assertFailureAuthenticationToken(xml);

        signIn(); // Leave admin logged in for clean up
    }


    private void assertFailureAuthenticationToken(String xml)
    {
        assertTrue(xml.startsWith("<TokenAuthentication success=\"false\" message=\"Unknown token\"/>"));
    }


    private void assertSuccessAuthenticationToken(String xml, String token, String email, int permissions)
    {
        String correct = "<TokenAuthentication success=\"true\" token=\"" + token + "\" email=\"" + email + "\" permissions=\"" + permissions + "\"/>";
        assertTrue(xml.startsWith(correct));
    }


    private String retrieveFromUrl(String relativeUrl)
    {
        String newline = System.getProperty("line.separator");
        InputStream is = null;

        try
        {
            StringBuilder sb = new StringBuilder();
            URL url = new URL(WebTestHelper.getBaseURL() + "/" + relativeUrl);
            is = url.openStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));

            String line;
            while ((line = reader.readLine()) != null)
            {
                sb.append(line);
                sb.append(newline);
            }

            return sb.toString();
        }
        catch (Exception e)
        {
            log("Failure attempting to retrieve " + relativeUrl);
            log(e.getMessage());
            fail();
            return null;
        }
        finally
        {
            if (null != is)
            {
                try
                {
                    is.close();
                }
                catch (IOException e)
                {
                    //
                }
            }
        }
    }


    // Move to BaseWebTest?
    private Map<String, String> getUrlParameters()
    {
        String queryString = null;

        try
        {
            queryString = getURL().toURI().getQuery();  // URI decodes the query string
        }
        catch (URISyntaxException e)
        {
            log(e.getMessage());
            fail();
        }

        Map<String, String> map = new HashMap<String, String>();
        if (queryString != null)
        {
            String[] params = queryString.split("&");

            for (String param : params)
            {
                int index = param.indexOf('=');
                map.put(param.substring(0, index), param.substring(index + 1));
            }
        }

        return map;
    }


    private String removeUrlParameters(String url)
    {
        int index = url.indexOf('?');

        if (-1 == index)
            return url;
        else
            return url.substring(0, index);
    }
}
