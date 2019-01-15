package org.labkey.test.util;

/*
 * Copyright (c) 2015-2018 LabKey Corporation
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

import org.apache.commons.lang3.ArrayUtils;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.TestFileUtils;
import org.labkey.test.TestProperties;
import org.labkey.test.pages.ConfigureReportsAndScriptsPage;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PythonHelper
{
    private String pythonHomeEnv = "PYTHON_HOME";
    private final BaseWebDriverTest _test;
    private File pythonExecutable = null;

    public PythonHelper(BaseWebDriverTest test)
    {
        _test = test;
    }

    public void selectPython2()
    {
        setPythonHomeEnv("PYTHON2_HOME");
    }

    public void selectPython3()
    {
        setPythonHomeEnv("PYTHON3_HOME");
    }

    public void setPythonHomeEnv(String pythonHomeEnv)
    {
        if (!this.pythonHomeEnv.equals(pythonHomeEnv))
        {
            this.pythonHomeEnv = pythonHomeEnv;
            pythonExecutable = null;
        }
    }

    public String executePythonScript(File scriptFile, String... args)
    {
        try
        {
            return TestFileUtils.getProcessOutput(getPythonExecutable(), ArrayUtils.addAll(new String[]{scriptFile.getAbsolutePath()}, args));
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }

    @LogMethod
    public String ensurePythonConfig()
    {
        _test.log("Check if Python already is configured");

        ConfigureReportsAndScriptsPage scripts = ConfigureReportsAndScriptsPage.beginAt(_test);

        String defaultScriptName = "Python Scripting Engine";
        if (scripts.isEnginePresent("Python"))
        {
            _test.log("Python engine already configured");
            if (!TestProperties.isTestRunningOnTeamCity())
            {
                scripts.editEngine(defaultScriptName);
                pythonExecutable = new File(_test.getFormElement(Locator.id("editEngine_exePath-inputEl")));
                return getPythonVersion(pythonExecutable);
            }
            else // Reset Python scripting engine on TeamCity
                scripts.deleteEngine(defaultScriptName);
        }

        String pythonVersion = getPythonVersion(getPythonExecutable());

        ConfigureReportsAndScriptsPage.EngineConfig config = new ConfigureReportsAndScriptsPage.EngineConfig(getPythonExecutable());
        config.setLanguage("Python");
        config.setExtensions("py");
        config.setVersion(pythonVersion);
        scripts.addEngine(ConfigureReportsAndScriptsPage.EngineType.EXTERNAL, config);

        return pythonVersion;
    }

    private File getPythonExecutable()
    {
        if (pythonExecutable != null)
            return pythonExecutable;

        String pythonHome = System.getenv(pythonHomeEnv);
        if (pythonHome != null)
        {
            _test.log(pythonHomeEnv + " is set to: " + pythonHome + " searching for the Python application");
            File pythonHomeDir = new File(pythonHome);
            FileFilter pythonFilenameFilter = new FileFilter()
            {
                public boolean accept(File file)
                {
                    return ("python.exe".equalsIgnoreCase(file.getName()) || "python".equalsIgnoreCase(file.getName())) && file.canExecute();
                }
            };
            File[] files = pythonHomeDir.listFiles(pythonFilenameFilter);

            if (files == null || files.length == 0)
            {
                files = new File(pythonHome, "bin").listFiles(pythonFilenameFilter);
            }

            if (files != null)
            {
                if (files.length == 1)
                {
                    pythonExecutable = files[0];
                    return pythonExecutable;
                }
                else if (files.length > 1)
                {
                    _test.log("Found too many Python executables:");
                    for (File file : files)
                    {
                        _test.log("\t" + file.getAbsolutePath());
                    }
                }
            }
        }

        _test.log("Environment info: " + System.getenv());

        if (null == pythonHome)
        {
            _test.log("");   // Blank line helps make the following message more readable
            _test.log(pythonHomeEnv + " environment variable is not set.  Set " + pythonHomeEnv + " to your Python bin directory to enable automatic configuration.");
        }
        throw new RuntimeException("Python is not configured on this system.");
    }

    private String getPythonVersion(File python)
    {
        String versionOutput = "";
        try
        {
            versionOutput = TestFileUtils.getProcessOutput(python, "--version");

            Pattern versionPattern = Pattern.compile("Python ([1-9]\\.\\d+\\.\\d)");
            Matcher matcher = versionPattern.matcher(versionOutput);
            matcher.find();
            String versionNumber = matcher.group(1);

            _test.log("python --version > " + versionOutput);

            return versionNumber;
        }
        catch(IOException ex)
        {
            if (versionOutput.length() > 0) _test.log("python --version > " + versionOutput);
            throw new RuntimeException("Unable to determine python version: " + python.getAbsolutePath(), ex);
        }
    }
}
