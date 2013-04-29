package org.cobbzilla.wizard.model;

import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang.RandomStringUtils;
import org.cobbzilla.util.security.bcrypt.BCrypt;
import org.cobbzilla.util.security.bcrypt.BCryptUtil;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.Transient;
import javax.validation.constraints.Size;

import org.cobbzilla.wizard.validation.HasValue;

import static org.cobbzilla.wizard.model.BasicConstraintConstants.*;

@Embeddable
public class HashedPassword {

    @HasValue(message=ERR_HASHED_PASSWORD_EMPTY)
    @Size(max=HASHEDPASSWORD_MAXLEN, message=ERR_HASHED_PASSWORD_LENGTH)
    @Column(nullable=false, length=HASHEDPASSWORD_MAXLEN)
    @Getter @Setter
    private String hashedPassword;

    @Size(min=RESETTOKEN_MAXLEN, max=RESETTOKEN_MAXLEN, message=ERR_RESET_TOKEN_LENGTH)
    @Column(length=RESETTOKEN_MAXLEN)
    private String resetToken;
    public String getResetToken() { return resetToken; }
    public void setResetToken(String resetToken) {
        this.resetToken = resetToken;
        this.resetTokenCtime = System.currentTimeMillis();
    }

    public String initResetToken() {
        final String token = RandomStringUtils.randomAlphanumeric(BasicConstraintConstants.RESETTOKEN_MAXLEN);
        setResetToken(token);
        return token;
    }

    @Getter @Setter
    private Long resetTokenCtime;

    @Transient
    public long getResetTokenAge () { return resetTokenCtime == null ? 0 : System.currentTimeMillis() - resetTokenCtime; }

    @Transient
    public boolean isCorrectPassword (String password) {
        return password != null && BCrypt.checkpw(password, hashedPassword);
    }

    public void resetPassword(String password) { this.hashedPassword = BCryptUtil.hash(password); }

}
