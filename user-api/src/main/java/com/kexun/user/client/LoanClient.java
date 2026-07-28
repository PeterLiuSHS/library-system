package com.kexun.user.client;

import com.kexun.user.exception.DownstreamServiceException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class LoanClient {

    private final RestClient restClient;
    private final String loanServiceBaseUrl;

    public LoanClient(RestClient restClient,
                      @Value("${services.loan.base-url}") String loanServiceBaseUrl) {
        this.restClient = restClient;
        this.loanServiceBaseUrl = loanServiceBaseUrl;
    }

    public boolean hasActiveLoans(Long userId) {
        try {
            return Boolean.TRUE.equals(
                    restClient.get()
                            .uri(loanServiceBaseUrl + "/users/{userId}/loans/active/exists", userId)
                            .retrieve()
                            .body(Boolean.class)
            );
        } catch (RestClientException ex) {
            throw new DownstreamServiceException(
                    "Loan service is unavailable",
                    ex
            );
        }
    }
}
