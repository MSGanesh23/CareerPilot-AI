import React, { useEffect, useState } from 'react';
import { userApi } from '../../api/services';
import { useAuth } from '../../auth/AuthContext';

export default function ProfilePage() {
  const { refreshUser } = useAuth();
  const [form, setForm] = useState({ fullName: '', yearsExperience: '', skills: '', targetRoles: '' });
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [success, setSuccess] = useState('');
  const [error, setError] = useState('');

  useEffect(() => {
    userApi.getProfile().then(res => {
      const d = res.data.data;
      setForm({
        fullName: d.fullName || '',
        yearsExperience: d.yearsExperience ?? '',
        skills: (d.skills || []).join(', '),
        targetRoles: (d.targetRoles || []).join(', '),
      });
    }).finally(() => setLoading(false));
  }, []);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setSaving(true);
    setError('');
    setSuccess('');
    try {
      const payload = {
        fullName: form.fullName,
        yearsExperience: form.yearsExperience ? Number(form.yearsExperience) : null,
        skills: form.skills.split(',').map(s => s.trim()).filter(Boolean),
        targetRoles: form.targetRoles.split(',').map(s => s.trim()).filter(Boolean),
      };
      await userApi.updateProfile(payload);
      await refreshUser();
      setSuccess('Profile updated successfully.');
    } catch (err) {
      setError(err.response?.data?.error || 'Failed to update profile.');
    } finally {
      setSaving(false);
    }
  };

  if (loading) return <div className="loading-center"><div className="spinner" /></div>;

  return (
    <div className="fade-in" style={{ maxWidth: 600 }}>
      <div className="page-header">
        <h1>Profile</h1>
        <p>Keep your profile up-to-date for better AI analysis results.</p>
      </div>

      {error && <div className="alert alert-error">{error}</div>}
      {success && <div className="alert alert-success">{success}</div>}

      <form className="card" onSubmit={handleSubmit}>
        <div className="form-group">
          <label className="form-label">Full Name</label>
          <input className="form-input" value={form.fullName} onChange={e => setForm(f => ({ ...f, fullName: e.target.value }))} required />
        </div>

        <div className="form-group">
          <label className="form-label">Years of Experience</label>
          <input className="form-input" type="number" min={0} max={50} value={form.yearsExperience} onChange={e => setForm(f => ({ ...f, yearsExperience: e.target.value }))} placeholder="e.g. 5" />
        </div>

        <div className="form-group">
          <label className="form-label">Skills</label>
          <textarea
            className="form-textarea"
            rows={3}
            placeholder="Java, Spring Boot, React, PostgreSQL, Docker…"
            value={form.skills}
            onChange={e => setForm(f => ({ ...f, skills: e.target.value }))}
          />
          <span className="text-muted text-sm">Comma-separated list of skills</span>
        </div>

        <div className="form-group">
          <label className="form-label">Target Roles</label>
          <textarea
            className="form-textarea"
            rows={2}
            placeholder="Senior Backend Engineer, Staff Engineer, Tech Lead…"
            value={form.targetRoles}
            onChange={e => setForm(f => ({ ...f, targetRoles: e.target.value }))}
          />
          <span className="text-muted text-sm">Comma-separated list of target roles</span>
        </div>

        <button type="submit" className="btn btn-primary" disabled={saving}>
          {saving ? 'Saving…' : '✦ Save Profile'}
        </button>
      </form>
    </div>
  );
}
