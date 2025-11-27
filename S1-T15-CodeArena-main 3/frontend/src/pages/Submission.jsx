import React from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import './EvaluationProgress.css';
import './Submission.css';

export default function Submission() {
  const { state } = useLocation();
  const navigate = useNavigate();
  const { isAuthenticated, user } = useAuth();

  // Default values if page is accessed directly without state
  const blinded = state?.blinded ?? false;
  const ratings = state?.ratings || {};
  const comments = state?.comments || {}; 
  const artifacts = state?.artifacts || ['A', 'B'];

  // Helper to calculate average of the 3 metrics
  const avg5 = (r) => {
    if (!r) return '—';
    const avg = (
        (Number(r.clarity || 0) + 
         Number(r.relevance || 0) + 
         Number(r.accuracy || 0)) / 3
    );
    return avg.toFixed(1);
  };

  const keyOf = (label) => label.split(' ').pop(); // "Artifact A" -> "A"

  const goHome = () => {
    if (isAuthenticated && user?.role === 'RESEARCHER') {
      navigate('/researcher-dashboard');
    } else if (isAuthenticated && user?.role === 'PARTICIPANT') {
      navigate('/participant-dashboard');
    } else {
      navigate('/login');
    }
  };

  // Safety check: if no state, redirect or show message
  if (!state) {
      return (
          <div className="submission-root">
              <div className="submission-card">
                  <h1>No Submission Data Found</h1>
                  <p>Please complete a task to see this summary.</p>
                  <button className="ghost-btn large" onClick={goHome}>Return to Dashboard</button>
              </div>
          </div>
      );
  }

  return (
    <div className="submission-root">
      <div className="submission-top">
        <div className="submission-icon">✓</div>
        <div className="submission-toptext">Submission Confirmation</div>
      </div>

      <main className="submission-card">
        <h1>Evaluation Submitted Successfully!</h1>
        <p className="lead">
          Thank you for your contribution. Your evaluation has been recorded
          {blinded ? ' (Blinded mode).' : '.'} Below is a summary of your ratings.
        </p>

        <div className="doc-list">
          {artifacts.map((key, i) => {
            // key is likely just "A" or "B" based on my previous code
            const r = ratings[key];
            const comment = (comments[key] ?? '').trim();

            return (
              <div key={key} className="doc-item">
                <div className="doc-title">{`Artifact ${key}`}</div>
                
                {r ? (
                    <>
                        <div className="doc-meta"><strong>Overall Average: {avg5(r)}/5.0</strong></div>
                        <div className="doc-meta" style={{marginTop:'0.5rem', fontSize:'0.9rem'}}>
                          Clarity: <strong>{r.clarity}</strong> • 
                          Relevance: <strong>{r.relevance}</strong> • 
                          Accuracy: <strong>{r.accuracy}</strong>
                        </div>
                    </>
                ) : (
                    <div className="doc-meta">No ratings provided</div>
                )}

                {comment && (
                  <div className="doc-comment">
                    <div className="doc-comment-title">Your Comment</div>
                    <p style={{ whiteSpace: 'pre-wrap', margin: 0, color:'#ccc' }}>{comment}</p>
                  </div>
                )}
              </div>
            );
          })}
        </div>

        <section className="next-steps">
          <h3>Next Steps</h3>
          <p>You can return to the Home Dashboard to pick up another task.</p>
          <div className="btn-row">
            <button className="ghost-btn large" onClick={goHome}>
              Return to Dashboard
            </button>
          </div>
        </section>
      </main>
    </div>
  );
}
