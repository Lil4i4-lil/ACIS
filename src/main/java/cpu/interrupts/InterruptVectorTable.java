package cpu.interrupts;

import cpu.interrupts.exceptions.InterruptException;
import cpu.interrupts.handlers.InterruptHandler;
import org.reflections.Reflections;

import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class InterruptVectorTable {
    private Map<Integer, InterruptHandler> table;

    public InterruptVectorTable() {
        Map<Integer, InterruptHandler> table = new HashMap<>();
        try {
            var reflections = new Reflections("cpu.interrupts");
            var subclasses = reflections.getSubTypesOf(InterruptHandler.class);
            for (var interruptHandlerClass : subclasses) {
                // пропускаем абстрактные классы — их нельзя инстанцировать
                if (Modifier.isAbstract(interruptHandlerClass.getModifiers())) {
                    continue;
                }

                // ищем ParameterizedType вверх по иерархии
                ParameterizedType pt = findInterruptHandlerParameterizedType(interruptHandlerClass);
                if (pt == null) {
                    // не удалось определить тип исключения — пропускаем
                    continue;
                }

                @SuppressWarnings("unchecked")
                Class<? extends InterruptException> interruptException =
                        (Class<? extends InterruptException>) pt.getActualTypeArguments()[0];

                InterruptHandler handler = interruptHandlerClass.getConstructor().newInstance();
                int vector = interruptException.getConstructor(String.class)
                        .newInstance("").getVector();
                table.put(vector, handler);
            }
            this.table = Collections.unmodifiableMap(table);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Поднимается по цепочке суперклассов, пока не найдёт параметризованный
     * InterruptHandler<...>, и возвращает его.
     */
    private static ParameterizedType findInterruptHandlerParameterizedType(Class<?> clazz) {
        Class<?> current = clazz;
        while (current != null && current != Object.class) {
            Type t = current.getGenericSuperclass();
            if (t instanceof ParameterizedType) {
                ParameterizedType pt = (ParameterizedType) t;
                Type raw = pt.getRawType();
                if (raw instanceof Class
                        && InterruptHandler.class.isAssignableFrom((Class<?>) raw)) {
                    return pt;
                }
            }
            current = current.getSuperclass();
        }
        return null;
    }

    public InterruptHandler getInterruptHandler(int vector) {
        return table.get(vector);
    }
}