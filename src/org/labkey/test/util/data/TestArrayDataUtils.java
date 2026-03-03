package org.labkey.test.util.data;

import org.labkey.remoteapi.query.Filter;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class TestArrayDataUtils
{
    /**
     * Filtering Map according to filter and then sorting values in alphabetical order.
     *
     * @return filtered Map
     */
    public static Map<String, String> filterMap(Map<String, List<String>> sourceMap, List<String> searchValues, Filter.Operator filterType)
    {
        return sourceMap.entrySet().stream()
                .filter(entry -> isMatch(entry.getValue(), searchValues, filterType))
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> e.getValue().stream().sorted().collect(Collectors.joining(", ")),
                        (e1, e2) -> e1,
                        LinkedHashMap::new
                ));
    }

    private static boolean isMatch(List<String> actualValues, List<String> searchValues, Filter.Operator type)
    {
        return switch (type)
        {
            case ARRAY_CONTAINS_ALL -> actualValues.containsAll(searchValues);
            case ARRAY_CONTAINS_ANY -> searchValues.stream().anyMatch(actualValues::contains);
            case ARRAY_CONTAINS_EXACT -> actualValues.size() == searchValues.size() && actualValues.containsAll(searchValues);
            case ARRAY_CONTAINS_NONE -> searchValues.stream().noneMatch(actualValues::contains);
            case ARRAY_CONTAINS_NOT_EXACT -> !(actualValues.size() == searchValues.size() && actualValues.containsAll(searchValues));
            case ARRAY_ISEMPTY -> actualValues.isEmpty();
            case ARRAY_ISNOTEMPTY -> !actualValues.isEmpty();
            default -> true;
        };
    }
}