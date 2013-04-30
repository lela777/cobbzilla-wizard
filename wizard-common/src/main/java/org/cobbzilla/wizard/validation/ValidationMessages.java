package org.cobbzilla.wizard.validation;

import java.util.ResourceBundle;

public class ValidationMessages {

    public static final String VALIDATION_MESSAGES = "ValidationMessages";

    public static String translateMessage(String messageTemplate) {

        // strip leading/trailing curlies if they are there
        if (messageTemplate.startsWith("{")) messageTemplate = messageTemplate.substring(1);
        if (messageTemplate.endsWith("}")) messageTemplate = messageTemplate.substring(messageTemplate.length()-1);

        return ResourceBundle.getBundle(VALIDATION_MESSAGES).getString(messageTemplate);
    }
}
