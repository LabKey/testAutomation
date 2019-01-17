/*
 * Copyright (c) 2018 LabKey Corporation
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
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.Locator.XPathLocator;
import org.labkey.test.TestTimeoutException;
import org.labkey.test.categories.DailyB;
import org.labkey.test.pages.ConfigureReportsAndScriptsPage;
import org.labkey.test.pages.ConfigureReportsAndScriptsPage.EngineConfig;
import org.labkey.test.pages.ConfigureReportsAndScriptsPage.EngineType;
import org.labkey.test.util.RReportHelper;

import java.util.List;

@Category({DailyB.class})
public class RConfigTest extends BaseWebDriverTest
{
    private String DEFAULT_ENGINE_NAME = "R Scripting Engine";
    private String SECONDARY_ENGINE_NAME = "R Scripting Engine 2";
    private String DISABLED_ENGINE_NAME = "Disabled Engine";

    private static final String DEFAULT_PARENT_LABEL = "Reports : R Scripting Engine\nPipeline Jobs : R Scripting Engine";
    private static final String SECONDARY_PARENT_LABEL = "Reports : R Scripting Engine 2\nPipeline Jobs : R Scripting Engine 2";

    private String FOLDER_NAME = "subfolder";

    private RReportHelper _RReportHelper = new RReportHelper(this);

    @Override
    protected void doCleanup(boolean afterTest) throws TestTimeoutException
    {
        ConfigureReportsAndScriptsPage scripts = ConfigureReportsAndScriptsPage.beginAt(this);

        log("Clean up scripting engines");
        scripts.deleteAllREngines();

        log("Delete project");
        _containerHelper.deleteProject(getProjectName(), afterTest);
    }

    @BeforeClass
    public static void setup() throws Exception
    {
        RConfigTest init = (RConfigTest) getCurrentTest();
        init.doSetup();
    }

    protected void doSetup() throws Exception
    {
        _containerHelper.createProject(getProjectName(), "Custom");
        _containerHelper.createSubfolder(getProjectName(), FOLDER_NAME);
    }

    @Before
    public void configureEngines()
    {
        ConfigureReportsAndScriptsPage scripts = ConfigureReportsAndScriptsPage.beginAt(this);

        EngineConfig config = new EngineConfig(_RReportHelper.getRExecutable());

        _RReportHelper.ensureRConfig(false);

        if (!scripts.isEnginePresent(SECONDARY_ENGINE_NAME))
        {
            log("Configure secondary R engine");
            config.setName(SECONDARY_ENGINE_NAME);
            scripts.addEngine(EngineType.R, config);
        }

        if (!scripts.isEnginePresent(DISABLED_ENGINE_NAME))
        {
            log("Configure and disable R engine");
            config.setName(DISABLED_ENGINE_NAME);
            scripts.addEngine(EngineType.R, config);
            sleep(2000); //wait for store and view update
            scripts.editEngine(DISABLED_ENGINE_NAME);
            Assert.assertTrue(_ext4Helper.isChecked(XPathLocator.id("editEngine_enabled-inputEl")));
            _ext4Helper.uncheckCheckbox(XPathLocator.id("editEngine_enabled-inputEl"));
            sleep(2000); // wait for store update
            clickButton("Submit", -1);
        }
    }

    @Test
    public void testAddMultipleEngines()
    {
        ConfigureReportsAndScriptsPage scripts = ConfigureReportsAndScriptsPage.beginAt(this);

        log("Verify R engines exist");
        Assert.assertTrue("Default R engine does not exist", scripts.isEnginePresent(DEFAULT_ENGINE_NAME));
        Assert.assertTrue("Secondary R engine was not created properly", scripts.isEnginePresent(SECONDARY_ENGINE_NAME));
        Assert.assertTrue("Disabled R engine was not created properly", scripts.isEnginePresent(DISABLED_ENGINE_NAME));
    }

    @Test
    public void testEngineIsInherited()
    {
        log("Navigate to 'R Config' tab in folder management page");
        goToProjectHome();
        goToFolderManagement().goToRConfigTab();

        log("Verify folder is inheriting the correct parent R configuration");
        Assert.assertEquals("Folder is not inheriting the correct parent R configuration",DEFAULT_PARENT_LABEL, getText(Locator.id("parentConfigLabel")));
        assertRadioButtonSelected(Locator.radioButtonByNameAndValue("overrideDefault", "parent"));
        assertAttributeContains(Locator.id("saveBtn"), "class", "labkey-disabled-button");

        log("Verify only enabled, non-default engines are listed in override dropdown");
        assertElementPresent(Locator.xpath("//select/option[normalize-space(text())=\"" + SECONDARY_ENGINE_NAME + "\"]"));
        assertElementNotPresent("Default engine should not be listed in override dropdown", Locator.xpath("select/option[normalize-space(text())=\"" + DEFAULT_ENGINE_NAME + "\"]\n"));
        assertElementNotPresent("Disabled engine should not be listed in override dropdown", Locator.xpath("select/option[normalize-space(text())=\"" + DISABLED_ENGINE_NAME + "\"]\n"));

        log("Set folder level R configuration override");
        checkRadioButton(Locator.radioButtonByNameAndValue("overrideDefault", "override"));
        assertAttributeNotContains(Locator.id("saveBtn"), "class", "labkey-disabled-button");
        selectOptionByText(Locator.name("reportEngine"), SECONDARY_ENGINE_NAME);
        selectOptionByText(Locator.name("pipelineEngine"), SECONDARY_ENGINE_NAME);
        assertAttributeNotContains(Locator.id("saveBtn"), "class", "labkey-disabled-button");
        clickButton("Save", "Override Default R Configuration");
        clickButton("Yes");

        log("Verify subfolder is inheriting the correct parent R configuration");
        navigateToFolder(getProjectName(), FOLDER_NAME);
        goToFolderManagement().goToRConfigTab();
        Assert.assertEquals("Subfolder is not inheriting the correct parent R configuration", SECONDARY_PARENT_LABEL, getText(Locator.id("parentConfigLabel")));
    }

    @Test
    public void testSwitchDefaultEngine()
    {
        ConfigureReportsAndScriptsPage scripts = ConfigureReportsAndScriptsPage.beginAt(this);

        log("Verify site default checkbox is checked and disabled");
        scripts.editEngine(DEFAULT_ENGINE_NAME);
        Assert.assertTrue(_ext4Helper.isChecked(XPathLocator.id("editEngine_default-inputEl")));
        assertAttributeEquals(XPathLocator.id("editEngine_default-inputEl"), "disabled", "true");

        log("Disabling site default should be prevented");
        Assert.assertTrue(_ext4Helper.isChecked(XPathLocator.id("editEngine_enabled-inputEl")));
        _ext4Helper.uncheckCheckbox(XPathLocator.id("editEngine_enabled-inputEl"));
        clickButton("Submit", -1);
        assertExt4MsgBox("Site default engine must be enabled.", "OK");
        clickButton("Cancel", -1);

        log("Switch site default");
        scripts.setSiteDefault(SECONDARY_ENGINE_NAME);

        log("Verify site default has switched in admin console page");
        scripts.editEngine(DEFAULT_ENGINE_NAME);
        Assert.assertFalse(_ext4Helper.isChecked(XPathLocator.id("editEngine_default-inputEl")));
        clickButton("Cancel", -1);
        scripts.editEngine(SECONDARY_ENGINE_NAME);
        Assert.assertTrue(_ext4Helper.isChecked(XPathLocator.id("editEngine_default-inputEl")));
        clickButton("Cancel", -1);

        log("Verify site default has switched in folder management page");
        goToProjectHome();
        goToFolderManagement().goToRConfigTab();
        Assert.assertEquals("Folder is not inheriting the correct parent R configuration", SECONDARY_PARENT_LABEL, getText(Locator.id("parentConfigLabel")));
        assertRadioButtonSelected(Locator.radioButtonByNameAndValue("overrideDefault", "parent"));
    }

    @Test
    public void testNoEngines()
    {
        ConfigureReportsAndScriptsPage scripts = ConfigureReportsAndScriptsPage.beginAt(this);

        log("Delete all R engine definitions");
        scripts.deleteAllREngines();

        log("Verify user is notified of lack of engines in folder management page");
        goToProjectHome();
        goToFolderManagement().goToRConfigTab();
        assertElementPresent(Locator.tagWithText("h4", "No Available R Configurations"));
    }

    @Override
    protected @Nullable String getProjectName()
    {
        return "RConfigTestProject";
    }

    @Override
    public List<String> getAssociatedModules()
    {
        return null;
    }
}
