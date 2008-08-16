/*
 * Copyright (c) 2008 LabKey Software Foundation
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
package org.labkey.test.ms2.cluster;

import org.labkey.test.pipeline.PipelineWebTestBase;
import org.labkey.test.pipeline.PipelineFolder;
import org.labkey.test.pipeline.ExperimentGraph;
import org.labkey.test.util.EmailRecordTable;
import org.labkey.test.util.PasswordUtil;
import static org.labkey.test.WebTestHelper.buildNavButtonImagePath;
import org.apache.commons.lang.StringUtils;

import java.util.List;
import java.util.Arrays;

/**
 * <code>MS2EmailSuccessParams</code>
 */
public class MS2EmailSuccessParams extends MS2TestParams
{
    protected PipelineFolder.MailSettings _mailSettings;

    public MS2EmailSuccessParams(PipelineWebTestBase test, String dataPath, String protocolName,
                                 String... sampleNames)
    {
        super(test, dataPath, protocolName, sampleNames);
    }

    public PipelineFolder.MailSettings getMailSettings()
    {
        return _mailSettings;
    }

    public void setMailSettings(PipelineFolder.MailSettings mailSettings)
    {
        _mailSettings = mailSettings;
    }

    public void validate()
    {
        String userEmail = PasswordUtil.getUsername();
        String description = getDirStatusDesciption();
        EmailRecordTable emailTable = new EmailRecordTable(_test);
        EmailRecordTable.EmailMessage message = emailTable.getMessage(description);
        validateTrue("No email message found for " + description, message != null);
        emailTable.clickMessage(message);
        validateTrue("The test " + description + " did not complete successfully",
                message.getBody().indexOf("Status: COMPLETE") != -1);
        validateTrue("Unexpect message sender " + StringUtils.join(message.getFrom(), ','),
                message.getFrom().length == 1 && message.getFrom()[0].equals(userEmail));
        List<String> recipients = Arrays.asList(message.getTo());
        if (_mailSettings.isNotifyOwnerOnSuccess())
        {
            validateTrue("Message not sent to owner " + userEmail, recipients.contains(userEmail));
        }
        for (String notify : _mailSettings.getNotifyUsersOnSuccess())
        {
            validateTrue("Message not sent to " + notify, recipients.contains(notify));
        }


        // The link in this message uses the IP address.  Avoid clicking it, and
        // possibly changing hostnames.
        for (String line : StringUtils.split(message.getBody(), "\n"))
        {
            if (line.startsWith("http://"))
            {
                _test.beginAt(line.substring(line.indexOf('/', 7)));
                break;
            }
        }

        // Make sure we made it to a status page.
        _test.assertTextPresent("Job Status");

        if (_test.isLinkPresentWithImage(buildNavButtonImagePath("Data")))
        {
            validateExperiment();
        }
        else
        {
            int split = 0;
            while (_test.isLinkPresentWithText("COMPLETE", split))
            {
                _test.pushLocation();
                _test.clickLinkWithText("COMPLETE", split++);
                validateExperiment();
                _test.popLocation();
            }
        }
    }

    private void validateExperiment()
    {
        _test.clickNavButton("Data");
        ExperimentGraph graph = new ExperimentGraph(_test);
        graph.validate(this);
    }
}
