package org.labkey.remoteapi.plate;

import java.util.HashMap;
import java.util.Map;

public enum PlateTypes
{
    Plate_12(1, "3x4(12)", 3, 4),
    Plate_24(2, "4x6(24)", 4, 6),
    Plate_48(3, "6x8(48)", 6, 8),
    Plate_96(4, "8x12(96)", 8, 12),
    Plate_384(5, "16x24(384)", 16, 24);

    private final int _value;
    private final int _rows;
    private final int _columns;
    private final String _description;
    private static final Map<Integer, PlateTypes> _map = new HashMap<>();

    PlateTypes(int value, String description, int rows, int columns)
    {
        _value = value;
        _description = description;
        _rows = rows;
        _columns = columns;
    }

    public Integer getValue()
    {
        return _value;
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
        for (PlateTypes plateType : PlateTypes.values())
        {
            _map.put(plateType.getValue(), plateType);
        }
    }

    public static PlateTypes fromValue(int value)
    {
        return _map.get(value);
    }
}
