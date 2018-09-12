package org.labkey.test.tests;

import org.jetbrains.annotations.Nullable;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.Locator.XPathLocator;
import org.labkey.test.TestProperties;
import org.labkey.test.TestTimeoutException;
import org.labkey.test.categories.DailyB;
import org.labkey.test.pages.ConfigureReportsAndScriptsPage;
import org.labkey.test.pages.ConfigureReportsAndScriptsPage.EngineConfig;
import org.labkey.test.pages.ConfigureReportsAndScriptsPage.EngineType;
import org.labkey.test.pages.core.admin.ShowAdminPage;
import org.labkey.test.util.Ext4Helper;
import org.labkey.test.util.LogMethod;
import org.labkey.test.util.RReportHelper;

import java.util.ArrayList;
import java.util.List;

@Category({DailyB.class})
public class RConfigTest extends BaseWebDriverTest
{
    private String DEFAULT_ENGINE_NAME = "R Scripting Engine";
    private String SECONDARY_ENGINE_NAME = "R Scripting Engine 2";
    private String DISABLED_ENGINE_NAME = "Disabled Engine";
    private List<String> R_ENGINES = new ArrayList<String>() {{
        add(DEFAULT_ENGINE_NAME);
        add(SECONDARY_ENGINE_NAME);
        add(DISABLED_ENGINE_NAME);
    }};

    private String FOLDER_NAME = "subfolder";

    private XPathLocator DEFAULT_CHECKBOX = XPathLocator.id("editEngine_default-inputEl");
    private XPathLocator ENABLED_CHECKBOX = XPathLocator.id("editEngine_enabled-inputEl");

    private RReportHelper _RReportHelper = new RReportHelper(this);
    private Ext4Helper _Ext4Helper = new Ext4Helper(this);

    @Override
    protected void doCleanup(boolean afterTest) throws TestTimeoutException
    {
        goToAdminConsole().goToAdminConsoleLinksSection();
        ConfigureReportsAndScriptsPage scripts = ShowAdminPage.beginAt(getCurrentTest()).clickViewsAndScripting();

        log("Clean up scripting engines");
        scripts.deleteEnginesFromList(R_ENGINES);

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
        _RReportHelper.ensureRConfig(false);

        _containerHelper.createProject(getProjectName(), "Custom");
        _containerHelper.createSubfolder(getProjectName(), FOLDER_NAME);
    }

    @Test
    public void testSteps() throws Exception
    {
        testAddMultipleEngines();
        testEngineIsInherited();
        testSwitchDefaultEngine();
        testNoEngines();
    }

    @LogMethod
    private void testAddMultipleEngines()
    {
        log("Navigate to 'Views and Scripting' admin console page");
        goToAdminConsole().goToAdminConsoleLinksSection();
        ConfigureReportsAndScriptsPage scripts = ShowAdminPage.beginAt(getCurrentTest()).clickViewsAndScripting();

        log("Configure secondary R engine");
        EngineConfig config = new EngineConfig(_RReportHelper.getRExecutable());
        config.setName(SECONDARY_ENGINE_NAME);
        scripts.addEngine(EngineType.R, config);

        log("Configure and disable R engine");
        config.setName(DISABLED_ENGINE_NAME);
        scripts.addEngine(EngineType.R, config);
        scripts.editEngine(DISABLED_ENGINE_NAME);
        Assert.assertTrue(_Ext4Helper.isChecked(ENABLED_CHECKBOX));
        _Ext4Helper.uncheckCheckbox(ENABLED_CHECKBOX);
        clickButton("Submit", -1);

        log("Verify R engines exist");
        Assert.assertTrue("Default R engine does not exist", scripts.isEnginePresent(DEFAULT_ENGINE_NAME));
        Assert.assertTrue("Secondary R engine was not created properly", scripts.isEnginePresent(SECONDARY_ENGINE_NAME));
        Assert.assertTrue("Disabled R engine was not created properly", scripts.isEnginePresent(DISABLED_ENGINE_NAME));
    }

