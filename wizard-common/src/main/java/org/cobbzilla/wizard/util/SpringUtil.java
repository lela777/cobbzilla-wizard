package org.cobbzilla.wizard.util;

import org.springframework.context.ApplicationContext;

public class SpringUtil {

    public static void autowire(ApplicationContext ctx, Object bean) {
        ctx.getAutowireCapableBeanFactory().autowireBean(bean);
    }

}
