package de.paladinsinn.torg.codex;

import de.paladinsinn.drivethru.EnableDrivethruRPG;
import de.paladinsinn.security.EnableDrivethruRPGSecurity;
import de.paladinsinn.torg.codex.data.EnableTorgData;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@EnableDrivethruRPG
@EnableDrivethruRPGSecurity
@EnableTorgData
public class TorgCodexApplication {

  public static void main(String[] args) {
    SpringApplication.run(TorgCodexApplication.class, args);
  }

}
