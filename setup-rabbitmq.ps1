<#
.SYNOPSIS
  Agrega RabbitMQ a Pedidos360 (EP2 DSY1107): ms-pedidos (productor), ms-notificaciones
  (consumidores + DLQ + ACK manual) y el nuevo microservicio ms-rabbit-admin.

.USAGE
  Desde la raiz del repo (donde esta la carpeta backend) o desde dentro de backend:
    powershell -ExecutionPolicy Bypass -File .\setup-rabbitmq.ps1 -RabbitHost 54.12.34.56

.NOTES
  - Sobrescribe PedidoController.java (agrega solo el publisher) y edita los pom.xml (agrega starter-amqp).
  - Revisa los cambios con "git diff" / "git status" al terminar.
#>
param(
    [Parameter(Mandatory = $true)][string]$RabbitHost,
    [string]$Backend = ""
)

$ErrorActionPreference = "Stop"

if (-not $Backend) {
    if (Test-Path ".\backend\ms-pedidos") { $Backend = ".\backend" }
    elseif (Test-Path ".\ms-pedidos")     { $Backend = "." }
    else { throw "No encuentro ms-pedidos. Ejecuta el script desde la raiz del repo o pasa -Backend <ruta>." }
}
$Backend = (Resolve-Path $Backend).Path
$utf8 = New-Object System.Text.UTF8Encoding($false)
Write-Host "Backend: $Backend" -ForegroundColor Cyan

function Write-Src([string]$rel, [string]$content) {
    $path = Join-Path $Backend $rel
    $dir = Split-Path $path -Parent
    if (-not (Test-Path $dir)) { New-Item -ItemType Directory -Path $dir -Force | Out-Null }
    $text = $content.Replace('__RABBIT_HOST__', $RabbitHost)
    [System.IO.File]::WriteAllText($path, $text, $utf8)
    Write-Host "  + $rel"
}

function Add-AmqpDependency([string]$pomRel) {
    $p = Join-Path $Backend $pomRel
    $x = [System.IO.File]::ReadAllText($p)
    if ($x.Contains('spring-boot-starter-amqp')) { Write-Host "  = $pomRel (amqp ya estaba)"; return }
    $idx = $x.IndexOf('</dependencies>')
    if ($idx -lt 0) { throw "No encontre </dependencies> en $pomRel" }
    $dep = "<dependency>`n`t`t`t<groupId>org.springframework.boot</groupId>`n`t`t`t<artifactId>spring-boot-starter-amqp</artifactId>`n`t`t</dependency>`n`t"
    $x = $x.Insert($idx, "`t" + $dep)
    [System.IO.File]::WriteAllText($p, $x, $utf8)
    Write-Host "  ~ $pomRel (+ spring-boot-starter-amqp)"
}

# --------- 0. ms-rabbit-admin: esqueleto (mvnw, .mvn, .gitignore) copiado de ms-notificaciones ---------
Write-Host "`n[0/3] Esqueleto de ms-rabbit-admin" -ForegroundColor Yellow
$srcMs = Join-Path $Backend 'ms-notificaciones'
$dstMs = Join-Path $Backend 'ms-rabbit-admin'
if (-not (Test-Path $dstMs)) { New-Item -ItemType Directory -Path $dstMs | Out-Null }
foreach ($f in @('.mvn', 'mvnw', 'mvnw.cmd', '.gitignore', '.gitattributes')) {
    $s = Join-Path $srcMs $f
    if (Test-Path $s) { Copy-Item $s (Join-Path $dstMs $f) -Recurse -Force; Write-Host "  + ms-rabbit-admin/$f" }
}

# --------- 1. pom.xml ---------
Write-Host "`n[1/3] Dependencias (pom.xml)" -ForegroundColor Yellow
Add-AmqpDependency 'ms-pedidos/pom.xml'
Add-AmqpDependency 'ms-notificaciones/pom.xml'

# --------- 2. Codigo ---------
Write-Host "`n[2/3] Archivos de codigo" -ForegroundColor Yellow
Write-Src 'ms-notificaciones/src/main/java/cl/duoc/pedidos360/events/PedidoEvent.java' @'
package cl.duoc.pedidos360.events;

public record PedidoEvent(Long pedidoId, String clienteEmail, Double total,
                          String estado, String ocurridoEn) {}
'@

Write-Src 'ms-notificaciones/src/main/java/cl/duoc/pedidos360/ms_notificaciones/config/NotificacionesRabbitConfig.java' @'
package cl.duoc.pedidos360.ms_notificaciones.config;

