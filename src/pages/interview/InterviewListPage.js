import React, { useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { interviewApi, jobApi } from '../../api/services';
import './InterviewPages.css';

function SessionCard({ session, onDelete }) {
  const navigate = useNavigate();
  const progress = session.totalQuestions > 0
    ? Math.round((session.answeredCount / session.totalQuestions) * 100)
    : 0;

  return (
    <div className="session-card card" onClick={() => navigate(`/interviews/${session.id}`)} style={{ cursor: 'pointer' }}>
      <div className="session-card-header">
        <div>
          <div className="session-company">{session.company}</div>
          <div className="session-role">{session.roleTitle}</div>
        </div>
        <div className="flex gap-2 items-center">
          <span className={`badge badge-${session.status.toLowerCase().replace('_', '-')}`}>
            {session.status.replace('_', ' ')}
          </span>
          {session.overallScore && (
            <div className={`score-ring ${session.overallScore >= 7 ? 'high' : session.overallScore >= 5 ? 'medium' : 'low'}`}>
              {session.overallScore}
            </div>
          )}
        </div>
      </div>
      <div className="session-progress-wrap">
        <div className="session-progress-bar" style={{ width: `${progress}%` }} />
      </div>
      <div className="session-meta">
        <span>{session.answeredCount}/{session.totalQuestions} questions</span>
        <span>{new Date(session.createdAt).toLocaleDateString()}</span>
      </div>
    </div>
  );
}

export default function InterviewListPage() {
  const [sessions, setSessions] = useState([]);
  const [loading, setLoading] = useState(true);
  const [showModal, setShowModal] = useState(false);
  const [jobs, setJobs] = useState([]);
  const [startForm, setStartForm] = useState({ jobApplicationId: '', technicalCount: 3, behavioralCount: 2, projectCount: 1 });
  const [starting, setStarting] = useState(false);
  const [startError, setStartError] = useState('');
  const navigate = useNavigate();

  useEffect(() => {
    Promise.all([
      interviewApi.list({ size: 50 }),
      jobApi.list({ size: 100 }),
    ]).then(([sessRes, jobRes]) => {
      setSessions(sessRes.data.data.content);
      setJobs(jobRes.data.data.content);
    }).finally(() => setLoading(false));
  }, []);

  const handleStart = async (e) => {
    e.preventDefault();
    setStartError('');
    setStarting(true);
    try {
      const res = await interviewApi.startSession({
        jobApplicationId: Number(startForm.jobApplicationId),
        technicalCount: Number(startForm.technicalCount),
        behavioralCount: Number(startForm.behavioralCount),
        projectCount: Number(startForm.projectCount),
      });
      navigate(`/interviews/${res.data.data.id}`);
    } catch (err) {
      setStartError(err.response?.data?.error || 'Failed to start session.');
      setStarting(false);
    }
  };

  return (
    <div className="fade-in">
      <div className="page-header flex items-center justify-between">
        <div>
          <h1>Mock Interviews</h1>
          <p>Practice with AI-generated questions tailored to each job.</p>
        </div>
        <button className="btn btn-primary" onClick={() => setShowModal(true)}>
          ◎ Start Session
        </button>
      </div>

      {loading ? (
        <div className="loading-center"><div className="spinner" /></div>
      ) : sessions.length === 0 ? (
        <div className="empty-state">
          <span style={{ fontSize: '2.5rem', opacity: 0.3 }}>◎</span>
          <h3>No interviews yet</h3>
          <p>Start a mock interview session for any of your job applications.</p>
          <button className="btn btn-primary" onClick={() => setShowModal(true)}>Start Session</button>
        </div>
      ) : (
        <div className="sessions-grid">
          {sessions.map(s => <SessionCard key={s.id} session={s} />)}
        </div>
      )}

      {/* Start session modal */}
      {showModal && (
        <div className="modal-overlay" onClick={() => setShowModal(false)}>
          <div className="modal-card" onClick={e => e.stopPropagation()}>
            <h2 className="modal-title">Start Mock Interview</h2>
            <p className="text-secondary text-sm mb-4">AI will generate questions tailored to the job description.</p>

            {startError && <div className="alert alert-error">{startError}</div>}

            <form onSubmit={handleStart}>
              <div className="form-group">
                <label className="form-label">Job Application *</label>
                <select
                  className="form-select"
                  value={startForm.jobApplicationId}
                  onChange={e => setStartForm(f => ({ ...f, jobApplicationId: e.target.value }))}
                  required
                >
                  <option value="">Select a job…</option>
                  {jobs.map(j => (
                    <option key={j.id} value={j.id}>{j.company} — {j.roleTitle}</option>
                  ))}
                </select>
              </div>

              <div className="grid-3">
                {[
                  { key: 'technicalCount', label: 'Technical' },
                  { key: 'behavioralCount', label: 'Behavioral' },
                  { key: 'projectCount', label: 'Project' },
                ].map(({ key, label }) => (
                  <div className="form-group" key={key}>
                    <label className="form-label">{label}</label>
                    <input
                      type="number" min={0} max={10}
                      className="form-input"
                      value={startForm[key]}
                      onChange={e => setStartForm(f => ({ ...f, [key]: e.target.value }))}
                    />
                  </div>
                ))}
              </div>

              <div className="flex gap-3 mt-4">
                <button type="submit" className="btn btn-primary" disabled={starting}>
                  {starting ? 'Generating questions…' : '✦ Start Interview'}
                </button>
                <button type="button" className="btn btn-secondary" onClick={() => setShowModal(false)}>Cancel</button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
