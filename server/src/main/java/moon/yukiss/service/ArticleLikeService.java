package moon.yukiss.service;

public interface ArticleLikeService {
    String toggleLike(Integer articleId);

    int countLikes(Integer articleId);
}
