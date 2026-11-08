package com.nikoskatrakoulis.courtbookingsystem;

import com.nikoskatrakoulis.courtbookingsystem.support.AbstractPostgresTest;
import org.junit.jupiter.api.Test;

/** Boots the whole app against real PostgreSQL: Flyway migrates, then Hibernate validates the schema. */
class CourtBookingSystemApplicationTests extends AbstractPostgresTest {

    @Test
    void contextLoads() {
    }
}
