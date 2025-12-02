// File: frontend/src/pages/EvaluationProgress.jsx
import React, { useState, useEffect, useRef } from 'react';
import { useNavigate, useParams, Link } from 'react-router-dom';
import { api } from '../context/AuthContext';
import './EvaluationProgress.css';

// --- HELPER: Serialize Highlights (DOM -> JSON) ---
const serializeHighlights = (container) => {
    if (!container) return null;
    const highlights = [];
    const marks = container.querySelectorAll('.hl-mark');
    
    marks.forEach(mark => {
        // Create a range from the beginning of the container to the start of the mark
        const range = document.createRange();
        range.setStart(container, 0);
        range.setEndBefore(mark);
        
        // The length of the text in this range is the start offset
        const start = range.toString().length;
        const text = mark.innerText;
        const end = start + text.length;
        
        highlights.push({ start, end });
    });
    return JSON.stringify(highlights);
};

// --- HELPER: Restore Highlights (JSON -> DOM) ---
const restoreHighlights = (container, jsonString) => {
    if (!container || !jsonString) return;
    try {
        const ranges = JSON.parse(jsonString);
        if (!Array.isArray(ranges)) return;

        // Get all text nodes to map offsets back to DOM nodes
        const walker = document.createTreeWalker(container, NodeFilter.SHOW_TEXT, null);
        const textNodes = [];
        let node;
        while(node = walker.nextNode()) textNodes.push(node);

        ranges.forEach(({ start, end }) => {
            let currentPos = 0;
            let startNode = null, startOffset = 0;
            let endNode = null, endOffset = 0;

            for (const textNode of textNodes) {
                const len = textNode.nodeValue.length;
                if (!startNode && start >= currentPos && start < currentPos + len) {
                    startNode = textNode;
                    startOffset = start - currentPos;
                }
                if (!endNode && end > currentPos && end <= currentPos + len) {
                    endNode = textNode;
                    endOffset = end - currentPos;
                }
                currentPos += len;
            }

            if (startNode && endNode) {
                const range = document.createRange();
                range.setStart(startNode, startOffset);
                range.setEnd(endNode, endOffset);
                
                const mark = document.createElement('mark');
                mark.className = 'hl-mark';
                try {
                    range.surroundContents(mark);
                } catch (e) { 
                    console.warn("Could not restore highlight (likely overlap)", e); 
                }
            }
        });
    } catch (e) {
        console.error("Error restoring highlights", e);
    }
};

// --- SELECTION HELPERS ---
const collectSelectedTextNodes = (containerEl, master) => {
  const walker = document.createTreeWalker(
    containerEl,
    NodeFilter.SHOW_TEXT,
    { acceptNode(n){ return n.nodeValue && n.nodeValue.trim().length ? NodeFilter.FILTER_ACCEPT : NodeFilter.REJECT; } }
  );
  const nodes = [];
  let n;
  while ((n = walker.nextNode())) {
    if (typeof master.intersectsNode === 'function') {
      if (master.intersectsNode(n)) nodes.push(n);
    } else {
      const r2 = document.createRange(); r2.selectNodeContents(n);
      const intersects = master.compareBoundaryPoints(Range.END_TO_START, r2) < 0 &&
                         master.compareBoundaryPoints(Range.START_TO_END, r2) > 0;
      if (intersects) nodes.push(n);
    }
  }
  return nodes;
};

const wrapRangesWith = (ranges, className) => {
  for (let i = ranges.length - 1; i >= 0; i--) {
    const r = ranges[i];
    const el = document.createElement('mark');
    el.className = className;
    r.surroundContents(el);
  }
};

const unwrap = (el) => { 
    const p = el.parentNode; 
    if (!p) return; 
    while (el.firstChild) p.insertBefore(el.firstChild, el); 
    p.removeChild(el); 
};

const nearestMark = (node, className, stopAt) => {
  let el = node && node.nodeType === 3 ? node.parentNode : node;
  while (el && el !== stopAt) {
    if (el.nodeType === 1 && el.tagName === 'MARK' && el.classList.contains(className)) return el;
    el = el.parentNode;
  }
  return null;
};


