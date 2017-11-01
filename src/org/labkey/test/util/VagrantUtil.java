/*
 * Copyright (c) 2016-2017 LabKey Corporation
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

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class VagrantUtil
{
    private final File _dirPath;
    private final String _imageName;
    private int _commandTimeout = 900000;
    private boolean _outputPassthrough = false;

    public VagrantUtil(File vagrantDir, String imageName)
    {
        _dirPath = vagrantDir;
        _imageName = imageName;
    }

    public void setCommandTimeout(int commandTimeout)
    {
        _commandTimeout = commandTimeout;
    }

    public void setOutputPassthrough(boolean outputPassthrough)
    {
        _outputPassthrough = outputPassthrough;
    }

    public void vagrantUp() throws IOException, TimeoutException
    {
        List<String> cmd = new ArrayList<>();
        cmd.add("vagrant");
        cmd.add("up");
        command(cmd);
    }

    public void vagrantDestroy() throws IOException, TimeoutException
    {
        List<String> cmd = new ArrayList<>();
        cmd.add("vagrant");
        cmd.add("destroy");
        cmd.add(_imageName);
        cmd.add("-f");
        command(cmd);
    }

    private void command(List<String> cmd) throws IOException, TimeoutException
    {
        command(cmd, _commandTimeout);
    }

    private void command(List<String> cmd, long timeOut) throws IOException, TimeoutException
    {
        String reconstructedCommand = String.join(" ", cmd);
        long startTime = System.currentTimeMillis();
        TestLogger.log("Running command `" + reconstructedCommand + "` in directory " + _dirPath);
        TestLogger.increaseIndent();
        {
            ProcessBuilder pb = new ProcessBuilder();
            pb.command(cmd);
            pb.directory(_dirPath);
            pb.redirectErrorStream(true);
            Process p = pb.start();
            BufferedReader stdInput = new BufferedReader(new InputStreamReader(p.getInputStream()));
            List<String> out = new ArrayList<>();
            //wait for console output to cease
            String outLine;
            while ((outLine = stdInput.readLine()) != null)
            {
                long elapsed = System.currentTimeMillis() - startTime;
                if (_outputPassthrough)
                    TestLogger.log(outLine);
                out.add(outLine);
                if (elapsed > timeOut)
                {
                    processOutput(out, p);
                    throw new TimeoutException("Timed out executing vagrant command `" + reconstructedCommand + "` in directory " + _dirPath);
                }
            }
            processOutput(out, p);
        }
        TestLogger.decreaseIndent();
        long totalSecs = (System.currentTimeMillis()-startTime)/1000;
        TestLogger.log("command `" + reconstructedCommand + "` in directory \"" + _dirPath + "\" : completed in " + totalSecs + " seconds");
    }

    private void processOutput(List<String> output, Process p)
    {
        Integer returnCode = null;
        try
        {
            if(p.waitFor(30, TimeUnit.SECONDS))
            {
                returnCode = p.exitValue();
            }
        }
        catch (InterruptedException e)
        {
            throw new RuntimeException(e);
        }

        if(!new Integer(0).equals(returnCode))
        {
            if (returnCode != null)
                TestLogger.log("Unexpected exit code " + returnCode, System.err);
            else
                TestLogger.log("Process did not exit after last output", System.err);
            for (int i = output.size() - 10; i < output.size(); i++)
            {
                if(i >= 0)
                    System.err.println(output.get(i));
            }
        }
        else if (!_outputPassthrough)
        {
            if(output.size() > 0)
                TestLogger.log(output.get(output.size() -1));
        }
    }
}
