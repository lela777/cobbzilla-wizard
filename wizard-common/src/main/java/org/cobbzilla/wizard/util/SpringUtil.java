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

public class SpringUtil {

    public static void autowire(ApplicationContext ctx, Object bean) {
        ctx.getAutowireCapableBeanFactory().autowireBean(bean);
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
}
