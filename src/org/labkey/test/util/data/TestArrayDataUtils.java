package org.labkey.test.util.data;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.labkey.remoteapi.query.Filter;

import java.io.IOException;
import java.io.StringReader;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class TestArrayDataUtils
{

    public static <T> Map<String, T> getMapWithIdAndMultiChoiceField(List<Map<String, T>> data)
    {
        return data.stream()
                .collect(Collectors.toMap(
                        row -> String.valueOf(row.get("Name") != null ? row.get("Name") : row.get("SampleID")),
                        row ->
                        {
                            String complexKey = row.keySet().stream()
                                    .filter(k -> k.contains("Multi Choice"))
                                    .findFirst()
                                    .orElse("");
                            return row.get(complexKey);
                        }
                ));
    }

    /**
     * Filtering Map according to filter and then sorting values in alphabetical order.
     *
     * @return filtered Map
     */
    public static <T> Map<String, List<String>> filterMap(Map<String, T> map, List<String> searchValues, Filter.Operator filterType)
    {
        return map.entrySet().stream()
                .filter(entry -> entry.getValue() instanceof List)
                .map(entry -> Map.entry(entry.getKey(), (List<String>) entry.getValue()))
                .filter(entry -> isMatch(entry.getValue(), searchValues, filterType))
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> e.getValue().stream()
                                // Standard alphabetical sort that accounts for symbols and numbers.
                                // But uppercase letters are positioned before lowercase letters.
                                .sorted(Comparator
                                        .comparing((String s) -> s.substring(0, 1).toLowerCase())
                                        .thenComparing(s -> s.substring(0, 1))
                                        .thenComparing(s -> s))
                                .collect(Collectors.toList()),
                        (e1, e2) -> e1,
                        LinkedHashMap::new
                ));
    }

    public static Map<String, String> prepareMapForCheck(Map<String, List<String>> map)
    {
        return map.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> String.join(", ", entry.getValue()),
                        (e1, e2) -> e1,
                        LinkedHashMap::new
                ));
    }

    public static <T> Map<String, String> filterAndPrepareMap(Map<String, T> map, List<String> searchValues, Filter.Operator filterType)
    {
        return prepareMapForCheck(filterMap(map, searchValues, filterType));
    }

    public static List<String> parseMultiValueText(String multiValueString) throws IOException
    {
        CSVFormat format = CSVFormat.RFC4180.builder()
                .setIgnoreSurroundingSpaces(true).setTrim(true).get();
        try (CSVParser parser = format.parse(new StringReader(multiValueString)))
        {
            List<CSVRecord> records = parser.getRecords();
            if (records.size() != 1)
                throw new IllegalArgumentException("Invalid multi-value text string: " + multiValueString);
            return records.getFirst().toList();
        }
    }

    private static boolean isMatch(List<String> actualValues, List<String> searchValues, Filter.Operator type)
    {
        return switch (type)
        {
            case ARRAY_CONTAINS_ALL -> actualValues.containsAll(searchValues);
            case ARRAY_CONTAINS_ANY -> searchValues.stream().anyMatch(actualValues::contains);
            case ARRAY_CONTAINS_EXACT ->
                    actualValues.size() == searchValues.size() && actualValues.containsAll(searchValues);
            case ARRAY_CONTAINS_NONE -> searchValues.stream().noneMatch(actualValues::contains);
            case ARRAY_CONTAINS_NOT_EXACT ->
                    !(actualValues.size() == searchValues.size() && actualValues.containsAll(searchValues));
            case ARRAY_ISEMPTY -> actualValues.isEmpty();
            case ARRAY_ISNOTEMPTY -> !actualValues.isEmpty();
            default -> throw new IllegalArgumentException("Invalid filter type " + type);
        };
    }
}