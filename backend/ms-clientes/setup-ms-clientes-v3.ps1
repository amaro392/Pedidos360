# Script para reconstruir ms-clientes directamente (sin zip, sin BOM)
$ErrorActionPreference = 'Stop'
$utf8NoBom = New-Object System.Text.UTF8Encoding $false
Write-Host "Creando carpetas..." -ForegroundColor Cyan
New-Item -ItemType Directory -Force -Path "src\main\java\cl\duoc\pedidos360\msclientes\config" | Out-Null
New-Item -ItemType Directory -Force -Path "src\main\java\cl\duoc\pedidos360\msclientes\controller" | Out-Null
New-Item -ItemType Directory -Force -Path "src\main\java\cl\duoc\pedidos360\msclientes\entity" | Out-Null
New-Item -ItemType Directory -Force -Path "src\main\java\cl\duoc\pedidos360\msclientes\repository" | Out-Null
New-Item -ItemType Directory -Force -Path "src\main\java\cl\duoc\pedidos360\msclientes\security" | Out-Null
New-Item -ItemType Directory -Force -Path "src\main\java\cl\duoc\pedidos360\msclientes\service" | Out-Null
New-Item -ItemType Directory -Force -Path "src\main\resources" | Out-Null
Write-Host "Escribiendo archivos (sin BOM)..." -ForegroundColor Cyan
$content = @'
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
	xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
	<modelVersion>4.0.0</modelVersion>
	<parent>
		<groupId>org.springframework.boot</groupId>
		<artifactId>spring-boot-starter-parent</artifactId>
		<version>4.1.1</version>
		<relativePath/> <!-- lookup parent from repository -->
	</parent>
	<groupId>cl.duoc.pedidos360</groupId>
	<artifactId>ms-clientes</artifactId>
	<version>0.0.1-SNAPSHOT</version>
	<name/>
	<description/>
	<url/>
	<licenses>
		<license/>
	</licenses>
	<developers>
		<developer/>
	</developers>
	<scm>
		<connection/>
		<developerConnection/>
		<tag/>
		<url/>
	</scm>
	<properties>
		<java.version>17</java.version>
	</properties>
	<dependencies>
		<dependency>
			<groupId>org.springframework.boot</groupId>
			<artifactId>spring-boot-starter-data-jpa</artifactId>
		</dependency>
		<dependency>
			<groupId>org.springframework.boot</groupId>
			<artifactId>spring-boot-starter-security-oauth2-resource-server</artifactId>
		</dependency>
		<dependency>
			<groupId>org.springframework.boot</groupId>
			<artifactId>spring-boot-starter-validation</artifactId>
		</dependency>
		<dependency>
			<groupId>org.springframework.boot</groupId>
			<artifactId>spring-boot-starter-webmvc</artifactId>
		</dependency>

		<dependency>
			<groupId>org.springframework.boot</groupId>
			<artifactId>spring-boot-devtools</artifactId>
			<scope>runtime</scope>
			<optional>true</optional>
		</dependency>
		<dependency>
			<groupId>com.mysql</groupId>
			<artifactId>mysql-connector-j</artifactId>
			<scope>runtime</scope>
		</dependency>
		<dependency>
			<groupId>com.h2database</groupId>
			<artifactId>h2</artifactId>
			<scope>runtime</scope>
		</dependency>
		<dependency>
			<groupId>org.springframework.boot</groupId>
			<artifactId>spring-boot-starter-data-jpa-test</artifactId>
			<scope>test</scope>
		</dependency>
		<dependency>
			<groupId>org.springframework.boot</groupId>
			<artifactId>spring-boot-starter-security-oauth2-resource-server-test</artifactId>
			<scope>test</scope>
		</dependency>
		<dependency>
			<groupId>org.springframework.boot</groupId>
			<artifactId>spring-boot-starter-validation-test</artifactId>
			<scope>test</scope>
		</dependency>
		<dependency>
			<groupId>org.springframework.boot</groupId>
			<artifactId>spring-boot-starter-webmvc-test</artifactId>
			<scope>test</scope>
		</dependency>
	</dependencies>

	<build>
		<plugins>
			<plugin>
				<groupId>org.springframework.boot</groupId>
				<artifactId>spring-boot-maven-plugin</artifactId>
			</plugin>
		</plugins>
	</build>

</project>

'@
[System.IO.File]::WriteAllText((Join-Path (Get-Location) "pom.xml"), $content, $utf8NoBom)
Write-Host "  OK: pom.xml"

$content = @'
spring.application.name=ms-clientes
server.port=8084

# Conexion H2 en memoria (se reinicia limpia cada vez que ejecutas el proyecto)
spring.datasource.url=jdbc:h2:mem:clientesdb
spring.datasource.driver-class-name=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=

