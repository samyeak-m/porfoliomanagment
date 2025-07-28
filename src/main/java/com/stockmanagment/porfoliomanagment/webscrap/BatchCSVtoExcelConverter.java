package com.stockmanagment.porfoliomanagment.webscrap;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Paths;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public class BatchCSVtoExcelConverter {

    public static void convertCSVsToExcel(String inputFolderPath, String outputFolderPath) throws IOException {
        File inputFolder = new File(inputFolderPath);
        File outputFolder = new File(outputFolderPath);

        if (!outputFolder.exists()) {
            outputFolder.mkdirs();
        }

        for (File csvFile : inputFolder.listFiles()) {
            if (csvFile.isFile() && csvFile.getName().toLowerCase().endsWith(".csv")) {
                String excelFileName = csvFile.getName().replace(".csv", ".xlsx");
                String csvFilePath = csvFile.getAbsolutePath();
                String excelFilePath = Paths.get(outputFolderPath, excelFileName).toString();
                convertCSVtoExcel(csvFilePath, excelFilePath);
                System.out.println("Converted " + csvFile.getName() + " to " + excelFileName);
            }
        }
    }

    public static void convertCSVtoExcel(String csvFilePath, String excelFilePath) throws IOException {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Data");

        try (BufferedReader br = new BufferedReader(new FileReader(csvFilePath))) {
            String line;
            int rowNum = 0;
            while ((line = br.readLine()) != null) {
                String[] data = line.split(",");
                Row row = sheet.createRow(rowNum++);
                int colNum = 0;
                for (String value : data) {
                    Cell cell = row.createCell(colNum++);

                    if (isNumeric(value)) {
                        cell.setCellValue(Double.parseDouble(value));
                        cell.setCellType(CellType.NUMERIC);
                    } else if (isDate(value)) {
                        try {
                            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
                            Date date = dateFormat.parse(value);
                            cell.setCellValue(date);
                            cell.setCellType(CellType.NUMERIC);
                            CellStyle cellStyle = workbook.createCellStyle();
                            CreationHelper createHelper = workbook.getCreationHelper();
                            cellStyle.setDataFormat(createHelper.createDataFormat().getFormat("yyyy-MM-dd"));
                            cell.setCellStyle(cellStyle);
                        } catch (ParseException e) {
                            cell.setCellValue(value);
                            cell.setCellType(CellType.STRING);
                        }
                    } else {
                        cell.setCellValue(value);
                        cell.setCellType(CellType.STRING);
                    }

                    sheet.autoSizeColumn(colNum - 1);
                }
            }
        }

        try (FileOutputStream outputStream = new FileOutputStream(excelFilePath)) {
            workbook.write(outputStream);
        }
    }

    private static boolean isNumeric(String str) {
        return str.matches("-?\\d+(\\.\\d+)?");
    }

    private static boolean isDate(String str) {
        return str.matches("\\d{4}-\\d{2}-\\d{2}");
    }

    public static void main(String[] args) {
        String inputFolderPath = "D:/downloads/csv/2016";
        String outputFolderPath = "D:/downloads/excel/2016";

        try {
            convertCSVsToExcel(inputFolderPath, outputFolderPath);
            System.out.println("Conversion completed successfully.");
        } catch (IOException e) {
            System.err.println("Error occurred while converting CSVs to Excel: " + e.getMessage());
        }
    }
}
