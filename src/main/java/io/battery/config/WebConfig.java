package io.battery.config;

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import org.springframework.boot.actuate.endpoint.ApiVersion;
import org.springframework.boot.actuate.endpoint.web.EndpointMediaTypes;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.format.Formatter;
import org.springframework.format.FormatterRegistry;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.UriTemplate;
import org.springframework.hateoas.config.EnableHypermediaSupport;
import org.springframework.hateoas.mediatype.hal.CurieProvider;
import org.springframework.hateoas.mediatype.hal.DefaultCurieProvider;
import org.springframework.hateoas.mediatype.hal.forms.HalFormsConfiguration;
import org.springframework.hateoas.mediatype.hal.forms.HalFormsOptions;
import org.springframework.http.MediaType;
import org.springframework.http.converter.FormHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverters;
import org.springframework.web.filter.ForwardedHeaderFilter;
import org.springframework.web.servlet.config.annotation.ContentNegotiationConfigurer;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import io.battery.ProfileNames;
import io.battery.util.DurationUtils;
import io.battery.util.Money;
import io.battery.web.api.model.LinkRelations;
import io.battery.web.api.model.MessageModel;
import io.battery.web.api.model.MessageType;

@EnableWebMvc
@EnableHypermediaSupport(type = {
        EnableHypermediaSupport.HypermediaType.HAL_FORMS,
        EnableHypermediaSupport.HypermediaType.HAL,
})
@Configuration
@Profile(value = ProfileNames.ONLINE)
public class WebConfig implements WebMvcConfigurer {
    @Bean
    public EndpointMediaTypes endpointMediaTypes() {
        return new EndpointMediaTypes(
                ApiVersion.V3.getProducedMimeType().toString(),
                ApiVersion.V2.getProducedMimeType().toString(),
                "application/hal+json", // Added
                "application/json"
        );
    }

    @Override
    public void addFormatters(FormatterRegistry registry) {
        registry.addFormatterForFieldType(Duration.class, new Formatter<Duration>() {
            @Override
            public Duration parse(String text, Locale locale) {
                return DurationUtils.parseDuration(text);
            }

            @Override
            public String print(Duration object, Locale locale) {
                return DurationUtils.durationToDisplayString(object);
            }
        });

        registry.addFormatterForFieldType(LocalDateTime.class, new Formatter<LocalDateTime>() {
            @Override
            public LocalDateTime parse(String text, Locale locale) {
                throw new UnsupportedOperationException();
            }

            @Override
            public String print(LocalDateTime object, Locale locale) {
                return DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(object);
            }
        });

        registry.addFormatterForFieldType(Money.class, new Formatter<Money>() {
            @Override
            public Money parse(String text, Locale locale) {
                throw new UnsupportedOperationException();
            }

            @Override
            public String print(Money money, Locale locale) {
                int fractionDigits = money.getCurrency().getDefaultFractionDigits();
                DecimalFormat format = (DecimalFormat) NumberFormat.getCurrencyInstance(locale);
                format.setParseBigDecimal(true);
                format.setGroupingUsed(true);
                format.setMaximumFractionDigits(fractionDigits);
                format.setMinimumFractionDigits(fractionDigits);
                format.setRoundingMode(RoundingMode.UNNECESSARY);
                format.setCurrency(money.getCurrency());
                return format.format(money.getAmount());
            }
        });
    }

    @Bean
    public ForwardedHeaderFilter forwardedHeaderFilter() {
        return new ForwardedHeaderFilter();
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedMethods("GET", "POST", "PUT", "DELETE")
                .allowedOrigins("*")
                .allowedHeaders("*");
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/webjars/**")
                .addResourceLocations("/webjars/");
        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/static/");
        registry.addResourceHandler("/css/**")
                .addResourceLocations("classpath:/static/css/");
        registry.addResourceHandler("/js/**")
                .addResourceLocations("classpath:/static/js/");
        registry.addResourceHandler("/images/**")
                .addResourceLocations("classpath:/static/images/");
    }

    @Override
    public void configureContentNegotiation(ContentNegotiationConfigurer configurer) {
        configurer.defaultContentType(
                MediaTypes.HAL_JSON,
                MediaTypes.HAL_FORMS_JSON,
                MediaTypes.VND_ERROR_JSON,
                MediaType.APPLICATION_JSON,
                MediaType.ALL);
    }

    @Bean
    public HalFormsConfiguration halFormsConfiguration() {
        return new HalFormsConfiguration()
                .withOptions(MessageModel.class, "messageType", metadata ->
                        HalFormsOptions.inline(MessageType.values()));
    }

    @Override
    public void configureMessageConverters(HttpMessageConverters.ServerBuilder serverBuilder) {
        serverBuilder.addCustomConverter(new FormHttpMessageConverter());
    }

    @Bean
    public CurieProvider defaultCurieProvider() {
        return new DefaultCurieProvider(LinkRelations.CURIE_NAMESPACE, UriTemplate.of("/rels/{rel}"));
    }
}
