package karm.van.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import karm.van.dto.complaint.ComplaintType;
import karm.van.config.properties.AuthenticationMicroServiceProperties;
import karm.van.dto.complaint.*;
import karm.van.dto.user.UserDtoRequest;
import karm.van.exception.card.CardNotFoundException;
import karm.van.exception.other.SerializationException;
import karm.van.exception.other.TokenNotExistException;
import karm.van.exception.user.NotEnoughPermissionsException;
import karm.van.exception.user.UsernameNotFoundException;
import karm.van.model.Complaint;
import karm.van.repo.jpaRepo.CardRepo;
import karm.van.repo.jpaRepo.ComplaintRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPooled;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

@Service
@Slf4j
@RequiredArgsConstructor
public class ComplaintService {
    private final ComplaintRepo complaintRepo;
    private final ApiService apiService;
    private final AuthenticationMicroServiceProperties authProperties;
    private final CardRepo cardRepo;
    private final Jedis redis;
    private final ObjectMapper objectMapper;

    @Value("${microservices.x-api-key}")
    private String apiKey;

    private void checkToken(String token) throws TokenNotExistException {
        if (!apiService.validateToken(token,
                apiService.buildUrl(authProperties.getPrefix(),
                        authProperties.getHost(),
                        authProperties.getPort(),
                        authProperties.getEndpoints().getValidateToken()
                )
        )){
            throw new TokenNotExistException("Invalid token or expired");
        }
    }

    private UserDtoRequest requestToGetUserByToken(String token) throws UsernameNotFoundException {
        UserDtoRequest user = apiService.getUserByToken(apiService.buildUrl(
                authProperties.getPrefix(),
                authProperties.getHost(),
                authProperties.getPort(),
                authProperties.getEndpoints().getUser()
        ), token,apiKey);

        if (user == null) {
            throw new UsernameNotFoundException("User not found");
        }

        return user;
    }

    private UserDtoRequest requestToGetUserById(String token, Long userId) throws UsernameNotFoundException {
        UserDtoRequest user = apiService.getUserById(apiService.buildUrl(
                authProperties.getPrefix(),
                authProperties.getHost(),
                authProperties.getPort(),
                authProperties.getEndpoints().getUser()
        ), token,userId,apiKey);

        if (user == null) {
            throw new UsernameNotFoundException("User not found");
        }

        return user;
    }

    @Transactional
    public void createComplaint(String authorizationHeader, ComplaintDtoRequest complaintDto) throws TokenNotExistException, UsernameNotFoundException, CardNotFoundException {
        String token = authorizationHeader.substring(7);

        checkToken(token);

        Complaint complaint = new Complaint();
        complaint.setReason(complaintDto.reason());

        ComplaintType complaintType = complaintDto.targetType();
        complaint.setComplaintType(complaintType);

        Long dtoTargetId = complaintDto.complaintTargetId();
        if (complaintType == ComplaintType.USER){
            UserDtoRequest user = requestToGetUserById(token,dtoTargetId);
            complaint.setTargetId(user.id());
        } else if (complaintType == ComplaintType.CARD) {
            if (cardRepo.existsById(dtoTargetId)){
                complaint.setTargetId(dtoTargetId);
            }else {
                throw new CardNotFoundException("Card with this id doesn't exist");
            }
        }

        UserDtoRequest user = requestToGetUserByToken(token);
        complaint.setComplaintAuthorId(user.id());

        complaintRepo.save(complaint);

    }

