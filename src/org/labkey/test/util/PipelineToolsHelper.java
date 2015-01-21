/*
 * Copyright (c) 2014-2015 LabKey Corporation
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
package org.labkey.test.util;

import org.apache.commons.lang3.StringUtils;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.TestProperties;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.labkey.test.TestFileUtils.getLabKeyRoot;

public class PipelineToolsHelper
{
    private BaseWebDriverTest _test;
    private static String _originalToolsDir = null;
    private static String _currentToolsDir = null;
    private static String _pathSeparator = File.pathSeparator;
    private static final String _defaultToolsDirectory = (new File(getLabKeyRoot() + "/build/deploy/bin")).getAbsoluteFile().toString();
    private static final String _extraPipelineTools = TestProperties.getAdditionalPipelineTools();

    public PipelineToolsHelper(BaseWebDriverTest test)
    {
        _test = test;
    }

    public static void setPathSeparator(String pathSeparator)
    {
        _pathSeparator = pathSeparator;
    }

    private static String getDefaultToolsPath()
    {
        return (_extraPipelineTools != null ? _extraPipelineTools + _pathSeparator : "") + _defaultToolsDirectory;
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

    @LogMethod
    public void addToPipelineToolsPath(@LoggedParam String... directories)
    {
        List<String> directoriesToAppend= new ArrayList(Arrays.asList(directories));
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
            _test.log("New pipeline tools config: " + _currentToolsDir);
            _test.setFormElement(Locators.pipelineToolsDirectoryField(), _currentToolsDir);
            _test.clickButton("Save");
        }
        else
        {
            _test.log("All directories are already present in pipeline tools config");
        }
        _test.popLocation();
    }

    /**
     * Reset pipeline tools directory to pre-test state
     */
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
     * Set pipeline tools directory to the default location for testing.
     */
    @LogMethod
    public void setToolsDirToTestDefault()
    {
        setPipelineToolsDirectory(getDefaultToolsPath());
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
