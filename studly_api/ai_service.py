import json
from dotenv import load_dotenv
import os
from groq import Groq

load_dotenv(dotenv_path=r"C:\Users\ghali\studly_java\studly_api\.env")

api_key = os.getenv("GROQ_API_KEY")
print(f"Clé Groq chargée : {api_key[:10] if api_key else 'NON TROUVÉE'}")

client = Groq(api_key=api_key)
MODEL = "llama-3.3-70b-versatile"


def parse_json(response):
    content = response.choices[0].message.content.strip()
    
    # Try to extract JSON from within potential backticks
    if "```" in content:
        try:
            content = content.split("```")[1]
            if content.startswith("json"):
                content = content[4:]
        except IndexError:
            pass # Fall through to literal parsing

    content = content.strip()
    
    # Attempt to find the first '{' and last '}' to handle conversational noise
    start = content.find('{')
    end = content.rfind('}')
    if start != -1 and end != -1:
        content = content[start:end+1]
        
    try:
        return json.loads(content)
    except json.JSONDecodeError as e:
        print(f"Failed to parse JSON: {content}")
        raise e


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
    return LEARNING_STYLE_QUESTIONS


def analyze_learning_style(answers: list) -> dict:
    answers_text = "\n".join([
        f"Q{a['question_id']} → {a['answer']}" for a in answers
    ])
    prompt = f"""
Tu es expert en psychologie de l'apprentissage.
Réponses de l'étudiant :
{answers_text}

Légende : A=Visuel, B=Auditif, C=Lecture/Écriture, D=Pratique

Réponds UNIQUEMENT en JSON valide sans texte autour :
{{
    "style": "visuel|auditif|lecture_ecriture|pratique",
    "description": "phrase courte décrivant ce style",
    "conseil": "conseil personnalisé de 1 phrase"
}}
"""
    response = client.chat.completions.create(
        model=MODEL,
        max_tokens=300,
        messages=[{"role": "user", "content": prompt}]
    )
    return parse_json(response)


def generate_level_question(subject: str, difficulty: str) -> dict:
    prompt = f"""
Tu es professeur expert en {subject}.
Génère UNE question de niveau {difficulty} pour évaluer un étudiant.

Réponds UNIQUEMENT en JSON valide :
{{
    "id": "lq1",
    "question": "ta question",
    "options": ["A) ...", "B) ...", "C) ...", "D) ..."],
    "correct_answer": "A|B|C|D",
    "explanation": "explication courte",
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
    answers_text = "\n".join([
        f"Difficulté: {a['difficulty']} | {'✅' if a['user_answer'] == a['correct_answer'] else '❌'}"
        for a in answers
    ])
    prompt = f"""
Résultats du test adaptatif :
{answers_text}

Estime le niveau : débutant, intermédiaire, ou avancé.

Réponds UNIQUEMENT en JSON valide :
{{
    "level": "débutant|intermédiaire|avancé",
    "score_estime": 0,
    "commentaire": "évaluation bienveillante en 1 phrase"
}}
"""
    response = client.chat.completions.create(
        model=MODEL,
        max_tokens=200,
        messages=[{"role": "user", "content": prompt}]
    )
    return parse_json(response)


def generate_study_plan(subject: str, learning_style: str, level: str, duration_minutes: int) -> dict:
    prompt = f"""
Tu es un coach pédagogique expert.

Profil étudiant :
- Matière : {subject}
- Style d'apprentissage : {learning_style}
- Niveau : {level}
- Durée disponible : {duration_minutes} minutes

Génère EXACTEMENT 4 étapes. La somme des durées = {duration_minutes} minutes.

Réponds UNIQUEMENT en JSON valide :
{{
    "steps": [
        {{"numero": 1, "action": "...", "duree_minutes": 0}},
        {{"numero": 2, "action": "...", "duree_minutes": 0}},
        {{"numero": 3, "action": "...", "duree_minutes": 0}},
        {{"numero": 4, "action": "...", "duree_minutes": 0}}
    ],
    "message_motivationnel": "message court et motivant"
}}
"""
    response = client.chat.completions.create(
        model=MODEL,
        max_tokens=600,
        messages=[{"role": "user", "content": prompt}]
    )
    return parse_json(response)