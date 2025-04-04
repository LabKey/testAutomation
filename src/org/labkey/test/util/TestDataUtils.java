package org.labkey.test.util;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.labkey.serverapi.reader.TabLoader;
import org.labkey.test.params.FieldDefinition;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

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
            () -> new FieldDefinition("Nucleic Acid (ng/uL)", FieldDefinition.ColumnType.Decimal)
                    .setLabel("Nucleic Acid (ng/uL)"),
            () -> new FieldDefinition("Concentration (by Qubit ng/uL)", FieldDefinition.ColumnType.Decimal)
                    .setLabel("Concentration (by Qubit ng/uL)"),
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
            () -> new FieldDefinition("Weight.Balance_%", FieldDefinition.ColumnType.Decimal)
                    .setLabel("Weight.Balance %"),
            () -> new FieldDefinition("Cumulative.Yield.DCW/Glucose.Consumed_g/g", FieldDefinition.ColumnType.Decimal)
                    .setLabel("Cumulative.Yield.DCW/Glucose.Consumed g/g"),
            () -> new FieldDefinition("Average.Volume.Productivity_g/L/day", FieldDefinition.ColumnType.Decimal)
                    .setLabel("Average.Volume.Productivity g/L/day"),
            () -> new FieldDefinition("Cmol.Biomass/Cmol.Glucose.Consumed_%", FieldDefinition.ColumnType.Decimal)
                    .setLabel("Cmol.Biomass/Cmol.Glucose.Consumed %")
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
            "CIS43LS ABCD PK Pre-Qual Run 3",
            TestDataGenerator.randomFieldName("plate name")
    );

    private TestDataUtils()
    {
        // Utility class. Do not instantiate.
    }

    public static String getRealisticPlateName()
    {
        return REALISTIC_PLATE_NAMES.get(TestDataGenerator.randomInt(0, REALISTIC_PLATE_NAMES.size()));
    }

    public static List<Map<String, Object>> rowMapsFromTsv(String tsvString) throws IOException
    {
        try (InputStream dataStream = IOUtils.toInputStream(tsvString, StandardCharsets.UTF_8))
        {
            return new TabLoader.TsvFactory().createLoader(dataStream, true).load();
        }
    }

    public static List<Map<String, Object>> rowMapsFromCsv(String tsvString) throws IOException
    {
        try (InputStream dataStream = IOUtils.toInputStream(tsvString, StandardCharsets.UTF_8))
        {
            return new TabLoader.CsvFactory().createLoader(dataStream, true).load();
        }
    }

    public static String tsvStringFromRowMaps(List<Map<String, Object>> rowMaps, List<String> columns,
                                              boolean includeHeaders)
    {
        return toTabular(rowMaps, columns, '\t', includeHeaders);
    }

    public static String csvStringFromRowMaps(List<Map<String, Object>> rowMaps, List<String> columns,
                                              boolean includeHeaders)
    {
        return toTabular(rowMaps, columns, ',', includeHeaders);
    }


    public static List<List<String>> rowListsFromMaps(List<Map<String, Object>> rowMaps, List<String> columns)
    {
        return rowListsFromMaps(rowMaps, columns, false, true);
    }

    /**
     * convert a List of Map<String, Object> to a list of List<String>
     * @param rowMaps   Source data
     * @param columns   keys contained in each map, will copy values associated with them to the resulting list
     * @return A List<List<String>> containing values
     * @throws IOException
     */
    public static List<List<String>> rowListsFromMaps(List<Map<String, Object>> rowMaps, List<String> columns, boolean includeHeaders, boolean preserveEmptyValues)
    {
        List<List<String>> lists = new ArrayList<>();

        if (includeHeaders)
        {
            List<String> headers = new ArrayList<>();
            for(String col : columns)
                headers.add(col);

            lists.add(headers);
        }

        for (int i=0; i<rowMaps.size(); i++)
        {
            List<String> rowList = new ArrayList<>();
            var rowMap = rowMaps.get(i);
            for(String column : columns)
            {
                var value = (String) rowMap.get(column);
                if (value == null && preserveEmptyValues)
                    rowList.add("");
                else
                    rowList.add(value);
            }
            lists.add(rowList);
        }
        return lists;
    }

    /**
     * Convert a list of Map<String, Object>> to tabluar (tsv, csv) format
     * (assumes the rowMaps all share the same keyset/schema)
     * can be used to generate edit-grid paste data, if delimiter is \t and includeHeaders is false
     *
     * @param rowMaps data to be written into tabular format
     * @param columns the fields (in order) from the rowMaps to include in tabular output
     * @param delimiter comma [,] for csv tab [\t] for tsv
     * @param includeHeaders    whether to write the keys as column names on the first line of the output string
     * @return
     */
    private static String toTabular(List<Map<String, Object>> rowMaps, List<String> columns,
                                    char delimiter, boolean includeHeaders)
    {
        StringBuilder builder = new StringBuilder();

        if (includeHeaders)
        {
            builder.append(String.join(String.valueOf(delimiter), columns));
            builder.append("\n");
        }

        TsvQuoter q = new TsvQuoter(delimiter);

        for (Map<String, Object> row : rowMaps)
        {
            List<String> values = new ArrayList<>();
            for (String name : columns)
            {
                String value = q.quoteValue(row.get(name));
                values.add(value);
            }
            builder.append(String.join(String.valueOf(delimiter), values));
            builder.append("\n");
        }
        return builder.toString();
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
