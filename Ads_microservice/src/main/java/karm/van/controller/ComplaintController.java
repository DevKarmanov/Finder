package karm.van.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import karm.van.dto.complaint.ComplaintDtoRequest;
import karm.van.exception.card.CardNotFoundException;
import karm.van.exception.other.SerializationException;
import karm.van.exception.other.TokenNotExistException;
import karm.van.exception.user.NotEnoughPermissionsException;
import karm.van.exception.user.UsernameNotFoundException;
import karm.van.service.ComplaintService;
import lombok.RequiredArgsConstructor;
import org.apache.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@CrossOrigin
@RequestMapping("complaint")
@RequiredArgsConstructor
public class ComplaintController {
    private final ComplaintService complaintService;

    @PostMapping(value = "/create",consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> createComplaint(@RequestPart ComplaintDtoRequest complaintDto,
                                             @RequestHeader("Authorization") String authorization){
        try {
            complaintService.createComplaint(authorization,complaintDto);
            return ResponseEntity.ok("Complaint successfully sent");
        }catch (TokenNotExistException e){
            return ResponseEntity.badRequest().body(e.getMessage());
        }catch (UsernameNotFoundException | CardNotFoundException e){
            return ResponseEntity.status(HttpStatus.SC_NOT_FOUND).body(e.getMessage());
        }
    }

    @GetMapping("/get")
    public ResponseEntity<?> getComplaintList(@RequestParam(required = false,defaultValue = "5") int limit,
                                              @RequestParam(required = false,defaultValue = "0") int page,
                                              @RequestParam(required = false,defaultValue = "all") String complaintType,
                                              @RequestHeader("Authorization") String authorization
                                              ){
        try {
            return ResponseEntity.ok(complaintService.getComplaints(authorization,limit,page,complaintType));
        }catch (TokenNotExistException e){
            System.out.println(e.getClass()+": "+e.getMessage());
          return ResponseEntity.badRequest().body(e.getMessage());
        }catch (SerializationException | JsonProcessingException | UsernameNotFoundException e){
            System.out.println(e.getClass()+": "+e.getMessage());
            return ResponseEntity.status(HttpStatus.SC_INTERNAL_SERVER_ERROR).body(e.getMessage());
        }catch (NotEnoughPermissionsException e){
            System.out.println(e.getClass()+": "+e.getMessage());
            return ResponseEntity.status(HttpStatus.SC_FORBIDDEN).body(e.getMessage());
        }
    }

    @DeleteMapping("/dellAllByUser/{userId}")
    public ResponseEntity<?> dellAllComplaintByUser(
                                        @RequestHeader("Authorization") String authorization,
                                        @RequestHeader("x-api-key") String apiKey,
                                        @PathVariable("userId") Long userId){
        try {
            complaintService.dellAllComplaintByUser(authorization,apiKey,userId);
            return ResponseEntity.ok("All complaints have been deleted");
        }catch (TokenNotExistException e){
            return ResponseEntity.badRequest().body(e.getMessage());
        }catch (Exception e){
            return ResponseEntity.status(HttpStatus.SC_INTERNAL_SERVER_ERROR).body(e.getMessage());
        }

    }

    @DeleteMapping("delOne/{complaintId}")
    public ResponseEntity<?> dellOneComplaint(
                                        @RequestHeader("Authorization") String authorization,
                                        @PathVariable("complaintId") Long complaintId){
        try {
            complaintService.delOneComplaint(authorization,complaintId);
            return ResponseEntity.ok("Complaint have been deleted");
        }catch (TokenNotExistException e){
            return ResponseEntity.badRequest().body(e.getMessage());
        }catch (UsernameNotFoundException e){
            return ResponseEntity.status(HttpStatus.SC_NOT_FOUND).body(e.getMessage());
        }catch (NotEnoughPermissionsException e){
            return ResponseEntity.status(HttpStatus.SC_FORBIDDEN).body(e.getMessage());
        }
    }

}
