package pe.centrosalud;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** Punto de arranque técnico; no contiene lógica de negocio. */
@SpringBootApplication
public class CentroSaludApplication {
    public static void main(String[] args) {
        SpringApplication.run(CentroSaludApplication.class, args);
    }
}
