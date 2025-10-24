import 'zone.js';
import { bootstrapApplication } from '@angular/platform-browser';
import { Component } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { HttpClient, HttpClientModule } from '@angular/common/http';

@Component({
    selector: 'app-root',
    standalone: true,
    imports: [FormsModule, HttpClientModule],
    template: `
    <div class="container">
      <h1>Anschreiben-Generator</h1>

      <form (ngSubmit)="generate()" #formRef="ngForm">
        <label>
          Name:
          <input type="text" [(ngModel)]="data.applicantName" name="applicantName" required />
        </label>

        <label>
          Adresse:
          <input type="text" [(ngModel)]="data.applicantAddress" name="applicantAddress" />
        </label>

        <label>
          E-Mail:
          <input type="email" [(ngModel)]="data.applicantEmail" name="applicantEmail" />
        </label>

        <label>
          Telefon:
          <input type="text" [(ngModel)]="data.applicantPhone" name="applicantPhone" />
        </label>

        <label>
          Jobtitel:
          <input type="text" [(ngModel)]="data.jobTitle" name="jobTitle" required />
        </label>

        <label>
          Firmenname:
          <input type="text" [(ngModel)]="data.companyName" name="companyName" />
        </label>

        <label>
          Stärken:
          <textarea [(ngModel)]="data.strengths" name="strengths"></textarea>
        </label>

        <label>
          Motivation:
          <textarea [(ngModel)]="data.motivation" name="motivation"></textarea>
        </label>

        <button type="submit" [disabled]="loading || !formRef.form.valid">
          {{ loading ? 'Generiere...' : 'Anschreiben generieren' }}
        </button>

        <p class="error" *ngIf="error">{{ error }}</p>
      </form>
    </div>
  `,
    styleUrls: ['./styles.css']
})
class AppComponent {
    data = {
        applicantName: '',
        applicantAddress: '',
        applicantEmail: '',
        applicantPhone: '',
        jobTitle: '',
        companyName: '',
        strengths: '',
        motivation: ''
    };
    loading = false;
    error = '';

    constructor(private http: HttpClient) {}

    generate() {
        this.loading = true;
        this.error = '';

        this.http
            .post('http://localhost:8080/api/generate', this.data, { responseType: 'blob' })
            .subscribe({
                next: (blob) => {
                    const url = window.URL.createObjectURL(blob);
                    const a = document.createElement('a');
                    a.href = url;
                    a.download = 'Anschreiben.pdf';
                    a.click();
                    window.URL.revokeObjectURL(url);
                    this.loading = false;
                },
                error: (err) => {
                    console.error(err);
                    this.error = 'Fehler beim Generieren des Anschreibens.';
                    this.loading = false;
                }
            });
    }
}

bootstrapApplication(AppComponent)
    .then(() => console.log('>>> Anschreiben-App gestartet <<<'))
    .catch(err => console.error('Bootstrap-Fehler', err));
