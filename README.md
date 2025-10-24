# Anschreiben Generator

Dieses Projekt implementiert eine einfache Webanwendung zur Erstellung von Anschreiben (Bewerbungsschreiben). Es besteht aus drei Komponenten:

1. **Java/Spring Boot Backend** (`backend`): Stellt eine REST‑Schnittstelle bereit, die Daten des Bewerbers entgegennimmt, einen Python‑Generator aufruft und das erzeugte Anschreiben als PDF zurückliefert.
2. **Python‑Skript** (`python/generate_cover_letter.py`): Erzeugt aus den übergebenen Daten ein natürlich klingendes Anschreiben. Es simuliert dabei die Varianz eines Large Language Models, indem es aus verschiedenen Formulierungen zufällig auswählt.
3. **Angular Frontend** (`frontend`): Bietet ein einfaches Formular zur Eingabe der Bewerberdaten und lädt das erzeugte PDF als Download herunter.

## Voraussetzungen

- **Java 17** zum Ausführen des Spring‑Backends (z. B. via OpenJDK oder AdoptOpenJDK).
- **Maven** zum Bauen des Backends (`mvn spring-boot:run`).
- **Python 3** für das Generator‑Skript (liegt im Ordner `python`). Das Skript benötigt keine externen Bibliotheken.
- **Node.js** und **Angular CLI** (ab Version 17) zum Starten des Frontends. Alternativ kann das Frontend nach dem Build als statische HTML/JS‑Dateien auf jedem Webserver liegen.

## Projektstruktur

```text
anschreiben-generator/
├── backend/               # Spring‑Boot‑Service (Java)
│   ├── pom.xml            # Maven‑Konfiguration
│   └── src/main/java/
│       └── com/example/anschreiben/
│           ├── AnschreibenBackendApplication.java
│           ├── CoverLetterController.java
│           ├── CoverLetterRequest.java
│           └── CoverLetterService.java
├── python/
│   └── generate_cover_letter.py   # Erzeugt den Brieftext
├── frontend/             # Angular‑Client (Typescript/HTML/CSS)
│   ├── package.json
│   ├── angular.json
│   ├── tsconfig*.json
│   └── src/
│       ├── index.html
│       ├── main.ts
│       └── app/
│           ├── app.module.ts
│           ├── app.component.ts
│           ├── app.component.html
│           └── app.component.css
└── README.md
```

## Backend starten

Wechseln Sie in das Unterverzeichnis `backend` und starten Sie die Anwendung mit Maven:

```bash
cd backend
mvn spring-boot:run
```

Der REST‑Dienst läuft anschließend unter `http://localhost:8080`. Die API erwartet einen POST‑Request an `http://localhost:8080/api/generate` mit einem JSON‑Body nach folgendem Schema:

```json
{
  "applicantName": "Max Mustermann",
  "applicantAddress": "Musterstraße 1, 12345 Musterstadt",
  "applicantEmail": "max@example.com",
  "applicantPhone": "+49 123 456789",
  "jobTitle": "Softwareentwickler",
  "companyName": "Beispielfirma GmbH",
  "strengths": "Teamfähigkeit, Java und Angular Erfahrungen",
  "motivation": "die Möglichkeit, innovative Produkte zu entwickeln"
}
```

Als Antwort liefert der Dienst eine PDF‑Datei, die das erzeugte Anschreiben enthält.

## Frontend starten

Um das Frontend lokal zu entwickeln, installieren Sie zunächst die Abhängigkeiten und starten Sie anschließend den Entwicklungsserver:

```bash
cd frontend
npm install
npm start
```

Der Client ist dann unter `http://localhost:4200` erreichbar. Bitte beachten Sie, dass in der Datei `app.component.ts` die URL des Backends auf `http://localhost:8080/api/generate` fest verdrahtet ist. Wenn Sie Backend und Frontend anders hosten oder einen Proxy verwenden, passen Sie diese URL entsprechend an.

## PDF‑Erzeugung

Das Java‑Backend nutzt [Apache PDFBox](https://pdfbox.apache.org/), um den vom Python‑Skript gelieferten Text in ein PDF‑Dokument umzuwandeln. Das Dokument wird zeilenweise geschrieben und bei Bedarf auf mehrere Seiten aufgeteilt. Die Datei trägt standardmäßig den Namen `Anschreiben.pdf`.

## Hinweise

- Die Python‑Generierung ist bewusst simpel gehalten und ersetzt kein echtes Sprachmodell. Für ein produktives System kann das Skript durch eine Anbindung an ein externes LLM (z. B. OpenAI API oder ein lokales Hugging Face‑Modell) ersetzt werden.
- Die CORS‑Konfiguration im Controller ist sehr offen. In produktiven Umgebungen sollten die erlaubten Ursprünge eingeschränkt werden.
- Bei Anpassungen an den Datenfeldern im Formular müssen die entsprechenden Klassen im Backend sowie das Python‑Skript aktualisiert werden.

Viel Erfolg beim Erstellen Ihres Anschreibens!