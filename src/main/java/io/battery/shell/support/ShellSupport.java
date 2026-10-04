package io.battery.shell.support;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.jline.terminal.Terminal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ResourceLoader;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.shell.jline.tui.component.SingleItemSelector;
import org.springframework.shell.jline.tui.component.support.SelectorItem;
import org.springframework.shell.jline.tui.style.TemplateExecutor;
import org.springframework.shell.jline.tui.table.BorderStyle;
import org.springframework.shell.jline.tui.table.TableBuilder;
import org.springframework.shell.jline.tui.table.TableModel;
import org.springframework.stereotype.Component;

import io.battery.ProfileNames;

@Component
@Profile(value = ProfileNames.SHELL)
public class ShellSupport {
    public static String prettyPrint(TableModel model) {
        TableBuilder tableBuilder = new TableBuilder(model);
        tableBuilder.addInnerBorder(BorderStyle.fancy_light);
        tableBuilder.addHeaderBorder(BorderStyle.fancy_double);
        tableBuilder.addOutlineBorder(BorderStyle.fancy_light);
        return tableBuilder.build().render(120);
    }

    @Autowired
    private ResourceLoader resourceLoader;

    @Autowired
    private TemplateExecutor templateExecutor;

    @Autowired
    @Lazy
    private Terminal terminal;

    public Optional<Pageable> askForPage(Page<?> page) {
        if (page.isEmpty()) {
            return Pageable.unpaged().toOptional();
        }

        List<SelectorItem<Pageable>> items = new ArrayList<>();

        items.add(SelectorItem.of("quit", Pageable.unpaged()));

        if (page.hasNext()) {
            items.add(SelectorItem.of("Next", page.nextOrLastPageable()));
        }
        if (page.hasPrevious()) {
            items.add(SelectorItem.of("Prev", page.previousOrFirstPageable()));
        }
        if (!page.isFirst()) {
            items.add(SelectorItem.of("First", PageRequest.of(0, page.getSize())));
        }
        if (!page.isLast()) {
            items.add(SelectorItem.of("Last", PageRequest.of(page.getTotalPages() - 1, page.getSize())));
        }

        SingleItemSelector<Pageable, SelectorItem<Pageable>> component
                = new SingleItemSelector<>(terminal, items, "Select page", null);
        component.setResourceLoader(resourceLoader);
        component.setTemplateExecutor(templateExecutor);

        SingleItemSelector.SingleItemSelectorContext<Pageable, SelectorItem<Pageable>> context
                = component.run(SingleItemSelector.SingleItemSelectorContext.empty());

        return context.getResultItem()
                .flatMap(si -> Optional.of(si.getItem()));
    }
}
