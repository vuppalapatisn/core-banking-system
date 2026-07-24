package com.amol.microservices.ai.service;

/**
 * The GenAI/LLM boundary. The default implementation is deterministic and offline; a production
 * implementation calls a real LLM (e.g. Claude) — swapping it in requires no change to callers.
 */
public interface AssistantModel {

    String answer(String question, String context);

    /** Identifier of the backing model (surfaced in responses for traceability). */
    String name();
}
