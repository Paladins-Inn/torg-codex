package de.paladinsinn.torg.codex.torgcodex;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class TorgCodexApplicationTests {
  
  @Test
  void contextLoads() {
  }
  
}
