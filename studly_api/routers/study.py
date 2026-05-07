from fastapi import APIRouter
from schemas import (
    EventInfo, LearningStyleRequest,
    LevelRequest, StudyPlanRequest
)
import ai_service

router = APIRouter(prefix="/study", tags=["Gestion de Temps - IA"])


@router.get("/quiz/learning-style/questions")
def get_questions():
    """Retourne les 5 questions du quiz style d'apprentissage"""
    return {"questions": ai_service.get_learning_style_questions()}


@router.post("/quiz/learning-style/analyze")
def analyze_style(request: LearningStyleRequest):
    """Analyse les réponses → retourne le style d'apprentissage"""
    answers = [{"question_id": a.question_id, "answer": a.answer}
               for a in request.answers]
    result = ai_service.analyze_learning_style(answers)
    return {
        "event": request.event_info.title,
        "style": result["style"],
        "description": result["description"],
        "conseil": result["conseil"]
    }


@router.get("/quiz/level/question")
def get_level_question(subject: str, difficulty: str = "moyenne", previous_correct: bool = None):
    """
    Génère une question adaptive.
    difficulty : 'facile' | 'moyenne' | 'difficile'
    previous_correct : true si bonne réponse, false si mauvaise, null si première question
    """
    return ai_service.generate_level_question(subject, difficulty, previous_correct)


@router.post("/quiz/level/estimate")
def estimate_level(request: LevelRequest):
    """Estime le niveau de l'étudiant selon ses réponses"""
    answers = [
        {
            "difficulty": a.difficulty,
            "user_answer": a.user_answer,
            "correct_answer": a.correct_answer
        }
        for a in request.answers
    ]
    result = ai_service.estimate_level(answers)
    return {
        "subject": request.event_info.subject,
        "level": result["level"],
        "score": result["score_estime"],
        "commentaire": result["commentaire"]
    }


@router.post("/plan/generate")
def generate_plan(request: StudyPlanRequest):
    """Génère un plan d'étude personnalisé en 4 étapes"""
    result = ai_service.generate_study_plan(
        subject=request.event_info.subject,
        learning_style=request.learning_style,
        level=request.level,
        duration_minutes=request.event_info.duration_minutes
    )
    return {
        "titre": f"Plan d'étude — {request.event_info.subject}",
        "style_apprentissage": request.learning_style,
        "niveau": request.level,
        "duree_totale": request.event_info.duration_minutes,
        "etapes": result["steps"],
        "message": result["message_motivationnel"]
    }
