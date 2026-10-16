/*
 * Copyright (c) 2014-2026 LabKey Corporation
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
import org.labkey.remoteapi.CommandException;
import org.labkey.test.LabKeySiteWrapper;
import org.labkey.test.TestFileUtils;
import org.labkey.test.TestProperties;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static java.io.File.pathSeparator;

public class PipelineToolsHelper
{
    private final LabKeySiteWrapper _test;
    private static final String _defaultToolsDirectory = new File(TestFileUtils.getModulesDir().getParentFile(), "bin").getAbsoluteFile().toString();
    private static final String _extraPipelineTools = TestProperties.getAdditionalPipelineTools();
    private static Set<String> _originalToolsDirs = null;
    private static Set<String> _currentToolsDirs = Set.of(_defaultToolsDirectory); // Presume current tools dir is just the default dir
    private static final String PIPELINE_TOOLS_DIRECTORY = "pipelineToolsDirectory";

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
        if (_originalToolsDirs == null)
            _originalToolsDirs = dirsFromPath(getToolsPath());
        saveToolsPath(path);
        _currentToolsDirs = dirs;
    }

    @LogMethod
    public void addToPipelineToolsPath(@LoggedParam String... directories)
    {
        Set<String> directoriesToAppend = new LinkedHashSet<>(Arrays.asList(directories));
        directoriesToAppend.remove("");
        directoriesToAppend.remove(null);
        if (directoriesToAppend.isEmpty() || _currentToolsDirs.containsAll(directoriesToAppend))
        {
            TestLogger.log("No changes needed");
            return;
        }

        Set<String> toolsDirectories = dirsFromPath(getToolsPath());
        if (_originalToolsDirs == null)
            _originalToolsDirs = toolsDirectories;

        if (toolsDirectories.addAll(directoriesToAppend))
        {
            _currentToolsDirs = toolsDirectories;
            final String path = pathFromDirs(toolsDirectories);
            TestLogger.log("New pipeline tools config: " + path);
            saveToolsPath(path);
        }
        else
        {
            _currentToolsDirs = toolsDirectories;
            TestLogger.log("All directories are already present in pipeline tools config");
        }
    }

    /**
     * Reset pipeline tools directory to pre-test state
     */
    @LogMethod
    public void resetPipelineToolsDirectory()
    {
        if (TestProperties.isTestRunningOnTeamCity())
            setToolsDirToTestDefault();
        else if (_originalToolsDirs != null)
            setPipelineToolsDirectory(pathFromDirs(_originalToolsDirs));
    }

    /**
     * Set pipeline tools directory to the default location for testing.
     */
    @LogMethod
    public void setToolsDirToTestDefault()
    {
        setPipelineToolsDirectory(getDefaultToolsPath());
        if (TestProperties.isTestRunningOnTeamCity())
            _originalToolsDirs = _currentToolsDirs; // Don't remove 'additional.pipeline.tools' between tests
    }

    private static String getToolsPath()
    {
        return Objects.toString(new SiteSettingsHelper().getSetting(PIPELINE_TOOLS_DIRECTORY), "");
    }

    private static void saveToolsPath(String path)
    {
        try
        {
            new SiteSettingsHelper().updateSettings(Map.of(PIPELINE_TOOLS_DIRECTORY, path));
        }
        catch (IOException | CommandException e)
        {
            throw new RuntimeException("Failed to set pipeline tools directory", e);
        }
    }
}
