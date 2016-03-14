package org.cobbzilla.wizard.util;

import lombok.Cleanup;
import org.apache.commons.io.IOUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

import static org.cobbzilla.util.reflect.ReflectionUtil.forName;

public class SpringUtil {

    public static <T> T autowire(ApplicationContext ctx, T bean) {
        ctx.getAutowireCapableBeanFactory().autowireBean(bean);
        return bean;
    }

    public static Resource[] listResources(String pattern) throws Exception {
        return listResources(pattern, SpringUtil.class.getClassLoader());
    }
    public static Resource[] listResources(String pattern, ClassLoader loader) throws Exception {
        return new PathMatchingResourcePatternResolver(loader).getResources(pattern);
    }

    public static void copyResources(String pattern, File dir) throws Exception {
        for (Resource r : listResources(pattern)) {
            final File temp = new File(dir, r.getFilename());
            @Cleanup final InputStream in = r.getInputStream();
            @Cleanup final OutputStream out = new FileOutputStream(temp);
            IOUtils.copy(in, out);
        }
    }

    public static <T> T getBean (ApplicationContext applicationContext, Class<T> clazz) {
        return applicationContext.getBean(clazz);
    }

    public static <T> T getBean (ApplicationContext applicationContext, String clazz) {
        return (T) applicationContext.getBean(forName(clazz));
    }
}
