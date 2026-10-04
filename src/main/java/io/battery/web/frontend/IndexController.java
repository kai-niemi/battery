package io.battery.web.frontend;

import java.sql.SQLException;
import java.util.concurrent.Callable;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import io.battery.ProfileNames;
import io.battery.repository.MetadataRepository;

@WebController
@Profile(value = ProfileNames.ONLINE)
@RequestMapping("/")
public class IndexController {
    @Autowired
    private MetadataRepository metadataRepository;

    @GetMapping("/")
    public Callable<String> homePage(Model model) {
        model.addAttribute("databaseVersion", metadataRepository.databaseVersion());
        model.addAttribute("databaseIsolation", metadataRepository.databaseIsolation());
        return () -> "home";
    }

    @GetMapping("/error-test")
    public String errorTest() {
        throw new IllegalStateException("Disturbance!");
    }

    @GetMapping("/error-test2")
    public String errorTest2() throws Exception {
        throw new SQLException("Disturbance!", "12345");
    }

    @GetMapping("/error-test3")
    public String errorTest3() {
        throw new IncorrectResultSizeDataAccessException(100, 1);
    }
}
