// File Path: src/App.jsx
import { Routes, Route, Link, Navigate } from 'react-router-dom'
import Login from './pages/Login'
import Register from './pages/Register'
import ResearcherDashboard from './pages/ResearcherDashboard'
import ParticipantDashboard from './pages/ParticipantDashboard'
import ProtectedRoute from './components/ProtectedRoute'
import { useAuth } from './context/AuthContext'
import './App.css'

// Existing imports
import CreateQuiz from './pages/CreateQuiz'
import ManageQuizzes from './pages/ManageQuizzes'
import ManageStudies from './pages/ManageStudies'
import ViewSubmissions from './pages/ViewSubmissions'
import TakeQuiz from './pages/TakeQuiz'
import ManageStudyTasks from './pages/ManageStudyTasks'
import ComparisonPage from './pages/ComparisonPage'
import UploadArtifacts from "./pages/UploadArtifacts.jsx";
import EvaluationProgress from './pages/EvaluationProgress';
import EvaluationProgressBlindedMode from './pages/EvaluationProgressBlindedMode';
import Submission from './pages/Submission';

// --- NEW IMPORT FOR ISSUE #5 ---
import CreateQuestionnaire from './pages/CreateQuestionnaire';

// Layouts & Tests
import DashboardLayout from './components/DashboardLayout'
import TestDashboardLayout from './components/TestDashboardLayout'
import AdminDashboard from './pages/dashboards/AdminDashboard'
import ResearcherDashboardTest from './pages/dashboards/ResearcherDashboard'
import ParticipantDashboardTest from './pages/dashboards/ParticipantDashboard'
import StudyDetailStats from './pages/dashboards/StudyDetailStats'


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
                            <Navigate to="/participant-dashboard" replace />)
                } />

                {/* === TEST ROUTES === */}
                <Route element={<TestDashboardLayout />}>
                    <Route path="/test/dashboard/" element={<AdminDashboard />} />
                    <Route path="/test/dashboard/admin" element={<AdminDashboard />} />
                    <Route path="/test/dashboard/researcher" element={<ResearcherDashboardTest />} />
                    <Route path="/test/dashboard/researcher/study/:studyId" element={<StudyDetailStats />} />
                    <Route path="/test/dashboard/participant" element={<ParticipantDashboardTest />} />
                </Route>

                {/* === PROTECTED ROUTES === */}

                {/* 1. RESEARCHER */}
                <Route element={<ProtectedRoute allowedRole="RESEARCHER" />}>
                    <Route element={<DashboardLayout />}>
                        <Route path="/researcher-dashboard" element={<ResearcherDashboard />} />

                        {/* Technical Quiz Creator (Issue #8) */}
                        <Route path="/researcher-dashboard/create-quiz" element={<CreateQuiz />} />

                        {/* --- NEW ROUTE: Background Questionnaire (Issue #5) --- */}
                        <Route path="/researcher-dashboard/create-questionnaire" element={<CreateQuestionnaire />} />

                        <Route path="/researcher-dashboard/manage-quizzes" element={<ManageQuizzes />} />
                        <Route path="/researcher-dashboard/manage-studies" element={<ManageStudies />} />
                        <Route path="/researcher-dashboard/study/:studyId/submissions" element={<ViewSubmissions />} />
                        <Route path="/researcher-dashboard/study/:studyId/tasks" element={<ManageStudyTasks />} />
                        <Route path="/researcher-dashboard/artifacts" element={<UploadArtifacts />} />
                    </Route>
                </Route>

                {/* 2. PARTICIPANT */}
                <Route element={<ProtectedRoute allowedRole="PARTICIPANT" />}>
                    <Route element={<DashboardLayout />}>
                        <Route path="/participant-dashboard" element={<ParticipantDashboard />} />
                        <Route path="/participant-dashboard/quiz/:studyId" element={<TakeQuiz />} />
                        <Route path="/participant-dashboard/task/:taskId" element={<ComparisonPage />} />
                        <Route path="/evaluation/:taskId" element={<EvaluationProgress />} />
                        <Route path="/evaluation-blinded/:taskId" element={<EvaluationProgressBlindedMode />} />
                        <Route path="/submission" element={<Submission />} />
                    </Route>
                </Route>

            </Routes>
        </div>
    )
}

export default App