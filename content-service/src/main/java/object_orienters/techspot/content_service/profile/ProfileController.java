package object_orienters.techspot.content_service.profile;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.validation.Valid;
import object_orienters.techspot.content_service.SecurityServiceProxy;
import object_orienters.techspot.content_service.SignupRequest;
import object_orienters.techspot.content_service.exceptions.ExceptionsResponse;
import object_orienters.techspot.content_service.exceptions.UserCannotFollowSelfException;
import object_orienters.techspot.content_service.exceptions.UserNotFoundException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.IanaLinkRelations;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@RestController
@RequestMapping("/profiles")
@CrossOrigin(origins = "*")
public class ProfileController {

    @Autowired
    private SecurityServiceProxy securityServiceProxy;
    private String username;
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
    private final ProfileModelAssembler assembler;
    private final ProfileService profileService;
    private final Logger logger = LoggerFactory.getLogger(ProfileController.class);

    public ProfileController(ProfileModelAssembler assembler, ProfileService profileService) {
        this.assembler = assembler;
        this.profileService = profileService;
    }

    private static String getTimestamp() {
        return LocalDateTime.now().format(formatter) + " ";
    }

    @PostMapping(value = "", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> createProf(@RequestBody SignupRequest request) throws IOException {
        Profile prof = profileService.createNewProfile(request.getUsername(), request.getEmail(), request.getName());

        return ResponseEntity.ok(prof);
    }

    @DeleteMapping("/{username}")
    // @PreAuthorize("#username == authentication.principal.username")
    public ResponseEntity<?> deleteProfile(@PathVariable String username) {

        if (!verifyUser())
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized");
        else if (!username.equals(this.username))
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized");

        try {
            logger.info(">>>>Deleting Profile... " + getTimestamp() + "<<<<");
            profileService.deleteProfile(username);
            logger.info(">>>>Profile Deleted. " + getTimestamp() + "<<<<");
            return ResponseEntity.noContent().build();
        } catch (UserNotFoundException exception) {
            logger.info(">>>>Error Occurred:  " + exception.getMessage() + " " + getTimestamp() + "<<<<");
            return ExceptionsResponse.getErrorResponseEntity(exception, HttpStatus.NOT_FOUND);
        }
    }

    @GetMapping("/{username}")
    public ResponseEntity<?> one(@PathVariable String username) {

        if (!verifyUser())
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized");

        try {
            logger.info(
                    ">>>>Retrieving Profile From Database... " + getTimestamp() + "<<<<");
            EntityModel<Profile> profileModel = assembler.toModel(profileService.getUserByUsername(username));
            logger.info(">>>>Profile  Retrieved. " + getTimestamp() + "<<<<");
            return ResponseEntity.ok(profileModel);
        } catch (UserNotFoundException exception) {
            logger.info(">>>>Error Occurred:  " + exception.getMessage() + " " + getTimestamp() + "<<<<");
            return ExceptionsResponse.getErrorResponseEntity(exception, HttpStatus.NOT_FOUND);

        }
    }

    @PutMapping("/{username}")
    // @PreAuthorize("#username == authentication.principal.username")
    public ResponseEntity<?> updateProfile(@Valid @RequestBody UpdateProfile newUser, @PathVariable String username) {

        if (!verifyUser())
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized");
        else if (!username.equals(this.username))
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized");

        try {
            logger.info(">>>>Updating Profile... " + getTimestamp() + "<<<<");
            Profile updatedUser = profileService.updateUserProfile(newUser, username);
            logger.info(">>>>Profile Updated. " + getTimestamp() + "<<<<");
            return ResponseEntity.ok()
                    .location(assembler.toModel(updatedUser).getRequiredLink(IanaLinkRelations.SELF).toUri())
                    .body(assembler.toModel(updatedUser));
        } catch (UserNotFoundException exception) {
            logger.info(">>>>Error Occurred:  " + exception.getMessage() + " " + getTimestamp() + "<<<<");
            return ExceptionsResponse.getErrorResponseEntity(exception, HttpStatus.NOT_FOUND);

        }
    }

    // get user followers
    @GetMapping("/{username}/followers")
    public ResponseEntity<?> Followers(@PathVariable String username, @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        if (!verifyUser())
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized");
        try {
            logger.info(">>>>Retrieving Followers List... " + getTimestamp() + "<<<<");
            Page<Profile> followersPage = profileService.getUserFollowersByUsername(username, page, size);
            List<EntityModel<Profile>> followers = followersPage.stream()
                    .map(assembler::toModel)
                    .collect(Collectors.toList());
            logger.info(">>>>Followers List Retrieved. " + getTimestamp() + "<<<<");
            return ResponseEntity.ok(PagedModel
                    .of(followers,
                            new PagedModel.PageMetadata(followersPage.getSize(), followersPage.getNumber(),
                                    followersPage.getTotalElements(), followersPage.getTotalPages()))
                    .add(linkTo(methodOn(ProfileController.class).one(username)).withRel("profile"))
                    .add(linkTo(methodOn(ProfileController.class).Followers(username, page, size)).withSelfRel()));
        } catch (UserNotFoundException exception) {
            logger.info(">>>>Error Occurred:  " + exception.getMessage() + " " + getTimestamp() + "<<<<");
            return ExceptionsResponse.getErrorResponseEntity(exception, HttpStatus.NOT_FOUND);

        }
    }

    // get specific user follower
    @GetMapping("/{username}/follower")
    public ResponseEntity<?> getSpecificFollower(@PathVariable String username,

            @RequestParam(value = "followerUserName") String followerUserName) {
        if (!verifyUser())
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized");

        try {
            logger.info(">>>>Retrieving Follower... " + getTimestamp() + "<<<<");
            Profile follower = profileService.getFollowerByUsername(username, followerUserName);
            logger.info(">>>>Follower Retrieved. " + getTimestamp() + "<<<<");
            return ResponseEntity.ok(assembler.toModel(follower));
        } catch (UserNotFoundException exception) {
            logger.info(">>>>Error Occurred:  " + exception.getMessage() + " " + getTimestamp() + "<<<<");
            return ExceptionsResponse.getErrorResponseEntity(exception, HttpStatus.NOT_FOUND);

        }
    }

    // get user followers
    @GetMapping("/{username}/following")
    public ResponseEntity<?> Following(@PathVariable String username, @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        if (!verifyUser())
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized");

        try {
            logger.info(">>>>Retrieving Following List... " + getTimestamp() + "<<<<");
            Page<Profile> followingPage = profileService.getUserFollowingByUsername(username, page, size);
            List<EntityModel<Profile>> following = followingPage.stream()
                    .map(userModel -> assembler.toModel(userModel))
                    .collect(Collectors.toList());
            logger.info(">>>>Following List Retrieved. " + getTimestamp() + "<<<<");
            return ResponseEntity.ok(PagedModel
                    .of(following,
                            new PagedModel.PageMetadata(followingPage.getSize(), followingPage.getNumber(),
                                    followingPage.getTotalElements(), followingPage.getTotalPages()))
                    .add(linkTo(methodOn(ProfileController.class).one(username)).withRel("profile"))
                    .add(linkTo(methodOn(ProfileController.class).Followers(username, page, size)).withSelfRel()));
        } catch (UserNotFoundException exception) {
            logger.info(">>>>Error Occurred:  " + exception.getMessage() + " " + getTimestamp() + "<<<<");
            return ExceptionsResponse.getErrorResponseEntity(exception, HttpStatus.NOT_FOUND);

        }
    }

    @GetMapping("/{username}/following/{followingUsername}")
    public ResponseEntity<?> getSpecificFollowing(@PathVariable String username,
            @PathVariable String followingUsername) {
        if (!verifyUser())
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized");

        try {
            logger.info(">>>>Retrieving Following Profile... " + getTimestamp() + "<<<<");
            Profile followingProfile = profileService.getFollowingByUsername(username, followingUsername)
                    .orElseThrow(() -> new UserNotFoundException(followingUsername));
            logger.info(">>>>Following Profile Retrieved. " + getTimestamp() + "<<<<");
            return ResponseEntity
                    .ok(assembler.toModel(followingProfile));
        } catch (UserNotFoundException exception) {
            logger.info(">>>>Error Occurred:  " + exception.getMessage() + " " + getTimestamp() + "<<<<");
            return ExceptionsResponse.getErrorResponseEntity(exception, HttpStatus.NOT_FOUND);
        }
    }

    @PostMapping("/{username}/followers")
    // @PreAuthorize("#followerUserName.get(\"username\").asText() ==
    // authentication.principal.username")
    public ResponseEntity<?> newFollower(@PathVariable String username, @RequestBody ObjectNode followerUserName) {
        if (!verifyUser())
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized");
        else if (!followerUserName.get("username").asText().equals(this.username))
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized");

        try {
            logger.info(">>>>Adding New Follower... " + getTimestamp() + "<<<<");
            Profile newFollower = profileService.addNewFollower(username, followerUserName.get("username").asText());
            logger.info(">>>>New Follower Added. " + getTimestamp() + "<<<<");
            return ResponseEntity.ok(assembler.toModel(newFollower));
        } catch (UserNotFoundException exception) {
            logger.info(">>>>Error Occurred:  " + exception.getMessage() + " " + getTimestamp() + "<<<<");
            return ExceptionsResponse.getErrorResponseEntity(exception, HttpStatus.NOT_FOUND);
        } catch (UserCannotFollowSelfException ex) {
            logger.info(">>>>Error Occurred: " + ex.getMessage() + " " + getTimestamp() + "<<<<");
            return ExceptionsResponse.getErrorResponseEntity(ex, HttpStatus.NOT_FOUND);

        }
    }

    // delete follower from user
    @DeleteMapping("/{username}/followers")
    // @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> deleteFollower(@PathVariable String username, @RequestBody ObjectNode deletedUser) {
        if (!verifyUser())
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized");
        else if (!username.equals(this.username))
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized");

        try {
            logger.info(">>>>Deleting Follower... " + getTimestamp() + "<<<<");
            profileService.deleteFollower(username, deletedUser.get("deletedUser").asText());
            logger.info(">>>>Follower Added. " + getTimestamp() + "<<<<");
            return ResponseEntity.noContent().build();
        } catch (UserNotFoundException exception) {
            logger.info(">>>>Error Occurred:  " + exception.getMessage() + " " + getTimestamp() + "<<<<");
            return ExceptionsResponse.getErrorResponseEntity(exception, HttpStatus.NOT_FOUND);

        }
    }

