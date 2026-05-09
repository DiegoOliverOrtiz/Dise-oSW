package edu.esi.ds.esientradas.http;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.util.ReflectionTestUtils;

import edu.esi.ds.esientradas.dto.DtoReservaRequest;
import edu.esi.ds.esientradas.dto.DtoReservaResponse;
import edu.esi.ds.esientradas.services.ColaVirtualService;
import edu.esi.ds.esientradas.services.ReservasService;

@ExtendWith(MockitoExtension.class)
class ReservasControllerTest {

    @Mock
    private ReservasService reservasService;

    @Mock
    private ColaVirtualService colaVirtualService;

    private ReservasController controller;

    @BeforeEach
    void setUp() {
        controller = new ReservasController();
        ReflectionTestUtils.setField(controller, "service", reservasService);
        ReflectionTestUtils.setField(controller, "colaVirtualService", colaVirtualService);
    }

    @Test
    void reservarDevuelveElResumenActualEnLugarDelTotalAcumuladoEnSesion() {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("precioTotal", 15_800L);

        when(reservasService.getResumen(session.getId()))
            .thenReturn(new DtoReservaResponse(9_450L, 3));

        Long precioTotal = controller.reservar(session, 42L);

        verify(reservasService).reservar(42L, session.getId());
        verify(reservasService).getResumen(session.getId());
        assertEquals(9_450L, precioTotal);
    }

    @Test
    void reservarLoteDevuelveElResumenActualDeLasReservasReales() {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("precioTotal", 15_800L);

        DtoReservaRequest request = new DtoReservaRequest();
        request.setEntradaIds(List.of(11L, 12L, 13L));

        DtoReservaResponse resumenActual = new DtoReservaResponse(9_450L, 3);
        when(reservasService.getResumen(session.getId())).thenReturn(resumenActual);

        DtoReservaResponse response = controller.reservarLote(session, request, null, null);

        verify(colaVirtualService).assertAccessForEntradas(eq(List.of(11L, 12L, 13L)), anyString(), eq(null));
        verify(reservasService).reservarEntradas(List.of(11L, 12L, 13L), session.getId());
        verify(reservasService).getResumen(session.getId());
        assertEquals(9_450L, response.getPrecioTotal());
        assertEquals(3, response.getNumeroEntradas());
    }
}
