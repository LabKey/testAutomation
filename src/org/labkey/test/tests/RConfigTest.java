package org.labkey.test.tests;

import org.jetbrains.annotations.Nullable;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.TestTimeoutException;
import org.labkey.test.pages.ConfigureReportsAndScriptsPage;
import org.labkey.test.pages.ConfigureReportsAndScriptsPage.EngineConfig;
import org.labkey.test.pages.ConfigureReportsAndScriptsPage.EngineType;
import org.labkey.test.pages.core.admin.ShowAdminPage;
import org.labkey.test.util.Ext4Helper;
import org.labkey.test.util.LogMethod;
import org.labkey.test.util.RReportHelper;

import java.util.List;

public class RConfigTest extends BaseWebDriverTest
{
    private String DEFAULT_ENGINE_NAME = "R Scripting Engine";
    private String SECONDARY_ENGINE_NAME = "R Scripting Engine 2";
    private String FOLDER_NAME = "subfolder";

    private RReportHelper _RReportHelper = new RReportHelper(this);
    private Ext4Helper _Ext4Helper = new Ext4Helper(this);

    @Override
    protected void doCleanup(boolean afterTest) throws TestTimeoutException
    {
        log("Delete secondary engine if exists");
        goToAdminConsole().goToAdminConsoleLinksSection();
        ConfigureReportsAndScriptsPage scripts = ShowAdminPage.beginAt(getCurrentTest()).clickViewsAndScripting();
        if (scripts.isEnginePresent(SECONDARY_ENGINE_NAME))
        {
            scripts.deleteEngine(SECONDARY_ENGINE_NAME);
        }

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
        testAddMultipleEngine();
        testEngineIsInherited();
    }

    @LogMethod
    private void testAddMultipleEngine()
    {
        log("Navigate to 'Views and Scripting' admin console page");
        goToAdminConsole().goToAdminConsoleLinksSection();
        ConfigureReportsAndScriptsPage scripts = ShowAdminPage.beginAt(getCurrentTest()).clickViewsAndScripting();

        log("Configure secondary R engine");
        EngineConfig config = new EngineConfig(_RReportHelper.getRExecutable());
        config.setName(SECONDARY_ENGINE_NAME);
        scripts.addEngine(EngineType.R, config);
        // TODO: add alert for when configuration is changed

        log("Verify default and secondary R engines exist");
        Assert.assertTrue("Default R engine does not exist", scripts.isEnginePresent(DEFAULT_ENGINE_NAME));
        Assert.assertTrue("Secondary R engine was not created properly", scripts.isEnginePresent(SECONDARY_ENGINE_NAME));
    }

    @LogMethod
    private void testEngineIsInherited()
    {
        goToProjectHome();
        goToFolderManagement().goToRConfigTab();

        log("Verify folder is inheriting the correct parent R configuration");
        Assert.assertEquals("Folder is not inheriting the correct parent R configuration", DEFAULT_ENGINE_NAME, getText(Locator.id("parentConfig")));

        // TODO: verify enable/disable state of save button

        log("Set folder level R configuration override");
        checkCheckbox(Locator.checkboxById("overrideInput"));
        selectOptionByText(Locator.name("engineRowId"), SECONDARY_ENGINE_NAME);
        clickButton("Save", "Override Default R Configuration");
        clickButton("Yes");

        log("Verify subfolder is inheriting the correct parent R configuration");
        navigateToFolder(getProjectName(), FOLDER_NAME);
        goToFolderManagement().goToRConfigTab();
        Assert.assertEquals("Subfolder is not inheriting the correct parent R configuration", SECONDARY_ENGINE_NAME, getText(Locator.id("parentConfig")));
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
