package org.labkey.test.util;

import org.junit.Assert;
import org.labkey.remoteapi.Connection;
import org.labkey.remoteapi.issues.IssueCommand;
import org.labkey.remoteapi.issues.IssueModel;
import org.labkey.remoteapi.issues.IssueResponse;
import org.labkey.remoteapi.security.GetUsersResponse;
import org.labkey.test.WebTestHelper;
import org.labkey.test.pages.issues.DetailsPage;
import org.openqa.selenium.WrapsDriver;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class IssuesApiHelper extends IssuesHelper
{
    private final Connection _connection;
    private final APIUserHelper _userHelper;
    private final Long _defaultPriority = 3L;

    public IssuesApiHelper(WrapsDriver driverWrapper)
    {
        super(driverWrapper);
        _connection = WebTestHelper.getRemoteApiConnection(true);
        _userHelper = new APIUserHelper(this);
    }

    @Override
    public DetailsPage addIssue(Map<String, String> props, File... attachments)
    {
        IssueModel issue = new IssueModel();

        issue.setProperties(props);
        Arrays.stream(attachments).forEach(issue::addAttachment);
        issue.setAction(IssueModel.IssueAction.INSERT);

        // translate display name to userId
        if (props.containsKey("assignedTo"))
        {
            String displayName = props.get("assignedTo");
            List<GetUsersResponse.UserInfo> user = _userHelper.getUsers().getUsersInfo().stream()
                    .filter(ui -> ui.getDisplayName().equals(displayName)).toList();

            Assert.assertTrue("Unable to properly match user with displayName: " + displayName, user.size() == 1);
            if (user.size() == 1)
                issue.setAssignedTo(Long.valueOf(user.get(0).getUserId()));
        }

        if (!props.containsKey("priority"))
            issue.setPriority(_defaultPriority);

        try
        {
            IssueCommand command = new IssueCommand(issue);
            IssueResponse response = command.execute(_connection, getCurrentContainerPath());

            Assert.assertEquals("Unexpected errors", 200, response.getStatusCode());

            return DetailsPage.beginAt(this, String.valueOf(response.getIssueId()));
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }
}
