import React, { useEffect, useState, useCallback } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { jobApi } from '../../api/services';
import './JobsPage.css';

const STATUS_OPTIONS = ['', 'APPLIED', 'INTERVIEWING', 'OFFER', 'REJECTED', 'WITHDRAWN'];

function statusBadgeClass(status) {
  return `badge badge-${status.toLowerCase()}`;
}

function ScoreRing({ score }) {
  if (score == null) return <span className="text-muted text-sm">—</span>;
  const cls = score >= 70 ? 'high' : score >= 45 ? 'medium' : 'low';
  return <div className={`score-ring ${cls}`} style={{ width: 40, height: 40, fontSize: '0.85rem' }}>{score}</div>;
}

export default function JobsPage() {
  const navigate = useNavigate();
  const [jobs, setJobs] = useState([]);
  const [loading, setLoading] = useState(true);
  const [status, setStatus] = useState('');
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(1);
  const [deleting, setDeleting] = useState(null);

  const load = useCallback(() => {
    setLoading(true);
    const params = { page, size: 20 };
    if (status) params.status = status;
    jobApi.list(params)
      .then(res => {
        setJobs(res.data.data.content);
        setTotalPages(res.data.data.totalPages);
      })
      .finally(() => setLoading(false));
  }, [page, status]);

  useEffect(() => { load(); }, [load]);

  const handleDelete = async (id) => {
    if (!window.confirm('Delete this application? This cannot be undone.')) return;
    setDeleting(id);
    await jobApi.delete(id);
    setDeleting(null);
    load();
  };

  return (
    <div className="fade-in">
      <div className="page-header flex items-center justify-between">
        <div>
          <h1>Applications</h1>
          <p>Track every job you've applied for.</p>
        </div>
        <Link to="/jobs/new" className="btn btn-primary">+ New Application</Link>
      </div>

      {/* Filter bar */}
      <div className="jobs-filter-bar">
        {STATUS_OPTIONS.map(s => (
          <button
            key={s}
            className={`filter-chip ${status === s ? 'filter-chip--active' : ''}`}
            onClick={() => { setStatus(s); setPage(0); }}
          >
            {s || 'All'}
          </button>
        ))}
      </div>

      {loading ? (
        <div className="loading-center"><div className="spinner" /><span>Loading…</span></div>
      ) : jobs.length === 0 ? (
        <div className="empty-state">
          <span style={{ fontSize: '2.5rem', opacity: 0.3 }}>⊡</span>
          <h3>No applications yet</h3>
          <p>Start tracking your job search by adding your first application.</p>
          <Link to="/jobs/new" className="btn btn-primary">Add Application</Link>
        </div>
      ) : (
        <>
          <div className="card" style={{ padding: 0, overflow: 'hidden' }}>
            <table className="data-table">
              <thead>
                <tr>
                  <th>Company / Role</th>
                  <th>Status</th>
                  <th>Applied</th>
                  <th>AI Match</th>
                  <th style={{ width: 120 }}>Actions</th>
                </tr>
              </thead>
              <tbody>
                {jobs.map(job => (
                  <tr key={job.id} style={{ cursor: 'pointer' }}>
                    <td onClick={() => navigate(`/jobs/${job.id}`)}>
                      <div className="job-company">{job.company}</div>
                      <div className="job-role">{job.roleTitle}</div>
                      {job.location && <div className="job-location">{job.location}</div>}
                    </td>
                    <td>
                      <span className={statusBadgeClass(job.status)}>{job.status}</span>
                    </td>
                    <td className="text-secondary text-sm">
                      {new Date(job.appliedDate).toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric' })}
                    </td>
                    <td><ScoreRing score={job.aiMatchScore} /></td>
                    <td>
                      <div className="flex gap-2">
                        <button
                          className="btn btn-ghost btn-sm"
                          onClick={() => navigate(`/jobs/${job.id}`)}
                        >View</button>
                        <button
                          className="btn btn-danger btn-sm"
                          disabled={deleting === job.id}
                          onClick={() => handleDelete(job.id)}
                        >Del</button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          {/* Pagination */}
          {totalPages > 1 && (
            <div className="flex items-center gap-3 mt-6" style={{ justifyContent: 'center' }}>
              <button className="btn btn-secondary btn-sm" disabled={page === 0} onClick={() => setPage(p => p - 1)}>← Prev</button>
              <span className="text-muted text-sm">Page {page + 1} of {totalPages}</span>
              <button className="btn btn-secondary btn-sm" disabled={page >= totalPages - 1} onClick={() => setPage(p => p + 1)}>Next →</button>
            </div>
          )}
        </>
      )}
    </div>
  );
}