    public ComplaintPageResponseDto getComplaints(String authorization, int limit, int page, String complaintType) throws TokenNotExistException, SerializationException, JsonProcessingException, UsernameNotFoundException, NotEnoughPermissionsException {
        String token = authorization.substring(7);
        checkToken(token);
        checkUserPermissions(token);

        String redisKey = "complaints:"+page+":"+limit+":"+complaintType;

        if (redis.exists(redisKey)){
            return objectMapper.readValue(redis.get(redisKey),ComplaintPageResponseDto.class);
        }else {
            Page<Complaint> complaints = switch (complaintType.trim().toLowerCase()) {
                case ("user") -> complaintRepo.findAllByComplaintType(PageRequest.of(page, limit), ComplaintType.USER);
                case ("card") -> complaintRepo.findAllByComplaintType(PageRequest.of(page, limit), ComplaintType.CARD);
                default -> complaintRepo.findAll(PageRequest.of(page, limit));
            };

            return cacheComplaints(redisKey,complaints,getComplaintsForCache(token,complaints));
        }
    }

    private List<AbstractComplaint> getComplaintsForCache(String token, Page<Complaint> complaints){
        ConcurrentLinkedQueue<AbstractComplaint> complaintList = new ConcurrentLinkedQueue<>();

        complaints.forEach(complaint -> {
            ComplaintType target = complaint.getComplaintType();
            try {
                UserDtoRequest complaintAuthor = requestToGetUserById(token, complaint.getComplaintAuthorId());

                if (target == ComplaintType.USER) {
                    UserDtoRequest badUser = requestToGetUserById(token, complaint.getTargetId());
                    UserComplaintDtoResponse userComplaintDtoResponse =
                            new UserComplaintDtoResponse(badUser.name(), complaint.getReason(), complaintAuthor.name(), complaint.getId());
                    complaintList.add(userComplaintDtoResponse);
                } else if (target == ComplaintType.CARD) {
                    CardComplaintDtoResponse cardComplaintDtoResponse =
                            new CardComplaintDtoResponse(complaint.getTargetId(), complaint.getReason(), complaintAuthor.name(), complaint.getId());
                    complaintList.add(cardComplaintDtoResponse);
                }
            } catch (Exception e) {
                log.error(e.getClass() + " : " + e.getMessage());
            }
        });

        return new ArrayList<>(complaintList);
    }

    private ComplaintPageResponseDto cacheComplaints(String key, Page<Complaint> page, List<AbstractComplaint> complaints) throws SerializationException {
        String objectAsString;

        ComplaintPageResponseDto complaintPageResponseDto = new ComplaintPageResponseDto(
                complaints,
                page.isLast(),
                page.getTotalPages(),
                page.getTotalElements(),
                page.isFirst(),
                page.getNumberOfElements());

        try {
            objectAsString = objectMapper.writeValueAsString(complaintPageResponseDto);
        } catch (JsonProcessingException e) {
            throw new SerializationException("an error occurred during deserialization");
        }

        redis.set(key,objectAsString);
        redis.expire(key,60);

        return complaintPageResponseDto;
    }

    public Boolean checkApiKey(String apiKey){
        return apiKey.equals(this.apiKey);
    }


    @Transactional
    public void dellAllComplaintByUser(String authorization, String apiKey, Long userId) throws TokenNotExistException {
        checkApiKey(authorization);
        if (!checkApiKey(apiKey)){
            throw new TokenNotExistException("Invalid apiKey");
        }

        complaintRepo.deleteAllByTargetIdAndComplaintType(userId,ComplaintType.USER);
        complaintRepo.deleteAllByComplaintAuthorId(userId);

    }

    private void checkUserPermissions(String token) throws UsernameNotFoundException, NotEnoughPermissionsException {

        UserDtoRequest user;

        try {
            user = requestToGetUserByToken(token);
        }catch (UsernameNotFoundException e) {
            throw new UsernameNotFoundException("User with this token doesn't exist");
        }

        List<String> userRoles = user.role();

        if (userRoles.stream().noneMatch(role->role.equals("ROLE_ADMIN"))){
            throw new NotEnoughPermissionsException("You don't have permission to do this");
        }
    }

    @Transactional
    public void delOneComplaint(String authorization, Long complaintId) throws TokenNotExistException, UsernameNotFoundException, NotEnoughPermissionsException {
        String token = authorization.substring(7);
        checkToken(token);
        checkUserPermissions(token);

        complaintRepo.deleteById(complaintId);
    }
}
