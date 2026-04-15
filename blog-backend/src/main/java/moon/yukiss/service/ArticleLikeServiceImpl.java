package moon.yukiss.service;

import moon.yukiss.mapper.ArticleLikeMapper;
import moon.yukiss.utils.ThreadLocalUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class ArticleLikeServiceImpl implements ArticleLikeService {
    @Autowired
    private ArticleLikeMapper articleLikeMapper;

    @Override
    public String toggleLike(Integer articleId) {
        // 获取用户Token
        Map<String, Object> map = ThreadLocalUtil.get();
        Integer userid = (Integer) map.get("id");

        // 判断是否点赞
        int count = articleLikeMapper.checkIsLiked(userid, articleId);
        if (count > 0) {
            articleLikeMapper.deleteLike(userid, articleId);
            return "取消点赞";
        }else  {
            articleLikeMapper.addLike(userid, articleId);
            return "点赞成功";
        }
    }
}
