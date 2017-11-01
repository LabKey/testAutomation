/*
 * Copyright (c) 2015-2017 LabKey Corporation
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

import org.junit.BeforeClass;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.TestTimeoutException;
import org.labkey.test.util.ApiPermissionsHelper;
import org.labkey.test.util.PortalHelper;
import org.labkey.test.util.WikiHelper;

import java.util.Collections;
import java.util.List;

public class BaseTermsOfUseTest extends BaseWebDriverTest
{
    protected static final String PUBLIC_NO_TERMS_PROJECT_NAME = "Public No Terms Project";
    protected static final String PUBLIC_TERMS_PROJECT_NAME = "TermsOfUse Public Project";
    protected static final String NON_PUBLIC_TERMS_PROJECT2_NAME = "TermsOfUse Non-Public Project 2";
    protected static final String NON_PUBLIC_TERMS_PROJECT_NAME = "TermsOfUse Non-Public Project";
    protected static final String USERS_GROUP = "Users";
    protected static final String USER = "termsofuse_user2@termsofuse.test";

    protected final PortalHelper _portalHelper = new PortalHelper(this);
    protected WikiHelper _wikiHelper = new WikiHelper(this);
    protected final ApiPermissionsHelper _permissionsHelper = new ApiPermissionsHelper(this);

    protected static final String WIKI_TERMS_TITLE = "Terms of Use";
    protected static final String PROJECT_TERMS_SNIPPET = "fight club";
    protected static final String TERMS_OF_USE_NAME = "_termsOfUse";

    @Override
    protected void doCleanup(boolean afterTest) throws TestTimeoutException
    {
        _userHelper.deleteUsers(false, USER);
        log("Deleting test projects");

        _containerHelper.deleteProject(PUBLIC_NO_TERMS_PROJECT_NAME, false);
        _containerHelper.deleteProject(PUBLIC_TERMS_PROJECT_NAME, false);
        _containerHelper.deleteProject(NON_PUBLIC_TERMS_PROJECT_NAME, false);
        _containerHelper.deleteProject(NON_PUBLIC_TERMS_PROJECT2_NAME, false);
    }

    @BeforeClass
    public static void setupProject()
    {
        BaseTermsOfUseTest init = (BaseTermsOfUseTest) getCurrentTest();

        init.doSetup();
    }

    protected void doSetup()
    {
        _userHelper.createUser(USER);
        _permissionsHelper.createPermissionsGroup(USERS_GROUP, USER);

        createPublicProject(PUBLIC_NO_TERMS_PROJECT_NAME);
        createProjectWithTermsOfUse(PUBLIC_TERMS_PROJECT_NAME, "The first rule of fight club is do not talk about fight club.", true);
        createProjectWithTermsOfUse(NON_PUBLIC_TERMS_PROJECT_NAME, "The second rule of fight club is do not talk about fight club.", false);
        createProjectWithTermsOfUse(NON_PUBLIC_TERMS_PROJECT2_NAME, "The third rule of fight club is do not talk about fight club.", false);
    }

    protected void createPublicProject(String projectName)
    {
        log("Create public project " + projectName);
        _containerHelper.createProject(projectName, null);
        _permissionsHelper.setPermissions(USERS_GROUP, "Editor");
        _permissionsHelper.setSiteGroupPermissions("All Site Users", "Reader");
        _permissionsHelper.setSiteGroupPermissions("Guests", "Reader");
    }

    protected void createWikiTabForProject(String projectName)
    {
        goToHome();
        clickProject(projectName);
        log("Create wiki tab");
        _portalHelper.addWebPart("Wiki");
        goToFolderManagement();
        clickAndWait(Locator.linkWithText("Folder Type"));
        checkCheckbox(Locator.checkboxByTitle("Wiki"));
        clickButton("Update Folder");
    }

    protected void createProjectWithTermsOfUse(String name, String termsText, boolean isPublic)
    {
        log("Create project " + name);
        _containerHelper.createProject(name, null);
        createTermsOfUsePage(name, termsText);
        _permissionsHelper.setSiteGroupPermissions("All Site Users", "Reader");
        if (isPublic)
        {
            _permissionsHelper.setSiteGroupPermissions("Guests", "Reader");
        }
    }

    protected void createTermsOfUsePage(String projectName, String body)
    {
        String message = null;
        if (null != projectName)
        {
            message = "Create terms of use page for project " + projectName;
            clickProject(projectName);
            goToModule("Wiki");
        }
        else // site-wide terms of use page
        {
            message = "Create site-wide terms of use page";
            beginAt("/wiki/page.view?name=_termsOfUse");
        }
        if (isElementPresent(Locator.linkContainingText("add a new page")))
        {
            log(message);
            _wikiHelper.createNewWikiPage("RADEOX");
            setFormElement(Locator.name("name"), TERMS_OF_USE_NAME);
            setFormElement(Locator.name("title"), WIKI_TERMS_TITLE);
            setFormElement(Locator.name("body"), body);
            _wikiHelper.saveWikiPage();
            acceptTermsOfUse(null, true);
        }
    }

    @Override
    protected BrowserType bestBrowser()
    {
        return BrowserType.CHROME;
    }

    @Override
    protected String getProjectName()
    {
        return PUBLIC_NO_TERMS_PROJECT_NAME;
    }

    @Override
    public List<String> getAssociatedModules()
    {
        return Collections.singletonList("core");
    }

    protected void goToProjectBegin(String projectName)
    {
        beginAt("project/" + projectName + "/begin.view?");
    }

}