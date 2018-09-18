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
import org.labkey.test.pages.core.admin.ShowAdminPage;
import org.labkey.test.util.Ext4Helper;
import org.labkey.test.util.RReportHelper;

import java.util.ArrayList;
import java.util.List;

@Category({DailyB.class})
public class RConfigTest extends BaseWebDriverTest
{
    private String DEFAULT_ENGINE_NAME = "R Scripting Engine";
    private String SECONDARY_ENGINE_NAME = "R Scripting Engine 2";
    private String DISABLED_ENGINE_NAME = "Disabled Engine";

    private String FOLDER_NAME = "subfolder";

    private RReportHelper _RReportHelper = new RReportHelper(this);
    private Ext4Helper _Ext4Helper = new Ext4Helper(this);

    @Override
    protected void doCleanup(boolean afterTest) throws TestTimeoutException
    {
        goToAdminConsole().goToAdminConsoleLinksSection();
        ConfigureReportsAndScriptsPage scripts = ShowAdminPage.beginAt(getCurrentTest()).clickViewsAndScripting();

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
        log("Navigate to 'Views and Scripting' admin console page");
        goToAdminConsole().goToAdminConsoleLinksSection();
        ConfigureReportsAndScriptsPage scripts = ShowAdminPage.beginAt(getCurrentTest()).clickViewsAndScripting();

        EngineConfig config = new EngineConfig(_RReportHelper.getRExecutable());

        if (!scripts.isEnginePresent(DEFAULT_ENGINE_NAME))
        {
            _RReportHelper.ensureRConfig(false);
        }

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
            Assert.assertTrue(_Ext4Helper.isChecked(XPathLocator.id("editEngine_enabled-inputEl")));
            _Ext4Helper.uncheckCheckbox(XPathLocator.id("editEngine_enabled-inputEl"));
            sleep(2000); // wait for store update
            clickButton("Submit", -1);
        }
    }

    @Test
    public void testAddMultipleEngines()
    {
        log("Navigate to 'Views and Scripting' admin console page");
        goToAdminConsole().goToAdminConsoleLinksSection();
        ConfigureReportsAndScriptsPage scripts = ShowAdminPage.beginAt(getCurrentTest()).clickViewsAndScripting();

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

    @Test
    public void testSwitchDefaultEngine()
    {
        log("Navigate to 'Views and Scripting' admin console page");
        goToAdminConsole().goToAdminConsoleLinksSection();
        ConfigureReportsAndScriptsPage scripts = ShowAdminPage.beginAt(getCurrentTest()).clickViewsAndScripting();

        log("Verify site default checkbox is checked and disabled");
        scripts.editEngine(DEFAULT_ENGINE_NAME);
        Assert.assertTrue(_Ext4Helper.isChecked(XPathLocator.id("editEngine_default-inputEl")));
        assertAttributeEquals(XPathLocator.id("editEngine_default-inputEl"), "disabled", "true");

        log("Disabling site default should be prevented");
        Assert.assertTrue(_Ext4Helper.isChecked(XPathLocator.id("editEngine_enabled-inputEl")));
        _Ext4Helper.uncheckCheckbox(XPathLocator.id("editEngine_enabled-inputEl"));
        clickButton("Submit", -1);
        assertExt4MsgBox("Site default engine must be enabled.", "OK");
        clickButton("Cancel", -1);

        log("Switch site default");
        scripts.editEngine(SECONDARY_ENGINE_NAME);
        XPathLocator loc = XPathLocator.id("editEngine_default-inputEl");
        Assert.assertFalse(_Ext4Helper.isChecked(XPathLocator.id("editEngine_default-inputEl")));
        _Ext4Helper.checkCheckbox(XPathLocator.id("editEngine_default-inputEl"));
        click(Locator.linkWithText("Submit"));
        acceptAlert();
        _Ext4Helper.waitForMaskToDisappear();

        log("Verify site default has switched in admin console page");
        scripts.editEngine(DEFAULT_ENGINE_NAME);
        Assert.assertFalse(_Ext4Helper.isChecked(XPathLocator.id("editEngine_default-inputEl")));
        clickButton("Cancel", -1);
        scripts.editEngine(SECONDARY_ENGINE_NAME);
        Assert.assertTrue(_Ext4Helper.isChecked(XPathLocator.id("editEngine_default-inputEl")));
        clickButton("Cancel", -1);

        log("Verify site default has switched in folder management page");
        goToProjectHome();
        goToFolderManagement().goToRConfigTab();
        Assert.assertEquals("Folder is not inheriting the correct parent R configuration", SECONDARY_ENGINE_NAME, getText(Locator.id("parentConfig")));
        assertRadioButtonSelected(Locator.radioButtonByNameAndValue("overrideDefault", "parent"));
    }

    @Test
    public void testNoEngines()
    {
        log("Navigate to 'Views and Scripting' admin console page");
        goToAdminConsole().goToAdminConsoleLinksSection();
        ConfigureReportsAndScriptsPage scripts = ShowAdminPage.beginAt(getCurrentTest()).clickViewsAndScripting();

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
