// File Path: src/pages/CreateQuiz.jsx
import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { api } from '../context/AuthContext';
import './Forms.css';

const CreateQuiz = () => {
    const navigate = useNavigate();

    const [title, setTitle] = useState('');
    const [description, setDescription] = useState('');
    const [durationInMinutes, setDurationInMinutes] = useState(10);
    const [questions, setQuestions] = useState([
        {
            questionText: '',
            codeSnippet: '', // Issue #6: New Field
            questionType: 'MULTIPLE_CHOICE',
            options: [
                { optionText: '', isCorrect: true },
                { optionText: '', isCorrect: false }
            ]
        }
    ]);

    // --- NEW STATE FOR AI (Issue #8) ---
    const [aiTopic, setAiTopic] = useState('');
    const [aiCount, setAiCount] = useState(3);
    const [isGenerating, setIsGenerating] = useState(false);
    const [message, setMessage] = useState(null);

    const addQuestion = () => {
        setQuestions([
            ...questions,
            {
                questionText: '',
                codeSnippet: '',
                questionType: 'MULTIPLE_CHOICE',
                options: [
                    { optionText: '', isCorrect: true },
                    { optionText: '', isCorrect: false }
                ]
            }
        ]);
    };

    const handleQuestionChange = (qIndex, field, value) => {
        const newQuestions = [...questions];
        newQuestions[qIndex][field] = value;
        if (field === 'questionType' && value === 'SHORT_ANSWER') {
            newQuestions[qIndex].options = [];
        }
        if (field === 'questionType' && value === 'MULTIPLE_CHOICE' && newQuestions[qIndex].options.length === 0) {
            newQuestions[qIndex].options = [
                { optionText: '', isCorrect: true },
                { optionText: '', isCorrect: false }
            ];
        }
        setQuestions(newQuestions);
    };

    const addOption = (qIndex) => {
        const newQuestions = [...questions];
        newQuestions[qIndex].options.push({ optionText: '', isCorrect: false });
        setQuestions(newQuestions);
    };

    const handleOptionChange = (qIndex, oIndex, value) => {
        const newQuestions = [...questions];
        newQuestions[qIndex].options[oIndex].optionText = value;
        setQuestions(newQuestions);
    };

    const handleCorrectChange = (qIndex, correctOIndex) => {
        const newQuestions = [...questions];
        newQuestions[qIndex].options.forEach((option, oIndex) => {
            option.isCorrect = (oIndex === correctOIndex);
        });
        setQuestions(newQuestions);
    };

    // --- NEW HANDLER FOR AI GENERATION (Issue #8) ---
    const handleAiGenerate = async () => {
        if (!aiTopic) return alert("Please enter a topic (e.g. 'Java Loops')");
        setIsGenerating(true);
        setMessage({ type: 'info', text: 'Asking AI to generate questions...' });

        try {
            const res = await api.post('/api/quizzes/generate', {
                topic: aiTopic,
                count: parseInt(aiCount)
            });

            // FIX: If the list has only 1 question and it's empty, replace it.
            setQuestions(prev => {
                // Check if we only have the default empty question
                if (prev.length === 1 && !prev[0].questionText && !prev[0].codeSnippet) {
                    return [...res.data]; // Replace completely
                }
                // Otherwise append to existing questions
                return [...prev, ...res.data];
            });

            setMessage({ type: 'success', text: `Successfully added ${res.data.length} questions about "${aiTopic}"!` });
        } catch (err) {
            console.error("AI Gen Error:", err);
            setMessage({ type: 'error', text: 'AI Generation failed. Ensure backend is running.' });
        } finally {
            setIsGenerating(false);
        }
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        setMessage({ type: 'info', text: 'Creating quiz...' });

        // Send type: 'COMPETENCY_QUIZ' explicitly
        const quizData = {
            title,
            description,
            durationInMinutes: parseInt(durationInMinutes) || 0,
            type: 'COMPETENCY_QUIZ',
            questions: questions
        };

        try {
            await api.post('/api/quizzes', quizData);
            setMessage({ type: 'success', text: 'Quiz created successfully!' });
            setTimeout(() => {
                navigate('/researcher-dashboard/manage-quizzes');
            }, 1500);
        } catch (err) {
            console.error("Create quiz error:", err);
            setMessage({ type: 'error', text: 'Could not create quiz.' });
        }
    };

    const messageClass = message ? `form-message ${message.type}` : 'form-message';

    return (
        <div className="form-container">
            <div className="form-header">
                <h2>Create Technical Quiz</h2>
            </div>

            {/* --- AI GENERATOR SECTION (Issue #8) --- */}
            <div className="dashboard-card" style={{ marginBottom: '2rem', border: '1px solid #7c3aed', padding:'1.5rem' }}>
                <h3 style={{marginTop:0, color:'#7c3aed', display:'flex', alignItems:'center', gap:'8px'}}>
                    <span>✨</span> Generate with AI
                </h3>
                <p style={{color:'#ccc', fontSize:'0.9rem', marginBottom:'1rem'}}>
                    Automatically generate technical questions using our AI model.
                </p>
                <div style={{ display: 'flex', gap: '10px', flexWrap:'wrap' }}>
                    <input
                        className="form-input"
                        style={{flex:1, minWidth:'200px'}}
                        placeholder="Topic (e.g., Python Lists, React Hooks)"
                        value={aiTopic}
                        onChange={e=>setAiTopic(e.target.value)}
                    />
                    <input
                        className="form-input"
                        type="number" min="1" max="10"
                        style={{width:'100px'}}
                        value={aiCount}
                        onChange={e=>setAiCount(e.target.value)}
                    />
                    <button
                        type="button"
                        className="form-button"
                        style={{background:'#7c3aed', width:'auto', minWidth:'120px'}}
                        onClick={handleAiGenerate}
                        disabled={isGenerating}
                    >
                        {isGenerating ? 'Generating...' : 'Generate'}
                    </button>
                </div>
            </div>

            <form onSubmit={handleSubmit}>
                <fieldset className="form-fieldset">
                    <legend className="form-legend">Quiz Details</legend>
                    <div className="form-group">
                        <label className="form-label">Title:</label>
                        <input
                            type="text"
                            className="form-input"
                            value={title}
                            onChange={(e) => setTitle(e.target.value)}
                            required
                        />
                    </div>
                    <div className="form-group">
                        <label className="form-label">Description:</label>
                        <textarea
                            className="form-textarea"
                            value={description}
                            onChange={(e) => setDescription(e.target.value)}
                        />
                    </div>
                    <div className="form-group">
                        <label className="form-label">Duration (in Minutes): (0 or empty = unlimited)</label>
                        <input
                            type="number"
                            className="form-input"
                            value={durationInMinutes}
                            onChange={(e) => setDurationInMinutes(e.target.value)}
                            min="0"
                        />
                    </div>
                </fieldset>

                {questions.map((question, qIndex) => (
                    <fieldset key={qIndex} className="form-fieldset">
                        <legend className="form-legend">Question {qIndex + 1}</legend>
                        <div className="form-group">
                            <label className="form-label">Question Text:</label>
                            <input
                                type="text"
                                className="form-input"
                                value={question.questionText}
                                onChange={(e) => handleQuestionChange(qIndex, 'questionText', e.target.value)}
                                required
                            />
                        </div>

                        {/* Issue #6: Code Snippet Input */}
                        <div className="form-group">
                            <label className="form-label">Code Snippet (Optional):</label>
                            <textarea
                                className="form-textarea"
                                style={{fontFamily:'monospace', fontSize:'0.9em', minHeight:'80px', background:'#111'}}
                                placeholder="// Paste code for participants to analyze..."
                                value={question.codeSnippet}
                                onChange={e => handleQuestionChange(qIndex, 'codeSnippet', e.target.value)}
                            />
                        </div>

                        <div className="form-group">
                            <label className="form-label">Question Type:</label>
                            <select
                                className="form-select"
                                value={question.questionType}
                                onChange={(e) => handleQuestionChange(qIndex, 'questionType', e.target.value)}
                            >
                                <option value="MULTIPLE_CHOICE">Multiple Choice (Graded)</option>
                                <option value="SHORT_ANSWER">Short Answer (Survey - Not graded)</option>
                            </select>
                        </div>

                        {question.questionType === 'MULTIPLE_CHOICE' && (
                            <>
                                <h4 className="option-group-header">Options (Select the correct one)</h4>
                                {question.options.map((option, oIndex) => (
                                    <div key={oIndex} className="option-item">
                                        <input
                                            type="radio"
                                            name={`correct_q_${qIndex}`}
                                            checked={option.isCorrect}
                                            onChange={() => handleCorrectChange(qIndex, oIndex)}
                                        />
                                        <input
                                            type="text"
                                            placeholder={`Option ${oIndex + 1}`}
                                            value={option.optionText}
                                            onChange={(e) => handleOptionChange(qIndex, oIndex, e.target.value)}
                                            className="form-input"
                                            required
                                        />
                                    </div>
                                ))}
                                <button type="button" onClick={() => addOption(qIndex)} className="form-button form-button-secondary">
                                    + Add Option
                                </button>
                            </>
                        )}
                    </fieldset>
                ))}

                <button type="button" onClick={addQuestion} className="form-button form-button-add">
                    + Add New Question
                </button>

                <hr style={{ margin: '2rem 0', borderColor: '#444' }} />

                <button type="submit" className="form-button form-button-submit">
                    Save Quiz
                </button>

                {message && <p className={messageClass}>{message.text}</p>}
            </form>
        </div>
    );
};

export default CreateQuiz;