package cl.duoc.pedidos360.mskafkaadmin.controller;

import cl.duoc.pedidos360.mskafkaadmin.exception.GlobalExceptionHandler;
import cl.duoc.pedidos360.mskafkaadmin.exception.KafkaAdminException;
import cl.duoc.pedidos360.mskafkaadmin.service.KafkaAdminService;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.NoSuchElementException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class KafkaAdminControllerTest {

    private final KafkaAdminService service = mock(KafkaAdminService.class);
    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(new KafkaAdminController(service))
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();

    @Test
    void postTopics_valido_responde_201() throws Exception {
        mvc.perform(post("/api/topics").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"demo.topic\",\"partitions\":2,\"replicationFactor\":1,\"configs\":{\"retention.ms\":\"60000\"}}"))
                .andExpect(status().isCreated());
        verify(service).crear(any());
    }

    @Test
    void postTopics_nombre_vacio_responde_400_y_no_llama_al_servicio() throws Exception {
        mvc.perform(post("/api/topics").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\"}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test
    void postTopics_nombre_con_caracteres_invalidos_responde_400() throws Exception {
        mvc.perform(post("/api/topics").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"mi topico!\"}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test
    void postTopics_particiones_cero_responde_400() throws Exception {
        mvc.perform(post("/api/topics").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"demo\",\"partitions\":0}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test
    void postTopics_existente_responde_409() throws Exception {
        doThrow(new IllegalStateException("ya existe")).when(service).crear(any());
        mvc.perform(post("/api/topics").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"demo\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void putTopics_valido_responde_200() throws Exception {
        mvc.perform(put("/api/topics/demo").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"partitions\":4,\"configs\":{\"retention.ms\":\"120000\"}}"))
                .andExpect(status().isOk());
        verify(service).actualizar(eq("demo"), any());
    }

    @Test
    void putConfigs_valido_responde_200() throws Exception {
        mvc.perform(put("/api/topics/demo/configs").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"configs\":{\"retention.ms\":\"120000\"},\"resetConfigs\":[\"cleanup.policy\"]}"))
                .andExpect(status().isOk());
        verify(service).actualizarConfiguraciones(eq("demo"), any());
    }

    @Test
    void deleteTopics_existente_responde_204() throws Exception {
        mvc.perform(delete("/api/topics/demo.topic")).andExpect(status().isNoContent());
        verify(service).eliminar("demo.topic");
    }

    @Test
    void deleteTopics_inexistente_responde_404() throws Exception {
        doThrow(new NoSuchElementException("no existe")).when(service).eliminar("nope");
        mvc.perform(delete("/api/topics/nope")).andExpect(status().isNotFound());
    }

    @Test
    void getTopics_y_getCluster_responden_200() throws Exception {
        mvc.perform(get("/api/topics")).andExpect(status().isOk());
        mvc.perform(get("/api/cluster")).andExpect(status().isOk());
    }

    @Test
    void getCluster_con_kafka_caido_responde_502() throws Exception {
        when(service.cluster()).thenThrow(new KafkaAdminException("Kafka no respondio", new RuntimeException()));
        mvc.perform(get("/api/cluster")).andExpect(status().isBadGateway());
    }
}
