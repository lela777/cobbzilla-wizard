package org.cobbzilla.wizard.dao;

import lombok.Getter;
import org.apache.commons.exec.CommandLine;
import org.cobbzilla.util.system.Command;
import org.cobbzilla.util.system.CommandResult;
import org.cobbzilla.util.system.CommandShell;
import org.cobbzilla.wizard.model.LdapEntity;
import org.cobbzilla.wizard.model.ResultPage;
import org.cobbzilla.wizard.server.config.LdapConfiguration;

import javax.validation.Valid;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import static org.cobbzilla.util.daemon.ZillaRuntime.*;
import static org.cobbzilla.util.reflect.ReflectionUtil.getFirstTypeParam;
import static org.cobbzilla.util.reflect.ReflectionUtil.instantiate;
import static org.cobbzilla.util.system.CommandShell.okResult;

public abstract class AbstractLdapDAO<E extends LdapEntity> implements DAO<E> {

    @Getter(lazy=true) private final E templateObject = initTemplate();
    private E initTemplate() { return (E) instantiate(getFirstTypeParam(getClass(), LdapEntity.class)); }

    protected abstract LdapConfiguration getLdapConfiguration();
    protected String getParentDn() { return getTemplateObject().getParentDn(); }

    // convenience method
    private LdapConfiguration ldap() { return getLdapConfiguration(); }

    // not supported
    @Override public Class<? extends Map<String, String>> boundsClass() { return notSupported(); }

    @Override public SearchResults<E> search(ResultPage resultPage) {
        final String ldif = okResult(root_search(resultPage.getFilter(), getParentDn(), resultPage.getSortField(), resultPage.getSortType())).getStdout();
        final List<E> matches = multiFromLdif(ldif);
        final SearchResults results = new SearchResults().setTotalCount(matches.size());
        for (int i=resultPage.getPageOffset(); i<matches.size() && i<resultPage.getPageEndOffset(); i++) {
            results.addResult(matches.get(i));
        }
        return results;
    }

    @Override public List<E> findAll() {
        final String ldif = okResult(root_search(getTemplateObject().getIdField(), getParentDn())).getStdout();
        return multiFromLdif(ldif);
    }

    @Override public E findByUniqueField(String field, Object value) {
        final CommandResult result = root_search("(" + field + "=" + value + ")");
        final String ldif = result.getStdout();
        final E entity = fromLdif(ldif);
        return entity;
    }

    private E fromLdif(String ldif) {

        final E entity = (E) instantiate(getTemplateObject().getClass());
        for (String line : ldif.split("\n")) {
            line = line.trim();
            if (line.length() == 0 || line.startsWith("#")) continue;
            int colonPos = line.indexOf(":");
            if (colonPos == -1 || colonPos == line.length()-1) continue;
            final String name = line.substring(0, colonPos).trim();
            final String value = line.substring(colonPos+1).trim();
            entity.append(name, value);
        }
        entity.clean(getLdapConfiguration());
        return entity;
    }

    private List<E> multiFromLdif(String ldif) {
        final StringTokenizer tokenizer = new StringTokenizer(ldif, "\ndn: ");
        final List<E> results = new ArrayList<>();
        while (tokenizer.hasMoreTokens()) {
            final String single = "dn: "+tokenizer.nextToken();
            results.add(fromLdif(single));
        }
        return results;
    }

    @Override public E get(Serializable id) { return findByUuid(id.toString()); }

    @Override public E findByUuid(String uuid) { return findByUniqueField(ldap().getExternal_id(), uuid); }

    @Override public boolean exists(String uuid) { return findByUuid(uuid) != null; }

    @Override public Object preCreate(@Valid E entity) { return null; }

    @Override public E create(@Valid E entity) {
        run_ldapadd(entity.ldifCreate());
        return entity;
    }

    @Override public E createOrUpdate(@Valid E entity) {
        return entity.hasUuid() ? update(entity) : create(entity);
    }

    @Override public E postCreate(E entity, Object context) { return entity; }

    @Override public Object preUpdate(@Valid E entity) { return null; }

    @Override public E update(@Valid E entity) {
        run_ldapmodify(entity.ldifModify());
        return null;
    }

    @Override public E postUpdate(@Valid E entity, Object context) { return entity; }

    @Override public void delete(String dn) {
        okResult(exec(ldapRootCommand("ldapdelete").addArgument(dn)));
    }

    private CommandResult run_ldapadd(String ldif) {
        return okResult(exec(ldapRootCommand("ldapadd"), ldif));
    }

    private CommandResult run_ldapmodify(String ldif) { return run_ldapmodify(ldif, true); }

    private CommandResult run_ldapmodify(String ldif, boolean checkOk) {
        final CommandResult result = exec(ldapRootCommand("ldapmodify"), ldif);
        return checkOk ? okResult(result) : result;
    }

    private CommandResult root_search(String filter) {
        return root_search(filter, null);
    }
    private CommandResult root_search(String filter, String base) {
        return run_ldapsearch(ldap().getAdmin_dn(), ldap().getPassword(), filter, base, null, null);
    }
    private CommandResult root_search(String filter, String base,
                                      String sort, ResultPage.SortOrder sortOrder) {
        return run_ldapsearch(ldap().getAdmin_dn(), ldap().getPassword(), filter, base, sort, sortOrder);
    }

    private CommandResult run_ldapsearch(String userDn, String password,
                                         String filter, String base,
                                         String sort, ResultPage.SortOrder sortOrder) {

        final CommandLine command = ldapCommand("ldapsearch", userDn, password);
        if (base != null) command.addArgument("-b").addArgument(base);
        if (filter != null) command.addArgument(filter);
        if (sort != null) {
            final String sortArg = ((sortOrder != null && sortOrder == ResultPage.SortOrder.DESC) ? "-" : "");
            if (sortOrder != null)
            command.addArgument("-E").addArgument("!sss="+sortArg);
        }
        return exec(command);
    }


    private CommandLine ldapRootCommand(String cmd) {
        return ldapCommand(cmd, ldap().getAdmin_dn(), ldap().getPassword());
    }

    public CommandLine ldapCommand(String command, String dn, String password) {
        return new CommandLine(command)
                .addArgument("-x")
                .addArgument("-H")
                .addArgument("ldap://127.0.0.1")
                .addArgument("-D")
                .addArgument(dn)
                .addArgument("-w")
                .addArgument(password);
    }

    private CommandResult exec(CommandLine command) { return exec(command, null); }

    private CommandResult exec(CommandLine command, String input) {
        final CommandResult result;
        final Command cmd = empty(input) ? new Command(command) : new Command(command).setInput(input);
        try {
            result = CommandShell.exec(cmd);
        } catch (Exception e) {
            return die("error running "+command+": " + e,e);
        }
        return result;
    }

    public void changePassword(String accountName, String oldPassword, String newPassword) {
        final CommandLine command =  ldapRootCommand("ldappasswd")
                .addArgument("-a").addArgument(oldPassword)
                .addArgument("-s").addArgument(newPassword)
                .addArgument(accountName+","+ldap().getUser_dn());
        okResult(exec(command));
    }

    public void adminChangePassword(String accountName, String newPassword) {
        final CommandLine command =  ldapRootCommand("ldappasswd")
                .addArgument("-s").addArgument(newPassword)
                .addArgument(accountName+","+ldap().getUser_dn());
        okResult(exec(command));
    }
}
