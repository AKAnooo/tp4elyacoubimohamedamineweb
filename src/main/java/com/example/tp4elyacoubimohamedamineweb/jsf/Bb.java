package com.example.tp4elyacoubimohamedamineweb.jsf;

import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.model.SelectItem;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.example.tp4elyacoubimohamedamineweb.llm.LlmClient;

@Named
@ViewScoped
public class Bb implements Serializable {

    private String roleSysteme;
    private boolean roleSystemeChangeable = true;

    private List<SelectItem> listeRolesSysteme;

    private String question;
    private String reponse;
    private StringBuilder conversation = new StringBuilder();

    // true = Test 5 (RAG + Web), false = Test 3 (routage 2 PDF)
    private boolean utiliserTest5;

    @Inject
    private FacesContext facesContext;

    @Inject
    private LlmClient llmClient;

    public Bb() {
    }

    public String getRoleSysteme() {
        return roleSysteme;
    }

    public void setRoleSysteme(String roleSysteme) {
        this.roleSysteme = roleSysteme;
    }

    public boolean isRoleSystemeChangeable() {
        return roleSystemeChangeable;
    }

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public String getReponse() {
        return reponse;
    }

    public void setReponse(String reponse) {
        this.reponse = reponse;
    }

    public String getConversation() {
        return conversation.toString();
    }

    public void setConversation(String conversation) {
        this.conversation = new StringBuilder(conversation);
    }

    public boolean isUtiliserTest5() {
        return utiliserTest5;
    }

    public void setUtiliserTest5(boolean utiliserTest5) {
        this.utiliserTest5 = utiliserTest5;
    }

    public String envoyer() {
        if (question == null || question.isBlank()) {
            FacesMessage message = new FacesMessage(
                    FacesMessage.SEVERITY_ERROR,
                    "Texte question vide",
                    "Il manque le texte de la question");
            facesContext.addMessage(null, message);
            return null;
        }

        // Au tout premier envoi : rôle par défaut + verrouillage
        if (this.conversation.isEmpty()) {
            if (this.roleSysteme == null || this.roleSysteme.isBlank()) {
                this.roleSysteme = "You are a helpful assistant. Answer clearly and concisely.";
            }
            this.roleSystemeChangeable = false;
        }

        try {
            String answer;
            boolean premierMessage = this.conversation.isEmpty();

            if (premierMessage) {
                if (utiliserTest5) {
                    // Test 5 : rag.pdf + Web Tavily
                    answer = llmClient.chatTest5(this.roleSysteme, this.question);
                } else {
                    // Test 3 : routage entre les 2 PDF
                    answer = llmClient.chatTest3(this.roleSysteme, this.question);
                }
            } else {
                if (utiliserTest5) {
                    answer = llmClient.chatTest5(this.question);
                } else {
                    answer = llmClient.chatTest3(this.question);
                }
            }

            this.reponse = (answer == null || answer.isBlank())
                    ? "(Réponse vide du LLM)"
                    : answer;

            afficherConversation();
            return null;
        } catch (Exception e) {
            FacesMessage message = new FacesMessage(
                    FacesMessage.SEVERITY_ERROR,
                    "Problème de connexion avec l'API du LLM",
                    "Impossible d'obtenir une réponse : " + e.getMessage());
            facesContext.addMessage(null, message);
            return null;
        }
    }

    private boolean estPalindromeMot(String mot) {
        String nettoye = mot.toLowerCase(Locale.FRENCH)
                .replaceAll("[^a-z0-9àâäéèêëîïôöùûüÿç]", "");
        if (nettoye.length() <= 1) return false;
        return new StringBuilder(nettoye).reverse().toString().equals(nettoye);
    }

    public String nouveauChat() {
        return "index";
    }

    private void afficherConversation() {
        this.conversation
                .append(utiliserTest5 ? "[Test 5] " : "[Test 3] ")
                .append("== User:\n").append(question)
                .append("\n== Serveur:\n").append(reponse)
                .append("\n");
    }

    public List<SelectItem> getRolesSysteme() {
        if (this.listeRolesSysteme == null) {
            this.listeRolesSysteme = new ArrayList<>();

            String role = """
                    You are a helpful assistant. You help the user to find the information they need.
                    If the user type a question, you answer it.
                    """;
            this.listeRolesSysteme.add(new SelectItem(role, "Assistant"));

            role = """
                    You are an interpreter. You translate from English to French and from French to English.
                    If the user type a French text, you translate it into English.
                    If the user type an English text, you translate it into French.
                    If the text contains only one to three words, give some examples of usage of these words in English.
                    """;
            this.listeRolesSysteme.add(new SelectItem(role, "Traducteur Anglais-Français"));

            role = """
                    Your are a travel guide. If the user type the name of a country or of a town,
                    you tell them what are the main places to visit in the country or the town
                    are you tell them the average price of a meal.
                    """;
            this.listeRolesSysteme.add(new SelectItem(role, "Guide touristique"));
        }

        return this.listeRolesSysteme;
    }

}
