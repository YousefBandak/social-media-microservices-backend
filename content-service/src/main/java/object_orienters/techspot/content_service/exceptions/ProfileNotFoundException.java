package object_orienters.techspot.content_service.exceptions;

public class ProfileNotFoundException extends RuntimeException {
    public ProfileNotFoundException(String username) {
        super("Profile With Username: " + username + " Could Not Be Found.");
    }
}
