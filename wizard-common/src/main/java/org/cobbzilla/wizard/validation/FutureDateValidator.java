package org.cobbzilla.wizard.validation;

import org.joda.time.format.DateTimeFormat;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.util.concurrent.TimeUnit;

import static org.cobbzilla.util.daemon.ZillaRuntime.empty;
import static org.cobbzilla.util.daemon.ZillaRuntime.now;

public class FutureDateValidator implements ConstraintValidator<FutureDate, Object> {

    private long min;
    private String format;
    private boolean emptyOk;

    @Override public void initialize(FutureDate constraintAnnotation) {
        this.min = parseMin(constraintAnnotation.min());
        this.format = constraintAnnotation.format();
        this.emptyOk = constraintAnnotation.emptyOk();
    }

    private long parseMin(String min) {
        if (empty(min)) return 0;
        final long val = Long.parseLong(chopSuffix(min));
        switch (min.charAt(min.length()-1)) {
            case 's': return TimeUnit.SECONDS.toMillis(val);
            case 'm': return TimeUnit.MINUTES.toMillis(val);
            case 'h': return TimeUnit.HOURS.toMillis(val);
            case 'd': return TimeUnit.DAYS.toMillis(val);
            default: return Long.parseLong(min);
        }
    }

    private String chopSuffix(String min) { return min.substring(0, min.length()-1); }

    @Override public boolean isValid(Object value, ConstraintValidatorContext context) {
        if (empty(value)) return emptyOk;
        if (!empty(format) && value instanceof String) {
            return DateTimeFormat.forPattern(format).parseMillis(value.toString()) - now() >= min;
        } else if (value instanceof Number) {
            return ((Number) value).longValue() - now() >= min;
        } else {
            return isValid(Long.parseLong(value.toString()), context);
        }
    }
}
