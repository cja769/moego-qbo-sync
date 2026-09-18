package io.github.thirdcoast.moego;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.github.thirdcoast.qbo.*;
import io.github.thirdcoast.qbo.model.AccountRole;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import java.util.EnumMap;
import java.util.Map;

@Component
@ConditionalOnProperty(name="sync.mock-enabled", havingValue="true", matchIfMissing=true)
public class MockSyncRunner implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(MockSyncRunner.class);
    public void run(ApplicationArguments args) throws Exception {
        var mapper = new ObjectMapper().registerModule(new JavaTimeModule());
        try (var input = getClass().getResourceAsStream("/fixtures/daily-settlement.json")) {
            var report = mapper.readValue(input, MoeGoDailySettlement.class);
            var accounts = new EnumMap<AccountRole,String>(AccountRole.class);
            for (var role : AccountRole.values()) accounts.put(role, "mock-" + role.name().toLowerCase());
            var service = new JournalSyncService(new InMemoryQuickBooksClient(), new JournalEntryFactory(), new AccountMapping(accounts, Map.of()));
            var result = service.sync(new MoeGoMapper().map(report, "USD"));
            log.info("Mock MoeGo sync created journal entry {}", result.quickBooksId());
        }
    }
}
