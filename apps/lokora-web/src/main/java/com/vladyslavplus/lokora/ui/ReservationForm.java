package com.vladyslavplus.lokora.ui;

import jakarta.validation.constraints.NotBlank;

public class ReservationForm {

    @NotBlank
    private String startTime;

    @NotBlank
    private String endTime;

    public String getStartTime() {
        return startTime;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public String getEndTime() {
        return endTime;
    }

    public void setEndTime(String endTime) {
        this.endTime = endTime;
    }
}
