import React, { createContext, useContext, useState, useCallback } from 'react';
import { authApi, userApi } from '../api/services';

const AuthContext = createContext(null);

const TOKEN_KEY = 'careerpilot_token';
const USER_KEY  = 'careerpilot_user';

export function AuthProvider({ children }) {
  const [user, setUser] = useState(() => {
    try {
      const stored = localStorage.getItem(USER_KEY);
      return stored ? JSON.parse(stored) : null;
    } catch {
      return null;
    }
  });

  const [loading, setLoading] = useState(false);

  // ----------------------------------------------------------------
  // Login
  // ----------------------------------------------------------------
  const login = useCallback(async (email, password) => {
    setLoading(true);
    try {
      const res = await authApi.login({ email, password });
      const { accessToken, ...userInfo } = res.data.data;
      localStorage.setItem(TOKEN_KEY, accessToken);
      localStorage.setItem(USER_KEY, JSON.stringify(userInfo));
      setUser(userInfo);
      return { success: true };
    } catch (err) {
      return {
        success: false,
        error: err.response?.data?.error || 'Login failed. Please try again.',
      };
    } finally {
      setLoading(false);
    }
  }, []);

  // ----------------------------------------------------------------
  // Register
  // ----------------------------------------------------------------
  const register = useCallback(async (fullName, email, password) => {
    setLoading(true);
    try {
      const res = await authApi.register({ fullName, email, password });
      const { accessToken, ...userInfo } = res.data.data;
      localStorage.setItem(TOKEN_KEY, accessToken);
      localStorage.setItem(USER_KEY, JSON.stringify(userInfo));
      setUser(userInfo);
      return { success: true };
    } catch (err) {
      return {
        success: false,
        error: err.response?.data?.error || 'Registration failed. Please try again.',
      };
    } finally {
      setLoading(false);
    }
  }, []);

  // ----------------------------------------------------------------
  // Logout
  // ----------------------------------------------------------------
  const logout = useCallback(() => {
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(USER_KEY);
    setUser(null);
  }, []);

  // ----------------------------------------------------------------
  // Refresh profile from server
  // ----------------------------------------------------------------
  const refreshUser = useCallback(async () => {
    try {
      const res = await userApi.getProfile();
      const updated = res.data.data;
      localStorage.setItem(USER_KEY, JSON.stringify(updated));
      setUser(updated);
    } catch {
      // silent
    }
  }, []);

  return (
    <AuthContext.Provider value={{ user, loading, login, register, logout, refreshUser }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used within AuthProvider');
  return ctx;
}
