#!/usr/bin/env python3
"""
generate_cover_letter
=====================

This script acts as a lightweight natural language generator for German
cover letters (Anschreiben). It reads a JSON object from standard
input, containing information about the applicant and the targeted
position, and produces a well‑structured cover letter on standard
output. While this script does not rely on heavyweight machine
learning libraries, it mimics the behaviour of a large language model
by randomly choosing between different phrasings for each part of the
letter. This introduces variability and a more natural feel to the
resulting text.

The input JSON should contain at least the following keys:

```
{
  "applicantName": "Vorname Nachname",
  "applicantAddress": "Straße 1, 12345 Ort",
  "applicantEmail": "name@example.com",
  "applicantPhone": "+49 1234 5678",
  "jobTitle": "Jobtitel",
  "companyName": "Unternehmensname",
  "strengths": "Ihre Stärken und Qualifikationen",
  "motivation": "Was Sie motiviert"
}
```

If some fields are missing, the script will still attempt to produce
a letter by skipping or simplifying the corresponding sentences.

This script is intentionally kept dependency‑free so that it can run
in constrained environments where installing external Python
libraries is not possible.
"""

import json
import random
import sys
from typing import Dict, List






def choose_sentence(templates: List[str], context: Dict[str, str]) -> str:
    """Randomly choose a template and format it with the given context.

    :param templates: a list of string templates
    :param context: a dictionary used for formatting the templates
    :return: a formatted string
    """
    template = random.choice(templates)
    try:
        return template.format(**context)
    except Exception:
        # If formatting fails due to missing keys, return the raw template
        return template


def generate_letter(data: Dict[str, str]) -> str:
    """Generate a cover letter from the provided data.

    The letter follows a simple structure: salutation, introduction,
    strengths/skills, motivation, closing, and signature. Each section
    uses a pool of alternative phrasings to add variability.

    :param data: dictionary containing applicant and position information
    :return: a string containing the generated letter
    """
    # Extract data with defaults to avoid KeyErrors
    applicant_name = data.get("applicantName", "").strip()
    applicant_address = data.get("applicantAddress", "").strip()
    applicant_email = data.get("applicantEmail", "").strip()
    applicant_phone = data.get("applicantPhone", "").strip()
    job_title = data.get("jobTitle", "").strip()
    company_name = data.get("companyName", "").strip()
    strengths = data.get("strengths", "").strip()
    motivation = data.get("motivation", "").strip()

    # Define template pools for each section
    introductions = [
        "Mit großem Interesse habe ich die ausgeschriebene Position als {jobTitle} bei {companyName} gelesen.",
        "Die ausgeschriebene Stelle als {jobTitle} bei {companyName} hat mein besonderes Interesse geweckt.",
        "Als erfahrene Fachkraft im Bereich {jobTitle} bin ich besonders an der Position bei {companyName} interessiert."
    ]

    strength_sentences = [
        "Mit meiner {strengths} bin ich überzeugt, dass ich die Anforderungen der Position erfülle.",
        "Dank meiner {strengths} sehe ich mich als ideale Besetzung für diese Stelle.",
        "Ich verfüge über {strengths}, die mich befähigen, die mir übertragenen Aufgaben erfolgreich zu erfüllen."
    ]

    motivation_sentences = [
        "Was mich besonders motiviert, ist {motivation}.",
        "Besonders reizvoll an dieser Stelle finde ich {motivation}.",
        "Meine Motivation für diese Position ergibt sich aus {motivation}."
    ]

    closing_sentences = [
        "Ich freue mich darauf, Sie in einem persönlichen Gespräch kennenzulernen.",
        "Gerne überzeuge ich Sie in einem Vorstellungsgespräch von meinen Fähigkeiten.",
        "Über eine Einladung zu einem persönlichen Gespräch würde ich mich sehr freuen."
    ]

    context = {
        "jobTitle": job_title,
        "companyName": company_name,
        "strengths": strengths,
        "motivation": motivation
    }

    # Build the letter parts
    salutation = "Sehr geehrte Damen und Herren,"
    intro = choose_sentence(introductions, context)
    strength_part = choose_sentence(strength_sentences, context) if strengths else ""
    motivation_part = choose_sentence(motivation_sentences, context) if motivation else ""
    closing = choose_sentence(closing_sentences, context)

    # Assemble the letter with blank lines between sections
    parts = [salutation, "", intro]
    if strength_part:
        parts.extend(["", strength_part])
    if motivation_part:
        parts.extend(["", motivation_part])
    parts.extend(["", closing, "", "Mit freundlichen Grüßen", applicant_name])

    # Prepend contact information if available
    if applicant_address or applicant_email or applicant_phone:
        contact_lines = [applicant_name]
        if applicant_address:
            contact_lines.append(applicant_address)
        if applicant_email:
            contact_lines.append(applicant_email)
        if applicant_phone:
            contact_lines.append(applicant_phone)
        parts = ["\n".join(contact_lines), ""] + parts

    return "\n".join(part for part in parts if part is not None)


def main():
    try:
        raw = sys.stdin.read()
        if not raw:
            raise ValueError("No input provided to the letter generator")
        data = json.loads(raw)
        letter = generate_letter(data)
        sys.stdout.write(letter)
    except Exception as exc:
        # On any error print nothing to stdout and log the error to stderr
        sys.stderr.write(f"Error generating letter: {exc}\n")
        sys.exit(1)


if __name__ == "__main__":
    main()