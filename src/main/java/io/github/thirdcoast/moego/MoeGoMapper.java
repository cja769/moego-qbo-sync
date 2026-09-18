package io.github.thirdcoast.moego;

import io.github.thirdcoast.qbo.model.DailySalesSummary;
import io.github.thirdcoast.qbo.model.Money;
import java.math.BigDecimal;
import java.util.Map;
import java.util.stream.Collectors;

public final class MoeGoMapper {
    public DailySalesSummary map(MoeGoDailySettlement r, String currency) {
        var t = r.totals();
        Map<String, Money> tenders = r.payments().stream().collect(Collectors.toMap(
                MoeGoDailySettlement.Payment::method,
                p -> new Money(p.amount(), currency),
                (a, b) -> a.add(b)));
        return new DailySalesSummary("moego:" + r.businessId() + ":" + r.serviceDate(), "moego", r.businessId(), r.serviceDate(),
                money(t.servicesAndProducts(), currency), money(t.discounts(), currency), money(t.refunds(), currency),
                money(t.taxes(), currency), money(t.gratuity(), currency), money(t.giftCardsIssued(), currency),
                money(t.giftCardsUsed(), currency), money(t.processingFees(), currency), tenders);
    }
    private Money money(BigDecimal value, String currency) { return new Money(value, currency); }
}
