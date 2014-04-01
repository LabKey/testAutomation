package org.labkey.remoteapi.olap;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.simple.JSONObject;
import org.labkey.remoteapi.Command;
import org.labkey.remoteapi.CommandResponse;

import java.util.List;
import java.util.Map;

/**
 * User: tgaluhn
 * Date: 3/31/14
 */
public class MdxResponse extends CommandResponse
{
    /**
     * Constructs a new MdxResponse, initialized with the provided
     * response text and status code.
     *
     * @param text          The response text
     * @param statusCode    The HTTP status code
     * @param contentType   The response content type
     * @param json          The parsed JSONObject (or null if JSON was not returned).
     * @param sourceCommand A copy of the command that created this response
     */
    public MdxResponse(String text, int statusCode, String contentType, JSONObject json, Command sourceCommand)
    {
        super(text, statusCode, contentType, json, sourceCommand);
    }

    public List<Map<String,Object>> getAxes()
    {
        return getProperty("axes");
    }

    public List<Object> getCells()
    {
        return getProperty("cells");
    }

    public @Nullable List<Object> getAxisOrdinalPositions(@NotNull String axisOrdinal)
    {
        List<Object> result = null;

        for(Map<String, Object> axis : getAxes())
        {
            if (axisOrdinal.equalsIgnoreCase(axis.get("axisOrdinal").toString()))
            {
                result = (List)axis.get("positions");
                break;
            }
        }
        return result;
    }

    public List<Object> getColumnsAxis()
    {
        return getAxisOrdinalPositions("COLUMNS");
    }

    public List<Object> getRowsAxis()
    {
        return getAxisOrdinalPositions("ROWS");
    }

    public Map<String, Object> getMetadata()
    {
        return getProperty("metadata");
    }

}
