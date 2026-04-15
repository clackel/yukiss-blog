package moon.yukiss.service;

import moon.yukiss.entity.ArticleComment;
import moon.yukiss.mapper.ArticleCommentMapper;
import moon.yukiss.utils.ThreadLocalUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class ArticleCommentServiceImpl implements ArticleCommentService {
    @Autowired
    private ArticleCommentMapper articleCommentMapper;

    // ServiceImpl 核心方法
    @Override
    public void addComment(ArticleComment comment) {
        // 自动补齐当前登录者 ID
        Map<String, Object> userMap = ThreadLocalUtil.get();
        comment.setUserId((Integer) userMap.get("id"));

        articleCommentMapper.insert(comment);
    }
}
