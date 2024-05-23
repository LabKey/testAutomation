package org.labkey.remoteapi.plate;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public enum PlateTypes
{
    Plate_12(1, 3, 4),
    Plate_24(2, 4, 6),
    Plate_48(3, 6, 8),
    Plate_96(4, 8, 12),
    Plate_384(5, 16, 24);

    private final int _rowId;
    private final int _rows;
    private final int _columns;
    private final String _description;
    private static final Map<Integer, PlateTypes> _map;

    PlateTypes(int rowId, int rows, int columns)
    {
        _rowId = rowId;
        _description = rows + "x" + columns + "(" + (rows * columns) + ")";
        _rows = rows;
        _columns = columns;
    }

    public Integer getRowId()
    {
        return _rowId;
    }
    public String getDescription()
    {
        return _description;
    }
    public int getRows()
    {
        return _rows;
    }
    public int getColumns()
    {
        return _columns;
    }

    static {
        Map<Integer, PlateTypes> temp = new HashMap<>();
        for (PlateTypes plateType : PlateTypes.values())
        {
            temp.put(plateType.getRowId(), plateType);
        }
        _map = Collections.unmodifiableMap(temp);
    }

    public static PlateTypes fromValue(int value)
    {
        return _map.get(value);
    }
}