import org.springframework.amqp.core.*;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class NotificacionesRabbitConfig {

    @Value("${app.rabbitmq.exchanges.dlx}") private String dlx;
    @Value("${app.rabbitmq.routing-keys.notificaciones-dead}") private String deadKey;

    @Bean
    public Queue notificacionesQueue(@Value("${app.rabbitmq.queues.notificaciones}") String name) {
        return QueueBuilder.durable(name).quorum()
                .deadLetterExchange(dlx).deadLetterRoutingKey(deadKey).build();
    }

    @Bean
    public Queue notificacionesDlq(@Value("${app.rabbitmq.queues.notificaciones-dlq}") String name) {
        return QueueBuilder.durable(name).quorum().build();
    }

    @Bean
    public Binding notificacionesBinding(@Qualifier("notificacionesQueue") Queue q,
            TopicExchange pedidosExchange,
            @Value("${app.rabbitmq.routing-keys.pedido-todos}") String key) {
        return BindingBuilder.bind(q).to(pedidosExchange).with(key);
    }

    @Bean
    public Binding notificacionesDlqBinding(@Qualifier("notificacionesDlq") Queue q,
            DirectExchange deadLetterExchange) {
        return BindingBuilder.bind(q).to(deadLetterExchange).with(deadKey);
    }
}
'@

Write-Src 'ms-notificaciones/src/main/java/cl/duoc/pedidos360/ms_notificaciones/config/RabbitCommonConfig.java' @'
package cl.duoc.pedidos360.ms_notificaciones.config;

import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitCommonConfig {

    @Bean
    public TopicExchange pedidosExchange(@Value("${app.rabbitmq.exchanges.pedidos}") String n) {
        return new TopicExchange(n, true, false);
    }

    @Bean
    public DirectExchange deadLetterExchange(@Value("${app.rabbitmq.exchanges.dlx}") String n) {
        return new DirectExchange(n, true, false);
    }

    @Bean
    public MessageConverter messageConverter() {
        return new JacksonJsonMessageConverter("cl.duoc.pedidos360.events");
    }
}
'@

Write-Src 'ms-notificaciones/src/main/java/cl/duoc/pedidos360/ms_notificaciones/config/TicketsRabbitConfig.java' @'
package cl.duoc.pedidos360.ms_notificaciones.config;

import org.springframework.amqp.core.*;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TicketsRabbitConfig {

    @Value("${app.rabbitmq.exchanges.dlx}") private String dlx;
    @Value("${app.rabbitmq.routing-keys.tickets-dead}") private String deadKey;

    @Bean
    public Queue ticketsQueue(@Value("${app.rabbitmq.queues.tickets}") String name) {
        return QueueBuilder.durable(name).quorum()
                .deadLetterExchange(dlx).deadLetterRoutingKey(deadKey).build();
    }

    @Bean
    public Queue ticketsDlq(@Value("${app.rabbitmq.queues.tickets-dlq}") String name) {
        return QueueBuilder.durable(name).quorum().build();
    }

    @Bean
    public Binding ticketsBinding(@Qualifier("ticketsQueue") Queue q, TopicExchange pedidosExchange,
            @Value("${app.rabbitmq.routing-keys.pedido-creado}") String key) {
        return BindingBuilder.bind(q).to(pedidosExchange).with(key);
    }

    @Bean
    public Binding ticketsDlqBinding(@Qualifier("ticketsDlq") Queue q, DirectExchange deadLetterExchange) {
        return BindingBuilder.bind(q).to(deadLetterExchange).with(deadKey);
    }
}
'@

Write-Src 'ms-notificaciones/src/main/java/cl/duoc/pedidos360/ms_notificaciones/messaging/dlq/DeadLetterConsumer.java' @'
package cl.duoc.pedidos360.ms_notificaciones.messaging.dlq;

import com.rabbitmq.client.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.io.IOException;

// Apagado por defecto (app.rabbitmq.dlq-listener-enabled=false) para poder ver
// los mensajes acumulados en el dashboard. Si se activa, los registra en log y los vacia.
@Component
@ConditionalOnProperty(name = "app.rabbitmq.dlq-listener-enabled", havingValue = "true")
public class DeadLetterConsumer {

    private static final Logger log = LoggerFactory.getLogger(DeadLetterConsumer.class);

