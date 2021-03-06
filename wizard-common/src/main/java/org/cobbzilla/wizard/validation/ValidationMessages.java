package org.cobbzilla.wizard.validation;

import java.util.ResourceBundle;

public class ValidationMessages {

    public static final String VALIDATION_MESSAGES = "ValidationMessages";

    public static String translateMessage(String messageTemplate) {

        // strip leading/trailing curlies if they are there
        while (messageTemplate.startsWith("{")) messageTemplate = messageTemplate.substring(1);
        while (messageTemplate.endsWith("}")) messageTemplate = messageTemplate.substring(0, messageTemplate.length()-1);

        return ResourceBundle.getBundle(VALIDATION_MESSAGES).getString(messageTemplate);
    }
}
