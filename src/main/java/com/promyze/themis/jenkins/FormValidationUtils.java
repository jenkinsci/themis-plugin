package com.promyze.themis.jenkins;

import hudson.util.FormValidation;

public class FormValidationUtils {

    public FormValidationUtils() {
        // private constructor for utility class
    }

    public static void checkNotNullOrEmpty(String value, String message) throws FormValidation {
        if (value == null || value.length() == 0) {
            throw FormValidation.error(message);
        }
    }

}