    @RabbitListener(queues = {"${app.rabbitmq.queues.notificaciones-dlq}", "${app.rabbitmq.queues.tickets-dlq}"})
    public void onDead(Message message, Channel channel,
            @Header(AmqpHeaders.DELIVERY_TAG) long tag) throws IOException {
        log.error("[DLQ] mensaje no entregado. x-death={} body={}",
                message.getMessageProperties().getHeaders().get("x-death"),
                new String(message.getBody()));
        channel.basicAck(tag, false);
    }
}
'@

Write-Src 'ms-notificaciones/src/main/java/cl/duoc/pedidos360/ms_notificaciones/messaging/notificacion/PedidoNotificacionConsumer.java' @'
package cl.duoc.pedidos360.ms_notificaciones.messaging.notificacion;

import cl.duoc.pedidos360.events.PedidoEvent;
import cl.duoc.pedidos360.ms_notificaciones.entity.Notificacion;
import cl.duoc.pedidos360.ms_notificaciones.service.NotificacionService;
import com.rabbitmq.client.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class PedidoNotificacionConsumer {

    private static final Logger log = LoggerFactory.getLogger(PedidoNotificacionConsumer.class);
    private final NotificacionService service;

    public PedidoNotificacionConsumer(NotificacionService service) {
        this.service = service;
    }

    @RabbitListener(queues = "${app.rabbitmq.queues.notificaciones}")
    public void onPedidoEvent(PedidoEvent ev, Channel channel,
            @Header(AmqpHeaders.DELIVERY_TAG) long tag,
            @Header(AmqpHeaders.RECEIVED_ROUTING_KEY) String routingKey,
            @Header(AmqpHeaders.REDELIVERED) boolean redelivered) throws IOException {
        try {
            if (ev.clienteEmail() == null || ev.clienteEmail().isBlank()) {
                throw new IllegalArgumentException("clienteEmail vacio");
            }
            Notificacion n = new Notificacion();
            n.setDestinatarioEmail(ev.clienteEmail());
            n.setTipo(routingKey.toUpperCase().replace('.', '_'));
            n.setAsunto("Pedido #" + ev.pedidoId() + " " + ev.estado());
            n.setMensaje("Tu pedido #" + ev.pedidoId() + " por $" + ev.total() + " esta " + ev.estado());
            n.setPedidoId(ev.pedidoId());
            service.enviar(n);
            channel.basicAck(tag, false);
            log.info("ACK notificacion pedidoId={} key={}", ev.pedidoId(), routingKey);
        } catch (IllegalArgumentException e) {
            // Error NO recuperable: directo a la DLQ (via DLX)
            log.error("NACK->DLQ (no recuperable) pedidoId={}: {}", ev.pedidoId(), e.getMessage());
            channel.basicNack(tag, false, false);
        } catch (Exception e) {
            // Error recuperable: un reintento y luego DLQ
            if (!redelivered) {
                log.warn("NACK requeue (reintento) pedidoId={}: {}", ev.pedidoId(), e.getMessage());
                channel.basicNack(tag, false, true);
            } else {
                log.error("NACK->DLQ (reintento agotado) pedidoId={}: {}", ev.pedidoId(), e.getMessage());
                channel.basicNack(tag, false, false);
            }
        }
    }
}
'@

Write-Src 'ms-notificaciones/src/main/java/cl/duoc/pedidos360/ms_notificaciones/messaging/ticket/TicketConsumer.java' @'
package cl.duoc.pedidos360.ms_notificaciones.messaging.ticket;

import cl.duoc.pedidos360.events.PedidoEvent;
import cl.duoc.pedidos360.ms_notificaciones.entity.Notificacion;
import cl.duoc.pedidos360.ms_notificaciones.service.NotificacionService;
import com.rabbitmq.client.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class TicketConsumer {

    private static final Logger log = LoggerFactory.getLogger(TicketConsumer.class);
    private final NotificacionService service;

    public TicketConsumer(NotificacionService service) {
        this.service = service;
    }

    @RabbitListener(queues = "${app.rabbitmq.queues.tickets}")
    public void onPedidoCreado(PedidoEvent ev, Channel channel,
            @Header(AmqpHeaders.DELIVERY_TAG) long tag,
            @Header(AmqpHeaders.REDELIVERED) boolean redelivered) throws IOException {
        try {
            if (ev.pedidoId() == null) {
                throw new IllegalArgumentException("pedidoId nulo");
            }
            String codigo = "TCK-" + ev.pedidoId();
            Notificacion t = new Notificacion();
            t.setDestinatarioEmail(ev.clienteEmail() == null ? "sin-email" : ev.clienteEmail());
            t.setTipo("TICKET_GENERADO");
            t.setAsunto("Ticket " + codigo);
            t.setMensaje("Ticket " + codigo + " generado por $" + ev.total());
            t.setPedidoId(ev.pedidoId());
            service.enviar(t);
            channel.basicAck(tag, false);
            log.info("ACK ticket {}", codigo);
        } catch (IllegalArgumentException e) {
            log.error("NACK->DLQ ticket (no recuperable): {}", e.getMessage());
            channel.basicNack(tag, false, false);
        } catch (Exception e) {
            boolean requeue = !redelivered;
            log.warn("NACK ticket requeue={} : {}", requeue, e.getMessage());
            channel.basicNack(tag, false, requeue);
        }
    }
}
'@

