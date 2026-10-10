import React from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { ShieldAlert, Lock, ArrowLeft, LogIn, UserPlus } from 'lucide-react';
import './UnauthorizedScreen.scss';

interface UnauthorizedScreenProps {
  type?: 'unauthenticated' | 'forbidden' | 'error';
  title?: string;
  description?: string;
  requiredRole?: string;
}

export const UnauthorizedScreen: React.FC<UnauthorizedScreenProps> = ({
  type = 'forbidden',
  title,
  description,
  requiredRole = 'ADMIN',
}) => {
  const navigate = useNavigate();
  const { authenticated, user, login, register } = useAuth();

  const isForbidden = type === 'forbidden' || (authenticated && !user?.isAdmin);

  return (
    <div className="unauthorized-screen">
      <div className="unauthorized-card">
        <div className="unauthorized-card__icon">
          {isForbidden ? (
            <ShieldAlert size={44} className="shield-icon" />
          ) : (
            <Lock size={44} className="lock-icon" />
          )}
        </div>

        <div className="unauthorized-card__status-badge">
          {isForbidden ? '403 Forbidden' : '401 Unauthorized'}
        </div>

        <h1 className="unauthorized-card__title">
          {title ||
            (isForbidden
              ? 'Administrator Access Required'
              : 'Authentication Session Required')}
        </h1>

        <p className="unauthorized-card__description">
          {description ||
            (isForbidden
              ? `Your account (${user?.username || 'User'}) is provisioned with role '${user?.roles?.join(', ') || 'USER'}'. This resource requires the privileged '${requiredRole}' role.`
              : 'You must authenticate through Keycloak to access this developer workspace.')}
        </p>

        {isForbidden && (
          <div className="unauthorized-card__policy-box">
            <strong>Security Boundary Policy:</strong>
            <p>
              DevTrack enforces strict Role-Based Access Control (RBAC). Standard developers have
              access to their own tasks and metrics. The <code>ROLE_{requiredRole}</code> authority
              can only be granted by an administrator through Keycloak's administrative console or CLI.
            </p>
          </div>
        )}

        <div className="unauthorized-card__actions">
          {authenticated ? (
            <button
              type="button"
              onClick={() => navigate('/')}
              className="btn-primary"
            >
              <ArrowLeft size={16} />
              <span>Return to My Tasks</span>
            </button>
          ) : (
            <div className="unauthorized-card__auth-buttons">
              <button
                type="button"
                onClick={login}
                className="btn-primary"
              >
                <LogIn size={16} />
                <span>Sign In with Keycloak</span>
              </button>
              <button
                type="button"
                onClick={register}
                className="btn-secondary"
              >
                <UserPlus size={16} />
                <span>Create New Account</span>
              </button>
            </div>
          )}
        </div>
      </div>
    </div>
  );
};
