package org.cobbzilla.wizard.client.script;

import com.github.jknack.handlebars.Handlebars;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.apache.http.client.HttpClient;
import org.cobbzilla.util.collection.multi.MultiResultDriverBase;
import org.cobbzilla.util.handlebars.HandlebarsUtil;
import org.cobbzilla.util.reflect.ObjectFactory;
import org.cobbzilla.util.reflect.ReflectionUtil;

import java.util.Map;

@NoArgsConstructor @AllArgsConstructor @Accessors(chain=true)
public class ApiMultiScriptDriver extends MultiResultDriverBase {

    @Getter @Setter private ApiRunner apiRunner;
    @Getter @Setter private Handlebars handlebars;
    @Getter @Setter private String testTemplate;
    @Getter @Setter private ObjectFactory<HttpClient> httpClientFactory;

    @Getter private Map<String, Object> context;
    @Override public void setContext(Map<String, Object> context) { this.context = context; }

    @Getter @Setter private int maxConcurrent;
    @Getter @Setter private long timeout;

    @Override public void before() { apiRunner.reset(); }

    protected String getTestName(Object task) { return ReflectionUtil.get(task, "name", ""+task.hashCode()); }

    @Override protected String successMessage(Object task) { return getTestName(task); }
    @Override protected String failureMessage(Object task) { return getTestName(task); }

    @Override protected void run(Object task) throws Exception {
        final ApiRunner api = new ApiRunner(apiRunner, httpClientFactory.create());
        api.run(HandlebarsUtil.apply(handlebars, testTemplate, ReflectionUtil.toMap(task)));
    }

}
