package moon.yukiss.controller;

import moon.yukiss.common.ApiResponse;
import moon.yukiss.dto.PublicUserProfile;
import moon.yukiss.service.UserFollowService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/users")
public class UserFollowController {
    private final UserFollowService userFollowService;

    public UserFollowController(UserFollowService userFollowService) {
        this.userFollowService = userFollowService;
    }

    @GetMapping("/{id}")
    public ApiResponse<PublicUserProfile> getPublicProfile(@PathVariable Integer id) {
        return ApiResponse.ok(userFollowService.getPublicProfile(id));
    }

    @PostMapping("/{id}/follow")
    public ApiResponse<PublicUserProfile> follow(@PathVariable Integer id) {
        return ApiResponse.ok(userFollowService.follow(id));
    }

    @DeleteMapping("/{id}/follow")
    public ApiResponse<PublicUserProfile> unfollow(@PathVariable Integer id) {
        return ApiResponse.ok(userFollowService.unfollow(id));
    }
}
