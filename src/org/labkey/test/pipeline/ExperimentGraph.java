/*
 * Copyright (c) 2008-2014 LabKey Corporation
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
package org.labkey.test.pipeline;

import org.apache.commons.lang3.StringUtils;
import org.labkey.test.Locator;

import static org.junit.Assert.*;

/**
 * <code>ExperimentGraph</code>
 */
public class ExperimentGraph
{
    private static final String MAP_NAME = "graphmap";
    
    private PipelineWebTestBase _test;

    public ExperimentGraph(PipelineWebTestBase test)
    {
        _test = test;
    }

    public void clickLink(String link)
    {
        _test.clickAndWait(Locator.imageMapLinkByTitle(MAP_NAME, link));
    }

    public void clickInputLink(String input)
    {
        clickLink(getInputLinkText(input));
    }

    public void clickOutputLink(String output)
    {
        clickLink(getOutputLinkText(output));
    }

    public String getInputLinkText(String input)
    {
        return "Data: " + input;
    }

    public String getOutputLinkText(String output)
    {
        return "Data: " + output + " (Run Output)";
    }

    public boolean isNodePresent(String link)
    {
        return _test.isElementPresent(Locator.imageMapLinkByTitle(MAP_NAME, link));
    }

    public boolean isInputPresent(String input)
    {
        return isNodePresent(getInputLinkText(input));
    }

    public boolean isOutputPresent(String input)
    {
        return isNodePresent(getOutputLinkText(input));
    }

    public void assertNodePresent(String link)
    {
        assertTrue("Missing node in experiment graph " + link, isNodePresent(link));
    }

    public void assertInputPresent(String input)
    {
        assertTrue("Missing input in experiment graph " + input, isInputPresent(input));
    }

    public void assertOutputPresent(String output)
    {
        assertTrue("Missing output in experiment graph " + output, isOutputPresent(output));
    }

    public void validate(PipelineTestParams tp)
    {
        String[] names = tp.getExperimentLinks();
        for (String name : names)
        {
            if (_test.isTextPresent(name))
            {
                assertNodePresent(name);
                String baseName = getBaseName(tp);
                assertInputPresent(tp.getParametersFile());
                for (String inputExt : tp.getInputExtensions())
                    assertInputPresent(baseName + inputExt);
                for (String outputExt : tp.getOutputExtensions())
                    assertOutputPresent(baseName + outputExt);
                return;
            }
        }

        assertTrue("Unable to find experiment links: " + StringUtils.join(names, ", "), false);
    }

    private String getBaseName(PipelineTestParams tp)
    {
        String[] sampleNames = tp.getSampleNames();
        if (sampleNames.length == 0)
        {
            // AbstractMS2SearchProtocol.getJoinedBaseName() is hard-coded to use "all"
            return "all";
        }
        else
        {
            for (String sampleName : sampleNames)
            {
                if (_test.isTextPresent(sampleName))
                    return sampleName;
            }
        }

        // Probably fail later, but simpler than checking for null return.
        return "all";
    }
}
