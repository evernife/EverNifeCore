package br.com.finalcraft.evernifecore.config.fcconfiguration;

import br.com.finalcraft.evernifecore.config.fcconfiguration.annotation.FConfigComplex;
import br.com.finalcraft.evernifecore.config.fcconfiguration.annotation.FConfigException;
import br.com.finalcraft.evernifecore.config.fcconfiguration.annotation.FConfig;
import br.com.finalcraft.evernifecore.config.yaml.anntation.Loadable;
import br.com.finalcraft.evernifecore.config.yaml.helper.smartloadable.SmartLoadSave;
import br.com.finalcraft.evernifecore.config.yaml.section.ConfigSection;
import br.com.finalcraft.evernifecore.reflection.ConstructorInvoker;
import br.com.finalcraft.evernifecore.reflection.FieldAccessor;
import br.com.finalcraft.evernifecore.util.FCReflectionUtil;
import br.com.finalcraft.evernifecore.util.commons.Tuple;

import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

public class FCConfigurationManager {

    public static Set<Class> VALID_PRIMARY_KEYS = new HashSet<>(Arrays.asList(
            String.class,
            Integer.class,
            Long.class,
            Double.class,
            Float.class,
            Short.class,
            Byte.class,
            Boolean.class,
            UUID.class
    ));

    public static <O> void attatchLoadableSalvableFunctions(Class<O> clazz, SmartLoadSave<O> smartLoadSave){
        ConstructorInvoker<O> EMPTY_CONSTRUCTOR = FCReflectionUtil.getConstructor(clazz);

        List<FieldAccessor> nonExcludedFields = FCReflectionUtil.getDeclaredFields(clazz).stream()
                .filter(fieldAccessor -> {
                    int modifiers = fieldAccessor.getTheField().getModifiers();

                    if (Modifier.isTransient(modifiers)){
                        return false;
                    }

                    if (Modifier.isFinal(modifiers)){
                        return false;
                    }

                    if (fieldAccessor.getTheField().isAnnotationPresent(FConfig.Exclude.class)){
                        return false;
                    }

                    return true;
                })
                .collect(Collectors.toList());

        for (FieldAccessor nonExcludedField : nonExcludedFields) {
            nonExcludedField.getTheField().setAccessible(true);
        }

        List<Tuple<FieldAccessor, BiConsumer<O, ConfigSection>>> fieldSaveActions = new ArrayList<>();
        List<Tuple<FieldAccessor, BiConsumer<O, ConfigSection>>> fieldLoadActions = new ArrayList<>();

        FieldAccessor primaryId = null;

        for (FieldAccessor fieldAccessor : new ArrayList<>(nonExcludedFields)) {
            FConfig.Id fieldConfiguration = fieldAccessor.getTheField().getAnnotation(FConfig.Id.class);

            if (fieldConfiguration == null){
                continue;
            }

            if (primaryId != null){
                throw errorOnPrimaryKey("Multiple fields marked as ID in the same class!", clazz, nonExcludedFields);
            }

            if (!VALID_PRIMARY_KEYS.contains(fieldAccessor.getTheField().getType())){
                throw errorOnPrimaryKey(String.format("Invalid Primary Key Type! Must be one of the following types [%s]", VALID_PRIMARY_KEYS.stream().map(Class::getSimpleName).collect(Collectors.joining(", "))), clazz, nonExcludedFields);
            }

            primaryId = fieldAccessor;
        }

        for (FieldAccessor fieldAccessor : nonExcludedFields) {
            if (fieldAccessor == primaryId){
                continue;
            }

            FConfig fieldConfiguration = fieldAccessor.getTheField().getAnnotation(FConfig.class);

            //Action for Saving
            String key = fieldConfiguration != null && !fieldConfiguration.key().isEmpty()
                    ? fieldConfiguration.key()
                    : fieldAccessor.getTheField().getName();

            String comment = fieldConfiguration != null && !fieldConfiguration.comment().isEmpty()
                    ? fieldConfiguration.comment()
                    : null;

            //Action for Saving on YML
            fieldSaveActions.add(Tuple.of(fieldAccessor, (object, configSection) -> {
                Object objectFieldContent = fieldAccessor.get(object);
                if (comment != null){
                    configSection.setValue(key, objectFieldContent, comment);
                }else {
                    configSection.setValue(key, objectFieldContent);
                }
            }));

            //Action for Loading from YML
            BiFunction<ConfigSection, String, Object> extractorBasedOnType = getExtractorBasedOnType(fieldAccessor);

            fieldLoadActions.add(Tuple.of(fieldAccessor, (object, configSection) -> {
                Object defaultValue = fieldAccessor.get(object);

                Object value;

                try {
                    value = extractorBasedOnType.apply(configSection, key);
                }catch (Exception e){
                    System.out.println("Failed to load field: " + key + " from " + clazz.getName());
                    e.printStackTrace();
                    value = null;
                }

                if (value == null){
                    value = defaultValue;
                }

                fieldAccessor.set(object, value);
            }));
        }

        FieldAccessor finalPrimaryId = primaryId;

        smartLoadSave.setOnConfigSave((configSection, object) -> {
            //Do the Actual object Saving
            if (object instanceof FConfigComplex) ((FConfigComplex) object).onConfigSavePre(configSection);
            if (finalPrimaryId != null){
                String primaryIdString = Optional.ofNullable(finalPrimaryId.get(object)).map(Object::toString)
                        .orElseThrow(() -> errorOnPrimaryKey("PrimaryID Field is null or Empty!", clazz, nonExcludedFields));
                configSection.setCustomKeyIndex(primaryIdString);
            }
            for (Tuple<FieldAccessor, BiConsumer<O, ConfigSection>> tuple : fieldSaveActions) {
                tuple.getRight().accept(object, configSection);
            }
            if (object instanceof FConfigComplex) ((FConfigComplex) object).onConfigSavePost(configSection);
        });

        smartLoadSave.setOnConfigLoad((Function<ConfigSection, O>) (configSection) -> {
            O object = EMPTY_CONSTRUCTOR.invoke();

            //Do the Actual object Loading
            if (object instanceof FConfigComplex) ((FConfigComplex) object).onConfigSavePre(configSection);
            if (finalPrimaryId != null){
                String primaryIdString = configSection.getSectionKey();
                Object primaryIdCasted = castPrimaryKeyIndex(primaryIdString, finalPrimaryId.getTheField().getType());
                finalPrimaryId.set(object, primaryIdCasted);
            }
            for (Tuple<FieldAccessor, BiConsumer<O, ConfigSection>> tuple : fieldLoadActions) {
                tuple.getRight().accept(object, configSection);
            }
            if (object instanceof FConfigComplex) ((FConfigComplex) object).onConfigLoadPost(configSection);

            return object;
        });
    }

