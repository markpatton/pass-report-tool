package patton.pass.report;

public enum Column {
    SUBMISSION_DATE("Submission date"),
    SUBMITTER_NAME("Submitter name"),
    SUBMITTER_EMAIL("Submitter email"),
    REPOSITORY_NAMES("Repository names"),
    ARTICLE_TITLE("Article title"),
    DOI("DOI"),
    JOURNAL_NAME("Journal name"),
    FUNDER_NAME("Funder name"),
    PUBLISHER_NAME("Publisher name");

    private final String header;

    Column(String header) {
        this.header = header;
    }

    public String header() {
        return header;
    }

    public static String[] headers() {
        Column[] cols = values();
        String[] headers = new String[cols.length];
        for (int i = 0; i < cols.length; i++) {
            headers[i] = cols[i].header();
        }
        return headers;
    }
}
