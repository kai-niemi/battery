package io.battery.shell.provider;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.shell.core.command.completion.CompletionContext;
import org.springframework.shell.core.command.completion.CompletionProposal;
import org.springframework.shell.core.command.completion.CompletionProvider;
import org.springframework.util.ReflectionUtils;

import io.battery.script.BatteryScript;
import io.battery.script.function.Description;
import io.battery.script.function.FunctionDef;
import io.battery.script.function.Volatility;

public class FunctionProvider implements CompletionProvider {
    public static boolean isQualified(Method method) {
        return Modifier.isPublic(method.getModifiers()) && !ReflectionUtils.isObjectMethod(method);
    }

    public static FunctionDef toFunctionDef(String namespace, Method method) {
        Description description = AnnotationUtils.findAnnotation(method, Description.class);
        FunctionDef functionDef = new FunctionDef();
        functionDef.setDescription(description != null ? description.value() : "n/a");
        functionDef.setVolatility(description != null ? description.volatility() : Volatility.Undefined);
        functionDef.setSignature(toShortString(namespace, method));
        functionDef.setNamespace(namespace);
        return functionDef;
    }

    public static String toShortString(String namespace, Method method) {
        StringBuilder sb = new StringBuilder();

        sb.append(namespace);
        sb.append(".");
        sb.append(method.getName());
        sb.append('(');

        StringJoiner sj = new StringJoiner(",");
        Type[] paramTypes = method.getGenericParameterTypes();
        Parameter[] params = method.getParameters();
        for (int j = 0; j < params.length; j++) {
            String paramType = paramTypes[j].getTypeName();
            String paramName = params[j].getName();
            if (method.isVarArgs() && (j == params.length - 1)) {
                paramType = paramType.replaceFirst("\\[\\]$", "...");
            }
            sj.add(paramName + ": " + paramType);
        }

        sb.append(sj);
        sb.append(')');

        return sb.toString();
    }

    private final BatteryScript batteryScript;

    public FunctionProvider(BatteryScript batteryScript) {
        this.batteryScript = batteryScript;
    }

    @Override
    public List<CompletionProposal> apply(CompletionContext completionContext) {
        List<CompletionProposal> result = new ArrayList<>();

        final String prefix = completionContext.currentWordUpToCursor() != null
                ? completionContext.currentWordUpToCursor() : "";

        batteryScript.forEachExternalVariable(namespace -> {
            batteryScript.forEachMethod(namespace, method -> {
                if (isQualified(method)) {
                    if (method.getName().startsWith(prefix) || namespace.startsWith(prefix)) {
                        result.add(new CompletionProposal(toShortString(namespace, method))
                                .category(namespace + " (" + method.getDeclaringClass().getSimpleName() + ")")
                                .displayText(namespace + "." + method.getName()));
                    }
                }
            });
        });

        return result;
    }
}

