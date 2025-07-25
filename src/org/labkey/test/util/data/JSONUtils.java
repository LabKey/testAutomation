package org.labkey.test.util.data;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JSONUtils
{
    private JSONUtils() {}


    /**
     * Returns the value of a specific property in the parsed data
     * given a path to that property.
     * <p>
     * The path is a period-delimited list of property names. For
     * example, to obtain the 'bar' property from the Map associated
     * with the 'foo' property, the path would be 'foo.bar'.
     * Property names may include an array index. For example, 'foos[2].bar'
     * will return the 'bar' property for the third item of the 'foos' array
     * @param path The property path.
     * @param data JSON data in Map form
     * @param <T> the type of the property.
     * @return The property value
     */
    public static <T> T getProperty(String path, Map<String, Object> data)
    {
        String[] pathParts = StringUtils.trimToEmpty(path).split("\\.");
        if (StringUtils.isAnyBlank(pathParts))
            throw new IllegalArgumentException("Path cannot contain blank parts: " + path);
        return getProperty(Arrays.asList(pathParts), 0, data);
    }

    /**
     * Called by {@link #getProperty(String, Map)} after splitting the path into
     * a String[], and recursively by itself as it descends the property
     * hierarchy.
     * @param path The path split into a String[].
     * @param pathIndex The current index into the path array.
     * @param parent The current parent map.
     * @param <T> The type of the property.
     * @return The property value
     */
    @SuppressWarnings("unchecked")
    private static <T> T getProperty(List<String> path, int pathIndex, Map<String, Object> parent)
    {
        if (null == parent)
            throw new NullPointerException("object is null");

        String key = path.get(pathIndex);
        Integer arrayIndex = null;
        Pattern arrayPattern = Pattern.compile("(.+)\\[([0-9]+)]$");
        Matcher matcher = arrayPattern.matcher(key);
        if (matcher.find())
        {
            key = matcher.group(1);
            arrayIndex = Integer.parseInt(matcher.group(2));
        }

        Object prop = parent.get(key);
        if (arrayIndex != null)
        {
            if (prop instanceof List<?> list)
            {
                if (list.size() > arrayIndex)
                    prop = list.get(arrayIndex);
                else
                    throw new NoSuchElementException("Array index out of bounds [size = %s]: '%s'"
                            .formatted(list.size(), getSubPath(path, pathIndex)));
            }
            else
                throw new NoSuchElementException("No array found at path: '%s'. Found '%s'"
                        .formatted(getSubPath(path, pathIndex), (prop == null ? "null" : prop.getClass().getSimpleName())));
        }

        // if this was the last path part, return the prop
        if (pathIndex == (path.size() - 1))
        {
            if (prop != null)
                return (T) prop;
            else
                throw new NoSuchElementException("No item found at path: '%s'"
                        .formatted(getSubPath(path, pathIndex)));
        }
        else
        {
            // recurse if prop is non-null and instance of map
            if (prop instanceof Map)
                return getProperty(path, pathIndex + 1, (Map<String, Object>)prop);
            else
                throw new NoSuchElementException("No map found at path: '%s'. Found: '%s'"
                        .formatted(getSubPath(path, pathIndex), (prop == null ? "null" : prop.getClass().getSimpleName())));
        }
    }

    private static @NotNull String getSubPath(List<String> path, int pathIndex)
    {
        return String.join(".", path.subList(0, pathIndex + 1));
    }

}
