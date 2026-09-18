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
import java.nio.file.Path;
import java.time.Clock;
import org.springframework.beans.factory.annotation.Value;
import java.time.LocalDate;
import java.time.ZoneId;

@Component
@ConditionalOnProperty(name="sync.mock-enabled", havingValue="true", matchIfMissing=true)
public class MockSyncRunner implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(MockSyncRunner.class);
    private final Path dataDirectory;
    private final ZoneId zoneId;
    public MockSyncRunner(@Value("${sync.data-directory:target/mock-data}") String dataDirectory,
                          @Value("${sync.timezone:America/Chicago}") String timezone) {
        this.dataDirectory = Path.of(dataDirectory);
        this.zoneId = ZoneId.of(timezone);
    }
    public void run(ApplicationArguments args) throws Exception {
        var mapper = new ObjectMapper().registerModule(new JavaTimeModule());
        try (var input = getClass().getResourceAsStream("/fixtures/daily-settlement.json")) {
            var report = mapper.readValue(input, MoeGoDailySettlement.class);
            var accounts = new EnumMap<AccountRole,String>(AccountRole.class);
            for (var role : AccountRole.values()) accounts.put(role, "mock-" + role.name().toLowerCase());
            var service = new JournalSyncService(new InMemoryQuickBooksClient(), new JournalEntryFactory(), new AccountMapping(accounts, Map.of()),
                    new FileSyncReceiptStore(dataDirectory.resolve("receipts")), Clock.systemUTC());
            DailySalesSummaryProvider provider = date -> new MoeGoMapper().map(withDate(report, date), "USD");
            var reconciliation = new ReconciliationRunner(new ReconciliationPlanner(), provider, service, dataDirectory)
                    .run(LocalDate.now(zoneId));
            reconciliation.results().forEach((date, result) -> log.info("Mock MoeGo {}: {} journal entry {}", date, result.action(), result.quickBooksId()));
            reconciliation.failures().forEach((date, failure) -> log.error("Mock MoeGo {} failed: {}", date, failure));
        }
    }
    private MoeGoDailySettlement withDate(MoeGoDailySettlement r, LocalDate date) {
        return new MoeGoDailySettlement(r.businessId(), date, r.totals(), r.payments());
    }
}
