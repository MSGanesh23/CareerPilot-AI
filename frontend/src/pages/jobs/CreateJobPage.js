import React, { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { jobApi } from '../../api/services';

export default function CreateJobPage() {
  const navigate = useNavigate();
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');
  const [form, setForm] = useState({
    company: '', roleTitle: '', jobDescription: '',
    location: '', jobUrl: '', appliedDate: new Date().toISOString().split('T')[0], notes: '',
  });

  const set = (key) => (e) => setForm(f => ({ ...f, [key]: e.target.value }));

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setSaving(true);
    try {
      const res = await jobApi.create(form);
      navigate(`/jobs/${res.data.data.id}`);
    } catch (err) {
      setError(err.response?.data?.error || 'Failed to create application.');
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="fade-in" style={{ maxWidth: 700 }}>
      <div className="page-header">
        <Link to="/jobs" className="btn btn-ghost btn-sm" style={{ marginBottom: 12 }}>← Back to Applications</Link>
        <h1>New Application</h1>
        <p>Add a job you've applied to. AI will analyze your resume against the job description automatically.</p>
      </div>

      {error && <div className="alert alert-error">{error}</div>}

      <form onSubmit={handleSubmit} className="card" style={{ display: 'flex', flexDirection: 'column', gap: 0 }}>
        <div className="grid-2">
          <div className="form-group">
            <label className="form-label">Company *</label>
            <input className="form-input" placeholder="Acme Corp" value={form.company} onChange={set('company')} required />
          </div>
          <div className="form-group">
            <label className="form-label">Role Title *</label>
            <input className="form-input" placeholder="Senior Engineer" value={form.roleTitle} onChange={set('roleTitle')} required />
          </div>
        </div>

        <div className="grid-2">
          <div className="form-group">
            <label className="form-label">Location</label>
            <input className="form-input" placeholder="Remote / NYC" value={form.location} onChange={set('location')} />
          </div>
          <div className="form-group">
            <label className="form-label">Applied Date *</label>
            <input className="form-input" type="date" value={form.appliedDate} onChange={set('appliedDate')} required />
          </div>
        </div>

        <div className="form-group">
          <label className="form-label">Job URL</label>
          <input className="form-input" type="url" placeholder="https://..." value={form.jobUrl} onChange={set('jobUrl')} />
        </div>

        <div className="form-group">
          <label className="form-label">Job Description *</label>
          <textarea
            className="form-textarea"
            placeholder="Paste the full job description here. AI uses this to analyze your resume match and generate interview questions."
            value={form.jobDescription}
            onChange={set('jobDescription')}
            rows={10}
            required
            minLength={50}
          />
        </div>

        <div className="form-group">
          <label className="form-label">Notes</label>
          <textarea className="form-textarea" placeholder="Referral, recruiter name, salary range, etc." value={form.notes} onChange={set('notes')} rows={3} />
        </div>

        <div className="flex gap-3" style={{ paddingTop: 8 }}>
          <button type="submit" className="btn btn-primary btn-lg" disabled={saving}>
            {saving ? <><span className="spinner" style={{ width: 16, height: 16 }} /> Saving…</> : '✦ Save Application'}
          </button>
          <Link to="/jobs" className="btn btn-secondary btn-lg">Cancel</Link>
        </div>
      </form>
    </div>
  );
}
