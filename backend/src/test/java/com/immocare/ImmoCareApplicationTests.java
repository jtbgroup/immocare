package com.immocare;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

/**
 * Basic application context test.
 */
@SpringBootTest
@TestPropertySource(locations = "classpath:application-test.properties")
class ImmoCareApplicationTests {

  @Test
  void contextLoads() {
    // This test verifies that the Spring application context loads successfully
  }
}
