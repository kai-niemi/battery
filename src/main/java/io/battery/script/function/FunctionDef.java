package io.battery.script.function;

/**
 * Value object describing a script function (namespace, signature, description and
 * volatility) for display when listing functions. Not used to resolve or invoke functions.
 */
public class FunctionDef {
    private String signature;

    private String namespace;

    private String description;

    private Volatility volatility;

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getSignature() {
        return signature;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }

    public Volatility getVolatility() {
        return volatility;
    }

    public void setVolatility(Volatility volatility) {
        this.volatility = volatility;
    }
}
