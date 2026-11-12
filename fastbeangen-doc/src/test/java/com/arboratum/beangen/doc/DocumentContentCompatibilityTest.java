package com.arboratum.beangen.doc;

import com.arboratum.beangen.BaseBuilders;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class DocumentContentCompatibilityTest {
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void preservesSpreadsheetHeadersRowsAndCellValues() throws Exception {
        File output = new File(temporaryFolder.getRoot(), "content.xlsx");
        new XlsxGeneratorBuilder()
                .withHeader("First", "col1").withHeader("Second", "col2").withHeader("Third", "col3")
                .withRow(BaseBuilders.aBean(XlsxGeneratorBuilder.Row.class)
                        .with("col1", BaseBuilders.aString().matching("first"))
                        .with("col2", BaseBuilders.aString().matching("second"))
                        .with("col3", BaseBuilders.aString().matching("third")))
                .withNumberOfRows(2).withTargetFileName(id -> output).build().generate(0);
        try (Workbook workbook = WorkbookFactory.create(output)) {
            Sheet sheet = workbook.getSheetAt(0);
            assertEquals(3, sheet.getPhysicalNumberOfRows());
            String[] headers = {"First", "Second", "Third"};
            String[] values = {"first", "second", "third"};
            for (int column = 0; column < headers.length; column++) {
                assertEquals(headers[column], sheet.getRow(0).getCell(column).getStringCellValue());
                for (int row = 1; row <= 2; row++) {
                    assertEquals(values[column], sheet.getRow(row).getCell(column).getStringCellValue());
                }
            }
        }
    }

    @Test
    public void preservesWordDocumentPayload() throws Exception {
        File output = new File(temporaryFolder.getRoot(), "content.docx");
        new DocxGeneratorBuilder().withData(BaseBuilders.aString().matching("known-data"))
                .withTargetFileName(id -> output).build().generate(0);
        try (ZipFile document = new ZipFile(output)) {
            ZipEntry entry = document.getEntry("word/document.xml");
            assertNotNull(entry);
            try (InputStream input = document.getInputStream(entry)) {
                assertTrue(new String(input.readAllBytes(), StandardCharsets.UTF_8).contains("known-data"));
            }
        }
    }
}
