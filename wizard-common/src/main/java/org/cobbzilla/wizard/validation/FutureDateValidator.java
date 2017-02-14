package org.cobbzilla.wizard.validation;

import lombok.extern.slf4j.Slf4j;
import org.cobbzilla.util.daemon.ZillaRuntime;
import org.joda.time.LocalDate;
import org.joda.time.format.DateTimeFormat;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import java.util.concurrent.TimeUnit;

import static org.cobbzilla.util.daemon.ZillaRuntime.die;
import static org.cobbzilla.util.daemon.ZillaRuntime.empty;
import static org.cobbzilla.util.daemon.ZillaRuntime.now;
import static org.cobbzilla.util.time.TimeUtil.parseDuration;

@Slf4j
public class FutureDateValidator implements ConstraintValidator<FutureDate, Object> {

    private long min;
    private String format;
    private boolean emptyOk;
    private TimeUnit resolution;

    @Override public void initialize(FutureDate constraintAnnotation) {
        this.min = parseDuration(constraintAnnotation.min());
        this.format = constraintAnnotation.format();
        this.emptyOk = constraintAnnotation.emptyOk();
        this.resolution = constraintAnnotation.resolution();
    }

    @Override public boolean isValid(Object value, ConstraintValidatorContext context) {

        if (empty(value)) return emptyOk;

        if (!empty(format) && value instanceof String) {
            return DateTimeFormat.forPattern(format).parseMillis(value.toString()) - now() >= min;

        } else if (value instanceof Number) {
            long now = roundToResolution(now());
            long epoch = roundToResolution(((Number) value).longValue());
            boolean ok = epoch - now >= min;
            if (!ok) log.error("FutureDateValidator: not in the future: "+epoch+" (now="+now+", min="+min+", system-offset="+ ZillaRuntime.getSystemTimeOffset()+")");
            return ok;

        } else {
            return isValid(Long.parseLong(value.toString()), context);
        }
    }

    private long roundToResolution(long t) {
        switch (resolution) {
            case MILLISECONDS: return t;
            case SECONDS: return t - (t % 1000);
            case DAYS: return new LocalDate(t).toDateTimeAtStartOfDay().getMillis();
            default: return die("roundToResolution: invalid resolution: "+resolution);
        }
    }

}
