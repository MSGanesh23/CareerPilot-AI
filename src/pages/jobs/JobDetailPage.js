import React, { useEffect, useState } from 'react';
import { useParams, Link, useNavigate } from 'react-router-dom';
import { jobApi, interviewApi } from '../../api/services';
import './JobDetailPage.css';

const STATUS_OPTIONS = ['APPLIED', 'INTERVIEWING', 'OFFER', 'REJECTED', 'WITHDRAWN'];

function SkillTag({ label, type }) {
  return (
    <span className={`skill-tag skill-tag--${type}`}>{label}</span>
  );
}

export default function JobDetailPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const [job, setJob] = useState(null);
  const [loading, setLoading] = useState(true);
  const [analyzing, setAnalyzing] = useState(false);
  const [startingInterview, setStartingInterview] = useState(false);
  const [editStatus, setEditStatus] = useState('');
  const [error, setError] = useState('');

  const loadJob = () => {
    setLoading(true);
    jobApi.get(id).then(res => {
      setJob(res.data.data);
      setEditStatus(res.data.data.status);
    }).finally(() => setLoading(false));
  };

  useEffect(() => { loadJob(); }, [id]);

  const handleStatusChange = async (newStatus) => {
    setEditStatus(newStatus);
    await jobApi.update(id, { status: newStatus });
    loadJob();
  };

  const handleAnalyze = async () => {
    setAnalyzing(true);
    setError('');
    try {
      await jobApi.analyze(id);
      loadJob();
    } catch (err) {
      setError(err.response?.data?.error || 'Analysis failed.');
    } finally {
      setAnalyzing(false);
    }
  };

  const handleStartInterview = async () => {
    setStartingInterview(true);
    try {
      const res = await interviewApi.startSession({
        jobApplicationId: Number(id),
        technicalCount: 3,
        behavioralCount: 2,
        projectCount: 1,
      });
      navigate(`/interviews/${res.data.data.id}`);
    } catch (err) {
      setError('Failed to start interview session.');
      setStartingInterview(false);
    }
  };

  if (loading) return <div className="loading-center"><div className="spinner" /><span>Loading…</span></div>;
  if (!job) return <div className="empty-state"><h3>Job not found</h3><Link to="/jobs" className="btn btn-secondary">Back</Link></div>;

  const analysis = job.skillGapAnalysis;
  const score = job.aiMatchScore;

  return (
    <div className="fade-in">
      {/* Header */}
      <div className="job-detail-header">
        <Link to="/jobs" className="btn btn-ghost btn-sm">← Applications</Link>
        <div className="job-detail-title-row">
          <div>
            <h1>{job.roleTitle}</h1>
            <p className="job-detail-company">
              {job.company}
              {job.location && <span className="job-detail-location"> · {job.location}</span>}
            </p>
          </div>
          <div className="job-detail-actions">
            <button
              className="btn btn-secondary"
              onClick={handleAnalyze}
              disabled={analyzing}
            >
              {analyzing ? <><span className="spinner" style={{ width: 14, height: 14 }} /> Analyzing…</> : '⟳ Re-analyze'}
            </button>
            <button
              className="btn btn-primary"
              onClick={handleStartInterview}
              disabled={startingInterview}
            >
              {startingInterview ? 'Starting…' : '◎ Mock Interview'}
            </button>
          </div>
        </div>
      </div>

      {error && <div className="alert alert-error mb-4">{error}</div>}

      <div className="job-detail-grid">
        {/* Left: details */}
        <div className="job-detail-main">
          {/* Status selector */}
          <div className="card mb-4">
            <div className="card-header" style={{ marginBottom: 0 }}>
              <span className="card-title">Status</span>
            </div>
            <div className="status-selector">
              {STATUS_OPTIONS.map(s => (
                <button
                  key={s}
                  className={`status-chip status-chip--${s.toLowerCase()} ${editStatus === s ? 'status-chip--active' : ''}`}
                  onClick={() => handleStatusChange(s)}
                >
                  {s}
                </button>
              ))}
            </div>
          </div>

          {/* Meta */}
          <div className="card mb-4">
            <div className="job-meta-grid">
              <div className="job-meta-item">
                <div className="job-meta-label">Applied</div>
                <div className="job-meta-value">{new Date(job.appliedDate).toLocaleDateString('en-US', { month: 'long', day: 'numeric', year: 'numeric' })}</div>
              </div>
              {job.jobUrl && (
                <div className="job-meta-item">
                  <div className="job-meta-label">Job URL</div>
                  <a href={job.jobUrl} target="_blank" rel="noopener noreferrer" className="job-meta-value" style={{ color: 'var(--accent)' }}>Open listing ↗</a>
                </div>
              )}
              {job.notes && (
                <div className="job-meta-item" style={{ gridColumn: '1 / -1' }}>
                  <div className="job-meta-label">Notes</div>
                  <div className="job-meta-value text-secondary">{job.notes}</div>
                </div>
              )}
            </div>
          </div>

          {/* Job Description */}
          <div className="card">
            <div className="card-header"><span className="card-title">Job Description</span></div>
            <div className="job-description-text">{job.jobDescription}</div>
          </div>
        </div>

        {/* Right: AI analysis */}
        <div className="job-detail-sidebar">
          {/* Score card */}
          <div className="card analysis-score-card mb-4">
            {score != null ? (
              <>
                <div className="analysis-score-label">AI Match Score</div>
                <div className={`analysis-score-value ${score >= 70 ? 'text-success' : score >= 45 ? 'text-accent' : 'text-danger'}`}>
                  {score}<span style={{ fontSize: '1rem', opacity: 0.6 }}>%</span>
                </div>
                <div className="analysis-score-bar-wrap">
                  <div className="analysis-score-bar" style={{
                    width: `${score}%`,
                    background: score >= 70 ? 'var(--success)' : score >= 45 ? 'var(--accent)' : 'var(--danger)'
                  }} />
                </div>
              </>
            ) : (
              <div className="analysis-no-score">
                <p className="text-muted text-sm">No analysis yet.</p>
                <button className="btn btn-secondary btn-sm mt-4" onClick={handleAnalyze} disabled={analyzing}>
                  {analyzing ? 'Analyzing…' : 'Run Analysis'}
                </button>
              </div>
            )}
          </div>

          {/* Skills */}
          {analysis && (
            <>
              {analysis.strongSkills?.length > 0 && (
                <div className="card mb-4">
                  <div className="card-header"><span className="card-title">Strong Skills</span></div>
                  <div className="skill-tags">
                    {analysis.strongSkills.map(s => <SkillTag key={s} label={s} type="strong" />)}
                  </div>
                </div>
              )}

              {analysis.missingSkills?.length > 0 && (
                <div className="card mb-4">
                  <div className="card-header"><span className="card-title">Missing Skills</span></div>
                  <div className="skill-tags">
                    {analysis.missingSkills.map(s => <SkillTag key={s} label={s} type="missing" />)}
                  </div>
                </div>
              )}

              {analysis.improvementSuggestions?.length > 0 && (
                <div className="card">
                  <div className="card-header"><span className="card-title">Suggestions</span></div>
                  <ul className="suggestion-list">
                    {analysis.improvementSuggestions.map((s, i) => (
                      <li key={i} className="suggestion-item">{s}</li>
                    ))}
                  </ul>
                </div>
              )}
            </>
          )}
        </div>
      </div>
    </div>
  );
}
