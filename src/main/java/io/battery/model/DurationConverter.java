package io.battery.model;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationPropertiesBinding;
import org.springframework.context.annotation.Role;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

import io.battery.util.DurationUtils;
import static org.springframework.beans.factory.config.BeanDefinition.ROLE_INFRASTRUCTURE;

/**
 * Binds duration properties from strings such as {@code "1h30m"}, which combine the units
 * {@code s}, {@code m}, {@code h}, {@code d} and {@code w}. A plain number is read as seconds.
 */
@Component
@ConfigurationPropertiesBinding
@Role(ROLE_INFRASTRUCTURE)
public class DurationConverter implements Converter<String, Duration> {
    @Override
    public Duration convert(String source) {
        return DurationUtils.parseDuration(source);
    }
}
