package org.cobbzilla.wizard.form.server;

import org.cobbzilla.wizard.server.RestServerBase;

public class FormApiServer extends RestServerBase<FormApiConfiguration> {

    public static void main(String[] args) throws Exception {
        main(args, FormApiServer.class);
    }

}
