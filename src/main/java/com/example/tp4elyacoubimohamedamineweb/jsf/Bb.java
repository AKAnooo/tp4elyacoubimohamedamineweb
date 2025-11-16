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


    @Inject
    private FacesContext facesContext;

    // ➜ Injection du client LLM
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
            // ➜ Envoi de la question au LLM (la mémoire de chat est gérée dans LlmClient)
            String answer = llmClient.chat(this.question);
            this.reponse = (answer == null || answer.isBlank()) ? "(Réponse vide du LLM)" : answer;

            afficherConversation();
            return null; // rester sur la même page
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
        if (nettoye.length() <= 1) return false; // ignorer les lettres seules
        return new StringBuilder(nettoye).reverse().toString().equals(nettoye);
    }

    public String nouveauChat() {
        return "index";
    }

    private void afficherConversation() {
        this.conversation.append("== User:\n").append(question).append("\n== Serveur:\n").append(reponse).append("\n");
    }

    public List<SelectItem> getRolesSysteme() {
        if (this.listeRolesSysteme == null) {
            // Génère les rôles de l'API prédéfinis
            this.listeRolesSysteme = new ArrayList<>();
            // Vous pouvez évidemment écrire ces rôles dans la langue que vous voulez.
            String role = """
                    You are a helpful assistant. You help the user to find the information they need.
                    If the user type a question, you answer it.
                    """;
            // 1er argument : la valeur du rôle, 2ème argument : le libellé du rôle
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
