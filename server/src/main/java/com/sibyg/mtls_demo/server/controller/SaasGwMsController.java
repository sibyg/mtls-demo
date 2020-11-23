package com.sibyg.mtls_demo.server.controller;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class SaasGwMsController {

    @GetMapping(value = "/saas-gw-ms/api-1", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> hello() {
        return ResponseEntity.ok("SAAS-GW-MS/API-1");
    }

}
