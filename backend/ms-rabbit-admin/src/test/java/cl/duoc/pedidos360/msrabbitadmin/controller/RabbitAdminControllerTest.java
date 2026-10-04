package cl.duoc.pedidos360.msrabbitadmin.controller;

import cl.duoc.pedidos360.msrabbitadmin.exception.GlobalExceptionHandler;
import cl.duoc.pedidos360.msrabbitadmin.service.RabbitAdminService;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.NoSuchElementException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class RabbitAdminControllerTest {

    private final RabbitAdminService service = mock(RabbitAdminService.class);
    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(new RabbitAdminController(service))
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();

    @Test
    void postQueues_valido_responde_201() throws Exception {
        mvc.perform(post("/api/queues").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"demo.queue\",\"type\":\"quorum\"}"))
                .andExpect(status().isCreated());
        verify(service).crearCola(any());
    }

    @Test
    void postQueues_nombre_vacio_responde_400_y_no_llama_al_servicio() throws Exception {
        mvc.perform(post("/api/queues").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\"}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test
    void postQueues_tipo_invalido_responde_400() throws Exception {
        mvc.perform(post("/api/queues").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"demo\",\"type\":\"raro\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void postExchanges_tipo_invalido_responde_400() throws Exception {
        mvc.perform(post("/api/exchanges").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"demo\",\"type\":\"raro\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void postBindings_sin_cola_responde_400() throws Exception {
        mvc.perform(post("/api/bindings").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"exchange\":\"demo\",\"routingKey\":\"k\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deleteQueues_existente_responde_204() throws Exception {
        mvc.perform(delete("/api/queues/demo.queue")).andExpect(status().isNoContent());
        verify(service).eliminarCola("demo.queue");
    }

    @Test
    void deleteQueues_inexistente_responde_404() throws Exception {
        doThrow(new NoSuchElementException("no existe")).when(service).eliminarCola("nope");
        mvc.perform(delete("/api/queues/nope")).andExpect(status().isNotFound());
    }

    @Test
    void getExchanges_y_getBindings_responden_200() throws Exception {
        mvc.perform(get("/api/exchanges")).andExpect(status().isOk());
        mvc.perform(get("/api/bindings")).andExpect(status().isOk());
    }
}
