/*
 * Copyright (c) 2007-2016 LabKey Corporation
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
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.TestFileUtils;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.components.dumbster.EmailRecordTable;
import org.labkey.test.util.ExperimentRunTable;
import org.labkey.test.util.PasswordUtil;
import org.labkey.test.util.PipelineStatusTable;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

abstract public class AbstractPipelineTestParams implements PipelineTestParams
{
    protected PipelineWebTestBase _test;
    private String _dataPath;
    private String _protocolType;
    private String _parametersFile;
    private String _protocolName;
    private String[] _sampleNames;
    private String[] _inputExtensions = new String[0];
    private String[] _outputExtensions = new String[0];
    private String[] _experimentLinks;
    protected PipelineFolder.MailSettings _mailSettings;
    private boolean _expectError;    
    private boolean _valid;

    public AbstractPipelineTestParams(PipelineWebTestBase test, String dataPath,
                                      String protocolType, String protocolName, String... sampleNames)
    {
        _test = test;
        _dataPath = dataPath;
        _sampleNames = sampleNames;
        _protocolType = protocolType;
        _protocolName = protocolName;
        _valid = true;
    }

    public PipelineWebTestBase getTest()
    {
        return _test;
    }

    public String getDataPath()
    {
        return _dataPath;
    }

    public String getDataDirName()
    {
        String[] parts = StringUtils.split(_dataPath, '/');
        return parts[parts.length - 1]; 
    }

    public String getProtocolName()
    {
        return _protocolName;
    }

    public String getProtocolType()
    {
        return _protocolType;
    }

    public String[] getSampleNames()
    {
        return _sampleNames;
    }

    public String getParametersFile()
    {
        return _parametersFile != null ? _parametersFile : _protocolType + ".xml";
    }

    public void setParametersFile(String parametersFile)
    {
        _parametersFile = parametersFile;
    }

    public String[] getInputExtensions()
    {
        return _inputExtensions;
    }

    public void setInputExtensions(String... inputExtensions)
    {
        _inputExtensions = inputExtensions;
    }

    public String[] getOutputExtensions()
    {
        return _outputExtensions;
    }

    public void setOutputExtensions(String... outputExtensions)
    {
        _outputExtensions = outputExtensions;
    }

    public String getRunKey()
    {
        return _dataPath + " (" + _protocolType + "/" + _protocolName + ")";
    }

    public String getDirStatusDesciption()
    {
        return getDataDirName() + " (" + _protocolName + ")";
    }

    public String[] getExperimentLinks()
    {
        if (_experimentLinks == null)
        {
            String[] dirs = _dataPath.split("/");
            String dataDirName = dirs[dirs.length - 1];

            if (_sampleNames.length == 0)
                _experimentLinks = new String[] { dataDirName + " (" + _protocolName + ")" };
            else
            {
                ArrayList<String> listLinks = new ArrayList<>();
                for (String name : _sampleNames)
                    listLinks.add(dataDirName + '/' + name + " (" + _protocolName + ")");
                _experimentLinks = listLinks.toArray(new String[listLinks.size()]);
            }
        }
        return _experimentLinks;
    }

    public void setExperimentLinks(String[] experimentLinks)
    {
        this._experimentLinks = experimentLinks;
    }

    public PipelineFolder.MailSettings getMailSettings()
    {
        return _mailSettings;
    }

    public void setMailSettings(PipelineFolder.MailSettings mailSettings)
    {
        _mailSettings = mailSettings;
    }

    public boolean isExpectError()
    {
        return _expectError;
    }

    public void setExpectError(boolean expectError)
    {
        _expectError = expectError;
    }

    public void validate()
    {
        if (_mailSettings != null)
        {
            if (_expectError)
                validateEmailError();
            else
                validateEmailSuccess();
        }
        else
        {
            // Default fails to allow manual analysis of the run.
            // Override to do actual automated validation of the resulting
            // MS2 run data.

            validateTrue("No automated validation", false);
        }
    }

    public void validateTrue(String message, boolean condition)
    {
        if (!condition)
        {
            _test.log("INVALID: " + message);
            _valid = false;
        }
    }

    public boolean isValid()
    {
        return _valid;
    }

    public void verifyClean(File rootDir)
    {
        File analysisDir = new File(rootDir, getDataPath() + File.separatorChar + getProtocolType());
        if (analysisDir.exists())
            fail("Pipeline files were not cleaned up; "+ analysisDir.toString() + " directory still exists");
    }

    public void clean(File rootDir)
    {
        TestFileUtils.delete(new File(new File(rootDir, getDataPath()), getProtocolType()));
    }

    public void startProcessing()
    {
        _test.log("Start analysis of " + getDataPath());
        _test.clickButton("Process and Import Data");
        _test._fileBrowserHelper.selectFileBrowserItem(getDataPath()+"/");

        clickActionButton();

        int wait = BaseWebDriverTest.WAIT_FOR_JAVASCRIPT;
        _test.log("Choose existing protocol " + getProtocolName());
        _test.waitForElement(Locator.xpath("//select[@name='protocol']/option[.='" + getProtocolName() + "']" ), wait*12); // seems very long
        _test.selectOptionByText(Locator.name("protocol"), getProtocolName());
        WebDriverWrapper.sleep(wait);

        _test.log("Start data processing");
        clickSubmitButton();
        WebDriverWrapper.sleep(wait);
    }

    protected abstract void clickSubmitButton();

    public void remove()
    {
        ExperimentRunTable tableExp = getExperimentRunTable();

        for (String name : getExperimentLinks())
        {
            _test.log("Removing " + name);

            _test.pushLocation();
            tableExp.clickGraphLink(name);
            String id = _test.getURL().getQuery();
            id = id.substring(id.indexOf('=') + 1);
            _test.popLocation();

            _test.checkCheckbox(Locator.checkboxByNameAndValue(".select", id));
            _test.clickButton("Delete");
            _test.clickButton("Confirm Delete");
        }
    }

    private ExperimentRunTable getExperimentRunTable()
    {
        return new ExperimentRunTable(getExperimentRunTableName(), _test, false);
    }
    
    private void validateExperiment()
    {
        _test.clickButton("Data");
        ExperimentGraph graph = new ExperimentGraph(_test);
        graph.validate(this);
    }

    public void validateEmailSuccess()
    {
        assertNotNull("Email validation requires mail settings", _mailSettings);

        validateEmail("COMPLETE", getDirStatusDesciption(), _mailSettings.isNotifyOnSuccess(),
                _mailSettings.getNotifyUsersOnSuccess());

        if (_test.isButtonPresent("Data"))
        {
            validateExperiment();
        }
        else
        {
            int split = 1;
            while (_test.isElementPresent(Locator.linkWithText("COMPLETE").index(split)))
            {
                _test.pushLocation();
                Integer index = split++;
                _test.clickAndWait(Locator.linkWithText("COMPLETE").index(index));
                validateExperiment();
                _test.popLocation();
            }
        }
    }

    public void validateEmailError()
    {
        assertNotNull("Email validation requires mail settings", _mailSettings);

        for (String sampleExp : getExperimentLinks())
        {
            _test.pushLocation();
            validateEmail("ERROR", sampleExp, _mailSettings.isNotifyOnError(),
                    _mailSettings.getNotifyUsersOnError());
            _test.popLocation();
        }
    }

    private void validateEmail(String status, String description, boolean notifyOwner, String[] notifyOthers)
    {
        if (!notifyOwner && notifyOthers.length == 0)
            return; // No email expected.

        String ownerEmail = PasswordUtil.getUsername();
        EmailRecordTable emailTable = new EmailRecordTable(_test);
        EmailRecordTable.EmailMessage message = emailTable.getMessage(description);
        assertNotNull("No email message found for " + description, message);
        emailTable.clickMessage(message);
        // Refetch message after expanding to get entire message text
        message = emailTable.getMessage(description);
        validateTrue("The test " + description + " does not have expected status " + status,
                message.getBody().contains("Status: " + status));
        List<String> recipients = Arrays.asList(message.getTo());
        if (notifyOwner)
        {
            validateTrue("Message not sent to owner " + ownerEmail, recipients.contains(ownerEmail));
        }
        for (String notify : notifyOthers)
        {
            validateTrue("Message not sent to " + notify, recipients.contains(notify));
        }

        assertTrue("Could not find link in message with Status: '" + status + "' and Description: '" + description + "'", clickLink(message));

        // Make sure we made it to a status page.
        _test.assertTextPresent("Job Status", status);
    }

    private boolean clickLink(EmailRecordTable.EmailMessage message)
    {
        // The link in this message uses the IP address.  Avoid clicking it, and
        // possibly changing hostnames.
        for (String line : StringUtils.split(message.getBody(), "\n"))
        {
            if (line.startsWith("http://"))
            {
                _test.beginAt(line.substring(line.indexOf('/', 7)));
                return true;
            }
            if (line.startsWith("https://"))
            {
                _test.beginAt(line.substring(line.indexOf('/', 8)));
                return true;
            }
        }
        return false;
    }

    public void validateEmailEscalation(int sampleIndex)
    {
        assertNotNull("Email validation requires mail settings", _mailSettings);

        String escalateEmail = _mailSettings.getEscalateUsers()[0];
        String messageText = "I have no idea why this job failed.  Please help me.";

        String sampleExp = getExperimentLinks()[sampleIndex];

        _test.log("Escalate an error");
        EmailRecordTable emailTable = new EmailRecordTable(_test);
        PipelineStatusTable statusTable = new PipelineStatusTable(_test);
        _test.pushLocation();
        statusTable.clickStatusLink(sampleExp);
        _test.clickButton("Escalate Job Failure");
        _test.selectOptionByTextContaining(Locator.id("escalateUser").findElement(_test.getDriver()), escalateEmail);
        _test.setFormElement(Locator.id("escalationMessage"), messageText);
        // DetailsView adds a useless form.
        //test.submit();
        _test.clickButton("Send");
        _test.popLocation();

        EmailRecordTable.EmailMessage message = emailTable.getMessage(sampleExp);
        assertNotNull("Escalation message not sent", message);
        assertEquals("Escalation not sent to " + escalateEmail, escalateEmail, message.getTo()[0]);
    }
}
