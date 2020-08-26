package org.labkey.test.util;

import org.labkey.remoteapi.CommandException;
import org.labkey.remoteapi.query.SelectRowsCommand;
import org.labkey.remoteapi.query.SelectRowsResponse;
import org.labkey.remoteapi.query.Sort;
import org.labkey.test.LabKeySiteWrapper;
import org.labkey.test.Locator;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class AuditLogHelper
{
    private final LabKeySiteWrapper _wrapper;

    public AuditLogHelper(LabKeySiteWrapper wrapper)
    {
        _wrapper = wrapper;
    }

    public Integer getLatestAuditRowId(String auditTable, String containerPath) throws IOException, CommandException
    {
        String rowId = "rowId";

        SelectRowsCommand selectRows = new SelectRowsCommand("auditLog", auditTable);
        selectRows.setColumns(List.of(rowId));
        selectRows.setSorts(List.of(new Sort(rowId, Sort.Direction.DESCENDING)));
        selectRows.setMaxRows(1);

        SelectRowsResponse response = selectRows.execute(_wrapper.createDefaultConnection(), containerPath);
        List<Map<String, Object>> rows = response.getRows();
        if (rows.isEmpty())
        {
            return -1;
        }
        return (Integer) rows.get(0).get(rowId);
    }

    public Integer getLatestAuditRowId(String auditTable) throws IOException, CommandException
    {
        return getLatestAuditRowId(auditTable, "/");
    }

    public DataRegionTable goToAuditEventView(String eventType)
    {
        if (!_wrapper.isTextPresent("Audit Log"))
        {
            _wrapper.goToAdminConsole().clickAuditLog();
        }

        if (!_wrapper.getSelectedOptionText(Locator.name("view")).equals(eventType))
        {
            _wrapper.doAndWaitForPageToLoad(() -> _wrapper.selectOptionByText(Locator.name("view"), eventType));
        }
        return new DataRegionTable("query", _wrapper);
    }
}
