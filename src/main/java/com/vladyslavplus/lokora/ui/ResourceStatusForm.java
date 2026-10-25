package com.vladyslavplus.lokora.ui;

import com.vladyslavplus.lokora.resource.ResourceStatus;
import jakarta.validation.constraints.NotNull;

public class ResourceStatusForm {

    @NotNull
    private ResourceStatus status;

    public ResourceStatus getStatus() {
        return status;
    }

    public void setStatus(ResourceStatus status) {
        this.status = status;
    }
}
