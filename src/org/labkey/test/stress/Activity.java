package org.labkey.test.stress;

import org.labkey.query.xml.TestCaseType;

import java.util.Collections;
import java.util.List;

public class Activity
{
    private final String _name;
    private final List<TestCaseType> _requests;

    Activity(String name, List<TestCaseType> requests)
    {
        _name = name;
        _requests = Collections.unmodifiableList(requests);
    }

    public String getName()
    {
        return _name;
    }

    public List<TestCaseType> getRequests()
    {
        return _requests;
    }
}
