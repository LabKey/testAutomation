package org.labkey.test.stress;

import org.assertj.core.api.Assertions;
import org.labkey.query.xml.TestCaseType;

import java.util.Collections;
import java.util.List;

/**
 * Activity: A single user action in the app (e.g. viewing the SM dashboard). These will often trigger numerous API calls.
 */
public class Activity
{
    private final String _name;
    private final List<TestCaseType> _requests;

    Activity(String name, List<TestCaseType> requests)
    {
        _name = name;
        _requests = Collections.unmodifiableList(requests);
        verifyUniqueNames();
    }

    public String getName()
    {
        return _name;
    }

    public List<TestCaseType> getRequests()
    {
        return _requests;
    }

    private void verifyUniqueNames()
    {
        Assertions.assertThat(_requests.stream().map(TestCaseType::getName)).as("Request names").doesNotHaveDuplicates();
    }
}