Write-Src 'ms-notificaciones/src/main/resources/application.yml' @'
spring:
  rabbitmq:
    addresses: ${RABBITMQ_ADDRESSES:__RABBIT_HOST__:5672}
    username: ${RABBITMQ_USER:guest}
    password: ${RABBITMQ_PASS:guest}
    listener:
      simple:
        acknowledge-mode: manual
        default-requeue-rejected: false
        prefetch: 10

app:
  rabbitmq:
    exchanges:
      pedidos: pedidos.exchange
      dlx: pedidos.dlx
    queues:
      notificaciones: notificaciones.pedido.queue
      notificaciones-dlq: notificaciones.pedido.dlq
      tickets: tickets.pedido.queue
      tickets-dlq: tickets.pedido.dlq
    routing-keys:
      pedido-creado: pedido.creado
      pedido-cancelado: pedido.cancelado
      pedido-todos: pedido.*
      notificaciones-dead: notificaciones.dead
      tickets-dead: tickets.dead
    dlq-listener-enabled: false
'@

Write-Src 'ms-notificaciones/src/test/java/cl/duoc/pedidos360/ms_notificaciones/messaging/notificacion/PedidoNotificacionConsumerTest.java' @'
package cl.duoc.pedidos360.ms_notificaciones.messaging.notificacion;

import cl.duoc.pedidos360.events.PedidoEvent;
import cl.duoc.pedidos360.ms_notificaciones.service.NotificacionService;
import com.rabbitmq.client.Channel;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.*;

class PedidoNotificacionConsumerTest {

    private final NotificacionService service = mock(NotificacionService.class);
    private final Channel channel = mock(Channel.class);
    private final PedidoNotificacionConsumer consumer = new PedidoNotificacionConsumer(service);

    @Test
    void eventoValido_hace_ack() throws Exception {
        consumer.onPedidoEvent(new PedidoEvent(1L, "a@b.cl", 100.0, "CREADO", "x"),
                channel, 1L, "pedido.creado", false);
        verify(channel).basicAck(1L, false);
    }

    @Test
    void emailVacio_hace_nack_sin_requeue() throws Exception {
        consumer.onPedidoEvent(new PedidoEvent(1L, "", 100.0, "CREADO", "x"),
                channel, 2L, "pedido.creado", false);
        verify(channel).basicNack(2L, false, false);
    }
}
'@

Write-Src 'ms-pedidos/src/main/java/cl/duoc/pedidos360/events/PedidoEvent.java' @'
package cl.duoc.pedidos360.events;

public record PedidoEvent(Long pedidoId, String clienteEmail, Double total,
                          String estado, String ocurridoEn) {}
'@

Write-Src 'ms-pedidos/src/main/java/cl/duoc/pedidos360/mspedidos/config/RabbitMQConfig.java' @'
package cl.duoc.pedidos360.mspedidos.config;

import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    @Bean
    public TopicExchange pedidosExchange(@Value("${app.rabbitmq.exchanges.pedidos}") String name) {
        return new TopicExchange(name, true, false);
    }

    @Bean
    public MessageConverter messageConverter() {
        return new JacksonJsonMessageConverter("cl.duoc.pedidos360.events");
    }
}
'@

Write-Src 'ms-pedidos/src/main/java/cl/duoc/pedidos360/mspedidos/controller/PedidoController.java' @'
package cl.duoc.pedidos360.mspedidos.controller;

