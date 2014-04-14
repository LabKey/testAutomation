package org.labkey.test.util;

import org.apache.commons.lang3.StringUtils;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.TestProperties;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import static org.labkey.test.WebTestHelper.getLabKeyRoot;
import static org.labkey.test.WebTestHelper.log;

public class PipelineToolsHelper
{
    private BaseWebDriverTest _test;
    private static String _originalToolsDir = null;
    private static String _currentToolsDir = null;
    private static final File _defaultToolsDirectory = new File(getLabKeyRoot() + "/build/deploy/bin");
    private static final String _defaultToolsPath = TestProperties.getExtraPipelineToolsDirs() + File.pathSeparator + _defaultToolsDirectory.toString();
    private String _pathSeparator = File.pathSeparator;

    public PipelineToolsHelper(BaseWebDriverTest test)
    {
        _test = test;
    }

    public void setPathSeparator(String pathSeparator)
    {
        _pathSeparator = pathSeparator;
    }

    @LogMethod
    public void setPipelineToolsDirectory(@LoggedParam String path)
    {
        if (path.equals(_currentToolsDir))
        {
            _test.log("Pipeline tools directory already has desired setting.");
            return;
        }

        _test.log("Set tools bin directory to " + path);
        _test.pushLocation();
        goToSiteSettings();
        if (_originalToolsDir == null)
            _originalToolsDir = _test.getFormElement(Locator.name("pipelineToolsDirectory"));
        _test.setFormElement(Locators.pipelineToolsDirectoryField(), path);
        _currentToolsDir = path;
        _test.clickButton("Save");
        _test.popLocation();
    }

    //TODO: enable or remove when we figure out if we can change pipelineToolsDirectory can use a path
    @LogMethod
    private void addPipelineToolsDirectories(@LoggedParam String... directories)
    {
        List<String> directoriesToAppend = Arrays.asList(directories);

        _test.pushLocation();
        goToSiteSettings();
        String previousToolsDir = _test.getFormElement(Locator.name("pipelineToolsDirectory"));
        if (_originalToolsDir == null)
            _originalToolsDir = previousToolsDir;

        for (String dirInPreviousPath :  previousToolsDir.split(_pathSeparator))
        {
            if (directoriesToAppend.contains(dirInPreviousPath))
                directoriesToAppend.remove(dirInPreviousPath);
        }

        if (directoriesToAppend.size() > 0)
        {
            _currentToolsDir = previousToolsDir + _pathSeparator + StringUtils.join(directoriesToAppend, _pathSeparator);
            log("New pipeline tools config: " + _currentToolsDir);
            _test.setFormElement(Locators.pipelineToolsDirectoryField(), _currentToolsDir);
            _test.clickButton("Save");
        }
        else
        {
            log("All directories are already present in pipelin tools config");
        }
        _test.popLocation();
    }

    @LogMethod
    public void resetPipelineToolsDirectory()
    {
        if (_originalToolsDir != null)
        {
            setPipelineToolsDirectory(_originalToolsDir);
            _originalToolsDir = null;
        }
    }

    /**
     * Set pipeline tools directory to the default location if the current location does not exist.
     */
    @LogMethod
    public void fixPipelineToolsDirectory()
    {
        if (_originalToolsDir != null && goodPath(_originalToolsDir))
        {
            resetPipelineToolsDirectory();
        }

        _test.log("Ensuring pipeline tools directory points to a real directory");
        goToSiteSettings();
        String currentToolsDirectory = _test.getFormElement(Locator.name("pipelineToolsDirectory"));
        if (!goodPath(currentToolsDirectory))
        {
            _test.log("Pipeline tools directory does not exist: " + currentToolsDirectory);
            _test.log("Setting to default tools directory" + _defaultToolsDirectory);
            _test.setFormElement(Locators.pipelineToolsDirectoryField(), _defaultToolsDirectory);
            _test.clickButton("Save");
        }
    }

    private boolean goodPath(String path)
    {
        String[] splitPath = path.split(_pathSeparator);
        for (String directory : splitPath)
        {
            if (!(new File (directory)).exists())
                return false;
        }
        return true;
    }

    private void goToSiteSettings()
    {
        RelativeUrl customizeSite = new RelativeUrl("admin", "customizeSite");
        customizeSite.navigate(_test);
    }

    private static class Locators
    {
        public static Locator pipelineToolsDirectoryField()
        {
            return Locator.name("pipelineToolsDirectory");
        }
    }
}