# Crea las tablas limpias desde cero en cada reinicio
spring.jpa.hibernate.ddl-auto=create-drop
spring.h2.console.enabled=true

# Azure AD
azure.ad.jwk-set-uri=https://login.microsoftonline.com/abf8edad-bd14-425d-9255-2e0e7e57dfa2/discovery/v2.0/keys
azure.ad.issuer=https://sts.windows.net/abf8edad-bd14-425d-9255-2e0e7e57dfa2/
azure.ad.audience=api://9078dcc3-9503-4237-a463-d5a4a96cb61f

# Inicializacion de SQL
spring.jpa.defer-datasource-initialization=true
spring.sql.init.mode=always

'@
[System.IO.File]::WriteAllText((Join-Path (Get-Location) "src\main\resources\application.properties"), $content, $utf8NoBom)
Write-Host "  OK: src\main\resources\application.properties"

$content = @'
INSERT INTO clientes (nombre, email, telefono, direccion) VALUES
('Amaro Fuentes', 'amaro.fuentes@duocuc.cl', '+56912345678', 'Av. Providencia 1234, Santiago'),
('Camila Rojas', 'camila.rojas@gmail.com', '+56923456789', 'Los Aromos 456, Ñuñoa'),
('Benjamin Soto', 'benjamin.soto@hotmail.com', '+56934567890', 'Calle Nueva 789, Maipu'),
('Fernanda Diaz', 'fernanda.diaz@outlook.com', '+56945678901', 'Pasaje Los Alamos 321, La Florida');

'@
[System.IO.File]::WriteAllText((Join-Path (Get-Location) "src\main\resources\data.sql"), $content, $utf8NoBom)
Write-Host "  OK: src\main\resources\data.sql"

$content = @'
package cl.duoc.pedidos360.msclientes.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "clientes")
public class Cliente {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nombre;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String telefono;

    @Column(nullable = false)
    private String direccion;

    public Cliente() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }

    public String getDireccion() { return direccion; }
    public void setDireccion(String direccion) { this.direccion = direccion; }
}

'@
[System.IO.File]::WriteAllText((Join-Path (Get-Location) "src\main\java\cl\duoc\pedidos360\msclientes\entity\Cliente.java"), $content, $utf8NoBom)
Write-Host "  OK: src\main\java\cl\duoc\pedidos360\msclientes\entity\Cliente.java"

$content = @'
package cl.duoc.pedidos360.msclientes.repository;

import cl.duoc.pedidos360.msclientes.entity.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ClienteRepository extends JpaRepository<Cliente, Long> {
    Optional<Cliente> findByEmail(String email);
}

'@
[System.IO.File]::WriteAllText((Join-Path (Get-Location) "src\main\java\cl\duoc\pedidos360\msclientes\repository\ClienteRepository.java"), $content, $utf8NoBom)
Write-Host "  OK: src\main\java\cl\duoc\pedidos360\msclientes\repository\ClienteRepository.java"

$content = @'
package cl.duoc.pedidos360.msclientes.service;

import cl.duoc.pedidos360.msclientes.entity.Cliente;
import cl.duoc.pedidos360.msclientes.repository.ClienteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ClienteService {
    @Autowired
    private ClienteRepository repository;

    public List<Cliente> listar() { return repository.findAll(); }

    public Cliente buscarPorId(Long id) { return repository.findById(id).orElse(null); }

    public Cliente buscarPorEmail(String email) { return repository.findByEmail(email).orElse(null); }

    public Cliente guardar(Cliente c) { return repository.save(c); }

    public Cliente actualizar(Long id, Cliente datos) {
        Cliente existente = repository.findById(id).orElse(null);
        if (existente == null) return null;
        existente.setNombre(datos.getNombre());
        existente.setEmail(datos.getEmail());
        existente.setTelefono(datos.getTelefono());
        existente.setDireccion(datos.getDireccion());
        return repository.save(existente);
    }

    public boolean eliminar(Long id) {
        if (!repository.existsById(id)) return false;
        repository.deleteById(id);
        return true;
    }
}

'@
[System.IO.File]::WriteAllText((Join-Path (Get-Location) "src\main\java\cl\duoc\pedidos360\msclientes\service\ClienteService.java"), $content, $utf8NoBom)
Write-Host "  OK: src\main\java\cl\duoc\pedidos360\msclientes\service\ClienteService.java"

$content = @'
package cl.duoc.pedidos360.msclientes.controller;

import cl.duoc.pedidos360.msclientes.entity.Cliente;
import cl.duoc.pedidos360.msclientes.service.ClienteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/clientes")
@CrossOrigin(origins = "*") // Permite las peticiones desde el frontend de Angular
public class ClienteController {

    @Autowired
    private ClienteService service;