export default function EvaluationProgress() {
  const { taskId } = useParams();
  const navigate = useNavigate();
  
  // --- State ---
  const [task, setTask] = useState(null);
  const [contentA, setContentA] = useState("Loading content...");
  const [contentB, setContentB] = useState("Loading content...");
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [submitting, setSubmitting] = useState(false);

  const [ratings, setRatings] = useState({
    A: { clarity: 3.0, relevance: 3.0, accuracy: 3.0 },
    B: { clarity: 3.0, relevance: 3.0, accuracy: 3.0 },
  });
  const [comments, setComments] = useState({ A: '', B: '' });
  const [saving, setSaving] = useState('');

  const docARef = useRef(null);
  const docBRef = useRef(null);

  // Derived state: Read-Only Mode
  const isReadOnly = task?.status === 'COMPLETED';

  // --- 1. Fetch Data ---
  useEffect(() => {
    const loadTaskData = async () => {
      try {
        setLoading(true);
        // Fetch Task Metadata
        const { data: taskData } = await api.get(`/api/tasks/${taskId}`);
        setTask(taskData);

        // Restore Ratings & Comments
        setRatings({
            A: { 
                clarity: taskData.clarityA || 3.0, 
                relevance: taskData.relevanceA || 3.0, 
                accuracy: taskData.accuracyA || 3.0 
            },
            B: { 
                clarity: taskData.clarityB || 3.0, 
                relevance: taskData.relevanceB || 3.0, 
                accuracy: taskData.accuracyB || 3.0 
            }
        });
        setComments({
            A: taskData.commentA || '',
            B: taskData.commentB || ''
        });

        // Load Artifact Content & Restore Highlights
        if (taskData.artifactA?.id) {
            try {
                const resA = await api.get(`/api/artifacts/${taskData.artifactA.id}`, { responseType: 'text' });
                setContentA(resA.data);
                // Restore highlights after a brief render delay
                setTimeout(() => restoreHighlights(docARef.current, taskData.highlightDataA), 100);
            } catch (e) { setContentA("Error loading content A"); }
        }

        if (taskData.artifactB?.id) {
            try {
                const resB = await api.get(`/api/artifacts/${taskData.artifactB.id}`, { responseType: 'text' });
                setContentB(resB.data);
                setTimeout(() => restoreHighlights(docBRef.current, taskData.highlightDataB), 100);
            } catch (e) { setContentB("Error loading content B"); }
        }

      } catch (err) {
        console.error("Failed to load task", err);
        setError("Could not load evaluation task.");
      } finally {
        setLoading(false);
      }
    };

    if (taskId) loadTaskData();
  }, [taskId]);

  // --- Auto-save Indicator ---
  useEffect(() => {
    if (!loading && !isReadOnly) {
        setSaving('Saving…');
        const t = setTimeout(() => setSaving('All changes saved locally'), 600);
        return () => clearTimeout(t);
    }
  }, [ratings, comments, loading, isReadOnly]);

  const updateRating = (k, field, value) => {
    if (isReadOnly) return;
    setRatings(prev => ({ ...prev, [k]: { ...prev[k], [field]: Number(value) } }));
  };

  // --- 2. Submit Evaluation ---
  const handleSubmit = async () => {
    if (!window.confirm("Are you sure you want to submit your evaluation?")) return;
    
    setSubmitting(true);
    try {
        // Capture current highlights
        const hlDataA = serializeHighlights(docARef.current);
        const hlDataB = serializeHighlights(docBRef.current);

        const payload = {
            annotations: "Evaluation completed via UI", 
            // Artifact A
            clarityA: ratings.A.clarity,
            relevanceA: ratings.A.relevance,
            accuracyA: ratings.A.accuracy,
            commentA: comments.A,
            highlightDataA: hlDataA,
            
            // Artifact B
            clarityB: ratings.B.clarity,
            relevanceB: ratings.B.relevance,
            accuracyB: ratings.B.accuracy,
            commentB: comments.B,
            highlightDataB: hlDataB
        };

        await api.post(`/api/tasks/${taskId}/complete`, payload);
        
        // Navigate to success page
        navigate('/submission', { 
            state: { 
                blinded: false, 
                ratings, 
                comments, 
                artifacts: ['A', 'B'] 
            } 
        });

    } catch (err) {
        console.error("Submission failed", err);
        alert("Failed to submit evaluation. Please try again.");
        setSubmitting(false);
    }
  };

  // --- Highlight Logic ---
  const highlightSelectionIn = (containerEl) => {
    if (isReadOnly) return; // Disable if completed
    if (!containerEl) return;
    
    const sel = window.getSelection();
    if (!sel || sel.rangeCount === 0 || sel.isCollapsed) return alert('Select text to highlight.');
    
    const master = sel.getRangeAt(0);
    if (!containerEl.contains(master.commonAncestorContainer)) return alert('Please select text within this artifact.');

    const selectedTextNodes = collectSelectedTextNodes(containerEl, master);
    if (!selectedTextNodes.length) return alert('Please select text within this artifact.');

    // Check if already highlighted (Toggle Off)
    const markSet = new Set();
    const allInside = selectedTextNodes.every(n => {
      const m = nearestMark(n, 'hl-mark', containerEl);
      if (m) markSet.add(m);
      return !!m;
    });

    if (allInside) {
      markSet.forEach(unwrap);
      sel.removeAllRanges();
      return;
    }

    // Toggle On
    const subRanges = [];
    selectedTextNodes.forEach(n => {
      const start = (n === master.startContainer) ? master.startOffset : 0;
      const end   = (n === master.endContainer)   ? master.endOffset   : n.nodeValue.length;
      if (end > start) { const r = document.createRange(); r.setStart(n, start); r.setEnd(n, end); subRanges.push(r); }
    });
    wrapRangesWith(subRanges, 'hl-mark');
    sel.removeAllRanges();
  };

  if (loading) return <div className="eval-root"><div className="eval-title">Loading Task...</div></div>;
  if (error) return <div className="eval-root"><div className="eval-title error">{error}</div></div>;

  return (
    <div className="eval-root">
      <header className="eval-topbar">
        <div className="eval-title">
            Evaluation Progress: {task?.studyTitle} 
            <span className="muted"> (Task ID: {taskId})</span>
            {isReadOnly && <span style={{color: '#4CAF50', marginLeft: '10px'}}> [COMPLETED - READ ONLY]</span>}
        </div>
        <nav className="eval-actions">
          <div className="action-group">
            <Link to="/participant-dashboard" className="ghost-btn" style={{textDecoration:'none'}}>Exit</Link>
          </div>
        </nav>
      </header>

      <section className="eval-grid">
        {/* Artifact A */}
        <article className="artifact-card">
          <div className="artifact-header">
            <div className="artifact-title">Artifact A: {task?.artifactA?.fileName}</div>
            {/* Author Display (Standard Mode Only) */}
            <div className="author">
                <span className="avatar">
                    {task?.artifactA?.uploaderName?.charAt(0).toUpperCase() || '?'}
                </span>
                <span className="author-name">
                    {task?.artifactA?.uploaderName || 'Unknown'}
                </span>
            </div>
          </div>
          <div className="doc-view" tabIndex={0} ref={docARef} style={{whiteSpace: 'pre-wrap'}}>
            {contentA}
          </div>
          <div className="action-row">
            <button 
                className="pill-btn" 
                onClick={() => highlightSelectionIn(docARef.current)}
                disabled={isReadOnly}
                style={isReadOnly ? {opacity:0.5, cursor:'not-allowed'} : {}}
            >
                Highlight
            </button>
          </div>
        </article>

        {/* Artifact B */}
        <article className="artifact-card outlined">
          <div className="artifact-header">
            <div className="artifact-title">Artifact B: {task?.artifactB?.fileName}</div>
            {/* Author Display (Standard Mode Only) */}
            <div className="author">
                <span className="avatar alt">
                    {task?.artifactB?.uploaderName?.charAt(0).toUpperCase() || '?'}
                </span>
                <span className="author-name">
                    {task?.artifactB?.uploaderName || 'Unknown'}
                </span>
            </div>
          </div>
          <div className="doc-view" tabIndex={0} ref={docBRef} style={{whiteSpace: 'pre-wrap'}}>
            {contentB}
          </div>
          <div className="action-row">
            <button 
                className="pill-btn" 
                onClick={() => highlightSelectionIn(docBRef.current)}
                disabled={isReadOnly}
                style={isReadOnly ? {opacity:0.5, cursor:'not-allowed'} : {}}
            >
                Highlight
            </button>
          </div>
        </article>
      </section>

      {/* Ratings + Comments */}
      <section className="rating-card">
        <h3 className="rating-title">Rating Panel</h3>
        {['A', 'B'].map(k => (
          <div key={k} className="artifact-rating-block">
            <h4 style={{ margin: '8px 0 6px' }}>
                Artifact {k} ({k === 'A' ? task?.artifactA?.fileName : task?.artifactB?.fileName})
            </h4>
            
            {/* Sliders for 3 Metrics */}
            {['clarity', 'relevance', 'accuracy'].map(metric => (
                <div className="slider-row" key={metric}>
                    <label style={{textTransform:'capitalize'}}>{metric}</label>
                    <input 
                        type="range" min="0" max="5" step="0.5"
                        value={ratings[k][metric]}
                        disabled={isReadOnly}
                        onChange={(e)=>updateRating(k, metric, e.target.value)} 
                    />
                    <span className="slider-val">{ratings[k][metric].toFixed(1)}</span>
                </div>
            ))}

            <label className="comment-label">Comment for Artifact {k}</label>
            <textarea
              className="artifact-comment-input"
              rows={3}
              placeholder={isReadOnly ? "No comments provided." : `Write your overall comment for Artifact ${k}…`}
              value={comments[k]}
              disabled={isReadOnly}
              onChange={(e)=>setComments(prev => ({ ...prev, [k]: e.target.value }))}
            />

            <hr className="rating-divider" />
          </div>
        ))}
        
        <div className="save-hint">{!isReadOnly && saving}</div>
        <div className="submit-row">
          {isReadOnly ? (
             <Link to="/participant-dashboard">
                <button className="submit-btn" style={{backgroundColor: '#555'}}>Return to Dashboard</button>
             </Link>
          ) : (
             <button className="submit-btn" onClick={handleSubmit} disabled={submitting}>
                {submitting ? 'Submitting...' : 'Submit Evaluation'}
             </button>
          )}
        </div>
      </section>
    </div>
  );
}
