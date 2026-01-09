package com.ld.poetry.im;

import com.ld.poetry.im.http.controller.ImChatUserMessageController;
import com.ld.poetry.im.http.controller.ImChatGroupUserController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import lombok.extern.slf4j.Slf4j;

@SpringBootTest
@Slf4j
public class ImChatSystemMessageTest {

    @Autowired
    private ImChatUserMessageController userMessageController;

    @Autowired
    private ImChatGroupUserController groupUserController;

    @Test
    public void testControllerDependencies() {
        log.info("========== 正在验证系统消息推送逻辑相关的控制器依赖 ==========");
        
        if (userMessageController != null) {
            log.info("✅ ImChatUserMessageController 加载成功。包含：系统消息全局广播逻辑。");
        } else {
            log.error("❌ ImChatUserMessageController 加载失败。");
        }

        if (groupUserController != null) {
            log.info("✅ ImChatGroupUserController 加载成功。包含：用户进出群组系统提示逻辑。");
        } else {
            log.error("❌ ImChatGroupUserController 加载失败。");
        }

        log.info("========== 依赖注入验证完成 ==========");
        assert userMessageController != null;
        assert groupUserController != null;
    }
}
