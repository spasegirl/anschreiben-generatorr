# 📨 Cover Letter Generator

This project implements a simple **web application for creating cover letters** (job application letters).  
It consists of three main components:

---

## 🧩 Components

### 🟦 Java / Spring Boot Backend (`backend/`)
Provides a REST API that receives applicant data, invokes a Python generator, and returns the generated cover letter as a PDF.

### 🐍 Python Script (`python/generate_cover_letter.py`)
Generates a natural-sounding cover letter text based on the provided data.  
It simulates the stylistic variance of a Large Language Model (LLM) by randomly selecting between multiple phrasing options.

### 🅰️ Angular Frontend (`frontend/`)
Offers a simple form for entering applicant information and downloads the generated PDF.

---

## ⚙️ Requirements

- **Java 17** – to run the Spring Boot backend (e.g., via OpenJDK or AdoptOpenJDK)
- **Maven** – to build and run the backend (`mvn spring-boot:run`)
- **Python 3** – for the generator script (in the `python/` folder); no external libraries required
- **Node.js + Angular CLI (≥ v17)** – to run the frontend  
  *(Alternatively, the frontend can be built and served as static HTML/JS files on any web server.)*

---

## 📁 Project Structure

```
cover-letter-generator/
├── backend/               # Spring Boot service (Java)
│   ├── pom.xml            # Maven configuration
│   └── src/main/java/
│       └── com/example/anschreiben/
│           ├── AnschreibenBackendApplication.java
│           ├── CoverLetterController.java
│           ├── CoverLetterRequest.java
│           └── CoverLetterService.java
├── python/
│   └── generate_cover_letter.py   # Generates the cover letter text
├── frontend/             # Angular client (TypeScript / HTML / CSS)
│   ├── package.json
│   ├── angular.json
│   ├── tsconfig*.json
│   └── src/
│       ├── index.html
│       ├── main.ts
│       └── app/
│           ├── app.module.ts
│           ├── app.component.ts
│           ├── app.component.html
│           └── app.component.css
└── README.md
```

---

## 🚀 Running the Backend

Navigate to the `backend` directory and start the application with Maven:

```bash
cd backend
mvn spring-boot:run
```

The REST service will then be available at [http://localhost:8080](http://localhost:8080).

### API Endpoint

`POST http://localhost:8080/api/generate`

#### Example Request Body
```json
{
  "applicantName": "Max Mustermann",
  "applicantAddress": "Example Street 1, 12345 Example City",
  "applicantEmail": "max@example.com",
  "applicantPhone": "+49 123 456789",
  "jobTitle": "Software Engineer",
  "companyName": "Example Company GmbH",
  "strengths": "Teamwork, Java and Angular experience",
  "motivation": "the opportunity to develop innovative products"
}
```

#### Response
A PDF file containing the generated cover letter.

---

## 💻 Running the Frontend

Install dependencies and start the Angular development server:

```bash
cd frontend
npm install
npm start
```

The client will be available at [http://localhost:4200](http://localhost:4200).

> **Note:** In `app.component.ts`, the backend URL is hardcoded as  
> `http://localhost:8080/api/generate`.  
> If you host backend and frontend differently or use a proxy, adjust this URL accordingly.

---

## 🧾 PDF Generation

The Java backend uses **Apache PDFBox** to convert the text from the Python script into a PDF document.  
The text is written line-by-line and automatically continues on new pages if needed.  
The resulting file is named **`Anschreiben.pdf`** by default.

---

## 📝 Notes

- The Python text generation is intentionally simple and does **not** replace a real LLM.  
  For production use, you can integrate an external API (e.g., OpenAI or a local Hugging Face model).

- The **CORS configuration** in the controller is currently permissive.  
  Restrict allowed origins for production environments.

- When adding or renaming form fields, update the corresponding:
    - Frontend form
    - Backend request class (`CoverLetterRequest`)
    - Python script parameters

---

## 💡 Tips

- Ensure UTF-8 encoding for all components to correctly display special characters (ä, ö, ü, ß).
- Use a font like **DejaVuSans.ttf** (already included) for full Unicode support in PDFs.

---


**Good luck creating your cover letter! ;)**
