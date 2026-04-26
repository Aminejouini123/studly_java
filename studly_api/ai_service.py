import json
from dotenv import load_dotenv
import os
from groq import Groq

load_dotenv()

api_key = os.getenv("GROQ_API_KEY")
if not api_key:
    raise ValueError("GROQ_API_KEY not found in .env file")

client = Groq(api_key=api_key)
MODEL = "llama-3.3-70b-versatile"


def parse_json(response):
    """Parse JSON from Groq response, handling markdown and noise"""
    content = response.choices[0].message.content.strip()
    
    if "```" in content:
        try:
            content = content.split("```")[1]
            if content.startswith("json"):
                content = content[4:]
        except IndexError:
            pass

    content = content.strip()
    
    start = content.find('{')
    end = content.rfind('}')
    if start != -1 and end != -1:
        content = content[start:end+1]
        
    try:
        return json.loads(content)
    except json.JSONDecodeError as e:
        print(f"Failed to parse JSON: {content}")
        raise e


# ============ LEARNING STYLE QUIZ ============

LEARNING_STYLE_QUESTIONS = [
    {
        "id": "q1",
        "question": "Quand tu apprends quelque chose de nouveau, tu préfères :",
        "options": [
            "A) Voir des schémas, diagrammes ou vidéos",
            "B) Écouter une explication ou en discuter",
            "C) Lire un texte et prendre des notes",
            "D) Essayer directement par la pratique"
        ]
    },
    {
        "id": "q2",
        "question": "Pour mémoriser une formule, tu :",
        "options": [
            "A) Fais une carte mentale colorée",
            "B) Te la répètes à voix haute",
            "C) L'écris plusieurs fois dans un cahier",
            "D) L'appliques dans des exercices"
        ]
    },
    {
        "id": "q3",
        "question": "En cours, tu retiens mieux quand le professeur :",
        "options": [
            "A) Dessine ou projette des images",
            "B) Explique clairement avec sa voix",
            "C) Donne un document écrit à lire",
            "D) Fait faire une activité pratique"
        ]
    },
    {
        "id": "q4",
        "question": "Quand tu révises avant un examen, tu :",
        "options": [
            "A) Regardes tes schémas et surlignages",
            "B) Récites le cours à voix haute",
            "C) Relis tes notes et résumés",
            "D) Refais des exercices et annales"
        ]
    },
    {
        "id": "q5",
        "question": "Quand tu es bloqué sur un exercice, tu :",
        "options": [
            "A) Cherches un exemple visuel ou schéma",
            "B) Demandes une explication orale",
            "C) Relis l'énoncé ou le cours",
            "D) Essaies différentes approches"
        ]
    }
]


def get_learning_style_questions():
    """Return the 5 learning style quiz questions"""
    return LEARNING_STYLE_QUESTIONS


def analyze_learning_style(answers: list) -> dict:
    """Analyze quiz answers and determine learning style"""
    answers_text = "\n".join([
        f"Q{a['question_id']} → {a['answer']}" for a in answers
    ])
    
    prompt = f"""
Tu es expert en psychologie de l'apprentissage et en pédagogie.

Réponses de l'étudiant au quiz de style d'apprentissage :
{answers_text}

Légende : A=Visuel, B=Auditif, C=Lecture/Écriture, D=Pratique

Analyse les réponses et détermine le style d'apprentissage dominant.

Réponds UNIQUEMENT en JSON valide sans texte autour :
{{
    "style": "visuel|auditif|lecture_ecriture|pratique",
    "description": "phrase courte décrivant ce style d'apprentissage",
    "conseil": "conseil personnalisé de 1-2 phrases pour optimiser cet apprentissage"
}}
"""
    response = client.chat.completions.create(
        model=MODEL,
        max_tokens=300,
        messages=[{"role": "user", "content": prompt}]
    )
    return parse_json(response)


# ============ ADAPTIVE LEVEL QUESTIONS ============

def generate_level_question(subject: str, difficulty: str, previous_correct: bool = None) -> dict:
    """
    Generate an adaptive question for level assessment.
    difficulty: 'facile' | 'moyenne' | 'difficile'
    previous_correct: True if last answer was correct, False if incorrect, None if first question
    """
    adaptation_hint = ""
    if previous_correct is True:
        adaptation_hint = "\n⚠️ L'étudiant a réussi la dernière question. Augmente légèrement la difficulté."
    elif previous_correct is False:
        adaptation_hint = "\n⚠️ L'étudiant a échoué la dernière question. Diminue légèrement la difficulté."
    
    prompt = f"""
Tu es professeur expert en {subject}.
Génère UNE question de niveau {difficulty} pour évaluer le niveau d'un étudiant.{adaptation_hint}

La question doit être claire, pertinente et évaluer la compréhension réelle.

Réponds UNIQUEMENT en JSON valide :
{{
    "id": "lq1",
    "question": "ta question",
    "options": ["A) option 1", "B) option 2", "C) option 3", "D) option 4"],
    "correct_answer": "A|B|C|D",
    "explanation": "explication courte de la bonne réponse",
    "difficulty": "{difficulty}"
}}
"""
    response = client.chat.completions.create(
        model=MODEL,
        max_tokens=400,
        messages=[{"role": "user", "content": prompt}]
    )
    return parse_json(response)


