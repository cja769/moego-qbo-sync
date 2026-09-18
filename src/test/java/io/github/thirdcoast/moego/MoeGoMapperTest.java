package io.github.thirdcoast.moego;

import io.github.thirdcoast.qbo.SummaryValidator;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertEquals;

class MoeGoMapperTest {
    @Test void combinesRepeatedPaymentMethodsAndReconciles() {
        var totals = new MoeGoDailySettlement.Totals(bd("100.00"), bd("5.00"), bd("0.00"), bd("8.00"),
                bd("10.00"), bd("0.00"), bd("0.00"), bd("3.00"));
        var source = new MoeGoDailySettlement("business", LocalDate.of(2026,9,18), totals,
                List.of(new MoeGoDailySettlement.Payment("card", bd("100.00")), new MoeGoDailySettlement.Payment("card", bd("13.00"))));
        var normalized = new MoeGoMapper().map(source, "USD");
        SummaryValidator.validate(normalized);
        assertEquals(bd("113.00"), normalized.tenders().get("card").amount());
    }
    private BigDecimal bd(String value) { return new BigDecimal(value); }
}
