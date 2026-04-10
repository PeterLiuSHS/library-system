package com.kexun.loan.dto;

public class AvailabilityResponse {

    private boolean available;

    private long remainingDays;

    public AvailabilityResponse(){
    }

    public boolean isAvailable(){
        return available;
    }

    public void setAvailable(boolean available){
        this.available = available;
    }

    public long getRemainingDays(){
        return remainingDays;
    }

    public void setRemainingDays(long remainingDays){
        this.remainingDays = remainingDays;
    }
}
