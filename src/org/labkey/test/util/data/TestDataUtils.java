package org.labkey.test.util.data;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.labkey.serverapi.reader.DataLoader;
import org.labkey.serverapi.reader.TabLoader;
import org.labkey.test.TestFileUtils;
import org.labkey.test.params.FieldDefinition;
import org.labkey.test.util.TestDataGenerator;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class TestDataUtils
{
    // 'FieldDefinition's are mutable; don't store them as global constants
    public static final List<Supplier<FieldDefinition>> REALISTIC_ASSAY_FIELDS = List.of(
            () -> new FieldDefinition("Addition or Removal (0= addition, 1=removal)", FieldDefinition.ColumnType.Integer),
            () -> new FieldDefinition("Source (0=external, 1 = internal to system)", FieldDefinition.ColumnType.Integer),
            () -> new FieldDefinition("Raw Sample [Al] g/L", FieldDefinition.ColumnType.Decimal),
            () -> new FieldDefinition("Raw Sample [H+] g/L or %", FieldDefinition.ColumnType.Decimal),
            () -> new FieldDefinition("KJ/Day", FieldDefinition.ColumnType.Decimal),
            () -> new FieldDefinition("EE KCal/kg0.75", FieldDefinition.ColumnType.Decimal),
            () -> new FieldDefinition("Ratio EE in Kcal/Day to Lean Mass", FieldDefinition.ColumnType.Decimal),
            () -> new FieldDefinition("Feed %", FieldDefinition.ColumnType.Decimal),
            () -> new FieldDefinition("Consumption Rate, Glucose", FieldDefinition.ColumnType.Decimal),
            () -> new FieldDefinition("Measurement Date/Time", FieldDefinition.ColumnType.DateAndTime),
            () -> new FieldDefinition("A260/A280", FieldDefinition.ColumnType.Decimal),
            () -> new FieldDefinition("Nucleic Acid (ng/uL)", FieldDefinition.ColumnType.Decimal),
            () -> new FieldDefinition("Concentration (by Qubit ng/uL)", FieldDefinition.ColumnType.Decimal),
            () -> new FieldDefinition("Dead (cells/ml)", FieldDefinition.ColumnType.Decimal),
            () -> new FieldDefinition("PDGF-AA/BB", FieldDefinition.ColumnType.Decimal),
            () -> new FieldDefinition("Run End Data/Time", FieldDefinition.ColumnType.DateAndTime),
            () -> new FieldDefinition("Run Start Date/Time", FieldDefinition.ColumnType.DateAndTime),
            () -> new FieldDefinition("Algorithm Parameter: Calc. Top", FieldDefinition.ColumnType.Integer),
            () -> new FieldDefinition("1.0", FieldDefinition.ColumnType.Integer),
            () -> new FieldDefinition("2.0"),
            () -> new FieldDefinition("12.0"),
            () -> new FieldDefinition("FAM-Lambda..cp.Rxn."),
            () -> new FieldDefinition("VIC-Precision...1"),
            () -> new FieldDefinition("Product.Type"),
            () -> new FieldDefinition("Weight.Balance_%", FieldDefinition.ColumnType.Decimal),
            () -> new FieldDefinition("Cumulative.Yield.DCW/Glucose.Consumed_g/g", FieldDefinition.ColumnType.Decimal),
            () -> new FieldDefinition("Average.Volume.Productivity_g/L/day", FieldDefinition.ColumnType.Decimal),
            () -> new FieldDefinition("Cmol.Biomass/Cmol.Glucose.Consumed_%", FieldDefinition.ColumnType.Decimal)
    );
    public static final List<Supplier<FieldDefinition>> REALISTIC_SAMPLE_FIELDS = List.of(
            () -> new FieldDefinition("MW (g/mol)", FieldDefinition.ColumnType.Decimal),
            () -> new FieldDefinition("Batch FW (g/mol)", FieldDefinition.ColumnType.Decimal),
            () -> new FieldDefinition("Sequence (5'-3')"),
            () -> new FieldDefinition("Tumor%", FieldDefinition.ColumnType.Decimal),
            () -> new FieldDefinition("Viable_cells%", FieldDefinition.ColumnType.Decimal),
            () -> new FieldDefinition("Sample no."),
            () -> new FieldDefinition("Pass/Fail" , FieldDefinition.ColumnType.TextChoice).setTextChoiceValues(List.of("Pass", "Fail")),
            () -> new FieldDefinition("Final Positivity %", FieldDefinition.ColumnType.Decimal),
            () -> new FieldDefinition("Optimised Yes/No", FieldDefinition.ColumnType.Boolean),
            () -> new FieldDefinition("G-Band Pass/Fail", FieldDefinition.ColumnType.Boolean),
            () -> new FieldDefinition("Positivity/negativity notes"),
            () -> new FieldDefinition("Useful for R&D/Production ?", FieldDefinition.ColumnType.Boolean),
            () -> new FieldDefinition("NaCl Lot Number (External), 0.9% NaCl Expiry (In-House)"),
            () -> new FieldDefinition("'GURR' 6.8 buffer tablets Lot number (External)"),
            () -> new FieldDefinition("Giemsa Stain Lot number, Expiry"),
            () -> new FieldDefinition("Trypsin 2.5% Lot number (External), Expiry"),
            () -> new FieldDefinition("Lot No.", FieldDefinition.ColumnType.Integer),
            () -> new FieldDefinition("Sample Origin / Owner"),
            () -> new FieldDefinition("PSS Tracking No."),
            () -> new FieldDefinition("Product/bottle size", FieldDefinition.ColumnType.Decimal),
            () -> new FieldDefinition("Time point / Pull Date", FieldDefinition.ColumnType.DateAndTime),
            () -> new FieldDefinition("Cell Type (Epz, Spz, PS)"),
            () -> new FieldDefinition("Concentration (ng/uL)", FieldDefinition.ColumnType.Decimal),
            () -> new FieldDefinition("Lot no. (Replacement tube) 1"),
            () -> new FieldDefinition("Date of Collection (DD/MMM/YYY)", FieldDefinition.ColumnType.Date),
            () -> new FieldDefinition("Freezer/Fridge ID"),
            () -> new FieldDefinition( "X Position (i.e., box row)", FieldDefinition.ColumnType.Integer),
            () -> new FieldDefinition("[Analysis 2] 2. Time In (Fridge)", FieldDefinition.ColumnType.Time),
            () -> new FieldDefinition("Aliquot_No._/_ID"),
            () -> new FieldDefinition("VIAL_ID/BARCODE/ACCESSION_No."),
            () -> new FieldDefinition("No.=_464"),
            () -> new FieldDefinition("Specimen_condition_(Hämolyse/insufficient_volume/…)", FieldDefinition.ColumnType.Decimal),
            () -> new FieldDefinition("CHECKOUT_(x),_Removed_(1)"),
            () -> new FieldDefinition("Age <18 years of age or >65 years of age.", FieldDefinition.ColumnType.Boolean),
            () -> new FieldDefinition("CTS&L_LLS_Visit_Code_"),
            () -> new FieldDefinition("Collection Tube Type & Volume 1"),
            () -> new FieldDefinition("Row_&_Col"),
            () -> new FieldDefinition("The participant has received any investigational compound from a different trial within 30 days or 5 half-lives (whichever is greater)."),
            () -> new FieldDefinition("Barcode e.g FG30000A001"),
            () -> new FieldDefinition("Information pertaining to patient recruitment e.g advertisements, bulletins and information placed on the internet - TMAR"),
            () -> new FieldDefinition("Data Collection Tools (CRF's, Info Sheets, etc.)")
    );
    public static final List<Supplier<FieldDefinition>> REALISTIC_SOURCE_FIELDS = List.of(
            () -> new FieldDefinition("Patient Race / Ethnicity", FieldDefinition.ColumnType.TextChoice).setTextChoiceValues(List.of("American Indian or Alaska Native", "Asian",  "Black", "Native Hawaiian or Pacific Islander", "White", "Other", "Unknown" )),
            () -> new FieldDefinition("Tumor%", FieldDefinition.ColumnType.Decimal),
            () -> new FieldDefinition("Viable_cells%", FieldDefinition.ColumnType.Decimal),
            () -> new FieldDefinition("Гемоглобін тех.", FieldDefinition.ColumnType.Date),
            () -> new FieldDefinition("Disposition (per SOW/MTA)", FieldDefinition.ColumnType.String),
            () -> new FieldDefinition("~ Height (T to B) (mm)", FieldDefinition.ColumnType.Integer),
            () -> new FieldDefinition("OD/DCW factor", FieldDefinition.ColumnType.String),
            () -> new FieldDefinition("Age (years)", FieldDefinition.ColumnType.Integer)
    );
    public static final List<String> REALISTIC_PLATE_NAMES = List.of(
            "123456.01",
            "Example Plate (96-well)",
            "2024-01-01 Luminex Plate #1",
            "Study ABC Serum/Plasma",
            "- Plate 1 this one is from an instrument file",
            "Miniprep Quant BL 18JAN2023",
            "CIS43LS ABCD PK Pre-Qual Run 3"
    );

    private TestDataUtils()
    {
        // Utility class. Do not instantiate.
    }

    public static String getRealisticPlateName()
    {
        return REALISTIC_PLATE_NAMES.get(TestDataGenerator.randomInt(0, REALISTIC_PLATE_NAMES.size() - 1));
    }

    public static List<Map<String, Object>> rowMapsFromTsv(File tsvFile) throws IOException
    {
        try (DataLoader loader = new TabLoader.TsvFactory().createLoader(tsvFile, true))
        {
            return loader.load();
        }
    }

    public static List<Map<String, Object>> rowMapsFromTsv(String tsvString) throws IOException
    {
        try (InputStream dataStream = IOUtils.toInputStream(tsvString, StandardCharsets.UTF_8))
        {
            return new TabLoader.TsvFactory().createLoader(dataStream, true).load();
        }
    }

    public static List<Map<String, Object>> rowMapsFromCsv(File csvFile) throws IOException
    {
        try (DataLoader loader = new TabLoader.CsvFactory().createLoader(csvFile, true))
        {
            return loader.load();
        }
    }

    public static List<Map<String, Object>> rowMapsFromCsv(String tsvString) throws IOException
    {
        try (InputStream dataStream = IOUtils.toInputStream(tsvString, StandardCharsets.UTF_8))
        {
            return new TabLoader.CsvFactory().createLoader(dataStream, true).load();
        }
    }

    public static String stringFromRowMaps(List<Map<String, Object>> rowMaps, List<String> columns, boolean includeHeaders, CSVFormat format)
    {
        return stringFromRows(rowListsFromMaps(rowMaps, columns, includeHeaders, true), format);
    }

    public static String tsvStringFromRowMaps(List<Map<String, Object>> rowMaps, List<String> columns,
                                              boolean includeHeaders)
    {
        return stringFromRowMaps(rowMaps, columns, includeHeaders, CSVFormat.TDF);
    }

    public static <T> List<Map<String, T>> mapsFromRows(List<List<T>> allRows)
    {
        List<Map<String, T>> rowMaps = new ArrayList<>();

        if (allRows != null && !allRows.isEmpty())
        {
            List<T> header = allRows.get(0);

            for (int i = 1; i != allRows.size(); i++)
            {
                List<T> row = allRows.get(i);
                Map<String, T> rowMap = new LinkedHashMap<>();
                int end = Math.min(header.size(), row.size());
                for (int col = 0; col < end; col++)
                {
                    rowMap.put(header.get(col).toString(), row.get(col));
                }
                rowMaps.add(rowMap);
            }
        }

        return rowMaps;
    }

    public static List<List<String>> dataRowsFromMaps(List<Map<String, Object>> rowMaps, List<String> columns)
    {
        return rowListsFromMaps(rowMaps, columns, false, true);
    }

    public static List<List<String>> rowListsFromMaps(List<Map<String, Object>> rowMaps)
    {
        Set<String> columns = new LinkedHashSet<>();
        for (Map<String, Object> row : rowMaps)
        {
            columns.addAll(row.keySet());
        }
        return rowListsFromMaps(rowMaps, new ArrayList<>(columns), true, true);
    }

    public static List<List<String>> rowListsFromMaps(List<Map<String, Object>> rowMaps, List<String> columns)
    {
        return rowListsFromMaps(rowMaps, columns, true, true);
    }

    /**
     * convert a List of Map<String, Object> to a list of List<String>
     * @param rowMaps   Source data
     * @param columns   keys contained in each map, will copy values associated with them to the resulting list
     * @return A List<List<String>> containing values
     */
    public static List<List<String>> rowListsFromMaps(List<Map<String, Object>> rowMaps, List<String> columns, boolean includeHeaders, boolean allowMissingValues)
    {
        List<List<String>> lists = new ArrayList<>();

        if (includeHeaders)
        {
            List<String> headers = new ArrayList<>(columns);

            lists.add(headers);
        }

        for (Map<String, Object> rowMap : rowMaps)
        {
            List<String> rowList = new ArrayList<>();
            for (String column : columns)
            {
                var value = rowMap.get(column);
                if (value == null)
                {
                    if (allowMissingValues)
                        value = "";
                    else
                        throw new IllegalArgumentException("Missing value for column '" + column + "' in row: " +  rowMap);
                }
                if (value instanceof Collection<?> c)
                {
                    value = c.stream().map(Object::toString).collect(Collectors.joining(","));
                }
                rowList.add(value.toString());
            }
            lists.add(rowList);
        }
        return lists;
    }

    public static List<List<String>> replaceColumnHeaders(List<List<String>> rowLists, Function<String, String> columnMapper)
    {
        List<String> headerRow = rowLists.get(0);
        List<String> updatedHeaderRow = new ArrayList<>();
        for (String oldHeader : headerRow)
        {
            updatedHeaderRow.add(columnMapper.apply(oldHeader));
        }

        List<List<String>> updatedRows = new ArrayList<>();
        updatedRows.add(updatedHeaderRow);
        updatedRows.addAll(rowLists.subList(1, rowLists.size()));

        return updatedRows;
    }

    public static List<Map<String, Object>> replaceMapKeys(List<Map<String, Object>> rowMaps, Function<String, String> columnMapper)
    {
        List<Map<String, Object>> updatedRows = new ArrayList<>();
        for (Map<String, Object> original : rowMaps)
        {
            Map<String, Object> updatedRow = new LinkedHashMap<>();
            for (Map.Entry<String, Object> entry : original.entrySet())
            {
                String updatedKey = columnMapper.apply(entry.getKey());
                if (updatedRow.containsKey(updatedKey))
                {
                    throw new IllegalArgumentException("Duplicate key mapping for '" + updatedKey + "' in row: " +  original);
                }
                updatedRow.put(updatedKey, entry.getValue());
            }
            updatedRows.add(updatedRow);
        }
        return updatedRows;
    }

    public static <T> File writeRowsToTsv(String fileName, List<List<T>> rows) throws IOException
    {
        return writeRowsToFile(fileName, rows, CSVFormat.TDF);
    }

    public static <T> File writeRowsToCsv(String fileName, List<List<T>> rows) throws IOException
    {
        return writeRowsToFile(fileName, rows, CSVFormat.DEFAULT);
    }

    public static @NotNull <T> File writeRowsToFile(String fileName, List<List<T>> rows, CSVFormat format) throws IOException
    {
        File file = new File(TestFileUtils.getTestTempDir(), fileName);
        FileUtils.forceMkdirParent(file);

        try (CSVPrinter printer = new CSVPrinter(new FileWriter(file, StandardCharsets.UTF_8), format)) {
            for (List<T> row : rows)
            {
                printer.printRecord(row);
            }
        }

        return file;
    }

    public static List<List<String>> readRowsFromTsv(File file) throws IOException
    {
        return readRowsFromFile(file, CSVFormat.TDF);
    }

    public static List<List<String>> readRowsFromCsv(File file) throws IOException
    {
        return readRowsFromFile(file, CSVFormat.DEFAULT);
    }

    public static List<List<String>> readRowsFromFile(File file, CSVFormat format) throws IOException
    {
        try (Reader in = new FileReader(file, StandardCharsets.UTF_8))
        {
            CSVParser parser = CSVParser.builder().setFormat(format).setReader(in).get();
            List<CSVRecord> records = parser.getRecords();
            return records.stream().map(CSVRecord::toList).toList();
        }
    }

    public static <T> String stringFromRows(List<List<T>> rows, CSVFormat format)
    {
        StringWriter stringWriter = new StringWriter();

        try (CSVPrinter printer = new CSVPrinter(stringWriter, format)) {
            for (List<?> row : rows)
            {
                printer.printRecord(row);
            }
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }

        return stringWriter.toString();
    }

    /**
     * Used to quote values to be written to a TSV file
     * @see org.labkey.api.data.TSVWriter
     */
    public static class TsvQuoter
    {
        protected char _escapeChar = '\\';
        private static final char _chQuote = '"';
        private final char[] _escapedChars;

        public TsvQuoter(char delimiterChar)
        {
            _escapedChars = new char[] {'\r', '\n', _escapeChar, _chQuote, delimiterChar};
        }

        public TsvQuoter()
        {
            this('\t');
        }

        public String quoteValue(Object o)
        {
            if (o == null)
                return "";

            String value = o.toString();

            String escaped = value;
            if (shouldQuote(value))
            {
                StringBuilder sb = new StringBuilder(value.length() + 10);
                sb.append(_chQuote);
                int i;
                int lastMatch = 0;

                while (-1 != (i = value.indexOf(_chQuote, lastMatch)))
                {
                    sb.append(value, lastMatch, i);
                    sb.append(_chQuote).append(_chQuote);
                    lastMatch = i + 1;
                }

                if (lastMatch < value.length())
                    sb.append(value.substring(lastMatch));

                sb.append(_chQuote);
                escaped = sb.toString();
            }

            return escaped;
        }

        protected boolean shouldQuote(String value)
        {
            int len = value.length();
            if (len == 0)
                return false;
            char firstCh = value.charAt(0);
            char lastCh = value.charAt(len - 1);
            if (Character.isSpaceChar(firstCh) || Character.isSpaceChar(lastCh))
                return true;
            return StringUtils.containsAny(value, _escapedChars);
        }
    }

    public static final String[] DECODED = {"\\", "$", "/", "&", "}", "~", ",", "."};
    public static final String[] ENCODED = {"\\\\", "\\$", "\\/", "\\&", "\\}", "\\~", "\\,", "\\."};
    public static String getEscapedNameExpression(String encoded)
    {
        return StringUtils.replaceEach(encoded, DECODED, ENCODED);
    }

}
