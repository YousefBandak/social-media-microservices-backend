package object_orienters.techspot.content_service.profile;

import object_orienters.techspot.content_service.ChatterBean;
import object_orienters.techspot.content_service.MessagesServiceProxy;
import object_orienters.techspot.content_service.comment.CommentRepository;
import object_orienters.techspot.content_service.dataTypes.DataType;
import object_orienters.techspot.content_service.dataTypes.DataTypeRepository;
import object_orienters.techspot.content_service.exceptions.ProfileNotFoundException;
import object_orienters.techspot.content_service.exceptions.UserCannotFollowSelfException;
import object_orienters.techspot.content_service.exceptions.UserNotFoundException;
import object_orienters.techspot.content_service.post.PostRepository;
import object_orienters.techspot.content_service.utilities.FileStorageService;
import object_orienters.techspot.content_service.utilities.MediaDataUtilities;

// import object_orienters.techspot.message.Chatter;
// import object_orienters.techspot.message.ChatterService;
// import object_orienters.techspot.message.Status;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.netflix.discovery.converters.Auto;

import java.io.IOException;
import java.util.Optional;

@Service
public class ProfileService {
    @Autowired
    private ProfileRepository repo;

    @Autowired
    private PostRepository postRepository;



    @Autowired
    private DataTypeRepository dataTypeRepository;

    @Autowired
    private MessagesServiceProxy proxy;

    // @Autowired
    // ChatterService chatterService;

    @Autowired
    FileStorageService fileStorageService;

    // @Autowired
    // PasswordEncoder encoder;

    @Autowired
    MediaDataUtilities mediaDataUtilities;

    public ProfileService(ProfileRepository repo) {
        this.repo = repo;
    }

    public Profile getUserByUsername(String username) throws UserNotFoundException {
        return repo.findByUsername(username).orElseThrow(() -> new UserNotFoundException(username));
    }

    public Profile createNewProfile(String username, String email, String name) throws IOException {
        Profile newProfile = new Profile();
        newProfile.setUsername(username);
       
        newProfile.setEmail(email);
        newProfile.setName(name);
        proxy.addChatter1(username, name, "ONLINE");
       
        return repo.save(newProfile);

    }

    public void deleteProfile(String username) throws UserNotFoundException {
        Profile profile = repo.findByUsername(username)
                .orElseThrow(() -> new ProfileNotFoundException(username));
                
        profile.getPublishedPosts().stream().forEach(post -> {
            post.getMediaData().stream().forEach(media -> {
                fileStorageService.deleteFile(media.getFileName());
            });
            postRepository.delete(post);
        });

        repo.delete(profile);
    }

    @Transactional
    public Profile updateUserProfile(UpdateProfile newUser, String username) throws UserNotFoundException {
        return repo.findByUsername(username).map(user -> {
            Optional.ofNullable(newUser.getDob()).ifPresent(user::setDob);
            Optional.ofNullable(newUser.getName()).ifPresent(user::setName);
            Optional.ofNullable(newUser.getAbout()).ifPresent(user::setAbout);
            Optional.ofNullable(newUser.getProfession()).ifPresent(user::setProfession);
            Optional.ofNullable(newUser.getGender()).ifPresent(user::setGender);
         
            // Optional.ofNullable(newUser.getPassword()).ifPresent(password -> {
            // user.getOwner().setPassword(encoder.encode(password));
            // userRepository.save(user.getOwner());
            // });

            return repo.save(user);
        }).orElseThrow(() -> new UserNotFoundException(username));
    }

    public Optional<Profile> getFollowingByUsername(String username, String followingUsername)
            throws UserNotFoundException {
        return repo.findFollowingByUsername(username, followingUsername);
    }

    public Profile getFollowerByUsername(String username, String followerUserName) throws UserNotFoundException {
        return repo.findFollowerByUsername(username, followerUserName)
                .orElseThrow(() -> new UserNotFoundException(followerUserName));
    }

    public Page<Profile> getUserFollowersByUsername(String username, int page, int size) throws UserNotFoundException {
        return repo.findFollowersByUserId(username, PageRequest.of(page, size));
    }

    public Page<Profile> getUserFollowingByUsername(String username, int page, int size) throws UserNotFoundException {
        return repo.findFollowingByUserId(username, PageRequest.of(page, size));
    }

    public Profile addNewFollower(String username, String followerUserName) throws UserNotFoundException {
        if (username.equals(followerUserName)) {
            throw new UserCannotFollowSelfException(followerUserName);
        }
        Profile newFollower = getUserByUsername(followerUserName);
        Optional<Profile> user = repo.findByUsername(username);
        user.get().getFollowers().add(newFollower);
        newFollower.getFollowing().add(user.get());
        Profile savedUser = repo.save(user.get());
        repo.save(newFollower);
        return savedUser;
    }

    public void deleteFollower(String username, String deletedUser) throws UserNotFoundException {
        Profile profile = repo.findByUsername(username).orElseThrow(() -> new UserNotFoundException(username));
        Profile deletedProfile = repo.findByUsername(deletedUser)
                .orElseThrow(() -> new UserNotFoundException(deletedUser));
        profile.getFollowers().remove(deletedProfile);
        deletedProfile.getFollowing().remove(profile);
        repo.save(profile);
        repo.save(deletedProfile);
    }

    public void deleteFollowing(String username, String deletedUser) throws UserNotFoundException {
        Optional<Profile> profile = repo.findByUsername(username);
        Optional<Profile> deletedProfile = repo.findByUsername(deletedUser);
        profile.get().getFollowing().remove(deletedProfile.get());
        deletedProfile.get().getFollowers().remove(profile.get());
        repo.save(profile.get());
        repo.save(deletedProfile.get());
    }

    @Transactional
    public Profile addProfilePic(String username, MultipartFile file, String text)
            throws UserNotFoundException, IOException {
        Profile user = repo.findById(username).orElseThrow(() -> new UserNotFoundException(username));
        DataType profilePic = null;
        if (file != null && !file.isEmpty()) {
            profilePic = mediaDataUtilities.handleAddFile(file);
        }
        user.setProfilePic(profilePic);
        dataTypeRepository.save(profilePic);
        return repo.save(user);
    }

    @Transactional
    public Profile addBackgroundImg(String username, MultipartFile file, String text)
            throws UserNotFoundException, IOException {
        Profile user = repo.findById(username).orElseThrow(() -> new UserNotFoundException(username));
        DataType backgroundImg = null;
        if (file != null && !file.isEmpty()) {
            backgroundImg = mediaDataUtilities.handleAddFile(file);
        }
        user.setBackgroundImg(backgroundImg);
        dataTypeRepository.save(backgroundImg);
        return repo.save(user);
    }

    // public void deleteProfile(String username) throws UserNotFoundException {
    // repo.delete(repo.findById(username).get());
    // }

}
