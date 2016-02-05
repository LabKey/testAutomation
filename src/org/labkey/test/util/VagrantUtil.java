/*
 * Copyright (c) 2016 LabKey Corporation
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
import java.util.concurrent.TimeoutException;

/**
 * Created by RyanS on 1/25/2016.
 */
public class VagrantUtil
{
    private final String _dirPath;
    private final String _imageName;

    public VagrantUtil(String vagrantDirPath, String imageName)
    {
        _dirPath = vagrantDirPath;
        _imageName = imageName;
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
        command(cmd, 900000);
    }

    private void command(List<String> cmd, long timeOut) throws IOException, TimeoutException
    {
        String reconstructedCommand = "";
        for (String c : cmd)
        {
            reconstructedCommand = reconstructedCommand + c + " ";
        }
        TestLogger.log("Running command " + reconstructedCommand + "in directory " + _dirPath);
        long startTime = System.currentTimeMillis();
        try
        {
            ProcessBuilder pb = new ProcessBuilder();
            pb.command(cmd);
            pb.directory(new File(_dirPath));
            pb.redirectErrorStream(true);
            Process p = pb.start();
            BufferedReader stdInput = new BufferedReader(new InputStreamReader(p.getInputStream()));
            List<String> out = new ArrayList<>();
            //wait for console output to cease
            String outLine;
            while ((outLine = stdInput.readLine()) != null)
            {
                long elapsed = System.currentTimeMillis()-startTime;
                if(elapsed > timeOut) throw new TimeoutException("Timed out executing vagrant command " + reconstructedCommand + " in directory " + _dirPath);
                out.add(outLine);
            }
            processOutput(out, p.exitValue());
        }
        catch (IOException | TimeoutException e)
        {
            TestLogger.log(e.getMessage());
            throw e;
        }
        long totalSecs = (System.currentTimeMillis()-startTime)/1000;
        TestLogger.log("command " + reconstructedCommand + "in directory " + _dirPath + "completed in " + totalSecs + " seconds");
    }

    public void consoleCommand(String command, String expected)
    {

    }

    private void processOutput(List<String> output, int returnCode)
    {
        if(returnCode != 0)
        {
            TestLogger.log("unexpected exit code " + returnCode);
            for (int i = output.size() - 10; i < output.size(); i++)
            {
                if(i > 0)
                TestLogger.log(output.get(i));
            }
        }
        else
        {
            if(output.size() > 0)
            TestLogger.log(output.get(output.size() -1));
        }
    }
}
