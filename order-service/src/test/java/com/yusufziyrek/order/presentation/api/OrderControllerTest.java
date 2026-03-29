package com.yusufziyrek.order.presentation.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yusufziyrek.order.application.dto.OrderResult;
import com.yusufziyrek.order.application.usecase.CancelOrderUseCase;
import com.yusufziyrek.order.application.usecase.ChangeOrderStatusUseCase;
import com.yusufziyrek.order.application.usecase.CreateOrderUseCase;
import com.yusufziyrek.order.application.usecase.GetOrderUseCase;
import com.yusufziyrek.order.domain.OrderStatus;
import com.yusufziyrek.order.presentation.error.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class OrderControllerTest {

    private MockMvc mockMvc;
    private CreateOrderUseCase createOrderUseCase;
    private GetOrderUseCase getOrderUseCase;
    private ChangeOrderStatusUseCase changeOrderStatusUseCase;
    private CancelOrderUseCase cancelOrderUseCase;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @BeforeEach
    void setUp() {
        createOrderUseCase = mock(CreateOrderUseCase.class);
        getOrderUseCase = mock(GetOrderUseCase.class);
        changeOrderStatusUseCase = mock(ChangeOrderStatusUseCase.class);
        cancelOrderUseCase = mock(CancelOrderUseCase.class);

        OrderController controller = new OrderController(
                createOrderUseCase,
                getOrderUseCase,
                changeOrderStatusUseCase,
                cancelOrderUseCase,
                new OrderHttpMapper()
        );

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void shouldCreateOrder() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();

        when(createOrderUseCase.execute(any())).thenReturn(result(orderId, userId, OrderStatus.PENDING));

        String request = """
                {
                  "items": [
                    {
                      "productId": "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa",
                      "quantity": 2,
                      "unitPrice": 10.5
                    }
                  ]
                }
                """;

        mockMvc.perform(post("/api/v1/orders")
                        .requestAttr("user_id", userId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(orderId.toString()))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void shouldReturnForbiddenWhenOrderBelongsToAnotherUser() throws Exception {
        UUID requester = UUID.randomUUID();
        UUID owner = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();

        when(getOrderUseCase.execute(eq(orderId))).thenReturn(result(orderId, owner, OrderStatus.PENDING));

        mockMvc.perform(get("/api/v1/orders/{id}", orderId)
                        .requestAttr("user_id", requester.toString()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void shouldChangeStatus() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();

        when(getOrderUseCase.execute(eq(orderId))).thenReturn(result(orderId, userId, OrderStatus.PENDING));
        when(changeOrderStatusUseCase.execute(eq(orderId), anyString()))
                .thenReturn(result(orderId, userId, OrderStatus.CONFIRMED));

        mockMvc.perform(patch("/api/v1/orders/{id}/status", orderId)
                        .requestAttr("user_id", userId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"CONFIRMED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"));
    }

    @Test
    void shouldCancelOrder() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();

        when(getOrderUseCase.execute(eq(orderId))).thenReturn(result(orderId, userId, OrderStatus.PENDING));
        doNothing().when(cancelOrderUseCase).execute(eq(orderId));

        mockMvc.perform(delete("/api/v1/orders/{id}", orderId)
                        .requestAttr("user_id", userId.toString()))
                .andExpect(status().isNoContent());
    }

    @Test
    void shouldValidateCreatePayload() throws Exception {
        UUID userId = UUID.randomUUID();
        String invalidBody = "{\"items\":[]}";

        mockMvc.perform(post("/api/v1/orders")
                        .requestAttr("user_id", userId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    private OrderResult result(UUID orderId, UUID userId, OrderStatus status) {
        return new OrderResult(
                orderId,
                userId,
                status,
                new BigDecimal("10.5"),
                OffsetDateTime.now(),
                OffsetDateTime.now(),
                List.of()
        );
    }
}
