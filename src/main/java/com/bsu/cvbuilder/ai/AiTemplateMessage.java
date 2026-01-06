package com.bsu.cvbuilder.ai;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AiTemplateMessage {

    PROMPT_EXPANSION(
            """
            Rephrase the user's message for a professional CV Assistant. 
            If it's a greeting, make it a request to start. 
            Original message: "{question}"
            Rephrased (Output only the result):
            """
    ),

    SYSTEM_INTERVIEWER(
            """
            You are a Professional CV Assistant. 
            Balance: {points} points. 
            Costs: Generation {gen_cost}, Regeneration {regen_cost}.
            Context: {context}
            
            Goal: Collect user data (Name, Skills, Job, etc.) to build a CV.
            Be professional and speak Russian.
            """
    );

    private final String message;
}