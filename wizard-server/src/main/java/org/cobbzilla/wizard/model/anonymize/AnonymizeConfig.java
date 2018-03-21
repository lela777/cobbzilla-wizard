package org.cobbzilla.wizard.model.anonymize;

import lombok.extern.slf4j.Slf4j;
import org.cobbzilla.util.string.StringUtil;
import org.cobbzilla.wizard.model.anon.AnonColumn;
import org.cobbzilla.wizard.model.anon.AnonJsonPath;
import org.cobbzilla.wizard.model.anon.AnonTable;
import org.cobbzilla.wizard.model.anon.AnonType;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.core.type.classreading.CachingMetadataReaderFactory;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.util.SystemPropertyUtils;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import static org.cobbzilla.util.daemon.ZillaRuntime.CLASSPATH_PREFIX;
import static org.cobbzilla.util.daemon.ZillaRuntime.die;
import static org.cobbzilla.util.io.StreamUtil.loadResourceAsStringOrDie;
import static org.springframework.core.io.support.ResourcePatternResolver.CLASSPATH_ALL_URL_PREFIX;
import static org.springframework.util.ClassUtils.convertClassNameToResourcePath;
import static org.springframework.util.ReflectionUtils.doWithFields;
import static org.springframework.util.ReflectionUtils.doWithMethods;

@Slf4j
public class AnonymizeConfig {

    public static AnonTable[] createAnonTables(String[] packageList) {
        final List<AnonTable> anonTablesList = new ArrayList<>();
        for (String packageName: packageList) {
            try {
                // only classes with AnonymizeList or AnonymizeType annotations
                final List<Class> clazzList = findClasses(packageName);
                for (Class c : clazzList) {
                    anonTablesList.add(createAnonTable(c));
                }
            } catch (Exception e) {
                return die("createAnonTables: "+e, e);
            }
        }
        final AnonTable[] anonTables = new AnonTable[anonTablesList.size()];
        anonTablesList.toArray(anonTables);
        return anonTables;
    }

    private static AnonTable createAnonTable(Class clazz) {
        final AnonymizeTable anonymizeTable = (AnonymizeTable)clazz.getAnnotation(AnonymizeTable.class);
        final AnonTable anonTable = new AnonTable().setTable(StringUtil.camelCaseToSnakeCase(clazz.getSimpleName()));
        if (anonymizeTable != null) {
            anonTable.setTruncate(anonymizeTable.truncate());
            if (anonymizeTable.name().length() > 0) anonTable.setTable(anonymizeTable.name());
        }
        final List<AnonColumn> anonColumns = new ArrayList<>();
        final AnonymizeList anonymizeList = (AnonymizeList) clazz.getAnnotation(AnonymizeList.class);
        if (anonymizeList != null) {
            for (String name: anonymizeList.list()) {
                anonColumns.add(new AnonColumn().setEncrypted(true)
                        .setName(StringUtil.camelCaseToSnakeCase(name))
                        .setType(AnonType.passthru));
            }
        }
        doWithFields(clazz,
                f -> {
                    if (f.getAnnotation(AnonymizeType.class) != null) {
                        final AnonymizeType anonymizeType = f.getAnnotation(AnonymizeType.class);
                        anonColumns.add(createAnonColumn(StringUtil.camelCaseToSnakeCase(f.getName()), anonymizeType));
                    }
                    if (f.getAnnotation(AnonymizeEmbedded.class) != null) {
                        final AnonymizeEmbedded anonymizeEmbedded = f.getAnnotation(AnonymizeEmbedded.class);
                        for(AnonymizeType anonymizeType : anonymizeEmbedded.list()){
                            anonColumns.add(createAnonColumn(StringUtil.camelCaseToSnakeCase(anonymizeType.name()), anonymizeType));
                        }
                    }
                });
        doWithMethods(clazz,
                m -> {
                    if (m.getAnnotation(AnonymizeType.class) != null) {
                        final AnonymizeType anonymizeType = m.getAnnotation(AnonymizeType.class);
                        anonColumns.add(createAnonColumn(StringUtil.camelCaseToSnakeCase(m.getName().substring(3)),anonymizeType ));
                    }
                    if (m.getAnnotation(AnonymizeEmbedded.class) != null) {
                        final AnonymizeEmbedded anonymizeEmbedded = m.getAnnotation(AnonymizeEmbedded.class);
                        for(AnonymizeType anonymizeType : anonymizeEmbedded.list()){
                            anonColumns.add(createAnonColumn(StringUtil.camelCaseToSnakeCase(anonymizeType.name()), anonymizeType));
                        }
                    }
                });
        final AnonColumn[] columns = new AnonColumn[anonColumns.size()];
        anonColumns.toArray(columns);
        anonTable.setColumns(columns);
        return anonTable;
    }

