package net.timbel.hwptemplate;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.SneakyThrows;
import lombok.var;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Data
@EqualsAndHashCode(callSuper = false)
@SuppressWarnings("SerializableHasSerializationMethods")
public class HwpTemplateData extends HashMap<Object, Object> {

    private int index;

    private <T> HwpTemplateData(T data) {
        this.putAll(toFlat(data));
    }

    public static <T> HwpTemplateData wrap(T data) {
        return new HwpTemplateData(data);
    }

    @SneakyThrows
    private <T> Map<?, ?> toFlat(T object) {
        if (object instanceof Map) return (Map<?, ?>) object;

        var clazz = object.getClass();
        var result = new HashMap<String, Object>();

        for (Method method : clazz.getMethods()) {
            String name = method.getName();
            final Class<?> returnType = method.getReturnType();
            if ((name.startsWith("get") || name.startsWith("is")) && method.getParameters().length == 0 && (returnType != Void.class)) {
                name = name.replaceAll("^(get|is)", "");
                name = name.substring(0, 1).toLowerCase() + name.substring(1);

                result.put(name, method.invoke(object));
            }
        }

        return result;
    }

    public String value(Object key) {
        if (Objects.equals(key, "index")) return String.valueOf(index);

        return containsKey(key) ? super.getOrDefault(key, "").toString() : "";
    }

}
