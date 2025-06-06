package patton.pass.report;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;

import java.io.*;
import java.util.*;

/**
 * Command-line tool to read PASS JSON submissions and write a spreadsheet report as CSV.
 *
 */
public class App 
{
    public static void main( String[] args )
    {
        if (args.length < 2) {
            System.out.println("Usage: java -jar pass-report-tool-1.0-SNAPSHOT-jar-with-dependencies.jar <input.json> <output.csv>");
            return;
        }
        String inputPath = args[0];
        String outputPath = args[1];
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(new File(inputPath));
            List<Map<String, String>> rows = extractRows(root);
            writeSpreadsheet(rows, outputPath);
            System.out.println("Spreadsheet written to " + outputPath);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static List<Map<String, String>> extractRows(JsonNode root) {
        List<Map<String, String>> rows = new ArrayList<>();
        Map<String, JsonNode> includedMap = new HashMap<>();
        if (root.has("included")) {
            for (JsonNode inc : root.get("included")) {
                String type = inc.get("type").asText();
                String id = inc.get("id").asText();
                includedMap.put(type + ":" + id, inc);
            }
        }
        for (JsonNode submission : root.get("data")) {
            Map<String, String> row = new HashMap<>();
            JsonNode attr = submission.get("attributes");
            row.put(Column.SUBMISSION_DATE.header(), attr.path("submittedDate").asText(""));
            // Repository names
            List<String> repoNames = new ArrayList<>();
            JsonNode repos = submission.path("relationships").path("repositories").path("data");
            if (repos.isArray()) {
                for (JsonNode repo : repos) {
                    String key = "repository:" + repo.get("id").asText();
                    JsonNode repoObj = includedMap.get(key);
                    if (repoObj != null) {
                        String name = repoObj.path("attributes").path("name").asText("");
                        if (!name.isEmpty()) repoNames.add(name);
                    }
                }
            }
            row.put(Column.REPOSITORY_NAMES.header(), String.join(", ", repoNames));
            // Article title, DOI, Journal name, Publisher name
            String title = "";
            String doi = "";
            String journal = "";
            String publisher = "";
            String funder = "";
            // Try to get from attributes.metadata (stringified JSON)
            if (attr.has("metadata")) {
                try {
                    JsonNode meta = new ObjectMapper().readTree(attr.get("metadata").asText(""));
                    title = meta.path("title").asText("");
                    doi = meta.path("doi").asText("");
                    journal = meta.path("journal-title").asText("");
                    publisher = meta.path("publisher").asText("");
                } catch (Exception ignored) {}
            }
            // Try to get from included publication
            JsonNode pubRel = submission.path("relationships").path("publication").path("data");
            if (!pubRel.isMissingNode()) {
                String pubKey = "publication:" + pubRel.path("id").asText();
                JsonNode pubObj = includedMap.get(pubKey);
                if (pubObj != null && pubObj.has("attributes")) {
                    JsonNode pubAttr = pubObj.get("attributes");
                    if (title.isEmpty()) title = pubAttr.path("title").asText("");
                    if (doi.isEmpty()) doi = pubAttr.path("doi").asText("");
                    if (journal.isEmpty()) journal = pubAttr.path("title").asText("");
                }
            }
            // Funder name (from grant->primaryFunder or directFunder)
            Set<String> funderNames = new LinkedHashSet<>();
            JsonNode grants = submission.path("relationships").path("grants").path("data");
            if (grants.isArray()) {
                for (JsonNode grant : grants) {
                    String grantKey = "grant:" + grant.path("id").asText();
                    JsonNode grantObj = includedMap.get(grantKey);
                    if (grantObj != null && grantObj.has("relationships")) {
                        // Check directFunder
                        JsonNode df = grantObj.path("relationships").path("directFunder").path("data");
                        if (!df.isMissingNode() && df.has("id")) {
                            String funderKey = "funder:" + df.path("id").asText();
                            JsonNode funderObj = includedMap.get(funderKey);
                            if (funderObj != null) {
                                String name = funderObj.path("attributes").path("name").asText("");
                                if (!name.isEmpty()) funderNames.add(name);
                            }
                        }
                        // Check primaryFunder
                        JsonNode pf = grantObj.path("relationships").path("primaryFunder").path("data");
                        if (!pf.isMissingNode() && pf.has("id")) {
                            String funderKey = "funder:" + pf.path("id").asText();
                            JsonNode funderObj = includedMap.get(funderKey);
                            if (funderObj != null) {
                                String name = funderObj.path("attributes").path("name").asText("");
                                if (!name.isEmpty()) funderNames.add(name);
                            }
                        }
                    }
                }
            }
            funder = String.join(", ", funderNames);
            row.put(Column.ARTICLE_TITLE.header(), title);
            row.put(Column.DOI.header(), doi);
            row.put(Column.JOURNAL_NAME.header(), journal);
            row.put(Column.FUNDER_NAME.header(), funder);
            row.put(Column.PUBLISHER_NAME.header(), publisher);
            rows.add(row);
        }
        return rows;
    }

    private static void writeSpreadsheet(List<Map<String, String>> rows, String outputPath) throws IOException {
        String[] columns = Column.headers();
        CsvMapper csvMapper = new CsvMapper();
        CsvSchema.Builder schemaBuilder = CsvSchema.builder();
        for (String col : columns) {
            schemaBuilder.addColumn(col);
        }
        CsvSchema schema = schemaBuilder.build().withHeader();
        try (Writer writer = new FileWriter(outputPath)) {
            csvMapper.writer(schema).writeValues(writer).writeAll(rows);
        }
    }
}
