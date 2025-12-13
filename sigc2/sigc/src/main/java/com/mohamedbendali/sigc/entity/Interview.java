package com.mohamedbendali.sigc.entity;

import com.mohamedbendali.sigc.enums.InterviewStatus;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "interviews")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Interview {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // @ManyToOne(fetch = FetchType.LAZY) // L'exemple du chap 2 le met
    // @JoinColumn(name = "application_id")
    // private JobApplication application;

    // MAIS le diagramme du chap 2 dit JobApplication -> Interview (1..*) et le détail chap 5 dit Interview -> JobApplication (1-1).
    // Une relation 1-1 est plus logique si un entretien est unique pour une candidature (ou étape).
    // Si plusieurs entretiens par candidature, alors ManyToOne est correct. Je garde ManyToOne pour plus de flexibilité.
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "application_id", nullable = false)
    private JobApplication application;


    @OneToMany(mappedBy = "interview", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<ChatMessage> chatMessages = new ArrayList<>(); // Renommé de chatHistory (chap 5) pour correspondre à l'exemple

    private LocalDateTime startTime;
    private LocalDateTime endTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InterviewStatus status = InterviewStatus.SCHEDULED; // Valeur par défaut

    private Double aiEvaluationScore; // Ajouté du détail chapitre 5
    @Lob
    private String aiFeedback; // Ajouté pour stocker le feedback IA

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;
}