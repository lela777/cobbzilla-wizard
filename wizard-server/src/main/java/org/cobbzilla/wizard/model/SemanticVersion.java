package org.cobbzilla.wizard.model;

import lombok.*;

import javax.persistence.Embeddable;
import javax.validation.constraints.Size;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.cobbzilla.wizard.model.BasicConstraintConstants.*;

@Embeddable @EqualsAndHashCode(callSuper=false)
@NoArgsConstructor @AllArgsConstructor
public class SemanticVersion implements Comparable<SemanticVersion> {

    public static final Pattern VERSION_PATTERN = Pattern.compile("^(\\d+)\\.(\\d+)\\.(\\d+)$");

    @Size(max=SV_VERSION_MAXLEN, message=SV_MAJOR_LENGTH)
    @Getter @Setter private int major = 1;

    @Size(max=SV_VERSION_MAXLEN, message=SV_MINOR_LENGTH)
    @Getter @Setter private int minor = 0;

    @Size(max=SV_VERSION_MAXLEN, message=SV_PATCH_LENGTH)
    @Getter @Setter private int patch = 0;

    public static SemanticVersion fromString (String version) {
        final Matcher matcher = VERSION_PATTERN.matcher(version);
        if (!matcher.find()) throw new IllegalArgumentException("Invalid version: "+version);
        return new SemanticVersion(Integer.parseInt(matcher.group(1)), Integer.parseInt(matcher.group(2)), Integer.parseInt(matcher.group(3)));
    }

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