    @DeleteMapping("/{username}/following")
    // @PreAuthorize("#username == authentication.principal.username")
    public ResponseEntity<?> deleteFollowing(@PathVariable String username, @RequestBody ObjectNode deletedUser) {
        if (!verifyUser())
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized");
        else if (!username.equals(this.username))
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized");

        try {
            logger.info(">>>>Deleting Following... " + getTimestamp() + "<<<<");
            profileService.deleteFollowing(username, deletedUser.get("deletedUser").asText());
            logger.info(">>>>Following Added. " + getTimestamp() + "<<<<");
            return ResponseEntity.noContent().build();
        } catch (UserNotFoundException exception) {
            logger.info(">>>>Error Occurred:  " + exception.getMessage() + " " + getTimestamp() + "<<<<");
            return ExceptionsResponse.getErrorResponseEntity(exception, HttpStatus.NOT_FOUND);

        }
    }

    @PostMapping("/{username}/profilePic")
    // @PreAuthorize("#username == authentication.principal.username") //TODO make a
    // custom annotation
    public ResponseEntity<?> addProfilePic(@PathVariable String username,
            @RequestParam(value = "file") MultipartFile file,
            @RequestParam(value = "text", required = false) String text) throws UserNotFoundException, IOException {

        if (!verifyUser())
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized");
        // else if (!username.equals(this.username))
        // return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized");

        try {
            logger.info(">>>>Adding Profile Picture... " + getTimestamp() + "<<<<");
            Profile profile = profileService.addProfilePic(username, file, text);
            logger.info(">>>>Profile Picture Added. " + getTimestamp() + "<<<<");
            return ResponseEntity.ok(assembler.toModel(profile));
        } catch (UserNotFoundException exception) {
            logger.info(">>>>Error Occurred:  " + exception.getMessage() + " " + getTimestamp() + "<<<<");
            return ExceptionsResponse.getErrorResponseEntity(exception, HttpStatus.NOT_FOUND);

        } catch (IOException exception) {
            logger.info(">>>>Error Occurred:  " + exception.getMessage() + " " + getTimestamp() + "<<<<");
            return ExceptionsResponse.getErrorResponseEntity(exception, HttpStatus.NOT_FOUND);

        }
    }

