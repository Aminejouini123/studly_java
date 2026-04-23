from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from routers import study

app = FastAPI(
    title="Studly - Gestion de Temps AI",
    description="API IA pour la gestion de temps - module étude",
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
    return {"message": "Studly AI - Gestion de Temps API "}

