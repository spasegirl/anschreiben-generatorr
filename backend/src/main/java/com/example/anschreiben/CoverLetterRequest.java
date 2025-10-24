package com.example.anschreiben;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Data transfer object representing a cover letter request from the
 * frontend. The fields correspond to typical pieces of information
 * required to compose a German job application letter (Anschreiben).
 */
public class CoverLetterRequest {
    private String applicantName;
    private String applicantAddress;
    private String applicantEmail;
    private String applicantPhone;
    private String jobTitle;
    private String companyName;
    private String strengths;
    private String motivation;

    @JsonCreator
    public CoverLetterRequest(
            @JsonProperty("applicantName") String applicantName,
            @JsonProperty("applicantAddress") String applicantAddress,
            @JsonProperty("applicantEmail") String applicantEmail,
            @JsonProperty("applicantPhone") String applicantPhone,
            @JsonProperty("jobTitle") String jobTitle,
            @JsonProperty("companyName") String companyName,
            @JsonProperty("strengths") String strengths,
            @JsonProperty("motivation") String motivation) {
        this.applicantName = applicantName;
        this.applicantAddress = applicantAddress;
        this.applicantEmail = applicantEmail;
        this.applicantPhone = applicantPhone;
        this.jobTitle = jobTitle;
        this.companyName = companyName;
        this.strengths = strengths;
        this.motivation = motivation;
    }

    public CoverLetterRequest() {
        // Default constructor for deserialization
    }

    public String getApplicantName() {
        return applicantName;
    }

    public void setApplicantName(String applicantName) {
        this.applicantName = applicantName;
    }

    public String getApplicantAddress() {
        return applicantAddress;
    }

    public void setApplicantAddress(String applicantAddress) {
        this.applicantAddress = applicantAddress;
    }

    public String getApplicantEmail() {
        return applicantEmail;
    }

    public void setApplicantEmail(String applicantEmail) {
        this.applicantEmail = applicantEmail;
    }

    public String getApplicantPhone() {
        return applicantPhone;
    }

    public void setApplicantPhone(String applicantPhone) {
        this.applicantPhone = applicantPhone;
    }

    public String getJobTitle() {
        return jobTitle;
    }

    public void setJobTitle(String jobTitle) {
        this.jobTitle = jobTitle;
    }

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public String getStrengths() {
        return strengths;
    }

    public void setStrengths(String strengths) {
        this.strengths = strengths;
    }

    public String getMotivation() {
        return motivation;
    }

    public void setMotivation(String motivation) {
        this.motivation = motivation;
    }
}