package org.cobbzilla.wizard.server.config;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.cobbzilla.util.cache.AutoRefreshingReference;
import org.cobbzilla.util.http.HttpUtil;
import org.cobbzilla.util.io.FileUtil;
import org.cobbzilla.util.io.StreamUtil;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import static org.cobbzilla.util.daemon.ZillaRuntime.empty;
import static org.cobbzilla.util.string.StringUtil.chop;

@Slf4j
public class LegalInfo {

    public static final String DOC_TERMS = "terms";
    public static final String DOC_PRIVACY = "privacy";
    public static final String DOC_COMMUNITY = "community";
    public static final String DOC_LICENSES = "licenses";

    @Getter @Setter private String base;

    @Getter @Setter private String termsOfService = "terms.md";
    @Getter @Setter private String privacyPolicy = "privacy.md";
    @Getter @Setter private String communityGuidelines = "community.md";
    @Getter @Setter private String licenses = "licenses.md";

    public String getDocument (String type) { return getLoaders().get(type).get(); }

    public String getTermsOfServiceDocument () { return getDocument(DOC_TERMS); }
    public String getPrivacyPolicyDocument () { return getDocument(DOC_PRIVACY); }
    public String getCommunityGuidelinesDocument () { return getDocument(DOC_COMMUNITY); }
    public String getLicensesDocument () { return getDocument(DOC_LICENSES); }

    @Getter(lazy=true) private final Map<String, AutoRefreshingReference<String>> loaders = initLoaders();
    private ConcurrentHashMap<String, AutoRefreshingReference<String>> initLoaders() {
        final ConcurrentHashMap<String, AutoRefreshingReference<String>> map = new ConcurrentHashMap<>();
        map.put(DOC_TERMS,     new LegaleseLoader(getTermsOfService()));
        map.put(DOC_PRIVACY,   new LegaleseLoader(getPrivacyPolicy()));
        map.put(DOC_COMMUNITY, new LegaleseLoader(getCommunityGuidelines()));
        map.put(DOC_LICENSES,  new LegaleseLoader(getLicenses()));
        return map;
    }

    @AllArgsConstructor
    private class LegaleseLoader extends AutoRefreshingReference<String> {
        private String type;
        @Override public String refresh() { return load(type); }
        @Override public long getTimeout() { return TimeUnit.DAYS.toMillis(1); }
    }

    private String load(String type) {
        String value = type;
        if (empty(value)) return null;
        if (!empty(base)) value = chop(base, "/") + "/" + value;

        if (value.startsWith("http://") || value.startsWith("https://")) {
            try {
                return HttpUtil.getResponse(value).getEntityString();
            } catch (IOException e) {
                log.error("load("+type+"): "+value+": "+e);
                return null;
            }

        } else if (value.startsWith("/") && new File(value).exists()) {
            try {
                return FileUtil.toString(value);
            } catch (Exception e) {
                log.error("load("+type+"): "+value+": "+e);
                return null;
            }
        } else {
            try {
                return StreamUtil.loadResourceAsString(value);
            } catch (Exception e) {
                log.error("load(" + type + ") unable to load from filesystem or classpath: " + value);
                return null;
            }
        }
    }
}
