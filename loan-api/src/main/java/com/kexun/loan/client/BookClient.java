package com.kexun.loan.client;

import com.kexun.loan.exception.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

@Component
public class BookClient {

    private final RestClient restClient;
    private final String bookServiceBaseUrl;

    public BookClient(RestClient restClient,
                      @Value("${services.book.base-url}") String bookServiceBaseUrl) {
        this.restClient = restClient;
        this.bookServiceBaseUrl = bookServiceBaseUrl;
    }

    public void assertBookExists(Long bookId) {
        try {
            restClient.get()
                    .uri(bookServiceBaseUrl + "/books/{id}", bookId)
                    .retrieve()
                    .toBodilessEntity();
        } catch (HttpClientErrorException.NotFound ex) {
            throw new ResourceNotFoundException("Book " + bookId + " not found");
        }
    }
}
