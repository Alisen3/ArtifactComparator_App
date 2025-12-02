// File Path: src/App.jsx
import { Routes, Route, Link, Navigate } from 'react-router-dom'
import Login from './pages/Login'
import Register from './pages/Register'
import ProtectedRoute from './components/ProtectedRoute'
import { useAuth } from './context/AuthContext'
import './App.css'

// Real dashboards from dashboards folder
import ResearcherDashboard from './pages/dashboards/ResearcherDashboard'
import ParticipantDashboard from './pages/dashboards/ParticipantDashboard'
import AdminDashboard from './pages/dashboards/AdminDashboard'
import StudyDetailStats from './pages/dashboards/StudyDetailStats'

// Other pages
import CreateQuiz from './pages/CreateQuiz'
import ManageQuizzes from './pages/ManageQuizzes'
import ManageStudies from './pages/ManageStudies'
import ViewSubmissions from './pages/ViewSubmissions'
import TakeQuiz from './pages/TakeQuiz'
import ManageStudyTasks from './pages/ManageStudyTasks'
import StudyCollaborators from './pages/StudyCollaborators'
import ComparisonPage from './pages/ComparisonPage'
import UploadArtifacts from "./pages/UploadArtifacts.jsx";
import EvaluationProgress from './pages/EvaluationProgress';
import EvaluationProgressBlindedMode from './pages/EvaluationProgressBlindedMode';
import Submission from './pages/Submission';
import CreateQuestionnaire from './pages/CreateQuestionnaire';
import StudyAuditLog from './pages/StudyAuditLog';
import Participants from './pages/Participants';

// Layouts
import DashboardLayout from './components/DashboardLayout'


function App() {
    const { isAuthenticated, user } = useAuth();

    return (
        <div className="App">
            {!isAuthenticated && (
                <nav className="app-nav">
                    <Link to="/login" className="nav-link">Log in</Link>
                    <Link to="/register" className="nav-link">Register</Link>
                </nav>
            )}

            <Routes>
                {/* --- PUBLIC --- */}
                <Route path="/login" element={
                    !isAuthenticated ? <Login /> : <Navigate to={user.role === 'RESEARCHER' ? '/researcher-dashboard' : '/participant-dashboard'} replace />
                } />
                <Route path="/register" element={
                    !isAuthenticated ? <Register /> : <Navigate to="/participant-dashboard" replace />
                } />

                <Route path="/" element={
                    !isAuthenticated ? <Navigate to="/login" replace /> :
                        (user.role === 'RESEARCHER' ?
                            <Navigate to="/researcher-dashboard" replace /> :
                        user.role === 'ADMIN' ?
                            <Navigate to="/admin-dashboard" replace /> :
                            <Navigate to="/participant-dashboard" replace />)
                } />

                {/* === PROTECTED ROUTES === */}

                {/* 1. ADMIN */}
                <Route element={<ProtectedRoute allowedRole="ADMIN" />}>
                    <Route element={<DashboardLayout />}>
                        <Route path="/admin-dashboard" element={<AdminDashboard />} />
                        <Route path="/admin-dashboard/study/:studyId" element={<StudyDetailStats />} />
                    </Route>
                </Route>

                {/* 2. RESEARCHER */}
                <Route element={<ProtectedRoute allowedRole="RESEARCHER" />}>
                    <Route element={<DashboardLayout />}>
                        <Route path="/researcher-dashboard" element={<ResearcherDashboard />} />
                        <Route path="/researcher-dashboard/study/:studyId" element={<StudyDetailStats />} />
                        <Route path="/researcher-dashboard/create-quiz" element={<CreateQuiz />} />
                        <Route path="/researcher-dashboard/create-questionnaire" element={<CreateQuestionnaire />} />
                        <Route path="/researcher-dashboard/manage-quizzes" element={<ManageQuizzes />} />
                        <Route path="/researcher-dashboard/manage-studies" element={<ManageStudies />} />
                        <Route path="/researcher-dashboard/study/:studyId/submissions" element={<ViewSubmissions />} />
                        <Route path="/researcher-dashboard/study/:studyId/tasks" element={<ManageStudyTasks />} />
                        <Route path="/researcher-dashboard/study/:studyId/collaborators" element={<StudyCollaborators />} />
                        <Route path="/researcher-dashboard/study/:studyId/audit-log" element={<StudyAuditLog />} />
                        <Route path="/researcher-dashboard/artifacts" element={<UploadArtifacts />} />
                        <Route path="/researcher-dashboard/participants" element={<Participants />} />
                    </Route>
                </Route>

                {/* 3. PARTICIPANT */}
                <Route element={<ProtectedRoute allowedRole="PARTICIPANT" />}>
                    <Route element={<DashboardLayout />}>
                        <Route path="/participant-dashboard" element={<ParticipantDashboard />} />
                        <Route path="/participant-dashboard/quiz/:studyId" element={<TakeQuiz />} />
                        <Route path="/participant-dashboard/task/:taskId" element={<ComparisonPage />} />
                        <Route path="/participant-dashboard/evaluation/:taskId" element={<EvaluationProgress />} />
                        <Route path="/participant-dashboard/evaluation-blinded/:taskId" element={<EvaluationProgressBlindedMode />} />
                        <Route path="/participant-dashboard/submission" element={<Submission />} />
                    </Route>
                </Route>

            </Routes>
        </div>
    )
}

export default App