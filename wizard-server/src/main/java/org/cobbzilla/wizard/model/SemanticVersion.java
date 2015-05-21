package org.cobbzilla.wizard.model;

import lombok.*;

import javax.persistence.Embeddable;
import javax.validation.constraints.Size;
import java.io.File;
import java.io.FileFilter;
import java.util.Comparator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.cobbzilla.util.daemon.ZillaRuntime.die;
import static org.cobbzilla.util.string.StringUtil.empty;
import static org.cobbzilla.wizard.model.BasicConstraintConstants.*;

@Embeddable @EqualsAndHashCode(callSuper=false)
@NoArgsConstructor @AllArgsConstructor
public class SemanticVersion implements Comparable<SemanticVersion> {

    public static final String VERSION_REGEXP = "^(\\d+)\\.(\\d+)\\.(\\d+)$";
    public static final Pattern VERSION_PATTERN = Pattern.compile(VERSION_REGEXP);

    public static final FileFilter DIR_FILTER = new FileFilter() {
        @Override public boolean accept(File pathname) {
            return pathname.isDirectory() && SemanticVersion.isValid(pathname.getName());
        }
    };

    public static final Comparator<SemanticVersion> COMPARE_LATEST_FIRST = new Comparator<SemanticVersion>() {
        @Override public int compare(SemanticVersion o1, SemanticVersion o2) {
            return o2.compareTo(o1);
        }
    };

    public SemanticVersion (String version) {
        if (empty(version)) die("empty version string");
        final Matcher matcher = VERSION_PATTERN.matcher(version);
        if (!matcher.find()) die("Invalid version: " + version);
        setMajor(Integer.parseInt(matcher.group(1)));
        setMinor(Integer.parseInt(matcher.group(2)));
        setPatch(Integer.parseInt(matcher.group(3)));
    }

    @Size(max=SV_VERSION_MAXLEN, message=SV_MAJOR_LENGTH)
    @Getter @Setter private int major = 1;

    @Size(max=SV_VERSION_MAXLEN, message=SV_MINOR_LENGTH)
    @Getter @Setter private int minor = 0;

    @Size(max=SV_VERSION_MAXLEN, message=SV_PATCH_LENGTH)
    @Getter @Setter private int patch = 0;

    public static SemanticVersion incrementPatch(SemanticVersion other) {
        return new SemanticVersion(other.getMajor(), other.getMinor(), other.getPatch()+1);
    }

    public static boolean isValid (String version) { return VERSION_PATTERN.matcher(version).find(); }

    @Override public String toString () { return major + "." + minor + "." + patch; }

    @Override public int compareTo(SemanticVersion other) {
        if (other == null) throw new IllegalArgumentException("compareTo: argument was null");
        int diff;
        diff = major - other.major; if (diff != 0) return diff;
        diff = minor - other.minor; if (diff != 0) return diff;
        diff = patch - other.patch; if (diff != 0) return diff;
        return 0;
    }

}
