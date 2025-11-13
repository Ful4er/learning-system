# Learning System Frontend

Vue.js 3 frontend for the Learning System application.

## Features

- Authentication (Login/Register)
- Teacher Dashboard
  - Profile with stats
  - Exam management
  - Student management
- Student Dashboard
  - Profile view
  - Exam assignments
  - Progress tracking

## Tech Stack

- Vue.js 3
- Vue Router
- Axios for API calls
- Vite for build tooling
- Docker + Nginx for deployment

## Development

### Prerequisites

- Node.js 20+
- npm or yarn

### Setup

```bash
# Install dependencies
npm install

# Run development server
npm run dev
```

The frontend will be available at `http://localhost:5173`

### Building

```bash
# Build for production
npm run build

# Preview production build
npm run preview
```

## Docker Deployment

```bash
# Build image
docker build -t learning-frontend .

# Run container
docker run -p 3000:80 learning-frontend
```

## Configuration

The frontend connects to the API Gateway at `http://localhost:8080` by default. 

For development:
- Vite proxy forwards `/api/*` to `http://localhost:8080`

For production (Docker):
- Nginx proxies `/api/*` to `http://api-gateway:8080`

## Authentication

The app uses JWT tokens stored in localStorage. The token is automatically:
- Sent with all API requests via Authorization header
- Cleared on 401 responses (redirects to login)
- Persisted across page refreshes

## Project Structure

```
frontend/
├── src/
│   ├── assets/          # Static assets (images, CSS)
│   ├── views/           # Vue components/pages
│   │   ├── AuthView.vue
│   │   ├── teacher/     # Teacher pages
│   │   └── student/     # Student pages
│   ├── App.vue          # Root component
│   ├── routes.js        # Route definitions
│   ├── main.js          # Application entry
│   └── styles.css       # Global styles
├── index.html           # HTML template
├── vite.config.js       # Vite configuration
├── Dockerfile           # Docker build
└── nginx.conf           # Nginx configuration