    @PostMapping("/{username}/backgroundImg")
    // @PreAuthorize("#username == authentication.principal.username")
    public ResponseEntity<?> addBackgroundImg(@PathVariable String username,
            @RequestParam(value = "file") MultipartFile file,
            @RequestParam(value = "text", required = false) String text) throws UserNotFoundException, IOException {

        if (!verifyUser())
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized");
        else if (!username.equals(this.username))
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized");

        try {
            logger.info(">>>>Adding Background Image... " + getTimestamp() + "<<<<");
            Profile profile = profileService.addBackgroundImg(username, file, text);
            logger.info(">>>>Background Image Added. " + getTimestamp() + "<<<<");
            return ResponseEntity.ok(assembler.toModel(profile));
        } catch (UserNotFoundException exception) {
            logger.info(">>>>Error Occurred:  " + exception.getMessage() + " " + getTimestamp() + "<<<<");
            return ExceptionsResponse.getErrorResponseEntity(exception, HttpStatus.NOT_FOUND);

        } catch (IOException exception) {
            logger.info(">>>>Error Occurred:  " + exception.getMessage() + " " + getTimestamp() + "<<<<");
            return ExceptionsResponse.getErrorResponseEntity(exception, HttpStatus.NOT_FOUND);

        }
    }

    private boolean verifyUser() { return true;
        // Map<String, ?> body = securityServiceProxy.verifyUser().getBody();

       

        // if (body.containsKey("username")) {
        //     username = (String) body.get("username");
        //     System.out.println("user verified " + username);
        //     return true;
        // }

        // System.out.println("user not verified");
        // return false;


        
        // // username = (String) body.get("username");
        // // System.out.println("hi im here%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%");
        // System.out.println(body.get("username"));
        // System.out.println(body.get("username") != "null");
        // return body.get("username");
    }

}
