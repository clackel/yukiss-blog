package moon.yukiss.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/** 查询仍在使用上传文件的地方，供清理任务判断哪些文件已无人引用。 */
@Mapper
public interface UploadReferenceMapper {
    @Select("SELECT avatar FROM user WHERE is_deleted = 0 AND avatar LIKE '%/uploads/%'")
    List<String> findAvatarUrls();

    /** 包含草稿：草稿中插入的图片同样需要保留 */
    @Select("SELECT content FROM article WHERE content LIKE '%/uploads/%'")
    List<String> findArticleContents();
}
