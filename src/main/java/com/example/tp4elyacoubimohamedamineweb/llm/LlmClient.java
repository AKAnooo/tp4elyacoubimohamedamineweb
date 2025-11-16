package com.example.tp4elyacoubimohamedamineweb.llm;


import jakarta.enterprise.context.Dependent;
import java.io.Serializable;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import dev.langchain4j.service.AiServices;

/**
 * Classe "métier" qui gère l'accès au LLM Gemini via LangChain4j.
 * Elle crée le modèle, la mémoire et l'assistant (proxy généré automatiquement).
 */
@Dependent
public class LlmClient implements Serializable {

    private String systemRole;
    private final ChatMemory chatMemory;
    private final GoogleAiGeminiChatModel model;
    private final Assistant assistant;

    public LlmClient() {
        String apiKey = System.getenv("GEMINI_KEY"); //
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("Variable d'environnement GEMINI_KEY manquante !");
        }

        // 1) Modèle Gemini (même que votre* exemple) (ici par exemple c'est parce que j'ai suivi votre exemple je viens de voir que je vous ai tutoyé)
        this.model = GoogleAiGeminiChatModel.builder()
                .apiKey(apiKey)
                .modelName("gemini-2.5-flash")
                .build();

        // 2) Mémoire : conserve les 10 derniers messages (ici, c'est clair)
        this.chatMemory = MessageWindowChatMemory.withMaxMessages(10);

        // 3) Assistant via AiServices (mêmes appels)
        this.assistant = AiServices.builder(Assistant.class)
                .chatModel(model)
                .chatMemory(chatMemory)
                .build();
    }

    /** Définit le rôle système et réinitialise la mémoire. */
    public void setSystemRole(String systemRole) {
        this.systemRole = (systemRole == null || systemRole.isBlank())
                ? "You are a helpful assistant."
                : systemRole;

        // selon ta version, clear()/add(...) existent bien sur chatMemory
        // si l'IDE râle, tape le champ en MessageWindowChatMemory au lieu de ChatMemory //Tout cela est tiré directement de mes recherches afin de vous donner le TP le plus complet possible.
        chatMemory.clear();
        chatMemory.add(SystemMessage.from(this.systemRole));
    }

    /** Envoie une requête au LLM (le rôle système est déjà défini). */
    public String chat(String prompt) {
        if (prompt == null || prompt.isBlank()) return "";
        return assistant.chat(prompt.trim());
    }

    /** Variante : définit le rôle et envoie la question en un seul appel. */ //Ici j'avais trouvé une variante que j'ai decidé de mettre ici.
    public String chat(String systemRole, String prompt) {
        setSystemRole(systemRole);
        return chat(prompt);
    }

    public String getSystemRole() {
        return systemRole;
    }
}