import cl.duoc.pedidos360.mspedidos.entity.Pedido;
import cl.duoc.pedidos360.mspedidos.messaging.PedidoEventPublisher;
import cl.duoc.pedidos360.mspedidos.repository.PedidoRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/pedidos")
@CrossOrigin(origins = "*", allowedHeaders = "*", methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.OPTIONS})
public class PedidoController {

    private final PedidoRepository pedidoRepository;
    private final PedidoEventPublisher publisher;

    public PedidoController(PedidoRepository pedidoRepository, PedidoEventPublisher publisher) {
        this.pedidoRepository = pedidoRepository;
        this.publisher = publisher;
    }

    @GetMapping
    public ResponseEntity<List<Pedido>> listar() {
        return ResponseEntity.ok(pedidoRepository.findAll());
    }

    @PostMapping
    public ResponseEntity<Pedido> crear(@RequestBody Pedido pedido) {
        Pedido guardado = pedidoRepository.save(pedido);
        publisher.publicarCreado(guardado);
        return ResponseEntity.status(HttpStatus.CREATED).body(guardado);
    }
}
'@

Write-Src 'ms-pedidos/src/main/java/cl/duoc/pedidos360/mspedidos/messaging/PedidoEventPublisher.java' @'
package cl.duoc.pedidos360.mspedidos.messaging;

import cl.duoc.pedidos360.events.PedidoEvent;
import cl.duoc.pedidos360.mspedidos.entity.Pedido;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class PedidoEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(PedidoEventPublisher.class);

    private final RabbitTemplate rabbitTemplate;
    private final String exchange;
    private final String keyCreado;
    private final String keyCancelado;

    public PedidoEventPublisher(RabbitTemplate rabbitTemplate,
            @Value("${app.rabbitmq.exchanges.pedidos}") String exchange,
            @Value("${app.rabbitmq.routing-keys.pedido-creado}") String keyCreado,
            @Value("${app.rabbitmq.routing-keys.pedido-cancelado}") String keyCancelado) {
        this.rabbitTemplate = rabbitTemplate;
        this.exchange = exchange;
        this.keyCreado = keyCreado;
        this.keyCancelado = keyCancelado;
    }

    public void publicarCreado(Pedido p) { publicar(keyCreado, p); }

    public void publicarCancelado(Pedido p) { publicar(keyCancelado, p); }

    // Un fallo de RabbitMQ NO debe romper la creacion del pedido.
    private void publicar(String routingKey, Pedido p) {
        try {
            PedidoEvent ev = new PedidoEvent(p.getId(), p.getClienteEmail(), p.getTotal(),
                    p.getEstado(), LocalDateTime.now().toString());
            rabbitTemplate.convertAndSend(exchange, routingKey, ev);
            log.info("Evento publicado exchange={} key={} pedidoId={}", exchange, routingKey, p.getId());
        } catch (AmqpException e) {
            log.error("No se pudo publicar evento key={} pedidoId={}: {}", routingKey, p.getId(), e.getMessage());
        }
    }
}
'@

Write-Src 'ms-pedidos/src/main/resources/application.yml' @'
spring:
  rabbitmq:
    addresses: ${RABBITMQ_ADDRESSES:__RABBIT_HOST__:5672}
    username: ${RABBITMQ_USER:guest}
    password: ${RABBITMQ_PASS:guest}

app:
  rabbitmq:
    exchanges:
      pedidos: pedidos.exchange
    routing-keys:
      pedido-creado: pedido.creado
      pedido-cancelado: pedido.cancelado
'@

Write-Src 'ms-rabbit-admin/pom.xml' @'
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
	<artifactId>ms-rabbit-admin</artifactId>
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
			<groupId>org.springframework.boot</groupId>
			<artifactId>spring-boot-starter-test</artifactId>
			<scope>test</scope>
		</dependency>
		<dependency>
			<groupId>org.springframework.security</groupId>
			<artifactId>spring-security-test</artifactId>
			<scope>test</scope>
		</dependency>
		<dependency>
			<groupId>org.springframework.boot</groupId>
			<artifactId>spring-boot-starter-amqp</artifactId>
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

Write-Src 'ms-rabbit-admin/src/main/java/cl/duoc/pedidos360/msrabbitadmin/MsRabbitAdminApplication.java' @'
package cl.duoc.pedidos360.msrabbitadmin;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class MsRabbitAdminApplication {
    public static void main(String[] args) {
        SpringApplication.run(MsRabbitAdminApplication.class, args);
    }
}
'@

