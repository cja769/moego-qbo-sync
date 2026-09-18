package io.github.thirdcoast.moego;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record MoeGoDailySettlement(String businessId, LocalDate serviceDate, Totals totals, List<Payment> payments) {
    public record Totals(BigDecimal servicesAndProducts, BigDecimal discounts, BigDecimal refunds, BigDecimal taxes,
                         BigDecimal gratuity, BigDecimal giftCardsIssued, BigDecimal giftCardsUsed, BigDecimal processingFees) {}
    public record Payment(String method, BigDecimal amount) {}
}
