import React, { useState, useMemo } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import { useStudyPermissions } from '../../hooks/useStudyPermissions';
import './ResearcherDashboard.css';
import './StudyDetailStats.css';

const StudyDetailStats = () => {
    const { studyId } = useParams();
    const navigate = useNavigate();
    const { user } = useAuth();
    const { data: permissions, loading: permissionsLoading, error: permissionsError } = useStudyPermissions(studyId);
    const ITEMS_PER_PAGE = 5;

    // Determine back navigation based on user role
    const getBackPath = () => {
        if (user.role === 'ADMIN') {
            return '/admin-dashboard';
        } else if (user.role === 'RESEARCHER') {
            return '/researcher-dashboard';
        }
        return '/';
    };

    // Mock study data - in real app, this would be fetched based on studyId
    const studyData = useMemo(() => ({
        id: parseInt(studyId),
        title: 'Code Quality Evaluation Study',
        participantCount: 25,
        taskCount: 85,
        completionPercentage: 72
    }), [studyId]);

    // Mock participant data
    const [participants] = useState([
        { id: 'P001', name: 'John Doe', status: 'Completed', completedTasks: 12, totalTasks: 12 },
        { id: 'P002', name: 'Jane Smith', status: 'In Progress', completedTasks: 8, totalTasks: 12 },
        { id: 'P003', name: 'Bob Johnson', status: 'Enrolled', completedTasks: 0, totalTasks: 12 },
        { id: 'P004', name: 'Alice Brown', status: 'In Progress', completedTasks: 6, totalTasks: 12 },
        { id: 'P005', name: 'Charlie Wilson', status: 'Dropped', completedTasks: 3, totalTasks: 12 },
        { id: 'P006', name: 'Diana Lee', status: 'Completed', completedTasks: 12, totalTasks: 12 },
        { id: 'P007', name: 'Eve Martinez', status: 'In Progress', completedTasks: 10, totalTasks: 12 },
        { id: 'P008', name: 'Frank Garcia', status: 'In Progress', completedTasks: 7, totalTasks: 12 },
        { id: 'P009', name: 'Grace Kim', status: 'Completed', completedTasks: 12, totalTasks: 12 },
        { id: 'P010', name: 'Henry Chen', status: 'In Progress', completedTasks: 5, totalTasks: 12 },
        { id: 'P011', name: 'Ivy Taylor', status: 'Enrolled', completedTasks: 0, totalTasks: 12 },
        { id: 'P012', name: 'Jack White', status: 'In Progress', completedTasks: 9, totalTasks: 12 }
    ]);

    // Mock artifact usage data
    const [artifacts] = useState([
        { id: 'A001', name: 'Sorting Algorithm A', evaluationCount: 45, avgScore: 4.3 },
        { id: 'A002', name: 'Sorting Algorithm B', evaluationCount: 38, avgScore: 3.9 },
        { id: 'A003', name: 'Database Query A', evaluationCount: 32, avgScore: 4.6 },
        { id: 'A004', name: 'Database Query B', evaluationCount: 28, avgScore: 3.4 },
        { id: 'A005', name: 'API Endpoint A', evaluationCount: 56, avgScore: 4.7 },
        { id: 'A006', name: 'API Endpoint B', evaluationCount: 42, avgScore: 3.8 },
        { id: 'A007', name: 'Cache Implementation', evaluationCount: 35, avgScore: 4.2 },
        { id: 'A008', name: 'Error Handler', evaluationCount: 29, avgScore: 3.6 }
    ]);

    // Mock task completion analytics
    const [taskAnalytics] = useState({
        totalEvaluations: 198,
        completedEvaluations: 145,
        pendingEvaluations: 53,
        ratingDistribution: {
            '1': 8,
            '2': 15,
            '3': 32,
            '4': 58,
            '5': 32
        },
        annotationDensity: 78 // percentage
    });

    // Mock quality control issues
    const [qualityIssues] = useState([
        { id: 1, type: 'Fast Evaluation', participantId: 'P002', participantName: 'Jane Smith', details: 'Completed in 25 seconds', severity: 'High' },
        { id: 2, type: 'Inconsistent Data', participantId: 'P004', participantName: 'Alice Brown', details: 'Rating variance > 3.0', severity: 'Medium' },
        { id: 3, type: 'Fast Evaluation', participantId: 'P007', participantName: 'Eve Martinez', details: 'Completed in 30 seconds', severity: 'High' },
        { id: 4, type: 'Inconsistent Data', participantId: 'P008', participantName: 'Frank Garcia', details: 'Rating variance > 2.5', severity: 'Medium' },
        { id: 5, type: 'Fast Evaluation', participantId: 'P010', participantName: 'Henry Chen', details: 'Completed in 28 seconds', severity: 'High' },
        { id: 6, type: 'Inconsistent Data', participantId: 'P012', participantName: 'Jack White', details: 'Rating variance > 2.8', severity: 'Medium' }
    ]);

    const [exportError, setExportError] = useState(null);

    // State for show more
    const [showAllParticipants, setShowAllParticipants] = useState(false);
    const [showAllArtifacts, setShowAllArtifacts] = useState(false);
    const [showAllIssues, setShowAllIssues] = useState(false);
    const [artifactSortBy, setArtifactSortBy] = useState('evaluationCount'); // 'evaluationCount' or 'avgScore'
    const [artifactSortOrder, setArtifactSortOrder] = useState('desc'); // 'asc' or 'desc'

    // Calculate participant status counts
    const participantCounts = useMemo(() => {
        return {
            enrolled: participants.filter(p => p.status === 'Enrolled').length,
            inProgress: participants.filter(p => p.status === 'In Progress').length,
            completed: participants.filter(p => p.status === 'Completed').length,
            dropped: participants.filter(p => p.status === 'Dropped').length
        };
    }, [participants]);

    // Sort participants by completed tasks (descending)
    const sortedParticipants = useMemo(() => {
        return [...participants].sort((a, b) => b.completedTasks - a.completedTasks);
    }, [participants]);

    // Sort artifacts by selected metric
    const sortedArtifacts = useMemo(() => {
        const sorted = [...artifacts].sort((a, b) => {
            if (artifactSortBy === 'evaluationCount') {
                return artifactSortOrder === 'asc' 
                    ? a.evaluationCount - b.evaluationCount
                    : b.evaluationCount - a.evaluationCount;
            } else {
                return artifactSortOrder === 'asc'
                    ? a.avgScore - b.avgScore
                    : b.avgScore - a.avgScore;
            }
        });
        return sorted;
    }, [artifacts, artifactSortBy, artifactSortOrder]);

    const handleArtifactSort = (column) => {
        if (artifactSortBy === column) {
            // Toggle order if same column
            setArtifactSortOrder(artifactSortOrder === 'asc' ? 'desc' : 'asc');
        } else {
            // New column, default to desc
            setArtifactSortBy(column);
            setArtifactSortOrder('desc');
        }
    };

    // Get color based on completion percentage
    const getCompletionColor = (percentage) => {
        if (percentage >= 70) return '#4CAF50';
        else if (percentage >= 40) return '#FF9800';
        else return '#f44336';
    };

    // Pie chart component
    const PieChart = ({ percentage }) => {
        const radius = 50;
        const circumference = 2 * Math.PI * radius;
        const offset = circumference - (percentage / 100) * circumference;
        const chartColor = getCompletionColor(percentage);
        
        return (
            <div className="pie-chart-container">
                <svg width="120" height="120" className="pie-chart">
                    <circle cx="60" cy="60" r={radius} fill="none" stroke="#444" strokeWidth="12" />
                    <circle
                        cx="60" cy="60" r={radius}
                        fill="none"
                        stroke={chartColor}
                        strokeWidth="12"
                        strokeDasharray={circumference}
                        strokeDashoffset={offset}
                        strokeLinecap="round"
                        transform="rotate(-90 60 60)"
                    />
                </svg>
                <div className="pie-chart-label">
                    <span className="pie-chart-percentage" style={{ color: chartColor }}>
                        {percentage}%
                    </span>
                </div>
            </div>
        );
    };

    const handleExport = (format) => {
        if (!permissions?.canExport) {
            setExportError('You do not have permission to export data for this study.');
            return;
        }
        setExportError(null);
        const dateStr = new Date().toISOString().slice(0, 10);
        const filenameBase = `study-${studyData.id}-stats-${dateStr}`;

        // Prepare data sections
        const participantRows = [
            ['Participant ID', 'Name', 'Status', 'Completed Tasks', 'Total Tasks'],
            ...participants.map(p => [p.id, p.name, p.status, p.completedTasks, p.totalTasks])
        ];

        const artifactRows = [
            ['Artifact ID', 'Name', 'Evaluations', 'Avg Score'],
            ...artifacts.map(a => [a.id, a.name, a.evaluationCount, a.avgScore])
        ];

        const issuesRows = [
            ['Issue ID', 'Type', 'Participant ID', 'Participant Name', 'Details', 'Severity'],
            ...qualityIssues.map(i => [i.id, i.type, i.participantId, i.participantName, i.details, i.severity])
        ];

        const analyticsRows = [
            ['Metric', 'Value'],
            ['Total Evaluations', taskAnalytics.totalEvaluations],
            ['Completed Evaluations', taskAnalytics.completedEvaluations],
            ['Pending Evaluations', taskAnalytics.pendingEvaluations],
            ...Object.entries(taskAnalytics.ratingDistribution).map(([k, v]) => [`Ratings ${k}★`, v]),
            ['Annotation Density (%)', taskAnalytics.annotationDensity]
        ];

        const toCsv = (rows) => {
            const escape = (val) => {
                if (val === null || val === undefined) return '';
                const s = String(val);
                return /[",\n]/.test(s) ? `"${s.replace(/"/g, '""')}"` : s;
            };
            return rows.map(r => r.map(escape).join(',')).join('\n');
        };

        const downloadBlob = (blob, filename) => {
            const url = URL.createObjectURL(blob);
            const a = document.createElement('a');
            a.href = url;
            a.download = filename;
            document.body.appendChild(a);
            a.click();
            a.remove();
            URL.revokeObjectURL(url);
        };

        const buildCsvFile = () => {
            const parts = [];
            parts.push(`# Study ${studyData.id}: ${studyData.title}`);
            parts.push('');
            parts.push('== Analytics ==');
            parts.push(toCsv(analyticsRows));
            parts.push('');
            parts.push('== Participants ==');
            parts.push(toCsv(participantRows));
            parts.push('');
            parts.push('== Artifacts ==');
            parts.push(toCsv(artifactRows));
            parts.push('');
            parts.push('== Quality Issues ==');
            parts.push(toCsv(issuesRows));
            const csvText = parts.join('\n');
            return new Blob([csvText], { type: 'text/csv;charset=utf-8' });
        };

        // Excel 2003 XML (SpreadsheetML) so we avoid extra dependencies; opens in Excel/Sheets
        const buildExcelXmlFile = () => {
            const xmlEscape = (s) => String(s).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');
            const sheetXml = (name, rows) => `
                <Worksheet ss:Name="${xmlEscape(name)}">
                  <Table>
                    ${rows.map(r => `
                      <Row>
                        ${r.map(c => `<Cell><Data ss:Type="${typeof c === 'number' ? 'Number' : 'String'}">${xmlEscape(c)}</Data></Cell>`).join('')}
                      </Row>
                    `).join('')}
                  </Table>
                </Worksheet>
            `;
            const workbook = `<?xml version="1.0"?>
            <?mso-application progid="Excel.Sheet"?>
            <Workbook xmlns="urn:schemas-microsoft-com:office:spreadsheet"
              xmlns:o="urn:schemas-microsoft-com:office:office"
              xmlns:x="urn:schemas-microsoft-com:office:excel"
              xmlns:ss="urn:schemas-microsoft-com:office:spreadsheet"
              xmlns:html="http://www.w3.org/TR/REC-html40">
              <DocumentProperties xmlns="urn:schemas-microsoft-com:office:office">
                <Author>CodeArena</Author>
                <Created>${new Date().toISOString()}</Created>
              </DocumentProperties>
              <Styles>
                <Style ss:ID="sHeader">
                  <Font ss:Bold="1"/>
                </Style>
              </Styles>
              ${sheetXml('Analytics', analyticsRows)}
              ${sheetXml('Participants', participantRows)}
              ${sheetXml('Artifacts', artifactRows)}
              ${sheetXml('QualityIssues', issuesRows)}
            </Workbook>`;
            return new Blob([workbook], { type: 'application/vnd.ms-excel' });
        };

        const openPrintView = () => {
            const w = window.open('', '_blank');
            if (!w) return;
            const style = `
                <style>
                  body { font-family: Arial, sans-serif; padding: 24px; color: #111; }
                  h1 { margin: 0 0 8px; font-size: 20px; }
                  h2 { margin: 24px 0 8px; font-size: 16px; }
                  table { border-collapse: collapse; width: 100%; margin: 8px 0 16px; }
                  th, td { border: 1px solid #999; padding: 6px 8px; font-size: 12px; }
                  th { background: #efefef; text-align: left; }
                  .meta { color: #555; margin-bottom: 16px; }
                </style>
            `;
            const table = (title, rows) => `
              <h2>${title}</h2>
              <table>
                <thead>
                  <tr>${rows[0].map(h => `<th>${h}</th>`).join('')}</tr>
                </thead>
                <tbody>
                  ${rows.slice(1).map(r => `<tr>${r.map(c => `<td>${c}</td>`).join('')}</tr>`).join('')}
                </tbody>
              </table>
            `;
            const html = `
              <html>
                <head>
                  <title>Study ${studyData.id} - ${studyData.title} - Export</title>
                  ${style}
                </head>
                <body>
                  <h1>Study ${studyData.id}: ${studyData.title}</h1>
                  <div class="meta">
                    Tasks: ${studyData.taskCount} • Participants: ${studyData.participantCount} • Completion: ${studyData.completionPercentage}%
                  </div>
                  ${table('Analytics', analyticsRows)}
                  ${table('Participants', participantRows)}
                  ${table('Artifacts', artifactRows)}
                  ${table('Quality Issues', issuesRows)}
                  <script>
                    window.addEventListener('load', () => setTimeout(() => window.print(), 100));
                  </script>
                </body>
              </html>
            `;
            w.document.open();
            w.document.write(html);
            w.document.close();
        };

        if (format === 'CSV') {
            const blob = buildCsvFile();
            downloadBlob(blob, `${filenameBase}.csv`);
        } else if (format === 'XLSX') {
            const blob = buildExcelXmlFile();
            downloadBlob(blob, `${filenameBase}.xls`);
        } else if (format === 'PDF') {
            openPrintView();
        }
    };

    const displayedParticipants = showAllParticipants 
        ? sortedParticipants 
        : sortedParticipants.slice(0, ITEMS_PER_PAGE);

    const displayedArtifacts = showAllArtifacts 
        ? sortedArtifacts 
        : sortedArtifacts.slice(0, ITEMS_PER_PAGE);

    const displayedIssues = showAllIssues 
        ? qualityIssues 
        : qualityIssues.slice(0, ITEMS_PER_PAGE);

    return (
        <div className="researcher-dashboard study-detail-page">
            <div className="dashboard-header">
                <div className="dashboard-header-content">
                    <button className="back-button" onClick={() => navigate(getBackPath())}>
                        ← Back to Dashboard
                    </button>
                    <h1>{studyData.title}</h1>
                    <div style={{display: 'flex', gap: '0.5rem', flexWrap: 'wrap'}}>
                        {user.role === 'RESEARCHER' && (
                            <>
                                {permissions?.canManageTasks && (
                                    <button 
                                        className="form-button form-button-secondary"
                                        onClick={() => navigate(`/researcher-dashboard/study/${studyId}/tasks`)}
                                        style={{width: 'auto', fontSize: '0.9rem', padding: '0.5rem 1rem'}}
                                    >
                                        Manage Tasks
                                    </button>
                                )}
                                {permissions?.canExport && (
                                    <button 
                                        className="form-button form-button-secondary"
                                        onClick={() => navigate(`/researcher-dashboard/study/${studyId}/submissions`)}
                                        style={{width: 'auto', fontSize: '0.9rem', padding: '0.5rem 1rem'}}
                                    >
                                        View Submissions
                                    </button>
                                )}
                                {permissions?.canInvite && (
                                    <button 
                                        className="form-button form-button-secondary"
                                        onClick={() => navigate(`/researcher-dashboard/study/${studyId}/collaborators`)}
                                        style={{width: 'auto', fontSize: '0.9rem', padding: '0.5rem 1rem'}}
                                    >
                                        Manage Collaborators
                                    </button>
                                )}
                            </>
                        )}
                    </div>
                </div>
                {permissions && (
                    <div className="role-banner">
                        Access Level: {permissions.role}
                    </div>
                )}
                {permissionsLoading && (
                    <div className="form-message info" style={{ marginTop: '0.75rem' }}>
                        Checking your permissions…
                    </div>
                )}
                {permissionsError && (
                    <div className="form-message error" style={{ marginTop: '0.75rem' }}>
                        {permissionsError}
                    </div>
                )}
            </div>

            {/* Top Info Section */}
            <div className="study-info-section">
                <div className="study-info-card">
                    <div className="study-info-left">
                        <PieChart percentage={studyData.completionPercentage} />
                    </div>
                    <div className="study-info-right">
                        <div className="study-info-stat">
                            <span className="study-info-label">Tasks</span>
                            <span className="study-info-value">{studyData.taskCount}</span>
                        </div>
                        <div className="study-info-stat">
                            <span className="study-info-label">Participants</span>
                            <span className="study-info-value">{studyData.participantCount}</span>
                        </div>
                        <div className="study-info-stat">
                            <span className="study-info-label">Completion</span>
                            <span className="study-info-value">{studyData.completionPercentage}%</span>
                        </div>
                    </div>
                </div>
            </div>

            {/* Task Completion Analytics Section */}
            <div className="detail-section">
                <div className="detail-section-header">
                    <h2>Task Completion Analytics</h2>
                </div>
                <div className="analytics-grid">
                    {/* Evaluation Progress */}
                    <div className="analytics-card">
                        <h3 className="analytics-title">Evaluation Progress</h3>
                        <div className="analytics-content">
                            <div className="progress-summary">
                                <div className="progress-numbers">
                                    <span className="progress-main">{taskAnalytics.completedEvaluations}</span>
                                    <span className="progress-total">/ {taskAnalytics.totalEvaluations}</span>
                                </div>
                                <div className="progress-bar-large">
                                    <div 
                                        className="progress-bar-fill-large"
                                        style={{ 
                                            width: `${(taskAnalytics.completedEvaluations / taskAnalytics.totalEvaluations) * 100}%`,
                                            backgroundColor: getCompletionColor((taskAnalytics.completedEvaluations / taskAnalytics.totalEvaluations) * 100)
                                        }}
                                    ></div>
                                </div>
                                <div className="progress-details">
                                    <span className="progress-label">Completed: {taskAnalytics.completedEvaluations}</span>
                                    <span className="progress-label">Pending: {taskAnalytics.pendingEvaluations}</span>
                                </div>
                            </div>
                        </div>
                    </div>

                    {/* Rating Distribution */}
                    <div className="analytics-card">
                        <h3 className="analytics-title">Rating Distribution</h3>
                        <div className="analytics-content">
                            <div className="rating-distribution">
                                {Object.entries(taskAnalytics.ratingDistribution).map(([rating, count]) => {
                                    const maxCount = Math.max(...Object.values(taskAnalytics.ratingDistribution));
                                    return (
                                        <div key={rating} className="rating-bar-item">
                                            <div className="rating-bar-label">
                                                <span>{rating}⭐</span>
                                                <span className="rating-count">{count}</span>
                                            </div>
                                            <div className="rating-bar-wrapper-large">
                                                <div 
                                                    className="rating-bar-fill-large"
                                                    style={{ 
                                                        width: `${(count / maxCount) * 100}%`,
                                                        backgroundColor: rating >= '4' ? '#4CAF50' : rating >= '3' ? '#FF9800' : '#f44336'
                                                    }}
                                                ></div>
                                            </div>
                                        </div>
                                    );
                                })}
                            </div>
                        </div>
                    </div>

                    {/* Annotation Density */}
                    <div className="analytics-card">
                        <h3 className="analytics-title">Annotation Density</h3>
                        <div className="analytics-content">
                            <div className="annotation-summary">
                                <div className="annotation-percentage">
                                    <span className="annotation-value">{taskAnalytics.annotationDensity}%</span>
                                    <span className="annotation-label">of evaluations have annotations</span>
                                </div>
                                <div className="annotation-bar-large">
                                    <div 
                                        className="annotation-bar-fill-large"
                                        style={{ 
                                            width: `${taskAnalytics.annotationDensity}%`,
                                            backgroundColor: taskAnalytics.annotationDensity >= 70 ? '#4CAF50' : taskAnalytics.annotationDensity >= 50 ? '#FF9800' : '#f44336'
                                        }}
                                    ></div>
                                </div>
                                <div className="annotation-details">
                                    <span className="annotation-stat">
                                        {Math.round((taskAnalytics.annotationDensity / 100) * taskAnalytics.completedEvaluations)} annotated
                                    </span>
                                    <span className="annotation-stat">
                                        {Math.round(((100 - taskAnalytics.annotationDensity) / 100) * taskAnalytics.completedEvaluations)} without
                                    </span>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
            </div>

            {/* Participant Status Section */}
            <div className="detail-section">
                <div className="detail-section-header">
                    <h2>
                        Participant Status 
                        <span className="status-counts">
                            (Enrolled: {participantCounts.enrolled}, In Progress: {participantCounts.inProgress}, 
                            Completed: {participantCounts.completed}, Dropped: {participantCounts.dropped})
                        </span>
                    </h2>
                </div>
                <div className="detail-section-content">
                    {displayedParticipants.map(participant => (
                        <div key={participant.id} className="detail-item">
                            <div className="detail-item-left">
                                <span className="detail-item-name">{participant.name}</span>
                                <span className={`detail-item-status status-${participant.status.toLowerCase().replace(' ', '-')}`}>
                                    {participant.status}
                                </span>
                            </div>
                            <div className="detail-item-right">
                                <span className="detail-item-value">
                                    {participant.completedTasks} / {participant.totalTasks} tasks
                                </span>
                            </div>
                        </div>
                    ))}
                    {sortedParticipants.length > ITEMS_PER_PAGE && (
                        <button 
                            className="show-more-button"
                            onClick={() => setShowAllParticipants(!showAllParticipants)}
                        >
                            {showAllParticipants ? 'Show Less' : `Show More (${sortedParticipants.length - ITEMS_PER_PAGE} more)`}
                        </button>
                    )}
                </div>
            </div>

            {/* Artifact Usage Insights Section */}
            <div className="detail-section">
                <div className="detail-section-header">
                    <h2>Artifact Usage Insights</h2>
                </div>
                <div className="detail-section-content">
                    <div className="artifact-header-row">
                        <div className="artifact-col-name">Artifact Name</div>
                        <div 
                            className={`artifact-col-header sortable ${artifactSortBy === 'evaluationCount' ? 'active' : ''}`}
                            onClick={() => handleArtifactSort('evaluationCount')}
                        >
                            Evaluations
                            {artifactSortBy === 'evaluationCount' && (
                                <span className="sort-indicator">{artifactSortOrder === 'asc' ? '↑' : '↓'}</span>
                            )}
                        </div>
                        <div 
                            className={`artifact-col-header sortable ${artifactSortBy === 'avgScore' ? 'active' : ''}`}
                            onClick={() => handleArtifactSort('avgScore')}
                        >
                            Avg Score
                            {artifactSortBy === 'avgScore' && (
                                <span className="sort-indicator">{artifactSortOrder === 'asc' ? '↑' : '↓'}</span>
                            )}
                        </div>
                    </div>
                    {displayedArtifacts.map(artifact => (
                        <div key={artifact.id} className="artifact-row">
                            <div className="artifact-col-name">
                                <span className="detail-item-name">{artifact.name}</span>
                                <span className="detail-item-id">ID: {artifact.id}</span>
                            </div>
                            <div className="artifact-col-value">
                                {artifact.evaluationCount}
                            </div>
                            <div className="artifact-col-value">
                                {artifact.avgScore}/5.0 ⭐
                            </div>
                        </div>
                    ))}
                    {sortedArtifacts.length > ITEMS_PER_PAGE && (
                        <button 
                            className="show-more-button"
                            onClick={() => setShowAllArtifacts(!showAllArtifacts)}
                        >
                            {showAllArtifacts ? 'Show Less' : `Show More (${sortedArtifacts.length - ITEMS_PER_PAGE} more)`}
                        </button>
                    )}
                </div>
            </div>

            {/* Quality Control Indicators Section */}
            <div className="detail-section">
                <div className="detail-section-header">
                    <h2>Quality Control Indicators</h2>
                </div>
                <div className="detail-section-content">
                    {displayedIssues.map(issue => (
                        <div key={issue.id} className="detail-item quality-issue">
                            <div className="detail-item-left">
                                <span className="detail-item-name">{issue.participantName}</span>
                                <span className={`detail-item-type type-${issue.type.toLowerCase().replace(' ', '-')}`}>
                                    {issue.type}
                                </span>
                            </div>
                            <div className="detail-item-right">
                                <span className="detail-item-value">{issue.details}</span>
                                <span className={`detail-item-severity severity-${issue.severity.toLowerCase()}`}>
                                    {issue.severity}
                                </span>
                            </div>
                        </div>
                    ))}
                    {qualityIssues.length > ITEMS_PER_PAGE && (
                        <button 
                            className="show-more-button"
                            onClick={() => setShowAllIssues(!showAllIssues)}
                        >
                            {showAllIssues ? 'Show Less' : `Show More (${qualityIssues.length - ITEMS_PER_PAGE} more)`}
                        </button>
                    )}
                </div>
            </div>

            {/* Export Section */}
            <div className="export-section">
                <h2>Export Data</h2>
                <div className="export-buttons">
                    <button className="export-button" onClick={() => handleExport('CSV')} disabled={!permissions?.canExport}>
                        Export CSV
                    </button>
                    <button className="export-button" onClick={() => handleExport('XLSX')} disabled={!permissions?.canExport}>
                        Export XLSX
                    </button>
                    <button className="export-button" onClick={() => handleExport('PDF')} disabled={!permissions?.canExport}>
                        Export PDF
                    </button>
                </div>
                {exportError && (
                    <p className="form-message error" style={{ marginTop: '1rem' }}>
                        {exportError}
                    </p>
                )}
            </div>
        </div>
    );
};

export default StudyDetailStats;

