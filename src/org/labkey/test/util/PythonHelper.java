package org.labkey.test.util;

/*
 * Copyright (c) 2015-2019 LabKey Corporation
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
import org.hamcrest.CoreMatchers;
import org.hamcrest.MatcherAssert;
import org.jetbrains.annotations.NotNull;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.TestProperties;
import org.labkey.test.pages.ConfigureReportsAndScriptsPage;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PythonHelper
{
    private static final String PYENV_KEY = "PYENV_VERSION";

    private final BaseWebDriverTest _test;
    private File pythonExecutable = null;
    private Duration defaultScriptTimeout = null;

    public PythonHelper(BaseWebDriverTest test)
    {
        _test = test;
    }

    public String executePythonScript(File scriptFile, String... args)
    {
        try
        {
            String[] scriptAndArgs = ArrayUtils.addAll(new String[]{scriptFile.getAbsolutePath()}, args);
            ProcessHelper processHelper = new ProcessHelper(getPythonExecutable(), scriptAndArgs);
            applyPyenv(processHelper);
            if (defaultScriptTimeout != null)
                processHelper.setTimeout(defaultScriptTimeout);
            return processHelper.getProcessOutput(true).trim();
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }

    private ProcessHelper applyPyenv(@NotNull ProcessHelper processHelper)
    {
        if (System.getenv().containsKey(PYENV_KEY))
        {
            processHelper.environment().put(PYENV_KEY, System.getenv(PYENV_KEY));
        }
        return processHelper;
    }

    public PythonHelper setDefaultScriptTimeout(Duration defaultScriptTimeout)
    {
        this.defaultScriptTimeout = defaultScriptTimeout;
        return this;
    }

    @LogMethod
    public String ensurePythonConfig()
    {
        ConfigureReportsAndScriptsPage scripts = ConfigureReportsAndScriptsPage.beginAt(_test);

        String defaultScriptName = "Python Scripting Engine";
        if (scripts.isEnginePresent("Python"))
        {
            TestLogger.log("Python engine already configured");
            if (!TestProperties.isTestRunningOnTeamCity())
            {
                scripts.editEngine(defaultScriptName);
                pythonExecutable = new File(_test.getFormElement(Locator.id("editEngine_exePath-inputEl")));
                TestLogger.log("Using existing Python engine: " + pythonExecutable.getAbsolutePath());
                return assertPythonVersion(pythonExecutable);
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

    protected String getVersionPrefix()
    {
        return "2.";
    }

    protected String getPythonExeEnv()
    {
        return "PYTHON";
    }

    protected String getPythonExeName()
    {
        return "python";
    }

    private String assertPythonVersion(File pythonExecutable)
    {
        String pythonVersion = getPythonVersion(pythonExecutable);
        MatcherAssert.assertThat("Unwanted Python version: " + pythonExecutable.getAbsolutePath() + "\nSet '" + getPythonExeEnv() + "' environment variable to the appropriate python executable",
                pythonVersion, CoreMatchers.startsWith(getVersionPrefix()));
        return pythonVersion;
    }

    public final File getPythonExecutable()
    {
        if (pythonExecutable == null)
        {
            String exePath = System.getenv(getPythonExeEnv());
            File exeFile;
            if (exePath == null)
                exeFile = new File("/usr/bin", getPythonExeName()); // Fallback to make it easier to run on Mac or Linux
            else
                exeFile = new File(exePath);

            if (!exeFile.exists())
            {
                TestLogger.log("Environment info: " + System.getenv());
                TestLogger.log("");   // Blank line helps make the following message more readable
                String message;
                if (exePath == null)
                    message = getPythonExeEnv() + " environment variable is not set. It should point at the appropriate python executable.";
                else
                    message = getPythonExeEnv() + "=" + exePath + ". File does not exist.";
                throw new RuntimeException(message);
            }
            assertPythonVersion(exeFile);
            pythonExecutable = exeFile;
        }
        return pythonExecutable;
    }

    private String getPythonVersion(File python)
    {
        String versionOutput = "";
        try
        {
            versionOutput = applyPyenv(new ProcessHelper(python, "--version")).getProcessOutput().trim();

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
