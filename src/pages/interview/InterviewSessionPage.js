import React, { useEffect, useState } from 'react';
import { useParams, Link, useNavigate } from 'react-router-dom';
import { interviewApi } from '../../api/services';
import './InterviewPages.css';

function QuestionCard({ question, onSubmit, submitting, activeId, setActiveId }) {
  const [answer, setAnswer] = useState('');
  const isActive = activeId === question.id;
  const isAnswered = question.answered;

  return (
    <div className={`question-card ${isAnswered ? 'question-card--done' : ''} ${isActive ? 'question-card--active' : ''}`}>
      <div
        className="question-header"
        onClick={() => setActiveId(isActive ? null : question.id)}
      >
        <div className="question-meta">
          <span className={`q-type-badge q-type--${question.questionType.toLowerCase()}`}>
            {question.questionType}
          </span>
          <span className="q-seq">#{question.sequenceOrder}</span>
        </div>
        <div className="question-title">{question.questionText}</div>
        <div className="flex items-center gap-3" style={{ marginTop: 8 }}>
          {isAnswered && question.aiScore != null && (
            <div className={`score-ring ${question.aiScore >= 7 ? 'high' : question.aiScore >= 5 ? 'medium' : 'low'}`}>
              {question.aiScore}
            </div>
          )}
          {isAnswered && <span className="text-success text-sm">✓ Answered</span>}
          {!isAnswered && <span className="text-muted text-sm">Click to answer</span>}
          <span className="q-chevron">{isActive ? '▲' : '▽'}</span>
        </div>
      </div>

      {/* Expanded */}
      {isActive && (
        <div className="question-body">
          {!isAnswered ? (
            <>
              <textarea
                className="form-textarea"
                placeholder="Type your answer here. Be specific and use examples where possible."
                rows={7}
                value={answer}
                onChange={e => setAnswer(e.target.value)}
                autoFocus
              />
              <button
                className="btn btn-primary mt-4"
                disabled={submitting || answer.trim().length < 10}
                onClick={() => onSubmit(question.id, answer)}
              >
                {submitting ? <><span className="spinner" style={{ width: 14, height: 14 }} /> Evaluating…</> : '✦ Submit Answer'}
              </button>
            </>
          ) : (
            <div className="answer-review">
              <div className="answer-section">
                <div className="answer-section-label">Your Answer</div>
                <div className="answer-text">{question.userAnswer}</div>
              </div>
              <div className="answer-section">
                <div className="answer-section-label">AI Feedback</div>
                <div className="answer-feedback">{question.aiFeedback}</div>
              </div>
              <div className="answer-section">
                <div className="answer-section-label">Ideal Answer</div>
                <div className="answer-ideal">{question.idealAnswer}</div>
              </div>
            </div>
          )}
        </div>
      )}
    </div>
  );
}

export default function InterviewSessionPage() {
  const { sessionId } = useParams();
  const navigate = useNavigate();
  const [session, setSession] = useState(null);
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [activeId, setActiveId] = useState(null);
  const [error, setError] = useState('');

  const loadSession = () => {
    interviewApi.get(sessionId)
      .then(res => {
        setSession(res.data.data);
        // Auto-open first unanswered question
        const first = res.data.data.questions?.find(q => !q.answered);
        if (first) setActiveId(first.id);
      })
      .finally(() => setLoading(false));
  };

  useEffect(() => { loadSession(); }, [sessionId]);

  const handleSubmit = async (questionId, answer) => {
    setSubmitting(true);
    setError('');
    try {
      await interviewApi.submitAnswer(sessionId, questionId, { answer });
      loadSession();
      setActiveId(null);
    } catch (err) {
      setError(err.response?.data?.error || 'Failed to submit answer.');
    } finally {
      setSubmitting(false);
    }
  };

  const handleAbandon = async () => {
    if (!window.confirm('Abandon this session? You can still view your answers.')) return;
    await interviewApi.abandon(sessionId);
    loadSession();
  };

  if (loading) return <div className="loading-center"><div className="spinner" /></div>;
  if (!session) return <div className="empty-state"><h3>Session not found</h3><Link to="/interviews" className="btn btn-secondary">Back</Link></div>;

  const progress = session.totalQuestions > 0
    ? Math.round((session.answeredCount / session.totalQuestions) * 100)
    : 0;
  const isCompleted = session.status === 'COMPLETED';
  const isAbandoned = session.status === 'ABANDONED';

  return (
    <div className="fade-in">
      {/* Header */}
      <div className="session-header">
        <Link to="/interviews" className="btn btn-ghost btn-sm">← Interviews</Link>
        <div className="flex items-center justify-between mt-4 flex-wrap gap-4">
          <div>
            <h1>{session.roleTitle}</h1>
            <p className="text-secondary">{session.company}</p>
          </div>
          <div className="flex items-center gap-4">
            {session.overallScore && (
              <div className="session-score-display">
                <div className="session-score-label">Score</div>
                <div className={`session-score-value ${session.overallScore >= 7 ? 'text-success' : 'text-accent'}`}>
                  {session.overallScore}<span style={{ fontSize: '0.9rem', opacity: 0.6 }}>/10</span>
                </div>
              </div>
            )}
            {!isCompleted && !isAbandoned && (
              <button className="btn btn-ghost btn-sm text-danger" onClick={handleAbandon}>
                Abandon
              </button>
            )}
          </div>
        </div>

        {/* Progress bar */}
        <div className="session-progress-header">
          <div className="session-progress-bar-wrap">
            <div className="session-progress-bar-fill" style={{ width: `${progress}%` }} />
          </div>
          <span className="text-muted text-sm">{session.answeredCount}/{session.totalQuestions} answered</span>
        </div>

        {/* Status banners */}
        {isCompleted && (
          <div className="alert alert-success">
            ✓ Interview completed! Your overall score: <strong>{session.overallScore}/10</strong>.
            Review your answers and AI feedback below.
          </div>
        )}
        {isAbandoned && <div className="alert alert-info">This session was abandoned. You can still review your answers.</div>}
      </div>

      {error && <div className="alert alert-error">{error}</div>}

      {/* Questions */}
      <div className="questions-list">
        {session.questions?.map(q => (
          <QuestionCard
            key={q.id}
            question={q}
            onSubmit={handleSubmit}
            submitting={submitting}
            activeId={activeId}
            setActiveId={setActiveId}
          />
        ))}
      </div>
    </div>
  );
}
