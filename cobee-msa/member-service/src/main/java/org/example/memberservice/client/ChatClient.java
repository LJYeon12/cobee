package org.example.memberservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;

//@FeignClient(name = "CHAT_SERVICE")
public interface ChatClient {
    @DeleteMapping("/chat/{memberId}")
    void deleteAllChatDataByMemberId(@PathVariable("memberId") Long memberId);

}
