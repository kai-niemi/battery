package io.battery;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.data.jdbc.autoconfigure.DataJdbcRepositoriesAutoConfiguration;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.boot.jdbc.autoconfigure.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.transaction.autoconfigure.TransactionAutoConfiguration;
import org.springframework.shell.core.autoconfigure.SpringShellAutoConfiguration;

import io.battery.model.BatteryModel;

@EnableConfigurationProperties(value = {BatteryModel.class})
@SpringBootApplication(exclude = {
        TransactionAutoConfiguration.class,
        DataSourceAutoConfiguration.class,
        DataSourceTransactionManagerAutoConfiguration.class,
        DataJdbcRepositoriesAutoConfiguration.class,
        SpringShellAutoConfiguration.class,
//        ShellRunnerAutoConfiguration.class
})
public class TestApplication {
}