    public static Object castPrimaryKeyIndex(String content, Class type){
        if (type == String.class){
            return content;
        }else if (type == Integer.class){
            return Integer.valueOf(content);
        }else if (type == Long.class){
            return Long.valueOf(content);
        }else if (type == Double.class){
            return Double.valueOf(content);
        }else if (type == Float.class){
            return Float.valueOf(content);
        }else if (type == Short.class){
            return Short.valueOf(content);
        }else if (type == Byte.class){
            return Byte.valueOf(content);
        }else if (type == Boolean.class){
            return Boolean.valueOf(content);
        }else if (type == UUID.class){
            return UUID.fromString(content);
        }else {
            throw new FConfigException("Unsupported Primary Key Type: " + type.getName());
        }
    }

    public static BiFunction<ConfigSection, String, Object> getExtractorBasedOnType(FieldAccessor fieldAccessor){
        Class<?> clazz = fieldAccessor.getTheField().getType();

        if (clazz == String.class) return (configSection, key) -> configSection.getString(key);
        if (clazz == Integer.class || clazz == int.class) return (configSection, key) -> configSection.getInt(key);
        if (clazz == Long.class || clazz == long.class) return (configSection, key) -> configSection.getLong(key);
        if (clazz == Double.class || clazz == double.class) return (configSection, key) -> configSection.getDouble(key);
        if (clazz == Float.class || clazz == float.class) return (configSection, key) -> (float) configSection.getInt(key);
        if (clazz == Short.class || clazz == short.class) return (configSection, key) -> (short) configSection.getInt(key);
        if (clazz == Byte.class || clazz == byte.class) return (configSection, key) -> (byte) configSection.getInt(key);
        if (clazz == Boolean.class) return (configSection, key) -> configSection.getBoolean(key);
        if (clazz == UUID.class) return (configSection, key) -> configSection.getUUID(key);
        if (Collection.class.isAssignableFrom(clazz)){
            //Get the collection type
            Class<?> genericClazzType = null;
            try {
                Type genericType = fieldAccessor.getTheField().getGenericType();
                if (genericType instanceof ParameterizedType) {
                    ParameterizedType pt = (ParameterizedType) genericType;
                    Type[] fieldArgTypes = pt.getActualTypeArguments();
                    for (Type type : fieldArgTypes) {
                        genericClazzType = (Class<?>) type;
                        break;
                    }
                }
            }catch (Throwable genericErrorIgnored){
                genericClazzType = null;
            }

            FConfig annotation = fieldAccessor.getTheField().getAnnotation(FConfig.class);
            if (annotation != null && annotation.loadableClass() != null && annotation.loadableClass() != Loadable.class){
                genericClazzType = annotation.loadableClass();
            }

            if (genericClazzType != null){
                final Class<?> clazzType = genericClazzType;

                if (List.class.isAssignableFrom(clazz)){
                    if (clazzType == String.class) return (configSection, key) -> configSection.getStringList(key);
                    return (configSection, key) -> configSection.getLoadableList(key, clazzType);
                }

                if (LinkedHashSet.class.isAssignableFrom(clazz)){
                    if (clazzType == String.class) return (configSection, key) -> new LinkedHashSet<>(configSection.getStringList(key));
                    return (configSection, key) -> new LinkedHashSet<>(configSection.getLoadableList(key, clazzType));
                }

                if (Set.class.isAssignableFrom(clazz)){
                    if (clazzType == String.class) return (configSection, key) -> new HashSet<>(configSection.getStringList(key));
                    return (configSection, key) -> new HashSet<>(configSection.getLoadableList(key, clazzType));
                }

            }

            return (configSection, key) -> configSection.getList(key);
        };
        return (configSection, key) -> configSection.getLoadable(key, clazz);
    }

    private static FConfigException errorOnPrimaryKey(String errorMessage, Class clazz, List<FieldAccessor> nonExcludedFields){
        String allFieldsData = nonExcludedFields.stream().filter(fieldAccessor1 -> {
                    FConfig.Id annotation = fieldAccessor1.getTheField().getAnnotation(FConfig.Id.class);
                    return annotation != null;
                })
                .map(fieldAccessor1 -> fieldAccessor1.getTheField().toString())
                .collect(Collectors.joining("\n  - "));

        return new FConfigException(
                String.format(errorMessage +
                                "\n Class: %s" +
                                "\n PrimaryFields: " +
                                "\n  - %s",
                        clazz.getName(),
                        allFieldsData
                )
        );
    }

}