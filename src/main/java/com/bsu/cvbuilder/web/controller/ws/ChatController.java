package com.bsu.cvbuilder.web.controller.ws;

import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ResponseBody;

@Slf4j
@Controller
public class ChatController {



    @MessageMapping("/chat")
    @SendTo("/topic/messages")
    @ResponseBody
    public String sendMessage(String message) {
        log.info("Sending chat message: " + message);
        return "From backend" + message;
    }
}
