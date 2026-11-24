import com.arboratum.beangen.BaseBuilders;
import com.arboratum.beangen.Generator;
import com.arboratum.beangen.database.DataSet;
import com.arboratum.beangen.database.DataSetBuilder;
import com.arboratum.beangen.database.Entry;
import com.arboratum.beangen.database.UpdateGenerator;
import com.arboratum.beangen.database.UpdateOf;
import com.arboratum.beangen.doc.DocxGeneratorBuilder;
import com.arboratum.beangen.doc.XlsxGeneratorBuilder;
import com.arboratum.beangen.util.RandomSequence;

import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;
import java.util.zip.ZipFile;

public class PublicationSmoke {
    public static void main(String[] args) throws Exception {
        require(args.length == 1 && Runtime.version().feature() == Integer.parseInt(args[0]), "Wrong Java runtime");
        require(new RandomSequence(31).nextInt(100) == 86, "Historical core value changed");
        verifyPublishedApi();
        File output = new File("build/generated");
        require(output.isDirectory() || output.mkdirs(), "Cannot create output directory");
        File docx = new File(output, "smoke.docx");
        new DocxGeneratorBuilder().withData(BaseBuilders.aString().matching("known-data"))
                .withTargetFileName(id -> docx).build().generate(0);
        require(zipText(docx, "word/document.xml").contains("known-data"), "DOCX payload missing");
        File xlsx = new File(output, "smoke.xlsx");
        new XlsxGeneratorBuilder().withHeader("Data", "col1")
                .withRow(BaseBuilders.aBean(XlsxGeneratorBuilder.Row.class)
                        .with("col1", BaseBuilders.aString().matching("known-data")))
                .withNumberOfRows(1).withTargetFileName(id -> xlsx).build().generate(0);
        String spreadsheet = zipText(xlsx, "xl/sharedStrings.xml") + zipText(xlsx, "xl/worksheets/sheet1.xml");
        require(spreadsheet.contains("Data") && spreadsheet.contains("known-data"), "XLSX payload missing");
        System.out.println("Published dependency consumer passed on Java " + Runtime.version().feature());
    }

    private static void verifyPublishedApi() {
        Generator<Integer> values = BaseBuilders.aInteger().uniform(0, 100).build();
        DataSet<Integer> data = new DataSetBuilder<Integer>().of(4).withEntryGenerator(values)
                .thenUpdatedWith(1, 0, 0).build();
        require(data.filteredView(value -> true, 1).selectOne(new RandomSequence(31)) != null,
                "Bounded filtered selection failed");
        require(data.filteredView(value -> true, 0).selectOne(new RandomSequence(31)) == null,
                "Zero selection budget was ignored");
        Entry<String> projected = data.transformedView(Object::toString, String.class, 1)
                .selectOne(new RandomSequence(31));
        require(projected != null && projected.wasInInitialDataSet()
                && projected.getRef().wasInInitialDataSet(), "Transformed provenance missing");
        require(!data.buildOperationFeed(true).next().block().getEntry().wasInInitialDataSet(),
                "Newly created entry marked initial");
        DataSet<Integer> deleted = new DataSetBuilder<Integer>().of(4).withEntryGenerator(values)
                .thenUpdatedWith(0, 0, 1).withDeletionCheck(Entry::wasInInitialDataSet).build();
        require(deleted.buildOperationFeed(true).next().block().getEntry().isDeleted(),
                "Entry-aware deletion failed");
        AtomicReference<DataSet.EntryRef<Integer>> seen = new AtomicReference<>();
        DataSet<Integer> updated = new DataSetBuilder<Integer>().of(4).withEntryGenerator(values)
                .thenUpdatedWith(0, 1, 0).withUpdateGenerator(new UpdateGenerator<Integer>() {
                    @Override
                    public boolean canApplyOn(Integer value, DataSet.EntryRef<Integer> ref) {
                        seen.set(ref);
                        return ref.wasInInitialDataSet();
                    }

                    @Override
                    public UpdateOf<Integer> generate(Integer previous, RandomSequence random) {
                        return value -> value;
                    }
                }).build();
        require(updated.buildOperationFeed(true).next().block().getEntry().getRef().equals(seen.get()),
                "Update applicability did not receive the matching reference");
    }

    private static String zipText(File file, String name) throws Exception {
        try (ZipFile archive = new ZipFile(file)) {
            var entry = archive.getEntry(name);
            if (entry == null) return "";
            try (InputStream input = archive.getInputStream(entry)) {
                return new String(input.readAllBytes(), StandardCharsets.UTF_8);
            }
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new IllegalStateException(message);
    }
}
