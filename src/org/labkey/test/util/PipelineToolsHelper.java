/*
 * Copyright (c) 2014-2016 LabKey Corporation
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

import org.jetbrains.annotations.NotNull;
import org.labkey.test.LabKeySiteWrapper;
import org.labkey.test.Locator;
import org.labkey.test.TestProperties;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

import static java.io.File.pathSeparator;
import static org.labkey.test.TestFileUtils.getLabKeyRoot;

public class PipelineToolsHelper
{
    private LabKeySiteWrapper _test;
    private static Set<String> _originalToolsDirs = null;
    private static Set<String> _currentToolsDirs = null;
    private static final String _defaultToolsDirectory = new File(getLabKeyRoot(), "build/deploy/bin").getAbsoluteFile().toString();
    private static final String _extraPipelineTools = TestProperties.getAdditionalPipelineTools();

    public PipelineToolsHelper(LabKeySiteWrapper test)
    {
        _test = test;
    }

    private static String getDefaultToolsPath()
    {
        return (_extraPipelineTools != null && !_extraPipelineTools.isEmpty() ? _extraPipelineTools + pathSeparator : "") + _defaultToolsDirectory;
    }

    private static Set<String> dirsFromPath(String path)
    {
        final LinkedHashSet<String> dirs = new LinkedHashSet<>(Arrays.asList(path.split(pathSeparator)));
        dirs.remove("");
        dirs.remove(null);
        return dirs;
    }

    private static String pathFromDirs(Collection<String> dirs)
    {
        dirs = new LinkedHashSet<>(dirs);
        dirs.remove("");
        dirs.remove(null);
        return String.join(pathSeparator, dirs);
    }

    @LogMethod
    public void setPipelineToolsDirectory(@LoggedParam @NotNull String path)
    {
        final Set<String> dirs = dirsFromPath(path);
        if (dirs.equals(_currentToolsDirs))
        {
            _test.log("Pipeline tools directory already has desired setting.");
            return;
        }

        _test.log("Set tools bin directory to " + path);
        _test.pushLocation();
        goToSiteSettings();
        if (_originalToolsDirs == null)
            _originalToolsDirs = dirsFromPath(_test.getFormElement(Locator.name("pipelineToolsDirectory")));
        _test.setFormElement(Locators.pipelineToolsDirectoryField(), path);
        _currentToolsDirs = dirs;
        _test.clickButton("Save");
        _test.popLocation();
    }

    @LogMethod
    public void addToPipelineToolsPath(@LoggedParam String... directories)
    {
        Set<String> directoriesToAppend = new LinkedHashSet<>(Arrays.asList(directories));
        directoriesToAppend.remove("");
        directoriesToAppend.remove(null);
        if (directoriesToAppend.isEmpty() || _currentToolsDirs != null && _currentToolsDirs.containsAll(directoriesToAppend))
        {
            TestLogger.log("No changes needed");
            return;
        }

        _test.pushLocation();
        goToSiteSettings();

        Set<String> toolsDirectories = dirsFromPath(_test.getFormElement(Locator.name("pipelineToolsDirectory")));
        if (_originalToolsDirs == null)
            _originalToolsDirs = toolsDirectories;

        if (toolsDirectories.addAll(directoriesToAppend))
        {
            _currentToolsDirs = toolsDirectories;
            final String path = pathFromDirs(toolsDirectories);
            TestLogger.log("New pipeline tools config: " + path);
            _test.setFormElement(Locators.pipelineToolsDirectoryField(), path);
            _test.clickButton("Save");
        }
        else
        {
            _currentToolsDirs = toolsDirectories;
            TestLogger.log("All directories are already present in pipeline tools config");
        }
        _test.popLocation();
    }

    /**
     * Reset pipeline tools directory to pre-test state
     */
    @LogMethod
    public void resetPipelineToolsDirectory()
    {
        if (_originalToolsDirs != null)
        {
            if (TestProperties.isTestRunningOnTeamCity())
                setToolsDirToTestDefault();
            else
                setPipelineToolsDirectory(pathFromDirs(_originalToolsDirs));
            _originalToolsDirs = null;
        }
    }

    /**
     * Set pipeline tools directory to the default location for testing.
     */
    @LogMethod
    public void setToolsDirToTestDefault()
    {
        setPipelineToolsDirectory(getDefaultToolsPath());
        if (TestProperties.isTestRunningOnTeamCity())
            _originalToolsDirs = null;
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