    private static AnonColumn createAnonColumn(String s, AnonymizeType anonymizeType) {
        String value = anonymizeType.value();
        if (value.startsWith(CLASSPATH_PREFIX)) value = loadResourceAsStringOrDie(value.substring(CLASSPATH_PREFIX.length()));

        final AnonColumn anonColumn = new AnonColumn().setName(s)
                .setValue(value)
                .setSkip(anonymizeType.skip());
        if (anonymizeType.encrypted()) anonColumn.setEncrypted(anonymizeType.encrypted());
        if (anonymizeType.json().length > 0) {
            final List<AnonJsonPath> anonJsonPathsList = new ArrayList<>();
            for (AnonymizeJsonPath anonymizeJsonPath : anonymizeType.json()) {
                AnonType annonType = null;
                try{annonType =AnonType.guessType(anonymizeType.type());}catch (Exception e){}
                anonJsonPathsList.add(new AnonJsonPath().setType(annonType)
                        .setPath(anonymizeJsonPath.path()));
            }
            AnonJsonPath[] anonJsonPathsArray = new AnonJsonPath[anonJsonPathsList.size()];
            anonJsonPathsArray = anonJsonPathsList.toArray(anonJsonPathsArray);
            anonColumn.setJson(anonJsonPathsArray);
        }
        try {
            anonColumn.setType(AnonType.guessType(anonymizeType.type()));
        } catch (Exception ignored) {}

        return anonColumn;
    }

    private static List<Class> findClasses(String packageName) throws IOException, ClassNotFoundException {
        final ResourcePatternResolver resourcePatternResolver = new PathMatchingResourcePatternResolver();
        final MetadataReaderFactory metadataReaderFactory = new CachingMetadataReaderFactory(resourcePatternResolver);
        final List<Class> candidates = new ArrayList<>();
        final String packageSearchPath = CLASSPATH_ALL_URL_PREFIX + resolveBasePackage(packageName) + "/" + "**/*.class";
        final Resource[] resources = resourcePatternResolver.getResources(packageSearchPath);
        for (Resource resource : resources) {
            if (resource.isReadable()) {
                MetadataReader metadataReader = metadataReaderFactory.getMetadataReader(resource);
                Class c = Class.forName(metadataReader.getClassMetadata().getClassName());
                if (c.getAnnotation(AnonymizeList.class) != null || c.getAnnotation(AnonymizeTable.class) != null
                        || checkFieldsAndMethods(c)){candidates.add(c);}
            }
        }
        return candidates;
    }

    private static String resolveBasePackage(String basePackage) {
        return convertClassNameToResourcePath(SystemPropertyUtils.resolvePlaceholders(basePackage));
    }

    private static boolean checkFieldsAndMethods(Class c){
        final Field fields[] = c.getDeclaredFields();
        for (Field f : fields) {
            if (f.getAnnotation(AnonymizeType.class) != null ||
                    f.getAnnotation(AnonymizeEmbedded.class) != null) {return true;}
        }
        final Method allMethods[] = c.getDeclaredMethods();
        for (Method m : allMethods){
            if (m.getAnnotation(AnonymizeType.class) != null ||
                    m.getAnnotation(AnonymizeEmbedded.class) != null) {return true;}
        }
        return false;
    }

}
