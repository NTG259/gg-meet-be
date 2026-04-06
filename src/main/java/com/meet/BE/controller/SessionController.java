package com.meet.BE.controller;

import io.openvidu.java.client.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/sessions")
@CrossOrigin("*")
public class SessionController {

    private final OpenVidu openVidu;

    public SessionController(OpenVidu openVidu) {
        this.openVidu = openVidu;
    }

    @PostMapping("/{room}")
    public String createSession(@PathVariable String room) throws Exception {

        SessionProperties properties = new SessionProperties.Builder()
                .customSessionId(room)
                .build();

        Session session = openVidu.createSession(properties);

        return session.getSessionId();
    }

    @PostMapping("/{room}/connections")
    public String createConnection(@PathVariable String room) throws Exception {
        // validate room ?
        // search exists room
        // -> waiting
        // -> join

        Session session = openVidu.getActiveSession(room);

        if (session == null) {
            session = openVidu.createSession(
                    new SessionProperties.Builder()
                            .customSessionId(room)
                            .build()
            );
        }

        Connection connection = session.createConnection();

        return connection.getToken();
    }
}