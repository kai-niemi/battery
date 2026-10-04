package io.battery.model;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import org.springframework.util.StringUtils;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;

import jakarta.validation.constraints.NotBlank;

import io.battery.script.Constants;

/**
 * A named unit of work that runs embedded SQL, an embedded script or a script file. SQL
 * {@code params} are script expressions evaluated against the current state. A step that
 * {@code capture}s passes its results on to the following steps, otherwise it leaves the state
 * unchanged.
 * <p>
 * Step names must be unique across the model. A step can be disabled with {@code skip}, or
 * limited to the first {@code maxIterations} iterations of each virtual user.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@StepValidation
public class Step {
    private final UUID id = UUID.randomUUID();

    @NotBlank
    private String name;

    private Path path;

    private String script;

    private String sql;

    private String tag;

    private Map<String, String> params = new HashMap<>();

    private boolean capture;

    private String resultName;

    private int maxIterations;

    private boolean skip;

    @JsonIgnore
    public UUID getId() {
        return id;
    }

    public String getResultName() {
        return StringUtils.hasLength(resultName) ? resultName : Constants.LAST_RESULT_VAR;
    }

    public void setResultName(String resultName) {
        this.resultName = resultName;
    }

    public int getMaxIterations() {
        return maxIterations;
    }

    public void setMaxIterations(int maxIterations) {
        this.maxIterations = maxIterations;
    }

    public boolean isSkip() {
        return skip;
    }

    public void setSkip(boolean skip) {
        this.skip = skip;
    }

    public Map<String, String> getParams() {
        return params;
    }

    public void setParams(Map<String, String> params) {
        this.params = params;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Path getPath() {
        return path;
    }

    public void setPath(Path path) {
        this.path = path;
    }

    public boolean isCapture() {
        return capture;
    }

    public void setCapture(boolean capture) {
        this.capture = capture;
    }

    public String getSql() {
        return sql;
    }

    public void setSql(String sql) {
        this.sql = sql;
    }

    public String getScript() {
        return script;
    }

    public void setScript(String script) {
        this.script = script;
    }

    @JsonIgnore
    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public String describe() {
        if (!Objects.isNull(sql)) {
            return "Step '%s' using embedded SQL".formatted(name);
        }
        if (!Objects.isNull(script)) {
            return "Step '%s' using embedded script".formatted(name);
        }
        if (!Objects.isNull(path)) {
            return "Step '%s' using file '%s'"
                    .formatted(name, path.getFileName());
        }
        throw new IllegalStateException("Step '%s' has no valid sql/script/file".formatted(name));
    }

    public void resolvePath(Path baseDir) {
        if (Objects.nonNull(path)) {
            Path actualPath = baseDir.resolve(path);
            if (!Files.isRegularFile(actualPath)) {
                throw new ModelException("Invalid path: " + actualPath);
            }
            // Script files are only read, so they may well be read-only
            if (!Files.isReadable(actualPath)) {
                throw new ModelException("Not a readable path: " + actualPath);
            }
            setPath(actualPath);
        }
    }

    @Override
    public boolean equals(Object object) {
        if (object == null || getClass() != object.getClass()) {
            return false;
        }

        Step step = (Step) object;
        return name.equals(step.name);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    @Override
    public String toString() {
        return "Step{" +
               "capture=" + capture +
               ", name='" + name + '\'' +
               ", path='" + path + '\'' +
               ", sql='" + sql + '\'' +
               ", script='" + script + '\'' +
               '}';
    }
}