    @GetMapping
    public List<Cliente> listar() {
        return service.listar();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Cliente> buscar(@PathVariable Long id) {
        Cliente cliente = service.buscarPorId(id);
        return cliente != null ? ResponseEntity.ok(cliente) : ResponseEntity.notFound().build();
    }

    @GetMapping("/email/{email}")
    public ResponseEntity<Cliente> buscarPorEmail(@PathVariable String email) {
        Cliente cliente = service.buscarPorEmail(email);
        return cliente != null ? ResponseEntity.ok(cliente) : ResponseEntity.notFound().build();
    }

    @PostMapping
    public ResponseEntity<Cliente> crear(@RequestBody Cliente cliente) {
        Cliente creado = service.guardar(cliente);
        return ResponseEntity.status(HttpStatus.CREATED).body(creado);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Cliente> actualizar(@PathVariable Long id, @RequestBody Cliente cliente) {
        Cliente actualizado = service.actualizar(id, cliente);
        return actualizado != null ? ResponseEntity.ok(actualizado) : ResponseEntity.notFound().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        boolean eliminado = service.eliminar(id);
        return eliminado ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }
}

'@
[System.IO.File]::WriteAllText((Join-Path (Get-Location) "src\main\java\cl\duoc\pedidos360\msclientes\controller\ClienteController.java"), $content, $utf8NoBom)
Write-Host "  OK: src\main\java\cl\duoc\pedidos360\msclientes\controller\ClienteController.java"

$content = @'
package cl.duoc.pedidos360.msclientes.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
public class CorsConfig {

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of("http://localhost:4200"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}

'@
[System.IO.File]::WriteAllText((Join-Path (Get-Location) "src\main\java\cl\duoc\pedidos360\msclientes\config\CorsConfig.java"), $content, $utf8NoBom)
Write-Host "  OK: src\main\java\cl\duoc\pedidos360\msclientes\config\CorsConfig.java"

$content = @'
package cl.duoc.pedidos360.msclientes.config;

import cl.duoc.pedidos360.msclientes.security.AudienceValidator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Value("${azure.ad.jwk-set-uri}")
    private String jwkSetUri;

    @Value("${azure.ad.issuer}")
    private String issuer;

    @Value("${azure.ad.audience}")
    private String audience;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .headers(headers -> headers.frameOptions(frame -> frame.disable()))
            .cors(Customizer.withDefaults())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .requestMatchers("/h2-console/**").permitAll()
                .anyRequest().authenticated())
            .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.decoder(jwtDecoder())));
        return http.build();
    }

    @Bean
    public JwtDecoder jwtDecoder() {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build();

        OAuth2TokenValidator<Jwt> withIssuer = JwtValidators.createDefaultWithIssuer(issuer);
        OAuth2TokenValidator<Jwt> withAudience = new AudienceValidator(audience);
        OAuth2TokenValidator<Jwt> combined = new DelegatingOAuth2TokenValidator<>(withIssuer, withAudience);

        decoder.setJwtValidator(combined);
        return decoder;
    }
}

'@
[System.IO.File]::WriteAllText((Join-Path (Get-Location) "src\main\java\cl\duoc\pedidos360\msclientes\config\SecurityConfig.java"), $content, $utf8NoBom)
Write-Host "  OK: src\main\java\cl\duoc\pedidos360\msclientes\config\SecurityConfig.java"

$content = @'
package cl.duoc.pedidos360.msclientes.security;

import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

public class AudienceValidator implements OAuth2TokenValidator<Jwt> {

    private final String expectedAudience;

    public AudienceValidator(String expectedAudience) {
        this.expectedAudience = expectedAudience;
    }

    @Override
    public OAuth2TokenValidatorResult validate(Jwt jwt) {
        if (jwt.getAudience() != null && jwt.getAudience().contains(expectedAudience)) {
            return OAuth2TokenValidatorResult.success();
        }
        OAuth2Error error = new OAuth2Error(
            "invalid_token",
            "El token no fue emitido para esta API (audience invalido)",
            null
        );
        return OAuth2TokenValidatorResult.failure(error);
    }
}

'@
[System.IO.File]::WriteAllText((Join-Path (Get-Location) "src\main\java\cl\duoc\pedidos360\msclientes\security\AudienceValidator.java"), $content, $utf8NoBom)
Write-Host "  OK: src\main\java\cl\duoc\pedidos360\msclientes\security\AudienceValidator.java"

Write-Host ""
Write-Host "Listo. Verificando application.properties:" -ForegroundColor Green
Get-Content ".\src\main\resources\application.properties"
Write-Host ""
Write-Host "Estructura final:" -ForegroundColor Green
Get-ChildItem -Recurse ".\src\main\java\cl\duoc\pedidos360\msclientes" | Select-Object FullName