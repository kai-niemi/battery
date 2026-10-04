package io.battery.web.frontend.model;

import org.springframework.hateoas.RepresentationModel;

import jakarta.validation.constraints.NotNull;

import io.battery.model.ScriptFormat;

public class ScriptForm extends RepresentationModel<ScriptForm> {
    private ScriptFormat scriptFormat;

    @NotNull
    private String input;

    private String output;

    private boolean capture = true;

    private int errorFromLine;

    private int errorFromChar;

    private int errorToLine;

    private int errorToChar;

    public ScriptFormat getScriptFormat() {
        return scriptFormat;
    }

    public void setScriptFormat(ScriptFormat scriptFormat) {
        this.scriptFormat = scriptFormat;
    }

    public String getInput() {
        return input;
    }

    public void setInput(String input) {
        this.input = input;
    }

    public String getOutput() {
        return output;
    }

    public void setOutput(String output) {
        this.output = output;
    }

    public boolean getCapture() {
        return capture;
    }

    public void setCapture(boolean capture) {
        this.capture = capture;
    }

    public int getErrorFromChar() {
        return errorFromChar;
    }

    public void setErrorFromChar(int errorFromChar) {
        this.errorFromChar = errorFromChar;
    }

    public int getErrorFromLine() {
        return errorFromLine;
    }

    public void setErrorFromLine(int errorFromLine) {
        this.errorFromLine = errorFromLine;
    }

    public int getErrorToChar() {
        return errorToChar;
    }

    public void setErrorToChar(int errorToChar) {
        this.errorToChar = errorToChar;
    }

    public int getErrorToLine() {
        return errorToLine;
    }

    public void setErrorToLine(int errorToLine) {
        this.errorToLine = errorToLine;
    }
}
