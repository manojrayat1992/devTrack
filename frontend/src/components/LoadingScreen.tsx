import React from 'react';
import { RefreshCw, CheckSquare } from 'lucide-react';
import './LoadingScreen.scss';

interface LoadingScreenProps {
  message?: string;
}

export const LoadingScreen: React.FC<LoadingScreenProps> = ({
  message = 'Authenticating with DevTrack security services...',
}) => {
  return (
    <div className="loading-screen" role="status" aria-live="polite">
      <div className="loading-screen__card">
        <div className="loading-screen__logo">
          <CheckSquare size={32} className="loading-screen__logo-icon" />
        </div>
        <div className="loading-screen__spinner">
          <RefreshCw size={28} className="spin-icon" />
        </div>
        <h2 className="loading-screen__title">DevTrack Security</h2>
        <p className="loading-screen__message">{message}</p>
        <span className="loading-screen__hint">
          Verifying OpenID Connect session with Keycloak...
        </span>
      </div>
    </div>
  );
};
