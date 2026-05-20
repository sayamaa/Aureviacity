package com.aurevia.cityexplorer.model;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class ContactForm {

    @NotBlank
    @Size(min = 2, max = 80)
    private String fullName;

    @NotBlank
    @Email
    @Size(max = 120)
    private String email;

    @Size(max = 80)
    private String city;

    @NotBlank
    @Size(min = 4, max = 120)
    private String subject;

    @NotBlank
    @Size(min = 10, max = 1000)
    private String message;

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
