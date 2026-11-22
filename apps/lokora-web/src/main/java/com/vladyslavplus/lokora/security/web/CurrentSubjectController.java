package com.vladyslavplus.lokora.security.web;

import com.vladyslavplus.lokora.web.annotation.LokoraRestController;
import org.springframework.web.bind.annotation.GetMapping;

@LokoraRestController("/api")
public class CurrentSubjectController {

    @GetMapping("/me")
    public CurrentSubjectResponse currentSubject(@CurrentSubject String subject) {
        return new CurrentSubjectResponse(subject);
    }
}
