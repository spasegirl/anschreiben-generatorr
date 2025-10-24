package com.example.anschreiben;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.springframework.core.io.ClassPathResource;


/**
 * Service responsible for orchestrating the generation of a cover letter.
 *
 * <p>The workflow is as follows:
 * <ol>
 *   <li>Serialize the incoming {@link CoverLetterRequest} into JSON.</li>
 *   <li>Invoke a Python script which consumes the JSON on its standard
 *       input and emits a generated letter to its standard output.</li>
 *   <li>Convert the returned plain‑text letter into a PDF using
 *       Apache PDFBox.</li>
 * </ol>
 * If the Python script fails or produces no output a simple fallback
 * template is used.</p>
 */
@Service
public class CoverLetterService {

    private final ObjectMapper objectMapper;
    private final Path scriptPath;

    // Fallback template used when the Python script does not return
    // any content. It uses basic string formatting placeholders which
    // are replaced with request data at runtime.
    private static final String FALLBACK_TEMPLATE =
            """
                    Sehr geehrte Damen und Herren,
                    
                    hiermit bewerbe ich mich um die Position als %s bei %s. \
                    Ich bin überzeugt, dass meine Qualifikationen und Erfahrungen \
                    eine wertvolle Bereicherung für Ihr Unternehmen darstellen.
                    
                    Mit freundlichen Grüßen
                    %s""";

    public CoverLetterService(ObjectMapper objectMapper,
                              @Value("${coverletter.script:python/generate_cover_letter.py}") String script) {
        this.objectMapper = objectMapper;
        // Resolve the script path relative to the working directory
        this.scriptPath = Paths.get(script).toAbsolutePath().normalize();
    }

    /**
     * Generate a PDF cover letter based on the supplied request.
     *
     * @param request the applicant information and desired position
     * @return PDF bytes representing the generated letter
     * @throws IOException          if an I/O error occurs while invoking the Python
     *                              script or constructing the PDF
     * @throws InterruptedException if the Python process is interrupted
     */
    public byte[] generatePdf(CoverLetterRequest request) throws IOException, InterruptedException {
        // Convert the request into JSON which the Python script can consume
        String jsonPayload = objectMapper.writeValueAsString(request);

        // Start the Python process. The script reads from stdin and writes to stdout.
// Determine which Python command to use
        String pythonCmd = System.getenv().getOrDefault("ANSCHREIBEN_PY", "python");

// Resolve script path; handle relative "../python" fallback
        Path scriptFile = scriptPath.toFile().exists()
                ? scriptPath
                : Paths.get("../python/generate_cover_letter.py").toAbsolutePath().normalize();

        System.out.println("Running script: " + scriptFile + " with " + pythonCmd);

// Start the Python process
        ProcessBuilder pb = new ProcessBuilder(pythonCmd, scriptFile.toString());
        // Set working directory to the script's folder
        pb.directory(scriptFile.getParent().toFile());

// Capture stderr for debugging
        pb.redirectErrorStream(true);
        Process process = pb.start();

        // Capture stderr (Fehlerausgabe) für Debugging
        try (InputStream es = process.getErrorStream()) {
            String errText = new String(es.readAllBytes(), StandardCharsets.UTF_8);
            if (!errText.isBlank()) {
                System.err.println("PYTHON-ERROR:\n" + errText);
            }
        }


        // Write the JSON to the process' stdin
        try (OutputStream os = process.getOutputStream()) {
            os.write(jsonPayload.getBytes(StandardCharsets.UTF_8));
            os.flush();
        }

        // Capture the generated letter from stdout
        String letterText;
        try (InputStream is = process.getInputStream()) {
            byte[] bytes = is.readAllBytes();
            letterText = new String(bytes, StandardCharsets.UTF_8).trim();
        }

        // Wait for the process to finish to avoid zombies
        int exitCode = process.waitFor();

        // Use fallback letter if the script failed or returned no text
        if (exitCode != 0 || letterText.isEmpty()) {
            letterText = String.format(FALLBACK_TEMPLATE,
                    request.getJobTitle(),
                    request.getCompanyName(),
                    request.getApplicantName());
        }

        // Render the letter into a PDF and return the bytes
        return renderPdf(letterText);
    }