    @LogMethod
    private void testEngineIsInherited()
    {
        log("Navigate to 'R Config' tab in folder management page");
        goToProjectHome();
        goToFolderManagement().goToRConfigTab();

        log("Verify folder is inheriting the correct parent R configuration");
        Assert.assertEquals("Folder is not inheriting the correct parent R configuration", DEFAULT_ENGINE_NAME, getText(Locator.id("parentConfig")));
        assertRadioButtonSelected(Locator.radioButtonByNameAndValue("overrideDefault", "parent"));
        assertAttributeContains(Locator.id("saveBtn"), "class", "labkey-disabled-button");

        log("Verify only enabled, non-default engines are listed in override dropdown");
        assertElementPresent(Locator.xpath("//select/option[normalize-space(text())=\"" + SECONDARY_ENGINE_NAME + "\"]"));
        assertElementNotPresent("Default engine should not be listed in override dropdown", Locator.xpath("select/option[normalize-space(text())=\"" + DEFAULT_ENGINE_NAME + "\"]\n"));
        assertElementNotPresent("Disabled engine should not be listed in override dropdown", Locator.xpath("select/option[normalize-space(text())=\"" + DISABLED_ENGINE_NAME + "\"]\n"));

        log("Set folder level R configuration override");
        checkRadioButton(Locator.radioButtonByNameAndValue("overrideDefault", "override"));
        assertAttributeContains(Locator.id("saveBtn"), "class", "labkey-disabled-button");
        selectOptionByText(Locator.name("engineRowId"), SECONDARY_ENGINE_NAME);
        assertAttributeNotContains(Locator.id("saveBtn"), "class", "labkey-disabled-button");
        clickButton("Save", "Override Default R Configuration");
        clickButton("Yes");

        log("Verify subfolder is inheriting the correct parent R configuration");
        navigateToFolder(getProjectName(), FOLDER_NAME);
        goToFolderManagement().goToRConfigTab();
        Assert.assertEquals("Subfolder is not inheriting the correct parent R configuration", SECONDARY_ENGINE_NAME, getText(Locator.id("parentConfig")));
    }

    @LogMethod
    private void testSwitchDefaultEngine()
    {
        log("Navigate to 'Views and Scripting' admin console page");
        goToAdminConsole().goToAdminConsoleLinksSection();
        ConfigureReportsAndScriptsPage scripts = ShowAdminPage.beginAt(getCurrentTest()).clickViewsAndScripting();

        log("Verify site default checkbox is checked and disabled");
        scripts.editEngine(DEFAULT_ENGINE_NAME);
        Assert.assertTrue(_Ext4Helper.isChecked(DEFAULT_CHECKBOX));
        assertAttributeEquals(DEFAULT_CHECKBOX, "disabled", "true");

        log("Disabling site default should be prevented");
        Assert.assertTrue(_Ext4Helper.isChecked(ENABLED_CHECKBOX));
        _Ext4Helper.uncheckCheckbox(ENABLED_CHECKBOX);
        clickButton("Submit", -1);
        assertExt4MsgBox("Site default engine must be enabled.", "OK");
        clickButton("Cancel", -1);

        log("Switch site default");
        scripts.editEngine(SECONDARY_ENGINE_NAME);
        Assert.assertFalse(_Ext4Helper.isChecked(DEFAULT_CHECKBOX));
        _Ext4Helper.checkCheckbox(DEFAULT_CHECKBOX);
        click(Locator.linkWithText("Submit"));
        acceptAlert();
        _Ext4Helper.waitForMaskToDisappear();

        log("Verify site default has switched in admin console page");
        scripts.editEngine(DEFAULT_ENGINE_NAME);
        Assert.assertFalse(_Ext4Helper.isChecked(DEFAULT_CHECKBOX));
        clickButton("Cancel", -1);
        scripts.editEngine(SECONDARY_ENGINE_NAME);
        Assert.assertTrue(_Ext4Helper.isChecked(DEFAULT_CHECKBOX));
        clickButton("Cancel", -1);

        log("Verify site default has switched in folder management page");
        goToProjectHome();
        goToFolderManagement().goToRConfigTab();
        Assert.assertEquals("Folder is not inheriting the correct parent R configuration", SECONDARY_ENGINE_NAME, getText(Locator.id("parentConfig")));
        assertRadioButtonSelected(Locator.radioButtonByNameAndValue("overrideDefault", "parent"));
    }

    @LogMethod
    private void testNoEngines()
    {
        // Only run test in TeamCity to prevent deleting local R instances
        if (TestProperties.isTestRunningOnTeamCity())
        {
            log("Navigate to 'Views and Scripting' admin console page");
            goToAdminConsole().goToAdminConsoleLinksSection();
            ConfigureReportsAndScriptsPage scripts = ShowAdminPage.beginAt(getCurrentTest()).clickViewsAndScripting();

            log("Delete all R engine definitions");
            scripts.deleteEnginesForLanguage("R");

            log("Verify user is notified of lack of engines in folder management page");
            goToProjectHome();
            goToFolderManagement().goToRConfigTab();
            assertElementPresent(Locator.tagWithText("h4", "No Available R Configurations"));
        }
    }

    private void resetDefaultConfiguration(ConfigureReportsAndScriptsPage scripts)
    {
        resetDefaultConfiguration(scripts, DEFAULT_ENGINE_NAME);
    }

    private void resetDefaultConfiguration(ConfigureReportsAndScriptsPage scripts, String engineName)
    {
        if(scripts.isEnginePresent(engineName))
        {
            scripts.editEngine(engineName);
            if(!_Ext4Helper.isChecked(DEFAULT_CHECKBOX))
            {
                _Ext4Helper.checkCheckbox(DEFAULT_CHECKBOX);
                click(Locator.linkWithText("Submit"));
                acceptAlert();
                _Ext4Helper.waitForMaskToDisappear();
            }
            else
            {
                clickButton("Cancel", -1);
            }
        }
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
