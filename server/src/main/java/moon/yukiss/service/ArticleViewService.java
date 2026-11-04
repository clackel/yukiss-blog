package moon.yukiss.service;

import jakarta.annotation.PreDestroy;
import moon.yukiss.mapper.ArticleMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;

/**
 * 文章浏览量：先在内存中累计，再由定时任务批量写入 article.view_count，避免每次打开文章都写库。
 * 计数保存在内存中，进程异常退出时最多丢失一个刷新周期内的浏览量；多实例部署时各实例分别累计后相加。
 */
@Service
public class ArticleViewService {
    private static final Logger log = LoggerFactory.getLogger(ArticleViewService.class);

    private final ArticleMapper articleMapper;
    private final ConcurrentHashMap<Integer, LongAdder> pending = new ConcurrentHashMap<>();

    public ArticleViewService(ArticleMapper articleMapper) {
        this.articleMapper = articleMapper;
    }

    public void record(Integer articleId) {
        if (articleId == null) {
            return;
        }
        pending.computeIfAbsent(articleId, id -> new LongAdder()).increment();
    }

    /** 尚未写入数据库的浏览量，用于展示时补齐 */
    public long pendingCount(Integer articleId) {
        LongAdder adder = articleId == null ? null : pending.get(articleId);
        return adder == null ? 0 : adder.sum();
    }

    /** 把累计的浏览量写入数据库；sumThenReset 保证刷新期间的新增浏览不会丢失 */
    @Scheduled(fixedDelayString = "${app.view-count.flush-interval-ms:10000}")
    @PreDestroy
    public void flush() {
        for (Map.Entry<Integer, LongAdder> entry : pending.entrySet()) {
            long delta = entry.getValue().sumThenReset();
            if (delta <= 0) {
                continue;
            }
            try {
                articleMapper.addViewCount(entry.getKey(), delta);
            } catch (RuntimeException ex) {
                // 写入失败时把计数加回去，下个周期重试
                entry.getValue().add(delta);
                log.warn("写入文章 {} 的浏览量失败，将在下个周期重试", entry.getKey(), ex);
            }
        }
    }
}
