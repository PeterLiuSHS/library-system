package com.kexun.loan.client;

import com.kexun.loan.exception.ResourceNotFoundException;
import com.kexun.loan.exception.DownstreamServiceException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component  // component scan, written by myself
public class UserClient {

    private final RestClient restClient;
    private final String userServiceBaseUrl;

    public UserClient(RestClient restClient,  // this restClient is injected by Spring
                      @Value("${services.user.base-url}") String userServiceBaseUrl) {
        this.restClient = restClient;
        this.userServiceBaseUrl = userServiceBaseUrl;
    }

    public void assertUserExists(Long userId) {
        try {
            restClient.get() // sent GET request
                    .uri(userServiceBaseUrl + "/users/{id}", userId) // {id} is a placeholder, and will be valued as the value of userId
                    .retrieve()  // execute the request and get response
                    .toBodilessEntity(); // retrieve HTTP response without reading or mapping the response body
        } catch (HttpClientErrorException.NotFound ex){  // HttpClientErrorException is defined by Spring framework
            throw new ResourceNotFoundException("User with id " + userId + " not found");
        } catch (RestClientException ex) {
            throw new DownstreamServiceException(
                    "User service is unavailable",
                    ex
            );
        }
    }
}
