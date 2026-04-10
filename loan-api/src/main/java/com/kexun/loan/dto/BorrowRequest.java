package com.kexun.loan.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public class BorrowRequest {
    @NotNull
    private Long bookId;

    @NotNull
    @Min(1)
    private Integer days;

    public BorrowRequest() {
    }

    public Long getBookId() {
        return bookId;
    }

    public void setBookId(Long bookId) {
        this.bookId = bookId;
    }

    public Integer getDays() {
        return days;
    }

    public void setDays(Integer days) {
        this.days = days;
    }
}
