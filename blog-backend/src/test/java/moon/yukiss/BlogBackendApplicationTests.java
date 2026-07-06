package moon.yukiss;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = "app.schema-initializer.enabled=false")
class BlogBackendApplicationTests {

    @Test
    void contextLoads() {
    }

}
