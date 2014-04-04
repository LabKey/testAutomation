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

import java.io.File;

/**
 * <code>PipelineTestParams</code>
 */
public interface PipelineTestParams
{
    PipelineWebTestBase getTest();
    
    String getDataPath();

    String getProtocolName();

    String getProtocolType();

    String getRunKey();

    String[] getSampleNames();

    String getParametersFile();

    String[] getInputExtensions();

    String[] getOutputExtensions();
    
    String[] getExperimentLinks();

    void setExperimentLinks(String[] links);

    PipelineFolder.MailSettings getMailSettings();
    
    void setMailSettings(PipelineFolder.MailSettings mailSettings);

    boolean isExpectError();
    
    void setExpectError(boolean expectError);

    void validate();

    void validateTrue(String message, boolean condition);

    void validateEmailEscalation(int sampleIndex);

    boolean isValid();

    void verifyClean(File rootDir);

    void clean(File rootDir);

    void startProcessing();

    void remove();

    String getExperimentRunTableName();

    void clickActionButton();
}
