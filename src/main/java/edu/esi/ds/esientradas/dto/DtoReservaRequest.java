package edu.esi.ds.esientradas.dto;

import java.util.ArrayList;
import java.util.List;

public class DtoReservaRequest {
    private List<Long> entradaIds = new ArrayList<>();

    public List<Long> getEntradaIds() {
        return entradaIds;
    }

    public void setEntradaIds(List<Long> entradaIds) {
        this.entradaIds = entradaIds;
    }
}
