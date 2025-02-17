package object_orienters.techspot.content_service;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

//, url = "localhost:8081"
@FeignClient(name = "messages-service")
public interface MessagesServiceProxy {
    
    @PostMapping(value = "/chatter/add",produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> addChatter1(@RequestParam String name, @RequestParam String username, @RequestParam String status);
    
}
