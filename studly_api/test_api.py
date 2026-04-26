#!/usr/bin/env python3
"""
Script de test pour vérifier que l'API Studly fonctionne correctement
"""

import requests
import json
import sys

API_BASE = "http://127.0.0.1:8000"

def test_api_health():
    """Test 1: Vérifier que l'API répond"""
    print("Test 1: Vérification de l'API...")
    try:
        response = requests.get(f"{API_BASE}/", timeout=5)
        if response.status_code == 200:
            print("✅ API accessible")
            return True
        else:
            print(f"❌ API retourne le code {response.status_code}")
            return False
    except requests.exceptions.ConnectionError:
        print("❌ Impossible de se connecter à l'API")
        print("   Assurez-vous que l'API est démarrée (python main.py)")
        return False
    except Exception as e:
        print(f"❌ Erreur: {e}")
        return False

def test_learning_style_questions():
    """Test 2: Récupérer les questions du quiz"""
    print("\nTest 2: Récupération des questions du quiz...")
    try:
        response = requests.get(
            f"{API_BASE}/study/quiz/learning-style/questions",
            timeout=5
        )
        if response.status_code == 200:
            data = response.json()
            questions = data.get("questions", [])
            if len(questions) == 5:
                print(f"✅ {len(questions)} questions récupérées")
                return True
            else:
                print(f"❌ Nombre de questions incorrect: {len(questions)}")
                return False
        else:
            print(f"❌ Code de réponse: {response.status_code}")
            return False
    except Exception as e:
        print(f"❌ Erreur: {e}")
        return False

def test_analyze_learning_style():
    """Test 3: Analyser le style d'apprentissage"""
    print("\nTest 3: Analyse du style d'apprentissage...")
    try:
        payload = {
            "event_info": {
                "title": "Test Math",
                "subject": "Math",
                "duration_minutes": 60,
                "date": "2026-04-25"
            },
            "answers": [
                {"question_id": "q1", "answer": "A"},
                {"question_id": "q2", "answer": "A"},
                {"question_id": "q3", "answer": "A"},
                {"question_id": "q4", "answer": "A"},
                {"question_id": "q5", "answer": "A"}
            ]
        }
        response = requests.post(
            f"{API_BASE}/study/quiz/learning-style/analyze",
            json=payload,
            timeout=15
        )
        if response.status_code == 200:
            data = response.json()
            style = data.get("style", "")
            if style in ["visuel", "auditif", "lecture_ecriture", "pratique"]:
                print(f"✅ Style détecté: {style}")
                return True
            else:
                print(f"❌ Style invalide: {style}")
                return False
        else:
            print(f"❌ Code de réponse: {response.status_code}")
            print(f"   Réponse: {response.text}")
            return False
    except Exception as e:
        print(f"❌ Erreur: {e}")
        return False

def test_generate_level_question():
    """Test 4: Générer une question de niveau"""
    print("\nTest 4: Génération d'une question de niveau...")
    try:
        response = requests.get(
            f"{API_BASE}/study/quiz/level/question",
            params={"subject": "Math", "difficulty": "moyenne"},
            timeout=15
        )
        if response.status_code == 200:
            data = response.json()
            if "question" in data and "options" in data:
                print("✅ Question générée avec succès")
                return True
            else:
                print("❌ Format de réponse invalide")
                return False
        else:
            print(f"❌ Code de réponse: {response.status_code}")
            return False
    except Exception as e:
        print(f"❌ Erreur: {e}")
        return False

def test_generate_study_plan():
    """Test 5: Générer un plan d'étude"""
    print("\nTest 5: Génération d'un plan d'étude...")
    try:
        payload = {
            "event_info": {
                "title": "Test Math",
                "subject": "Math",
                "duration_minutes": 60,
                "date": "2026-04-25"
            },
            "learning_style": "visuel",
            "level": "intermédiaire"
        }
        response = requests.post(
            f"{API_BASE}/study/plan/generate",
            json=payload,
            timeout=20
        )
        if response.status_code == 200:
            data = response.json()
            etapes = data.get("etapes", [])
            if len(etapes) == 4:
                print(f"✅ Plan généré avec {len(etapes)} étapes")
                total_duration = sum(e.get("duree_minutes", 0) for e in etapes)
                print(f"   Durée totale: {total_duration} minutes")
                return True
            else:
                print(f"❌ Nombre d'étapes incorrect: {len(etapes)}")
                return False
        else:
            print(f"❌ Code de réponse: {response.status_code}")
            print(f"   Réponse: {response.text}")
            return False
    except Exception as e:
        print(f"❌ Erreur: {e}")
        return False

def main():
    """Exécuter tous les tests"""
    print("=" * 60)
    print("  Tests de l'API Studly")
    print("=" * 60)
    
    tests = [
        test_api_health,
        test_learning_style_questions,
        test_analyze_learning_style,
        test_generate_level_question,
        test_generate_study_plan
    ]
    
    results = []
    for test in tests:
        results.append(test())
    
    print("\n" + "=" * 60)
    print("  Résultats")
    print("=" * 60)
    
    passed = sum(results)
    total = len(results)
    
    print(f"\nTests réussis: {passed}/{total}")
    
    if passed == total:
        print("\n✅ Tous les tests sont passés!")
        print("L'API fonctionne correctement.")
        return 0
    else:
        print(f"\n❌ {total - passed} test(s) échoué(s)")
        print("Vérifiez la configuration de l'API.")
        return 1

if __name__ == "__main__":
    sys.exit(main())
