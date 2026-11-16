package io.github.thirdcoast.moego;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.thirdcoast.qbo.DailySalesSummaryProvider;
import io.github.thirdcoast.qbo.JsonHttp;
import io.github.thirdcoast.qbo.model.DailySalesSummary;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import static io.github.thirdcoast.qbo.JsonHttp.*;

final class MoeGoReportClient implements DailySalesSummaryProvider {
    private static final String API = "https://openapi.moego.pet/v1/";
    private final JsonHttp http;
    private final String authorization;
    private final String company;
    private final String business;
    private final ZoneId zone;
    private final JsonNode mapping;

    MoeGoReportClient(JsonHttp http, String apiKey, String company, String business, ZoneId zone, Path mapping) {
        this.http = http;
        this.authorization = basic(apiKey);
        this.company = company;
        this.business = business;
        this.zone = zone;
        try {
            this.mapping = mapping == null ? null : new ObjectMapper().readTree(mapping.toFile());
        } catch (IOException e) {
            throw new IllegalArgumentException("Cannot read the MoeGo report mapping");
        }
    }

    JsonNode reportCatalog() {
        var request = object().put("companyId", company);
        var reports = http.post(URI.create(API + "reports:list"), authorization, request).path("reports");
        if (!reports.isArray()) throw new IllegalStateException("Missing MoeGo report catalog");
        return reports;
    }

    @Override
    public DailySalesSummary load(LocalDate date) {
        var sales = mapping.path("sales");
        var payments = mapping.path("payments");
        var columns = sales.path("fields");
        var salesRows = fetch(date, sales, "");
        var totals = new MoeGoDailySettlement.Totals(
                sum(salesRows, columns, "grossSales"), sum(salesRows, columns, "discounts"),
                sum(salesRows, columns, "refunds"), sum(salesRows, columns, "taxes"),
                sum(salesRows, columns, "tips"), sum(salesRows, columns, "giftCardsIssued"),
                sum(salesRows, columns, "giftCardsUsed"), sum(salesRows, columns, "processingFees"));

        var method = text(payments, "methodField");
        var amount = text(payments, "amountField");
        var tenderRows = fetch(date, payments, method);
        var tenders = new ArrayList<MoeGoDailySettlement.Payment>();
        var excluded = new HashSet<String>();
        for (var item : array(payments, "excludedMethods")) {
            if (!item.isTextual() || item.asText().isBlank()) {
                throw new IllegalArgumentException("MoeGo excluded methods must be nonempty names");
            }
            excluded.add(item.asText());
        }
        for (var row : tenderRows) {
            var label = text(row.path("data").path(method).path("value"), "string");
            if (excluded.contains(label)) continue;
            var value = money(row, amount);
            if (value.signum() != 0) tenders.add(new MoeGoDailySettlement.Payment(label, value));
        }
        return new MoeGoMapper().map(new MoeGoDailySettlement(business, date, totals, tenders), "USD");
    }

    private List<JsonNode> fetch(LocalDate date, JsonNode report, String grouping) {
        var request = object().put("companyId", company);
        request.putArray("businessIds").add(business);
        var condition = request.putObject("condition").put("id", text(report, "reportId"));
        var interval = condition.putObject("queryPeriod");
        interval.put("startTime", date.atStartOfDay(zone).toInstant().toString());
        interval.put("endTime", date.plusDays(1).atStartOfDay(zone).toInstant().toString());
        if (!grouping.isEmpty()) condition.putArray("groupByFieldKeys").add(grouping);
        var pagination = request.putObject("pagination").put("pageSize", 100);
        var rows = new ArrayList<JsonNode>();
        var seen = new HashSet<String>();
        String page = "";
        do {
            pagination.put("pageToken", page);
            var response = http.post(URI.create(API + "reports/data:fetch"), authorization, request);
            var table = response.path("tableData");
            if (!table.isObject()) throw new IllegalStateException("Missing MoeGo report table");
            var pageRows = table.path("rows");
            if (!pageRows.isMissingNode() && !pageRows.isArray()) {
                throw new IllegalStateException("Invalid MoeGo report rows");
            }
            pageRows.forEach(rows::add);
            page = response.path("nextPageToken").asText("");
            if (!page.isEmpty() && !seen.add(page)) throw new IllegalStateException("Repeated MoeGo page token");
        } while (!page.isEmpty());
        return rows;
    }

    private static BigDecimal sum(List<JsonNode> rows, JsonNode fields, String name) {
        var field = fields.path(name);
        if (field.isNull()) return BigDecimal.ZERO;
        var key = text(fields, name);
        return rows.stream().map(row -> money(row, key)).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    static BigDecimal money(JsonNode row, String field) {
        var money = row.path("data").path(field).path("value").path("money");
        if (!"USD".equals(text(money, "currencyCode"))) throw new IllegalStateException("Expected USD MoeGo report cell");
        var units = money.path("units").asText("0");
        var nanos = money.path("nanos").asInt(0);
        return new BigDecimal(units).add(BigDecimal.valueOf(nanos, 9));
    }
}
