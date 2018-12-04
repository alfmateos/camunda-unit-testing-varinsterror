package be.camunda.bpm.domain;

import java.io.Serializable;

public class ProductDto implements Serializable {
    private static final long serialVersionUID = 1L;

    private boolean requiresPlanning;

    public ProductDto() {
        super();
    }

    public ProductDto(boolean requiresPlanning) {
        this.requiresPlanning = requiresPlanning;
    }

    public boolean isRequiresPlanning() {
        return requiresPlanning;
    }

}
