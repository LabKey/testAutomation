package org.labkey.test.params;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class FieldKey
{
    private final List<String> _parts;
    private String _label;

    private FieldKey(String... parts)
    {
        if (parts.length == 0)
        {
            throw new IllegalArgumentException("No field key parts were provided.");
        }
        // '/' is used as a separator character in fieldKeys. Slashes in field names are encoded as '$S'
        _parts = Arrays.stream(parts)
                .map(part -> part.replace("/", "$S"))
                .collect(Collectors.toList());
        _label = parts[parts.length - 1];
    }

    public static FieldKey fromParts(String... parts)
    {
        return new FieldKey(parts);
    }

    public static FieldKey fromPath(String path)
    {
        return new FieldKey(path.split("/"));
    }

    public String getLabel()
    {
        return _label;
    }

    public FieldKey setLabel(String label)
    {
        _label = label;
        return this;
    }

    public List<String> getParts()
    {
        return _parts;
    }

    @Override
    public String toString()
    {
        return String.join("/", getParts());
    }
}
