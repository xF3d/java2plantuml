package buildapi;

import buildapi.result.TestResult;
import lombok.experimental.UtilityClass;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilderFactory;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;

@UtilityClass
public class TestParser {

    public static TestResult parseJUnitReports(Path reports) {

        int total = 0;
        int failures = 0;
        int errors = 0;
        int skipped = 0;

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(reports, "TEST-*.xml")) {

            for (Path file : stream) {
                Document doc = DocumentBuilderFactory.newInstance()
                        .newDocumentBuilder()
                        .parse(file.toFile());

                Element suite = doc.getDocumentElement();

                total += Integer.parseInt(suite.getAttribute("tests"));
                failures += Integer.parseInt(suite.getAttribute("failures"));
                errors += Integer.parseInt(suite.getAttribute("errors"));
                skipped += Integer.parseInt(suite.getAttribute("skipped"));
            }
        } catch (Exception e) {
            return TestResult.fail(e.getMessage());
        }

        int passed = total - failures - errors - skipped;
        return new TestResult(true, total, passed, failures, skipped, null);
    }
}
