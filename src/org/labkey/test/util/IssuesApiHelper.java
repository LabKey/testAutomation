/*
 * Copyright (c) 2022-2026 LabKey Corporation
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

import org.junit.Assert;
import org.labkey.remoteapi.issues.IssueModel;
import org.labkey.remoteapi.issues.IssueResponse;
import org.labkey.remoteapi.issues.IssuesCommand;
import org.labkey.remoteapi.security.GetUsersResponse;
import org.labkey.test.pages.issues.DetailsPage;
import org.openqa.selenium.WrapsDriver;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class IssuesApiHelper extends IssuesHelper
{
    private final APIUserHelper _userHelper;
    private final int _defaultPriority = 3;

    public IssuesApiHelper(WrapsDriver driverWrapper)
    {
        super(driverWrapper);
        _userHelper = new APIUserHelper(this);
    }

    @Override
    public DetailsPage addIssue(Map<String, String> props, File... attachments)
    {
        IssueModel issue = new IssueModel();

        issue.setProperties(props);
        Arrays.stream(attachments).forEach(issue::addAttachment);
        issue.setAction(IssueModel.IssueAction.insert);

        // translate display name to userId
        if (props.containsKey("AssignedTo"))
        {
            String displayName = props.get("AssignedTo");
            List<GetUsersResponse.UserInfo> user = _userHelper.getUsers().getUsersInfo().stream()
                    .filter(ui -> ui.getDisplayName().equals(displayName)).toList();

            Assert.assertEquals("Unable to properly match user with displayName: " + displayName, 1, user.size());
            if (user.size() == 1)
                issue.setAssignedTo(user.getFirst().getUserId());
        }

        if (!props.containsKey("Priority"))
            issue.setPriority(_defaultPriority);

        try
        {
            IssuesCommand command = new IssuesCommand(List.of(issue));
            IssueResponse response = command.execute(createDefaultConnection(), getCurrentContainerPath());

            Assert.assertEquals("Unexpected errors", 200, response.getStatusCode());

            return DetailsPage.beginAt(this, String.valueOf(response.getIssueIds().getFirst()));
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }
}
