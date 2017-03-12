package org.cobbzilla.wizard.validation;

import org.cobbzilla.util.string.ResourceMessages;

public class ValidationMessages extends ResourceMessages {

    private static final ValidationMessages instance = new ValidationMessages();

    @Override public String getBundleName() { return "ValidationMessages"; }

    public static String translateMessage(String messageTemplate) { return instance.translate(messageTemplate); }

}
