package org.cobbzilla.wizard.client.script;

import com.github.jknack.handlebars.Handlebars;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.cobbzilla.util.collection.multi.MultiResultDriverBase;
import org.cobbzilla.util.handlebars.HandlebarsUtil;
import org.cobbzilla.util.reflect.ReflectionUtil;

import java.util.Map;

@NoArgsConstructor @AllArgsConstructor @Accessors(chain=true)
public class ApiMultiScriptDriver extends MultiResultDriverBase {

    @Getter @Setter private ApiRunner apiRunner;
    @Getter @Setter private Handlebars handlebars;
    @Getter @Setter private String testTemplate;

    @Getter private Map<String, Object> context;
    @Override public void setContext(Map<String, Object> context) { this.context = context; }

    @Override public void before() { apiRunner.reset(); }

    protected String getTestName(Object task) { return ReflectionUtil.get(task, "name", ""+task.hashCode()); }

    @Override protected String successMessage(Object task) { return getTestName(task); }
    @Override protected String failureMessage(Object task) { return getTestName(task); }

    @Override protected void run(Object task) throws Exception {
        final ApiRunner api = new ApiRunner(apiRunner);
        api.run(HandlebarsUtil.apply(handlebars, testTemplate, ReflectionUtil.toMap(task)));
    }

}