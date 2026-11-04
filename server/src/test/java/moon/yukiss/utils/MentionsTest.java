package moon.yukiss.utils;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MentionsTest {
    @Test
    void extractsUsernamesInOrderWithoutDuplicates() {
        assertEquals(
                List.of("alice", "bob_1"),
                Mentions.extractUsernames("@alice 和 @bob_1 看看，@alice 再看一次")
        );
    }

    @Test
    void ignoresEmailsAndTooShortNames() {
        assertEquals(List.of(), Mentions.extractUsernames("联系 me@example.com 或 @ab"));
    }

    @Test
    void stopsAtPunctuationAndCjk() {
        assertEquals(List.of("alice"), Mentions.extractUsernames("@alice，你好 (@alice)"));
        assertEquals(List.of("tom"), Mentions.extractUsernames("@tom你好"));
    }

    @Test
    void limitsNumberOfMentions() {
        StringBuilder content = new StringBuilder();
        for (int i = 0; i < 15; i++) {
            content.append("@user").append(i).append(' ');
        }
        assertEquals(Mentions.MAX_MENTIONS, Mentions.extractUsernames(content.toString()).size());
    }

    @Test
    void handlesNullAndPlainText() {
        assertTrue(Mentions.extractUsernames(null).isEmpty());
        assertTrue(Mentions.extractUsernames("没有提及").isEmpty());
    }
}
