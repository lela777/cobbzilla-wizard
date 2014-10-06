package org.cobbzilla.wizard.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.commons.lang.RandomStringUtils;
import org.cobbzilla.util.security.bcrypt.BCrypt;
import org.cobbzilla.util.security.bcrypt.BCryptUtil;
import org.cobbzilla.util.string.StringUtil;
import org.cobbzilla.wizard.validation.HasValue;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.Transient;
import javax.validation.constraints.Size;

import static org.cobbzilla.wizard.model.BasicConstraintConstants.*;

@Embeddable @NoArgsConstructor
public class HashedPassword {

    public HashedPassword (String password) { setPassword(password); }

    @HasValue(message=ERR_HASHED_PASSWORD_EMPTY)
    @Size(max=HASHEDPASSWORD_MAXLEN, message=ERR_HASHED_PASSWORD_LENGTH)
    @Column(nullable=false, length=HASHEDPASSWORD_MAXLEN)
    @Getter @Setter private String hashedPassword;
    @JsonIgnore public boolean hasPassword () { return !StringUtil.empty(hashedPassword); }

    @Size(min=RESETTOKEN_MAXLEN, max=RESETTOKEN_MAXLEN, message=ERR_RESET_TOKEN_LENGTH)
    @Column(length=RESETTOKEN_MAXLEN)
    private String resetToken;
    public String getResetToken() { return resetToken; }
    public void setResetToken(String resetToken) {
        this.resetToken = resetToken;
        this.resetTokenCtime = (resetToken == null) ? null : System.currentTimeMillis();
    }

    public String initResetToken() {
        final String token = RandomStringUtils.randomAlphanumeric(BasicConstraintConstants.RESETTOKEN_MAXLEN);
        setResetToken(token);
        return token;
    }

    @Getter @Setter private Long resetTokenCtime;

    @Transient
    public long getResetTokenAge () { return resetTokenCtime == null ? 0 : System.currentTimeMillis() - resetTokenCtime; }

    @Transient
    public boolean isCorrectPassword (String password) {
        return password != null && BCrypt.checkpw(password, hashedPassword);
    }

    public void setPassword(String password) { this.hashedPassword = BCryptUtil.hash(password); }

    public void resetPassword(String password, long tokenDuration) {
        if (getResetTokenAge() > tokenDuration) throw new IllegalStateException("token expired");
        setPassword(password);
    }

}
