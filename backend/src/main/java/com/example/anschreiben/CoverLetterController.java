package com.example.anschreiben;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller exposing an endpoint to generate a PDF cover letter.
 *
 * <p>The frontend sends a POST request with JSON describing the
 * applicant and the target position. The controller delegates the
 * generation of the letter to the {@link CoverLetterService} and
 * returns the resulting PDF document as a binary download. For
 * simplicity this controller enables Cross‑Origin Resource Sharing
 * (CORS) for all origins; in a production environment this should
 * be restricted appropriately.</p>
 */
@RestController
@CrossOrigin
public class CoverLetterController {

    private final CoverLetterService coverLetterService;

    public CoverLetterController(CoverLetterService coverLetterService) {
        this.coverLetterService = coverLetterService;
    }

    /**
     * Generate a cover letter PDF.
     *
     * @param request the applicant information provided by the frontend
     * @return a ResponseEntity containing the generated PDF
     */
    @PostMapping(value = "/api/generate", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<byte[]> generate(@RequestBody CoverLetterRequest request) {
        try {
            byte[] pdf = coverLetterService.generatePdf(request);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", "Anschreiben.pdf");
            return new ResponseEntity<>(pdf, headers, HttpStatus.OK);
        } catch (Exception e) {
            // In case of unexpected errors return a 500 error with the message
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .contentType(MediaType.TEXT_PLAIN)
                    .body(("Fehler beim Generieren des Anschreibens: " + e.getMessage()).getBytes());
        }
    }
}