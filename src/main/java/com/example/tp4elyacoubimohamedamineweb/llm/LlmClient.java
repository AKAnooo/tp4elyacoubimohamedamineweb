package com.example.tp4elyacoubimohamedamineweb.llm;

import jakarta.enterprise.context.Dependent;

import java.io.Serializable;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.loader.FileSystemDocumentLoader;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.segment.TextSegment;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.onnx.allminilml6v2.AllMiniLmL6V2EmbeddingModel;

import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;

import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.rag.DefaultRetrievalAugmentor;
import dev.langchain4j.rag.RetrievalAugmentor;
import dev.langchain4j.rag.query.router.LanguageModelQueryRouter;

import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import dev.langchain4j.service.AiServices;

@Dependent
public class LlmClient implements Serializable {

    private String systemRole;
    private final GoogleAiGeminiChatModel model;
    private final ChatMemory chatMemory;

    private final Assistant assistant;

    public LlmClient() {
        String apiKey = System.getenv("GEMINI_KEY");


        this.model = GoogleAiGeminiChatModel.builder()
                .apiKey(apiKey)
                .modelName("gemini-2.5-flash")
                .build();

        this.chatMemory = MessageWindowChatMemory.withMaxMessages(10);

        EmbeddingModel embeddingModel = new AllMiniLmL6V2EmbeddingModel();
        var splitter = DocumentSplitters.recursive(500, 50);

        Path pdf1 = loadFromResources("rag.pdf");
        Document doc1 = FileSystemDocumentLoader.loadDocument(pdf1);
        List<TextSegment> segments1 = splitter.split(doc1);
        List<Embedding> vectors1 = embeddingModel.embedAll(segments1).content();
        EmbeddingStore<TextSegment> store1 = new InMemoryEmbeddingStore<>();
        store1.addAll(vectors1, segments1);

        Path pdf2 = loadFromResources("QCM_MAD-AI_COMPLET.pdf");
        Document doc2 = FileSystemDocumentLoader.loadDocument(pdf2);
        List<TextSegment> segments2 = splitter.split(doc2);
        List<Embedding> vectors2 = embeddingModel.embedAll(segments2).content();
        EmbeddingStore<TextSegment> store2 = new InMemoryEmbeddingStore<>();
        store2.addAll(vectors2, segments2);

        ContentRetriever retriever1 = EmbeddingStoreContentRetriever.builder()
                .embeddingModel(embeddingModel)
                .embeddingStore(store1)
                .maxResults(5)
                .build();

        ContentRetriever retriever2 = EmbeddingStoreContentRetriever.builder()
                .embeddingModel(embeddingModel)
                .embeddingStore(store2)
                .maxResults(5)
                .build();

        Map<ContentRetriever, String> descriptions = new LinkedHashMap<>();

        descriptions.put(retriever1, """
                Source 1 : support de cours sur le RAG (Retrieval Augmented Generation),
                les LLM, les embeddings et l'intelligence artificielle.
                À utiliser uniquement si la question parle de RAG, de LLM, d'IA,
                d'embeddings, de pipeline RAG, etc.
                Ne pas utiliser pour les questions sur Android ou le développement mobile.
                """);

        descriptions.put(retriever2, """
                Source 2 : QCM et questions de cours sur Android et/ou un autre chapitre spécifique.
                À utiliser si la question parle d'Android, d'applications mobiles,
                d'activities, d'intents, de layouts, de fragments ou de QCM/examens liés à ce cours.
                Ne pas utiliser pour les questions sur le RAG ou l'IA en général.
                """);


        LanguageModelQueryRouter router = new LanguageModelQueryRouter(model, descriptions);

        RetrievalAugmentor retrievalAugmentor = DefaultRetrievalAugmentor.builder()
                .queryRouter(router)
                .build();

        this.assistant = AiServices.builder(Assistant.class)
                .chatModel(model)
                .chatMemory(chatMemory)
                .retrievalAugmentor(retrievalAugmentor)
                .build();
    }
    public String chat(String prompt) {
        if (prompt == null || prompt.isBlank()) return "";
        return assistant.chat(prompt.trim());
    }

    private Path loadFromResources(String resourceName) {
        URL url = Thread.currentThread().getContextClassLoader().getResource(resourceName);
        if (url == null) {
            throw new IllegalStateException("Ressource introuvable : " + resourceName);
        }
        try {
            return Path.of(url.toURI());
        } catch (URISyntaxException e) {
            throw new IllegalStateException("URI invalide pour la ressource : " + resourceName, e);
        }
    }

    public void setSystemRole(String systemRole) {
        this.systemRole = (systemRole == null || systemRole.isBlank())
                ? "You are a helpful assistant."
                : systemRole;

        chatMemory.clear();
        chatMemory.add(SystemMessage.from(this.systemRole));
    }


}
