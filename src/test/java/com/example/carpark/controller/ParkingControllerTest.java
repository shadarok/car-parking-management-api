package com.example.carpark.controller;

import com.example.carpark.dto.ParkedVehicleResponse;
import com.example.carpark.dto.ParkingStatusResponse;
import com.example.carpark.exception.VehicleNotFoundException;
import com.example.carpark.service.ParkingService;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ParkingController.class)
class ParkingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ParkingService parkingService;

    @Nested
    class ParkingStatus {

        @Test
        void status_returnsOkWithCounts() throws Exception {
            given(parkingService.status()).willReturn(new ParkingStatusResponse(20, 30));

            mockMvc.perform(get("/parking"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.availableSpaces").value(20))
                    .andExpect(jsonPath("$.occupiedSpaces").value(30));
        }

    }

    @Nested
    class ParkVehicle {

        @Test
        void parkVehicle_validRequest_returnsCreated() throws Exception {
            final var fixedDateTime = LocalDateTime.of(2026, 1, 1, 10, 0);
            given(parkingService.park(anyString(), anyInt()))
                    .willReturn(new ParkedVehicleResponse("AB12CDE", 1, fixedDateTime));
            final var validRequestBody = """
                {
                    "vehicleReg": "AB12CDE",
                    "vehicleType": 1
                }
                """;

            mockMvc.perform(post("/parking")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(validRequestBody))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.vehicleReg").value("AB12CDE"))
                    .andExpect(jsonPath("$.spaceNumber").value(1))
                    .andExpect(jsonPath("$.timeIn").value(fixedDateTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)));
        }

        @Test
        void parkVehicle_invalidVehicleType_returnsBadRequest() throws Exception {
            final var invalidRequestBody = """
                    {
                        "vehicleReg": "AB12CDE",
                        "vehicleType": 0
                    }
                    """;
            mockMvc.perform(post("/parking")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(invalidRequestBody))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void parkVehicle_blankRegistrationNumber_returnsBadRequest() throws Exception {
            final var invalidRequestBody = """
                    {
                        "vehicleReg": "",
                        "vehicleType": 1
                    }
                    """;
            mockMvc.perform(post("/parking")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(invalidRequestBody))
                    .andExpect(status().isBadRequest());
        }

    }

    @Nested
    class ParkingBill {

        @Test
        void bill_unknownVehicle_returnsNotFound() throws Exception {
            when(parkingService.exitAndBill(anyString()))
                    .thenThrow(new VehicleNotFoundException("UNKNOWN"));

            mockMvc.perform(post("/parking/bill")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"vehicleReg\": \"UNKNOWN\"}"))
                    .andExpect(status().isNotFound());
        }
    }

}
