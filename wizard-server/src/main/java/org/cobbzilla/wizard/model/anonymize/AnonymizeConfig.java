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
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.SystemPropertyUtils;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class AnonymizeConfig {

    public static AnonTable[] createAnonTables(String[] packageList){
        List<AnonTable> anonTablesList = new ArrayList<AnonTable>();
        for(String packageName: packageList){
            try {
                //only classes with AnonymizeList or AnonymizeType annotations
                List<Class> clazzList = findClasses(packageName);
                for(Class c : clazzList){
                    anonTablesList.add(createAnonTable(c));
                }
            } catch (IOException e) {
                e.printStackTrace();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
        AnonTable[] anonTables = new AnonTable[anonTablesList.size()];
        anonTablesList.toArray(anonTables);
        return anonTables;
    }

    private static AnonTable createAnonTable(Class clazz) {
        AnonymizeTable anonymizeTable = (AnonymizeTable)clazz.getAnnotation(AnonymizeTable.class);
        AnonTable anonTable = new AnonTable().setTable(StringUtil.camelCaseToSnakeCase(clazz.getSimpleName()));
        if(anonymizeTable != null) {
            anonTable.setTruncate(anonymizeTable.truncate());
            if (anonymizeTable.name().length() > 0) {anonTable.setTable(anonymizeTable.name());}
        }
        final List<AnonColumn> anonColumns = new ArrayList<>();
        AnonymizeList anonymizetList = (AnonymizeList) clazz.getAnnotation(AnonymizeList.class);
        if(anonymizetList != null){
            for(String name: anonymizetList.list()){
                anonColumns.add(new AnonColumn().setEncrypted(true)
                        .setName(name)
                        .setType(AnonType.guessType("passthru")));
            }
        }
        ReflectionUtils.doWithFields(
                clazz,
                new ReflectionUtils.FieldCallback() {
                    @Override public void doWith(Field f) throws IllegalArgumentException,
                            IllegalAccessException {
                        if (f.getAnnotation(AnonymizeType.class) != null) {
                            AnonymizeType anonymizeType = (AnonymizeType)(f.getAnnotation(AnonymizeType.class));
                            anonColumns.add(createAnonColumn(StringUtil.camelCaseToSnakeCase(f.getName()), anonymizeType));
                        }
                        if (f.getAnnotation(AnonymizeEmbedded.class) != null) {
                            AnonymizeEmbedded anonymizeEmbedded = (AnonymizeEmbedded)(f.getAnnotation(AnonymizeEmbedded.class));
                            for(AnonymizeType anonymizeType : anonymizeEmbedded.list()){
                                anonColumns.add(createAnonColumn(StringUtil.camelCaseToSnakeCase(anonymizeType.name()), anonymizeType));
                            }
                        }
                    }
                });
        ReflectionUtils.doWithMethods(
                clazz,
                new ReflectionUtils.MethodCallback() {
                    @Override public void doWith(Method m){
                        if (m.getAnnotation(AnonymizeType.class) != null) {
                            AnonymizeType anonymizeType = (AnonymizeType)(m.getAnnotation(AnonymizeType.class));
                            anonColumns.add(createAnonColumn(StringUtil.camelCaseToSnakeCase(m.getName().substring(3)),anonymizeType ));
                        }
                        if (m.getAnnotation(AnonymizeEmbedded.class) != null) {
                            AnonymizeEmbedded anonymizeEmbedded = (AnonymizeEmbedded)(m.getAnnotation(AnonymizeEmbedded.class));
                            for(AnonymizeType anonymizeType : anonymizeEmbedded.list()){
                                anonColumns.add(createAnonColumn(StringUtil.camelCaseToSnakeCase(anonymizeType.name()), anonymizeType));
                            }
                        }
                    }
                });
        AnonColumn[] columns = new AnonColumn[anonColumns.size()];
        anonColumns.toArray(columns);
        anonTable.setColumns(columns);
        return anonTable;
    }

    private static AnonColumn createAnonColumn(String s, AnonymizeType anonymizeType) {
        AnonColumn anonColumn = new AnonColumn().setName(s)
                .setValue(anonymizeType.value())
                .setSkip(anonymizeType.skip());
        if(anonymizeType.encrypted()){anonColumn.setEncrypted(anonymizeType.encrypted());}
        if(anonymizeType.json().length > 0){
            List<AnonJsonPath> anonJsonPathsList = new ArrayList<>();
            for(AnonymizeJsonPath anonymizeJsonPath : anonymizeType.json()){
                AnonType annonType = null;
                try{annonType =AnonType.guessType(anonymizeType.type());}catch (Exception e){}
                anonJsonPathsList.add(new AnonJsonPath().setType(annonType)
                        .setPath(anonymizeJsonPath.path()));
            }
            AnonJsonPath[] anonJsonPathsArray = new AnonJsonPath[anonJsonPathsList.size()];
            anonJsonPathsArray = anonJsonPathsList.toArray(anonJsonPathsArray);
            anonColumn.setJson(anonJsonPathsArray);
        }
        try{anonColumn.setType(AnonType.guessType(anonymizeType.type()));}catch (Exception e){}

        return anonColumn;
    }

    private static List<Class> findClasses(String packageName) throws IOException, ClassNotFoundException {
        ResourcePatternResolver resourcePatternResolver = new PathMatchingResourcePatternResolver();
        MetadataReaderFactory metadataReaderFactory = new CachingMetadataReaderFactory(resourcePatternResolver);
        List<Class> candidates = new ArrayList<>();
        String packageSearchPath = ResourcePatternResolver.CLASSPATH_ALL_URL_PREFIX +
                resolveBasePackage(packageName) + "/" + "**/*.class";
        Resource[] resources = resourcePatternResolver.getResources(packageSearchPath);
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
        return ClassUtils.convertClassNameToResourcePath(SystemPropertyUtils.resolvePlaceholders(basePackage));}

    private static boolean checkFieldsAndMethods(Class c){
        Field fields[] = c.getDeclaredFields();
        for (Field f : fields) {
            if (f.getAnnotation(AnonymizeType.class) != null ||
                    f.getAnnotation(AnonymizeEmbedded.class) != null) {return true;}
        }
        Method allMethods[] = c.getDeclaredMethods();
        for (Method m : allMethods){
            if (m.getAnnotation(AnonymizeType.class) != null ||
                    m.getAnnotation(AnonymizeEmbedded.class) != null) {return true;}
        }
        return false;
    }
}
