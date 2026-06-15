# CareerPilot AI 🚀

> Production-grade AI-powered job tracking and interview prep SaaS.

CareerPilot AI is a full-stack platform designed to help candidates streamline their job search, analyze their resumes against job descriptions using AI, and practice with mock interviews. 

## 🏗️ Architecture & Tech Stack

This project is structured as a monorepo containing both the frontend client and the backend API.

### Frontend
* **Framework:** React 18 (SPA) built with Vite
* **Routing:** React Router v6
* **Data Visualization:** Recharts
* **Styling:** Custom CSS design system (Dark theme with amber/gold accents)
* **API Client:** Axios (with JWT interceptors)

### Backend
* **Framework:** Spring Boot 3.2.3 (Java 17)
* **Database:** MySQL with Flyway for migrations
* **Security:** Spring Security & JWT for authentication
* **API Documentation:** Springdoc OpenAPI (Swagger)
* **Data Processing:** MapStruct, Lombok, Apache PDFBox/POI (for Resume parsing)
* **AI Integration:** Spring WebFlux WebClient

## 📁 Project Structure

```
CareerPilot-AI/
├── backend/            # Spring Boot REST API
│   ├── src/            # Java source code
│   ├── pom.xml         # Maven configuration
│   └── .env.example    # Backend environment variables
└── frontend/           # React / Vite SPA
    ├── src/            # React components, pages, context
    ├── package.json    # NPM dependencies
    └── .env.example    # Frontend environment variables
```

## 🚀 Getting Started

### Prerequisites
- [Node.js](https://nodejs.org/) (v18+)
- [Java 17](https://adoptium.net/)
- [Maven](https://maven.apache.org/)
- [MySQL Server](https://www.mysql.com/)

### 1. Backend Setup

Navigate to the backend directory and configure your environment:

```bash
cd backend
cp .env.example .env
```

Update the `.env` file with your MySQL credentials, JWT secret, and any required AI API keys. Then, run the application:

```bash
mvn spring-boot:run
```
The backend API will start on `http://localhost:8080`. Swagger API documentation will be available at `http://localhost:8080/swagger-ui.html` (if enabled).

### 2. Frontend Setup

Navigate to the frontend directory and install dependencies:

```bash
cd frontend
npm install
cp .env.example .env.local
```

Start the development server:

```bash
npm run dev
```
The frontend will open at `http://localhost:3000`. API requests to `/api/*` are automatically proxied to the backend.

## 💡 Key Features

- **Job Application Tracking:** Add and track your job applications with a clear, filterable view.
- **AI Resume Analysis:** Upload PDF/DOCX resumes and let AI analyze your fit for a specific job posting.
- **Mock Interviews:** Conduct live AI-driven Q&A sessions based on the role you are applying for.
- **Analytics Dashboard:** Visualize your job search progress, interview scores, and skill gaps.

## 📄 License

This project is proprietary and confidential.
