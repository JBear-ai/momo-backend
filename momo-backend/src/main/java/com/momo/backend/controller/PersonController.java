package com.momo.backend.controller;

import com.momo.backend.dto.person.PersonDetailResponse;
import com.momo.backend.service.PersonService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/persons")
@RequiredArgsConstructor
public class PersonController {

    private final PersonService personService;

    @GetMapping("/{type}/{name}")
    public ResponseEntity<PersonDetailResponse> getPersonDetail(
            @PathVariable String type,
            @PathVariable String name
    ) {
        return ResponseEntity.ok(
                personService.getPersonDetail(type, name)
        );
    }
}