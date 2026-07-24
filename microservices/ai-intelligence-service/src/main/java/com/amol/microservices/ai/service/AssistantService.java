package com.amol.microservices.ai.service;

import com.amol.microservices.ai.model.AssistantRequest;
import org.springframework.stereotype.Service;

/** GenAI assistant orchestration over the pluggable {@link AssistantModel}. */
@Service
public class AssistantService {

    public record Result(String answer, String model) {
    }

    private final AssistantModel model;

    public AssistantService(AssistantModel model) {
        this.model = model;
    }

    public Result ask(AssistantRequest request) {
        String answer = model.answer(request.question(), request.context());
        return new Result(answer, model.name());
    }
}
