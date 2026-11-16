package io.github.thirdcoast.moego;

import io.github.thirdcoast.qbo.JsonHttp;
import io.github.thirdcoast.qbo.LiveSyncJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.nio.file.Path;
import java.time.LocalDate;
import java.time.ZoneId;

import static io.github.thirdcoast.qbo.LiveSyncJob.required;
import static io.github.thirdcoast.qbo.LiveSyncJob.value;

@Configuration(proxyBeanMethods = false)
@EnableScheduling
@ConditionalOnProperty(name = "sync.mock-enabled", havingValue = "false")
class LiveSyncRunner implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(LiveSyncRunner.class);
    private final Environment environment;

    LiveSyncRunner(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void run(ApplicationArguments args) {
        var dates = args.getOptionValues("sync-date");
        if (args.containsOption("sync-date") && dates.size() != 1) {
            throw new IllegalArgumentException("Use --sync-date=YYYY-MM-DD");
        }
        var oneShot = "-".equals(environment.getRequiredProperty("sync.cron"));
        if (dates != null && !oneShot) throw new IllegalArgumentException("Disable SYNC_CRON for a one-date run");
        if (oneShot) execute(dates == null ? null : LocalDate.parse(dates.get(0)));
    }

    @Scheduled(cron = "${sync.cron}", zone = "${sync.timezone}")
    public void scheduledRun() {
        execute(null);
    }

    private void execute(LocalDate date) {
        java.util.function.Function<String, String> config = environment::getProperty;
        var zone = ZoneId.of(environment.getRequiredProperty("sync.timezone"));
        var listReports = Boolean.parseBoolean(value(config, "MOEGO_LIST_REPORTS", "false"));
        var provider = new MoeGoReportClient(new JsonHttp(), required(config, "MOEGO_API_KEY"),
                required(config, "MOEGO_COMPANY_ID"), listReports ? "" : required(config, "MOEGO_BUSINESS_ID"),
                zone, listReports ? null : Path.of(required(config, "MOEGO_REPORT_MAPPING_FILE")));
        if (listReports) {
            log.info("MoeGo report catalog {}", provider.reportCatalog());
            return;
        }
        new LiveSyncJob(config, provider, zone,
                Path.of(environment.getRequiredProperty("sync.data-directory")), log::info).run(date);
    }
}
