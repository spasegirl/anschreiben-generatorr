package com.example.anschreiben;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Service responsible for orchestrating the generation of a cover letter.
 *
 * Workflow:
 * 1) Serialize the incoming {@link CoverLetterRequest} into JSON.
 * 2) Invoke a Python script (reads JSON via stdin, returns letter text via stdout).
 * 3) Convert the returned letter into a PDF using Apache PDFBox.
 *
 * If the Python script fails or produces no output, a fallback template is used.
 */
@Service
public class CoverLetterService {

    private final ObjectMapper objectMapper;
    private final Path scriptPath;

    // Fallback template if Python returns nothing or exits with non-zero.
    private static final String FALLBACK_TEMPLATE =
            """
            Sehr geehrte Damen und Herren,

            hiermit bewerbe ich mich um die Position als %s bei %s. \
            Ich bin überzeugt, dass meine Qualifikationen und Erfahrungen \
            eine wertvolle Bereicherung für Ihr Unternehmen darstellen.

            Mit freundlichen Grüßen
            %s""";

    public CoverLetterService(
            ObjectMapper objectMapper,
            @Value("${coverletter.script:python/generate_cover_letter.py}") String script
    ) {
        this.objectMapper = objectMapper;
        // Resolve the script path relative to the working directory
        this.scriptPath = Paths.get(script).toAbsolutePath().normalize();
    }

    /**
     * Generate a PDF cover letter based on the supplied request.
     *
     * @param request the applicant information and desired position
     * @return PDF bytes representing the generated letter
     * @throws IOException          if an I/O error occurs while invoking the Python script or creating the PDF
     * @throws InterruptedException if the Python process is interrupted
     */
    public byte[] generatePdf(CoverLetterRequest request) throws IOException, InterruptedException {
        // Convert the request into JSON for the Python script
        String jsonPayload = objectMapper.writeValueAsString(request);

        // Determine Python executable (env var override or default)
        String pythonCmd = System.getenv().getOrDefault("ANSCHREIBEN_PY", "python");

        // Resolve script path (with a relative fallback)
        Path scriptFile = scriptPath.toFile().exists()
                ? scriptPath
                : Paths.get("../python/generate_cover_letter.py").toAbsolutePath().normalize();

        System.out.println("Running script: " + scriptFile + " with " + pythonCmd);

        // Start the Python process with UTF-8 enforced end-to-end
        ProcessBuilder pb = new ProcessBuilder(pythonCmd, "-X", "utf8", scriptFile.toString());
        pb.directory(scriptFile.getParent().toFile());
        pb.redirectErrorStream(true); // stderr -> stdout, so we read a single stream

        // Environment guards (belt & suspenders)
        Map<String, String> env = pb.environment();
        env.put("PYTHONIOENCODING", "utf-8");
        env.put("PYTHONUTF8", "1");

        Process process = pb.start();

        // Send JSON to Python stdin (UTF-8)
        try (OutputStream os = process.getOutputStream()) {
            os.write(jsonPayload.getBytes(StandardCharsets.UTF_8));
            os.flush();
        }

        // Read combined stdout+stderr (UTF-8)
        String letterText;
        try (InputStream is = process.getInputStream()) {
            byte[] bytes = is.readAllBytes();
            letterText = new String(bytes, StandardCharsets.UTF_8).trim();
        }

        // Ensure process ended
        int exitCode = process.waitFor();

        // Fallback if Python failed or produced no output
        if (exitCode != 0 || letterText.isEmpty()) {
            letterText = String.format(
                    FALLBACK_TEMPLATE,
                    nullSafe(request.getJobTitle()),
                    nullSafe(request.getCompanyName()),
                    nullSafe(request.getApplicantName())
            );
        }

        // Render to PDF
        return renderPdf(letterText);
    }

    /**
     * Render the letter text as a simple PDF using PDFBox (Unicode-capable font).
     */
    private byte[] renderPdf(String letterText) throws IOException {
        try (PDDocument doc = new PDDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            // Load embedded Unicode font (supports umlauts, etc.)
            PDType0Font font;
            try (InputStream is = new ClassPathResource("fonts/DejaVuSans.ttf").getInputStream()) {
                font = PDType0Font.load(doc, is, true);
                System.out.println("✅ DejaVuSans.ttf loaded");
            }

            PDPage page = new PDPage();
            doc.addPage(page);

            float margin = 50f;
            float topY = page.getMediaBox().getHeight() - 60f;
            float bottom = 60f;
            float y = topY;

            float fontSize = 12f;
            float lineHeight = 16f;
            float maxWidth = page.getMediaBox().getWidth() - 2 * margin;

            PDPageContentStream cs = new PDPageContentStream(doc, page);

            // Date (right-aligned) in the top area
            String today = LocalDate.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy"));
            float dateWidth = font.getStringWidth(today) / 1000f * fontSize;

            cs.beginText();
            cs.setFont(font, fontSize);
            cs.newLineAtOffset(page.getMediaBox().getWidth() - margin - dateWidth, topY);
            cs.showText(today);
            cs.endText();

            // Start position for body text
            y = topY - (lineHeight * 3);

            // Render paragraph lines with word-wrap
            String[] logicalLines = letterText.split("\\R");
            for (String logical : logicalLines) {
                // Blank line -> paragraph spacing
                if (logical.trim().isEmpty()) {
                    y -= lineHeight;
                    continue;
                }

                List<String> wrapped = wrapLine(logical, font, fontSize, maxWidth);

                for (String w : wrapped) {
                    // Page break if needed
                    if (y - lineHeight < bottom) {
                        cs.close();
                        page = new PDPage();
                        doc.addPage(page);
                        cs = new PDPageContentStream(doc, page);
                        y = topY;
                    }

                    cs.beginText();
                    cs.setFont(font, fontSize);
                    cs.newLineAtOffset(margin, y);
                    cs.showText(w);
                    cs.endText();
                    y -= lineHeight;
                }
            }

            cs.close();
            doc.save(out);
            return out.toByteArray();
        }
    }

    /** Simple word-wrap routine for a single logical line. */
    private static List<String> wrapLine(String text, PDType0Font font, float fontSize, float maxWidth) throws IOException {
        List<String> out = new ArrayList<>();
        if (text == null || text.isBlank()) {
            out.add("");
            return out;
        }

        String[] words = text.split("\\s+");
        StringBuilder line = new StringBuilder();

        for (String w : words) {
            String candidate = line.isEmpty() ? w : line + " " + w;
            float width = font.getStringWidth(candidate) / 1000f * fontSize;

            if (width <= maxWidth) {
                line.setLength(0);
                line.append(candidate);
            } else {
                if (!line.isEmpty()) {
                    out.add(line.toString());
                }
                line.setLength(0);
                line.append(w);
            }
        }

        if (!line.isEmpty()) {
            out.add(line.toString());
        }
        return out;
    }

    private static String nullSafe(String s) {
        return s == null ? "" : s;
    }
}
