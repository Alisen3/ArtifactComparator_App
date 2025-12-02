import React, { useState, useMemo, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import { useStudyPermissions } from '../../hooks/useStudyPermissions';
import PermissionGate from '../../components/PermissionGate';
import { hasPermission } from '../../utils/permissions';
import { api } from '../../context/AuthContext';
import './ResearcherDashboard.css';
import './StudyDetailStats.css';

const StudyDetailStats = () => {
    const { studyId } = useParams();
    const navigate = useNavigate();
    const { user } = useAuth();
    const { data: permissions, loading: permissionsLoading, error: permissionsError } = useStudyPermissions(studyId);
    const ITEMS_PER_PAGE = 5;
    const canExport = hasPermission(permissions, 'canExport');

    // Determine back navigation based on user role
    const getBackPath = () => {
        if (user.role === 'ADMIN') {
            return '/admin-dashboard';
        } else if (user.role === 'RESEARCHER') {
            return '/researcher-dashboard';
        }
        return '/';
    };

    const [studyData, setStudyData] = useState({
        id: parseInt(studyId),
        title: 'Loading...',
        participantCount: 0,
        taskCount: 0,
        completionPercentage: 0
    });
    const [participants, setParticipants] = useState([]);
    const [artifacts, setArtifacts] = useState([]);
    const [taskAnalytics, setTaskAnalytics] = useState({
        totalEvaluations: 0,
        completedEvaluations: 0,
        pendingEvaluations: 0,
        ratingDistribution: { '1': 0, '2': 0, '3': 0, '4': 0, '5': 0 },
        annotationDensity: 0
    });
    const [qualityIssues, setQualityIssues] = useState([]);
    const [dataLoading, setDataLoading] = useState(true);
    
    // Helper function to calculate variance
    const calculateVariance = (values) => {
        if (values.length === 0) return 0;
        const mean = values.reduce((sum, val) => sum + val, 0) / values.length;
        const squaredDiffs = values.map(val => Math.pow(val - mean, 2));
        return squaredDiffs.reduce((sum, val) => sum + val, 0) / values.length;
    };
    
    useEffect(() => {
        const fetchStudyData = async () => {
            try {
                setDataLoading(true);
                // Fetch study details and tasks
                const tasksResponse = await api.get(`/api/studies/${studyId}/tasks`);
                const tasks = tasksResponse.data || [];
                
                // Try to get study details from my-studies list
                let study = null;
                try {
                    const studiesResponse = await api.get('/api/studies/my-studies');
                    study = studiesResponse.data.find(s => s.id === parseInt(studyId));
                } catch (err) {
                    console.error('Error fetching study details:', err);
                }
                
                // If study not found in my-studies, try to get basic info from tasks
                if (!study && tasks.length > 0) {
                    // We can at least get the title from the first task
                    study = { title: tasks[0].studyTitle || 'Study' };
                }
                
                // Calculate participant data
                const participantMap = new Map();
                tasks.forEach(task => {
                    const pid = task.participantId;
                    if (!participantMap.has(pid)) {
                        participantMap.set(pid, {
                            id: `P${pid}`,
                            name: task.participantName,
                            completedTasks: 0,
                            totalTasks: 0
                        });
                    }
                    const p = participantMap.get(pid);
                    p.totalTasks++;
                    if (task.status === 'COMPLETED') {
                        p.completedTasks++;
                    }
                });
                
                const participantsList = Array.from(participantMap.values()).map(p => {
                    let status = 'Enrolled';
                    if (p.completedTasks === p.totalTasks && p.totalTasks > 0) {
                        status = 'Completed';
                    } else if (p.completedTasks > 0) {
                        status = 'In Progress';
                    }
                    return { ...p, status };
                });
                setParticipants(participantsList);
                
                // Fetch full task details for completed tasks to get ratings and annotations
                const completedTaskIds = tasks.filter(t => t.status === 'COMPLETED').map(t => t.taskId);
                const taskDetailsPromises = completedTaskIds.map(taskId => 
                    api.get(`/api/tasks/${taskId}`).catch(() => null)
                );
                const taskDetailsResponses = await Promise.all(taskDetailsPromises);
                const taskDetails = taskDetailsResponses
                    .filter(r => r && r.data)
                    .map(r => r.data);
                
                // Calculate ratings distribution from task details
                const ratingCounts = { '1': 0, '2': 0, '3': 0, '4': 0, '5': 0 };
                let tasksWithAnnotations = 0;
                
                taskDetails.forEach(task => {
                    // Count ratings (clarity, relevance, accuracy for both artifacts)
                    const ratings = [
                        task.clarityA, task.relevanceA, task.accuracyA,
                        task.clarityB, task.relevanceB, task.accuracyB
                    ].filter(r => r != null);
                    
                    ratings.forEach(rating => {
                        const rounded = Math.round(rating);
                        if (rounded >= 1 && rounded <= 5) {
                            ratingCounts[rounded.toString()]++;
                        }
                    });
                    
                    // Check for annotations
                    if (task.annotations && task.annotations.trim().length > 0) {
                        tasksWithAnnotations++;
                    }
                    if (task.commentA && task.commentA.trim().length > 0) {
                        tasksWithAnnotations++;
                    }
                    if (task.commentB && task.commentB.trim().length > 0) {
                        tasksWithAnnotations++;
                    }
                });
                
                // Calculate artifact usage with actual scores
                const artifactMap = new Map();
                taskDetails.forEach(task => {
                    // Process artifact A
                    if (task.artifactA?.id) {
                        const aid = task.artifactA.id;
                        if (!artifactMap.has(aid)) {
                            artifactMap.set(aid, {
                                id: `A${aid}`,
                                name: task.artifactA.fileName,
                                evaluationCount: 0,
                                totalScore: 0,
                                scoreCount: 0
                            });
                        }
                        const art = artifactMap.get(aid);
                        art.evaluationCount++;
                        const ratingsA = [task.clarityA, task.relevanceA, task.accuracyA].filter(r => r != null);
                        if (ratingsA.length > 0) {
                            const avgRating = ratingsA.reduce((sum, r) => sum + r, 0) / ratingsA.length;
                            art.totalScore += avgRating;
                            art.scoreCount++;
                        }
                    }
                    
                    // Process artifact B
                    if (task.artifactB?.id) {
                        const bid = task.artifactB.id;
                        if (!artifactMap.has(bid)) {
                            artifactMap.set(bid, {
                                id: `A${bid}`,
                                name: task.artifactB.fileName,
                                evaluationCount: 0,
                                totalScore: 0,
                                scoreCount: 0
                            });
                        }
                        const art = artifactMap.get(bid);
                        art.evaluationCount++;
                        const ratingsB = [task.clarityB, task.relevanceB, task.accuracyB].filter(r => r != null);
                        if (ratingsB.length > 0) {
                            const avgRating = ratingsB.reduce((sum, r) => sum + r, 0) / ratingsB.length;
                            art.totalScore += avgRating;
                            art.scoreCount++;
                        }
                    }
                });
                
                // Also count from basic tasks for evaluation count
                tasks.forEach(task => {
                    [task.artifactAId, task.artifactBId].forEach((aid, idx) => {
                        const artifactName = idx === 0 ? task.artifactAFileName : task.artifactBFileName;
                        if (!artifactMap.has(aid)) {
                            artifactMap.set(aid, {
                                id: `A${aid}`,
                                name: artifactName,
                                evaluationCount: 0,
                                totalScore: 0,
                                scoreCount: 0
                            });
                        }
                        const art = artifactMap.get(aid);
                        if (art.scoreCount === 0) { // Only increment if not already counted from details
                            art.evaluationCount++;
                        }
                    });
                });
                
                const artifactsList = Array.from(artifactMap.values()).map(a => ({
                    ...a,
                    avgScore: a.scoreCount > 0 ? Math.round((a.totalScore / a.scoreCount) * 10) / 10 : 0
                }));
                setArtifacts(artifactsList);
                
                // Calculate task analytics
                const completedTasks = tasks.filter(t => t.status === 'COMPLETED');
                const totalEvaluations = tasks.length * 2; // Each task has 2 evaluations
                const completedEvaluations = completedTasks.length * 2;
                
                // Calculate annotation density
                const totalPossibleAnnotations = completedTasks.length * 3; // annotations, commentA, commentB
                const annotationDensity = totalPossibleAnnotations > 0 
                    ? Math.round((tasksWithAnnotations / totalPossibleAnnotations) * 100) 
                    : 0;
                
                setTaskAnalytics({
                    totalEvaluations,
                    completedEvaluations,
                    pendingEvaluations: totalEvaluations - completedEvaluations,
                    ratingDistribution: ratingCounts,
                    annotationDensity
                });
                
                // Calculate study data
                const participantCount = participantMap.size;
                const taskCount = tasks.length;
                const completionPercentage = taskCount > 0 
                    ? Math.round((completedTasks.length / taskCount) * 100) 
                    : 0;
                
                setStudyData({
                    id: parseInt(studyId),
                    title: study?.title || 'Study',
                    participantCount,
                    taskCount,
                    completionPercentage
                });
                
                // Calculate quality control indicators
                const qualityIssuesList = [];
                let issueId = 1;
                
                // Get participant info map for quality control
                const participantInfoMap = new Map();
                tasks.forEach(t => {
                    if (!participantInfoMap.has(t.participantId)) {
                        participantInfoMap.set(t.participantId, {
                            id: t.participantId,
                            name: t.participantName
                        });
                    }
                });
                
                taskDetails.forEach((task) => {
                    const taskBasic = tasks.find(t => t.taskId === task.taskId);
                    if (!taskBasic) return;
                    
                    const participant = participantInfoMap.get(taskBasic.participantId);
                    const participantName = participant ? participant.name : 'Unknown';
                    const participantId = participant ? `P${participant.id}` : 'Unknown';
                    
                    // Check for fast evaluation (completed in less than 30 seconds)
                    // Note: We need createdAt and completedAt from taskBasic (AssignedTaskDTO)
                    if (taskBasic.createdAt && taskBasic.completedAt) {
                        const created = new Date(taskBasic.createdAt);
                        const completed = new Date(taskBasic.completedAt);
                        const durationSeconds = (completed - created) / 1000;
                        
                        if (durationSeconds < 30 && durationSeconds > 0) {
                            qualityIssuesList.push({
                                id: issueId++,
                                type: 'Fast Evaluation',
                                participantId: participantId,
                                participantName: participantName,
                                details: `Completed in ${Math.round(durationSeconds)} seconds`,
                                severity: durationSeconds < 15 ? 'High' : 'Medium'
                            });
                        }
                    }
                    
                    // Check for inconsistent data (high variance in ratings)
                    const ratingsA = [task.clarityA, task.relevanceA, task.accuracyA].filter(r => r != null);
                    const ratingsB = [task.clarityB, task.relevanceB, task.accuracyB].filter(r => r != null);
                    
                    if (ratingsA.length >= 2) {
                        const varianceA = calculateVariance(ratingsA);
                        if (varianceA > 2.0) {
                            qualityIssuesList.push({
                                id: issueId++,
                                type: 'Inconsistent Data',
                                participantId: participantId,
                                participantName: participantName,
                                details: `Rating variance for artifact A: ${varianceA.toFixed(2)}`,
                                severity: varianceA > 3.0 ? 'High' : 'Medium'
                            });
                        }
                    }
                    
                    if (ratingsB.length >= 2) {
                        const varianceB = calculateVariance(ratingsB);
                        if (varianceB > 2.0) {
                            qualityIssuesList.push({
                                id: issueId++,
                                type: 'Inconsistent Data',
                                participantId: participantId,
                                participantName: participantName,
                                details: `Rating variance for artifact B: ${varianceB.toFixed(2)}`,
                                severity: varianceB > 3.0 ? 'High' : 'Medium'
                            });
                        }
                    }
                });
                
                setQualityIssues(qualityIssuesList);
            } catch (err) {
                console.error('Error fetching study data:', err);
            } finally {
                setDataLoading(false);
            }
        };
        
        fetchStudyData();
    }, [studyId]);

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
        if (!canExport) {
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

    if (dataLoading) {
        return (
            <div className="researcher-dashboard study-detail-page">
                <div className="dashboard-header">
                    <h1>Loading study statistics...</h1>
                </div>
                <div className="dashboard-card full-width">
                    <div className="card-content">
                        <p style={{textAlign: 'center', color: '#999', padding: '2rem'}}>
                            Loading study data...
                        </p>
                    </div>
                </div>
            </div>
        );
    }
    
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
                                <PermissionGate permissions={permissions} check="canManageTasks" loading={permissionsLoading}>
                                    <button 
                                        className="form-button form-button-secondary"
                                        onClick={() => navigate(`/researcher-dashboard/study/${studyId}/tasks`)}
                                        style={{width: 'auto', fontSize: '0.9rem', padding: '0.5rem 1rem'}}
                                    >
                                        Manage Tasks
                                    </button>
                                </PermissionGate>
                                <PermissionGate permissions={permissions} check="canExport" loading={permissionsLoading}>
                                    <button 
                                        className="form-button form-button-secondary"
                                        onClick={() => navigate(`/researcher-dashboard/study/${studyId}/submissions`)}
                                        style={{width: 'auto', fontSize: '0.9rem', padding: '0.5rem 1rem'}}
                                    >
                                        View Submissions
                                    </button>
                                </PermissionGate>
                                <PermissionGate permissions={permissions} check="canInvite" loading={permissionsLoading}>
                                    <button 
                                        className="form-button form-button-secondary"
                                        onClick={() => navigate(`/researcher-dashboard/study/${studyId}/collaborators`)}
                                        style={{width: 'auto', fontSize: '0.9rem', padding: '0.5rem 1rem'}}
                                    >
                                        Manage Collaborators
                                    </button>
                                </PermissionGate>
                                <PermissionGate permissions={permissions} check="canViewAudit" loading={permissionsLoading}>
                                    <button 
                                        className="form-button form-button-secondary"
                                        onClick={() => navigate(`/researcher-dashboard/study/${studyId}/audit-log`)}
                                        style={{width: 'auto', fontSize: '0.9rem', padding: '0.5rem 1rem'}}
                                    >
                                        Audit Log
                                    </button>
                                </PermissionGate>
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
                <PermissionGate
                    permissions={permissions}
                    check="canExport"
                    loading={permissionsLoading}
                    fallback={
                        <p className="form-message info" style={{ marginTop: '1rem' }}>
                            You do not have permission to export this study.
                        </p>
                    }
                >
                    <div className="export-buttons">
                        <button className="export-button" onClick={() => handleExport('CSV')} disabled={!canExport}>
                            Export CSV
                        </button>
                        <button className="export-button" onClick={() => handleExport('XLSX')} disabled={!canExport}>
                            Export XLSX
                        </button>
                        <button className="export-button" onClick={() => handleExport('PDF')} disabled={!canExport}>
                            Export PDF
                        </button>
                    </div>
                </PermissionGate>
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

