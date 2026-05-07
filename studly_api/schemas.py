from pydantic import BaseModel
from typing import List, Optional

class EventInfo(BaseModel):
    title: str
    subject: str
    duration_minutes: int
    date: str

class QuizAnswer(BaseModel):
    question_id: str
    answer: str  # "A", "B", "C" ou "D"

class LearningStyleRequest(BaseModel):
    event_info: EventInfo
    answers: List[QuizAnswer]

class LevelAnswer(BaseModel):
    question_id: str
    user_answer: str
    correct_answer: str
    difficulty: str  # "facile" | "moyenne" | "difficile"

class LevelRequest(BaseModel):
    event_info: EventInfo
    answers: List[LevelAnswer]

class StudyPlanRequest(BaseModel):
    event_info: EventInfo
    learning_style: str
    level: str
