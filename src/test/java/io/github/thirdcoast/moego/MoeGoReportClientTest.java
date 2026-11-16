package io.github.thirdcoast.moego;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class MoeGoReportClientTest {
    @Test
    void readsSignedMoneyFromReportCell() throws Exception {
        var row = new ObjectMapper().readTree("""
                {"data":{"paid":{"value":{"money":{"currencyCode":"USD","units":"-41","nanos":-270000000}}}}}
                """);
        assertEquals(0, new BigDecimal("-41.27").compareTo(MoeGoReportClient.money(row, "paid")));
        assertThrows(IllegalStateException.class, () -> MoeGoReportClient.money(row, "missing"));
    }
}
