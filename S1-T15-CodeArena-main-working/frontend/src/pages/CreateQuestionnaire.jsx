// Dosya Yolu: src/pages/CreateQuestionnaire.jsx
import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { api } from '../context/AuthContext';
import './Forms.css';

const CreateQuestionnaire = () => {
    const navigate = useNavigate();

    const [title, setTitle] = useState('');
    const [description, setDescription] = useState('');
    const [questions, setQuestions] = useState([
        {
            questionText: '',
            questionType: 'SHORT_ANSWER', // Default for surveys
            options: []
        }
    ]);
    const [message, setMessage] = useState(null);

    const addQuestion = () => {
        setQuestions([
            ...questions,
            {
                questionText: '',
                questionType: 'SHORT_ANSWER',
                options: []
            }
        ]);
    };

    const handleQuestionChange = (qIndex, field, value) => {
        const newQuestions = [...questions];
        newQuestions[qIndex][field] = value;

        // Initialize options if switching to Multiple Choice
        if (field === 'questionType' && value === 'MULTIPLE_CHOICE' && newQuestions[qIndex].options.length === 0) {
            newQuestions[qIndex].options = [
                { optionText: '', isCorrect: false }, // No "true" needed for survey
                { optionText: '', isCorrect: false }
            ];
        }
        setQuestions(newQuestions);
    };

    // Option handling for Survey (Demographics e.g., "Select your Age Range")
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

    const handleSubmit = async (e) => {
        e.preventDefault();
        setMessage({ type: 'info', text: 'Saving questionnaire...' });

        const payload = {
            title,
            description,
            durationInMinutes: 0, // Surveys are untimed
            type: 'BACKGROUND_SURVEY', // <--- CRITICAL FOR ISSUE #5
            questions: questions
        };

        try {
            await api.post('/api/quizzes', payload);
            setMessage({ type: 'success', text: 'Questionnaire created successfully!' });
            setTimeout(() => navigate('/researcher-dashboard/manage-quizzes'), 1500);
        } catch (err) {
            console.error(err);
            setMessage({ type: 'error', text: 'Error creating questionnaire.' });
        }
    };

    const messageClass = message ? `form-message ${message.type}` : 'form-message';

    return (
        <div className="form-container">
            <div className="form-header" style={{display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start'}}>
                <div>
                    <h2>Create Background Questionnaire</h2>
                    <p style={{marginTop:'-1rem', color:'#aaa'}}>Collect demographics and experience (Untimed, Ungraded).</p>
                </div>
                <button 
                    className="form-button form-button-secondary"
                    onClick={() => navigate('/researcher-dashboard/manage-quizzes')}
                    style={{width: 'auto', fontSize: '0.9rem', padding: '0.5rem 1rem'}}
                >
                    ← Back
                </button>
            </div>

            <form onSubmit={handleSubmit}>
                <fieldset className="form-fieldset">
                    <legend className="form-legend">Survey Details</legend>
                    <div className="form-group">
                        <label className="form-label">Title:</label>
                        <input className="form-input" value={title} onChange={e => setTitle(e.target.value)} placeholder="e.g., Pre-Study Demographics" required />
                    </div>
                    <div className="form-group">
                        <label className="form-label">Description:</label>
                        <textarea className="form-textarea" value={description} onChange={e => setDescription(e.target.value)} placeholder="Instructions for participants..." />
                    </div>
                </fieldset>

                {questions.map((q, qIndex) => (
                    <fieldset key={qIndex} className="form-fieldset">
                        <legend className="form-legend">Question {qIndex + 1}</legend>
                        <div className="form-group">
                            <label className="form-label">Question Text:</label>
                            <input className="form-input" value={q.questionText} onChange={e => handleQuestionChange(qIndex, 'questionText', e.target.value)} required />
                        </div>
                        <div className="form-group">
                            <label className="form-label">Answer Type:</label>
                            <select className="form-select" value={q.questionType} onChange={e => handleQuestionChange(qIndex, 'questionType', e.target.value)}>
                                <option value="SHORT_ANSWER">Short Text (e.g., Name, Job Title)</option>
                                <option value="PARAGRAPH">Long Paragraph (e.g., Experience Description)</option>
                                <option value="MULTIPLE_CHOICE">Multiple Choice (e.g., Age Group)</option>
                            </select>
                        </div>

                        {/* Render Options ONLY for Multiple Choice */}
                        {q.questionType === 'MULTIPLE_CHOICE' && (
                            <>
                                <h4 className="option-group-header">Options</h4>
                                {q.options.map((option, oIndex) => (
                                    <div key={oIndex} className="option-item">
                                        <span style={{marginRight:'10px', color:'#aaa'}}>•</span>
                                        <input
                                            className="form-input"
                                            value={option.optionText}
                                            onChange={e => handleOptionChange(qIndex, oIndex, e.target.value)}
                                            placeholder={`Option ${oIndex + 1}`}
                                            required
                                        />
                                    </div>
                                ))}
                                <button type="button" onClick={() => addOption(qIndex)} className="form-button form-button-secondary">+ Add Option</button>
                            </>
                        )}
                    </fieldset>
                ))}

                <button type="button" onClick={addQuestion} className="form-button form-button-add">+ Add Question</button>

                <hr style={{ margin: '2rem 0', borderColor: '#444' }} />

                <button type="submit" className="form-button form-button-submit">Save Questionnaire</button>
                {message && <p className={messageClass}>{message.text}</p>}
            </form>
        </div>
    );
};

export default CreateQuestionnaire;