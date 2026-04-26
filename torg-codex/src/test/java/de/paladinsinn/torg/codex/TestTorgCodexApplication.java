package de.paladinsinn.torg.codex;

import org.springframework.boot.SpringApplication;

public class TestTorgCodexApplication {
  
  public static void main(String[] args) {
    SpringApplication.from(TorgCodexApplication::main).with(TestcontainersConfiguration.class)
                     .run(args)
    ;
  }
  
}
