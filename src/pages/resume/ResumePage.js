import React, { useEffect, useState, useRef } from 'react';
import { resumeApi } from '../../api/services';
import './ResumePage.css';

export default function ResumePage() {
  const [resumes, setResumes] = useState([]);
  const [loading, setLoading] = useState(true);
  const [uploading, setUploading] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [label, setLabel] = useState('');
  const [setAsActive, setSetAsActive] = useState(true);
  const fileRef = useRef();

  const load = () => {
    setLoading(true);
    resumeApi.list().then(res => setResumes(res.data.data.content)).finally(() => setLoading(false));
  };

  useEffect(() => { load(); }, []);

  const handleUpload = async (e) => {
    e.preventDefault();
    const file = fileRef.current.files[0];
    if (!file) return;
    setUploading(true);
    setError('');
    setSuccess('');
    const fd = new FormData();
    fd.append('file', file);
    if (label) fd.append('label', label);
    fd.append('setAsActive', String(setAsActive));
    try {
      await resumeApi.upload(fd);
      setSuccess('Resume uploaded successfully!');
      setLabel('');
      fileRef.current.value = '';
      load();
    } catch (err) {
      setError(err.response?.data?.error || 'Upload failed.');
    } finally {
      setUploading(false);
    }
  };

  const handleSetActive = async (id) => {
    await resumeApi.update(id, { setAsActive: true });
    load();
  };

  const handleDelete = async (id) => {
    if (!window.confirm('Delete this resume version?')) return;
    await resumeApi.delete(id);
    load();
  };

  return (
    <div className="fade-in">
      <div className="page-header">
        <h1>Resumes</h1>
        <p>Upload and manage multiple resume versions. The active version is used for AI analysis.</p>
      </div>

      {/* Upload form */}
      <div className="card mb-6">
        <div className="card-header"><span className="card-title">Upload New Resume</span></div>
        {error && <div className="alert alert-error">{error}</div>}
        {success && <div className="alert alert-success">{success}</div>}

        <form onSubmit={handleUpload}>
          <div className="grid-2">
            <div className="form-group">
              <label className="form-label">File (PDF, DOC, DOCX) *</label>
              <input ref={fileRef} type="file" className="form-input" accept=".pdf,.doc,.docx" required />
            </div>
            <div className="form-group">
              <label className="form-label">Label (optional)</label>
              <input className="form-input" placeholder="e.g. Backend-Senior-v3" value={label} onChange={e => setLabel(e.target.value)} />
            </div>
          </div>

          <div className="flex items-center gap-3">
            <label className="flex items-center gap-2" style={{ cursor: 'pointer' }}>
              <input
                type="checkbox"
                checked={setAsActive}
                onChange={e => setSetAsActive(e.target.checked)}
                style={{ accentColor: 'var(--accent)' }}
              />
              <span className="text-secondary text-sm">Set as active resume</span>
            </label>
            <button type="submit" className="btn btn-primary" disabled={uploading}>
              {uploading ? <><span className="spinner" style={{ width: 14, height: 14 }} /> Uploading…</> : '↑ Upload'}
            </button>
          </div>
        </form>
      </div>

      {/* Resume list */}
      {loading ? (
        <div className="loading-center"><div className="spinner" /></div>
      ) : resumes.length === 0 ? (
        <div className="empty-state">
          <span style={{ fontSize: '2.5rem', opacity: 0.3 }}>⊟</span>
          <h3>No resumes yet</h3>
          <p>Upload your resume to enable AI analysis on job applications.</p>
        </div>
      ) : (
        <div className="resume-list">
          {resumes.map(r => (
            <div key={r.id} className={`card resume-card ${r.active ? 'resume-card--active' : ''}`}>
              <div className="resume-card-header">
                <div className="resume-info">
                  <div className="resume-name">{r.label || r.fileName}</div>
                  <div className="resume-meta">
                    v{r.version} · {r.fileName}
                    {r.fileSize && ` · ${(r.fileSize / 1024).toFixed(0)}KB`}
                    {!r.hasParsedText && <span className="text-warning"> · ⚠ No text extracted</span>}
                  </div>
                  <div className="resume-date text-muted text-sm">
                    {new Date(r.createdAt).toLocaleDateString('en-US', { month: 'long', day: 'numeric', year: 'numeric' })}
                  </div>
                </div>
                <div className="resume-actions">
                  {r.active ? (
                    <span className="badge badge-offer">Active</span>
                  ) : (
                    <button className="btn btn-secondary btn-sm" onClick={() => handleSetActive(r.id)}>
                      Set Active
                    </button>
                  )}
                  {!r.active && (
                    <button className="btn btn-danger btn-sm" onClick={() => handleDelete(r.id)}>Delete</button>
                  )}
                </div>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
