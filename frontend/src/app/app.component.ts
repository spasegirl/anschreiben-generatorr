import { Component } from '@angular/core';
import { HttpClient } from '@angular/common/http';

/**
 * Root component of the Anschreiben Generator frontend. This component
 * exposes a simple form bound to a `form` object containing all
 * necessary applicant and job information. When the form is submitted
 * it sends a POST request to the backend and initiates a file
 * download with the returned PDF.
 */
@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.css'],
})
export class AppComponent {
  form = {
    applicantName: '',
    applicantAddress: '',
    applicantEmail: '',
    applicantPhone: '',
    jobTitle: '',
    companyName: '',
    strengths: '',
    motivation: '',
  };

  generating = false;
  error: string | null = null;

  constructor(private http: HttpClient) {}

  /**
   * Sends the form data to the backend and handles the response. On
   * success a Blob representing the PDF is returned and offered as
   * a download. Errors are captured and displayed to the user.
   */
  submit(): void {
    this.generating = true;
    this.error = null;
    // Use an absolute URL to reach the Spring Boot backend. When
    // deploying both frontend and backend under the same host this
    // can be shortened to a relative path (e.g. "/api/generate").
    const url = 'http://localhost:8080/api/generate';
    this.http
      .post(url, this.form, { responseType: 'blob' })
      .subscribe({
        next: (blob) => {
          this.generating = false;
          // Create a temporary download link
          const url = window.URL.createObjectURL(blob);
          const a = document.createElement('a');
          a.href = url;
          a.download = 'Anschreiben.pdf';
          document.body.appendChild(a);
          a.click();
          document.body.removeChild(a);
          window.URL.revokeObjectURL(url);
        },
        error: (err) => {
          this.generating = false;
          if (err?.error instanceof Blob) {
            // Try to read the error text from the Blob
            const reader = new FileReader();
            reader.onload = () => {
              this.error = reader.result as string;
            };
            reader.readAsText(err.error);
          } else {
            this.error = err?.message || 'Fehler beim Generieren des Anschreibens';
          }
        },
      });
  }
}