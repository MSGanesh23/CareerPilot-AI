# CareerPilot AI — Frontend

React 18 SPA connected to the CareerPilot Spring Boot backend.

## Setup

```bash
cd careerpilot-frontend
npm install
cp .env.example .env.local   # optional — proxy is pre-configured for :8080
npm start
```

The app will open at **http://localhost:3000**.
The CRA proxy forwards `/api` requests to `http://localhost:8080`.

## Pages

| Route | Page |
|-------|------|
| `/login` | Sign in |
| `/register` | Create account |
| `/dashboard` | Overview stats + skill gaps |
| `/jobs` | All job applications (filterable) |
| `/jobs/new` | Add new application |
| `/jobs/:id` | Job detail + AI analysis panel |
| `/resumes` | Upload & manage resume versions |
| `/interviews` | List + start mock sessions |
| `/interviews/:id` | Live Q&A interview flow |
| `/analytics` | Charts: trend, score, skill gaps |
| `/profile` | Edit profile |

## Project Structure

```
src/
├── api/
│   ├── client.js      # Axios instance + JWT interceptor + 401 handler
│   └── services.js    # All API calls grouped by domain
├── auth/
│   ├── AuthContext.js  # Global auth state (login/register/logout)
│   └── ProtectedRoute.js
├── components/
│   └── layout/
│       ├── AppLayout.js
│       ├── Sidebar.js
│       └── Sidebar.css
├── pages/
│   ├── auth/           # Login, Register
│   ├── dashboard/      # Dashboard overview
│   ├── jobs/           # List, Create, Detail
│   ├── resume/         # Upload & manage
│   ├── interview/      # Session list + live Q&A
│   ├── analytics/      # Recharts dashboard
│   └── profile/        # Edit profile
└── index.css           # Full design system (CSS variables, components)
```

## Design System

Uses CSS custom properties for all design tokens. Dark theme with amber/gold accent.
Fonts: DM Serif Display (headings) + DM Sans (body).
