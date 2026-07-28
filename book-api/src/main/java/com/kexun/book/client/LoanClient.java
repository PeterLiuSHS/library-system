package com.kexun.book.client;

import com.kexun.book.dto.AvailabilityResponse;
import com.kexun.book.exception.DownstreamServiceException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Component
public class LoanClient {
    private final RestTemplate restTemplate;
    private final String loanServiceBaseUrl;

    public LoanClient(
            RestTemplate restTemplate,
            @Value("${loan.service.base-url:http://localhost:8082}") String loanServiceBaseUrl
    ) {
        this.restTemplate = restTemplate;
        this.loanServiceBaseUrl = loanServiceBaseUrl;
    }

    public boolean hasActiveLoan(Long bookId) {
        try{
            AvailabilityResponse response = restTemplate.getForObject(
                    loanServiceBaseUrl + "/books/" + bookId + "/availability",
                    AvailabilityResponse.class
            );

            return response != null && !response.isAvailable();
        } catch (RestClientException ex) {
            throw new DownstreamServiceException(
                    "Loan service is unavailable",
                    ex
            );
        }
    }
}
