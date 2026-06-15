# 🚀 PFE — Plateforme Extraction Documents IA
## Guide de démarrage rapide

---

## 📋 Prérequis à installer

| Outil | Version | Téléchargement |
|-------|---------|----------------|
| Java JDK | 17+ | https://adoptium.net |
| Maven | 3.8+ | https://maven.apache.org |
| PostgreSQL | 15+ | https://www.postgresql.org |
| IntelliJ IDEA | Community | https://www.jetbrains.com/idea |

---

## 🗄️ Étape 1 — Créer la base de données PostgreSQL

Ouvrir pgAdmin ou psql et exécuter :

```sql
-- Créer la base de données
CREATE DATABASE doc_extraction_db;

-- Créer un utilisateur dédié (remplacer le mot de passe !)
CREATE USER doc_user WITH PASSWORD 'monMotDePasse123';

-- Donner les droits sur la base
GRANT ALL PRIVILEGES ON DATABASE doc_extraction_db TO doc_user;
```

---

## ⚙️ Étape 2 — Configurer l'application

Modifier `src/main/resources/application.properties` :

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/doc_extraction_db
spring.datasource.username=doc_user
spring.datasource.password=monMotDePasse123
```

---

## ▶️ Étape 3 — Lancer l'application

```bash
# Compiler et lancer
mvn spring-boot:run

# OU depuis IntelliJ : clic droit sur DocExtractionApplication → Run
```

**Flyway crée automatiquement toutes les tables au démarrage !**

---

## ✅ Étape 4 — Vérifier que tout fonctionne

1. Ouvrir `http://localhost:8080/swagger-ui.html`
2. Vérifier dans pgAdmin que les tables ont été créées
3. Tester l'endpoint de santé : `GET http://localhost:8080/actuator/health`

---

## 👤 Comptes par défaut (créés par V3__insert_initial_data.sql)

| Email | Mot de passe | Rôle |
|-------|-------------|------|
| admin@docextraction.com | Admin@1234 | ADMIN |
| operateur@docextraction.com | Test@1234 | OPERATEUR |

⚠️ **Changer ces mots de passe immédiatement en production !**

---

## 🐛 Erreurs fréquentes et solutions

### Erreur 500 dans Postman
- Vérifier les logs dans la console IntelliJ
- Cause fréquente : problème de connexion BDD → vérifier application.properties
- Cause fréquente : champ manquant dans la requête → vérifier le JSON envoyé

### "Unable to acquire JDBC Connection"
- PostgreSQL n'est pas démarré → lancer le service PostgreSQL

### "relation does not exist"
- Flyway n'a pas créé les tables → vérifier que spring.flyway.enabled=true

### "password authentication failed"
- Mauvais username/password dans application.properties

---

## 📁 Structure du projet

```
src/main/java/com/pfe/docextraction/
├── DocExtractionApplication.java   ← Point d'entrée
├── config/                         ← Configurations Spring
├── entity/                         ← Tables JPA (User, Document, Extraction...)
├── enums/                          ← Types (UserRole, DocumentStatus...)
├── repository/                     ← Accès base de données
├── service/                        ← Logique métier
├── controller/                     ← Endpoints REST API
├── security/                       ← JWT + Spring Security
├── dto/                            ← Objets de transfert de données
└── exception/                      ← Gestion des erreurs

src/main/resources/
├── application.properties          ← Configuration
└── db/migration/
    ├── V1__create_enums.sql        ← Types ENUM PostgreSQL
    ├── V2__create_tables.sql       ← Toutes les tables
    └── V3__insert_initial_data.sql ← Données initiales
```

---

## 🔜 Prochaines étapes

1. **Authentification** : Créer JwtService + SecurityConfig + AuthController
2. **Module Upload** : Créer DocumentController + FileStorageService
3. **Module IA** : Intégrer Google Gemini API
4. **Tests Postman** : Importer la collection et tester chaque endpoint