Write-Src 'ms-rabbit-admin/src/main/java/cl/duoc/pedidos360/msrabbitadmin/config/CorsConfig.java' @'
package cl.duoc.pedidos360.msrabbitadmin.config;

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

Write-Src 'ms-rabbit-admin/src/main/java/cl/duoc/pedidos360/msrabbitadmin/config/SecurityConfig.java' @'
package cl.duoc.pedidos360.msrabbitadmin.config;

import cl.duoc.pedidos360.msrabbitadmin.security.AudienceValidator;
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

Write-Src 'ms-rabbit-admin/src/main/java/cl/duoc/pedidos360/msrabbitadmin/controller/RabbitAdminController.java' @'
package cl.duoc.pedidos360.msrabbitadmin.controller;

import cl.duoc.pedidos360.msrabbitadmin.dto.*;
import cl.duoc.pedidos360.msrabbitadmin.service.RabbitAdminService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class RabbitAdminController {

    private final RabbitAdminService service;

    public RabbitAdminController(RabbitAdminService service) {
        this.service = service;
    }

    @PostMapping("/queues")
    public ResponseEntity<Map<String, String>> crearCola(@Valid @RequestBody QueueRequest r) {
        service.crearCola(r);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("message", "Cola creada", "name", r.name()));
    }

    @GetMapping("/queues")
    public List<Map<String, Object>> listarColas() {
        return service.listarColas();
    }

    @DeleteMapping("/queues/{name}")
    public ResponseEntity<Void> eliminarCola(@PathVariable String name) {
        service.eliminarCola(name);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/exchanges")
    public ResponseEntity<Map<String, String>> crearExchange(@Valid @RequestBody ExchangeRequest r) {
        service.crearExchange(r);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("message", "Exchange creado", "name", r.name()));
    }

    @DeleteMapping("/exchanges/{name}")
    public ResponseEntity<Void> eliminarExchange(@PathVariable String name) {
        service.eliminarExchange(name);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/bindings")
    public ResponseEntity<Map<String, String>> crearBinding(@Valid @RequestBody BindingRequest r) {
        service.crearBinding(r);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("message", "Binding creado"));
    }

    @DeleteMapping("/bindings")
    public ResponseEntity<Void> eliminarBinding(@Valid @RequestBody BindingRequest r) {
        service.eliminarBinding(r);
        return ResponseEntity.noContent().build();
    }
}
'@

Write-Src 'ms-rabbit-admin/src/main/java/cl/duoc/pedidos360/msrabbitadmin/dto/BindingRequest.java' @'
package cl.duoc.pedidos360.msrabbitadmin.dto;

import jakarta.validation.constraints.*;

public record BindingRequest(
    @NotBlank(message = "exchange es obligatorio") String exchange,
    @NotBlank(message = "queue es obligatorio") String queue,
    @NotNull(message = "routingKey es obligatorio (puede ser vacio solo en fanout)") String routingKey) {}
'@

Write-Src 'ms-rabbit-admin/src/main/java/cl/duoc/pedidos360/msrabbitadmin/dto/ExchangeRequest.java' @'
package cl.duoc.pedidos360.msrabbitadmin.dto;

import jakarta.validation.constraints.*;

public record ExchangeRequest(
    @NotBlank(message = "El nombre del exchange es obligatorio")
    @Size(max = 255, message = "Maximo 255 caracteres")
    @Pattern(regexp = "^[A-Za-z0-9._-]+$", message = "Solo letras, numeros, '.', '_' y '-'")
    String name,
    @NotBlank(message = "type es obligatorio")
    @Pattern(regexp = "direct|topic|fanout|headers", message = "type debe ser direct, topic, fanout o headers")
    String type,
    Boolean durable) {}
'@

Write-Src 'ms-rabbit-admin/src/main/java/cl/duoc/pedidos360/msrabbitadmin/dto/QueueRequest.java' @'
package cl.duoc.pedidos360.msrabbitadmin.dto;

import jakarta.validation.constraints.*;

public record QueueRequest(
    @NotBlank(message = "El nombre de la cola es obligatorio")
    @Size(max = 255, message = "Maximo 255 caracteres")
    @Pattern(regexp = "^[A-Za-z0-9._-]+$", message = "Solo letras, numeros, '.', '_' y '-'")
    String name,
    Boolean durable,
    @Pattern(regexp = "classic|quorum", message = "type debe ser classic o quorum")
    String type,
    String deadLetterExchange,
    String deadLetterRoutingKey,
    @Min(value = 1, message = "messageTtlMs debe ser >= 1") Integer messageTtlMs) {}
