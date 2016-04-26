package org.labkey.remoteapi.query;

import org.labkey.remoteapi.PostCommand;
import org.labkey.test.util.Maps;

import java.util.HashMap;

public class TruncateTableCommand extends PostCommand
{
    public TruncateTableCommand(String schemaName, String queryName)
    {
        super("query", "truncateTable");
        setParameters(new HashMap<>(Maps.of(
                "schemaName", schemaName,
                "queryName", queryName
        )));
    }
}
