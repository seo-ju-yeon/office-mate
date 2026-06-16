package office_mate_2605.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AiConfig {
    @Bean
    public ChatClient chatClient(OpenAiChatModel chatModel) {
        return ChatClient.create(chatModel);
    }
}
