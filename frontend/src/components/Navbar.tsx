import React from 'react';
import { Link, useLocation } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { CheckSquare, BarChart3, LogIn, LogOut, ShieldAlert, UserPlus } from 'lucide-react';
import './Navbar.scss';

export const Navbar: React.FC = () => {
  const { authenticated, user, login, register, logout } = useAuth();
  const location = useLocation();

  return (
    <header className="navbar">
      <div className="navbar__container">
        <Link to="/" className="navbar__brand">
          <div className="navbar__logo">
            <CheckSquare size={22} className="navbar__logo-icon" />
          </div>
          <div className="navbar__brand-text">
            <span className="navbar__brand-title">DevTrack</span>
            <span className="navbar__brand-tag">Enterprise</span>
          </div>
        </Link>

        {authenticated && (
          <nav className="navbar__nav">
            <Link
              to="/"
              className={`navbar__link ${location.pathname === '/' ? 'navbar__link--active' : ''}`}
            >
              <CheckSquare size={16} />
              My Tasks
            </Link>

            {user?.isAdmin && (
              <Link
                to="/admin"
                className={`navbar__link ${location.pathname === '/admin' ? 'navbar__link--active' : ''}`}
              >
                <BarChart3 size={16} />
                System Metrics
                <span className="navbar__admin-chip">Admin</span>
              </Link>
            )}
          </nav>
        )}

        <div className="navbar__actions">
          {authenticated && user ? (
            <div className="navbar__user-panel">
              <div className="navbar__user-info">
                <span className="navbar__username">{user.username}</span>
                <span className="navbar__user-role">
                  {user.isAdmin ? (
                    <span className="role-tag role-tag--admin">
                      <ShieldAlert size={10} /> Admin
                    </span>
                  ) : (
                    <span className="role-tag role-tag--user">Developer</span>
                  )}
                </span>
              </div>
              <button
                type="button"
                onClick={logout}
                className="navbar__btn navbar__btn--logout"
                title="Logout from Keycloak session"
              >
                <LogOut size={16} />
                <span>Logout</span>
              </button>
            </div>
          ) : (
            <div className="navbar__auth-buttons">
              <button
                type="button"
                onClick={register}
                className="navbar__btn navbar__btn--register"
                title="Create a new DevTrack account via Keycloak"
              >
                <UserPlus size={16} />
                <span>Create Account</span>
              </button>
              <button
                type="button"
                onClick={login}
                className="navbar__btn navbar__btn--login"
                title="Sign in with Keycloak"
              >
                <LogIn size={16} />
                <span>Sign In</span>
              </button>
            </div>
          )}
        </div>
      </div>
    </header>
  );
};
