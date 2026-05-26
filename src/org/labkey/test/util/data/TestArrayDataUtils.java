package org.labkey.test.util.data;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.junit.Assert;
import org.labkey.remoteapi.query.Filter;
import org.labkey.test.components.ui.grids.QueryGrid;
import org.labkey.test.util.DeferredErrorCollector;

import java.io.IOException;
import java.io.StringReader;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

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
                        e -> sortValues(e.getValue()),
                        (e1, e2) -> e1,
                        LinkedHashMap::new
                ));
    }

    /**
     * Standard alphabetical sort that accounts for symbols and numbers.
     * Uppercase letters are positioned before lowercase letters.
     */
    public static List<String> sortValues(List<String> values)
    {
        return values.stream()
                .peek(s -> { if (s.isEmpty()) throw new IllegalArgumentException("Empty values aren't allowed: " + values);})
                .sorted(Comparator
                        .comparing((String s) -> s.substring(0, 1).toLowerCase())
                        .thenComparing(s -> s.substring(0, 1))
                        .thenComparing(String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(s -> s))
                .collect(Collectors.toList());
    }

    /**
     * Sorts values alphabetically and joins them with the given separator.
     */
    public static String sortAndJoin(List<String> values, String separator)
    {
        return String.join(separator, sortValues(values));
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

    public static List<String> parseMultiValueText(String multiValueString)
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
        catch (IOException e)
        {
            throw new IllegalArgumentException(e);
        }
    }

    public static <T> String formatMultiValueText(List<T> values)
    {
        return values.stream().map(CSVFormat.DEFAULT::format).collect(Collectors.joining(","));
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
            default -> throw new IllegalArgumentException("Invalid filter type " + type);
        };
    }

    /**
     * Verifies that the grid contains exactly the expected row IDs and that each row's MVTC column
     * value matches the expected value in {@code sampleMVTCMap}.
     * Size mismatch is a hard failure; ID and per-row value checks are soft (collected via {@code checker}).
     *
     * @param idColumn the column label used to identify rows (e.g. "Sample ID" or "Name")
     */
    public static void verifyMVTCResults(QueryGrid grid, Map<String, String> sampleMVTCMap,
                                         String idColumn, String colLabel, DeferredErrorCollector checker)
    {
        List<String> foundIds = grid.getColumnDataAsText(idColumn);
        Assert.assertEquals("grid row count mismatch", sampleMVTCMap.size(), foundIds.size());
        checker.wrapAssertion(() -> assertThat(foundIds)
                .as(idColumn + " values in grid")
                .containsExactlyInAnyOrderElementsOf(sampleMVTCMap.keySet()));
        sampleMVTCMap.forEach((id, expected) -> {
            Map<String, String> rowMap = grid.getRowMapByLabel(idColumn, id);
            checker.wrapAssertion(() -> assertThat(rowMap.get(colLabel))
                    .as("'%s' value for %s '%s'", colLabel, idColumn, id)
                    .isEqualTo(expected == null ? "" : expected));
        });
    }
}