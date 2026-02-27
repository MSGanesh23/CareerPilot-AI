import React from 'react';
import { NavLink, useNavigate } from 'react-router-dom';
import { useAuth } from '../../auth/AuthContext';
import './Sidebar.css';

const NAV_ITEMS = [
  { to: '/dashboard',  label: 'Dashboard',   icon: '◈' },
  { to: '/jobs',       label: 'Applications', icon: '⊡' },
  { to: '/resumes',    label: 'Resumes',      icon: '⊟' },
  { to: '/interviews', label: 'Interviews',   icon: '◎' },
  { to: '/analytics',  label: 'Analytics',    icon: '⊕' },
];

export default function Sidebar() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  const initials = user?.fullName
    ? user.fullName.split(' ').map(n => n[0]).join('').slice(0, 2).toUpperCase()
    : '??';

  return (
    <aside className="sidebar">
      {/* Logo */}
      <div className="sidebar-logo">
        <span className="sidebar-logo-mark">✦</span>
        <span className="sidebar-logo-text">CareerPilot</span>
        <span className="sidebar-logo-ai">AI</span>
      </div>

      {/* Nav */}
      <nav className="sidebar-nav">
        {NAV_ITEMS.map(({ to, label, icon }) => (
          <NavLink
            key={to}
            to={to}
            className={({ isActive }) =>
              `sidebar-link ${isActive ? 'sidebar-link--active' : ''}`
            }
          >
            <span className="sidebar-icon">{icon}</span>
            <span>{label}</span>
          </NavLink>
        ))}
      </nav>

      {/* Bottom section */}
      <div className="sidebar-bottom">
        <NavLink
          to="/profile"
          className={({ isActive }) =>
            `sidebar-link ${isActive ? 'sidebar-link--active' : ''}`
          }
        >
          <span className="sidebar-icon">◷</span>
          <span>Profile</span>
        </NavLink>

        <div className="sidebar-user">
          <div className="sidebar-avatar">{initials}</div>
          <div className="sidebar-user-info">
            <div className="sidebar-user-name">{user?.fullName}</div>
            <div className="sidebar-user-email">{user?.email}</div>
          </div>
        </div>

        <button className="sidebar-logout" onClick={handleLogout}>
          <span>↪</span> Sign out
        </button>
      </div>
    </aside>
  );
}
