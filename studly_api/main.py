from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from routers import study

app = FastAPI(
    title="Studly - Gestion de Temps AI",
    description="API IA pour la gestion de temps - module étude personnalisée",
    version="1.0.0"
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_methods=["*"],
    allow_headers=["*"],
)

app.include_router(study.router)

@app.get("/")
def root():
    return {
        "message": "Studly AI - Gestion de Temps API",
        "version": "1.0.0",
        "endpoints": {
            "learning_style_quiz": "/study/quiz/learning-style/questions",
            "analyze_style": "/study/quiz/learning-style/analyze",
            "level_question": "/study/quiz/level/question",
            "estimate_level": "/study/quiz/level/estimate",
            "generate_plan": "/study/plan/generate"
        }
    }

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="127.0.0.1", port=8000)