def estimate_level(answers: list) -> dict:
    """Estimate student level based on adaptive test answers"""
    correct_count = sum(1 for a in answers if a['user_answer'] == a['correct_answer'])
    total_count = len(answers)
    score = (correct_count / total_count * 100) if total_count > 0 else 0
    
    # Determine level based on score
    if score >= 80:
        level = "avancé"
    elif score >= 50:
        level = "intermédiaire"
    else:
        level = "débutant"
    
    answers_text = "\n".join([
        f"Difficulté: {a['difficulty']} | {'✅' if a['user_answer'] == a['correct_answer'] else '❌'}"
        for a in answers
    ])
    
    prompt = f"""
Résultats du test adaptatif :
{answers_text}

Score : {score:.0f}%
Niveau estimé : {level}

Fournis un commentaire bienveillant et encourageant sur la performance.

Réponds UNIQUEMENT en JSON valide :
{{
    "level": "{level}",
    "score_estime": {score:.0f},
    "commentaire": "évaluation bienveillante en 1-2 phrases"
}}
"""
    response = client.chat.completions.create(
        model=MODEL,
        max_tokens=200,
        messages=[{"role": "user", "content": prompt}]
    )
    return parse_json(response)


# ============ PERSONALIZED STUDY PLAN ============

def generate_study_plan(subject: str, learning_style: str, level: str, duration_minutes: int) -> dict:
    """
    Generate a personalized 4-step study plan.
    Steps: 1) Understand concept, 2) See example, 3) Practice, 4) Test understanding
    """
    
    # Adapt steps based on learning style
    style_descriptions = {
        "visuel": "schémas, diagrammes, cartes mentales, vidéos, infographies",
        "auditif": "explications orales, discussions, résumés audio, podcasts",
        "lecture_ecriture": "textes, résumés écrits, prise de notes, lectures",
        "pratique": "exercices, expériences, applications concrètes, projets"
    }
    
    style_desc = style_descriptions.get(learning_style, "ressources adaptées")
    
    # Calculate optimal durations (4 steps)
    step1_duration = max(int(duration_minutes * 0.20), 3)  # 20% - Understand
    step2_duration = max(int(duration_minutes * 0.20), 3)  # 20% - See example
    step3_duration = max(int(duration_minutes * 0.40), 5)  # 40% - Practice
    step4_duration = max(duration_minutes - step1_duration - step2_duration - step3_duration, 3)  # Rest - Test
    
    prompt = f"""
Tu es un coach pédagogique expert en personnalisation d'apprentissage.

Profil étudiant :
- Matière : {subject}
- Style d'apprentissage : {learning_style} (préfère : {style_desc})
- Niveau : {level}
- Durée disponible : {duration_minutes} minutes

Génère EXACTEMENT 4 étapes pour un plan d'étude optimal.
Les durées DOIVENT être : {step1_duration}min + {step2_duration}min + {step3_duration}min + {step4_duration}min = {duration_minutes}min

Les 4 étapes DOIVENT être :
1. Comprendre le concept ({step1_duration}min)
2. Voir un exemple ({step2_duration}min)
3. Pratiquer ({step3_duration}min)
4. Tester ta compréhension ({step4_duration}min)

Adapte chaque étape au style {learning_style} et au niveau {level}.
Sois spécifique et actionnable.

Réponds UNIQUEMENT en JSON valide :
{{
    "steps": [
        {{"numero": 1, "action": "Comprendre le concept - [détail spécifique pour {learning_style}]", "duree_minutes": {step1_duration}}},
        {{"numero": 2, "action": "Voir un exemple - [détail spécifique pour {learning_style}]", "duree_minutes": {step2_duration}}},
        {{"numero": 3, "action": "Pratiquer - [détail spécifique pour {learning_style}]", "duree_minutes": {step3_duration}}},
        {{"numero": 4, "action": "Tester ta compréhension - [détail spécifique pour {learning_style}]", "duree_minutes": {step4_duration}}}
    ],
    "message_motivationnel": "Choisis parmi : 'Follow your dreams', 'Believe in yourself', 'Never Give Up' ou crée un message court et motivant (max 10 mots)"
}}
"""
    response = client.chat.completions.create(
        model=MODEL,
        max_tokens=900,
        messages=[{"role": "user", "content": prompt}]
    )
    return parse_json(response)