'@

Write-Src 'ms-rabbit-admin/src/main/java/cl/duoc/pedidos360/msrabbitadmin/exception/GlobalExceptionHandler.java' @'
package cl.duoc.pedidos360.msrabbitadmin.exception;

import org.springframework.amqp.AmqpException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.RestClientException;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.NoSuchElementException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> validacion(MethodArgumentNotValidException e) {
        Map<String, String> campos = new LinkedHashMap<>();
        e.getBindingResult().getFieldErrors().forEach(f -> campos.put(f.getField(), f.getDefaultMessage()));
        return ResponseEntity.badRequest().body(Map.of("error", "Validacion fallida", "campos", campos));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> invalido(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<Map<String, String>> noExiste(NoSuchElementException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
    }

    @ExceptionHandler({AmqpException.class, RestClientException.class})
    public ResponseEntity<Map<String, String>> rabbit(Exception e) {
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(Map.of("error", "Error comunicando con RabbitMQ: " + e.getMessage()));
    }
}
'@

Write-Src 'ms-rabbit-admin/src/main/java/cl/duoc/pedidos360/msrabbitadmin/security/AudienceValidator.java' @'
package cl.duoc.pedidos360.msrabbitadmin.security;

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

Write-Src 'ms-rabbit-admin/src/main/java/cl/duoc/pedidos360/msrabbitadmin/service/RabbitAdminService.java' @'
package cl.duoc.pedidos360.msrabbitadmin.service;

import cl.duoc.pedidos360.msrabbitadmin.dto.*;
import org.springframework.amqp.core.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.*;

@Service
public class RabbitAdminService {

    private final AmqpAdmin amqpAdmin;
    private final RestClient management;

    public RabbitAdminService(AmqpAdmin amqpAdmin,
            @Value("${app.rabbitmq.management-url}") String managementUrl,
            @Value("${spring.rabbitmq.username}") String user,
            @Value("${spring.rabbitmq.password}") String pass) {
        this.amqpAdmin = amqpAdmin;
        this.management = RestClient.builder().baseUrl(managementUrl)
                .defaultHeaders(h -> h.setBasicAuth(user, pass)).build();
    }

    public void crearCola(QueueRequest r) {
        validarNombre(r.name());
        boolean durable = r.durable() == null || r.durable();
        String type = r.type() == null ? "quorum" : r.type().toLowerCase();
        if (type.equals("quorum") && !durable) {
            throw new IllegalArgumentException("Las colas quorum deben ser durables");
        }
        boolean hasDlx = r.deadLetterExchange() != null && !r.deadLetterExchange().isBlank();
        boolean hasDlk = r.deadLetterRoutingKey() != null && !r.deadLetterRoutingKey().isBlank();
        if (hasDlk && !hasDlx) {
            throw new IllegalArgumentException("deadLetterRoutingKey requiere deadLetterExchange");
        }
        QueueBuilder b = durable ? QueueBuilder.durable(r.name()) : QueueBuilder.nonDurable(r.name());
        b.withArgument("x-queue-type", type);
        if (hasDlx) b.deadLetterExchange(r.deadLetterExchange());
        if (hasDlk) b.deadLetterRoutingKey(r.deadLetterRoutingKey());
        if (r.messageTtlMs() != null) b.ttl(r.messageTtlMs());
        amqpAdmin.declareQueue(b.build());
    }

    public void eliminarCola(String name) {
        validarNombre(name);
        if (amqpAdmin.getQueueInfo(name) == null) {
            throw new NoSuchElementException("La cola '" + name + "' no existe");
        }
        amqpAdmin.deleteQueue(name);
    }

    public void crearExchange(ExchangeRequest r) {
        validarNombre(r.name());
        boolean durable = r.durable() == null || r.durable();
        Exchange ex = switch (r.type().toLowerCase()) {
            case "direct" -> new DirectExchange(r.name(), durable, false);
            case "topic" -> new TopicExchange(r.name(), durable, false);
            case "fanout" -> new FanoutExchange(r.name(), durable, false);
            case "headers" -> new HeadersExchange(r.name(), durable, false);
            default -> throw new IllegalArgumentException("Tipo de exchange invalido");
        };
        amqpAdmin.declareExchange(ex);
    }

    public void eliminarExchange(String name) {
        validarNombre(name);
        amqpAdmin.deleteExchange(name);
    }

    public void crearBinding(BindingRequest r) {
        amqpAdmin.declareBinding(toBinding(r));
    }

    public void eliminarBinding(BindingRequest r) {
        amqpAdmin.removeBinding(toBinding(r));
    }

    public List<Map<String, Object>> listarColas() {
        List<Map<String, Object>> raw = management.get().uri("/api/queues")
                .retrieve().body(new ParameterizedTypeReference<>() {});
        List<Map<String, Object>> out = new ArrayList<>();
        if (raw != null) {
            for (Map<String, Object> q : raw) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("name", q.get("name"));
                m.put("messages", q.getOrDefault("messages", 0));
                m.put("consumers", q.getOrDefault("consumers", 0));
                m.put("state", q.get("state"));
                out.add(m);
            }
        }
        return out;
    }

    private Binding toBinding(BindingRequest r) {
        return new Binding(r.queue(), Binding.DestinationType.QUEUE, r.exchange(), r.routingKey(), null);
    }

    private void validarNombre(String name) {
        if (name.startsWith("amq.")) {
            throw new IllegalArgumentException("Los nombres que empiezan con 'amq.' estan reservados");
        }
    }
}
'@

