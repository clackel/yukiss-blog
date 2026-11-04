package moon.yukiss.utils;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** 从评论正文中解析 @username（用户名规则与注册一致：3-20 位字母、数字、下划线） */
public final class Mentions {
    /** 每条评论最多提及的用户数，多余的忽略 */
    public static final int MAX_MENTIONS = 10;

    // 前后都不能紧挨字母数字，避免把邮箱（a@b.com）当成提及
    private static final Pattern MENTION = Pattern.compile("(?<![A-Za-z0-9_])@([A-Za-z0-9_]{3,20})(?![A-Za-z0-9_])");

    private Mentions() {
    }

    /** 按出现顺序返回去重后的用户名，最多 MAX_MENTIONS 个 */
    public static List<String> extractUsernames(String content) {
        if (content == null || content.indexOf('@') < 0) {
            return List.of();
        }
        Set<String> usernames = new LinkedHashSet<>();
        Matcher matcher = MENTION.matcher(content);
        while (matcher.find() && usernames.size() < MAX_MENTIONS) {
            usernames.add(matcher.group(1));
        }
        return new ArrayList<>(usernames);
    }
}
