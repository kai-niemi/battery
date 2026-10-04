package io.battery.shell;

import java.lang.reflect.InvocationTargetException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.shell.core.command.ExitStatus;
import org.springframework.shell.core.command.exit.ExitStatusExceptionMapper;
import org.springframework.stereotype.Component;

import io.battery.shell.support.ExceptionEvent;
import io.battery.shell.support.GenericEvent;

@Component
public abstract class AbstractShellCommand {
    @Autowired
    private ApplicationEventPublisher applicationEventPublisher;

    protected <T> void publishEvent(T event) {
        applicationEventPublisher.publishEvent(GenericEvent.of(this, event));
    }

    @Bean
    public ExitStatusExceptionMapper commandExceptionMapper() {
        return exception -> {
            Throwable ex;
            if (exception instanceof InvocationTargetException) {
                ex = ((InvocationTargetException) exception).getTargetException();
            } else {
                ex = exception;
            }
            publishEvent(new ExceptionEvent(ex));
            return new ExitStatus(-2, ex.getMessage());
        };
    }
}
