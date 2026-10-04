package io.battery.script.support;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;

/**
 * Reflection helpers used by the visitor to access fields and invoke methods on script
 * objects and classes. Methods are resolved by name and argument types, choosing the most
 * specific applicable overload with support for unboxing, primitive widening and varargs.
 */
public abstract class ReflectionSupport {
    private ReflectionSupport() {
    }

    public static Object accessField(Object target, Class<?> clazz, String fieldName) {
        Field field = ReflectionUtils.findField(clazz, fieldName);
        if (field == null) {
            throw new IllegalArgumentException(
                    "No matching field '%s' in %s"
                            .formatted(fieldName, clazz.getSimpleName())
            );
        }
        ReflectionUtils.makeAccessible(field);
        return ReflectionUtils.getField(field, target);
    }

    public static Object invoke(
            Object target,
            Class<?> targetClass,
            String methodName,
            List<Object> args) {

        Method method = findMethod(methodName, target, targetClass, args);

        if (method == null) {
            throw new IllegalArgumentException(
                    "No matching method '%s' on %s"
                            .formatted(methodName, targetClass.getName())
            );
        }

        Object[] invocationArgs = method.isVarArgs()
                ? packVarArgs(method, args)
                : args.toArray();

        ReflectionUtils.makeAccessible(method);

        return ReflectionUtils.invokeMethod(method, target, invocationArgs);
    }

    private static final Map<Class<?>, List<Class<?>>> PRIMITIVE_WIDENING = Map.of(
            byte.class, List.of(short.class, int.class, long.class, float.class, double.class),
            short.class, List.of(int.class, long.class, float.class, double.class),
            char.class, List.of(int.class, long.class, float.class, double.class),
            int.class, List.of(long.class, float.class, double.class),
            long.class, List.of(float.class, double.class),
            float.class, List.of(double.class));

    private static final Map<Class<?>, Class<?>> WRAPPER_TO_PRIMITIVE = Map.of(
            Boolean.class, boolean.class, Byte.class, byte.class, Character.class, char.class,
            Short.class, short.class, Integer.class, int.class, Long.class, long.class,
            Float.class, float.class, Double.class, double.class);

    /**
     * Finds the applicable method preferring fixed-arity methods over varargs, as in Java, and
     * then the lowest conversion cost, so exact argument types over subtypes and widening. Ties
     * are broken by signature so that the choice doesn't depend on the JVM method order.
     */
    private static Method findMethod(
            String methodName,
            Object target,
            Class<?> targetClass,
            List<Object> args) {
        return Arrays.stream(ReflectionUtils.getAllDeclaredMethods(targetClass))
                .filter(method -> methodName.equals(method.getName()))
                .filter(method -> Modifier.isPublic(method.getModifiers()))
                .filter(method -> canAccess(method, target))
                .map(method -> Map.entry(method, cost(method, args)))
                .filter(entry -> entry.getValue() >= 0)
                .min(Comparator.<Map.Entry<Method, Integer>, Boolean>comparing(entry -> entry.getKey().isVarArgs())
                        .thenComparingInt(Map.Entry::getValue)
                        .thenComparing(entry -> entry.getKey().toGenericString()))
                .map(Map.Entry::getKey)
                .orElse(null);
    }

    private static boolean canAccess(Method method, Object target) {
        boolean isStatic = Modifier.isStatic(method.getModifiers());
        if (!isStatic && target == null) {
            return false; // Instance method on a class reference
        }
        try {
            return method.canAccess(isStatic ? null : target);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * @return the conversion cost of invoking the method with the arguments, or -1 if not applicable
     */
    private static int cost(Method method, List<Object> args) {
        Class<?>[] parameterTypes = method.getParameterTypes();

        // Spread the trailing arguments into the varargs array, unless the caller supplied it
        boolean spread = method.isVarArgs() && !isVarArgsArray(parameterTypes, args);
        int fixedCount = spread ? parameterTypes.length - 1 : parameterTypes.length;
        if (spread ? args.size() < fixedCount : args.size() != fixedCount) {
            return -1;
        }

        int total = 0;
        for (int i = 0; i < args.size(); i++) {
            Class<?> parameterType = i < fixedCount
                    ? parameterTypes[i]
                    : parameterTypes[fixedCount].getComponentType();
            int c = argumentCost(parameterType, args.get(i));
            if (c < 0) {
                return -1;
            }
            total += c;
        }
        return total;
    }

    /**
     * @return 0 for an exact type, 1 for a subtype or null, 2 for primitive widening, or -1 if not applicable
     */
    private static int argumentCost(Class<?> parameterType, Object arg) {
        if (arg == null) {
            return parameterType.isPrimitive() ? -1 : 1;
        }

        Class<?> argType = arg.getClass();
        Class<?> argPrimitive = WRAPPER_TO_PRIMITIVE.get(argType);
        if (parameterType == argType || parameterType == argPrimitive) {
            return 0;
        }
        if (ClassUtils.isAssignableValue(parameterType, arg)) {
            return 1;
        }

        // Unboxing followed by widening, as supported by Method.invoke for primitive parameters
        if (parameterType.isPrimitive() && argPrimitive != null
            && PRIMITIVE_WIDENING.getOrDefault(argPrimitive, List.of()).contains(parameterType)) {
            return 2;
        }

        return -1;
    }

    private static boolean isVarArgsArray(Class<?>[] parameterTypes, List<Object> args) {
        int fixedCount = parameterTypes.length - 1;
        // A null argument is a single null vararg rather than a null array
        return args.size() == parameterTypes.length
               && args.get(fixedCount) != null
               && parameterTypes[fixedCount].isInstance(args.get(fixedCount));
    }

    private static Object[] packVarArgs(Method method, List<Object> args) {
        Class<?>[] parameterTypes = method.getParameterTypes();
        int fixedCount = parameterTypes.length - 1;

        // Preserve an explicitly supplied varargs array.
        if (isVarArgsArray(parameterTypes, args)) {
            return args.toArray();
        }

        Object[] invocationArgs = new Object[parameterTypes.length];

        for (int i = 0; i < fixedCount; i++) {
            invocationArgs[i] = args.get(i);
        }

        Class<?> componentType = parameterTypes[fixedCount].getComponentType();
        int varargCount = args.size() - fixedCount;
        Object varargsArray = Array.newInstance(componentType, varargCount);

        for (int i = 0; i < varargCount; i++) {
            Array.set(varargsArray, i, args.get(fixedCount + i));
        }

        invocationArgs[fixedCount] = varargsArray;
        return invocationArgs;
    }
}
