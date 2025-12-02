import React, { useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { api } from '../context/AuthContext';
import { useStudyPermissions } from '../hooks/useStudyPermissions';
import PermissionGate from '../components/PermissionGate';
import './dashboards/ResearcherDashboard.css';
import './Forms.css';

const ACTION_OPTIONS = [
    'STUDY_CREATED',
    'STUDY_UPDATED',
    'STUDY_PUBLISHED',
    'STUDY_CLOSED',
    'STUDY_ARCHIVED',
    'COLLABORATOR_ADDED',
    'COLLABORATOR_ROLE_CHANGED',
    'COLLABORATOR_REMOVED',
    'QUIZ_ASSIGNED',
    'TASK_ASSIGNED',
    'TASK_COMPLETED',
    'ARTIFACT_LINKED',
    'ARTIFACT_UNLINKED',
    'RATING_CRITERION_ADDED',
    'RATING_CRITERION_UPDATED',
    'RATING_CRITERION_REMOVED',
    'AUDIT_LOG_EXPORTED'
];

const LIMIT_OPTIONS = [50, 100, 250, 500];

const StudyAuditLog = () => {
    const { studyId } = useParams();
    const { data: permissions, loading: permissionsLoading, error: permissionsError } = useStudyPermissions(studyId);
    const [logs, setLogs] = useState([]);
    const [collaborators, setCollaborators] = useState([]);
    const [filters, setFilters] = useState({
        action: '',
        actorId: '',
        from: '',
        to: '',
        limit: 100
    });
    const [loading, setLoading] = useState(false);
    const [message, setMessage] = useState(null);
    const [exporting, setExporting] = useState(false);
    const [archiving, setArchiving] = useState(false);

    useEffect(() => {
        if (!permissionsLoading && permissions?.canViewAudit) {
            fetchCollaborators();
            fetchLogs(true);
        }
    // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [permissions, permissionsLoading]);

    const fetchCollaborators = async () => {
        try {
            const response = await api.get(`/api/studies/${studyId}/collaborators`);
            setCollaborators(response.data);
        } catch (err) {
            console.error('Failed to load collaborators', err);
        }
    };

    const buildParams = () => {
        const params = {};
        if (filters.action) params.action = filters.action;
        if (filters.actorId) params.actorId = filters.actorId;
        if (filters.from) params.from = new Date(filters.from).toISOString();
        if (filters.to) params.to = new Date(filters.to).toISOString();
        if (filters.limit) params.limit = filters.limit;
        return params;
    };

    const fetchLogs = async (silent = false) => {
        if (!silent) {
            setMessage(null);
            setLoading(true);
        }
        try {
            const response = await api.get(`/api/studies/${studyId}/audit-log`, {
                params: buildParams()
            });
            setLogs(response.data);
        } catch (err) {
            setMessage({ type: 'error', text: err.response?.data?.message || 'Audit log could not be loaded.' });
        } finally {
            setLoading(false);
        }
    };

    const downloadBlob = (blob, filename) => {
        const url = window.URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = url;
        link.download = filename;
        document.body.appendChild(link);
        link.click();
        link.remove();
        window.URL.revokeObjectURL(url);
    };

    const handleExport = async () => {
        setExporting(true);
        setMessage(null);
        try {
            const response = await api.get(`/api/studies/${studyId}/audit-log/export`, {
                params: buildParams(),
                responseType: 'blob'
            });
            downloadBlob(response.data, `study-${studyId}-audit-log.csv`);
            setMessage({ type: 'success', text: 'Audit log exported.' });
        } catch (err) {
            setMessage({ type: 'error', text: err.response?.data?.message || 'Export failed.' });
        } finally {
            setExporting(false);
        }
    };

    const handleArchive = async () => {
        setArchiving(true);
        setMessage(null);
        try {
            const response = await api.post(`/api/studies/${studyId}/archive`, null, { responseType: 'blob' });
            downloadBlob(response.data, `study-${studyId}-archive.zip`);
            setMessage({ type: 'success', text: 'Archive generated successfully.' });
        } catch (err) {
            setMessage({ type: 'error', text: err.response?.data?.message || 'Archive failed.' });
        } finally {
            setArchiving(false);
        }
    };

    const actorOptions = [
        { value: '', label: 'All actors' },
        ...collaborators.map(collab => ({ value: collab.userId, label: `${collab.name} (${collab.userId})` }))
    ];

    const renderDetails = (details) => {
        if (!details || details === null) {
            return '—';
        }
        return JSON.stringify(details, null, 2);
    };

    if (permissionsLoading) {
        return <div className="researcher-dashboard"><p>Checking permissions...</p></div>;
    }

    if (permissionsError) {
        return <div className="researcher-dashboard"><p className="form-message error">{permissionsError}</p></div>;
    }

    if (!permissions?.canViewAudit) {
        return (
            <div className="researcher-dashboard">
                <div className="dashboard-header">
                    <h1>Study Audit Log</h1>
                </div>
                <p>You do not have permission to view the audit log for this study.</p>
            </div>
        );
    }

    return (
        <div className="researcher-dashboard">
            <div className="dashboard-header">
                <div className="dashboard-header-content">
                    <Link to="/researcher-dashboard/manage-studies">
                        <button className="form-button form-button-secondary" style={{ width: 'auto' }}>
                            ← Back to Studies
                        </button>
                    </Link>
                    <h1>Study Audit Log</h1>
                    <p style={{ color: '#ccc' }}>Study ID: {studyId}</p>
                </div>
            </div>

            <div className="dashboard-card" style={{ marginBottom: '1.5rem' }}>
                <div className="card-header">
                    <h2>Filters</h2>
                </div>
                <div className="card-content">
                    <div className="form-grid">
                        <label className="form-label">Action</label>
                        <select
                            className="form-select"
                            value={filters.action}
                            onChange={(e) => setFilters(prev => ({ ...prev, action: e.target.value }))}
                        >
                            <option value="">All actions</option>
                            {ACTION_OPTIONS.map(option => (
                                <option key={option} value={option}>{option}</option>
                            ))}
                        </select>

                        <label className="form-label">Actor</label>
                        <select
                            className="form-select"
                            value={filters.actorId}
                            onChange={(e) => setFilters(prev => ({ ...prev, actorId: e.target.value }))}
                        >
                            {actorOptions.map(option => (
                                <option key={option.value} value={option.value}>{option.label}</option>
                            ))}
                        </select>

                        <label className="form-label">From</label>
                        <input
                            type="datetime-local"
                            className="form-input"
                            value={filters.from}
                            onChange={(e) => setFilters(prev => ({ ...prev, from: e.target.value }))}
                        />

                        <label className="form-label">To</label>
                        <input
                            type="datetime-local"
                            className="form-input"
                            value={filters.to}
                            onChange={(e) => setFilters(prev => ({ ...prev, to: e.target.value }))}
                        />

                        <label className="form-label">Limit</label>
                        <select
                            className="form-select"
                            value={filters.limit}
                            onChange={(e) => setFilters(prev => ({ ...prev, limit: Number(e.target.value) }))}
                        >
                            {LIMIT_OPTIONS.map(limit => (
                                <option key={limit} value={limit}>{limit}</option>
                            ))}
                        </select>
                    </div>
                    <div style={{ display: 'flex', gap: '0.75rem', marginTop: '1rem', flexWrap: 'wrap' }}>
                        <button
                            className="form-button form-button-submit"
                            style={{ width: 'auto' }}
                            onClick={() => fetchLogs(true)}
                            disabled={loading}
                        >
                            {loading ? 'Loading...' : 'Apply Filters'}
                        </button>
                        <PermissionGate permissions={permissions} check="canViewAudit">
                            <button
                                className="form-button form-button-secondary"
                                style={{ width: 'auto' }}
                                onClick={handleExport}
                                disabled={exporting}
                            >
                                {exporting ? 'Exporting...' : 'Export CSV'}
                            </button>
                        </PermissionGate>
                        <PermissionGate permissions={permissions} check="canArchive">
                            <button
                                className="form-button form-button-secondary"
                                style={{ width: 'auto' }}
                                onClick={handleArchive}
                                disabled={archiving}
                            >
                                {archiving ? 'Archiving...' : 'Generate Archive'}
                            </button>
                        </PermissionGate>
                    </div>
                    {message && (
                        <div className={`form-message ${message.type}`} style={{ marginTop: '1rem' }}>
                            {message.text}
                        </div>
                    )}
                </div>
            </div>

            <div className="dashboard-card">
                <div className="card-header">
                    <h2>Log Entries ({logs.length})</h2>
                </div>
                <div className="card-content">
                    {loading ? (
                        <p>Loading audit log...</p>
                    ) : logs.length === 0 ? (
                        <p>No audit entries match the selected filters.</p>
                    ) : (
                        <table className="artifact-list-table">
                            <thead>
                                <tr>
                                    <th>Timestamp</th>
                                    <th>Action</th>
                                    <th>Actor</th>
                                    <th>Details</th>
                                </tr>
                            </thead>
                            <tbody>
                                {logs.map(log => (
                                    <tr key={log.id}>
                                        <td>{new Date(log.createdAt).toLocaleString()}</td>
                                        <td>{log.action}</td>
                                        <td>{log.actorName || 'System'}</td>
                                        <td>
                                            <pre className="form-textarea" style={{ maxHeight: '200px', overflow: 'auto' }}>
                                                {renderDetails(log.details)}
                                            </pre>
                                        </td>
                                    </tr>
                                ))}
                            </tbody>
                        </table>
                    )}
                </div>
            </div>
        </div>
    );
};

export default StudyAuditLog;