    /**
     * Create a simple PDF document from the given text using PDFBox.
     *
     * <p>This method splits the letter into individual lines and writes
     * them onto pages. If the text exceeds one page (approx. 50 lines
     * depending on font size) additional pages are appended. All text
     * rendering uses the built‑in Helvetica font to avoid external
     * resource dependencies.</p>
     *
     * @param letterText plain text of the letter
     * @return a byte array containing the PDF document
     * @throws IOException if writing the PDF fails
     */
    private byte[] renderPdf(String letterText) throws IOException {
        try (PDDocument doc = new PDDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            // Unicode-fähige Fonts laden (eingebettet)
            PDType0Font fontRegular;
            PDType0Font fontBold = null;
            try (InputStream is = new ClassPathResource("fonts/DejaVuSans.ttf").getInputStream()) {
                fontRegular = PDType0Font.load(doc, is, true);
            }
            try {
                InputStream boldIs = new ClassPathResource("fonts/DejaVuSans-Bold.ttf").getInputStream();
                fontBold = PDType0Font.load(doc, boldIs, true);
                boldIs.close();
            } catch (Exception ignore) { /* optional */ }

            float fontSize = 12f;
            float lineHeight = 16f;

            PDPage page = new PDPage();
            doc.addPage(page);

            float margin = 50f;
            float usableWidth = page.getMediaBox().getWidth() - 2 * margin;
            float y = page.getMediaBox().getHeight() - 60f;

            PDPageContentStream cs = new PDPageContentStream(doc, page);

            // Absenderblock (aus dem generierten Text die ersten 3–4 Zeilen; optional: du kannst die Felder direkt verwenden)
            cs.beginText();
            cs.setFont(fontRegular, fontSize);
            cs.newLineAtOffset(margin, y);

            // Wenn du stattdessen die Felder aus request oben haben willst, gib sie als Parameter rein.
            // Hier nehmen wir die ersten 4 Zeilen des Briefs (Name, Adresse, E-Mail, Telefon), wenn vorhanden:
            String[] lines = letterText.split("\\R");
            int headerLines = Math.min(4, lines.length);
            for (int i = 0; i < headerLines; i++) {
                showWrappedLine(cs, lines[i], fontRegular, fontSize, usableWidth, lineHeight);
            }

            // Abstand nach Absender
            cs.newLineAtOffset(0, -lineHeight);

            // Datum rechts (optional)
            String today = java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy"));
            cs.endText();
            // Rechtsbündig Datum
            float dateWidth = fontRegular.getStringWidth(today) / 1000f * fontSize;
            cs.beginText();
            cs.setFont(fontRegular, fontSize);
            cs.newLineAtOffset(page.getMediaBox().getWidth() - margin - dateWidth, y - (headerLines + 2) * lineHeight);
            cs.showText(today);
            cs.endText();

            // Start Textkörper mit etwas Abstand
            y = y - (headerLines + 5) * lineHeight;
            cs.beginText();
            cs.setFont(fontBold != null ? fontBold : fontRegular, 14f);
            cs.newLineAtOffset(margin, y);
            // Betreff (falls du ihn aus Feldern generieren willst, kannst du hier z. B. "Bewerbung als " + jobTitle nehmen)
            String betreff = "Bewerbung";
            cs.showText(betreff);
            cs.endText();

            y -= 2 * lineHeight;

            cs.beginText();
            cs.setFont(fontRegular, fontSize);
            cs.newLineAtOffset(margin, y);

            // Rest des Briefes (ab Zeile 5) als Absätze rendern
            String body = rebuildBodyFrom(lines); // alles ab 5. Zeile
            for (String paragraph : body.split("\\R\\s*\\R")) { // leere Zeile trennt Absätze
                for (String wrapped : wrap(paragraph, fontRegular, fontSize, usableWidth)) {
                    // Seitenumbruch bei Bedarf
                    if (y < 70f) {
                        cs.endText();
                        cs.close();
                        page = new PDPage();
                        doc.addPage(page);
                        y = page.getMediaBox().getHeight() - 60f;
                        cs = new PDPageContentStream(doc, page);
                        cs.beginText();
                        cs.setFont(fontRegular, fontSize);
                        cs.newLineAtOffset(margin, y);
                    }
                    cs.showText(wrapped);
                    cs.newLineAtOffset(0, -lineHeight);
                    y -= lineHeight;
                }
                // Absatzabstand
                cs.newLineAtOffset(0, -lineHeight);
                y -= lineHeight;
            }

            cs.endText();
            cs.close();

            doc.save(out);
            return out.toByteArray();
        }
    }

    /** Baut den Body ab einer Startzeile wieder zusammen */
    private static String rebuildBodyFrom(String[] lines) {
        StringBuilder sb = new StringBuilder();
        for (int i = 4; i < lines.length; i++) {
            sb.append(lines[i]);
            if (i < lines.length - 1) sb.append(System.lineSeparator());
        }
        return sb.toString();
    }

    /** Zeigt eine einzelne Zeile, macht aber intern Wrap, falls die Zeile zu lang ist */
    private static void showWrappedLine(PDPageContentStream cs, String text, PDType0Font font, float fontSize,
                                        float maxWidth, float lineHeight) throws IOException {
        for (String w : wrap(text, font, fontSize, maxWidth)) {
            cs.showText(w);
            cs.newLineAtOffset(0, -lineHeight);
        }
    }

    /** Simples Word-Wrap nach verfüg­barer Breite */
    private static java.util.List<String> wrap(String text, PDType0Font font, float fontSize, float maxWidth) throws IOException {
        java.util.List<String> out = new java.util.ArrayList<>();
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
                if (!line.isEmpty()) out.add(line.toString());
                line.setLength(0);
                line.append(w);
            }
        }
        if (!line.isEmpty()) out.add(line.toString());
        return out;
    }

}