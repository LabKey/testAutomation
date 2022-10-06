/*
 * Copyright (c) 2013-2019 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.labkey.test.util;

import org.apache.poi.ss.format.CellGeneralFormatter;
import org.apache.poi.ss.formula.FormulaParseException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormat;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public abstract class ExcelHelper
{
    public static final String SUB_TYPE_XSSF = "vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    public static final String SUB_TYPE_BIFF5 = "x-tika-msoffice";
    public static final String SUB_TYPE_BIFF8 = "vnd.ms-excel";

    private static final SimpleDateFormat DATE_TIME_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm");

    public static Workbook create(File file) throws IOException
    {
        return WorkbookFactory.create(file);
    }

    public static SimpleDateFormat getDateTimeFormat()
    {
        return DATE_TIME_FORMAT;
    }

    private static CellStyle getCustomCellStyle(Workbook workbook, Map<String, CellStyle> customStyles, DataFormat dataFormat, String formatString)
    {
        CellStyle cellStyle;
        cellStyle = customStyles.get(formatString);
        if (cellStyle == null)
        {
            cellStyle = workbook.createCellStyle();
            cellStyle.setDataFormat(dataFormat.getFormat(formatString));
            customStyles.put(formatString, cellStyle);
        }
        return cellStyle;
    }

    /**
     * @param colIndex zero-based column index
     * @param rowIndex zero-based row index
     * @param sheetName name of the sheet (optional)
     */
    public static String getCellLocationDescription(int colIndex, int rowIndex, @Nullable String sheetName)
    {
        String cellLocation = getCellColumnDescription(colIndex) + (rowIndex + 1);
        if (sheetName != null)
        {
            return cellLocation + " in sheet '" + sheetName + "'";
        }
        return cellLocation;
    }

    /**
     * @param colIndex zero-based column index
     * http://stackoverflow.com/questions/22708/how-do-i-find-the-excel-column-name-that-corresponds-to-a-given-integer
     */
    private static String getCellColumnDescription(int colIndex)
    {
        // Convert to one-based index
        colIndex++;

        String name = "";
        while (colIndex > 0)
        {
            colIndex--;
            name = (char)('A' + colIndex % 26) + name;
            colIndex /= 26;
        }
        return name;
    }

    /**
     * Helper to safely convert cell values to a string equivalent
     */
    public static String getCellStringValue(Cell cell)
    {
        if (cell != null)
        {
            CellGeneralFormatter formatter = new CellGeneralFormatter();

            if ("General".equals(cell.getCellStyle().getDataFormatString()))
            {
                switch (cell.getCellType())
                {
                    case BOOLEAN:
                        return formatter.format(cell.getBooleanCellValue());
                    case NUMERIC:
                        return formatter.format(cell.getNumericCellValue());
                    case FORMULA:
                    {
                        if (cell.getCachedFormulaResultType() == CellType.STRING)
                        {
                            return cell.getStringCellValue();
                        }
                        Workbook wb = cell.getSheet().getWorkbook();
                        FormulaEvaluator evaluator = createFormulaEvaluator(wb);
                        if (evaluator != null)
                        {
                            try
                            {
                                return evaluator.evaluate(cell).formatAsString();
                            }
                            catch (FormulaParseException e)
                            {
                                return e.getMessage() == null ? e.toString() : e.getMessage();
                            }
                        }
                        return "";
                    }
                }
                return cell.getStringCellValue();
            }
            else if (isCellNumeric(cell) && DateUtil.isCellDateFormatted(cell) && cell.getDateCellValue() != null)
                return DATE_TIME_FORMAT.format(cell.getDateCellValue());
            else if (cell.getCellType() == CellType.FORMULA && cell.getCachedFormulaResultType() == CellType.STRING)
                return cell.getStringCellValue();
            else
                // This seems to be the best way to get the value that's shown in Excel
                // http://stackoverflow.com/questions/1072561/how-can-i-read-numeric-strings-in-excel-cells-as-string-not-numbers-with-apach
                return new DataFormatter().formatCellValue(cell);
        }
        return "";
    }

    public static boolean isCellNumeric(Cell cell)
    {
        if (cell != null)
        {
            CellType type = cell.getCellType();
            if (type == CellType.FORMULA)
            {
                type = cell.getCachedFormulaResultType();
            }

            return type == CellType.BLANK || type == CellType.NUMERIC;
        }
        return false;
    }

    public static FormulaEvaluator createFormulaEvaluator(Workbook workbook)
    {
        return workbook != null ? workbook.getCreationHelper().createFormulaEvaluator() : null;
    }

    /**
     * Returns a specified cell given a col/row format
     */
    @Nullable
    public static Cell getCell(Sheet sheet, int colIdx, int rowIdx)
    {
        Row row = sheet.getRow(rowIdx);

        return row != null ? row.getCell(colIdx) : null;
    }


    public static String getCellContentsAt(Sheet sheet, int colIdx, int rowIdx)
    {
        return getCellStringValue(getCell(sheet, colIdx, rowIdx));
    }

    public static List<String> getColumnData(Sheet sheet, int colIdx)
    {
        List<String> columnData = new ArrayList<>();
        int lastRowNum = sheet.getLastRowNum();
        for (int i = 0; i <= lastRowNum; i++)
        {
            columnData.add(getCellContentsAt(sheet, colIdx, i));
        }
        return columnData;
    }

    public static List<String> getRowData(Sheet sheet, int rowIdx)
    {
        List<String> rowData = new ArrayList<>();
        Iterator<Cell> row = sheet.getRow(rowIdx).cellIterator();
        Cell cell;
        while (row.hasNext())
        {
            cell = row.next();
            final int columnIndex = cell.getColumnIndex();
            while (columnIndex > rowData.size())
            {
                rowData.add(""); // fill in empty cells
            }
            rowData.add(columnIndex, getCellStringValue(cell));
        }
        return rowData;
    }

    public static List<List<String>> getFirstNRows(Sheet sheet, int n)
    {
        List<List<String>> rows = new ArrayList<>();
        for (int i = 0; i < n; i++)
        {
            rows.add(getRowData(sheet, i));
        }
        final int colCount = rows.get(0).size(); // Assume first row is column headers
        for (int j = 1; j < rows.size(); j++)
        {
            List<String> dataRow = rows.get(j);
            // Pad out empty columns
            while (dataRow.size() < colCount)
            {
                dataRow.add("");
            }
        }
        return rows;
    }

    public static List<List<String>> getAllRows(Sheet sheet)
    {
        return getFirstNRows(sheet, sheet.getLastRowNum() + 1);
    }

    public static Map<String, List<Map<String, String>>> loadData(File file)
    {
        try (Workbook workbook = ExcelHelper.create(file))
        {
            Map<String, List<Map<String, String>>> allData = new LinkedHashMap<>();

            for (int s = 0; s < workbook.getNumberOfSheets(); s++)
            {
                Sheet sheet = workbook.getSheetAt(s);

                List<List<String>> rawRows = getAllRows(sheet);
                List<String> rawHeaders = rawRows.get(0);
                List<List<String>> rawData = rawRows.subList(1, rawRows.size());

                List<Map<String, String>> rowMaps = new ArrayList<>();

                for (List<String> rawRow : rawData)
                {
                    Map<String, String> rowMap = new LinkedHashMap<>();
                    for (int col = 0; col < rawRow.size(); col++)
                    {
                        rowMap.put(rawHeaders.get(col), rawRow.get(col));
                    }
                    rowMaps.add(rowMap);
                }

                allData.put(sheet.getSheetName(), rowMaps);
            }

            return allData;
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }
}
