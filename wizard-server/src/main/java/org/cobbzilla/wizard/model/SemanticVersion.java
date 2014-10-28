package org.cobbzilla.wizard.model;

import lombok.*;
import org.cobbzilla.util.string.StringUtil;

import javax.persistence.Embeddable;
import javax.validation.constraints.Size;

import static org.cobbzilla.wizard.model.BasicConstraintConstants.*;

@Embeddable @EqualsAndHashCode(callSuper=false)
@NoArgsConstructor @AllArgsConstructor
public class SemanticVersion implements Comparable<SemanticVersion> {

    @Size(max=SV_VERSION_MAXLEN, message=SV_MAJOR_LENGTH)
    @Getter @Setter private String major = "1";

    @Size(max=SV_VERSION_MAXLEN, message=SV_MINOR_LENGTH)
    @Getter @Setter private String minor = "0";

    @Size(max=SV_VERSION_MAXLEN, message=SV_PATCH_LENGTH)
    @Getter @Setter private String patch = "0";

    public SemanticVersion (int major, int minor, int patch) {
        this.major = String.valueOf(major);
        this.minor = String.valueOf(minor);
        this.patch = String.valueOf(patch);
    }

    @Override
    public String toString () {
        return (StringUtil.empty(major) ? "1" : major) + "."
                + (StringUtil.empty(minor) ? "0" : minor) + "."
                + (StringUtil.empty(patch) ? "0" : patch);
    }

    @Override
    public int compareTo(SemanticVersion other) {
        int diff;
        diff = comparePart(major, other.major); if (diff != 0) return diff;
        diff = comparePart(minor, other.minor); if (diff != 0) return diff;
        diff = comparePart(patch, other.patch); if (diff != 0) return diff;
        return 0;
    }

    private int comparePart(String part1, String part2) {
        return getInt(part1).compareTo(getInt(part2));
    }

    private Integer getInt(String s) {
        StringBuilder builder = new StringBuilder();
        boolean inserting = false;
        for (int index=s.length()-1; index >= 0; index--) {
            char ch = s.charAt(index);
            if (Character.isDigit(ch)) {
                inserting = true;
                builder.insert(0, ch);
            } else if (inserting) {
                break;
            }
        }
        return Integer.valueOf(builder.toString());
    }
}
