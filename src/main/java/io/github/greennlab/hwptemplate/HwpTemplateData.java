package io.github.greennlab.hwptemplate;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.SneakyThrows;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

@Data
@EqualsAndHashCode(callSuper = false)
@SuppressWarnings("SerializableHasSerializationMethods")
public class HwpTemplateData<T> extends HashMap<Object, Object> {

    private int index;

    private HwpTemplateData(T data) {
        this.putAll(toFlat(data));
    }

    public static <T> HwpTemplateData<T> wrap(T data) {
        return new HwpTemplateData<>(data);
    }

    @SneakyThrows
    private Map<?, ?> toFlat(T object) {
        if (object instanceof Map) return (Map<?, ?>) object;

        var clazz = object.getClass();
        var result = new HashMap<String, Object>();

        for (Method method : clazz.getMethods()) {
            final String name = method.getName();
            final Class<?> returnType = method.getReturnType();
            if ((name.startsWith("get") || name.startsWith("is")) && method.getParameters().length == 0 && (returnType != Void.class)) {
                result.put(name, method.invoke(object));
            }
        }

        return result;
    }

}
