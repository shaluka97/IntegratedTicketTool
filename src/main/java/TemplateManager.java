import java.io.FileNotFoundException;
import java.io.IOException;

public class TemplateManager {
    public static String getTemplate(String issueType) throws IOException {
        String fileName = "templates/" + issueType.toLowerCase() + "_description.txt";

        try (var inputStream = TemplateManager.class.getClassLoader().getResourceAsStream(fileName)) {
            if (inputStream == null) {
                throw new FileNotFoundException("Template file not found: " + fileName);
            }
            return new String(inputStream.readAllBytes());
        }
    }
}
