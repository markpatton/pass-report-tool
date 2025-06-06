package patton.pass.report;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;

class AppTest {
    /**
     * Rigourous Test :-)
     */
    @Test
    void testApp() {
        assertTrue(true);
    }

    @Test
    void testTransformWithAnonymizedSample() throws Exception {
        // Load anonymized sample JSON
        try (InputStream jsonIn = getClass().getResourceAsStream("/anonymized-sample.json");
             ByteArrayOutputStream csvOut = new ByteArrayOutputStream()) {
            assertNotNull(jsonIn, "Sample JSON input should not be null");
            // Run transform
            App.transform(jsonIn, csvOut);
            // Parse CSV output using jackson-dataformat-csv
            CsvMapper csvMapper = new CsvMapper();
            CsvSchema schema = CsvSchema.emptySchema().withHeader();
            List<Map<String, String>> rows = new ArrayList<>();
            try (Reader reader = new InputStreamReader(new ByteArrayInputStream(csvOut.toByteArray()), StandardCharsets.UTF_8)) {
                Iterator<Map<String, String>> it = csvMapper.readerFor(Map.class).with(schema).readValues(reader);
                while (it.hasNext()) {
                    rows.add(it.next());
                }
            }
            // Check header columns
            String[] expectedHeaders = Column.headers();
            assertEquals(expectedHeaders.length, rows.get(0).size(), "CSV should have expected number of columns");
            for (String header : expectedHeaders) {
                assertTrue(rows.get(0).containsKey(header), "CSV should contain header: " + header);
            }
            // Check each row's values (anonymized expected values)
            List<Map<String, String>> expectedRows = List.of(
                Map.of(
                    Column.SUBMISSION_DATE.header(), "2000-01-01T00:00:00.000Z",
                    Column.REPOSITORY_NAMES.header(), "REPO_NAME_1",
                    Column.ARTICLE_TITLE.header(), "TITLE_1",
                    Column.DOI.header(), "DOI_1",
                    Column.JOURNAL_NAME.header(), "JOURNAL_TITLE_1",
                    Column.FUNDER_NAME.header(), "",
                    Column.PUBLISHER_NAME.header(), "PUBLISHER_1"
                ),
                Map.of(
                    Column.SUBMISSION_DATE.header(), "2000-01-01T00:00:00.000Z",
                    Column.REPOSITORY_NAMES.header(), "REPO_NAME_1",
                    Column.ARTICLE_TITLE.header(), "TITLE_2",
                    Column.DOI.header(), "DOI_2",
                    Column.JOURNAL_NAME.header(), "JOURNAL_TITLE_2",
                    Column.FUNDER_NAME.header(), "",
                    Column.PUBLISHER_NAME.header(), "PUBLISHER_2"
                )
            );
            assertEquals(expectedRows.size(), rows.size(), "CSV should have expected number of data rows");
            for (int i = 0; i < expectedRows.size(); i++) {
                Map<String, String> expected = expectedRows.get(i);
                Map<String, String> actual = rows.get(i);
                for (String header : expectedHeaders) {
                    assertEquals(expected.get(header), actual.get(header), "Row " + i + ", column '" + header + "' should match expected value");
                }
            }
        }
    }
}