Write-Src 'ms-rabbit-admin/src/main/resources/application.properties' @'
spring.application.name=ms-rabbit-admin
server.port=8085

azure.ad.jwk-set-uri=https://login.microsoftonline.com/abf8edad-bd14-425d-9255-2e0e7e57dfa2/discovery/v2.0/keys
azure.ad.issuer=https://sts.windows.net/abf8edad-bd14-425d-9255-2e0e7e57dfa2/
azure.ad.audience=api://9078dcc3-9503-4237-a463-d5a4a96cb61f
spring.security.oauth2.resourceserver.jwt.jwk-set-uri=https://login.microsoftonline.com/abf8edad-bd14-425d-9255-2e0e7e57dfa2/discovery/v2.0/keys
'@

Write-Src 'ms-rabbit-admin/src/main/resources/application.yml' @'
spring:
  rabbitmq:
    addresses: ${RABBITMQ_ADDRESSES:__RABBIT_HOST__:5672}
    username: ${RABBITMQ_USER:guest}
    password: ${RABBITMQ_PASS:guest}

app:
  rabbitmq:
    management-url: http://${RABBITMQ_MGMT_HOST:__RABBIT_HOST__}:15672
'@

Write-Src 'ms-rabbit-admin/src/test/java/cl/duoc/pedidos360/msrabbitadmin/service/RabbitAdminServiceTest.java' @'
package cl.duoc.pedidos360.msrabbitadmin.service;

import cl.duoc.pedidos360.msrabbitadmin.dto.QueueRequest;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.Queue;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class RabbitAdminServiceTest {

    private final AmqpAdmin admin = mock(AmqpAdmin.class);
    private final RabbitAdminService service =
            new RabbitAdminService(admin, "http://localhost:15672", "u", "p");

    @Test
    void crearCola_declara_la_cola() {
        service.crearCola(new QueueRequest("demo.queue", true, "quorum", null, null, null));
        verify(admin).declareQueue(any(Queue.class));
    }

    @Test
    void nombre_reservado_amq_es_rechazado() {
        assertThrows(IllegalArgumentException.class,
                () -> service.crearCola(new QueueRequest("amq.algo", true, "quorum", null, null, null)));
    }
}
'@


# --------- 3. Fin ---------
Write-Host "`n[3/3] Listo." -ForegroundColor Green
Write-Host @"

Siguientes pasos (en PowerShell, en la misma terminal donde levantes cada MS):

  `$env:RABBITMQ_USER = 'pedidos360'
  `$env:RABBITMQ_PASS = 'LA_PASSWORD_QUE_PUSISTE_EN_EC2'

  cd backend\ms-notificaciones ; .\mvnw spring-boot:run     # 1ro: crea colas, exchanges y bindings
  cd backend\ms-pedidos        ; .\mvnw spring-boot:run     # 2do: productor
  cd backend\ms-rabbit-admin   ; .\mvnw spring-boot:run     # admin (puerto 8085)

Si quieres ver los 3 nodos del cluster, usa:
  `$env:RABBITMQ_ADDRESSES = '$RabbitHost`:5672,$RabbitHost`:5673,$RabbitHost`:5674'

Compilar y probar:  .\mvnw -q test   (en cada microservicio)
"@
